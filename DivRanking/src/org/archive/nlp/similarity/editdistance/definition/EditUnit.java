package org.archive.nlp.similarity.editdistance.definition;

import org.archive.util.Language.Lang;

//
public abstract class EditUnit {
	//
	public abstract String getUnitString();
	
	//
	public double getSubstitutionCost(EditUnit other, Lang lang){
		return this.equals(other)?0:1;
	}
	
	//
    public double getDeletionCost(){
        return 1.0;
    }    
    
    //
    public double getInsertionCost(){
        return 1.0;
    }    	
    
	public boolean equals(Object other, Lang lang){
    	if(!(other instanceof EditUnit)) return false;
		return getUnitString().equals(((EditUnit)other).getUnitString());
	}
	
	@Override
	public String toString(){
		return getUnitString();
	}
}
