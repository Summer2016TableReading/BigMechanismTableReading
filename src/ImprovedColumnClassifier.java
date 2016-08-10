import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

import java.util.Scanner;

import extract.ColumnData;
import extract.HTMLTable;
import extract.HTMLTableExtractor;
import lookup.TabLookup;

public class ImprovedColumnClassifier {
	public static void main(String[] args){
		OntobeeQuery.parseOntologies();
		ArrayList<ArrayList<Double>> means = new ArrayList<ArrayList<Double>>();
		ArrayList<ArrayList<Double>> headerMeans = new ArrayList<ArrayList<Double>>();
		HashMap<Integer, String> classes = new HashMap<Integer, String>();
		HashMap<String, String> classesSingle = new HashMap<String, String>();
		HashMap<String, String> topics = new HashMap<String,String>();
		
		try {
			File vectorFile = new File("columnVectors");
			FileInputStream fis = new FileInputStream(vectorFile);
			Scanner s = new Scanner(fis);
			s.nextLine(); //Data Types
			for(int j = 0; j < 100; j++){
				String[] line = s.nextLine().split(" ");
				ArrayList<Double> vec = new ArrayList<Double>();
				for(int i = 1; i < line.length; i++){
					vec.add(Double.parseDouble(line[i]));
				}
				means.add(vec);
			}
			s.nextLine(); //Column Types
			while(s.hasNext()){
				String[] line = s.nextLine().split(" ");
				ArrayList<Double> vec = new ArrayList<Double>();
				for(int i = 1; i < line.length; i++){
					vec.add(Double.parseDouble(line[i]));
				}
				headerMeans.add(vec);
			}
			File classFile = new File("columnTypes");
			FileInputStream fisClasses = new FileInputStream(classFile);
			Scanner classScanner = new Scanner(fisClasses);
			while(classScanner.hasNext()){
				String[] line = classScanner.nextLine().split(":");
				classes.put(Integer.parseInt(line[0]), line[1]);
			}
			
			File classFileSingle = new File("columnTypesSingle");
			FileInputStream fisClassesSingle = new FileInputStream(classFileSingle);
			Scanner classScannerSingle = new Scanner(fisClassesSingle);
			while(classScannerSingle.hasNext()){
				String[] line = classScannerSingle.nextLine().split(":");
				classesSingle.put(line[0], line[1]);
			}
			
			s.close();
			classScanner.close();
			classScannerSingle.close();
			
			//Reading Json
			fis = new FileInputStream(new File("topic_model300k.json"));
			JsonReader jr = new JsonReader(new InputStreamReader(fis));
			JsonObject jsonObject = new JsonParser().parse(jr).getAsJsonObject();
			Iterator<Entry<String, JsonElement>> iter = jsonObject.entrySet().iterator();
			while(iter.hasNext()){
				Entry<String, JsonElement> entry = iter.next();
				JsonArray je = entry.getValue().getAsJsonArray();
				for(JsonElement jeElement: je){
					topics.put(jeElement.getAsString(), entry.getKey());
				}
			}
			jr.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		Scanner queryScanner = new Scanner(System.in);
		System.out.println("Please input a query: ");
		String query = queryScanner.nextLine();
		while(!query.equals("exit")){
			File f = new File(query);
			
			HTMLTableExtractor hte = new HTMLTableExtractor();
			Collection<HTMLTable> list = hte.parseHTML("files/" + f.getName());
			if(list.size() != 0){
				
				HTMLTable table = list.iterator().next();
				ColumnData[] cols = table.getColumnData();
				for(ColumnData col: cols){
					if(col.getHeader() != null && checkNotNull(col.getData())){
						HashMap<String, Integer> topicCounts = new HashMap<String, Integer>();
						
						ArrayList<Double> vec = new ArrayList<Double>();
						for(int i = 0; i < means.size(); i++){
							vec.add(0.0);
						}
						double totalEntries = 0.0;
						
						double longEntries = 0.0;
						for(String s: col.getData()){
							if(s != null && s.length() > 0){
								if(s.length() < 50){
									int clusterNum = findCluster(generateVec(s), means);
									totalEntries++;
									vec.set(clusterNum, vec.get(clusterNum) + 1.0);
								} else {
									longEntries++;
								}
							}
						}
						if(totalEntries > 0){
							for (int i = 0; i < means.size(); i++){
								vec.set(i, vec.get(i)/totalEntries);
							}
						}
						int type = findCluster(vec, headerMeans);
						String category = classes.get(type);
						String next = "";
						if(type == 0){
							for(String checkWord: classesSingle.keySet()){
								if(checkColumn(checkWord, col.getData())){
									category = classesSingle.get(checkWord);
								}
							}
						} else if (category.contains("Inconclusive")){
							next = ", Best guess: ";
							if(longEntries < totalEntries){
								HashSet<Integer> exclude = new HashSet<Integer>();
								for(Entry<Integer, String> entry: classes.entrySet()){
									if(entry.getValue().contains("Inconclusive")){
										exclude.add(entry.getKey());
									}
								}
								int tempType = findCluster(vec, headerMeans, exclude);
								next += classes.get(tempType) + " (Type " + tempType + ")";
							} else {
								next += "Long Descriptions";
							}
						}
						String ontologyProperty = "";
						if (category.toLowerCase().contains("number") || category.toLowerCase().contains("decimal") || category.toLowerCase().contains("percentage")
								|| next.toLowerCase().contains("number") || next.toLowerCase().contains("decimal") || next.toLowerCase().contains("percentage")){
							HashSet<String> types = new HashSet<String>();
							String[] words = col.getHeader().split(" ");
							for(String s : words){
								if(s.length() > 2){
									types.addAll(OntobeeQuery.queryAllOntologies(s));
								}
							}
							if(types.contains("concentration endpoint")){
								ontologyProperty = " Process: concentration test";
							}
						}
						String proteinConfirmed = "";
						if (category.toLowerCase().contains("english") || category.toLowerCase().contains("inconclusive")){
							if(lookupColumn(TabLookup.getInstance().english, col.getData())){
								proteinConfirmed = "(english protein names found)";
							}
						}
						if (category.toLowerCase().contains("protein")){
							if(lookupColumn(TabLookup.getInstance().genename, col.getData())){
								proteinConfirmed = "(protein/gene found)";
							} 
						}
						
						System.out.print(col.getHeader() + ": " +  category + " (Type " + type + ")" + next + ontologyProperty + " " + proteinConfirmed);
						
						String[] headerWords = col.getHeader().split("\\W");
						for(String s : headerWords){
							if(s != null && topics.containsKey(s.toLowerCase().replace(" ","_"))){
								String topic = topics.get(s.toLowerCase().replace(" ","_"));
								if(topicCounts.containsKey(topic)){
									topicCounts.put(topic, topicCounts.get(topic) + 1);
								} else {
									topicCounts.put(topic, 1);
								}
							}
						}
						System.out.print("Header type: ");
						if(topics.containsKey(col.getHeader().toLowerCase().replace(" ","_"))){
							System.out.print(topics.get(col.getHeader().toLowerCase().replace(" ","_")) + " ");
						}
						System.out.println(topicCounts);
					}
				}
			}
			System.out.println("Please input a query: ");
			query = queryScanner.nextLine();
		}
		queryScanner.close();
	}
	
	private static boolean checkNotNull(String[] data) {
		for(int i = 0; i < data.length; i++){
			if(data[i] != null){
				return true;
			}
		}
		return false;
	}

	private static boolean lookupColumn(HashMap<String,?> database, String[] data) {
		double match = 0.0;
		for (int i = 0; i < data.length; i++){
			if(data[i] != null){
				if(database.containsKey(data[i].toUpperCase().replaceAll("\\)", "").replaceAll("\\W+", " "))){
					match++;
				}
			}
		}
		return match >= 0.5 * data.length || match > 5;
	}

	private static boolean checkColumn(String checkWord, String[] data) {
		if(data != null){
			for(String s: data){
				if(s != null && checkWord.toLowerCase().equals(s.toLowerCase())){
					return true;
				}
			}
		}
		return false;
	}



	private static int findCluster(ArrayList<Double> vec, ArrayList<ArrayList<Double>> means) {
		double distance = calculateDistance(vec, means.get(0));
		int index = 0;
		for(int i = 1; i < means.size(); i++){
			double newDist = calculateDistance(vec, means.get(i));
			if (newDist < distance){
				distance = newDist;
				index = i;
			}
		}
		return index;
	}
	
	private static int findCluster(ArrayList<Double> vec, ArrayList<ArrayList<Double>> means, HashSet<Integer> exclude) {
		double distance = calculateDistance(vec, means.get(0));
		int index = 0;
		for(int i = 1; i < means.size(); i++){
			if(!exclude.contains(i)){
				double newDist = calculateDistance(vec, means.get(i));
				if (newDist < distance){
					distance = newDist;
					index = i;
				}
			}
			
		}
		return index;
	}
	
	private static ArrayList<Double> generateVec (String entry){
		ArrayList<Double> vec = new ArrayList<Double>();
		double longestWord = 0;
		vec.add(Math.log(entry.length()) + 0.0);
		for (int i = 0; i < 9; i++){
			vec.add(0.0);
		}
		HashSet<Character> chars = new HashSet<Character>();
		for (int i = 0; i < entry.length(); i++){
			chars.add(entry.charAt(i));
			longestWord++;
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
				longestWord = 0;
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
	
	private static double calculateDistance(ArrayList<Double> a, ArrayList<Double> b){
		double dist = 0;
		for(int i = 0; i < a.size(); i++){
			dist += (a.get(i) - b.get(i))*(a.get(i) - b.get(i));
		}
		return dist;
	}
}
