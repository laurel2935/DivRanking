package org.archive.a1.search.another.search;

/**
 * A class to represent a basic edge.
 * Each edge has a begin- and an endnode.
 *
 * @author Remko Tron&ccedil;on
 */
public class Edge
{
	private Node _begin;
	private Node _end;

	
	/**
	 * Constructs an edge.
	 *
	 * @param begin the begin node of the edge. Cannot be <tt>null</tt>.
	 * @param end the end node of the edge. Cannot be <tt>null</tt>.
	 */
	public Edge(Node begin, Node end) {
		//assert (begin != null && end != null);
		_begin = begin;
		_end = end;    
	}
	

	/** 
	 * Retrieves the begin node of the edge.
	 */
	public Node getBeginNode() {
		return _begin;
	}


	/**
	 * Retrieves the end node of the edge.
	 */
	public Node getEndNode() {
		return _end;
	}


	/**
	 * Compares the edge to an objects.
	 *
	 * @param o the object to compare with. 
	 * @return <tt>true</tt> if <tt>o</tt> is an edge with the same begin
	 *		   and end node as the current.
	 */
	public boolean equals(Object o) {
		if ( o == null) 
			return false;
		try {
			return ((Edge) o).getBeginNode().equals(getBeginNode()) 
				&& ((Edge) o).getEndNode().equals(getEndNode());
		}
		catch (ClassCastException e) {
			return false;
		}
	}


	/**
	 * Returns the string representation of this edge.
	 */
	public String toString() {
		return _begin.toString() + "-" + _end.toString();
	}
}
