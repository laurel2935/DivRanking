/** Frequently used document utilities (reading files, tokenization, extraction
 *  of TF vectors).
 *   
 * @author Scott Sanner (ssanner@gmail.com)
 */

package org.archive.util;

import java.io.*;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.archive.nlp.tokenizer.SimpleTokenizer;


public class DocUtils {
	
	public static final SimpleTokenizer ST = new SimpleTokenizer();
	
	public static final String SPLIT_TOKENS = "[!\"#$%&'()*+,./:;<=>?\\[\\]^`{|}~\\s]"; // missing: [_-@]
		
	public final static DecimalFormat DF2 = new DecimalFormat("#.##");
	public final static DecimalFormat DF3 = new DecimalFormat("#.###");

	public static String ReadFile(File f) {
		return ReadFile(f, false);
	}
	
	public static String ReadFile(File f, boolean keep_newline) {
		try {
			StringBuilder sb = new StringBuilder();
			java.io.BufferedReader br = new BufferedReader(new FileReader(f));
			String line = null;
			while ((line = br.readLine()) != null) {
				//System.out.println(line);
				sb.append((sb.length()> 0 ? (keep_newline ? "\n" : " ") : "") + line);
			}
			br.close();
			return sb.toString();
		} catch (Exception e) {
			System.out.println("ERROR: " + e);
			return null;
		}
	}

	// Note: this split-based tokenizer breaks words down into smaller 
	//       components than ST but the results seem improved.
	public static ArrayList<String> Tokenize(String sent) {
		ArrayList<String> result = new ArrayList<String>();
		String tokens[] = sent.split(SPLIT_TOKENS); 
		//ArrayList<String> tokens = ST.extractTokens(sent, true);
		for (String token : tokens) {
			token = token.trim().toLowerCase();
			if (token.length() == 0)
				continue;
			result.add(token);
		}
		return result;
	}
	
	//
	/**
	 * Return a feature map for a sentence, formulated as <token, token_frequency>.
	 * Note: this split-based tokenizer breaks words down into smaller components than ST
	 * but the results seem improved.
	 * **/
	public static Map<Object,Double> ConvertToFeatureMap(String sent) {
		Map<Object,Double> map = new HashMap<Object,Double>();
		String tokens[] = sent.split(SPLIT_TOKENS);
		//ArrayList<String> tokens = ST.extractTokens(sent, true);
		for (String token : tokens) {
			token = token.trim().toLowerCase();
			if (token.length() == 0)
				continue;
			if (map.containsKey(token))
				map.put(token, map.get(token) + 1d);
			else
				map.put(token, 1d);
		}
		return map;
	}
	
	//
	public static boolean Move(File srcFile, String destPath){
        // Destination directory
        File dir = new File(destPath);
       
        // Move file to new directory
        boolean success = srcFile.renameTo(new File(dir, srcFile.getName()));
       
        return success;
    }
	
	//
	public static boolean Copy(File oldfile, String newFile){    
		  try{    
	          //int bytesum = 0;    
	          int byteread = 0;    
	          	
	          InputStream inStream = new FileInputStream(oldfile);     
              FileOutputStream  fs = new FileOutputStream(newFile);    
              byte[] buffer = new byte[4096];    
              while( (byteread = inStream.read(buffer))!= -1){    
                 // bytesum += byteread;        
                  //System.out.println(bytesum);    
                  fs.write(buffer, 0, byteread);    
              }    
              inStream.close();   
              fs.flush();
              fs.close();  
	          return true;
		  }catch(Exception e){    
	          System.out.println( "error  ");    
	          e.printStackTrace(); 
	          return false;
		  }    		  
    }    
	
	//
	public static boolean createNewFile(File newFile, String content){
		try {
			newFile.createNewFile();
			BufferedWriter bWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(newFile)));
			bWriter.write(content);
			bWriter.flush();
			bWriter.close();
			return true;
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
			return false;			
		}		
	}
}
