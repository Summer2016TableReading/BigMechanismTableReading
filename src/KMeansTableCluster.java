import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import java.util.Scanner;

import org.apache.commons.io.FileUtils;

public class KMeansTableCluster {
	public static void main(String[] args){
		cluster("model-final.theta", "fileNames.txt", 15);
		/*int clusters = cluster();
		while (clusters < 25){
			clusters = cluster();
		}*/
	}
	public static ArrayList<ArrayList<String>> cluster(String model, String tableNames, int topics) {
		File wordVectors = new File(model);
		File names = new File(tableNames);
		HashMap<String,ArrayList<Double>> vectors = new HashMap<String,ArrayList<Double>>();
		FileInputStream fis = null;
		FileInputStream fisNames = null;
		try {
			fis = new FileInputStream(wordVectors);
			fisNames = new FileInputStream(names);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Scanner s = new Scanner(fis);
		Scanner t = new Scanner(fisNames);
		while(s.hasNext()){
			String[] line = s.nextLine().split(" ");
			ArrayList<Double> vec = new ArrayList<Double>();
			for (int i = 0; i < line.length; i++){
				vec.add(Double.parseDouble(line[i]));
			}
			vectors.put(t.nextLine(), vec);
		}
		s.close();
		t.close();
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
		File outputDir = new File("sortedPapers");
		try {
			FileUtils.cleanDirectory(outputDir);
		} catch (IOException e) {
			e.printStackTrace();
		}
		for (int i = 0; i < means.size(); i++){
			if(closest.get(i).size() != 0){
				String[] words = getClosest(vectors, means.get(i));
				File topic = new File("sortedPapers/topic" + i);
				topic.mkdir();
				System.out.println("Topic " + i);
				System.out.println(means.get(i));
				for (int j = 0; j < 9; j++){
					System.out.print(words[j] + ", ");
					File source = new File("files/" + words[j]);
					File target = new File("sortedPapers/topic" + i + "/"+ words[j]);
					try {
						Files.copy(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				System.out.println(words[10] + "");
			}
		}
		/*Scanner userQuery = new Scanner(System.in);
		int a = userQuery.nextInt();
		while(a != -1){
			
			double max = means.get(0).get(a);
			int index = 0;
			for(int i = 1; i < means.size(); i++){
				if (means.get(i).get(a) > max){
					max = means.get(i).get(a);
					index = i;
				}
			}
			System.out.println("Topic " + index + " " + max + " " + closest.get(index).size());
			a = userQuery.nextInt();
		}
		String a = userQuery.nextLine();
		while(!a.equals("exit")){
			
			int index = -1;
			for(int i = 0; i < means.size(); i++){
				if(closest.get(i).contains(a)){
					index = i;
				}
			}
			System.out.println("Location: Cluster " + index);
			System.out.println("Table Properties: " + vectors.get(a));
			System.out.println("Cluster Properties: " + means.get(index));
			a = userQuery.nextLine();
		}
		try {
			Scanner tables = new Scanner(new File("RasTables.txt"));
			while(tables.hasNext()){
				String a = tables.next();
				int index = -1;
				for(int i = 0; i < means.size(); i++){
					if(closest.get(i).contains(a)){
						index = i;
					}
				}
				double max = 0;
				int ind = 0;
				for (int i = 0; i < means.get(index).size(); i++){
					if (means.get(index).get(i) > max){
						max = means.get(index).get(i);
						ind = i;
					}
				}
				System.out.println("Location: Cluster " + index + " Closest Topic: " + ind + " " + a );
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Scanner userQuery = new Scanner(System.in);
		String a = userQuery.nextLine();
		while(!a.equals("exit")){
			
			String[] list = getClosest(vectors, vectors.get(a));
			for(int i = 0; i < 10; i++){
				System.out.println(list[i]);
			}
			a = userQuery.nextLine();
		}
		userQuery.close();*/
		return closest;
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
}
