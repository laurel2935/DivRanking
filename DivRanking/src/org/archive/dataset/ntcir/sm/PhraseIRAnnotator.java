package org.archive.dataset.ntcir.sm;

import java.util.ArrayList;
import java.util.Vector;

import org.archive.util.Language.Lang;

/**
 * Np kernel-object
 * **/

public class PhraseIRAnnotator extends IRAnnotator {
	private final static boolean DEBUG = false;
	
	public IRAnnotation irAnnotate(ArrayList<ArrayList<TaggedTerm>> canTaggedPhraseList, IRAnnotation topicIRAnnotation){
		if(null == canTaggedPhraseList){
			return null;
		}
		
		ArrayList<Modifier> moSet = null;
    	KernelObject canKO = null;
    	boolean include = false;
		for(ArrayList<TaggedTerm> termList: canTaggedPhraseList){
			include = false;
			moSet = new ArrayList<Modifier>();
	    	canKO = null;
	    	for(TaggedTerm taggedTerm: termList){    		
	    		if(taggedTerm.koMatch(topicIRAnnotation.ko)){
	    			include = true;
	    			canKO = taggedTerm.toKernelObject();
	    		}else{
	    			moSet.add(taggedTerm.toModifier());
	    		}    		  		
	    	}
	    	if(include){
	    		break;
	    	}
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
	
	public ArrayList<IRAnnotation> irAnnotate(ArrayList<ArrayList<TaggedTerm>> taggedPhraseList, Lang lang){
		if(null == taggedPhraseList){
			return null;
		}		
		//
		ArrayList<IRAnnotation> irAnnotations = new ArrayList<IRAnnotation>();
		for(ArrayList<TaggedTerm> termList: taggedPhraseList){
			Vector<Integer> acceptPosIndex = new Vector<Integer>();
			for(int i=0; i<termList.size(); i++){
				if(accept(termList.get(i).posTag, lang)){
					acceptPosIndex.add(i);
				}
			}
			if(acceptPosIndex.size()>0){				
				for(int k=0; k<acceptPosIndex.size(); k++){
					IRAnnotation irAnnotation = getIRAnnotation(acceptPosIndex.get(k), termList);
					if(DEBUG){
						System.out.println("Annotation-"+(k+1)+":");
						System.out.println("\t"+irAnnotation.toString());					
					}
					irAnnotations.add(irAnnotation);
				}				
			}			
		}
		
		if(irAnnotations.size()>0){			
			return irAnnotations;
		}else{
			if(DEBUG){				
				System.out.println("No accepted term case!");
			}
			return null;
		}
	}
	
	protected boolean accept(String posTag, Lang lang){
		if(posTag.equals("NP")){
			return true;
		}else{
			return false;
		}
	}

}
