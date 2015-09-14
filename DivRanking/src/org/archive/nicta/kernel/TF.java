/** Term Frequency (TF) Kernel
 *   
 * @author Scott Sanner (ssanner@gmail.com)
 */

package org.archive.nicta.kernel;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.archive.util.DocUtils;
import org.archive.util.Language;
import org.archive.util.VectorUtils;



public class TF extends Kernel {

	public boolean _bReweightedSimilarity = false;
	
	public TF(HashMap<String,String> docs, boolean weighted_similarity) {
		super(docs);
		_bReweightedSimilarity = weighted_similarity;
	}
	
	public void clearInfoOfTopNDocs() {
		// Nothing to clear
	}

	public void initTonNDocs(Set<String> docs_topn) {
		// Nothing to init
	}

	public Object getNoncachedObjectRepresentation(String content) {
		Map<Object, Double> features = DocUtils.ConvertToFeatureMap(content);
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
		return "TF-Kernel (reweighted_sim=" + _bReweightedSimilarity + ")";
	}
	
	public String getString(){
		return "TF";
	}
}
