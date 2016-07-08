import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

public class TfIdfAnalysis {
	
	public static void main(String[] args) {
		File pdirectory = new File("formattedPapers");
		File tdirectory = new File("rasTables");
		File[] papers = pdirectory.listFiles();
		File[] tables = tdirectory.listFiles();
		for(File p: papers){
			String PMCID = p.getName().replace(".html","");
			for(File t: tables){
				if(t.getName().startsWith(PMCID)){
					annotatePaper(PMCID, p.getName(), t.getName());
				}
			}
		}
	}

	public static HashMap<String, Double> annotatePaper(String PMCID, String paperPath, String tablePath) {
		HashMap<String, Integer> documentFreq = new HashMap<String, Integer>();
		HashMap<String, Double> inverseDocFreq = new HashMap<String, Double>();
		ArrayList<HashMap<String, Double>> tfidfVecs = new ArrayList<HashMap<String, Double>>();
		
		File pFile = new File("formattedPapers/" + paperPath);
		File tFile = new File("files/" + tablePath);
		
		FileInputStream fis;
		
		Document paper = null;
		try {
			//paper = Jsoup.parse(pFile, null);
			fis = new FileInputStream(pFile);
			paper = Jsoup.parse(fis, null, pFile.toURI().toString(), Parser.xmlParser());
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		Document table = null;
		try {
			table = Jsoup.parse(tFile, null);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		String tableSentence = table.text();
		tableSentence = processString(tableSentence);
		
		String paperText = getText(paper);
		String[] sentences = paperText.split("\\. ");
		int number = sentences.length + 1;
		processSentence(tableSentence, documentFreq);
		for(String s: sentences){
			processSentence(s, documentFreq);
		}
		Iterator<String> iter = documentFreq.keySet().iterator();
		while(iter.hasNext()){
			String s = iter.next();
			if(documentFreq.get(s) == 1){
				iter.remove();
			} else {
				inverseDocFreq.put(s, Math.log((double)number/(double)documentFreq.get(s)));
			}
		}
		HashMap<String, Double> proximityWeights = new HashMap<String, Double>();
		HashMap<String, Double> tableVec = calculateVec(tableSentence, inverseDocFreq);
		for(String s: sentences){
			HashMap<String, Double> weightVec = calculateVec(s,inverseDocFreq);
			tfidfVecs.add(weightVec);
			double dotProduct = calcDotProduct(weightVec,tableVec);
			proximityWeights.put(s,dotProduct);
		}
		
		Arrays.sort(sentences, new Comparator<String>() {
			public int compare(String a, String b) {
				if (proximityWeights.get(a) > proximityWeights.get(b)){
					return 1;
				} else if (proximityWeights.get(a) < proximityWeights.get(b)){
					return -1;
				} else {
					return 0;
				}
			}
		});
		
		File output = new File("annotationLogs/" + tablePath.replace(".html", ".txt"));
		PrintWriter pwOut = null;
		try {
			pwOut = new PrintWriter(new FileOutputStream(output));
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}
		System.out.println("Table Sentence:" + tableSentence);
		for(String s: sentences){
			System.out.println("Score: " + proximityWeights.get(s) + " Sentence: " + s);
			pwOut.println("Score: " + proximityWeights.get(s) + " Sentence: " + s);
		}
		pwOut.close();
		colorPaper(paper, proximityWeights);
		
		File outFile = new File("annotations/" + tablePath);
		FileOutputStream fos;
		try {
			fos = new FileOutputStream(outFile);
			PrintWriter pw = new PrintWriter(fos);
			pw.write(paper.toString());
			pw.close();
			fos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return proximityWeights;
	}
	
	
	private static void colorPaper(Document paper, HashMap<String, Double> proximityWeights){
		Iterator<String> iter = proximityWeights.keySet().iterator();
		iter = proximityWeights.keySet().iterator();
		while(iter.hasNext()){
			String query = iter.next();
			if(paper.text().contains(query)){
				double weight = proximityWeights.get(query);
				Element section = paper.getElementsContainingText(query).last();
				String text = section.text();
				String html = section.html();
				if(text.contains(query)){
					int start = 0;
					if(html.contains(query)){
						start = html.indexOf(query);
						int r = 255;
						int b = 255 - (int)(255 * weight/100);
						int g = 255 - (int)(255 * weight/100);
						if(b < 0 ){
							b = 0;
						} 
						if(g < 0 ){
							g = 0;
						}
						String hex = String.format("#%02x%02x%02x", r, g, b);
						
						section.html(html.substring(0,start) + "<span style=\"background-color:"  + hex + "\">" + query + "</span>" + html.substring(start + query.length()));
					} else {
						start = text.indexOf(query);
						String[] textSnippet = text.split("\\. ");
						String[] htmlSnippet = html.split("\\. ");
						int index = 0;
						for (int i = 0; i < textSnippet.length; i++){
							if(textSnippet[i].contains(query)){
								index = i;
							}
						}
						
						
						int r = 255;
						int b = 255 - (int)(255 * weight/100);
						int g = 255 - (int)(255 * weight/100);
						if(b < 0 ){
							b = 0;
						} 
						if(g < 0 ){
							g = 0;
						}
						String hex = String.format("#%02x%02x%02x", r, g, b);
						try {
						section.html(html.substring(0,html.indexOf(htmlSnippet[index])) + "<span style=\"background-color:"  + hex + "\">" + query + "</span>" + html.substring(html.indexOf(htmlSnippet[index]) + htmlSnippet[index].length()));
						} catch (ArrayIndexOutOfBoundsException a){
							System.out.println("unusual formatting");
						}
					}
				}
			}
		}
	}
	
	private static String getText(Document paper){
		Elements body = paper.select("sec").select("p");
		return body.text();
	}

	private static double calcDotProduct( HashMap<String, Double> a,  HashMap<String, Double> b){
		double sum = 0;
		Iterator<String> iter = a.keySet().iterator();
		while(iter.hasNext()){
			String s = iter.next();
			if(b.containsKey(s)){
				sum += a.get(s)*b.get(s);
			}
		}
		return sum;
	}
	private static HashMap<String, Double> calculateVec(String s, HashMap<String, Double> inverseDocFreq) {
		HashSet<String> words = new HashSet<String>();
		String processedSentence = processString(s);
		String[] tokens = processedSentence.split(" ");
		HashMap<String, Integer> tfVec = new HashMap<String, Integer>();
		for(String w: tokens){
			if(!words.contains(w)){
				if(tfVec.get(w) == null){
					tfVec.put(w, 1);
				} else {
					tfVec.put(w, tfVec.get(w)+1);
				}
				words.add(w);
			}
		}
		HashMap<String, Double> vec = new HashMap<String, Double>();
		Iterator<String> iter = tfVec.keySet().iterator();
		while(iter.hasNext()){
			String temp = iter.next();
			if(inverseDocFreq.containsKey(temp)){
				vec.put(temp, (double) (tfVec.get(temp) * inverseDocFreq.get(temp)));
			}
		}
		return vec;
	}

	private static void processSentence(String s, HashMap<String, Integer> documentFreq) {
		HashSet<String> words = new HashSet<String>();
		String processedSentence = processString(s);
		String[] tokens = processedSentence.split(" ");
		for(String w: tokens){
			if(!words.contains(w)){
				if(documentFreq.get(w) == null){
					documentFreq.put(w, 1);
				} else {
					documentFreq.put(w, documentFreq.get(w)+1);
				}
				words.add(w);
			}
		}
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
