package org.archive.dataset.ntcir.sm;

import java.util.ArrayList;

import edu.stanford.nlp.ling.TaggedWord;

public class TaggedTopic {
	public String _pennString;	
	public ArrayList<TaggedTerm> _taggedTerms;
	public ArrayList<ArrayList<TaggedTerm>> _taggedPhraseList;
	
	public TaggedTopic(){}
	
	//
	public void setPennString(String pennString){
		this._pennString = pennString;
	}	
	public void setTaggedTerms(ArrayList<TaggedTerm> taggedTerms){
		this._taggedTerms = taggedTerms;
	}
	public void setTaggedPhraseList(ArrayList<ArrayList<TaggedTerm>> taggedPhraseList){
		this._taggedPhraseList = taggedPhraseList;
	}
	//
	public String toString(){
		StringBuffer strBuffer = new StringBuffer();
		strBuffer.append("Tagged Terms:");
		for(TaggedTerm taggedTerm: _taggedTerms){
			strBuffer.append(taggedTerm.toString()+"\t");			
		}
		strBuffer.append("\n");		
		if(null != _taggedPhraseList){
			int i = 1;
			for(ArrayList<TaggedTerm> taggedPhrase: _taggedPhraseList){
				strBuffer.append("Tagged Phrase["+(i++)+"]:"+taggedPhrase.toString()+"\n");			
			}
			//strBuffer.append("\n");
		}		
		return strBuffer.toString();		
	}

}
