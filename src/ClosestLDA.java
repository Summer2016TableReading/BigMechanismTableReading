import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Scanner;

public class ClosestLDA {
	public static void main(String[] args){
		File model = new File("model-paper.theta");
		File sFile = new File("OutputPMC1557711.txt");
		FileInputStream fis;
		FileInputStream fisSentences;
		Scanner sentenceScanner = null;
		Scanner s = null;
		try {
			fis = new FileInputStream(model);
			s = new Scanner(fis);
			fisSentences = new FileInputStream(sFile);
			sentenceScanner = new Scanner(fisSentences);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		ArrayList<ArrayList<Double>> vectors = new ArrayList<ArrayList<Double>>();
		while(s.hasNext()){
			String line = s.nextLine();
			String[] values = line.split(" ");
			ArrayList<Double> vec = new ArrayList<Double>();
			for (int i = 0; i< values.length; i++){
				vec.add(Double.parseDouble(values[i]));
			}
			vectors.add(vec);
		}
		ArrayList<String> sentences = new ArrayList<String>();
		while(sentenceScanner.hasNext()){
			sentences.add(sentenceScanner.nextLine());
		}
		ArrayList<ArrayList<Double>> sortedVectors = new ArrayList<ArrayList<Double>>();
		for (int i = 1; i < vectors.size(); i++){
			sortedVectors.add(vectors.get(i));
		}
		Collections.sort(sortedVectors, new Comparator<ArrayList<Double>>() {
			public int compare(ArrayList<Double> a, ArrayList<Double> b) {
				if (calculateDistance(vectors.get(0),a) > calculateDistance(vectors.get(0),b)){
					return 1;
				} else if (calculateDistance(vectors.get(0),a) < calculateDistance(vectors.get(0),b)){
					return -1;
				} else {
					return 0;
				}
			}
		});
		for (int i = 0; i < 20; i++){
			System.out.println("Distance: " + calculateDistance(sortedVectors.get(i),vectors.get(0)) + " " + sentences.get(vectors.indexOf(sortedVectors.get(i + 1))+1));
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
