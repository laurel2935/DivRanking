package org.archive.a1.analysis;

import java.util.Vector;

public class DivResult{
	public Vector<Double> alphanDCG5;
	public Vector<Double> alphanDCG10;
	public Vector<Double> alphanDCG20;
	
	public Vector<Double> nERRIA5;
	public Vector<Double> nERRIA10;
	public Vector<Double> nERRIA20;
	
	public Vector<Double> nNRBP20;
	
	public Vector<Double> strec10;
	public Vector<Double> pIA10;
	
	public DivResult(){
		alphanDCG5 = new Vector<Double>();
		alphanDCG10 = new Vector<Double>();
		alphanDCG20 = new Vector<Double>();
		
		nERRIA5 = new Vector<Double>();
		nERRIA10 = new Vector<Double>();
		nERRIA20 = new Vector<Double>();
		
		nNRBP20 = new Vector<>();
		
		strec10 = new Vector<Double>();
		
		pIA10 = new Vector<>();		
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
	
	public void addnNRBP20(Double v){
		this.nNRBP20.add(v);
	}
	
	public void addPIA10(Double v){
		this.pIA10.add(v);
	}
	
	public String toString(){
		StringBuffer buffer = new StringBuffer();
		
		for(int i=0; i<this.alphanDCG5.size(); i++){
			buffer.append(nERRIA5.get(i).toString()+"\t");
			buffer.append(nERRIA10.get(i).toString()+"\t");
			buffer.append(nERRIA20.get(i).toString()+"\t");
			
			buffer.append(alphanDCG5.get(i).toString()+"\t");
			buffer.append(alphanDCG10.get(i).toString()+"\t");
			buffer.append(alphanDCG20.get(i).toString()+"\t");
			
			buffer.append(nNRBP20.get(i).toString()+"\t");	
			
			buffer.append(pIA10.get(i).toString()+"\t");
			
			buffer.append(strec10.get(i).toString()+"\t");
			
			buffer.append("\n");
		}
		
		return buffer.toString();
	}
}
