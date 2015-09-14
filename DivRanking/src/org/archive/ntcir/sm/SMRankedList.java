package org.archive.ntcir.sm;

import java.util.Vector;

public class SMRankedList {
	public Vector<SMRankedRecord> recordList;
	//
	public void addRecord(SMRankedRecord record){
		if(null == this.recordList){
			this.recordList = new Vector<SMRankedRecord>();
		}
		//
		this.recordList.add(record);
	}
	//
	public int getSize(){
		return this.recordList.size();
	}
	//
	public String tosString() {
		StringBuffer buffer = new StringBuffer();
		for(SMRankedRecord r: recordList){
			buffer.append(r.toString());
			buffer.append("\n");			
		}
		return buffer.toString();		
	}
}
