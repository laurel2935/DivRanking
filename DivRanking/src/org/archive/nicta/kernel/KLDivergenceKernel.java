package org.archive.nicta.kernel;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.archive.terrier.TrecScorer;

public class KLDivergenceKernel extends Kernel {
	//the default is Dirichlet LM
	private static TrecScorer _trecScorer = new TrecScorer();;
	
	//maximum LM 
	public HashMap<String,Object> _mleLMReprCache = new HashMap<String,Object>();
	//dirichlet LM
	public HashMap<String,Object> _dirichletLMReprCache = new HashMap<String,Object>();
	
	public KLDivergenceKernel(HashMap<String, String> docs){
		super(docs);
	}
	
	public void initTonNDocs(Set<String> docs_topn) {
		// Have to go through all documents
		for (String doc : docs_topn) {
			String content = _docs_all.get(doc);
			_mleLMReprCache.put(doc, getNoncachedMLELMObjectRepresentation(content));
			_dirichletLMReprCache.put(doc, getNoncachedObjectRepresentation(content));
		}		
	}
	
	public void clearInfoOfTopNDocs() {
		_mleLMReprCache.clear();
		_dirichletLMReprCache.clear();		
	}
	
	
	/**
	 * the default is feature vector based on Dirichlet LM 
	 * **/
	public Object getNoncachedObjectRepresentation(String docText) {
		Map<Object,Double> features = _trecScorer.getDirLMFeatureVector(docText);
		return features;
	}
	
	public Object getNoncachedMLELMObjectRepresentation(String docText) {
		Map<Object,Double> features = _trecScorer.getMLELMFeatureVector(docText);
		return features;
	}
	
	public Object getMLELMObjectRepresentation(String doc_name) {
		Object doc_repr = null;
		
		if ((doc_repr = _mleLMReprCache.get(doc_name)) != null){
			return doc_repr;
		}
			
		String doc_content = _docs_all.get(doc_name);
		doc_repr = getNoncachedMLELMObjectRepresentation(doc_content);
		_mleLMReprCache.put(doc_name, doc_repr);
		return doc_repr;
	}
	
	/**
	 * how to differentiate the two input parameters ???!!!
	 * **/
	public double sim(Object objQuery, Object objDoc) {		
		//return -distance(objQuery, objDoc);	
		return expSim(objQuery, objDoc);
	}
	/**
	 * according to 
	 * 2005-PageRank without hyperlinks Structural re-ranking using links induced by language models
	 * **/
	public double expSim(Object objQuery, Object objDoc) {		
		double sum = distance(objQuery, objDoc);		
		return Math.exp(-sum);
	}
	
	/**
	 * KL divergence, i.e., according to its definition
	 * for computing distance between two documents, !&2012-Top-k retrieval using facility location analysis
	 * **/
	public double distance(Object objQuery, Object objDoc){
		Map<Object, Double> queryFeatureVec = (Map<Object, Double>)objQuery;
		Map<Object, Double> docFeatureVec   = (Map<Object, Double>)objDoc;
		
		double sum = 0.0;
		
		for(Object term: queryFeatureVec.keySet()){			
			//System.out.println();
			//System.out.println();
			
			if((null==queryFeatureVec.get(term))
					|| (null==docFeatureVec.get(term))){
				continue;
			}
			
			sum += (queryFeatureVec.get(term)*Math.log(queryFeatureVec.get(term)/docFeatureVec.get(term)));
		}
		
		return sum;	
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
