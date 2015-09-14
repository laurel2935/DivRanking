package org.archive.nlp.htmlparser.pk;

import java.util.LinkedList;
/**
 * 存储HTML文档的树结构
 * */
public class HTree {
	private LinkedList<Node> list;
	private LinkedList<Node> leafList;
	public HTree(){
		list=new LinkedList<Node>();
		leafList=new LinkedList<Node>();
	}
	/**
	 * 插入一个节点
	 * */
	public void insert(Node node){
		list.add(node);
	}
	/**
	 * 插入到叶子节点
	 * */
	private void insertLeaf(Node node){
		leafList.add(node);
	}
	/**
	 * 打印树
	 * */
	public void print(){
		int len=list.size();
		String str;
		Node node;
		int k=0;
		for(int i=len-1;i>=0;i--){
			node=list.get(i);
			str=node.content.trim();
			if (str.equals(""))continue;
			System.out.println(k+++":"+str);
		}
	}
	/**
	 * 打印树
	 * */
	public void print2(){
		int len=list.size();
		String str;
		Node node;
		for(int i=len-1;i>=0;i--){
			node=list.get(i);
			if (!node.isBlock)continue;
			System.out.println(node.content);
		}
	}
	
	/**
	 * 计算非链接文本内容与链接文本内容比
	 * float是32位的double是64位的 
	 * 都是浮点型但是表示范围是不一样的，转换的时候当然会提示精度损失，虽然这个数字在两个类型中都是不溢出的。
	 * 当你不声明时，默认为double的
	 */
	public double contentRatio(){
		int linkContent = 0, nonLinkContent = 0;
		int len = list.size();
		Node node;
		String str, del;
		
		for(int i=len - 1; i>=0; i--){
			node = list.get(i);
			str = node.content.trim();
			if(str.equals(""))
				continue;
			if(node.tag.equals("a")){
				//System.out.println(str);
				del=str.replaceAll("<[0-9]*>", "");
				linkContent+=del.getBytes().length;
				//System.out.println(del);
			}else{
				del=str.replaceAll("<[0-9]*>", "");
				nonLinkContent+=del.getBytes().length;
				//System.out.println(del);
			}
		}
		if(nonLinkContent!=0 && linkContent!=0){
			//System.out.println(nonLinkContent);
			//System.out.println(linkContent);
			return (0.1*nonLinkContent)/(0.1*linkContent);
		}
		return 0;		
	}
	
	
	/**
	 * 合并块，对于父块有内容而子块中也有内容的合并子块到父块<p>
	 * 自底向上合并
	 * */
	public void mergee(){
		Node node,next,curr;
		String str,temp;
		int len=list.size();
		for(int i=len-1;i>=0;i--){
			node=list.get(i);
			if (!node.isLeaf)
				continue;
			curr=node;
			str=curr.content;
			for(;curr!=null;){
				next=curr.parent;
				if (next!=null){
					if (next.hasContent){      //如果父节点有内容
						temp=next.content;
						next.addContent(str);
						next.setLeaf(true);
						node.setBlock(false);
						break;
					}
					else
						next.setLeaf(false);
					curr=next;
				}
				else
					curr=null;
			}
			if (curr==null){
				node.setBlock(true);
			}
		}
	}
	/**
	 * 叶子节点的父节点若连续有内容，则向上合并，直到父节点无内容
	 * */
	public void merge2(){
		Node node,next,curr;
		String str,temp;
		int len=list.size();
		for(int i=len-1;i>=0;i--){
			node=list.get(i);
			if (!node.isLeaf)
				continue;
			curr=node;
			str=curr.content;
			for(;curr!=null;){
				next=curr.parent;
				if (next!=null){
					if (next.hasContent){      //如果父节点有内容
						temp=next.content;
						next.addContent(str);
						next.setLeaf(true);
						node.setBlock(false);
						//break;
					}
					else{
						next.setLeaf(false);
						node.setBlock(true);
					}
					break;
				}
				else
					curr=null;
			}
			if (curr==null){
				node.setBlock(true);
			}
		}
	}
	/**
	 * 返回内容块
	 * <p>如果没有内容块 返回null
	 * */
	public String[] getBlock(){
		String[] contBlock;
		String[] res;
		int len=list.size();
		int num=0;
		String str;
		Node node;
		//for(int i=len-1;i>=0;i--){
		//	node=list.get(i);
		//	str=node.content.trim();
		//	if (str.equals(""))continue;
		//	num++;
		//}
		//num=len;
		contBlock=new String[len];
		//if (num<=0) return null;
		//num=0;
		for(int i=len-1;i>=0;i--){
			node=list.get(i);
			str=node.content.trim();
			if (str.equals(""))continue;
			contBlock[num++]=str;
		}
		res=new String[num];
		System.arraycopy(contBlock,0,res,0,num);
		return res;
		
	}
	/**
	 * 对每个内容块 进行结束标志整理
	 * */
	public void repair(){
		int len=list.size();
		Node node;
		for(int i=len-1;i>=0;i--){
			node=list.get(i);
			if (node.hasContent&&node.endpos!=0)
			    node.content+="<"+node.endpos+">";;
		}
	}
}

