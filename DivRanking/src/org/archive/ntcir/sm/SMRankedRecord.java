package org.archive.ntcir.sm;

import java.text.DecimalFormat;


/**
 * 
 * **/
public class SMRankedRecord {
	private static DecimalFormat dataFormat =new DecimalFormat("0.########");
	//
	public String topicID;
	private static String zeroStr = "0";
	public String firStStr;
	public int firRank;
	public double firRankValue;
	public String secStStr;
	public int secRank;
	public double secRankValue;
	public String runTitle;
	//
	public SMRankedRecord(String topicID, String firStStr, int firRank, double firRankV,
			String secStStr, int secRank, double secRankV, String runTitle){
		this.topicID = topicID;
		this.firStStr = firStStr;
		this.firRank = firRank;
		this.firRankValue = firRankV;
		
		this.secStStr = secStStr;
		this.secRank = secRank;
		this.secRankValue = secRankV;		
		this.runTitle = runTitle;		
	}
	//
	public String toString(){
		StringBuffer strBuffer = new StringBuffer();
		strBuffer.append(this.topicID+";");
		if(Double.isNaN(this.firRankValue) || Double.isNaN(this.secRankValue)){
			System.err.println("Nan Error!");
		}		
		strBuffer.append(zeroStr+";"+this.firStStr+";"+Integer.toString(this.firRank)+";"+dataFormat.format(this.firRankValue)+";");
		strBuffer.append(zeroStr+";"+this.secStStr+";"+Integer.toString(this.secRank)+";"+dataFormat.format(this.secRankValue)+";");
		strBuffer.append(this.runTitle);
		return strBuffer.toString();
	}

}
