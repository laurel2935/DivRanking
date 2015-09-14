package org.archive.nlp.lcs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Vector;

import org.archive.util.Language.Lang;
import org.archive.util.tuple.PairComparatorBySecond_Desc;
import org.archive.util.tuple.StrInt;
import org.archive.util.tuple.StrIntComparatorByFirst_Desc;

//for the case of checking
class Matrix{
	public int level;
	public int index;
	public int parent;
	public UnitTreeNode node;
	Matrix(int level, int index, int parent, UnitTreeNode node){
		this.level = level;
		this.index = index;
		this.parent = parent;
		this.node = node;
	}
}

/**
 * extract longest common substrings given a session of queries
 * 
 * For queries that contain irregular alphabets, 
 * they should be first segmented and then inputed as different query instances!
 * 
 * Usage:
 * (1) all longest common substrings between each pair of consecutive queries
 * (2) all longest common substrings among predefined k queries
 * **/

public class LCSScaner {
	private Vector<String> queryVec;	
	private int queryNumber;
	//
	private Lang language;
	private UnitVocabulary vocab;
	//
	private UnitTreeNode root = null;
	//for the case that multiple same los
	private static Vector<String> LOS_ALL = new Vector<String>();
	//
	private static String BLANK = " ";
	//
	public LCSScaner(Lang language){
		this.language = language;
	}
	//
	public LCSScaner(Vector<String> strSet, Lang language){
		this.queryVec = new Vector<String>();
		this.language = language;
		//
		for(String str: strSet){
			this.queryVec.add(str);
		}
		//
		this.queryNumber = queryVec.size();
		//
		String [] strArray = (String[])this.queryVec.toArray(new String[1]);
		this.buildSuffixTree(strArray);
	}
	//
	private void initializeVocab(String [] strArray, Lang language){
		this.vocab = new UnitVocabulary();
		//
		this.vocab.initialize(strArray, language);
	}
	//
	private void buildSuffixTree(String [] qArray){
		this.root = new UnitTreeNode(-1, -1, -1, null);
		//
		this.initializeVocab(qArray, language);		
		//System.out.println(this.vocab.getSize());		
		this.root.setChildrenSize(this.vocab.getSize());
		//
		String q;
		String [] words;
		UnitTreeNode node;
		for(int i=0; i<qArray.length; i++){
			q = qArray[i];
			//
			if(Lang.Chinese == this.language){
				for(int j=0; j<q.length(); j++){
					node = this.root;
					for(int k=j; k<q.length(); k++){
						node = node.addChildNode(i, k, this.vocab.getID(String.valueOf(q.charAt(k))));
					}				
				}	
			}else if(Lang.English == this.language){
				words = q.split(" ");
				for(int j=0; j<words.length; j++){
					node = this.root;
					for(int k=j; k<words.length; k++){
						node = node.addChildNode(i, k, this.vocab.getID(words[k]));
					}
				}				
			}else{
				System.out.println("Unaccepted language type!");
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
	public void breadthFirstTraverse(UnitTreeNode root){
    	//
    	Queue<Matrix> queue = new LinkedList<Matrix>();
    	//
    	int []index = new int[10];
    	for(int i=0; i<10; i++){        		
    		index[i] = 0;        		
    	}
    	//
    	for(UnitTreeNode child: root.children){
    		queue.offer(new Matrix(0, index[0]++, 0, child));
    	}
    	//
    	while(!queue.isEmpty()){
    		Matrix m = queue.poll();
    		//
    		System.out.println(m.level+":"+m.index+":"+m.parent+"@"+
    				this.vocab.getUnit(m.node.unitID)+"\t:"+m.node.unitID+"\t"+m.node.nodeAlphabet.appearCount);
    		if(m.node.hasChildren()){
    			for(UnitTreeNode child: m.node.children){
    				queue.offer(new Matrix(m.level+1, index[m.level+1]++, m.index, child));
    			}
    		}
    	}
    }
	//--
	public void getLOS(){
		//this.los_All = this.getLOSAll();		
	}
	//--
	public String getLOSAll(){
		return this.getLOSAll(this.root, this.queryNumber);
	}
	//
	private String getLOSAll_K(int kSource){
		return this.getLOSAll_K(this.root, kSource, null);
	}
	/**
	 * @param s the number of strings that lcs derives from
	 * 
	 * enumerate all the longest common substrings that are shared by at least common s strings
	 * Note that: lcs in common k, is not equal to lcsSet of each pair member query 
	 * **/
	public ArrayList<StrInt> enumerateLCS_AtLeastK(int kSource){
		//for statistics
		Hashtable<String, StrInt> strTable = new Hashtable<String, StrInt>();
		//iterate for each k
		for(int k=kSource; k<=this.queryNumber; k++){
			//System.out.println("Common with - "+k+" queries!");
			//
			this.getLOSAll_K(k);
			//
			for(String cStr: this.LOS_ALL){
				//System.out.println("\tcode:\t"+cStr);
				//convert
				cStr = convert(cStr, this.language);
				//
				//System.out.println("\tsubString:\t"+cStr);
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
		//output
		/*
		System.out.println("-------------");
		for(StrInt element: commonList){
			System.out.println(element.second+"\t"+element.first);
		}	
		System.out.println();
		*/
		//
		return commonList;
	}
	//
	public static ArrayList<StrInt> enumerateLCS_AtLeastK(Vector<String> qVector, int kSource, Lang language){
		LCSScaner lcsScaner = new LCSScaner(qVector, language);
		return lcsScaner.enumerateLCS_AtLeastK(kSource);
	}
	//
	public String getLOSAll(UnitTreeNode root, int queryNum){
		String los = "";
		String tmp;
		//
		if(null == root.parent){
			if(root.hasChildren()){
				for(UnitTreeNode child: root.children){
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
					for(UnitTreeNode child: root.children){
						tmp = this.vocab.getUnit(root.unitID)+getLOSAll(child, queryNum);
						if(los.length()<tmp.length()){
							los = tmp;							
						}
					}
					return los;
				}else{					
					return los+this.vocab.getUnit(root.unitID);
				}
			}else{				
				return los;	
			}					
		}
	}
	//
	private String getLOSAll_K(UnitTreeNode root, int kSource, Vector<Integer> kSuperSet){
		String los = "";
		String tmp;
		//
		if(null == root.parent){
			if(root.hasChildren()){
				//multiple equal length
				for(UnitTreeNode child: root.children){
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
					for(UnitTreeNode child: root.children){
						//System.out.println(root.unitID);
						//tmp = this.vocab.getUnit(root.unitID) + getLOSAll_K(child, kSource, commonKSuperSet);
						tmp = Integer.toString(root.unitID) + "-" + getLOSAll_K(child, kSource, commonKSuperSet);
						if(los.length()<tmp.length()){
							los = tmp;							
						}
					}
					return los;
				}else{					
					//return los + this.vocab.getUnit(root.unitID);
					return los + "-" + Integer.toString(root.unitID);
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
	public static Vector<LCSSet> enumerateAllCCS(Vector<String> qVector, int kSource, Lang language, boolean removeSuffixCS){
		LCSScaner lcsScaner = new LCSScaner(qVector, language);
		return lcsScaner.enumerateAllCCS(removeSuffixCS);
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
			for(UnitTreeNode child: this.root.children){
				if((ops=getChildrenOPS(child, qid_1, qid_2)).length()>0){
					//convert
					ops = convert(ops, language);
					//
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
			for(UnitTreeNode child: this.root.children){
				if((ops=getChildrenOPS(child, qid_1, qid_2)).length()>0){
					//convert
					ops = convert(ops, language);
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
	private String getChildrenOPS(UnitTreeNode child, int qid_1, int qid_2){
		String ops = "";
		String tmp;
		//
		if(child.nodeAlphabet.commonInGivenTwo(qid_1, qid_2)){
			if(child.hasChildren()){
				for(UnitTreeNode son: child.children){
					//tmp = this.vocab.getUnit(child.unitID)+getChildrenOPS(son, qid_1, qid_2);
					tmp = Integer.toString(child.unitID) + "-" + getChildrenOPS(son, qid_1, qid_2);
					if(ops.length()<tmp.length()){
						ops = tmp;
					}
				}
				return ops;
			}else{
				//return ops+this.vocab.getUnit(child.unitID);
				return ops + "-" + Integer.toString(child.unitID);
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
	//convert
	private String convert(String encodedLCS, Lang language){
		String [] nodeArray = encodedLCS.split("-");
		String lcsString = "";
		if(Lang.Chinese == language){
			for(int i=0; i<nodeArray.length; i++){
				if(nodeArray[i].length() > 0){
					lcsString += this.vocab.getUnit(Integer.parseInt(nodeArray[i]));
				}				
			}
			return lcsString;
		}else if(Lang.English == language){
			if(nodeArray.length == 1){
				if(nodeArray[0].length() > 0){
					return this.vocab.getUnit(Integer.parseInt(nodeArray[0]));
				}else{
					return lcsString;
				}				
			}else {
				for(int i=0; i<nodeArray.length-1; i++){
					if(nodeArray[i].length() > 0){
						lcsString += this.vocab.getUnit(Integer.parseInt(nodeArray[i]));
						lcsString += BLANK;
					}					
				}
				if(nodeArray[nodeArray.length-1].length() > 0){
					lcsString += this.vocab.getUnit(Integer.parseInt(nodeArray[nodeArray.length-1]));
				}				
			}
			return lcsString;
		}else {
			System.out.println("Unaccepted Language !");
			return null;
		}		
	}
	//
	public static void main(String []args){
		/**1**/	
		//1
		//Chinese test:	观看电视剧三国演义	84集电视剧+三国演义	观看电视剧红楼梦	
		/*
		Vector<String> strSet = new Vector<String>();
		strSet.add("观看电视剧三国演义");
		strSet.add("观看84集电视剧三国演义");
		strSet.add("观看电视剧红楼梦");
		strSet.add("油库设计招标");
		strSet.add("油库设计招标");
		LCSScaner losFactory = new LCSScaner();
		losFactory.buildSuffixTree(strSet, LanType.Chinese);
		//losFactory.enumerateLCS_AtLeastK(2, LanType.Chinese);
		losFactory.enumerateAllCCS(LanType.Chinese, true);
		//losFactory.ccsStatistics(false);
		//losFactory.breadthFirstTraverse(losFactory.root);
		//System.out.println("All:\t"+losFactory.getLOSAll_K(2));
		//System.out.println("All:\t"+losFactory.LOS_ALL);
		//System.out.println("Former2:\t"+losFactory.getLOSFormer2());
		//System.out.println("Former2:\t"+losFactory.LOS_FORMER2);
		//System.out.println("Later2:\t"+losFactory.getLOSLatter2());
		//System.out.println("Later2:\t"+losFactory.LOS_LATER2);
		//	
		*/
		///*
		//2 English test
		/*
		Vector<String> strSet = new Vector<String>();
		strSet.add("adb ba 1234 a");
		strSet.add("adb bf 1234 a");
		strSet.add("adbf 1234 sad");
		//
		LCSScaner losFactory = new LCSScaner(LanType.English);
		losFactory.buildSuffixTree(strSet);
		losFactory.enumerateLCS_AtLeastK(2);	
		losFactory.enumerateAllCCS(true);
		*/
		
		//2
		Vector<String> strSet_1 = new Vector<String>();
		strSet_1.add("adb ba 1234 a");
		strSet_1.add("adb bf 1234 a");
		strSet_1.add("adbf 1234 sad");
		
		Vector<String> strSet_2 = new Vector<String>();
		strSet_2.add("观看电视剧三国演义");
		strSet_2.add("观看84集电视剧三国演义");
		strSet_2.add("观看电视剧红楼梦");
		strSet_2.add("油库设计招标");
		strSet_2.add("油库设计招标");
		
		LCSScaner.enumerateLCS_AtLeastK(strSet_1, 2, Lang.English);
		LCSScaner.enumerateLCS_AtLeastK(strSet_2, 2, Lang.Chinese);
		
	}
}
