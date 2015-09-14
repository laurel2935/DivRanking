package org.archive.nlp.similarity.editdistance.definition;

public class Term {
	//
	private String str;
	//
	private String pos = null;
	
	public Term(String tTxt, String pos){
		this.str = tTxt;
		this.pos = pos;
	}	
	public Term(String tTxt){
		this.str = tTxt;
	}
	public String getTerm() {
		return str;
	}
	public void setTerm(String tTxt) {
		this.str = tTxt;
	}
	public String getPos() {
		return pos;
	}
	public void setPos(String pos) {
		this.pos = pos;
	}			
}