package org.archive.ireval;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;

import org.archive.dataset.ntcir.NTCIRLoader;
import org.archive.dataset.ntcir.NTCIRLoader.NTCIR11_TOPIC_TYPE;
import org.archive.dataset.ntcir.NTCIRLoader.NTCIR_EVAL_TASK;
import org.archive.rscript.TauCorrelation;
import org.archive.util.io.IOText;
import org.archive.util.tuple.Pair;
import org.archive.util.tuple.PairComparatorBySecond_Desc;
import org.archive.util.tuple.Triple;
import org.archive.util.tuple.TripleComparatorByThird_Desc;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;


/**
 * 
 * For the comparative analysis of metrics for novelty and diversity
 * 
 * **/

public class MetricComparison {
	
	public static final boolean debug = false;
	private static DecimalFormat resultFormat = new DecimalFormat("#.####");
	//subtopic-level
	private static enum STLevel{FLS, SLS};	
	//metric dimension
	private static enum IDimension{I_AWARE, I_SQUARE};
	
	//graded relevance mode of g
	private static int gMode = 2;
	
	//alphaNDCg
	private static double ALPHA = 0.5;
	//D#-nDCG
	private static double GAMA = 0.5;
	//DIN#-nDCG
	private static double ETA = 0.5;
	//B
	private static int Experiment_B = 5000;
	private static double CurveCut = 0.1;
	//
	private static TauCorrelation tauCorrelation;
 	
	//
	public static ArrayList<TwoLevelTopic> _2LTList = new ArrayList<TwoLevelTopic>();
	//{queryid -> TwoLevelTopic}
	public static HashMap<String, TwoLevelTopic> _2LTMap = new HashMap<String, TwoLevelTopic>();

	MetricComparison(){
		//
		tauCorrelation = new TauCorrelation();
	}
	
	
	/**
	 * Two-level hierarchy of subtopics
	 * **/
	public static void load2LT(String file){
		try {
			
			SAXBuilder saxBuilder = new SAXBuilder();
		    Document xmlDoc = saxBuilder.build(new File(file)); 
		    //new InputStreamReader(new FileInputStream(targetFile), DEFAULT_ENCODING)
		    
		    Element rootElement = xmlDoc.getRootElement();
		    
		    List<Element> topicElementList = rootElement.getChildren("topic");
		    
		    for(Element topicElement: topicElementList){
		    	String id = topicElement.getAttributeValue("id");
		    	String topic = topicElement.getAttributeValue("content");
		    	
		    	TwoLevelTopic twoLT = new TwoLevelTopic(id, topic);
		    	
		    	List<Element> flsElementList = topicElement.getChildren("fls");
		    	
		    	int flsID=1;
		    	//fls
		    	ArrayList<Pair<String, Double>> flsList = new ArrayList<Pair<String,Double>>(); 
		    	ArrayList<ArrayList<String>> flsExampleSetList = new ArrayList<ArrayList<String>>();
		    	HashMap<String, HashSet<Integer>> flsStrMap = new HashMap<String, HashSet<Integer>>();
		    	//sls
		    	ArrayList<ArrayList<Pair<String, Double>>> slsSetList = new ArrayList<ArrayList<Pair<String,Double>>>();
		    	HashMap<Pair<Integer, Integer>,	ArrayList<String>> slsExampleSetMap = new HashMap<Pair<Integer,Integer>, ArrayList<String>>();		    	
		    	HashMap<String, HashSet<Pair<Integer, Integer>>> slsExampleMap = new HashMap<String, HashSet<Pair<Integer, Integer>>>();
		    	
		    	for(; flsID<=flsElementList.size(); flsID++){
		    		Element flsElement = flsElementList.get(flsID-1);
		    		//fls and its possibility
		    		flsList.add(new Pair<String, Double>(flsElement.getAttributeValue("content"),
		    				Double.parseDouble(flsElement.getAttributeValue("poss"))));
		    		
		    		//exampleSet of fls
		    		Element examplesElement = flsElement.getChild("examples");
		    		ArrayList<String> exampleSet = new ArrayList<String>();
		    		List<Element> exampleElementList = examplesElement.getChildren("example");
		    		for(Element exampleElement: exampleElementList){
		    			String flsExample = exampleElement.getText(); 
		    			exampleSet.add(flsExample);
		    			
		    			if(flsStrMap.containsKey(flsExample)){
		    				flsStrMap.get(flsExample).add(flsID);
		    			}else{
		    				HashSet<Integer> flsIDSet = new HashSet<Integer>();
		    				flsIDSet.add(flsID);
		    				flsStrMap.put(flsExample, flsIDSet);
		    			}
		    		}
		    		flsExampleSetList.add(exampleSet);
		    		
		    		//
		    		int slsID = 1;
		    		ArrayList<Pair<String, Double>> slsSet = new ArrayList<Pair<String,Double>>();
		    		List<Element> slsElementList = flsElement.getChildren("sls");
		    		for(; slsID<=slsElementList.size(); slsID++){
		    			Element slsElement = slsElementList.get(slsID-1);
		    			slsSet.add(new Pair<String, Double>(slsElement.getAttributeValue("content"),
		    					Double.parseDouble(slsElement.getAttributeValue("poss"))));
		    			//
		    			Pair<Integer, Integer> idPair = new Pair<Integer, Integer>(flsID, slsID);
		    			ArrayList<String> slsExampleList = new ArrayList<String>();
		    			List<Element> slsExampleElementList = slsElement.getChildren("example");
		    			for(Element slsExampleElement: slsExampleElementList){
		    				String slsExample = slsExampleElement.getText();
		    				slsExampleList.add(slsExample);
		    				
		    				if(slsExampleMap.containsKey(slsExample)){
		    					slsExampleMap.get(slsExample).add(idPair);
		    				}else{
		    					HashSet<Pair<Integer, Integer>> pairSet = new HashSet<Pair<Integer,Integer>>();
		    					pairSet.add(idPair);
		    					slsExampleMap.put(slsExample, pairSet);
		    				}	    				
		    			}
		    			slsExampleSetMap.put(idPair, slsExampleList);
		    		}
		    		slsSetList.add(slsSet);
		    	}
		    	
		    	twoLT.setFlsList(flsList);
		    	twoLT.setFlsExampleSetList(flsExampleSetList);
		    	twoLT.setFlsStrMap(flsStrMap);
		    	
		    	twoLT.setSlsSetList(slsSetList);
		    	twoLT.setSlsExampleSetMap(slsExampleSetMap);
		    	twoLT.setSlsStrMap(slsExampleMap);
		    	
		    	twoLT.getSlsContentMap();
		    	
		    	_2LTList.add(twoLT);
		    	_2LTMap.put(id, twoLT);
		    }
		    
		    if(debug){
		    	System.out.println(_2LTMap.size());
		    	System.out.println();
		    	/*
		    	System.out.println(_2LTMap.get("0001").toString());
		    	
		    	//
		    	TwoLevelTopic t = _2LTMap.get("0001");
		    	ArrayList<Pair<String, Double>> flsList = t._flsList;
		    	ArrayList<ArrayList<Pair<String, Double>>> slsSetList = t._slsSetList;
		    	
		    	int flsID = 1;
		    	
		    	double sumFls = 0.0, sumSls = 0.0;
		    	
		    	for(; flsID<=flsList.size(); flsID++){
		    		Pair<String, Double> fls = flsList.get(flsID-1);
		    		sumFls += fls.getSecond();
		    		System.out.println(fls.getSecond());
		    		
		    		ArrayList<Pair<String, Double>> slsSet = slsSetList.get(flsID-1);
		    		Double slsSum = 0.0;
		    		for(Pair<String, Double> sls: slsSet){
		    			slsSum += sls.getSecond();
		    		}
		    		sumSls += slsSum;
		    		System.out.println("\t"+slsSum);
		    	}	   
		    	
		    	System.out.println("sumFls:\t"+sumFls);
		    	System.out.println("sumSls:\t"+sumSls);
		    	*/
		    }		    
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}
	
	
	////R, I, V:	mapping functions from graded relevance assessments to relevance probabilities or numerical values
	//R:editorial relevance probability
	private static double R(int grade, int mode){
		return ((Math.pow(2, grade)-1)/Math.pow(2, mode));		
	}
	//I: binary mapping
	private static double I(int grade){
		return grade>0?1:0;
	}
	//V: 
	private static double V(int grade){
		return grade;
	}
	
	
	//{docid -> {queryid -> {flsID -> relevanceLevel}}}
	private static HashMap<String, HashMap<String, HashMap<Integer, Integer>>> fls_DocReleMap = null;
	//{docid -> {queryid -> {flsID -> {slsID -> relevanceLevel}}}}
	private static HashMap<String, HashMap<String, HashMap<Integer, HashMap<Integer, Integer>>>> sls_DocReleMap = null;
	////for analyze dependency of relevance among documents
	//docid -> {queryid -> Set of slsStr}
	private static HashMap<String, HashMap<String, HashSet<String>>> netDocUsageMap = null;
	
	/**
	 * Extract qrel xml file
	 * Qrel's xml file consists of two parts:
	 * <1> first relevance part corresponds to relevance judgments for all topics
	 * <2> second relevance part merely corresponds to broad and ambiguous queries, besides the same component,
	 *     it also indicates the 2nd-level subtopic to which a document is in fact relevant.
	 *     And a document may be relevant to several 2nd-level subtopics. 
	 *     
	 * Return
	 * <1>  docid | queryid | releLevel
	 * <2>  docid | queryid+"\t"+slsStr | releLevel
	 * **/
	public static ArrayList<Triple<String, String, Integer>> getXmlQrel(String qrelDir, 
			NTCIRLoader.NTCIR_EVAL_TASK eval, NTCIRLoader.NTCIR11_TOPIC_TYPE type){
		
		//String dir = "H:/v-haiyu/TaskPreparation/Ntcir11-IMine/Eval-IMine/20140830/";
		
		String drFile = null;
		
		if(eval == NTCIR_EVAL_TASK.NTCIR11_DR_CH){
			drFile = qrelDir+"IMine.Qrel.DRC/IMine.Qrel.DRC.xml";
		}else if(eval == NTCIR_EVAL_TASK.NTCIR11_DR_EN){
			drFile = qrelDir+"IMine.Qrel.DRE/IMine.Qrel.DRE.xml";
		}else{
			System.err.println("Type Error!");
			System.exit(0);
		}
		
		try {			
			SAXBuilder saxBuilder = new SAXBuilder();
		    Document xmlDoc = saxBuilder.build(new File(drFile)); 
		    //new InputStreamReader(new FileInputStream(targetFile), DEFAULT_ENCODING)
		    
		    Element rootElement = xmlDoc.getRootElement();
		    
		    List<Element> relevanceElementList = rootElement.getChildren("relevance");
		    
		    if(type == NTCIR11_TOPIC_TYPE.CLEAR){
		    	
		    	Element firstPart = relevanceElementList.get(0);
		    	
		    	List<Element> docElementList = firstPart.getChildren("doc");
		    	ArrayList<Triple<String, String, Integer>> triList = new ArrayList<Triple<String,String,Integer>>();
		    	
		    	for(Element docElement: docElementList){
		    		String docid = docElement.getAttributeValue("docid");
		    		String queryid = docElement.getAttributeValue("queryid");
		    		int releLevel = Integer.parseInt(docElement.getAttributeValue("relevance"));
		    		releLevel = releLevel-1;
		    		
		    		if(releLevel>0){
		    			triList.add(new Triple<String, String, Integer>(docid, queryid, releLevel));
		    		}
		    	}
		        
		    	return triList;  	
		    	
		    }else if(type == NTCIR11_TOPIC_TYPE.UNCLEAR){
		    	
		    	Element secondPart = relevanceElementList.get(1);
		    	
		    	List<Element> docElementList = secondPart.getChildren("doc");
		    	ArrayList<Triple<String, String, Integer>> triList = new ArrayList<Triple<String,String,Integer>>();
		    	
		    	for(Element docElement: docElementList){
		    		String docid = docElement.getAttributeValue("docid");
		    		String queryid = docElement.getAttributeValue("queryid"); 
		    		int releLevel = Integer.parseInt(docElement.getAttributeValue("relevance"));
		    		releLevel = releLevel-1;	
		    		
		    		String slsStr = docElement.getAttributeValue("sls");
		    		
		    		if(releLevel > 0){
		    			triList.add(new Triple<String, String, Integer>(docid, queryid+"\t"+slsStr, releLevel));
		    		}		    		
		    	}
		    	
		    	return triList;
		    	
		    }else{
		    	System.err.println("Type Error!");
		    	System.exit(0);
		    }		    
		}catch(Exception e){
			e.printStackTrace();			
		}
		
		return null;
	}
	//get the relevance assessments of a document w.r.t. second-level subtopics
	private static void getDocUsageMap(String xmlDir, NTCIR_EVAL_TASK eval, NTCIR11_TOPIC_TYPE type){
		//1 ini
		fls_DocReleMap = new HashMap<String, HashMap<String, HashMap<Integer, Integer>>>();
		sls_DocReleMap = new HashMap<String, HashMap<String, HashMap<Integer, HashMap<Integer, Integer>>>>();
		
		netDocUsageMap = new HashMap<String, HashMap<String,HashSet<String>>>();
		
		ArrayList<Triple<String, String, Integer>> triList = null;
		
		if(type == NTCIR11_TOPIC_TYPE.CLEAR){
			System.err.println("Input type error!");
		}else{
			triList = getXmlQrel(xmlDir, eval, type);
		}
		
		//2 parse
		for(Triple<String, String, Integer> triple: triList){
			String docid = triple.getFirst();
			Integer releLevel = triple.getThird();
			
			String [] array = triple.getSecond().split("\t");
			String queryid = array[0];
			String slsStr = array[1];
			
			//mapping from sls's content to fls's id
			Pair<Integer, Integer> e = _2LTMap.get(queryid)._slsContentMap.get(slsStr);
						
			//fls
			Integer flsID = e.getFirst();
			if(fls_DocReleMap.containsKey(docid)){
				HashMap<String, HashMap<Integer, Integer>> docUsage = fls_DocReleMap.get(docid);
				
				if(docUsage.containsKey(queryid)){					
					docUsage.get(queryid).put(flsID, releLevel);
				}else{
					HashMap<Integer, Integer> flsReleMap = new HashMap<Integer, Integer>();
					flsReleMap.put(flsID, releLevel);
					
					docUsage.put(queryid, flsReleMap);
				}				
			}else{
				HashMap<String, HashMap<Integer, Integer>> docUsage = new HashMap<String, HashMap<Integer, Integer>>();
				HashMap<Integer, Integer> flsReleMap = new HashMap<Integer, Integer>();
				flsReleMap.put(flsID, releLevel);				
				docUsage.put(queryid, flsReleMap);
				
				fls_DocReleMap.put(docid, docUsage);
			}
			
			//sls
			Integer slsID = e.getSecond();
			if(sls_DocReleMap.containsKey(docid)){
				//queryid->
				HashMap<String, HashMap<Integer, HashMap<Integer, Integer>>> slsDocUsage = sls_DocReleMap.get(docid);
				
				if(slsDocUsage.containsKey(queryid)){
					//flsID->
					HashMap<Integer, HashMap<Integer, Integer>> slsQReleMap = slsDocUsage.get(queryid);					
					if(slsQReleMap.containsKey(flsID)){
						slsQReleMap.get(flsID).put(slsID, releLevel);
					}else{
						HashMap<Integer, Integer> slsReleMap = new HashMap<Integer, Integer>();
						
						slsReleMap.put(slsID, releLevel);						
						slsQReleMap.put(flsID, slsReleMap);						
					}					
				}else{
					HashMap<Integer, HashMap<Integer, Integer>> slsQReleMap = new HashMap<Integer, HashMap<Integer,Integer>>();
					HashMap<Integer, Integer> slsReleMap = new HashMap<Integer, Integer>();
					
					slsReleMap.put(slsID, releLevel);					
					slsQReleMap.put(flsID, slsReleMap);	
					slsDocUsage.put(queryid, slsQReleMap);
				}
			}else{
				HashMap<String, HashMap<Integer, HashMap<Integer, Integer>>> slsDocUsage = 
						new HashMap<String, HashMap<Integer,HashMap<Integer,Integer>>>();
				HashMap<Integer, HashMap<Integer, Integer>> slsQReleMap = 
						new HashMap<Integer, HashMap<Integer,Integer>>();
				HashMap<Integer, Integer> slsReleMap = 
						new HashMap<Integer, Integer>();
				
				slsReleMap.put(slsID, releLevel);				
				slsQReleMap.put(flsID, slsReleMap);
				slsDocUsage.put(queryid, slsQReleMap);				
				sls_DocReleMap.put(docid, slsDocUsage);
			}
			
			//net
			if(netDocUsageMap.containsKey(docid)){
				HashMap<String, HashSet<String>> docUsage = netDocUsageMap.get(docid);
				
				if(docUsage.containsKey(queryid)){
					docUsage.get(queryid).add(slsStr);
				}else{
					HashSet<String> slsSet = new HashSet<String>();
					slsSet.add(slsStr);
					
					docUsage.put(queryid, slsSet);
				}				
			}else{
				HashMap<String, HashSet<String>> docUsage = new HashMap<String, HashSet<String>>();
				HashSet<String> slsSet = new HashSet<String>();
				slsSet.add(slsStr);				
				docUsage.put(queryid, slsSet);
				
				netDocUsageMap.put(docid, docUsage);
			}
		}		
	}
	
	//per file, queryid -> ranked results
	private static HashMap<String, ArrayList<String>> loadSysRun(String sysFile){
		HashMap<String, ArrayList<String>> sysRun = new HashMap<String, ArrayList<String>>();
		
		try {
			ArrayList<String> lineList = IOText.getLinesAsAList_UTF8(sysFile);
			
			String []array;
			for(int i=0; i<lineList.size(); i++){
				if(lineList.get(i).indexOf("SYSDESC") >= 0){
					continue;
				}
				//array[0]:topic-id / array[1]:0 / array[2]:doc-name / array[3]:rank / array[4]:score / array[5]:run-name
				array = lineList.get(i).split("\\s");
				String topicid = array[0];
				String item = array[2];
				
				if(sysRun.containsKey(topicid)){
					sysRun.get(topicid).add(item);
				}else{
					ArrayList<String> itemList = new ArrayList<String>();
					itemList.add(item);
					sysRun.put(topicid, itemList);
				}
			}			
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		
		//test
		/*
		for(Entry<String, ArrayList<String>> runPerTopic: sysRun.entrySet()){
			System.out.println(runPerTopic.getKey()+"\t"+runPerTopic.getValue().size());
		}
		*/
		
		return sysRun;
	}
	//runid -> runMap
	public static HashMap<String, HashMap<String, ArrayList<String>>> loadSysRuns(String dir){
		
		HashMap<String, HashMap<String, ArrayList<String>>> allRuns = 
				new HashMap<String, HashMap<String,ArrayList<String>>>();
		
		try {
			File fileDir = new File(dir);
			File [] sysRuns = fileDir.listFiles();
			
			for(File oneSysRun: sysRuns){
				String fileName = oneSysRun.getName();
				String runID = fileName.substring(0, fileName.indexOf("."));
				HashMap<String, ArrayList<String>> runMap = loadSysRun(oneSysRun.getAbsolutePath());
				
				allRuns.put(runID, runMap);
			}			
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		
		//test
		/*
		for(Entry<String, HashMap<String, ArrayList<String>>> oneSubmittedRun: allRuns.entrySet()){
			System.out.println(oneSubmittedRun.getKey()+"\t"+oneSubmittedRun.getValue().size());
		}
		*/
		/*
		HashMap<String, ArrayList<String>> run1 = allRuns.get("FRDC-D-C-2A");
		System.out.println(run1.get("0001").get(0));
		System.out.println(run1.get("0050").get(12));
		System.out.println();
		
		HashMap<String, ArrayList<String>> run2 = allRuns.get("THUSAM-D-C-1A");
		System.out.println(run2.get("0001").get(0));
		System.out.println(run2.get("0050").get(199));
		*/
		
		
		return allRuns;
	}
	
	
	////////////
	//Metrics
	////////////
	
	/////	Subtopic-recall	////
	/**
	 * For I-Aware,  compute the covered subtopics for one first-level subtopic, 
	 * 		always the id of this first-level subtopic if there exists one or more relevant documents w.r.t. this first-level subtopic 
	 * For I-Square, compute the covered subtopics for one second-level subtopic
	 * 		always the id of this second-level subtopic if there exists one or more relevant documents w.r.t. this second-level subtopic
	 * 
	 * **/
	private static HashSet<Integer> SRecall_i(String queryid, STLevel stLevel, int flsID, 
			ArrayList<String> sysList, int cutoff, TwoLevelTopic twoLevelTopic, int squareSlsID){
		
		if(null==sysList || sysList.size()==0){
			return null;
		}
		
		//without enough documents provided
		int onCutoff = Math.min(sysList.size(), cutoff);
		
		if(STLevel.FLS==stLevel && 0>squareSlsID){
			
			HashSet<Integer> coveredFlsSet_i = new HashSet<Integer>();	
			
			for(int k=1; k<=onCutoff; k++){
				String docid = sysList.get(k-1);		
				
				if(fls_DocReleMap.containsKey(docid)){
					
					HashMap<String, HashMap<Integer, Integer>> docReleMap = fls_DocReleMap.get(docid);
					if(docReleMap.containsKey(queryid)){
						
						HashMap<Integer, Integer> qReleMap = docReleMap.get(queryid);
						if(qReleMap.containsKey(flsID)){
							
							coveredFlsSet_i.add(flsID);
						}										
					}					
				}				
			}
			
			return coveredFlsSet_i;
			
		}else if(STLevel.SLS==stLevel && 0<squareSlsID){	
			//computing subtopic-specific retrieved documents for one specific second-level subtopic
			
			//representing the covered second-level subtopics
			HashSet<Integer> coveredSlsSet_slsi = new HashSet<Integer>();
			
			for(int k=1; k<=onCutoff; k++){
				String docid = sysList.get(k-1);
				
				if(sls_DocReleMap.containsKey(docid)){
					HashMap<String, HashMap<Integer, HashMap<Integer, Integer>>> slsDocReleMap = sls_DocReleMap.get(docid);
					if(slsDocReleMap.containsKey(queryid)){
						HashMap<Integer, HashMap<Integer, Integer>> slsQReleMap = slsDocReleMap.get(queryid);
						if(slsQReleMap.containsKey(flsID)){
							HashMap<Integer, Integer> slsFlsReleMap = slsQReleMap.get(flsID);
							if(slsFlsReleMap.containsKey(squareSlsID)){
								coveredSlsSet_slsi.add(squareSlsID);
							}							
						}						
					}					
				}
			}

			return coveredSlsSet_slsi;
		}else{
			System.err.println("Non-accepted setting error!");
			System.exit(0);
			return null;	
		}		
	}
	
	/**
	 * For I-Aware,  compute the SRecall for the query
	 * For I-Square, compute the SRecall for a first-level subtopic
	 * **/
	
	private static double SRecall(String queryid, IDimension iDimension, 
			ArrayList<String> sysList, int cutoff, int squareFlsID){	
		
		TwoLevelTopic topic = _2LTMap.get(queryid);
		//intent-aware
		if(IDimension.I_AWARE==iDimension && 0>squareFlsID){
			
			int flsCount = topic._flsList.size();			
			
			HashSet<Integer> coveredFlsSet = new HashSet<Integer>();
			
			for(int i=1; i<=flsCount; i++){
				HashSet<Integer> coveredFlsSet_i = SRecall_i(queryid, STLevel.FLS, i, sysList, cutoff, topic, -1);
				if(null != coveredFlsSet_i){
					coveredFlsSet.addAll(coveredFlsSet_i);
				}
			}
			
			double sRecall = (1.0*coveredFlsSet.size())/flsCount;
			
			return sRecall;
			
		//intent-square
		}else if(IDimension.I_SQUARE == iDimension && 0<squareFlsID){
			//covered second-level subtopics for the given fls
			HashSet<Integer> coveredSlsSet_flsi = new HashSet<Integer>();
			
			ArrayList<Pair<String, Double>> slsList = topic._slsSetList.get(squareFlsID-1);
			
			for(int slsI = 1; slsI<=slsList.size(); slsI++){
				HashSet<Integer> coveredSlsSet_slsi = SRecall_i(queryid, STLevel.SLS, squareFlsID, sysList, cutoff, topic, slsI);
				if(null != coveredSlsSet_slsi){
					coveredSlsSet_flsi.addAll(coveredSlsSet_slsi);
				}
			}	
			
			double sRecall_flsi = (1.0*coveredSlsSet_flsi.size())/slsList.size();	
			
			return sRecall_flsi;
		}else{
			System.err.println("Non-accepted setting error!");
			System.exit(0);
			return Double.NaN;	
		}
	}
	
	private static double Square_SRecall(String queryid, boolean squareEqual, ArrayList<String> sysList, int cutoff){
		
		TwoLevelTopic topic = _2LTMap.get(queryid);
		int flsCount = topic._flsList.size();
		
		double squareSRecall = 0.0;
		
		for(int i=1; i<=flsCount; i++){
			//System.out.println("flsPro:\t"+topic.getFlsPro(i, squareEqual));
			//System.out.println(SRecall(queryid, IDimension.I_SQUARE, sysList, cutoff, i));
			
			squareSRecall += topic.getFlsPro(i, squareEqual)*SRecall(queryid, IDimension.I_SQUARE, sysList, cutoff, i);
			
			//System.out.println(squareSRecall);
			//System.out.println();
		}
		
		return squareSRecall;		
	}
	
	////	AP-IA	////
	/**
	 * For I-Aware,  compute AP for one first-level subtopic
	 * For I-Square, compute AP for for one second-level subtopic
	 * **/
	private static double AP_i(String queryid, STLevel stLevel, int flsID, 
			ArrayList<String> sysList, int cutoff, TwoLevelTopic twoLevelTopic, int squareSlsID){
		//
		if(null ==sysList || sysList.size() == 0){
			return 0.0;
		}
		
		//without enough documents provided
		int onCutoff = Math.min(sysList.size(), cutoff);
		
		if(STLevel.FLS==stLevel && 0>squareSlsID){
			double  ap_i = 0.0;
			double  relesCountByNow = 0;
			
			for(int k=1; k<=onCutoff; k++){
				String docid = sysList.get(k-1);				
				if(fls_DocReleMap.containsKey(docid)){					
					HashMap<String, HashMap<Integer, Integer>> docReleMap = fls_DocReleMap.get(docid);
					if(docReleMap.containsKey(queryid)){						
						HashMap<Integer, Integer> qReleMap = docReleMap.get(queryid);
						if(qReleMap.containsKey(flsID)){							
							double satPro = I(qReleMap.get(flsID));							
							relesCountByNow += I(qReleMap.get(flsID));							
							ap_i += satPro*(1.0/k)*relesCountByNow;
						}										
					}					
				}
			}
			
			ap_i = ap_i/cutoff;			
			return ap_i;			
		}else if(STLevel.SLS==stLevel && 0<squareSlsID){			
			double ap_slsi = 0.0;
			double relesCountByNow_slsi = 0;
			
			for(int k=1; k<=onCutoff; k++){
				String docid = sysList.get(k-1);				
				if(sls_DocReleMap.containsKey(docid)){
					HashMap<String, HashMap<Integer, HashMap<Integer, Integer>>> slsDocReleMap = sls_DocReleMap.get(docid);
					if(slsDocReleMap.containsKey(queryid)){
						HashMap<Integer, HashMap<Integer, Integer>> slsQReleMap = slsDocReleMap.get(queryid);
						if(slsQReleMap.containsKey(flsID)){
							HashMap<Integer, Integer> slsFlsReleMap = slsQReleMap.get(flsID);
							if(slsFlsReleMap.containsKey(squareSlsID)){								
								double satPro = I(slsFlsReleMap.get(squareSlsID));								
								relesCountByNow_slsi += I(slsFlsReleMap.get(squareSlsID));								
								ap_slsi += satPro*(1.0/k)*relesCountByNow_slsi;								
							}							
						}						
					}					
				}				
			}
			
			ap_slsi = ap_slsi/cutoff;			
			return ap_slsi;			
		}else{
			System.err.println("Non-accepted setting error!");
			System.exit(0);
			return Double.NaN;	
		}				
	}
	/**
	 * For I-Aware,  compute APIA for the query
	 * For I-Square, compute APIA for a first-level subtopic
	 * **/
	private static double APIA(boolean awareEqual, String queryid, IDimension iDimension, 
			ArrayList<String> sysList, int cutoff, int squareFlsID, boolean hierarchicalDiv){
		
		TwoLevelTopic topic = _2LTMap.get(queryid);
		//intent-aware
		if(IDimension.I_AWARE == iDimension && 0>squareFlsID){	
			int flsCount = topic._flsList.size();
			
			double apIA = 0.0;
			for(int i=1; i<=flsCount; i++){
				apIA += (topic.getFlsPro(i, awareEqual)*AP_i(queryid, STLevel.FLS, i, sysList, cutoff, topic, -1));
			}
			
			return apIA;
		
		//intent-square
		}else if(IDimension.I_SQUARE == iDimension && 0<squareFlsID){		
			
			ArrayList<Pair<String, Double>> slsList = topic._slsSetList.get(squareFlsID-1);
			
			double apIA_flsi = 0.0;
			for(int slsI = 1; slsI<=slsList.size(); slsI++){
				apIA_flsi += (topic.getSlsPro(squareFlsID, slsI, awareEqual, hierarchicalDiv))*AP_i(queryid, STLevel.SLS, squareFlsID, sysList, cutoff, topic, slsI);				
			}
			
			return apIA_flsi;			
		}else{
			System.err.println("Non-accepted setting error!");
			System.exit(0);
			return Double.NaN;	
		}		
	}
	
	private static double Square_AP_Ari(String queryid, boolean squareEqual, ArrayList<String> sysList, int cutoff,
			boolean awareEqual, boolean hierarchicalDiv){
		TwoLevelTopic topic = _2LTMap.get(queryid);
		int flsCount = topic._flsList.size();
		
		double squareAP = 0.0;
		
		for(int i=1; i<=flsCount; i++){
			//System.out.println("flsPro:\t"+topic.getFlsPro(i, squareEqual));
			//System.out.println("ERRIA for "+i+":\t"+APIA(awareEqual, queryid, IDimension.I_SQUARE, sysList, cutoff, i));
			
			squareAP += topic.getFlsPro(i, squareEqual)*APIA(awareEqual, queryid, IDimension.I_SQUARE, sysList, cutoff, i, hierarchicalDiv);
			
			//System.out.println(squareAP);
			//System.out.println();
		}
		
		return squareAP;
	}
	
	////	ERR-IA	////
	/**
	 * ERR for a given first-level subtopic i
	 * 
	 * @param equal true means that subtopics are equally distributed, otherwise biased, i.e, the official probability
	 * @param flsID	first-level subtopic id
	 * @param sysList system ranking results for a specific query
	 * **/
	private static double ERRIA_i(String queryid, STLevel stLevel, int flsID, 
			ArrayList<String> sysList, int cutoff, TwoLevelTopic twoLevelTopic, int squareSlsID){
		
		if(null==sysList || sysList.size() == 0){
			return 0.0;
		}			
		
		//without enough documents provided
		int onCutoff = Math.min(sysList.size(), cutoff);
		
		if(STLevel.FLS==stLevel && 0>squareSlsID){
			double err_i = 0.0;
			double disPro = 1.0;
			
			for(int k=1; k<=onCutoff; k++){
				String docid = sysList.get(k-1);
				
				if(fls_DocReleMap.containsKey(docid)){					
					HashMap<String, HashMap<Integer, Integer>> docReleMap = fls_DocReleMap.get(docid);
					if(docReleMap.containsKey(queryid)){						
						HashMap<Integer, Integer> qReleMap = docReleMap.get(queryid);
						if(qReleMap.containsKey(flsID)){							
							double satPro = R(qReleMap.get(flsID), 2);																	
							double tem = (1.0/k)*satPro*disPro;							
							err_i += tem;							
							disPro *= (1-satPro);	
						}										
					}					
				}			
			}			
			return err_i;
		}else if(STLevel.SLS==stLevel && 0<squareSlsID){
			double err_slsi = 0.0;
			double disPro_slsi = 1.0;
			
			for(int k=1; k<=onCutoff; k++){
				String docid = sysList.get(k-1);				
				if(sls_DocReleMap.containsKey(docid)){
					HashMap<String, HashMap<Integer, HashMap<Integer, Integer>>> slsDocReleMap = sls_DocReleMap.get(docid);
					if(slsDocReleMap.containsKey(queryid)){
						HashMap<Integer, HashMap<Integer, Integer>> slsQReleMap = slsDocReleMap.get(queryid);
						if(slsQReleMap.containsKey(flsID)){
							HashMap<Integer, Integer> slsFlsReleMap = slsQReleMap.get(flsID);
							if(slsFlsReleMap.containsKey(squareSlsID)){								
								double satPro_slsi = R(slsFlsReleMap.get(squareSlsID), 2);
								//System.out.println("satPro_slsi\t"+satPro_slsi);
								double tem_slsi = (1.0/k)*satPro_slsi*disPro_slsi;
								err_slsi += tem_slsi;
								disPro_slsi *= (1-satPro_slsi);													
							}							
						}						
					}					
				}				
			}					
			return err_slsi;			
		}else{
			System.err.println("Non-accepted setting error!");
			System.exit(0);
			return Double.NaN;	
		}				
	}
	/**
	 * ERR-IA
	 * **/
	private static double ERRIA(boolean awareEqual, String queryid, IDimension iDimension, 
			ArrayList<String> sysList, int cutoff, int squareFlsID, boolean hierarchicalDiv){
		
		TwoLevelTopic topic = _2LTMap.get(queryid);
		//fls
		if(IDimension.I_AWARE == iDimension && 0>squareFlsID){			
			int flsCount = topic._flsList.size();			
			double errIA = 0.0;
			for(int i=1; i<=flsCount; i++){
				errIA += (topic.getFlsPro(i, awareEqual)*ERRIA_i(queryid, STLevel.FLS, i, sysList, cutoff, topic, -1));
			}			
			return errIA;			
		}else if(IDimension.I_SQUARE == iDimension && 0<squareFlsID){			
			ArrayList<Pair<String, Double>> slsList = topic._slsSetList.get(squareFlsID-1);	
			//
			double errIA_flsi = 0.0;
			for(int slsI = 1; slsI<=slsList.size(); slsI++){
				//System.out.println("slsPro-"+slsI+"\t"+topic.getSlsPro(squareFlsID, slsI, awareEqual));
				//System.out.println("ERRIA_i\t"+ERRIA_i(queryid, STLevel.SLS, squareFlsID, sysList, cutoff, topic, slsI));
				errIA_flsi += (topic.getSlsPro(squareFlsID, slsI, awareEqual, hierarchicalDiv))*ERRIA_i(queryid, STLevel.SLS, squareFlsID, sysList, cutoff, topic, slsI);	
				//System.out.println();
			}			
			return errIA_flsi;			
		}else{
			System.err.println("Non-accepted setting error!");
			System.exit(0);
			return Double.NaN;	
		}			
	}
	/**
	 * weighted arithmetic mean w.r.t. per-fls intrinsic diversity 
	 * **/	
	private static double Square_ERR_Ari(String queryid, boolean squareEqual, ArrayList<String> sysList, int cutoff,
			boolean awareEqual, boolean hierarchicalDiv){
		TwoLevelTopic topic = _2LTMap.get(queryid);
		int flsCount = topic._flsList.size();
		
		double squareERR_Ari = 0.0;
		
		for(int i=1; i<=flsCount; i++){
			//System.out.println("flsPro:\t"+topic.getFlsPro(i, squareEqual));
			//System.out.println("ERRIA for "+i+":\t"+ERRIA(awareEqual, queryid, IDimension.I_SQUARE, sysList, cutoff, i));
			
			squareERR_Ari += topic.getFlsPro(i, squareEqual)*ERRIA(awareEqual, queryid, IDimension.I_SQUARE, sysList, cutoff, i, hierarchicalDiv);
			
			//System.out.println(squareERR);
			//System.out.println();
		}
		
		return squareERR_Ari;
	}
	/**
	 * weighted geometric mean 
	 * **/
	private static double Square_ERR_Geo(String queryid, boolean squareEqual, ArrayList<String> sysList, int cutoff,
			boolean awareEqual, boolean hierarchicalDiv){
		TwoLevelTopic topic = _2LTMap.get(queryid);
		int flsCount = topic._flsList.size();
		
		double squareERR_Geo = 1.0;
		
		double flsWeight, perFlsDiversity;
		for(int i=1; i<=flsCount; i++){
			//System.out.println("flsPro:\t"+topic.getFlsPro(i, squareEqual));
			//System.out.println("ERRIA for "+i+":\t"+ERRIA(awareEqual, queryid, IDimension.I_SQUARE, sysList, cutoff, i));
			flsWeight = topic.getFlsPro(i, squareEqual);
			perFlsDiversity = ERRIA(awareEqual, queryid, IDimension.I_SQUARE, sysList, cutoff, i, hierarchicalDiv);
						
			squareERR_Geo += Math.pow(perFlsDiversity, flsWeight);
			
			//System.out.println(squareERR);
			//System.out.println();
		}
		
		return squareERR_Geo;
	}
	
	////	alpha-nDCg	////
	/**
	 * alpha-nDCG_i
	 * **/
	private static double alpha_DCG_i(String queryid, STLevel stLevel, int flsID, 
			ArrayList<String> sysList, int cutoff, TwoLevelTopic twoLevelTopic, int squareSlsID){
		
		if(null==sysList || sysList.size() == 0){
			return 0.0;
		}
		
		//without enough documents provided
		int onCutoff = Math.min(sysList.size(), cutoff);
		
		if(STLevel.FLS==stLevel && 0>squareSlsID){
			double  alpha_DCG_i = 0.0;
			double  countPriorReles = 0;		
			for(int k=1; k<=onCutoff; k++){
				String docid = sysList.get(k-1);
				
				if(fls_DocReleMap.containsKey(docid)){				
					HashMap<String, HashMap<Integer, Integer>> docReleMap = fls_DocReleMap.get(docid);
					if(docReleMap.containsKey(queryid)){					
						HashMap<Integer, Integer> qReleMap = docReleMap.get(queryid);
						if(qReleMap.containsKey(flsID)){						
							double satPro = I(qReleMap.get(flsID));						
							alpha_DCG_i += (1.0/(Math.log10(k+1)/Math.log10(2)))*satPro*Math.pow((1-ALPHA), countPriorReles);						
							countPriorReles += I(qReleMap.get(flsID));
						}										
					}					
				}
			}				
			return alpha_DCG_i;
		}else if(STLevel.SLS==stLevel && 0<squareSlsID){			
			double alpha_DCG_slsi = 0.0;
			double countPriorReles_slsi = 0;
			
			for(int k=1; k<=onCutoff; k++){
				String docid = sysList.get(k-1);				
				if(sls_DocReleMap.containsKey(docid)){
					HashMap<String, HashMap<Integer, HashMap<Integer, Integer>>> slsDocReleMap = sls_DocReleMap.get(docid);
					if(slsDocReleMap.containsKey(queryid)){
						HashMap<Integer, HashMap<Integer, Integer>> slsQReleMap = slsDocReleMap.get(queryid);
						if(slsQReleMap.containsKey(flsID)){
							HashMap<Integer, Integer> slsFlsReleMap = slsQReleMap.get(flsID);
							if(slsFlsReleMap.containsKey(squareSlsID)){	
								
								double satPro_slsi = I(slsFlsReleMap.get(squareSlsID));
								alpha_DCG_slsi += (1.0/(Math.log10(k+1)/Math.log10(2)))*satPro_slsi*Math.pow((1-ALPHA), countPriorReles_slsi);
								
								countPriorReles_slsi += I(slsFlsReleMap.get(squareSlsID));												
							}							
						}						
					}					
				}				
			}								
			return alpha_DCG_slsi;			
		}else{
			System.err.println("Non-accepted setting error!");
			System.exit(0);
			return Double.NaN;	
		}		
	}
	/**
	 * alpha-nDCG
	 * **/
	private static double alpha_DCG(String queryid, IDimension iDimension, 
			ArrayList<String> sysList, int cutoff, int squareFlsID){
		
		TwoLevelTopic topic = _2LTMap.get(queryid);
		//fls
		if(IDimension.I_AWARE == iDimension && 0>squareFlsID){
			int flsCount = topic._flsList.size();			
			double alpha_DCG = 0.0;
			for(int i=1; i<=flsCount; i++){
				alpha_DCG += alpha_DCG_i(queryid, STLevel.FLS, i, sysList,cutoff, topic, -1);
			}			
			return alpha_DCG;
		}else if(IDimension.I_SQUARE == iDimension && 0<squareFlsID){
			ArrayList<Pair<String, Double>> slsList = topic._slsSetList.get(squareFlsID-1);			
			double alpha_DCG_flsi = 0.0;
			for(int slsI = 1; slsI<=slsList.size(); slsI++){
				alpha_DCG_flsi += alpha_DCG_i(queryid, STLevel.SLS, squareFlsID, sysList, cutoff, topic, slsI);				
			}			
			return alpha_DCG_flsi;
			
		}else{			
			System.err.println("Non-accepted STLevel!");
			System.exit(0);
			return Double.NaN;
		}
	}
	//
	private static double alpha_n_DCG(String queryid, IDimension iDimension, 
			ArrayList<String> sysList, int cutoff, int squareFlsID){
		
		if(IDimension.I_AWARE == iDimension && 0>squareFlsID){
			
			ArrayList<String> awareIdealList = idealList_alpha_DCG(queryid, STLevel.FLS, cutoff, -1);
			
			
			if(null != awareIdealList){
				double aware_alpha_DCG = alpha_DCG(queryid, IDimension.I_AWARE, sysList, cutoff, -1);
				double aware_ideal_alpha_DCG = alpha_DCG(queryid, IDimension.I_AWARE, awareIdealList, cutoff, -1);
				
				/*
				if(Double.isNaN(aware_alpha_DCG)){
					System.err.println("aware_alpha_DCG");
				}
				if(Double.isNaN(aware_ideal_alpha_DCG) || 0 == aware_ideal_alpha_DCG){
					System.out.println(queryid);
					System.out.println(awareIdealList.size());
					System.err.println("aware_ideal_alpha_DCG");
				}
				*/
				
				return aware_alpha_DCG/aware_ideal_alpha_DCG;
			}else{
				System.err.println(queryid);
				return 0.0;
			} 
			
		}else if(IDimension.I_SQUARE == iDimension && 0<squareFlsID){
			
			double square_Alpha_DCG = alpha_DCG(queryid, IDimension.I_SQUARE, sysList, cutoff, squareFlsID);
			
			if(Double.isNaN(square_Alpha_DCG)){
				System.err.println("1");
			}
			
			ArrayList<String> squareIdealList = idealList_alpha_DCG(queryid, STLevel.SLS, cutoff, squareFlsID);
			
			if(null != squareIdealList){
				
				double ideal_Square_Alpha_DCG = alpha_DCG(queryid, IDimension.I_SQUARE, squareIdealList, cutoff, squareFlsID);
				
				/*
				//for calculating subtopic-specific ideal Alpha_DCG@20				
				//and mu_1^(*,d_2)=0.5/3.1075 for 0078 = 0.1657
				// and 0.5/4.1383=0.1208
				if(1 == squareFlsID && queryid.equals("0080")){
					System.out.println("Alpha_DCG w.r.t. IdealList:\t"+ideal_Square_Alpha_DCG);					
				}
				*/
				
				if(Double.isNaN(ideal_Square_Alpha_DCG) || 0.0==ideal_Square_Alpha_DCG){
					System.err.println("2\t"+squareIdealList.size());
				}		
				
				return square_Alpha_DCG/ideal_Square_Alpha_DCG;
				
			}else{
				
				return 0.0;
			}			
		}else{			
			System.err.println("Non-accepted STLevel!");
			System.exit(0);
			return Double.NaN;
		}		
	}
	/**
	 * weighted arithmetic mean
	 * **/
	private static double Square_alpha_n_DCG_Ari(String queryid, boolean squareEqual, ArrayList<String> sysList, int cutoff){
		TwoLevelTopic topic = _2LTMap.get(queryid);
		int flsCount = topic._flsList.size();
		
		double squareAlpha_N_DCGERR_Ari = 0.0;
		
		for(int i=1; i<=flsCount; i++){
			squareAlpha_N_DCGERR_Ari += topic.getFlsPro(i, squareEqual)*alpha_n_DCG(queryid, IDimension.I_SQUARE, sysList, cutoff, i);
		}
		
		return squareAlpha_N_DCGERR_Ari;
	}
	/**
	 * weighted geometric mean
	 * **/
	private static double Square_alpha_n_DCG_Geo(String queryid, boolean squareEqual, ArrayList<String> sysList, int cutoff){
		TwoLevelTopic topic = _2LTMap.get(queryid);
		int flsCount = topic._flsList.size();
		
		double squareAlpha_N_DCGERR_Geo = 1.0;
		
		double flsWeight, perFlsDiversity;
		for(int i=1; i<=flsCount; i++){
			flsWeight = topic.getFlsPro(i, squareEqual);
			perFlsDiversity = alpha_n_DCG(queryid, IDimension.I_SQUARE, sysList, cutoff, i);
			
			squareAlpha_N_DCGERR_Geo += Math.pow(perFlsDiversity, flsWeight);
		}
		
		return squareAlpha_N_DCGERR_Geo;
	}
	
	/**
	 * find the ideal list by greedy search
	 * **/
	private static ArrayList<String> idealList_alpha_DCG(String queryid, STLevel stLevel, int cutoff, int squareFlsID){
				
		if(STLevel.FLS==stLevel && 0>squareFlsID){
			//list of relevant doc
			ArrayList<String> releDocList = new ArrayList<String>();		
			ArrayList<String> idealList = new ArrayList<String>();		
			HashSet<String> selectedDocSet = new HashSet<String>();
			//list preparation
			for(Entry<String, HashMap<String, HashMap<Integer, Integer>>> flsEntry: fls_DocReleMap.entrySet()){				
				if(flsEntry.getValue().containsKey(queryid)){
					releDocList.add(flsEntry.getKey());
				}				
			}
			
			if(0 == releDocList.size()){
				System.out.println("queryid:"+queryid+"\tflsID:"+squareFlsID);
				return null;
			}
			
			
			TwoLevelTopic topic = _2LTMap.get(queryid);
			int flsCount = topic._flsList.size();
			//size as the count of fls, flsID corresponds to the (flsID-1)-th element
			ArrayList<Integer> priorReleCountList = new ArrayList<Integer>();
			for(int i=1; i<=flsCount; i++){
				priorReleCountList.add(0);
			}
			//buffer the releList of each doc
			HashMap<String, ArrayList<Integer>> releListMap = new HashMap<String, ArrayList<Integer>>();
			//
			int availableCut = Math.min(cutoff, releDocList.size());
			//
			while(idealList.size() < availableCut){
				ArrayList<Pair<String, Double>> desDocListByGain = new ArrayList<Pair<String,Double>>();
				
				
				for(String candidateDoc: releDocList){
					if(!selectedDocSet.contains(candidateDoc)){
						double gain = 0.0;
						
						if(releListMap.containsKey(candidateDoc)){
							ArrayList<Integer> releList = releListMap.get(candidateDoc);
							
							for(int i=1; i<=flsCount; i++){
								gain += releList.get(i-1)*(Math.pow((1-ALPHA), priorReleCountList.get(i-1)));							
							}
							
						}else{
							
							ArrayList<Integer> releList = new ArrayList<Integer>();
							for(int i=1; i<=flsCount; i++){
								int binRele = 0;
								if(fls_DocReleMap.get(candidateDoc).get(queryid).containsKey(i)){
									binRele = (int)I(fls_DocReleMap.get(candidateDoc).get(queryid).get(i));
								}
								releList.add(binRele);
								//
								gain += binRele*(Math.pow((1-ALPHA), priorReleCountList.get(i-1)));							
							}
							releListMap.put(candidateDoc, releList);
						}						
						
						desDocListByGain.add(new Pair<String, Double>(candidateDoc, gain));												
					}
				}
				
				Collections.sort(desDocListByGain, new PairComparatorBySecond_Desc<String, Double>());
				
				//the ideal one
				idealList.add(desDocListByGain.get(0).getFirst());
				selectedDocSet.add(desDocListByGain.get(0).getFirst());
				//fls-specific rele count
				ArrayList<Integer> addedReleList = releListMap.get(desDocListByGain.get(0).getFirst());
				for(int i=1; i<=flsCount; i++){
					priorReleCountList.set(i-1, addedReleList.get(i-1)+priorReleCountList.get(i-1));
				}				
			}
			
			return idealList;
			
		}else if(STLevel.SLS==stLevel && 0<squareFlsID){
			
			//list of relevant doc
			ArrayList<String> releDocList_flsi = new ArrayList<String>();		
			ArrayList<String> idealList_flsi = new ArrayList<String>();		
			HashSet<String> selectedDocSet_flsi = new HashSet<String>();
			//
			for(Entry<String, HashMap<String, HashMap<Integer, HashMap<Integer, Integer>>>> slsEntry: sls_DocReleMap.entrySet()){				
				if(slsEntry.getValue().containsKey(queryid)){
					HashMap<Integer, HashMap<Integer, Integer>> slsQReleMap = slsEntry.getValue().get(queryid);
					if(slsQReleMap.containsKey(squareFlsID)){
						releDocList_flsi.add(slsEntry.getKey());
					}					
				}				
			}
			
			//System.out.println("releDocList_flsi\t"+);
			
			if(0 == releDocList_flsi.size()){
				//System.out.println("queryid:"+queryid+"\tflsID:"+squareFlsID);
				return null;
			}
			
			TwoLevelTopic topic = _2LTMap.get(queryid);
			ArrayList<Pair<String, Double>> slsList_flsi = topic._slsSetList.get(squareFlsID-1);
			int slsCount = slsList_flsi.size();
			//size as the count of sls w.r.t. the given flsID, flsID corresponds to the (flsID-1)-th element
			ArrayList<Integer> priorReleCountList_flsi = new ArrayList<Integer>();
			for(int i=1; i<=slsCount; i++){
				priorReleCountList_flsi.add(0);
			}
			//buffer the releList of each doc
			HashMap<String, ArrayList<Integer>> releListMap_flsi = new HashMap<String, ArrayList<Integer>>();
			//
			int availableCut = Math.min(cutoff, releDocList_flsi.size());
			//
			while(selectedDocSet_flsi.size() < availableCut){
				ArrayList<Pair<String, Double>> desDocListByGain_flsi = new ArrayList<Pair<String,Double>>();
				
				
				for(String candidateDoc_flsi: releDocList_flsi){
					if(!selectedDocSet_flsi.contains(candidateDoc_flsi)){
						double gain_flsi = 0.0;
						
						if(releListMap_flsi.containsKey(candidateDoc_flsi)){
							ArrayList<Integer> releList_flsi = releListMap_flsi.get(candidateDoc_flsi);
							
							for(int slsI=1; slsI<=slsCount; slsI++){
								gain_flsi += releList_flsi.get(slsI-1)*(Math.pow((1-ALPHA), priorReleCountList_flsi.get(slsI-1)));							
							}
							
						}else{
							
							ArrayList<Integer> releList_flsi = new ArrayList<Integer>();
							for(int slsI=1; slsI<=slsCount; slsI++){
								int binRele = 0;
								if(sls_DocReleMap.get(candidateDoc_flsi).get(queryid).get(squareFlsID).containsKey(slsI)){
									binRele = (int)I(sls_DocReleMap.get(candidateDoc_flsi).get(queryid).get(squareFlsID).get(slsI));
								}
								releList_flsi.add(binRele);
								//
								gain_flsi += binRele*(Math.pow((1-ALPHA), priorReleCountList_flsi.get(slsI-1)));							
							}
							releListMap_flsi.put(candidateDoc_flsi, releList_flsi);
						}						
						
						desDocListByGain_flsi.add(new Pair<String, Double>(candidateDoc_flsi, gain_flsi));												
					}
				}
				
				Collections.sort(desDocListByGain_flsi, new PairComparatorBySecond_Desc<String, Double>());
				
				//the ideal one
				idealList_flsi.add(desDocListByGain_flsi.get(0).getFirst());
				selectedDocSet_flsi.add(desDocListByGain_flsi.get(0).getFirst());
				
				//sls-specific rele count w.r.t. the given squareFlsID
				ArrayList<Integer> addedReleList = releListMap_flsi.get(desDocListByGain_flsi.get(0).getFirst());
				for(int slsI=1; slsI<=slsCount; slsI++){
					priorReleCountList_flsi.set(slsI-1, addedReleList.get(slsI-1)+priorReleCountList_flsi.get(slsI-1));
				}				
			}
			
			return idealList_flsi;
			
		}else{			
			System.err.println("Non-accepted STLevel!");
			System.exit(0);
			return null;
		}		
	}
	
	////	D#-nDCG	////
	private static double D_Sharp_n_DCG(boolean awareEqual, String queryid, IDimension iDimension, 
			ArrayList<String> sysList, int cutoff, int squareFlsID, boolean hierarchicalDiv){
		
		if(IDimension.I_AWARE == iDimension && 0>squareFlsID){
			double sRecall = SRecall(queryid, iDimension, sysList, cutoff, squareFlsID);
			
			ArrayList<String> idealList = idealList_D_Sharp_DCG(awareEqual, queryid, STLevel.FLS, cutoff, -1, hierarchicalDiv);
			
			if(null != idealList){
				double d_n_DCG = D_Sharp_DCG(awareEqual, queryid, iDimension, sysList, cutoff, squareFlsID, hierarchicalDiv)/D_Sharp_DCG(
						awareEqual, queryid, iDimension, idealList, cutoff, squareFlsID, hierarchicalDiv);
				
				//System.out.println("D-nDCG:\t"+d_n_DCG);
				
				return GAMA*sRecall + (1-GAMA)*d_n_DCG;
			}else{
				System.err.println(queryid);
				return 0.0;
			}			
		}else if(IDimension.I_SQUARE == iDimension && 0<squareFlsID){
			
			double sRecall_flsi = SRecall(queryid, iDimension, sysList, cutoff, squareFlsID);
			
			ArrayList<String> idealList_flsi = idealList_D_Sharp_DCG(awareEqual, queryid, STLevel.SLS, cutoff, squareFlsID, hierarchicalDiv);
			
			if(null != idealList_flsi){
				double d_Sharp_n_DCG_flsi = D_Sharp_DCG(awareEqual, queryid, iDimension, sysList, cutoff, squareFlsID, hierarchicalDiv)/D_Sharp_DCG(
						awareEqual, queryid, iDimension, idealList_flsi, cutoff, squareFlsID, hierarchicalDiv);
				
				return GAMA*sRecall_flsi + (1-GAMA)*d_Sharp_n_DCG_flsi;
			
			}else{
				return 0.0;
			}
			
			
			
		}else{			
			System.err.println("Non-accepted STLevel!");
			System.exit(0);
			return Double.NaN;
		}				
	}	
	
	private static double Square_D_Sharp_n_DCG_Ari(String queryid, boolean squareEqual, ArrayList<String> sysList, int cutoff,
			boolean awareEqual, boolean hierarchicalDiv){
		TwoLevelTopic topic = _2LTMap.get(queryid);
		int flsCount = topic._flsList.size();
		
		double squareDSharpNDCG = 0.0;
		
		for(int i=1; i<=flsCount; i++){
			squareDSharpNDCG += topic.getFlsPro(i, squareEqual)*D_Sharp_n_DCG(awareEqual, queryid, IDimension.I_SQUARE, sysList, cutoff, i, hierarchicalDiv);
		}
		
		return squareDSharpNDCG;
	}
	
	private static double D_Sharp_DCG(boolean awareEqual, String queryid, IDimension iDimension, 
			ArrayList<String> sysList, int cutoff, int squareFlsID, boolean hierarchicalDiv){
		
		TwoLevelTopic topic = _2LTMap.get(queryid);
		//fls
		if(IDimension.I_AWARE == iDimension && 0>squareFlsID){		
			
			int flsCount = topic._flsList.size();
			
			double d_Sharp_DCG = 0.0;
			for(int i=1; i<=flsCount; i++){
				d_Sharp_DCG += (topic.getFlsPro(i, awareEqual)*D_Sharp_DCG_i(queryid, STLevel.FLS, i, sysList, cutoff, topic, -1));
			}
			
			return d_Sharp_DCG;
			
		}else if(IDimension.I_SQUARE == iDimension && 0<squareFlsID){
			
			ArrayList<Pair<String, Double>> slsList = topic._slsSetList.get(squareFlsID-1);			
			double d_Sharp_DCG_flsi = 0.0;
			for(int slsI = 1; slsI<=slsList.size(); slsI++){
				d_Sharp_DCG_flsi += (topic.getSlsPro(squareFlsID, slsI, awareEqual, hierarchicalDiv))*D_Sharp_DCG_i(queryid, STLevel.SLS, squareFlsID, sysList, cutoff, topic, slsI);				
			}			
			return d_Sharp_DCG_flsi;			
			
		}else{			
			System.err.println("Non-accepted STLevel!");
			System.exit(0);
			return Double.NaN;
		}
	}
	
	private static double D_Sharp_DCG_i(String queryid, STLevel stLevel, int flsID, 
			ArrayList<String> sysList, int cutoff, TwoLevelTopic twoLevelTopic, int squareSlsID){
		
		if(null==sysList || sysList.size() == 0){
			return 0.0;
		}
		
		//without enough documents provided
		int onCutoff = Math.min(sysList.size(), cutoff);
		
		if(STLevel.FLS==stLevel && 0>squareSlsID){
			double  d_Sharp_DCG_i = 0.0;			
			for(int k=1; k<=onCutoff; k++){
				String docid = sysList.get(k-1);				
				if(fls_DocReleMap.containsKey(docid)){					
					HashMap<String, HashMap<Integer, Integer>> docReleMap = fls_DocReleMap.get(docid);
					if(docReleMap.containsKey(queryid)){						
						HashMap<Integer, Integer> qReleMap = docReleMap.get(queryid);
						if(qReleMap.containsKey(flsID)){							
							double gain = V(qReleMap.get(flsID));							
							d_Sharp_DCG_i += (1.0/(Math.log10(k+1)/Math.log10(2)))*gain;							
						}										
					}					
				}
			}			
			return d_Sharp_DCG_i;
			
		}else if(STLevel.SLS==stLevel && 0<squareSlsID){
			
			double d_Sharp_DCG_slsi = 0.0;
			
			for(int k=1; k<=onCutoff; k++){
				String docid = sysList.get(k-1);				
				if(sls_DocReleMap.containsKey(docid)){
					HashMap<String, HashMap<Integer, HashMap<Integer, Integer>>> slsDocReleMap = sls_DocReleMap.get(docid);
					if(slsDocReleMap.containsKey(queryid)){
						HashMap<Integer, HashMap<Integer, Integer>> slsQReleMap = slsDocReleMap.get(queryid);
						if(slsQReleMap.containsKey(flsID)){
							HashMap<Integer, Integer> slsFlsReleMap = slsQReleMap.get(flsID);
							if(slsFlsReleMap.containsKey(squareSlsID)){	
								double gain = V(slsFlsReleMap.get(squareSlsID));
								d_Sharp_DCG_slsi += (1.0/(Math.log10(k+1)/Math.log10(2)))*gain;										
							}							
						}						
					}					
				}				
			}					
			return d_Sharp_DCG_slsi;
			
		}else{			
			System.err.println("Non-accepted STLevel!");
			System.exit(0);
			return Double.NaN;
		}			
	}
	//	
	/** 
	 * find the ideal list by greedy search
	 * **/
	private static ArrayList<String> idealList_D_Sharp_DCG(boolean awareEqual, String queryid, STLevel stLevel, int cutoff,
			int squareFlsID, boolean hierarchicalDiv){
		if(STLevel.FLS==stLevel && 0>squareFlsID){
			//list of relevant doc
			ArrayList<String> releDocList = new ArrayList<String>();		
			ArrayList<String> idealList = new ArrayList<String>();
			
			TwoLevelTopic topic = _2LTMap.get(queryid);
			int flsCount = topic._flsList.size();
			
			//list preparation
			for(Entry<String, HashMap<String, HashMap<Integer, Integer>>> flsEntry: fls_DocReleMap.entrySet()){				
				if(flsEntry.getValue().containsKey(queryid)){
					releDocList.add(flsEntry.getKey());
				}				
			}
			
			if(0 == releDocList.size()){
				return null;
			}
			
			ArrayList<Pair<String, Double>> desDocListByGG = new ArrayList<Pair<String,Double>>();
			
			for(String releDoc: releDocList){
				double globalGain = 0.0;
				
				for(int i=1; i<=flsCount; i++){					
					if(fls_DocReleMap.get(releDoc).get(queryid).containsKey(i)){
						globalGain += (topic.getFlsPro(i, awareEqual))*V(fls_DocReleMap.get(releDoc).get(queryid).get(i));
					}					
				}
				
				desDocListByGG.add(new Pair<String, Double>(releDoc, globalGain));
			}
			
			Collections.sort(desDocListByGG, new PairComparatorBySecond_Desc<String, Double>());
			
			int availableCutoff = Math.min(cutoff, desDocListByGG.size());
			
			for(int k=1; k<=availableCutoff; k++){
				idealList.add(desDocListByGG.get(k-1).getFirst());
			}
			
			return idealList;
			
		}else if(STLevel.SLS==stLevel && 0<squareFlsID){
			
			//list of relevant doc
			ArrayList<String> releDocList_flsi = new ArrayList<String>();		
			ArrayList<String> idealList_flsi = new ArrayList<String>();
			
			TwoLevelTopic topic = _2LTMap.get(queryid);
			int flsCount = topic._flsList.size();
			
			//list preparation
			for(Entry<String, HashMap<String, HashMap<Integer, HashMap<Integer, Integer>>>> slsEntry: sls_DocReleMap.entrySet()){				
				if(slsEntry.getValue().containsKey(queryid)){
					HashMap<Integer, HashMap<Integer, Integer>> slsQReleMap = slsEntry.getValue().get(queryid);
					if(slsQReleMap.containsKey(squareFlsID)){
						releDocList_flsi.add(slsEntry.getKey());
					}					
				}				
			}
			
			if(0 == releDocList_flsi.size()){
				return null;
			}
			
			ArrayList<Pair<String, Double>> desDocListByGG_flsi = new ArrayList<Pair<String,Double>>();
			
			ArrayList<Pair<String, Double>> slsList_flsi = topic._slsSetList.get(squareFlsID-1);
			int slsCount = slsList_flsi.size();
			
			for(String releDoc_flsi: releDocList_flsi){
				double globalGain_flsi = 0.0;
				
				for(int slsI=1; slsI<=slsCount; slsI++){					
					if(sls_DocReleMap.get(releDoc_flsi).get(queryid).get(squareFlsID).containsKey(slsI)){
						globalGain_flsi += (topic.getSlsPro(squareFlsID, slsI, awareEqual, hierarchicalDiv))*V(sls_DocReleMap.get(releDoc_flsi).get(queryid).get(squareFlsID).get(slsI));
					}					
				}
				
				desDocListByGG_flsi.add(new Pair<String, Double>(releDoc_flsi, globalGain_flsi));
			}
			
			Collections.sort(desDocListByGG_flsi, new PairComparatorBySecond_Desc<String, Double>());
			
			int availableCutoff = Math.min(cutoff, desDocListByGG_flsi.size());
			
			for(int k=1; k<=availableCutoff; k++){
				idealList_flsi.add(desDocListByGG_flsi.get(k-1).getFirst());
			}
			
			return idealList_flsi;
			
		}else{			
			System.err.println("Non-accepted STLevel!");
			System.exit(0);
			return null;
		}	
	}
	
	/**
	 * @param infFlsSet_Map queryID-> infFls set 
	 * **/
	private static double DIN_Sharp_DCG_i(HashMap<String, HashSet<Integer>> infFlsSet_Map, String queryid, int flsID,
			ArrayList<String> sysList, int cutoff, TwoLevelTopic twoLevelTopic){
		
		if(null==sysList || sysList.size() == 0){
			return 0.0;
		}		
		//without enough documents provided
		int onCutoff = Math.min(sysList.size(), cutoff);
		HashSet<Integer> infFlsSet = infFlsSet_Map.get(queryid);
		
		double  d_Sharp_DCG_i = 0.0;
		
		if(infFlsSet.contains(flsID)){
			for(int k=1; k<=onCutoff; k++){
				String docid = sysList.get(k-1);				
				if(fls_DocReleMap.containsKey(docid)){					
					HashMap<String, HashMap<Integer, Integer>> docReleMap = fls_DocReleMap.get(docid);
					if(docReleMap.containsKey(queryid)){						
						HashMap<Integer, Integer> qReleMap = docReleMap.get(queryid);
						if(qReleMap.containsKey(flsID)){							
							double gain = V(qReleMap.get(flsID));							
							d_Sharp_DCG_i += (1.0/(Math.log10(k+1)/Math.log10(2)))*gain;							
						}										
					}					
				}
			}			
			return d_Sharp_DCG_i;
		}else{
			for(int k=1; k<=onCutoff; k++){
				String docid = sysList.get(k-1);				
				if(fls_DocReleMap.containsKey(docid)){					
					HashMap<String, HashMap<Integer, Integer>> docReleMap = fls_DocReleMap.get(docid);
					if(docReleMap.containsKey(queryid)){						
						HashMap<Integer, Integer> qReleMap = docReleMap.get(queryid);
						if(qReleMap.containsKey(flsID)){							
							double gain = V(qReleMap.get(flsID));							
							d_Sharp_DCG_i += (1.0/(Math.log10(k+1)/Math.log10(2)))*gain;	
							
							return d_Sharp_DCG_i;
						}										
					}					
				}
			}
			
			return d_Sharp_DCG_i;
		}		
	}
	//
	private static double DIN_Sharp_DCG(boolean awareEqual, HashMap<String, HashSet<Integer>> infFlsSet_Map, String queryid, ArrayList<String> sysList, int cutoff){
		TwoLevelTopic topic = _2LTMap.get(queryid);
		
		int flsCount = topic._flsList.size();
		
		double din_Sharp_DCG = 0.0;
		for(int i=1; i<=flsCount; i++){
			din_Sharp_DCG += (topic.getFlsPro(i, awareEqual)*DIN_Sharp_DCG_i(infFlsSet_Map, queryid, i, sysList, cutoff, topic));
		}
		
		return din_Sharp_DCG;
	}
	//
	private static double DIN_Sharp_nDCG(boolean awareEqual, HashMap<String, HashSet<Integer>> infFlsSet_Map,
			String queryid, ArrayList<String> sysList, int cutoff, boolean hierarchicalDiv){
		
		double sRecall = SRecall(queryid, IDimension.I_AWARE, sysList, cutoff, -1);
		
		ArrayList<String> idealList = idealList_D_Sharp_DCG(awareEqual, queryid, STLevel.FLS, cutoff, -1, hierarchicalDiv);
		
		if(null != idealList){
			double din_n_DCG = DIN_Sharp_DCG(awareEqual, infFlsSet_Map, queryid, sysList, cutoff)/D_Sharp_DCG(
					awareEqual, queryid, IDimension.I_AWARE, idealList, cutoff, -1, hierarchicalDiv);
			
			System.out.println("DIN-nDCG:\t"+din_n_DCG);
			
			return ETA*sRecall + (1-ETA)*din_n_DCG;
		}else{
			System.err.println(queryid);
			return 0.0;
		}			
		
	}
	
	////	ERRIA^{net}	////
	private static double ERRIA_Net(boolean awareEqual, String queryid, ArrayList<String> sysList, int cutoff){
		
		TwoLevelTopic topic = _2LTMap.get(queryid);
		
		int flsCount = topic._flsList.size();	
		
		double errIA_Net = 0.0;
		for(int i=1; i<=flsCount; i++){
			errIA_Net += (topic.getFlsPro(i, awareEqual)*ERRIA_Net_i(queryid, i, sysList, cutoff, topic));
		}			
		return errIA_Net;		
	}
	
	public static double ERRIA_Net_i(String queryid, int flsID, ArrayList<String> sysList,
			int cutoff, TwoLevelTopic twoLevelTopic){
		
		if(null==sysList || sysList.size() == 0){
			return 0.0;
		}
		
		//used as L^{k-1}={d_1,...,d_{k-1}}
		HashSet<String> slsPool = new HashSet<String>();
		
		double err = 0.0;
		//without enough documents provided	
		int acceptedCut = Math.min(sysList.size(), cutoff);
		
		for(int i=0; i<acceptedCut; i++){
			String docid = sysList.get(i);
			
			if(netDocUsageMap.containsKey(docid)){
				
				HashMap<String, HashSet<String>> qMap = netDocUsageMap.get(docid);
				if(qMap.containsKey(queryid)){
					HashSet<String> slsSet = qMap.get(queryid);
					
					//topic units covered by the current document
					HashSet<String> tempSlsSet = new HashSet<String>();
					tempSlsSet.addAll(slsSet);
					
					//novel topic units covered by the current document
					slsSet.removeAll(slsPool);	
					
					// if some nuggets are not relevant, it also means that some topic units have not happened,
					// thus the relevance probability of subsequent documents will be changed, 
					// e.g., for the documents including novel nuggets will be more relevant!!!
					double satPro = twoLevelTopic.getRelePro(flsID, slsSet);
					double tem = 1.0/(i+1)*satPro;
					
					err += tem;
					//
					slsPool.addAll(tempSlsSet);
				}
				
			}
		}
		
		return err;		
	}
	
	
	
	
	//test intent-aware metrics
	public static void test(boolean awareEqual){
		boolean hierarchicalDiv = true;
		
		String queryid = "0001";
		//topic
		TwoLevelTopic aTwoLevelTopic = new TwoLevelTopic(queryid, "test");
		
		ArrayList<Pair<String, Double>> flsList = new ArrayList<Pair<String,Double>>();
		if(awareEqual){
			flsList.add(new Pair<String, Double>("i", 0.5));
			flsList.add(new Pair<String, Double>("j", 0.5));
		}else{
			flsList.add(new Pair<String, Double>("i", 0.7));
			flsList.add(new Pair<String, Double>("j", 0.3));
		}
		
		aTwoLevelTopic.setFlsList(flsList);
		
		_2LTMap.put("0001", aTwoLevelTopic);
		
		//doc
		fls_DocReleMap = new HashMap<String, HashMap<String,HashMap<Integer,Integer>>>();
		
		//doc_1		
		String doc_1 = "doc_1";
		HashMap<Integer,Integer> qMap_1 = new HashMap<Integer, Integer>();
		qMap_1.put(1, 1);
		
		HashMap<String,HashMap<Integer,Integer>> docMap_1 = new HashMap<String, HashMap<Integer,Integer>>();
		docMap_1.put(queryid, qMap_1);
		
		fls_DocReleMap.put(doc_1, docMap_1);
		
		//doc_2		
		String doc_2 = "doc_2";
		HashMap<Integer,Integer> qMap_2 = new HashMap<Integer, Integer>();
		qMap_2.put(1, 1);
		
		HashMap<String,HashMap<Integer,Integer>> docMap_2 = new HashMap<String, HashMap<Integer,Integer>>();
		docMap_2.put(queryid, qMap_2);
		
		fls_DocReleMap.put(doc_2, docMap_2);
		
		
		//doc_3
		String doc_3 = "doc_3";
		HashMap<Integer,Integer> qMap_3 = new HashMap<Integer, Integer>();
		qMap_3.put(2, 1);
		
		HashMap<String,HashMap<Integer,Integer>> docMap_3 = new HashMap<String, HashMap<Integer,Integer>>();
		docMap_3.put(queryid, qMap_3);
		
		fls_DocReleMap.put(doc_3, docMap_3);
		
		ArrayList<String> rankedList = new ArrayList<String>();
		rankedList.add(doc_1);
		rankedList.add(doc_2);
		rankedList.add(doc_3);
		
		//
		int cutoff = 3;
		System.out.println("SRecall:\t"+SRecall(queryid, IDimension.I_AWARE, rankedList, cutoff, -1));
		System.out.println("APIA:\t"+APIA(awareEqual, queryid, IDimension.I_AWARE, rankedList, cutoff, -1, false));
		System.out.println("ERRIA:\t"+ERRIA(awareEqual, queryid, IDimension.I_AWARE, rankedList, cutoff, -1, false));
		System.out.println("alpha_n_DCG:\t"+alpha_n_DCG(queryid, IDimension.I_AWARE, rankedList, cutoff, -1));
		System.out.println("D#-nDCG:\t"+D_Sharp_n_DCG(awareEqual, queryid, IDimension.I_AWARE, rankedList, cutoff, -1, false));
		//System.out.println(":\t"+);	
		
		
	}
	
	//test intent-square metrics
	private static enum DivCase{Case_1, Case_2, Case_3};
	//
	public static void caseCal(boolean awareEqual, boolean squareEqual, DivCase divCase){
		
		///////
		//topic construction
		///////
		String queryid = "0001";
		TwoLevelTopic aTwoLevelTopic = new TwoLevelTopic(queryid, "test");
		
		ArrayList<Pair<String, Double>> flsList = new ArrayList<Pair<String,Double>>();
		if(squareEqual){
			flsList.add(new Pair<String, Double>("i", 0.5));
			flsList.add(new Pair<String, Double>("j", 0.5));
		}else{
			flsList.add(new Pair<String, Double>("i", 0.7));
			flsList.add(new Pair<String, Double>("j", 0.3));
		}
		
		aTwoLevelTopic.setFlsList(flsList);
		
		ArrayList<ArrayList<Pair<String, Double>>> slsSetList = new ArrayList<ArrayList<Pair<String,Double>>>();
		ArrayList<Pair<String, Double>> slsList_i = new ArrayList<Pair<String,Double>>();
		slsList_i.add(new Pair<String, Double>("o1", 0.25));
		slsList_i.add(new Pair<String, Double>("o2", 0.25));
		slsList_i.add(new Pair<String, Double>("o3", 0.25));
		slsList_i.add(new Pair<String, Double>("o4", 0.25));
		ArrayList<Pair<String, Double>> slsList_j = new ArrayList<Pair<String,Double>>();
		slsList_j.add(new Pair<String, Double>("e1", 1.0));
		
		slsSetList.add(slsList_i);
		slsSetList.add(slsList_j);
		
		aTwoLevelTopic.setSlsSetList(slsSetList);		
		_2LTMap.put("0001", aTwoLevelTopic);
		
		////doc relevant map
		fls_DocReleMap = new HashMap<String, HashMap<String,HashMap<Integer,Integer>>>();
		sls_DocReleMap = new HashMap<String, HashMap<String,HashMap<Integer,HashMap<Integer,Integer>>>>();
		
		////documents
		//d_1
		String doc_1 = "doc_1";
		//fls
		HashMap<String, HashMap<Integer,Integer>> flsDocReleMap_1 = new HashMap<String, HashMap<Integer,Integer>>();
		HashMap<Integer,Integer> flsQReleMap_1 = new HashMap<Integer, Integer>();
		//sls
		HashMap<String,HashMap<Integer,HashMap<Integer,Integer>>> slsDocReleMap_1 = new HashMap<String, HashMap<Integer,HashMap<Integer,Integer>>>();
		HashMap<Integer, HashMap<Integer,Integer>> slsQReleMap_1 = new HashMap<Integer, HashMap<Integer,Integer>>();
		HashMap<Integer,Integer> slsFlsReleMap_1 = new HashMap<Integer, Integer>();
		
		//d_2
		String doc_2 = "doc_2";
		//fls
		HashMap<String, HashMap<Integer,Integer>> flsDocReleMap_2 = new HashMap<String, HashMap<Integer,Integer>>();
		HashMap<Integer,Integer> flsQReleMap_2 = new HashMap<Integer, Integer>();
		//sls
		HashMap<String,HashMap<Integer,HashMap<Integer,Integer>>> slsDocReleMap_2 = new HashMap<String, HashMap<Integer,HashMap<Integer,Integer>>>();
		HashMap<Integer, HashMap<Integer,Integer>> slsQReleMap_2 = new HashMap<Integer, HashMap<Integer,Integer>>();
		HashMap<Integer,Integer> slsFlsReleMap_2 = new HashMap<Integer, Integer>();
		
		//d_3
		String doc_3 = "doc_3";
		//fls
		HashMap<String, HashMap<Integer,Integer>> flsDocReleMap_3 = new HashMap<String, HashMap<Integer,Integer>>();
		HashMap<Integer,Integer> flsQReleMap_3 = new HashMap<Integer, Integer>();
		//sls
		HashMap<String,HashMap<Integer,HashMap<Integer,Integer>>> slsDocReleMap_3 = 
				new HashMap<String, HashMap<Integer,HashMap<Integer,Integer>>>();
		HashMap<Integer, HashMap<Integer,Integer>> slsQReleMap_3 = new HashMap<Integer, HashMap<Integer,Integer>>();
		HashMap<Integer,Integer> slsFlsReleMap_3 = new HashMap<Integer, Integer>();
		
		////cases of relevant judgments
		//d_1 fls setting:	the same for case-1, case-2, case-3
		flsQReleMap_1.put(1, 1);
		flsDocReleMap_1.put(queryid, flsQReleMap_1);
		//d_1 sls setting:	the same for case-1, case-2, case-3
		slsFlsReleMap_1.put(1, 1);
		slsFlsReleMap_1.put(2, 1);
		slsQReleMap_1.put(1, slsFlsReleMap_1);		
		slsDocReleMap_1.put(queryid, slsQReleMap_1);
		//d_2 fls setting: the same for case-1, case-2, case-3
		flsQReleMap_2.put(1, 1);
		flsDocReleMap_2.put(queryid, flsQReleMap_2);
		
		if(DivCase.Case_1 == divCase){
			//d_2 sls the same for case-1 and case-3
			slsFlsReleMap_2.put(1, 1);
			slsFlsReleMap_2.put(2, 1);
			slsQReleMap_2.put(1, slsFlsReleMap_2);		
			slsDocReleMap_2.put(queryid, slsQReleMap_2);
			//d_3 fls setting, the same for case-1 and case-2
			flsQReleMap_3.put(1, 1);
			flsDocReleMap_3.put(queryid, flsQReleMap_3);
			//d_3 sls setting:	the same for case-1 and case-2
			slsFlsReleMap_3.put(3, 1);
			slsQReleMap_3.put(1, slsFlsReleMap_3);		
			slsDocReleMap_3.put(queryid, slsQReleMap_3);			
		}else if(DivCase.Case_2 == divCase){
			//d_2 sls setting: different from case-1 and case-3
			slsFlsReleMap_2.put(3, 1);
			slsFlsReleMap_2.put(4, 1);
			slsQReleMap_2.put(1, slsFlsReleMap_2);		
			slsDocReleMap_2.put(queryid, slsQReleMap_2);
			
			//d_3 fls setting, the same for case-1 and case-2
			flsQReleMap_3.put(1, 1);
			flsDocReleMap_3.put(queryid, flsQReleMap_3);
			//d_3 sls setting:	the same for case-1 and case-2
			slsFlsReleMap_3.put(3, 1);
			slsQReleMap_3.put(1, slsFlsReleMap_3);		
			slsDocReleMap_3.put(queryid, slsQReleMap_3);			
		}else if(DivCase.Case_3 == divCase){
			//d_2 sls the same for case-1 and case-3
			slsFlsReleMap_2.put(1, 1);
			slsFlsReleMap_2.put(2, 1);
			slsQReleMap_2.put(1, slsFlsReleMap_2);		
			slsDocReleMap_2.put(queryid, slsQReleMap_2);
			
			//d_3 fls setting, different from case-1 and case-2
			flsQReleMap_3.put(2, 1);
			flsDocReleMap_3.put(queryid, flsQReleMap_3);
			//d_3 sls setting:	different from case-1 and case-2
			slsFlsReleMap_3.put(1, 1);
			slsQReleMap_3.put(2, slsFlsReleMap_3);		
			slsDocReleMap_3.put(queryid, slsQReleMap_3);			
		}else{
			System.err.println("Non-accepted setting error!");
			System.exit(0);
		}		
		////
		//doc_1
		////		
		fls_DocReleMap.put(doc_1, flsDocReleMap_1);		
		sls_DocReleMap.put(doc_1, slsDocReleMap_1);		
		////
		//doc_2		
		////		
		fls_DocReleMap.put(doc_2, flsDocReleMap_2);		
		sls_DocReleMap.put(doc_2, slsDocReleMap_2);		
		////
		//doc_3
		////		
		fls_DocReleMap.put(doc_3, flsDocReleMap_3);		
		sls_DocReleMap.put(doc_3, slsDocReleMap_3);
		
		//
		HashMap<String, HashSet<Integer>> infFlsSet_Map = new HashMap<String, HashSet<Integer>>();
		HashSet<Integer> infFlsSet = new HashSet<Integer>();
		infFlsSet.add(1);
		infFlsSet_Map.put(queryid, infFlsSet);
		
		//global ranked list
		ArrayList<String> rankedList = new ArrayList<String>();
		rankedList.add(doc_1);
		rankedList.add(doc_2);
		rankedList.add(doc_3);
		
		//
		int cutoff = 3;
		System.out.println("##Case-Specific Study: "+divCase.toString()+"##");
		System.out.println();
		System.out.println("SRecall:\t"+resultFormat.format(SRecall(queryid, IDimension.I_AWARE, rankedList, cutoff, -1)));
		System.out.println();
		System.out.println("AP-IA:\t"+resultFormat.format(APIA(awareEqual, queryid, IDimension.I_AWARE, rankedList, cutoff, -1, false)));
		System.out.println();
		System.out.println("ERR-IA:\t"+resultFormat.format(ERRIA(awareEqual, queryid, IDimension.I_AWARE, rankedList, cutoff, -1, false)));
		System.out.println();
		System.out.println("Alpha_n_DCG:\t"+resultFormat.format(alpha_n_DCG(queryid, IDimension.I_AWARE, rankedList, cutoff, -1)));
		System.out.println();
		System.out.println("D_Sharp_n_DCG:\t"+resultFormat.format(D_Sharp_n_DCG(awareEqual, queryid, IDimension.I_AWARE, rankedList, cutoff, -1, false)));
		System.out.println();
		System.out.println("DIN_Sharp_n_DCG:\t"+resultFormat.format(DIN_Sharp_nDCG(awareEqual, infFlsSet_Map, queryid, rankedList, cutoff, false)));
		System.out.println();
		System.out.println("Square-SRecall:\t"+resultFormat.format(Square_SRecall(queryid, squareEqual, rankedList, cutoff)));
		System.out.println();
		//System.out.println("Square-APIA:\t"+resultFormat.format(Square_AP(queryid, squareEqual, rankedList, cutoff, awareEqual)));
		//System.out.println();
		//System.out.println("Square-ERRIA:\t"+resultFormat.format(Square_ERR(queryid, squareEqual, rankedList, cutoff, true)));
		System.out.println();
		//System.out.println("Square-alpha_n_DCG:\t"+resultFormat.format(Square_alpha_n_DCG(queryid, squareEqual, rankedList, cutoff, true)));
		System.out.println();
		//System.out.println("Square-D#-nDCG:\t"+resultFormat.format(Square_D_Sharp_n_DCG(queryid, squareEqual, rankedList, cutoff, awareEqual)));
		
	}
	//
	//count of repeated relevant documents w.r.t. a specific sls
	private static double getAvgReReleDoc_flsi(String queryid, int cutoff, int flsID, ArrayList<String> sysList){
		//
		TwoLevelTopic topic = _2LTMap.get(queryid);
		ArrayList<Pair<String, Double>> slsList_flsi = topic._slsSetList.get(flsID-1);
		int slsCount = slsList_flsi.size();
		
		//size as the count of sls w.r.t. the given flsID, flsID corresponds to the (flsID-1)-th element
		ArrayList<Integer> priorReleCountList_flsi = new ArrayList<Integer>();
		for(int i=1; i<=slsCount; i++){
			priorReleCountList_flsi.add(0);
		}
		
		if(null==sysList || sysList.size()==0){
			return 0.0;
		}
		
		int availableCut = Math.min(cutoff, sysList.size());
		
		for(int k=0; k<availableCut; k++){
			String docid = sysList.get(k);
			
			ArrayList<Integer> releList_flsi = new ArrayList<Integer>();
			
			if(sls_DocReleMap.containsKey(docid)){
				HashMap<String, HashMap<Integer, HashMap<Integer, Integer>>> slsDocReleMap = sls_DocReleMap.get(docid);
				if(slsDocReleMap.containsKey(queryid)){
					HashMap<Integer, HashMap<Integer, Integer>> slsQReleMap = slsDocReleMap.get(queryid);
					if(slsQReleMap.containsKey(flsID)){
						HashMap<Integer, Integer> slsFlsReleMap = slsQReleMap.get(flsID);						
						for(int slsI=1; slsI<=slsCount; slsI++){
							int binRele = 0;
							if(slsFlsReleMap.containsKey(slsI)){
								binRele = (int)I(slsFlsReleMap.get(slsI));
							}
							releList_flsi.add(binRele);
						}												
					}
				}
			}
			//refresh count of relevant documents
			if(releList_flsi.size() > 0){
				for(int slsI=1; slsI<=slsCount; slsI++){
					priorReleCountList_flsi.set(slsI-1, releList_flsi.get(slsI-1)+priorReleCountList_flsi.get(slsI-1));
				}
			}			
		}	
		/*
		for(int slsI=1; slsI<=slsCount; slsI++){
			int releCountByNow = priorReleCountList_flsi.get(slsI-1);
			if(releCountByNow > 0){
				priorReleCountList_flsi.set(slsI-1, releCountByNow-1);
			}
		}
		*/
		//get average redundant relevant documents per sls
		double reReleDocSum = 0.0;
		for(int slsI=1; slsI<=slsCount; slsI++){
			int releCountByNow = priorReleCountList_flsi.get(slsI-1);
			if(releCountByNow > 1){
				reReleDocSum += (releCountByNow-1);
			}
		}
		double avgReReleDoc_flsi = reReleDocSum/slsCount;
		
		return avgReReleDoc_flsi;		
	}
	//
	private static double getAvgReReleDoc(String queryid, int cutoff, ArrayList<String> sysList){
		TwoLevelTopic topic = _2LTMap.get(queryid);
		int flsCount = topic._flsList.size();
		//overall average redundant relevant documents per sls throughout a query
		double overallAvgReReleDoc = 0.0;		
		for(int i=1; i<=flsCount; i++){
			overallAvgReReleDoc += getAvgReReleDoc_flsi(queryid, cutoff, i, sysList);
		}
		
		overallAvgReReleDoc = overallAvgReReleDoc/flsCount;
		
		return overallAvgReReleDoc;		
	}
	//
	private static void getAvgReReleDocAcrossRuns(ArrayList<String> orderedQIDList, int cutoff,
			HashMap<String, HashMap<String, ArrayList<String>>> allRuns, int runNum){
		
		ArrayList<Double> avgReReleDocAcrossRunsPerQList = new ArrayList<Double>();
		
		for(String qID: orderedQIDList){
			double avgPerQ = 0.0;
			
			for(Entry<String, HashMap<String, ArrayList<String>>> run: allRuns.entrySet()){
				//String runID = run.getKey();
				HashMap<String, ArrayList<String>> aSystemRun = run.getValue();
				ArrayList<String> rankedListPerQuery = aSystemRun.get(qID);
				
				avgPerQ += getAvgReReleDoc(qID, cutoff, rankedListPerQuery);
			}
			
			avgPerQ = avgPerQ/runNum;
			
			avgReReleDocAcrossRunsPerQList.add(avgPerQ);
		}
		
		System.out.println("##\tavgReReleDoc Across Runs Per Query\t##");
		for(String qID: orderedQIDList){
			System.out.print(qID+"\t");
		}
		System.out.println();
		for(Double avgV: avgReReleDocAcrossRunsPerQList){
			System.out.print(resultFormat.format(avgV)+"\t");
		}
		System.out.println();
		System.out.println();		
	}
	
	
	/////////ARC
	private static HashMap<String, ArrayList<Pair<String, Double>>> getSpecificRunSet(ArrayList<String> globalQueryIDList, int cutoff, double avgReReleCut,
			HashMap<String, HashMap<String, ArrayList<String>>> allRuns, int runNum){
		
		HashMap<String, ArrayList<Pair<String, Double>>> desiredRunsPerQMap = new HashMap<String, ArrayList<Pair<String, Double>>>();
		
		for(String qID: globalQueryIDList){
			for(Entry<String, HashMap<String, ArrayList<String>>> run: allRuns.entrySet()){
				String runID = run.getKey();
				HashMap<String, ArrayList<String>> aSystemRun = run.getValue();
				ArrayList<String> rankedListPerQuery = aSystemRun.get(qID);
				
				double avgPerQ = getAvgReReleDoc(qID, cutoff, rankedListPerQuery);
				
				if(avgPerQ > avgReReleCut){
					//metric value
					if(desiredRunsPerQMap.containsKey(qID)){
						
						desiredRunsPerQMap.get(qID).add(new Pair<String, Double>(runID, avgPerQ));
						
					}else{
						ArrayList<Pair<String, Double>> runList = new ArrayList<Pair<String, Double>>();
						runList.add(new Pair<String, Double>(runID, avgPerQ));
						
						desiredRunsPerQMap.put(qID, runList);
					}
				}
			}
		}
		
		return desiredRunsPerQMap;		
	}
	//
	private static void compareWithSpecificRunSet(int cutoff, HashMap<String, ArrayList<Pair<String, Double>>> SpecificRunSetMap, 
			HashMap<String, HashMap<String, ArrayList<String>>> allRuns){
		
		for(Entry<String, ArrayList<Pair<String, Double>>> comEntry: SpecificRunSetMap.entrySet()){
			String comQID = comEntry.getKey();
			ArrayList<Pair<String, Double>> comRunList = comEntry.getValue(); 
			
			Collections.sort(comRunList, new PairComparatorBySecond_Desc<String, Double>());
			
			if(comRunList.size() > 2){
				ArrayList<Triple<Double, Double, Double>> comMetricValList = new ArrayList<Triple<Double,Double,Double>>();
				
				for(Pair<String, Double> pair: comRunList){
					HashMap<String, ArrayList<String>> aSystemRun = allRuns.get(pair.getFirst());
					ArrayList<String> rankedListPerQuery = aSystemRun.get(comQID);
					
					//Square-SRecall, SRecall, ERR-IA
					double sRecall_Square = Square_SRecall(comQID, true, rankedListPerQuery, cutoff);
					
					double sRecall_Aware = SRecall(comQID, IDimension.I_AWARE, rankedListPerQuery, cutoff, -1);
					
					double errIA = ERRIA(true, comQID, IDimension.I_AWARE, rankedListPerQuery, cutoff, -1, false);
					
					comMetricValList.add(new Triple<Double, Double, Double>(sRecall_Square, sRecall_Aware, errIA));					
				}
				
				System.out.println("#\tSpecific comparison:\t"+Square_SRecall+" "+SRecall+" "+ERR_IA+"\t#");
				System.out.print("ReReleScore:\t");
				for(Pair<String, Double> pair: comRunList){
					System.out.print(resultFormat.format(pair.getSecond())+"\t");					
				}
				System.out.println();
				System.out.print("Square_SRecall:\t");
				for(Triple<Double, Double, Double> triple: comMetricValList){
					System.out.print(resultFormat.format(triple.getFirst())+"\t");
				}
				System.out.println();
				System.out.print("SRecall:\t");
				for(Triple<Double, Double, Double> triple: comMetricValList){
					System.out.print(resultFormat.format(triple.getSecond())+"\t");
				}
				System.out.println();
				System.out.print("ERR_IA:\t");
				for(Triple<Double, Double, Double> triple: comMetricValList){
					System.out.print(resultFormat.format(triple.getThird())+"\t");
				}
				System.out.println();
				System.out.println();
			}
			
			
		}
		
	}
	/////////
	//Achieved Significance Level
	/////////
	/**
	 * Algorithm for obtaining the Achieved Significance Level
	 * with the two-sided, randomised Tukey’s HSD
	 * given a performance value matrix X whose rows represent topics and columns represent runs
	 * **/
	private static void testShuffle(){
		//1 test
		ArrayList<Integer> originalList = new ArrayList<Integer>();
		originalList.add(1);
		originalList.add(2);
		originalList.add(3);
		
		ArrayList<Integer> newList = new ArrayList<Integer>();
		newList.addAll(originalList);
		Collections.shuffle(newList);
		
		System.out.println("original:\t"+originalList);
		System.out.println("new:\t"+newList);
	}
	/**
	 * @param ArrayList<ArrayList<Double>> X: topicNum*runNUM matrix row is the result list of all runs for the same topic
	 * 
	 * **/
	private static void HSD(int B, String metricID, ArrayList<ArrayList<Double>> X, int topicNum, int runNum){			
		//column mean of X
		double [] columnMeanArray_X = new double [runNum];
		for(int t=0; t<topicNum; t++){
			ArrayList<Double> runResultsPerTopic_X = X.get(t);
			for(int runIndex=0; runIndex<runNum; runIndex++){
				columnMeanArray_X[runIndex] += runResultsPerTopic_X.get(runIndex);
			}			
		}
		for(int runIndex=0; runIndex<runNum; runIndex++){
			columnMeanArray_X[runIndex] = columnMeanArray_X[runIndex]/topicNum;
		}
		//count matrix
		int [][] countMatrix = new int [runNum][runNum];
		for(int runIndex=0; runIndex<runNum; runIndex++){
			countMatrix [runIndex] = new int [runNum];
		}
		//ASL matrix
		double [][] aslMatrix = new double [runNum][runNum];
		for(int runIndex=0; runIndex<runNum; runIndex++){
			aslMatrix[runIndex] = new double [runNum];
		}		
		//list of B metrics like X_star_b
		//ArrayList<ArrayList<ArrayList<Double>>> X_star_b_List = new ArrayList<ArrayList<ArrayList<Double>>>();
		//create B metrics
		for(int b=1; b<=B; b++){
			ArrayList<ArrayList<Double>> X_star_b = new ArrayList<ArrayList<Double>>();
			//a permutation of row t of X
			for(int t=0; t<topicNum; t++){
				//new row
				ArrayList<Double> permutedTRow = new ArrayList<Double>();
				permutedTRow.addAll(X.get(t));
				//permutation
				Collections.shuffle(permutedTRow);
				//create the t-th row of the new matrix
				X_star_b.add(permutedTRow);				
			}		
			//compute the maxMeanColumn_star_b, minMeanColumn_star_b
			double [] columnSumArray = new double [runNum];
			for(int t=0; t<topicNum; t++){
				ArrayList<Double> runResultsPerTopic = X_star_b.get(t);
				for(int runIndex=0; runIndex<runNum; runIndex++){
					columnSumArray[runIndex] += runResultsPerTopic.get(runIndex);
				}
			}
			//initial minimum value
			double maxColumnMean_star_b = Double.MIN_VALUE;
			//initial maximum value
			double minColumnMean_star_b = Double.MAX_VALUE;
			
			for(int runIndex=0; runIndex<runNum; runIndex++){
				//mean value
				double columnMean = columnSumArray[runIndex]/topicNum;
				
				if(columnMean > maxColumnMean_star_b){
					maxColumnMean_star_b = columnMean;
				}
				
				if(columnMean < minColumnMean_star_b){
					minColumnMean_star_b = columnMean;
				}
			}			
			//count
			for(int runI=0; runI<runNum-1; runI++){
				for(int runJ=runI+1; runJ<runNum; runJ++){
					if( (maxColumnMean_star_b-minColumnMean_star_b) > Math.abs(columnMeanArray_X[runI]-columnMeanArray_X[runJ])){						
						countMatrix[runI][runJ] += 1;
					}					
				}
			}						
		}	
		//for sorting run-pairs by asl
		ArrayList<Triple<Integer, Integer, Double>> sortedRunPairList = new ArrayList<Triple<Integer,Integer,Double>>();
		
		//ASL
		for(int runI=0; runI<runNum-1; runI++){
			for(int runJ=runI+1; runJ<runNum; runJ++){
				aslMatrix[runI][runJ] = (1.0*countMatrix[runI][runJ])/B;
				//
				sortedRunPairList.add(new Triple<Integer, Integer, Double>(runI, runJ, aslMatrix[runI][runJ]));
			}				
		}
		//
		System.out.println("ASL curves(size="+sortedRunPairList.size()+"):");		
		Collections.sort(sortedRunPairList, new TripleComparatorByThird_Desc<Integer, Integer, Double>());
		System.out.print(metricID+":\t");
		//
		boolean cut = false;
		ArrayList<Integer> sortedIDList = new ArrayList<Integer>();
		if(cut){
			for(int sortedID=1; sortedID<=sortedRunPairList.size(); sortedID++){
				Triple<Integer, Integer, Double> aslOfRunPair = sortedRunPairList.get(sortedID-1);
				if(aslOfRunPair.getThird() <= CurveCut){
					sortedIDList.add(sortedID);
					System.out.print(resultFormat.format(aslOfRunPair.getThird())+"\t");
				}
			}
			System.out.println();
			System.out.print("SortedID:\t");
			for(Integer sortedID: sortedIDList){
				System.out.print(sortedID+"\t");
			}
			System.out.println();
		}else{			
			for(int sortedID=1; sortedID<=sortedRunPairList.size(); sortedID++){
				Triple<Integer, Integer, Double> aslOfRunPair = sortedRunPairList.get(sortedID-1);
				sortedIDList.add(sortedID);
				System.out.print(resultFormat.format(aslOfRunPair.getThird())+"\t");
			}
			System.out.println();
			System.out.print("SortedID:\t");
			for(Integer sortedID: sortedIDList){
				System.out.print(sortedID+"\t");
			}
			System.out.println();
			System.out.println();
		}		
	}
	
	/**
	 * @param HashMap<String, ArrayList<Double>> perQueryEvalMapAllRun runID -> list of per query result
	 * @return ArrayList<ArrayList<Double>> topicNum*runNUM matrix row is the result list of all runs for the same topic
	 * **/
	private static ArrayList<ArrayList<Double>> getTopicRunMatrix(int topicNum, ArrayList<String> runIDList, 
			HashMap<String, ArrayList<Double>> perQueryEvalMapAllRun){
		//topic*runID matrix
		ArrayList<ArrayList<Double>> desiredX = new ArrayList<ArrayList<Double>>();
		
		for(int i=0; i<topicNum; i++){
			ArrayList<Double> resultsForOneTopic = new ArrayList<Double>();
			for(String runID: runIDList){
				resultsForOneTopic.add(perQueryEvalMapAllRun.get(runID).get(i));				
			}
			desiredX.add(resultsForOneTopic);
		}
		
		return desiredX;
	}
	
	//////////
	//Kendall's Tau score 
	/////////
	//between two metrics
	private static void TauAP(TauCorrelation tauCorrelation, ArrayList<String> globalRunIDList, 
			String metric_A, HashMap<String, Pair<String, Double>> metric_A_AvgMapAllRun,
			String metric_B, HashMap<String, Pair<String, Double>> metric_B_AvgMapAllRun){
		//
		ArrayList<Double> valList_A = new ArrayList<Double>();
		ArrayList<Double> valList_B = new ArrayList<Double>();
		
		for(String runid: globalRunIDList){
			valList_A.add(metric_A_AvgMapAllRun.get(runid).getSecond());
			valList_B.add(metric_B_AvgMapAllRun.get(runid).getSecond());			
		}
		
		double corrValue = tauCorrelation.TauAP_CorrespondingID(valList_A, valList_B);
		
		System.out.println("Tau_AP:\t"+metric_A+"\t-\t"+metric_B+"\t"+resultFormat.format(corrValue));		
	}
	//correlation matrix w.r.t. a set of metrics
	private static void correlationMatrix_TauAP(TauCorrelation tauCorrelation, ArrayList<String> globalRunIDList,
			ArrayList<String> metricIDList, ArrayList<HashMap<String, Pair<String, Double>>> metricAvgMapAllRun_list){
		
		int metricNum = metricIDList.size();
		
		for(int i=0; i<metricNum-1; i++){
			String metric_A = metricIDList.get(i);
			for(int j=i+1; j<metricNum; j++){
				String metric_B = metricIDList.get(j);
				
				TauAP(tauCorrelation, globalRunIDList, metric_A, metricAvgMapAllRun_list.get(i),
						metric_B, metricAvgMapAllRun_list.get(j));				
			}
		}
	}
	//between one specific metric and another family of metrics
	private static void corrBetweenOneAndAnotherFamily_TauAP(TauCorrelation tauCorrelation, ArrayList<String> globalRunIDList,
			String starMetric, HashMap<String, Pair<String, Double>> starMetric_AvgMapAllRun,
			ArrayList<String> metricFamily, ArrayList<HashMap<String, Pair<String, Double>>> metricAvgMapAllRun_Family){		
		//-
		int metricNum = metricFamily.size();
		for(int i=0; i<metricNum; i++){
			String memberMetric = metricFamily.get(i);
			HashMap<String, Pair<String, Double>> memberMetric_AvgMapAllRun = metricAvgMapAllRun_Family.get(i);
			
			TauAP(tauCorrelation, globalRunIDList, 
					starMetric, starMetric_AvgMapAllRun,
					memberMetric, memberMetric_AvgMapAllRun);
		}
	}
	/**
	 * Per-query correlation, i.e., rank the metrics results of one query corresponding to all the runs
	 * then compute the correlation of two metrics
	 * 
	 * **/
	private static ArrayList<String> perQueryCorr_TauAP(boolean sortCorrValues, TauCorrelation tauCorrelation, ArrayList<String> globalQueryIDList, ArrayList<String> globalRunIDList,
			String metric_A, HashMap<String, ArrayList<Double>> runToPerQueryResults_AllRunMap_A,
			String metric_B, HashMap<String, ArrayList<Double>> runToPerQueryResults_AllRunMap_B){
		
		ArrayList<Triple<String, String, Double>> perQCorrList = new ArrayList<Triple<String,String,Double>>();
		
		for(int qIndex=0; qIndex<globalQueryIDList.size(); qIndex++){
			String queryID = globalQueryIDList.get(qIndex);
			ArrayList<Double> perQResults_AllRun_A = new ArrayList<Double>();
			ArrayList<Double> perQResults_AllRun_B = new ArrayList<Double>();
			
			for(String runID: globalRunIDList){
				ArrayList<Double> allQResult_OneRun_A = runToPerQueryResults_AllRunMap_A.get(runID);
				ArrayList<Double> allQResult_OneRun_B = runToPerQueryResults_AllRunMap_B.get(runID);
				
				perQResults_AllRun_A.add(allQResult_OneRun_A.get(qIndex));
				perQResults_AllRun_B.add(allQResult_OneRun_B.get(qIndex));
			}
			
			double perQCorr = tauCorrelation.TauAP_CorrespondingID(perQResults_AllRun_A, perQResults_AllRun_B);
			perQCorrList.add(new Triple<String, String, Double>(queryID, metric_A+" "+metric_B, perQCorr));
		}
		
		if(sortCorrValues){
			Collections.sort(perQCorrList, new TripleComparatorByThird_Desc<String, String, Double>());
		}
		
		ArrayList<String> orderedQIDList = new ArrayList<String>(); 
		double sum = 0.0;
		
		System.out.println("##\t"+metric_A+" vs. "+metric_B+" Per query correlation & sorting["+sortCorrValues+"]");
		for(Triple<String, String, Double> corrTri: perQCorrList){
			System.out.print(corrTri.getFirst()+"\t");
			
			orderedQIDList.add(corrTri.getFirst());
		}
		System.out.println();
		for(Triple<String, String, Double> corrTri: perQCorrList){
			sum += corrTri.getThird();
			System.out.print(resultFormat.format(corrTri.getThird())+"\t");
		}
		System.out.println();
		System.out.println("Avg-PerQuery-CorrScore:\t"+resultFormat.format(sum/perQCorrList.size()));
		System.out.println();	
		
		return orderedQIDList;
	}
	
	///////////
	//Performance score
	//Systems ordered by D#-nDCG@20 scores
	//Chinese runs ordered by ERR-IA@20 scores
	//System performance & order difference
	//Queries 
	/////////////////
	
	private static void referencedSystemRanking(String starMetric, ArrayList<Pair<String, Double>> avgResultList_AllRun_StarMetric,
			ArrayList<String> metricIDList_family, ArrayList<HashMap<String, Pair<String, Double>>> metricAvgMapAllRun_list_family){
		//
		Collections.sort(avgResultList_AllRun_StarMetric, new PairComparatorBySecond_Desc<String, Double>());
		//star
		System.out.println("##\tSystem Performance Ranking:\t##");
		System.out.print("Star-Metric ["+starMetric+"] Order:\t");
		for(Pair<String, Double> starPair: avgResultList_AllRun_StarMetric){
			System.out.print(starPair.getFirst()+"\t");
		}
		System.out.println();
		System.out.print("Star-Metric Value:\t");
		for(Pair<String, Double> starPair: avgResultList_AllRun_StarMetric){
			System.out.print(resultFormat.format(starPair.getSecond())+"\t");
		}
		System.out.println();
		System.out.println();
		
		//compared metrics
		for(int i=0; i<metricIDList_family.size(); i++){
			String comMetric = metricIDList_family.get(i);
			HashMap<String, Pair<String, Double>> avgResultMap = metricAvgMapAllRun_list_family.get(i);
			
			System.out.print("Com-Metric ["+comMetric+"] Value:\t");
			for(Pair<String, Double> starPair: avgResultList_AllRun_StarMetric){
				System.out.print(resultFormat.format(avgResultMap.get(starPair.getFirst()).getSecond())+"\t");
			}
			System.out.println();
			System.out.println();
		}		
	}
	//for a single run
	//
	private static void referencedSystemRankingForOneRun(String runID, ArrayList<String> globalQIDList, 
			String starMetric, HashMap<String, ArrayList<Double>> starMetric_PerQueryEvalMapAllRun,
			ArrayList<String> comMetricIDList_family_1, ArrayList<HashMap<String, ArrayList<Double>>> comMetric_PerQueryEvalMapAllRun_family_1,
			ArrayList<String> comMetricIDList_family_2, ArrayList<HashMap<String, ArrayList<Double>>> comMetric_PerQueryEvalMapAllRun_family_2){
		//
		HashSet<String> qSet = new HashSet<String>();
		qSet.add("0078");
		qSet.add("0053");
		qSet.add("0080");
		boolean filter = false;
		//
		ArrayList<Double> starPerQueryEvalList = starMetric_PerQueryEvalMapAllRun.get(runID);
		ArrayList<Triple<String, Integer, Double>> starTripList = new ArrayList<Triple<String,Integer,Double>>();
		for(int id=1; id<=starPerQueryEvalList.size(); id++){
			starTripList.add(new Triple<String, Integer, Double>(globalQIDList.get(id-1), id, starPerQueryEvalList.get(id-1)));			
		}
		Collections.sort(starTripList, new TripleComparatorByThird_Desc<String, Integer, Double>());
		
		//star
		System.out.println("##\tPer-Query Performance Ranking for a single runID:\t##");
		System.out.print("Star-Metric ["+starMetric+"] Order:\t");
		for(Triple<String, Integer, Double> starTrip: starTripList){
			//1
			///*
			if(filter){
				if(qSet.contains(starTrip.getFirst())){
					System.out.print(starTrip.getFirst()+"\t");
				}
			}else{
				//2
				//System.out.print(starTrip.getFirst()+"\t");
			}
		}
		System.out.println();
		for(Triple<String, Integer, Double> starTrip: starTripList){
			System.out.print(starTrip.getSecond()+"\t");
		}
		System.out.println();
		System.out.print("Star-Metric Value:\t");
		for(Triple<String, Integer, Double> starTrip: starTripList){
			//1
			if(filter){
				if(qSet.contains(starTrip.getFirst())){
					System.out.print(resultFormat.format(starTrip.getThird())+"\t");
				}
			}else{
				//2
				System.out.print(resultFormat.format(starTrip.getThird())+"\t");
			}
		}
		System.out.println();
		System.out.println();
		//family-1
		for(int i=0; i<comMetricIDList_family_1.size(); i++){
			String comMetric_1 = comMetricIDList_family_1.get(i);
			
			HashMap<String, ArrayList<Double>> comMetric_PerQueryEvalMapAllRun_1 = comMetric_PerQueryEvalMapAllRun_family_1.get(i);
			ArrayList<Double> comMetric_QueryEval_OneRun_1 = comMetric_PerQueryEvalMapAllRun_1.get(runID);
			
			System.out.print("Com-Metric ["+comMetric_1+"] Value:\t");
			for(Triple<String, Integer, Double> starTrip: starTripList){
				//1
				if(filter){
					if(qSet.contains(starTrip.getFirst())){
						System.out.print(resultFormat.format(comMetric_QueryEval_OneRun_1.get(starTrip.getSecond()-1))+"\t");
					}
				}else{
					System.out.print(resultFormat.format(comMetric_QueryEval_OneRun_1.get(starTrip.getSecond()-1))+"\t");

				}
			}
			System.out.println();
			System.out.println();
		}
		//family-2
		for(int i=0; i<comMetricIDList_family_2.size(); i++){
			String comMetric_2 = comMetricIDList_family_2.get(i);
			
			HashMap<String, ArrayList<Double>> comMetric_PerQueryEvalMapAllRun_2 = comMetric_PerQueryEvalMapAllRun_family_2.get(i);
			ArrayList<Double> comMetric_QueryEval_OneRun_2 = comMetric_PerQueryEvalMapAllRun_2.get(runID);
			
			System.out.print("Com-Metric ["+comMetric_2+"] Value:\t");
			for(Triple<String, Integer, Double> starTrip: starTripList){
				if(filter){
					if(qSet.contains(starTrip.getFirst())){
						System.out.print(resultFormat.format(comMetric_QueryEval_OneRun_2.get(starTrip.getSecond()-1))+"\t");
					}
				}else{
					System.out.print(resultFormat.format(comMetric_QueryEval_OneRun_2.get(starTrip.getSecond()-1))+"\t");
				}
			}
			System.out.println();
			System.out.println();
		}		
	}
	///////////
	
	///Check Relevance Assessment
	//
	private static void checkRele(String runID, String queryID, int cutoff, 
			TwoLevelTopic topic, HashMap<String, HashMap<String, ArrayList<String>>> allRuns){
		//
		HashMap<String, ArrayList<String>> aSystemRun = allRuns.get(runID);
		ArrayList<String> rankedListForQuery = aSystemRun.get(queryID);
		
		for(int k=1; k<=cutoff; k++){
			String docID = rankedListForQuery.get(k-1);
			System.out.println("d_"+k+":\t"+docID);
			getReleFingerprint(docID, queryID, topic);			
		}		
	}
	//
	private static void getReleFingerprint(String docid, String queryid, TwoLevelTopic topic){
		
		ArrayList<Integer> releFlsList = new ArrayList<Integer>();
		ArrayList<ArrayList<Integer>> releSls_to_FlsList = new ArrayList<ArrayList<Integer>>();
		
		int flsCount = topic._flsList.size();
		
		for(int fls_i=1; fls_i<=flsCount; fls_i++){
			
			if(fls_DocReleMap.containsKey(docid)){					
				HashMap<String, HashMap<Integer, Integer>> docReleMap = fls_DocReleMap.get(docid);
				if(docReleMap.containsKey(queryid)){						
					HashMap<Integer, Integer> qReleMap = docReleMap.get(queryid);
					
					if(qReleMap.containsKey(fls_i)){							
						releFlsList.add(fls_i);	
						//
						int slsCount = topic._slsSetList.get(fls_i-1).size();
						releSls_to_FlsList.add(getReleSlsSet(docid, queryid, fls_i, slsCount));
					}										
				}					
			}			
		}
		
		System.out.println();
		for(int k=0; k<releFlsList.size(); k++){
			Integer releFlsID = releFlsList.get(k);
			ArrayList<Integer> releSlsList = releSls_to_FlsList.get(k);
			System.out.print("fls-'"+releFlsID+"' ->sls: "+releSlsList+""+"\t");
		}
		System.out.println();
		System.out.println();
	}
	//
	private static ArrayList<Integer> getReleSlsSet(String docid, String queryid, int flsID, int slsCount){
		ArrayList<Integer> releSlsSet = new ArrayList<Integer>();

		for(int sls_i = 1; sls_i<=slsCount; sls_i++){
			if(sls_DocReleMap.containsKey(docid)){
				HashMap<String, HashMap<Integer, HashMap<Integer, Integer>>> slsDocReleMap = sls_DocReleMap.get(docid);
				if(slsDocReleMap.containsKey(queryid)){
					HashMap<Integer, HashMap<Integer, Integer>> slsQReleMap = slsDocReleMap.get(queryid);
					if(slsQReleMap.containsKey(flsID)){
						HashMap<Integer, Integer> slsFlsReleMap = slsQReleMap.get(flsID);
						if(slsFlsReleMap.containsKey(sls_i)){								
							releSlsSet.add(sls_i);							
						}							
					}						
				}					
			}
		}
		
		return releSlsSet;
	}
	
	////////
	//Comparison
	////////
	
	private static final String SRecall = "SRecall";
	private static final String AP_IA = "AP_IA";
	private static final String ERR_IA = "ERR_IA";
	private static final String Alpha_n_DCG = "Alpha_n_DCG";
	private static final String D_Sharp_n_DCG = "D_Sharp_n_DCG";
	private static final String DIN_Sharp_n_DCGl = "DIN_Sharp_n_DCGl";
	
	private static final String Square_SRecall = "Square_SRecall";
	private static final String Square_AP_IA = "Square_AP_IA";
	private static final String Square_ERR_IA_Ari = "Square_ERR_IA_Ari";
	private static final String Square_ERR_IA_Geo = "Square_ERR_IA_Geo";
	private static final String Square_Alpha_n_DCG_Ari = "Square_Alpha_n_DCG_Ari";
	private static final String Square_Alpha_n_DCG_Geo = "Square_Alpha_n_DCG_Geo";
	private static final String Square_D_Sharp_n_DCG = "Square_D_Sharp_n_DCG";
	
	private static final String Net_ERRIA = "Net_ERRIA";
	
	
	public static void metricValue(NTCIR_EVAL_TASK eval, NTCIR11_TOPIC_TYPE type, 
			boolean awareEqual, int cutoff, boolean squareEqual, boolean hierarchicalDiv){
		
		String xmlDir = "C:/T/Research/CurrentResearch/NTCIR/NTCIR-11/Ntcir11-IMine/Eval-IMine/20150428/";
		String runDir = null;
		
		//filtered due to no sls, i.e., 0003(4), 0017(2) for Ch
		HashSet<String> filteredTopicSet = new HashSet<String>();
		//
		ArrayList<String> globalQueryIDList = new ArrayList<String>();
		int globalQueryNum;
		
		
		if(type == NTCIR11_TOPIC_TYPE.UNCLEAR){		
			//
			if(eval == NTCIR_EVAL_TASK.NTCIR11_DR_CH){
				
				String chLevelFile = xmlDir + "IMine.Qrel.SMC/IMine.Qrel.SMC.xml";
				load2LT(chLevelFile);					
				
				runDir = "C:/T/Research/CurrentResearch/NTCIR/NTCIR-11/Ntcir11-IMine/Eval-IMine/IMine.submitted/DR-Run/C/";
				
				filteredTopicSet.add("0003");
				filteredTopicSet.add("0017");
				//0033 due to no fls relevant documents				
				filteredTopicSet.add("0033");
				//0032 due to zero probability of sls w.r.t. flsID=4
				//filteredTopicSet.add("0032");
				
			}else if(eval == NTCIR_EVAL_TASK.NTCIR11_DR_EN){
				
				String enLevelFile = xmlDir + "IMine.Qrel.SME/IMine.Qrel.SME.xml";
				load2LT(enLevelFile);
				
				runDir = "C:/T/Research/CurrentResearch/NTCIR/NTCIR-11/Ntcir11-IMine/Eval-IMine/IMine.submitted/DR-Run/E/";
				
				filteredTopicSet.add("0070");
			}
			
			//for global usage
			for(TwoLevelTopic topic: _2LTList){
				String queryid = topic.getTopicID();				
				//filter the topics if it includes a subtopic that has no second-level subtopic
				if(filteredTopicSet.contains(queryid)){
					continue;
				}				
				globalQueryIDList.add(queryid);
			}
			globalQueryNum = globalQueryIDList.size();
			
			//runs to be evaluated
			HashMap<String, HashMap<String, ArrayList<String>>> allRuns = loadSysRuns(runDir);
			
			int globalRunNum = allRuns.size();			
			ArrayList<String> globalRunIDList = new ArrayList<String>();
			
			for(Entry<String, HashMap<String, ArrayList<String>>> run: allRuns.entrySet()){
				String runID = run.getKey();
				globalRunIDList.add(runID);
			}
			
			//test collection information
			collectionInfo(filteredTopicSet);
			
			//document relevance
			getDocUsageMap(xmlDir, eval, type);			
			
			///////////////
			//Intent-Aware
			///////////////
			ArrayList<String> metricIDList_family_1 = new ArrayList<String>();
			
			ArrayList<HashMap<String, Pair<String, Double>>> metricAvgMapAllRun_list_family_1 
			= new ArrayList<HashMap<String,Pair<String,Double>>>();
			
			ArrayList<HashMap<String, ArrayList<Double>>> metricPerQueryEvalMapAllRun_family_1 
			= new ArrayList<HashMap<String,ArrayList<Double>>>();
			
			//runID -> <runID, avgValue> // for avgOfOneRun
			HashMap<String, Pair<String, Double>> SRecall_AvgMapAllRun_1 = new HashMap<String, Pair<String, Double>>();
			//<runID, avgValue> // for run ranking
			ArrayList<Pair<String, Double>> SRecall_AvgList_1 = new ArrayList<Pair<String,Double>>();
			//runID -> list of per query result // for per-query analysis
			HashMap<String, ArrayList<Double>> SRecall_PerQueryEvalMapAllRun_1 = new HashMap<String, ArrayList<Double>>();			
			metricIDList_family_1.add(SRecall);			
			
			HashMap<String, Pair<String, Double>> APIA_AvgMapAllRun_1 = new HashMap<String, Pair<String, Double>>();
			ArrayList<Pair<String, Double>> APIA_AvgList_1 = new ArrayList<Pair<String,Double>>();
			HashMap<String, ArrayList<Double>>  APIA_PerQueryEvalMapAllRun_1 = new HashMap<String, ArrayList<Double>>();
			metricIDList_family_1.add(AP_IA);
			
			HashMap<String, Pair<String, Double>> ERRIA_AvgMapAllRun_1 = new HashMap<String, Pair<String, Double>>();
			ArrayList<Pair<String, Double>> ERRIA_AvgList_1 = new ArrayList<Pair<String,Double>>();
			HashMap<String, ArrayList<Double>> ERRIA_PerQueryEvalMapAllRun_1 = new HashMap<String, ArrayList<Double>>();
			metricIDList_family_1.add(ERR_IA);
			
			HashMap<String, Pair<String, Double>> AlphaNDCG_AvgMapAllRun_1 = new HashMap<String, Pair<String, Double>>();
			ArrayList<Pair<String, Double>> AlphaNDCG_AvgList_1 = new ArrayList<Pair<String,Double>>();
			HashMap<String, ArrayList<Double>> AlphaNDCG_PerQueryEvalMapAllRun_1 = new HashMap<String, ArrayList<Double>>();
			metricIDList_family_1.add(Alpha_n_DCG);
			
			HashMap<String, Pair<String, Double>> DSharpNDCG_AvgMapAllRun_1 = new HashMap<String, Pair<String, Double>>();
			ArrayList<Pair<String, Double>> DSharpNDCG_AvgList_1 = new ArrayList<Pair<String,Double>>();
			HashMap<String, ArrayList<Double>> DSharpNDCG_PerQueryEvalMapAllRun_1 = new HashMap<String, ArrayList<Double>>();
			metricIDList_family_1.add(D_Sharp_n_DCG);
			
			for(Entry<String, HashMap<String, ArrayList<String>>> run: allRuns.entrySet()){
				String runID = run.getKey();
				//queryid -> ranked list
				HashMap<String, ArrayList<String>> aSystemRun = run.getValue();
				
				ArrayList<Double> SRecall_PerQueryEvalList_1 = new ArrayList<Double>();	
				ArrayList<Double> APIA_PerQueryEvalList_1 = new ArrayList<Double>();
				ArrayList<Double> ERRIA_PerQueryEvalList_1 = new ArrayList<Double>();
				ArrayList<Double> AlphaNDCG_PerQueryEvalList_1 = new ArrayList<Double>();
				ArrayList<Double> DSharpNDCG_PerQueryEvalList_1 = new ArrayList<Double>();
				
				double SRecall_sum_1 = 0.0;
				double APIA_sum_1 = 0.0;
				double ERRIA_sum_1 = 0.0;
				double AlphaNDCG_sum_1 = 0.0;
				double DSharpNDCG_sum_1 = 0.0;
				
				int countOfUsedTopics = 0;
				
				for(TwoLevelTopic topic: _2LTList){
					String queryid = topic.getTopicID();
										
					//filter the topics if it includes a subtopic that has no second-level subtopic
					if(filteredTopicSet.contains(queryid)){
						continue;
					}
					
					ArrayList<String> rankedListPerQuery = aSystemRun.get(queryid);
					
					countOfUsedTopics++;
					
					//SRecall
					double sRecall_1 = SRecall(queryid, IDimension.I_AWARE, rankedListPerQuery, cutoff, -1);
					SRecall_PerQueryEvalList_1.add(sRecall_1);
					SRecall_sum_1 += sRecall_1;
					
					//APIA
					double apIA_1 = APIA(awareEqual, queryid, IDimension.I_AWARE, rankedListPerQuery, cutoff, -1, false);
					APIA_PerQueryEvalList_1.add(apIA_1);
					APIA_sum_1 += apIA_1;
					
					//ERRIA
					double errIA_1 = ERRIA(awareEqual, queryid, IDimension.I_AWARE, rankedListPerQuery, cutoff, -1, false);
					ERRIA_PerQueryEvalList_1.add(errIA_1);
					ERRIA_sum_1 += errIA_1;
					
					//alpha-nDCG
					double alphaNDCG_1 = alpha_n_DCG(queryid, IDimension.I_AWARE, rankedListPerQuery, cutoff, -1);
					AlphaNDCG_PerQueryEvalList_1.add(alphaNDCG_1);
					AlphaNDCG_sum_1 += alphaNDCG_1;
					
					//D#-nDCG
					double dSharpNDCG_1 = D_Sharp_n_DCG(awareEqual, queryid, IDimension.I_AWARE, rankedListPerQuery, cutoff, -1, false);
					DSharpNDCG_PerQueryEvalList_1.add(dSharpNDCG_1);
					DSharpNDCG_sum_1 += dSharpNDCG_1;					
				}
				
				Pair<String, Double> SRecall_Avg_1 = new Pair<String, Double>(runID, SRecall_sum_1/countOfUsedTopics);
				SRecall_AvgMapAllRun_1.put(runID, SRecall_Avg_1);
				SRecall_AvgList_1.add(SRecall_Avg_1);
				SRecall_PerQueryEvalMapAllRun_1.put(runID, SRecall_PerQueryEvalList_1);
				
				Pair<String, Double> APIA_Avg_1 = new Pair<String, Double>(runID, APIA_sum_1/countOfUsedTopics);
				APIA_AvgMapAllRun_1.put(runID, APIA_Avg_1);
				APIA_AvgList_1.add(APIA_Avg_1);
				APIA_PerQueryEvalMapAllRun_1.put(runID, APIA_PerQueryEvalList_1);
				
				Pair<String, Double> ERRIA_Avg_1 = new Pair<String, Double>(runID, ERRIA_sum_1/countOfUsedTopics);
				ERRIA_AvgMapAllRun_1.put(runID, ERRIA_Avg_1);
				ERRIA_AvgList_1.add(ERRIA_Avg_1);
				ERRIA_PerQueryEvalMapAllRun_1.put(runID, ERRIA_PerQueryEvalList_1);
				
				Pair<String, Double> AlphaNDCG_Avg_1 = new Pair<String, Double>(runID, AlphaNDCG_sum_1/countOfUsedTopics);
				AlphaNDCG_AvgMapAllRun_1.put(runID, AlphaNDCG_Avg_1);
				AlphaNDCG_AvgList_1.add(AlphaNDCG_Avg_1);
				AlphaNDCG_PerQueryEvalMapAllRun_1.put(runID, AlphaNDCG_PerQueryEvalList_1);
				
				Pair<String, Double> DSharpNDCG_Avg_1 = new Pair<String, Double>(runID, DSharpNDCG_sum_1/countOfUsedTopics);
				DSharpNDCG_AvgMapAllRun_1.put(runID, DSharpNDCG_Avg_1);
				DSharpNDCG_AvgList_1.add(DSharpNDCG_Avg_1);
				DSharpNDCG_PerQueryEvalMapAllRun_1.put(runID, DSharpNDCG_PerQueryEvalList_1);				
			}
			
			//-
			metricAvgMapAllRun_list_family_1.add(SRecall_AvgMapAllRun_1);
			metricAvgMapAllRun_list_family_1.add(APIA_AvgMapAllRun_1);
			metricAvgMapAllRun_list_family_1.add(ERRIA_AvgMapAllRun_1);
			metricAvgMapAllRun_list_family_1.add(AlphaNDCG_AvgMapAllRun_1);
			metricAvgMapAllRun_list_family_1.add(DSharpNDCG_AvgMapAllRun_1);		
			
			metricPerQueryEvalMapAllRun_family_1.add(SRecall_PerQueryEvalMapAllRun_1);
			metricPerQueryEvalMapAllRun_family_1.add(APIA_PerQueryEvalMapAllRun_1);
			metricPerQueryEvalMapAllRun_family_1.add(ERRIA_PerQueryEvalMapAllRun_1);
			metricPerQueryEvalMapAllRun_family_1.add(AlphaNDCG_PerQueryEvalMapAllRun_1);
			metricPerQueryEvalMapAllRun_family_1.add(DSharpNDCG_PerQueryEvalMapAllRun_1);
			//-
			
			//check the ranked order of submitted runs
			//SRecall
			Collections.sort(SRecall_AvgList_1, new PairComparatorBySecond_Desc<String, Double>());
			System.out.print("SRecall:\t");
			for(Pair<String, Double> SRecall_PerRun_1: SRecall_AvgList_1){
				System.out.print(SRecall_PerRun_1.getFirst()+" : "+resultFormat.format(SRecall_PerRun_1.getSecond())+"\t");
			}
			System.out.println();
			System.out.println();
			//APIA
			System.out.print("APIA:\t\t");
			Collections.sort(APIA_AvgList_1, new PairComparatorBySecond_Desc<String, Double>());
			for(Pair<String, Double> APIA_PerRun_1: APIA_AvgList_1){
				System.out.print(APIA_PerRun_1.getFirst()+" : "+resultFormat.format(APIA_PerRun_1.getSecond())+"\t");
			}
			System.out.println();
			System.out.println();
			//ERRIA
			System.out.print("ERRIA:\t\t");
			Collections.sort(ERRIA_AvgList_1, new PairComparatorBySecond_Desc<String, Double>());
			for(Pair<String, Double> ERRIA_PerRun_1: ERRIA_AvgList_1){
				System.out.print(ERRIA_PerRun_1.getFirst()+" : "+resultFormat.format(ERRIA_PerRun_1.getSecond())+"\t");
			}
			System.out.println();
			System.out.println();
			//AlphaNDCG
			System.out.print("AlphaNDCG:\t");
			Collections.sort(AlphaNDCG_AvgList_1, new PairComparatorBySecond_Desc<String, Double>());
			for(Pair<String, Double> AlphaNDCG_PerRun_1: AlphaNDCG_AvgList_1){
				System.out.print(AlphaNDCG_PerRun_1.getFirst()+" : "+resultFormat.format(AlphaNDCG_PerRun_1.getSecond())+"\t");
			}
			System.out.println();
			System.out.println();
			//DSharpNDCG
			System.out.print("DSharpNDCG:\t");
			Collections.sort(DSharpNDCG_AvgList_1, new PairComparatorBySecond_Desc<String, Double>());
			for(Pair<String, Double> DSharpNDCG_PerRun_1: DSharpNDCG_AvgList_1){
				System.out.print(DSharpNDCG_PerRun_1.getFirst()+" : "+resultFormat.format(DSharpNDCG_PerRun_1.getSecond())+"\t");
			}
			System.out.println();
			System.out.println();
			
			
			///////////////////
			//Intent-Square {Ari + Geo}& ERR-IA_Net
			///////////////////
			System.out.println();
			System.out.println();
			
			ArrayList<String> metricIDList_family_2 = new ArrayList<String>();
			
			ArrayList<HashMap<String, Pair<String, Double>>> metricAvgMapAllRun_list_family_2 
			= new ArrayList<HashMap<String,Pair<String,Double>>>();			
			
			ArrayList<HashMap<String, ArrayList<Double>>> metricPerQueryEvalMapAllRun_family_2 
			= new ArrayList<HashMap<String,ArrayList<Double>>>();
			
			//runID -> <runID, avgValue> // for avgOfOneRun
			HashMap<String, Pair<String, Double>> SRecall_AvgMapAllRun_2 = new HashMap<String, Pair<String, Double>>();
			//<runID, avgValue> // for run ranking
			ArrayList<Pair<String, Double>> SRecall_AvgList_2 = new ArrayList<Pair<String,Double>>();
			//runID -> list of per query result // for per-query analysis
			HashMap<String, ArrayList<Double>> SRecall_PerQueryEvalMapAllRun_2 = new HashMap<String, ArrayList<Double>>();
			metricIDList_family_2.add(Square_SRecall);			
			
			HashMap<String, Pair<String, Double>> APIA_AvgMapAllRun_2 = new HashMap<String, Pair<String, Double>>();
			ArrayList<Pair<String, Double>> APIA_AvgList_2 = new ArrayList<Pair<String,Double>>();
			HashMap<String, ArrayList<Double>>  APIA_PerQueryEvalMapAllRun_2 = new HashMap<String, ArrayList<Double>>();
			metricIDList_family_2.add(Square_AP_IA);			
			
			//ari
			HashMap<String, Pair<String, Double>> ari_ERRIA_AvgMapAllRun_2 = new HashMap<String, Pair<String, Double>>();
			ArrayList<Pair<String, Double>> ari_ERRIA_AvgList_2 = new ArrayList<Pair<String,Double>>();
			HashMap<String, ArrayList<Double>> ari_ERRIA_PerQueryEvalMapAllRun_2 = new HashMap<String, ArrayList<Double>>();
			metricIDList_family_2.add(Square_ERR_IA_Ari);	
			//geo
			HashMap<String, Pair<String, Double>> geo_ERRIA_AvgMapAllRun_2 = new HashMap<String, Pair<String, Double>>();
			ArrayList<Pair<String, Double>> geo_ERRIA_AvgList_2 = new ArrayList<Pair<String,Double>>();
			HashMap<String, ArrayList<Double>> geo_ERRIA_PerQueryEvalMapAllRun_2 = new HashMap<String, ArrayList<Double>>();
			metricIDList_family_2.add(Square_ERR_IA_Geo);
			
			//ari
			HashMap<String, Pair<String, Double>> ari_AlphaNDCG_AvgMapAllRun_2 = new HashMap<String, Pair<String, Double>>();
			ArrayList<Pair<String, Double>> ari_AlphaNDCG_AvgList_2 = new ArrayList<Pair<String,Double>>();
			HashMap<String, ArrayList<Double>> ari_AlphaNDCG_PerQueryEvalMapAllRun_2 = new HashMap<String, ArrayList<Double>>();
			metricIDList_family_2.add(Square_Alpha_n_DCG_Ari);
			//geo
			HashMap<String, Pair<String, Double>> geo_AlphaNDCG_AvgMapAllRun_2 = new HashMap<String, Pair<String, Double>>();
			ArrayList<Pair<String, Double>> geo_AlphaNDCG_AvgList_2 = new ArrayList<Pair<String,Double>>();
			HashMap<String, ArrayList<Double>> geo_AlphaNDCG_PerQueryEvalMapAllRun_2 = new HashMap<String, ArrayList<Double>>();
			metricIDList_family_2.add(Square_Alpha_n_DCG_Geo);
			
			HashMap<String, Pair<String, Double>> DSharpNDCG_AvgMapAllRun_2 = new HashMap<String, Pair<String, Double>>();
			ArrayList<Pair<String, Double>> DSharpNDCG_AvgList_2 = new ArrayList<Pair<String,Double>>();
			HashMap<String, ArrayList<Double>> DSharpNDCG_PerQueryEvalMapAllRun_2 = new HashMap<String, ArrayList<Double>>();
			metricIDList_family_2.add(Square_D_Sharp_n_DCG);
			
			//--
			HashMap<String, Pair<String, Double>> NetERRIA_AvgMapAllRun_2 = new HashMap<String, Pair<String, Double>>();
			ArrayList<Pair<String, Double>> NetERRIA_AvgList_2 = new ArrayList<Pair<String,Double>>();
			HashMap<String, ArrayList<Double>> NetERRIA_PerQueryEvalMapAllRun_2 = new HashMap<String, ArrayList<Double>>();
			metricIDList_family_2.add(Net_ERRIA);
			//--
			
			
			
			for(Entry<String, HashMap<String, ArrayList<String>>> run: allRuns.entrySet()){
				String runID = run.getKey();
				//queryid -> ranked list
				HashMap<String, ArrayList<String>> aSystemRun = run.getValue();
				
				ArrayList<Double> SRecall_PerQueryEvalList_2 = new ArrayList<Double>();	
				ArrayList<Double> APIA_PerQueryEvalList_2 = new ArrayList<Double>();
				
				ArrayList<Double> ari_ERRIA_PerQueryEvalList_2 = new ArrayList<Double>();
				ArrayList<Double> geo_ERRIA_PerQueryEvalList_2 = new ArrayList<Double>();
				
				ArrayList<Double> ari_AlphaNDCG_PerQueryEvalList_2 = new ArrayList<Double>();
				ArrayList<Double> geo_AlphaNDCG_PerQueryEvalList_2 = new ArrayList<Double>();
				
				ArrayList<Double> DSharpNDCG_PerQueryEvalList_2 = new ArrayList<Double>();
				
				ArrayList<Double> NetERRIA_PerQueryEvalList_2 = new ArrayList<Double>();
				
				double SRecall_sum_2 = 0.0;
				double APIA_sum_2 = 0.0;
				
				double ari_ERRIA_sum_2 = 0.0;
				double geo_ERRIA_sum_2 = 0.0;
				
				double ari_AlphaNDCG_sum_2 = 0.0;
				double geo_AlphaNDCG_sum_2 = 0.0;
				
				double DSharpNDCG_sum_2 = 0.0;
				
				double Net_ERRIA_sum_2 = 0.0;
				
				int countOfUsedTopics = 0;
				
				for(TwoLevelTopic topic: _2LTList){
					String queryid = topic.getTopicID();
					
					//filter the topics if it includes a subtopic that has no second-level subtopic
					if(filteredTopicSet.contains(queryid)){
						continue;
					}
					
					ArrayList<String> rankedListPerQuery = aSystemRun.get(queryid);
					
					countOfUsedTopics++;
					
					//SRecall
					double sRecall_2 = Square_SRecall(queryid, squareEqual, rankedListPerQuery, cutoff);
					SRecall_PerQueryEvalList_2.add(sRecall_2);
					SRecall_sum_2 += sRecall_2;
					
					//APIA
					double apIA_2 = Square_AP_Ari(queryid, squareEqual, rankedListPerQuery, cutoff, awareEqual, true);
					APIA_PerQueryEvalList_2.add(apIA_2);
					APIA_sum_2 += apIA_2;
					
					//ERRIA-IS
					//Ari
					double ari_ErrIA_2 = Square_ERR_Ari(queryid, squareEqual, rankedListPerQuery, cutoff, awareEqual, hierarchicalDiv);
					ari_ERRIA_PerQueryEvalList_2.add(ari_ErrIA_2);
					ari_ERRIA_sum_2 += ari_ErrIA_2;
					//Geo
					double geo_ErrIA_2 = Square_ERR_Geo(queryid, squareEqual, rankedListPerQuery, cutoff, awareEqual, hierarchicalDiv);
					geo_ERRIA_PerQueryEvalList_2.add(geo_ErrIA_2);
					geo_ERRIA_sum_2 += geo_ErrIA_2;
					
					//alpha-nDCG
					//Ari
					double ari_alphaNDCG_2 = Square_alpha_n_DCG_Ari(queryid, squareEqual, rankedListPerQuery, cutoff);
					ari_AlphaNDCG_PerQueryEvalList_2.add(ari_alphaNDCG_2);
					ari_AlphaNDCG_sum_2 += ari_alphaNDCG_2;
					//Geo
					double geo_alphaNDCG_2 = Square_alpha_n_DCG_Geo(queryid, squareEqual, rankedListPerQuery, cutoff);
					geo_AlphaNDCG_PerQueryEvalList_2.add(geo_alphaNDCG_2);
					geo_AlphaNDCG_sum_2 += geo_alphaNDCG_2;
					
					//D#-nDCG
					double dSharpNDCG_2 = Square_D_Sharp_n_DCG_Ari(queryid, squareEqual, rankedListPerQuery, cutoff, awareEqual, true);
					DSharpNDCG_PerQueryEvalList_2.add(dSharpNDCG_2);
					DSharpNDCG_sum_2 += dSharpNDCG_2;	
					
					//Net-ERRIA
					double netERRIA_2 = ERRIA_Net(awareEqual, queryid, rankedListPerQuery, cutoff);
					NetERRIA_PerQueryEvalList_2.add(netERRIA_2);
					Net_ERRIA_sum_2 += netERRIA_2;					
				}
				
				Pair<String, Double> SRecall_Avg_2 = new Pair<String, Double>(runID, SRecall_sum_2/countOfUsedTopics);
				SRecall_AvgMapAllRun_2.put(runID, SRecall_Avg_2);
				SRecall_AvgList_2.add(SRecall_Avg_2);
				SRecall_PerQueryEvalMapAllRun_2.put(runID, SRecall_PerQueryEvalList_2);
				
				Pair<String, Double> APIA_Avg_2 = new Pair<String, Double>(runID, APIA_sum_2/countOfUsedTopics);
				APIA_AvgMapAllRun_2.put(runID, APIA_Avg_2);
				APIA_AvgList_2.add(APIA_Avg_2);
				APIA_PerQueryEvalMapAllRun_2.put(runID, APIA_PerQueryEvalList_2);
				//ari
				Pair<String, Double> ari_ERRIA_Avg_2 = new Pair<String, Double>(runID, ari_ERRIA_sum_2/countOfUsedTopics);
				ari_ERRIA_AvgMapAllRun_2.put(runID, ari_ERRIA_Avg_2);
				ari_ERRIA_AvgList_2.add(ari_ERRIA_Avg_2);
				ari_ERRIA_PerQueryEvalMapAllRun_2.put(runID, ari_ERRIA_PerQueryEvalList_2);
				//geo
				Pair<String, Double> geo_ERRIA_Avg_2 = new Pair<String, Double>(runID, geo_ERRIA_sum_2/countOfUsedTopics);
				geo_ERRIA_AvgMapAllRun_2.put(runID, geo_ERRIA_Avg_2);
				geo_ERRIA_AvgList_2.add(geo_ERRIA_Avg_2);
				geo_ERRIA_PerQueryEvalMapAllRun_2.put(runID, geo_ERRIA_PerQueryEvalList_2);
				
				//ari
				Pair<String, Double> ari_AlphaNDCG_Avg_2 = new Pair<String, Double>(runID, ari_AlphaNDCG_sum_2/countOfUsedTopics);
				ari_AlphaNDCG_AvgMapAllRun_2.put(runID, ari_AlphaNDCG_Avg_2);
				ari_AlphaNDCG_AvgList_2.add(ari_AlphaNDCG_Avg_2);
				ari_AlphaNDCG_PerQueryEvalMapAllRun_2.put(runID, ari_AlphaNDCG_PerQueryEvalList_2);
				//geo
				Pair<String, Double> geo_AlphaNDCG_Avg_2 = new Pair<String, Double>(runID, geo_AlphaNDCG_sum_2/countOfUsedTopics);
				geo_AlphaNDCG_AvgMapAllRun_2.put(runID, geo_AlphaNDCG_Avg_2);
				geo_AlphaNDCG_AvgList_2.add(geo_AlphaNDCG_Avg_2);
				geo_AlphaNDCG_PerQueryEvalMapAllRun_2.put(runID, geo_AlphaNDCG_PerQueryEvalList_2);
				
				Pair<String, Double> DSharpNDCG_Avg_2 = new Pair<String, Double>(runID, DSharpNDCG_sum_2/countOfUsedTopics);
				DSharpNDCG_AvgMapAllRun_2.put(runID, DSharpNDCG_Avg_2);
				DSharpNDCG_AvgList_2.add(DSharpNDCG_Avg_2);
				DSharpNDCG_PerQueryEvalMapAllRun_2.put(runID, DSharpNDCG_PerQueryEvalList_2);	
				
				Pair<String, Double> NetERRIA_Avg_2 = new Pair<String, Double>(runID, Net_ERRIA_sum_2/countOfUsedTopics);
				NetERRIA_AvgMapAllRun_2.put(runID, NetERRIA_Avg_2);
				NetERRIA_AvgList_2.add(NetERRIA_Avg_2);
				NetERRIA_PerQueryEvalMapAllRun_2.put(runID, NetERRIA_PerQueryEvalList_2);
			}			
			//--
			metricAvgMapAllRun_list_family_2.add(SRecall_AvgMapAllRun_2);
			metricAvgMapAllRun_list_family_2.add(APIA_AvgMapAllRun_2);
			//ari
			metricAvgMapAllRun_list_family_2.add(ari_ERRIA_AvgMapAllRun_2);
			//geo
			metricAvgMapAllRun_list_family_2.add(geo_ERRIA_AvgMapAllRun_2);
			
			//ari
			metricAvgMapAllRun_list_family_2.add(ari_AlphaNDCG_AvgMapAllRun_2);
			//geo
			metricAvgMapAllRun_list_family_2.add(geo_AlphaNDCG_AvgMapAllRun_2);
			
			metricAvgMapAllRun_list_family_2.add(DSharpNDCG_AvgMapAllRun_2);
			
			metricAvgMapAllRun_list_family_2.add(NetERRIA_AvgMapAllRun_2);
			
			metricPerQueryEvalMapAllRun_family_2.add(SRecall_PerQueryEvalMapAllRun_2);
			metricPerQueryEvalMapAllRun_family_2.add(APIA_PerQueryEvalMapAllRun_2);
			//ari
			metricPerQueryEvalMapAllRun_family_2.add(ari_ERRIA_PerQueryEvalMapAllRun_2);
			//geo
			metricPerQueryEvalMapAllRun_family_2.add(geo_ERRIA_PerQueryEvalMapAllRun_2);
			
			//ari
			metricPerQueryEvalMapAllRun_family_2.add(ari_AlphaNDCG_PerQueryEvalMapAllRun_2);
			//geo
			metricPerQueryEvalMapAllRun_family_2.add(geo_AlphaNDCG_PerQueryEvalMapAllRun_2);
			
			metricPerQueryEvalMapAllRun_family_2.add(DSharpNDCG_PerQueryEvalMapAllRun_2);
			
			metricPerQueryEvalMapAllRun_family_2.add(NetERRIA_PerQueryEvalMapAllRun_2);
			//--
			
			//check the ranked order of submitted runs
			//Square-SRecall
			Collections.sort(SRecall_AvgList_2, new PairComparatorBySecond_Desc<String, Double>());
			System.out.print("Square-SRecall:\t\t");
			for(Pair<String, Double> SRecall_PerRun_2: SRecall_AvgList_2){
				System.out.print(SRecall_PerRun_2.getFirst()+" : "+resultFormat.format(SRecall_PerRun_2.getSecond())+"\t");
			}
			System.out.println();
			System.out.println();
			//Square-APIA
			System.out.print("Square-APIA:\t\t");
			Collections.sort(APIA_AvgList_2, new PairComparatorBySecond_Desc<String, Double>());
			for(Pair<String, Double> APIA_PerRun_2: APIA_AvgList_2){
				System.out.print(APIA_PerRun_2.getFirst()+" : "+resultFormat.format(APIA_PerRun_2.getSecond())+"\t");
			}
			System.out.println();
			System.out.println();
			//Square-ERRIA-Ari
			System.out.print("Square-ERRIA-Ari:\t\t");
			Collections.sort(ari_ERRIA_AvgList_2, new PairComparatorBySecond_Desc<String, Double>());
			for(Pair<String, Double> ari_ERRIA_PerRun_2: ari_ERRIA_AvgList_2){
				System.out.print(ari_ERRIA_PerRun_2.getFirst()+" : "+resultFormat.format(ari_ERRIA_PerRun_2.getSecond())+"\t");
			}
			System.out.println();
			System.out.println();
			//Square-ERRIA-Geo
			System.out.print("Square-ERRIA-Geo:\t\t");
			Collections.sort(geo_ERRIA_AvgList_2, new PairComparatorBySecond_Desc<String, Double>());
			for(Pair<String, Double> geo_ERRIA_PerRun_2: geo_ERRIA_AvgList_2){
				System.out.print(geo_ERRIA_PerRun_2.getFirst()+" : "+resultFormat.format(geo_ERRIA_PerRun_2.getSecond())+"\t");
			}
			System.out.println();
			System.out.println();
			
			//Square-AlphaNDCG-Ari
			System.out.print("Square-AlphaNDCG-Ari:\t");
			Collections.sort(ari_AlphaNDCG_AvgList_2, new PairComparatorBySecond_Desc<String, Double>());
			for(Pair<String, Double> ari_AlphaNDCG_PerRun_2: ari_AlphaNDCG_AvgList_2){
				System.out.print(ari_AlphaNDCG_PerRun_2.getFirst()+" : "+resultFormat.format(ari_AlphaNDCG_PerRun_2.getSecond())+"\t");
			}
			System.out.println();
			System.out.println();
			//Square-AlphaNDCG-Geo
			System.out.print("Square-AlphaNDCG-Geo:\t");
			Collections.sort(geo_AlphaNDCG_AvgList_2, new PairComparatorBySecond_Desc<String, Double>());
			for(Pair<String, Double> geo_AlphaNDCG_PerRun_2: geo_AlphaNDCG_AvgList_2){
				System.out.print(geo_AlphaNDCG_PerRun_2.getFirst()+" : "+resultFormat.format(geo_AlphaNDCG_PerRun_2.getSecond())+"\t");
			}
			System.out.println();
			System.out.println();
			
			//Square-DSharpNDCG
			System.out.print("Square-DSharpNDCG:\t");
			Collections.sort(DSharpNDCG_AvgList_2, new PairComparatorBySecond_Desc<String, Double>());
			for(Pair<String, Double> DSharpNDCG_PerRun_2: DSharpNDCG_AvgList_2){
				System.out.print(DSharpNDCG_PerRun_2.getFirst()+" : "+resultFormat.format(DSharpNDCG_PerRun_2.getSecond())+"\t");
			}
			System.out.println();
			System.out.println();	
			//Net-ERRIA
			System.out.print("Net-ERRIA:\t");
			Collections.sort(NetERRIA_AvgList_2, new PairComparatorBySecond_Desc<String, Double>());
			for(Pair<String, Double> NetERRIA_PerRun_2: NetERRIA_AvgList_2){
				System.out.print(NetERRIA_PerRun_2.getFirst()+" : "+resultFormat.format(NetERRIA_PerRun_2.getSecond())+"\t");
			}
			System.out.println();
			System.out.println();
			
			
			
			////////
			//Experimental Analysis
			////////
			System.out.println("##\tExperimental Analysis\t##");
			
			//1	AvgFlsSRecall vs. AvgSlsSRecall
			System.out.println(eval.toString());
			getAvgFlsSRecallVersusAvgSlsSRecall(allRuns, filteredTopicSet, SRecall_AvgMapAllRun_1, SRecall_AvgMapAllRun_2);
			
			//////
			//system performance and ranking
			//////
			///*
			/*
			referencedSystemRanking(ERR_IA, ERRIA_AvgList_1, 
					metricIDList_family_2, metricAvgMapAllRun_list_family_2);
			referencedSystemRanking(ERR_IA, ERRIA_AvgList_1, 
					metricIDList_family_1, metricAvgMapAllRun_list_family_1);
			*/
			
			/*
			referencedSystemRanking(Alpha_n_DCG, AlphaNDCG_AvgList_1,
					metricIDList_family_2, metricAvgMapAllRun_list_family_2);
			*/
			
			//referencedSystemRanking(D_Sharp_n_DCG, DSharpNDCG_AvgList_1,
			//		metricIDList_family_2, metricAvgMapAllRun_list_family_2);
			
			//cikm2015:	for a single run: Sem13-D-E-1A (En)
			//
			//ERR-IA
			/*
			referencedSystemRankingForOneRun("THUSAM-D-C-1A", globalQueryIDList, 
					ERR_IA, ERRIA_PerQueryEvalMapAllRun_1, 
					metricIDList_family_1, metricPerQueryEvalMapAllRun_family_1, 
					metricIDList_family_2, metricPerQueryEvalMapAllRun_family_2);
			*/
			//Alpha_nDCg ictir2015:	Sem13-D-E-1A (En)	THUSAM-D-C-1A (Ch)
			/*
			referencedSystemRankingForOneRun("Sem13-D-E-1A", globalQueryIDList, 
					Alpha_n_DCG, AlphaNDCG_PerQueryEvalMapAllRun_1, 
					metricIDList_family_1, metricPerQueryEvalMapAllRun_family_1, 
					metricIDList_family_2, metricPerQueryEvalMapAllRun_family_2);
			*/
			//wsdm for a single run: Sem13-D-E-1A (En)
			//err-ia
			/*
			referencedSystemRankingForOneRun("Sem13-D-E-1A", globalQueryIDList, 
					ERR_IA, ERRIA_PerQueryEvalMapAllRun_1, 
					metricIDList_family_1, metricPerQueryEvalMapAllRun_family_1, 
					metricIDList_family_2, metricPerQueryEvalMapAllRun_family_2);
			*/
			//"THUSAM-D-C-1A"
			/*
			referencedSystemRankingForOneRun("THUSAM-D-C-1A", globalQueryIDList, 
					ERR_IA, ERRIA_PerQueryEvalMapAllRun_1, 
					metricIDList_family_1, metricPerQueryEvalMapAllRun_family_1, 
					metricIDList_family_2, metricPerQueryEvalMapAllRun_family_2);
			*/
			//
			
			//
			//String query_1 = "0078";
			//System.out.println("----"+query_1+"---");
			//checkRele("Sem13-D-E-1A", query_1, cutoff,  _2LTMap.get(query_1), allRuns);
			//
			//String query_2 = "0053";
			//System.out.println("----"+query_2+"---");
			//checkRele("Sem13-D-E-1A", query_2, cutoff,  _2LTMap.get(query_2), allRuns);
			//
			//String query_3 = "0080";
			//System.out.println("----"+query_3+"---");
			//checkRele("Sem13-D-E-1A", query_3, cutoff,  _2LTMap.get(query_3), allRuns);
			//*/
			
			//////
			//correlation
			//////
			
			////Correlation Matrix of a family based on run rankings w.r.t. averaged value of all topics
			/*
			System.out.println("##\tCorrelation Analysis [Intent-Aware]\t##");
			correlationMatrix_TauAP(tauCorrelation, globalRunIDList, metricIDList_family_1, metricAvgMapAllRun_list_family_1);
			System.out.println();
			System.out.println("##\tCorrelation Analysis [Intent-Square]\t##");
			correlationMatrix_TauAP(tauCorrelation, globalRunIDList, metricIDList_family_2, metricAvgMapAllRun_list_family_2);
			System.out.println();
			
			////Correlation one vs. a family
			System.out.println("##\tCorrelation Analysis [one vs. a family]\t##");			
			//correlation: square-SRecall w.r.t. intent-aware family
			corrBetweenOneAndAnotherFamily_TauAP(tauCorrelation, globalRunIDList, 
					Square_SRecall, SRecall_AvgMapAllRun_2, 
					metricIDList_family_1, metricAvgMapAllRun_list_family_1);
			System.out.println();
			*/
			
			////Per query correlation between two metrics based on run rankings w.r.t. value of a specific topic
			
			//ArrayList<String> orderedQIDList_SquareSRecall_AlphaNDCG = 
			//getAvgReReleDocAcrossRuns(orderedQIDList_SquareSRecall_AlphaNDCG, cutoff, allRuns, globalRunNum);
			
			//(1) SquareSRecall w.r.t. SRecall, AP-IA, ERR-IA, alpha_nDCG, D#-nDCG	
			/*
			boolean sortCorrelationValues_SSRecall = true;
			//SRecall
			perQueryCorr_TauAP(sortCorrelationValues_SSRecall, tauCorrelation, globalQueryIDList, globalRunIDList,
					Square_SRecall, SRecall_PerQueryEvalMapAllRun_2, 
					SRecall, SRecall_PerQueryEvalMapAllRun_1);	
			//AP-IA
			perQueryCorr_TauAP(sortCorrelationValues_SSRecall, tauCorrelation, globalQueryIDList, globalRunIDList, 
					Square_SRecall, SRecall_PerQueryEvalMapAllRun_2, 
					AP_IA, APIA_PerQueryEvalMapAllRun_1);
			//ERR-IA
			perQueryCorr_TauAP(sortCorrelationValues_SSRecall, tauCorrelation, globalQueryIDList, globalRunIDList, 
					Square_SRecall, SRecall_PerQueryEvalMapAllRun_2, 
					ERR_IA, ERRIA_PerQueryEvalMapAllRun_1);
			//alpha_nDCG
			perQueryCorr_TauAP(sortCorrelationValues_SSRecall, tauCorrelation, globalQueryIDList, globalRunIDList, 
					Square_SRecall, SRecall_PerQueryEvalMapAllRun_2, 
					Alpha_n_DCG, AlphaNDCG_PerQueryEvalMapAllRun_1);	
			//D#-nDCG
			perQueryCorr_TauAP(sortCorrelationValues_SSRecall, tauCorrelation, globalQueryIDList, globalRunIDList, 
					Square_SRecall, SRecall_PerQueryEvalMapAllRun_2, 
					D_Sharp_n_DCG, DSharpNDCG_PerQueryEvalMapAllRun_1);
			*/
			
			//(2) Square_Alpha_nDCG w.r.t. SRecall, AP-IA, ERR-IA, alpha_nDCG, D#-nDCG
			/*
			boolean sortCorrelationValues_SAlpha_nDCG = true;
			//SRecall
			perQueryCorr_TauAP(sortCorrelationValues_SAlpha_nDCG, tauCorrelation, globalQueryIDList, globalRunIDList,
					Square_Alpha_n_DCG, AlphaNDCG_PerQueryEvalMapAllRun_2, 
					SRecall, SRecall_PerQueryEvalMapAllRun_1);	
			//AP-IA
			perQueryCorr_TauAP(sortCorrelationValues_SAlpha_nDCG, tauCorrelation, globalQueryIDList, globalRunIDList, 
					Square_Alpha_n_DCG, AlphaNDCG_PerQueryEvalMapAllRun_2, 
					AP_IA, APIA_PerQueryEvalMapAllRun_1);
			//ERR-IA
			perQueryCorr_TauAP(sortCorrelationValues_SAlpha_nDCG, tauCorrelation, globalQueryIDList, globalRunIDList, 
					Square_Alpha_n_DCG, AlphaNDCG_PerQueryEvalMapAllRun_2, 
					ERR_IA, ERRIA_PerQueryEvalMapAllRun_1);
			//alpha_nDCG
			perQueryCorr_TauAP(sortCorrelationValues_SAlpha_nDCG, tauCorrelation, globalQueryIDList, globalRunIDList, 
					Square_Alpha_n_DCG, AlphaNDCG_PerQueryEvalMapAllRun_2, 
					Alpha_n_DCG, AlphaNDCG_PerQueryEvalMapAllRun_1);	
			//D#-nDCG
			perQueryCorr_TauAP(sortCorrelationValues_SAlpha_nDCG, tauCorrelation, globalQueryIDList, globalRunIDList, 
					Square_Alpha_n_DCG, AlphaNDCG_PerQueryEvalMapAllRun_2, 
					D_Sharp_n_DCG, DSharpNDCG_PerQueryEvalMapAllRun_1);
			*/
			
			//(3) Square_ERRIA w.r.t. SRecall, AP-IA, ERR-IA, alpha_nDCG, D#-nDCG
			/*
			boolean sortCorrelationValues_SERRIA = true;
			perQueryCorr_TauAP(sortCorrelationValues_SERRIA, tauCorrelation, globalQueryIDList, globalRunIDList, 
					Square_ERR_IA, ERRIA_PerQueryEvalMapAllRun_2, 
					SRecall, SRecall_PerQueryEvalMapAllRun_1);
			perQueryCorr_TauAP(sortCorrelationValues_SERRIA, tauCorrelation, globalQueryIDList, globalRunIDList, 
					Square_ERR_IA, ERRIA_PerQueryEvalMapAllRun_2, 
					AP_IA, APIA_PerQueryEvalMapAllRun_1);
			perQueryCorr_TauAP(sortCorrelationValues_SERRIA, tauCorrelation, globalQueryIDList, globalRunIDList, 
					Square_ERR_IA, ERRIA_PerQueryEvalMapAllRun_2, 
					ERR_IA, ERRIA_PerQueryEvalMapAllRun_1);
			perQueryCorr_TauAP(sortCorrelationValues_SERRIA, tauCorrelation, globalQueryIDList, globalRunIDList, 
					Square_ERR_IA, ERRIA_PerQueryEvalMapAllRun_2, 
					Alpha_n_DCG, AlphaNDCG_PerQueryEvalMapAllRun_1);
			perQueryCorr_TauAP(sortCorrelationValues_SERRIA, tauCorrelation, globalQueryIDList, globalRunIDList, 
					Square_ERR_IA, ERRIA_PerQueryEvalMapAllRun_2, 
					D_Sharp_n_DCG, DSharpNDCG_PerQueryEvalMapAllRun_1);
			*/
			
			//(4) ERR-IA w.r.t. Alpha_nDCG, SRecall-IS, ERR-IS, Alpha_nDCG-IS, ERR-IA-Net
			/*
			//
			boolean sortCorrelationValues_SERRIA = true;
			perQueryCorr_TauAP(sortCorrelationValues_SERRIA, tauCorrelation, globalQueryIDList, globalRunIDList, 
					ERR_IA, ERRIA_PerQueryEvalMapAllRun_1, 
					Alpha_n_DCG, AlphaNDCG_PerQueryEvalMapAllRun_1);
			perQueryCorr_TauAP(sortCorrelationValues_SERRIA, tauCorrelation, globalQueryIDList, globalRunIDList, 
					ERR_IA, ERRIA_PerQueryEvalMapAllRun_1, 
					Square_SRecall, SRecall_PerQueryEvalMapAllRun_2);
			perQueryCorr_TauAP(sortCorrelationValues_SERRIA, tauCorrelation, globalQueryIDList, globalRunIDList, 
					ERR_IA, ERRIA_PerQueryEvalMapAllRun_1, 
					Square_ERR_IA_Ari, ari_ERRIA_PerQueryEvalMapAllRun_2);
			perQueryCorr_TauAP(sortCorrelationValues_SERRIA, tauCorrelation, globalQueryIDList, globalRunIDList, 
					ERR_IA, ERRIA_PerQueryEvalMapAllRun_1, 
					Square_Alpha_n_DCG_Ari, ari_AlphaNDCG_PerQueryEvalMapAllRun_2);
			perQueryCorr_TauAP(sortCorrelationValues_SERRIA, tauCorrelation, globalQueryIDList, globalRunIDList, 
					ERR_IA, ERRIA_PerQueryEvalMapAllRun_1, 
					Net_ERRIA, NetERRIA_PerQueryEvalMapAllRun_2);
			*/
			
			//(5) Alpha_nDCG w.r.t. ERR-IA, SRecall-IS, ERR-IS, Alpha_nDCG-IS, ERR-IA-Net
			///*
			//
			boolean sortCorrelationValues_SERRIA = true;
			perQueryCorr_TauAP(sortCorrelationValues_SERRIA, tauCorrelation, globalQueryIDList, globalRunIDList, 
					Alpha_n_DCG, AlphaNDCG_PerQueryEvalMapAllRun_1, 
					ERR_IA, ERRIA_PerQueryEvalMapAllRun_1);
			perQueryCorr_TauAP(sortCorrelationValues_SERRIA, tauCorrelation, globalQueryIDList, globalRunIDList, 
					Alpha_n_DCG, AlphaNDCG_PerQueryEvalMapAllRun_1, 
					Square_SRecall, SRecall_PerQueryEvalMapAllRun_2);
			perQueryCorr_TauAP(sortCorrelationValues_SERRIA, tauCorrelation, globalQueryIDList, globalRunIDList, 
					Alpha_n_DCG, AlphaNDCG_PerQueryEvalMapAllRun_1, 
					Square_ERR_IA_Ari, ari_ERRIA_PerQueryEvalMapAllRun_2);
			perQueryCorr_TauAP(sortCorrelationValues_SERRIA, tauCorrelation, globalQueryIDList, globalRunIDList, 
					Alpha_n_DCG, AlphaNDCG_PerQueryEvalMapAllRun_1, 
					Square_Alpha_n_DCG_Ari, ari_AlphaNDCG_PerQueryEvalMapAllRun_2);
			perQueryCorr_TauAP(sortCorrelationValues_SERRIA, tauCorrelation, globalQueryIDList, globalRunIDList, 
					Alpha_n_DCG, AlphaNDCG_PerQueryEvalMapAllRun_1, 
					Net_ERRIA, NetERRIA_PerQueryEvalMapAllRun_2);
			//*/
			
			
			/////////
			//ASL curve
			/////////
			
			///*
			////2 ASL curve w.r.t. intent-aware
			/*
			ArrayList<ArrayList<Double>> sRecallMatrix_TopicRunID = 
					getTopicRunMatrix(globalQueryNum, globalRunIDList, SRecall_PerQueryEvalMapAllRun_1);
			HSD(Experiment_B, SRecall, sRecallMatrix_TopicRunID, globalQueryNum, globalRunNum);
			
			ArrayList<ArrayList<Double>> apIAMatrix_TopicRunID = 
					getTopicRunMatrix(globalQueryNum, globalRunIDList, APIA_PerQueryEvalMapAllRun_1);
			HSD(Experiment_B, AP_IA, apIAMatrix_TopicRunID, globalQueryNum, globalRunNum);			
			
			ArrayList<ArrayList<Double>> errMatrix_TopicRunID = 
					getTopicRunMatrix(globalQueryNum, globalRunIDList, ERRIA_PerQueryEvalMapAllRun_1);
			HSD(Experiment_B, ERR_IA, errMatrix_TopicRunID, globalQueryNum, globalRunNum);
			
			ArrayList<ArrayList<Double>> alpha_nDCGMatrix_TopicRunID = 
					getTopicRunMatrix(globalQueryNum, globalRunIDList, AlphaNDCG_PerQueryEvalMapAllRun_1);
			HSD(Experiment_B, Alpha_n_DCG, alpha_nDCGMatrix_TopicRunID, globalQueryNum, globalRunNum);
			
			ArrayList<ArrayList<Double>> dSharp_nDCGMatrix_TopicRunID = 
					getTopicRunMatrix(globalQueryNum, globalRunIDList, DSharpNDCG_PerQueryEvalMapAllRun_1);
			HSD(Experiment_B, D_Sharp_n_DCG, dSharp_nDCGMatrix_TopicRunID, globalQueryNum, globalRunNum);
			
			////2 ASL curve w.r.t. Intent-square
			ArrayList<ArrayList<Double>> squareSRecall_TopicRunID = 
					getTopicRunMatrix(globalQueryNum, globalRunIDList, SRecall_PerQueryEvalMapAllRun_2);			
			HSD(Experiment_B, Square_SRecall, squareSRecall_TopicRunID, globalQueryNum, globalRunNum);
			
			ArrayList<ArrayList<Double>> squareErrMatrix_TopicRunID = 
					getTopicRunMatrix(globalQueryNum, globalRunIDList, ERRIA_PerQueryEvalMapAllRun_2);			
			HSD(Experiment_B, Square_ERR_IA, squareErrMatrix_TopicRunID, globalQueryNum, globalRunNum);
			
			ArrayList<ArrayList<Double>> squareAlpha_nDCGMatrix_TopicRunID = 
					getTopicRunMatrix(globalQueryNum, globalRunIDList, AlphaNDCG_PerQueryEvalMapAllRun_2);			
			HSD(Experiment_B, Square_Alpha_n_DCG, squareAlpha_nDCGMatrix_TopicRunID, globalQueryNum, globalRunNum);
			
			ArrayList<ArrayList<Double>> netERRIA_Matrix_TopicRunID = 
					getTopicRunMatrix(globalQueryNum, globalRunIDList, NetERRIA_PerQueryEvalMapAllRun_2);
			HSD(Experiment_B, Net_ERRIA, netERRIA_Matrix_TopicRunID, globalQueryNum, globalRunNum);
			
			*/
			
			
			
			
			////////Specific Query based comparison
			
			/*
			HashMap<String, ArrayList<Pair<String, Double>>> SpecificRunSetMap = 
					getSpecificRunSet(globalQueryIDList, cutoff, 0.4, allRuns, globalRunNum);
			
			compareWithSpecificRunSet(cutoff, SpecificRunSetMap, allRuns);
			*/			
			
			
		}else {
			System.err.println("Unsupported NTCIR11_TOPIC_TYPE error!");
		}
	}
	
	
	//////////
	//Experimental Analysis
	//////////
	
	
	
	/////
	
	private static void getAvgFlsSRecallVersusAvgSlsSRecall(HashMap<String, HashMap<String, ArrayList<String>>> allRuns,
			HashSet<String> filteredTopicSet, HashMap<String, Pair<String, Double>> SRecall_AvgMapAllRun_Fls, 
			HashMap<String, Pair<String, Double>> SRecall_AvgMapAllRun_Sls){
		
		ArrayList<String> runIDList = new ArrayList<String>();
		//fls
		ArrayList<Double> avgSRecallList_Fls = new ArrayList<Double>();
		//sls
		ArrayList<Double> avgSRecallList_Sls = new ArrayList<Double>();
		
		for(Entry<String, HashMap<String, ArrayList<String>>> run: allRuns.entrySet()){
			String runID = run.getKey();
			
			if(filteredTopicSet.contains(runID)){
				continue;
			}
			
			runIDList.add(runID);
			avgSRecallList_Fls.add(SRecall_AvgMapAllRun_Fls.get(runID).getSecond());
			avgSRecallList_Sls.add(SRecall_AvgMapAllRun_Sls.get(runID).getSecond());
		}
		
		//run
		System.out.println("##\tAvgFlsSRecall vs. AvgSlsSRecall");
		System.out.println("runIDList:");
		for(String runID: runIDList){
			System.out.print(runID+"\t");
		}
		System.out.println();
		System.out.println();
		
		//fls
		System.out.println("Fls avgSRecall:");
		for(Double flsAvgSRecall: avgSRecallList_Fls){
			System.out.print(resultFormat.format(flsAvgSRecall)+"\t");
		}
		System.out.println();
		System.out.println();
		
		//sls
		System.out.println("Sls avgSRecall:");
		for(Double slsAvgSRecall: avgSRecallList_Sls){
			System.out.print(resultFormat.format(slsAvgSRecall)+"\t");
		}
		System.out.println();
		System.out.println();		
	}
	
	
	
	///////////////////////////////
	
	/////
	//Test collection information
	/////
	private static void collectionInfo(HashSet<String> filteredTopicSet){
		int qNum = 0;
		int flsNum = 0;
		int slsNum = 0;
		
		//for global usage
		for(TwoLevelTopic topic: _2LTList){
			String queryid = topic.getTopicID();				
			//filter the topics if it includes a subtopic that has no second-level subtopic
			if(filteredTopicSet.contains(queryid)){
				continue;
			}			
			
			if(queryid.equals("0080")){
				subtopicInfor(queryid, topic);
			}
			
			qNum++;
			
			int flsCount = topic._flsList.size();
			
			flsNum += flsCount;
			
			for(int fls_i=1; fls_i<=flsCount; fls_i++){
				ArrayList<Pair<String, Double>> slsList = topic._slsSetList.get(fls_i-1);
				slsNum += slsList.size();
			}			
		}
		
		System.out.println("Query Number:\t"+qNum);
		double avgFlsPerQ = (1.0*flsNum)/qNum;
		System.out.println("Avg. number of fls per query:\t"+resultFormat.format(avgFlsPerQ));
		double avgSlsPerFls = (slsNum*1.0)/flsNum;
		System.out.println("Avg. number of sls per fls:\t"+resultFormat.format(avgSlsPerFls));
	}
	
	//
	private static void subtopicInfor(String queryid, TwoLevelTopic topic){
		System.out.println("---"+queryid+"---");
		int flsCount = topic._flsList.size();
		System.out.println("fls number:\t"+flsCount);
		System.out.println();
		for(int fls_i=1; fls_i<=flsCount; fls_i++){
			ArrayList<Pair<String, Double>> slsList = topic._slsSetList.get(fls_i-1);
			System.out.println("sls number for fls-"+fls_i+":\t"+slsList.size());
		}
		System.out.println();
		
	}
	
	
	//////////////////////////
	//Main
	//////////////////////////
	public static void main(String []args){
		//////////////
		//CIKM2015
		//////////////
		//1
		//MetricComparison.test(true);
		/*
		boolean awareEqual = true;
		boolean squareEqual = false;
		MetricComparison.test_2(awareEqual, squareEqual);
		*/
		
		//2
		/*
		boolean awareEqual = true;
		boolean squareEqual = true;
		int cutoff = 20;
		*/
		/*
		MetricComparison.metricValue(NTCIR_EVAL_TASK.NTCIR11_DR_EN, NTCIR11_TOPIC_TYPE.UNCLEAR, 
				awareEqual, STLevel.FLS, cutoff, squareEqual);
		*/
		//
		/*
		MetricComparison.metricValue(NTCIR_EVAL_TASK.NTCIR11_DR_CH, NTCIR11_TOPIC_TYPE.UNCLEAR, 
				awareEqual, STLevel.FLS, cutoff, squareEqual);
		*/
		
		//3
		//MetricComparison.testShuffle();
		
		//4 ASL curve
		///*
		//MetricComparison metricComparison = new MetricComparison();
		//boolean awareEqual = true;
		//boolean squareEqual = true;
		//int cutoff = 20;
		//MetricComparison.metricValue(NTCIR_EVAL_TASK.NTCIR11_DR_CH, NTCIR11_TOPIC_TYPE.UNCLEAR, awareEqual, cutoff, squareEqual);
		//metricComparison.metricValue(NTCIR_EVAL_TASK.NTCIR11_DR_EN, NTCIR11_TOPIC_TYPE.UNCLEAR, awareEqual, cutoff, squareEqual);
		//*/
		
		//5 Case Calculation
		//MetricComparison metricComparison = new MetricComparison();
		
		//case-1
		/*
		DivCase case1 = DivCase.Case_1;
		//equal fls probability		
		boolean awareEqual = true;
		metricComparison.caseCal(awareEqual, awareEqual, case1);
		//biased fls probability
		//boolean awareEqual = false;
		//metricComparison.caseCal(awareEqual, awareEqual, case1);
		*/
		//case-2
		/*
		DivCase case2 = DivCase.Case_2;
		//equal fls probability		
		boolean awareEqual = true;
		metricComparison.caseCal(awareEqual, awareEqual, case2);
		//biased fls probability
		//boolean awareEqual = false;
		//metricComparison.caseCal(awareEqual, awareEqual, case2);
		*/
		//case-3
		/*
		DivCase case3 = DivCase.Case_3;
		//equal fls probability		
		//boolean awareEqual = true;
		//metricComparison.caseCal(awareEqual, awareEqual, case3);
		//biased fls probability
		boolean awareEqual = false;
		metricComparison.caseCal(awareEqual, awareEqual, case3);
		*/
		
		//6 collection information
		/*
		MetricComparison metricComparison = new MetricComparison();
		boolean awareEqual = true;
		boolean squareEqual = true;
		int cutoff = 20;
		metricComparison.metricValue(NTCIR_EVAL_TASK.NTCIR11_DR_EN, NTCIR11_TOPIC_TYPE.UNCLEAR, awareEqual, cutoff, squareEqual);
		*/
		
		
		
		
		////////////////////
		//WSDM2015
		////////////////////
		MetricComparison metricComparison = new MetricComparison();
		boolean awareEqual = true;
		boolean squareEqual = true;
		boolean hierarchicalDiv = false;
		int cutoff = 40;
		
		//## English ##
		///*
		metricComparison.metricValue(NTCIR_EVAL_TASK.NTCIR11_DR_EN, NTCIR11_TOPIC_TYPE.UNCLEAR, awareEqual,
				cutoff, squareEqual, hierarchicalDiv);
		//*/

		//## Chinese ##
		/*
		MetricComparison.metricValue(NTCIR_EVAL_TASK.NTCIR11_DR_CH, NTCIR11_TOPIC_TYPE.UNCLEAR, awareEqual,
				cutoff, squareEqual, hierarchicalDiv);
		*/
		//Performance score
		
	}
	
}
