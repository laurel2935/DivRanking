package org.archive.nlp.htmlparser.pk;

import java.util.regex.*;

public class HTML2Tree {
	private Stack<Node> myStack;               //建立树的过程中的辅助栈
	private HTree tree;
	public HTML2Tree(){
		tree=new HTree();
		myStack=new Stack<Node>();
	}
	public void emptyTree(){
		tree=new HTree();
	}
	public void main(StringBuffer sb){
		String s="",tag="",content,tmp="";
		int pos=0,start=0,end;                  //start和end是提取内容的起始终止位置
		int num=0,position=0;
		int status;                             //0 表示初始状态，在找body标记
		                                        //1 表示找下一个< 标记 
		                                        //2 表示找 >标记
		                                        //3 表示跳过标记和它的内容，如<stript></stript>
		                                        //4 表示提取两个标记之间的内容
		
		boolean getNewLine;                     //获取新行吗
		int posByteLen=0;                       //cwirf测试中用的字节数
		status=0;
		getNewLine=true;
		insert("html"); /* 插入根节点 */
		String[] lines;
		//sb=delNestTag(sb);
		lines=sb.toString().split("\\n");
		int index=0;
		boolean hasBegin=false;
		while(true){    
			if (getNewLine){
				if(lines.length == 0)
					break;
		   	    s=lines[index++];
		   	    tmp=s;
		   	    num++;
		   	    //改过，原来为＝＝
		   	    if (index>=lines.length) break;
		   	    
		   	    if(hasBegin){
		   	    	position+=(s.getBytes()).length+1;
		   	    	//System.out.println(s);
		   	    	//tmp=(myStack.getTop()).content;
		   	    	//System.out.println(tmp);
		   	    	//tmp=(myStack.getTop()).tag;
		   	    	//System.out.println(position);
		        }
		   	    else if(s.trim().equals(""))
		   	    	hasBegin=true;
		   	    if(num==75){
		   	    	//System.out.println(s);
		   	    	//tmp=(myStack.getTop()).content;
		   	    	//System.out.println(tmp);
		   	    	
		   	    }
		        if (s.equalsIgnoreCase(""))
		        	continue;
		        //s=s.toLowerCase();
		        s=s.replaceAll("</p>|<p>|<br>","\n");
		        pos=0;
		        //System.out.println(s);
		    }
		    if (status==0){                // 找到body开始的位置
		    	pos=s.indexOf("<body");
		    	if (pos<0) {
		    		getNewLine=true;
		    		continue;
		    	}
		    	insert("body");
		    	status=1;
		    	pos++;
		    	getNewLine=false;
		    }
		    else if(status==1){            // 找下一个< 标记
		    	pos=s.indexOf("<",pos);         
		    	if (pos<0) {               //没找到，转让下一行
		    		getNewLine=true;
		    		pos=0;
		    		continue;
		    	}
		    	tag=getTag(s,pos+1);
		        if (tag!=null){            //找到了标记
		        	if (isEndTag(tag)){    //是结束标记
		        	    if (mayIgnor(tag)){//是可以忽略的标记
		        	        status=2;
		        	        getNewLine=false;
		        	        continue;
		        	    }
		        		if (match(tag)){   //与当前栈顶匹配的标记
		        		    //if((myStack.getTop()).hasContent)
		        			//(myStack.getTop()).addEndTag(position-(tmp.substring(pos).getBytes()).length-1);
		        		    //(myStack.getTop()).addEndTag(position);
		        			//注释调
		        			//(myStack.getTop()).addEndTag();
		        			myStack.pop();
		        		    if (myStack.empty())
		        		    	break;
		        	        pos++;
		        	        getNewLine=false;
		        	        status=4;
		        	        start=s.indexOf(">",pos);
		        	        if (start<0){
		        	        	getNewLine=true;
		        	        	pos=0;
		        	        	status=1;
		        	        	continue;
		        	        }
		        	        else 
		        	        	start+=1;
		        	        pos=start;
		        	        continue;
		        	    }
		        	    else{              //不匹配表示网页有错误，当作可忽略的标记
		        	    	//System.out.println(num);
		        	    	//myStack.print();
		        	    	//System.out.println();
		        	    	status=2;
		        	    	getNewLine=false;
		        	    	continue;
		        	    }
		        	}
		        	else{                  //不是结束标记
		        		if (mayIgnor(tag.trim())){//是可以忽略的标记
		        	        status=2;
		        	        getNewLine=false;
		        	        continue;
		        		}
		        	    else{              //不可以忽略的标记    
		        	    	getNewLine=false;
		        	    	if (!isJump(tag)){
		        	    		status=2;
		        	    		insert(tag);
		        	    	}
		        	    	else{          //可以跳过的标记
		        	    		status=3;
		        	    	}
		        	    }
		        	}
		        }
		        else {
		        	getNewLine=true;
		        }
		        continue;
		    }
		    else if (status==2){           /* 找>标记*/
		    	if(tag.indexOf("!")>=0){
		    		start=s.indexOf("-->",pos);
		    		if (start<0){              //当前行没找到，则获取下一行
			    		getNewLine=true;
			    		pos=0;
			    		continue;
			    	}
		    		start+=3;
		    	}else{
		    		start=s.indexOf(">",pos);
		    		if (start<0){              //当前行没找到，则获取下一行
		    			getNewLine=true;
		    			pos=0;
		    			continue;
		    		}
		    		start++;
		    	}
		    	//posByteLen=(s.substring(start).getBytes()).length;
		    	status=4;
		    	getNewLine=false;
		    }
		    else if (status==3){           /* 找跳过的</...>标记*/
		    	pos=s.indexOf("</"+tag+">",pos);
		    	if (pos<0){
		    		getNewLine=true;
		    		pos=0;
		    		continue;
		    	}
		    	pos=s.indexOf(">",pos);
		    	pos++;
		    	//posByteLen=(s.substring(pos).getBytes()).length;
		    	start=pos;
		    	status=4;
		    	getNewLine=false;
		    	continue;
		    }
		    else if(status==4){            /* 获取两个标记<> <>之间的内容*/
		    	end=s.indexOf("<",start);
		    	if (end<0){                /* 内容延伸到了下一行*/
		    			//添加空格
	                   content=remove(s.substring(start)).trim();
	                   //content=remove(s.substring(start));
	                   if (!content.equalsIgnoreCase("")){
	                      posByteLen=(tmp.substring(start).getBytes()).length;
	                	   (myStack.getTop()).addContent(content+" ");
	                      //(myStack.getTop()).addContent2(content,position-posByteLen-1);
	                      (myStack.getTop()).endpos=position;
	                   }
	                   getNewLine=true;
	                   start=0;
	                   continue;
		    	}
		    	content=s.substring(start,end);
		    	//添加空格
		    	content=remove(content).trim();
		    	//content=remove(content);
		    	//posByteLen=(s.substring(end+1).getBytes()).length;
		    	if(!content.equalsIgnoreCase("")){
		    		posByteLen=(tmp.substring(start).getBytes()).length;
		    		(myStack.getTop()).addContent(content+"");
		    		//(myStack.getTop()).addContent2(content,position);
		    	    //(myStack.getTop()).addContent2(content,position-posByteLen-1);
		    	    (myStack.getTop()).endpos=position-(tmp.substring(end).getBytes()).length;
		    	}
                status=1;
                pos=end;
                getNewLine=false;
		    }
		    
		}
		//tree.repair();
	}
	/**
	 * 提取标记类型
	 * 如，标记<html>的类型是html
	 * @param line 标记所在行
	 * @param pos  <位置
	 * */
	private String getTag(String line,int pos){
		int end1,end2;
		end1=line.indexOf(">",pos);
		end2=line.indexOf(" ",pos);
		if ((end1<0 && end2>=0))               //<html sad 或 <html sdfadfa
			return line.substring(pos,end2);
		if (end1>0 && end2>=0 && end2<end1)    // <html fds>
			return line.substring(pos,end2);
		if (end2<0 && end1>=0)                 // <html>
			return line.substring(pos,end1);
		if (end1<0 && end2<0)                  //此情况表示，该<符合不是tag标记
			return line.substring(pos);
		if (end1>=0 && end2>=0 && end1<end2)   //表示是<html>类型
			return line.substring(pos,end1);
		return line.substring(pos);            //当一行以<html类型结束，我们不考虑一个标记分在两行的情况<ht
	}
	/**
	 * 判断当前标记是否是应该跳过的类型
	 * 如, <script>
	 * @param tag 标记
	 * */
	private boolean isJump(String tag){
		//String s=(String)myStack.getTop();
		int size=Symbol.jump.length;
		for(int i=0;i<size;i++){
			if (tag.equalsIgnoreCase(Symbol.jump[i]))
			    return true;
		}
		return false;
	}
	/**
	 * 当找到了<标记时
	 * 判断当前标记是开始标记<html>还是结束标记</html>
	 * */
	private boolean isEndTag(String tag){
		int pos;
		pos=tag.indexOf("/");
		if (pos<0) return false;
		return true;
	}
	/**
	 * 当前结束标记和栈顶的标记是同一对吗？
	 * */
	private boolean match(String tag){
		String str;
		Node node;
		node=myStack.getTop();
		str=node.tag;
		int pos;
		pos=tag.indexOf(str);
		if (pos<0) return false;
		return true;
	}
	/**
	 * 两个标记是同一对吗？
	 * @param beginTag  开始标记
	 * @param endTag    结束标记
	 * */
	private boolean match2(String beginTag,String endTag){
		int pos;
		pos=endTag.indexOf(beginTag);
		if (pos<0) return false;
		return true;
	}
	/**
	 * 是可以忽略的标记吗
	 * */
	private boolean mayIgnor(String tag){
		boolean res;
		
		for(int i=0,size=Symbol.ignore.length;i<size;i++){
			res=tag.equalsIgnoreCase(Symbol.ignore[i]);
			if (res) return true;
			if (tag.matches(".*!.+|.*\\?.+")) return true;
		}
		return false;
	}
	/**
	 * 判断与当前内容未结束标记是否匹配
	 * @param  unEndTag   当前内容未结束时的标记
	 * @param  tag        当前的结束标记
	 * */
	private boolean unEndTagMatch(String unEndTag, String tag){
		int pos=tag.indexOf(unEndTag);
		if(pos<0) return false;
		return true;
	}
	/**
	 * 去处一段字符串中的在Symbol.remove中出现的字符
	 * */
	public String remove(String str){
		int i=0;
		String regx="";
		for(;i<Symbol.remove.length-1;i++){
			regx+=Symbol.remove[i]+"|";
		}
		regx+=Symbol.remove[i];                  
		str=str.replaceAll(regx," ");
		return str;
	}
	/**
	 * 栈和插入批处理操作
	 * @param tag    标记
	 * */
	private void insert(String tag){
		Node node;
		if (myStack.empty())
			node=new Node("",tag,null);
		else
			node=new Node("",tag,myStack.getTop());
		myStack.push(node);
		tree.insert(node);
	}
	/**
	 * 返回建好的树
	 * */
	public HTree getTree(){
		return tree;
	}
	/**
	 * 删除<script>标记之间的内容
	 * */
	private static StringBuffer delNestTag(StringBuffer sb){
		String str=sb.toString();
		String regx="(<script((?!<script).)*/script>)";
		Pattern p=Pattern.compile(regx,Pattern.CASE_INSENSITIVE);
		try{
			Matcher m=p.matcher(str);
			if(m.find()){
				str=m.replaceAll("");
			}
		}catch(Exception e){}
		return new StringBuffer(str);
	}
}




