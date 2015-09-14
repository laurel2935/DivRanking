/** Abstract class for a kernel specification...
 *  subclasses must implement the abstract methods.
 *   
 * @author Scott Sanner (ssanner@gmail.com)
 */

package org.archive.nicta.kernel;

import java.util.HashMap;
import java.util.Set;

import org.archive.util.Language;

/** 
 * (1)
 * A kernel denotes a space, under which the similarity or dissimilarity or distance of two input factors
 * or data points can be quantified.
 * i.e., k(q,d)=<\phi(q),\theta(d)>=\sum_{i}{\phi(q)_i * \theta(d)_i} 
 * k(q,d) can be symmetric or asymmetric...refer to {2010-Relevance Ranking using Kernels}
 * 
 * (2)
 * Specifically, the run of a kernel is query-specific.
 * _docs buffers all the top-n documents for all queries,
 * per query, initialize the top-n each time using a set<String>
 * thus, when run for all queries, we have to call : clear() & init() per query.
 *  **/

public abstract class Kernel {
	
	//HashMap as <doc_name,doc_content>, which buffers the raw document text (e.g., be filtered or parsed from html).
	public HashMap<String,String> _docs_all = null;
	//buffer of doc's representation within the kernel-space
	public HashMap<String,Object> _reprCache = new HashMap<String,Object>();
	//
	public Kernel(HashMap<String,String> docs_all) {
		_docs_all = docs_all;
	}
	//non-weighted similarity
	public abstract double sim(Object s1, Object s2);
	//commonly weighted similarity
	public abstract double sim(Object s1, Object s2, Object q);
	//set-level similarity
	public boolean supportsSetSim() { return false; }
	//
	public Double setSim(Object s1, Set<Object> s2, Object q) { return null; } 
	//
	public double distance(Object s1, Object s2){
		System.err.println("distance should be Overrided!");
		System.exit(1);
		return -1;
	}
	/**
	 * clear() & init() should be extended when the doc representation should be pre-calculated.
	 * e.g., LDA training, the calculation of idf of tf-idf model 
	 * 
	 * **/
	//e.g., for each query, it should be called
	public abstract void clearInfoOfTopNDocs();
	//e.g., for each query, it should be called
	public abstract void initTonNDocs(Set<String> docs_topn);
	/**
	 * Should only be called after init(...) has been called
	 **/
	public Object getObjectRepresentation(String doc_name) {
		Object doc_repr = null;
		
		if ((doc_repr = _reprCache.get(doc_name)) != null){
			return doc_repr;
		}
			
		String doc_content = _docs_all.get(doc_name);
		doc_repr = getNoncachedObjectRepresentation(doc_content);
		_reprCache.put(doc_name, doc_repr);
		return doc_repr;
	}
	//
	public abstract Object getNoncachedObjectRepresentation(String doc_content);
	//
	public abstract String getObjectStringDescription(Object obj);
	//
	public abstract String getKernelDescription();
	//
	public abstract String getString();
}
