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

public class TableSentenceAnalyzer {
	public static void main(String[] args){
		//Parsing for table and unhighlighted sentences
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
							tableSentences.add(s.toLowerCase().replaceAll("table\\W+\\d+",""));
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
				String s = scan.nextLine();
				if(nSentences < 2000){
					tableSentences.add(s.toLowerCase());
					labels.add(0.0);
					nSentences++;
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		//initializing ngrams for tfidf calculations
		HashMap<String, Integer> ngrams = new HashMap<String, Integer>();
		HashMap<String, Integer> sentenceNGrams = new HashMap<String,Integer>();
		//formatting sentences for tfidf(word splitting, phrase creation and number removal)
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
		//creation of tfidf vectors
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
				if(sentenceNGrams.get(phrase) < 250 && sentenceNGrams.get(phrase) > 10){
					double termFrequency = getTermFrequency(s, phrase);
					double inverseDocFrequency = Math.log((double)tableSentences.size()/(double)(sentenceNGrams.get(phrase)));
					//vec.add(new DoubleWritable(termFrequency*inverseDocFrequency));
					if(termFrequency*inverseDocFrequency > 10){
						vec.add(new DoubleWritable(1.0));
					} else {
						vec.add(new DoubleWritable(0.0));
					}
				}
			}
			vec.add(new DoubleWritable(labels.get(i)));
			TfIdfVectors.add(vec);
		}
		System.out.println(TfIdfVectors.get(0).size());
		//splitting tfidf vectors into training and testing sets, then building the neural network
		Collections.shuffle(TfIdfVectors);
		ArrayList<ArrayList<Writable>> trainingSet = new ArrayList<ArrayList<Writable>>();
		ArrayList<ArrayList<Writable>> testingSet = new ArrayList<ArrayList<Writable>>();
		
		for(int i = 0; i < TfIdfVectors.size()-1; i+=2){
			trainingSet.add(TfIdfVectors.get(i));
			testingSet.add(TfIdfVectors.get(i+1));
		}
		
		buildDeepLearning(trainingSet, testingSet);
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
	
	private static void buildDeepLearning(ArrayList<ArrayList<Writable>> training, ArrayList<ArrayList<Writable>> testing) {
		RecordReader recordReader = new CollectionRecordReader(training);
		DataSetIterator iter = new RecordReaderDataSetIterator(recordReader, 32, training.get(0).size()-1, 2);
		System.out.println("building nn");
		//building of the actual neural net, all params are self explanatory
		int seed = 100;
		int iterations = 10;
		MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
			       .seed(seed)
			       .iterations(iterations)
			       .learningRate(0.006)
			        .updater(Updater.NESTEROVS).momentum(0.9)
			        .regularization(true).l2(1e-4)
			        .weightInit(WeightInit.XAVIER)
			        .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
			       .list()
			       .layer(0, new DenseLayer.Builder()
			                .nIn(training.get(0).size()-1) // Number of input datapoints.
			                .nOut(20) // Number of output datapoints.
			                .activation("relu") // Activation function.
			                .weightInit(WeightInit.XAVIER) // Weight initialization.
			                .build())
			       .layer(1, new DenseLayer.Builder()
			                .nIn(20) // Number of input datapoints.
			                .nOut(50) // Number of output datapoints.
			                .activation("relu") // Activation function.
			                .weightInit(WeightInit.XAVIER) // Weight initialization.
			                .build())
			       .layer(2, new DenseLayer.Builder()
			                .nIn(50) // Number of input datapoints.
			                .nOut(50) // Number of output datapoints.
			                .activation("relu") // Activation function.
			                .weightInit(WeightInit.XAVIER) // Weight initialization.
			                .build())
			       .layer(3, new DenseLayer.Builder()
			                .nIn(50) // Number of input datapoints.
			                .nOut(20) // Number of output datapoints.
			                .activation("relu") // Activation function.
			                .weightInit(WeightInit.XAVIER) // Weight initialization.
			                .build())
			       .layer(4, new OutputLayer.Builder(LossFunction.NEGATIVELOGLIKELIHOOD).activation("softmax")
			                .nIn(20).nOut(2).build())
			       .pretrain(false).backprop(true)
			            .build();
		        
		        
		        
		 MultiLayerNetwork network = new MultiLayerNetwork(conf);
		 //initialization of the browser-based listener and fitting of the network to the data
		 network.init();
		 network.setListeners(new HistogramIterationListener(1));
		 int data_processed = 0;
		 while(iter.hasNext()){
			 DataSet next = iter.next();
			 System.out.println(next);
		     System.out.println(data_processed + "0 data points processed...");
		     network.fit(next);
		     data_processed++;
		 }
		 
		//evaluation of the neural network 
	     Evaluation eval = new Evaluation(2);
	        
		 recordReader = new CollectionRecordReader(testing);
		 iter = new RecordReaderDataSetIterator(recordReader, testing.get(0).size()-1, 2);
		 System.out.println("evaulation starting...");
		 
		 while(iter.hasNext()){
		      DataSet next = iter.next();
		      INDArray predict2 = network.output(next.getFeatureMatrix());
		      System.out.println(predict2);
	          eval.eval(next.getLabels(), predict2);
		}
		 System.out.println(eval.stats());
		 System.out.println("hi");
	}

}
