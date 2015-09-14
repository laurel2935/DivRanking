package org.archive.nlp.lcs.chinese;

import java.util.Vector;

public class ChTreeNode {
	//id in vocabulary
	public int chVocabID;	
	//alphabet distribution in analyzed query set
	public ChNodeAlphabet nodeAlphabet;
	//
	public ChTreeNode parent;
	//
	public Vector<ChTreeNode> children;
	//
	public  int childCount;	
	//
	public ChTreeNode(int sourceStrID, int innerIndex, int vocabID, ChTreeNode parent){
		this.chVocabID = vocabID;
		this.parent = parent;
		//
		nodeAlphabet = new ChNodeAlphabet(sourceStrID, innerIndex, vocabID);
	}
	//
	public void addSourceStrID(int sourceStrID, int innerIndex){
		this.nodeAlphabet.addStrAlphabet(new StrAlphabet(sourceStrID, innerIndex, this.chVocabID, 0, 0));
	}
	//
	public void setChildrenSize(int size){
		if(null == this.children){
			this.children = new Vector<ChTreeNode>(size);
		}
		//
		this.children.setSize(size);
	}

	//
	public ChTreeNode addChildNode(int sourceStrID, int innerIndex, int vocabID){
		ChTreeNode child = null;
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
                        if(child.chVocabID > vocabID){
                        	top = mid;
                        }else if(child.chVocabID < vocabID){
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
					this.children = new Vector<ChTreeNode>(1);
				}
				child = new ChTreeNode(sourceStrID, innerIndex, vocabID, this);
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
			this.children = new Vector<ChTreeNode>(1);
			//
			child = new ChTreeNode(sourceStrID, innerIndex, vocabID, this);
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
