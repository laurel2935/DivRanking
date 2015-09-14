package org.archive.nicta.evaluation.metricfunction;

import java.util.ArrayList;
import java.util.List;

import org.archive.dataset.trec.query.TRECQueryAspects;

public class MSnDCG extends Metric {
	
	public Object eval(TRECQueryAspects qa, List<String> docs) {		
		return null;
	}
	
	public Object calMSnDCG(int subtopicIndex, TRECQueryAspects qa, ArrayList<String> docs, int predefinedCutoff) {
		if(0 > cutoff){
			cutoff = predefinedCutoff;
		}		
		return qa.calMSnDCG(subtopicIndex, docs, predefinedCutoff);			
	}
	
	public Object getMetricArray(){
		String [] metricArray = new String[cutoff];
		for(int i=0; i<cutoff; i++){
			metricArray[i] = "MSnDCG@"+Integer.toString(i+1);
		}
		return metricArray;
	}
}
