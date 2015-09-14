package org.archive.ntcir.sm;

import java.util.ArrayList;

import org.archive.dataset.ntcir.sm.SMSubtopicItem;
import org.archive.dataset.ntcir.sm.SubtopicInstance;

public class PolyCluster implements Comparable{
	String polysemyString;
	SMSubtopicItem delegaterItem;
	ArrayList<SMSubtopicItem> smSubtopicItemList;
	int instanceNumber = 0;
	double weight = 0.0;
	
	public PolyCluster(String polysemyString){
		this.polysemyString = polysemyString;
	}
	
	public void addSMSubtopicItem(SMSubtopicItem item){
		if(null == this.smSubtopicItemList){
			this.smSubtopicItemList = new ArrayList<SMSubtopicItem>();
		}
		this.smSubtopicItemList.add(item);
	}
	
	public void calInstanceNumber(String topicText){
		if(null == smSubtopicItemList){
			this.instanceNumber = 0;
		}else{
			for(SMSubtopicItem item: smSubtopicItemList){
				for(SubtopicInstance instance: item.subtopicInstanceGroup){
					if(instance._text.length()>0 && !instance._text.equals(topicText)){
						instanceNumber++;
					}
				}
			}
		}
	}
	
	public void setSMSubtopicItemList(ArrayList<SMSubtopicItem> smSubtopicItemList){
		this.smSubtopicItemList = smSubtopicItemList;
		
	}
	
	public int compareTo(Object o) {
		PolyCluster cmp = (PolyCluster) o;			
		if(this.instanceNumber > cmp.instanceNumber){
			return -1;
		}else if(this.instanceNumber < cmp.instanceNumber){
			return 1;
		}else{
			return 0;
		}
	}		
}
