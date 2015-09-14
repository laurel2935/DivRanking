package org.archive.nlp.lcs;

import java.util.Vector;

/**
 * common substring vector among two consecutive queries
 * **/
public class LCSSet {
	//unique common_substring between two consecutive queries
	public Vector<String> csVec;
	//
	public LCSSet(Vector<String> opVec){
		this.csVec = opVec;
	}
}
