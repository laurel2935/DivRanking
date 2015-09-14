package org.archive.nlp.similarity.editdistance.definition;

import org.archive.nlp.similarity.hownet.concept.LiuConceptParser;
import org.archive.nlp.similarity.wordnet.WordNetSimilarity;
import org.archive.util.Language.Lang;

/**
 * no pos tag
 * **/

public class SemanticTermEditUnit  extends EditUnit {
	protected Term term = null;
	
	public SemanticTermEditUnit(String tTxt){
		this.term = new Term(tTxt);
	}
	
	//semantic similarity of two terms are considered
	@Override
	public double getSubstitutionCost(EditUnit otherUnit, Lang lang){
		if(!(otherUnit instanceof SemanticTermEditUnit)) return 1.0;
		if(equals(otherUnit)) return 0.0;
		
		SemanticTermEditUnit other = (SemanticTermEditUnit)otherUnit;
		
		if(Lang.Chinese == lang){
			return 1 - LiuConceptParser.getInstance().getSimilarity(getUnitString(), other.getUnitString());
		}else{
			return 1 - WordNetSimilarity.JCSimilarity_Average(getUnitString(), other.getUnitString());
		}
	}
	
	@Override
	public boolean equals(Object other, Lang lang){
    	if(!(other instanceof SemanticTermEditUnit)) return false;
    	SemanticTermEditUnit otherUnit = (SemanticTermEditUnit)other;
    	//
    	if(Lang.Chinese == lang){
    		double sim = LiuConceptParser.getInstance().getSimilarity(getUnitString(), otherUnit.getUnitString());
    		return sim>0.5;
    	}else{
    		double sim = WordNetSimilarity.JCSimilarity_Average(getUnitString(), otherUnit.getUnitString());
    		return sim>0.5;
    	}		
	}
	
	public String getUnitString() {
		return term.getTerm();
	}

}
