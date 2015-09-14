package org.archive.nlp.similarity.editdistance.definition;

import java.util.ArrayList;
import java.util.List;

import org.archive.util.tuple.StrStr;

// sequential tokens 
public class SuperString<T> {
	private List<T> contents = new ArrayList<T>();
	
	public SuperString(List<T> contents){
		this.contents = contents;
	}
	
	public static SuperString<CharEditUnit> createCharSuperString(String str){
		List<CharEditUnit> list = new ArrayList<CharEditUnit>(str.length());
		for(int i=0; i<str.length(); i++){
			list.add(new CharEditUnit(str.charAt(i)));
		}
		SuperString<CharEditUnit> s = new SuperString<CharEditUnit>(list);
		return s;
	}
	
	public static SuperString<TermEditUnit> createTermSuperString_1(ArrayList<String> wList){
		//
		List<TermEditUnit> unitList = new ArrayList<TermEditUnit>(wList.size());
		for(int i=0; i<wList.size(); i++){
			unitList.add(new TermEditUnit(wList.get(i)));
		}
		SuperString<TermEditUnit> s = new SuperString<TermEditUnit>(unitList);
		return s;
	}
	
	public static SuperString<TermEditUnit> createTermSuperString_2(ArrayList<StrStr> wList){
		//
		List<TermEditUnit> unitList = new ArrayList<TermEditUnit>(wList.size());
		for(int i=0; i<wList.size(); i++){
			unitList.add(new TermEditUnit(wList.get(i).getFirst(), wList.get(i).getSecond()));
		}
		SuperString<TermEditUnit> s = new SuperString<TermEditUnit>(unitList);
		return s;
	}
	
	public static SuperString<SemanticTermEditUnit> createSemanticTermSuperString_1(ArrayList<StrStr> wList){
		//
		List<SemanticTermEditUnit> unitList = new ArrayList<SemanticTermEditUnit>(wList.size());
		for(int i=0; i<wList.size(); i++){
			unitList.add(new SemanticTermEditUnit(wList.get(i).getFirst()));
		}
		SuperString<SemanticTermEditUnit> s = new SuperString<SemanticTermEditUnit>(unitList);
		return s;
	}
	
	public static SuperString<SemanticTermEditUnit> createSemanticTermSuperString_2(ArrayList<String> wList){
		//
		List<SemanticTermEditUnit> unitList = new ArrayList<SemanticTermEditUnit>(wList.size());
		for(int i=0; i<wList.size(); i++){
			unitList.add(new SemanticTermEditUnit(wList.get(i)));
		}
		SuperString<SemanticTermEditUnit> s = new SuperString<SemanticTermEditUnit>(unitList);
		return s;
	}
	
	public static SuperString<RawTermEditUnit> createRawTermSuperString(ArrayList<String> wList){
		//
		List<RawTermEditUnit> unitList = new ArrayList<RawTermEditUnit>(wList.size());
		for(int i=0; i<wList.size(); i++){
			unitList.add(new RawTermEditUnit(wList.get(i)));
		}
		SuperString<RawTermEditUnit> s = new SuperString<RawTermEditUnit>(unitList);
		return s;
	}
	
	public T elementAt(int pos){
		if(pos<0 || pos>=contents.size()){
			throw new ArrayIndexOutOfBoundsException("!!");
		}
		return contents.get(pos);
	}
	
	public int indexOf(SuperString<?> substring){
		int result = -1;
		for(int i=0; i<length(); i++){
			int j=0;
			if(i+substring.length()>length()) return -1;
			
			for(;j<substring.length();j++){				
				if(elementAt(i+j).equals(substring.elementAt(j))){
					continue;					
				}else{
					break;
				}
			}
			if(j==substring.length()){
				return i;
			}
		}
		return result;
	}
	
	public SuperString<T> substring(int fromIndex, int toIndex){
		return new SuperString<T>(contents.subList(fromIndex, toIndex));
	}
	
	public SuperString<T> substring(int fromIndex){
		return new SuperString<T>(contents.subList(fromIndex, contents.size()));
	}
	
	public int length(){
		return contents.size();
	}
	
	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder();
		for(int i=0; i<length(); i++){
			sb.append(elementAt(i));
		}
		return sb.toString();
	}
	
	@Override
	public boolean equals(Object other){
    	return toString().equals(other.toString());
	}
}
