package org.archive.ntcir.sm.preprocess;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.archive.OutputDirectory;
import org.archive.dataset.DataSetDiretory;
import org.archive.dataset.ntcir.NTCIRLoader;
import org.archive.dataset.ntcir.NTCIRLoader.NTCIR_EVAL_TASK;
import org.archive.dataset.ntcir.sm.SMTopic;
import org.archive.nlp.chunk.lpt.ltpService.LTML;
import org.archive.nlp.chunk.lpt.ltpService.LTPOption;
import org.archive.nlp.chunk.lpt.ltpService.LTPService;
import org.archive.nlp.chunk.lpt.ltpService.SRL;
import org.archive.nlp.chunk.lpt.ltpService.Word;
import org.archive.nlp.fetch.WikiFetcher;
import org.archive.nlp.fetch.baike.BaikeParser;
import org.archive.nlp.qpunctuation.QueryPreParser;
import org.archive.nlp.tokenizer.Tokenizer;
import org.archive.util.Language.Lang;
import org.archive.util.io.IOText;
import org.archive.util.tuple.StrStr;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

public class TopicParser {
	//
	public static final boolean DEBUG = true;
	
	public static enum StringType {TopicStr, SubtopicStr}
	//0001	先知
	public static ArrayList<StrStr> loadTopicList_NTCIR11_SM_CH(){
		ArrayList<StrStr> topicList_NTCIR11_SM_CH = new ArrayList<StrStr>();
		try {
			String file = DataSetDiretory.ROOT+DataSetDiretory.NTCIR11_SM + "IMine.Query.txt";
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), NTCIRLoader.CODE_UTF8));
			//
			String line = null;
			String [] array;
			int lineCount = 1;
			while(null != (line=reader.readLine())){
				array = line.split("\t");
				topicList_NTCIR11_SM_CH.add(new StrStr(array[0], array[1]));
				lineCount++;
				if(lineCount > 50){
					break;
				}
			}
			reader.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		if(DEBUG){
			System.out.println("-----");
			for(StrStr topic: topicList_NTCIR11_SM_CH){
				System.out.println(topic.first+"\t"+topic.second);
			}
			System.out.println("-----");
		}
		return topicList_NTCIR11_SM_CH;
	}
	//parse each topic string using LTPservice
	private static void parseNTCIR11SMChTopics(ArrayList<StrStr> topicList, String ltpOption){
		//
		File dirFile = new File(OutputDirectory.NTCIR11_Buffer);
		if(!dirFile.exists()){
			dirFile.mkdirs();
		}
		//
		String outputFile = null;
		LTPService ls = new LTPService("yu-haitao@iss.tokushima-u.ac.jp:IF7ynN42"); 
		for(StrStr topic: topicList){
			 outputFile = OutputDirectory.NTCIR11_Buffer+topic.first+".xml";
			 //
			 try {
	            ls.setEncoding(LTPOption.UTF8);
	            //perform all operations!!!
	            LTML ltml = ls.analyze(ltpOption, topic.getSecond());
	            ltml.saveDom(outputFile);
			 }catch(Exception e1){
				 e1.printStackTrace();				 
			 }
			 //
			 try{
				 System.out.println("snooze...");
				 Thread.sleep(100);
			 }catch(InterruptedException e){
				 e.printStackTrace();
			 }
		}
		ls.close();
	}
	//
	private static void parseNTCIR11SMChTopics(String ltpOption){
		parseNTCIR11SMChTopics(loadTopicList_NTCIR11_SM_CH(), ltpOption);
	}
		
	//
	private static void getUniqueSubtopicStringFile(){
		List<SMTopic> smTopicList = NTCIRLoader.loadNTCIR11TopicList(NTCIR_EVAL_TASK.NTCIR11_SM_CH, false);
		try {
			File dirFile = new File(OutputDirectory.NTCIR11_Buffer);
			if(!dirFile.exists()){
				dirFile.mkdirs();
			}
			//
			String targetFile = OutputDirectory.NTCIR11_Buffer+"UniqueSubtopicStrings.txt";
			BufferedWriter writer = IOText.getBufferedWriter_UTF8(targetFile);
			for(SMTopic smTopic: smTopicList){
				int id = 1;
				String reference = Tokenizer.isDirectWord(smTopic.getTopicText(), Lang.Chinese)?smTopic.getTopicText():null;
				for(String str: smTopic.uniqueRelatedQueries){
					if(!QueryPreParser.isOddQuery(str, Lang.Chinese)){
						continue;
					}
					writer.write(smTopic.getID()+"\t"
							+(id++)+"\t"
							+str+"\t@@"
							+Tokenizer.replaceSymboleAsBlank(Lang.Chinese, str, reference));
					writer.newLine();
				}
			}
			writer.flush();
			writer.close();			
		} catch (Exception e) {
			// TODO: handle exception
		}
	}
	//
	private static void parseNTCIR11SMChSubtopicStrings(){
		try {
			String inputFile = "E:/v-haiyu/CodeBench/Pool_Output/Output_DiversifiedRanking/ntcir-11/SM/SubtopicString/UniqueSubtopicStrings.txt";
			//
			LTPService ls = new LTPService("yu-haitao@iss.tokushima-u.ac.jp:IF7ynN42"); 
			ArrayList<String> lineList = IOText.getLinesAsAList_UTF8(inputFile);
			//
			for(String line: lineList){
				String [] array = line.split("\t");
				if(array.length == 4){
					String topicID = array[0];
					String id = array[1];
					String targetString = array[3].substring(2);
					//
					String outputFile = OutputDirectory.NTCIR11_Buffer+topicID+"-"+id+".xml";
					 //
					 try {
			            ls.setEncoding(LTPOption.UTF8);
			            //perform all operations!!!
			            LTML ltml = ls.analyze(LTPOption.ALL, targetString);
			            ltml.saveDom(outputFile);
					 }catch(Exception e1){
						 e1.printStackTrace();				 
					 }
					 //
					 try{
						 System.out.println("snooze...");
						 Thread.sleep(100);
					 }catch(InterruptedException e){
						 e.printStackTrace();
					 }
				}else{
					System.out.println("bad line:\t"+line);
				}
			}
		} catch (Exception e) {
			// TODO: handle exception
		}
	}
	
	//
	public static void ltpParsing(ArrayList<StrStr> objectList, String targetFile){
		Format format = Format.getCompactFormat();
		format.setEncoding(LTPOption.UTF8);		
		format.setIndent("  ");
		XMLOutputter outputter = new XMLOutputter(format);
		//
		try {
			BufferedWriter writer = IOText.getBufferedWriter_UTF8(targetFile);
			int count = 1;
			//
			LTPService ls = new LTPService("yu-haitao@iss.tokushima-u.ac.jp:IF7ynN42"); 
			ls.setEncoding(LTPOption.UTF8);
			//
			for(StrStr object: objectList){
				count++;
				if(0 == count%10000){
					writer.flush();
				}
				//perform all operations!!!
				try {					
		            LTML ltml = ls.analyze(LTPOption.ALL, object.getSecond());
		            writer.write(object.getFirst());
		            writer.newLine();
		            outputter.output(ltml.getDom(), writer);	
		            writer.newLine();
				} catch (Exception e) {					
					e.printStackTrace();
					continue;
				}
				//snooze
				try{
					 System.out.println("snooze...");
					 Thread.sleep(100);
				 }catch(InterruptedException e){
					 e.printStackTrace();
					 continue;
				 }
			}
			//
			writer.flush();
			writer.close();
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}		
	}
	
	//
	private static String loadAFile(String targetFile){
		try {
			StringBuffer buffer = new StringBuffer();
			//
			BufferedReader reader = IOText.getBufferedReader_UTF8(targetFile);
			String line = null;			
			while(null != (line=reader.readLine())){
				if(line.length() > 0){					
					buffer.append(line);
					buffer.append("\n");
				}				
			}
			reader.close();
			return buffer.toString();			
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		return null;
	}
	//
	public static LTML loadParsedObject(StringType strtype, String xmlFile){
		String dir = null;
		if(StringType.TopicStr == strtype){
			dir = OutputDirectory.ROOT+"ntcir-11/SM/ParsedTopic/PerFile/";
		}else if(StringType.SubtopicStr == strtype){
			dir = OutputDirectory.ROOT+"ntcir-11/SM/SubtopicString/ParsedWithLTP/";
		}
		//
		try {
			LTML ltml = new LTML();
			System.out.println(dir+xmlFile);
			ltml.build(loadAFile(dir+xmlFile));
			return ltml;
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		//
		return null;		
	}
	public static LTML loadParsedObject(String xmlFile){		
		try {
			LTML ltml = new LTML();
			System.out.println(xmlFile);
			ltml.build(loadAFile(xmlFile));
			System.out.println(ltml.toString());
			return ltml;
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		//
		return null;		
	}

	
	public static void parseTest(){
		ArrayList<StrStr> topicList = loadTopicList_NTCIR11_SM_CH();
		String targetFile = OutputDirectory.ROOT+"ntcir-11/buffer/ParsedTopics.txt";
		ltpParsing(topicList, targetFile);
	}
	//-------------
	private static void parseTopicTest(){
		StrStr smTopic = new StrStr("0017", "̩���ز�");
		File dirFile = new File(OutputDirectory.NTCIR11_Buffer);
		if(!dirFile.exists()){
			dirFile.mkdirs();
		}
		String outputFile = OutputDirectory.NTCIR11_Buffer+smTopic.first+".xml";
		//
		LTPService ls = new LTPService("yu-haitao@iss.tokushima-u.ac.jp:IF7ynN42"); 
		//
        try {
            ls.setEncoding(LTPOption.UTF8);
            LTML ltml = ls.analyze(LTPOption.PARSER, smTopic.getSecond());
            ltml.saveDom(outputFile);
            /*
            int sentNum = ltml.countSentence();
            for(int i = 0; i< sentNum; ++i){
                ArrayList<Word> wordList = ltml.getWords(i);
                System.out.println(ltml.getSentenceContent(i));
                for(int j = 0; j < wordList.size(); ++j){
                    System.out.print("\t" + wordList.get(j).getWS());
                    System.out.print("\t" + wordList.get(j).getPOS());
                    System.out.print("\t" + wordList.get(j).getNE());
                    System.out.print("\t" + wordList.get(j).getParserParent() + 
                            "\t" + wordList.get(j).getParserRelation());
                    if(ltml.hasSRL() && wordList.get(j).isPredicate()){
                        ArrayList<SRL> srls = wordList.get(j).getSRLs();
                        System.out.println();
                        for(int k = 0; k <srls.size(); ++k){
                            System.out.println("\t\t" + srls.get(k).type + 
                                    "\t" + srls.get(k).beg +
                                    "\t" + srls.get(k).end);
                        }
                    }
                    System.out.println();
                }
            }
            */
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            ls.close();
        }
	}
	//
	public static void loadTest(){
		String xmlFile = "0001.xml";		
		LTML parstedStr = loadParsedObject(StringType.TopicStr, xmlFile);
		//
		System.out.println("---------");
		parstedStr.printXml();		
		System.out.println("---------");
		//
		int sentNum = parstedStr.countSentence();
        for(int i = 0; i< sentNum; ++i){
            ArrayList<Word> wordList = parstedStr.getWords(i);
            System.out.println(parstedStr.getSentenceContent(i));
            for(int j = 0; j < wordList.size(); ++j){
                System.out.print("\t" + wordList.get(j).getWS());
                System.out.print("\t" + wordList.get(j).getPOS());
                System.out.print("\t" + wordList.get(j).getNE());
                System.out.print("\t" + wordList.get(j).getParserParent() + 
                        "\t" + wordList.get(j).getParserRelation());
                if(parstedStr.hasSRL() && wordList.get(j).isPredicate()){
                    ArrayList<SRL> srls = wordList.get(j).getSRLs();
                    System.out.println();
                    for(int k = 0; k <srls.size(); ++k){
                        System.out.println("\t\t" + srls.get(k).type + 
                                "\t" + srls.get(k).beg +
                                "\t" + srls.get(k).end);
                    }
                }
                System.out.println();
            }
        }
        //
		System.out.println(parstedStr.toString());
	}
	
	/////////////////////
	//fetch pages
	/////////////////////
	public static void fetchWikiPagesForEnTopics(){
		try {
			List<SMTopic> smTopicList = NTCIRLoader.loadNTCIR11TopicList(NTCIR_EVAL_TASK.NTCIR11_SM_EN, false);
			
			WikiFetcher enWikiFetcher = new WikiFetcher(Lang.English);
			
			for(SMTopic smTopic: smTopicList){
				String title = smTopic.getTopicText();
				String idString = smTopic.getID();
				
				String pageFile = OutputDirectory.NTCIR11_Buffer+"wikipage_"+idString+".txt";
				String disamPageFile = OutputDirectory.NTCIR11_Buffer+"wikidispage_"+idString+".txt";
				
				String page = enWikiFetcher.fetchWikiPage(title);
				String dispage = enWikiFetcher.fetchWikiDisambiguationPage(title);
				
				if(null!=page && page.length()>0){
					BufferedWriter pageWriter = IOText.getBufferedWriter_UTF8(pageFile);
					pageWriter.write(page);
					pageWriter.flush();
					pageWriter.close();
				}
				
				if(null!=dispage && dispage.length()>0){
					BufferedWriter dispageWriter = IOText.getBufferedWriter_UTF8(disamPageFile);
					dispageWriter.write(dispage);
					dispageWriter.flush();
					dispageWriter.close();
				}
				
				 try{
					 System.out.println("snooze...");
					 Thread.sleep(10000);
				 }catch(InterruptedException e){
					 e.printStackTrace();
				 }				
			}
		} catch (Exception e) {
			// TODO: handle exception
		}
	}
	//
	public static void fetchBaikePolyPagesForChTopics(){
		try {
			List<SMTopic> smTopicList = NTCIRLoader.loadNTCIR11TopicList(NTCIR_EVAL_TASK.NTCIR11_SM_CH, false);
			
			BaikeParser baikeFetcher = new BaikeParser();
			
			for(SMTopic smTopic: smTopicList){
				String title = smTopic.getTopicText();
				String idString = smTopic.getID();
				
				String polyFile = OutputDirectory.NTCIR11_Buffer+"baikepoly_"+idString+".txt";
				
				ArrayList<String> polyList = baikeFetcher.polyBaikeParse(title, idString);
				
				if(null!=polyList && polyList.size()>0){
					BufferedWriter pageWriter = IOText.getBufferedWriter_UTF8(polyFile);
					for(String se: polyList){
						pageWriter.write(se);
						pageWriter.newLine();
					}					
					pageWriter.flush();
					pageWriter.close();
				}
				
				 try{
					 System.out.println("snooze...");
					 Thread.sleep(10000);
				 }catch(InterruptedException e){
					 e.printStackTrace();
				 }				
			}
		} catch (Exception e) {
			// TODO: handle exception
		}
	}
	
	
	
	
	
	
	
	public static void main(String []args){
		// test
		//TopicParser.parseTopicTest();
		//TopicParser.loadTopicList_NTCIR11_SM_CH();
		
		//1 parseNTCIR11SMChTopics
		//TopicParser.parseNTCIR11SMChTopics(LTPOption.ALL);
		
		//2
		///////////////////// cases ///////////////////////////////
		/**
		 * (1) adobe+flash+player+官方下载	@@adobe+flash+player 官方下载
		 * (2) 0003	23	猫头鹰与小飞象国语版	@@猫头鹰 与小飞象国语版
		 * (3) 0001	15	弟于长 宜先知	@@弟于长 宜 先知
		 * **/
		//TopicParser.getUniqueSubtopicStringFile();
		
		//3 parse subtopic strings
		//TopicParser.parseNTCIR11SMChSubtopicStrings();
		
		//4
		//TopicParser.loadTest();
		//String file = "E:/v-haiyu/CodeBench/Pool_Output/Output_DiversifiedRanking/ntcir-11/SM/ParsedTopic/PerFile/0001.xml";
		//TopicParser.loadParsedObject(file);
		
		//5
		//TopicParser.parseTest();
		
		//6
		//TopicParser.fetchWikiPagesForEnTopics();
		//TopicParser.fetchBaikePolyPagesForChTopics();
		
		//7
		
	}

}
