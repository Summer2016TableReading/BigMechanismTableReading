import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class WordComparer {
	public static void main(String[] args){
		File wordDoc = new File("PMC 2361658 Table 1.htm");
		try {
			Document doc = Jsoup.parse(wordDoc, null);
			Elements spans = doc.getElementsByTag("span").select("*[style*='background']");
			ArrayList<String> sentences = new ArrayList<String>();
			for (Element span: spans){
				String[] sentenceArray = span.text().split("\\. ");
				for(String s:sentenceArray){
					sentences.add(s);
				}
			}
			HashMap<String,Double> weights = TfIdfAnalysis.annotatePaper("PMC2361658", "PMC2361658.html", "PMC2361658tbl1.html");
			for(String s: sentences){
				double weight = findMatch(weights, s);
				System.out.println(weight + " " + s);
			}
						
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	private static double findMatch(HashMap<String, Double> weights, String s) {
		Iterator<String> iter = weights.keySet().iterator();
		while(iter.hasNext()){
			String sentence = iter.next();
			if(sentence.toLowerCase().replace(" ", "").replace(".", "").contains(s.toLowerCase().replace(" ", "").replace(".", ""))){
				return weights.get(sentence);
			}
		}
		return -1;
	}
}
