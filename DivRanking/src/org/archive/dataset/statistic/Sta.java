package org.archive.dataset.statistic;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.archive.dataset.trec.TRECDivLoader;
import org.archive.dataset.trec.TRECDivLoader.DivVersion;
import org.archive.dataset.trec.query.TRECQueryAspects;

public class Sta {
	
	/**
	 * Statistics w.r.t. TREC
	 * **/
	//number for relevant documents per subtopic
	private static void avgDocNumPerSubtopic(DivVersion divVersion, boolean checkAvailable){
		List<String> qList = TRECDivLoader.getDivEvalQueryIDList(true, divVersion);
		Map<String,TRECQueryAspects> trecDivQueryAspects = TRECDivLoader.loadTrecDivQueryAspects(true, divVersion);
		
		int count = 1;
		for(String q: qList){
			TRECQueryAspects trecQueryAspects = trecDivQueryAspects.get(q);
			trecQueryAspects.iniSubtopic2ReleSet();
			int sum = 0;
			int subNum = 0;
			for(Entry<Integer, HashSet<String>> entry: trecQueryAspects._subtopic2ReleSet.entrySet()){
				if(checkAvailable){
					HashSet<String> releDocSet = entry.getValue();
					sum += getIntersetCnt(releDocSet, trecQueryAspects._topnDocs);
					subNum++;
				}else{
					sum += entry.getValue().size();		
					subNum++;
				}				
			}
			System.out.println((count++)+":\t"+(sum*1.0/subNum));
		}
	}
	
	private static int getIntersetCnt(HashSet<String> releDocSet, Set<String> topNSet){
		//System.out.println(topNSet.size()+"\t"+topNSet);
		int cnt=0;
		for(String releDoc: releDocSet){
			if(topNSet.contains(releDoc)){
				cnt++;
			}
		}
		return cnt;
	}
	
	
	//
	public static void main(String []args){
		//1
		//Sta.avgDocNumPerSubtopic(DivVersion.Div2009);
		
		//2
		boolean checkAvailable = true;
		Sta.avgDocNumPerSubtopic(DivVersion.Div2012, checkAvailable);
	}
	

}
