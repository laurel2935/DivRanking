package org.archive.nlp.similarity.wordnet;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Vector;

import edu.mit.jwi.Dictionary;
import edu.mit.jwi.IDictionary;
import edu.mit.jwi.item.IIndexWord;
import edu.mit.jwi.item.IWord;
import edu.mit.jwi.item.IWordID;
import edu.mit.jwi.item.POS;


public class WordNetFactory {
	private static String wordnetPath = "C:/Program Files (x86)/WordNet/2.1/dict";	
	private static IDictionary wordnetDict = null;
	//private static IRAMDictionary wordnetDict = null;
	//initialization
	static{
		try{			
			//wordnetDict = new RAMDictionary(new File(wordnetPath), ILoadPolicy.NO_LOAD);
			wordnetDict = new Dictionary(new URL("file", null, wordnetPath));
			wordnetDict.open();
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	WordNetFactory(){}	
	//traversing  possible ���ʣ����ʣ����ݴʺ͸���
	public static boolean existInWordNet(String wordStr){
		IIndexWord idxWord = null;
		//
		if(null != (idxWord=wordnetDict.getIndexWord(wordStr, POS.NOUN))){
			/*
			IWordID wordID = (IWordID)idxWord.getWordIDs().get(0);
			IWord word = wordnetDict.getWord(wordID);
			System.out.println("Id = " + wordID);
			System.out.println("Lemma = " + word.getLemma());
			System.out.println("Gloss = " + word.getSynset().getGloss());
			*/
			return true;
		}
		if(null != (idxWord=wordnetDict.getIndexWord(wordStr, POS.VERB))){
			/*
			IWordID wordID = (IWordID)idxWord.getWordIDs().get(0);
			IWord word = wordnetDict.getWord(wordID);
			System.out.println("Id = " + wordID);
			System.out.println("Lemma = " + word.getLemma());
			System.out.println("Gloss = " + word.getSynset().getGloss());
			*/
			return true;
		}
		if(null != (idxWord=wordnetDict.getIndexWord(wordStr, POS.ADJECTIVE))){
			/*
			IWordID wordID = (IWordID)idxWord.getWordIDs().get(0);
			IWord word = wordnetDict.getWord(wordID);
			System.out.println("Id = " + wordID);
			System.out.println("Lemma = " + word.getLemma());
			System.out.println("Gloss = " + word.getSynset().getGloss());
			*/
			return true;
		}
		if(null != (idxWord=wordnetDict.getIndexWord(wordStr, POS.ADVERB))){
			/*
			IWordID wordID = (IWordID)idxWord.getWordIDs().get(0);
			IWord word = wordnetDict.getWord(wordID);
			System.out.println("Id = " + wordID);
			System.out.println("Lemma = " + word.getLemma());
			System.out.println("Gloss = " + word.getSynset().getGloss());
			*/
			return true;
		}
		//
		//System.out.println(wordStr+"\t!exist");
		return false;
	}
	//
	public static Vector<String> commonPOSVec(String word_1, String word_2){
		Vector<String> commonPOSVec = new Vector<String>();
		IIndexWord idxWord_1 = null;
		IIndexWord idxWord_2 = null;
		//
		if(null!=(idxWord_1=wordnetDict.getIndexWord(word_1, POS.NOUN)) && 
				null!=(idxWord_2=wordnetDict.getIndexWord(word_2, POS.NOUN))){
			//
			commonPOSVec.add("n");
		}
		if(null!=(idxWord_1=wordnetDict.getIndexWord(word_1, POS.VERB)) && 
				null!=(idxWord_2=wordnetDict.getIndexWord(word_2, POS.VERB))){
			//
			commonPOSVec.add("v");
		}
		if(null!=(idxWord_1=wordnetDict.getIndexWord(word_1, POS.ADJECTIVE)) && 
				null!=(idxWord_2=wordnetDict.getIndexWord(word_2, POS.ADJECTIVE))){
			//
			commonPOSVec.add("a");
		}
		if(null!=(idxWord_1=wordnetDict.getIndexWord(word_1, POS.ADVERB)) && 
				null!=(idxWord_2=wordnetDict.getIndexWord(word_2, POS.ADVERB))){
			//
			commonPOSVec.add("r");
		}
		//
		return commonPOSVec;
	}
	//
	 public void test() throws IOException {
		//����ָ��WordNet�ʵ�Ŀ¼��URL��
		 // String wnhome = System.getenv("WNHOME");
		  String wnhome = "C:/Program Files (x86)/WordNet/2.1";
		  String path = wnhome + File.separator + "dict";
		  URL url = new URL("file", null, path);
		 
		//�����ʵ���󲢴�����
		  IDictionary dict = new Dictionary(url);
		  dict.open();		  

		//��ѯmoney����ʵĵ�һ����˼��POS����Ĳ�����ʾҪѡ�����ִ��Եĺ���
		  IIndexWord idxWord = dict.getIndexWord("money", POS.NOUN);
		  IWordID wordID = (IWordID)idxWord.getWordIDs().get(0);
		  IWord word = dict.getWord(wordID);
		  System.out.println("Id = " + wordID);
		  System.out.println("Lemma = " + word.getLemma());
		  System.out.println("Gloss = " + word.getSynset().getGloss());
		 
		  //�ڶ�����˼
		  IWordID wordID2 = (IWordID)idxWord.getWordIDs().get(1);
		  IWord word2 = dict.getWord(wordID2);
		  System.out.println(word2.getSynset().getGloss());
		  //��������˼
		  IWordID wordID3 = (IWordID)idxWord.getWordIDs().get(2);
		  IWord word3 = dict.getWord(wordID3);
		  System.out.println(word3.getSynset().getGloss());
		 }
	 //
	 public static void main(String[] args){
		 /**1**/
		 String word_1 = "dog";
		 String word_2 = "dog1";
		 String word_3 = "hit";
		 String word_4 = "hit2";
		 //
		 //WordNetFactory wordnetF = new WordNetFactory();
		 WordNetFactory.existInWordNet(word_1);
		 WordNetFactory.existInWordNet(word_2);
		 WordNetFactory.existInWordNet(word_3);
		 WordNetFactory.existInWordNet(word_4);		 
	 }

}
