package org.archive.a1.search.another.search;

import java.util.Iterator;
import java.util.List;


/**
 * A* algorithm.
 *
 * @author Remko Tron&ccedil;on
 */
public class AStarAlgorithm extends EEUniformCostAlgorithm
{
	public AStarAlgorithm(Graph graph) {
		super(graph);
	}
	
	
	public void doStep() {
		/* Execute the estimation extended uniform cost algorithm */
		super.doStep();

		/* Prune queue */
		List paths = getQueue().getPaths();
		for (Iterator i = paths.iterator(); i.hasNext(); ) {
			Path p = (Path) i.next();
			for (Iterator j = paths.iterator(); j.hasNext(); ) {
				Path q = (Path) j.next(); 
				if (!p.equals(q) && q.contains(p.getEndNode()) 
						&& computeCost(p) > computeCost(q)) {
					getQueue().remove(p); 
				}
			}
		}
	}
}
