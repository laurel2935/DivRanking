package org.archive.a1.analysis;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.TimeZone;
import java.util.Vector;
import java.util.Map.Entry;

import org.archive.OutputDirectory;
import org.archive.dataset.trec.TRECDivLoader;
import org.archive.dataset.trec.TRECDivLoader.DivVersion;
import org.archive.dataset.trec.query.TRECDivQuery;
import org.archive.nicta.evaluation.evaluator.Evaluator;
import org.archive.nicta.evaluation.metricfunction.NDEvalLosses;
import org.archive.util.io.IOText;
import org.archive.util.tuple.IntStrInt;
import org.archive.util.tuple.StrDouble;

public class ResultAnalyzer {
	private final static boolean DEBUG = true;
	
	public static void getTopicDistributionOfLambda(boolean commonIndri, DivVersion divVersion, String resultFile){
		
		HashMap<String, StrDouble> topicLambdaMap = getMaxLambdaSetting(resultFile);
		
		HashMap<String, HashSet<String>> lambdaTopicMap = new HashMap<String, HashSet<String>>();
		
		for(Entry<String, StrDouble> entry: topicLambdaMap.entrySet()){
			String topicID = entry.getKey();
			String lambdaStr = getLambdaStr(entry.getValue().first);
			
			if(lambdaTopicMap.containsKey(lambdaStr)){
				lambdaTopicMap.get(lambdaStr).add(topicID);
			}else{
				HashSet<String> topicSet = new HashSet<String>();
				topicSet.add(topicID);
				lambdaTopicMap.put(lambdaStr, topicSet);
			}
		}
		
		Map<String,TRECDivQuery> trecDivQueries = TRECDivLoader.loadTrecDivQueries(commonIndri, divVersion);	
		
		ArrayList<IntStrInt> list = new ArrayList<IntStrInt>();
		
		for(Entry<String, HashSet<String>> entry: lambdaTopicMap.entrySet()){
			String lambdaStr = entry.getKey();
			HashSet<String> topicSet = entry.getValue();
			
			int facetedCount = 0;
			int amCount = 0;
			
			for(String topic: topicSet){
				if(trecDivQueries.get(topic)._type.equals("faceted")){
					facetedCount++;
				}else{
					amCount++;
				}
			}
			
			list.add(new IntStrInt(facetedCount, lambdaStr, amCount));
		}
		
		Collections.sort(list);
		
		System.out.println();
		System.out.println(divVersion.toString());
		for(IntStrInt element: list){
			System.out.println(element.second+"\t"+"faceted: "+element.first+"\t"+"ambiguous: "+element.third+"\tTotal:"+(element.first+element.third));
		}
	}
	private static String getLambdaStr(String lambdaStr){
		return lambdaStr.substring(lambdaStr.indexOf("[")+1, lambdaStr.indexOf("]"));		
	}
	
	
	////topic-id -> [lambdaString & alphaNDCG@20]
	public static HashMap<String, StrDouble> getMaxLambdaSetting(String resultFile){
		
		HashMap<String, StrDouble> topicLambdaMap = new HashMap<String, StrDouble>();
		
		ArrayList<String> lineList = IOText.getLinesAsAList_UTF8(resultFile);
		for(String line: lineList){
			line = line.replaceAll("[\\s]+", "\t");
			String [] fields = line.split("\\s");
			
			/*
			System.out.println(fields.length);
			for(int k=0; k<fields.length; k++){
				System.out.println(fields[k]);
			}
			System.out.println();
			*/
			
			String topicID = fields[0];
			String lambdaStr = fields[1];
			String alphaNDCG20Str = fields[14];
			
			//System.out.println(topicID);
			//System.out.println(lambdaStr);
			//System.out.println(alphaNDCG20Str);
			
			Double currV = getDouble(alphaNDCG20Str.trim());
			
			if(topicLambdaMap.containsKey(topicID)){				
				if(currV > topicLambdaMap.get(topicID).second){
					topicLambdaMap.put(topicID, new StrDouble(lambdaStr, currV));
				}
			}else{
				topicLambdaMap.put(topicID, new StrDouble(lambdaStr, currV));
			}			
		}
		
		if(DEBUG){
			for(Entry<String, StrDouble> entry: topicLambdaMap.entrySet()){
				String topicID = entry.getKey();
				StrDouble strD = entry.getValue();

				System.out.println(topicID+":\t"+strD.first+"\t"+strD.second);
			}
		}
		
		return topicLambdaMap;
	}
	private static Double getDouble(String alphaNDCG20Str){
		String targetStr = alphaNDCG20Str.substring(alphaNDCG20Str.indexOf(":")+1).trim();
		//System.out.println(targetStr);
		return Double.valueOf(targetStr);			
	}
	
	
	
	////ideal result with an adaptive lambda
	public static void getIdealResultsOfLambda(String resultFile){
		
		HashMap<String, StrDouble> topicLambdaMap = new HashMap<String, StrDouble>();
		
		ArrayList<String> lineList = IOText.getLinesAsAList_UTF8(resultFile);
		for(String line: lineList){
			line = line.replaceAll("[\\s]+", "\t");
			String [] fields = line.split("\\s");
			
			/*
			System.out.println(fields.length);
			for(int k=0; k<fields.length; k++){
				System.out.println(fields[k]);
			}
			System.out.println();
			*/
			
			String topicID = fields[0];			
			String alphaNDCG20Str = fields[14];
			
			//System.out.println(topicID);
			//System.out.println(lambdaStr);
			//System.out.println(alphaNDCG20Str);
			
			Double currV = getDouble(alphaNDCG20Str.trim());
			
			if(topicLambdaMap.containsKey(topicID)){				
				if(currV > topicLambdaMap.get(topicID).second){
					topicLambdaMap.put(topicID, new StrDouble(line, currV));
				}
			}else{
				topicLambdaMap.put(topicID, new StrDouble(line, currV));
			}			
		}
		
		if(DEBUG){
			for(Entry<String, StrDouble> entry: topicLambdaMap.entrySet()){
				String topicID = entry.getKey();
				StrDouble strD = entry.getValue();

				System.out.println(topicID+"->"+strD.first+"\t"+strD.second);
			}
			System.out.println();
		}
		
		ArrayList<String> idealPerResultList = new ArrayList<String>();
		
		for(Entry<String, StrDouble> entry: topicLambdaMap.entrySet()){
			//String topicID = entry.getKey();
			StrDouble strD = entry.getValue();
			idealPerResultList.add(strD.first);			
		}
		
		double [] sumArray = new double [21];
		for(int i=0; i<sumArray.length; i++){
			sumArray[i] = 0.0d;
		}
		
		for(String resultLine: idealPerResultList){
			String [] fields = resultLine.split("\t");
			for(int i=3; i<fields.length; i++){
				sumArray[i-3] += getDouble(fields[i]);
			}
		}
		
		for(int i=0; i<sumArray.length; i++){
			sumArray[i] = sumArray[i]/idealPerResultList.size();
		}
		
		StringBuffer buffer = new StringBuffer();
		for(int i=0; i<sumArray.length; i++){
			buffer.append(NDEvalLosses.metricVector.get(i)+":");
			buffer.append(Evaluator.fourResultFormat.format(sumArray[i])+"\t");
		}
		
		String resultString = buffer.toString();
		resultString = resultString.replaceAll("\n", "");
		
		System.out.println(resultString);		
	}
	
	
	
	//time
	public static void logTime(){
		SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss:SS");  
        TimeZone timeZone = timeFormat.getTimeZone();  
        timeZone.setRawOffset(0);  
        timeFormat.setTimeZone(timeZone); 
        
        String test = "00:01:32:760";
        try {
        	 Date date = timeFormat.parse(test);
        	 System.out.println(date.getTime());
        	 
		} catch (Exception e) {
			// TODO: handle exception
		}
       
	}
	
	//////////////////////
	//WilcoxonSignedRankTest
	///////////////////////
	
	//
	public static DivResult loadDivResult(String resultFile){
		ArrayList<String> lineList = IOText.getLinesAsAList_UTF8(resultFile);
		
		DivResult divResult = new DivResult();
		
		for(String line: lineList){
			line = line.replaceAll("[\\s]+", "\t");
			String [] fields = line.split("\\s");
			
			double anDCG5 = getDouble(fields[12].trim());
			double anDCG10 = getDouble(fields[13].trim());
			double anDCG20 = getDouble(fields[14].trim());
			
			double nERRIA5 = getDouble(fields[6].trim());
			double nERRIA10 = getDouble(fields[7].trim());
			double nERRIA20 = getDouble(fields[8].trim());
			
			double strec10 = getDouble(fields[22].trim());
			
			divResult.addAlphanDCG5(anDCG5);
			divResult.addAlphanDCG10(anDCG10);
			divResult.addAlphanDCG20(anDCG20);
			
			divResult.addnERRIA5(nERRIA5);
			divResult.addnERRIA10(nERRIA10);
			divResult.addnERRIA20(nERRIA20);
			
			divResult.addStrec10(strec10);
		}
		
		if(DEBUG){
			System.out.println(divResult.toString());
		}
		
		return divResult;
	}
	//
	private static double [] getDArray(Vector<Double> dVector){
		double [] dArray = new double[dVector.size()];
		
		for(int i=0; i<dVector.size(); i++){
			dArray[i] = dVector.get(i);
		}
		
		return dArray;
	}
	//
	public static void WilcoxonSignedRankTest(DivResult aDivResult, DivResult bDivResult){
		org.apache.commons.math3.stat.inference.WilcoxonSignedRankTest wsrTest = new org.apache.commons.math3.stat.inference.WilcoxonSignedRankTest();
		
		//
		System.out.println("anDCG@5-test\t\t"+wsrTest.wilcoxonSignedRankTest(getDArray(aDivResult.alphanDCG5), getDArray(bDivResult.alphanDCG5), false));
		//System.out.println();
		System.out.println("anDCG@10-test\t\t"+wsrTest.wilcoxonSignedRankTest(getDArray(aDivResult.alphanDCG10), getDArray(bDivResult.alphanDCG10), false));
		//System.out.println();
		System.out.println("anDCG@20-test\t\t"+wsrTest.wilcoxonSignedRankTest(getDArray(aDivResult.alphanDCG20), getDArray(bDivResult.alphanDCG20), false));
		//System.out.println();
		System.out.println();
		
		System.out.println("nERRIA@5-test\t\t"+wsrTest.wilcoxonSignedRankTest(getDArray(aDivResult.nERRIA5), getDArray(bDivResult.nERRIA5), false));
		//System.out.println();
		System.out.println("nERRIA@10-test\t\t"+wsrTest.wilcoxonSignedRankTest(getDArray(aDivResult.nERRIA10), getDArray(bDivResult.nERRIA10), false));
		//System.out.println();
		System.out.println("nERRIA@20-test\t\t"+wsrTest.wilcoxonSignedRankTest(getDArray(aDivResult.nERRIA20), getDArray(bDivResult.nERRIA20), false));
		//System.out.println();
		System.out.println();
		
		System.out.println("strec@10-test\t\t"+wsrTest.wilcoxonSignedRankTest(getDArray(aDivResult.strec10), getDArray(bDivResult.strec10), false));
		System.out.println();		
	}
	//
	public static void statisticalSignificanceTest(){
		//baseline results
		String baselineDir = OutputDirectory.ROOT+"DivEvaluation/NoDescription_Evaluation/Baseline/";
		String div2009_Baseline = "NoDescription_Div2009BFS_BM25Baseline_ndeval.txt";
		String div2010_Baseline = "NoDescription_Div2010BFS_BM25Baseline_ndeval.txt";
		
		DivResult r2009_Baseline = loadDivResult(baselineDir+div2009_Baseline);
		DivResult r2010_Baseline = loadDivResult(baselineDir+div2010_Baseline);	
		
		String singleLambdaDir = OutputDirectory.ROOT+"DivEvaluation/NoDescription_Evaluation/SingleLambdaEvaluation/";
		
		//mmr
		DivResult r2009_mmr = loadDivResult(singleLambdaDir+"Div2009BFS_BM25Kernel_A1+TFIDF_A1_SingleLambda_ndeval.txt");
		DivResult r2010_mmr = loadDivResult(singleLambdaDir+"Div2010BFS_BM25Kernel_A1+TFIDF_A1_SingleLambda_ndeval.txt");
		
		//dfp
		DivResult r2009_dfp = loadDivResult(singleLambdaDir+"Div2009MDP_MDP_SingleLambda_ndeval.txt");
		DivResult r2010_dfp = loadDivResult(singleLambdaDir+"Div2010MDP_MDP_SingleLambda_ndeval.txt");
		
		//1-call@k
		DivResult r2009_1callk = loadDivResult(singleLambdaDir+"Div2009BFS_PLSR_ndeval.txt");
		DivResult r2010_1callk = loadDivResult(singleLambdaDir+"Div2010BFS_PLSR_ndeval.txt");
		
		//0-1 mskp
		String mskpDir = OutputDirectory.ROOT+"DivEvaluation/NoDescription_Evaluation/01MSKP/";
		DivResult r2009_mskp = loadDivResult(mskpDir+"Div2009FL_Y_Belief_ndeval.txt");
		DivResult r2010_mskp = loadDivResult(mskpDir+"Div2010FL_Y_Belief_ndeval.txt");
		
		//mmr to bm25 
		/*
		System.out.println("2009 mmr->bm25");
		WilcoxonSignedRankTest(r2009_Baseline, r2009_mmr);
		System.out.println("2010 mmr->bm25");
		WilcoxonSignedRankTest(r2010_Baseline, r2010_mmr);
		System.out.println();
		*/
		
		//dfp to bm25
		/*
		System.out.println("2009 dfp->bm25");
		WilcoxonSignedRankTest(r2009_Baseline, r2009_dfp);
		System.out.println("2010 dfp->bm25");
		WilcoxonSignedRankTest(r2010_Baseline, r2010_dfp);
		System.out.println();
		*/
		
		//1callk to bm25
		/*
		System.out.println("2009 1callk->bm25");
		WilcoxonSignedRankTest(r2009_Baseline, r2009_1callk);
		System.out.println("2010 1callk->bm25");
		WilcoxonSignedRankTest(r2010_Baseline, r2010_1callk);
		System.out.println();
		*/
		
		//mskp to bm25
		/*
		System.out.println("[2009 mskp->bm25]");
		WilcoxonSignedRankTest(r2009_Baseline, r2009_mskp);
		System.out.println("[2010 mskp->bm25]");
		WilcoxonSignedRankTest(r2010_Baseline, r2010_mskp);
		System.out.println();
		*/
		
		//dfp to mmr
		/*
		System.out.println("[2009 dfp->mmr]");
		WilcoxonSignedRankTest(r2009_mmr, r2009_dfp);
		System.out.println("[2010 dfp->mmr]");
		WilcoxonSignedRankTest(r2010_mmr, r2010_dfp);
		System.out.println();
		*/
		
		//1callk to mmr
		/*
		System.out.println("[2009 1callk->mmr]");
		WilcoxonSignedRankTest(r2009_mmr, r2009_1callk);
		System.out.println("[2010 1callk->mmr]");
		WilcoxonSignedRankTest(r2010_mmr, r2010_1callk);
		System.out.println();
		*/
		
		//mskp to mmr
		/*
		System.out.println("[2009 mskp->mmr]");
		WilcoxonSignedRankTest(r2009_mmr, r2009_mskp);
		System.out.println("[2010 mskp->mmr]");
		WilcoxonSignedRankTest(r2010_mmr, r2010_mskp);
		System.out.println();
		*/
		
		//1callk to dfp
		/*
		System.out.println("[2009 1callk->dfp]");
		WilcoxonSignedRankTest(r2009_dfp, r2009_1callk);
		System.out.println("[2010 1callk->dfp]");
		WilcoxonSignedRankTest(r2010_dfp, r2010_1callk);
		System.out.println();
		*/
		
		//mskp to dfp
		/*
		System.out.println("[2009 mskp->dfp]");
		WilcoxonSignedRankTest(r2009_dfp, r2009_mskp);
		System.out.println("[2010 mskp->dfp]");
		WilcoxonSignedRankTest(r2010_dfp, r2010_mskp);
		System.out.println();
		*/
		
		//mskp to 1callk
		/*
		System.out.println("[2009 mskp->1callk]");
		WilcoxonSignedRankTest(r2009_1callk, r2009_mskp);
		System.out.println("[2010 mskp->1callk]");
		WilcoxonSignedRankTest(r2010_1callk, r2010_mskp);
		System.out.println();
		*/
		
		
		
	}
	//
	public static void main(String []args){
		////////////////////////////
		//1
		////////////////////////////
		//1 
		String perLambdaResultdir = OutputDirectory.ROOT+"DivEvaluation/PerLambdaEvaluation/";
		
		/**DivVersion.Div2009 BM25Kernel_A1+TFIDF_A1**/
		//(1)
		//Using description
		//String Div2009File = "BM25Kernel_A1+TFIDF_A1-Div2009BFS_PerLambda_ndeval.txt";			
		//No description
		//String Div2009File = "Div2009BFS_BM25Kernel_A1+TFIDF_A1_PerLambda_ndeval.txt";
		
		//ResultAnalyzer.getTopicDistributionOfLambda(DivVersion.Div2009, perLambdaResultdir+Div2009File);
		
		//(2)
		/**DivVersion.Div2010 BM25Kernel_A1+TFIDF_A1**/
		//Using description
		//String Div2010File = "BM25Kernel_A1+TFIDF_A1-Div2010BFS_PerLambda_ndeval.txt";				
		//No description
		//String Div2010File = "Div2010BFS_BM25Kernel_A1+TFIDF_A1_PerLambda_ndeval.txt";
		
		//ResultAnalyzer.getTopicDistributionOfLambda(DivVersion.Div2010, perLambdaResultdir+Div2010File);
		
		/**DivVersion.Div2009 MDP-TFIDF_A1**/
		//(1)
		//Using description
		//String Div2009File_mdp = "MDP-TFIDF_A1-Div2009MDP_PerLambda_ndeval.txt";	
		//No description
		//String Div2009File_mdp = "Div2009MDP_MDP_PerLambda_ndeval.txt";	
		//ResultAnalyzer.getTopicDistributionOfLambda(DivVersion.Div2009, perLambdaResultdir+Div2009File_mdp);
		
		//(2)
		/**DivVersion.Div2010 MDP-TFIDF_A1**/
		//Using description
		//String Div2010File_mdp = "MDP-TFIDF_A1-Div2010MDP_PerLambda_ndeval.txt";		
		//No description
		//String Div2010File_mdp = "Div2010MDP_MDP_PerLambda_ndeval.txt";
		//ResultAnalyzer.getTopicDistributionOfLambda(DivVersion.Div2010, perLambdaResultdir+Div2010File_mdp);
		
		//2 ideal results
		//DivVersion.Div2009 BM25Kernel_A1+TFIDF_A1-Div2009BFS_PerLambda_ndeval.txt
		//String Div2009File = "BM25Kernel_A1+TFIDF_A1-Div2009BFS_PerLambda_ndeval.txt";				
		//ResultAnalyzer.getIdealResultsOfLambda(perLambdaResultdir+Div2009File);
		
		//2
		
		//ResultAnalyzer.logTime();
		
		
		////////////////////////////
		//2
		////////////////////////////
		ResultAnalyzer.statisticalSignificanceTest();
		
	}

}
