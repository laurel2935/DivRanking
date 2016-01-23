package org.archive.a1.analysis;

import java.io.BufferedWriter;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
	private final static boolean DEBUG = false;
	
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
			
			double nERRIA5 = getDouble(fields[6].trim());
			double nERRIA10 = getDouble(fields[7].trim());
			double nERRIA20 = getDouble(fields[8].trim());
			
			double anDCG5 = getDouble(fields[12].trim());
			double anDCG10 = getDouble(fields[13].trim());
			double anDCG20 = getDouble(fields[14].trim());			
			
			double pIA10 = getDouble(fields[19].trim());
			
			double strec10 = getDouble(fields[22].trim());
			
			divResult.addAlphanDCG5(anDCG5);
			divResult.addAlphanDCG10(anDCG10);
			divResult.addAlphanDCG20(anDCG20);
			
			divResult.addnERRIA5(nERRIA5);
			divResult.addnERRIA10(nERRIA10);
			divResult.addnERRIA20(nERRIA20);
			
			divResult.addPIA10(pIA10);
			
			divResult.addStrec10(strec10);
		}
		
		if(DEBUG){
			System.out.println(divResult.toString());
		}
		
		return divResult;
	}
	//
	public static DivResult loadAvgDivResult(String resultFile){
		ArrayList<String> lineList = IOText.getLinesAsAList_UTF8(resultFile);
		
		DivResult divResult = new DivResult();
		
		String line = lineList.get(1).trim(); 


		line = line.replaceAll("[\\s]+", "\t");
		String [] fields = line.split("\\s");
		
		double nERRIA5 = getDouble(fields[3].trim());
		double nERRIA10 = getDouble(fields[4].trim());
		double nERRIA20 = getDouble(fields[5].trim());
		
		double anDCG5 = getDouble(fields[9].trim());
		double anDCG10 = getDouble(fields[10].trim());
		double anDCG20 = getDouble(fields[11].trim());	
		
		double nNRBP20 = getDouble(fields[13].trim());
		
		double pIA10 = getDouble(fields[16].trim());
		
		double strec10 = getDouble(fields[19].trim());
		
		divResult.addAlphanDCG5(anDCG5);
		divResult.addAlphanDCG10(anDCG10);
		divResult.addAlphanDCG20(anDCG20);
		
		divResult.addnERRIA5(nERRIA5);
		divResult.addnERRIA10(nERRIA10);
		divResult.addnERRIA20(nERRIA20);
		
		divResult.addnNRBP20(nNRBP20);
		
		divResult.addPIA10(pIA10);
		
		divResult.addStrec10(strec10);
		
		if(DEBUG){
			System.out.println(divResult.toString());
		}
		
		return divResult;
	}
	//w.r.t. lambda
	public static DivResult loadGroupAvgDivResult(String dir, String part1, String part2){
		DivResult groupAvgDivResult = new DivResult();
		
		double span = 0.1;
		for(double lam = 0.0; lam<=1.0; lam+= span){
			String lamStr = Evaluator.oneResultFormat.format(lam);
			
			String file = dir+part1 + lamStr + part2;
			
			//--
			ArrayList<String> lineList = IOText.getLinesAsAList_UTF8(file);		
			String line = lineList.get(1).trim(); 


			line = line.replaceAll("[\\s]+", "\t");
			String [] fields = line.split("\\s");
			
			double nERRIA5 = getDouble(fields[3].trim());
			double nERRIA10 = getDouble(fields[4].trim());
			double nERRIA20 = getDouble(fields[5].trim());
			
			double anDCG5 = getDouble(fields[9].trim());
			double anDCG10 = getDouble(fields[10].trim());
			double anDCG20 = getDouble(fields[11].trim());	
			
			double nNRBP20 = getDouble(fields[13].trim());
			
			double pIA10 = getDouble(fields[16].trim());
			
			double strec10 = getDouble(fields[19].trim());
			
			//
			
			groupAvgDivResult.addAlphanDCG5(anDCG5);
			groupAvgDivResult.addAlphanDCG10(anDCG10);
			groupAvgDivResult.addAlphanDCG20(anDCG20);
			
			groupAvgDivResult.addnERRIA5(nERRIA5);
			groupAvgDivResult.addnERRIA10(nERRIA10);
			groupAvgDivResult.addnERRIA20(nERRIA20);
			
			groupAvgDivResult.addnNRBP20(nNRBP20);
			
			groupAvgDivResult.addPIA10(pIA10);
			
			groupAvgDivResult.addStrec10(strec10);
			
			if(DEBUG){
				System.out.println(groupAvgDivResult.toString());
			}			
		}	
		
		return groupAvgDivResult;
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
		
		System.out.println("pIA@10-test\t\t"+wsrTest.wilcoxonSignedRankTest(getDArray(aDivResult.pIA10), getDArray(bDivResult.pIA10), false));
		System.out.println();
		
		System.out.println("strec@10-test\t\t"+wsrTest.wilcoxonSignedRankTest(getDArray(aDivResult.strec10), getDArray(bDivResult.strec10), false));
		System.out.println();		
	}
	
	//paired t-test
	public static void tTest(DivResult aDivResult, DivResult bDivResult){
		org.apache.commons.math3.stat.inference.TTest wsrTest = new org.apache.commons.math3.stat.inference.TTest();
		
		//
		System.out.println("anDCG@5-test\t\t"+wsrTest.pairedTTest(getDArray(aDivResult.alphanDCG5), getDArray(bDivResult.alphanDCG5)));
		//System.out.println();
		System.out.println("anDCG@10-test\t\t"+wsrTest.pairedTTest(getDArray(aDivResult.alphanDCG10), getDArray(bDivResult.alphanDCG10)));
		//System.out.println();
		System.out.println("anDCG@20-test\t\t"+wsrTest.pairedTTest(getDArray(aDivResult.alphanDCG20), getDArray(bDivResult.alphanDCG20)));
		//System.out.println();
		System.out.println();
		
		System.out.println("nERRIA@5-test\t\t"+wsrTest.pairedTTest(getDArray(aDivResult.nERRIA5), getDArray(bDivResult.nERRIA5)));
		//System.out.println();
		System.out.println("nERRIA@10-test\t\t"+wsrTest.pairedTTest(getDArray(aDivResult.nERRIA10), getDArray(bDivResult.nERRIA10)));
		//System.out.println();
		System.out.println("nERRIA@20-test\t\t"+wsrTest.pairedTTest(getDArray(aDivResult.nERRIA20), getDArray(bDivResult.nERRIA20)));
		//System.out.println();
		System.out.println();
		
		System.out.println("pIA@10-test\t\t"+wsrTest.pairedTTest(getDArray(aDivResult.pIA10), getDArray(bDivResult.pIA10)));
		System.out.println();
		
		System.out.println("strec@10-test\t\t"+wsrTest.pairedTTest(getDArray(aDivResult.strec10), getDArray(bDivResult.strec10)));
		System.out.println();		
	}
	
	//cikm 2014
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
	
	////sigir 2016////
	public static void sigir2016_ImplicitSRD_StatisticalSignificanceTest(){
		//baseline results
		String dir = OutputDirectory.ROOT+"/sigir2016/RunOutput/Cross/2-fold/ImplicitSRD-commonIndri/UniformReleDivKernelsComparison/";
		//String div2009_Baseline = "NoDescription_Div2009BFS_BM25Baseline_ndeval.txt";
		//String div2010_Baseline = "NoDescription_Div2010BFS_BM25Baseline_ndeval.txt";
		
		//DivResult r2009_Baseline = loadDivResult(baselineDir+div2009_Baseline);
		//DivResult r2010_Baseline = loadDivResult(baselineDir+div2010_Baseline);	
		
		//String singleLambdaDir = OutputDirectory.ROOT+"DivEvaluation/NoDescription_Evaluation/SingleLambdaEvaluation/";
		
		//mmr
		DivResult r2009_mmr = loadDivResult(dir+"Div2009BFS_true_test_optimal_Div2009_1.0_BM25Kernel_A1_TFIDF_A1_ndeval.txt");
		DivResult r2010_mmr = loadDivResult(dir+"Div2010BFS_true_test_optimal_Div2010_0.9_BM25Kernel_A1_TFIDF_A1_ndeval.txt");
		
		//dfp
		DivResult r2009_dfp = loadDivResult(dir+"Div2009MDP_true_test_optimal_Div2009_0.7_BM25Kernel_A1_TFIDF_A1_ndeval.txt");
		DivResult r2010_dfp = loadDivResult(dir+"Div2010MDP_true_test_optimal_Div2010_0.9_BM25Kernel_A1_TFIDF_A1_ndeval.txt");
		
		//1-call@k
		//DivResult r2009_1callk = loadDivResult(singleLambdaDir+"Div2009BFS_PLSR_ndeval.txt");
		//DivResult r2010_1callk = loadDivResult(singleLambdaDir+"Div2010BFS_PLSR_ndeval.txt");
		
		//I-MP
		//String mskpDir = OutputDirectory.ROOT+"DivEvaluation/NoDescription_Evaluation/01MSKP/";
		DivResult r2009_IMP = loadDivResult(dir+"Div2009ImpSRD_true_test_optimal_Div2009_0.1_BM25Kernel_A1_TFIDF_A1_Y_Belief_ndeval.txt");
		DivResult r2010_IMP = loadDivResult(dir+"Div2010ImpSRD_true_test_optimal_Div2010_0.7_BM25Kernel_A1_TFIDF_A1_Y_Belief_ndeval.txt");
		
		
		
		//dfp to mmr		
		System.out.println("2009 dfp->mmr");
		WilcoxonSignedRankTest(r2009_mmr, r2009_dfp);
		System.out.println("2010 dfp->mmr");
		WilcoxonSignedRankTest(r2010_mmr, r2010_dfp);
		System.out.println();
		
		//I-MP to mmr
		System.out.println("2009 I-MP->mmr");
		WilcoxonSignedRankTest(r2009_mmr, r2009_IMP);
		System.out.println("2010 I-MP->mmr");
		WilcoxonSignedRankTest(r2010_mmr, r2010_IMP);
		System.out.println();
	}
	//
	public static void sigir2016_ExplicitSRD_StatisticalSignificanceTest(){
		//baseline results
		String baselineDir = OutputDirectory.ROOT+"sigir2016/RunOutput/Cross/2-fold/ExplicitSRD-commonIndri/";
		
		//xquad
		DivResult r2009_xquad = loadDivResult(baselineDir+"XQuAD-tfidf/Div2009XQuAD_true_test_optimal_Div2009_0.0_TFIDF_A1_ndeval.txt");
		DivResult r2010_xquad = loadDivResult(baselineDir+"XQuAD-tfidf/Div2010XQuAD_true_test_optimal_Div2010_0.0_TFIDF_A1_ndeval.txt");
				
		//pm2
		DivResult r2009_pm2 = loadDivResult(baselineDir+"PM2-tfidf/Div2009PM2_true_test_optimal_Div2009_0.8_TFIDF_A1_ndeval.txt");
		DivResult r2010_pm2 = loadDivResult(baselineDir+"PM2-tfidf/Div2010PM2_true_test_optimal_Div2010_0.9_TFIDF_A1_ndeval.txt");
						
		//E-MP
		String dir = "sigir2016/RunOutput/Evaluation_ExplicitSRD_Final/ExpSRD_commonIndri_bm25/";
		//String mskpDir = OutputDirectory.ROOT+"DivEvaluation/NoDescription_Evaluation/01MSKP/";
		DivResult r2009_EMP = loadDivResult(OutputDirectory.ROOT+dir+"Div2009ExpSRD_true_BM25Kernel_A1_Y_Belief_ndeval.txt");
		DivResult r2010_EMP = loadDivResult(OutputDirectory.ROOT+dir+"Div2010ExpSRD_true_BM25Kernel_A1_Y_Belief_ndeval.txt");
		
		
		
		//pm2 to xquad		
		System.out.println("2009 pm2->xquad");
		WilcoxonSignedRankTest(r2009_xquad, r2009_pm2);
		System.out.println("2010 pm2->xquad");
		WilcoxonSignedRankTest(r2010_xquad, r2010_pm2);
		System.out.println();
		
		//E-MP to xquad
		System.out.println("2009 E-MP->xquad");
		WilcoxonSignedRankTest(r2009_xquad, r2009_EMP);
		System.out.println("2010 E-MP->xquad");
		WilcoxonSignedRankTest(r2010_xquad, r2010_EMP);
		System.out.println();
		
		//E-MP to pm2
		System.out.println("2009 E-MP->pm2");
		WilcoxonSignedRankTest(r2009_pm2, r2009_EMP);
		System.out.println("2010 E-MP->pm2");
		WilcoxonSignedRankTest(r2010_pm2, r2010_EMP);
		System.out.println();
	}
	//// improvement ////
	public static DecimalFormat oneResultFormat = new DecimalFormat("0");
	public static String improveBoverA(double a, double b){
		double sub = b-a;
		double impP = sub/a*100;
		return Integer.toString((int)impP);		
	}
	public static void improvementPercentage(DivResult aDivResult, DivResult bDivResult){	
		//
		System.out.println("nERRIA@5-test\t\t"+improveBoverA(aDivResult.nERRIA5.get(0) ,bDivResult.nERRIA5.get(0)));
		System.out.println();
		System.out.println("nERRIA@10-test\t\t"+improveBoverA(aDivResult.nERRIA10.get(0) ,bDivResult.nERRIA10.get(0)));
		//System.out.println();
		System.out.println("nERRIA@20-test\t\t"+improveBoverA(aDivResult.nERRIA20.get(0) ,bDivResult.nERRIA20.get(0)));
		//System.out.println();
		System.out.println();
		
		//
		System.out.println("anDCG@5-test\t\t"+improveBoverA(aDivResult.alphanDCG5.get(0), bDivResult.alphanDCG5.get(0)));
		System.out.println();
		System.out.println("anDCG@10-test\t\t"+improveBoverA(aDivResult.alphanDCG10.get(0) ,bDivResult.alphanDCG10.get(0)));
		//System.out.println();
		System.out.println("anDCG@20-test\t\t"+improveBoverA(aDivResult.alphanDCG20.get(0) ,bDivResult.alphanDCG20.get(0)));
		//System.out.println();
		System.out.println();
		
		System.out.println("nNRBP@20-test\t\t"+improveBoverA(aDivResult.nNRBP20.get(0) ,bDivResult.nNRBP20.get(0)));
		System.out.println();
		
		
		//System.out.println("pIA@10-test\t\t"+improveBoverA(aDivResult.pIA10.get(0) ,bDivResult.pIA10.get(0)));
		//System.out.println();
		
		//System.out.println("strec@10-test\t\t"+improveBoverA(aDivResult.strec10.get(0) ,bDivResult.strec10.get(0)));
		//System.out.println();		
	}
	//
	public static void sigir2016_ImplicitSRD_Improvement(){
		//baseline results
		String dir = OutputDirectory.ROOT+"/sigir2016/RunOutput/Cross/2-fold/ImplicitSRD-commonIndri/UniformReleDivKernelsComparison/";
		//String div2009_Baseline = "NoDescription_Div2009BFS_BM25Baseline_ndeval.txt";
		//String div2010_Baseline = "NoDescription_Div2010BFS_BM25Baseline_ndeval.txt";
		
		//DivResult r2009_Baseline = loadDivResult(baselineDir+div2009_Baseline);
		//DivResult r2010_Baseline = loadDivResult(baselineDir+div2010_Baseline);	
		
		//String singleLambdaDir = OutputDirectory.ROOT+"DivEvaluation/NoDescription_Evaluation/SingleLambdaEvaluation/";
		
		//mmr
		DivResult r2009_mmr = loadAvgDivResult(dir+"Div2009BFS_true_test_optimal_Div2009_1.0_BM25Kernel_A1_TFIDF_A1_ndeval.avg.txt");
		DivResult r2010_mmr = loadAvgDivResult(dir+"Div2010BFS_true_test_optimal_Div2010_0.9_BM25Kernel_A1_TFIDF_A1_ndeval.avg.txt");
		
		//dfp
		DivResult r2009_dfp = loadAvgDivResult(dir+"Div2009MDP_true_test_optimal_Div2009_0.7_BM25Kernel_A1_TFIDF_A1_ndeval.avg.txt");
		DivResult r2010_dfp = loadAvgDivResult(dir+"Div2010MDP_true_test_optimal_Div2010_0.9_BM25Kernel_A1_TFIDF_A1_ndeval.avg.txt");
		
		//I-MP
		//String mskpDir = OutputDirectory.ROOT+"DivEvaluation/NoDescription_Evaluation/01MSKP/";
		DivResult r2009_IMP = loadAvgDivResult(dir+"Div2009ImpSRD_true_test_optimal_Div2009_0.1_BM25Kernel_A1_TFIDF_A1_Y_Belief_ndeval.avg.txt");
		DivResult r2010_IMP = loadAvgDivResult(dir+"Div2010ImpSRD_true_test_optimal_Div2010_0.7_BM25Kernel_A1_TFIDF_A1_Y_Belief_ndeval.avg.txt");
		
		
		
		//dfp to mmr		
		System.out.println("2009 dfp->mmr");
		improvementPercentage(r2009_mmr, r2009_dfp);
		System.out.println("2010 dfp->mmr");
		improvementPercentage(r2010_mmr, r2010_dfp);
		System.out.println();
		
		//I-MP to mmr
		System.out.println("2009 I-MP->mmr");
		improvementPercentage(r2009_mmr, r2009_IMP);
		System.out.println("2010 I-MP->mmr");
		improvementPercentage(r2010_mmr, r2010_IMP);
		System.out.println();
	}
	//
	public static void sigir2016_ExplicitSRD_Improvement_ToIndriBaseline(){
		String initialRunDir = OutputDirectory.ROOT+"sigir2016/RunOutput/baseline_commonIndri/";
		//baseline results
		DivResult r2009_ini = loadAvgDivResult(initialRunDir+"Div2009CommonIndriBaseline_true_ndeval.avg.txt");
		DivResult r2010_ini = loadAvgDivResult(initialRunDir+"Div2010CommonIndriBaseline_true_ndeval.avg.txt");
		
		//
		
		String baseDir = OutputDirectory.ROOT+"sigir2016/RunOutput/Cross/2-fold/ExplicitSRD-commonIndri/";
		
		//xquad lm
		DivResult r2009_xquad_lm = loadAvgDivResult(baseDir+"XQuAD-terrier/Div2009XQuAD_true_test_optimal_Div2009_1.0_TerrierKernel_ndeval.avg.txt");
		DivResult r2010_xquad_lm = loadAvgDivResult(baseDir+"XQuAD-terrier/Div2010XQuAD_true_test_optimal_Div2010_1.0_TerrierKernel_ndeval.avg.txt");
				
		//xquad _tfidf
		DivResult r2009_xquad_tfidf = loadAvgDivResult(baseDir+"XQuAD-tfidf/Div2009XQuAD_true_test_optimal_Div2009_0.0_TFIDF_A1_ndeval.avg.txt");
		DivResult r2010_xquad_tfidf = loadAvgDivResult(baseDir+"XQuAD-tfidf/Div2010XQuAD_true_test_optimal_Div2010_0.0_TFIDF_A1_ndeval.avg.txt");
				
		
		//pm2 lm
		DivResult r2009_pm2_lm = loadAvgDivResult(baseDir+"PM2-terrier/Div2009PM2_true_test_optimal_Div2009_1.0_TerrierKernel_ndeval.avg.txt");
		DivResult r2010_pm2_lm = loadAvgDivResult(baseDir+"PM2-terrier/Div2010PM2_true_test_optimal_Div2010_0.4_TerrierKernel_ndeval.avg.txt");

		//pm2 tfidf
		DivResult r2009_pm2_tfidf = loadAvgDivResult(baseDir+"PM2-tfidf/Div2009PM2_true_test_optimal_Div2009_0.8_TFIDF_A1_ndeval.avg.txt");
		DivResult r2010_pm2_tfidf = loadAvgDivResult(baseDir+"PM2-tfidf/Div2010PM2_true_test_optimal_Div2010_0.9_TFIDF_A1_ndeval.avg.txt");

		
		//E-MP
		String dir = "sigir2016/RunOutput/Evaluation_ExplicitSRD_Final/ExpSRD_commonIndri_bm25/";
		//String mskpDir = OutputDirectory.ROOT+"DivEvaluation/NoDescription_Evaluation/01MSKP/";
		DivResult r2009_EMP = loadAvgDivResult(OutputDirectory.ROOT+dir+"Div2009ExpSRD_true_BM25Kernel_A1_Y_Belief_ndeval.avg.txt");
		DivResult r2010_EMP = loadAvgDivResult(OutputDirectory.ROOT+dir+"Div2010ExpSRD_true_BM25Kernel_A1_Y_Belief_ndeval.avg.txt");
		
		
		//xquad_lm to ini	
		System.out.println("2009 xquad_lm->ini");
		improvementPercentage(r2009_ini, r2009_xquad_lm);
		System.out.println("2010 xquad_lm->ini");
		improvementPercentage(r2010_ini, r2010_xquad_lm);
		System.out.println();
		
		//xquad_tfidf to ini	
		System.out.println("2009 xquad_tfidf->ini");
		improvementPercentage(r2009_ini, r2009_xquad_tfidf);
		System.out.println("2010 xquad_tfidf->ini");
		improvementPercentage(r2010_ini, r2010_xquad_tfidf);
		System.out.println();
		
		//pm2_lm to ini	
		System.out.println("2009 pm2_lm->ini");
		improvementPercentage(r2009_ini, r2009_pm2_lm);
		System.out.println("2010 pm2_lm->ini");
		improvementPercentage(r2010_ini, r2010_pm2_lm);
		System.out.println();
		
		//pm2_tfidf to ini	
		System.out.println("2009 pm2_tfidf->ini");
		improvementPercentage(r2009_ini, r2009_pm2_tfidf);
		System.out.println("2010 pm2_tfidf->ini");
		improvementPercentage(r2010_ini, r2010_pm2_tfidf);
		System.out.println();
		
		//EMP to ini	
		System.out.println("2009 EMP->ini");
		improvementPercentage(r2009_ini, r2009_EMP);
		System.out.println("2010 EMP->ini");
		improvementPercentage(r2010_ini, r2010_EMP);
		System.out.println();
		
	}
	
	public static void sigir2016_ExplicitSRD_Improvement_ToPM2LM(){
		//String initialRunDir = OutputDirectory.ROOT+"sigir2016/RunOutput/baseline_commonIndri/";
		//baseline results
		//DivResult r2009_ini = loadAvgDivResult(initialRunDir+"Div2009CommonIndriBaseline_true_ndeval.avg.txt");
		//DivResult r2010_ini = loadAvgDivResult(initialRunDir+"Div2010CommonIndriBaseline_true_ndeval.avg.txt");
		
		//
		
		String baseDir = OutputDirectory.ROOT+"sigir2016/RunOutput/Cross/2-fold/ExplicitSRD-commonIndri/";
		
		//pm2 lm
		DivResult r2009_pm2_lm = loadAvgDivResult(baseDir+"PM2-terrier/Div2009PM2_true_test_optimal_Div2009_1.0_TerrierKernel_ndeval.avg.txt");
		DivResult r2010_pm2_lm = loadAvgDivResult(baseDir+"PM2-terrier/Div2010PM2_true_test_optimal_Div2010_0.4_TerrierKernel_ndeval.avg.txt");

		//pm2 tfidf
		DivResult r2009_pm2_tfidf = loadAvgDivResult(baseDir+"PM2-tfidf/Div2009PM2_true_test_optimal_Div2009_0.8_TFIDF_A1_ndeval.avg.txt");
		DivResult r2010_pm2_tfidf = loadAvgDivResult(baseDir+"PM2-tfidf/Div2010PM2_true_test_optimal_Div2010_0.9_TFIDF_A1_ndeval.avg.txt");

		
		//xquad lm
		DivResult r2009_xquad_lm = loadAvgDivResult(baseDir+"XQuAD-terrier/Div2009XQuAD_true_test_optimal_Div2009_1.0_TerrierKernel_ndeval.avg.txt");
		DivResult r2010_xquad_lm = loadAvgDivResult(baseDir+"XQuAD-terrier/Div2010XQuAD_true_test_optimal_Div2010_1.0_TerrierKernel_ndeval.avg.txt");
				
		//xquad _tfidf
		DivResult r2009_xquad_tfidf = loadAvgDivResult(baseDir+"XQuAD-tfidf/Div2009XQuAD_true_test_optimal_Div2009_0.0_TFIDF_A1_ndeval.avg.txt");
		DivResult r2010_xquad_tfidf = loadAvgDivResult(baseDir+"XQuAD-tfidf/Div2010XQuAD_true_test_optimal_Div2010_0.0_TFIDF_A1_ndeval.avg.txt");
				
		
		
		
		//E-MP
		String dir = "sigir2016/RunOutput/Evaluation_ExplicitSRD_Final/ExpSRD_commonIndri_bm25/";
		//String mskpDir = OutputDirectory.ROOT+"DivEvaluation/NoDescription_Evaluation/01MSKP/";
		DivResult r2009_EMP = loadAvgDivResult(OutputDirectory.ROOT+dir+"Div2009ExpSRD_true_BM25Kernel_A1_Y_Belief_ndeval.avg.txt");
		DivResult r2010_EMP = loadAvgDivResult(OutputDirectory.ROOT+dir+"Div2010ExpSRD_true_BM25Kernel_A1_Y_Belief_ndeval.avg.txt");
		
//		//pm2_tfidf to pm2 lm	
//		System.out.println("2009 pm2_tfidf->ini");
//		improvementPercentage(r2009_ini, r2009_pm2_tfidf);
//		System.out.println("2010 pm2_tfidf->ini");
//		improvementPercentage(r2010_ini, r2010_pm2_tfidf);
//		System.out.println();
//		
//		//xquad_lm to pm2 lm	
//		System.out.println("2009 xquad_lm->ini");
//		improvementPercentage(r2009_ini, r2009_xquad_lm);
//		System.out.println("2010 xquad_lm->ini");
//		improvementPercentage(r2010_ini, r2010_xquad_lm);
//		System.out.println();
//		
//		//xquad_tfidf to pm2 lm	
//		System.out.println("2009 xquad_tfidf->ini");
//		improvementPercentage(r2009_ini, r2009_xquad_tfidf);
//		System.out.println("2010 xquad_tfidf->ini");
//		improvementPercentage(r2010_ini, r2010_xquad_tfidf);
//		System.out.println();
//		
//		
//		
//		//EMP to pm2 lm
//		System.out.println("2009 EMP->ini");
//		improvementPercentage(r2009_ini, r2009_EMP);
//		System.out.println("2010 EMP->ini");
//		improvementPercentage(r2010_ini, r2010_EMP);
//		System.out.println();
		
	}
		
	////	vary w.r.t. \lambda	////
	public static void sigir2016_ImpPerformanceVsLambda(){
		String dir = OutputDirectory.ROOT+"/sigir2016/RunOutput/Cross/2-fold/ImplicitSRD-commonIndri/UniformReleDivKernelsComparison/";

		//I-MP
		String imp_2009Part1 = "Div2009ImpSRD_true_train_Div2010_";
		String imp_2009Part2 = "_BM25Kernel_A1_TFIDF_A1_Y_Belief_ndeval.avg.txt";
		DivResult imp_2009_AvgGroupVsLam = 
				loadGroupAvgDivResult(dir, imp_2009Part1, imp_2009Part2);
		String imp_2010Part1 = "Div2010ImpSRD_true_train_Div2009_";
		String imp_2010Part2 = "_BM25Kernel_A1_TFIDF_A1_Y_Belief_ndeval.avg.txt";
		
		DivResult imp_2010_AvgGroupVsLam = 
				loadGroupAvgDivResult(dir, imp_2010Part1, imp_2010Part2);
		//
		System.out.println(imp_2009_AvgGroupVsLam.nERRIA20);
		System.out.println(imp_2010_AvgGroupVsLam.nERRIA20);
		System.out.println();
		
		//dfp
		String dfp_2009Part1 = "Div2009MDP_true_train_Div2010_";
		String dfp_2009Part2 = "_BM25Kernel_A1_TFIDF_A1_ndeval.avg.txt";
		DivResult dfp_2009_AvgGroupVsLam = 
				loadGroupAvgDivResult(dir, dfp_2009Part1, dfp_2009Part2);
		String dfp_2010Part1 = "Div2010MDP_true_train_Div2009_";
		String dfp_2010Part2 = "_BM25Kernel_A1_TFIDF_A1_ndeval.avg.txt";
		
		DivResult dfp_2010_AvgGroupVsLam = 
				loadGroupAvgDivResult(dir, dfp_2010Part1, dfp_2010Part2);
		//
		System.out.println(dfp_2009_AvgGroupVsLam.nERRIA20);
		System.out.println(dfp_2010_AvgGroupVsLam.nERRIA20);
		System.out.println();
		
				
		//mmr
		String mmr_2009Part1 = "Div2009BFS_true_train_Div2010_";
		String mmr_2009Part2 = "_BM25Kernel_A1_TFIDF_A1_ndeval.avg.txt";
		DivResult mmr_2009_AvgGroupVsLam = 
				loadGroupAvgDivResult(dir, mmr_2009Part1, mmr_2009Part2);
		String mmr_2010Part1 = "Div2010BFS_true_train_Div2009_";
		String mmr_2010Part2 = "_BM25Kernel_A1_TFIDF_A1_ndeval.avg.txt";
		
		DivResult mmr_2010_AvgGroupVsLam = 
				loadGroupAvgDivResult(dir, mmr_2010Part1, mmr_2010Part2);
		//
		System.out.println(mmr_2009_AvgGroupVsLam.nERRIA20);
		System.out.println(mmr_2010_AvgGroupVsLam.nERRIA20);
		System.out.println();
	}
	
	public static void sigir2016_ExpPerformanceVsLambda(){
		String dir = OutputDirectory.ROOT+"/sigir2016/RunOutput/Cross/2-fold/ExplicitSRD-commonIndri/";

		//xquad tfidf
		String xquad_tfidf_2009Part1 = "Div2009XQuAD_true_trainForTesting_Div2010_";
		String xquad_tfidf_2009Part2 = "_TFIDF_A1_ndeval.avg.txt";
		DivResult xquad_tfidf_2009_AvgGroupVsLam = 
				loadGroupAvgDivResult(dir+"XQuAD-tfidf/", xquad_tfidf_2009Part1, xquad_tfidf_2009Part2);
		
		String xquad_tfidf_2010Part1 = "Div2010XQuAD_true_trainForTesting_Div2009_";
		String xquad_tfidf_2010Part2 = "_TFIDF_A1_ndeval.avg.txt";
		
		DivResult xquad_tfidf_2010_AvgGroupVsLam = 
				loadGroupAvgDivResult(dir+"XQuAD-tfidf/", xquad_tfidf_2010Part1, xquad_tfidf_2010Part2);
		//
		System.out.println(xquad_tfidf_2009_AvgGroupVsLam.nERRIA20);
		System.out.println(xquad_tfidf_2010_AvgGroupVsLam.nERRIA20);
		System.out.println();
				
		//xquad lm
		String xquad_lm_2009Part1 = "Div2009XQuAD_true_trainForTesting_Div2010_";
		String xquad_lm_2009Part2 = "_TerrierKernel_ndeval.avg.txt";
		DivResult xquad_lm_2009_AvgGroupVsLam = 
				loadGroupAvgDivResult(dir+"XQuAD-terrier/", xquad_lm_2009Part1, xquad_lm_2009Part2);
		String xquad_lm_2010Part1 = "Div2010XQuAD_true_trainForTesting_Div2009_";
		String xquad_lm_2010Part2 = "_TerrierKernel_ndeval.avg.txt";
		
		DivResult xquad_lm_2010_AvgGroupVsLam = 
				loadGroupAvgDivResult(dir+"XQuAD-terrier/", xquad_lm_2010Part1, xquad_lm_2010Part2);
		//
		System.out.println(xquad_lm_2009_AvgGroupVsLam.nERRIA20);
		System.out.println(xquad_lm_2010_AvgGroupVsLam.nERRIA20);
		System.out.println();
		
		
		
		//pm2 tfidf
		String pm2_tfidf_2009Part1 = "Div2009PM2_true_train_Div2010_";
		String pm2_tfidf_2009Part2 = "_TFIDF_A1_ndeval.avg.txt";
		DivResult pm2_tfidf_2009_AvgGroupVsLam = 
				loadGroupAvgDivResult(dir+"PM2-tfidf/", pm2_tfidf_2009Part1, pm2_tfidf_2009Part2);
		String pm2_tfidf_2010Part1 = "Div2010PM2_true_train_Div2009_";
		String pm2_tfidf_2010Part2 = "_TFIDF_A1_ndeval.avg.txt";
		
		DivResult pm2_tfidf_2010_AvgGroupVsLam = 
				loadGroupAvgDivResult(dir+"PM2-tfidf/", pm2_tfidf_2010Part1, pm2_tfidf_2010Part2);
		//
		System.out.println(pm2_tfidf_2009_AvgGroupVsLam.nERRIA20);
		System.out.println(pm2_tfidf_2010_AvgGroupVsLam.nERRIA20);
		System.out.println();
		
				
		//pm2 lm
		String pm2_lm_2009Part1 = "Div2009PM2_true_train_Div2010_";
		String pm2_lm_2009Part2 = "_TerrierKernel_ndeval.avg.txt";
		DivResult pm2_lm_2009_AvgGroupVsLam = 
				loadGroupAvgDivResult(dir+"PM2-terrier/", pm2_lm_2009Part1, pm2_lm_2009Part2);
		String pm2_lm_2010Part1 = "Div2010PM2_true_train_Div2009_";
		String pm2_lm_2010Part2 = "_TerrierKernel_ndeval.avg.txt";
		
		DivResult pm2_lm_2010_AvgGroupVsLam = 
				loadGroupAvgDivResult(dir+"PM2-terrier/", pm2_lm_2010Part1, pm2_lm_2010Part2);
		//
		System.out.println(pm2_lm_2009_AvgGroupVsLam.nERRIA20);
		System.out.println(pm2_lm_2010_AvgGroupVsLam.nERRIA20);
		System.out.println();
	}
	
	/**
	 * win and loss ratio
	 * **/
	public static void sigir2016_ExplicitSRD_WinLossRatio(){
		boolean commonIndri = true;
		
		List<String> qList2009 = TRECDivLoader.getDivEvalQueryIDList(true, DivVersion.Div2009);
		Map<String,TRECDivQuery> trecDivQueries2009 = TRECDivLoader.loadTrecDivQueries(commonIndri, DivVersion.Div2009);
		
		List<String> qList2010 = TRECDivLoader.getDivEvalQueryIDList(true, DivVersion.Div2010);
		Map<String,TRECDivQuery> trecDivQueries2010 = TRECDivLoader.loadTrecDivQueries(commonIndri, DivVersion.Div2010);
		
		//baseline results
		String initialRunDir = OutputDirectory.ROOT+"sigir2016/RunOutput/baseline_commonIndri/";
		DivResult r2009_ini = loadDivResult(initialRunDir+"Div2009CommonIndriBaseline_true_ndeval.txt");
		DivResult r2010_ini = loadDivResult(initialRunDir+"Div2010CommonIndriBaseline_true_ndeval.txt");
		
		//		
		String baseDir = OutputDirectory.ROOT+"sigir2016/RunOutput/Cross/2-fold/ExplicitSRD-commonIndri/";		
		//xquad lm
		DivResult r2009_xquad_lm = loadDivResult(baseDir+"XQuAD-terrier/Div2009XQuAD_true_test_optimal_Div2009_1.0_TerrierKernel_ndeval.txt");
		DivResult r2010_xquad_lm = loadDivResult(baseDir+"XQuAD-terrier/Div2010XQuAD_true_test_optimal_Div2010_1.0_TerrierKernel_ndeval.txt");
				
		//xquad _tfidf
		DivResult r2009_xquad_tfidf = loadDivResult(baseDir+"XQuAD-tfidf/Div2009XQuAD_true_test_optimal_Div2009_0.0_TFIDF_A1_ndeval.txt");
		DivResult r2010_xquad_tfidf = loadDivResult(baseDir+"XQuAD-tfidf/Div2010XQuAD_true_test_optimal_Div2010_0.0_TFIDF_A1_ndeval.txt");
				
		
		//pm2 lm
		DivResult r2009_pm2_lm = loadDivResult(baseDir+"PM2-terrier/Div2009PM2_true_test_optimal_Div2009_1.0_TerrierKernel_ndeval.txt");
		DivResult r2010_pm2_lm = loadDivResult(baseDir+"PM2-terrier/Div2010PM2_true_test_optimal_Div2010_0.4_TerrierKernel_ndeval.txt");

		//pm2 tfidf
		DivResult r2009_pm2_tfidf = loadDivResult(baseDir+"PM2-tfidf/Div2009PM2_true_test_optimal_Div2009_0.8_TFIDF_A1_ndeval.txt");
		DivResult r2010_pm2_tfidf = loadDivResult(baseDir+"PM2-tfidf/Div2010PM2_true_test_optimal_Div2010_0.9_TFIDF_A1_ndeval.txt");

		
		//E-MP
		String dir = "sigir2016/RunOutput/Evaluation_ExplicitSRD_Final/ExpSRD_commonIndri_bm25/";
		//String mskpDir = OutputDirectory.ROOT+"DivEvaluation/NoDescription_Evaluation/01MSKP/";
		DivResult r2009_EMP = loadDivResult(OutputDirectory.ROOT+dir+"Div2009ExpSRD_true_BM25Kernel_A1_Y_Belief_ndeval.txt");
		DivResult r2010_EMP = loadDivResult(OutputDirectory.ROOT+dir+"Div2010ExpSRD_true_BM25Kernel_A1_Y_Belief_ndeval.txt");
		
		
		//xquad_lm to ini	
		System.out.print("2009 xquad_lm->baseline\t\t");
		getWinLossRatio(r2009_ini, r2009_xquad_lm, "faceted", qList2009, trecDivQueries2009);getWinLossRatio(r2009_ini, r2009_xquad_lm, "ambiguous", qList2009, trecDivQueries2009);
		System.out.println();
		System.out.print("2010 xquad_lm->baseline\t\t");
		getWinLossRatio(r2010_ini, r2010_xquad_lm, "faceted", qList2010, trecDivQueries2010);getWinLossRatio(r2010_ini, r2010_xquad_lm, "ambiguous", qList2010, trecDivQueries2010);
		System.out.println();
		
		//xquad_tfidf to ini	
		System.out.print("2009 xquad_cos->baseline\t");
		getWinLossRatio(r2009_ini, r2009_xquad_tfidf, "faceted", qList2009, trecDivQueries2009);getWinLossRatio(r2009_ini, r2009_xquad_tfidf, "ambiguous", qList2009, trecDivQueries2009);
		System.out.println();
		System.out.print("2010 xquad_cos->baseline\t");
		getWinLossRatio(r2010_ini, r2010_xquad_tfidf, "faceted", qList2010, trecDivQueries2010);getWinLossRatio(r2010_ini, r2010_xquad_tfidf, "ambiguous", qList2010, trecDivQueries2010);
		System.out.println();
		
		//pm2_lm to ini	
		System.out.print("2009 pm2_lm->baseline\t\t");
		getWinLossRatio(r2009_ini, r2009_pm2_lm, "faceted", qList2009, trecDivQueries2009);getWinLossRatio(r2009_ini, r2009_pm2_lm, "ambiguous", qList2009, trecDivQueries2009);
		System.out.println();
		System.out.print("2010 pm2_lm->baseline\t\t");
		getWinLossRatio(r2010_ini, r2010_pm2_lm, "faceted", qList2010, trecDivQueries2010);getWinLossRatio(r2010_ini, r2010_pm2_lm, "ambiguous", qList2010, trecDivQueries2010);
		System.out.println();
		
		//pm2_tfidf to ini	
		System.out.print("2009 pm2_cos->baseline\t\t");
		getWinLossRatio(r2009_ini, r2009_pm2_tfidf, "faceted", qList2009, trecDivQueries2009);getWinLossRatio(r2009_ini, r2009_pm2_tfidf, "ambiguous", qList2009, trecDivQueries2009);
		System.out.println();
		System.out.print("2010 pm2_cos->baseline\t\t");
		getWinLossRatio(r2010_ini, r2010_pm2_tfidf, "faceted", qList2010, trecDivQueries2010);getWinLossRatio(r2010_ini, r2010_pm2_tfidf, "ambiguous", qList2010, trecDivQueries2010);
		System.out.println();
		
		//EMP to ini	
		System.out.print("2009 EMP->baseline\t\t");
		getWinLossRatio(r2009_ini, r2009_EMP, "faceted", qList2009, trecDivQueries2009);getWinLossRatio(r2009_ini, r2009_EMP, "ambiguous", qList2009, trecDivQueries2009);
		System.out.println();
		System.out.print("2010 EMP->baseline\t\t");
		getWinLossRatio(r2010_ini, r2010_EMP, "faceted", qList2010, trecDivQueries2010);getWinLossRatio(r2010_ini, r2010_EMP, "ambiguous", qList2010, trecDivQueries2010);
		System.out.println();
	}
	
	public static void getWinLossRatio(DivResult baseDivResult, DivResult sysDivResult,
			String typeStr, List<String> qList, Map<String,TRECDivQuery> divQueryMap){
		//
		int winCnt=0, lossCnt=0;
		if(null != typeStr){
			for(int qI=0; qI<qList.size(); qI++){
				String qNum = qList.get(qI);			
				if(divQueryMap.get(qNum)._type.trim().equals(typeStr)){				
					if(baseDivResult.nERRIA10.get(qI) >= sysDivResult.nERRIA10.get(qI)){
						lossCnt++;
					}else if(baseDivResult.nERRIA10.get(qI) < sysDivResult.nERRIA10.get(qI)){
						winCnt++;
					}				
				}				
			}
		}else{
			for(int qI=0; qI<qList.size(); qI++){
				if(baseDivResult.nERRIA10.get(qI) >= sysDivResult.nERRIA10.get(qI)){
					lossCnt++;
				}else if(baseDivResult.nERRIA10.get(qI) < sysDivResult.nERRIA10.get(qI)){
					winCnt++;
				}					
			}			
		}
		
		//
		System.out.print(winCnt+" / "+lossCnt+"\t");
	}
	
	//format: topicid baseline proposedMethod
	public static void getAFile(){
		//boolean commonIndri = true;		
		List<String> qList2009 = TRECDivLoader.getDivEvalQueryIDList(true, DivVersion.Div2009);	
		List<String> qList2010 = TRECDivLoader.getDivEvalQueryIDList(true, DivVersion.Div2010);
		
		//baseline results
		String initialRunDir = OutputDirectory.ROOT+"sigir2016/RunOutput/baseline_commonIndri/";
		DivResult r2009_ini = loadDivResult(initialRunDir+"Div2009CommonIndriBaseline_true_ndeval.txt");
		DivResult r2010_ini = loadDivResult(initialRunDir+"Div2010CommonIndriBaseline_true_ndeval.txt");
		//E-MP
		String dir = "sigir2016/RunOutput/Evaluation_ExplicitSRD_Final/ExpSRD_commonIndri_bm25/";
		//String mskpDir = OutputDirectory.ROOT+"DivEvaluation/NoDescription_Evaluation/01MSKP/";
		DivResult r2009_EMP = loadDivResult(OutputDirectory.ROOT+dir+"Div2009ExpSRD_true_BM25Kernel_A1_Y_Belief_ndeval.txt");
		DivResult r2010_EMP = loadDivResult(OutputDirectory.ROOT+dir+"Div2010ExpSRD_true_BM25Kernel_A1_Y_Belief_ndeval.txt");
		
		try {
			BufferedWriter writer2009 = IOText.getBufferedWriter_UTF8(OutputDirectory.ROOT+"sigir2016/RunOutput/2009BaselineVsEMP.txt");
			writer2009.write("topicID\tBaseline(nERRIA@20)\tE-MP(nERRIA@20)");
			writer2009.newLine();
			for(int i=0; i<qList2009.size(); i++){
				String q = qList2009.get(i);
				
				writer2009.write(q+"\t"+r2009_ini.nERRIA20.get(i)+"\t"+r2009_EMP.nERRIA20.get(i));
				writer2009.newLine();
			}
			writer2009.flush();
			writer2009.close();
			
			BufferedWriter writer2010 = IOText.getBufferedWriter_UTF8(OutputDirectory.ROOT+"sigir2016/RunOutput/2010BaselineVsEMP.txt");
			writer2010.write("topicID\tBaseline(nERRIA@20)\tE-MP(nERRIA@20)");
			writer2010.newLine();
			for(int i=0; i<qList2010.size(); i++){
				String q = qList2010.get(i);
				writer2010.write(q+"\t"+r2010_ini.nERRIA20.get(i)+"\t"+r2010_EMP.nERRIA20.get(i));
				writer2010.newLine();
			}
			writer2010.flush();
			writer2010.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	//
 	public static void main(String []args){
		////////////////////////////
		//1
		////////////////////////////
		//1 
		//String perLambdaResultdir = OutputDirectory.ROOT+"DivEvaluation/PerLambdaEvaluation/";
		
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
		//ResultAnalyzer.statisticalSignificanceTest();
		
		
		
		
		
		
		///////sigir2016
		//1
		//ResultAnalyzer.sigir2016_ImplicitSRD_StatisticalSignificanceTest();
		//2
		//ResultAnalyzer.sigir2016_ExplicitSRD_StatisticalSignificanceTest();
		
		//3
		//ResultAnalyzer.sigir2016_ImplicitSRD_Improvement();
		//4
		//ResultAnalyzer.sigir2016_ExplicitSRD_Improvement();
 		
 		//5 Implicit SRD performance w.r.t. lambda in terms of ERR-IA@20
 		//ResultAnalyzer.sigir2016_ImpPerformanceVsLambda();
 		
 		//6 Explicit SRD performance w.r.t. lambda in terms of ERR-IA@20
 		//ResultAnalyzer.sigir2016_ExpPerformanceVsLambda();
 		//ResultAnalyzer.sigir2016_ExplicitSRD_WinLossRatio();
 		
 		//
 		ResultAnalyzer.getAFile();
	}

}
