package org.archive.ntcir.sm;

import java.io.BufferedReader;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.archive.OutputDirectory;
import org.archive.dataset.ntcir.NTCIRLoader;
import org.archive.dataset.ntcir.NTCIRLoader.NTCIR_EVAL_TASK;
import org.archive.dataset.ntcir.sm.SMTopic;
import org.archive.nlp.chunk.lpt.ltpService.LTML;
import org.archive.nlp.qpunctuation.QueryPreParser;
import org.archive.nlp.tokenizer.Tokenizer;
import org.archive.ntcir.sm.preprocess.TopicParser;
import org.archive.ntcir.sm.preprocess.TopicParser.StringType;
import org.archive.util.Language.Lang;
import org.archive.util.io.IOText;

public class SMRunParameter {
	//////////////
	//set up parameters
	//////////////
	
	public static enum SimilarityFunction{StandardTermEditDistance, SemanticTermEditDistance, GregorEditDistance, AveragedSemanticSimilarity}
	
	public static enum ClusteringFunction{StandardAP, K_UFL}
	
	NTCIR_EVAL_TASK eval;
	protected String runTitle;
	protected String runIntroduction;
	
	protected String outputDir = OutputDirectory.ROOT+"SMResult/";
	
	protected SimilarityFunction simFunction;
	
	protected ClusteringFunction cFunction;
	
	public List<SMTopic> topicList;
	//(topicID+SubtopicStrID) -> subtopic string, for ch and en
	protected HashMap<String, String> subtopicStrMap = null;
	
	//////////////////
	//Specific objects
	//////////////////
	//for Ch topics and subtopic strings
	protected HashMap<String, LTML> ltmlMap = null;
	
	protected HashMap<String, ArrayList<String>> SegmentedStringMap = null;
	
	
	
	public SMRunParameter(NTCIR_EVAL_TASK eval, String runTitle, String runIntroduction, SimilarityFunction simFunction, ClusteringFunction cFunction){		
		this.eval = eval;
		this.runTitle = runTitle;		
		this.runIntroduction = "<SYSDESC>"+runIntroduction+"</SYSDESC>";
		this.simFunction = simFunction;
		this.cFunction = cFunction;
		
		if(NTCIR_EVAL_TASK.NTCIR11_SM_EN == eval){
			this.topicList = NTCIRLoader.loadNTCIR11TopicList(eval, true);
		}else{
			this.topicList = NTCIRLoader.loadNTCIR11TopicList(eval, true);
			//
			//segment(this.topicList);
		}	
	}
	
	//
	protected void segment(List<SMTopic> smTopicList){
		SegmentedStringMap = new HashMap<String, ArrayList<String>>();
		//
		for(SMTopic smTopic: smTopicList){			
			ArrayList<String> tWords = Tokenizer.adaptiveQuerySegment(Lang.Chinese, smTopic.getTopicText(), null, true, true);			
			if(null != tWords){
				SegmentedStringMap.put(smTopic.getID(), tWords);
			}else{
				new Exception("Segment Topic Error!").printStackTrace();
				continue;
			}			
			
			int id = 1;
			String reference = Tokenizer.isDirectWord(smTopic.getTopicText(), Lang.Chinese)?smTopic.getTopicText():null;
			for(String str: smTopic.uniqueRelatedQueries){
				if(!QueryPreParser.isOddQuery(str, Lang.Chinese)){
					continue;
				}
				String subTKey = smTopic.getID()+"-"+Integer.toString(id);
				ArrayList<String> subTWords = Tokenizer.adaptiveQuerySegment(Lang.Chinese, str, reference, true, true);	
				if(null != subTWords){
					SegmentedStringMap.put(subTKey, subTWords);					
				}		
				//
				id++;
			}
		}
	}
	//
	public HashMap<String, LTML> loadLTMLForChTopics(List<SMTopic> smTopicList){
		String tDir = OutputDirectory.ROOT+"ntcir-11/SM/ParsedTopic/PerFile/";
		String subTDir = OutputDirectory.ROOT+"ntcir-11/SM/SubtopicString/ParsedWithLTP/";
		
		ltmlMap = new HashMap<String, LTML>();
		//
		for(SMTopic smTopic: smTopicList){
			String tXMLFile = tDir+smTopic.getID()+".xml";
			LTML tLTML = loadLTML(tXMLFile);
			if(null != tLTML){
				ltmlMap.put(smTopic.getID(), tLTML);
			}else{
				continue;
			}			
			
			int id = 1;
			for(String str: smTopic.uniqueRelatedQueries){
				if(!QueryPreParser.isOddQuery(str, Lang.Chinese)){
					continue;
				}
				String subTXMLFile = subTDir+smTopic.getID()+"-"+Integer.toString(id)+".xml";
				File xmlFile = new File(subTXMLFile);
				if(xmlFile.exists()){
					LTML subTLTML = loadLTML(subTXMLFile);
					if(null != subTLTML){
						ltmlMap.put(smTopic.getID()+"-"+Integer.toString(id), subTLTML);
					}
				}		
				//
				id++;
			}
		}
		
		return ltmlMap;
	}
		
	public static LTML loadLTML(String xmlFile){		
		try {
			LTML ltml = new LTML();
			//System.out.println(xmlFile);
			ltml.build(loadAFile(xmlFile));
			return ltml;
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}		
		return null;		
	}
	private static String loadAFile(String targetFile){
		try {
			StringBuffer buffer = new StringBuffer();
			//
			BufferedReader reader = IOText.getBufferedReader_UTF8(targetFile);
			String line = null;			
			while(null != (line=reader.readLine())){
				if(line.length() > 0){					
					buffer.append(line);
					buffer.append("\n");
				}				
			}
			reader.close();
			return buffer.toString();			
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		return null;
	}
	
	//check clear topics
	public static void checkClearTopic(NTCIR_EVAL_TASK task){
		String runTitle = "";
		String runIntroduction = "";
		SMRunParameter runParameter = new SMRunParameter(task, runTitle, runIntroduction, SimilarityFunction.AveragedSemanticSimilarity, ClusteringFunction.StandardAP);
		
		int clearCount = 0, polyCount = 0, amCount = 0;
		ArrayList<String> clearList = new ArrayList<String>();
		ArrayList<String> polyList = new ArrayList<String>();
		ArrayList<String> amList = new ArrayList<String>();
		
		for(int t=0; t<runParameter.topicList.size(); t++){
			
			SMTopic smTopic = runParameter.topicList.get(t);								
			
			if(null != smTopic.polysemyList){
				
				polyList.add(smTopic.getID());
				polyCount++;
				
			}else if(smTopic.CompleteSentence){
				
				clearList.add(smTopic.getID());
				clearCount++;
				
			}else{
				
				amList.add(smTopic.getID());
				amCount++;				
			}
		}
		
		System.out.println("Clear:\t"+clearCount+"\t"+clearList);
		System.out.println("Poly:\t"+polyCount+"\t"+polyList);
		System.out.println("Am:\t"+amCount+"\t"+amList);		
	}
	
	//
	public static void main(String []args){
		//1
		/*
		String file = "E:/v-haiyu/CodeBench/Pool_Output/Output_DiversifiedRanking/ntcir-11/SM/ParsedTopic/PerFile/0001.xml";
		TopicParser.loadParsedObject(file);
		RunParameter.loadLTML(file);
		*/
		
		//2
		//(1)
		//SMRunParameter.checkClearTopic(NTCIR_EVAL_TASK.NTCIR11_SM_EN);
		//(2)
		SMRunParameter.checkClearTopic(NTCIR_EVAL_TASK.NTCIR11_SM_CH);
	}

}
