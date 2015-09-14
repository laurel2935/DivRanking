package org.archive.dataset.ntcir;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import org.archive.util.tuple.Pair;
import org.archive.util.tuple.PairComparatorBySecond_Desc;


/**
 * A subtopic string or a document, and the labeled relevance level
 * **/
class LabeledItem implements Comparable{
	Integer itemIndex;
	Integer releLevel;
	private transient final int hash;
	
	//
	LabeledItem(Integer tStrID, Integer level)
	{
		this.itemIndex = tStrID;
		this.releLevel = level;
		//
		this.hash = (tStrID==null? 0 : tStrID.hashCode()*31)+(level==null? 0 : level.hashCode());
	}
	//
	public  int compareTo(Object o){
		LabeledItem comp = (LabeledItem)o;
		if(this.releLevel > comp.releLevel){
			return -1;
		}else if(this.releLevel < comp.releLevel){
			return 1;
		}else{
			return 0;
		}
	}
	//
	//
	@Override
    public int hashCode()
    {
		return hash;
    }
    @Override
    public boolean equals(Object cmp)
    {
    	if(this == cmp){
        	return true;
        }
        if (cmp == null || !(getClass().isInstance(cmp)))
        {
        	return false;
        }
        LabeledItem other = this.getClass().cast(cmp);
        return (itemIndex==null? other.itemIndex==null : itemIndex.equals(other.itemIndex))
        && (releLevel==null? other.releLevel==null : releLevel.equals(other.releLevel));
    }
}

public class NTCIRTopic {
	public static final boolean DEBUG = false;
	
	//topic id
	private String _id;
	//raw text
	private String _text;
	//e.g., simple segmented format
	private String _representation;
	//sequential subtopics or intents with respect to their probability
	//subtopic index -> probability
	private HashMap<Integer, Double> _iprobMap;
	//subtopic index -> relevant item list
	private HashMap<Integer, ArrayList<LabeledItem>> _subtopicToReleItems;
	//subtopic strings or doc_names
	private ArrayList<String> _itemPooL;
	//item -> item index
	private HashMap<String, Integer> _itemToIndex;
	//item index -> multiple cases of relevance regarding to multiple subtopics or intents
	//Pair<Integer, Integer> denotes subtopic index & relevance level
	private HashMap<Integer, HashSet<Pair<Integer, Integer>>> _itemUsageFingerprint;
	//ranked item list by global gain value
	private ArrayList<Pair<String, Double>> _idealListDecByGG;
	
	
	public NTCIRTopic(String id, String text, String naiveSegment){
		this._id = id;
		this._text = text;
		//
		this._representation = naiveSegment;
		//
		this._iprobMap = new HashMap<Integer, Double>();
		this._subtopicToReleItems = new HashMap<Integer, ArrayList<LabeledItem>>();
		this._itemPooL = new ArrayList<String>();
		this._itemToIndex = new HashMap<String, Integer>();
		this._itemUsageFingerprint = new HashMap<Integer, HashSet<Pair<Integer,Integer>>>();
		this._idealListDecByGG = new ArrayList<Pair<String,Double>>();
	}
	
	//compute I-rec, D-nDCG, D#-nDCG per topic
	public ArrayList<Double> cal(ArrayList<String> sysRankedItemList, int cutoff){		
		//preprocess
		getIdealListByGG();
		//sequential metric value: I-rec -> D-nDCG -> D#-nDCG  
		ArrayList<Double> metricTriple = new ArrayList<Double>();
		if(null != sysRankedItemList){			
			//I-rec			
			HashSet<Integer> diffSubtopicIndexSet = new HashSet<Integer>();
			int cursor = Math.min(sysRankedItemList.size(), cutoff);
			for(int k=0; k<cursor; k++){
				String k_th_item = sysRankedItemList.get(k);
				//
				if(this._itemToIndex.containsKey(k_th_item)){
					Iterator<Pair<Integer, Integer>> itr = this._itemUsageFingerprint.get(
							this._itemToIndex.get(k_th_item)).iterator();
					while(itr.hasNext()){
						Pair<Integer, Integer> pairOfSubtopicIndexAndLevel = itr.next();
						diffSubtopicIndexSet.add(pairOfSubtopicIndexAndLevel.first);
					}
				}				
			}			
			//System.out.println("Included intent Number:\t"+inSubtopicIDSet.size());			
			double Irec = diffSubtopicIndexSet.size()*1.0/this._iprobMap.size();
			if (DEBUG) {
				System.out.println("_iprobMap\t"+this._iprobMap.size());
				System.out.println("Irec\t"+Irec);
			}
			//MSnDCG
			double sysCGN = getSysCGN(sysRankedItemList, cutoff);
			double idealCGN = getIdealCGN(cutoff);
			//System.out.println("System cgv:\t"+sysCGN);
			//System.out.println("Ideal cgv:\t"+idealCGN);
			double msnDCG = sysCGN/idealCGN;
			//D#-nDCG
			double DSharpnDCG = Irec*0.5 + msnDCG*0.5;
			//			
			///*
			DecimalFormat df = new DecimalFormat("0.0000");
			metricTriple.add(Double.parseDouble(df.format(Irec)));
			metricTriple.add(Double.parseDouble(df.format(msnDCG)));
			metricTriple.add(Double.parseDouble(df.format(DSharpnDCG)));
			//*/
			/*
			metricTriple.add(Irec);
			metricTriple.add(msnDCG);
			metricTriple.add(DSharpnDCG);	
			*/
		}else{
			//no result case
			metricTriple.add(0.0);
			metricTriple.add(0.0);
			metricTriple.add(0.0);				
		}
		
		return metricTriple;
	}
	//ranked item list by global gain value
	private void getIdealListByGG(){
		//global ideal list for each topic		
		if (DEBUG) {
			System.out.println("_subtopicToReleItems 's size:\t" + _subtopicToReleItems.size());
		}
		for(String item: this._itemPooL){
			int itemIndex = this._itemToIndex.get(item);
			HashSet<Pair<Integer, Integer>> releItemSet = this._itemUsageFingerprint.get(itemIndex);
			Iterator<Pair<Integer, Integer>> itr = releItemSet.iterator();
			//global gain
			double gg = 0.0;
			while(itr.hasNext())
			{
				Pair<Integer, Integer> subtopicIndexToLevel = itr.next();
				//
				gg += this._iprobMap.get(subtopicIndexToLevel.first)*subtopicIndexToLevel.second;
			}
			//				
			_idealListDecByGG.add(new Pair<String,Double>(item, gg));
		}
		//
		Collections.sort(_idealListDecByGG, new PairComparatorBySecond_Desc<String, Double>());	
		/*
		for(Entry<Integer, ArrayList<LabeledItem>> entry: this._subtopicToReleItems.entrySet())
		{			
					
		}
		*/
	}
	//
	public void addIprob(int subtopicIndex, double p){
		this._iprobMap.put(subtopicIndex, p);
	}
	
	public void addLabeledItem(int subtopicIndex, String item, int level)
	{
		int itemIndex = getItemIndex(item);
		//
		LabeledItem labeledItem = new LabeledItem(itemIndex, level);
		//
		addLabeledItemForIthSubtopic(subtopicIndex, labeledItem);
		//
		recordItemUsage(itemIndex, subtopicIndex, level);
	}
	//system's cumulative gain value
	private double getSysCGN(ArrayList<String> sysRankedItemList, int cutoff){
		int cursor = Math.min(sysRankedItemList.size(), cutoff);
		double cgn = 0.0;
		for(int k=0; k<cursor; k++){
			String k_th_item = sysRankedItemList.get(k);
			for(Pair<String, Double> idealRankedItemByGG: this._idealListDecByGG){
				if(idealRankedItemByGG.first.equals(k_th_item)){
					cgn += idealRankedItemByGG.second/Math.log10(k+2);
					//
					break;
				}
			}
		}		
		return cgn;
	}
	//
	private double getIdealCGN(int cutoff){
		double cgn = 0.0;
		int cursor = Math.min(cutoff, this._idealListDecByGG.size());
		for(int i=0; i<cursor; i++){
			cgn += this._idealListDecByGG.get(i).second/Math.log10(i+2);
		}
		return cgn;
	}
	
	public void getSubtopicCoverage(){
		
	}
	
	private int getItemIndex(String item)
	{
		Integer itemIndex = this._itemToIndex.get(item);
		if(null == itemIndex)
		{
			int index = this._itemPooL.size();
			this._itemPooL.add(item);
			this._itemToIndex.put(item, index);
			//
			return index;
		}else{
			return itemIndex;
		}
	}
	
	private void addLabeledItemForIthSubtopic(int subtopicIndex, LabeledItem labeledItem)
	{		
		//
		if(this._subtopicToReleItems.containsKey(subtopicIndex)){
			ArrayList<LabeledItem> tStrList = this._subtopicToReleItems.get(subtopicIndex);
			tStrList.add(labeledItem);
		}else{
			ArrayList<LabeledItem> tStrList = new ArrayList<LabeledItem>();
			tStrList.add(labeledItem);
			this._subtopicToReleItems.put(subtopicIndex, tStrList);
		}
	}
	
	//record the relevant intent set per tStr
	private void recordItemUsage(int itemIndex, int subtopicIndex, int level)
	{
		if(this._itemUsageFingerprint.containsKey(itemIndex))
		{
			HashSet<Pair<Integer, Integer>> set = this._itemUsageFingerprint.get(itemIndex);
			set.add(new Pair<Integer, Integer>(subtopicIndex, level));
		}else{
			HashSet<Pair<Integer, Integer>> set = new HashSet<Pair<Integer, Integer>>();
			set.add(new Pair<Integer, Integer>(subtopicIndex, level));
			this._itemUsageFingerprint.put(itemIndex, set);
		}
	}
		
	public String getTopicID(){
		return this._id;
	}
	
	public String getTopicText(){
		return this._text;
	}
	
	public String getTopicRepresentation(){
		return this._representation;
	}
}
