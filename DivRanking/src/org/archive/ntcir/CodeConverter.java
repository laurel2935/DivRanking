package org.archive.ntcir;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.archive.util.io.IOText;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

public class CodeConverter {
	
	public static void convert (String oriFile, String oriCode, String targetCode, String targetFile) {
		try {
			//
			ArrayList<String> lineList = IOText.getLinesAsAList(oriFile, oriCode);
			//build a standard pseudo-xml file
		    StringBuffer buffer = new StringBuffer();
		    buffer.append("<add>");
		    for(String line: lineList){
		    	//System.out.println(line);
		    	buffer.append(line);
		    	buffer.append("\n");
		    }		    
		    buffer.append("</add>"); 
	    	
	    	SAXBuilder saxBuilder = new SAXBuilder();      
	        Document xmlDocSet = saxBuilder.build(new InputStreamReader(new ByteArrayInputStream(buffer.toString().getBytes("utf-8"))));
	        
	        Element rootElement = xmlDocSet.getRootElement(); 
	        List docList = rootElement.getChildren("doc");
			
			
			BufferedWriter writer = IOText.getBufferedWriter(targetFile, targetCode);
			
			for(int i=0; i<docList.size(); i++){
				Element docElement = (Element)docList.get(i);				
				String url = docElement.getChildText("url");
				String docno = docElement.getChildText("docno");
				String contenttitle = docElement.getChildText("contenttitle");
				String content = docElement.getChildText("content");
				/**
				 * String str="[\u3002\uff1b\uff0c\uff1a\u201c\u201d\uff08\uff09\u3001\uff1f\u300a\u300b]"
				 * 该表达式可以识别出： 。 ；  ， ： “ ”（ ） 、 ？ 《 》 这些标点符号。
				 * **/
				content = content.replaceAll("[^０１２３４５６７８９a-z0-9A-Z\u4E00-\u9FFF\u3002\uff1b\uff0c\uff1a\u201c\u201d\uff08\uff09\u3001\uff1f\u300a\u300b]+", "");
				
				writer.write("<doc>");
				writer.newLine();
				writer.write("<url>"+url+"</url>");
				writer.newLine();
				writer.write("<docno>"+docno+"</docno>");
				writer.newLine();
				writer.write("<contenttitle>"+contenttitle+"</contenttitle>");
				writer.newLine();
				writer.write("<content>"+content+"</content>");
				writer.newLine();
				writer.write("</doc>");
			}
			
			writer.flush();
			writer.close();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	//
	
	public static void main(String []args) {
		//1
		CodeConverter.convert("C:/T/WorkBench/Bench_Java/HeidelTimeKit/collectionTest/news_tensite_xml.smarty.dat", 
				"gbk", 
				"utf-8", 
				"C:/T/WorkBench/Bench_Java/HeidelTimeKit/collectionTest/news_tensite_xml.utf8");
		
	}

}
