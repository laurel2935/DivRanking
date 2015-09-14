package org.archive.ireval;

import java.io.BufferedWriter;
import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.archive.dataset.ntcir.NTCIRLoader;
import org.archive.dataset.ntcir.NTCIRLoader.NTCIR11_TOPIC_LEVEL;
import org.archive.dataset.ntcir.NTCIRLoader.NTCIR11_TOPIC_TYPE;
import org.archive.dataset.ntcir.NTCIRLoader.NTCIR_EVAL_TASK;
import org.archive.util.io.IOText;
import org.archive.util.tuple.Pair;
import org.archive.util.tuple.PairComparatorBySecond_Desc;
import org.archive.util.tuple.Triple;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

public class PreProcessor {
	
	public static final boolean debug = false;
	
	private static DecimalFormat resultFormat = new DecimalFormat("#.####");
	
	//
	public static ArrayList<TwoLevelTopic> _2LTList = new ArrayList<TwoLevelTopic>();
	//{queryid -> TwoLevelTopic}
	public static HashMap<String, TwoLevelTopic> _2LTMap = new HashMap<String, TwoLevelTopic>();

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
	public static ArrayList<Triple<String, String, Integer>> getXmlQrel(String qrelDir, NTCIRLoader.NTCIR_EVAL_TASK eval, NTCIRLoader.NTCIR11_TOPIC_TYPE type){
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
	
	/**
	 * generate standard qrel file
	 * **/
	public static void generateQrelFile(String xmlDir, String dir, NTCIR_EVAL_TASK eval, NTCIR11_TOPIC_TYPE type, NTCIR11_TOPIC_LEVEL stLevel){
		
		ArrayList<Triple<String, String, Integer>> triList = getXmlQrel(xmlDir, eval, type);
		
		HashSet<String> topicSet = new HashSet<String>();
		
		try {						
			if(type == NTCIR11_TOPIC_TYPE.CLEAR){
				
				String file = null;
				
				if(eval == NTCIR_EVAL_TASK.NTCIR11_DR_CH){
					file = dir+"IMine-DR-Qrel-C-Clear";
				}else if(eval == NTCIR_EVAL_TASK.NTCIR11_DR_EN){
					file = dir+"IMine-DR-Qrel-E-Clear";
				}
				//used topic
				ArrayList<String> topicList = NTCIRLoader.loadNTCIR11Topic(eval, type);
				topicSet.addAll(topicList);
				
				BufferedWriter writer = IOText.getBufferedWriter_UTF8(file);
				
				for(Triple<String, String, Integer> triple: triList){
					if(topicSet.contains(triple.getSecond())){
						writer.write(triple.getSecond()+" "+triple.getFirst()+" "+"L"+triple.getThird());
						writer.newLine();
					}					
				}				
				writer.flush();
				writer.close();
				
			}else if(type == NTCIR11_TOPIC_TYPE.UNCLEAR){
				
				String dFile = null;
				String iFile = null;
				
				if(eval == NTCIR_EVAL_TASK.NTCIR11_DR_CH){					
					dFile = dir+"IMine-DR-C-Unclear-Dqrels-";
					iFile = dir+"IMine-DR-C-Unclear-Iprob-";
					
					String chLevelFile = xmlDir + "IMine.Qrel.SMC/IMine.Qrel.SMC.xml";
					load2LT(chLevelFile);					
				}else if(eval == NTCIR_EVAL_TASK.NTCIR11_DR_EN){					
					dFile = dir+"IMine-DR-E-Unclear-Dqrels-";
					iFile = dir+"IMine-DR-E-Unclear-Iprob-";
					
					String enLevelFile = xmlDir + "IMine.Qrel.SME/IMine.Qrel.SME.xml";
					load2LT(enLevelFile);
				}
				
				ArrayList<String> topicList = NTCIRLoader.loadNTCIR11Topic(eval, type);
				topicSet.addAll(topicList);
				
				if(stLevel == NTCIR11_TOPIC_LEVEL.FLS){
					
					dFile += NTCIR11_TOPIC_LEVEL.FLS.toString();
					iFile += NTCIR11_TOPIC_LEVEL.FLS.toString();
					
					BufferedWriter dWriter = IOText.getBufferedWriter_UTF8(dFile);					
					for(Triple<String, String, Integer> triple: triList){
						String [] array = triple.getSecond().split("\t");
						String queryid = array[0];
						String slsStr = array[1];
						
						if(topicSet.contains(queryid)){	
							//mapping from sls's content to fls's id
							Pair<Integer, Integer> e = _2LTMap.get(queryid)._slsContentMap.get(slsStr);
							if(null == e){
								System.err.println(queryid+"\t"+slsStr);
								continue;
							}
							//mapped 1st-level subtopic id
							dWriter.write(queryid+" "+e.getFirst()+" "+triple.getFirst()+" "+"L"+triple.getThird());
							dWriter.newLine();							
						}					
					}					
					dWriter.flush();
					dWriter.close();
					
					BufferedWriter iWriter = IOText.getBufferedWriter_UTF8(iFile);					
					for(String id: topicList){
						TwoLevelTopic t = _2LTMap.get(id);
						int flsID = 1;
						for(; flsID<=t._flsList.size(); flsID++){
							Pair<String, Double> fls = t._flsList.get(flsID-1);
							iWriter.write(id+" "+flsID+" "+fls.getSecond());
							iWriter.newLine();
						}
					}
					iWriter.flush();
					iWriter.close();
					
				}else{
					
					dFile += NTCIR11_TOPIC_LEVEL.SLS.toString();
					iFile += NTCIR11_TOPIC_LEVEL.SLS.toString();
					
					BufferedWriter dWriter = IOText.getBufferedWriter_UTF8(dFile);					
					for(Triple<String, String, Integer> triple: triList){
						String [] array = triple.getSecond().split("\t");
						String queryid = array[0];
						String slsStr = array[1];
						
						if(topicSet.contains(queryid)){							
							Pair<Integer, Integer> e = _2LTMap.get(queryid)._slsContentMap.get(slsStr);
							if(null == e){
								System.err.println(queryid+"\t"+slsStr);
								continue;
							}
							dWriter.write(queryid+" "+e.getSecond()+" "+triple.getFirst()+" "+"L"+triple.getThird());
							dWriter.newLine();							
						}					
					}					
					dWriter.flush();
					dWriter.close();
					
					BufferedWriter iWriter = IOText.getBufferedWriter_UTF8(iFile);					
					for(String id: topicList){
						TwoLevelTopic t = _2LTMap.get(id);
						
						int slsID = 1;						 
						for(ArrayList<Pair<String, Double>> slsSet: t._slsSetList){
							for(Pair<String, Double> sls: slsSet){
								iWriter.write(id+" "+(slsID++)+" "+sls.getSecond());
								iWriter.newLine();
							}							
						}
					}
					iWriter.flush();
					iWriter.close();
				}				
			} else{
				System.err.println("Type Error!");
				System.exit(0);
			}			
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}
	
	/**
	 * find special documents, like the document that is relevant to multiple 1st-level subtopics
	 * **/
	public static void findDoc(){
		//ch
		String xmlDir = "C:/T/Research/CurrentResearch/NTCIR/NTCIR-11/Ntcir11-IMine/Eval-IMine/0913/";
		
		NTCIRLoader.NTCIR_EVAL_TASK eval = NTCIR_EVAL_TASK.NTCIR11_DR_CH;
		NTCIR11_TOPIC_TYPE type = NTCIR11_TOPIC_TYPE.UNCLEAR;
		
		ArrayList<Triple<String, String, Integer>> triList = getXmlQrel(xmlDir, eval, type);
		
		//IMine.Qrel.SMC.xml
		String chLevelFile = xmlDir + "IMine.Qrel.SMC/IMine.Qrel.SMC.xml";
		load2LT(chLevelFile);
		
		//topic
		HashSet<String> topicSet = new HashSet<String>();
		ArrayList<String> topicList = NTCIRLoader.loadNTCIR11Topic(eval, type);
		topicSet.addAll(topicList);
		
		Map<String, HashSet<Integer>> releMap = new HashMap<String, HashSet<Integer>>();
		
		for(Triple<String, String, Integer> triple: triList){
			String [] array = triple.getSecond().split("\t");
			String queryid = array[0];
			String slsStr = array[1];
			
			if(topicSet.contains(queryid)){	
				//mapping from sls's content to fls's id
				Pair<Integer, Integer> e = _2LTMap.get(queryid)._slsContentMap.get(slsStr);
				if(null == e){
					System.err.println(queryid+"\t"+slsStr);
					continue;
				}
				
				//document's relevant w.r.t. 1st-level subtopic id
				//dWriter.write(queryid+" "+e.getFirst()+" "+triple.getFirst()+" "+"L"+triple.getThird());
				//dWriter.newLine();
				
				Integer flsID = e.getFirst();
				String docID = triple.getFirst();
				
				if(releMap.containsKey(docID)){
					releMap.get(docID).add(flsID);					
				}else{
					HashSet<Integer> flsSet = new HashSet<Integer>();
					flsSet.add(flsID);
					releMap.put(docID, flsSet);
				}
			}
			
			//
			for(Entry<String, HashSet<Integer>> entry: releMap.entrySet()){
				if(entry.getValue().size() > 1){
					System.out.println(entry.getKey() +"\t"+entry.getValue().size());
				}
			}
		}
	}
	
	/**
	 * perform basic statistics w.r.t. the official dataset
	 * **/
	//Temporalia-1
	
	
	//// for analyze dependency of relevance among documents
	//docid -> {queryid -> Set of slsStr}
	private static HashMap<String, HashMap<String, HashSet<String>>> docUsageMap = null;
	
	public static void getDocUsageMap(String xmlDir, NTCIR_EVAL_TASK eval, NTCIR11_TOPIC_TYPE type){
		//1 ini
		docUsageMap = new HashMap<String, HashMap<String,HashSet<String>>>();
		
		ArrayList<Triple<String, String, Integer>> triList = null;
		
		if(type == NTCIR11_TOPIC_TYPE.CLEAR){
			System.err.println("Input type error!");
		}else{
			triList = getXmlQrel(xmlDir, eval, type);
		}
		
		//
		for(Triple<String, String, Integer> triple: triList){
			String [] array = triple.getSecond().split("\t");
			String queryid = array[0];
			String slsStr = array[1];
			String docid = triple.getFirst();
			
			if(docUsageMap.containsKey(docid)){
				HashMap<String, HashSet<String>> docUsage = docUsageMap.get(docid);
				
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
				
				docUsageMap.put(docid, docUsage);
			}
		}		
	}
	/**
	 * the commonly used metric of ERR-IA
	 * @param flsID	for a specific subtopic
	 * **/
	public static double ERRIA_Common(String queryid, int flsID, ArrayList<String> sysList, int cutoff,
			TwoLevelTopic twoLevelTopic){
		
		if(null==sysList || sysList.size() == 0){
			return 0.0;
		}
		
		double err = 0.0;
		double disPro = 1.0;		
		
		//without enough documents provided
		int acceptedCut = Math.min(sysList.size(), cutoff);
		
		for(int i=0; i<acceptedCut; i++){
			String docid = sysList.get(i);
			
			if(docUsageMap.containsKey(docid)){
				
				HashMap<String, HashSet<String>> qMap = docUsageMap.get(docid);
				if(qMap.containsKey(queryid)){
					HashSet<String> slsSet = qMap.get(queryid);
					
					double satPro = twoLevelTopic.getRelePro(flsID, slsSet);
															
					double tem = 1.0/(i+1)*satPro*disPro;
					
					err += tem;
					
					disPro *= (1-satPro);					
				}
				
			}
		}
		
		if(Double.isNaN(err)){
			System.out.println(queryid);
		}
		
		return err;		
	}
	/**
	 * the proposed ERR-IA^{net}
	 * **/
	public static double ERRIA_Updated(String queryid, int flsID, ArrayList<String> sysList, int cutoff,
			TwoLevelTopic twoLevelTopic){
		
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
			
			if(docUsageMap.containsKey(docid)){
				
				HashMap<String, HashSet<String>> qMap = docUsageMap.get(docid);
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
	
	/**
	 * @param xmlDir	official subtopic hierarchy
	 * @param runDir	directory of runs		
	 * @param eval		NTCIR_EVAL_TASK.NTCIR11_DR_CH or NTCIR_EVAL_TASK.NTCIR11_DR_EN
	 * @param type		clear or unclear topics, of course, should be unclear ones
	 * **/
	
	public static void compareMetricERRIA(NTCIR_EVAL_TASK eval, NTCIR11_TOPIC_TYPE type, int cutoff){
		
		String xmlDir = "C:/T/Research/CurrentResearch/NTCIR/NTCIR-11/Ntcir11-IMine/Eval-IMine/0913/";
		String runDir = null;
		
		//filtered due to no sls, i.e., 0003(4), 0017(2) for Ch
		HashSet<String> filteredTopicSet = new HashSet<String>();
		
		if(type == NTCIR11_TOPIC_TYPE.UNCLEAR){			
			if(eval == NTCIR_EVAL_TASK.NTCIR11_DR_CH){
				String chLevelFile = xmlDir + "IMine.Qrel.SMC/IMine.Qrel.SMC.xml";
				load2LT(chLevelFile);					
				
				runDir = "C:/T/Research/CurrentResearch/NTCIR/NTCIR-11/Ntcir11-IMine/Eval-IMine/IMine.submitted/DR-Run/C/";
				
				filteredTopicSet.add("0003");
				filteredTopicSet.add("0017");
			}else if(eval == NTCIR_EVAL_TASK.NTCIR11_DR_EN){					
				String enLevelFile = xmlDir + "IMine.Qrel.SME/IMine.Qrel.SME.xml";
				load2LT(enLevelFile);
				
				runDir = "C:/T/Research/CurrentResearch/NTCIR/NTCIR-11/Ntcir11-IMine/Eval-IMine/IMine.submitted/DR-Run/E/";
				
				filteredTopicSet.add("0070");
			}
			
			//runs to be evaluated
			HashMap<String, HashMap<String, ArrayList<String>>> allRuns = loadSysRuns(runDir);
			
			//
			getDocUsageMap(xmlDir, eval, type);
			
			//compute common err
			///*
			HashMap<String, ArrayList<Double>> commonERRIAMap = new HashMap<String, ArrayList<Double>>();
			
			for(Entry<String, HashMap<String, ArrayList<String>>> run: allRuns.entrySet()){
				String runID = run.getKey();
				HashMap<String, ArrayList<String>> runMap = run.getValue();
				ArrayList<Double> commonErrIAList = new ArrayList<Double>();				
				
				for(TwoLevelTopic topic: _2LTList){
					String queryid = topic.getTopicID();
					
					if(filteredTopicSet.contains(queryid)){
						continue;
					}
					
					int flsCount = topic._flsList.size();
					
					double err = 0.0;
					for(int i=1; i<=flsCount; i++){
						err += (topic.getFlsPro(i)*ERRIA_Common(queryid, i, runMap.get(queryid), cutoff, topic));
					}
					
					commonErrIAList.add(err);
				}
				
				commonERRIAMap.put(runID, commonErrIAList);
			}
			//*/
			
			//compute updated err
			HashMap<String, ArrayList<Double>> updatedERRIAMap = new HashMap<String, ArrayList<Double>>();
			for(Entry<String, HashMap<String, ArrayList<String>>> run: allRuns.entrySet()){
				String runID = run.getKey();
				HashMap<String, ArrayList<String>> runMap = run.getValue();
				ArrayList<Double> updatedErrIAList = new ArrayList<Double>();				
				
				for(TwoLevelTopic topic: _2LTList){
					String queryid = topic.getTopicID();
					
					if(filteredTopicSet.contains(queryid)){
						continue;
					}					
					
					int flsCount = topic._flsList.size();
					
					double updatedERR = 0.0;
					for(int i=1; i<=flsCount; i++){
						updatedERR += (topic.getFlsPro(i)*ERRIA_Updated(queryid, i, runMap.get(queryid), cutoff, topic));
					}
					
					updatedErrIAList.add(updatedERR);
				}
				
				updatedERRIAMap.put(runID, updatedErrIAList);
			}
			
			//for computing avg
			ArrayList<String> runIDList = new ArrayList<String>();
			ArrayList<Double> avgCommonERRIAList = new ArrayList<Double>();
			ArrayList<Double> avgUpdatedERRIAList = new ArrayList<Double>();
			
			//output-1 specific results
			for(Entry<String, ArrayList<Double>> resultPerRun: commonERRIAMap.entrySet()){
				
				runIDList.add(resultPerRun.getKey());
				
				System.out.println(resultPerRun.getKey()+"\t"+"CommonERRIA(line-1) vs. UpdatedERRIA(line-2):");
				
				double comSum = 0.0;
				for(Double commonErr: resultPerRun.getValue()){
					//System.out.print(commonErr.doubleValue()+" ");
					comSum += commonErr.doubleValue();
					System.out.print(resultFormat.format(commonErr.doubleValue())+"\t");
				}
				avgCommonERRIAList.add(comSum/resultPerRun.getValue().size());
				
				System.out.println();
				
				
				double upSum = 0.0;
				//System.out.println(resultPerRun.getKey()+"\t"+":");
				for(Double updatedErr: updatedERRIAMap.get(resultPerRun.getKey())){
					//System.out.print(updatedErr.doubleValue()+" ");
					upSum += updatedErr.doubleValue();
					System.out.print(resultFormat.format(updatedErr.doubleValue())+"\t");
				}
				avgUpdatedERRIAList.add(upSum/updatedERRIAMap.get(resultPerRun.getKey()).size());
				
				System.out.println();
				System.out.println();				
			}
			
			//output-2: avg results
			System.out.println("Avg:");
			for(String id: runIDList){
				System.out.print(id+"\t");
			}
			System.out.println();
			for(Double avgComERRIA: avgCommonERRIAList){
				System.out.print(resultFormat.format(avgComERRIA.doubleValue())+"\t");
			}
			System.out.println();
			for(Double avgUpERRIA: avgUpdatedERRIAList){
				System.out.print(resultFormat.format(avgUpERRIA.doubleValue())+"\t");
			}
			System.out.println();
			
			//order systems by their avg
			ArrayList<Pair<Integer, Double>> rankedSysByCom = new ArrayList<Pair<Integer,Double>>();
			ArrayList<Pair<Integer, Double>> rankedSysByUp = new ArrayList<Pair<Integer,Double>>();
			
			for(int i=0; i<runIDList.size(); i++){
				rankedSysByCom.add(new Pair<Integer, Double>(i+1, avgCommonERRIAList.get(i)));
				
				rankedSysByUp.add(new Pair<Integer, Double>(i+1, avgUpdatedERRIAList.get(i)));
			}
			
			Collections.sort(rankedSysByCom, new PairComparatorBySecond_Desc<Integer, Double>());
			Collections.sort(rankedSysByUp, new PairComparatorBySecond_Desc<Integer, Double>());
			
			System.out.println();
			//1
			System.out.println("Order by avgComERR-IA:");
			for(Pair<Integer, Double> comR: rankedSysByCom){
				System.out.print(comR.getFirst()+"\t");
			}
			System.out.println();
			for(Pair<Integer, Double> comR: rankedSysByCom){
				System.out.print(resultFormat.format(comR.getSecond())+"\t");
			}
			System.out.println();
			//2
			System.out.println();
			System.out.println("Order by avgUpERR-IA:");
			for(Pair<Integer, Double> upR: rankedSysByUp){
				System.out.print(upR.getFirst()+"\t");
			}
			System.out.println();
			for(Pair<Integer, Double> upR: rankedSysByUp){
				System.out.print(resultFormat.format(upR.getSecond())+"\t");
			}
			System.out.println();
			
		}else {
			System.err.println("Unsupported type error!");
		}
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
	
	//
	public static void main(String []args){
		//1
		/*
		String file = "H:/v-haiyu/TaskPreparation/Ntcir11-IMine/Eval-IMine/IMine.Qrel.SMC/IMine.Qrel.SMC.xml";
		PreProcessor.load2LT(file);
		*/
		
		//2
		//String dir = "H:/CurrentResearch/Ntcir11-IMine/Eval-IMine/20140830/CheckEval/";
		//String dir = "H:/CurrentResearch/Ntcir11-IMine/Eval-IMine/0913/CheckEval/";
		//String xmlDir = "H:/CurrentResearch/Ntcir11-IMine/Eval-IMine/0913/";
		
		//ch, clear
		//PreProcessor.generateQrelFile(xmlDir, dir, NTCIR_EVAL_TASK.NTCIR11_DR_CH, NTCIR11_TOPIC_TYPE.CLEAR, null);		
		//ch, unclear, fls
		//PreProcessor.generateQrelFile(xmlDir, dir, NTCIR_EVAL_TASK.NTCIR11_DR_CH, NTCIR11_TOPIC_TYPE.UNCLEAR, NTCIR11_TOPIC_LEVEL.FLS);		
		//ch, unclear, sls
		//PreProcessor.generateQrelFile(xmlDir, dir, NTCIR_EVAL_TASK.NTCIR11_DR_CH, NTCIR11_TOPIC_TYPE.UNCLEAR, NTCIR11_TOPIC_LEVEL.SLS);
		
		//en, clear
		//PreProcessor.generateQrelFile(xmlDir, dir, NTCIR_EVAL_TASK.NTCIR11_DR_EN, NTCIR11_TOPIC_TYPE.CLEAR, null);
		//en, unclear, fls
		//PreProcessor.generateQrelFile(xmlDir, dir, NTCIR_EVAL_TASK.NTCIR11_DR_EN, NTCIR11_TOPIC_TYPE.UNCLEAR, NTCIR11_TOPIC_LEVEL.FLS);
		//en, unclear, sls
		//PreProcessor.generateQrelFile(xmlDir, dir, NTCIR_EVAL_TASK.NTCIR11_DR_EN, NTCIR11_TOPIC_TYPE.UNCLEAR, NTCIR11_TOPIC_LEVEL.SLS);
		
		//3 
		//PreProcessor.findDoc();
		
		
		//4	test load runs
		/*
		String comDir = "C:/T/Research/CurrentResearch/NTCIR/NTCIR-11/Ntcir11-IMine/Eval-IMine/IMine.submitted/DR-Run/";
		String dir = comDir+"C/";
		PreProcessor.loadSysRuns(dir);
		*/
		
		//5	compared results of different versions of ERR-IA
		//PreProcessor.compareMetricERRIA(NTCIR_EVAL_TASK.NTCIR11_DR_CH, NTCIR11_TOPIC_TYPE.UNCLEAR, 20);
		PreProcessor.compareMetricERRIA(NTCIR_EVAL_TASK.NTCIR11_DR_EN, NTCIR11_TOPIC_TYPE.UNCLEAR, 20);
	}
}
