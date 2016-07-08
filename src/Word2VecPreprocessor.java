import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

public class Word2VecPreprocessor {
	public static void main(String[] args){
		File directory = new File("papers");
		File[] papers = directory.listFiles();
		File output = new File("paperText.txt");
		
		try {
			FileOutputStream fos = new FileOutputStream(output);
			PrintWriter outputpw = new PrintWriter(fos);
			for (int i = 0; i < papers.length; i++){	
				FileInputStream fis = new FileInputStream(papers[i]);
				try {
					Document paper = Jsoup.parse(fis, null, papers[i].toURI().toString(), Parser.xmlParser());
					String text = getText(paper);
					String[] sentences = text.split("\\. ");
					for(String s: sentences){
						outputpw.write(processString(s));
					}
				} catch (IndexOutOfBoundsException e){
					System.out.println("bad paper");
				}
				if(i % 500 == 0){
					System.out.println(i + " papers processed");
				}
			}
			outputpw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static String getText(Document paper){
		Elements body = paper.select("sec").select("p");
		return body.text();
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
