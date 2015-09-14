package org.archive.nlp.lcs;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

//appearing footprint of one element in a query
class StrAlphabet{
	//source query
	int sourceStrID;
	//inner index of the source query
	//a case for multiple times in one query
	int innerIndex;
	int vocabID;
	int head;
	int disToEnd;
	//
	StrAlphabet(int sourceStrID, int innerIndex, int vocabID, int head, int disToEnd){
		this.sourceStrID = sourceStrID;
		this.innerIndex = innerIndex;
		this.vocabID = vocabID;
		this.head = head;
		this.disToEnd = disToEnd;
	}	
}


public class UnitNode {
	//notice the case that multiple time in the same query for one alphabet
	public int appearCount = 0;
	//record for each exact existence of alphabet in source query
	public Hashtable<String, StrAlphabet> distributionTable;
	//existence in source query appearing footprint
	public Hashtable<Integer, Boolean> sourceStrIDDTable;
	//source-query id, index of query, corresponding vocabulary id
	public UnitNode(int sourceStrID, int innerIndex, int vocabID){
		this.distributionTable = new Hashtable<String, StrAlphabet>();
		this.sourceStrIDDTable = new Hashtable<Integer, Boolean>();
		//
		this.distributionTable.put(Integer.toString(sourceStrID)+Integer.toString(innerIndex), 
				new StrAlphabet(sourceStrID, innerIndex, vocabID, 0, 0));
		this.appearCount++;
		//
		if(!this.sourceStrIDDTable.containsKey(sourceStrID)){
			this.sourceStrIDDTable.put(sourceStrID, true);
		}
	}
	//
	public void addStrAlphabet(StrAlphabet strAlphabet){
		this.distributionTable.put(Integer.toString(strAlphabet.sourceStrID)+Integer.toString(strAlphabet.innerIndex), 
				strAlphabet);
		this.appearCount++;
		//
		if(!this.sourceStrIDDTable.containsKey(strAlphabet.sourceStrID)){
			this.sourceStrIDDTable.put(strAlphabet.sourceStrID, true);
		}
	}
	//
	public boolean commonInAll(int queryNum){
		return this.sourceStrIDDTable.size()==queryNum;
	}
	//
	public Vector<Integer> kSourceQuery(int kSource, Vector<Integer> kSuperSet){
		if(this.sourceStrIDDTable.size() >= kSource){
			Vector<Integer> commonKSuperSet = new Vector<Integer>();
			//
			Enumeration<Integer> keyEnum = this.sourceStrIDDTable.keys();			
			Integer key;
			while(keyEnum.hasMoreElements()){
				key = keyEnum.nextElement();
				if(kSuperSet.contains(key)){
					commonKSuperSet.add(key);
				}				
			}
			//
			if(commonKSuperSet.size() >= kSource){
				return commonKSuperSet;
			}else{
				return null;
			}			
		}else{
			return null;
		}
	}
	//return a super set of possible k-source query
	public Vector<Integer> kSourceQuery(int kSource){
		if(this.sourceStrIDDTable.size() >= kSource){
			Vector<Integer> commonKSuperSet = new Vector<Integer>();
			//
			Enumeration<Integer> keyEnum = this.sourceStrIDDTable.keys();
			//
			while(keyEnum.hasMoreElements()){
				commonKSuperSet.add(keyEnum.nextElement());
			}
			//
			return commonKSuperSet;
		}else{
			return null;
		}		
	}
	//
	public boolean commonInFormer2(){
		if(this.sourceStrIDDTable.containsKey(0) && this.sourceStrIDDTable.containsKey(1)){
			return true;
		}else{
			return false;
		}
	}
	//
	public boolean commonInLatter2(){
		if(this.sourceStrIDDTable.containsKey(1) && this.sourceStrIDDTable.containsKey(2)){
			return true;
		}else{
			return false;
		}
	}
	//
	public boolean commonInGivenTwo(int formerID, int latterID){
		if(this.sourceStrIDDTable.containsKey(formerID) && this.sourceStrIDDTable.containsKey(latterID)){
			return true;
		}else{
			return false;
		}
	}
}
