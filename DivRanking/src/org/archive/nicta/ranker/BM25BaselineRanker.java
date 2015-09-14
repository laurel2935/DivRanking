package org.archive.nicta.ranker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import org.archive.dataset.ntcir.sm.SMTopic;
import org.archive.dataset.trec.query.TRECDivQuery;
import org.archive.nicta.kernel.BM25Kernel_A1;
import org.archive.ntcir.dr.rank.DRRunParameter;
import org.archive.util.tuple.PairComparatorBySecond_Desc;
import org.archive.util.tuple.StrDouble;

public class BM25BaselineRanker extends ResultRanker{
	
	private BM25Kernel_A1 bm25_A1_Kernel;
	
	public BM25BaselineRanker(HashMap<String, String> docs) { 
		super(docs);	
		//
		double k1, k3, b;
		k1=1.2d; k3=0.5d; b=0.5d;   // achieves the best
		//k1=0.5d; k3=0.5d; b=0.5d; //better than the group of b=1000d;
		//k1=1.2d; k3=0.5d; b=1000d;
		bm25_A1_Kernel = new BM25Kernel_A1(docs, k1, k3, b);
	}
	
	//be called when a new query comes
	public void addATopNDoc(String doc_name) {
		_docs_topn.add(doc_name);
	}
	
	//refresh each time for a query
	public void clearInfoOfTopNDocs() {
		_docRepr.clear();		
		_docs_topn.clear();
		bm25_A1_Kernel.clearInfoOfTopNDocs();		
	}
	
	public void initTonNDocsForInnerKernels() {
		bm25_A1_Kernel.initTonNDocs(_docs_topn); // LDA should maintain keys for mapping later		
		// Store local representation for later use with kernels
		// (should we let _sim handle everything and just interact with keys?)
		for (String doc : _docs_topn) {
			Object repr = bm25_A1_Kernel.getObjectRepresentation(doc);
			_docRepr.put(doc, repr);			
		}
	}
	
	@Override
	public ArrayList<String> getResultList(String query, int list_size) {
		
		ArrayList<String> resultList = new ArrayList<String>();

		// Intialize document set
		initTonNDocsForInnerKernels();
		
		// Get representation for query
		Object query_repr = bm25_A1_Kernel.getNoncachedObjectRepresentation(query);		
		
		ArrayList<StrDouble> topNDocRankList = new ArrayList<StrDouble>();
		
		for(String doc_name: _docs_topn){
			Object doc_repr = _docRepr.get(doc_name);
			double sim_score = bm25_A1_Kernel.sim(query_repr, doc_repr);
			
			topNDocRankList.add(new StrDouble(doc_name, sim_score));
		}		
		
		Collections.sort(topNDocRankList, new PairComparatorBySecond_Desc<String, Double>());				
		
		//bm25 ordered list
		for(int i=0; i<list_size; i++){
			resultList.add(topNDocRankList.get(i).getFirst());
		}
		
		return resultList;
	}
	
	public ArrayList<String> getResultList(TRECDivQuery trecDivQuery, int size) {
		return null;
	}
	
	public ArrayList<StrDouble> getResultList(DRRunParameter drRunParameter, SMTopic smTopic, ArrayList<String> subtopicList, int cutoff){
		return null;
	}
	
	@Override
	public String getDescription() {
		// TODO Auto-generated method stub
		return "BM25Baseline"+bm25_A1_Kernel.getKernelDescription();
	}
	
	public String getString(){
		return "BM25Baseline";
	}

}
