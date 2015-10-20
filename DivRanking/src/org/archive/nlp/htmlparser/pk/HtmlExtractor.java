package org.archive.nlp.htmlparser.pk;

import java.io.*;

public class HtmlExtractor {
	private Extractor extractor = null;	
	
	public HtmlExtractor(){
		extractor = new Extractor();
	}
	//
	public StringBuffer htmlToText(File html, String encoding){
		StringBuffer sbHtml = loadHtml(html, encoding);
		if(null == sbHtml){
			return null;
		}
		return extractor.htmlToText(sbHtml);		
	}
	//
	private static StringBuffer loadHtml(File html, String encoding){		
		StringBuffer sb=new StringBuffer();
		String str;
		try{
			BufferedReader br = new BufferedReader(
					new InputStreamReader(new FileInputStream(html), encoding));
			
			while(null != (str=br.readLine())){
				sb.append(str);
				//System.out.println(str);
				sb.append("\n");//�����ķ��д���ָ���
			}
		}catch(Exception e){e.printStackTrace();}
		return sb;
	}
	public StringBuffer htmlToText(BufferedReader br){
		StringBuffer sbHtml = loadHtml(br);
		if(null == sbHtml){
			return null;
		}
		return extractor.htmlToText(sbHtml);		
	}
	private static StringBuffer loadHtml(BufferedReader br){		
		StringBuffer sb=new StringBuffer();
		String str;
		try{
			//BufferedReader br = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(array)));
			
			while(null != (str=br.readLine())){
				sb.append(str);
				//System.out.println(str);
				sb.append("\n");//�����ķ��д���ָ���
			}
		}catch(Exception e){e.printStackTrace();}
		return sb;
	}
	/**
	 * Sample test
	 * @param args filePath + fileName:the html file to be processed
	 */
	public static void main(String args[]) {
		//String file="G:/Data_Log/page.txt";
		String htm_2 = "E:/Data_Log/DataSource_Raw/NTCIR-10/DocumentRanking/doc/top-100/0004/1f86a7e6678bf8a1-071d026ec8f3fbc0.htm";
		HtmlExtractor ec = new HtmlExtractor();		
		System.out.println(ec.htmlToText(new File(htm_2), "GB2312"));
	}
}
