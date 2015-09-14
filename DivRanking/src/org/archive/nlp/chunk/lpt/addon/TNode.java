package org.archive.nlp.chunk.lpt.addon;

import java.util.Vector;

public class TNode implements Comparable{
	//
	private TNode _parent;
	private Vector<TNode> _children;
	//id within the sentence, which starts from 0
	private Integer _id;
	private String _cont;
	private String _pos;
	private String _ne;
	private Integer _intParent;
	private String _relate;	
	//
	private boolean _isLeaf;
	
	public TNode(int id, String cont, String pos, String ne, int intParent, String relate){
		this._id = id; this._cont = cont;
		this._pos=pos; this._ne = ne;
		this._intParent = intParent;
		this._relate = relate;
		//
		this._isLeaf = true;
	}
	//
	public int compareTo(Object object){
		TNode cmpNode = (TNode)object;
		return this._id.compareTo(cmpNode._id);
		
	}
	//
	public TNode getParent(){
		return this._parent;
	}
	//
	public void setParent(TNode parent){
		this._parent = parent;
	}
	//
	public Vector<TNode> getChildren(){
		return this._children;
	}
	//
	public void addChild(TNode tNode){
		if(null == this._children){
			this._children = new Vector<TNode>();
			this._isLeaf = false;
		}
		this._children.add(tNode);
	}
	//
	public boolean isLeaf(){
		return this._isLeaf;
	}
	//
	public Integer getIntParent(){
		return this._intParent;
	}
	//
	public Integer getID(){
		return this._id;
	}
	//
	public String getContent(){
		return this._cont;
	}
	//
	public String getPosTag(){
		return this._pos;
	}
	//
	public String getNeTag(){
		return this._ne;
	}
	//
	public String getRelateTag(){
		return this._relate;
	}

}
