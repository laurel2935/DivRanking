package org.archive.nlp.similarity;

import java.util.ArrayList;

import org.archive.dataset.ntcir.sm.SMSubtopicItem;

public abstract class SimilarityFunction {	
	//
	public abstract double calSimilarity(ArrayList<String> moSet_A, ArrayList<String> moSet_B);
	
	//
	public abstract double calSimilarity(SMSubtopicItem smSubtopicItem_A, SMSubtopicItem smSubtopicItem_B);
}
