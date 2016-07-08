import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map.Entry;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

public class Word2VecTableCluster {
	public static void main(String[] args){
		ArrayList<ArrayList<String>> topics = new ArrayList<ArrayList<String>>();
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(new File("topic_model300k.json"));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		JsonReader jr = new JsonReader(new InputStreamReader(fis));
		JsonObject jsonObject = new JsonParser().parse(jr).getAsJsonObject();
		Iterator<Entry<String, JsonElement>> iter = jsonObject.entrySet().iterator();
		while(iter.hasNext()){
			Entry<String, JsonElement> entry = iter.next();
			ArrayList<String> words = new ArrayList<String>();
			for(JsonElement s: entry.getValue().getAsJsonArray()){
				words.add(s.getAsString());
			}
			topics.add(words);
		}
		
		File directory = new File("files");
		File[] tables = directory.listFiles();
		
		File clusterModel = new File("model-300k.theta");
		File fileNames = new File("fileNames.txt");
		FileOutputStream fos = null;
		FileOutputStream fosNames = null;
		PrintWriter pw = null;
		PrintWriter pwNames = null;
		try {
			fos = new FileOutputStream(clusterModel);
			fosNames = new FileOutputStream(fileNames);
			pw = new PrintWriter(fos);
			pwNames = new PrintWriter(fosNames);
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		int numTables = 0;
		for(File f: tables){
			if(f.getName().endsWith(".html")){
				try {
					Document paper = Jsoup.parse(new FileInputStream(f), null, f.toURI().toString(), Parser.xmlParser());
					String headers = processString(getHeader(paper));
					int numWords = 0;
					ArrayList<Double> vec = new ArrayList<Double>();
					for (int i = 0 ; i < topics.size(); i++){
						vec.add(0.0);
					}
					String[] words = headers.split(" ");
					for(String s: words){
						int index = findTopic(s, topics);
						if(index != -1){
							vec.set(index, vec.get(index) + 1);
							numWords++;
						}
					}
					if(numWords != 0){
						for (int i = 0 ; i < topics.size(); i++){
							vec.set(i, vec.get(i)/numWords);
						}
					}
					pwNames.write(f.getName() + "\n");
					for(int i = 0; i < vec.size(); i++){
						pw.write(vec.get(i) + " ");
					}
					pw.write("\n");
				} catch (IOException e) {
					e.printStackTrace();
				}
				numTables++;
				if(numTables % 500 == 0){
					System.out.println(numTables + " tables processed...");
				}
			}
		}
		pw.close();
		pwNames.close();
		try {
			fos.close();
			fosNames.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	private static int findTopic(String s, ArrayList<ArrayList<String>> topics) {
		for(int i = 0; i < topics.size(); i++){
			for(int j = 0; j < topics.get(i).size(); j++){
				if(s.equals(topics.get(i).get(j))){
					return i;
				}
			}
		}
		return -1;
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
	private static String getHeader(Document doc){
		String documentData = "";
		Elements tables = doc.getElementsByTag("table");
		if(tables.size() > 0){
			Elements header = tables.get(0).getElementsByTag("thead");
			if(header.size() > 0){
				Elements headers = header.get(0).getElementsByTag("td");
				if(headers.size()>0){
					documentData += headers.text();
				} else {
					headers = header.get(0).getElementsByTag("th");
					documentData += headers.text();
				}
			} else {
				Elements headers = tables.get(0).getElementsByTag("td");
				if(headers.size()>0){
					documentData += headers.text();
				}
			}
		}
		return documentData;
	}
}
