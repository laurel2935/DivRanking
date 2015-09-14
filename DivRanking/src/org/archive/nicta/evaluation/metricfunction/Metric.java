/** Loss function interface for diversity
 *   
 * @author Scott Sanner (ssanner@gmail.com)
 */

package org.archive.nicta.evaluation.metricfunction;

import java.util.List;

import org.archive.dataset.trec.query.TRECQueryAspects;

public abstract class Metric {
	
	public int cutoff = -1;

	public String getName() {
		String[] split = this.getClass().toString().split("[\\.]");
		return split[split.length - 1];
	}
	
	public abstract Object eval(TRECQueryAspects qa, List<String> docs);
	
	public abstract Object getMetricArray();
}
