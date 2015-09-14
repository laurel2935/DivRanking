package org.archive.ntcir.dr.rank;

import java.io.BufferedWriter;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import org.archive.OutputDirectory;
import org.archive.a1.ranker.fa.DCKUFLRanker;
import org.archive.a1.ranker.fa.DCKUFLRanker.Strategy;
import org.archive.dataset.ntcir.NTCIRLoader;
import org.archive.dataset.ntcir.NTCIRLoader.NTCIR_EVAL_TASK;
import org.archive.dataset.ntcir.sm.SMTopic;
import org.archive.ml.ufl.DCKUFL.ExemplarType;
import org.archive.nicta.kernel.BM25Kernel_A1;
import org.archive.nicta.kernel.TFIDF_A1;
import org.archive.nicta.ranker.ResultRanker;
import org.archive.nlp.tokenizer.Tokenizer;
import org.archive.ntcir.sm.SMRankedList;
import org.archive.ntcir.sm.SMRankedRecord;
import org.archive.ntcir.sm.SMRunParameter;
import org.archive.ntcir.sm.SMRunParameter.ClusteringFunction;
import org.archive.ntcir.sm.SMRunParameter.SimilarityFunction;
import org.archive.util.Language.Lang;
import org.archive.util.io.IOText;
import org.archive.util.tuple.StrDouble;

public class NTCIR11DRRanker {
	private final static boolean DEBUG = true;
	
	public void run(DRRunParameter drRunParameter, Lang lang) throws Exception{
		//formal system result
		File dirFile = new File(drRunParameter.outputDir);
		if(!dirFile.exists()){
			dirFile.mkdirs();
		}
		String formalRun = drRunParameter.outputDir+drRunParameter.runTitle+".txt";
		BufferedWriter writer = IOText.getBufferedWriter_UTF8(formalRun);		
		writer.write(drRunParameter.runIntroduction);
		writer.newLine();
		//		
		run(drRunParameter, writer, lang);
		
	}
	
	private void run(DRRunParameter drRunParameter, BufferedWriter writer, Lang lang) throws Exception{
		
		HashMap<String, ArrayList<String>> smResult = loadSMResult(drRunParameter, lang);
		
		DCKUFLRanker dckuflRanker = loadRanker(drRunParameter);
		
		for(int t=0; t<drRunParameter.topicList.size(); t++){
			
			SMTopic smTopic = drRunParameter.topicList.get(t);
			ArrayList<String> baseline = drRunParameter.baselineMap.get(smTopic.getID());
			
			if(null == baseline){
				continue;
			}
						
			if(DEBUG){
				System.out.println("Processing topic "+ smTopic.getID()+"\t"+smTopic.getTopicText());
			}
			
			if(smTopic.belongToBadCase()){
				continue;
			}
			
			DRRankedList rankedList = null;
			//
			if(smTopic.CompleteSentence){
				
				rankedList = new DRRankedList();
				
				int size = Math.min(100, baseline.size());
				
				for(int i=0; i<size; i++){
					String [] fields = baseline.get(i).split("\\s");					
					DRRankedRecord record = new DRRankedRecord(smTopic.getID(), fields[2], (i+1), Double.parseDouble(fields[4]), drRunParameter.runTitle);					
					rankedList.addRecord(record);
				}
				
				System.out.println("For clear topic:");
				System.out.println(rankedList.tosString());
				
			}else{
				//1
				ArrayList<String> subtopicList = smResult.get(smTopic.getID());		
				
				if(null == subtopicList){
					System.err.println("Null Subtopic List Case!");
					continue;
				}
				
				//
				ArrayList<String> topnDocList = getTopnDocs(baseline);
				
				//
				dckuflRanker.clearInfoOfTopNDocs();				
				if (DEBUG){
					System.out.println("- Evaluating with " + topnDocList.size() + " docs");
				}
				
				int usedDocNumber = 0;
				
				for (String doc_name : topnDocList) {
					if (!drRunParameter.docMap.containsKey(doc_name) || null==drRunParameter.docMap.get(doc_name))
						System.err.println("ERROR: '" + doc_name + "' not found for '" + smTopic.getTopicText() + "'");
					else {
						dckuflRanker.addATopNDoc(doc_name);		
						usedDocNumber++;
					}
				}				
				// Get the results
				if (DEBUG){
					System.out.println("- Running alg: " + dckuflRanker.getDescription());
				}	
				
				rankedList = new DRRankedList();
				
				ArrayList<StrDouble> resultList = null;
				
				int cutoff = 0;
				if(usedDocNumber > 20){
					cutoff = 20;
				}else{
					cutoff = Math.min(usedDocNumber-2, 20);
				}
				
				
				resultList = dckuflRanker.getResultList(drRunParameter, smTopic, subtopicList, cutoff);
				
				int others = 100 - resultList.size();
				
				HashSet<String> rSet = new HashSet<String>();
				for(int i=0; i<resultList.size(); i++){
					StrDouble r = resultList.get(i);
					DRRankedRecord record = new DRRankedRecord(smTopic.getID(), r.first, (i+1), r.second, drRunParameter.runTitle);					
					rankedList.addRecord(record);
					
					rSet.add(r.first);
				}
				
				int goon = resultList.size()+1;
				int jj = 0;
				for(String line: baseline){
					String [] fields = line.split("\\s");
					if(!rSet.contains(fields[2]) && jj<others){
						DRRankedRecord record = new DRRankedRecord(smTopic.getID(), fields[2], goon++, Double.parseDouble(fields[4]), drRunParameter.runTitle);					
						rankedList.addRecord(record);
						jj++;
					}
				}
			}
			///*
			if(null != rankedList && null!=rankedList.recordList){				
				for(DRRankedRecord record: rankedList.recordList){
					writer.write(record.toString());
					writer.newLine();
				}
			}else{
				System.err.println("Null RankedList Error!");
				continue;
				//new Exception("Null RankedList Error!").printStackTrace();
			}
			//*/
		}
		///*
		writer.flush();
		writer.close();
		//*/
	}
	
	private DCKUFLRanker loadRanker(DRRunParameter drRunParameter){
		//NTCIR results have not consider this parameter 
		ExemplarType  exemplarType = ExemplarType.Y;
		Strategy flStrategy = Strategy.QDSim;
		
		double k1, k3, b;
		k1=1.2d; k3=0.5d; b=0.5d; // achieves the best
		//k1=0.5d; k3=0.5d; b=0.5d; //better than the group of b=1000d;
		//k1=1.2d; k3=0.5d; b=1000d;
		BM25Kernel_A1 bm25_A1_Kernel = new BM25Kernel_A1(drRunParameter.docMap, k1, k3, b);
		
		//1
		double lambda_1 = 0.5;
		int iterationTimes_1 = 5000;
		int noChangeIterSpan_1 = 10; 
		//DCKUFLRanker dckuflRanker = new DCKUFLRanker(trecDivDocs, bm25_A1_Kernel, lambda_1, iterationTimes_1, noChangeIterSpan_1);
		DCKUFLRanker dckuflRanker = new DCKUFLRanker(drRunParameter.docMap, bm25_A1_Kernel, lambda_1, iterationTimes_1, noChangeIterSpan_1, exemplarType, flStrategy);
		
		return dckuflRanker;
	}
	
	private ArrayList<String> getTopnDocs(ArrayList<String> baseline){
		ArrayList<String> topnDocList = new ArrayList<String>();
		int size = Math.min(100, baseline.size()); 
		
		for(int i=0; i<size; i++){
			String [] fields = baseline.get(i).split("\\s");
			try {
				topnDocList.add(fields[2]);
			} catch (Exception e) {
				System.out.println(baseline.get(i));
				System.err.println("fields:\t");
				for(int k=0; k<fields.length; k++){
					System.out.print(fields[k]+"|");
				}
				System.out.println();
				e.printStackTrace();
				// TODO: handle exception
			}
			
		}
		
		return topnDocList;
	}
	
	private static HashMap<String, ArrayList<String>> loadSMResult(DRRunParameter drRunParameter, Lang lang){
		String dir = OutputDirectory.ROOT+"SMResult/";
		String file = null;
		if(Lang.Chinese == lang){
			file = dir+"TUTA1-S-C-1A.txt";
		}else{
			file = dir+"TUTA1-S-E-1A.txt";
		}
		
		ArrayList<String> lineList = IOText.getLinesAsAList_UTF8(file);
		lineList.remove(0);
		
		HashMap<String, ArrayList<String>> lineMap = new HashMap<String, ArrayList<String>>();
		for(String line: lineList){
			String [] fields = line.split(";");
			if(lineMap.containsKey(fields[0])){
				lineMap.get(fields[0]).add(line);
			}else{
				ArrayList<String> group = new ArrayList<String>();
				group.add(line);
				lineMap.put(fields[0], group);
			}
		}
		
		if(DEBUG){
			for(Entry<String, ArrayList<String>> entry: lineMap.entrySet()){
				System.out.println(entry.getKey());
				for(String line: entry.getValue()){
					System.out.println("\t"+line);
				}				
			}
		}
		
		HashMap<String, ArrayList<String>> subtopicMap = new HashMap<String, ArrayList<String>>();
		for(Entry<String, ArrayList<String>> entry: lineMap.entrySet()){
			ArrayList<String> subtopicList = getSubtopicList(drRunParameter, entry.getValue(), lang);
			subtopicMap.put(entry.getKey(), subtopicList);
		}
		
		if(DEBUG){			
			for(Entry<String, ArrayList<String>> entry: subtopicMap.entrySet()){
				System.out.println(entry.getKey());
				for(String line: entry.getValue()){
					System.out.println("\t"+line);
				}				
			}
		}
		
		return subtopicMap;
	}
	
	private static ArrayList<String> getSubtopicList(DRRunParameter drRunParameter, ArrayList<String> rankedList, Lang lang){
		HashMap<String, ArrayList<String>> substrMap = new HashMap<String, ArrayList<String>>();
		for(String record: rankedList){
			String [] fields = record.split(";");
			if(substrMap.containsKey(fields[2])){
				substrMap.get(fields[2]).add(fields[6]);
			}else{
				ArrayList<String> substrGroup = new ArrayList<String>();
				substrGroup.add(fields[6]);
				substrMap.put(fields[2], substrGroup);
			}
		}
		
		ArrayList<String> subtopicList = new ArrayList<String>();
		for(Entry<String, ArrayList<String>> entry: substrMap.entrySet()){
			ArrayList<String> group = entry.getValue();
			HashSet<String> termSet = new HashSet<String>();
			for(String substr: group){
				ArrayList<String> termList = null;
				if(Lang.Chinese == lang){
					if(drRunParameter.SegmentedStringMap.containsKey(substr)){
						termList = drRunParameter.SegmentedStringMap.get(substr);
					}										
				}else{
					termList = Tokenizer.segment(substr, lang);
				}
				if(null != termList){
					for(String term: termList){
						if(!termSet.contains(term)){
							termSet.add(term);
						}
					}
				}								
			}
			subtopicList.add(convert(termSet.toArray(new String [0])));			
		}
		
		return subtopicList;		
	}
			
	private static String convert(String [] tList){
		StringBuffer buffer = new StringBuffer();
		for(String t: tList){
			buffer.append(t);
			buffer.append(" ");
		}
		return buffer.toString().trim();
	}
	
	
	
	
	
	
	
	
	
	//
	public static void main(String []args){
		NTCIR11DRRanker drRanker = new NTCIR11DRRanker();
		
		////////////////////
		//Document Ranking for Ch-Topics
		////////////////////		
		/*
		String drRunTitle = "TUTA1-D-C-1B";
		String drRunIntroduction = "For the Chinese document ranking subtask, the results of subtopic mining are used as input."
				+ " Corresponding to different kinds of topics, different ranking strategies are adopted.";
		
		DRRunParameter drRunParameter = new DRRunParameter(NTCIR_EVAL_TASK.NTCIR11_DR_CH, drRunTitle, drRunIntroduction);
		try {
			drRanker.run(drRunParameter, Lang.Chinese);
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		*/
		/////////////////////////
		//Document Ranking for En-Topics
		/////////////////////////
		///*
		String drRunTitle = "TUTA1-D-E-1B";
		String drRunIntroduction = "For the English document ranking subtask, the results of subtopic mining are used as input."
				+ "Corresponding to different kinds of topics, different ranking strategies are adopted.";
		
		DRRunParameter drRunParameter = new DRRunParameter(NTCIR_EVAL_TASK.NTCIR11_DR_EN, drRunTitle, drRunIntroduction);
		try {
			drRanker.run(drRunParameter, Lang.English);
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		//*/
	}
	
	

}
