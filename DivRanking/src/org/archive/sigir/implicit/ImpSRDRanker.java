package org.archive.sigir.implicit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import org.archive.a1.ranker.fa.DCKUFLRanker.Strategy;
import org.archive.dataset.ntcir.sm.SMTopic;
import org.archive.dataset.trec.query.TRECDivQuery;
import org.archive.ml.clustering.ap.affinitymain.InteractionData;
import org.archive.ml.ufl.DCKUFL.ExemplarType;
import org.archive.nicta.kernel.Kernel;
import org.archive.nicta.ranker.ResultRanker;
import org.archive.ntcir.dr.rank.DRRunParameter;
import org.archive.sigir.implicit.ImpSRD.UFLMode;
import org.archive.util.tuple.PairComparatorBySecond_Desc;
import org.archive.util.tuple.StrDouble;

public class ImpSRDRanker extends ResultRanker{	
	
	private double _lambda = 0.5;
	private int _iterationTimes = 5000;
	private int _noChangeIterSpan = 10; 
	//balance the similarity and diversity
	private double _SimDivLambda = 0.5;
	
	//kernel, under which each query, document is represented
	public Kernel _kernel;
	
	ExemplarType _exemplarType;
	Strategy _flStrategy;
	
	// Constructor
	public ImpSRDRanker(HashMap<String, String> docs, Kernel kernel, double lambda, int iterationTimes, int noChangeIterSpan, double SimDivLambda, ExemplarType exemplarType, Strategy flStrategy) { 
		super(docs);				
		this._kernel = kernel;		
		this._lambda = lambda;
		this._iterationTimes = iterationTimes;
		this._noChangeIterSpan = noChangeIterSpan;
		this._SimDivLambda = SimDivLambda;
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
		// The similarity kernel may need to do pre-processing (e.g., LDA training)
		_kernel.initTonNDocs(_docs_topn); 
	}	
	//
	public ArrayList<String> getResultList(String query, int size) {
		return null;
	}
	
	//
	private ArrayList<InteractionData> getReleMatrix(){
		ArrayList<InteractionData> releMatrix = new ArrayList<InteractionData>();		
		
		String [] topNDocNames = _docs_topn.toArray(new String[0]);
		for(int i=0; i<topNDocNames.length-1; i++){
			String iDocName = topNDocNames[i]; 
			Object iDocRepr = _kernel.getObjectRepresentation(iDocName);
			for(int j=i+1; j<topNDocNames.length; j++){
				String jDocName = topNDocNames[j];
				Object jDocRepr = _kernel.getObjectRepresentation(jDocName);
				//
				releMatrix.add(new InteractionData(iDocName, jDocName, (1-_SimDivLambda)*_kernel.sim(iDocRepr, jDocRepr)));				
			}
		}
		
		return releMatrix;		
	}
	
	private ArrayList<InteractionData> getCostMatrix(ArrayList<InteractionData> interList){
		ArrayList<InteractionData> costMatrix = new ArrayList<InteractionData>();
		for(InteractionData itr: interList){
			costMatrix.add(new InteractionData(itr.getFrom(), itr.getTo(), -itr.getSim()));
		}
		return costMatrix;		
	}
	
	public double getMedian(ArrayList<Double> vList){
		Collections.sort(vList);
		//
		if(vList.size() % 2 == 0){
			return (vList.get(vList.size()/2)+vList.get(vList.size()/2 - 1))/2.0d;
		}else{
			return vList.get(vList.size()/2);
		}
	}
	
	//list of document relevance w.r.t. query
	private ArrayList<StrDouble> getFacilityCostList(TRECDivQuery trecDivQuery, double lambda){		
		Object queryRepr = _kernel.getNoncachedObjectRepresentation(trecDivQuery.getQueryContent());
		ArrayList<StrDouble> facilityCostList = new ArrayList<StrDouble>();
    	
		String [] topNDocNames = _docs_topn.toArray(new String[0]);
		for(String doc_name: topNDocNames){
    		Object jDocRepr = _kernel.getObjectRepresentation(doc_name);
    		//
    		facilityCostList.add(new StrDouble(doc_name, 0-_kernel.sim(queryRepr, jDocRepr)*lambda));
    	}
		
		return facilityCostList;
	}
	
	private static int zeroCnt = 1;
	public ArrayList<String> getResultList(TRECDivQuery trecDivQuery, int size) {
		//
		initTonNDocsForInnerKernels();		
		//
		ArrayList<InteractionData> releMatrix = getReleMatrix();
		ArrayList<InteractionData> costMatrix = getCostMatrix(releMatrix);		
		//
    	ArrayList<Double> vList = new ArrayList<Double>();
    	for(InteractionData itrData: costMatrix){
    		vList.add(itrData.getSim());
    	}
    	
    	double costPreference = getMedian(vList);
    	
    	ArrayList<StrDouble> facilityCostList = getFacilityCostList(trecDivQuery, this._SimDivLambda);
    	//
    	ImpSRD impSRD = new ImpSRD(trecDivQuery._number, _lambda, _iterationTimes, _noChangeIterSpan,
    			costPreference, size, UFLMode.C_Same_F, costMatrix, facilityCostList);
    	//
      	
    	impSRD.run();
    	
    	ArrayList<String> facilityList = impSRD.getSelectedFacilities(this._exemplarType, size);
    	
    	if(facilityList.size() == 0){
    		System.err.println(zeroCnt++);
    		return null;
    	}
    	
    	ArrayList<String> resultList;
    	
    	if(Strategy.Belief == this._flStrategy){    		
    		resultList = facilityList;    		
    	}else{    		
    		//(1)final ranking by similarity between query and document
        	ArrayList<StrDouble> objList = new ArrayList<StrDouble>();
        	
        	for(String docName: facilityList){
        		objList.add(new StrDouble(docName, 0-impSRD._Y.get(0, impSRD.getFacilityID(docName))));
        	}
        	Collections.sort(objList, new PairComparatorBySecond_Desc<String, Double>());
        	
        	resultList = new ArrayList<String>();
        	for(int i=0; i<size; i++){
        		resultList.add(objList.get(i).getFirst());
        	}        	
    	}    	
    	
    	return resultList;		
	}
	
	public Kernel getKernel(){
		return this._kernel;
	}
	
	//
	public String getString(){
		return "ImpSRDRanker";
	}
	//
	public String getDescription() {
		// TODO Auto-generated method stub
		return "ImpSRDRanker - sbkernel: " + _kernel.getKernelDescription();
	}
	
	//--
	public ArrayList<StrDouble> getResultList(DRRunParameter drRunParameter, SMTopic smTopic, ArrayList<String> subtopicList, int cutoff){
		return null;
	}

}
