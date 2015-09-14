package org.archive.nlp.htmlparser.pk;

public class Symbol {
	static public String jump[]  ={"select",                //可以跳过的标记,<>..</>,配套标记之间的内容可以忽略
		                    "script","h","ul",
		                    "marquee","object",
		                    "TEXTAREA","style"}; 
	static public String ignore[]={"/p","li","/li","br","form","/form","param","/img","strong","/strong",
							"span","/span",
							"font","/font","b","/b",
		                    "p","img","hr","!","input","?"};//本身可以忽略的标记<br><p></p>
	
	static public String remove[]={"&.+?;","&#[0-9]+;?","　"};    //写正则表达式,此处删除所有&...; 和&#890;类型的字符串
	//static String ignore1[]={"li","br","form","param","strong","span","font","b","p","img","hr","!","input","?"};//本身可以忽略的标记<br><p></p>
}
