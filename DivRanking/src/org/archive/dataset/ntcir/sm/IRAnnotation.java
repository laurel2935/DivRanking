package org.archive.dataset.ntcir.sm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * intent role annotation
 * **/
public class IRAnnotation {	
	//not a correct annotation, because of not including ko
	public boolean _bad = false;
	
	public KernelObject ko = null;
	
	public ArrayList<Modifier> moSet = null;
	//
	public IRAnnotation(KernelObject ko, ArrayList<Modifier> moSet){
		this.ko = ko;
		this.moSet = moSet;	
		//
		if(null != moSet){
			//System.out.println("before:\t"+moSet);
			Collections.sort(moSet);
			//System.out.println("after:\t"+moSet);
		}
	}		
	//
	public IRAnnotation(boolean bad){
		this._bad = bad;
	}
	
	public int hashCode()
    {
		int hash = 0;
		//
		for(Modifier mo: this.moSet){
			hash += mo.hashCode();
		}		
		hash += this.ko.hashCode();
		//
		return hash;
    }
	/**
	 * equal version of completely same
	 * **/
	public boolean equals(Object other){
		if(this == other){
			return true;
		}
		if(other instanceof IRAnnotation){
			final IRAnnotation cmpIRA = (IRAnnotation)other;
			//
			if(this.ko.equals(cmpIRA.ko)){
				if(this.moSet.size() == cmpIRA.moSet.size()){
					if(this.moSet.size()>0){
						return compareVector(this.moSet, cmpIRA.moSet);
					}
					return true;					
				}else{
					return false;
				}
			}else{
				return false;
			}						
		}else{
			return false;
		}
	}	
	private boolean compareVector(ArrayList<Modifier> vectorA, ArrayList<Modifier> vectorB){
		if(vectorA.containsAll(vectorB) && vectorB.containsAll(vectorA)){
			return true;
		}else{
			return false;
		}	
	}
	//	
	private boolean shinkCompare(ArrayList<Modifier> vectorA, ArrayList<Modifier> vectorB){
		if(vectorA.size()==0 || vectorB.size()==0){
			return true;
		}else{
			int minA = Math.min(SMTopic.ShrinkThreshold, vectorA.size());
			int minB = Math.min(SMTopic.ShrinkThreshold, vectorB.size());
			List<Modifier> tempA = vectorA.subList(0, minA);
			List<Modifier> tempB = vectorB.subList(0, minB);
			if(tempA.containsAll(tempB) || tempB.containsAll(tempA)){
				return true;
			}else{
				return false;
			}
		}
	}
	/**
	 * equal version : the same ko, but the modifier comparison depends on the first two modifiers (i.e., ShrinkThreshold) if has.
	 * Compare modifier vector corresponding to the MergeThreshold:
	 * When two irAnnotation share the same kernel-object,
	 * we determine whether they indicate the same intent based on the first <ShrinkThreshold> modifiers
	 * **/
	public boolean shrinkEquals(Object other){
		if(this == other){
			return true;
		}
		if(other instanceof IRAnnotation){
			final IRAnnotation cmpIRA = (IRAnnotation)other;
			//
			if(this.ko.equals(cmpIRA.ko)){
				return shinkCompare(this.moSet, cmpIRA.moSet);
			}else{
				return false;
			}						
		}else{
			return false;
		}
	}
	//
	public String toString(){
		String string = "";
		for(Modifier mo: moSet){
			string += (" "+mo.toString());
		}
		return "ko:["+ko.toString()+"] MOSet: "+string;
	}
	//
	public static void main(String []args){
		
	}
}
