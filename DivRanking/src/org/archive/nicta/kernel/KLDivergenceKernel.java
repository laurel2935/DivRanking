package org.archive.nicta.kernel;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.archive.terrier.TrecScorer;

public class KLDivergenceKernel extends Kernel {
	//the default is Dirichlet LM
	private static TrecScorer _trecScorer = new TrecScorer();;
	
	//maximum LM 
	public HashMap<String,Object> _queryReprCache = new HashMap<String,Object>();
	//dirichlet LM
	public HashMap<String,Object> _docReprCache = new HashMap<String,Object>();
	
	KLDivergenceKernel(HashMap<String, String> docs){
		super(docs);
	}
	
	public void initTonNDocs(Set<String> docs_topn) {
		// Have to go through all documents
		for (String doc : docs_topn) {
			String content = _docs_all.get(doc);
			_queryReprCache.put(doc, getMLELMObjectRepresentation(content));
			_docReprCache.put(doc, getNoncachedObjectRepresentation(content));
		}		
	}
	
	public void clearInfoOfTopNDocs() {
		_queryReprCache.clear();
		_docReprCache.clear();		
	}
	
	
	/**
	 * the default is feature vector based on Dirichlet LM 
	 * **/
	public Object getNoncachedObjectRepresentation(String docText) {
		Map<Object,Double> features = _trecScorer.getDirLMFeatureVector(docText);
		return features;
	}
	
	public Object getMLELMObjectRepresentation(String docText) {
		Map<Object,Double> features = _trecScorer.getMLELMFeatureVector(docText);
		return features;
	}
	
	/**
	 * how to differentiate the two input parameters ???!!!
	 * **/
	public double sim(Object objQuery, Object objDoc) {
		
		Map<Object, Double> queryFeatureVec = (Map<Object, Double>)objQuery;
		Map<Object, Double> docFeatureVec   = (Map<Object, Double>)objDoc;
		
		double sum = 0.0;
		
		for(Object term: queryFeatureVec.keySet()){
			sum += (queryFeatureVec.get(term)*Math.log(queryFeatureVec.get(term)/docFeatureVec.get(term)));
		}
		
		return Math.exp(-sum);
		//return -sum;		
	}
	
	public double sim(Object o1, Object o2, Object ow) {
		System.out.println("ERROR: Cannot do DL query-reweighted similarity");
		System.exit(1);
		return -1d;
	}
	
	@Override
	public String getObjectStringDescription(Object obj) {
		return obj.toString();
	}
	
	@Override
	public String getKernelDescription() {
		return "KLDivergenceKernel";
	}
	
	public String getString(){
		return "KLDivergenceKernel";
	}
}
