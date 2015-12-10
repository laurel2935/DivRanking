package org.archive.nicta.ranker.xquad;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.Vector;

import org.archive.dataset.ntcir.sm.SMTopic;
import org.archive.dataset.trec.query.TRECDivQuery;
import org.archive.dataset.trec.query.TRECSubtopic;
import org.archive.ml.clustering.ap.affinitymain.InteractionData;
import org.archive.ml.ufl.Mat;
import org.archive.nicta.kernel.Kernel;
import org.archive.nicta.ranker.ResultRanker;
import org.archive.ntcir.dr.rank.DRRunParameter;
import org.archive.util.Pair;
import org.archive.util.tuple.StrDouble;

public class XQuADRanker extends ResultRanker {
	
	private double _lambda;
	//// !!! probability kernel is required !!!
	private Kernel _kernel;
	
	//buffer relevance score of a document w.r.t. the query
	private HashMap<Pair,Double>   _releToQCache;
	//buffer relevance probability of a document w.r.t. subquery (i.e., subtopic) list
	private ArrayList<HashMap<Pair,Double>> _releToSubtopicCache;
	
	public XQuADRanker(HashMap<String, String> docs_all, double lambda){
		super(docs_all);
		
		this._lambda = lambda;
		this._releToQCache = new HashMap<>();
		this._releToSubtopicCache = new ArrayList<>();
	}
	
	////core ranking part
	public ArrayList<String> getResultList(TRECDivQuery trecDivQuery, int size) {
		////1
		Vector<TRECSubtopic> trecSubtopicList = trecDivQuery.getSubtopicList();
		ArrayList<Double> popList = getPopularityList(trecDivQuery);
		Object query_repr = _kernel.getNoncachedObjectRepresentation(trecDivQuery.getQueryContent());
		////2		
		Set<String> D = _docs_topn;	
		ArrayList<String> S = new ArrayList<String>();		
		ArrayList<String> D_Minus_S = new ArrayList<String>();
		//possible k
		int k = Math.min(size, D.size());
		D_Minus_S.addAll(D);

		//query
		String query_repr_key = query_repr.toString();
		//subtopic
		ArrayList<String> subtopicRepStrList = new ArrayList<>();
		ArrayList<Object> subtopicRepObjList = new ArrayList<>();
		for(TRECSubtopic trecSubtopic: trecSubtopicList){
			Object subtopicRepr = _kernel.getNoncachedObjectRepresentation(trecSubtopic.getContent());
			subtopicRepObjList.add(subtopicRepr);
			subtopicRepStrList.add(subtopicRepr.toString());
			
			this._releToSubtopicCache.add(new HashMap<>());
		}
		
		Pair releToQ_key = null;
		Pair releToSub_key = null;
		Double releToQ_score = null;
		Double releToSub_score = null;
		Object doc_repr = null;
		
		for(String doc_name: D){
			//w.r.t. query
			releToQ_key = new Pair(query_repr_key, doc_name);
			if (null == (releToQ_score = _releToQCache.get(releToQ_key))) {
				doc_repr = _docRepr.get(doc_name);
				releToQ_score = _kernel.sim(query_repr, doc_repr);
				_releToQCache.put(releToQ_key, releToQ_score);
			}
			//w.r.t. subtopics
			for(int subI=0; subI<trecSubtopicList.size(); subI++){
				HashMap<Pair,Double> releToSubCatch = _releToSubtopicCache.get(subI);
				String sub_repr_key = subtopicRepStrList.get(subI);				
				releToSub_key = new Pair(sub_repr_key, doc_name);
				
				if(null == (releToSub_score=releToSubCatch.get(releToSub_key))){
					doc_repr = _docRepr.get(doc_name);
					releToSub_score = _kernel.sim(subtopicRepObjList.get(subI), doc_repr);
					releToSubCatch.put(releToSub_key, releToSub_score);
				}
			}			
		}
		
		ArrayList<Double> likelihoodOfNotObserving_S_PerSubtopic = new ArrayList<>();
		for(int i=0; i<popList.size(); i++){
			likelihoodOfNotObserving_S_PerSubtopic.add(1.0);
		}
		////
		while(S.size() < k){
			
			String d_star = get_d_star(query_repr_key, subtopicRepStrList, popList, S, D_Minus_S, likelihoodOfNotObserving_S_PerSubtopic);
			S.add(d_star);
			D_Minus_S.remove(d_star);
			
			updateLikelihoodOfNotObservingS(likelihoodOfNotObserving_S_PerSubtopic, subtopicRepStrList, d_star);
			
		}
		
		return S;
	}
	
	private String get_d_star(String query_repr_key, ArrayList<String> subtopicRepStrList, ArrayList<Double> popList,
			ArrayList<String> S, ArrayList<String> D_Minus_S, ArrayList<Double> likelihoodOfNotObserving_S_PerSubtopic){
		
		double maxV = Double.NEGATIVE_INFINITY;
		String d_star = null;
		for(String d: D_Minus_S){
			//part-1
			Pair releToQ_key = new Pair(query_repr_key, d);
			Double releToQ_Score = _releToQCache.get(releToQ_key);
			double heuristicV = _lambda*releToQ_Score;
			
			//part-2;
			for(int subI=0; subI<popList.size(); subI++){
				String sub_repr_key = subtopicRepStrList.get(subI);
				double pop = popList.get(subI);
				Pair releToSub_key = new Pair(sub_repr_key, d);
				double reletoSub_Score = _releToSubtopicCache.get(subI).get(releToSub_key);
				
				heuristicV += (1-_lambda)*pop*reletoSub_Score*likelihoodOfNotObserving_S_PerSubtopic.get(subI);
			}
			
			if(heuristicV > maxV){
				maxV = heuristicV;
				d_star = d;
			}			
		}
		return d_star;
	}
	
	//the likelihood of not observing the documents in S
	
	private void updateLikelihoodOfNotObservingS(ArrayList<Double> likelihoodOfNotObserving_S_PerSubtopic,
			ArrayList<String> subtopicRepStrList, String doc_name){
		//
		for(int i=0; i<subtopicRepStrList.size(); i++){
			String sub_repr_key = subtopicRepStrList.get(i);
			Pair releToSub_key = new Pair(sub_repr_key, doc_name);
			double reletoSub_Score = _releToSubtopicCache.get(i).get(releToSub_key);
			likelihoodOfNotObserving_S_PerSubtopic.set(i, 
					(1-reletoSub_Score)*likelihoodOfNotObserving_S_PerSubtopic.get(i));
		}		
	}
	
	private ArrayList<Double> getPopularityList(TRECDivQuery trecDivQuery){
		int subtopicNumber = trecDivQuery.getSubtopicList().size();
		return Mat.getUniformList(1.0d/subtopicNumber, subtopicNumber);		
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
		//
		_releToQCache.clear();
		_releToSubtopicCache.clear();
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
	//
	public ArrayList<String> getResultList(String query, int size) {
		return null;
	}
	//--
	public ArrayList<StrDouble> getResultList(DRRunParameter drRunParameter, SMTopic smTopic, ArrayList<String> subtopicList, int cutoff){
		return null;
	}
	
	////
	public String getDescription() {
		return "XQuADRanker";
	}
	public String getString(){
		return "XQuADRanker";
	}

}
