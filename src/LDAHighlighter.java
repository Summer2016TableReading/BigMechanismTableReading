import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Scanner;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

import jgibblda.LDA;

public class LDAHighlighter {
	public static void main(String[] args){
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
	static String[] LDAargs = {"-est", "-ntopics", "50", "-twords", "15", "-niters", 
			"1000", "-savestep", "1000", "-dir", "C:\\Users\\hsiaov\\workspace\\TableTextExtractor\\LDAinput",
			"-dfile", "processedTableText.txt"};
	public static void annotatePaper(String PMCID, String paperPath, String tablePath) {
		SentenceExtractor.extractSentence(PMCID, tablePath);
		LDAargs[12] = paperPath.replace(".html", ".txt");
		LDA.main(LDAargs);
		
		File model = new File("LDAInput/model-final.theta");
		FileInputStream fis;
		Scanner s = null;
		try {
			fis = new FileInputStream(model);
			s = new Scanner(fis);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		File pFile = new File("formattedPapers/" + paperPath);
		
		Document paper = null;
		try {
			//paper = Jsoup.parse(pFile, null);
			fis = new FileInputStream(pFile);
			paper = Jsoup.parse(fis, "ISO-8859-1", pFile.toURI().toString(), Parser.xmlParser());
		} catch (IOException e) {
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
		String[] parsedSentences = getText(paper).split("\\. ");
		ArrayList<String> sentences = new ArrayList<String>();
		for(String sent: parsedSentences){
			sentences.add(sent);
		}

		HashMap<String, Double> proximityWeights = new HashMap<String, Double>();
		double minDistance = 5.0;
		double maxDistance = 0.0;
		for (int i = 1; i < vectors.size(); i++){
			double distance = calculateDistance(vectors.get(i), vectors.get(0));
			if (distance < minDistance){
				minDistance = distance;
			}
			if (distance > maxDistance){
				maxDistance = distance;
			}
		}
		for (int i = 0; i < sentences.size(); i++){
			double distance = (maxDistance - calculateDistance(vectors.get(i + 1), vectors.get(0))) * 200.0/(maxDistance - minDistance) - 100;
			if(distance < 0){
				distance = 0;
			}
			proximityWeights.put(sentences.get(i), distance);
		}
		
		colorPaper(paper, proximityWeights);
		
		File outFile = new File("LDAnnotations/" + PMCID + ".html");
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
	
	private static double calculateDistance(ArrayList<Double> a, ArrayList<Double> b){
		double dist = 0;
		for(int i = 0; i < a.size(); i++){
			dist += (a.get(i) - b.get(i))*(a.get(i) - b.get(i));
		}
		return dist;
	}
}
