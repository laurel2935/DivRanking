package org.archive.nlp.qpunctuation;

/**
 * a real instance surrounded by symmetric punctuation
 * */
public class SPunctuationInstance implements Comparable{
	int headIndex;
	int endIndex;
	String symmetricP;
	String innerStr;
	//
	boolean doubleCase = false;
	//
	SPunctuationInstance(int headIndex, int endIndex,String symmetricP, String innerStr){
		this.headIndex = headIndex;
		this.endIndex = endIndex;
		this.symmetricP = symmetricP;
		this.innerStr = innerStr;
	}
	//
	public void setCase(boolean state){
		this.doubleCase = state;
	}
	//
	public  int compareTo(Object o){
		SPunctuationInstance comp = (SPunctuationInstance)o;
		if(this.headIndex > comp.headIndex){
			return 1;
		}else if(this.headIndex < comp.headIndex){
			return -1;
		}else{
			return 0;
		}
	}
}

