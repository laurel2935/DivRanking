package org.archive.a1.search.another.search;

import java.util.List;


/**
 * A breadth first algorithm.
 *
 * @author Remko Tron&ccedil;on
 */
public class BreadthFirstAlgorithm extends Algorithm
{
	public BreadthFirstAlgorithm(Graph graph) {
		super(graph);
	}
	
	
	public void doStep() {
		Path p = getQueue().removeFirst();
		List children = p.getChildren();
		getQueue().addBack(children);
	}
}
