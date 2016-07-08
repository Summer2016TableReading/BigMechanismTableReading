import java.io.File;
import java.util.Collection;

import extract.ColumnData;
import extract.HTMLTable;
import extract.HTMLTableExtractor;

public class TableExtractorTest {
	public static void main(String[] args){
		File directory = new File("rasTables");
		for (File f: directory.listFiles()){
			HTMLTableExtractor hte = new HTMLTableExtractor();
			Collection<HTMLTable> list = hte.parseHTML("rasTables/" + f.getName());
			HTMLTable table = list.iterator().next();
			ColumnData[] cols = table.getColumnData();
			for(ColumnData col: cols){
				System.out.print(col.getHeader() + ": "); 
				for(String s: col.getData()){
					System.out.print(s + " ");
				}
				System.out.println("");
			}
		}
	}
}
