package deeplearning;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.canova.api.io.data.DoubleWritable;
import org.canova.api.records.reader.RecordReader;
import org.canova.api.records.reader.impl.CollectionRecordReader;
import org.canova.api.writable.Writable;
import org.deeplearning4j.datasets.canova.RecordReaderDataSetIterator;
import org.deeplearning4j.datasets.iterator.DataSetIterator;
import org.deeplearning4j.eval.Evaluation;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.GradientNormalization;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.Updater;
import org.deeplearning4j.nn.conf.layers.AutoEncoder;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.conf.layers.RBM;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.ui.weights.HistogramIterationListener;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.api.DataSet;
import org.nd4j.linalg.lossfunctions.LossFunctions.LossFunction;

public class TableSentenceClassifier {
	public static void main(String[] args){
		File directory = new File("papers");
		File tableDirectory = new File("files");
		Pattern p = Pattern.compile("table(\\d+)");
		Pattern q = Pattern.compile("(PMC[0-9]+)");
		Pattern r = Pattern.compile("(\\d+)\\.html");
		ArrayList<String> tableSentences = new ArrayList<String>();
		ArrayList<Double> labels = new ArrayList<Double>();
		HashSet<String> paperNumbers = new HashSet<String>();
		HashMap<String, String> sentenceToPaper = new HashMap<String, String>();
		int num_papers = 0;
		for(File f: directory.listFiles()){
			if(f.getName().endsWith(".html") && num_papers < 1000){
				try {
					Document d = Jsoup.parse(f, null);
					String[] sentences = d.text().split("\\. ");
					for(String s: sentences){
						Matcher m = p.matcher(s.replaceAll("\\W","").toLowerCase());
						if(m.find()){
							try {
								int i = Integer.parseInt(m.group(1));
								if (i < 5){
									String formattedSentence = s.toLowerCase().replaceAll("table\\W+\\d+","");
									tableSentences.add(formattedSentence);
									labels.add(0.0 + i);
									
									Matcher n = q.matcher(f.getName());
									if(n.find()){
										String paperNum = n.group(1);
										paperNumbers.add(paperNum);
										sentenceToPaper.put(formattedSentence, paperNum);
									}
								}
							} catch (NumberFormatException e){
								System.out.println(m.group(1) + " " + s);
							}
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
				}	
				num_papers++;
				if(num_papers % 500 == 0){
					System.out.println(num_papers + " papers processed...");
				}
			}
		}
		
		/*File negatives = new File("unhighlightedsentences.txt");
		Scanner scan;
		try {
			scan = new Scanner(negatives);
			int nSentences = 0;
			while(scan.hasNext()){
				String s = scan.nextLine();
				if(nSentences < 1000){
					tableSentences.add(s.toLowerCase());
					labels.add(0.0);
					nSentences++;
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		File positives = new File("highlightedsentences.txt");
		try {
			scan = new Scanner(positives);
			while(scan.hasNext()){
				String s = scan.nextLine();
				tableSentences.add(s.toLowerCase());
				labels.add(1.0);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}*/
		
		HashMap<String, Integer> ngrams = new HashMap<String, Integer>();
		HashMap<String, Integer> sentenceNGrams = new HashMap<String,Integer>();
		for(String s: tableSentences){
			processSentence(s, ngrams, sentenceNGrams);
		}
		
		HashMap<String, String> tableFileSentences = new HashMap<String, String>();
		for(File f: tableDirectory.listFiles()){
			if(!f.getName().contains(".xls")){
				Matcher n = q.matcher(f.getName());
				Matcher l = r.matcher(f.getName());
				if(n.find() && l.find()){
					String paperNum = n.group(1);
					if(paperNumbers.contains(paperNum)){
						try {
							Document d;
							d = Jsoup.parse(f, null);
							String s = d.text().toLowerCase();
							processSentence(s, ngrams, sentenceNGrams);
							tableFileSentences.put(paperNum + "t" + l.group(1), s);
						} catch (IOException e) {
							e.printStackTrace();
						}
						
					}
				}
			}
		}
		
		ArrayList<String> commonNGrams = new ArrayList<String>();
		commonNGrams.addAll(ngrams.keySet());
		Collections.sort(commonNGrams, new Comparator<String>(){
			public int compare(String a, String b) {
				return ngrams.get(b) - ngrams.get(a);
			}
		});
		
		for(int i = 0; i < 100; i++){
			System.out.println(commonNGrams.get(i) + " " + ngrams.get(commonNGrams.get(i)) + 
					" " + sentenceNGrams.get(commonNGrams.get(i)) + 
					" " + sentenceNGrams.get(commonNGrams.get(i))/((double)tableSentences.size()));
		}
		
		System.out.println("Beginning vectors generation... ");
		System.out.println("Vocab size: " + ngrams.size());
		int threshSize = 0;
		
		for(Iterator<String> iter = ngrams.keySet().iterator(); iter.hasNext();) {
			String phrase = iter.next();
			if(sentenceNGrams.get(phrase) < 350 && sentenceNGrams.get(phrase) > 10){
				threshSize++;
			} else {
				iter.remove();
			}
		}
		System.out.println("Cutoff Vocab size: " + threshSize);
		
		ArrayList<ArrayList<Writable>> PositiveTfIdfVectors = new ArrayList<ArrayList<Writable>>();
		ArrayList<ArrayList<Writable>> NegativeTfIdfVectors = new ArrayList<ArrayList<Writable>>();
		HashMap<String, ArrayList<Writable>> cachedTableVecs = new HashMap<String, ArrayList<Writable>>();
		int sentences_processed = 0;
		for(int i = 0; i < tableSentences.size(); i++){
			String s = tableSentences.get(i);
			ArrayList<Writable> vec = generateVec(s, ngrams, sentenceNGrams, tableSentences.size() + tableFileSentences.size(), 1);
			for (int label = 1; label < 5; label ++){
				String table = tableFileSentences.get(sentenceToPaper.get(s) + "t" + labels.get(i).intValue());
				if (table != null){
					ArrayList<Writable> vec2;
					if(cachedTableVecs.containsKey(table)){
						vec2 = cachedTableVecs.get(table);
					} else {
						vec2 = generateVec(table, ngrams, 
								sentenceNGrams, tableSentences.size() + tableFileSentences.size(), label == labels.get(i).intValue() ? 1 : 0);
						cachedTableVecs.put(table,vec2);
					}

					
					ArrayList<Writable> product = hadamardProduct(vec,vec2);
					if (label == labels.get(i).intValue()){
						PositiveTfIdfVectors.add(product);
						PositiveTfIdfVectors.add(product);
					} else {
						NegativeTfIdfVectors.add(product);
					}
					
					sentences_processed++;
					if(sentences_processed % 50 == 0){
						System.out.println(sentences_processed + " sentences processed...");
					}
				}
			}
			if (cachedTableVecs.size() > 30){
				cachedTableVecs = new HashMap<String, ArrayList<Writable>>();
			}
		}
		
		for (Iterator<ArrayList<Writable>> iter = PositiveTfIdfVectors.iterator(); iter.hasNext();){
			ArrayList<Writable> vec = iter.next();
			if(checkZero(vec)){
				iter.remove();
			} 
		}
		for (Iterator<ArrayList<Writable>> iter = NegativeTfIdfVectors.iterator(); iter.hasNext();){
			ArrayList<Writable> vec = iter.next();
			if(checkZero(vec)){
				iter.remove();
			} 
		}
		
		Collections.shuffle(NegativeTfIdfVectors, new Random(100L));
		Collections.shuffle(PositiveTfIdfVectors, new Random(100L));
		ArrayList<ArrayList<Writable>> TfIdfVectors = new ArrayList<ArrayList<Writable>>();
		for(int i = 0; i < NegativeTfIdfVectors.size(); i++){
			TfIdfVectors.add(NegativeTfIdfVectors.get(i));
			if (i % 3 == 0){
				for(int j = 0; j < 3; j++){
					TfIdfVectors.add(PositiveTfIdfVectors.get(i/3));
				}
			}
		}
		
		File trainingFile = new File("trainingVectors");
		File testingFile = new File("testingVectors");
		try {
			PrintWriter pw = new PrintWriter(trainingFile);
			PrintWriter pwTest = new PrintWriter(testingFile);
			
			for(int j = 0; j < TfIdfVectors.size(); j++){
				ArrayList<Writable> vec = TfIdfVectors.get(j);
				if(j < TfIdfVectors.size()*0.8){
					for (int i = 0; i < vec.size() -1; i++){
						pw.print(vec.get(i).toDouble() + ",");
					}
					pw.print((int) (vec.get(vec.size()-1).toDouble()));
					pw.println();;
				} else {
					for (int i = 0; i < vec.size() -1; i++){
						pwTest.print(vec.get(i).toDouble() + ",");
					}
					pwTest.print((int) (vec.get(vec.size()-1).toDouble()));
					pwTest.println();
				}
			}
			pw.close();
			pwTest.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
	}
	
	private static boolean checkZero(ArrayList<Writable> vec) {
		for(int i = 0; i < vec.size() - 1; i++){
			if(vec.get(i).toDouble() > 0){
				return false;
			}
		}
		return true;
	}

	private static ArrayList<Writable> hadamardProduct(ArrayList<Writable> vec, ArrayList<Writable> vec2) {
		ArrayList<Writable> newVec = new ArrayList<Writable>();
		for(int i = 0; i < vec.size(); i++){
			newVec.add(new DoubleWritable(vec.get(i).toDouble() * vec2.get(i).toDouble()));
		}
		return newVec;
	}

	private static ArrayList<Writable> generateVec(String s, HashMap<String, Integer> ngrams, HashMap<String, Integer> sentenceNGrams, int size, double label) {
		ArrayList<Writable> vec = new ArrayList<Writable>();
		for(String phrase: ngrams.keySet()){
			double termFrequency = getTermFrequency(s, phrase);
			double inverseDocFrequency = Math.log((double)size/(double)(sentenceNGrams.get(phrase)));
			vec.add(new DoubleWritable(termFrequency*inverseDocFrequency));
			/*if(termFrequency*inverseDocFrequency > 10){
				vec.add(new DoubleWritable(1.0));
			} else {
				vec.add(new DoubleWritable(0.0));
			}*/
		}
		vec.add(new DoubleWritable(label));
		return vec;
	}

	private static void processSentence(String s, HashMap<String, Integer> ngrams, HashMap<String, Integer> sentenceNGrams) {
		HashSet<String> phrasesFound = new HashSet<String>();
		String[] words = s.replaceAll("\\W+"," ").split("\\W");
		ArrayList<String> corrected = new ArrayList<String>();
		for(String word: words){
			if(word.length() > 0){
				if(checkNumber(word)){
					corrected.add(word);
				} else {
					corrected.add("numval");
				}
			}
		}
		for(int l = 1; l < 3; l++){
			for(int i = 0; i < corrected.size() - l; i++){
				String phrase = getNGram(corrected, l, i);
				if(ngrams.containsKey(phrase)){
					ngrams.put(phrase, ngrams.get(phrase) + 1);
				} else {
					ngrams.put(phrase, 1);
				}
				phrasesFound.add(phrase);
			}
		}
		for(String phrase: phrasesFound){
			if(sentenceNGrams.containsKey(phrase)){
				sentenceNGrams.put(phrase, sentenceNGrams.get(phrase) + 1);
			} else {
				sentenceNGrams.put(phrase, 1);
			}
		}
	}

	private static boolean checkNumber(String word) {
		for(int i = 0; i < word.length(); i++){
			if(!Character.isDigit(word.charAt(i))){
				return true;
			}
		}
		return false;
	}

	private static double getTermFrequency(String s, String phrase) {
		int occurances = 0;
		int index = 0;
		while(s.indexOf(phrase, index) != -1){
			index += phrase.split("\\W").length;
			occurances++;
		}
		return occurances;
	}

	private static String getNGram(ArrayList<String> s, int length, int index){
		String phrase = "";
		while(length > 1){
			phrase += s.get(index) + " ";
			length--;
			index++;
		}
		phrase += s.get(index);
		return phrase;
	}
}
