package org.archive.a1.search.another.search;

import java.util.List;

/**
 * A class for representing graphs (or search problems). 
 * 
 * @author Remko Tron&ccedil;on
 */
public abstract class Graph
{
	public abstract Node getStartNode();
	public abstract Node getGoalNode();
	
	/**
	 * Retrieves the outgoing edges of a given node.
	 *
	 * @param n The node to retrieve the outgoing edges from. Cannot be 
	 *			 <tt>null</tt>
	 * @return The list of outgoing edges.
	 */
	public abstract List getOutgoingEdges(Node n);
	
	
	/**
	 * Returns the initial path of the graph.
	 * For normal graphs, this is just the Path containing only the start node.
	 * 
	 * @return the initial path of the graph.
	 */
	public Path getInitialPath() {
		return new Path(this,getStartNode());
	}
}
