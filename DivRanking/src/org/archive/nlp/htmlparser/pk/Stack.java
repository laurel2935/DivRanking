package org.archive.nlp.htmlparser.pk;

import java.util.*;

public class Stack <T>{
	private LinkedList<T> list;
	private boolean isEmpty;
	public Stack(){
		list=new LinkedList<T>();
		isEmpty=true;
	}
	/**
	 * 入栈
	 * */
	public void push(T node){
		list.add(node);
		if (isEmpty)
			isEmpty=false;
	}
	/**
	 * 出栈
	 * */
	public T pop(){
		T node;
		if (!isEmpty){
			node=list.getLast();
		    list.removeLast();
		    if (list.size()==0)
		    	isEmpty=true;
		    return node;
		}
		else
			return null;
	}
	/**
	 * 得到栈顶元素的值
	 * */
	public T getTop(){
		if (!isEmpty)
		    return list.getLast();
		return null;
	}
	/**
	 * 栈的大小
	 * */
	public int getSize(){
		return list.size();
	}
	/**
	 * 取得栈中某个位置的元素
	 * */
	public T getElement(int index){
		int size;
		size=getSize();
		if (index<0||index>size)
			return null;
		return list.get(index);
	}
	/**
	 * 删除栈中某个元素
	 * */
	public void remove(int index){
		list.remove(index);
	}
	/**
	 * 栈是空的吗
	 * */
	public boolean empty(){
		if (isEmpty) return true;
		return false;
	}
	/**
	 * 打印
	 * */
	public void print(){
		int size=list.size();
		for(int i=size-1;i>=0;i--){
			System.out.print(list.get(i).toString()+" ");
		}
	}
}
