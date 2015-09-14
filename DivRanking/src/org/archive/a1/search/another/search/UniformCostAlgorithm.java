package org.archive.a1.search.another.search;

import java.util.List;
import java.util.Iterator;
import java.util.Comparator;

/**
 * Uniform Cost algorithm.
 *
 * @author Remko Tron&ccedil;on
 */
public class UniformCostAlgorithm extends Algorithm
{
	/**
	 * Constructs a new algorithm.
	 *
	 * @param graph The target graph. Cannot be <tt>null</tt>.
	 */
	public UniformCostAlgorithm(Graph graph) {
		super(graph);
	}
	
	
	public void doStep() {
		Path path = getQueue().removeFirst();
		List children = path.getChildren();
		getQueue().addFront(children);
		getQueue().sort(new PathComparator());
	}


	private int computeCost(Path p) {
		int cost = 0;
		for (Iterator it = p.getEdges().iterator(); it.hasNext(); ) {
			try {
				cost += ((WeighedEdge) it.next()).getWeight();
			}
			catch (ClassCastException e) { }
		}
		
		return cost;
	}
		
	
	/**
	 * Retrieves the textual representation of the state of the algorithm.
	 */
	public String getStateString() {
		return getQueue().toString(true,true,false);
	}
	
	
	private class PathComparator implements Comparator
	{
		public int compare(Object o1, Object o2) {
			return computeCost((Path) o1) - computeCost((Path) o2);
		}
	}
}
