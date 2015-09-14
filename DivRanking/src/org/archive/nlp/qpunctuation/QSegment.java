package org.archive.nlp.qpunctuation;

/**
 * a segment of a user query
 * */

public class QSegment extends Segment{	
	//imply whether the segment should not be segmented or not
	public boolean unit;
	//symbol based unit
	public int unitType;	
	//
	public QSegment(String str, boolean unit){
		super(str);		
		this.unit = unit;
		this.unitType = -1;		
	}
	//
	public QSegment(QSegment qSegment){
		super(qSegment.str);
		this.unit = qSegment.unit;
		this.unitType = qSegment.unitType;		
	}	
	//
	public void setType(int unitType){
		this.unitType = unitType;
	}
	@Override
	public boolean equals(Object obj){
		if (this == obj) {
			return true;
		}
		if (null == obj) {
			return false;
		}
		if (!(obj instanceof QSegment)) {
			return false;
		}
		QSegment other = (QSegment)obj;
		//
		return this.str.equals(other.str) 
				&& this.unit==other.unit
				&& this.unitType==other.unitType;
	}
	@Override
	public final int hashCode(){
		return this.str.hashCode()+this.unitType;
	}
	
	public String toString() {
		return this.str+"/"+Integer.toString(this.unitType);
	}
}
