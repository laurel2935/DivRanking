package org.archive.nicta.kernel;

import java.util.HashMap;
import java.util.Set;

import org.archive.terrier.TrecScorer;

public class TerrierKernel extends Kernel{
	TrecScorer _trecScorer;
	
	public TerrierKernel(HashMap<String, String> docs, TrecScorer trecScorer){
		super(docs);
		
		this._trecScorer = trecScorer;
	}
	
	//-- based on Terrier access --//
	/**
	 * get the relevance probability
	 * @param qText query text
	 * @param docName document name
	 * **/
	public double getRelePro(String qText, String docName){
		return Double.NaN;
	}
	
	//---- non-used part ----//
	public void clearInfoOfTopNDocs() {
		
	}	
	public void initTonNDocs(Set<String> docs_topn) {
		
	}
	public Object getNoncachedObjectRepresentation(String content) {
		return content;
	}
	public double sim(Object objQuery, Object objDoc) {
		//return Double.NaN;
		double relePro = _trecScorer.socre((String)objQuery, (String)objDoc);
		//System.out.println(relePro);
		return relePro;
	}
	public double sim(Object o1, Object o2, Object ow) {
		return Double.NaN;
	}
	@Override
	public String getObjectStringDescription(Object obj) {
		return obj.toString();
	}
	//---- non-used part ----//
	
	
	@Override
	public String getKernelDescription() {
		return "TerrierKernel";
	}
	public String getString(){
		return "TerrierKernel";
	}

}
