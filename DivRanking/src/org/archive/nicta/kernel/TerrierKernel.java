package org.archive.nicta.kernel;

import java.util.HashMap;
import java.util.Set;

public class TerrierKernel extends Kernel{
	
	
	TerrierKernel(HashMap<String,String> docs){
		super(docs);
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
	
	//-- non-used part --//
	public void clearInfoOfTopNDocs() {
		
	}	
	public void initTonNDocs(Set<String> docs_topn) {
		
	}
	public Object getNoncachedObjectRepresentation(String content) {
		return null;
	}
	public double sim(Object objQuery, Object objDoc) {
		return Double.NaN;
	}
	public double sim(Object o1, Object o2, Object ow) {
		return Double.NaN;
	}
	@Override
	public String getObjectStringDescription(Object obj) {
		return obj.toString();
	}
	//-- non-used part --//
	@Override
	public String getKernelDescription() {
		return "TerrierKernel";
	}
	public String getString(){
		return "TerrierKernel";
	}

}
