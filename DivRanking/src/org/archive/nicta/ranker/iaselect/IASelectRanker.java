package org.archive.nicta.ranker.iaselect;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.Vector;

import org.archive.dataset.ntcir.sm.SMTopic;
import org.archive.dataset.trec.query.TRECDivQuery;
import org.archive.dataset.trec.query.TRECSubtopic;
import org.archive.ml.ufl.Mat;
import org.archive.nicta.kernel.Kernel;
import org.archive.nicta.ranker.ResultRanker;
import org.archive.ntcir.dr.rank.DRRunParameter;
import org.archive.util.Pair;
import org.archive.util.tuple.StrDouble;

public class IASelectRanker extends ResultRanker{
	
	////!!! probability kernel is required !!!
	private Kernel _kernel;
	//buffer relevance probability of a document w.r.t. a subquery (i.e., subtopic)
	private ArrayList<HashMap<Pair,Double>> _releproToSubtopicCache;
	
	IASelectRanker(HashMap<String, String> docs_all){
		super(docs_all);
		
		_releproToSubtopicCache = new ArrayList<>();
	}
	
	public ArrayList<String> getResultList(TRECDivQuery trecDivQuery, int size) {
		Vector<TRECSubtopic> trecSubtopicList = trecDivQuery.getSubtopicList();
		ArrayList<Double> popList = getPopularityList(trecDivQuery);
		
		////2		
		Set<String> D = _docs_topn;	
		ArrayList<String> S = new ArrayList<String>();		
		ArrayList<String> D_Minus_S = new ArrayList<String>();
		int desiredK = Math.min(size, D.size());
		D_Minus_S.addAll(D);
		
		
		ArrayList<Double> U_c_q_SList = new ArrayList<>();
		for(Double pop: popList){
			U_c_q_SList.add(pop);
		}
		
		////buffer part
		//query
		//String query_repr_key = query_repr.toString();
		//subtopic
		ArrayList<String> subtopicRepStrList = new ArrayList<>();
		ArrayList<Object> subtopicRepObjList = new ArrayList<>();
		for(TRECSubtopic trecSubtopic: trecSubtopicList){
			Object subtopicRepr = _kernel.getNoncachedObjectRepresentation(trecSubtopic.getContent());
			subtopicRepObjList.add(subtopicRepr);
			subtopicRepStrList.add(subtopicRepr.toString());
			
			this._releproToSubtopicCache.add(new HashMap<>());
		}
		
		Pair releToSub_key = null;
		Double releproToSub = null;
		Object doc_repr = null;
		
		for(String doc_name: D){
			//w.r.t. subtopics
			for(int subI=0; subI<trecSubtopicList.size(); subI++){
				HashMap<Pair,Double> releToSubCatch = this._releproToSubtopicCache.get(subI);
				String sub_repr_key = subtopicRepStrList.get(subI);				
				releToSub_key = new Pair(sub_repr_key, doc_name);
				
				if(null == (releproToSub=releToSubCatch.get(releToSub_key))){
					doc_repr = _docRepr.get(doc_name);
					releproToSub = _kernel.sim(subtopicRepObjList.get(subI), doc_repr);
					releToSubCatch.put(releToSub_key, releproToSub);
				}
			}			
		}

		while(S.size() < desiredK){
			
			String d_star = get_d_star(subtopicRepStrList, U_c_q_SList, D_Minus_S);
			
			S.add(d_star);
			D_Minus_S.remove(d_star);
			
			//update
			for(int subI=0; subI<subtopicRepStrList.size(); subI++){
				String subRepreKey = subtopicRepStrList.get(subI);
				Pair releproToSub_key = new Pair(subRepreKey, d_star);
				double relepro = this._releproToSubtopicCache.get(subI).get(releproToSub_key);
				
				U_c_q_SList.set(subI, U_c_q_SList.get(subI)*(1-relepro));
			}			
		}
		
		return S;
	}
	
	private ArrayList<Double> getPopularityList(TRECDivQuery trecDivQuery){
		int subtopicNumber = trecDivQuery.getSubtopicList().size();
		return Mat.getUniformList(1.0d/subtopicNumber, subtopicNumber);		
	}
	
	private String get_d_star(ArrayList<String> subtopicRepStrList, ArrayList<Double> U_c_q_SList,
			ArrayList<String> D_Minus_S){
		////
		double maxV = Double.NEGATIVE_INFINITY;
		String d_star = null;
		
		ArrayList<String> tieList = new ArrayList<>();
		
		for(String d: D_Minus_S){
			double val = 0.0;
			for(int subI=0; subI<subtopicRepStrList.size(); subI++){
				String sub_repr_key = subtopicRepStrList.get(subI);
				Pair releproToSub_key = new Pair(sub_repr_key, d);
				double relepro = this._releproToSubtopicCache.get(subI).get(releproToSub_key);
				
				val += (U_c_q_SList.get(subI)*relepro);				
			}
			
			if(val > maxV){
				maxV = val;
				d_star = d;
				tieList.clear();
				tieList.add(d_star);
			}else if(val == maxV){
				tieList.add(d);
			}
		}
		
		if(1==tieList.size()){
			return tieList.get(0);
		}else if (tieList.size() > 1) {
			 int size = tieList.size();
			 int randomI = (int)(Math.random()*size);
			 return tieList.get(randomI);
		}else{
			System.out.println("tieList error!");
			System.exit(0);
			return null;
		}
	}
	
	////update part w.r.t. each new query
	//be called when a new query comes
	public void addATopNDoc(String doc_name) {
		_docs_topn.add(doc_name);
	}
	//refresh each time for a query
	//_docOrig, i.e., the top-n set of a query
	public void clearInfoOfTopNDocs() {
		_docs_topn.clear();
		_kernel.clearInfoOfTopNDocs();	
		
		this._releproToSubtopicCache.clear();
	}
	//called when a new query come
	public void initTonNDocsForInnerKernels() {
		// The similarity kernel may need to do pre-processing (e.g., LDA training)
		_kernel.initTonNDocs(_docs_topn); 
		
		for (String doc : _docs_topn) {
			Object repr = _kernel.getObjectRepresentation(doc);
			_docRepr.put(doc, repr);			
		}
	}	
	
	////null part
	public ArrayList<String> getResultList(String query, int size) {
		return null;
	}
	//--
	public ArrayList<StrDouble> getResultList(DRRunParameter drRunParameter, SMTopic smTopic, ArrayList<String> subtopicList, int cutoff){
		return null;
	}
	
	////
	public String getDescription() {
		return "PM2-Ranker";
	}
	public String getString(){
		return "PM2-Ranker";
	}

}
