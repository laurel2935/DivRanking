package org.archive.nlp.chunk.lpt.addon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Vector;

import org.archive.nlp.chunk.lpt.ltpService.Word;

public class TTree {
	//
	private TNode _root = null;
	private Vector<TNode> _nodeList = new Vector<TNode>(); 
	
	//
	public TTree(ArrayList<Word> wordList){
		//ini
		for(Word word: wordList){
			this._nodeList.add(new TNode(word.getID(), word.getWS(), word.getPOS(), word.getNE(),
					word.getParserParent(), word.getParserRelation()));
		}
		//parent-child relation ini
		for(TNode tNode: this._nodeList){
			if(-1 == tNode.getIntParent()){
				tNode.setParent(null);
				this._root = tNode;
			}else{
				this._nodeList.get(tNode.getIntParent()).addChild(tNode);
			}
		}
		//sorting based on word id
		for(TNode tNode: this._nodeList){
			if(!tNode.isLeaf()){
				Collections.sort(tNode.getChildren());
			}
		}		
	}
	//
	public TNode getRoot(){
		return this._root;
	}
	//
	public Vector<TNode> getNodes(){
		return this._nodeList;
	}

}
