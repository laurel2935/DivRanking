package org.archive.a1.search.another.search;

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * Estimate extended uniform cost algorithm.
 *
 * @author Remko Tron&ccedil;on
 */
public class EEUniformCostAlgorithm extends Algorithm
{
	public EEUniformCostAlgorithm(Graph graph) {
		super(graph);
	}
	
	
	public void doStep() {
		Path path = getQueue().removeFirst();
		List children = path.getChildren();
		getQueue().addFront(children);
		getQueue().sort(new PathComparator());
	}


	public boolean finished() {
		return (getQueue().isEmpty() || 
				getQueue().getFirst().contains(getGraph().getGoalNode()));
	}

	protected int computeCost(Path p) {
		int cost = 0;
		for (Iterator it = p.getEdges().iterator(); it.hasNext(); ) {
			try {
				cost += ((WeighedEdge) it.next()).getWeight();
			}
			catch (ClassCastException e) { }
		}
		
		return cost;
	}
		
	
	protected int computeFValue(Path p) {
		int h = 0;
		try {
			h = ((HeuristicNode) p.getEndNode()).getHeuristic();
		}
		catch (ClassCastException e) { }

		return h + computeCost(p);
	}
						
	
	/**
	 * Retrieves the textual representation of the state of the algorithm.
	 */
	public String getStateString() {
		return getQueue().toString(true,true,true);
	}


	private class PathComparator implements Comparator
	{
		public int compare(Object o1, Object o2) {
			return computeFValue((Path) o1) - computeFValue((Path) o2);
		}
	}
}
