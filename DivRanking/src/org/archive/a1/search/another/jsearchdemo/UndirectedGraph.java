package org.archive.a1.search.another.jsearchdemo;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;

import org.archive.a1.search.another.search.Edge;
import org.archive.a1.search.another.search.Graph;
import org.archive.a1.search.another.search.Node;
import org.archive.a1.search.another.search.Path;


/**
 * A class for representing undirected graphs.
 * An undirected graph has a series of edges, nodes, a starting node, and an
 * endnode. Nodes are added implicitly by adding edges.
 * 
 * @author Remko Tron&ccedil;on
 */
public class UndirectedGraph extends Graph
{
	private Node _start;
	private Node _goal;

	/* A map which maps nodes to a list of all their children */
	private TreeMap _nodes = new TreeMap();

	
	/**
	 * Creates an empty undirected graph.
	 */
	public UndirectedGraph () {
	}


	/**
	 * Retrieves the outgoing edges of a node in the graph.
	 *
	 * @param n the node of which the outgoing edges are retrieved.
	 * @return The list of outgoing edges.
	 */
	public List getOutgoingEdges(Node n) {
		if (n != null && _nodes.containsKey(n))
			return (List) _nodes.get(n); 
		else
			return new LinkedList();
	}


	/**
	 * Retrieves the start node of the graph.
	 */
	public Node getStartNode() {
		return _start;
	}

	
	/**
	 * Retrieves the goal node of the graph.
	 */
	public Node getGoalNode() {
		return _goal;
	}

	
	/**
	 * Sets the start node of the graph.
	 *
	 * @param n the start node. Cannot be <tt>null</tt>.
	 */
	public void setStartNode(Node n) {
		// assert n != null;
		_start = n;
	}


	/**
	 * Sets the goal node of the graph.
	 *
	 * @param n the goal node. Cannot be <tt>null</tt>.
	 */
	public void setGoalNode(Node n) {
		// assert n != null;
		_goal = n;
	}

	
	/**
	 * Adds an edge from to the graph.
	 *
	 * @param edge The edge to add. Cannot be <tt>null</tt>.
	 */
	public void addEdge(Edge edge) {
		// assert edge != null;

		Node begin = edge.getBeginNode();
		Node end = edge.getEndNode();

		if (!_nodes.containsKey(begin)) 
			_nodes.put(begin,new LinkedList());
		// FIXME: Check if there already exists an edge
		((List) _nodes.get(begin)).add(edge);

		if (!_nodes.containsKey(end)) 
			_nodes.put(end,new LinkedList());
		// FIXME: Check if there already exists an edge
		((List) _nodes.get(end)).add(edge);
	}

	
	/**
	 * Removes an edge from the graph.
	 * If the edge is not in the graph, nothing is done.
	 * 
	 * @param edge The edge to remove.
	 */
	public void removeEdge(Edge edge) {
		if (edge == null) return;

		Node begin = edge.getBeginNode();
		Node end = edge.getEndNode();
		if (_nodes.containsKey(begin)) {
			((List) _nodes.get(begin)).remove(edge);
			if (((List) _nodes.get(begin)).isEmpty())
				_nodes.remove(begin);
		}
		if (_nodes.containsKey(end)) {
			((List) _nodes.get(end)).remove(edge);
			if (((List) _nodes.get(end)).isEmpty())
				_nodes.remove(end);
		}
	}


	/**
	 * Checks if the graph contains a given node.
	 * A node is in the graph if it is connected to another node in the
	 * graph.
	 *
	 * @param node the node to check
	 * @return <tt>true</tt> if the node is in (connected to) the graph, 
	 *		   <tt>false</tt> otherwise.
	 */
	public boolean contains(Node node) {
		if (node == null) return false;
		return _nodes.containsKey(node);
	}
	

	/**
	 * Removes a node and all its connected edges from the graph.
	 * If the node is not in the graph, nothing is done.
	 * If by deleting an edge another node becomes unconnected, it is
	 * removed from the graph.
	 * 
	 * @param node The node to remove
	 */
	public void removeNode(Node node) {
		if (node == null) return;
		if (_nodes.containsKey(node)) {
			for (Iterator it = ((List) _nodes.get(node)).iterator(); 
						it.hasNext(); ) {
				Edge e = (Edge) it.next();
				Node n = (e.getBeginNode().equals(node) ? 
								e.getEndNode() : e.getBeginNode());
				((List) _nodes.get(n)).remove(e);
				if (((List) _nodes.get(n)).isEmpty()) 
					_nodes.remove(n);
			}
			_nodes.remove(node);
		} 
	}


	/**
	 * Returns the textual representation of the graph.
	 */
	public String toString() {
		String s = new String();
		for (Iterator it=_nodes.keySet().iterator(); it.hasNext(); ) {
			Node n = (Node) it.next();
			s += n + ": ";
			for (Iterator jt=((List)_nodes.get(n)).iterator(); 
					jt.hasNext(); ) {
				s += ((Edge) jt.next()) + " ";
			}
			s += "\n";
		}		 
		return s;
	}
	
	
	/* (non-Javadoc)
	 * @see search.Graph#getInitialPath()
	 */
	public Path getInitialPath() {
		return new UndirectedPath(this,getStartNode());
	}
}
