/** Loss function implementation for diversity evaluation
 *   
 * @author Scott Sanner (ssanner@gmail.com)
 */

package org.archive.nicta.evaluation.metricfunction;

import java.util.List;

import org.archive.dataset.trec.query.TRECQueryAspects;



public class AllUSLoss_A1 extends Metric {

	@Override
	public Object eval(TRECQueryAspects qa, List<String> docs) {
		
		if(0 > cutoff){
			cutoff = docs.size();
		}
		
		double scores[] = new double[docs.size()];
		for (int r = 1; r <= docs.size(); r++)
			scores[r-1] = qa.getUniformSubtopicLoss_A1(docs, r); 
		
		return scores;
	}
	
	public Object getMetricArray(){
		String [] metricArray = new String[cutoff];
		for(int i=0; i<cutoff; i++){
			metricArray[i] = "USL@"+Integer.toString(i+1);
		}
		return metricArray;
	}

}
