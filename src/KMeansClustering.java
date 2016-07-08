import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import java.util.Scanner;

public class KMeansClustering {

	public static void main(String[] args) {
		int topics = 100;
		File wordVectors = new File("words.txt");
		HashMap<String,ArrayList<Double>> vectors = new HashMap<String,ArrayList<Double>>();
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(wordVectors);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Scanner s = new Scanner(fis);
		while(s.hasNext()){
			String[] line = s.nextLine().split(" ");
			ArrayList<Double> vec = new ArrayList<Double>();
			for (int i = 1; i < line.length; i++){
				vec.add(Double.parseDouble(line[i]));
			}
			vectors.put(line[0], vec);
		}
		s.close();
		Random r = new Random();
		ArrayList<ArrayList<Double>> means = new ArrayList<ArrayList<Double>>();
		ArrayList<ArrayList<Double>> vectorSet = new ArrayList<ArrayList<Double>>(vectors.values());
		int size = vectorSet.size();
		for(int i = 0; i < topics; i++){
			int index = r.nextInt(size);
			ArrayList<Double> mean = new ArrayList<Double>(vectorSet.get(index));
			means.add(mean);
		}
		int vectorSize = vectorSet.get(0).size();
		int iterations = 0;
		while(iterations < 100){
			ArrayList<ArrayList<String>> closest = new ArrayList<ArrayList<String>>();
			for (int i = 0; i < topics; i++){
				closest.add(new ArrayList<String>());
			}
			Iterator<String> iter = vectors.keySet().iterator();
			while(iter.hasNext()){
				String word = iter.next();
				ArrayList<Double> vec = vectors.get(word);
				double distance = calculateDistance(vec,means.get(0));
				int index = 0;
				for (int i = 0; i < means.size(); i++){
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
		for (int i = 0; i < means.size(); i++){
			HashMap<String,Double> distances = new HashMap<String,Double>();
			Iterator<String> iter = vectors.keySet().iterator();
			while(iter.hasNext()){
				String word = iter.next();
				distances.put(word, calculateDistance(means.get(i), vectors.get(word)));
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
			System.out.println("Topic " + i);
			for (int j = 0; j < 9; j++){
				System.out.print(words[j] + ", ");
			}
			System.out.println(words[10] + "");
		}
	}
	private static double calculateDistance(ArrayList<Double> a, ArrayList<Double> b){
		double dist = 0;
		for(int i = 0; i < a.size(); i++){
			dist += (a.get(i) - b.get(i))*(a.get(i) - b.get(i));
		}
		return dist;
	}
}
