package org.archive.dataset.trec.query;

import java.util.Vector;

public class TRECDivQuery extends TRECQuery implements Comparable<TRECQuery> {
	//Usage cases
	public static final boolean INCLUDE_QUERY_TITLE       = true;
	//
	public static final boolean INCLUDE_QUERY_DESCRIPTION = false;
	//ambiguous or faceted
	public String _type;
	//
	private Vector<TRECSubtopic> subtopicList = new Vector<TRECSubtopic>();
	
	// Constructors
	public TRECDivQuery(String number, String title, String description, String type) {
		super(number, title, description);		
		_type = type;
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(_number + " -> " + _type + "\t" + _title + "\t" + _description);
		for(TRECSubtopic subtopic: subtopicList){
			sb.append("\n"+subtopic.toString());
		}		
		return sb.toString(); 
	}
	
	public String getQueryContent() {
		StringBuilder sb = new StringBuilder();
		//
		if (INCLUDE_QUERY_TITLE) {
			sb.append(_title);
		} 
		if (INCLUDE_QUERY_DESCRIPTION) {
			sb.append((sb.length() > 0 ? " " : "") + _description);			
		}
		/*
		if (INCLUDE_QUERY_INSTANCE) {
			sb.append((sb.length() > 0 ? " " : "") + _instance);
		}
		*/
		return sb.toString();
	}
	//
	public Vector<TRECSubtopic> getSubtopicList(){
		return this.subtopicList;
	}
	//
	public void addSubtopic(TRECSubtopic subtopic){
		subtopicList.add(subtopic);
	}
	//@Override
	public int compareTo(TRECQuery o) {
		// TODO Auto-generated method stub
		return toString().compareTo(o.toString());
	}	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
