import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import org.apache.poi.xssf.usermodel.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import org.jsoup.select.Elements;
public class TextExtractor {
	public static void main(String[] args){
		File directory = new File("files");
		File[] tables = directory.listFiles();
		
	    XSSFWorkbook wb = new XSSFWorkbook();
	    XSSFSheet sheet = wb.createSheet();
		int rows = 0; 
		
		FileOutputStream text= null;
		FileOutputStream LDAtext= null;
		FileOutputStream fileNames = null;
		try {
            text = new FileOutputStream("tableText.txt");
            LDAtext = new FileOutputStream("processedTableText.txt");
            fileNames = new FileOutputStream("fileNames.txt");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } 
		PrintWriter pw = new PrintWriter(text);
		PrintWriter pw2 = new PrintWriter(LDAtext);
		PrintWriter pw3 = new PrintWriter(fileNames);
		
		for (File f: tables){
			if(f.getName().endsWith(".html")){
				String data = parseHTMLTable(f, sheet,rows);
				rows++;
				pw.write(data);
				String processedData = processString(data);
				if(!processedData.equals("\n")){
					pw2.write(processedData);
				}
				pw3.write(f.getName() + "\n");
				if(rows % 100 == 0){
					System.out.println(rows + " tables processed...");
				}
			}
		}
		pw.close();
		pw2.close();
		pw3.close();
		
		try {
            FileOutputStream fos = new FileOutputStream("output.xlsx");
            wb.write(fos);
            fos.close();
            wb.close();
            System.out.println("The output is successfully written");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

	}

	
	public static String[] stopWords = {"from", "and", "the", "of", "or", "that", "to",
										"as", "each", "are", "an", "than", "this", "in", 
										"on", "with", "into", "had", "was", "have", "which",
										"were", "by", "for", "at", "a", "these", "can",
										"is", "be", "has", "it", "also", "if", "they"};
	private static String processString(String data) {
		String processedData = data.toLowerCase();
		processedData = processedData.replaceAll(";", "");
		processedData = processedData.replaceAll(",", "");
		processedData = processedData.replaceAll("\\. ", " ");
		for(String word: stopWords){
			processedData = processedData.replaceAll(" " + word + " ", " ");
			processedData = processedData.replaceAll("^" + word + " ", "");
		}
		processedData = processedData.replaceAll("[\\s]+", " ");
		processedData = processedData + "\n";
		return processedData;
	}

	private static String parseHTMLTable(File f,  XSSFSheet sheet, int row) {
		String documentData = "";
		Document doc = null;
		try {
			doc = Jsoup.parse(f, null);
		} catch (IOException e) {
			e.printStackTrace();
		}
		XSSFRow r = sheet.createRow(row);
		int col = 0;
		XSSFCell c = r.createCell(col);
		c.setCellValue(f.getName());
		
		col++;
		c = r.createCell(col);
		
		Elements tables = doc.getElementsByTag("table");
		if(tables.size() > 0){
			Elements header = tables.get(0).getElementsByTag("thead");
			if(header.size() > 0){
				Elements headers = header.get(0).getElementsByTag("td");
				if(headers.size()>0){
					//System.out.println(f.getName() + " " + headers.text());
					c.setCellValue(headers.text());
					documentData += headers.text();
					if(headers.text().endsWith(".")){
						documentData += " \n";
					} else {
						documentData += ". \n";
					}
				} else {
					headers = header.get(0).getElementsByTag("th");
					c.setCellValue(headers.text());
					documentData += headers.text();
					if(headers.text().endsWith(".")){
						documentData += " \n";
					} else {
						documentData += ". \n";
					}
				}
			}
		}
		
		Elements captions = doc.getElementsByTag("p");
		if(captions.size()>0){
			for(Element caption: captions){
				col++;
				c = r.createCell(col);
				c.setCellValue(caption.text());
				documentData += caption.text();
				if(caption.text().endsWith(".")){
					documentData += " \n";
				} else {
					documentData += ". \n";
				}
			}
		}
		return documentData;
		//System.out.println(doc.select("table").toString());
	}
}
