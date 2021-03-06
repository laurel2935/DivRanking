/** Term Frequency - Inverse Document Frequency (TF-IDF) Kernel
 *   
 * @author Scott Sanner (ssanner@gmail.com)
 */

package org.archive.nicta.kernel;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.archive.util.DocUtils;
import org.archive.util.VectorUtils;


public class TFIDF extends Kernel {

	public final static boolean DEBUG = false;
	//use weighted version or not
	public boolean _bReweightedSimilarity = false;
	
	
	/**corresponding to a specific query**/
	public double  _dDefaultIDF = -1d;
	//word and its idf
	public Map<Object, Double> _hmKey2IDF = null;
	//set of doc_name, which merely buffers doc_name and avoids duplicate assignment
	public Set<String> _mPrevInit = null;
	
	public TFIDF(HashMap<String,String> docs, boolean weighted_similarity) {
		//initialize member variable HashMap<String,String> _docs
		super(docs);
		//
		_bReweightedSimilarity = weighted_similarity;
	}

	// Kernels are reused by different MMR invocations
	// TODO: Is it OK to continue caching this information?
	public void clearInfoOfTopNDocs() {
		_dDefaultIDF = -1d;
		_hmKey2IDF = null;
		_mPrevInit = null;	
	}
	
	public void initTonNDocs(Set<String> docs_topn) {
		if (docs_topn == _mPrevInit)
			return; // Already initialized
		
		_mPrevInit = docs_topn;
		_hmKey2IDF = new HashMap<Object,Double>();
		for (String doc : docs_topn) {
			//!!!!!!!!!!!!!!??? initial step, should be "DocUtils.ConvertToFeatureMap(content)"
			Map<Object,Double> features = (Map<Object,Double>)getObjectRepresentation(doc);
			//word & 1 or 0
			//unique words in a doc
			features = VectorUtils.ConvertToBoolean(features);
			//the sum corresponds to the doc numbers including one particular word
			_hmKey2IDF = VectorUtils.Sum(_hmKey2IDF, features);
		}
		//computed idf for observed words
		for (Object key : _hmKey2IDF.keySet()) {
			Double idf = (docs_topn.size() + 1d)/ (_hmKey2IDF.get(key) + 1d);
			_hmKey2IDF.put(key, Math.log(idf));
		}
		//default idf for oov word
		_dDefaultIDF = Math.log((docs_topn.size() + 1d) / 1d);
		if (DEBUG)
			System.out.println("IDF after log: " + _hmKey2IDF);
	}
	//doc representation under the specific model
	//initial step: get word & its frequency
	//tfidf representation for doc step: means tf*idf {computed during the initial step}
	//differeniated by the input key
	//??? tfidf effective ???
	public Object getNoncachedObjectRepresentation(String content) {
		//word & word frequency
		Map<Object,Double> features = DocUtils.ConvertToFeatureMap(content);
		for (Object key : features.keySet())
			if (!_hmKey2IDF.containsKey(key))
				_hmKey2IDF.put(key, _dDefaultIDF);
		features = VectorUtils.ElementWiseMultiply(features, _hmKey2IDF);
		return features;
	}
	
	public double sim(Object o1, Object o2) {
		Map<Object, Double> s1 = (Map<Object, Double>)o1;
		Map<Object, Double> s2 = (Map<Object, Double>)o2;
		return VectorUtils.CosSim(s1, s2);
	}

	public double sim(Object o1, Object o2, Object ow) {

		Map<Object, Double> s1 = (Map<Object, Double>)o1;
		Map<Object, Double> s2 = (Map<Object, Double>)o2;

		if (_bReweightedSimilarity) { 			
			Map<Object, Double> w  = (Map<Object, Double>)ow;
			return VectorUtils.WeightedCosSim(s1, s2, w);
		} else
			return VectorUtils.CosSim(s1, s2);
	}

	@Override
	public String getObjectStringDescription(Object obj) {
		return obj.toString();
	}
	
	@Override
	public String getKernelDescription() {
		// TODO Auto-generated method stub
		return "TFIDF-Kernel (reweighted_sim=" + _bReweightedSimilarity + ")";
	}
	
	public String getString(){
		return "TFIDF";
	}
}
