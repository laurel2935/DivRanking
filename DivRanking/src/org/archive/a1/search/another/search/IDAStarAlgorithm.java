package org.archive.a1.search.another.search;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;


/**
 * IDA* algorithm.
 *
 * @author Remko Tron&ccedil;on
 */
public class IDAStarAlgorithm extends EEUniformCostAlgorithm
{
	private int f_bound;
	private int f_new = Integer.MAX_VALUE;
	private boolean fixedPointReached = false;
	 
	public IDAStarAlgorithm(Graph graph) {
		super(graph);
		f_bound = ((HeuristicNode) graph.getStartNode()).getHeuristic();
	}
	
	public void doStep() {
		if (!getQueue().isEmpty()) {
			Path path = getQueue().removeFirst();
			List children = path.getChildren();
				
			/* Remove paths which have f(path) > f_bound */
			LinkedList newChildren = new LinkedList();
			for (Iterator i = children.iterator(); i.hasNext(); ) {
				Path p = (Path) i.next(); 
				int f = computeFValue(p);
				if (f > f_bound) 
					f_new = (f_new > f ? f : f_new);
				else 
					newChildren.addFirst(p);
			}
			
			getQueue().addFront(newChildren);
		}
		else {
			if (! (f_bound == f_new)) {
				/* Reset the queue */
				getQueue().addFront(new Path(getGraph(),
											 getGraph().getStartNode()));

				/* Reset f bounds values */
				f_bound = f_new;
				f_new = Integer.MAX_VALUE;
			}
			else
				fixedPointReached = true;
		}
	}

	public boolean finished() {
		return (fixedPointReached || getQueue().goalReached());
	}
	
	
	/**
	 * Retrieves the textual representation of the state of the algorithm.
	 */
	public String getStateString() {
		return "<FONT SIZE=-1>[f_bound:" + f_bound + 
			   ",f_new:" 
			   + (f_new == Integer.MAX_VALUE ? 
					"inf" : Integer.toString(f_new))
			   + "]</FONT> " + super.getStateString();
	}
}
