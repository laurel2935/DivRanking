package org.archive.ntcir.dr.rank;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.archive.OutputDirectory;
import org.archive.dataset.ntcir.NTCIRLoader;
import org.archive.dataset.ntcir.NTCIRLoader.NTCIR_EVAL_TASK;
import org.archive.dataset.ntcir.sm.SMTopic;
import org.archive.nlp.qpunctuation.QueryPreParser;
import org.archive.nlp.tokenizer.Tokenizer;
import org.archive.util.Language.Lang;

public class DRRunParameter {
	private final static boolean DEBUG = false;
	
	public NTCIR_EVAL_TASK eval;
	protected String runTitle;
	protected String runIntroduction;
	
	protected String outputDir = OutputDirectory.ROOT+"DRResult/";
	
	
	public List<SMTopic> topicList;
	public HashMap<String, String> docMap;
	public HashMap<String, ArrayList<String>> baselineMap;
	public HashMap<String, ArrayList<String>> SegmentedStringMap = null;
	
	public DRRunParameter(NTCIR_EVAL_TASK eval, String runTitle, String runIntroduction){		
		this.eval = eval;
		this.runTitle = runTitle;		
		this.runIntroduction = "<SYSDESC>"+runIntroduction+"</SYSDESC>";		
		
		if(NTCIR_EVAL_TASK.NTCIR11_DR_EN == eval){
			
			this.topicList = NTCIRLoader.loadNTCIR11TopicList(NTCIR_EVAL_TASK.NTCIR11_SM_EN, true);
			this.docMap = NTCIRLoader.loadNTCIR11BaselineDocs_EN(true);
			this.baselineMap = NTCIRLoader.loadNTCIR11Baseline_EN();
			
		}else if(NTCIR_EVAL_TASK.NTCIR11_DR_CH == eval){
			
			this.topicList = NTCIRLoader.loadNTCIR11TopicList(NTCIR_EVAL_TASK.NTCIR11_SM_CH, true);
			this.docMap = NTCIRLoader.loadNTCIR11Docs_CH();
			this.baselineMap = NTCIRLoader.loadNTCIR11Baseline_CH();
			//			
			segment(this.topicList);			
		}else{
			System.err.println("Task Input Error!");
			System.exit(1);
		}
	}
	
	protected HashMap<String, ArrayList<String>> segment(List<SMTopic> smTopicList){		
		SegmentedStringMap = new HashMap<String, ArrayList<String>>();
		//
		for(SMTopic smTopic: smTopicList){	
			//System.out.println(smTopic.toString());
			ArrayList<String> tWords = Tokenizer.adaptiveQuerySegment(Lang.Chinese, smTopic.getTopicText(), null, true, true);			
			if(null != tWords){
				SegmentedStringMap.put(smTopic.getTopicText(), tWords);
			}else{
				new Exception("Segment Topic Error!").printStackTrace();
				continue;
			}	
			
			String reference = Tokenizer.isDirectWord(smTopic.getTopicText(), Lang.Chinese)?smTopic.getTopicText():null;
			for(String str: smTopic.uniqueRelatedQueries){
				
				if(QueryPreParser.isOddQuery(str, Lang.Chinese)){
					continue;
				}
				//String toSegString = new String(str);
				//String toReferString = null;
				//if(null != reference){
				//	toReferString = new String(reference);
				//}
				//String subTKey = smTopic.getID()+"-"+Integer.toString(id);
				ArrayList<String> subTWords = null;
				try {
					//System.out.println(toSegString);
					subTWords = Tokenizer.adaptiveQuerySegment(Lang.Chinese, str, reference, true, true);
				} catch (Exception e) {
					// TODO: handle exception
					System.err.println("segment error!");
					subTWords = null;
				}
					
				if(null != subTWords){
					SegmentedStringMap.put(str, subTWords);					
				}	
				
			}
		}
		if(DEBUG){
			System.out.println("segmented subtopic string:");
			for(Entry<String, ArrayList<String>> entry: SegmentedStringMap.entrySet()){
				System.out.println(entry.getKey()+"\t"+entry.getValue());
								
			}
		}
		
		return SegmentedStringMap;
	}
	
	//
	public static void checkNoDocTopic(){
		String drRunTitle = "";
		String drRunIntroduction = "";
		
		DRRunParameter drRunParameter = new DRRunParameter(NTCIR_EVAL_TASK.NTCIR11_DR_EN, drRunTitle, drRunIntroduction);
		
		int hasCount = 0, noCount = 0;
		for(int t=0; t<drRunParameter.topicList.size(); t++){
			
			SMTopic smTopic = drRunParameter.topicList.get(t);
			ArrayList<String> baseline = drRunParameter.baselineMap.get(smTopic.getID());
			
			if(null == baseline){
				System.out.println(smTopic.getID());
				noCount++;
			}else{
				hasCount++;
			}
		}
		
		System.out.println("noCount"+noCount);
		System.out.println("hasCount"+hasCount);			
	}
	//
	public static void  main(String []args) {
		//1
		//NTCIRLoader.loadNTCIR11TopicList(NTCIR_EVAL_TASK.NTCIR11_SM_CH, true);
		
		//2		
		/*
		String drRunTitle = "TUTA1-D-C-1B";
		String drRunIntroduction = "For the Chinese document ranking subtask, the results of subtopic mining are used as input."
				+ " Corresponding to different kinds of topics, different ranking strategies are adopted.";
		
		DRRunParameter drRunParameter = new DRRunParameter(NTCIR_EVAL_TASK.NTCIR11_DR_CH, drRunTitle, drRunIntroduction);
		*/
		
		//3
		/*
		String drRunTitle = "TUTA1-D-E-1B";
		String drRunIntroduction = "For the English document ranking subtask, the results of subtopic mining are used as input."
				+ " Corresponding to different kinds of topics, different ranking strategies are adopted.";
		
		DRRunParameter drRunParameter = new DRRunParameter(NTCIR_EVAL_TASK.NTCIR11_DR_EN, drRunTitle, drRunIntroduction);
		*/
		
		//4
		//DRRunParameter.checkNoDocTopic();
		
		//5
		HashMap<String, String> docMap = NTCIRLoader.loadNTCIR11Docs_CH();
		
	}

}
