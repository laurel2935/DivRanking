package org.archive.nlp.qpunctuation;

import java.util.Collections;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.archive.util.pattern.PatternFactory;

/**
 * Goal: parse the punctuation within a query that act as a separator
 * Note: the input query should be queries without substrings like URL, Mail.
 * Output: a group of segments divided by separator-like symbols
 * **/
public class QPunctuationParser {	
	//possible separator-punctuation
	private static final Pattern possibleSeparatorPunctuationPattern = Pattern.compile("[^a-z0-9A-Z\u4E00-\u9FFF]+");
	//dot punctuation
	private static final Vector<String> dotPunctuationSet;
	//symmetric punctuation
	private static final Vector<SPunctuation> symmetricPunctuationSet;
	//initialize the dot punctuation
	static {
		dotPunctuationSet = new Vector<String>();
		//blank?
		dotPunctuationSet.add("+");
		dotPunctuationSet.add("＋");
		//
		dotPunctuationSet.add(",");
		dotPunctuationSet.add("、");
		dotPunctuationSet.add("，");
		//
		dotPunctuationSet.add(":");
		dotPunctuationSet.add("：");
		//
		dotPunctuationSet.add(";");
		dotPunctuationSet.add("；");
		//
		dotPunctuationSet.add("&");
		dotPunctuationSet.add("#");
		dotPunctuationSet.add(" ");
		//
		dotPunctuationSet.add("...");
		//
		dotPunctuationSet.add("、");	
		//
		dotPunctuationSet.add(".");
		dotPunctuationSet.add("。");
		//
		dotPunctuationSet.add("/");	
		//
		dotPunctuationSet.add("\\");	
		//
		dotPunctuationSet.add("-");	
		//name note
		dotPunctuationSet.add("·");
		//
		dotPunctuationSet.add("?");
		dotPunctuationSet.add("？");
		//
		dotPunctuationSet.add("～");		
	}
	//initialize the symmetric punctuation
	static{		
		symmetricPunctuationSet = new Vector<SPunctuation>();
		//
		String separatorHead;
		String separatorTail;
		Pattern pattern;
		SPunctuation ss;		
		//@@
		separatorHead = "@";
		separatorTail = "@";
		pattern = Pattern.compile("@[^@]{1,50}@");
		ss = new SPunctuation(separatorHead, separatorTail, pattern);
		symmetricPunctuationSet.add(ss);	
		
		//<>
		separatorHead = "<";
		separatorTail = ">";
		pattern = Pattern.compile("<[^>]{1,50}>");
		ss = new SPunctuation(separatorHead, separatorTail, pattern);
		symmetricPunctuationSet.add(ss);
		//＜＞
		separatorHead = "＜";
		separatorTail = "＞";
		pattern = Pattern.compile("＜[^＞]{1,50}＞");
		ss = new SPunctuation(separatorHead, separatorTail, pattern);
		symmetricPunctuationSet.add(ss);		
		//＜>
		separatorHead = "＜";
		separatorTail = ">";
		pattern = Pattern.compile("＜[^>]{1,50}>");
		ss = new SPunctuation(separatorHead, separatorTail, pattern);
		symmetricPunctuationSet.add(ss);
		//<＞
		separatorHead = "<";
		separatorTail = "＞";
		pattern = Pattern.compile("<[^＞]{1,50}＞");
		ss = new SPunctuation(separatorHead, separatorTail, pattern);
		symmetricPunctuationSet.add(ss);
		//《》
		separatorHead = "《";
		separatorTail = "》";
		pattern = Pattern.compile("《[^》]{1,50}》");
		ss = new SPunctuation(separatorHead, separatorTail, pattern);
		symmetricPunctuationSet.add(ss);
		//()
		separatorHead = "(";
		separatorTail = ")";
		pattern = Pattern.compile("\\([^\\)]{1,50}\\)");
		ss = new SPunctuation(separatorHead, separatorTail, pattern);
		symmetricPunctuationSet.add(ss);
		//（）
		separatorHead = "（";
		separatorTail = "）";
		pattern = Pattern.compile("（[^）]{1,50}）");
		ss = new SPunctuation(separatorHead, separatorTail, pattern);
		symmetricPunctuationSet.add(ss);
		//{}
		separatorHead = "{";
		separatorTail = "}";
		pattern = Pattern.compile("\\{[^\\}]{1,50}\\}");
		ss = new SPunctuation(separatorHead, separatorTail, pattern);
		symmetricPunctuationSet.add(ss);
		//【】
		separatorHead = "【";
		separatorTail = "】";
		pattern = Pattern.compile("【[^】]{1,50}】");
		ss = new SPunctuation(separatorHead, separatorTail, pattern);
		symmetricPunctuationSet.add(ss);
		//[]
		separatorHead = "[";
		separatorTail = "]";
		pattern = Pattern.compile("\\[[^\\]]{1,50}\\]");
		ss = new SPunctuation(separatorHead, separatorTail, pattern);
		symmetricPunctuationSet.add(ss);		
		//""
		separatorHead = "\"";
		separatorTail = "\"";
		pattern = Pattern.compile("\"[^\"]{1,50}\"");
		ss = new SPunctuation(separatorHead, separatorTail, pattern);
		symmetricPunctuationSet.add(ss);
		//“”
		separatorHead = "“";
		separatorTail = "”";
		pattern = Pattern.compile("“[^”]{1,50}”");
		ss = new SPunctuation(separatorHead, separatorTail, pattern);
		symmetricPunctuationSet.add(ss);		
	}
	
	//
	public static boolean includePunctuation(String query){
		Matcher matcher = possibleSeparatorPunctuationPattern.matcher(query);
		if(matcher.find()){
			return true;
		}else{
			return false;
		}
	}
	/**
	 * Interface
	 * Good example: 国产三大著名杀毒软件（江民、瑞星、金山）
	 * separate the given query using the separator within the query
	 * */
	public static Vector<QSegment> parse(String rawQ){
		try{
			return parsePunctuations(rawQ);
		}catch(Exception e){
			System.out.println("Segment analysis problem");
		}
		return null;
	}
	private static Vector<QSegment> parsePunctuations(String rawQ){		
		//single-separator index
		Vector<DPunctuationInstance> SingleSymbolSet= distillDotP(rawQ);
		//symmetry separator index
		Vector<SPunctuationInstance> SymmetrySymbolSet= distillSymmetricP(rawQ);
		//
		Vector<Integer> symbolIndexSet = new Vector<Integer>();
		//
		if(null != SingleSymbolSet){
			for(DPunctuationInstance s: SingleSymbolSet){
				symbolIndexSet.add(s.index);
			}
		}
		if(null != SymmetrySymbolSet){
			for(SPunctuationInstance ss: SymmetrySymbolSet){
				if(ss.symmetricP.indexOf("@")>=0){
					symbolIndexSet.add(ss.headIndex);
					symbolIndexSet.add(ss.headIndex+1);
					symbolIndexSet.add(ss.endIndex+1);
					symbolIndexSet.add(ss.endIndex+2);
				}else{
					symbolIndexSet.add(ss.headIndex);
					symbolIndexSet.add(ss.endIndex);
				}				
			}
		}
		//
		Vector<QSegment> segmentSet = new Vector<QSegment>();
		//
		if(symbolIndexSet.size()>0){
			//
			Collections.sort(symbolIndexSet);
			//
			if(null != SymmetrySymbolSet){				
				boolean [] singleArray = new boolean[rawQ.length()];
				for(Integer index: symbolIndexSet){
					singleArray[index.intValue()] = true;
				}
				//
				boolean [] doubleArray = new boolean[rawQ.length()];
				for(SPunctuationInstance ss: SymmetrySymbolSet){
					//
					if(ss.symmetricP.indexOf("@")>=0){
						//
						singleArray[ss.headIndex] = true;
						singleArray[ss.headIndex+1] = true;
						singleArray[ss.endIndex+1] = true;
						singleArray[ss.endIndex+2] = true;
						for(int i=ss.headIndex+2; i<ss.endIndex+1; i++){
							doubleArray[i] = true;
						}	
					}else{
						singleArray[ss.headIndex] = true;
						singleArray[ss.endIndex] = true;
						for(int i=ss.headIndex+1; i<ss.endIndex; i++){
							doubleArray[i] = true;
						}	
					}									
				}
				//
				StringBuffer commonBuffer = new StringBuffer();
				StringBuffer intactBuffer = new StringBuffer();
				for(int i=0; i<rawQ.length(); i++){
					if(!singleArray[i] && !doubleArray[i]){
						if(intactBuffer.length()>0){
							QSegment s = new QSegment(intactBuffer.toString(), true);
							s.setType(2);
							segmentSet.add(s);
							intactBuffer.delete(0, intactBuffer.length());
						}
						commonBuffer.append(rawQ.charAt(i));
					}else{
						if(commonBuffer.length()>0){
							QSegment s = new QSegment(commonBuffer.toString(), false);
							segmentSet.add(s);
							commonBuffer.delete(0, commonBuffer.length());
						}
						//
						if(singleArray[i]){
							if(intactBuffer.length()>0){
								QSegment s = new QSegment(intactBuffer.toString(), true);
								s.setType(2);
								segmentSet.add(s);
								intactBuffer.delete(0, intactBuffer.length());
							}
						}
						//
						if(doubleArray[i] && !singleArray[i]){
							intactBuffer.append(rawQ.charAt(i));
						}
					}					
				}
				//
				if(commonBuffer.length()>0){
					QSegment s = new QSegment(commonBuffer.toString(), false);
					segmentSet.add(s);
					commonBuffer.delete(0, commonBuffer.length());
				}
				//
				commonBuffer = null;
				intactBuffer = null;
				//
				return segmentSet;
			}else{
				boolean [] singleArray = new boolean[rawQ.length()];
				for(Integer index: symbolIndexSet){
					singleArray[index.intValue()] = true;
				}
				//
				StringBuffer commonBuffer = new StringBuffer();
				for(int i=0; i<rawQ.length(); i++){
					if(singleArray[i]){
						if(commonBuffer.length()>0){
							QSegment s = new QSegment(commonBuffer.toString(), false);
							segmentSet.add(s);
							commonBuffer.delete(0, commonBuffer.length());
						}
					}else{
						commonBuffer.append(rawQ.charAt(i));
					}
				}
				//
				if(commonBuffer.length()>0){
					QSegment s = new QSegment(commonBuffer.toString(), false);
					segmentSet.add(s);
					commonBuffer.delete(0, commonBuffer.length());
				}
				//
				commonBuffer = null;
				//
				return segmentSet;
			}			
		}else{
			QSegment s = new QSegment(rawQ, false);
			segmentSet.add(s);
			return segmentSet;
		}		
	}
	
	/**
	 * a dot punctuation that acts as a separator
	 * **/
	private static boolean separatorDotP(int index, String str){
		if((index-1)>=0 && index+1<str.length()){
			if(PatternFactory.containHanCharacter(str.substring(index-1, index)) || 
					PatternFactory.containHanCharacter(str.substring(index+1, index+2))){
				//
				return true;				
			}else if(!PatternFactory.allNAStr(str.substring(index-1, index)) && 
					!PatternFactory.allNAStr(str.substring(index+1, index+2))){
				//+++
				return true;
			}else{
				return false;
			}
		}else{
			//end case
			return true;
		}
	}	
	/**
	 * check the single-separator information
	 * */
	private static Vector<DPunctuationInstance> distillDotP(String rawQ){
		Vector<DPunctuationInstance> separatorSet = new Vector<DPunctuationInstance>();
		for(String separator:dotPunctuationSet){
			//
			int index = rawQ.indexOf(separator);				
			while(index>=0){
				if(separatorDotP(index, rawQ)){
					separatorSet.add(new DPunctuationInstance(separator, index));
					index = rawQ.indexOf(separator, index+1);
				}else{
					index = rawQ.indexOf(separator, index+1);
				}
			}
			/*
			if(separator.indexOf(".") >= 0){
				int index = userQuery.indexOf(separator);				
				while(index>=0){
					if(trueSymbol(index, userQuery)){
						separatorSet.add(new SingleSymbol(separator, index));
						index = userQuery.indexOf(separator, index+1);
					}else{
						index = userQuery.indexOf(separator, index+1);
					}
				}
			}else{
				int index = userQuery.indexOf(separator);
				while(index>=0){
					separatorSet.add(new SingleSymbol(separator, index));
					index = userQuery.indexOf(separator, index+1);
				}
			}
			*/
		}
		if(separatorSet.size()>0){
			Collections.sort(separatorSet);
			return separatorSet;
		}else{
			separatorSet = null;
			return separatorSet;
		}		
	}
	/**
	 * check the symmetry separator information
	 * */
	private static Vector<SPunctuationInstance> distillSymmetricP(String rawQ){
		//pre-process		
		if(rawQ.indexOf("<<")>=0){
			rawQ = rawQ.replaceAll("<<", "@");
			rawQ = rawQ.replaceAll(">>", "@");
		}
		if(rawQ.indexOf("＜＜")>=0){
			rawQ = rawQ.replaceAll("＜＜", "@");
			rawQ = rawQ.replaceAll("＞＞", "@");
		}
		//
		Vector<SPunctuationInstance> SSeparatorSet = new Vector<SPunctuationInstance>();
		Matcher matcher;
		String mStr;
		int head;
		for(SPunctuation symSeparator: symmetricPunctuationSet){
			if(rawQ.indexOf(symSeparator.symbolHead)>=0){
				matcher = symSeparator.pattern.matcher(rawQ);
				int fromIndex=0;
				while(matcher.find()){
					mStr = matcher.group();
					head = rawQ.indexOf(mStr, fromIndex);
					SPunctuationInstance instance = new SPunctuationInstance(head, 
							head+mStr.length()-1, symSeparator.symbolHead+symSeparator.symbolTail, mStr.substring(1, mStr.length()-1));
					SSeparatorSet.add(instance);
					fromIndex = head+mStr.length();
				}
			}			
		}
		if(SSeparatorSet.size()>0){
			Collections.sort(SSeparatorSet);
			return SSeparatorSet;
		}else{
			SSeparatorSet=null;
			return SSeparatorSet;
		}
	}
	/****/
	public static void main(String []args){
		///*		
		String test = "不想长大.歌曲2.3中www.net";
		String test_1 = "I-Worm/Mytob.q";
		Vector<QSegment> result = QPunctuationParser.parse(test);
		for(QSegment s: result){
			if(s.unit){
				System.out.println(s.getStr()+"\tintact");
			}else{
				System.out.println(s.getStr());
			}			
		}
		//*/
		/**2**/
		/*
		if(QuerySymbolParser.effectiveHeadTail("1")){
			System.out.println("yes");
		}else{
			System.out.println("not");
		}
		*/
	}
}
