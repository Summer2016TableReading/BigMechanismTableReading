package deeplearning;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
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
import org.deeplearning4j.nn.conf.layers.AutoEncoder;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.api.DataSet;
import org.nd4j.linalg.lossfunctions.LossFunctions.LossFunction;

public class TableSentenceAnalyzer {
	public static void main(String[] args){
		File directory = new File("papers");
		File[] papers = directory.listFiles();
		Pattern p = Pattern.compile("table\\d+");
		ArrayList<String> tableSentences = new ArrayList<String>();
		ArrayList<Double> labels = new ArrayList<Double>();
		int num_papers = 0;
		for(File f: papers){
			if(f.getName().endsWith(".html") && num_papers < 1000){
				try {
					Document d = Jsoup.parse(f, null);
					String[] sentences = d.text().split("\\. ");
					for(String s: sentences){
						Matcher m = p.matcher(s.replaceAll("\\W","").toLowerCase());
						if(m.find()){
							tableSentences.add(s.toLowerCase());
							labels.add(1.0);
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
		
		File negatives = new File("unhighlightedsentences.txt");
		Scanner scan;
		try {
			scan = new Scanner(negatives);
			int nSentences = 0;
			while(scan.hasNext()){
				if(nSentences < 1000){
					tableSentences.add(scan.nextLine().toLowerCase());
					labels.add(-1.0);
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		HashMap<String, Integer> ngrams = new HashMap<String, Integer>();
		HashMap<String, Integer> sentenceNGrams = new HashMap<String,Integer>();
		for(String s: tableSentences){
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
		
		ArrayList<ArrayList<Writable>> TfIdfVectors = new ArrayList<ArrayList<Writable>>();
		for(int i = 0; i < tableSentences.size(); i++){
			String s = tableSentences.get(i);
			ArrayList<Writable> vec = new ArrayList<Writable>();
			for(String phrase: ngrams.keySet()){
				if(sentenceNGrams.get(phrase) < 500 && sentenceNGrams.get(phrase) > 10){
					double termFrequency = getTermFrequency(s, phrase);
					double inverseDocFrequency = Math.log((double)tableSentences.size()/(double)(sentenceNGrams.get(phrase)));
					vec.add(new DoubleWritable(termFrequency*inverseDocFrequency));
				}
			}
			vec.add(new DoubleWritable(labels.get(i)));
			TfIdfVectors.add(vec);
		}
		
		buildDeepLearning(TfIdfVectors);
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
	
	private static void buildDeepLearning(ArrayList<ArrayList<Writable>> vectors) {
		RecordReader recordReader = new CollectionRecordReader(vectors);
		DataSetIterator iter = new RecordReaderDataSetIterator(recordReader, vectors.get(0).size()-1, vectors.size());
		System.out.println("buildilng nn");
		int seed = 100;
		int iterations = 10;
		 MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
			       .seed(seed)
			       .gradientNormalization(GradientNormalization.ClipElementWiseAbsoluteValue)
			       .gradientNormalizationThreshold(1.0)
			       .iterations(iterations)
			       .momentum(0.5)
			       .momentumAfter(Collections.singletonMap(3, 0.9))
			       .optimizationAlgo(OptimizationAlgorithm.CONJUGATE_GRADIENT)
			       .list(5)
			       .layer(0, new AutoEncoder.Builder().nIn(vectors.get(0).size()-1).nOut(1000)
				           .weightInit(WeightInit.XAVIER).lossFunction(LossFunction.RMSE_XENT)
				           .corruptionLevel(0.3)
				           .build())
			       .layer(1, new AutoEncoder.Builder().nIn(1000).nOut(500)
		                   .weightInit(WeightInit.XAVIER).lossFunction(LossFunction.RMSE_XENT)
		                   .corruptionLevel(0.3)
		                   .build())
		            .layer(2, new AutoEncoder.Builder().nIn(500).nOut(250)
			               .weightInit(WeightInit.XAVIER).lossFunction(LossFunction.RMSE_XENT)
			               .corruptionLevel(0.3)
			               .build())
			            .layer(3, new AutoEncoder.Builder().nIn(250).nOut(100)
			                    .weightInit(WeightInit.XAVIER).lossFunction(LossFunction.RMSE_XENT)
			                    .corruptionLevel(0.3)
			                    .build())
			            .layer(4, new OutputLayer.Builder(LossFunction.NEGATIVELOGLIKELIHOOD).activation("softmax")
			                    .nIn(100).nOut(2).build())
			       .pretrain(true).backprop(false)
			            .build();
		 MultiLayerNetwork network = new MultiLayerNetwork(conf);
		 network.fit(iter);
		 
	     Evaluation eval = new Evaluation(2);
	        
		 recordReader = new CollectionRecordReader(vectors);
		iter = new RecordReaderDataSetIterator(recordReader, vectors.get(0).size()-1, vectors.size());

		 while(iter.hasNext()){
		      DataSet next = iter.next();
		      INDArray predict2 = network.output(next.getFeatureMatrix());
	          eval.eval(next.getLabels(), predict2);
		}
		 System.out.println("hi");
	}

}
