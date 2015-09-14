package org.archive.dataset.trec.query;

public class TRECSubtopic{
	public String _sNumber;
	public String _sType;
	public String _sDescription;
	//
	public TRECSubtopic(String sNumber, String sDescription, String sType){
		this._sNumber = sNumber;		
		this._sDescription = sDescription;	
		this._sType = sType;
	}
	//
	public String toString() {
		return _sNumber+"\t"+_sType+"\t"+_sDescription;
	}
	//
	public String getContent(){
		return this._sDescription;
	}
}
