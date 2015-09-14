package org.archive.nicta.evaluation.metricfunction;

import java.util.List;

import org.archive.dataset.trec.query.TRECQueryAspects;

public class SubtopicRecall extends Metric {
	@Override
	public Object eval(TRECQueryAspects qa, List<String> docs) {
		
		if(0 > cutoff){
			cutoff = docs.size();
		}
		
		double scores[] = new double[docs.size()];
		for (int r = 1; r <= docs.size(); r++){
			scores[r-1] = qa.getUniformSubtopicRecall(docs, r); 
		}			
		
		return scores;
	}
	
	public Object eval(TRECQueryAspects qa, List<String> docs, int predefinedCutoff) {
		
		if(0 > cutoff){
			cutoff = predefinedCutoff;
		}
		
		double scores[] = new double[predefinedCutoff];
		for (int r = 1; r <= docs.size(); r++){
			scores[r-1] = qa.getUniformSubtopicRecall(docs, r); 
		}			
		
		return scores;
	}
	
	public Object getMetricArray(){
		String [] metricArray = new String[cutoff];
		for(int i=0; i<cutoff; i++){
			metricArray[i] = "SRecall@"+Integer.toString(i+1);
		}
		return metricArray;
	}
	
	public Object getMetricArray_Ratio(){
		String [] metricArray = new String[cutoff];
		for(int i=0; i<cutoff; i++){
			metricArray[i] = "URatio@"+Integer.toString(i+1);
		}
		return metricArray;
	}
	
	public Object utilityEval(TRECQueryAspects qa, List<String> docs, int predefinedCutoff) {
		
		if(0 > cutoff){
			cutoff = predefinedCutoff;
		}
		
		double scores[] = new double[predefinedCutoff];
		for (int r = 1; r <= docs.size(); r++){
			scores[r-1] = qa.getUtilityRatio(docs, r); 
		}			
		
		return scores;
	}
}
