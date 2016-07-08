import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Scanner;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class TableHeaderQuery {
	public static void main(String[] args){
		File directory = new File("files");
		Scanner s = new Scanner(System.in);
		String query = s.nextLine();
		while(!query.equals("exit")){
			ArrayList<String> matched = new ArrayList<String>();
			int num_tables = 0;
			for(File f: directory.listFiles()){
				if(f.getName().endsWith(".html")){
					try {
						Document table = Jsoup.parse(f, null);
						ArrayList<String> headers = getHeader(table);
						if(headers != null){
							if(matchQuery(query,headers)){
								matched.add(f.getName());
							}
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
					num_tables++;
					if(num_tables % 500 == 0){
						System.out.println(num_tables + " tables processed...");
					}
				}
			}
			
			if(matched.size() > 0){
				File topic = new File("queries/" + query);
				topic.mkdir();
				for(String f: matched){
					File source = new File("files/" + f);
					File target = new File("queries/" + query + "/"+ f);
					try {
						Files.copy(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				System.out.println(matched.size() + " matches found...");
			} else{
				System.out.println("No matches found...");
			}
			query = s.nextLine();
		}
		s.close();
	}
	
	private static boolean matchQuery(String s, ArrayList<String> list){
		for(String a: list){
			if(s.equals(a)){
				return true;
			}
		}
		return false;
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
