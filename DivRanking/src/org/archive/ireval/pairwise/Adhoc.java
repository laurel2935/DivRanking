package org.archive.ireval.pairwise;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.archive.OutputDirectory;
import org.archive.dataset.DataSetDiretory;
import org.archive.dataset.trec.TRECDivLoader;
import org.archive.dataset.trec.TRECDivLoader.DivVersion;
import org.archive.dataset.trec.query.TRECDivQuery;
import org.archive.util.io.IOText;
import org.archive.util.tuple.Triple;

public class Adhoc {
	//
	public static enum AdhocVersion{Adhoc2009, Adhoc2010, Adhoc2011, Adhoc2012}
	
	private final static String TREC_AdhocQRELS_09       = DataSetDiretory.ROOT+"trec/Adhoc2009/";
	private final static String TREC_AdhocQRELS_10       = DataSetDiretory.ROOT+"trec/Adhoc2010/";	
	private final static String TREC_AdhocQRELS_11       = DataSetDiretory.ROOT+"trec/Adhoc2011/qrels.adhoc";
	private final static String TREC_AdhocQRELS_12       = DataSetDiretory.ROOT+"trec/Adhoc2012/qrels.adhoc";
	
	private final static String PairwiseDir	 = "C:\\T\\WorkBench\\Bench_Output\\Output_HigherOrder\\";
	private final static String PreferenceDir	 = PairwiseDir+"Preference\\";
	private final static String MarginalUtilityDir	 = PairwiseDir+"MarginalUtility\\";
	
	/*
	 * Return format
	 * <1>  queryid | docid | releLevel
	 * **/
	public static ArrayList<Triple<String, String, Integer>> getAdhocQrel(AdhocVersion adhocVersion){
		ArrayList<Triple<String, String, Integer>> qDocReleTriList = new ArrayList<Triple<String,String,Integer>>();
		
		String aspect_file = null;
		int usefulnessThreshold = 1;
		if(AdhocVersion.Adhoc2009 == adhocVersion){
			aspect_file = TREC_AdhocQRELS_09;						
		}else if(AdhocVersion.Adhoc2010 == adhocVersion){
			aspect_file = TREC_AdhocQRELS_10;	
		}else if(adhocVersion == AdhocVersion.Adhoc2011){
			aspect_file = TREC_AdhocQRELS_11;
		}else if(adhocVersion == AdhocVersion.Adhoc2012){
			aspect_file = TREC_AdhocQRELS_12;
		}else{
			System.out.println("ERROR: unexpected DivVersion!");
			new Exception().printStackTrace();
			System.exit(1);				
		}
		
		ArrayList<String> lineList = IOText.getLinesAsAList_UTF8(aspect_file);
		for(String line: lineList){
			line = line.replaceAll("[\\s]+", "\t");
			String [] parts = line.split("\\s");
			if(parts.length != 4){
				System.err.println("ERROR: unexpected line format!");
				System.err.println(line);
				System.exit(1);
			}
			String queryid = parts[0];
			String docid = parts[2];			
			Integer releLevel = Integer.parseInt(parts[3].trim());
			
			if(releLevel >= usefulnessThreshold){
				qDocReleTriList.add(new Triple<String, String, Integer>(queryid, docid, releLevel));
			}			
		}
		
		//
		/*
		for(Triple<String, String, Integer> t: qDocReleTriList){
			System.out.println(t.toString());
		}
		System.out.println(qDocReleTriList.size());
		*/
		return qDocReleTriList;
	}
	
	class AdhocDocReleFingerprint{
		String _docid;
		//queryid-> ReleDegree
		HashMap<String, Integer> _ReleMap;		
		
		AdhocDocReleFingerprint(String docid){
			this._docid = docid;
			this._ReleMap = new HashMap<>();
		}
		// queryid | docid | releLevel
		public void addReleRecord(String queryid, Integer releDegree){
			_ReleMap.put(queryid, releDegree);
		}
	}	
	//
	private HashMap<String, AdhocDocReleFingerprint> getDocReleMap(ArrayList<Triple<String, String, Integer>> qDocReleTriList){
		HashMap<String, AdhocDocReleFingerprint> docReleMap = new HashMap<>();
		
		for(Triple<String, String, Integer> tri: qDocReleTriList){
			String docid = tri.getSecond();
			
			if(docReleMap.containsKey(docid)){
				AdhocDocReleFingerprint adhocDocReleFingerprint = docReleMap.get(docid);
				adhocDocReleFingerprint.addReleRecord(tri.getFirst(), tri.getThird());
			}else{
				AdhocDocReleFingerprint adhocDocReleFingerprint = new AdhocDocReleFingerprint(docid);
				adhocDocReleFingerprint.addReleRecord(tri.getFirst(), tri.getThird());
				docReleMap.put(docid, adhocDocReleFingerprint);
			}
		}
		
		return docReleMap;
	}
	//
	private ArrayList<String> getReleDocList(String queryid, HashMap<String, AdhocDocReleFingerprint> docReleMap){
		ArrayList<String> releDocList = new ArrayList<>();
		
		for(Entry<String, AdhocDocReleFingerprint> entry: docReleMap.entrySet()){
			AdhocDocReleFingerprint adhocDocReleFingerprint = entry.getValue();
			if(adhocDocReleFingerprint._ReleMap.containsKey(queryid)){
				releDocList.add(adhocDocReleFingerprint._docid);
			}
		}
		
		return releDocList;
	}
	
	//// prefer ////
	/**
	 * 1:  prefer page-1 to page-2
	 * 0:  unsure: e.g., parallel
	 * -1: prefer page-2 to page-1
	 * **/
	private int preferRelation(String topic, String page_1, String page_2, 
			HashMap<String, AdhocDocReleFingerprint> docReleMap){
		AdhocDocReleFingerprint fingerprint_1 = docReleMap.get(page_1);
		AdhocDocReleFingerprint fingerprint_2 = docReleMap.get(page_2);	
		
		if(fingerprint_1._ReleMap.get(topic) > fingerprint_2._ReleMap.get(topic)){
			return 1;
		}else if(fingerprint_1._ReleMap.get(topic) < fingerprint_2._ReleMap.get(topic)){
			return -1;
		}else{
			return 0;
		}		
	}
	
	//// marginal utility ////
	/**
	 * does page-2 provides marginally useful information or not?
	 * 1:  yes
	 * 0:  unsure, e.g., unsure
	 * -1: not 
	 * **/
	private int marginalUtilityRelation(String topic, String page_1, String page_2, 
			HashMap<String, AdhocDocReleFingerprint> docReleMap){
		//
		AdhocDocReleFingerprint fingerprint_1 = docReleMap.get(page_1);
		AdhocDocReleFingerprint fingerprint_2 = docReleMap.get(page_2);
		
		if(fingerprint_1._ReleMap.get(topic) < fingerprint_2._ReleMap.get(topic)){
			//a document with a higher relevance degree, but with a lower rank position 
			return 1;
		}else{
			//unsure
			//case-1: though page-1 with a higher relevance degree, but we don't know the pairwise comparison
			//case-2: parallel case
			return 0;
		}
	}
	
	
	private void getComparisonStr(ArrayList<String> cmpStrList, String page_1, String page_2,
			String topic, HashMap<String, AdhocDocReleFingerprint> docReleMap){
		////
		String cmpStr = "";		
		cmpStr+= (page_1+","+Integer.toString(docReleMap.get(page_1)._ReleMap.get(topic))+",");
		cmpStr+= (page_2+","+Integer.toString(docReleMap.get(page_2)._ReleMap.get(topic)));
		
		cmpStrList.add(cmpStr);	
	}
	
	//// unsure pairs ////
	public void getUnsurePairs_Preference(DivVersion divVersion, AdhocVersion adhocVersion){
		String outputUncertainPrePairFile = PreferenceDir+adhocVersion.toString()+"_UncertainPair_Preference.xlsx";	
		
		boolean commonIndri = true;
		List<String> topicList = TRECDivLoader.getDivEvalQueryIDList(commonIndri, divVersion);
		Map<String,TRECDivQuery> trecDivQueries = TRECDivLoader.loadTrecDivQueries(commonIndri, divVersion);
		
		ArrayList<Triple<String, String, Integer>> qDocReleTriList = getAdhocQrel(adhocVersion);
		HashMap<String, AdhocDocReleFingerprint> docReleMap = getDocReleMap(qDocReleTriList);
		
		////////////////////
		//pairwise comparison w.r.t.preference
		////////////////////
		int totalUnsureCnt = 0;
		int totalSureCnt = 0;
		//excel file
	    ////unsure
		int unsureBeginIndex = 1;
		XSSFWorkbook unsureOutBook = new XSSFWorkbook();					
		XSSFSheet unsureGroupSheet = unsureOutBook.createSheet("Preference");
		//head
		XSSFRow unsureNameRow = unsureGroupSheet.createRow(0);
		
		Cell unsurecell_1 = unsureNameRow.createCell(0);
		unsurecell_1.setCellValue("topicid");
		
		Cell unsurecell_2 = unsureNameRow.createCell(1);						
		unsurecell_2.setCellValue("topicstr");
		
		Cell unsurecell_3 = unsureNameRow.createCell(2);
		unsurecell_3.setCellValue("InforNeedDescription");
		
		Cell unsurecell_4 = unsureNameRow.createCell(3);						
		unsurecell_4.setCellValue("Page_1");
		
		Cell unsurecell_4_1 = unsureNameRow.createCell(4);						
		unsurecell_4_1.setCellValue("Page_1_ReleDegree");
		
		Cell unsurecell_5 = unsureNameRow.createCell(5);
		unsurecell_5.setCellValue("Page_2");
		
		Cell unsurecell_5_1 = unsureNameRow.createCell(5);
		unsurecell_5_1.setCellValue("Page_2_ReleDegree");

		for(String topic: topicList){
			int unsureCnt = 0;
			int sureCnt = 0;
			
			ArrayList<String> releDocList = getReleDocList(topic, docReleMap);
			System.out.println(topic+":\tRele count: "+releDocList.size());
								
			ArrayList<String> cmpStrList = new ArrayList<>();		
			
			int size = releDocList.size();
			for(int i=0; i<size-1; i++){
				String page_1 = releDocList.get(i);
				
				for(int j=i+1; j<size; j++){
					String page_2 = releDocList.get(j);
					
					//only need one-direction comparison, since preference is a reversable relation
					if(0 == preferRelation(topic, page_1, page_2, docReleMap)){
						////
						unsureCnt++;
						//output(topic, page_1, page_2, docReleMap);
						getComparisonStr(cmpStrList, page_1, page_2, topic, docReleMap);
					}else{
						sureCnt++;
					}
				}
			}
			
			System.out.println(topic+":\tUnsure count: "+unsureCnt);
			System.out.println();
			
			totalUnsureCnt += unsureCnt;
			totalSureCnt   += sureCnt;
			
			//
			try {
			////unsure body
				for(int k=0; k<cmpStrList.size(); k++){
					
					XSSFRow unsureKRow = unsureGroupSheet.createRow(k+unsureBeginIndex);
					
					String pairStr = cmpStrList.get(k);
					
					String [] array = pairStr.split(",");
					
					//topicid
					Cell uncell_1 = unsureKRow.createCell(0);
					uncell_1.setCellValue(topic);
					//topicStr
					Cell uncell_2 = unsureKRow.createCell(1);
					uncell_2.setCellValue(trecDivQueries.get(topic)._title);
					
					//information need description
					Cell uncell_3 = unsureKRow.createCell(2);
					uncell_3.setCellValue(trecDivQueries.get(topic)._description);
					//page-1
					Cell uncell_4 = unsureKRow.createCell(3);
					uncell_4.setCellValue(array[0]);
					//releDegree
					Cell uncell_4_1 = unsureKRow.createCell(4);
					uncell_4_1.setCellValue(array[1]);
					
					//page-2
					Cell uncell_5 = unsureKRow.createCell(5);
					uncell_5.setCellValue(array[2]);
					Cell uncell_5_1 = unsureKRow.createCell(6);
					uncell_5_1.setCellValue(array[3]);
					
					
					//Cell cell_6 = kRow.createCell(5);
					//cell_6.setCellValue(array[3]);
					//Cell cell_7 = kRow.createCell(6);
					//cell_7.setCellValue(array[4]);
				}
				
				unsureBeginIndex += cmpStrList.size();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}	
		
		////
		try {
			OutputStream unsureOutputStream = new FileOutputStream(outputUncertainPrePairFile);
			unsureOutBook.write(unsureOutputStream);
			unsureOutputStream.flush();
			unsureOutputStream.close();
		} catch (Exception e) {
			e.printStackTrace();
		}		
		
		System.out.println();
		System.out.println("Total sure count:\t"+totalSureCnt);	
		System.out.println("Total unsure count:\t"+totalUnsureCnt);		
	}
	
	public void getUnsurePairs_MU(DivVersion divVersion, AdhocVersion adhocVersion){
		String outputUncertainMUPairFile = MarginalUtilityDir+adhocVersion.toString()+"_UncertainPair_MU.xlsx";	
		
		boolean commonIndri = true;
		List<String> topicList = TRECDivLoader.getDivEvalQueryIDList(commonIndri, divVersion);
		Map<String,TRECDivQuery> trecDivQueries = TRECDivLoader.loadTrecDivQueries(commonIndri, divVersion);
		
		ArrayList<Triple<String, String, Integer>> qDocReleTriList = getAdhocQrel(adhocVersion);
		HashMap<String, AdhocDocReleFingerprint> docReleMap = getDocReleMap(qDocReleTriList);
		
		////////////////////
		//pairwise comparison w.r.t.preference
		////////////////////
		int totalUnsureCnt = 0;
		int totalSureCnt = 0;
		//excel file
	    ////unsure
		int unsureBeginIndex = 1;
		XSSFWorkbook unsureOutBook = new XSSFWorkbook();					
		XSSFSheet unsureGroupSheet = unsureOutBook.createSheet("MU");
		//head
		XSSFRow unsureNameRow = unsureGroupSheet.createRow(0);
		
		Cell unsurecell_1 = unsureNameRow.createCell(0);
		unsurecell_1.setCellValue("topicid");
		
		Cell unsurecell_2 = unsureNameRow.createCell(1);						
		unsurecell_2.setCellValue("topicstr");
		
		Cell unsurecell_3 = unsureNameRow.createCell(2);
		unsurecell_3.setCellValue("InforNeedDescription");
		
		Cell unsurecell_4 = unsureNameRow.createCell(3);						
		unsurecell_4.setCellValue("Page_1");
		
		Cell unsurecell_4_1 = unsureNameRow.createCell(4);						
		unsurecell_4_1.setCellValue("Page_1_ReleDegree");
		
		Cell unsurecell_5 = unsureNameRow.createCell(5);
		unsurecell_5.setCellValue("Page_2");
		
		Cell unsurecell_5_1 = unsureNameRow.createCell(5);
		unsurecell_5_1.setCellValue("Page_2_ReleDegree");

		for(String topic: topicList){
			int unsureCnt = 0;
			int sureCnt = 0;
			
			ArrayList<String> releDocList = getReleDocList(topic, docReleMap);
			System.out.println(topic+":\tRele count: "+releDocList.size());
								
			ArrayList<String> cmpStrList = new ArrayList<>();		
			
			int size = releDocList.size();
			for(int i=0; i<size-1; i++){
				String page_1 = releDocList.get(i);
				
				for(int j=i+1; j<size; j++){
					String page_2 = releDocList.get(j);
					
					if(0 == marginalUtilityRelation(topic, page_1, page_2, docReleMap)){
						unsureCnt++;
						getComparisonStr(cmpStrList, page_1, page_2, topic, docReleMap);
						
					}else{
						sureCnt++;
					}
					
					if(0 == marginalUtilityRelation(topic, page_2, page_1, docReleMap)){
						unsureCnt++;
						getComparisonStr(cmpStrList, page_2, page_1, topic, docReleMap);
					}else{
						sureCnt++;
					}
				}
			}
			
			System.out.println(topic+":\tUnsure count: "+unsureCnt);
			System.out.println();
			
			totalUnsureCnt += unsureCnt;
			totalSureCnt   += sureCnt;
			
			//
			try {
			////unsure body
				for(int k=0; k<cmpStrList.size(); k++){
					
					XSSFRow unsureKRow = unsureGroupSheet.createRow(k+unsureBeginIndex);
					
					String pairStr = cmpStrList.get(k);
					
					String [] array = pairStr.split(",");
					
					//topicid
					Cell uncell_1 = unsureKRow.createCell(0);
					uncell_1.setCellValue(topic);
					//topicStr
					Cell uncell_2 = unsureKRow.createCell(1);
					uncell_2.setCellValue(trecDivQueries.get(topic)._title);
					
					//information need description
					Cell uncell_3 = unsureKRow.createCell(2);
					uncell_3.setCellValue(trecDivQueries.get(topic)._description);
					//page-1
					Cell uncell_4 = unsureKRow.createCell(3);
					uncell_4.setCellValue(array[0]);
					//releDegree
					Cell uncell_4_1 = unsureKRow.createCell(4);
					uncell_4_1.setCellValue(array[1]);
					
					//page-2
					Cell uncell_5 = unsureKRow.createCell(5);
					uncell_5.setCellValue(array[2]);
					Cell uncell_5_1 = unsureKRow.createCell(6);
					uncell_5_1.setCellValue(array[3]);
					
					
					//Cell cell_6 = kRow.createCell(5);
					//cell_6.setCellValue(array[3]);
					//Cell cell_7 = kRow.createCell(6);
					//cell_7.setCellValue(array[4]);
				}
				
				unsureBeginIndex += cmpStrList.size();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}	
		
		////
		try {
			OutputStream unsureOutputStream = new FileOutputStream(outputUncertainMUPairFile);
			unsureOutBook.write(unsureOutputStream);
			unsureOutputStream.flush();
			unsureOutputStream.close();
		} catch (Exception e) {
			e.printStackTrace();
		}		
		
		System.out.println();
		System.out.println("Total sure count:\t"+totalSureCnt);	
		System.out.println("Total unsure count:\t"+totalUnsureCnt);		
	}
	
	
	//
	public static void main(String []args){
		//1
		//Adhoc.getAdhocQrel(AdhocVersion.Adhoc2011);
		//Adhoc.getAdhocQrel(AdhocVersion.Adhoc2012);
		
		////
		/**
		 * preference
		 * **/		
		//Adhoc adhoc = new Adhoc();
		
		////2011
		//Total sure count:	90428
		//Total unsure count:	107124
		//adhoc.getUnsurePairs_Preference(DivVersion.Div2011, AdhocVersion.Adhoc2011);
		
		////2012
		//Total sure count:	77038
		//Total unsure count:	120367
		//adhoc.getUnsurePairs_Preference(DivVersion.Div2012, AdhocVersion.Adhoc2012);
		
		/**
		 * marginal utility
		 * **/
		Adhoc adhoc = new Adhoc();
		
		////2011
		//Total sure count:	90428
		//Total unsure count:	304676
		//adhoc.getUnsurePairs_MU(DivVersion.Div2011, AdhocVersion.Adhoc2011);
		
		////2012
		//Total sure count:	77038
		//Total unsure count:	317772
		adhoc.getUnsurePairs_MU(DivVersion.Div2012, AdhocVersion.Adhoc2012);
	}
	

}
