package org.archive.ntcir.dr.rank;

import java.util.Vector;

public class DRRankedList {
	
	public Vector<DRRankedRecord> recordList;
	
	public void addRecord(DRRankedRecord record){
		if(null == this.recordList){
			this.recordList = new Vector<DRRankedRecord>();
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
		for(DRRankedRecord r: recordList){
			buffer.append(r.toString());
			buffer.append("\n");			
		}
		return buffer.toString();		
	}
}
