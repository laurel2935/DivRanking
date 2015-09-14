package org.archive.a1.kernel;

import java.util.HashMap;

import org.archive.nicta.kernel.LDAKernel;

/**
 * Subtopic Kernel, i.e, within this kernel (subtopic space),
 * each data point is represented a subtopic vector.
 * */

public class SBKernel extends LDAKernel {
	
	public SBKernel(HashMap<String,String> docs, int num_topics){
		//by setting spherical=true, weighted_similarity=false
		super(docs, num_topics, true, false);
	}
	//
	/**
	 * @return the distance between two points, i.e., 1 minus cosine similarity;
	 * */
	public double distance(Object s1, Object s2){
		return 1-sim(s1, s2);		
	}

}
