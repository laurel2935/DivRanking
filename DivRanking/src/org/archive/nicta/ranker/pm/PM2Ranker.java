package org.archive.nicta.ranker.pm;

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

public class PM2Ranker extends ResultRanker{
	
	private double _lambda;
	
	////!!! probability kernel is required !!!
	private Kernel _kernel;
	
	
	//buffer relevance probability of a document w.r.t. a subquery (i.e., subtopic)
	private ArrayList<HashMap<Pair,Double>> _releproToSubtopicCache;
	
	public PM2Ranker(HashMap<String, String> docs_all, double lambda, Kernel kernel){
		super(docs_all);
		
		this._lambda = lambda;
		this._releproToSubtopicCache = new ArrayList<>();
		
		this._kernel = kernel;
		
		this._indexOfGetResultMethod = 1;
	}
	
	////core ranking part
	public ArrayList<String> getResultList(TRECDivQuery trecDivQuery, int size) {
		initTonNDocsForInnerKernels();
		////
		Vector<TRECSubtopic> trecSubtopicList = trecDivQuery.getSubtopicList();
		ArrayList<Double> popList = getPopularityList(trecDivQuery);
		//Object query_repr = _kernel.getNoncachedObjectRepresentation(trecDivQuery.getQueryContent());
		
		////2		
		Set<String> D = _docs_topn;	
		ArrayList<String> S = new ArrayList<String>();		
		ArrayList<String> D_Minus_S = new ArrayList<String>();
		int desiredK = Math.min(size, D.size());
		D_Minus_S.addAll(D);
		
		////buffer part
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
		
		////
		Vector<Double> quotientList = new Vector<Double>(trecSubtopicList.size());
		Vector<Double> voteList = new Vector<Double>(trecSubtopicList.size());
		Vector<Double> doneSeatList = new Vector<Double>(trecSubtopicList.size());
		
		for(int i=0; i<trecSubtopicList.size(); i++){
			quotientList.add(0.0);
			voteList.add(popList.get(i));
			doneSeatList.add(0.0);
		}
				
		int s = 0;
		//first time
		calQuotient(quotientList, voteList, doneSeatList);
		//
		do {
			
			int i_star = getMaxIndex(quotientList);
			
			String d_star = get_d_star(i_star, quotientList, subtopicRepStrList, D_Minus_S);
			
			S.add(d_star);
			D_Minus_S.remove(d_star);
			
			//update seats	
			double sum = 0.0;
			for(int subI=0; subI<subtopicRepStrList.size(); subI++){
				String subRepreKey = subtopicRepStrList.get(subI);
				Pair releproToSub_key = new Pair(subRepreKey, d_star);
				double relepro = this._releproToSubtopicCache.get(subI).get(releproToSub_key);
				
				if(0.0 == relepro){
					relepro = 0.000001;
				}
				
				sum += relepro;
			}
			for(int subI=0; subI<subtopicRepStrList.size(); subI++){
				String subRepreKey = subtopicRepStrList.get(subI);
				Pair releproToSub_key = new Pair(subRepreKey, d_star);
				double relepro = this._releproToSubtopicCache.get(subI).get(releproToSub_key);
				
				if(0.0 == relepro){
					relepro = 0.000001;
				}
				
				doneSeatList.set(subI, doneSeatList.get(subI)+(relepro/sum));
			}
			//update 
			calQuotient(quotientList, voteList, doneSeatList);
			
			s++;
			
		} while (s<desiredK);
		
		return S;
	}
	
	private String get_d_star(int i_star, Vector<Double> quotientList, ArrayList<String> subtopicRepStrList,
			ArrayList<String> D_Minus_S){
		////
		double quotient_star = quotientList.get(i_star);
		String sub_repr_key_star = subtopicRepStrList.get(i_star);
		
		double maxV = Double.NEGATIVE_INFINITY;
		String d_star = null;
		
		for(String cand: D_Minus_S){
			//part-1			
			Pair releproToSub_key_star = new Pair(sub_repr_key_star, cand);
			double releproToStar = this._releproToSubtopicCache.get(i_star).get(releproToSub_key_star);
			double heuristicV = this._lambda*quotient_star*releproToStar;
			
			//part-2
			for(int subI=0; subI<subtopicRepStrList.size(); subI++){
				if(subI == i_star){
					continue;
				}else {
					String sub_repr_key = subtopicRepStrList.get(subI);
					Pair releproToSub_key = new Pair(sub_repr_key, cand);
					double relepro = this._releproToSubtopicCache.get(subI).get(releproToSub_key);
					double temp = (1-this._lambda)*quotientList.get(subI)*relepro;
					heuristicV += temp;
				}
			}
			
			if(heuristicV > maxV){
				maxV = heuristicV;
				d_star = cand;
			}
		}
		
		return d_star;
	}
	
	private static void calQuotient(Vector<Double> quotientList, Vector<Double> voteList, Vector<Double> doneSeatList){
		//System.out.println("before Quo:\t"+quotientList);
		//System.out.println("voteList:\t"+voteList);
		//System.out.println("doneList:\t"+doneSeatList);
		
		for(int i=0; i<quotientList.size(); i++){
			quotientList.set(i, voteList.get(i)/(2.0*doneSeatList.get(i)+1));		
		}		
		
		//System.out.println("after Quo:\t"+quotientList);
	}
	
	private static int getMaxIndex(Vector<Double> quotientList){
		//System.out.println(quotientList);
		int i_star=-1;
		double maxValue = Double.NEGATIVE_INFINITY;
		for(int k=0; k<quotientList.size(); k++){
			if(quotientList.get(k) > maxValue){
				maxValue = quotientList.get(k);
				i_star = k;
			}
		}
		return i_star;
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
