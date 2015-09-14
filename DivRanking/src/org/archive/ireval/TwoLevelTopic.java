package org.archive.ireval;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import org.archive.util.tuple.Pair;

/**
 * A wrapper of a two-level hierarchy of subtopics
 * **/

public class TwoLevelTopic {
	
	String _id;
	String _topic;
	
	//element: fls and its probability
	ArrayList<Pair<String, Double>> _flsList;
	//element: a set of example subtopic string, i-th element corresponds to the i-th element of _flsList
	ArrayList<ArrayList<String>> _flsExampleSetList;
	//key: flsStr, value: flsID
	HashMap<String, HashSet<Integer>> _flsStrMap;
	
	//element: second-level subtopic set, i-the element corresponds to the i-th element of _flsList
	ArrayList<ArrayList<Pair<String, Double>>> _slsSetList;
	//key: pair of flsID & slsID, value: set of example subtopic strings	
	HashMap<Pair<Integer, Integer>,	ArrayList<String>> _slsExampleSetMap;
	//key: slsStr, value: pair of flsID & slsID
	HashMap<String, HashSet<Pair<Integer, Integer>>> _slsExampleMap;
	
	//mapping from sls's content to fls's content 
	//key: sls's content, value: pair of flsID, slsID(itself)
	HashMap<String, Pair<Integer, Integer>> _slsContentMap;
	//
	TwoLevelTopic(String id, String topic){
		this._id = id;
		this._topic = topic;
		
		this._flsList = new ArrayList<Pair<String,Double>>();
		this._flsExampleSetList = new ArrayList<ArrayList<String>>();
		this._flsStrMap = new HashMap<String, HashSet<Integer>>();
		
		this._slsSetList = new ArrayList<ArrayList<Pair<String,Double>>>();
		this._slsExampleSetMap = new HashMap<Pair<Integer,Integer>, ArrayList<String>>();
		this._slsExampleMap = new HashMap<String, HashSet<Pair<Integer, Integer>>>();
	}
	
	public void setFlsList(ArrayList<Pair<String, Double>> flsList){
		this._flsList = flsList;
	}
	
	public void setFlsExampleSetList(ArrayList<ArrayList<String>> flsExampleSetList){
		this._flsExampleSetList = flsExampleSetList;
	}
	
	public void setFlsStrMap(HashMap<String, HashSet<Integer>> flsStrMap){
		this._flsStrMap = flsStrMap;
	}
	
	public void setSlsSetList(ArrayList<ArrayList<Pair<String, Double>>> slsSetList){
		this._slsSetList = slsSetList;
	}
	
	public void setSlsExampleSetMap(HashMap<Pair<Integer, Integer>,	ArrayList<String>> slsExampleSetMap){
		this._slsExampleSetMap = slsExampleSetMap;
	}
	
	public void setSlsStrMap(HashMap<String, HashSet<Pair<Integer, Integer>>> slsExampleMap){
		this._slsExampleMap = slsExampleMap;
	}
	
	public void getSlsContentMap(){
		_slsContentMap = new HashMap<String, Pair<Integer,Integer>>();
		
		int flsID=1;
		for(; flsID<=_flsList.size(); flsID++){
			ArrayList<Pair<String, Double>> slsSet = _slsSetList.get(flsID-1);
			
			int slsID = 1;
			for(; slsID<=slsSet.size(); slsID++){
				_slsContentMap.put(slsSet.get(slsID-1).getFirst(), new Pair<Integer, Integer>(flsID, slsID));
			}
		}
	}
	
	/**
	 * get first-level subtopic probability
	 * 
	 * @param equal true means that the fls are equally treated, 1/(number of fls) false means using the official given pro
	 * **/
	public double getFlsPro(int flsID, boolean equal){
		if(equal){
			return (1.0/this._flsList.size());
		}else{
			return this._flsList.get(flsID-1).getSecond();
		}
	}
	/**
	 * get second-level subtopic probability
	 * **/
	public double getSlsPro_notused(int flsID, int slsID, boolean equal){
		ArrayList<Pair<String, Double>> slsList_flsi = this._slsSetList.get(flsID-1);
		
		if(equal){
			
			return (1.0/slsList_flsi.size());
			
		}else{
			
			return slsList_flsi.get(slsID-1).getSecond();
			
		}		
	}
	/**
	 * Further version:
	 * we consider whether the set of sls are used to perform traditional diversity evaluation
	 * hierarchicalDiv: we use the relative popularity based on the weight from IMine annotation
	 * **/
	public double getSlsPro(int flsID, int slsID, boolean equal, boolean hierarchicalDiv){
		ArrayList<Pair<String, Double>> slsList_flsi = this._slsSetList.get(flsID-1);
		
		if(hierarchicalDiv){
			if(equal){			
				return (1.0/slsList_flsi.size());			
			}else{		
				double sum = 0.0;
				for(Pair<String, Double> element: slsList_flsi){
					sum += element.second;
				}
				double pop = slsList_flsi.get(slsID-1).getSecond()/sum;
				
				/*
				if(Double.isNaN(pop)){
					System.err.println("pop");
					System.out.println(_id+"\t"+_topic);
					System.out.println("fls:\t"+flsID);
				}
				*/
				return pop;			
			}
		}
		
		if(equal){			
			return (1.0/slsList_flsi.size());			
		}else{			
			return slsList_flsi.get(slsID-1).getSecond();			
		}		
	}
	
	/**
	 * get fls probability based on number of descendant topic units (i.e., second-level subtopics), which are equally treated 
	 * **/
	public double getFlsPro(int flsID){
		int sumOfTopicUnits = 0;
		for(ArrayList<Pair<String, Double>> slsSet: _slsSetList){
			sumOfTopicUnits += slsSet.size();
		}
		
		return (_slsSetList.get(flsID-1)).size()*1.0/sumOfTopicUnits;
	}
	
	/**
	 * get relevance probability for a specific fls given the set of sls of a document
	 * this function can also be used for computing the marginal utility for a document w.r.t. a specific subtopic when
	 * slsSetCoveredByDoc is the set difference, i.e., $(d_k,t_h^i)\$(L^(k-1),t_h^i)
	 * 
	 * @param flsID A specific first-level subtopic id
	 * @param slsSetCoveredByDoc the set of second-level subtopics covered by a document
	 * **/
	public double getRelePro(int flsID, HashSet<String> slsSetCoveredByDoc){
		ArrayList<Pair<String, Double>> slsList = _slsSetList.get(flsID-1);
		//used as the denominator |#(t_h^i)|
		int numberOfTopicUnits = slsList.size();
		
		/*
		if(0 == numberOfTopicUnits){
			System.err.println(_id+"\t"+flsID);
		}
		*/
		
		HashSet<String> slsSet = new HashSet<String>();
		for(Pair<String, Double> sls: slsList){
			slsSet.add(sls.first);
		}		
		slsSet.retainAll(slsSetCoveredByDoc);
		
		return (slsSet.size()*1.0)/numberOfTopicUnits;		
	}
	
	public String getTopicID(){
		return this._id;
	}
	
	public String toString(){
		StringBuffer buffer = new StringBuffer();
		buffer.append(_id+"\t"+_topic+"\n");
		int flsID = 1;
		for(; flsID <= _flsList.size(); flsID++){
			Pair<String, Double> fls = _flsList.get(flsID-1);
			buffer.append("  "+flsID+"-"+fls.getFirst()+"\t"+fls.getSecond()+"\n");
			
			ArrayList<String> flsExampleList = _flsExampleSetList.get(flsID-1);
			buffer.append("  fls-example: "+flsExampleList.size()+" : "+flsExampleList.toString()+"\n");
			
			ArrayList<Pair<String, Double>> slsSet = _slsSetList.get(flsID-1);
			int slsID = 1;
			for(; slsID<= slsSet.size(); slsID++){
				Pair<String, Double> sls = slsSet.get(slsID-1);
				buffer.append("\t"+slsID+"-"+sls.getFirst()+"\t"+sls.getSecond()+"\n");
				
				Pair<Integer, Integer> pair = new Pair<Integer, Integer>(flsID, slsID);
				int sCount = 1;
				for(Entry<String, HashSet<Pair<Integer, Integer>>> slsEntry: _slsExampleMap.entrySet()){
					HashSet<Pair<Integer, Integer>> slsESet = slsEntry.getValue();
					if(slsESet.contains(pair)){
						buffer.append("\tslsExample-"+(sCount++)+": "+slsEntry.getKey()+"\n");
					}
				}				
			}			
		}
		
		return buffer.toString();
	}
}
