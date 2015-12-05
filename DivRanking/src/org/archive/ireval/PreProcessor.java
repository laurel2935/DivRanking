package org.archive.ireval;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.archive.dataset.DataSetDiretory;
import org.archive.dataset.ntcir.NTCIRLoader;
import org.archive.dataset.ntcir.NTCIRLoader.NTCIR11_TOPIC_LEVEL;
import org.archive.dataset.ntcir.NTCIRLoader.NTCIR11_TOPIC_TYPE;
import org.archive.dataset.ntcir.NTCIRLoader.NTCIR_EVAL_TASK;
import org.archive.ireval.toy.Accessor;
import org.archive.util.FileFinder;
import org.archive.util.Language.Lang;
import org.archive.util.format.StandardFormat;
import org.archive.util.io.IOText;
import org.archive.util.tuple.Pair;
import org.archive.util.tuple.PairComparatorBySecond_Desc;
import org.archive.util.tuple.StrStr;
import org.archive.util.tuple.Triple;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

public class PreProcessor {
	
	public static final boolean debug = false;
	public static final String NEWLINE = System.getProperty("line.separator");
	
	public static final String br = "<br>";
	
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
	 * <2> second relevance part merely corresponds to broad and ambiguous queries, besides the same component in the first part,
	 *     it also indicates the 2nd-level subtopic to which a document is in fact relevant.
	 *     And a document may be relevant to several 2nd-level subtopics. 
	 *     
	 * Return format
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
	public static void generateQrelFile(String xmlDir, String dir, NTCIR_EVAL_TASK eval,
			NTCIR11_TOPIC_TYPE type, NTCIR11_TOPIC_LEVEL stLevel){
		
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
						//since use the second-part to get the desired relevance
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
						//since use the second-part to get the desired relevance
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
	
	
	
	///////
	//Marginal Utility
	///////
	class DocReleFingerprint{
		String _docid;
		//flsID-> flsReleDegree
		//HashMap<Integer, Integer> _flsReleMap;		
		//queryid->
		HashMap<String, HashMap<Integer, Integer>> _q2FlsReleMap;
		//flsID-> {slsID->slsReleDegree}
		//HashMap<Integer, HashMap<Integer, Integer>> _slsReleMap;
		//queryid->
		HashMap<String, HashMap<Integer, HashMap<Integer, Integer>>> _q2SlsReleMap;
		
		DocReleFingerprint(String docid){
			this._docid = docid;
			this._q2FlsReleMap = new HashMap<>();
			this._q2SlsReleMap = new HashMap<>();
		}
		// docid | queryid+"\t"+slsStr | releLevel
		public void addReleRecord(String queryid, int flsID, int slsID, int releDegree){			
			//check w.r.t. fls
			if(_q2FlsReleMap.containsKey(queryid)){
				HashMap<Integer, Integer> flsReleMap = _q2FlsReleMap.get(queryid);
				if(flsReleMap.containsKey(flsID)){
					int curReleDegree = flsReleMap.get(flsID);
					//update to the highest relevance degree
					if(curReleDegree < releDegree){
						flsReleMap.put(flsID, releDegree);
					}
				}else{
					flsReleMap.put(flsID, releDegree);
				}
			}else{
				HashMap<Integer, Integer> flsReleMap = new HashMap<>();
				flsReleMap.put(flsID, releDegree);
				_q2FlsReleMap.put(queryid, flsReleMap);
			}			
			
			//check w.r.t. sls
			if(_q2SlsReleMap.containsKey(queryid)){
				HashMap<Integer, HashMap<Integer, Integer>> slsReleMap = _q2SlsReleMap.get(queryid);
				if(slsReleMap.containsKey(flsID)){
					HashMap<Integer, Integer> slsMap = slsReleMap.get(flsID);
					slsMap.put(slsID, releDegree);
				}else{
					HashMap<Integer, Integer> slsMap = new HashMap<>();
					slsMap.put(slsID, releDegree);
					slsReleMap.put(flsID, slsMap);
				}
			}else{
				HashMap<Integer, HashMap<Integer, Integer>> slsReleMap = new HashMap<>();
				HashMap<Integer, Integer> slsMap = new HashMap<>();
				slsMap.put(slsID, releDegree);
				slsReleMap.put(flsID, slsMap);
				
				_q2SlsReleMap.put(queryid, slsReleMap);
			}			
		}		
	}
	
	//
	public void getDocReleMap_marginal_unclear(String xmlDir, String dir, NTCIR_EVAL_TASK eval,
			NTCIR11_TOPIC_TYPE type, String outputDir){
		
		////if needed
		//loadDocs();		
		HashMap<String, String> docid2HtmlMap = null;
		
		////1
		ArrayList<Triple<String, String, Integer>> triList = getXmlQrel(xmlDir, eval, type);		
		HashSet<String> topicSet = new HashSet<String>();
		
		////2
		String egStr = null;
		String outputUncertainMUPairFile = null;
		String outputYesSureMUPairFile = null;
		String outputNoSureMUPairFile = null;
		
		//for getting irrelevant documents
		HashMap<String, ArrayList<String>> baselineMap = null;
		ArrayList<String> usedIrrelDocList = new ArrayList<>();
		HashSet<String> usedReleDocSet = new HashSet<>();
		
		try {						
			if(type == NTCIR11_TOPIC_TYPE.CLEAR){
				//not used
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
				
				String outlierFile = outputDir+"outlier";
								
				HashMap<String, DocReleFingerprint> docReleMap = new HashMap<>();
				
				if(eval == NTCIR_EVAL_TASK.NTCIR11_DR_CH){
					String chLevelFile = xmlDir + "IMine.Qrel.SMC/IMine.Qrel.SMC.xml";
					load2LT(chLevelFile);			
					outputDir += "CH/";
					egStr = "比如 ";
					//needed for getting the outlier documents
					//docid2HtmlMap = _chDocid2HtmlMap;
					
					//all documents are obtained, and there is not outlier now
					docid2HtmlMap = Accessor.loadDocs_Ch();
					outlierFile += "_CH.txt";
					
					outputUncertainMUPairFile = outputDir+"CH_UncertainMarginalUtilityPair.xlsx";
					outputYesSureMUPairFile = outputDir+"CH_YesSureMarginalUtilityPair.xlsx";
					outputNoSureMUPairFile = outputDir+"CH_NoSureMarginalUtilityPair.xlsx";
					
					baselineMap = NTCIRLoader.loadNTCIR11Baseline_CH();
					
				}else if(eval == NTCIR_EVAL_TASK.NTCIR11_DR_EN){
					String enLevelFile = xmlDir + "IMine.Qrel.SME/IMine.Qrel.SME.xml";
					load2LT(enLevelFile);
					outputDir += "EN/";
					egStr = "such as ";
					//needed for getting the outlier documents
					//docid2HtmlMap = _enDocid2HtmlMap;
					
					//all documents are obtained, and there is not outlier now
					docid2HtmlMap = Accessor.loadDocs_En();
					
					outlierFile += "_EN.txt";
					
					outputUncertainMUPairFile = outputDir+"EN_UncertainMarginalUtilityPair.xlsx";
					outputYesSureMUPairFile = outputDir+"EN_YesSureMarginalUtilityPair.xlsx";
					outputNoSureMUPairFile = outputDir+"EN_NoSureMarginalUtilityPair.xlsx";
					
					baselineMap = NTCIRLoader.loadNTCIR11Baseline_EN();
				}
				
				ArrayList<String> topicList = NTCIRLoader.loadNTCIR11Topic(eval, type);
				topicSet.addAll(topicList);
				
				////get the document usage information
				for(Triple<String, String, Integer> triple: triList){
					//since use the second-part to get the desired relevance
					String [] array = triple.getSecond().split("\t");
					String queryid = array[0];
					String slsStr = array[1];
					String docid = triple.getFirst();
					int releDegree = triple.getThird();

					if(topicSet.contains(queryid)){	
						//mapping from sls's content to fls's id
						Pair<Integer, Integer> e = _2LTMap.get(queryid)._slsContentMap.get(slsStr);
						if(null == e){
							System.err.println(queryid+"\t"+slsStr);
							System.exit(0);
							continue;
						}
						//
						if(docReleMap.containsKey(docid)){
							DocReleFingerprint docReleFingerprint = docReleMap.get(docid);
							docReleFingerprint.addReleRecord(queryid, e.getFirst(), e.getSecond(), releDegree);
						}else{
							DocReleFingerprint docReleFingerprint = new DocReleFingerprint(docid);
							docReleFingerprint.addReleRecord(queryid, e.getFirst(), e.getSecond(), releDegree);
							docReleMap.put(docid, docReleFingerprint);
						}
					}					
				}		
				
				////////////////////
				//pairwise comparison w.r.t. marginal utility
				////////////////////
				int totalUnsureCnt = 0;
				int totalSureCnt = 0;
				
				int totalNotIncludedDocCnt = 0;
				int totalIncludedDocCnt = 0;
				HashSet<String> outlierDocSet = new HashSet<>();
				
				//--
				XSSFWorkbook unsureOutBook = null;
				XSSFSheet unsureGroupSheet = null;
				int unsureBeginIndex = 1;
				
				XSSFWorkbook yesSureOutBook = null;
				XSSFSheet yesSureGroupSheet = null;
				int yesSureBeginIndex = 1;
				
				
				XSSFWorkbook noSureOutBook = null;
				XSSFSheet noSureGroupSheet = null;
				int noSureBeginIndex = 1;
				
				////head of excel file
				try {
					////noSure
					noSureOutBook = new XSSFWorkbook();
					noSureGroupSheet = noSureOutBook.createSheet("MU");
					XSSFRow noSureNameRow = noSureGroupSheet.createRow(0);
					Cell noSurecell_1 = noSureNameRow.createCell(0);
					noSurecell_1.setCellValue("topicid");
					
					Cell noSurecell_2 = noSureNameRow.createCell(1);						
					noSurecell_2.setCellValue("topicstr");
					
					Cell noSurecell_3 = noSureNameRow.createCell(2);
					noSurecell_3.setCellValue("Page_1");
					
					Cell noSurecell_4 = noSureNameRow.createCell(3);						
					noSurecell_4.setCellValue("Page_2");
					
					Cell noSurecell_5 = noSureNameRow.createCell(4);
					noSurecell_5.setCellValue("Page_1_InforNeeds");
					
					////yessure
					yesSureOutBook = new XSSFWorkbook();
					yesSureGroupSheet = yesSureOutBook.createSheet("MU");
					XSSFRow yesSureNameRow = yesSureGroupSheet.createRow(0);
					Cell yesSurecell_1 = yesSureNameRow.createCell(0);
					yesSurecell_1.setCellValue("topicid");
					
					Cell yesSurecell_2 = yesSureNameRow.createCell(1);						
					yesSurecell_2.setCellValue("topicstr");
					
					Cell yesSurecell_3 = yesSureNameRow.createCell(2);
					yesSurecell_3.setCellValue("Page_1");
					
					Cell yesSurecell_4 = yesSureNameRow.createCell(3);						
					yesSurecell_4.setCellValue("Page_2");
					
					Cell yesSurecell_5 = yesSureNameRow.createCell(4);
					yesSurecell_5.setCellValue("Page_1_InforNeeds");
					
					
					
					
					////unsure
					unsureOutBook = new XSSFWorkbook();					
					unsureGroupSheet = unsureOutBook.createSheet("MU");
					//head
					XSSFRow unsureNameRow = unsureGroupSheet.createRow(0);
					
					Cell unsurecell_1 = unsureNameRow.createCell(0);
					unsurecell_1.setCellValue("topicid");
					
					Cell unsurecell_2 = unsureNameRow.createCell(1);						
					unsurecell_2.setCellValue("topicstr");
					
					Cell unsurecell_3 = unsureNameRow.createCell(2);
					unsurecell_3.setCellValue("Page_1");
					
					Cell unsurecell_4 = unsureNameRow.createCell(3);						
					unsurecell_4.setCellValue("Page_2");
					
					Cell unsurecell_5 = unsureNameRow.createCell(4);
					unsurecell_5.setCellValue("Page_1_InforNeeds");
					
					//Cell cell_6 = nameRow.createCell(5);
					//cell_6.setCellValue("Page_1_ST_ID");
					
					//Cell cell_7 = nameRow.createCell(6);
					//cell_7.setCellValue("Page_2_ST_ID");
				}catch(Exception e){
					e.printStackTrace();
				}
				//--
								
				for(String topic: topicList){
					int unsureCnt = 0;
					int sureCnt = 0;
					int notIncludedCnt = 0;
					int  includedCnt = 0;
					
					//all relevant documents
					ArrayList<String> releDocList = getReleDocList(topic, docReleMap);
					System.out.println(topic+":\tRele count: "+releDocList.size());
					
					usedReleDocSet.addAll(releDocList);
					
					///*
					//check outlier documents, i.e., relevant but not available documents right now
					for(String releDoc: releDocList){
						if(!docid2HtmlMap.containsKey(releDoc)){
							notIncludedCnt ++;
							//System.out.println(releDoc);
							outlierDocSet.add(releDoc);
						}else{
							includedCnt++;
						}
					}
					//*/
					
					totalNotIncludedDocCnt += notIncludedCnt;
					totalIncludedDocCnt += includedCnt;
										
					ArrayList<String> pairList = new ArrayList<>();
					
					ArrayList<String> yesSurePairList = new ArrayList<>();	
					
					ArrayList<String> noSurePairList = new ArrayList<>();
					
					int size = releDocList.size();
					
					////for specifying the pair of documents w.r.t. the corresponding topic
					HashMap<String, HashSet<String>> topic2DocSetMap = new HashMap<>(); 
					
					for(int i=0; i<size-1; i++){
						String page_1 = releDocList.get(i);
						
						/*
						if(!docid2HtmlMap.containsKey(page_1)){
							continue;
						}
						*/
						
						for(int j=i+1; j<size; j++){
							String page_2 = releDocList.get(j);
							
							/*
							if(!docid2HtmlMap.containsKey(page_2)){
								continue;
							}
							*/
							
							if(calMarginalUtilityOf2ndPage(topic, page_1, page_2, docReleMap) < 0){
								unsureCnt++;
								//output(topic, page_1, page_2, docReleMap);
								outputToExcel(egStr, true, pairList, topic, page_1, page_2, docReleMap);
								
								////for specifying the pair of documents w.r.t. the corresponding topic
								/*
								if(topic2DocSetMap.containsKey(topic)){
									HashSet<String> docSet = topic2DocSetMap.get(topic);
									docSet.add(page_1);
									docSet.add(page_2);
								}else {
									HashSet<String> docSet = new HashSet<>();
									docSet.add(page_1);
									docSet.add(page_2);
									topic2DocSetMap.put(topic, docSet);
								}
								*/
							}else{
								sureCnt++;
								
								outputToExcel(egStr, true, yesSurePairList, topic, page_1, page_2, docReleMap);
							}
							
							if(calMarginalUtilityOf2ndPage(topic, page_2, page_1, docReleMap) < 0){
								unsureCnt++;
								//output(topic, page_2, page_1, docReleMap);
								outputToExcel(egStr, true, pairList, topic, page_2, page_1, docReleMap);
								
								/*
								////for specifying the pair of documents w.r.t. the corresponding topic
								if(topic2DocSetMap.containsKey(topic)){
									HashSet<String> docSet = topic2DocSetMap.get(topic);
									docSet.add(page_1);
									docSet.add(page_2);
								}else {
									HashSet<String> docSet = new HashSet<>();
									docSet.add(page_1);
									docSet.add(page_2);
									topic2DocSetMap.put(topic, docSet);
								}
								*/
							}else{
								sureCnt++;
								
								outputToExcel(egStr, true, yesSurePairList, topic, page_2, page_1, docReleMap);
							}
						}
					}

					////for specifying the pair of documents w.r.t. the corresponding topic
					/*
					generateHtmls("C:/T/Research/CurrentResearch/NTCIR/NTCIR-11/Ntcir11-IMine/Eval-IMine"
							+ "/UncertainPairs/MarginalUtility/Unsure_Html/En/", topic2DocSetMap, docid2HtmlMap);
					*/
					
					//excel file per topic
					/*
					//.csv
					BufferedWriter wr = new BufferedWriter(new FileWriter(outputDir+topic+"_UncertainMarginalUtilityPair.csv"));
					Collections.shuffle(pairList);
					for(String pairStr: pairList){
						wr.write(pairStr);
						wr.newLine();
					}
					wr.flush();
					wr.close();
					*/
					
					//--
					try {
						//excel file per topic
						/*
						XSSFWorkbook outBook = new XSSFWorkbook();
						
						XSSFSheet groupSheet = outBook.createSheet(topic);
						//head
						XSSFRow nameRow = groupSheet.createRow(0);
						
						Cell cell_1 = nameRow.createCell(0);
						cell_1.setCellValue("topicid");
						
						Cell cell_2 = nameRow.createCell(1);						
						cell_2.setCellValue("topicstr");
						
						Cell cell_3 = nameRow.createCell(2);
						cell_3.setCellValue("Page_1");
						
						Cell cell_4 = nameRow.createCell(1);						
						cell_4.setCellValue("Page_2");
						
						Cell cell_5 = nameRow.createCell(2);
						cell_5.setCellValue("Page_1_InforNeeds");
						
						Cell cell_6 = nameRow.createCell(3);
						cell_6.setCellValue("Page_1_ST_ID");
						
						Cell cell_7 = nameRow.createCell(4);
						cell_7.setCellValue("Page_2_ST_ID");
						*/
						
						////unsure body
						for(int k=0; k<pairList.size(); k++){
							
							XSSFRow unsureKRow = unsureGroupSheet.createRow(k+unsureBeginIndex);
							
							String pairStr = pairList.get(k);
							
							String [] array = pairStr.split(",");
							
							Cell uncell_1 = unsureKRow.createCell(0);
							uncell_1.setCellValue(topic);
							Cell uncell_2 = unsureKRow.createCell(1);
							uncell_2.setCellValue(_2LTMap.get(topic)._topic);
							
							Cell uncell_3 = unsureKRow.createCell(2);
							uncell_3.setCellValue(array[0]);
							Cell uncell_4 = unsureKRow.createCell(3);
							uncell_4.setCellValue(array[1]);
							Cell uncell_5 = unsureKRow.createCell(4);
							uncell_5.setCellValue(array[2]);
							//Cell cell_6 = kRow.createCell(5);
							//cell_6.setCellValue(array[3]);
							//Cell cell_7 = kRow.createCell(6);
							//cell_7.setCellValue(array[4]);
						}
						
						unsureBeginIndex += pairList.size();
						
						////yes sure body
						Collections.shuffle(yesSurePairList);
						
						int topK = 10;
						
						int yesSureTopK = Math.min(topK, yesSurePairList.size());
						for(int k=0; k<yesSureTopK; k++){
							XSSFRow yesSureRow = yesSureGroupSheet.createRow(k+yesSureBeginIndex);
							
							String yesSurePairStr = yesSurePairList.get(k);							
							String [] array = yesSurePairStr.split(",");
							
							Cell yesSurecell_1 = yesSureRow.createCell(0);
							yesSurecell_1.setCellValue(topic);
							Cell yesSurecell_2 = yesSureRow.createCell(1);
							yesSurecell_2.setCellValue(_2LTMap.get(topic)._topic);
							
							Cell yesSurecell_3 = yesSureRow.createCell(2);
							yesSurecell_3.setCellValue(array[0]);
							Cell yesSurecell_4 = yesSureRow.createCell(3);
							yesSurecell_4.setCellValue(array[1]);
							Cell yesSurecell_5 = yesSureRow.createCell(4);
							yesSurecell_5.setCellValue(array[2]);
						}
						yesSureBeginIndex += yesSureTopK;
						
						/*
						OutputStream outputStream = new FileOutputStream(outputFile);
						outBook.write(outputStream);
						outputStream.flush();
						outputStream.close();
						*/
						
						////for no sure pairs
						//get irrelevant documents
						ArrayList<String> baseline = baselineMap.get(topic);
						ArrayList<String> irrelList = new ArrayList<>();
						for(String line: baseline){
							String[] fields = line.split("\\s");
							if(!releDocList.contains(fields[2])){
								irrelList.add(fields[2]);
							}
						}
						//
						for(int m=0; m<topK; m++){
							String p1 = releDocList.get(m);
							String p2 = irrelList.get(m);
							outputToExcel(egStr, true, noSurePairList, topic, p1, p2, docReleMap);
							
							usedIrrelDocList.add(p2);
						}
						//
						for(int k=0; k<noSurePairList.size(); k++){
							String noSurePairStr = noSurePairList.get(k);
							XSSFRow noSureRow = noSureGroupSheet.createRow(k+noSureBeginIndex);
							
							String [] array = noSurePairStr.split(",");
							
							Cell noSurecell_1 = noSureRow.createCell(0);
							noSurecell_1.setCellValue(topic);
							Cell noSurecell_2 = noSureRow.createCell(1);
							noSurecell_2.setCellValue(_2LTMap.get(topic)._topic);
							
							Cell noSurecell_3 = noSureRow.createCell(2);
							noSurecell_3.setCellValue(array[0]);
							Cell noSurecell_4 = noSureRow.createCell(3);
							noSurecell_4.setCellValue(array[1]);
							Cell noSurecell_5 = noSureRow.createCell(4);
							noSurecell_5.setCellValue(array[2]);
						}
						noSureBeginIndex += noSurePairList.size();
						
					}catch(Exception e){
						e.printStackTrace();
					}
					//--					
					
					System.out.println(topic+":\tUnsure count: "+unsureCnt);
					System.out.println();
					
					totalUnsureCnt += unsureCnt;
					totalSureCnt   += sureCnt;
				}	
				
				////1
				OutputStream unsureOutputStream = new FileOutputStream(outputUncertainMUPairFile);
				unsureOutBook.write(unsureOutputStream);
				unsureOutputStream.flush();
				unsureOutputStream.close();
				
				////2
				OutputStream yesSureOutputStream = new FileOutputStream(outputYesSureMUPairFile);
				yesSureOutBook.write(yesSureOutputStream);
				yesSureOutputStream.flush();
				yesSureOutputStream.close();
				
				////3
				OutputStream noSureOutputStream = new FileOutputStream(outputNoSureMUPairFile);
				noSureOutBook.write(noSureOutputStream);
				noSureOutputStream.flush();
				noSureOutputStream.close();
				
				////generate relevant htmls
				///*
				generateReleHtmls("C:/T/Research/CurrentResearch/NTCIR/NTCIR-11/Ntcir11-IMine/Eval-IMine"
						+ "/UncertainPairs/MarginalUtility/UnSureHtml/En/", usedReleDocSet, docid2HtmlMap);
				//*/
				
				////generate irrelevant htmls
				generateIrrelHtmls("C:/T/Research/CurrentResearch/NTCIR/NTCIR-11/Ntcir11-IMine/Eval-IMine"
							+ "/UncertainPairs/MarginalUtility/NoSureHtml/En/", usedIrrelDocList, docid2HtmlMap);
				
				System.out.println();
				System.out.println("Total unsure count:\t"+totalUnsureCnt);
				System.out.println("Total sure count:\t"+totalSureCnt);
				System.out.println("Total unincluded cnt:\t"+totalNotIncludedDocCnt);
				System.out.println("Total included cnt:\t"+totalIncludedDocCnt);
				System.out.println("Total docs cnt:\t"+docReleMap.size());
				
				/*
				//buffer outlier doc
				BufferedWriter outlierWriter = IOText.getBufferedWriter_UTF8(outlierFile);
				ArrayList<String> outlierList = new ArrayList<>();				
				for(String outlierDoc: outlierDocSet){
					outlierList.add(outlierDoc);
				}				
				Collections.sort(outlierList);				
				for(String outlierDoc: outlierList){
					outlierWriter.write(outlierDoc);
					outlierWriter.newLine();
				}
								
				outlierWriter.flush();
				outlierWriter.close();
				*/
				
				/////////////////////
				//pairwise comparison w.r.t. preference
				/////////////////////
				
			} else{
				System.err.println("Type Error!");
				System.exit(0);
			}			
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}
	
	public void getDocReleMap_marginal_clear(String xmlDir, NTCIR_EVAL_TASK eval,
			NTCIR11_TOPIC_TYPE type, String outputDir){
		////
		ArrayList<Triple<String, String, Integer>> triList = getXmlQrel(xmlDir, eval, type);
		HashSet<String> topicSet = new HashSet<String>();
		
		//docid->{queryid->releDegree}
		HashMap<String, HashMap<String, Integer>> clearDocReleMap = new HashMap<>();
		
		HashSet<String> outlierDocSet = new HashSet<>();
		
		try {
			if(type == NTCIR11_TOPIC_TYPE.CLEAR){
				String outputUncertainMUPairFile = null;
				String egStr = null;
				String outlierFile = outputDir+"Clear_outlier";		
				
				ArrayList<StrStr> topicStrList = null;
				
				if(eval == NTCIR_EVAL_TASK.NTCIR11_DR_CH){
					outlierFile += "_CH.txt";
					egStr = "比如 ";
					topicStrList = NTCIRLoader.loadNTCIR11TopicList(Lang.Chinese);
				}else if(eval == NTCIR_EVAL_TASK.NTCIR11_DR_EN){
					topicStrList = NTCIRLoader.loadNTCIR11TopicList(Lang.English);
					outlierFile += "_EN.txt";
					loadNTCIR11BaselineDocs_EN();
					egStr = "such as ";
					outputUncertainMUPairFile = outputDir+"EN_Clear_UncertainMarginalUtilityPair.xlsx";					
				}
				
				//used topic
				ArrayList<String> topicList = NTCIRLoader.loadNTCIR11Topic(eval, type);
				topicSet.addAll(topicList);	
				HashMap<String, String> topicStrMap = new HashMap<>();
				for(StrStr element: topicStrList){
					String topicid = element.getFirst();
					if(topicSet.contains(topicid)){
						topicStrMap.put(topicid, element.getSecond());
					}
				}
								
				//docid, queryid, releLevel
				for(Triple<String, String, Integer> triple: triList){
					if(topicSet.contains(triple.getSecond())){
						String docid = triple.getFirst();
						String queryid = triple.getSecond();
						Integer releLevel = triple.getThird();
						//
						if(clearDocReleMap.containsKey(docid)){
							HashMap<String, Integer> qMap = clearDocReleMap.get(docid);
							qMap.put(queryid, releLevel);
						}else{
							HashMap<String, Integer> qMap = new HashMap<>();
							qMap.put(queryid, releLevel);
							clearDocReleMap.put(docid, qMap);
						}
					}					
				}
				
				////
				int totalUnsureCnt = 0;
				int totalSureCnt = 0;
				
				int totalNotIncludedDocCnt = 0;
				int totalIncludedDocCnt = 0;
				//
				
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
				unsurecell_3.setCellValue("Page_1");
				
				Cell unsurecell_4 = unsureNameRow.createCell(3);						
				unsurecell_4.setCellValue("Page_2");
				
				Cell unsurecell_5 = unsureNameRow.createCell(4);
				unsurecell_5.setCellValue("Page_1_InforNeeds");
				////
				for(String topic: topicList){
					int unsureCnt = 0;
					int sureCnt = 0;
					
					int notIncludedCnt = 0;
					int  includedCnt = 0;
					
					
					ArrayList<String> releDocList = getReleDocList_clear(topic, clearDocReleMap);
					
					
					///*
					//check outlier documents, i.e., relevant but not available documents right now
					for(String releDoc: releDocList){
						if(!_enDocid2HtmlMap.containsKey(releDoc)){
							notIncludedCnt ++;
							//System.out.println(releDoc);
							outlierDocSet.add(releDoc);
						}else{
							includedCnt++;
						}
					}
					
					totalNotIncludedDocCnt += notIncludedCnt;
					totalIncludedDocCnt += includedCnt;
					//*/
					
					ArrayList<String> pairList = new ArrayList<>();
					
					int size = releDocList.size();
					for(int i=0; i<size-1; i++){
						String page_1 = releDocList.get(i);
						
						for(int j=i+1; j<size; j++){
							String page_2 = releDocList.get(j);
							
							if(calMarginalUtilityOf2ndPage_clear(topic, page_1, page_2, clearDocReleMap) < 0){
								unsureCnt++;
								outputToExcel_clear(topicStrMap.get(topic), egStr, true, pairList, page_1, page_2);								
							}else{
								sureCnt++;
							}
							
							if(calMarginalUtilityOf2ndPage_clear(topic, page_2, page_1, clearDocReleMap) < 0){
								unsureCnt++;
								outputToExcel_clear(topicStrMap.get(topic), egStr, true, pairList, page_2, page_1);
							}else{
								sureCnt++;								
							}
						}
					}
					
					totalUnsureCnt += unsureCnt;
					totalSureCnt   += sureCnt;
					
					//
					try {
					////unsure body
						for(int k=0; k<pairList.size(); k++){
							
							XSSFRow unsureKRow = unsureGroupSheet.createRow(k+unsureBeginIndex);
							
							String pairStr = pairList.get(k);
							
							String [] array = pairStr.split(",");
							
							Cell uncell_1 = unsureKRow.createCell(0);
							uncell_1.setCellValue(topic);
							Cell uncell_2 = unsureKRow.createCell(1);
							uncell_2.setCellValue(topicStrMap.get(topic));
							
							Cell uncell_3 = unsureKRow.createCell(2);
							uncell_3.setCellValue(array[0]);
							Cell uncell_4 = unsureKRow.createCell(3);
							uncell_4.setCellValue(array[1]);
							Cell uncell_5 = unsureKRow.createCell(4);
							uncell_5.setCellValue(array[2]);
							//Cell cell_6 = kRow.createCell(5);
							//cell_6.setCellValue(array[3]);
							//Cell cell_7 = kRow.createCell(6);
							//cell_7.setCellValue(array[4]);
						}
						
						unsureBeginIndex += pairList.size();
					} catch (Exception e) {
						e.printStackTrace();
					}
					
				}
				
				///*
				//buffer outlier doc
				BufferedWriter outlierWriter = IOText.getBufferedWriter_UTF8(outlierFile);
				ArrayList<String> outlierList = new ArrayList<>();				
				for(String outlierDoc: outlierDocSet){
					outlierList.add(outlierDoc);
				}				
				Collections.sort(outlierList);				
				for(String outlierDoc: outlierList){
					outlierWriter.write(outlierDoc);
					outlierWriter.newLine();
				}
								
				outlierWriter.flush();
				outlierWriter.close();
				//*/
					
				////1
				OutputStream unsureOutputStream = new FileOutputStream(outputUncertainMUPairFile);
				unsureOutBook.write(unsureOutputStream);
				unsureOutputStream.flush();
				unsureOutputStream.close();
				
				////
				
				System.out.println();
				System.out.println("Total unsure count:\t"+totalUnsureCnt);
				System.out.println("Total sure count:\t"+totalSureCnt);
				System.out.println("Total unincluded cnt:\t"+totalNotIncludedDocCnt);
				System.out.println("Total included cnt:\t"+totalIncludedDocCnt);
				System.out.println("Total docs cnt:\t"+clearDocReleMap.size());
				
			}else{
				System.err.println("Error!");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}		
	}
	
	private void generateHtmls(String outputDir, 
			HashMap<String, HashSet<String>> topic2DocSetMap, HashMap<String, String> docid2HtmlMap){
		
		/*
		for(Entry<String, HashSet<String>> topicEntry: topic2DocSetMap.entrySet()){
			String topic = topicEntry.getKey();
			HashSet<String> docSet = topicEntry.getValue();
			for(String docid: docSet){
				String htmlFile = outputDir+topic+"-"+docid+".html";
				try {
					BufferedWriter writer = IOText.getBufferedWriter_UTF8(htmlFile);
					writer.write(docid2HtmlMap.get(docid));
					writer.flush();
					writer.close();					
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		*/
		
		HashSet<String> allDocs = new HashSet<>();
		for(Entry<String, HashSet<String>> topicEntry: topic2DocSetMap.entrySet()){
			HashSet<String> docSet = topicEntry.getValue();
			allDocs.addAll(docSet);
		}
		
		for(String docid: allDocs){
			String htmlFile = outputDir+docid+".html";
			try {
				BufferedWriter writer = IOText.getBufferedWriter_UTF8(htmlFile);
				writer.write(docid2HtmlMap.get(docid));
				writer.flush();
				writer.close();					
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	private void generateReleHtmls(String outputDir, HashSet<String> releSet,
			HashMap<String, String> docid2HtmlMap){
		
		for(String docid: releSet){
			String htmlFile = outputDir+docid+".html";
			try {
				BufferedWriter writer = IOText.getBufferedWriter_UTF8(htmlFile);
				writer.write(docid2HtmlMap.get(docid));
				writer.flush();
				writer.close();					
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	private void generateIrrelHtmls(String outputDir, ArrayList<String> irrelList,
			HashMap<String, String> docid2HtmlMap){
		
		for(String docid: irrelList){
			String htmlFile = outputDir+docid+".html";
			try {
				BufferedWriter writer = IOText.getBufferedWriter_UTF8(htmlFile);
				writer.write(docid2HtmlMap.get(docid));
				writer.flush();
				writer.close();					
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	private void output(String topic, String page_1, String page_2, HashMap<String, DocReleFingerprint> docReleMap){
		DocReleFingerprint fingerprint_1 = docReleMap.get(page_1);
		DocReleFingerprint fingerprint_2 = docReleMap.get(page_2);	
		
		HashMap<Integer, Integer> flsReleMap_1 = fingerprint_1._q2FlsReleMap.get(topic);		
		HashMap<Integer, HashMap<Integer, Integer>> slsReleMap_1 = fingerprint_1._q2SlsReleMap.get(topic);
		
		for(Entry<Integer, Integer> flsEntry: flsReleMap_1.entrySet()){
			System.out.print("Head-"+page_1+"\t"+topic+" "+flsEntry.getKey()+":"+flsEntry.getValue()+"\t{");
			HashMap<Integer, Integer> slsMap = slsReleMap_1.get(flsEntry.getKey());
			for(Entry<Integer, Integer> slsEntry: slsMap.entrySet()){
				System.out.print(slsEntry.getKey()+":"+slsEntry.getValue()+" ");
			}
			System.out.print("}\t");
		}
		System.out.println();
		
		HashMap<Integer, Integer> flsReleMap_2 = fingerprint_2._q2FlsReleMap.get(topic);
		HashMap<Integer, HashMap<Integer, Integer>> slsReleMap_2 = fingerprint_2._q2SlsReleMap.get(topic);
		
		for(Entry<Integer, Integer> flsEntry: flsReleMap_2.entrySet()){
			System.out.print("Tail-"+page_2+"\t"+topic+" "+flsEntry.getKey()+":"+flsEntry.getValue()+"\t{");
			HashMap<Integer, Integer> slsMap = slsReleMap_2.get(flsEntry.getKey());
			for(Entry<Integer, Integer> slsEntry: slsMap.entrySet()){
				System.out.print(slsEntry.getKey()+":"+slsEntry.getValue()+" ");
			}
			System.out.print("}\t");
		}
		System.out.println();
	}
	
	private void outputToExcel(String egStr, boolean plusContent, ArrayList<String> pairList, String topic, String page_1, String page_2,
			HashMap<String, DocReleFingerprint> docReleMap){
		//
		TwoLevelTopic twoLTopic = _2LTMap.get(topic);
		StringBuffer descripBuffer = new StringBuffer();
		
		StringBuffer buffer_1 = new StringBuffer();		
		DocReleFingerprint fingerprint_1 = docReleMap.get(page_1);		
		HashMap<Integer, Integer> flsReleMap_1 = fingerprint_1._q2FlsReleMap.get(topic);		
		HashMap<Integer, HashMap<Integer, Integer>> slsReleMap_1 = fingerprint_1._q2SlsReleMap.get(topic);
		
		int fNum = 1;
		
		for(Entry<Integer, Integer> flsEntry: flsReleMap_1.entrySet()){
			String flsStr = twoLTopic._flsList.get(flsEntry.getKey()-1).first;
			flsStr = flsStr.replaceAll("_", " ");
			descripBuffer.append(fNum+" "+flsStr+br);
			//System.out.print("Head-"+page_1+"\t"+topic+" "+flsEntry.getKey()+":"+flsEntry.getValue()+"\t{");
			buffer_1.append(flsEntry.getKey()+":"+flsEntry.getValue()+"{");
			HashMap<Integer, Integer> slsMap = slsReleMap_1.get(flsEntry.getKey());
			
			ArrayList<Pair<String, Double>> slsList = twoLTopic._slsSetList.get(flsEntry.getKey()-1);
			
			int sNum = 1;
			for(Entry<Integer, Integer> slsEntry: slsMap.entrySet()){
				//System.out.print(slsEntry.getKey()+":"+slsEntry.getValue()+" ");
				buffer_1.append(slsEntry.getKey()+":"+slsEntry.getValue()+"#");
				
				String slsStr = slsList.get(slsEntry.getKey()-1).first;
				slsStr = slsStr.replaceAll("_", " ");
				descripBuffer.append(fNum+"."+sNum+" "+slsStr+"  ");
				
				ArrayList<String> exampleSlsStrList = twoLTopic._slsExampleSetMap.get(
						new Pair<Integer, Integer>(flsEntry.getKey(), slsEntry.getKey()));
				
				int size = Math.min(3, exampleSlsStrList.size());
				descripBuffer.append("{"+egStr);
				for(int i=0; i<size-1; i++){
					descripBuffer.append(" "+exampleSlsStrList.get(i)+"; ");
				}
				descripBuffer.append(" "+exampleSlsStrList.get(size-1));
				descripBuffer.append("}"+br);
				
				sNum++;
			}
			//System.out.print("}\t");
			buffer_1.append("}");
			
			fNum++;
		}
		//System.out.println();
		
		StringBuffer buffer_2 = new StringBuffer();
		if(docReleMap.containsKey(page_2)){			
			DocReleFingerprint fingerprint_2 = docReleMap.get(page_2);
			HashMap<Integer, Integer> flsReleMap_2 = fingerprint_2._q2FlsReleMap.get(topic);
			HashMap<Integer, HashMap<Integer, Integer>> slsReleMap_2 = fingerprint_2._q2SlsReleMap.get(topic);
			
			for(Entry<Integer, Integer> flsEntry: flsReleMap_2.entrySet()){
				//System.out.print("Tail-"+page_2+"\t"+topic+" "+flsEntry.getKey()+":"+flsEntry.getValue()+"\t{");
				buffer_2.append(flsEntry.getKey()+":"+flsEntry.getValue()+"{");
				HashMap<Integer, Integer> slsMap = slsReleMap_2.get(flsEntry.getKey());
				for(Entry<Integer, Integer> slsEntry: slsMap.entrySet()){
					//System.out.print(slsEntry.getKey()+":"+slsEntry.getValue()+" ");
					buffer_2.append(slsEntry.getKey()+":"+slsEntry.getValue()+"#");
				}
				//System.out.print("}\t");
				buffer_2.append("}");
			}
		}else{
			buffer_2.append("{}");
		}		
		
		//System.out.println();
		/*
		try {
			wr.write(page_1+","
						+page_2+","
						+buffer_1.toString()+","
						+buffer_2.toString());
			wr.newLine();
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		*/
		String pairStr = "";
		pairStr+= (page_1+",");
		pairStr+= (page_2+",");
		if(plusContent){
			pairStr+= (descripBuffer.toString()+",");
		}
		pairStr+= (buffer_1.toString()+",");
		pairStr+= (buffer_2.toString());
		
		pairList.add(pairStr);		
	}
	
	private void outputToExcel_clear(String topicStr, String egStr, boolean plusContent, ArrayList<String> pairList,
			String page_1, String page_2){
		////
		String pairStr = "";
		pairStr+= (page_1+",");
		pairStr+= (page_2+",");
		if(plusContent){
			pairStr+= (topicStr+",");
		}
		pairStr+= ("null"+",");
		pairStr+= ("null");
		
		pairList.add(pairStr);		
	}
	
	private ArrayList<String> getReleDocList(String queryid, HashMap<String, DocReleFingerprint> docReleMap){
		ArrayList<String> releDocList = new ArrayList<>();
		
		for(Entry<String, DocReleFingerprint> entry: docReleMap.entrySet()){
			DocReleFingerprint docReleFingerprint = entry.getValue();
			if(docReleFingerprint._q2FlsReleMap.containsKey(queryid)){
				releDocList.add(docReleFingerprint._docid);
			}
		}
		
		return releDocList;
	}
	
	private ArrayList<String> getReleDocList_clear(String queryid, HashMap<String, HashMap<String, Integer>> clearDocReleMap){
		ArrayList<String> releDocList = new ArrayList<>();
		
		for(Entry<String, HashMap<String, Integer>> entry: clearDocReleMap.entrySet()){
			HashMap<String, Integer> qMap = entry.getValue();
			if(qMap.containsKey(queryid)){
				releDocList.add(entry.getKey());
			}
		}
		
		return releDocList;
	}
	
	private int calMarginalUtilityOf2ndPage(String topic, String page_1, String page_2,
			HashMap<String, DocReleFingerprint> docReleMap){
		DocReleFingerprint fingerprint_1 = docReleMap.get(page_1);
		DocReleFingerprint fingerprint_2 = docReleMap.get(page_2);	
		
		HashMap<Integer, Integer> flsReleMap_1 = fingerprint_1._q2FlsReleMap.get(topic);
		HashMap<Integer, HashMap<Integer, Integer>> slsReleMap_1 = fingerprint_1._q2SlsReleMap.get(topic);
		
		HashMap<Integer, Integer> flsReleMap_2 = fingerprint_2._q2FlsReleMap.get(topic);
		HashMap<Integer, HashMap<Integer, Integer>> slsReleMap_2 = fingerprint_2._q2SlsReleMap.get(topic);
		
		//1 definitely sure about having mu
		//-1 not sure having no mu, maybe ties; maybe less relevant compared with highly relevant, but we are not sure
		if(coverNewOrHigherReleDegreeBy2nd(flsReleMap_1, flsReleMap_2)){
			return 1;
		}else if(regidReleTie(flsReleMap_1, flsReleMap_2) || relaxedReleTie(flsReleMap_1, flsReleMap_2)){
			//case-2: 2fa309745097b3e0-6d342f71826112e0,39e0a14bc4d51e90-75521245fd3b7e10,1:2{2:2#},1:1{1:1#}
			for(Integer flsID: flsReleMap_2.keySet()){
				HashMap<Integer, Integer> slsMap_1 = slsReleMap_1.get(flsID);
				if(null == slsMap_1){
					System.out.println(topic+"\t"+flsID+"\t"+fingerprint_1._docid);
					System.exit(0);
				}
				
				HashMap<Integer, Integer> slsMap_2 = slsReleMap_2.get(flsID);
				if(null == slsMap_2){
					System.out.println(topic+"\t"+flsID+"\t"+fingerprint_2._docid);
					System.exit(0);
				}
				
				if(coverNewOrHigherReleDegreeBy2nd(slsMap_1, slsMap_2)){
					return 1;
				}
			}
			return -1;
		}else{
			return -1;
		}
	}
	
	private int calMarginalUtilityOf2ndPage_clear(String topic, String page_1, String page_2,
			HashMap<String, HashMap<String, Integer>> clearDocReleMap){
		//
		HashMap<String, Integer> qMap_1 = clearDocReleMap.get(page_1);
		HashMap<String, Integer> qMap_2 = clearDocReleMap.get(page_2);
		if(qMap_2.get(topic) > qMap_1.get(topic)){
			return 1;
		}else{
			return -1;
		}		
	}
	
	//suitable to fls and sls
	//when it is true, thus be sure that there is no marginal utility;
	//when false, not sure about (1) ties; (2) no marginal utility;
	private boolean coverNewOrHigherReleDegreeBy2nd (HashMap<Integer, Integer> releMap_1, HashMap<Integer, Integer> releMap_2){
		//certainly cover new fls/sls subtopics
		if(releMap_2.size() > releMap_1.size()){
			return true;
		}
		//higher degree of relevance or new
		for(Entry<Integer, Integer> entry_2: releMap_2.entrySet()){
			int tID = entry_2.getKey();
			int degree_2 = entry_2.getValue();
			
			if(releMap_1.containsKey(tID)){
				//higher relevance degree by 2nd page
				if(releMap_1.get(tID) < degree_2){
					return true;
				}
			}else{
				//covering new fls/sls subtopic by 2nd page
				return true;
			}
		}
		
		return false;
	}
	//the same set of relevant fls/sls subtopics, and the same rele degree
	private boolean regidReleTie(HashMap<Integer, Integer> releMap_1, HashMap<Integer, Integer> releMap_2){
		//first: same number of relevant fls/sls subtopics
		if(releMap_1.size() == releMap_2.size()){
			//second: equal relevance degree w.r.t. each 
			for(Entry<Integer, Integer> entry_2: releMap_2.entrySet()){
				int tID = entry_2.getKey();
				int degree_2 = entry_2.getValue();
				
				if(releMap_1.containsKey(tID) && (releMap_1.get(tID)==degree_2)){
					
				}else{
					return false;
				}
			}
			return true;
		}else{
			return false;
		}
	}
	//page-1 is a superset of relevant fls/sls subtopics covered by page-1
	//now we are considering the case that: for fls subtopics, page-2 has a lower rele degree, but it relates to more sls subtopics
	//e.g., 2fa309745097b3e0-6d342f71826112e0,39e0a14bc4d51e90-75521245fd3b7e10,1:2{2:2#},1:1{1:1#}
	private boolean relaxedReleTie(HashMap<Integer, Integer> releMap_1, HashMap<Integer, Integer> releMap_2){
		//first: same number of relevant fls/sls subtopics
		for(Entry<Integer, Integer> entry_2: releMap_2.entrySet()){
			int tID = entry_2.getKey();
			//int degree_2 = entry_2.getValue();			
			if(releMap_1.containsKey(tID)){
				
			}else{
				return false;
			}
		}
		return true;
	}
	
	////////////////////preference
	public void getDocReleMap_preference(boolean skipParallel, String xmlDir, String dir, NTCIR_EVAL_TASK eval, NTCIR11_TOPIC_TYPE type, String outputDir){
		////1
		ArrayList<Triple<String, String, Integer>> triList = getXmlQrel(xmlDir, eval, type);		
		HashSet<String> topicSet = new HashSet<String>();
		////2
		try {						
			if(type == NTCIR11_TOPIC_TYPE.CLEAR){
				//not used
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
				
				HashMap<String, DocReleFingerprint> docReleMap = new HashMap<>();
				
				if(eval == NTCIR_EVAL_TASK.NTCIR11_DR_CH){
					String chLevelFile = xmlDir + "IMine.Qrel.SMC/IMine.Qrel.SMC.xml";
					load2LT(chLevelFile);			
					outputDir += "CH/";
				}else if(eval == NTCIR_EVAL_TASK.NTCIR11_DR_EN){
					String enLevelFile = xmlDir + "IMine.Qrel.SME/IMine.Qrel.SME.xml";
					load2LT(enLevelFile);
					outputDir += "EN/";
				}
				
				ArrayList<String> topicList = NTCIRLoader.loadNTCIR11Topic(eval, type);
				topicSet.addAll(topicList);
				
				for(Triple<String, String, Integer> triple: triList){
					//since use the second-part to get the desired relevance
					String [] array = triple.getSecond().split("\t");
					String queryid = array[0];
					String slsStr = array[1];
					String docid = triple.getFirst();
					int releDegree = triple.getThird();

					if(topicSet.contains(queryid)){	
						//mapping from sls's content to fls's id
						Pair<Integer, Integer> e = _2LTMap.get(queryid)._slsContentMap.get(slsStr);
						if(null == e){
							System.err.println(queryid+"\t"+slsStr);
							System.exit(0);
							continue;
						}
						//
						if(docReleMap.containsKey(docid)){
							DocReleFingerprint docReleFingerprint = docReleMap.get(docid);
							docReleFingerprint.addReleRecord(queryid, e.getFirst(), e.getSecond(), releDegree);
						}else{
							DocReleFingerprint docReleFingerprint = new DocReleFingerprint(docid);
							docReleFingerprint.addReleRecord(queryid, e.getFirst(), e.getSecond(), releDegree);
							docReleMap.put(docid, docReleFingerprint);
						}
					}					
				}		
				
				////////////////////
				//pairwise comparison w.r.t.preference
				////////////////////
				int totalCnt = 0;
				for(String topic: topicList){
					int unsureCnt = 0;
					ArrayList<String> releDocList = getReleDocList(topic, docReleMap);
					System.out.println(topic+":\tRele count: "+releDocList.size());
										
					ArrayList<String> pairList = new ArrayList<>();		
					
					int size = releDocList.size();
					for(int i=0; i<size-1; i++){
						String page_1 = releDocList.get(i);
						
						for(int j=i+1; j<size; j++){
							String page_2 = releDocList.get(j);
							
							if(preferPage1TwoPage2(skipParallel, topic, page_1, page_2, docReleMap)<0
									&& preferPage1TwoPage2(skipParallel, topic, page_2, page_1, docReleMap)<0){
								////
								unsureCnt++;
								//output(topic, page_1, page_2, docReleMap);
								outputToExcel("", false, pairList, topic, page_1, page_2, docReleMap);
							}
						}
					}
					
					BufferedWriter wr = new BufferedWriter(new FileWriter(outputDir+topic+"_UncertainPreferencePair.csv"));
					Collections.shuffle(pairList);
					for(String pairStr: pairList){
						wr.write(pairStr);
						wr.newLine();
					}
					wr.flush();
					wr.close();
					
					System.out.println(topic+":\tUnsure count: "+unsureCnt);
					System.out.println();
					
					totalCnt += unsureCnt;
				}	
				
				System.out.println();
				System.out.println("Total unsure count:\t"+totalCnt);
				
				/////////////////////
				//pairwise comparison w.r.t. preference
				/////////////////////
				
			} else{
				System.err.println("Type Error!");
				System.exit(0);
			}			
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}
	private int preferPage1TwoPage2(boolean skipParallel, String topic, String page_1, String page_2, HashMap<String, DocReleFingerprint> docReleMap){
		DocReleFingerprint fingerprint_1 = docReleMap.get(page_1);
		DocReleFingerprint fingerprint_2 = docReleMap.get(page_2);	
		
		HashMap<Integer, Integer> flsReleMap_1 = fingerprint_1._q2FlsReleMap.get(topic);
		HashMap<Integer, HashMap<Integer, Integer>> slsReleMap_1 = fingerprint_1._q2SlsReleMap.get(topic);
		
		HashMap<Integer, Integer> flsReleMap_2 = fingerprint_2._q2FlsReleMap.get(topic);
		HashMap<Integer, HashMap<Integer, Integer>> slsReleMap_2 = fingerprint_2._q2SlsReleMap.get(topic);
		
		if(skipParallel && isParallel(flsReleMap_1, flsReleMap_2)){
			return 1;
		}
		
		if(regidReleTie(flsReleMap_1, flsReleMap_2) && sls_regidReleTie(slsReleMap_1, slsReleMap_2)){
			return -1;
		}else{
			if((lowerPage2IsSubsetOrTie(flsReleMap_1, flsReleMap_2)
					&&sls_lowerPage2IsSubsetOrTie(slsReleMap_1, slsReleMap_2))){
				////
				return 1;
			}else if((lowerPage2IsSubsetOrTie(flsReleMap_2, flsReleMap_1)
					&&sls_lowerPage2IsSubsetOrTie(slsReleMap_2, slsReleMap_1))){
				////
				return 1;
			}else{
				return -1;
			}
		}			
	}
	
	boolean lowerPage2IsSubsetOrTie(HashMap<Integer, Integer> releMap_1, HashMap<Integer, Integer> releMap_2){
		for(Entry<Integer, Integer> entry_2: releMap_2.entrySet()){
			int tID = entry_2.getKey();
			int releDegree = entry_2.getValue();
			if(releMap_1.containsKey(tID) && releMap_1.get(tID)>=releDegree){
				
			}else{
				return false;
			}
		}
		return true;
	}
	
	boolean sls_lowerPage2IsSubsetOrTie(HashMap<Integer, HashMap<Integer, Integer>> slsReleMap_1,
									HashMap<Integer, HashMap<Integer, Integer>> slsReleMap_2){
		//
		for(Entry<Integer, HashMap<Integer, Integer>> slsMapEntry: slsReleMap_2.entrySet()){
			int tID = slsMapEntry.getKey();
			HashMap<Integer, Integer> slsMap = slsMapEntry.getValue();
			
			if(slsReleMap_1.containsKey(tID) && lowerPage2IsSubsetOrTie(slsReleMap_1.get(tID), slsMap)){
				
			}else{
				return false;
			}
		}
		return true;
	}
		
	boolean sls_regidReleTie(HashMap<Integer, HashMap<Integer, Integer>> slsReleMap_1,
									HashMap<Integer, HashMap<Integer, Integer>> slsReleMap_2){
		//
		for(Entry<Integer, HashMap<Integer, Integer>> slsMapEntry: slsReleMap_2.entrySet()){
			int tID = slsMapEntry.getKey();
			HashMap<Integer, Integer> slsMap = slsMapEntry.getValue();
			
			if(regidReleTie(slsReleMap_1.get(tID), slsMap)){
				
			}else{
				return false;
			}
		}
		return true;
	}
	
	boolean isParallel(HashMap<Integer, Integer> releMap_1, HashMap<Integer, Integer> releMap_2){
		for(Entry<Integer, Integer> entry_2: releMap_2.entrySet()){
			int tID = entry_2.getKey();
			if(releMap_1.containsKey(tID)){
				return false;
			}
		}
		return true;
	}
	
	private int calPreference_clear(String topic, String page_1, String page_2,
			HashMap<String, HashMap<String, Integer>> clearDocReleMap){
		//
		HashMap<String, Integer> qMap_1 = clearDocReleMap.get(page_1);
		HashMap<String, Integer> qMap_2 = clearDocReleMap.get(page_2);
		if(qMap_2.get(topic) != qMap_1.get(topic)){
			return 1;
		}else{
			return -1;
		}		
	}
	
	public void getDocReleMap_preference_clear(String xmlDir, NTCIR_EVAL_TASK eval,
			NTCIR11_TOPIC_TYPE type, String outputDir){
		//
		////
		ArrayList<Triple<String, String, Integer>> triList = getXmlQrel(xmlDir, eval, type);
		HashSet<String> topicSet = new HashSet<String>();
		
		//docid->{queryid->releDegree}
		HashMap<String, HashMap<String, Integer>> clearDocReleMap = new HashMap<>();
		
		HashSet<String> outlierDocSet = new HashSet<>();
		HashMap<String, String> docid2HtmlMap = null;
		
		try {
			if(type == NTCIR11_TOPIC_TYPE.CLEAR){
				String outputUncertainPrePairFile = null;
				String egStr = null;
				String outlierFile = outputDir+"Clear_outlier";		
				
				ArrayList<StrStr> topicStrList = null;
				
				if(eval == NTCIR_EVAL_TASK.NTCIR11_DR_CH){
					outlierFile += "_CH.txt";
					egStr = "比如 ";
					topicStrList = NTCIRLoader.loadNTCIR11TopicList(Lang.Chinese);
					docid2HtmlMap = _chDocid2HtmlMap;
					
				}else if(eval == NTCIR_EVAL_TASK.NTCIR11_DR_EN){
					topicStrList = NTCIRLoader.loadNTCIR11TopicList(Lang.English);
					outlierFile += "_EN.txt";
					loadNTCIR11BaselineDocs_EN();
					egStr = "such as ";
					outputUncertainPrePairFile = outputDir+"EN_Clear_UncertainPrePair.xlsx";	
					docid2HtmlMap = _enDocid2HtmlMap;
				}
				
				//used topic
				ArrayList<String> topicList = NTCIRLoader.loadNTCIR11Topic(eval, type);
				topicSet.addAll(topicList);	
				HashMap<String, String> topicStrMap = new HashMap<>();
				for(StrStr element: topicStrList){
					String topicid = element.getFirst();
					if(topicSet.contains(topicid)){
						topicStrMap.put(topicid, element.getSecond());
					}
				}
								
				//docid, queryid, releLevel
				for(Triple<String, String, Integer> triple: triList){
					if(topicSet.contains(triple.getSecond())){
						String docid = triple.getFirst();
						String queryid = triple.getSecond();
						Integer releLevel = triple.getThird();
						//
						if(clearDocReleMap.containsKey(docid)){
							HashMap<String, Integer> qMap = clearDocReleMap.get(docid);
							qMap.put(queryid, releLevel);
						}else{
							HashMap<String, Integer> qMap = new HashMap<>();
							qMap.put(queryid, releLevel);
							clearDocReleMap.put(docid, qMap);
						}
					}					
				}
				
				////
				int totalUnsureCnt = 0;
				int totalSureCnt = 0;
				
				int totalNotIncludedDocCnt = 0;
				int totalIncludedDocCnt = 0;
				//
				
				////unsure
				int unsureBeginIndex = 1;
				XSSFWorkbook unsureOutBook = new XSSFWorkbook();					
				XSSFSheet unsureGroupSheet = unsureOutBook.createSheet("Pre");
				//head
				XSSFRow unsureNameRow = unsureGroupSheet.createRow(0);
				
				Cell unsurecell_1 = unsureNameRow.createCell(0);
				unsurecell_1.setCellValue("topicid");
				
				Cell unsurecell_2 = unsureNameRow.createCell(1);						
				unsurecell_2.setCellValue("topicstr");
				
				Cell unsurecell_3 = unsureNameRow.createCell(2);
				unsurecell_3.setCellValue("Page_1");
				
				Cell unsurecell_4 = unsureNameRow.createCell(3);						
				unsurecell_4.setCellValue("Page_2");
				
				Cell unsurecell_5 = unsureNameRow.createCell(4);
				unsurecell_5.setCellValue("Page_1_InforNeeds");
				////
				for(String topic: topicList){
					int unsureCnt = 0;
					int sureCnt = 0;
					
					int notIncludedCnt = 0;
					int includedCnt = 0;
					
					
					ArrayList<String> releDocList = getReleDocList_clear(topic, clearDocReleMap);
					
					
					///*
					//check outlier documents, i.e., relevant but not available documents right now
					for(String releDoc: releDocList){
						if(!docid2HtmlMap.containsKey(releDoc)){
							notIncludedCnt ++;
							//System.out.println(releDoc);
							outlierDocSet.add(releDoc);
						}else{
							includedCnt++;
						}
					}
					
					totalNotIncludedDocCnt += notIncludedCnt;
					totalIncludedDocCnt += includedCnt;
					//*/
					
					ArrayList<String> pairList = new ArrayList<>();
					
					int size = releDocList.size();
					for(int i=0; i<size-1; i++){
						String page_1 = releDocList.get(i);
						
						for(int j=i+1; j<size; j++){
							String page_2 = releDocList.get(j);
							
							if(calPreference_clear(topic, page_1, page_2, clearDocReleMap) < 0){
								unsureCnt++;
								outputToExcel_clear(topicStrMap.get(topic), egStr, true, pairList, page_1, page_2);								
							}else{
								sureCnt++;
							}							
						}
					}
					
					totalUnsureCnt += unsureCnt;
					totalSureCnt   += sureCnt;
					
					//
					try {
					////unsure body
						for(int k=0; k<pairList.size(); k++){
							
							XSSFRow unsureKRow = unsureGroupSheet.createRow(k+unsureBeginIndex);
							
							String pairStr = pairList.get(k);
							
							String [] array = pairStr.split(",");
							
							Cell uncell_1 = unsureKRow.createCell(0);
							uncell_1.setCellValue(topic);
							Cell uncell_2 = unsureKRow.createCell(1);
							uncell_2.setCellValue(topicStrMap.get(topic));
							
							Cell uncell_3 = unsureKRow.createCell(2);
							uncell_3.setCellValue(array[0]);
							Cell uncell_4 = unsureKRow.createCell(3);
							uncell_4.setCellValue(array[1]);
							Cell uncell_5 = unsureKRow.createCell(4);
							uncell_5.setCellValue(array[2]);
							//Cell cell_6 = kRow.createCell(5);
							//cell_6.setCellValue(array[3]);
							//Cell cell_7 = kRow.createCell(6);
							//cell_7.setCellValue(array[4]);
						}
						
						unsureBeginIndex += pairList.size();
					} catch (Exception e) {
						e.printStackTrace();
					}
					
				}
				
				///*
				//buffer outlier doc
				BufferedWriter outlierWriter = IOText.getBufferedWriter_UTF8(outlierFile);
				ArrayList<String> outlierList = new ArrayList<>();				
				for(String outlierDoc: outlierDocSet){
					outlierList.add(outlierDoc);
				}				
				Collections.sort(outlierList);				
				for(String outlierDoc: outlierList){
					outlierWriter.write(outlierDoc);
					outlierWriter.newLine();
				}
								
				outlierWriter.flush();
				outlierWriter.close();
				//*/
					
				////1
				OutputStream unsureOutputStream = new FileOutputStream(outputUncertainPrePairFile);
				unsureOutBook.write(unsureOutputStream);
				unsureOutputStream.flush();
				unsureOutputStream.close();
				
				////				
				System.out.println();
				System.out.println("Total unsure count:\t"+totalUnsureCnt);
				System.out.println("Total sure count:\t"+totalSureCnt);
				System.out.println("Total unincluded cnt:\t"+totalNotIncludedDocCnt);
				System.out.println("Total included cnt:\t"+totalIncludedDocCnt);
				System.out.println("Total docs cnt:\t"+clearDocReleMap.size());
				
			}else{
				System.err.println("Error!");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}		
	}
	
	
	////crowdsourcing task
	static HashMap<String, String> _enDocid2HtmlMap = new HashMap<>();
	static HashMap<String, String> _chDocid2HtmlMap = new HashMap<>();
	
	private static void loadDocs(){
		loadNTCIR11BaselineDocs_EN();
		System.out.println("en size:\t"+_enDocid2HtmlMap.size());
		loadNTCIR11BaselineDocs_CH();
		System.out.println("ch size:\t"+_chDocid2HtmlMap.size());
	}
	//docName -> docContent
	public static HashMap<String, String> loadNTCIR11BaselineDocs_EN(){
		//HashMap<String, String> docMap = new HashMap<String, String>();
		
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
						_enDocid2HtmlMap.put(docName, buffer.toString());						
						buffer = null;
						//						
						fields = line.split("\\s");
						docName = fields[2];
						buffer = new StringBuffer();						
					}else{
						buffer.append(line);
						buffer.append(NEWLINE);						
					}
				}
				
				_enDocid2HtmlMap.put(docName, buffer.toString());				
				buffer = null;
			}
		
		}catch(Exception e){
			e.printStackTrace();
		}
		
		return _enDocid2HtmlMap;
	}	
	//
	public static HashMap<String, String> loadNTCIR11BaselineDocs_CH(){
		String htmlDir=DataSetDiretory.ROOT+"/ntcir/ntcir-11/DR/CH/DR_Baseline/docs/";
		ArrayList<File> files = FileFinder.GetAllFiles(htmlDir, "", true);
		
		
		String doc_name;
		for(File f: files){					
			int index = f.getAbsolutePath().lastIndexOf("\\");
			doc_name = f.getAbsolutePath().substring(index+1, f.getAbsolutePath().indexOf("."));
			
			ArrayList<String> lineList = IOText.getLinesAsAList(f.getAbsolutePath(), "GB2312");
			StringBuffer buffer = new StringBuffer();
			for(String line: lineList){
				if(line.equals("<DOC>")
						|| line.equals("</DOC>")
						|| (line.indexOf("<DOCNO>")>=0 && line.indexOf("</DOCNO>")>=0)
						|| (line.indexOf("<URL>")>=0 && line.indexOf("</URL>")>=0)){
					////
					continue;					
				}else{
					buffer.append(line);
					buffer.append(NEWLINE);						
				}
			}
			
			_chDocid2HtmlMap.put(doc_name, buffer.toString());
		}
		
		return _chDocid2HtmlMap;
	}
	
	private static String getEnHtml(String docid){
		if(_enDocid2HtmlMap.containsKey(docid)){
			return _enDocid2HtmlMap.get(docid);
		}else{
			System.err.println("!!!");
			return null;
		}		
	}
	private static String getChHtml(String docid){
		if(_chDocid2HtmlMap.containsKey(docid)){
			return _chDocid2HtmlMap.get(docid);
		}else{
			System.err.println("!!!");
			return null;
		}		
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
		//PreProcessor.compareMetricERRIA(NTCIR_EVAL_TASK.NTCIR11_DR_EN, NTCIR11_TOPIC_TYPE.UNCLEAR, 20);
		
		////marginal utility
		/**
		 * marginal utility should go beyond coverage-based evaluation
		 * (1) for getting the outlier, 
		 * **/
		//6-1
		/*
		
		String xmlDir    = "C:/T/Research/CurrentResearch/NTCIR/NTCIR-11/Ntcir11-IMine/Eval-IMine/0913/";
		String outputDir = "C:/T/Research/CurrentResearch/NTCIR/NTCIR-11/Ntcir11-IMine/Eval-IMine/UncertainPairs/MarginalUtility/";
		
		PreProcessor preProcessor = new PreProcessor();
		
		//(1) Total unsure count:	6162 Total sure count:	18712
		//preProcessor.getDocReleMap_marginal_unclear(xmlDir, null, NTCIR_EVAL_TASK.NTCIR11_DR_EN, NTCIR11_TOPIC_TYPE.UNCLEAR, outputDir);
		
		//(2) Total unsure count:	8106 Total sure count:	27140
		//preProcessor.getDocReleMap_marginal(xmlDir, null, NTCIR_EVAL_TASK.NTCIR11_DR_CH, NTCIR11_TOPIC_TYPE.UNCLEAR, outputDir);
		
		//(3)
		//preProcessor.getDocReleMap_marginal_clear(xmlDir, NTCIR_EVAL_TASK.NTCIR11_DR_EN, NTCIR11_TOPIC_TYPE.CLEAR, outputDir);
		
		*/
		/**Based on the output, we can observe the significant redundancy!**/
		
		////preference
		/**
		 * how to deal with documents that are relevant to parallel subtopics?!
		 * In the context of subtopic based evaluation, when using preference, it is hard to deal with parallel subtopics?
		 * is it only feasible to deal with first-level subtopics?
		 * **/
		//7
		///*
		boolean skipParallel = false;
		String xmlDir    = "C:/T/Research/CurrentResearch/NTCIR/NTCIR-11/Ntcir11-IMine/Eval-IMine/0913/";
		String outputDir = "C:/T/Research/CurrentResearch/NTCIR/NTCIR-11/Ntcir11-IMine/Eval-IMine/UncertainPairs/Preference/";
		
		PreProcessor preProcessor = new PreProcessor();
		//(1)
		//preProcessor.getDocReleMap(xmlDir, null, NTCIR_EVAL_TASK.NTCIR11_DR_EN, NTCIR11_TOPIC_TYPE.UNCLEAR, outputDir);
		
		//(2)
		//preProcessor.getDocReleMap_preference(skipParallel, xmlDir, null, NTCIR_EVAL_TASK.NTCIR11_DR_CH, NTCIR11_TOPIC_TYPE.UNCLEAR, outputDir);
		
		//(3)
		preProcessor.getDocReleMap_preference_clear(xmlDir, NTCIR_EVAL_TASK.NTCIR11_DR_EN, NTCIR11_TOPIC_TYPE.CLEAR, outputDir);
		//*/
		
		//8 first load documents
		/*
		PreProcessor preProcessor = new PreProcessor();
		preProcessor.loadDocs();
		*/
		
	}
}
