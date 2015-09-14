package org.archive.dataset.statistic;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.archive.dataset.trec.TRECDivLoader;
import org.archive.dataset.trec.TRECDivLoader.DivVersion;
import org.archive.dataset.trec.query.TRECQueryAspects;

public class Sta {
	
	/**
	 * Statistics w.r.t. TREC
	 * **/
	//number for relevant documetns per subtopic
	private static void avgDocNumPerSubtopic(DivVersion divVersion){
		List<String> qList = TRECDivLoader.getDivEvalQueries(divVersion);
		Map<String,TRECQueryAspects> trecDivQueryAspects = TRECDivLoader.loadTrecDivQueryAspects(divVersion);
		
		for(String q: qList){
			TRECQueryAspects trecQueryAspects = trecDivQueryAspects.get(q);
			trecQueryAspects.iniSubtopic2ReleSet();
			int sum = 0;
			int subNum = 0;
			for(Entry<Integer, HashSet<String>> entry: trecQueryAspects._subtopic2ReleSet.entrySet()){
				sum += entry.getValue().size();		
				subNum++;
			}
			System.out.println((sum*1.0/subNum));
		}
	}
	
	
	//
	public static void main(String []args){
		//1
		//Sta.avgDocNumPerSubtopic(DivVersion.Div2009);
		
		//2
		Sta.avgDocNumPerSubtopic(DivVersion.Div2010);
	}
	

}
