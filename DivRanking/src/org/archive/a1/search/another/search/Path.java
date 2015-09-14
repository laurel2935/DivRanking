package org.archive.a1.search.another.search;

import java.util.List;
import java.util.LinkedList;
import java.util.Iterator;


/**
 * A class for paths.
 * 
 * @author Remko Tron&ccedil;on
 */
public class Path implements Cloneable
{
	private Node _startNode;
	private LinkedList _edges = new LinkedList();
	private Graph _graph;

	
	/**
	 * Constructs a path.
	 *
	 * @param graph The graph to which this path belongs. Cannot be
	 *				<tt>null</tt>.
	 * @param start The start node of the path. Cannot be <tt>null</tt>.
	 */ 
	public Path(Graph graph, Node start) {
		// assert graph != null && start != null;
		_graph = graph;
		_startNode = start;
	}
	
	/**
	 * Constructs a copy of a path.
	 * 
	 * @param p the path to copy
	 */
	public Path(Path p) {
		_graph = p._graph;
		_startNode = p._startNode;
		_edges = (LinkedList) p._edges.clone();
	}
	
	
	/**
	 * Returns all the paths which can be constructed by adding a 
	 * child node to the last node of this path. The loop paths 
	 * are automatically removed.
	 *
	 * @return The list of children paths.
	 */
	public List getChildren() {
		LinkedList childrenPaths = new LinkedList();
		List childrenEdges = _graph.getOutgoingEdges(getEndNode());
		for (Iterator it = childrenEdges.iterator(); it.hasNext(); ) {
			Edge e = (Edge) it.next();
			/* Loop check */
			if (!contains(e.getEndNode())) {
				Path p = (Path) clone();
				p.addEdge(e);
				childrenPaths.add(p);
			}
		}

		return childrenPaths;
	}

	
	/**
	 * Retrieves the last node of this path.
	 */
	public Node getEndNode() {
		if (_edges.isEmpty())
			return _startNode;		
		else
			return ((Edge) _edges.getLast()).getEndNode();
	}


	/**
	 * Checks if a given node is in this path.
	 *
	 * @param n The node to check. 
	 * @return <tt>true</tt> if the node is in the path, <tt>false</tt>
	 *		   otherwise.
	 */
	public boolean contains(Node n) {
		if (n==null) return false;
		if (_startNode.equals(n)) return true;
		for (Iterator it = _edges.iterator(); it.hasNext();) {
			if (((Edge) it.next()).getEndNode().equals(n))
				return true;
		}
		return false;
	}


	/**
	 * Adds an edge to the end of this path.
	 * Assumes that the edge can be added to the path.
	 *
	 * @param e the edge to add.
	 * @throws IllegalArgumentException 
	 *		if <tt>e</tt> is <tt>null</tt>
	 */
	protected void addEdge(Edge e) {
		if (e == null)
			throw new IllegalArgumentException("Edge is null");
	   
		_edges.add(e);
	}


	/**
	 * Retrieves all the edges in this path.
	 *
	 * @return A list of edges.
	 */
	public List getEdges() {
		return (List) _edges.clone();
	}


	/**
	 * Returns the textual representation of the path.
	 */
	public String toString() {
		return toString(false,false,false);
	}


	/**
	 * Returns the textual representation of the path, with specified 
	 * information.
	 */
	public String toString(boolean heuristic, boolean cost, boolean f_value) {
		String s = new String("<B>");
		
		s += _startNode.toString();
		for (Iterator it = _edges.iterator(); it.hasNext(); ) {
			s += ((Edge) it.next()).getEndNode().toString();
		}
		s += "</B>";
		
		int h = 0;
		if (getEndNode() instanceof HeuristicNode) {
			h = ((HeuristicNode) getEndNode()).getHeuristic();
		}
		
		int c = 0;
		for (Iterator it = getEdges().iterator(); it.hasNext(); ) {
			Edge e = (Edge) it.next();
			if (e instanceof WeighedEdge) 
				c += ((WeighedEdge) e).getWeight();
		}

		if (heuristic || cost || f_value) {
			s += " <FONT SIZE=-1><I>(";
			if (heuristic) 
				s += "H:" + h + (cost || f_value ? "," : "");
			if (cost) 
				s += "C:" + c + (f_value ? "," : "");
			if (f_value) 
				s += "F:" + (c+h);
			s += ")</I></FONT>";
		}
		return s;
	}


	/**
	 * Creates a copy of this path.
	 */
	public Object clone() {
		return new Path(this);
	}


	/**
	 * Checks if this path reaches the goal.
	 * 
	 * @return <tt>true</tt> if this path reaches the goal.
	 */
	public boolean goalReached() {
		return contains(_graph.getGoalNode());
	}
	
	/**
	 * Returns the graph associated with this path.
	 * 
	 * @return the graph to which this path belongs.
	 */
	protected Graph getGraph() {
		return _graph;
	}
}
