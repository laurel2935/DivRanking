package org.archive.nlp.qpunctuation;

import java.util.regex.Pattern;

/**
 * symmetric punctuation
 * */
public class SPunctuation{
	//
	public String symbolHead;
	public String symbolTail;
	public Pattern pattern;
	//
	public SPunctuation(String separatorHead, String separatorTail, Pattern pattern){
		this.symbolHead = separatorHead;
		this.symbolTail = separatorTail;
		this.pattern = pattern;
	}
}