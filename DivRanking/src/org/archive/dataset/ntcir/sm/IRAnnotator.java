package org.archive.dataset.ntcir.sm;

import java.util.ArrayList;
import java.util.Vector;

import org.archive.util.Language.Lang;

public abstract class IRAnnotator {
	private final static boolean DEBUG = false;
	
	//
	protected IRAnnotation getIRAnnotation(int koIndex, ArrayList<TaggedTerm> topicTaggedTerms){
		ArrayList<Modifier> moSet = new ArrayList<Modifier>();
		KernelObject ko = topicTaggedTerms.get(koIndex).toKernelObject();
		for(int i=0; i<topicTaggedTerms.size(); i++){
			if(i != koIndex){
				moSet.add(topicTaggedTerms.get(i).toModifier());
			}
		}
		return new IRAnnotation(ko, moSet);
	}
	//
	protected abstract boolean accept(String posTag, Lang lang);
}
