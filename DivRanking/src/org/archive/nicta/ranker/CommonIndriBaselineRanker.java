package org.archive.nicta.ranker;

import java.util.ArrayList;
import java.util.HashMap;

import org.archive.dataset.ntcir.sm.SMTopic;
import org.archive.dataset.trec.TRECDivLoader;
import org.archive.dataset.trec.TRECDivLoader.DivVersion;
import org.archive.dataset.trec.query.TRECDivQuery;
import org.archive.ntcir.dr.rank.DRRunParameter;
import org.archive.util.tuple.StrDouble;

public class CommonIndriBaselineRanker  extends ResultRanker {
	
	HashMap<String, ArrayList<String>> _baselineMap;
	
	public CommonIndriBaselineRanker(DivVersion divVersion, HashMap<String, String> docs) { 
		super(docs);
		
		_baselineMap = TRECDivLoader.loadTRECDivBaseline(divVersion);
		this._indexOfGetResultMethod = 1;
	}

	
	@Override
	public ArrayList<String> getResultList(String query, int list_size) {
		return null;	
	}
	
	//--
	
	public void addATopNDoc(String doc_name) {}
	
	public void clearInfoOfTopNDocs() {}
	
	public void initTonNDocsForInnerKernels() {}
	
	public ArrayList<String> getResultList(TRECDivQuery trecDivQuery, int size) {
		ArrayList<String> resultList = new ArrayList<>();
		//System.out.println(query);
		ArrayList<String> rankedList = _baselineMap.get(trecDivQuery._number);
		for(int i=0; i<size; i++){
			resultList.add(rankedList.get(i));
		}
		
		return resultList;	
	}
	
	public ArrayList<StrDouble> getResultList(DRRunParameter drRunParameter, SMTopic smTopic, ArrayList<String> subtopicList, int cutoff){
		return null;
	}
	//--
	
	
	@Override
	public String getDescription() {
		// TODO Auto-generated method stub
		return "CommonIndriBaseline";
	}
	
	public String getString(){
		return "CommonIndriBaseline";
	}
}
