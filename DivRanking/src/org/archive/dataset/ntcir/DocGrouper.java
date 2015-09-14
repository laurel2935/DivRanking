package org.archive.dataset.ntcir;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.File;
import java.io.PrintStream;

import org.archive.util.DocUtils;

public class DocGrouper {
	
	private static void groupFiles(int top_k){
		String sourceDir = "E:/Data_Log/DataSource_Raw/NTCIR-10/DocumentRanking/BASELINE-D-C-1-top1000contents/BASELINE-D-C-1-top1000contents/";
		String destinationDir = "E:/Data_Log/DataSource_Raw/NTCIR-10/DocumentRanking/doc/";
		String baselineFile = "./dataset/ntcir/ntcir-10/BASELINE-D-C-1.txt";		
		//
		try {
			PrintStream logPrinter = new PrintStream(new FileOutputStream(new File(
					"E:/Data_Log/DataSource_Raw/NTCIR-10/DocumentRanking/GroupingLog.txt")));
			//
			System.setOut(logPrinter);	
			String line;
			String []parts;			
			//
			BufferedReader br = new BufferedReader(new FileReader(baselineFile));
			//without first line
			br.readLine();
			while(null != (line=br.readLine())){
				parts = line.split(" ");
				//System.out.println(parts[0]+"\t"+parts[1]+"\t"+parts[2]);
				if(Integer.parseInt(parts[3]) < top_k){
					File dir = new File(destinationDir+parts[0]);
					System.out.println("Processing\t"+parts[0]);
					if (!dir.exists()) {
						dir.mkdir();
					} 
					//
					File file = new File(sourceDir+parts[2]+".htm");
					if(file.exists()){      
						DocUtils.Copy(file, dir.getAbsolutePath()+"/"+parts[2]+".htm");
			        }else{
			        	  System.out.println("Non-exist of File : "+file.getAbsolutePath());
			        } 
				}else{
					continue;
				}				
			}
			br.close();
			//
			logPrinter.flush();
			logPrinter.close();
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}
	
	//
	public static void main(String []args){
		//1
		DocGrouper.groupFiles(100);
	}

}
