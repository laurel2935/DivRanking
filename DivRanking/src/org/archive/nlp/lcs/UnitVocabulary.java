package org.archive.nlp.lcs;

import java.util.Collections;
import java.util.Hashtable;
import java.util.Vector;
import java.util.Map.Entry;

import org.archive.util.Language.Lang;

/**
 * Unit vocabulary
 * English: word
 * Chinese: an alphabet
 * **/

public class UnitVocabulary {
	 //alphabet to its id
     private Hashtable<String, Integer> unitTable = new Hashtable<String, Integer>();
     //alphabet in id order
     private Vector<String> unitList = new Vector<String>();
     //
     public void initialize(String []qArray, Lang language){
    	 //unit table
    	 Hashtable<String, String> tmpTable = new Hashtable<String, String>();
    	 //
    	 String q;
    	 String [] words;
    	 for(int i=0; i<qArray.length; i++){
    		 q = qArray[i];
    		 //Chinese
    		 if(Lang.Chinese == language){
    			 for(int j=0; j<q.length(); j++){    			 
        			 String strChar = String.valueOf(q.charAt(j));
        			 //alphabet one by one
        			 if(!tmpTable.containsKey(strChar)){
        				 tmpTable.put(strChar, strChar);
        			 }
        		 }
    		 }else if(Lang.English == language){
    			 words = q.split(" ");
    			 for(int j=0; j<words.length; j++){
    				 //word one by one
    				 if(!tmpTable.containsKey(words[j])){
    					 tmpTable.put(words[j], words[j]);
    				 }
    			 }    			 
    		 }else{
    			 System.out.println("Unaccepted language type!");
    		 }    		 
    	 }
    	 //unit vector
    	 Vector<String> tmpVector = new Vector<String>();
    	 //
    	 for(Entry<String, String> element : tmpTable.entrySet()){
    		 tmpVector.add(element.getValue());
    	 }    	 
    	 //
    	 Collections.sort(tmpVector);
    	 //
    	 for(String unitStr: tmpVector){
    		 addUnit(unitStr);
    	 }
     }
     //
     private int addUnit(String unitStr){
    	 //System.out.print("unitStr:\t"+unitStr+"\t");
    	 //
    	 Integer unitID = this.unitTable.get(unitStr);    	 
         //
    	 if(null == unitID){
    		 int id = this.unitTable.size();
    		 this.unitList.add(unitStr);
    		 this.unitTable.put(unitStr, id);
    		 //
    		 //System.out.print(id);
    		 //System.out.println();
    		 return id;
    	 }else{
    		 //
    		 //System.out.print(unitID);
    		 //System.out.println();
    		 return unitID;
    	 }
     }
     //
     public int getSize(){
    	 return this.unitList.size();
     }
     //
     public int getID(String unitStr){
    	 return this.unitTable.get(unitStr);
     }
     //
     public String getUnit(int id){
    	 return this.unitList.get(id);
     }
}
