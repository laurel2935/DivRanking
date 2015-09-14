package org.archive.nlp.qpunctuation;

/**
 * A string segment
 * **/

public class Segment {
	//
	public String str;
	//
	public Segment(String str){
		this.str = str;
	}
	//
	public final String getStr(){
		return this.str;
	} 
	//
	public boolean equals(Object obj){
		if (this == obj) {
			return true;
		}
		if (null == obj) {
			return false;
		}
		if (!(obj instanceof Segment)) {
			return false;
		}
		Segment other = (Segment)obj;
		//
		return other.str.equals(str);
	}
	@Override
	public int hashCode(){
		return this.str.hashCode();		
	}

}
