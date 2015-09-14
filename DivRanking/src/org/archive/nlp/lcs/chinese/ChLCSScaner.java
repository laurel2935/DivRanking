package org.archive.nlp.lcs.chinese;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Vector;

import org.archive.util.tuple.PairComparatorBySecond_Desc;
import org.archive.util.tuple.StrInt;
import org.archive.util.tuple.StrIntComparatorByFirst_Desc;

//for check
class Matrix{
	public int level;
	public int index;
	public int parent;
	public ChTreeNode node;
	Matrix(int level, int index, int parent, ChTreeNode node){
		this.level = level;
		this.index = index;
		this.parent = parent;
		this.node = node;
	}
}
/**
 * 
 * **/
public class ChLCSScaner {
	Vector<String> queryVec;	
	int queryNumber;
	//
	ChVocabulary vocab;
	//
	ChTreeNode root = null;	
	//for the case that multiple same los
	public String los_All = null;	
	public static Vector<String> LOS_ALL = new Vector<String>();	
	//
	public ChLCSScaner(){
		
	}
	//
	ChLCSScaner(Vector<String> strSet){
		this.queryVec = new Vector<String>();
		//
		for(String str: strSet){
			this.queryVec.add(str);
		}
	}
	//
	private void initializeVocab(String [] strArray){
		this.vocab = new ChVocabulary();
		//
		this.vocab.initialize(strArray);
	}
	//
	private void buildSuffixTree(String [] strArray){
		this.root = new ChTreeNode(-1, -1, -1, null);
		//
		this.initializeVocab(strArray);
		
		//System.out.println(this.vocab.getSize());
		
		this.root.setChildrenSize(this.vocab.getSize());
		//
		String str;
		ChTreeNode node;
		for(int i=0; i<strArray.length; i++){
			str = strArray[i];
			//
			for(int j=0; j<str.length(); j++){
				node = this.root;
				for(int k=j; k<str.length(); k++){
					node = node.addChildNode(i, k, this.vocab.getID(String.valueOf(str.charAt(k))));
				}				
			}			
		}
	}
	//
	public void buildSuffixTree(Vector<String> strVector){
		this.queryVec = strVector;
		this.queryNumber = strVector.size();
		//
		String [] strArray = (String[])this.queryVec.toArray(new String[1]);
		this.buildSuffixTree(strArray);
	}	
	//breadth first traverse for checking something
	public void breadthFirstTraverse(ChTreeNode root){
    	//
    	Queue<Matrix> queue = new LinkedList<Matrix>();
    	//
    	int []index = new int[10];
    	for(int i=0; i<10; i++){        		
    		index[i] = 0;        		
    	}
    	//
    	for(ChTreeNode child: root.children){
    		queue.offer(new Matrix(0, index[0]++, 0, child));
    	}
    	//
    	while(!queue.isEmpty()){
    		Matrix m = queue.poll();
    		//
    		System.out.println(m.level+":"+m.index+":"+m.parent+"@"+
    				this.vocab.getAlphabet(m.node.chVocabID)+"\t:"+m.node.chVocabID+"\t"+m.node.nodeAlphabet.appearCount);
    		if(m.node.hasChildren()){
    			for(ChTreeNode child: m.node.children){
    				queue.offer(new Matrix(m.level+1, index[m.level+1]++, m.index, child));
    			}
    		}
    	}
    }
	//
	public void getLOS(){
		this.los_All = this.getLOSAll();		
	}
	//
	public String getLOSAll(){
		return this.getLOSAll(this.root, this.queryNumber);
	}
	//
	public String getLOSAll_K(int kSource){
		return this.getLOSAll_K(this.root, kSource, null);
	}
	/**
	 * @param s the number of strings that lcs derives from
	 * 
	 * enumerate all the longest common substrings 
	 * that are shared by at least common s strings
	 * Note that: lcs in common k, is not equal to lcsSet of each pair member query 
	 * **/
	public void enumerateLCS_AtLeastS(int s){		
		Hashtable<String, StrInt> strTable = new Hashtable<String, StrInt>();
		//
		for(int k=s; k<=this.queryNumber; k++){
			System.out.println("Common - "+k);
			this.getLOSAll_K(k);
			for(String cStr: this.LOS_ALL){
				System.out.println("\t"+cStr);
				//
				if(strTable.containsKey(cStr)){
					strTable.get(cStr).intPlus1();
				}else{
					strTable.put(cStr, new StrInt(cStr));
				}
			}		
			//
			if(this.LOS_ALL.size()>0){
				this.LOS_ALL.clear();
			}			
		}
		//
		Enumeration<StrInt> commonEnum = strTable.elements();
		ArrayList<StrInt> commonList = Collections.list(commonEnum);
		Collections.sort(commonList, new PairComparatorBySecond_Desc<String, Integer>());
		//
		System.out.println("-------------");
		for(StrInt element: commonList){
			System.out.println(element.second+"\t"+element.first);
		}	
		System.out.println();		
	}
	//
	public String getLOSAll(ChTreeNode root, int queryNum){
		String los = "";
		String tmp;
		//
		if(null == root.parent){
			if(root.hasChildren()){
				for(ChTreeNode child: root.children){
					tmp = getLOSAll(child, queryNum);
					if(los.length() < tmp.length()){
						los = tmp;
						//
						this.LOS_ALL.clear();
						this.LOS_ALL.add(los);
					}else if(los.length() == tmp.length()){
						this.LOS_ALL.add(tmp);
					}
				}
				return los;
			}else{
				System.out.println("input error !");
				return null;				
			}			
		}else{
			if(root.nodeAlphabet.commonInAll(queryNum)){
				if(root.hasChildren()){
					for(ChTreeNode child: root.children){
						tmp = this.vocab.getAlphabet(root.chVocabID)+getLOSAll(child, queryNum);
						if(los.length()<tmp.length()){
							los = tmp;							
						}
					}
					return los;
				}else{					
					return los+this.vocab.getAlphabet(root.chVocabID);
				}
			}else{				
				return los;	
			}					
		}
	}
	public String getLOSAll_K(ChTreeNode root, int kSource, Vector<Integer> kSuperSet){
		String los = "";
		String tmp;
		//
		if(null == root.parent){
			if(root.hasChildren()){
				//multiple equal length
				for(ChTreeNode child: root.children){
					Vector<Integer> commonKSuperSet = child.nodeAlphabet.kSourceQuery(kSource);
					if(null!=commonKSuperSet){
						tmp = getLOSAll_K(child, kSource, commonKSuperSet);
						if(los.length() < tmp.length()){
							los = tmp;
							//
							this.LOS_ALL.clear();
							this.LOS_ALL.add(los);
						}else if(los.length() == tmp.length()){
							this.LOS_ALL.add(tmp);
						}
					}					
				}
				return los;
			}else{
				System.out.println("input error !");
				return null;				
			}			
		}else{
			//common k-source super set with parent node
			Vector<Integer> commonKSuperSet = root.nodeAlphabet.kSourceQuery(kSource, kSuperSet);
			if(null!=commonKSuperSet){
				if(root.hasChildren()){		
					for(ChTreeNode child: root.children){
						tmp = this.vocab.getAlphabet(root.chVocabID)+getLOSAll_K(child, kSource, commonKSuperSet);
						if(los.length()<tmp.length()){
							los = tmp;							
						}
					}
					return los;
				}else{					
					return los+this.vocab.getAlphabet(root.chVocabID);
				}
			}else{				
				return los;	
			}					
		}
	}
	/**
	 * @param removeSuffixCS if true, remove suffix-type common substrings
	 * given a group of strings, enumerate all common substrings between each pair of consecutive strings
	 * **/
	public Vector<LCSSet> enumerateAllCCS(boolean removeSuffixCS){
		Vector<LCSSet> sessionOPS = new Vector<LCSSet>();
		//
		Vector<String> opSet;
		for(int i=0; i<this.queryNumber-1; i++){
			if(null!=(opSet=enumerateCSGiven2Q(i, i+1, removeSuffixCS))){				
				sessionOPS.add(new LCSSet(opSet));
			}else{				
				sessionOPS.add(new LCSSet(null));
			}
		}
		//
		///*
		for(LCSSet csV: sessionOPS){
			if(null!=csV.csVec){
				for(String ops: csV.csVec){
					System.out.print(ops+"\t");
				}
				System.out.println();
			}else{
				System.out.println("null");
			}
		}
		//*/
		//
		return sessionOPS;
	}
	//
	public void ccsStatistics(boolean removeSuffixCS){
		Hashtable<String, StrInt> ccsTable = new Hashtable<String, StrInt>();
		//
		Vector<String> opSet;
		for(int i=0; i<this.queryNumber-1; i++){
			if(null!=(opSet=enumerateCSGiven2Q(i, i+1, removeSuffixCS))){				
				for(String ccs: opSet){
					if(ccsTable.containsKey(ccs)){
						ccsTable.get(ccs).intPlus1();
					}else{
						ccsTable.put(ccs, new StrInt(ccs, 2));
					}
				}
			}
		}
		//
		///*
		Enumeration<StrInt> ccsEnumeration = ccsTable.elements();
		ArrayList<StrInt> ccsArrayList = Collections.list(ccsEnumeration);
		
		Collections.sort(ccsArrayList, new StrIntComparatorByFirst_Desc(StrIntComparatorByFirst_Desc.CmpType.StrLength));
		Collections.sort(ccsArrayList, new PairComparatorBySecond_Desc<String, Integer>());
		
		for(StrInt ccs: ccsArrayList){
			System.out.println(ccs.first+"\t"+ccs.second);
		}
		//*/
		//		
	}
	/**
	 * extract all common substrings between two given queries
	 * there is no overlapping between any two common substrings
	 * **/
	private Vector<String> enumerateCSGiven2Q(int qid_1, int qid_2, boolean removeSuffixCS){
		if(removeSuffixCS){
			//StrInt used as: cs/its length
			Vector<StrInt> csSet = new Vector<StrInt>();
			//
			String ops;
			for(ChTreeNode child: this.root.children){
				if((ops=getChildrenOPS(child, qid_1, qid_2)).length()>0){
					csSet.add(new StrInt(ops, ops.length()));
				}
			}
			//
			if(csSet.size()>0){
				return removeSuffixCS(csSet);
			}else{
				return null;
			}
		}else{
			//do not remove suffix-type common substrings
			Vector<String> allCSSet = new Vector<String>();
			String ops;
			for(ChTreeNode child: this.root.children){
				if((ops=getChildrenOPS(child, qid_1, qid_2)).length()>0){
					allCSSet.add(ops);
				}
			}
			if(allCSSet.size() > 0){
				return allCSSet;
			}else{
				return null;
			}			
		}
		
	}
	//extract all ops of a specified child containing given two query id
	private String getChildrenOPS(ChTreeNode child, int qid_1, int qid_2){
		String ops = "";
		String tmp;
		//
		if(child.nodeAlphabet.commonInGivenTwo(qid_1, qid_2)){
			if(child.hasChildren()){
				for(ChTreeNode son: child.children){
					tmp = this.vocab.getAlphabet(child.chVocabID)+getChildrenOPS(son, qid_1, qid_2);
					if(ops.length()<tmp.length()){
						ops = tmp;
					}
				}
				return ops;
			}else{
				return ops+this.vocab.getAlphabet(child.chVocabID);
			}
		}else{
			return ops;	
		}
	}
	//remove suffix-type common substrings
	private Vector<String> removeSuffixCS(Vector<StrInt> opsTOTAL){		
		Vector<String> opsMap = new Vector<String>();
		//
		if(opsTOTAL.size()>1){
			Collections.sort(opsTOTAL, new PairComparatorBySecond_Desc<String, Integer>());
			//
			opsMap.add(opsTOTAL.get(0).first);
			//
			for(int i=1; i<opsTOTAL.size(); i++){
				if(nonSuffix(opsTOTAL.get(i).first, opsMap)){
					opsMap.add(opsTOTAL.get(i).first);
				}
			}			
			//
			return opsMap;
		}else if(opsTOTAL.size()>0){
			opsMap.add(opsTOTAL.get(0).first);			
			//
			return opsMap;
		}else{
			return null;
		}
	}
	//suffix ops check
	private boolean nonSuffix(String str, Vector<String> strSet){
		for(String element: strSet){
			if(element.indexOf(str)>=0){
				return false;
			}
		}
		return true;
	}
	//
	public static void main(String []args){
		/**1**/		
		/*
		strSet.add("abcde");
		strSet.add("bcdef");
		strSet.add("ccdef");
		*/
		//观看电视剧三国演义	84集电视剧+三国演义	观看电视剧红楼梦	
		///*
		Vector<String> strSet = new Vector<String>();
		strSet.add("观看电视剧三国演义");
		strSet.add("观看84集电视剧三国演义");
		strSet.add("观看电视剧红楼梦");
		//strSet.add("油库设计招标");
		//strSet.add("油库设计招标");
		//*/
		/*
		strSet.add("adbba12341A");
		strSet.add("adbbf1234sa");
		strSet.add("adbf1234sad");
		*/
		//
		///*
		ChLCSScaner losFactory = new ChLCSScaner();
		losFactory.buildSuffixTree(strSet);
		//losFactory.enumerateLCS_AtLeastS(2);
		//losFactory.enumerateAllCCS(true);
		losFactory.ccsStatistics(false);
		//losFactory.breadthFirstTraverse(losFactory.root);
		//System.out.println("All:\t"+losFactory.getLOSAll_K(2));
		//System.out.println("All:\t"+losFactory.LOS_ALL);
		//System.out.println("Former2:\t"+losFactory.getLOSFormer2());
		//System.out.println("Former2:\t"+losFactory.LOS_FORMER2);
		//System.out.println("Later2:\t"+losFactory.getLOSLatter2());
		//System.out.println("Later2:\t"+losFactory.LOS_LATER2);
		//		
		//*/
	}
}
