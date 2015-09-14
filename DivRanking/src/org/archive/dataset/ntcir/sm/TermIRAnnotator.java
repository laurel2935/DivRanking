package org.archive.dataset.ntcir.sm;

import java.util.ArrayList;
import java.util.Vector;

import org.archive.util.Language.Lang;

/**
 * NN or NR kernel-object
 * **/

public class TermIRAnnotator extends IRAnnotator {	
	private final static boolean DEBUG = false;
	
	public IRAnnotation irAnnotate(ArrayList<TaggedTerm> canTaggedTerms, IRAnnotation topicIRAnnotation){
		if(null == canTaggedTerms){
			return null;
		}
		//
		boolean include = false;
		ArrayList<Modifier> moSet = new ArrayList<Modifier>();
    	KernelObject canKO = null;
    	for(TaggedTerm taggedTerm: canTaggedTerms){    		
    		if(taggedTerm.koMatch(topicIRAnnotation.ko)){
    			include = true;
    			canKO = taggedTerm.toKernelObject();
    		}else{
    			moSet.add(taggedTerm.toModifier());
    		}    		  		
    	}
    	if(DEBUG){
    		System.out.println("Target:\t"+canTaggedTerms);
    		System.out.println("Reference Topic Annotation:\t"+topicIRAnnotation.toString());
    	}
    	if(include){
    		IRAnnotation canIrAnnotation = new IRAnnotation(canKO, moSet);
    		if(DEBUG){
    			//CO-KO Subtopic String IRA:	ko:[泰国 ns] MOSet:  特产 n 哪里 r 买 v 最 d 便宜 a
    			System.out.println("CO-KO Subtopic String IRA:\t"+canIrAnnotation.toString());
    		}
    		return canIrAnnotation;
    	}else {
    		if(DEBUG){
    			System.out.println("No CO-KO IRA!");
    		}
			return null;
		}
	}
	
	public ArrayList<IRAnnotation> irAnnotate(ArrayList<TaggedTerm> topicTaggedTerms, Lang lang){
		if(null == topicTaggedTerms){
			return null;
		}
		if(DEBUG){
			System.out.println("IRAnnotation for Topic:\t"+topicTaggedTerms);
		}
		//
		Vector<Integer> acceptPosIndex = new Vector<Integer>();
		for(int i=0; i<topicTaggedTerms.size(); i++){
			if(accept(topicTaggedTerms.get(i).posTag, lang)){
				acceptPosIndex.add(i);
			}
		}
		//
		if(acceptPosIndex.size()>0){
			ArrayList<IRAnnotation> irAnnotations = new ArrayList<IRAnnotation>();
			//
			for(int k=0; k<acceptPosIndex.size(); k++){
				IRAnnotation irAnnotation = getIRAnnotation(acceptPosIndex.get(k), topicTaggedTerms);
				if(DEBUG){
					System.out.println("Annotation-"+(k+1)+":");
					System.out.println("\t"+irAnnotation.toString());					
				}
				irAnnotations.add(irAnnotation);
			}
			return irAnnotations;
		}else{
			if(DEBUG){				
				System.out.println("No accepted term case!");
			}
			return null;
		}
	}	
		
	protected boolean accept(String posTag, Lang lang){
		if(Lang.English == lang){
			if(posTag.startsWith("NN") || posTag.startsWith("NR")
					|| posTag.startsWith("VB")|| posTag.startsWith("JJ")){
				return true;
			}else{
				return false;
			}
		}else{
			if(posTag.startsWith("n") || posTag.startsWith("v")){
				return true;
			}else{
				return false;
			}
		}		
	}

}
