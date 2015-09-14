package org.archive.nlp.qpunctuation;

import java.util.Collections;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.archive.nlp.qpunctuation.PSegment;
import org.archive.nlp.qpunctuation.QPunctuationParser;
import org.archive.nlp.qpunctuation.QSegment;
import org.archive.util.Language.Lang;
import org.archive.util.pattern.PatternFactory;

/**
 * symbol pattern based query parsing, namely analyze the symbol character of a given query
 * mainly using <<>>,"",...and so on to divide symbol-queries
 * **/
public class QueryPreParser {	
	private final static boolean DEBUG = false;
	//interface
	/**
	 * parse a given query into segments based on the symbols included in the query 
	 * **/
	public static Vector<QSegment> symbolAnalysis(String query){
		//
		Vector<QSegment> finalPoll = new Vector<QSegment>();
		Vector<QSegment> sSet;
		//
		if(PatternFactory.containNonSimpleChC(query)){
			//System.out.println("containNonSimpleChC");
			//pattern based unit identification
			sSet = multiPatternSegment(query);
			for(QSegment seg: sSet){
				if(!seg.unit){
					seg.str = formatQuery(seg.str);
				}
			}
			//			
			Vector<QSegment> sBag;
			for(QSegment seg: sSet){
				if(!seg.unit){
					sBag = QPunctuationParser.parse(seg.str);
					for(QSegment s: sBag){
						/*
						if(PatternInQuery.allNAStr(s.segment)){
							s.unitType = 2;
						}
						*/
						//
						finalPoll.add(s);
					}
				}else{
					finalPoll.add(seg);
				}				
			}
		}else{
			finalPoll.add(new QSegment(query, false));
		}
		//
		
		if(DEBUG){
			for(QSegment qSegment: finalPoll){
				System.out.println(qSegment.toString());
			}
		}
		
		return finalPoll;
	}
	//
	public static Vector<QSegment> symbolAnalysis(String query, String reference){
		if(null != reference){
			//using preDefinedUnit
			Pattern unitPattern = Pattern.compile(reference);
			Vector<QSegment> qSegmentSet = new Vector<QSegment>();
			qSegmentSet.add(new QSegment(query, false));
			patternSegment(qSegmentSet, unitPattern);	
			//
			Vector<QSegment> resultQSegmentSet = new Vector<QSegment>();
			Vector<QSegment> temp = null;
			for(QSegment qSegment: qSegmentSet){
				if(qSegment.unit){
					resultQSegmentSet.add(qSegment);
				}else if(null != (temp=symbolAnalysis(qSegment.str))){
					resultQSegmentSet.addAll(temp);
				}
			}
			//
			return resultQSegmentSet;
		}else{
			return symbolAnalysis(query);
		}		
	}
	//	
	/**
	 * odd queries:
	 * (1) including url, mail;
	 * (2) length>=30 for Chinese query
	 * (3) more than 30 words for English query 
	 * **/
	public static boolean isOddQuery(String query, Lang lang){
		if(Lang.Chinese==lang && query.length()>=30){
			return true;
		}else if(Lang.English==lang && query.split(" ").length>=30){
			return true;
		}else{
			if(PatternFactory.containNonSimpleChC(query)){
				Vector<PSegment> mat;
				if(null!=(mat=patternMatch(query, PatternFactory.netPattern))){
					return true;
				}else if(null!=(mat=patternMatch(query, PatternFactory.mailPattern))){
					return true;
				}else{
					return false;
				}				
			}else{
				return false;
			}
		}		
	}
	//
	private static boolean containNetMail(String str){
		Matcher matcher_1, matcher_2;
		matcher_1 = PatternFactory.netPattern.matcher(str);
		if(matcher_1.find()){
			return true;
		}else{
			matcher_2 = PatternFactory.mailPattern.matcher(str);
			if(matcher_2.find()){
				return true;
			}else{
				return false;
			}
		}
	}
	//
	private static String formatQuery(String query){
		String formatedQ = query;
		//combine successive +
		if(query.indexOf("+")>=0){
			formatedQ = query.replaceAll("[\\+]+", "+");
		}
		//
		/*
		if(FontAnalyzer.TraditionalType(formatedQ)){
			formatedQ = FontAnalyzer.fontConverter_T_S(formatedQ);
		}
		*/
		//
		return formatedQ;
	}	
	//
	private static Vector<QSegment> multiPatternSegment(String query){
		Vector<QSegment> segmentSet = new Vector<QSegment>();
		segmentSet.add(new QSegment(query, false));
		//1:mail,tv,time,edition
		//2:net		
		//patternSegment(segmentSet, PatternInQuery.mailPattern);
		patternSegment(segmentSet, PatternFactory.tvPattern);		
		patternSegment(segmentSet, PatternFactory.editionPattern);		
		//
		patternSegment(segmentSet, PatternFactory.nacPattern);
		//��ֹ�����Ϊ���ֵ��ص�
		patternSegment(segmentSet, PatternFactory.timePattern);
		//patternSegment(segmentSet, PatternInQuery.netPattern);		
		//
		return segmentSet;
	}
	//
	private static void patternSegment(Vector<QSegment> segmentSet, Pattern pattern){		
		//
		Vector<QSegment> bufferSet = new Vector<QSegment>();
		//
		Vector<PSegment> matSubSet;
		//
		boolean match = false;
		for(QSegment segment: segmentSet){
			if(!segment.unit){
				if(null != (matSubSet=patternMatch(segment.str, pattern))){
					match = true;
					//
					if(matSubSet.size()>1){
						Collections.sort(matSubSet);
						//
						PSegment sUnit;
						//
						sUnit = matSubSet.get(0);
						if(sUnit.head>0){							
							bufferSet.add(new QSegment(segment.str.substring(0, sUnit.head),false));
						}
						//
						QSegment matSegment = new QSegment(sUnit.str, true); 
						matSegment.setType(1);
						bufferSet.add(matSegment);
						//		
						if(sUnit.tail<matSubSet.get(1).head-1){
							bufferSet.add(new QSegment(segment.str.substring(sUnit.tail+1, matSubSet.get(1).head), false));
						}						
						//
						for(int i=1; i<matSubSet.size()-1; i++){
							sUnit = matSubSet.get(i);
							//
							matSegment = new QSegment(sUnit.str, true); 
							matSegment.setType(1);
							bufferSet.add(matSegment);
							//		
							if(sUnit.tail<matSubSet.get(i+1).head-1){
								bufferSet.add(new QSegment(segment.str.substring(sUnit.tail+1, matSubSet.get(i+1).head), false));
							}	
						}	
						//
						sUnit = matSubSet.get(matSubSet.size()-1);	
						matSegment = new QSegment(sUnit.str, true); 
						matSegment.setType(1);
						bufferSet.add(matSegment);
						//
						if(sUnit.tail<segment.str.length()-1){
							bufferSet.add(new QSegment(segment.str.substring(sUnit.tail+1, segment.str.length()), false));
						}
					}else{
						PSegment sUnit = matSubSet.get(0);
						if(sUnit.head>0){							
							bufferSet.add(new QSegment(segment.str.substring(0, sUnit.head),false));
						}
						//
						QSegment matSegment = new QSegment(sUnit.str, true); 
						matSegment.setType(1);
						bufferSet.add(matSegment);
						//
						if(sUnit.tail<segment.str.length()-1){							
							bufferSet.add(new QSegment(segment.str.substring(sUnit.tail+1, segment.str.length()), false));
						}	
					}					
				}else{
					bufferSet.add(segment);
				}
			}else{
				bufferSet.add(segment);
			}
		}
		//
		if(match){
			segmentSet.clear();
			segmentSet.addAll(bufferSet);
		}		
	}	
	//match the given pattern
	private static Vector<PSegment> patternMatch(String query, Pattern pattern){		
		//
		String mStr;
		Vector<String> uniqueStrSet = new Vector<String>();
		Matcher matcher = pattern.matcher(query);
		while(matcher.find()){
			mStr = matcher.group();			
			if(!uniqueStrSet.contains(mStr)){
				uniqueStrSet.add(mStr);
			}			
			//System.out.println(mStr);
		}
		//
		if(uniqueStrSet.size()>0){
			Vector<PSegment> matSubSet = new Vector<PSegment>();
			PSegment subUnit;
			//
			for(String uS: uniqueStrSet){
				int firstIndex = 0;
				while(0<=(firstIndex=query.indexOf(uS, firstIndex))){
					subUnit = new PSegment(uS, firstIndex, firstIndex+uS.length()-1);
					matSubSet.add(subUnit);
					//
					firstIndex++;
				}
			}		
			//
			if(matSubSet.size()>0){
				return matSubSet;
			}else{
				return null;
			}
		}else{
			return null;
		}		
	}
	//match the time substring
	private static Vector<PSegment> timeMatch(String query){		
		//
		String mStr;
		Vector<String> uniqueStrSet = new Vector<String>();
		Matcher matcher = PatternFactory.timePattern.matcher(query);
		while(matcher.find()){
			mStr = matcher.group();			
			if(!uniqueStrSet.contains(mStr)){
				uniqueStrSet.add(mStr);
			}			
			//System.out.println(mStr);
		}
		//
		if(uniqueStrSet.size()>0){
			Vector<PSegment> matSubSet = new Vector<PSegment>();
			PSegment subUnit;
			//
			for(String uS: uniqueStrSet){
				int firstIndex = 0;
				while(0<=(firstIndex=query.indexOf(uS, firstIndex))){
					subUnit = new PSegment(uS, firstIndex, firstIndex+uS.length()-1);
					matSubSet.add(subUnit);
					//
					firstIndex++;
				}
			}		
			//
			if(matSubSet.size()>0){
				return matSubSet;
			}else{
				return null;
			}
		}else{
			return null;
		}		
	}
	//match the edition PSegmenting
	private static Vector<PSegment> editionMatch(String query){		
		//
		String mStr;
		Vector<String> uniqueStrSet = new Vector<String>();
		Matcher matcher = PatternFactory.editionPattern.matcher(query);
		while(matcher.find()){
			mStr = matcher.group();			
			if(!uniqueStrSet.contains(mStr)){
				uniqueStrSet.add(mStr);
			}			
			//System.out.println(mStr);
		}
		//
		if(uniqueStrSet.size()>0){
			Vector<PSegment> matSubSet = new Vector<PSegment>();
			PSegment subUnit;
			//
			for(String uS: uniqueStrSet){
				int firstIndex = 0;
				while(0<=(firstIndex=query.indexOf(uS, firstIndex))){
					subUnit = new PSegment(uS, firstIndex, firstIndex+uS.length()-1);
					matSubSet.add(subUnit);
					//
					firstIndex++;
				}
			}		
			//
			if(matSubSet.size()>0){
				return matSubSet;
			}else{
				return null;
			}
		}else{
			return null;
		}		
	}
	
	
	//
	public static void main(String []args){
		Vector<PSegment> matSubSet;
		/**1**/
		/*
		String query = "˿·2.3��2.3˵1��072";		
		matSubSet = PatternFactory.editionMatch(query);
		if(null!=matSubSet){
			for(SubStrUnit subUnit: matSubSet){
				System.out.println(subUnit.subString+"\t"+subUnit.head+"\t"+subUnit.tail);
			}
		}
		*/
		/**2**/
		/*
		String query = "2005��߿�2��ȫ3�չ�3�վ�2";		
		matSubSet = PatternFactory.timeMatch(query);
		if(null!=matSubSet){
			for(SubStrUnit subUnit: matSubSet){
				System.out.println(subUnit.subString+"\t"+subUnit.head+"\t"+subUnit.tail);
			}
		}
		*/
		/**3**/
		/*
		String query = "flash�桶�����⴫��";		
		matSubSet = PatternFactory.patternMatch(query, PatternFactory.numAlphabetPattern);
		if(null!=matSubSet){
			for(SubStrUnit subUnit: matSubSet){
				System.out.println(subUnit.subString+"\t"+subUnit.head+"\t"+subUnit.tail);
			}
		}
		*/
		/**4**/
		/*
		String query = "rdbh@com.cn";		
		matSubSet = PatternFactory.patternMatch(query, PatternFactory.mailPattern);
		if(null!=matSubSet){
			for(SubStrUnit subUnit: matSubSet){
				System.out.println(subUnit.subString+"\t"+subUnit.head+"\t"+subUnit.tail);
			}
		}
		*/
		/**5**/
		/*
		String query = "flash�桶�����⴫��";		
		matSubSet = PatternFactory.patternMatch(query, PatternFactory.netPattern);
		if(null!=matSubSet){
			for(SubStrUnit subUnit: matSubSet){
				System.out.println(subUnit.subString+"\t"+subUnit.head+"\t"+subUnit.tail);
			}
		}
		*/
		/**6**/
		/*
		String query = "�������qq��";
		Vector<Segment> result;
		result= QuerySymbolPattern.symbolPreprocess(query);
		for(Segment s: result){
			System.out.println(s.segment+"\t"+s.unit);
		}
		*/
		/**7**/
		//QuerySymbolPattern.checkEffectiveQuery();
	}
}
