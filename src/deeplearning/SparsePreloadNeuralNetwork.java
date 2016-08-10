package deeplearning;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;

import org.canova.api.io.data.DoubleWritable;
import org.canova.api.records.reader.RecordReader;
import org.canova.api.records.reader.impl.CollectionRecordReader;
import org.canova.api.writable.Writable;
import org.deeplearning4j.datasets.canova.RecordReaderDataSetIterator;
import org.deeplearning4j.datasets.iterator.DataSetIterator;
import org.deeplearning4j.eval.Evaluation;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.Updater;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.ui.weights.HistogramIterationListener;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.api.DataSet;
import org.nd4j.linalg.lossfunctions.LossFunctions.LossFunction;

public class SparsePreloadNeuralNetwork {
	public static void main(String[] args){
		ArrayList<ArrayList<Writable>> trainingSet = new ArrayList<ArrayList<Writable>>();
		ArrayList<ArrayList<Writable>> testingSet = new ArrayList<ArrayList<Writable>>();
		
		try {
			Scanner sTraining = new Scanner(new File("trainingVectors"));
			Scanner sTesting = new Scanner(new File("trainingVectors"));
			while(sTraining.hasNext()){
				String s = sTraining.nextLine();
				String[] vals = s.split(",");
				ArrayList<Writable> vec = new ArrayList<Writable>();
				for(int i = 0; i < vals.length - 1; i++){
					String val = vals[i];
					double d = Math.log(Double.parseDouble(val)+1);
					vec.add(new DoubleWritable(d));
				}
				vec.add(new DoubleWritable(Integer.parseInt(vals[vals.length-1])));
				trainingSet.add(vec);
			}
			while(sTesting.hasNext()){
				String s = sTesting.nextLine();
				String[] vals = s.split(",");
				ArrayList<Writable> vec = new ArrayList<Writable>();
				for(int i = 0; i < vals.length - 1; i++){
					String val = vals[i];
					double d = Math.log10(Double.parseDouble(val)+1);
					vec.add(new DoubleWritable(d));
				}
				vec.add(new DoubleWritable(Integer.parseInt(vals[vals.length-1])));
				testingSet.add(vec);
			}
			sTraining.close();
			sTesting.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		
		buildDeepLearning(trainingSet, testingSet, 2);
	}
	
	private static void buildDeepLearning(ArrayList<ArrayList<Writable>> training, ArrayList<ArrayList<Writable>> testing, int num_labels) {
		int batchSize = training.size();
		int seed = 100;
		int iterations = 10;
		
		RecordReader recordReader = new CollectionRecordReader(training);
		System.out.println("building nn");
		
		int layer = 0;
		MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
			       .seed(seed)
			       .iterations(iterations)
			       .learningRate(0.001)
			        .updater(Updater.NESTEROVS).momentum(0.9)
			        .regularization(true).l2(1e-4)
			        .weightInit(WeightInit.XAVIER)
			        .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
			       .list()
			       .layer(layer++, new DenseLayer.Builder()
			                .nIn(training.get(0).size()-1) // Number of input datapoints.
			                .nOut(200) // Number of output datapoints.
			                .activation("relu") // Activation function. 
			                .weightInit(WeightInit.XAVIER) // Weight initialization.
			                .build())
			       /*.layer(layer++, new DenseLayer.Builder()
			                .nIn(70) // Number of input datapoints.
			                .nOut(50) // Number of output datapoints.
			                .activation("relu") // Activation function.
			                .weightInit(WeightInit.XAVIER) // Weight initialization.
			                .build())
			       .layer(layer++, new DenseLayer.Builder()
			                .nIn(50) // Number of input datapoints.
			                .nOut(30) // Number of output datapoints.
			                .activation("relu") // Activation function.
			                .weightInit(WeightInit.XAVIER) // Weight initialization.
			                .build())
			       .layer(layer++, new DenseLayer.Builder()
			                .nIn(30) // Number of input datapoints.
			                .nOut(20) // Number of output datapoints.
			                .activation("relu") // Activation function.
			                .weightInit(WeightInit.XAVIER) // Weight initialization.
			                .build())*/
			       .layer(layer++, new OutputLayer.Builder(LossFunction.NEGATIVELOGLIKELIHOOD).activation("softmax")
			                .nIn(200).nOut(num_labels).build())
			       .pretrain(false).backprop(true)
			            .build();
		        
		        
		        
		 MultiLayerNetwork network = new MultiLayerNetwork(conf);
		 network.init();
		 network.setListeners(new HistogramIterationListener(1));
		 int data_processed = 0;
		 
		 for (int i = 0; i < 100; i ++){
			 recordReader = new CollectionRecordReader(training);
			 DataSetIterator iter = new RecordReaderDataSetIterator(recordReader, batchSize, training.get(0).size()-1, num_labels);
			 while(iter.hasNext()){
				 DataSet next = iter.next();
				 //System.out.println(next);
		    	System.out.println((batchSize*data_processed) + " data points processed...");
		    	network.fit(next);
		    	data_processed++;
			 }
			 if(i % 10 == 0){
				 evalauteEpoch(testing, num_labels, network);
			 }
		 }
		
	     Evaluation eval = new Evaluation(num_labels);
	        
		 recordReader = new CollectionRecordReader(testing);
		 DataSetIterator iter = new RecordReaderDataSetIterator(recordReader, testing.get(0).size()-1, num_labels);
		 System.out.println("evaulation starting...");
		 
		 while(iter.hasNext()){
		      DataSet next = iter.next();
		      INDArray predict2 = network.output(next.getFeatureMatrix());
		      System.out.println(predict2);
	          eval.eval(next.getLabels(), predict2);
		}
		 System.out.println(eval.stats());
		 System.out.println("hi"); 
		 
		 //System.out.println(network.getLayer(0).paramTable());
	}

	private static void evalauteEpoch(ArrayList<ArrayList<Writable>> testing, int num_labels, MultiLayerNetwork network) {
		 RecordReader recordReader = new CollectionRecordReader(testing);
		 DataSetIterator iter = new RecordReaderDataSetIterator(recordReader, testing.get(0).size()-1, num_labels);
		 Evaluation eval = new Evaluation(num_labels);
		 
		 while(iter.hasNext()){
		      DataSet next = iter.next();
		      INDArray predict2 = network.output(next.getFeatureMatrix());
	          eval.eval(next.getLabels(), predict2);
		}
		 System.out.println(eval.stats());
	}
}
