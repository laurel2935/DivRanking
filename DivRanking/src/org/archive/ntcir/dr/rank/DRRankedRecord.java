package org.archive.ntcir.dr.rank;

import java.text.DecimalFormat;

public class DRRankedRecord {
	private static DecimalFormat dataFormat =new DecimalFormat("0.###");
	//
	public String topicID;
	private static String zeroStr = "0";
	public String docID;
	public int rank;
	public double rankValue;
	public String runTitle;
	//
	public DRRankedRecord(String topicID, String docID, int rank, double rankV, String runTitle){
		this.topicID = topicID;
		this.docID = docID;
		this.rank = rank;
		this.rankValue = rankV;
		this.runTitle = runTitle;		
	}
	//
	public String toString(){
		StringBuffer strBuffer = new StringBuffer();
		strBuffer.append(this.topicID+" "+zeroStr+" "+this.docID+" "+Integer.toString(this.rank)+" ");
		strBuffer.append(dataFormat.format(this.rankValue)+" "+this.runTitle);
		return strBuffer.toString();
	}

}