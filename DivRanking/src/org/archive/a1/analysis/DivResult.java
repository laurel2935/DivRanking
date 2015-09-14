package org.archive.a1.analysis;

import java.util.Vector;

public class DivResult{
	Vector<Double> alphanDCG5;
	Vector<Double> alphanDCG10;
	Vector<Double> alphanDCG20;
	
	Vector<Double> nERRIA5;
	Vector<Double> nERRIA10;
	Vector<Double> nERRIA20;
	
	Vector<Double> strec10;
	
	public DivResult(){
		alphanDCG5 = new Vector<Double>();
		alphanDCG10 = new Vector<Double>();
		alphanDCG20 = new Vector<Double>();
		
		nERRIA5 = new Vector<Double>();
		nERRIA10 = new Vector<Double>();
		nERRIA20 = new Vector<Double>();
		
		strec10 = new Vector<Double>();
	}
	
	public void addAlphanDCG5(Double v){
		this.alphanDCG5.add(v);
	}
	public void addAlphanDCG10(Double v){
		this.alphanDCG10.add(v);
	}
	public void addAlphanDCG20(Double v){
		this.alphanDCG20.add(v);
	}
	
	public void addnERRIA5(Double v){
		this.nERRIA5.add(v);
	}
	public void addnERRIA10(Double v){
		this.nERRIA10.add(v);
	}
	public void addnERRIA20(Double v){
		this.nERRIA20.add(v);
	}
	
	public void addStrec10(Double v){
		this.strec10.add(v);
	}
	
	public String toString(){
		StringBuffer buffer = new StringBuffer();
		
		for(int i=0; i<this.alphanDCG5.size(); i++){
			buffer.append(alphanDCG5.get(i).toString()+"\t");
			buffer.append(alphanDCG10.get(i).toString()+"\t");
			buffer.append(alphanDCG20.get(i).toString()+"\t");
			
			buffer.append(nERRIA5.get(i).toString()+"\t");
			buffer.append(nERRIA10.get(i).toString()+"\t");
			buffer.append(nERRIA20.get(i).toString()+"\t");
			
			buffer.append(strec10.get(i).toString());
			buffer.append("\n");
		}
		
		return buffer.toString();
	}
}
