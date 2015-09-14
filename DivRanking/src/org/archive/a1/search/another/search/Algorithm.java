package org.archive.a1.search.another.search;

import java.util.LinkedList;


/**
 * The base class for a search algorithm.
 * Each search algorithm is constructed with a graph as its target. 
 *
 * @author Remko Tron&ccedil;on
 */
public abstract class Algorithm 
{
	private LinkedList _listeners = new LinkedList();
	private Graph _graph;
	private Queue _queue = new Queue();

	/**
	 * Basic constructor.
	 * Sets target graph of the algorithm and initializes the queue with
	 * the start node of the graph.
	 * 
	 * @param graph the graph on which the algorithm is applied.
	 *              Cannot be <tt>null</tt>
	 */
	public Algorithm(Graph graph) {
		//assert graph != null;
		_graph = graph;
		_queue.addFront(getGraph().getInitialPath());
	}

    
	/**
	 * Executes one loop of the algorithm.
	 * If the algorithm is finished, nothing is done.
	 */ 
	public void step() { 
		if (!finished()) 
			doStep();
	}

	/**
	 * Executes one loop of the algorithm.
	 */
	protected abstract void doStep();
    
	/**
	 * Checks if the algorithm is finished.
	 *
	 * @return <tt>true</tt> if the algorithm is finished (i.e. the queue is 
	 *			empty or the goal is reached), false otherwise.
	 */
	public boolean finished() {
		return (getQueue().isEmpty() || getQueue().goalReached());
	}
	
	
	/**
	 * Retrieves the graph the algorithm is working on.
	 */
	public Graph getGraph() { 
		return _graph; 
	}
	
	
	/**
	 * Retrieves the current queue of the algorithm.
	 */
	public Queue getQueue() { 
		return _queue; 
	}

	/**
	 * Retrieves the textual representation of the state of the algorithm.
	 */
	public String getStateString() {
		return getQueue().toString();
	}
}
