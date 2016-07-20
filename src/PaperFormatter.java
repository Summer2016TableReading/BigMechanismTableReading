import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;

public class PaperFormatter {
	public static void main(String[] args){
		File pdirectory = new File("rasPapers2");
		File[] papers = pdirectory.listFiles();
		for(File p: papers){
			formatPaper(p);
		}
	}

	private static void formatPaper(File p) {
		FileInputStream fis;
		Document paper = null;
		try {
			//paper = Jsoup.parse(pFile, null);
			fis = new FileInputStream(p);
			paper = Jsoup.parse(fis, null, p.toURI().toString(), Parser.xmlParser());
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		paper.html(paper.html().replace("title", "h3"));
		paper.html(paper.html().replace("<abstract>", "<abstract><h3>Abstract</h3><sec>"));
		paper.html(paper.html().replace("</abstract>", "</sec></abstract>"));
		/*Elements titles = paper.select("title");
		
		for(Element t:titles){
			t.html(t.html().replace("title", "h3"));
		}*/
		
		File outFile = new File("formattedPapers2/" + p.getName());
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
}
