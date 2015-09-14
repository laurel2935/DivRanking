package org.archive.nlp.tokenizer;

import java.util.ArrayList;
import java.util.Vector;

import org.archive.nlp.chunk.ShallowParser;
import org.archive.nlp.qpunctuation.QPunctuationParser;
import org.archive.nlp.qpunctuation.QSegment;
import org.archive.nlp.qpunctuation.QueryPreParser;
import org.archive.nlp.tokenizer.ictclas.ICTCLAS2014;
import org.archive.util.Language.Lang;

public class Tokenizer {
	private final static boolean DEBUG = false;
	//for English text segmentation
	private static ShallowParser stanfordParser = null;
	private static final String BLANK = " ";
	
	
	//for Chinese, a string including fewer than 3 or just 3 characters is directly regarded as a word	 
	static final int chDirectWordLength_Threshold = 3;
	//for English, a string including no blank, is directly regarded as one word 
	private static final int enDirectWordLength_Threshold = 1;
		
	////////////////////////
	//common segmentation, e.g., without considering symbols, etc.
	////////////////////////
	public static ArrayList<String> segment(String text, Lang lang){
		return 	segment(text, lang, false);
	}
	//
	private static ArrayList<String> segment(String text, Lang lang, boolean checkLength){
		//
		if(checkLength && isDirectWord(text, lang)){
			ArrayList<String> wordList = new ArrayList<String>();
			wordList.add(text);
			return wordList;
		}
		//
		if(Lang.English == lang){
			if(null == stanfordParser){
				stanfordParser = new ShallowParser(Lang.English);
			}
			return stanfordParser.segment(text);
		}else if(Lang.Chinese == lang){
			return ICTCLAS2014.segment(text);
		}else{
			new Exception("Language type error!").printStackTrace();
			return null;
		}		
	}
	//
	public static boolean isDirectWord(String text, Lang lang){
		if(Lang.English == lang){
			return text.split(BLANK).length<=enDirectWordLength_Threshold? true:false;
		}else if(Lang.Chinese == lang){
			return text.length()<=chDirectWordLength_Threshold? true:false;
		}else{
			new Exception("Language type error!").printStackTrace();
			return false;
		}	
	}
	//
	
	////////////////////
	//query-oriented segmentation
	////////////////////
	/**
	 * taking the possible symbols into consideration 
	 * **/
	public static ArrayList<String> adaptiveQuerySegment(Lang lang, String rawQuery, String reference, boolean checkSymbol, boolean checkLength){
		ArrayList<String> words=null;
		//
		if(!checkSymbol && !checkLength){
			return segment(rawQuery, lang, false);
		}else if(!checkSymbol && checkLength){
			return segment(rawQuery, lang, true);
		}else if(checkSymbol && Lang.Chinese==lang){	
			Vector<QSegment> symbolSegmentSet = QueryPreParser.symbolAnalysis(rawQuery, reference);
			words = segment(symbolSegmentSet, lang);
			/*
			if(QPunctuationParser.includePunctuation(rawQuery)){
				Vector<QSegment> symbolSegmentSet;
				if(null!=(symbolSegmentSet=QueryPreParser.symbolAnalysis(rawQuery)) && symbolSegmentSet.size()>0){			
					//
					words = segment(symbolSegmentSet, lang);
				}
			}else{			
				//
				words = segment(rawQuery, lang, true);
			}
			*/
			//
			if(null!=words && words.size()>0){
				return words;
			}else{
				return null;
			}
		}else{
			new Exception("Symbol check is not available for English").printStackTrace();
			return null;
		}		
	}
	///
	private static ArrayList<String> segment(Vector<QSegment> segmentSet, Lang lang){
		ArrayList<String> wordSet = new ArrayList<String>();
		//		
		for(QSegment seg: segmentSet){
			if(seg.unit){
				wordSet.add(seg.getStr());
			}else if(isDirectWord(seg.getStr(), lang)){
				wordSet.add(seg.getStr());
			}else{
				ArrayList<String> wSet = segment(seg.getStr(), lang);
				if(null != wSet){
					wordSet.addAll(wSet);
				}					
			}
		}
		//
		for(int i=0; i<wordSet.size(); i++){
			if(wordSet.get(i).indexOf("+") >= 0){
				wordSet.set(i, wordSet.get(i).replaceAll("\\+", " ").trim());
			}
		}		
		return wordSet;
	}
	//
	
	
	////////////////////////
	//additional utility
	////////////////////////
	//targeted as the LTP service, in order to avoid unsatisfactory segmentations
	public static String replaceSymboleAsBlank(Lang lang, String rawText, String reference){
		if(Lang.Chinese == lang){
			if(null == reference){
				if((QPunctuationParser.includePunctuation(rawText))){
					Vector<QSegment> symbolSegmentSet;
					if(null!=(symbolSegmentSet=QueryPreParser.symbolAnalysis(rawText)) && symbolSegmentSet.size()>0){			
						//
						String result = "";
						result += symbolSegmentSet.get(0).getStr();
						for(int i=1; i<symbolSegmentSet.size(); i++){
							result += BLANK;
							result += symbolSegmentSet.get(i).getStr();
						}
						if(DEBUG){
							System.out.println(result);
						}
						return result;				
					}else{
						System.out.println("No meaning input!");
						return null;
					}
				}else{
					if(DEBUG){
						System.out.println(rawText);
					}
					return rawText;
				}
			}else{
				Vector<QSegment> symbolSegmentSet;
				if(null!=(symbolSegmentSet=QueryPreParser.symbolAnalysis(rawText, reference)) && symbolSegmentSet.size()>0){			
					//
					String result = "";
					result += symbolSegmentSet.get(0).getStr();
					for(int i=1; i<symbolSegmentSet.size(); i++){
						result += BLANK;
						result += symbolSegmentSet.get(i).getStr();
					}
					if(DEBUG){
						System.out.println(result);
					}
					return result;				
				}else{
					System.out.println("No meaning input!");
					return null;
				}
			}			
		}else{
			new Exception("Lang type error!").printStackTrace();
			return null;
		}		
	}
	
	
	//
	public static void main(String []args){
		//1 query pre-parsing test
		//《饥饿游戏》电影原声	[《, 饥饿, 游戏, 》, 电影, 原, 声]
		//Tokenizer.replaceSymboleAsBlank(rawText, Lang.Chinese);
		
		//2
		String tString = "《饥饿游戏》电影原声";
		System.out.println(Tokenizer.adaptiveQuerySegment(Lang.Chinese, tString, null, true, true));
		
		//
		//String oString = "WWW.10010.COM";
		//System.out.println(QueryPreParser.isOddQuery(oString, Lang.Chinese));
		
	}
}
