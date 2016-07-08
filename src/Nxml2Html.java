import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import net.sf.saxon.Transform;

public class Nxml2Html {
	static String[] commandArgs = {"-t", "-s:jatsSpec\\temp.nxml", "-xsl:jatsSpec\\jats-html.xsl", "output"};
	public static void main(String[] args){
		File directory = new File("nxml");
		File[] nxmlFiles = directory.listFiles();
		File temp = new File("jatsSpec\\temp.nxml");
		Transform t = new Transform();
		for(File f: nxmlFiles){
			try {
				Files.copy(f.toPath(), temp.toPath(), StandardCopyOption.REPLACE_EXISTING);
				commandArgs[3] = "-o:newPapers\\" + f.getName().replace(".nxml", ".html");
				t.doTransform(commandArgs, null);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
