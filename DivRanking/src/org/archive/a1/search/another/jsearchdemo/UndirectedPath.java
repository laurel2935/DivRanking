package org.archive.a1.search.another.jsearchdemo;

import java.util.List;
import java.util.LinkedList;
import java.util.Iterator;

import org.archive.a1.search.another.search.Edge;
import org.archive.a1.search.another.search.Graph;
import org.archive.a1.search.another.search.HeuristicNode;
import org.archive.a1.search.another.search.Node;
import org.archive.a1.search.another.search.Path;
import org.archive.a1.search.another.search.WeighedEdge;



/**
 * A class for paths for undirected graphs.
 * 
 * @author Remko Tron&ccedil;on
 */
public class UndirectedPath extends Path
{
	private LinkedList _nodes = new LinkedList();
		
	/**
	 * Constructs a path.
	 *
	 * @param graph The graph to which this path belongs. Cannot be
	 *				<tt>null</tt>.
	 * @param start The start node of the path. Cannot be <tt>null</tt>.
	 */ 
	public UndirectedPath(Graph graph, Node start) {
		super(graph,start);
		_nodes.add(start);
	}
	
	
	public UndirectedPath(UndirectedPath p) {
		super(p);
		_nodes = (LinkedList) p._nodes.clone();
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
		List childrenEdges = getGraph().getOutgoingEdges(getEndNode());
		for (Iterator it = childrenEdges.iterator(); it.hasNext(); ) {
			Edge e = (Edge) it.next();
			/* Loop check */
			if (!contains(e.getBeginNode()) 
					|| !contains(e.getEndNode())) {
				UndirectedPath p = (UndirectedPath) clone();
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
		return (Node) _nodes.getLast();
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
		return _nodes.contains(n);
	}


	/**
	 * Adds an edge to the end of this path.
	 *
	 * @param e the edge to add.
	 */
	protected void addEdge(Edge e) {
		if (e == null) return;
		if (getEndNode().equals(e.getBeginNode()))
			_nodes.add(e.getEndNode());
		else if (getEndNode().equals(e.getEndNode())) 
			_nodes.add(e.getBeginNode());
		else {
			throw new IllegalArgumentException("Path.addEdge(): Invalid edge");
		}
		super.addEdge(e);
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
		for (Iterator it = _nodes.iterator(); it.hasNext(); ) {
			s += it.next().toString();
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

	public Object clone() {
		return new UndirectedPath(this);
	}
}
