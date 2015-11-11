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
	////For NTCIR11 DR En
	private static void getBaselineDocs_NTCIR11_DR_En(){
		String ntcir11_DR_EN_Dir = "C:/T/WorkBench/Bench_Dataset/DataSet_DiversifiedRanking/ntcir/ntcir-11/DR/EN/";
		
		String intputDir = ntcir11_DR_EN_Dir+"Clueweb12ForNTCIR11/";
		String outputDir = ntcir11_DR_EN_Dir+"BaselineDoc/";
		
		try {
			
			ArrayList<String> entireBaseline = new ArrayList<String>();
			
			for(int i=1; i<=25; i++){
				String file = intputDir+StandardFormat.serialFormat(i, "00")+".txt";
				
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
			
			String entireBaselineFile = ntcir11_DR_EN_Dir+"EntireBaseline.txt";
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
	////For TREC Div 2009-2012
	private static void getBaselineDocs_TREC_Div(String root, String identifier){
		//String ntcir11_DR_EN_Dir = "C:/T/WorkBench/Bench_Dataset/DataSet_DiversifiedRanking/ntcir/ntcir-11/DR/EN/";
		
		String intputDir = root+"TopResults_5/";
		String outputDir = root+"BaselineDoc/";
		
		try {
			
			ArrayList<String> entireBaseline = new ArrayList<String>();
			
			for(int i=1; i<=10; i++){
				String file = intputDir+StandardFormat.serialFormat(i, "00")+".txt";
				
				ArrayList<String> lineList = IOText.getLinesAsAList(file, "utf-8");
				
				String firstTarget = null;
				int firstIndex = 0;
				for(int k=0; k<lineList.size(); k++){
					String line = lineList.get(k);
					if(line.indexOf("Q0")>0 && line.indexOf("clueweb09")>=0 && line.indexOf("indri")>=0){
						firstIndex = k;
						firstTarget = line;
						break;
					}
				}
				
				entireBaseline.add(firstTarget);
				
				////output each html file
				
				String [] fields = null;
				String docFile = null;
				BufferedWriter writer = null;
				//first doc
				fields = firstTarget.split("\\s");
				docFile = outputDir+fields[2]+".txt";				
				writer = IOText.getBufferedWriter_UTF8(docFile);				
				
				for(int k=firstIndex+1; k<lineList.size(); k++){
					String line = lineList.get(k);
					
					if(line.indexOf("Q0")>0 && line.indexOf("clueweb09")>=0 && line.indexOf("indri")>=0){
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
			
			String entireBaselineFile = root+identifier+"_Baseline.txt";
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
	////
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
	/**
	 * generate file for accessing interface for documents
	 * **/
	private static void getOnlineAccessQueryFile(String queryFieldFile, String outputDir, String identifier){
		ArrayList<String> qList = new ArrayList<>();
		try {
			ArrayList<String> lineList = IOText.getLinesAsAList_UTF8(queryFieldFile);
			for(String line: lineList){
				qList.add(line.split(":")[1]);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		System.out.println(qList.size());
						
		try{
			int fileID = 1;
			String fileName;
			XmlWriter writer;
			
			fileName = identifier+"_SQ_"+StandardFormat.serialFormat(fileID, "000")+".txt";	
			writer = IOText.getXmlWriter_UTF8(outputDir+fileName);
			
			writer.startDocument("parameters");			
			writer.writeElement("printDocuments", Boolean.TRUE.toString());			
			
			for(int i=1; i<=qList.size(); i++){
				String topic = qList.get(i-1);
				System.out.println(topic);
				
				if(i%5 == 0){
					writer.startElement("query");					
					writer.writeElement("type", "indri");
					writer.writeElement("number", identifier+StandardFormat.serialFormat(i, "000"));					
					writer.writeElement("text", "#combine("+topic.trim()+")");					
					writer.endElement("query");
					
					writer.endDocument("parameters");
					writer.flush();
					writer.close();
					writer = null;
					//
					fileID++;
					if((i+1) > qList.size()){
						break;
					}else{
						fileName = identifier+"_SQ_"+StandardFormat.serialFormat(fileID, "000")+".txt";	
						writer = IOText.getXmlWriter_UTF8(outputDir+fileName);
						writer.startDocument("parameters");			
						writer.writeElement("printDocuments", Boolean.TRUE.toString());
					}					
				}else{
					writer.startElement("query");					
					writer.writeElement("type", "indri");
					writer.writeElement("number", identifier+StandardFormat.serialFormat(i, "000")); 					
					writer.writeElement("text", "#combine("+topic.trim()+")");					
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
		//Preprocessor.getBaselineDocs_NTCIR11_DR_En();
		
		//3
		/*
		String dir = "C:\\T\\WorkBench\\Bench_Dataset\\DataSet_DiversifiedRanking\\trec\\";
		
		String file_1 = dir+"Div2009\\wt09.topics.queries-only"; 
		String outputDir_1 = dir+"Div2009"+"\\SearchFile\\";		
		Preprocessor.getOnlineAccessQueryFile(file_1, outputDir_1, "Div2009");
		
		
		String file_2 = dir+"Div2010\\wt10.topics.queries-only";
		String outputDir_2 = dir+"Div2010"+"\\SearchFile\\";
		Preprocessor.getOnlineAccessQueryFile(file_2, outputDir_2, "Div2010");
		
		
		String file_3 = dir+"Div2011\\queries.101-150.txt";
		String outputDir_3 = dir+"Div2011"+"\\SearchFile\\";
		Preprocessor.getOnlineAccessQueryFile(file_3, outputDir_3, "Div2011");
		
		String file_4 = dir+"Div2012\\queries.151-200.txt";
		String outputDir_4 = dir+"Div2012"+"\\SearchFile\\";
		Preprocessor.getOnlineAccessQueryFile(file_4, outputDir_4, "Div2012");
		*/
		
		//4
		/*
		String superRoot = "C:/T/WorkBench/Bench_Dataset/DataSet_DiversifiedRanking/trec/";
		
		String identifier_1 = "Div2009";
		String root_1 = superRoot+identifier_1+"/";
		Preprocessor.getBaselineDocs_TREC_Div(root_1, identifier_1);
		
		String identifier_2 = "Div2010";
		String root_2 = superRoot+identifier_2+"/";
		Preprocessor.getBaselineDocs_TREC_Div(root_2, identifier_2);
		
		String identifier_3 = "Div2011";
		String root_3 = superRoot+identifier_3+"/";
		Preprocessor.getBaselineDocs_TREC_Div(root_3, identifier_3);
		
		String identifier_4 = "Div2012";
		String root_4 = superRoot+identifier_4+"/";
		Preprocessor.getBaselineDocs_TREC_Div(root_4, identifier_4);
		*/
	}

}
