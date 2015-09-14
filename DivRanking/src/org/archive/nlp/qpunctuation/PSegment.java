package org.archive.nlp.qpunctuation;

/**
 * a segment of a query with punctuation
 * **/

public class PSegment extends Segment implements Comparable{	
	//
	public int head;
	public int tail;
	//
	public PSegment(String str, int head, int tail){
		super(str);
		this.head = head;
		this.tail = tail;
	}	
	//
	public  int compareTo(Object o){
		PSegment comp = (PSegment)o;
		if(this.head > comp.head){
			return 1;
		}else if(this.head < comp.head){
			return -1;
		}else{
			return 0;
		}
	}
}
