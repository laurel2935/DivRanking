package org.archive.dataset.trec.query;

public abstract class TRECQuery {
	// number
	public String _number;
	//query text
	public String _title;
	//description
	public String _description;
	
	public TRECQuery(String number, String title, String description){
		this._number = number;
		this._title = title;
		this._description = description;		
	}
	/**
	 * @return the content to be used to perform IR, e.g., merely title, or title + description, etc.
	 * **/
	public abstract String getQueryContent();
}
