package org.archive.nlp.similarity.editdistance;

import org.archive.nlp.similarity.Similaritable;
import org.archive.nlp.similarity.editdistance.definition.EditUnit;
import org.archive.nlp.similarity.editdistance.definition.SuperString;
import org.archive.util.Language.Lang;


public abstract class EditDistance implements Similaritable {
        
    public abstract double getEditDistance(SuperString<? extends EditUnit> S, SuperString<? extends EditUnit> T, Lang lang);    
 
    /*
    public double getSimilarity(String s1, String s2){
    	
    	SuperString<SemanticTermEditUnit> S = SuperString.createSemanticTermSuperString(s1);
    	SuperString<SemanticTermEditUnit> T = SuperString.createSemanticTermSuperString(s2);
    	
    	return 1-(getEditDistance(S, T))/(Math.max(S.length(), T.length()));
    	
    }
    */
}
