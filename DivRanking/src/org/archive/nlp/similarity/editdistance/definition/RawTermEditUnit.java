package org.archive.nlp.similarity.editdistance.definition;

import org.archive.util.Language.Lang;

public class RawTermEditUnit extends EditUnit {
	protected Term term = null;
	
	public RawTermEditUnit(String tTxt){
		this.term = new Term(tTxt);
	}
	
	public String getUnitString() {
		return term.getTerm();
	}
	
	@Override
	public double getSubstitutionCost(EditUnit otherUnit, Lang lang){
		if(!(otherUnit instanceof RawTermEditUnit)) return 1.0;
		if(equals(otherUnit)){
			return 0.0;
		}else{
			return 1.0;
		} 
	}
	//*/
	///*
	@Override	
	public boolean equals(Object other, Lang lang){
    	if(!(other instanceof RawTermEditUnit)) return false;
    	RawTermEditUnit otherUnit = (RawTermEditUnit)other;
    	Term otherTerm = otherUnit.term;
    	//
    	return null==this.term.getTerm()? null==otherTerm.getTerm() : this.term.getTerm().equals(otherTerm.getTerm());						
	}

}
