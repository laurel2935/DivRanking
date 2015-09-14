package org.archive.dataset.ntcir.sm;

import java.util.ArrayList;
/**
 * corresponding to a subtopic instance
 * **/
public class SubtopicInstance implements Comparable {
	//public static enum OddCase {NoMatchKO}
	//corresponding to each kernel-object of the topic
	//null will be added if it doesn't include the kernel-object
	public String _text;
	public int _fre;
	//public ArrayList<OddCase> oddCases = null;
	private boolean odd = true;
	
	public ArrayList<IRAnnotation> termIRAnnotationList;
	public ArrayList<IRAnnotation> phraseIRAnnotationList;
	
	public SubtopicInstance(String text){
		this._text = text;
		this.termIRAnnotationList = new ArrayList<IRAnnotation>();
		this.phraseIRAnnotationList = new ArrayList<IRAnnotation>();		
	}
	
	//
	public void addTermIRAnnotation(IRAnnotation irAnnotation){
		if(null!=irAnnotation && true==odd){
			odd = false;
		}
		this.termIRAnnotationList.add(irAnnotation);
	}
	//
	public void addPhraseIRAnnotation(IRAnnotation irAnnotation){
		if(null!=irAnnotation && true==odd){
			odd = false;
		}
		this.phraseIRAnnotationList.add(irAnnotation);
	}
	//
	public boolean belongToOddCase(){
		return odd;
	}
	//
	public boolean shrinkMatch(SubtopicInstance cmpSubtopicInstance){
		//term-level
		for(int i=0; i<this.termIRAnnotationList.size(); i++){
			IRAnnotation termIRAnnotation = this.termIRAnnotationList.get(i);
			IRAnnotation cmpTermIRAnnotation = cmpSubtopicInstance.termIRAnnotationList.get(i);
			if(null!=termIRAnnotation && null!=cmpTermIRAnnotation){
				if(termIRAnnotation.shrinkEquals(cmpTermIRAnnotation)){
					return true;
				}
			}
			
		}
		//phrase-level
		for(int j=0; j<this.phraseIRAnnotationList.size(); j++){
			IRAnnotation phraseIRAnnotation = this.phraseIRAnnotationList.get(j);
			IRAnnotation cmpPhraseIRAnnotation = cmpSubtopicInstance.phraseIRAnnotationList.get(j);
			if(null!=phraseIRAnnotation && null!=cmpPhraseIRAnnotation){
				if(phraseIRAnnotation.shrinkEquals(cmpPhraseIRAnnotation)){
					return true;
				}
			}		
		}
		return false;
	}
	//
	public String toString(){
		return this._text;
	}
	
	public int compareTo(Object o) {	
		SubtopicInstance cmp = (SubtopicInstance)o;
		if(this._fre > cmp._fre){
			return -1;
		}else if(this._fre < cmp._fre){
			return 1;
		}else{
			return 0;
		}
	}
}
