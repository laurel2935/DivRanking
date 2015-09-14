package org.archive.nlp.lcs;

import java.util.Vector;

public class UnitTreeNode {
	//id in vocabulary
	public int unitID;	
	//alphabet distribution in analyzed query set
	public UnitNode nodeAlphabet;
	//
	public UnitTreeNode parent;
	//
	public Vector<UnitTreeNode> children;
	//
	public  int childCount;	
	//
	public UnitTreeNode(int sourceStrID, int innerIndex, int vocabID, UnitTreeNode parent){
		this.unitID = vocabID;
		this.parent = parent;
		//
		nodeAlphabet = new UnitNode(sourceStrID, innerIndex, vocabID);
	}
	//
	public void addSourceStrID(int sourceStrID, int innerIndex){
		this.nodeAlphabet.addStrAlphabet(new StrAlphabet(sourceStrID, innerIndex, this.unitID, 0, 0));
	}
	//
	public void setChildrenSize(int size){
		if(null == this.children){
			this.children = new Vector<UnitTreeNode>(size);
		}
		//
		this.children.setSize(size);
	}

	//
	public UnitTreeNode addChildNode(int sourceStrID, int innerIndex, int vocabID){
		UnitTreeNode child = null;
		//
		int top, mid=0, bot=0;
		//
		if(null != this.children){
			if(null == this.parent){
				//fill the gap
				while(vocabID >= this.children.size()){
					this.children.add(null);
				}
				//
				child = this.children.get(vocabID);
			}else{
				top = this.children.size();
                while(bot < top){
                        mid = (bot+top)/2;
                        child = this.children.get(mid);
                        if(child.unitID > vocabID){
                        	top = mid;
                        }else if(child.unitID < vocabID){
                        	bot = ++mid;
                        }else{
                        	//
                        	child.addSourceStrID(sourceStrID, innerIndex);
                        	return child;
                        }    
                }
                child = null;
			}
			//
			if(null == child){
				if(null == this.children){
					this.children = new Vector<UnitTreeNode>(1);
				}
				child = new UnitTreeNode(sourceStrID, innerIndex, vocabID, this);
				//
				if(this.parent == null){
					this.children.set(vocabID, child);
				}else{
					this.children.insertElementAt(child, mid);
				} 
				//
				this.childCount++;
			}else{
				//for the child already inserted
				child.addSourceStrID(sourceStrID, innerIndex);
			}
			//
			return child;
		}else{
			this.children = new Vector<UnitTreeNode>(1);
			//
			child = new UnitTreeNode(sourceStrID, innerIndex, vocabID, this);
			//
			if(null == parent){
				 this.children.set(vocabID, child);
			}else{
				children.insertElementAt(child, mid);
			}
			//
			this.childCount++;
			//
			return child;
		}
	}
	//
	public boolean hasChildren(){
		return this.children!=null;
	}
}
