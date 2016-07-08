import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

public class Word2VecClusterAnalyzer {
	public static void main(String[] args) {
		ArrayList<ArrayList<String>> clusters = KMeansTableCluster.cluster("model-300k.theta", "fileNames.txt", 50);
		File output = new File("300k_clusters.txt");
		FileOutputStream fos;
		try {
			fos = new FileOutputStream(output);
			PrintWriter pw = new PrintWriter(fos);
			for (ArrayList<String> c : clusters) {
				for (String s : c) {
					pw.write(s + " ");
				}
				pw.write("\n");
			}
			pw.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		File headers = new File("300k_headers.txt");
		FileOutputStream fosHeaders;
		try {
			fosHeaders = new FileOutputStream(headers);
			PrintWriter pw = new PrintWriter(fosHeaders);
			for (ArrayList<String> c : clusters) {
				HashMap<String, Integer> histogram = new HashMap<String, Integer>();
				for (String s : c) {
					File table = new File("files/" + s);
					Document paper = Jsoup.parse(new FileInputStream(table), null, table.toURI().toString(), Parser.xmlParser());
					ArrayList<String> headerArray = getHeader(paper);
					if(headerArray != null){
						for (String h : headerArray) {
							if(!(h.equals("") || h.equals(" ") || h.equals(" "))){
								String data = processString(h);
								if (histogram.containsKey(data)) {
									histogram.put(data, histogram.get(data) + 1);
								} else {
									histogram.put(data, 1);
								}
							}
						}
					}
				}
				ArrayList<String> cols = new ArrayList<String>();
				Iterator<String> iter = histogram.keySet().iterator();
				while(iter.hasNext()){
					cols.add(iter.next());
				}
				Collections.sort(cols, new Comparator<String>() {
					public int compare(String a, String b) {
						if (histogram.get(a) < histogram.get(b)){
							return 1;
						} else if (histogram.get(a) > histogram.get(b)){
							return -1;
						} else {
							return 0;
						}
					}
				});
				iter = cols.iterator();
				while(iter.hasNext()){
					String s = iter.next();
					pw.write("\"" + s + "\": " + histogram.get(s) + " ");
				}
				pw.write("\n");
			}
			pw.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static String processString(String data) {
		String processedData = data.toLowerCase();
		processedData = processedData.replaceAll(";", "");
		processedData = processedData.replaceAll(":", "");
		processedData = processedData.replaceAll("\\(", " ");
		processedData = processedData.replaceAll("\\)", "");
		processedData = processedData.replaceAll("\\[", " ");
		processedData = processedData.replaceAll("\\]", "");
		processedData = processedData.replaceAll("%", "");
		processedData = processedData.replaceAll(",", "");
		processedData = processedData.replaceAll("\\. ", " ");
		processedData = processedData.replaceAll("[\\s]+", " ");
		return processedData;
	}
	
	private static ArrayList<String> getHeader(Document doc){
		ArrayList<String> documentData = null;
		Elements tables = doc.getElementsByTag("table");
		if(tables.size() > 0){
			Elements header = tables.get(0).getElementsByTag("thead");
			if(header.size() > 0){
				Elements headers = header.get(0).getElementsByTag("td");
				if(headers.size()>0){
					documentData = separateHeaders(headers);
				} else {
					headers = header.get(0).getElementsByTag("th");
					documentData = separateHeaders(headers);
				}
			} else {
				Elements headers = tables.get(0).getElementsByTag("td");
				if(headers.size()>0){
					documentData = separateHeaders(headers);
				}
			}
		}
		return documentData;
	}
	
	private static ArrayList<String> separateHeaders(Elements headers){
		ArrayList<String> docData = new ArrayList<String>();
		for (Element a: headers){
			docData.add(a.text());
		}
		return docData;
	}
}
