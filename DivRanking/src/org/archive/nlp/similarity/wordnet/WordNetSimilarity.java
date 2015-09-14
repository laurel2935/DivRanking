package org.archive.nlp.similarity.wordnet;

import java.util.TreeMap;
import java.util.Vector;

import edu.sussex.nlp.jws.JWS;
import edu.sussex.nlp.jws.JiangAndConrath;


public class WordNetSimilarity {
	private static String wordnetPath = null;
	private static String version = null;	
	private static JWS	ws = null;	
	private static JiangAndConrath jcn = null;
	//
	static{
		wordnetPath = "C:/Program Files (x86)/WordNet";
		version = "2.1";		
		ws = new JWS(wordnetPath, version);
		jcn = ws.getJiangAndConrath();
	}		
	//on the premise that existing in WordNet
	public static double JCSimilarity_Average(String word_1, String word_2){
		//not included
		if(!WordNetFactory.existInWordNet(word_1) || !WordNetFactory.existInWordNet(word_2)){
			return 0.0;
		}
		
		Vector<String> commonPOSVec = WordNetFactory.commonPOSVec(word_1, word_2);
		
		if(null==commonPOSVec || commonPOSVec.size()==0){
			return 0.0;
		}
		
		int count = 0;
		double temp;
		double sum = 0;
		for(String pos: commonPOSVec){
			
			if(null == pos || null==word_1 || null==word_2){
				continue;
			}
			TreeMap<String, Double> scores = null;
			try {
				scores = jcn.jcn(word_1, word_2, pos);
			} catch (Exception e) {
				// TODO: handle exception
				//e.printStackTrace();
				//System.err.println("En Similarity Error!");
				continue;
			}
			
			
			if(null == scores){
				continue;
			}
			
			for(String s : scores.keySet()){
				//System.out.println(s + "\t" + scores.get(s));
				//
				temp = scores.get(s);				
				if(temp > 0){
					count ++;
					if(temp > 1){
						temp = 1;
					}
					sum += temp;
				}
				
			}
		}	
		//
		
		if(0==count){
			return 0.0;
		}
		
		double value = sum/count;
		//System.out.println(value);
		return value;			
	}
	//
	public static double JCSimilarity_Max(String word_1, String word_2){
		Vector<String> commonPOSVec = WordNetFactory.commonPOSVec(word_1, word_2);
		int count = 0;
		double temp;
		double sum = 0;
		for(String pos: commonPOSVec){
			//System.out.println(pos);
			temp = jcn.max(word_1, word_2, pos);
			if(temp > 1){
				temp = 1;
			}
			sum += temp;
			//System.out.println(sum);
			count++;
		}		
		//System.out.println(count);
		double value = sum/count;
		System.out.println(value);
		return value;
	}
	//
	public static void main(String []args){
		/**1**/
		/*
		apple#n#1,banana#n#1	0.04658846107981151
		apple#n#1,banana#n#2	0.13115409462626942
		apple#n#2,banana#n#1	0.0
		apple#n#2,banana#n#2	0.0
		specific pair	=	0.04658846107981151
		highest score	=	0.13115409462626942
		 * */		
		String word_1 = "hit";
		String word_2 = "shoot";
		WordNetSimilarity.JCSimilarity_Average(word_1, word_2);
		WordNetSimilarity.JCSimilarity_Max(word_1, word_2);
	}
}
