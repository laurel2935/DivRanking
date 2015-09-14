package org.archive.nlp.htmlparser.pk;

public class Node{
	String  content;
	public String  tag;
	boolean hasContent;
	boolean isLeaf;
	boolean isBlock;                //当最后所有块合并完成后，用于标明该块是否是最后需要的块
	Node    parent;
	public int endpos;
	public Node(){
		content    ="";
		hasContent =false;
		parent     =null;
		isLeaf     =true;
	}
	/**
	 * @param content  内容
	 * @param tag      标记
	 * @param parent   父节点
	 * */
	public Node(String content,String tag,Node parent){
		this.content=content;
		this.tag=tag;
		if (content.equalsIgnoreCase(""))
			hasContent=false;
		else 
			hasContent=true;
		this.parent=parent;
		isLeaf=true;
	}
	public void addContent(String str){
		content+=str;
		//content+=str.trim();
		if (content.equalsIgnoreCase(""))
			hasContent=false;
		else 
			hasContent=true;
	}
	/**
	 * cwirf测试用，为一个块的起始位置添加行号和位置
	 * */
	public void addContent2(String str,int pos){
		if(!hasContent&&!str.equalsIgnoreCase("")){
		    content+="<"+pos+">"+str;
		    hasContent=true;
		}
		else
			content+=str;
	}
	/**
	 * cwirf 测试用，为一个块添加结束位置
	 * */
	public void addEndTag(int pos){
		if(hasContent)
			content+="<"+pos+">";
	}
	public void addEndTag(){
		if(hasContent){
			content+="<"+endpos+">";
			endpos=0;
		}
	}
	/**
	 * 设置该节点是叶子节点
	 * */
	public void setLeaf(boolean is){
		isLeaf=is;
	}
	/**
	 * 设置该节点，在list中是不是一个block<p>
	 * 我们最后需要的是isBlock为true的节点。
	 * */
	public void setBlock(boolean is){
		isBlock=is;
	}
	public String toString(){
		return content;
	}
	
}
