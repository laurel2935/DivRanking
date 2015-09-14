package org.archive.nlp.htmlparser.tika;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.html.*;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

public class TIKAParser {
	
	public static String htmlToText(File f) {  
        Parser parser = new AutoDetectParser();//自动检测文档类型，自动创建相应的解析器  
        InputStream is = null;  
        try {  
            Metadata metadata = new Metadata();  
            metadata.set(Metadata.AUTHOR, "空号");//重新设置文档的媒体内容  
            metadata.set(Metadata.RESOURCE_NAME_KEY, f.getName());  
            is = new FileInputStream(f);  
            ContentHandler handler = new BodyContentHandler();  
            ParseContext context = new ParseContext();  
            context.set(Parser.class,parser);  
            parser.parse(is,handler, metadata,context);
            ///*
            for(String name:metadata.names()){
            	System.out.println(name+"\t");
            }
            System.out.println();
            for(String name:metadata.names()) {  
                System.out.println(name+":"+metadata.get(name));  
            }  
            //*/
            return handler.toString();  
        } catch (FileNotFoundException e) {  
            e.printStackTrace();  
        } catch (IOException e) {  
            e.printStackTrace();  
        } catch (SAXException e) {  
            e.printStackTrace();  
        } catch (TikaException e) {  
            e.printStackTrace();  
        } finally {  
            try {  
                if(is!=null) is.close();  
            } catch (IOException e) {  
                e.printStackTrace();  
            }  
        }  
        return null;  
    }  
	
	public static void htmlToText_2(File f) { 
		try {
			  Parser parser = new HtmlParser();
			  InputStream iStream = new BufferedInputStream(new FileInputStream(f));
			  //OutputStream oStream = new BufferedOutputStream(new FileOutputStream(new File(OUTPATH)));
			  
			  ContentHandler iHandler = new BodyContentHandler();
			  parser.parse(iStream, iHandler, new Metadata(), new ParseContext());
			  
//			  BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(new File(OUTPATH))));
//			  StringBuffer s = new StringBuffer(2000);
//			  String l = "";
//			  while((l = br.readLine()) != null){
//			   s.append(l);
//			  }
//			  br.close();
//			  iStream.close();
//			  oStream.close();
			  System.out.println(iHandler.toString());
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}		  
	}
	
	//
	public static void main(String []args){
		
		//String htm = "E:/Data_Log/DataSource_Raw/NTCIR-10/DocumentRanking/doc/top-100/0201/a2a6db21a7a5d8ba-1368c369d8c38c60.htm";
		String htm_2 = "E:/Data_Log/DataSource_Raw/NTCIR-10/DocumentRanking/doc/top-100/0004/1f86a7e6678bf8a1-071d026ec8f3fbc0.htm";
		File file = new File(htm_2);
		//1
		System.out.println(TIKAParser.htmlToText(file));
		//2
		//TIKAParser.htmlToText_2(file);
	}

}
