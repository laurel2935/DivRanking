package org.archive.nlp.fetch.baike;

import java.io.BufferedWriter;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.archive.nlp.fetch.HttpDownloader;
import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.Parser;
import org.htmlparser.filters.AndFilter;
import org.htmlparser.filters.HasAttributeFilter;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.util.NodeList;

class MultiCatalog{
	//义项数目；
	int multiC;
	//义项列表
	Vector<String> catalogNameList = new Vector<String>();
	//不同义项的目录，内在序列的一致性
	Vector<Catalog> catalogList = new Vector<Catalog>();
	//添加义项
	public void addCatalogName(String catalogName){
		this.catalogNameList.add(catalogName);
		//corresponding add
		this.catalogList.add(new Catalog());
	}
	//添加义项的目录
	public void addCatalogStr(int i, String catalogStr){
		this.catalogList.get(i).addCatalogStr(catalogStr);
	}
	//
	public String toString(){
		StringBuffer buffer = new StringBuffer();
		for(int i=0; i<catalogNameList.size(); i++){
			buffer.append(catalogNameList.get(i)+"\t"+catalogList.get(i).toString());
			buffer.append("\n");
		}
		return buffer.toString();
	}
	
}
/**
 * catalog list of one single meaning
 * **/
class Catalog{
	//subtopic list for single mean
	Vector<String> catalogStrList = new Vector<String>();
	//
	public void addCatalogStr(String catalogStr){
		this.catalogStrList.add(catalogStr);
	}
	//
	public String toString(){
		StringBuffer buffer = new StringBuffer();
		for(String str: catalogStrList){
			buffer.append(str+"\t");
		}
		return buffer.toString();
	}
}

public class BaikeParser {
	HttpDownloader downLoader = new HttpDownloader(3, 1000);	
	//for "#1"
	public static Pattern firstCatalogEditionPattern = Pattern.compile("\"#\\d+\"");
	//for "#3-2"
	public static Pattern secondCatalogEditionPattern = Pattern.compile("\"#\\d+-\\d+\"");
	//#6_1
	public static Pattern fourthCatalogEditionPattern = Pattern.compile("\"#\\d+_\\d+\"");
	//
	public static ArrayList<String> polyParse(String idStr, StringBuffer html){
		//null case
		if(null == html){
			return null;
		}
		//
		try {
			//type-1:
			Parser parser = new Parser(html.toString());
			//whether included in baike
			//<div class="direct_holder">
			NodeFilter firIncludeFilter = new AndFilter(new TagNameFilter("div"),
					new HasAttributeFilter("class","polysemeBodyCon"));
			
			NodeList firIncludePoly = parser.parse(firIncludeFilter);
			
			if(firIncludePoly.size() > 0){
				Node polNode = firIncludePoly.elementAt(0);
				String polyHtml = polNode.toHtml();
				
				Parser polyParser = new Parser(polyHtml);
				NodeFilter liFilter = new TagNameFilter("li");
				NodeList liList = polyParser.parse(liFilter);
				
				ArrayList<String> semList = new ArrayList<String>();
				
				for(int i=0; i<liList.size(); i++){
					Node liNode = liList.elementAt(i);					
					//a title="?????" href=
					String liHtml = liNode.toHtml();
					if(liHtml.indexOf("a title=") >= 0){
						String seString = liHtml.substring(liHtml.indexOf("a title=")+9, liHtml.indexOf("href=")-2);
						System.out.println(seString);
						semList.add(seString);
					}else if(liHtml.indexOf("polysemeTitle") >= 0){
						String seString = liHtml.substring(liHtml.indexOf("polysemeTitle")+15, liHtml.indexOf("</span></li>"));
						System.out.println(seString);
						semList.add(seString);
					}
				}		
				
				return semList;
			}		
			
			//type-2:			//<div class="view-tip-pannel clearfix" 
			parser.reset();
			NodeFilter secIncludeFilter = new AndFilter(new TagNameFilter("div"),
					new HasAttributeFilter("class","view-tip-pannel clearfix"));
			
			NodeList secIncludePoly = parser.parse(secIncludeFilter);
			
			if(secIncludePoly.size() > 0){
				Node polNode = secIncludePoly.elementAt(0);
				String pHtml = polNode.toHtml();
				if(pHtml.indexOf("多义词") >= 0){
					parser.reset();
					//<ul class="custom_dot  para-list list-paddingleft-1">
					NodeFilter polyIncludeFilter = new AndFilter(new TagNameFilter("ul"),
							new HasAttributeFilter("class","custom_dot  para-list list-paddingleft-1"));
					
					NodeList polyTarget = parser.parse(polyIncludeFilter);
					if(polyTarget.size() > 0){
						Node targetNode = polyTarget.elementAt(0);						
						Parser polyParser = new Parser(targetNode.toHtml());
						NodeFilter liFilter = new TagNameFilter("li");
						NodeList liList = polyParser.parse(liFilter);
						
						ArrayList<String> semlList = new ArrayList<String>();
						
						for(int i=0; i<liList.size(); i++){
							Node liNode = liList.elementAt(i);	
							Node aNode = liNode.getFirstChild().getFirstChild();							
							String aHtml = aNode.toHtml();
							String text = aHtml.substring(aHtml.indexOf("htm")+5, aHtml.indexOf("</a>"));
							String finalTextString = text.substring(text.indexOf("：")+1);
							System.out.println(finalTextString);
							semlList.add(finalTextString);
						}
						
						return semlList;
					}
				}			
			}
			
			return null;
			
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		
		return null;
		
	}
	//
	public static void parseBaikeHtml(String idStr, StringBuffer html){
		//null case
		if(null == html){
			return;
		}
		//
		try{
			Parser parser = new Parser(html.toString());
			//whether included in baike
			//<div class="direct_holder">
			NodeFilter include_Filter = new AndFilter(new TagNameFilter("div"),
					new HasAttributeFilter("class","direct_holder"));
			NodeList include_NL = parser.parse(include_Filter);
			if(include_NL.size() > 0){
				//not directly included
				System.out.println(idStr + " not directly included!");
				try{
					//String listF = "E:/Data_Log/ExperimentData/ntcir_10/BaiKe/"+idStr+"-0.txt";
					//BufferedWriter listW = IOBox.getBufferedWriter(listF);
					//listW.flush();
					//listW.close();
				}catch(Exception e){
					e.printStackTrace();
				}
			}else{
				parser.reset();
				//
				//multiple meanings
				//<span class="small thin">
				NodeFilter multiM_Filter = new AndFilter(	new TagNameFilter("span"),
						new HasAttributeFilter("class","small thin"));
				//
				NodeList multiM_NL = parser.parse(multiM_Filter);
				//
				if(multiM_NL.size()>0){
					parser.reset();
					//
					MultiCatalog multiCatalog = new MultiCatalog();
					//
					NodeFilter sublist_Filter = new AndFilter(	new TagNameFilter("span"),
							new HasAttributeFilter("class","sublist-span"));
					//
					NodeList sublist_NL = parser.parse(sublist_Filter);
					//
					for(int i=0; i<sublist_NL.size(); i++){
						String htmlSegment = sublist_NL.elementAt(i).toHtml();
						//System.out.println(htmlSegment);
						htmlSegment = htmlSegment.replaceAll("<[^>]+>", "");
						multiCatalog.addCatalogName(htmlSegment);
						//System.out.println(htmlSegment);
					}
					//
					parser.reset();
					//				
					NodeFilter catalog_Filter = new AndFilter(new TagNameFilter("a"),
							new AndFilter(new HasAttributeFilter("catalog","true"), 
									new HasAttributeFilter("name","STAT_ONCLICK_UNSUBMIT_CATALOG")));
					//
					NodeList catalog_NL = parser.parse(catalog_Filter);
					String htmlStr;
					for(int i=0; i<catalog_NL.size(); i++){
						htmlStr = catalog_NL.elementAt(i).toHtml();
						//System.out.println(htmlStr);	
						Matcher firstMat, secondMat;
						firstMat = firstCatalogEditionPattern.matcher(htmlStr);					
						if(firstMat.find()){
							//"#2"
							String numStr = firstMat.group();						
							String num = numStr.substring(2, numStr.length()-1);						
							//System.out.println(num);
							//
							String catalogStr = htmlStr.replaceAll("<[^>]+>", "");
							multiCatalog.addCatalogStr(0, catalogStr);
						}else{
							secondMat = secondCatalogEditionPattern.matcher(htmlStr);
							if(secondMat.find()){
								//"#3-1"
								String numStr = secondMat.group();
								int splitIndex = numStr.indexOf("-");
								String rNum = numStr.substring(2, splitIndex);
								String cNum = numStr.substring(splitIndex+1, numStr.length()-1);							
								//System.out.println(rNum+"-"+cNum);
								//
								String catalogStr = htmlStr.replaceAll("<[^>]+>", "");
								multiCatalog.addCatalogStr(Integer.parseInt(rNum), catalogStr);							
							}						
						}				
					}
					//
					//outputMultiCatalog(idStr, multiCatalog);
					System.out.println("multiple!");
					System.out.println(multiCatalog.toString());
				}else{
					//single
					//li class="hold-catalog-li"
					parser.reset();
					//
					MultiCatalog multiCatalog = new MultiCatalog();
					//
					NodeFilter catalog_Filter = new AndFilter(new TagNameFilter("a"),
							new AndFilter(new HasAttributeFilter("catalog","true"), 
									new HasAttributeFilter("name","STAT_ONCLICK_UNSUBMIT_CATALOG")));
					//
					NodeList catalog_NL = parser.parse(catalog_Filter);
					String htmlStr;
					for(int i=0; i<catalog_NL.size(); i++){
						htmlStr = catalog_NL.elementAt(i).toHtml();
						String catalogStr = htmlStr.replaceAll("<[^>]+>", "");
						//catalog.addCatalogStr(catalogStr);
						multiCatalog.addCatalogName(catalogStr);
					}
					//
					parser.reset();
					//				
					NodeFilter subCatalog_Filter = new AndFilter(	new TagNameFilter("li"),
							new HasAttributeFilter("class","hold-catalog-li"));					
					//
					NodeList subCatalog_NL = parser.parse(subCatalog_Filter);
					String subCatalogHtmlStr;
					for(int i=0; i<subCatalog_NL.size(); i++){
						subCatalogHtmlStr = subCatalog_NL.elementAt(i).toHtml();
						//System.out.println(subCatalogHtmlStr);	
						///*
						Matcher fourthMat;
						fourthMat = fourthCatalogEditionPattern.matcher(subCatalogHtmlStr);
						if(fourthMat.find()){
							//"#3_1"
							String numStr = fourthMat.group();
							int splitIndex = numStr.indexOf("_");
							String rNum = numStr.substring(2, splitIndex);
							String cNum = numStr.substring(splitIndex+1, numStr.length()-1);							
							//System.out.println(rNum+"-"+cNum);
							//
							String subCatalogStr = subCatalogHtmlStr.replaceAll("<[^>]+>", "");
							multiCatalog.addCatalogStr(Integer.parseInt(rNum)-1, subCatalogStr);							
						}
						//*/
					}
					//
					//outputSingleCatalog(idStr, multiCatalog);
					System.out.println("single!");
					System.out.println(multiCatalog.toString());					
				}
			}			
		}catch(Exception e){
			e.printStackTrace();
		}		
	}
	/*
	public static void outputMultiCatalog(String idStr, MultiCatalog multiCatalog){
		
		try{
			String listF;
			//
			if(multiCatalog.catalogNameList.size() > 0){
				listF = "E:/Data_Log/ExperimentData/ntcir_10/BaiKe/"+idStr+"-1.txt";
			}else{
				listF = "E:/Data_Log/ExperimentData/ntcir_10/BaiKe/"+idStr+"-1-0.txt";
			}
			//
			BufferedWriter listW = IOBox.getBufferedWriter(listF);
			//
			listW.write("Multiple");
			listW.newLine();
			//
			for(int i=0; i<multiCatalog.catalogNameList.size(); i++){
				listW.write(multiCatalog.catalogNameList.get(i));
				//System.out.println(multiCatalog.catalogNameList.get(i));
				for(String ca: multiCatalog.catalogList.get(i).catalogStrList){
					//System.out.println("\t"+ca);
					listW.write("\t"+ca);
				}
				listW.newLine();
			}
			listW.flush();
			listW.close();
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	*/
	//
	/*
	public static void outputSingleCatalog(String idStr, MultiCatalog multiCatalog){
		
		try{
			String listF;
			//
			if(multiCatalog.catalogNameList.size() > 0){
				listF = "E:/Data_Log/ExperimentData/ntcir_10/BaiKe/"+idStr+"-1.txt";
			}else{
				listF = "E:/Data_Log/ExperimentData/ntcir_10/BaiKe/"+idStr+"-1-0.txt";
			}
			//
			BufferedWriter listW = IOBox.getBufferedWriter(listF);
			//
			listW.write("Single");
			listW.newLine();
			//
			for(int i=0; i<multiCatalog.catalogNameList.size(); i++){
				listW.write(multiCatalog.catalogNameList.get(i));
				//System.out.println(multiCatalog.catalogNameList.get(i));
				for(String ca: multiCatalog.catalogList.get(i).catalogStrList){
					//System.out.println("\t"+ca);
					listW.write("\t"+ca);
				}
				listW.newLine();
			}
			listW.flush();
			listW.close();
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	*/
	//
	public void baikeParse(String topicText, String topicID){
		//FormalTopic formalTopic = CandidateFactory.getAnSpecificFormalTopic(index-1);
		//
		try{
			String encodedTopic = URLEncoder.encode(topicText, "utf-8");
			String baikeUrl = "http://baike.baidu.com/searchword/?word="+encodedTopic+"&pic=1&sug=1&enc=utf-8";
			StringBuffer strBuffer = downLoader.getContent(baikeUrl);
			System.out.println(strBuffer.toString());
			//parseBaikeHtml(topicID, strBuffer);
			polyParse(topicID, strBuffer);
		}catch(Exception e){
			e.printStackTrace();
		}		
	}
	
	public ArrayList<String> polyBaikeParse(String topicText, String topicID){
		//FormalTopic formalTopic = CandidateFactory.getAnSpecificFormalTopic(index-1);
		//
		try{
			String encodedTopic = URLEncoder.encode(topicText, "utf-8");
			String baikeUrl = "http://baike.baidu.com/searchword/?word="+encodedTopic+"&pic=1&sug=1&enc=utf-8";
			StringBuffer strBuffer = downLoader.getContent(baikeUrl);
			System.out.println("processing \t"+topicText);
			return polyParse(topicID, strBuffer);
		}catch(Exception e){
			e.printStackTrace();
		}		
		
		return null;
	}
	
	public static void test(){
		String topicText = "猫头鹰";
		String id = "0001";
		
		BaikeParser baikeParser = new BaikeParser();
		
		baikeParser.baikeParse(topicText, id);
	}
	
	public static void main(String []args){
		//1
		BaikeParser.test();
		
	}
	
	}

	
