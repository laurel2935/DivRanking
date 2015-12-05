package org.archive.ireval.toy;
import java.io.BufferedWriter;

public class SogouTHtml {
	
	public String _docno;
	public String _url;
	public String _htmlStr;
	public String _date;

	public SogouTHtml(String docno, String url, String htmlStr) {
		this._docno = docno;
		this._url = url;
		this._htmlStr = htmlStr;
	}
	
	public SogouTHtml(String docno, String url, String htmlStr, String date) {
		this._docno = docno;
		this._url = url;
		this._htmlStr = htmlStr;
		this._date = date;
	}
	
	public void sysOutput(){
		System.out.println("docno: "+this._docno);
		System.out.println("date: "+this._date);
		System.out.println("url: "+this._url);
		
		/*
		if(ClickThroughAnalyzer.isUTF8(this._htmlStr)){
			System.out.println("!!!!!!"+this._docno);
			try {
				byte [] array = this._htmlStr.getBytes("GBK");
				String htmlStr = new String(array, "UTF-8");
				System.out.println(htmlStr);
			} catch (Exception e) {
				// TODO: handle exception
				e.printStackTrace();
			}			
		}else {
			System.out.println(this._htmlStr);
		}
		*/
		System.out.println(this._htmlStr);
	}
	
	public void toSogouTString(BufferedWriter writer) throws Exception{
		  writer.write("<doc>"+Accessor.newline);
		  writer.write("<docno>"+_docno+"</docno>"+Accessor.newline);
		  //writer.write("<date>" +_date +"</date>"+IndexFiles.NEWLINE);
		  writer.write("<url>"  +_url  +"</url>"+Accessor.newline);
		  writer.write(_htmlStr.trim()+Accessor.newline);
		  writer.write("</doc>"+Accessor.newline);	  
		}
	
	public String getHtmlStr(){
		return this._htmlStr;
	}
	
	public String getUrl(){
		return this._url;
	}
	
	public String getDocNo(){
		return this._docno;
	}
}
