import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class AssayFinder {
	public static void main(String[] args){
		HashMap<String, ArrayList<String>> assayMap = new HashMap<String, ArrayList<String>>();
		OntobeeQuery.parseOntologies();
		HashSet<String> assays = OntobeeQuery.queryOntologyChildren(OntobeeQuery.obi, "assay");
		for(File file: new File("rasPapers").listFiles()){
			try {
				if(file.getName().endsWith(".html")){
					Document paper = Jsoup.parse(file, null);
					String text = paper.text().toLowerCase();
					System.out.println(file.getName());
					for (String s: assays){
						if (text.contains(s)){
							System.out.println(s);
							if(assayMap.containsKey(s)){
								assayMap.get(s).add(file.getName());
							} else {
								ArrayList<String> files = new ArrayList<String>();
								files.add(file.getName());
								assayMap.put(s, files);
							}
						}
					}
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		for(Entry<String, ArrayList<String>> entry: assayMap.entrySet()){
			System.out.println(entry.getKey() + " " + entry.getValue().size());
		}
	}
}
