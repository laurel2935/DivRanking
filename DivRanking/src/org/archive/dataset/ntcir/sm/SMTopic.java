package org.archive.dataset.ntcir.sm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.archive.util.tuple.StrInt;

public class SMTopic {
	private static boolean DEBUG = false;
	public static enum BadCase{NoRelatedQuery, NoIRAnnotation}
	
	//
	public static final int ShrinkThreshold = 2;
	private ArrayList<BadCase> badCases = null;
	
	private String _id;
	private String _text;
	private TaggedTopic _taggedTopic;
	public boolean DirectTermAndNoIRAnnotation = false;
	public boolean CompleteSentence = false;
	private ArrayList<IRAnnotation> _termIRAnnotationList;
	private ArrayList<IRAnnotation> _phraseIRAnnotationList;
	//for Chinese topic
	public ArrayList<String> suggestionSougou;
	public ArrayList<String> suggestionBaidu;
	public ArrayList<String> relatedQList;	
	//for English topic
	public ArrayList<String> suggestionYahoo;
	//both
	public ArrayList<String> suggestionBing;	
	public ArrayList<String> suggestionGoogle;	
	
	//
	public ArrayList<String> uniqueRelatedQueries;
	public HashMap<String, StrInt> rqMap;
	
	public ArrayList<SMSubtopicItem> smSubtopicItemList;
	public ArrayList<SubtopicInstance> oddSubtopicInstances;
	
	//polysemy
	public ArrayList<String> polysemyList;
	//for en topic
	public boolean listStyle = false;
	
	public SMTopic(String id, String text){
		this._id = id;
		this._text = text;
		//
		suggestionBaidu = new ArrayList<String>();
		suggestionBing = new ArrayList<String>();
		suggestionGoogle = new ArrayList<String>();
		suggestionSougou = new ArrayList<String>();
		suggestionYahoo = new ArrayList<String>();
		relatedQList = new ArrayList<String>();
	}
	//
	public void addBaidu(String baSuggestion){
		suggestionBaidu.add(baSuggestion);
	}
	public void addBing(String biSuggestion){
		suggestionBing.add(biSuggestion);
	}
	public void addGoogle(String gSuggestion){
		suggestionGoogle.add(gSuggestion);
	}
	public void addSougou(String sSuggestion){
		suggestionSougou.add(sSuggestion);
	}
	public void addYahoo(String ySuggestion){
		suggestionYahoo.add(ySuggestion);
	}
	public void addRelatedQ(String rSuggestion){
		relatedQList.add(rSuggestion);
	}
	//
	public String getID(){
		return this._id;
	}
	public String getTopicText(){
		return this._text;
	}
	//
	public void setSentenceState(boolean aSentence){
		this.CompleteSentence = aSentence;
	}
	//
	public void setTaggedTopic(TaggedTopic taggedTopic){
		this._taggedTopic = taggedTopic;
	}
	public TaggedTopic getTaggedTopic(){
		return this._taggedTopic;
	}
	//
	public ArrayList<IRAnnotation> getTermIRAnnotations(){
		return this._termIRAnnotationList;
	}
	public void setTermIRAnnotations(ArrayList<IRAnnotation> termIRAnnotationList){
		this._termIRAnnotationList = termIRAnnotationList;
	}
	public ArrayList<IRAnnotation> getPhraseIRAnnotations(){
		return this._phraseIRAnnotationList;
	}
	public void setPhraseIRAnnotations(ArrayList<IRAnnotation> phraseIRAnnotationList){
		this._phraseIRAnnotationList = phraseIRAnnotationList;
	}
	//
	public void getUniqueRelatedQueries(){
		this.uniqueRelatedQueries = new ArrayList<String>();
		
		this.rqMap = new HashMap<String, StrInt>();
		
		//HashSet<String> qSet = new HashSet<String>();
		for(String q: this.suggestionBaidu){
			if(!rqMap.containsKey(q)){
				StrInt strInt = new StrInt(q);
				rqMap.put(q, strInt);				
				
				this.uniqueRelatedQueries.add(q);
			}else{
				rqMap.get(q).intPlus1();
			}
		}
		for(String q: this.suggestionBing){
			if(!rqMap.containsKey(q)){
				StrInt strInt = new StrInt(q);
				rqMap.put(q, strInt);				
				
				this.uniqueRelatedQueries.add(q);
			}else{
				rqMap.get(q).intPlus1();
			}
		}
		for(String q: this.suggestionGoogle){
			if(!rqMap.containsKey(q)){
				StrInt strInt = new StrInt(q);
				rqMap.put(q, strInt);				
				
				this.uniqueRelatedQueries.add(q);
			}else{
				rqMap.get(q).intPlus1();
			}
		}
		for(String q: this.suggestionSougou){
			if(!rqMap.containsKey(q)){
				StrInt strInt = new StrInt(q);
				rqMap.put(q, strInt);				
				
				this.uniqueRelatedQueries.add(q);
			}else{
				rqMap.get(q).intPlus1();
			}
		}
		for(String q: this.suggestionYahoo){
			if(!rqMap.containsKey(q)){
				StrInt strInt = new StrInt(q);
				rqMap.put(q, strInt);				
				
				this.uniqueRelatedQueries.add(q);
			}else{
				rqMap.get(q).intPlus1();
			}
		}
		for(String q: this.relatedQList){
			if(!rqMap.containsKey(q)){
				StrInt strInt = new StrInt(q);
				rqMap.put(q, strInt);				
				
				this.uniqueRelatedQueries.add(q);
			}else{
				rqMap.get(q).intPlus1();
			}
		}
		//
		if(this.uniqueRelatedQueries.size() == 0){
			this.badCases.add(BadCase.NoRelatedQuery);
		}
	}
	//
	public void checkIRAnnotation(){		
		if(null==this._termIRAnnotationList && null==this._phraseIRAnnotationList){
			if(null == this.badCases){
				this.badCases = new ArrayList<SMTopic.BadCase>();
			}
			this.badCases.add(BadCase.NoIRAnnotation);
		}
	}
	//
	public String toString(){
		StringBuffer strBuffer = new StringBuffer();
		strBuffer.append(this._id+" - "+this._text+"\n");
		if(null != this._taggedTopic){
			strBuffer.append(this._taggedTopic.toString());
		}		
		return strBuffer.toString();
	}
	//
	public boolean belongToBadCase(){
		return null!=this.badCases;
	}
	public void printBadCase(){
		System.out.println(this.badCases);
	}
	//first meet first match, which may leads to non-consistent results
	public void getSubtopicItemList(ArrayList<SubtopicInstance> subtopicInstanceList){
		if(DEBUG){
			System.out.println("subtopicInstance number:\t"+subtopicInstanceList.size());
		}
		//
		this.oddSubtopicInstances = new ArrayList<SubtopicInstance>();
		//
		this.smSubtopicItemList = new ArrayList<SMSubtopicItem>();
		//
		SubtopicInstance subtopicInstance = subtopicInstanceList.get(subtopicInstanceList.size()-1);
		if(DEBUG){
			System.out.println(subtopicInstance.toString());
		}
		//
		while(subtopicInstance.belongToOddCase()){
			if(DEBUG){
				System.out.println("odd case:\t"+subtopicInstance.toString());
			}
			//
			this.oddSubtopicInstances.add(subtopicInstance);
			subtopicInstanceList.remove(subtopicInstanceList.size()-1);
			subtopicInstance = null;
			//
			if(subtopicInstanceList.size() > 0){
				subtopicInstance = subtopicInstanceList.get(subtopicInstanceList.size()-1);
			}else{				
				break;
			}			
		}
		if(null != subtopicInstance){
			SMSubtopicItem smSubtopicItem = new SMSubtopicItem(subtopicInstance);
			if(DEBUG){
				System.out.println("add new item:\t"+subtopicInstance.toString());
			}
			this.smSubtopicItemList.add(smSubtopicItem);
			//
			subtopicInstanceList.remove(subtopicInstanceList.size()-1);			
		}		
		//
		while(subtopicInstanceList.size() > 0){
			SubtopicInstance instance = subtopicInstanceList.get(subtopicInstanceList.size()-1);
			if(DEBUG){
				System.out.println("running for:\t"+instance.toString());
			}
			//
			if(instance.belongToOddCase()){
				if(DEBUG){
					System.out.println("odd case:\t"+instance.toString());
				}
				this.oddSubtopicInstances.add(instance);
				subtopicInstanceList.remove(subtopicInstanceList.size()-1);
			}else{
				boolean matched = false;
				for(SMSubtopicItem item: this.smSubtopicItemList){
					if(instance.shrinkMatch(item.itemDelegater)){
						item.addSubtopicInstance(instance);
						matched = true;
						if(DEBUG){
							System.out.println("matched for :\t"+instance.toString());
						}
						break;
					}
					/*
					for(SubtopicInstance taggedInstance: item.subtopicInstanceGroup){
						if(DEBUG){
							System.out.println("matching ---\t"+taggedInstance.toString());
						}
						if(instance.shrinkMatch(taggedInstance)){
							item.addSubtopicInstance(instance);
							matched = true;
							if(DEBUG){
								System.out.println("matched for :\t"+instance.toString());
							}
							break;
						}
					}
					if(matched){
						break;
					}
					*/
				}
				//
				if(matched){
					subtopicInstanceList.remove(subtopicInstanceList.size()-1);
				}else{
					SMSubtopicItem newSmItem = new SMSubtopicItem(instance);
					this.smSubtopicItemList.add(newSmItem);
					if(DEBUG){
						System.out.println("add new item:\t"+instance.toString());
					}
					subtopicInstanceList.remove(subtopicInstanceList.size()-1);
				}
			}			
		}
		//
		for(SMSubtopicItem smSubtopicItem: this.smSubtopicItemList){
			smSubtopicItem.getModifierGroupList();
			smSubtopicItem.resetDelegater(this);
		}
	}
	
	public void setPolysemyList(ArrayList<String> polysemyList){
		this.polysemyList = polysemyList;
	}
}
