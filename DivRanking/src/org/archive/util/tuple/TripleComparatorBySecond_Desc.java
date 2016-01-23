package org.archive.util.tuple;

import java.util.Comparator;

public class TripleComparatorBySecond_Desc <FIRST extends Comparable<? super FIRST>,
											SECOND extends Comparable<? super SECOND>,
											THIRD extends Comparable<? super THIRD>>
												implements Comparator<Triple <FIRST, SECOND, THIRD>> {
	//@Override
	public int compare(Triple <FIRST, SECOND, THIRD> obj1, Triple <FIRST, SECOND, THIRD> obj2){
		if(null!=obj1.second && null!=obj2.second){
			return obj2.second.compareTo(obj1.second);			
		}else if(null != obj1.second){
			return -1;
		}else if(null != obj2.second){
			return 1;
		}else{
			return 0;
		}		
	}
}
