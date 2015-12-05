package org.archive.ireval.toy;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import org.archive.util.format.StandardFormat;
import org.archive.util.io.IOText;

public class Accessor {
	public static final String newline = System.getProperty("line.separator");

	//static HashMap<String, String> _enDocid2HtmlMap = new HashMap<>();
	//static HashMap<String, String> _chDocid2HtmlMap = new HashMap<>();
	
	//docName -> docContent
	private static HashMap<String, String> loadNTCIR11BaselineDocs_EN(){
		HashMap<String, String> enDocid2HtmlMap = new HashMap<>();
		
		String docDir = "C:/T/WorkBench/Bench_Dataset/DataSet_DiversifiedRanking/ntcir/ntcir-11/DR/EN/Clueweb12ForNTCIR11/";
		
		try {
			for(int i=1; i<=25; i++){
				String file = docDir+StandardFormat.serialFormat(i, "00")+".txt";				
				ArrayList<String> lineList = getLinesAsAList_UTF8(file);
				
				String firstTarget = null;
				int firstIndex = 0;
				for(int k=0; k<lineList.size(); k++){
					String line = lineList.get(k);
					if(line.indexOf("Q0")>0 && line.indexOf("clueweb12")>=0 && line.indexOf("indri")>=0){
						firstIndex = k;
						firstTarget = line;
						break;
					}
				}
				
				String [] fields = null;		
				String docName = null;
				StringBuffer buffer = null;
				//first doc
				fields = firstTarget.split("\\s");				
				docName = fields[2];				
				buffer = new StringBuffer();				
				
				for(int k=firstIndex+1; k<lineList.size(); k++){
					String line = lineList.get(k);
					
					if(line.indexOf("Q0")>0 && line.indexOf("clueweb12")>=0 && line.indexOf("indri")>=0){
						enDocid2HtmlMap.put(docName, buffer.toString());						
						buffer = null;
						//						
						fields = line.split("\\s");
						docName = fields[2];
						buffer = new StringBuffer();						
					}else{
						buffer.append(line);
						buffer.append(newline);						
					}
				}
				
				enDocid2HtmlMap.put(docName, buffer.toString());				
				buffer = null;
			}
		
		}catch(Exception e){
			e.printStackTrace();
		}
		
		return enDocid2HtmlMap;
	}
	
	private static HashMap<String, String> loadOutlierDocs_En(){
		String inputFile = "C:/T/Research/CurrentResearch/NTCIR/NTCIR-11/Ntcir11-IMine/"
				+ "Eval-IMine/UncertainPairs/MarginalUtility/EN_OutlierDocs.txt";

		ArrayList<SogouTHtml> sogouTHtmlList = new ArrayList<>();
		
		try {			
			//GBK & UTF-8
			//System.out.println("inputFile:\t"+inputFile);
			BufferedReader tReader = getBufferedReader_UTF8(inputFile);
			
			String line=null, docno=null, url=null;
			StringBuffer buffer = new StringBuffer();
			
			while(null != (line=tReader.readLine())){
				if(line.length() > 0){	
					if(line.equals("</doc>")){ 							
						SogouTHtml sogouTHtml = new SogouTHtml(docno, url, buffer.toString());
						sogouTHtmlList.add(sogouTHtml);							
					}else if(line.startsWith("<docno>") && line.endsWith("</docno>")){
						
						docno = line.substring(7, line.length()-8);
						buffer.delete(0, buffer.length());
						
					}else if(line.startsWith("<url>") && line.endsWith("</url>")){
						
						url = line.substring(5, line.length()-6);
						
					}else if(line.equals("<doc>")){
						
					}else{
						buffer.append(line+newline);
					}									
				} 					
			}
			
			tReader.close();
			tReader = null; 				
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		HashMap<String, String> enDocid2HtmlMap = new HashMap<>();
		
		for(SogouTHtml sogouTHtml: sogouTHtmlList){
			enDocid2HtmlMap.put(sogouTHtml.getDocNo(), sogouTHtml.getHtmlStr());
		}
		
		return enDocid2HtmlMap;
	}
	
	public static HashMap<String, String> loadDocs_En(){
		HashMap<String, String> map1 = loadNTCIR11BaselineDocs_EN();
		HashMap<String, String> map2 = loadOutlierDocs_En();
		System.out.println(map1.size());
		System.out.println(map2.size());
		
		for(Entry<String, String> entry: map2.entrySet()){
			map1.put(entry.getKey(), entry.getValue());
		}
		map2=null;
		
		return map1;
	}
	
	public static HashMap<String, String> loadDocs_Ch(){
		return null;
	}
	////
	public static ArrayList<String> getLinesAsAList_UTF8(String targetFile){
		try {
			ArrayList<String> lineList = new ArrayList<String>();
			//
			BufferedReader reader = getBufferedReader_UTF8(targetFile);
			String line = null;			
			while(null != (line=reader.readLine())){
				if(line.length() > 0){					
					lineList.add(line);					
				}				
			}
			reader.close();
			return lineList;			
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		return null;
	}
	////
	public static BufferedReader getBufferedReader_UTF8(String targetFile) throws IOException{
		File file = new File(targetFile);
		if(!file.exists()){
			return null;
		}
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(targetFile), "utf-8"));
		return reader;
	}

	//
	public static void getPolishedHtmls(String dir){
		String inputDir = dir+"En/";
		//String outputDir = dir+"PolishedEn/";
		
		File inputD = new File(inputDir);
		File [] inputFiles = inputD.listFiles();
		
		for(File file: inputFiles){
			String path = file.getAbsolutePath();
			ArrayList<String> lineList = IOText.getLinesAsAList_UTF8(path);
			try {
				int firstIndex = -1;
				
				for(int k=0; k<lineList.size(); k++){
					String line = lineList.get(k);
					if(line.indexOf("Content-Type")>=0){
						firstIndex = k+1;
						break;
					}
					if(line.indexOf("<!DOCTYPE")>=0 || line.indexOf("<html>")>=0 ||
							line.startsWith("<!DOCTYPE ")){
						firstIndex = k;
					}
				}
				
				if(firstIndex >= 0){
					BufferedWriter writer = IOText.getBufferedWriter_UTF8(path.replaceFirst("En", "PolishedEn"));
					for(int k=firstIndex; k<lineList.size(); k++){
						writer.write(lineList.get(k));
						writer.newLine();
					}
					writer.flush();
					writer.close();
				}else{
					System.err.println(path);
				}				
			} catch (Exception e) {
				// TODO: handle exception
				e.printStackTrace();
			}
		}		
	}
	
	////
	public static void main(String []args){
		//1 unsure
		///*
		String dir1 = "C:/T/Research/CurrentResearch/NTCIR/NTCIR-11/Ntcir11-IMine/"
				+ "Eval-IMine/UncertainPairs/MarginalUtility/UnSureHtml/";
		Accessor.getPolishedHtmls(dir1);
		//*/
		
		//2 nosure
		String dir2 = "C:/T/Research/CurrentResearch/NTCIR/NTCIR-11/Ntcir11-IMine/"
				+ "Eval-IMine/UncertainPairs/MarginalUtility/NoSureHtml/";
		Accessor.getPolishedHtmls(dir2);
		
	}
}
