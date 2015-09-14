package org.archive.ntcir.dr.preprocess;

import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.HashMap;

import org.archive.OutputDirectory;
import org.archive.dataset.DataSetDiretory;
import org.archive.dataset.ntcir.NTCIRLoader;
import org.archive.util.Language.Lang;
import org.archive.util.format.StandardFormat;
import org.archive.util.io.IOText;
import org.archive.util.io.XmlWriter;
import org.archive.util.tuple.StrStr;

public class Preprocessor {
	
	//topicID -> baseline line of doc names
	public static HashMap<String, ArrayList<String>> loadNTCIR11Baseline_EN(){
		HashMap<String, ArrayList<String>> baselineMap = new HashMap<String, ArrayList<String>>();
		
		String baselineFile = DataSetDiretory.ROOT+"/ntcir/ntcir-11/DR/EN/EntireBaseline.txt";
		ArrayList<String> lineList = IOText.getLinesAsAList_UTF8(baselineFile);
		
		for(String line: lineList){
			String[] fields = line.split("\\s");
			String topicID = fields[0];
			
			if(baselineMap.containsKey(topicID)){
				baselineMap.get(topicID).add(fields[2]);
			}else{
				ArrayList<String> baseline = new ArrayList<String>();
				baseline.add(fields[2]);
				baselineMap.put(topicID, baseline);
			}
		}
		
		return baselineMap;
	}
	//
	public static HashMap<String, ArrayList<String>> loadNTCIR11Baseline_CH(){
		HashMap<String, ArrayList<String>> baselineMap = new HashMap<String, ArrayList<String>>();
		
		String baselineFile = DataSetDiretory.ROOT+"/ntcir/ntcir-11/DR/CH/DR_Baseline/BASELINE.txt";
		ArrayList<String> lineList = IOText.getLinesAsAList_UTF8(baselineFile);
		
		for(String line: lineList){
			String[] fields = line.split("\\s");
			String topicID = fields[0];
			
			if(baselineMap.containsKey(topicID)){
				baselineMap.get(topicID).add(fields[2]);
			}else{
				ArrayList<String> baseline = new ArrayList<String>();
				baseline.add(fields[2]);
				baselineMap.put(topicID, baseline);
			}
		}
		
		return baselineMap;
	}
	//docName -> docContent
	public static HashMap<String, String> loadNTCIR11BaselineDocs_EN(){
		HashMap<String, String> docMap = new HashMap<String, String>();
		
		String docDir = DataSetDiretory.ROOT+"ntcir/ntcir-11/DR/EN/Clueweb12ForNTCIR11/";
		
		try {
			for(int i=1; i<=25; i++){
				String file = docDir+StandardFormat.serialFormat(i, "00")+".txt";				
				ArrayList<String> lineList = IOText.getLinesAsAList(file, "utf-8");
				
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
						docMap.put(docName, buffer.toString());						
						buffer = null;
						//						
						fields = line.split("\\s");
						docName = fields[2];
						buffer = new StringBuffer();						
					}else{
						buffer.append(line);
						buffer.append("\n");						
					}
				}
				
				docMap.put(docName, buffer.toString());				
				buffer = null;
			}
		
		}catch(Exception e){
			e.printStackTrace();
		}
		
		return docMap;
	}
	
	private static void getBaselineDocs(){
		String dir = "H:/v-haiyu/CodeBench/Pool_DataSet/DataSet_DiversifiedRanking/ntcir/ntcir-11/DR/EN/Clueweb12ForNTCIR11/";
		String outputDir = "H:/v-haiyu/CodeBench/Pool_DataSet/DataSet_DiversifiedRanking/ntcir/ntcir-11/DR/EN/BaselineDoc/";
		
		try {
			
			ArrayList<String> entireBaseline = new ArrayList<String>();
			
			for(int i=1; i<=25; i++){
				String file = dir+StandardFormat.serialFormat(i, "00")+".txt";
				
				ArrayList<String> lineList = IOText.getLinesAsAList(file, "utf-8");
				
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
				
				entireBaseline.add(firstTarget);
				
				String [] fields = null;
				String docFile = null;
				BufferedWriter writer = null;
				//first doc
				fields = firstTarget.split("\\s");
				docFile = outputDir+fields[2]+".txt";				
				writer = IOText.getBufferedWriter_UTF8(docFile);				
				
				for(int k=firstIndex+1; k<lineList.size(); k++){
					String line = lineList.get(k);
					
					if(line.indexOf("Q0")>0 && line.indexOf("clueweb12")>=0 && line.indexOf("indri")>=0){
						writer.flush();
						writer.close();
						writer = null;
						//
						entireBaseline.add(line);
						fields = line.split("\\s");
						docFile = outputDir+fields[2]+".txt";
						writer = IOText.getBufferedWriter_UTF8(docFile);
					}else{
						writer.write(line);
						writer.newLine();
					}
				}
				
				writer.flush();
				writer.close();
				writer = null;
			}
			
			String entireBaselineFile = "H:/v-haiyu/CodeBench/Pool_DataSet/DataSet_DiversifiedRanking/ntcir/ntcir-11/DR/EN/EntireBaseline.txt";
			BufferedWriter baselinewWriter = IOText.getBufferedWriter_UTF8(entireBaselineFile);
			for(String line: entireBaseline){
				baselinewWriter.write(line);
				baselinewWriter.newLine();
			}
			baselinewWriter.flush();
			baselinewWriter.close();
			
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}
	//
	private static void generateQueryFile(){
		ArrayList<StrStr> topicList = NTCIRLoader.loadNTCIR11TopicList(Lang.English);		
		for(StrStr topic: topicList){
			System.out.println(topic.toString());
		}
		
		String dir = OutputDirectory.ROOT+"/ntcir-11/buffer/";
		
		try{
			int id = 1;
			String fileName;
			XmlWriter writer;
			
			fileName = "NTCIR11_SQ_"+StandardFormat.serialFormat(id, "00")+".txt";	
			writer = IOText.getXmlWriter_UTF8(dir+fileName);
			writer.startDocument("parameters");			
			writer.writeElement("printDocuments", Boolean.TRUE.toString());
			System.out.println(topicList.size());
			for(int i=1; i<=topicList.size(); i++){
				StrStr smTopic = topicList.get(i-1);
				System.out.println(smTopic.toString());
				
				if(i%2 == 0){
					writer.startElement("query");					
					writer.writeElement("type", "indri");
					writer.writeElement("number", smTopic.first);					
					writer.writeElement("text", "#combine("+smTopic.second.trim()+")");					
					writer.endElement("query");
					
					writer.endDocument("parameters");
					writer.flush();
					writer.close();
					writer = null;
					//
					id++;
					if((i+1) > topicList.size()){
						break;
					}else{
						fileName = "NTCIR11_SQ_"+StandardFormat.serialFormat(id, "00")+".txt";	
						writer = IOText.getXmlWriter_UTF8(dir+fileName);
						writer.startDocument("parameters");			
						writer.writeElement("printDocuments", Boolean.TRUE.toString());
					}					
				}else{
					writer.startElement("query");					
					writer.writeElement("type", "indri");
					writer.writeElement("number", smTopic.first); 					
					writer.writeElement("text", "#combine("+smTopic.second.trim()+")");					
					writer.endElement("query");
				}
			}	
		}catch(Exception e){
			e.printStackTrace();
		}	
	}
	
	public static void main(String []args){
		//1
		//Preprocessor.generateQueryFile();
		
		//2
		Preprocessor.getBaselineDocs();
	}

}
