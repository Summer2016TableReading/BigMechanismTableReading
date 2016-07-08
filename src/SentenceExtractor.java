import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

public class SentenceExtractor {
	public static void main(String[] args){
		String paperId = "PMC1557711";
		String tablePath = "PMC1557711T1.html";
		extractSentence(paperId, tablePath);
	}
	public static void extractSentence(String paperId, String tablePath){
		String paperPath = paperId + ".html";
		File pFile = new File("formattedPapers/" + paperPath );
		File tFile = new File("files/" + tablePath );
		FileInputStream fis;
		Document paper = null;
		Document table = null;
		try {
			fis = new FileInputStream(pFile); 
			paper = Jsoup.parse(fis, "ISO-8859-1", pFile.toURI().toString(), Parser.xmlParser());
			table = Jsoup.parse(tFile, "ISO-8859-1");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		String tableText = processString(table.text());
		String text = getText(paper);
		String[] sentences = text.split("\\. ");
		String[] processedSentences = new String[sentences.length];
		for (int i = 0; i < sentences.length; i++){
			processedSentences[i] = processString(sentences[i]);
		}
		FileOutputStream fos;
		try {
			fos = new FileOutputStream(new File("LDAInput/" + paperId + ".txt"));
			PrintWriter pw = new PrintWriter(fos);
			pw.write((processedSentences.length + 1) + "\n");
			pw.write(tableText);
			for (String s: processedSentences){
				pw.write(s);
			}
			pw.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	private static String getText(Document paper){
		Elements body = paper.select("sec").select("p");
		return body.text();
	}

	public static String[] stopWords = { "from", "and", "the", "of", "or", "that", "to", "as", "each", "are", "an",
			"than", "this", "in", "on", "with", "into", "had", "was", "have", "which", "were", "by", "for", "at", "a",
			"these", "can", "is", "be", "has", "it", "also", "if", "they" };

	private static String processString(String data) {
		String processedData = data.toLowerCase();
		processedData = processedData.replaceAll(";", "");
		processedData = processedData.replaceAll(",", "");
		processedData = processedData.replaceAll("\\. ", " ");
		for (String word : stopWords) {
			processedData = processedData.replaceAll(" " + word + " ", " ");
			processedData = processedData.replaceAll("^" + word + " ", "");
		}
		processedData = processedData.replaceAll("[\\s]+", " ");
		processedData = processedData + "\n";
		return processedData;
}
}
