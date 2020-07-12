package sessonControl;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

public class xmlReader {
	public String getXml(String fileName){
		BufferedReader br;
		try {
			br = new BufferedReader(new InputStreamReader(new FileInputStream(fileName)));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		}
		String line;
		StringBuilder sb = new StringBuilder();
		try {
			while((line = br.readLine()) != null ){
				sb.append(line);
			}
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		return sb.toString();
		
	}
	
	public String getXml(){

		return getXml("xmlTest.txt");
	}
}
