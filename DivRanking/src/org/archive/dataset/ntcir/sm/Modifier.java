package org.archive.dataset.ntcir.sm;

import java.util.ArrayList;
import java.util.Collections;

import org.archive.nlp.chunk.lpt.addon.LTPPosTag;


public class Modifier implements Comparable{
	public String moStr;
	public String posTag;
	private int fre = 0;
	//
	public Modifier(String moStr, String posTag){
		this.moStr = moStr;
		this.posTag = posTag;
	}
	//
	public void setFre(int fre){
		this.fre = fre;
	}
	public int getFre(){
		return this.fre;
	}
	//
	public int hashCode()
    {
		//return this.moStr.hashCode()+this.posTag.hashCode();
		return this.moStr.hashCode();
    }
	//
	public boolean equals(Object other){
		if(this == other){
			return true;
		}
		if(other instanceof Modifier){
			final Modifier cmpMO = (Modifier)other;
			//
			//return this.moStr.equals(cmpMO.moStr) && this.posTag.equals(cmpMO.posTag);
			return this.moStr.equals(cmpMO.moStr);
		}else{
			return false;
		}
	}
	//
	public  int compareTo(Object o){
		Modifier comp = (Modifier)o;
		//
		return LTPPosTag.compare(this.posTag, comp.posTag);
	}
	//
	public String toString(){
		return moStr+" "+posTag;
	}
	
	public static void main(String []args){
		//1
		//before:	[特产 n, 哪里 r, 买 v, 最 d, 便宜 a]
		//after:	[特产 n, 哪里 r, 买 v, 最 d, 便宜 a]
		ArrayList<Modifier> moList = new ArrayList<Modifier>();
		moList.add(new Modifier("哪里", "r"));
		moList.add(new Modifier("买", "v"));
		Collections.sort(moList);
		System.out.println(moList);
	}
}
