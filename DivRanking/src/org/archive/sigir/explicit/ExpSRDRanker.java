package org.archive.sigir.explicit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Vector;

import org.archive.a1.ranker.fa.DCKUFLRanker.Strategy;
import org.archive.dataset.ntcir.sm.SMTopic;
import org.archive.dataset.trec.query.TRECDivQuery;
import org.archive.dataset.trec.query.TRECSubtopic;
import org.archive.ml.clustering.ap.affinitymain.InteractionData;
import org.archive.ml.ufl.DCKUFL.ExemplarType;
import org.archive.ml.ufl.Mat;
import org.archive.nicta.kernel.Kernel;
import org.archive.nicta.ranker.ResultRanker;
import org.archive.ntcir.dr.rank.DRRunParameter;
import org.archive.util.Pair;
import org.archive.util.tuple.PairComparatorBySecond_Desc;
import org.archive.util.tuple.StrDouble;

public class ExpSRDRanker extends ResultRanker{
	
	private static final boolean debug = false;
	
	//public static enum Strategy{Belief, QDSim}
	
	ExemplarType _exemplarType;
	Strategy _flStrategy;
	
	private double _lambda = 0.5;
	private int _iterationTimes = 5000;
	private int _noChangeIterSpan = 10; 
	
	//kernel, under which each query, subtopic, document is represented
	public Kernel _kernel;
	
	//buffer similarity values by _sbKernel for two items
	public HashMap<Pair,Double>   _simCache;
		
	// Constructor
	public ExpSRDRanker(HashMap<String, String> docs, Kernel kernel, double lambda, int iterationTimes, int noChangeIterSpan, ExemplarType exemplarType, Strategy flStrategy) { 
		super(docs);				
		this._kernel = kernel;	
		this._simCache = new HashMap<Pair,Double>();
		this._lambda = lambda;
		this._iterationTimes = iterationTimes;
		this._noChangeIterSpan = noChangeIterSpan;
		//
		this._indexOfGetResultMethod = 1;
		
		this._exemplarType = exemplarType;
		this._flStrategy = flStrategy;
	}
	
	//be called when a new query comes
	public void addATopNDoc(String doc_name) {
		_docs_topn.add(doc_name);
	}
	//refresh each time for a query
	//_docOrig, i.e., the top-n set of a query
	public void clearInfoOfTopNDocs() {
		//_docRepr.clear();		
		_docs_topn.clear();
		_kernel.clearInfoOfTopNDocs();	
	}
	//called when a new query come
	public void initTonNDocsForInnerKernels() {
		//The similarity kernel may need to do pre-processing (e.g., LDA training)
		_kernel.initTonNDocs(_docs_topn);
	}	
	//
	public ArrayList<String> getResultList(String query, int size) {
		return null;
	}
	
	////////////////////////
	//TrecDive style
	////////////////////////
	
	//relevance between subtopics and documents
	private ArrayList<InteractionData> getReleMatrix(TRECDivQuery trecDivQuery){		
		initTonNDocsForInnerKernels();
		
		ArrayList<InteractionData> releMatrix = new ArrayList<InteractionData>();		
		//subtopic <-> docs
		Vector<TRECSubtopic> trecSubtopicList = trecDivQuery.getSubtopicList();
		String [] topNDocNames = _docs_topn.toArray(new String[0]);		
		for(TRECSubtopic trecSubtopic: trecSubtopicList){	
			Object subtopicRepr = _kernel.getNoncachedObjectRepresentation(trecSubtopic.getContent());
			for(String docName: topNDocNames){
				Object docRepr = _kernel.getObjectRepresentation(docName);
				//
				releMatrix.add(new InteractionData(trecSubtopic._sNumber, docName, _kernel.sim(subtopicRepr, docRepr)));
			}
		}
		
		return releMatrix;		
	}
	//similarity among subtopics
	private ArrayList<InteractionData> getSubtopicSimMatrix(TRECDivQuery trecDivQuery){
		ArrayList<InteractionData> simMatrix = new ArrayList<InteractionData>();
		
		Vector<TRECSubtopic> trecSubtopicList = trecDivQuery.getSubtopicList();		
		for(int i=0; i<trecSubtopicList.size()-1; i++){
			TRECSubtopic iSubtopic = trecSubtopicList.get(i);
			Object iRepr = _kernel.getNoncachedObjectRepresentation(iSubtopic.getContent());
			for(int j=i+1; j<trecSubtopicList.size(); j++){
				TRECSubtopic jSubtopic = trecSubtopicList.get(j);
				Object jRepr = _kernel.getNoncachedObjectRepresentation(jSubtopic.getContent());
				
				simMatrix.add(new InteractionData(iSubtopic._sNumber, jSubtopic._sNumber, _kernel.sim(iRepr, jRepr)));
			}
		}
		
		return simMatrix;	
	}
	//equal size of capacity of each subtopic
	private ArrayList<Double> getCapacityList_old(TRECDivQuery trecDivQuery, int topK){
		int subtopicNumber = trecDivQuery.getSubtopicList().size();
		if(topK%subtopicNumber == 0){
			double cap = topK/subtopicNumber;
			return Mat.getUniformList(cap, subtopicNumber);
		}else{
			double cap = topK/subtopicNumber+1;
			return Mat.getUniformList(cap, subtopicNumber);
		}		
	}
	//
	private ArrayList<Double> getCapacityList_new(TRECDivQuery trecDivQuery, int topK){
		int subtopicNumber = trecDivQuery.getSubtopicList().size();
		if(topK%subtopicNumber == 0){
			double cap = topK/subtopicNumber;
			return Mat.getUniformList(cap, subtopicNumber);
		}else{
			int cap = topK/subtopicNumber+1;
			ArrayList<Integer> intCapList = new ArrayList<Integer>();
			for(int i=0; i<subtopicNumber; i++){
				intCapList.add(cap);
			}
			
			int gap = cap*subtopicNumber - topK;
			
			int count = 0;
			for(int j=subtopicNumber-1; j>=0; j--){				
				intCapList.set(j, intCapList.get(j)-1);
				count++;
				if(count == gap){
					break;
				}
			}
			
			ArrayList<Double> dCapList = new ArrayList<Double>();
			for(Integer intCap: intCapList){
				dCapList.add(intCap.doubleValue());
			}
			
			return dCapList;			
		}		
	}
	//
	private ArrayList<Double> getPopularityList(TRECDivQuery trecDivQuery){
		int subtopicNumber = trecDivQuery.getSubtopicList().size();
		return Mat.getUniformList(1.0d/subtopicNumber, subtopicNumber);		
	}
	//
	private ArrayList<StrDouble> getUtilityList(TRECDivQuery trecDivQuery){		
		ArrayList<StrDouble> utiList = new ArrayList<StrDouble>();
		Object queryRepr = _kernel.getNoncachedObjectRepresentation(trecDivQuery.getQueryContent());
		String [] topNDocNames = _docs_topn.toArray(new String[0]);	
			
		String query_repr_key = queryRepr.toString();		
		Double sim_score = null;
		Object doc_repr = null;
		Pair sim_key = null;
		
		for(String doc_name: topNDocNames){
			sim_key = new Pair(query_repr_key, doc_name);
			
			if (null == (sim_score = _simCache.get(sim_key))) {
				doc_repr = _kernel.getObjectRepresentation(doc_name);
				sim_score = _kernel.sim(queryRepr, doc_repr);
				_simCache.put(sim_key, sim_score);
			}
			
			utiList.add(new StrDouble(doc_name, sim_score));
		}	
		
		return utiList;
	}
	//
	public ArrayList<String> getResultList(TRECDivQuery trecDivQuery, int size) {
		
		ArrayList<InteractionData> releMatrix = getReleMatrix(trecDivQuery);
		ArrayList<InteractionData> subSimMatrix = getSubtopicSimMatrix(trecDivQuery);
		ArrayList<Double> capList = getCapacityList_new(trecDivQuery, size);
		ArrayList<Double> popList = getPopularityList(trecDivQuery);
		ArrayList<StrDouble> utilityList = getUtilityList(trecDivQuery);
		    	
    	int preK = (int)Mat.sum(capList); 
    	
    	ExpSRD expSRD = new ExpSRD(trecDivQuery._number, _lambda, _iterationTimes, _noChangeIterSpan, preK, releMatrix, subSimMatrix, capList, popList, utilityList);
    	expSRD.run();
    	
    	//ArrayList<String> facilityList = dckufl.getSelectedFacilities();
    	ArrayList<String> facilityList = expSRD.getSelectedFacilities(this._exemplarType, size);
    	
    	ArrayList<String> resultList = null;
    	
    	if(Strategy.Belief == this._flStrategy){
    		
    		resultList = facilityList;
    		
    	}else{
    		
    		//(1)final ranking by similarity between query and document
        	ArrayList<StrDouble> objList = new ArrayList<StrDouble>();
        	Object queryRepr = _kernel.getNoncachedObjectRepresentation(trecDivQuery.getQueryContent());
        	String query_repr_key = queryRepr.toString();
        	
        	for(String docName: facilityList){
        		Pair sim_key = new Pair(query_repr_key, docName);
        		Double sim_score = null;
    			if (null == (sim_score = _simCache.get(sim_key))) {
    				Object doc_repr = _kernel.getObjectRepresentation(docName);
    				sim_score = _kernel.sim(queryRepr, doc_repr);
    				_simCache.put(sim_key, sim_score);
    			}
        		//--
        		objList.add(new StrDouble(docName, sim_score));
        	}
        	
        	//(2)??? similarity between subtopic vector and document
        	
        	Collections.sort(objList, new PairComparatorBySecond_Desc<String, Double>());
        	        	
        	resultList = new ArrayList<String>();
        	for(int i=0; i<size; i++){
        		resultList.add(objList.get(i).getFirst());
        	}
        	
        	if(debug){
        		System.out.println("Similarity Order:");
            	for(StrDouble obj: objList){
            		System.out.print(expSRD.getFacilityID(obj.first)+"\t");
            	}
            	System.out.println();
        	}        	
    	}
    	
    	return resultList;		
	}
	//
	public String getString(){
		return "ExpSRDRanker";
	}
	//
	public Kernel getKernel(){
		return this._kernel;
	}
	//
	public String getDescription() {
		// TODO Auto-generated method stub
		return "ExpSRDRanker - kernel: " + _kernel.getKernelDescription();
	}
	
	
	////
	public ArrayList<StrDouble> getResultList(DRRunParameter drRunParameter, SMTopic smTopic, ArrayList<String> subtopicList, int cutoff){
		return null;
	}	
}
