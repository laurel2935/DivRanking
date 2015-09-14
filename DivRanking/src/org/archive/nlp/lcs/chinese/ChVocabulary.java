package org.archive.nlp.lcs.chinese;

import java.util.Collections;
import java.util.Hashtable;
import java.util.Vector;
import java.util.Map.Entry;

import org.archive.util.tuple.StrInt;

/**
 * abstract vocabulary
 * with a Chinese alphabet as an element
 * **/

public class ChVocabulary {
	 //alphabet to its id
     private Hashtable<String, Integer> chAlphabetTable = new Hashtable<String, Integer>();
     //alphabet in id order
     private Vector<String> chAlphabetList = new Vector<String>();
     //
     public void initialize(Vector<StrInt> ClickThroughQueryList){
    	 Hashtable<String, String> tmpTable = new Hashtable<String, String>();
    	 //
    	 for(StrInt instance: ClickThroughQueryList){
    		 String str = instance.first;
    		 //
    		 for(int j=0; j<str.length(); j++){    			 
    			 String strChar = String.valueOf(str.charAt(j));
    			 //
    			 if(!tmpTable.containsKey(strChar)){
    				 tmpTable.put(strChar, strChar);
    			 }
    		 }
    	 }    	 
    	 //
    	 Vector<String> tmpVector = new Vector<String>();
    	 //
    	 for(Entry<String, String> element : tmpTable.entrySet()){
    		 tmpVector.add(element.getValue());
    	 }
    	 Collections.sort(tmpVector);
    	 //
    	 for(String strChar: tmpVector){
    		 addAlphabet(strChar);
    	 }
     }
     //
     public void initialize(String []strArray){
    	 Hashtable<String, String> tmpTable = new Hashtable<String, String>();
    	 //
    	 for(int i=0; i<strArray.length; i++){
    		 String str = strArray[i];
    		 for(int j=0; j<str.length(); j++){    			 
    			 String strChar = String.valueOf(str.charAt(j));
    			 //
    			 if(!tmpTable.containsKey(strChar)){
    				 tmpTable.put(strChar, strChar);
    			 }
    		 }
    	 }
    	 //
    	 Vector<String> tmpVector = new Vector<String>();
    	 //
    	 for(Entry<String, String> element : tmpTable.entrySet()){
    		 tmpVector.add(element.getValue());
    	 }
    	 Collections.sort(tmpVector);
    	 //
    	 for(String strChar: tmpVector){
    		 addAlphabet(strChar);
    	 }
     }
     //
     private int addAlphabet(String strAlphabet){
    	 Integer currentID = this.chAlphabetTable.get(strAlphabet);    	 
         //
    	 if(null == currentID){
    		 int id = this.chAlphabetTable.size();
    		 this.chAlphabetList.add(strAlphabet);
    		 this.chAlphabetTable.put(strAlphabet, id);
    		 return id;
    	 }else{
    		 return currentID;
    	 }
     }
     //
     public int getSize(){
    	 return this.chAlphabetList.size();
     }
     //
     public int getID(String charStr){
    	 return this.chAlphabetTable.get(charStr);
     }
     //
     public String getAlphabet(int id){
    	 return this.chAlphabetList.get(id);
     }
}
