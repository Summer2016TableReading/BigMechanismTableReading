import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

import lookup.TabLookup;

public class ProteinTableFinder {
	public static void main(String[] args){
		TabLookup tl = TabLookup.getInstance();
		File output = new File("proteinPMCs.txt");
		File directory = new File("files");
		PrintWriter pw = null;
		try {
			pw = new PrintWriter(new FileOutputStream(output));
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}
		for (File table: directory.listFiles()){
			if(table.getName().endsWith(".html")){
				try {
					Document paper = Jsoup.parse(new FileInputStream(table), null, table.toURI().toString(), Parser.xmlParser());
					ArrayList<String> headers = getHeader(paper);
					if(headers != null && headers.size() > 0){
						String p = getProtein(headers, tl) ;
						if(p != null){
								pw.write(table.getName() + " " + p + "\n");
						}
					}
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
	
	private static String getProtein(ArrayList<String> headers, TabLookup tl) {
		for(String s: headers){
			String ps = processString(s);
			if (ps.length() > 2){
				if(/*tl.english.containsKey(ps) ||*/ tl.genename.containsKey(ps.toUpperCase())){
					return ps;
				} else {
					String[] words = ps.split(" ");
					for(String word: words){
						if(word.length() > 2){
							if(/*tl.english.containsKey(word) ||*/ tl.genename.containsKey(word.toUpperCase())){
								return word;
							}
						}
					}
				}	
			}
		}
		return null;
	}

	private static String processString(String s) {
		s = s.toLowerCase();
		s = s.replaceAll("\\W+", " ");
		return s;
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
				Elements headers = tables.get(0).getElementsByTag("tr");
				if(headers.size()>0){
					headers = headers.get(0).getElementsByTag("td");
					if(headers.size()>0){
						documentData = separateHeaders(headers);
					}
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
