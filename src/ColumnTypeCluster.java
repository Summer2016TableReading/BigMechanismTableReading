import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import extract.ColumnData;
import extract.HTMLTable;
import extract.HTMLTableExtractor;

public class ColumnTypeCluster {
	public static void main(String[] args){
		File directory = new File("files");
		HashMap<String, ArrayList<Double>> vectors = new HashMap<String, ArrayList<Double>>();
		int num_tables = 0;
		for (File f: directory.listFiles()){
			if(f.getName().endsWith(".html")){
				try {
					Document table = Jsoup.parse(f, null);
					ArrayList<String> tableEntries = getEntries(table);
					if(tableEntries != null && tableEntries.size() > 0){
						for (String entry: tableEntries){
							if(entry.length() != 0 && entry.length() < 50){
								vectors.put(entry, generateVec(entry, f));
							}
						}
					}
					
				} catch (IOException e) {
					e.printStackTrace();
				}
				num_tables++;
				if(num_tables % 500 == 0){
					System.out.println(num_tables + " tables processed...");
				}
			}
		}
		ArrayList<ArrayList<Double>> means = cluster(vectors, 100);
		ArrayList<ArrayList<String>> clusters = buildClosest(vectors, means);
		for(int i = 0; i < means.size(); i ++){
			String[] words = getClosest(vectors ,means.get(i));
			System.out.print("Type " + i + " (" + clusters.get(i).size() + "): ");
			for(int j = 0; j < 10 && j < clusters.get(i).size(); j ++){
				System.out.print(words[j] + "| ");
			}
			System.out.println();
		}
		
		
		System.out.println("Starting to cluster headers....");
		HashMap<String, ArrayList<Double>> headerVectors = new HashMap<String, ArrayList<Double>>();
		HashMap<String, Double> headerHits = new HashMap<String, Double>();
		num_tables = 0;
		for (File f: directory.listFiles()){
			if(f.getName().endsWith(".html")){
				HTMLTableExtractor hte = new HTMLTableExtractor();
				Collection<HTMLTable> list = hte.parseHTML("files/" + f.getName());
				if(list.size() != 0){
					HTMLTable table = list.iterator().next();
					ColumnData[] cols = table.getColumnData();
					for(ColumnData col: cols){
						if(col.getHeader() != null){
							ArrayList<Double> vec = new ArrayList<Double>();
							for(int i = 0; i < clusters.size(); i++){
								vec.add(0.0);
							}
							double totalEntries = 0.0;
							for(String s: col.getData()){
								if(s != null){
									int clusterNum = findCluster(s, clusters);
									if (findCluster(s, clusters) != -1){
										totalEntries++;
										vec.set(clusterNum, vec.get(clusterNum) + 1.0);
									}
								}
							}
							if(totalEntries > 0){
								for (int i = 0; i < clusters.size(); i++){
									vec.set(i, vec.get(i)/totalEntries);
								}
								if(headerHits.containsKey(col.getHeader())){
									double hits = headerHits.get(col.getHeader());
									ArrayList<Double> prevVec = headerVectors.get(col.getHeader());
									for(int i = 0; i < prevVec.size(); i++){
										prevVec.set(i, (prevVec.get(i)*hits + vec.get(i))/(hits + 1));
									}
									headerHits.put(col.getHeader(),hits + 1);
								} else {
									headerHits.put(col.getHeader(), 1.0);
									headerVectors.put(col.getHeader(), vec);
								}
							}
						}
					}
				}
				num_tables++;
				if(num_tables % 500 == 0){
					System.out.println(num_tables + " tables processed...");
				}
			}
		}
		ArrayList<ArrayList<Double>> headerMeans = cluster(headerVectors, 100);
		ArrayList<ArrayList<String>> headerClusters = buildClosest(headerVectors, headerMeans);
		for(int i = 0; i < headerMeans.size(); i ++){
			String[] words = getClosest(headerVectors ,headerMeans.get(i));
			System.out.print("Type " + i + " (" + headerClusters.get(i).size() + "): ");
			for(int j = 0; j < 10 && j < headerClusters.get(i).size(); j ++){
				System.out.print(words[j] + "| ");
			}
			System.out.println();
		}
		
		File output = new File("columnVectors");
		try {
			FileOutputStream fos = new FileOutputStream(output);
			PrintWriter pw = new PrintWriter(fos);
			int counter = 0;
			pw.write("Data Types\n");
			for (ArrayList<Double> arr: means){
				pw.write(counter + ": ");
				for (Double d: arr){
					pw.write(d + " ");
				}
				counter++;
				pw.write("\n");
			}
			counter = 0;
			pw.write("Column Types\n");
			for (ArrayList<Double> arr: headerMeans){
				pw.write(counter + ": ");
				for (Double d: arr){
					pw.write(d + " ");
				}
				counter++;
				pw.write("\n");
			}
			pw.close();
			fos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static int findCluster(String query, ArrayList<ArrayList<String>> clusters){
		for (int i = 0; i < clusters.size() ;i++){
			for(String s: clusters.get(i)){
				if(query.equals(s)){
					return i;
				}
			}
		}
		return -1;
	}
	
	//Entry Vector: {length, zeroes, digits, uppercase, lowercase, spaces, 
	//               commas, periods, percentage, special, longest word, number of unique chars} 
	private static ArrayList<Double> generateVec (String entry, File f){
		ArrayList<Double> vec = new ArrayList<Double>();
		double longestWord = 0;
		vec.add(Math.log(entry.length()) + 0.0);
		for (int i = 0; i < 9; i++){
			vec.add(0.0);
		}
		double wordLength = 0;
		HashSet<Character> chars = new HashSet<Character>();
		for (int i = 0; i < entry.length(); i++){
			chars.add(entry.charAt(i));
			wordLength++;
			if(longestWord < wordLength){
				longestWord = wordLength;
			}
			if(Character.isDigit(entry.charAt(i))){
				if(entry.charAt(i) == '0'){
					vec.set(1, vec.get(1)+ 1);
				} else {
					vec.set(2, vec.get(2)+ 1);
				}
			} else if (Character.isUpperCase(entry.charAt(i))){
				vec.set(3, vec.get(3)+ 1);
			} else if (Character.isLowerCase(entry.charAt(i))){
				vec.set(4, vec.get(4)+ 1);
			} else if (Character.isWhitespace(entry.charAt(i))){
				wordLength = 0;
				vec.set(5, vec.get(5)+ 1);
			} else if (entry.charAt(i) == ','){
				vec.set(6, vec.get(6)+ 1);
			} else if (entry.charAt(i) == '.'){
				vec.set(7, vec.get(7)+ 1);
			} else if (entry.charAt(i) == '%'){
				vec.set(8, vec.get(8)+ 1);
			} else {
				vec.set(9, vec.get(9)+ 1);
			}
		}
		vec.add(longestWord);
		vec.add(chars.size() + 0.0);
		return vec;
	}
	
	private static ArrayList<String> getEntries(Document doc){
		ArrayList<String> documentData = null;
		Elements tables = doc.getElementsByTag("table");
		if(tables.size() > 0){
			Elements header = tables.get(0).getElementsByTag("tbody");
			if(header.size() > 0){
				Elements rows = header.get(0).getElementsByTag("tr");
				for(int i = 1; i < rows.size(); i++){
					Elements entries = rows.get(i).getElementsByTag("td");
					documentData = separateEntries(entries);
				} 
			}
		}
		return documentData;
	}
	
	private static ArrayList<String> separateEntries(Elements entries){
		ArrayList<String> docData = new ArrayList<String>();
		for (Element a: entries){
			docData.add(a.text());
		}
		return docData;
	}
	private static ArrayList<ArrayList<Double>> cluster(HashMap<String,ArrayList<Double>> vectors, int topics){
		
		Random r = new Random();
		ArrayList<ArrayList<Double>> means = new ArrayList<ArrayList<Double>>();
		ArrayList<ArrayList<Double>> vectorSet = new ArrayList<ArrayList<Double>>(vectors.values());
		int size = vectorSet.size();
		//Initialize Centroids
		int randInd = r.nextInt(size);
		ArrayList<Double> initMean = new ArrayList<Double>(vectorSet.get(randInd));
		means.add(initMean);
		//k-means++ implementation
		for(int i = 1; i < topics; i++){
			double totalProbability = 0.0;
			HashMap<String,Double> distances = new HashMap<String,Double>();
			Iterator<String> iter = vectors.keySet().iterator();
			while(iter.hasNext()){
				String word = iter.next();
				double closest = calculateDistance(initMean, vectors.get(word));
				for (int j = 1; j < means.size(); j++){
					double distance = calculateDistance(means.get(j), vectors.get(word));
					if (distance < closest){
						closest = distance;
					}
				}
				distances.put(word, closest*closest);
				totalProbability += closest*closest;
			}
			double nextInd = r.nextDouble()*totalProbability;
			iter = distances.keySet().iterator();
			String nextCentroid = null;
			while(nextInd > 0){
				nextCentroid = iter.next();
				nextInd -= distances.get(nextCentroid);
			}
			ArrayList<Double> addMean = new ArrayList<Double>(vectors.get(nextCentroid));
			means.add(addMean);
		}
		int vectorSize = vectorSet.get(0).size();
		int iterations = 0;
		
		//Cluster
		ArrayList<ArrayList<String>> closest = null;
		while(iterations < 100){
			closest = new ArrayList<ArrayList<String>>();
			for (int i = 0; i < topics; i++){
				closest.add(new ArrayList<String>());
			}
			Iterator<String> iter = vectors.keySet().iterator();
			while(iter.hasNext()){
				String word = iter.next();
				ArrayList<Double> vec = vectors.get(word);
				double distance = calculateDistance(vec,means.get(0));
				int index = 0;
				for (int i = 1; i < means.size(); i++){
					double newDistance = calculateDistance(vec,means.get(i));
					if (distance > newDistance){
						distance = newDistance;
						index = i;
					}
				}
				closest.get(index).add(word);
			}
			for (int i = 0; i < topics; i++){
				ArrayList<Double> mean = new ArrayList<Double>();
				for (int j = 0; j < vectorSize; j++){
					mean.add(0.0);
				}
				for (int j = 0; j < closest.get(i).size(); j++){
					for (int k = 0; k < vectorSize; k++){
						mean.set(k, mean.get(k) + vectors.get(closest.get(i).get(j)).get(k));
					}
				}
				if(closest.get(i).size() != 0){
					for (int j = 0; j < mean.size(); j++){
						mean.set(j, mean.get(j)/closest.get(i).size());
					}
				} else {
					mean = means.get(i);
				}
				means.set(i, mean);
			}
			iterations++;
			if(iterations % 5 == 0){
				System.out.println(iterations + " iterations done");
			}
		}
		return means;
	}
	
	private static double calculateDistance(ArrayList<Double> a, ArrayList<Double> b){
		double dist = 0;
		for(int i = 0; i < a.size(); i++){
			dist += (a.get(i) - b.get(i))*(a.get(i) - b.get(i));
		}
		return dist;
	}

	private static String[] getClosest(HashMap<String,ArrayList<Double>> vectors, ArrayList<Double> vec){
		HashMap<String,Double> distances = new HashMap<String,Double>();
		Iterator<String> iter = vectors.keySet().iterator();
		while(iter.hasNext()){
			String word = iter.next();
			distances.put(word, calculateDistance(vec, vectors.get(word)));
		}
		String[] words = new String[vectors.keySet().size()];
		iter = vectors.keySet().iterator();
		for (int j = 0; j < words.length; j++){
			words[j] = iter.next();
		}
		Arrays.sort(words, new Comparator<String>() {
			public int compare(String a, String b) {
				if (distances.get(a) > distances.get(b)){
					return 1;
				} else if (distances.get(a) < distances.get(b)){
					return -1;
				} else {
					return 0;
				}
			}
		});
		return words;
	}
	private static ArrayList<ArrayList<String>> buildClosest(HashMap<String,ArrayList<Double>> vectors, ArrayList<ArrayList<Double>> means){
		ArrayList<ArrayList<String>> closest = new ArrayList<ArrayList<String>>();
		for (int i = 0; i < means.size(); i++){
			closest.add(new ArrayList<String>());
		}
		Iterator<String> iter = vectors.keySet().iterator();
		while (iter.hasNext()) {
			String word = iter.next();
			double distance = calculateDistance(vectors.get(word), means.get(0));
			int index = 0;
			for (int i = 1; i < means.size(); i++) {
				double newDistance = calculateDistance(vectors.get(word), means.get(i));
				if (distance > newDistance) {
					distance = newDistance;
					index = i;
				}
			}
			closest.get(index).add(word);
		}
		return closest;
	}
}
