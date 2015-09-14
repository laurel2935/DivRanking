package org.archive.ireval;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Vector;

import org.archive.util.Language.Lang;
import org.archive.util.io.IOText;
import org.archive.util.tuple.Pair;
import org.archive.util.tuple.PairComparatorByFirst_Asc;
import org.archive.util.tuple.PairComparatorBySecond_Desc;
import org.archive.util.tuple.Triple;

/**
 * A set of evaluation metrics w.r.t. information retrieval
 * 
 * **/

public class IREval {
	private static final boolean debug = true;
	/**
	 * DIV_NTCIR_SM		;
	 * DIV_NTCIR_DR		blank
	 * DIV_TREC			blank
	 * CLEAR_NTCIR		no subtopic, metric as nDCG, used for clear topics of ntcir-11, temporal rank (TIR) of temporalia-1 subtask
	 * 					(since TIR in Temporalia-1 is essentially a per-intent ranking instead of diversified ranking)
	 * 
	 * TDIV_NTCIR		temporal setting w.r.t. TIR in Temporalia-1
	 * **/
	public static enum EVAL_TYPE{DIV_NTCIR_SM, DIV_NTCIR_DR, DIV_TREC, CLEAR_NTCIR, TDIV_NTCIR};
	
	/////////////////////
	//Common
	/////////////////////
	
	//a system run, i.e., topic -> list of <rank, item-id>
	//split symbol may different due to different tasks
	HashMap<String, ArrayList<Integer>> _sysRun;
	
	//topic list
	ArrayList<String> _topicList;
	
	
	//here item refers to subtopic strings or doc_names
	ArrayList<String> _itemPooL;
	//item -> item index
	HashMap<String, Integer> _itemToIndex;
	
	
	////////////////////
	//Without subtopic consideration
	////////////////////
	
	//relevance assessment, i.e., topic -> list of <item-id, relevance-level>, the list is ranked in decreasing order of relevance-level
	HashMap<String, ArrayList<Pair<Integer, Integer>>> _topicToReleItemMap;	
	//relevance assessment, i.e., item-id -> Map of <topic, relevance-level>
	HashMap<Integer, HashMap<String, Integer>> _itemReleMap;
	
	
	
	////////////////////
	//With subtopic consideration
	////////////////////
	
	//topic -> list of <subtopic index, probability>
	HashMap<String, ArrayList<Pair<Integer, Double>>> _topicToSubtopicMap;
	
	//relevance assessment, i.e., topic -> subtopic -> list of <item-id, relevance-level>, the list is ranked in an decreasing order of relevance-level 
	HashMap<String, HashMap<Integer, ArrayList<Pair<Integer, Integer>>>> _topicToSubtopicToReleItemMap;
	
	//relevance assessment, i.e., item-id -> map of <topicid, set of <subtopic-id, relevance level>>
	HashMap<Integer, HashMap<String, HashSet<Pair<Integer, Integer>>>> _divItemReleMap;
	
	
	IREval(){
		this._sysRun = new HashMap<String, ArrayList<Integer>>();
		
		this._itemPooL = new ArrayList<String>();
		this._itemToIndex = new HashMap<String, Integer>();
		
		this._topicList = new ArrayList<String>();
	}
	
	
	/////////////////////
	//Preliminaries
	/////////////////////
	/**
	 * 
	 * **/
	private int getItemIndex(String item)
	{
		Integer itemIndex = this._itemToIndex.get(item);
		if(null == itemIndex)
		{
			int index = this._itemPooL.size();
			this._itemPooL.add(item);
			this._itemToIndex.put(item, index);
			//
			return index;
		}else{
			return itemIndex;
		}
	}
	
	private String getItem(Integer itemID){
		if(0<=itemID && itemID<_itemPooL.size()){
			return _itemPooL.get(itemID);
		}else{
			return null;
		}		
	}
	
	/**
	 * 
	 * **/
	private void loadSysRun(EVAL_TYPE evalType, String sysRunFile){	
		
		try {
			ArrayList<String> lineList = IOText.getLinesAsAList_UTF8(sysRunFile);
			
			//skip head-description section, i.e., the first line, thus we start from index=1
			
			if(evalType == EVAL_TYPE.DIV_NTCIR_SM){				
				
				String []array;
				for(int i=1; i<lineList.size(); i++){
					//array[0]:topic-id / array[1]:0 / array[2]:item-string / array[3]:rank / array[4]:score / array[5]:run-name
					array = lineList.get(i).split(";");
					String topicid = array[0];
					String item = array[2];
					
					if(_sysRun.containsKey(topicid)){
						ArrayList<Integer> itemList = _sysRun.get(topicid);
						itemList.add(getItemIndex(item));
					}else{
						ArrayList<Integer> itemList = new ArrayList<Integer>();
						itemList.add(getItemIndex(item));
						
						_sysRun.put(topicid, itemList);
					}
				}
				
			}else if(evalType == EVAL_TYPE.DIV_NTCIR_DR){
				
				String []array;
				for(int i=1; i<lineList.size(); i++){
					//array[0]:topic-id / array[1]:0 / array[2]:doc-name / array[3]:rank / array[4]:score / array[5]:run-name
					array = lineList.get(i).split("\\s");
					String topicid = array[0];
					String item = array[2];
					
					if(_sysRun.containsKey(topicid)){
						ArrayList<Integer> itemList = _sysRun.get(topicid);
						itemList.add(getItemIndex(item));
					}else{
						ArrayList<Integer> itemList = new ArrayList<Integer>();
						itemList.add(getItemIndex(item));
						_sysRun.put(topicid, itemList);
					}
				}
				
			}else if(evalType == EVAL_TYPE.CLEAR_NTCIR){
				
				String []array;
				for(int i=1; i<lineList.size(); i++){
					//array[0]:subtopic-id / array[1]:rank / array[2]:item-string / array[3]:group-id / array[4]:run-name
					array = lineList.get(i).split("\\s");
					String subtopicid = array[0];
					String item = array[2];
					
					if(_sysRun.containsKey(subtopicid)){
						ArrayList<Integer> itemList = _sysRun.get(subtopicid);
						itemList.add(getItemIndex(item));
					}else{
						ArrayList<Integer> itemList = new ArrayList<Integer>();
						itemList.add(getItemIndex(item));
						_sysRun.put(subtopicid, itemList);
					}
				}
				
			}else{
				System.err.println("Unexcepted Eval-Type!");
				System.exit(0);
			}
			
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}
	
	/**
	 * 
	 * **/
	private void loadStandardFile(EVAL_TYPE evalType, String standardQreleFile, String standardIprobFile){
		
		try {			
			
			HashSet<String> topicSet = new HashSet<String>();
			
			if(evalType == EVAL_TYPE.CLEAR_NTCIR){
				
				ArrayList<String> clearLineList = IOText.getLinesAsAList_UTF8(standardQreleFile);
				
				this._topicToReleItemMap = new HashMap<String, ArrayList<Pair<Integer,Integer>>>();
				this._itemReleMap = new HashMap<Integer, HashMap<String,Integer>>();
				
				String []array;
				for(String line: clearLineList){
					//array[0]:subtopic-id / array[1]:item-string / array[3]:relevance-level
					array = line.split("\\s");
					String subtopicid = array[0];
					//String item = array[1];
					Integer itemID = getItemIndex(array[1]);
					Integer releLevel = Integer.parseInt(array[2].substring(1));
					
					//for _topicList
					if(!topicSet.contains(subtopicid)){
						topicSet.add(subtopicid);
						
						_topicList.add(subtopicid);
					}
					
					//for _topicToReleItemMap
					if(_topicToReleItemMap.containsKey(subtopicid)){
						ArrayList<Pair<Integer,Integer>> itemPairList = _topicToReleItemMap.get(subtopicid);
						itemPairList.add(new Pair<Integer, Integer>(itemID, releLevel));
					}else{
						ArrayList<Pair<Integer,Integer>> itemPairList = new ArrayList<Pair<Integer,Integer>>();
						itemPairList.add(new Pair<Integer, Integer>(itemID, releLevel));
						_topicToReleItemMap.put(subtopicid, itemPairList);
					}
					
					//for _itemReleMap
					if(_itemReleMap.containsKey(itemID)){
						_itemReleMap.get(itemID).put(subtopicid, releLevel);
					}else{
						HashMap<String, Integer> releMap = new HashMap<String, Integer>();
						releMap.put(subtopicid, releLevel);
						
						_itemReleMap.put(itemID, releMap);
					}
				}	
				
				//sort item list
				for(Entry<String, ArrayList<Pair<Integer,Integer>>> entry: _topicToReleItemMap.entrySet()){
					ArrayList<Pair<Integer,Integer>> itemPairList = entry.getValue();
					Collections.sort(itemPairList, new PairComparatorBySecond_Desc<Integer, Integer>());
				}		
				
			} else if (evalType.toString().startsWith("DIV_NTCIR")){
				
				ArrayList<String> divLineList = IOText.getLinesAsAList_UTF8(standardQreleFile);
				
				String split = null;
				if(evalType == EVAL_TYPE.DIV_NTCIR_DR){
					split = "\\s";
				}else {
					split = ";";
				}
				
				//1
				this._topicToSubtopicMap = new HashMap<String, ArrayList<Pair<Integer,Double>>>();
				
				ArrayList<String> probLineList = IOText.getLinesAsAList_UTF8(standardIprobFile);
				String [] probArray;
				for(String probLine: probLineList){
					//[0]: topicid / [1]: subtopic-id / [2]: probability
					probArray = probLine.split(split);
					String topicid = probArray[0];
					Integer stID = Integer.parseInt(probArray[1]);
					Double stPro = Double.parseDouble(probArray[2]);
					
					//for _topicList
					if(!topicSet.contains(topicid)){
						topicSet.add(topicid);
						
						_topicList.add(topicid);
					}					
					
					//sorted in order of subtopic id later: necessary
					if(this._topicToSubtopicMap.containsKey(topicid)){
						ArrayList<Pair<Integer, Double>> stList = this._topicToSubtopicMap.get(topicid);
						stList.add(new Pair<Integer, Double>(stID, stPro));
					}else{
						ArrayList<Pair<Integer, Double>> stList = new ArrayList<Pair<Integer,Double>>();
						stList.add(new Pair<Integer, Double>(stID, stPro));
						
						this._topicToSubtopicMap.put(topicid, stList);
					}
				}	
				//for
				for(Entry<String, ArrayList<Pair<Integer, Double>>> topicEntry: this._topicToSubtopicMap.entrySet()){
					ArrayList<Pair<Integer, Double>> stList = topicEntry.getValue();
					
					Collections.sort(stList, new PairComparatorByFirst_Asc<Integer, Double>());
				}				
				
				//2
				this._topicToSubtopicToReleItemMap = new HashMap<String, HashMap<Integer, ArrayList<Pair<Integer,Integer>>>>();
				this._divItemReleMap = new HashMap<Integer, HashMap<String,HashSet<Pair<Integer,Integer>>>>();
				
				String [] array;
				for(String line: divLineList){
					//[0]: topic-id / array[1]: subtopic-id / array[2]: doc-name / array[3]: releLevel of L
					array = line.split(split);
					String topicid = array[0];
					Integer stID = Integer.parseInt(array[1]);
					Integer itemID = getItemIndex(array[2]);
					Integer releLevel = Integer.parseInt(array[3].substring(1).trim());
										
					//if necessary
					if(releLevel > 0){
						//for _topicToSubtopicToReleItemMap
						if(this._topicToSubtopicToReleItemMap.containsKey(topicid)){
							HashMap<Integer, ArrayList<Pair<Integer, Integer>>> stToReleItemMap = this._topicToSubtopicToReleItemMap.get(topicid);
							
							if(stToReleItemMap.containsKey(stID)){								
								ArrayList<Pair<Integer, Integer>> releItemList = stToReleItemMap.get(stID);
								releItemList.add(new Pair<Integer, Integer>(itemID, releLevel));								
							}else{								
								ArrayList<Pair<Integer, Integer>> releItemList = new ArrayList<Pair<Integer,Integer>>();
								releItemList.add(new Pair<Integer, Integer>(itemID, releLevel));
								stToReleItemMap.put(stID, releItemList);								
							}
							
						}else{
							HashMap<Integer, ArrayList<Pair<Integer, Integer>>> stToReleItemMap = new HashMap<Integer, ArrayList<Pair<Integer,Integer>>>();
							
							ArrayList<Pair<Integer, Integer>> releItemList = new ArrayList<Pair<Integer,Integer>>();
							releItemList.add(new Pair<Integer, Integer>(itemID, releLevel));
							stToReleItemMap.put(stID, releItemList);
							
							this._topicToSubtopicToReleItemMap.put(topicid, stToReleItemMap);
						}
						
						//for _divItemReleMap
						if(this._divItemReleMap.containsKey(itemID)){
							HashMap<String, HashSet<Pair<Integer, Integer>>> usageMap = this._divItemReleMap.get(itemID);
							if(usageMap.containsKey(topicid)){
								HashSet<Pair<Integer, Integer>> releSet = usageMap.get(topicid);
								releSet.add(new Pair<Integer, Integer>(stID, releLevel));
							}else {
								HashSet<Pair<Integer, Integer>> releSet = new HashSet<Pair<Integer,Integer>>();
								releSet.add(new Pair<Integer, Integer>(stID, releLevel));
								
								usageMap.put(topicid, releSet);
							}
						}else{
							HashMap<String, HashSet<Pair<Integer, Integer>>> usageMap = new HashMap<String, HashSet<Pair<Integer,Integer>>>();
							
							HashSet<Pair<Integer, Integer>> releSet = new HashSet<Pair<Integer,Integer>>();
							releSet.add(new Pair<Integer, Integer>(stID, releLevel));							
							usageMap.put(topicid, releSet);
							
							this._divItemReleMap.put(itemID, usageMap);							
						}
					}					
				}	
				
				//sort releItemList for each subtopic
				for(Entry<String, HashMap<Integer, ArrayList<Pair<Integer, Integer>>>> itemEntry: this._topicToSubtopicToReleItemMap.entrySet()){
					HashMap<Integer, ArrayList<Pair<Integer, Integer>>> stReleMap = itemEntry.getValue();
					for(Entry<Integer, ArrayList<Pair<Integer, Integer>>> stEntry: stReleMap.entrySet()){
						ArrayList<Pair<Integer, Integer>> releList = stEntry.getValue();
						
						Collections.sort(releList, new PairComparatorBySecond_Desc<Integer, Integer>());
					}
				}	
				
			}else if(evalType.toString().startsWith("TDIV_NTCIR")){
				
				ArrayList<String> tdivLineList = IOText.getLinesAsAList_UTF8(standardQreleFile);
				
				String split = "\\s";
				
				//1
				this._topicToSubtopicToReleItemMap = new HashMap<String, HashMap<Integer, ArrayList<Pair<Integer,Integer>>>>();
				this._divItemReleMap = new HashMap<Integer, HashMap<String,HashSet<Pair<Integer,Integer>>>>();
				
				String [] array;
				for(String tdivLine: tdivLineList){
					
					//array[0]: subtopic-id / array[1]: doc-name / array[2]: releLevel of L
					array = tdivLine.split(split);
					String topicid = array[0].substring(0, 3);
					Integer stID = toSubID(array[0]);
					Integer itemID = getItemIndex(array[1]);
					Integer releLevel = Integer.parseInt(array[2].substring(1).trim());
					
					//for _topicList
					if(!topicSet.contains(topicid)){
						topicSet.add(topicid);
						
						_topicList.add(topicid);
					}
										
					//if necessary
					if(releLevel > 0){
						//for _topicToSubtopicToReleItemMap
						if(this._topicToSubtopicToReleItemMap.containsKey(topicid)){
							HashMap<Integer, ArrayList<Pair<Integer, Integer>>> stToReleItemMap = this._topicToSubtopicToReleItemMap.get(topicid);
							
							if(stToReleItemMap.containsKey(stID)){								
								ArrayList<Pair<Integer, Integer>> releItemList = stToReleItemMap.get(stID);
								releItemList.add(new Pair<Integer, Integer>(itemID, releLevel));								
							}else{								
								ArrayList<Pair<Integer, Integer>> releItemList = new ArrayList<Pair<Integer,Integer>>();
								releItemList.add(new Pair<Integer, Integer>(itemID, releLevel));
								stToReleItemMap.put(stID, releItemList);								
							}
							
						}else{
							HashMap<Integer, ArrayList<Pair<Integer, Integer>>> stToReleItemMap = new HashMap<Integer, ArrayList<Pair<Integer,Integer>>>();
							
							ArrayList<Pair<Integer, Integer>> releItemList = new ArrayList<Pair<Integer,Integer>>();
							releItemList.add(new Pair<Integer, Integer>(itemID, releLevel));
							stToReleItemMap.put(stID, releItemList);
							
							this._topicToSubtopicToReleItemMap.put(topicid, stToReleItemMap);
						}
						
						//for _divItemReleMap
						if(this._divItemReleMap.containsKey(itemID)){
							HashMap<String, HashSet<Pair<Integer, Integer>>> usageMap = this._divItemReleMap.get(itemID);
							if(usageMap.containsKey(topicid)){
								HashSet<Pair<Integer, Integer>> releSet = usageMap.get(topicid);
								releSet.add(new Pair<Integer, Integer>(stID, releLevel));
							}else {
								HashSet<Pair<Integer, Integer>> releSet = new HashSet<Pair<Integer,Integer>>();
								releSet.add(new Pair<Integer, Integer>(stID, releLevel));
								
								usageMap.put(topicid, releSet);
							}
						}else{
							HashMap<String, HashSet<Pair<Integer, Integer>>> usageMap = new HashMap<String, HashSet<Pair<Integer,Integer>>>();
							
							HashSet<Pair<Integer, Integer>> releSet = new HashSet<Pair<Integer,Integer>>();
							releSet.add(new Pair<Integer, Integer>(stID, releLevel));							
							usageMap.put(topicid, releSet);
							
							this._divItemReleMap.put(itemID, usageMap);							
						}
					}					
				}	
				
				//sort releItemList for each subtopic
				for(Entry<String, HashMap<Integer, ArrayList<Pair<Integer, Integer>>>> itemEntry: this._topicToSubtopicToReleItemMap.entrySet()){
					HashMap<Integer, ArrayList<Pair<Integer, Integer>>> stReleMap = itemEntry.getValue();
					for(Entry<Integer, ArrayList<Pair<Integer, Integer>>> stEntry: stReleMap.entrySet()){
						ArrayList<Pair<Integer, Integer>> releList = stEntry.getValue();
						
						Collections.sort(releList, new PairComparatorBySecond_Desc<Integer, Integer>());
					}
				}
				
				//for tir in Temporalia-1
				this._topicToSubtopicMap = new HashMap<String, ArrayList<Pair<Integer,Double>>>();
				for(String topicid: _topicList){
					//sorted in order of subtopic id later: necessary
					ArrayList<Pair<Integer, Double>> stList = new ArrayList<Pair<Integer,Double>>();
					stList.add(new Pair<Integer, Double>(1, 0.25));
					stList.add(new Pair<Integer, Double>(2, 0.25));
					stList.add(new Pair<Integer, Double>(3, 0.25));
					stList.add(new Pair<Integer, Double>(4, 0.25));
					
					this._topicToSubtopicMap.put(topicid, stList);
				}				
				
			}else if(evalType.toString().startsWith("DIV_TREC")){
				
				ArrayList<String> qrelLineList = IOText.getLinesAsAList_UTF8(standardQreleFile);
				
				String split = "\\s";
				
				/*
				//1
				this._topicToSubtopicMap = new HashMap<String, ArrayList<Pair<Integer,Double>>>();
				
				ArrayList<String> probLineList = IOText.getLinesAsAList_UTF8(standardIprobFile);
				String [] probArray;
				for(String probLine: probLineList){
					//[0]: topicid / [1]: subtopic-id / [2]: probability
					probArray = probLine.split(split);
					String topicid = probArray[0];
					Integer stID = Integer.parseInt(probArray[1]);
					Double stPro = Double.parseDouble(probArray[2]);
					
					//for _topicList
					if(!topicSet.contains(topicid)){
						topicSet.add(topicid);
						
						_topicList.add(topicid);
					}					
					
					//sorted in order of subtopic id later: necessary
					if(this._topicToSubtopicMap.containsKey(topicid)){
						ArrayList<Pair<Integer, Double>> stList = this._topicToSubtopicMap.get(topicid);
						stList.add(new Pair<Integer, Double>(stID, stPro));
					}else{
						ArrayList<Pair<Integer, Double>> stList = new ArrayList<Pair<Integer,Double>>();
						stList.add(new Pair<Integer, Double>(stID, stPro));
						
						this._topicToSubtopicMap.put(topicid, stList);
					}
				}	
				//for
				for(Entry<String, ArrayList<Pair<Integer, Double>>> topicEntry: this._topicToSubtopicMap.entrySet()){
					ArrayList<Pair<Integer, Double>> stList = topicEntry.getValue();
					
					Collections.sort(stList, new PairComparatorByFirst_Asc<Integer, Double>());
				}
				*/
				
				//2
				this._topicToSubtopicToReleItemMap = new HashMap<String, HashMap<Integer, ArrayList<Pair<Integer,Integer>>>>();
				this._divItemReleMap = new HashMap<Integer, HashMap<String,HashSet<Pair<Integer,Integer>>>>();
				
				String [] array;
				for(String line: qrelLineList){
					//[0]: topic-id / array[1]: subtopic-id / array[2]: doc-name / array[3]: releLevel of L 
					//as for array[3]: releLevel of L. Commonly, it should be >=0 as relevant, in 2011, there is a -2 span case
					array = line.split(split);
					
					String topicid = array[0];
					Integer stID = Integer.parseInt(array[1]);
					Integer itemID = getItemIndex(array[2]);
					Integer releLevel = Integer.parseInt(array[3]);
										
					//if necessary
					if(releLevel > 0){
						//for _topicList
						if(!topicSet.contains(topicid)){
							topicSet.add(topicid);
							
							_topicList.add(topicid);
						} 
						
						//for _topicToSubtopicToReleItemMap
						if(this._topicToSubtopicToReleItemMap.containsKey(topicid)){
							HashMap<Integer, ArrayList<Pair<Integer, Integer>>> stToReleItemMap = this._topicToSubtopicToReleItemMap.get(topicid);
							
							if(stToReleItemMap.containsKey(stID)){								
								ArrayList<Pair<Integer, Integer>> releItemList = stToReleItemMap.get(stID);
								releItemList.add(new Pair<Integer, Integer>(itemID, releLevel));								
							}else{								
								ArrayList<Pair<Integer, Integer>> releItemList = new ArrayList<Pair<Integer,Integer>>();
								releItemList.add(new Pair<Integer, Integer>(itemID, releLevel));
								stToReleItemMap.put(stID, releItemList);								
							}
							
						}else{
							HashMap<Integer, ArrayList<Pair<Integer, Integer>>> stToReleItemMap = new HashMap<Integer, ArrayList<Pair<Integer,Integer>>>();
							
							ArrayList<Pair<Integer, Integer>> releItemList = new ArrayList<Pair<Integer,Integer>>();
							releItemList.add(new Pair<Integer, Integer>(itemID, releLevel));
							stToReleItemMap.put(stID, releItemList);
							
							this._topicToSubtopicToReleItemMap.put(topicid, stToReleItemMap);
						}
						
						//for _divItemReleMap
						if(this._divItemReleMap.containsKey(itemID)){
							HashMap<String, HashSet<Pair<Integer, Integer>>> usageMap = this._divItemReleMap.get(itemID);
							if(usageMap.containsKey(topicid)){
								HashSet<Pair<Integer, Integer>> releSet = usageMap.get(topicid);
								releSet.add(new Pair<Integer, Integer>(stID, releLevel));
							}else {
								HashSet<Pair<Integer, Integer>> releSet = new HashSet<Pair<Integer,Integer>>();
								releSet.add(new Pair<Integer, Integer>(stID, releLevel));
								
								usageMap.put(topicid, releSet);
							}
						}else{
							HashMap<String, HashSet<Pair<Integer, Integer>>> usageMap = new HashMap<String, HashSet<Pair<Integer,Integer>>>();
							
							HashSet<Pair<Integer, Integer>> releSet = new HashSet<Pair<Integer,Integer>>();
							releSet.add(new Pair<Integer, Integer>(stID, releLevel));							
							usageMap.put(topicid, releSet);
							
							this._divItemReleMap.put(itemID, usageMap);							
						}
					}					
				}	
				
				//sort releItemList for each subtopic
				for(Entry<String, HashMap<Integer, ArrayList<Pair<Integer, Integer>>>> itemEntry: this._topicToSubtopicToReleItemMap.entrySet()){
					HashMap<Integer, ArrayList<Pair<Integer, Integer>>> stReleMap = itemEntry.getValue();
					for(Entry<Integer, ArrayList<Pair<Integer, Integer>>> stEntry: stReleMap.entrySet()){
						ArrayList<Pair<Integer, Integer>> releList = stEntry.getValue();
						
						Collections.sort(releList, new PairComparatorBySecond_Desc<Integer, Integer>());
					}
				}
				
			}			
			
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		
	}
	
	private Integer toSubID(String stIDStr){
		String str = stIDStr.substring(3);
		if(str.equals("a")){
			return 1;
		}else if(str.equals("f")){
			return 2;
		}else if(str.equals("p")){
			return 3;
		}else if(str.equals("r")){
			return 4;
		}else{
			System.err.println("type error!");
			return null;
		}		
	}
	
	/**
	 * basic statistics w.r.t. qrel of Temporalia-1
	 * **/

	private void staOfTemporalia_1(){
		//
		//check
		for(String topicid: _topicList){
			int stSum = 0;
			HashMap<Integer, ArrayList<Pair<Integer, Integer>>> stMap = _topicToSubtopicToReleItemMap.get(topicid);
			for(Entry<Integer, ArrayList<Pair<Integer, Integer>>> stEntry: stMap.entrySet()){
				stSum += stEntry.getValue().size();				
			}
			
			int docSum = 0;
			for(Entry<Integer, HashMap<String, HashSet<Pair<Integer, Integer>>>> docMap : _divItemReleMap.entrySet()){
				if(docMap.getValue().containsKey(topicid)){
					docSum += docMap.getValue().get(topicid).size();					
				}
			}
			
			if(stSum != docSum){
				System.out.println(topicid);
				System.out.println("stSum:\t"+stSum);
				System.out.println("docSum:\t"+docSum);
				return;
			}
		}
		
		//Average number of rele-doc per subtopic	
		int docStSum = 0;
		for(Entry<String, HashMap<Integer, ArrayList<Pair<Integer, Integer>>>> tEntry: 
			_topicToSubtopicToReleItemMap.entrySet()){
			
			for(Entry<Integer, ArrayList<Pair<Integer, Integer>>> stEntry: tEntry.getValue().entrySet()){
				
				docStSum += stEntry.getValue().size();
				
			}			
		}		
		System.out.println("AveNumOfReleDocPerSubtopic:\t"+docStSum/(1.0*4*_topicToSubtopicToReleItemMap.size()));
		
		//Average number of rele-doc per topic
		int docTSum = 0;
		for(Entry<String, HashMap<Integer, ArrayList<Pair<Integer, Integer>>>> tEntry: 
			_topicToSubtopicToReleItemMap.entrySet()){
			
			HashSet<Integer> itemSet = new HashSet<Integer>();
			
			for(Entry<Integer, ArrayList<Pair<Integer, Integer>>> stEntry: tEntry.getValue().entrySet()){
				
				for(Pair<Integer, Integer> p: stEntry.getValue()){
					if(!itemSet.contains(p.first)){
						itemSet.add(p.first);
					}
				}
				
			}		
			
			docTSum += itemSet.size();
		}		
		System.out.println("AveNumOfReleDocPerTopic:\t"+docTSum/(1.0*_topicToSubtopicToReleItemMap.size()));
		
		//Average number of mul-rele-doc per topic
		int muldocTSum = 0;
		for(Entry<String, HashMap<Integer, ArrayList<Pair<Integer, Integer>>>> tEntry: 
			_topicToSubtopicToReleItemMap.entrySet()){
			
			HashSet<Integer> itemSet = new HashSet<Integer>();
			HashSet<Integer> mulitemSet = new HashSet<Integer>();
			int tem = 0;
			
			for(Entry<Integer, ArrayList<Pair<Integer, Integer>>> stEntry: tEntry.getValue().entrySet()){
				
				for(Pair<Integer, Integer> p: stEntry.getValue()){
					if(!itemSet.contains(p.first)){
						itemSet.add(p.first);
					}else{
						if(!mulitemSet.contains(p.first)){
							mulitemSet.add(p.first);
							tem++;
						}
					}
				}				
			}		
			
			muldocTSum += tem;
			System.out.print(tem+" ");
		}
		System.out.println();
		
		System.out.println("AveNumOfMulreleDocPerTopic:\t"+muldocTSum/(1.0*_topicToSubtopicToReleItemMap.size()));

		//distribution of mul-rele-document
		int [] mulArray = new int[4];
		mulArray[0] = 0;
		mulArray[1] = 0;
		mulArray[2] = 0;
		mulArray[3] = 0;
		for(Entry<Integer, HashMap<String, HashSet<Pair<Integer, Integer>>>> itemEntry: 
			_divItemReleMap.entrySet()){
			
			HashMap<String, HashSet<Pair<Integer, Integer>>> usageMap = itemEntry.getValue();
			
			for(Entry<String, HashSet<Pair<Integer, Integer>>> usageEntry: usageMap.entrySet()){
				
				mulArray[usageEntry.getValue().size()-1]++;
				
			}			
		}
		//
		System.out.println("NumOf 1-rele documents:\t"+mulArray[0]);
		System.out.println("NumOf 2-rele documents:\t"+mulArray[1]);
		System.out.println("NumOf 3-rele documents:\t"+mulArray[2]);
		System.out.println("NumOf 4-rele documents:\t"+mulArray[3]);
		
	}
	////////////////////////////////////////
	
	
	/**
	 * Metric: P@k
	 * **/
	private ArrayList<Pair<String, Double>> P(int cutoff){
		ArrayList<Pair<String, Double>> pList = new ArrayList<Pair<String,Double>>();
		
		for(String topic: _topicList){
			/*
			if(debug){
				System.out.print("#syslen="+_sysRun.get(topic).size()+"\t");
				
				ArrayList<Pair<Integer,Integer>> itemPairList = _topicToReleItemMap.get(topic);
				int releCount = 0; int nonReleCount = 0;
				for(Pair<Integer, Integer> pair: itemPairList){
					if(pair.second > 0){
						releCount ++;
					}else {
						nonReleCount++;
					}
				}
				System.out.println("#jrel="+releCount+"\t#jnonrele="+nonReleCount);
				
				//
				if(topic.equals("001a")){
					ArrayList<Integer> itemList = _sysRun.get(topic);
					for(int i=0; i<20; i++){
						System.out.println((i+1)+":\t"+getItem(itemList.get(i)));
					}
				}
			}
			*/
			
			if(_sysRun.containsKey(topic)){
				ArrayList<Integer> sysList = _sysRun.get(topic);
				
				int k = Math.min(cutoff, sysList.size());
				
				int releCount = 0;
				for(int i=0; i<k; i++){
					Integer itemID = sysList.get(i);
					if(_itemReleMap.containsKey(itemID)){
						HashMap<String, Integer> releMap = _itemReleMap.get(itemID);
						if(releMap.containsKey(topic) && releMap.get(topic)>0){
							releCount++;
						}
					}
				}
				
				pList.add(new Pair<String, Double>(topic, releCount*1.0/cutoff));
				
			}else{
				System.err.println("No result for topic:\t"+topic);
				pList.add(new Pair<String, Double>(topic, 0.0));
			}
		}
		/*
		if(debug){
			for(Pair<String, Double> p: pList){
				System.out.println(p.toString());
			}
		}
		*/
		return pList;		
	}
	
	private ArrayList<Pair<String, Double>> avgP(EVAL_TYPE evalType, ArrayList<Pair<String, Double>> pList, int cutoff){
		ArrayList<Pair<String, Double>> avgPList = new ArrayList<Pair<String,Double>>();
		
		if(evalType == EVAL_TYPE.CLEAR_NTCIR){
			double pastSum = 0.0;
			int pastCount = 0;
			
			double recencySum = 0.0;
			int recencyCount = 0;
			
			double futureSum = 0.0;
			int futureCount = 0;
			
			double atemporalSum = 0.0;
			int atemporalCount = 0;
			
			double allSum = 0.0;			
			
			for(Pair<String, Double> pair: pList){
				allSum += pair.second;
				
				if(pair.first.endsWith("p")){
					pastSum += pair.second;
					pastCount ++;
				}else if(pair.first.endsWith("r")){
					recencyCount++;
					recencySum += pair.second;
				}else if(pair.first.endsWith("f")){
					futureCount ++;
					futureSum += pair.second;
				}else {
					atemporalCount++;
					atemporalSum += pair.second;
				}
			}			
			
			DecimalFormat df = new DecimalFormat("0.0000");//Double.parseDouble(df.format())
			
			avgPList.add(new Pair<String, Double>("all\t\tP@"+Integer.toString(cutoff), Double.parseDouble(df.format(allSum/pList.size())) ));
			avgPList.add(new Pair<String, Double>("past\t\tP@"+Integer.toString(cutoff), Double.parseDouble(df.format(pastSum/pastCount)) ));
			avgPList.add(new Pair<String, Double>("recency\t\tP@"+Integer.toString(cutoff), Double.parseDouble(df.format(recencySum/recencyCount)) ));
			avgPList.add(new Pair<String, Double>("future\t\tP@"+Integer.toString(cutoff), Double.parseDouble(df.format(futureSum/futureCount)) ));
			avgPList.add(new Pair<String, Double>("atemporal\tP@"+Integer.toString(cutoff), Double.parseDouble(df.format(atemporalSum/atemporalCount)) ));
			
			if(debug){
				for(Pair<String, Double> avgP: avgPList){
					System.out.println(avgP.toString());
				}
			}
		}
		
		return avgPList;
	}
	
	/**
	 * Metric: AP
	 * **/
	private ArrayList<Pair<String, Double>> AP(int cutoff){
		ArrayList<Pair<String, Double>> apList = new ArrayList<Pair<String,Double>>();
		
		for(String topic: _topicList){

			ArrayList<Integer> sysList = _sysRun.get(topic);
						
			int releCount = 0;
			ArrayList<Double> pList = new ArrayList<Double>();
			for(int i=0; i<cutoff; i++){
				Integer itemID = sysList.get(i);
				if(_itemReleMap.containsKey(itemID)){
					HashMap<String, Integer> releMap = _itemReleMap.get(itemID);
					if(releMap.containsKey(topic) && releMap.get(topic)>0){
						releCount++;
						pList.add(releCount*1.0/(i+1));
					}else {
						pList.add(0.0);
					}
				}else{
					pList.add(0.0);
				}
			}
			
			double sum = 0.0;
			for(Double p: pList){
				sum += p;
			}
			
			apList.add(new Pair<String, Double>(topic, sum/cutoff));			
		}
		
		if(debug){
			for(Pair<String, Double> ap: apList){
				System.out.println(ap.toString());
			}
		}
		
		return apList;
	}
	
	private ArrayList<Pair<String, Double>> MAP(EVAL_TYPE evalType, ArrayList<Pair<String, Double>> apList, int cutoff){
		ArrayList<Pair<String, Double>> mapList = new ArrayList<Pair<String,Double>>();
		
		if(evalType == EVAL_TYPE.CLEAR_NTCIR){
			double pastSum = 0.0;
			int pastCount = 0;
			
			double recencySum = 0.0;
			int recencyCount = 0;
			
			double futureSum = 0.0;
			int futureCount = 0;
			
			double atemporalSum = 0.0;
			int atemporalCount = 0;
			
			double allSum = 0.0;			
			
			for(Pair<String, Double> pair: apList){
				allSum += pair.second;
				
				if(pair.first.endsWith("p")){
					pastSum += pair.second;
					pastCount ++;
				}else if(pair.first.endsWith("r")){
					recencyCount++;
					recencySum += pair.second;
				}else if(pair.first.endsWith("f")){
					futureCount ++;
					futureSum += pair.second;
				}else {
					atemporalCount++;
					atemporalSum += pair.second;
				}
			}			
			
			mapList.add(new Pair<String, Double>("all\t\tMAP@"+Integer.toString(cutoff), allSum/apList.size()));
			mapList.add(new Pair<String, Double>("past\t\tMAP@"+Integer.toString(cutoff), pastSum/pastCount));
			mapList.add(new Pair<String, Double>("recency\t\tMAP@"+Integer.toString(cutoff), recencySum/recencyCount));
			mapList.add(new Pair<String, Double>("future\t\tMAP@"+Integer.toString(cutoff), futureSum/futureCount));
			mapList.add(new Pair<String, Double>("atemporal\tMAP@"+Integer.toString(cutoff), atemporalSum/atemporalCount));
			
			if(debug){
				for(Pair<String, Double> map: mapList){
					System.out.println(map.toString());
				}
			}
		}
		
		return mapList;
	}
	
	/**
	 * Metric: MSnDCG@k
	 * **/
	private ArrayList<Pair<String, Double>> MSnDCG(int cutoff){
		ArrayList<Pair<String, Double>> MSnDCGList = new ArrayList<Pair<String,Double>>();
		
		for(String topic: _topicList){
			//run
			ArrayList<Integer> runItemList = _sysRun.get(topic);
			double runCumuGainValue = 0.0;
			for(int i=0; i<cutoff; i++){
				Integer runItemID = runItemList.get(i);
				if(_itemReleMap.containsKey(runItemID)){
					HashMap<String, Integer> releMap = _itemReleMap.get(runItemID);
					if(releMap.containsKey(topic)){
						Integer runGainValue = _itemReleMap.get(runItemID).get(topic);
						runCumuGainValue += (runGainValue*1.0/Math.log10(i+2));
					}					
				}				
			}
			
			//ideal
			ArrayList<Pair<Integer, Integer>> idealItemList = _topicToReleItemMap.get(topic);
			double idealCumuGainValue = 0.0;
			for(int j=0; j<cutoff; j++){
				Integer idealGainValue = idealItemList.get(j).second;
				idealCumuGainValue += (idealGainValue*1.0/Math.log10(j+2));
			}
			
			//MSnDCG@k
			MSnDCGList.add(new Pair<String, Double>(topic, runCumuGainValue/idealCumuGainValue));			
		}
		
		if(debug){
			for(Pair<String, Double> MSnDCG: MSnDCGList){
				System.out.println(MSnDCG.toString());
			}
		}
		
		return MSnDCGList;
	}
	
	private ArrayList<Pair<String, Double>> avgMSnDCG(EVAL_TYPE evalType, ArrayList<Pair<String, Double>> MSnDCGList, int cutoff){
		ArrayList<Pair<String, Double>> avgMSnDCGList = new ArrayList<Pair<String,Double>>();
		
		if(evalType == EVAL_TYPE.CLEAR_NTCIR){
			double pastSum = 0.0;
			int pastCount = 0;
			
			double recencySum = 0.0;
			int recencyCount = 0;
			
			double futureSum = 0.0;
			int futureCount = 0;
			
			double atemporalSum = 0.0;
			int atemporalCount = 0;
			
			double allSum = 0.0;			
			
			for(Pair<String, Double> pair: MSnDCGList){
				allSum += pair.second;
				
				if(pair.first.endsWith("p")){
					pastSum += pair.second;
					pastCount ++;
				}else if(pair.first.endsWith("r")){
					recencyCount++;
					recencySum += pair.second;
				}else if(pair.first.endsWith("f")){
					futureCount ++;
					futureSum += pair.second;
				}else {
					atemporalCount++;
					atemporalSum += pair.second;
				}
			}			
			
			avgMSnDCGList.add(new Pair<String, Double>("all\t\tMSnDCG@"+Integer.toString(cutoff), allSum/MSnDCGList.size()));
			avgMSnDCGList.add(new Pair<String, Double>("past\t\tMSnDCG@"+Integer.toString(cutoff), pastSum/pastCount));
			avgMSnDCGList.add(new Pair<String, Double>("recency\t\tMSnDCG@"+Integer.toString(cutoff), recencySum/recencyCount));
			avgMSnDCGList.add(new Pair<String, Double>("future\t\tMSnDCG@"+Integer.toString(cutoff), futureSum/futureCount));
			avgMSnDCGList.add(new Pair<String, Double>("atemporal\tMSnDCG@"+Integer.toString(cutoff), atemporalSum/atemporalCount));
			
			if(debug){
				for(Pair<String, Double> avgMSnDCG: avgMSnDCGList){
					System.out.println(avgMSnDCG.toString());
				}
			}
		}
		
		return avgMSnDCGList;
		
	}
	
	/**
	 * Metric: nDCG@k Note: the original definition of nDCG due to base
	 * **/
	private ArrayList<Pair<String, Double>> nDCG(int base, int cutoff){
		ArrayList<Pair<String, Double>> nDCGList = new ArrayList<Pair<String,Double>>();
		
		for(String topic: _topicList){
			//run 
			ArrayList<Integer> runItemList = _sysRun.get(topic);
			
			if(null == runItemList){
				System.err.println("No results for "+topic);				
				nDCGList.add(new Pair<String, Double>(topic, 0.0));					
				continue;
			}
			
			int runMaxCur = Math.min(cutoff, runItemList.size()); 
			//i<b
			double runCumuGainValue = 0.0;
			for(int i=0; i<base; i++){
				Integer runItemID = runItemList.get(i);
				if(_itemReleMap.containsKey(runItemID)){
					HashMap<String, Integer> releMap = _itemReleMap.get(runItemID);
					if(releMap.containsKey(topic)){
						Integer runGainValue = _itemReleMap.get(runItemID).get(topic);
						runCumuGainValue += runGainValue;
					}					
				}
			}
			//i>=b
			for(int i=base; i<runMaxCur; i++){
				Integer runItemID = runItemList.get(i);
				if(_itemReleMap.containsKey(runItemID)){
					HashMap<String, Integer> releMap = _itemReleMap.get(runItemID);
					if(releMap.containsKey(topic)){
						Integer runGainValue = _itemReleMap.get(runItemID).get(topic);
						runCumuGainValue += (runGainValue*1.0/(Math.log10(i+1)/Math.log10(base)));
					}					
				}				
			}
			
			//ideal 
			ArrayList<Pair<Integer, Integer>> idealItemList = _topicToReleItemMap.get(topic);
			int idealMaxCur = Math.min(cutoff, idealItemList.size());
			//i<b
			double idealCumuGainValue = 0.0;						
			for(int j=0; j<base; j++){
				Integer idealGainValue = idealItemList.get(j).second;
				idealCumuGainValue += idealGainValue;
			}
			//i>=b
			for(int j=base; j<idealMaxCur; j++){
				Integer idealGainValue = idealItemList.get(j).second;
				idealCumuGainValue += (idealGainValue*1.0/(Math.log10(j+1)/Math.log10(base)));
			}
			
			//MSnDCG@k
			nDCGList.add(new Pair<String, Double>(topic, runCumuGainValue/idealCumuGainValue));			
		}
		
		/*
		if(debug){
			for(Pair<String, Double> nDCG: nDCGList){
				System.out.println(nDCG.toString());
			}
		}
		*/
		
		return nDCGList;
	}
	
	private ArrayList<Pair<String, Double>> avgNDCG(EVAL_TYPE evalType, ArrayList<Pair<String, Double>> nDCGList, int cutoff){
		ArrayList<Pair<String, Double>> avgNDCGList = new ArrayList<Pair<String,Double>>();
		
		if(evalType == EVAL_TYPE.CLEAR_NTCIR){
			double pastSum = 0.0;
			int pastCount = 0;
			
			double recencySum = 0.0;
			int recencyCount = 0;
			
			double futureSum = 0.0;
			int futureCount = 0;
			
			double atemporalSum = 0.0;
			int atemporalCount = 0;
			
			double allSum = 0.0;			
			
			for(Pair<String, Double> pair: nDCGList){
				allSum += pair.second;
				
				if(pair.first.endsWith("p")){
					pastSum += pair.second;
					pastCount ++;
				}else if(pair.first.endsWith("r")){
					recencyCount++;
					recencySum += pair.second;
				}else if(pair.first.endsWith("f")){
					futureCount ++;
					futureSum += pair.second;
				}else {
					atemporalCount++;
					atemporalSum += pair.second;
				}
			}			
			
			DecimalFormat df = new DecimalFormat("0.0000");//Double.parseDouble(df.format())
			
			avgNDCGList.add(new Pair<String, Double>("all\t\tnDCG@"+Integer.toString(cutoff), Double.parseDouble(df.format(allSum/nDCGList.size()))));
			avgNDCGList.add(new Pair<String, Double>("past\t\tnDCG@"+Integer.toString(cutoff), Double.parseDouble(df.format(pastSum/pastCount))));
			avgNDCGList.add(new Pair<String, Double>("recency\t\tnDCG@"+Integer.toString(cutoff), Double.parseDouble(df.format(recencySum/recencyCount))));
			avgNDCGList.add(new Pair<String, Double>("future\t\tnDCG@"+Integer.toString(cutoff), Double.parseDouble(df.format(futureSum/futureCount))));
			avgNDCGList.add(new Pair<String, Double>("atemporal\tnDCG@"+Integer.toString(cutoff), Double.parseDouble(df.format(atemporalSum/atemporalCount))));
			
			if(debug){
				for(Pair<String, Double> avgNDCG: avgNDCGList){
					System.out.println(avgNDCG.toString());
				}
			}
		}
		
		return avgNDCGList;
		
	}
	
	/**
	 * Metric: ERR@k
	 * **/
	//satisfying probability
	private double satPro(EVAL_TYPE evalType, int releInt){
		int maxReleLevel = 0;
		if(evalType == EVAL_TYPE.CLEAR_NTCIR){
			maxReleLevel = 2;
		}else {
			System.err.println("Non-determined EvalType!");
			System.exit(0);
		}
		//version: power
		//return (Math.pow(2, releInt)-1)/Math.pow(2, maxReleLevel);
		
		//version: use highest relevance level
		return releInt*1.0/(maxReleLevel+1);
	}
	
	private int getMaxReleLevel(String topic){
		ArrayList<Pair<Integer, Integer>> releItemList = _topicToReleItemMap.get(topic);
		
		int max  = 0;
		for(Pair<Integer, Integer> releItem: releItemList){
			if(releItem.second > max){
				max = releItem.second;
			}
		}
		
		return max;
	}
	
	private ArrayList<Pair<String, Double>> ERR(EVAL_TYPE evalType, int cutoff){
		ArrayList<Pair<String, Double>> errList = new ArrayList<Pair<String,Double>>();
		
		for(String topic: _topicList){
			
			//int maxReleLevel = getMaxReleLevel(topic);

			ArrayList<Integer> sysList = _sysRun.get(topic);
						
			double err = 0.0;
			double disPro = 1.0;
			
			for(int i=0; i<cutoff; i++){
				Integer itemID = sysList.get(i);
				if(_itemReleMap.containsKey(itemID)){
					HashMap<String, Integer> releMap = _itemReleMap.get(itemID);
					if(releMap.containsKey(topic) && releMap.get(topic)>0){
						double satPro = satPro(evalType, releMap.get(topic));
						double tem = 1.0/(i+1)*satPro*disPro;
						
						err += tem;
						disPro *= (1-satPro);
					}
				}
			}
			
			errList.add(new Pair<String, Double>(topic, err));			
		}
		
		if(debug){
			for(Pair<String, Double> err: errList){
				System.out.println(err.toString());
			}
		}
		
		return errList;
	}
	
	private ArrayList<Pair<String, Double>> avgERR(EVAL_TYPE evalType, ArrayList<Pair<String, Double>> errList, int cutoff){
		ArrayList<Pair<String, Double>> avgERRList = new ArrayList<Pair<String,Double>>();
		
		if(evalType == EVAL_TYPE.CLEAR_NTCIR){
			double pastSum = 0.0;
			int pastCount = 0;
			
			double recencySum = 0.0;
			int recencyCount = 0;
			
			double futureSum = 0.0;
			int futureCount = 0;
			
			double atemporalSum = 0.0;
			int atemporalCount = 0;
			
			double allSum = 0.0;			
			
			for(Pair<String, Double> pair: errList){
				allSum += pair.second;
				
				if(pair.first.endsWith("p")){
					pastSum += pair.second;
					pastCount ++;
				}else if(pair.first.endsWith("r")){
					recencyCount++;
					recencySum += pair.second;
				}else if(pair.first.endsWith("f")){
					futureCount ++;
					futureSum += pair.second;
				}else {
					atemporalCount++;
					atemporalSum += pair.second;
				}
			}			
			
			avgERRList.add(new Pair<String, Double>("all\t\tERR@"+Integer.toString(cutoff), allSum/errList.size()));
			avgERRList.add(new Pair<String, Double>("past\t\tERR@"+Integer.toString(cutoff), pastSum/pastCount));
			avgERRList.add(new Pair<String, Double>("recency\t\tERR@"+Integer.toString(cutoff), recencySum/recencyCount));
			avgERRList.add(new Pair<String, Double>("future\t\tERR@"+Integer.toString(cutoff), futureSum/futureCount));
			avgERRList.add(new Pair<String, Double>("atemporal\tERR@"+Integer.toString(cutoff), atemporalSum/atemporalCount));
			
			if(debug){
				for(Pair<String, Double> avgERR: avgERRList){
					System.out.println(avgERR.toString());
				}
			}
		}
		
		return avgERRList;		
	}
	
	/**
	 * Metric: nERR@k
	 * **/
	private ArrayList<Pair<String, Double>> nERR(EVAL_TYPE evalType, int cutoff){
		ArrayList<Pair<String, Double>> nERRList = new ArrayList<Pair<String,Double>>();
		
		for(String topic: _topicList){
			
			int maxReleLevel = getMaxReleLevel(topic);

			ArrayList<Integer> sysList = _sysRun.get(topic);
						
			//for run
			double runERR = 0.0;
			double runDisPro = 1.0;			
			for(int i=0; i<cutoff; i++){
				Integer runItemID = sysList.get(i);
				if(_itemReleMap.containsKey(runItemID)){
					HashMap<String, Integer> runReleMap = _itemReleMap.get(runItemID);
					if(runReleMap.containsKey(topic) && runReleMap.get(topic)>0){
						double runSatPro = satPro(evalType, runReleMap.get(topic));
						double tem = 1.0/(i+1)*runSatPro*runDisPro;
						
						runERR += tem;
						runDisPro *= (1-runSatPro);
					}
				}
			}
			
			//for ideal
			ArrayList<Pair<Integer, Integer>> idealReleMap = _topicToReleItemMap.get(topic);
			
			double idealERR = 0.0;
			double idealDisPro = 1.0;
			for(int i=0; i<cutoff; i++){
				Pair<Integer, Integer> idealItem = idealReleMap.get(i);
				double idealSatPro = satPro(evalType, idealItem.second);
				double tem = 1.0/(i+1)*idealSatPro*idealDisPro;
				
				idealERR += tem;
				idealDisPro *= (1-idealSatPro);
			}
						
			nERRList.add(new Pair<String, Double>(topic, runERR/idealERR));			
		}
		
		if(debug){
			for(Pair<String, Double> nERR: nERRList){
				System.out.println(nERR.toString());
			}
		}
		
		return nERRList;
	}
	
	private ArrayList<Pair<String, Double>> avgNERR(EVAL_TYPE evalType, ArrayList<Pair<String, Double>> nERRList, int cutoff){
		ArrayList<Pair<String, Double>> avgNERRList = new ArrayList<Pair<String,Double>>();
		
		if(evalType == EVAL_TYPE.CLEAR_NTCIR){
			double pastSum = 0.0;
			int pastCount = 0;
			
			double recencySum = 0.0;
			int recencyCount = 0;
			
			double futureSum = 0.0;
			int futureCount = 0;
			
			double atemporalSum = 0.0;
			int atemporalCount = 0;
			
			double allSum = 0.0;			
			
			for(Pair<String, Double> pair: nERRList){
				allSum += pair.second;
				
				if(pair.first.endsWith("p")){
					pastSum += pair.second;
					pastCount ++;
				}else if(pair.first.endsWith("r")){
					recencyCount++;
					recencySum += pair.second;
				}else if(pair.first.endsWith("f")){
					futureCount ++;
					futureSum += pair.second;
				}else {
					atemporalCount++;
					atemporalSum += pair.second;
				}
			}			
			
			avgNERRList.add(new Pair<String, Double>("all\t\tnERR@"+Integer.toString(cutoff), allSum/nERRList.size()));
			avgNERRList.add(new Pair<String, Double>("past\t\tnERR@"+Integer.toString(cutoff), pastSum/pastCount));
			avgNERRList.add(new Pair<String, Double>("recency\t\tnERR@"+Integer.toString(cutoff), recencySum/recencyCount));
			avgNERRList.add(new Pair<String, Double>("future\t\tnERR@"+Integer.toString(cutoff), futureSum/futureCount));
			avgNERRList.add(new Pair<String, Double>("atemporal\tnERR@"+Integer.toString(cutoff), atemporalSum/atemporalCount));
			
			if(debug){
				for(Pair<String, Double> avgNERR: avgNERRList){
					System.out.println(avgNERR.toString());
				}
			}
		}
		
		return avgNERRList;		
	}
	
	/**
	 * Metric: D#-nDCG consisting of I-rec, D-nDCG, D#-nDCG
	 * **/
	private ArrayList<Triple<Double, Double, Double>> DSharpnDCG(int base, int cutoff){
		
		ArrayList<Triple<Double, Double, Double>> tripleList = new ArrayList<Triple<Double,Double,Double>>();
		
		for(String topicid: this._topicList){
			ArrayList<Integer> sysRun = this._sysRun.get(topicid);
			//
			tripleList.add(calMetricTriple(topicid, sysRun, base, cutoff));
		}
		///*
		if(debug){			
			int i=0;
			for(; i<tripleList.size(); i++){
				Triple<Double, Double, Double> triple = tripleList.get(i);
				System.out.println(this._topicList.get(i)+"\t"+triple.toString());
			}		
			System.out.println("size:\t"+tripleList.size());			
		}
		//*/
		
		return tripleList;		
	}
	//mean D#-nDCG
	private Triple<Double, Double, Double> avgDSharpnDCG(ArrayList<Triple<Double, Double, Double>> tripleList){
		double irecSum = 0.0, ndcgSum = 0.0, sharpndcgSum = 0.0;
		for(Triple<Double, Double, Double> triple: tripleList){
			irecSum += triple.getFirst();
			ndcgSum += triple.getSecond();
			sharpndcgSum += triple.getThird();
		}
		
		int size = tripleList.size();
		Triple<Double, Double, Double> avgTriple = new Triple<Double, Double, Double>(irecSum/size, ndcgSum/size, sharpndcgSum/size);
		if(debug){
			System.out.println("Avg D#-nDCG:\t"+avgTriple.toString());
		}
		
		return avgTriple;
	}
		
	//get ranked item list by global gain value
	private ArrayList<Pair<Integer, Double>> getIdealListByGG(String topicid){
		//ranked item list by global gain value
		ArrayList<Pair<Integer, Double>> idealListDecByGG = new ArrayList<Pair<Integer,Double>>();		
		//global ideal list for each topic
		for(Entry<Integer, HashMap<String, HashSet<Pair<Integer, Integer>>>> itemEntry: this._divItemReleMap.entrySet()){
			if(itemEntry.getValue().containsKey(topicid)){
				Integer itemID = itemEntry.getKey();
				
				HashSet<Pair<Integer, Integer>> stSet = itemEntry.getValue().get(topicid);
				//global gain
				double gg = 0.0;
				for(Pair<Integer, Integer> stRelePair: stSet){
					gg += this._topicToSubtopicMap.get(topicid).get(stRelePair.getFirst()-1).getSecond()*stRelePair.getSecond();
				}
				
				idealListDecByGG.add(new Pair<Integer, Double>(itemID, gg));
			}
		}
		//
		Collections.sort(idealListDecByGG, new PairComparatorBySecond_Desc<Integer, Double>());	

		return idealListDecByGG;
	}
	//system's cumulative gain value
	private double getSysCGN(ArrayList<Integer> sysRun, int base, int cutoff, ArrayList<Pair<Integer, Double>> idealListDecByGG){
		int cursor = Math.min(sysRun.size(), cutoff);
		double cgn = 0.0;
		for(int k=0; k<cursor; k++){			
			Integer k_th_itemID = sysRun.get(k);			
			for(Pair<Integer, Double> idealRankedItemByGG: idealListDecByGG){
				if(idealRankedItemByGG.first.equals(k_th_itemID)){
					cgn += idealRankedItemByGG.second/(Math.log10(k+2)/Math.log10(base));
					//
					break;
				}
			}			
		}		
		return cgn;
	}
	//ideal's cumulative gain value
	private double getIdealCGN(int base, int cutoff, ArrayList<Pair<Integer, Double>> idealListDecByGG){
		double cgn = 0.0;
		int cursor = Math.min(cutoff, idealListDecByGG.size());
		for(int i=0; i<cursor; i++){
			cgn += idealListDecByGG.get(i).second/(Math.log10(i+2)/Math.log10(base));
		}
		return cgn;
	}
	//compute I-rec, D-nDCG, D#-nDCG per topic
	public Triple<Double, Double, Double> calMetricTriple(String topicid, ArrayList<Integer> sysRun, int base, int cutoff){		
		//preprocess
		ArrayList<Pair<Integer, Double>> idealListDecByGG = this.getIdealListByGG(topicid);
		Triple<Double, Double, Double> metricTriple = null;
		
		if(null != sysRun){			
			//I-rec			
			HashSet<Integer> stIDSet = new HashSet<Integer>();
			int cursor = Math.min(sysRun.size(), cutoff);
			for(int k=0; k<cursor; k++){
				Integer k_th_itemID = sysRun.get(k);
				//
				if(this._divItemReleMap.containsKey(k_th_itemID)){
					if(this._divItemReleMap.get(k_th_itemID).containsKey(topicid)){
						HashSet<Pair<Integer, Integer>> stSet = this._divItemReleMap.get(k_th_itemID).get(topicid);
						for(Pair<Integer, Integer> relePair: stSet){
							stIDSet.add(relePair.first);
						}
					}
				}								
			}			
			//System.out.println("Included intent Number:\t"+inSubtopicIDSet.size());			
			double Irec = stIDSet.size()*1.0/this._topicToSubtopicMap.get(topicid).size();
			/*
			if (debug) {
				System.out.println("subtopic number:\t"+this._topicToSubtopicMap.get(topicid).size());
				System.out.println("Irec\t"+Irec);
			}
			*/
			//MSnDCG
			double sysCGN = this.getSysCGN(sysRun, base, cutoff, idealListDecByGG);
			double idealCGN = this.getIdealCGN(base, cutoff, idealListDecByGG);
			//System.out.println("System cgv:\t"+sysCGN);
			//System.out.println("Ideal cgv:\t"+idealCGN);
			double msnDCG = sysCGN/idealCGN;
			//D#-nDCG
			double DSharpnDCG = Irec*0.5 + msnDCG*0.5;
			//			
			///*
			DecimalFormat df = new DecimalFormat("0.0000");
			//sequential metric value: I-rec -> D-nDCG -> D#-nDCG  
			metricTriple = new Triple<Double, Double, Double>(Double.parseDouble(df.format(Irec)),
					Double.parseDouble(df.format(msnDCG)), Double.parseDouble(df.format(DSharpnDCG)));
		}else{
			//no result case
			metricTriple = new Triple<Double, Double, Double>(0.0, 0.0, 0.0);							
		}
		
		return metricTriple;
	}
	
	
	//subtopic-level
	private static enum STLevel{FLS, SLS};	
	//specific for NTCIR-11 DR all topic
	private void DSharpnDCG(String sysRunFile, Lang lang, STLevel stLevel, String standardDir, int cutoff){
		if(lang == Lang.Chinese){						
			//unclear			
			String ch_unclear_qrel = null, ch_unclear_ipro = null;
			if(stLevel == STLevel.FLS){
				ch_unclear_qrel = standardDir+"IMine-DR-C-Unclear-Dqrels-FLS";
				ch_unclear_ipro = standardDir+"IMine-DR-C-Unclear-Iprob-FLS";
			}else{
				ch_unclear_qrel = standardDir+"IMine-DR-C-Unclear-Dqrels-SLS";
				ch_unclear_ipro = standardDir+"IMine-DR-C-Unclear-Iprob-SLS";
			}
			
			IREval unclearIREval = new IREval();
			unclearIREval.loadSysRun(EVAL_TYPE.DIV_NTCIR_DR, sysRunFile);
			unclearIREval.loadStandardFile(EVAL_TYPE.DIV_NTCIR_DR, ch_unclear_qrel, ch_unclear_ipro);
			ArrayList<Triple<Double, Double, Double>> tripleList = unclearIREval.DSharpnDCG(2, cutoff);
			
			System.out.println("\n\n"+stLevel.toString()+"\t"+"D#-nDCG for Unclear Topics"+"\tsize: "+unclearIREval._topicList.size());			
			int i=0;
			for(; i<tripleList.size(); i++){
				Triple<Double, Double, Double> triple = tripleList.get(i);
				System.out.println(unclearIREval._topicList.get(i)+"\t"+"I-rec: "+triple.getFirst().toString()+"\tD-nDCG: "+triple.getSecond().toString()+"\tD#-nDCG: "+triple.getThird().toString());
			}		
			//System.out.println("size:\t"+tripleList.size());
			
			//clear
			String ch_clear_qrel = standardDir+"IMine-DR-Qrel-C-Clear";
			IREval clearIREval = new IREval();
			clearIREval.loadSysRun(EVAL_TYPE.CLEAR_NTCIR, sysRunFile);
			clearIREval.loadStandardFile(EVAL_TYPE.CLEAR_NTCIR, ch_clear_qrel, null);
			//nDCG@k
			ArrayList<Pair<String, Double>> nDCGList = clearIREval.nDCG(2, 20);
			
			System.out.println("\nnDCG for Clear Topics"+"\tsize: "+clearIREval._topicList.size());
			for(Pair<String, Double> pair: nDCGList){
				System.out.println(pair.toString());
			}
			
			//over all topics
			double sum = 0.0;
			for(int j=0; j<tripleList.size(); j++){
				Triple<Double, Double, Double> triple = tripleList.get(j);
				sum += triple.getThird();				
			}
			//-1 due to 0033
			System.out.println("\n"+lang.toString()+"-"+stLevel.toString()+"  #avg w.r.t. unclear topics:\t"+sum/(unclearIREval._topicList.size()-1));
			
			for(Pair<String, Double> pair: nDCGList){
				sum += pair.getSecond();
			}
			//-1 due to 0033
			System.out.println("\n"+lang.toString()+"-"+stLevel.toString()+"  #avg w.r.t. all topics:\t"+sum/(unclearIREval._topicList.size()+clearIREval._topicList.size()-1));
			
		}else{
		
			//unclear			
			String ch_unclear_qrel = null, ch_unclear_ipro = null;
			if(stLevel == STLevel.FLS){
				ch_unclear_qrel = standardDir+"IMine-DR-E-Unclear-Dqrels-FLS";
				ch_unclear_ipro = standardDir+"IMine-DR-E-Unclear-Iprob-FLS";
			}else{
				ch_unclear_qrel = standardDir+"IMine-DR-E-Unclear-Dqrels-SLS";
				ch_unclear_ipro = standardDir+"IMine-DR-E-Unclear-Iprob-SLS";
			}
			
			IREval unclearIREval = new IREval();
			unclearIREval.loadSysRun(EVAL_TYPE.DIV_NTCIR_DR, sysRunFile);
			unclearIREval.loadStandardFile(EVAL_TYPE.DIV_NTCIR_DR, ch_unclear_qrel, ch_unclear_ipro);
			ArrayList<Triple<Double, Double, Double>> tripleList = unclearIREval.DSharpnDCG(2, cutoff);
			
			System.out.println("\n\n"+stLevel.toString()+"\t"+"D#-nDCG for Unclear Topics"+"\tsize: "+unclearIREval._topicList.size());			
			int i=0;
			for(; i<tripleList.size(); i++){
				Triple<Double, Double, Double> triple = tripleList.get(i);
				System.out.println(unclearIREval._topicList.get(i)+"\t"+"I-rec: "+triple.getFirst().toString()+"\tD-nDCG: "+triple.getSecond().toString()+"\tD#-nDCG: "+triple.getThird().toString());
			}		
			//System.out.println("size:\t"+tripleList.size());
			
			//clear
			String ch_clear_qrel = standardDir+"IMine-DR-Qrel-E-Clear";
			IREval clearIREval = new IREval();
			clearIREval.loadSysRun(EVAL_TYPE.CLEAR_NTCIR, sysRunFile);
			clearIREval.loadStandardFile(EVAL_TYPE.CLEAR_NTCIR, ch_clear_qrel, null);
			//nDCG@k
			ArrayList<Pair<String, Double>> nDCGList = clearIREval.nDCG(2, 20);
			
			System.out.println("\n\nnDCG for Clear Topics"+"\tsize: "+clearIREval._topicList.size());
			for(Pair<String, Double> pair: nDCGList){
				System.out.println(pair.toString());
			}
			
			//over all topics
			double sum = 0.0;
			for(int j=0; j<tripleList.size(); j++){
				Triple<Double, Double, Double> triple = tripleList.get(j);
				sum += triple.getThird();				
			}
			//-1 because of 0076 due we failed to obtain baseline documents
			System.out.println("\n"+lang.toString()+"-"+stLevel.toString()+"  #avg w.r.t. unclear topics:\t"+sum/(unclearIREval._topicList.size()-1));
			
			for(Pair<String, Double> pair: nDCGList){
				sum += pair.getSecond();
			}
			//-1 becasue of 0100 due we failed to obtain baseline documents, meanwhile, 0084,0085,00925 are not included due to no rele judgements
			System.out.println("\n"+lang.toString()+"-"+stLevel.toString()+"  #avg w.r.t. all topics:\t"+sum/(unclearIREval._topicList.size()+clearIREval._topicList.size()-1));
		}
	}
	
	
	
	//check with ntcireval
	public void check(ArrayList<Pair<String, Double>> apList){
		try {
			Vector<Double> teVector = new Vector<Double>();
			Vector<Double> myVector = new Vector<Double>();
			//
			String teFile = "C:/Users/cicee/Desktop/TIR-Eval/TUTA1-TIR-RUN-1.20140808clean.nev";
			ArrayList<String> lineList = IOText.getLinesAsAList_UTF8(teFile);			
		
			for(String lineString: lineList){
				
				if(lineString.indexOf("ERR=")>=0){
					
					//arrayStrings = lineString.split(" ");
					if(lineString.indexOf("0.") >= 0){
						//System.out.println(lineString.substring(lineString.indexOf("0.")));
						teVector.add(Double.parseDouble(lineString.substring(lineString.indexOf("0."))));
					}else{
						//System.out.println(lineString.substring(lineString.indexOf("1.")));
						teVector.add(Double.parseDouble(lineString.substring(lineString.indexOf("1."))));
					}			
				}
			}		
						
			DecimalFormat df = new DecimalFormat("0.0000");
			for(Pair<String, Double> ap: apList){
				myVector.add(Double.parseDouble(df.format(ap.second)));
			}
			//
			System.out.println("te size:\t"+teVector.size());
			System.out.println("my size:\t"+myVector.size());
			//
			int size = Math.min(teVector.size(), myVector.size());
			for(int i=0; i<size; i++){
				if(!teVector.get(i).equals(myVector.get(i))){
					System.out.println(teVector.get(i));
					System.out.println(myVector.get(i));
					System.out.println("un:\t"+(i+1)+"\t"+apList.get(i).toString());
					//break;
				}
			}			
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}
	
	//////////////////////////
	//WilcoxonSignedRankTest
	//////////////////////////
	private static double [] getDArray(ArrayList<Pair<String, Double>> mList){
		double [] dArray = new double[mList.size()];
		
		for(int i=0; i<mList.size(); i++){
			dArray[i] = mList.get(i).second;
		}
		
		return dArray;
	}
	
	public static void wilcoxonSignedRankTest(String des, ArrayList<Pair<String, Double>> mList_1, ArrayList<Pair<String, Double>> mList_2){
		org.apache.commons.math3.stat.inference.WilcoxonSignedRankTest wsrTest = new org.apache.commons.math3.stat.inference.WilcoxonSignedRankTest();
		
		//seperate
		//2
		ArrayList<Pair<String, Double>> pList_2 = new ArrayList<Pair<String,Double>>();
		ArrayList<Pair<String, Double>> rList_2 = new ArrayList<Pair<String,Double>>();
		ArrayList<Pair<String, Double>> fList_2 = new ArrayList<Pair<String,Double>>();
		ArrayList<Pair<String, Double>> aList_2 = new ArrayList<Pair<String,Double>>();
		
		for(Pair<String, Double> pair_2: mList_2){
			if(pair_2.first.endsWith("p")){
				pList_2.add(pair_2);
			}else if(pair_2.first.endsWith("r")){
				rList_2.add(pair_2);
			}else if(pair_2.first.endsWith("f")){
				fList_2.add(pair_2);
			}else {
				aList_2.add(pair_2);
			}
		}
		
		//1
		ArrayList<Pair<String, Double>> pList_1 = new ArrayList<Pair<String,Double>>();
		ArrayList<Pair<String, Double>> rList_1 = new ArrayList<Pair<String,Double>>();
		ArrayList<Pair<String, Double>> fList_1 = new ArrayList<Pair<String,Double>>();
		ArrayList<Pair<String, Double>> aList_1 = new ArrayList<Pair<String,Double>>();
		
		for(Pair<String, Double> pair_1: mList_1){
			if(pair_1.first.endsWith("p")){
				pList_1.add(pair_1);
			}else if(pair_1.first.endsWith("r")){
				rList_1.add(pair_1);
			}else if(pair_1.first.endsWith("f")){
				fList_1.add(pair_1);
			}else {
				aList_1.add(pair_1);
			}
		}		
		
		//sig-test
		System.out.println(des+" Sig-test for past:\t"+wsrTest.wilcoxonSignedRankTest(getDArray(pList_1), getDArray(pList_2), false));
		System.out.println();	
		System.out.println(des+" Sig-test for recency:\t"+wsrTest.wilcoxonSignedRankTest(getDArray(rList_1), getDArray(rList_2), false));
		System.out.println();	
		System.out.println(des+" Sig-test for future:\t"+wsrTest.wilcoxonSignedRankTest(getDArray(fList_1), getDArray(fList_2), false));
		System.out.println();	
		System.out.println(des+" Sig-test for atemporal:\t"+wsrTest.wilcoxonSignedRankTest(getDArray(aList_1), getDArray(aList_2), false));		
		System.out.println();		
		System.out.println(des+" Sig-test for all:\t"+wsrTest.wilcoxonSignedRankTest(getDArray(mList_1), getDArray(mList_2), false));		
		System.out.println();
	}
	
	public static void pairedSigTest(){
		//old 185
		//String _standardReleFile = "C:/Users/cicee/Desktop/TIR-Eval/tir_formalrun_20140808clean.qrels";
		//new 200
		String standardReleFile = "H:/v-haiyu/TaskPreparation/Ntcir11-Temporalia/Eval-Tem-Results/TIR/tir_formalrun_2014080829.qrels";
		for(int refID=1; refID<=4; refID++){
			if(refID < 4){
				String refRun = "H:/v-haiyu/CodeBench/Pool_Output/Output_Temporalia/FinalRuns/TUTA1-TIR/TUTA1-TIR-RUN-"+Integer.toString(refID);
				IREval refIREval = new IREval();
				refIREval.loadSysRun(EVAL_TYPE.CLEAR_NTCIR, refRun);
				refIREval.loadStandardFile(EVAL_TYPE.CLEAR_NTCIR, standardReleFile, null);
				ArrayList<Pair<String, Double>> refPList = refIREval.P(20);
				ArrayList<Pair<String, Double>> refNDCGList = refIREval.nDCG(2, 20);
				
				for(int followID=refID+1; followID<=4; followID++){
					String followRun = "H:/v-haiyu/CodeBench/Pool_Output/Output_Temporalia/FinalRuns/TUTA1-TIR/TUTA1-TIR-RUN-"+Integer.toString(followID);				
					IREval followIREval = new IREval();
					followIREval.loadSysRun(EVAL_TYPE.CLEAR_NTCIR, followRun);
					followIREval.loadStandardFile(EVAL_TYPE.CLEAR_NTCIR, standardReleFile, null);
					ArrayList<Pair<String, Double>> followPList = followIREval.P(20);
					ArrayList<Pair<String, Double>> followNDCGList = followIREval.nDCG(2, 20);
					//->
					IREval.wilcoxonSignedRankTest("P@20\t"+followID+"->"+refID, refPList, followPList);
					IREval.wilcoxonSignedRankTest("nDCG@20\t"+followID+"->"+refID, refNDCGList, followNDCGList);
					System.out.println();
				}
			}				
		}		
	}
	
	////////////////////////////
	//eval via formal run data
	////////////////////////////
	private static HashSet<String> loadRerankSubtopic(){
        
	    HashSet<String> subtopicSet = new HashSet<String>();
	    
	    String file_1 = "H:/v-haiyu/CodeBench/Pool_DataSet/DataSet_Temporalia/Temporalia/FormalRun/RandomSplit/p-3";
	    String file_2 = "H:/v-haiyu/CodeBench/Pool_DataSet/DataSet_Temporalia/Temporalia/FormalRun/RandomSplit/r-3";
	    String file_3 = "H:/v-haiyu/CodeBench/Pool_DataSet/DataSet_Temporalia/Temporalia/FormalRun/RandomSplit/f-3";
	    String file_4 = "H:/v-haiyu/CodeBench/Pool_DataSet/DataSet_Temporalia/Temporalia/FormalRun/RandomSplit/a-3";
	    
	    ArrayList<String> lineList_1 = IOText.getLinesAsAList_UTF8(file_1);
	    for(String line: lineList_1){
	      subtopicSet.add(line);
	    }
	    
	    ArrayList<String> lineList_2 = IOText.getLinesAsAList_UTF8(file_2);
	    for(String line: lineList_2){
	      subtopicSet.add(line);
	    }
	    
	    ArrayList<String> lineList_3 = IOText.getLinesAsAList_UTF8(file_3);
	    for(String line: lineList_3){
	      subtopicSet.add(line);
	    }
	    
	    ArrayList<String> lineList_4 = IOText.getLinesAsAList_UTF8(file_4);
	    for(String line: lineList_4){
	      subtopicSet.add(line);
	    }
	    
	    if(debug){
	      System.out.println("train subtopic size:\t"+subtopicSet.size());
	    }
	    
	    return subtopicSet;    
	  }
	//
    private void loadStandardReleFile_formal(EVAL_TYPE evalType, String standardReleFile){
	
	HashSet<String> subtopicSet = loadRerankSubtopic();
		
		try {
			ArrayList<String> lineList = IOText.getLinesAsAList_UTF8(standardReleFile);
			
			HashSet<String> topicSet = new HashSet<String>();
			
			if(evalType == EVAL_TYPE.CLEAR_NTCIR){
				
				this._topicToReleItemMap = new HashMap<String, ArrayList<Pair<Integer,Integer>>>();
				this._itemReleMap = new HashMap<Integer, HashMap<String,Integer>>();
				
				String []array;
				for(String line: lineList){
					//array[0]:subtopic-id / array[1]:item-string / array[3]:relevance-level
					array = line.split("\\s");
					String subtopicid = array[0];
					
					if(!subtopicSet.contains(subtopicid)){
						continue;
					}
					
					//String item = array[1];
					Integer itemID = getItemIndex(array[1]);
					Integer releLevel = Integer.parseInt(array[2].substring(1));
					
					//for _topicList
					if(!topicSet.contains(subtopicid)){
						topicSet.add(subtopicid);
						
						_topicList.add(subtopicid);
					}
					
					//for _topicToReleItemMap
					if(_topicToReleItemMap.containsKey(subtopicid)){
						ArrayList<Pair<Integer,Integer>> itemPairList = _topicToReleItemMap.get(subtopicid);
						itemPairList.add(new Pair<Integer, Integer>(itemID, releLevel));
					}else{
						ArrayList<Pair<Integer,Integer>> itemPairList = new ArrayList<Pair<Integer,Integer>>();
						itemPairList.add(new Pair<Integer, Integer>(itemID, releLevel));
						_topicToReleItemMap.put(subtopicid, itemPairList);
					}
					
					//for _itemReleMap
					if(_itemReleMap.containsKey(itemID)){
						_itemReleMap.get(itemID).put(subtopicid, releLevel);
					}else{
						HashMap<String, Integer> releMap = new HashMap<String, Integer>();
						releMap.put(subtopicid, releLevel);
						
						_itemReleMap.put(itemID, releMap);
					}
				}	
				
				//sort item list
				for(Entry<String, ArrayList<Pair<Integer,Integer>>> entry: _topicToReleItemMap.entrySet()){
					ArrayList<Pair<Integer,Integer>> itemPairList = entry.getValue();
					Collections.sort(itemPairList, new PairComparatorBySecond_Desc<Integer, Integer>());
				}				
			}						
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		
	}
	//
    private void check(){
    	ArrayList<String> sysTopicList = new ArrayList<String>();
    	sysTopicList.addAll(_sysRun.keySet());
    	
    	ArrayList<String> standardTopicList = new ArrayList<String>();
    	standardTopicList.addAll(_topicList);
    	
    	ArrayList<String> releTopicList = new ArrayList<String>();
    	releTopicList.addAll(_topicToReleItemMap.keySet());
    	
    	System.out.println(sysTopicList.size());
    	System.out.println(standardTopicList.size());
    	System.out.println(releTopicList.size());
    	
    	if(sysTopicList.containsAll(standardTopicList) && standardTopicList.containsAll(sysTopicList)){
    		System.out.println("true");
    	}
    	
    	if(sysTopicList.containsAll(releTopicList) && releTopicList.containsAll(sysTopicList)){
    		System.out.println("true");
    	}
    }
	
	/////
	
	public static void main(String []args){
		//1 precision test
		/*
		String sysRunFile = "H:/v-haiyu/CodeBench/Pool_Output/Output_Temporalia/FinalRuns/TUTA1-TIR/TUTA1-TIR-RUN-3";
		//String sysRunFile = "H:/v-haiyu/CodeBench/Pool_Output/Output_Temporalia/FinalRuns/TUTA1-TIR/TUTA1-TIR-RUN-4";
		//old-TIR-185
		//String _standardReleFile = "H:/v-haiyu/TaskPreparation/Ntcir11-Temporalia/Eval-Tem-Results/TIR-185/tir_formalrun_20140808clean.qrels";
		//new 200
		String standardReleFile = "H:/v-haiyu/TaskPreparation/Ntcir11-Temporalia/Eval-Tem-Results/TIR/tir_formalrun_2014080829.qrels";
		
		IREval irEval = new IREval();
		irEval.loadSysRun(EVAL_TYPE.TIR_NTCIR11, sysRunFile);
		irEval.loadStandardFile(EVAL_TYPE.TIR_NTCIR11, standardReleFile, null);
		
		//P@k
		ArrayList<Pair<String, Double>> precisionList = irEval.P(20);
		irEval.avgP(EVAL_TYPE.TIR_NTCIR11, precisionList, 20);
		
		//MSnDCG@k
		//ArrayList<Pair<String, Double>> MSnDCGList = irEval.MSnDCG(20);
		//irEval.avgMSnDCG(EVAL_TYPE.TIR_NTCIR11, MSnDCGList, 20);
		
		//nDCG@k base discussion, which is strictly implemented as the original definition
		//ArrayList<Pair<String, Double>> nDCGList = irEval.nDCG(2, 20);
		//irEval.avgNDCG(EVAL_TYPE.TIR_NTCIR11, nDCGList, 20);
		
		//AP & MAP ?! to be verified by the 3rd software
		//ArrayList<Pair<String, Double>> apList = irEval.AP(20);
		//irEval.check(apList);
		//irEval.MAP(EVAL_TYPE.TIR_NTCIR11, apList, 20);
		
		//ERR@k ?！ to be verified by the 3rd software
		//ArrayList<Pair<String, Double>> errList = irEval.ERR(EVAL_TYPE.TIR_NTCIR11, 20);
		//irEval.check(errList);
		//irEval.avgERR(EVAL_TYPE.TIR_NTCIR11, errList, 20);
		
		//nERR@k
		//ArrayList<Pair<String, Double>> nERRList = irEval.nERR(20);
		//irEval.avgERR(EVAL_TYPE.TIR_NTCIR11, nERRList, 20);
		*/
		
		////////////
		//sig-test
		///////////
		
		//IREval.pairedSigTest();		
		
		///////////////
		//via formal run
		///////////////
		/*
		String sysRunFile = "H:/v-haiyu/CodeBench/Pool_Output/Output_Temporalia/RerankViaFormalRun/TUTA1-TIR-RUNVIA-2.txt";
		String standardReleFile = "C:/Users/cicee/Desktop/TIR-Eval/tir_formalrun_20140808clean.qrels";
				
		IREval irEval = new IREval();
		irEval.loadSysRun(EVAL_TYPE.TIR_NTCIR11, sysRunFile);
		irEval.loadStandardReleFile_formal(EVAL_TYPE.TIR_NTCIR11, standardReleFile);
		*/
		//irEval.check();
		
		//nDCG@k
		/*
		ArrayList<Pair<String, Double>> nDCGList = irEval.nDCG(2, 20);
		irEval.avgNDCG(EVAL_TYPE.TIR_NTCIR11, nDCGList, 20);
		*/
		//P@k
		/*
		ArrayList<Pair<String, Double>> precisionList = irEval.P(20);
		irEval.avgP(EVAL_TYPE.TIR_NTCIR11, precisionList, 20);
		*/
		
		//check D#-nDCG -> ok
		/*
		String sysRunFile = "H:/v-haiyu/CodeBench/Pool_Output/Output_DiversifiedRanking/ntcir-10/SM/TUTA1-S-C-1A";
		String ch_sm_qrel = "H:/v-haiyu/CodeBench/Pool_DataSet/DataSet_DiversifiedRanking/ntcir/ntcir-10/SM/INTENT-2SMC.rev.Dqrels";
		String ch_sm_ipro = "H:/v-haiyu/CodeBench/Pool_DataSet/DataSet_DiversifiedRanking/ntcir/ntcir-10/SM/INTENT-2SMC.Iprob";
		
		IREval irEval = new IREval();
		irEval.loadSysRun(EVAL_TYPE.DIV_NTCIR_SM, sysRunFile);
		irEval.loadStandardFile(EVAL_TYPE.DIV_NTCIR_SM, ch_sm_qrel, ch_sm_ipro);
		ArrayList<Triple<Double, Double, Double>> tripleList = irEval.DSharpnDCG(10, 10);
		irEval.avgDSharpnDCG(tripleList);
		*/
		
		////DR subtask
		/*
		String dir = "H:/CurrentResearch/Ntcir11-IMine/Eval-IMine/0913/CheckEval/";
		//Chinese unclear
		String sysRunFile = "H:/v-haiyu/CodeBench/Pool_Output/Output_DiversifiedRanking/DRResult/SubmittedVersion/TUTA1-D-C-1B.txt";
		String ch_unclear_qrel = dir+"IMine-DR-C-Unclear-Dqrels-SLS";
		String ch_unclear_ipro = dir+"IMine-DR-C-Unclear-Iprob-SLS";
		IREval irEval = new IREval();
		irEval.loadSysRun(EVAL_TYPE.DIV_NTCIR_DR, sysRunFile);
		irEval.loadStandardFile(EVAL_TYPE.DIV_NTCIR_DR, ch_unclear_qrel, ch_unclear_ipro);
		ArrayList<Triple<Double, Double, Double>> tripleList = irEval.DSharpnDCG(10, 20);
		irEval.avgDSharpnDCG(tripleList);
		*/
		
		////DR subtask of NTCIR-11
		//standard files
		///*
		String dir = "H:/CurrentResearch/Ntcir11-IMine/Eval-IMine/0913/CheckEval/";
		IREval irEval = new IREval();	
		//*/
		
		//submitted run
		//Chinese
		/*		
		String sysRunFile = "H:/v-haiyu/CodeBench/Pool_Output/Output_DiversifiedRanking/DRResult/SubmittedVersion/TUTA1-D-C-1B.txt";
		irEval.DSharpnDCG(sysRunFile, Lang.Chinese, STLevel.FLS, dir, 20);
		*/
		//50
		//String sysRunFile = "H:/v-haiyu/CodeBench/Pool_Output/Output_DiversifiedRanking/DRResult/SubmittedVersion/TUTA1-D-C-1B.txt";
		//irEval.DSharpnDCG(sysRunFile, Lang.Chinese, STLevel.SLS, dir, 50);
		
		//baseline run
		/*		
		String sysRunFile = "H:/v-haiyu/CodeBench/Pool_DataSet/DataSet_DiversifiedRanking/ntcir/ntcir-11/DR/CH/DR_Baseline/BASELINE.txt";
		irEval.DSharpnDCG(sysRunFile, Lang.Chinese, STLevel.FLS, dir, 20);
		*/
		//50
		//String sysRunFile = "H:/v-haiyu/CodeBench/Pool_DataSet/DataSet_DiversifiedRanking/ntcir/ntcir-11/DR/CH/DR_Baseline/BASELINE.txt";
		//irEval.DSharpnDCG(sysRunFile, Lang.Chinese, STLevel.SLS, dir, 50);
		
		//English
		
		//TUTA1-D-E-1B.txt & FLS & 20
		///*
		String sysRunFile = "H:/v-haiyu/CodeBench/Pool_Output/Output_DiversifiedRanking/DRResult/SubmittedVersion/TUTA1-D-E-1B.txt";
		irEval.DSharpnDCG(sysRunFile, Lang.English, STLevel.FLS, dir, 20);
		//*/
		//TUTA1-D-E-1B.txt & SLS
		/*
		String sysRunFile = "H:/v-haiyu/CodeBench/Pool_Output/Output_DiversifiedRanking/DRResult/SubmittedVersion/TUTA1-D-E-1B.txt";
		irEval.DSharpnDCG(sysRunFile, Lang.English, STLevel.SLS, dir, 20);
		*/
		//SLS & 50
		//String sysRunFile = "H:/v-haiyu/CodeBench/Pool_Output/Output_DiversifiedRanking/DRResult/SubmittedVersion/TUTA1-D-E-1B.txt";
		//irEval.DSharpnDCG(sysRunFile, Lang.English, STLevel.SLS, dir, 50);
		
		//TUTA1-D-E-2B.txt & FLS
		/*
		String sysRunFile = "H:/v-haiyu/CodeBench/Pool_Output/Output_DiversifiedRanking/DRResult/SubmittedVersion/TUTA1-D-E-2B.txt";
		irEval.DSharpnDCG(sysRunFile, Lang.English, STLevel.FLS, dir, 20);
		*/
		//TUTA1-D-E-1B.txt & SLS
		/*
		String sysRunFile = "H:/v-haiyu/CodeBench/Pool_Output/Output_DiversifiedRanking/DRResult/SubmittedVersion/TUTA1-D-E-2B.txt";
		irEval.DSharpnDCG(sysRunFile, Lang.English, STLevel.SLS, dir, 20);
		*/
		//SLS & 50
		//String sysRunFile = "H:/v-haiyu/CodeBench/Pool_Output/Output_DiversifiedRanking/DRResult/SubmittedVersion/TUTA1-D-E-2B.txt";
		//irEval.DSharpnDCG(sysRunFile, Lang.English, STLevel.SLS, dir, 50);
		
		//indri baseline & FLS
		/*
		String sysRunFile = "H:/v-haiyu/CodeBench/Pool_DataSet/DataSet_DiversifiedRanking/ntcir/ntcir-11/DR/EN/EntireBaseline.txt";
		irEval.DSharpnDCG(sysRunFile, Lang.English, STLevel.FLS, dir, 20);
		*/
		//indri baseline & SLS
		/*
		String sysRunFile = "H:/v-haiyu/CodeBench/Pool_DataSet/DataSet_DiversifiedRanking/ntcir/ntcir-11/DR/EN/EntireBaseline.txt";
		irEval.DSharpnDCG(sysRunFile, Lang.English, STLevel.SLS, dir, 20);
		*/
		//SLS & 50
		/*
		String sysRunFile = "H:/v-haiyu/CodeBench/Pool_DataSet/DataSet_DiversifiedRanking/ntcir/ntcir-11/DR/EN/EntireBaseline.txt";
		irEval.DSharpnDCG(sysRunFile, Lang.English, STLevel.SLS, dir, 50);
		*/
		
		
		/**
		 * basic statistics w.r.t. qrel of Temporalia-1
		 * **/
		/*
		String standardReleFile = "C:/T/WorkBench/Bench_Dataset/DataSet_Temporalia/Temporalia/FormalRun/tir_formalrun_20140808clean.qrels";

		IREval irEval = new IREval();
		irEval.loadStandardFile(EVAL_TYPE.TDIV_NTCIR, standardReleFile, "");
		irEval.staOfTemporalia_1();
		*/
	}
	

}
