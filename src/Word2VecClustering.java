import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import java.util.Map.Entry;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

public class Word2VecClustering {
	public static void main(String[] args){
		HashMap<String,ArrayList<Double>> vectors = new HashMap<String,ArrayList<Double>>();
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(new File("w2v_model_data.json"));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		JsonReader jr = new JsonReader(new InputStreamReader(fis));
		JsonObject jsonObject = new JsonParser().parse(jr).getAsJsonObject();
		Iterator<Entry<String, JsonElement>> iter = jsonObject.entrySet().iterator();
		System.out.println("Reading Input...");
		while(iter.hasNext()){
			Entry<String, JsonElement> entry = iter.next();
			JsonArray je = entry.getValue().getAsJsonArray();
			ArrayList<Double> vec = new ArrayList<Double>();
			for(JsonElement jeElement: je){
				vec.add(jeElement.getAsDouble());
			}
			vectors.put(entry.getKey(), vec);
		}
		try {
			jr.close();
			fis.close();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		System.out.println("Clustering Started...");
		ArrayList<ArrayList<Double>> means = cluster(vectors, Integer.parseInt(args[0]));
		System.out.println("Clustering Finished...");
		
		ArrayList<ArrayList<String>> closest = buildClosest(vectors, means);
		
		System.out.println("Writing Output...");
		
		File output = new File("word2vec_clustered.json");
		File topic_model = new File("topic_model.json");
		JsonObject jsonOut = new JsonObject();
		JsonObject jsonTopic = new JsonObject();
		for (int i = 0; i < means.size(); i++){
			//cluster file
			JsonObject jo = new JsonObject();
			JsonArray je = new JsonArray();
			for (Double d: means.get(i)){
				je.add(d);
			}
			JsonArray jw = new JsonArray();
			String[] words = getClosest(vectors,means.get(i));
			for(int j = 0; j < words.length && j < 10; j++){
				jw.add(words[j]);
			}
			jo.add("value", je);
			jo.add("words", jw);
			jsonOut.add("Topic " + i, jo);
			//words file
			JsonArray jaTopic = new JsonArray();
			for (int j =0; j < closest.get(i).size(); j++){
				jaTopic.add(closest.get(i).get(j));
			}
			jsonTopic.add("Topic " + i, jaTopic);
		}
		try {
			//cluster file
			FileOutputStream fos = new FileOutputStream(output);
			PrintWriter pw = new PrintWriter(fos);
			Gson gson = new GsonBuilder().create();
			pw.write(gson.toJson(jsonOut));
			pw.close();
			//words file
			FileOutputStream fosWords = new FileOutputStream(topic_model);
			PrintWriter pwWords = new PrintWriter(fosWords);
			pwWords.write(gson.toJson(jsonTopic));
			pwWords.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	private static ArrayList<ArrayList<Double>> cluster(HashMap<String, ArrayList<Double>> vectors, int topics) {
		Random r = new Random();
		ArrayList<ArrayList<Double>> means = new ArrayList<ArrayList<Double>>();
		ArrayList<ArrayList<Double>> vectorSet = new ArrayList<ArrayList<Double>>(vectors.values());
		int size = vectorSet.size();
		//Initialize Centroids
		int randInd = r.nextInt(size);
		ArrayList<Double> initMean = new ArrayList<Double>(vectorSet.get(randInd));
		means.add(initMean);
		//k-means++ implementation
		System.out.println("Generating Initial Centroids...");
		HashMap<String,Double> distances = new HashMap<String,Double>();
		for(int i = 1; i < topics; i++){
			double totalProbability = 0.0;
			distances.clear();
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
			means.add(vectors.get(nextCentroid));
			if(i % 10 == 0){
				System.out.println("Generated " + i + " Centroids");
			}
		}
		int vectorSize = vectorSet.get(0).size();
		int iterations = 0;
		System.out.println("Starting Iterations..");
		//Cluster
		ArrayList<ArrayList<String>> closest = new ArrayList<ArrayList<String>>();
		for (int i = 0; i < topics; i++){
			closest.add(new ArrayList<String>());
		}
		while(iterations < 100){
			for (int i = 0; i < topics; i++){
				closest.get(i).clear();
			}
			System.gc();
			Iterator<String> iter = vectors.keySet().iterator();
			while(iter.hasNext()){
				String word = iter.next();
				double distance = calculateDistance(vectors.get(word),means.get(0));
				int index = 0;
				for (int i = 1; i < means.size(); i++){
					double newDistance = calculateDistance(vectors.get(word),means.get(i));
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
