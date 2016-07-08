import java.io.File;
import java.io.IOException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import jgibblda.Inferencer;
import jgibblda.LDACmdOption;
import jgibblda.Model;

public class LDAnalysis {
static String[] test = {"mice","gene","protein"};
	public static void main(String[] args) {
		LDACmdOption ldaOption = new LDACmdOption(); 
		ldaOption.inf = true; 
		ldaOption.dir = "C:/Users/hsiaov/Downloads/JGibbLDA-v.1.0/tableModel"; 
		ldaOption.modelName = "model-final"; 
		ldaOption.niters = 1000;
		
		Inferencer inferencer = new Inferencer(); 
		inferencer.init(ldaOption);
		
		File tFile = new File("files/PMC1557711T1.html");
		
		Document table = null;
		try {
			table = Jsoup.parse(tFile, null);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		String tableSentence = table.text();
		tableSentence = processString(tableSentence);
		String[] a = new String[1];
		a[0] = tableSentence;
		
		Model newModel = inferencer.inference(a);
		newModel.twords = 25;
		newModel.saveModelTwords("test.twords");
		newModel.saveModelTheta("vec.theta");
	}
	private static String processString(String data) {
		String processedData = data.toLowerCase();
		processedData = processedData.replaceAll(";", "");
		processedData = processedData.replaceAll(",", "");
		processedData = processedData.replaceAll("\\. ", " ");
		processedData = processedData.replaceAll("[\\s]+", " ");
		processedData = processedData + "\n";
		return processedData;
		
	}

}
