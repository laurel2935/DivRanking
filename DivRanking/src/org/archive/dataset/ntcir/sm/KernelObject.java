package org.archive.dataset.ntcir.sm;

public class KernelObject {
	public String koStr;
	public String posTag;
	//
	public KernelObject(String str, String posTag){
		this.koStr = str;
		this.posTag = posTag;
	}
	public int hashCode()
    {
		//return this.koStr.hashCode()+this.posTag.hashCode();
		return this.koStr.hashCode();
    }
	//
	public boolean equals(Object other){
		if(this == other){
			return true;
		}
		if(other instanceof KernelObject){
			final KernelObject cmpKO = (KernelObject)other;
			//
			//return this.koStr.equals(cmpKO.koStr) && this.posTag.equals(cmpKO.posTag);	
			return this.koStr.equals(cmpKO.koStr);	
		}else{
			return false;
		}
	}
	//
	public String toString(){
		return koStr+" "+posTag;
	}
}
