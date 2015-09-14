package org.archive.dataset.ntcir.sm;

public class TaggedTerm {
	public String termStr;
	public String posTag;
	
	public TaggedTerm(String termStr, String posTag){
		this.termStr = termStr;
		this.posTag = posTag;
	}
	//
	public boolean koMatch(KernelObject ko){
		/*
		if(termStr.equals(ko.koStr) && posTag.equals(ko.posTag)){
			return true;
		}else {
			return false;
		}
		*/
		if(termStr.equals(ko.koStr)){
			return true;
		}else {
			return false;
		}
	}
	//
	public Modifier toModifier(){
		return new Modifier(this.termStr, this.posTag);
	}
	//
	public KernelObject toKernelObject(){
		return new KernelObject(this.termStr, this.posTag);
	}
	//
	public String toString(){
		return termStr+"/"+posTag;
	}

}
