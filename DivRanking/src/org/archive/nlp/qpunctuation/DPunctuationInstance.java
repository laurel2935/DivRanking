package org.archive.nlp.qpunctuation;

/**
 * dot punctuation instance
 * */

public class DPunctuationInstance implements Comparable{
	int index;
	String symbol;
	DPunctuationInstance(String separator, int index){
		this.symbol = separator;
		this.index = index;
	}
	public  int compareTo(Object o){
		DPunctuationInstance comp = (DPunctuationInstance)o;
		if(this.index > comp.index){
			return 1;
		}else if(this.index < comp.index){
			return -1;
		}else{
			return 0;
		}
	}
}
