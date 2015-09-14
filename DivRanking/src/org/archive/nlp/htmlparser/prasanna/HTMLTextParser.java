package org.archive.nlp.htmlparser.prasanna;

/*
 * HTMLTextParser.java
 * Author: S.Prasanna
 *
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.PrintWriter;

import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.xml.sax.InputSource;

import org.cyberneko.html.parsers.DOMFragmentParser;

import org.apache.xerces.dom.CoreDocumentImpl;
 
public class HTMLTextParser {
	
	FileInputStream fin = null;
	StringBuffer TextBuffer = null;
	InputSource inSource = null;
	
	// HTMLTextParser Constructor 
    public HTMLTextParser() {
    }
    
    //Gets the text content from Nodes recursively
    void processNode(Node node) {
    	if (node == null) return;
    	
    	//Process a text node
    	if (node.getNodeType() == node.TEXT_NODE) {
    		TextBuffer.append(node.getNodeValue());
    	} else if (node.hasChildNodes()) {
			//Process the Node's children 
					
			NodeList childList = node.getChildNodes();
			int childLen = childList.getLength();
			
			for (int count = 0; count < childLen; count ++) 
				processNode(childList.item(count));					
		}
		else return;
    }
    
    // Extracts text from HTML Document
    String htmltoText(String fileName, String code) {
    	
    	DOMFragmentParser parser = new DOMFragmentParser();
    	
    	System.out.println("Parsing text from HTML file " + fileName + "....");
        File f = new File(fileName);
        
        if (!f.isFile()) {
            System.out.println("File " + fileName + " does not exist.");
            return null;
        }
        
        try {
            fin = new FileInputStream(f);
        } catch (Exception e) {
            System.out.println("Unable to open HTML file " + fileName + " for reading.");
            return null;
        } 
        	
        try {
            inSource = new InputSource(fin);
            inSource.setEncoding(code);
        } catch (Exception e) {
            System.out.println("Unable to open Input source from HTML file " + fileName);
            return null;
        } 
        
        CoreDocumentImpl codeDoc = new CoreDocumentImpl();
        DocumentFragment doc = codeDoc.createDocumentFragment();
        
        try {
        	parser.parse(inSource, doc);
        } catch (Exception e) {
        	System.out.println("Unable to parse HTML file " + fileName);
        	return null;       	
        }
        
        TextBuffer = new StringBuffer();
        
        //Node is a super interface of DocumentFragment, so no typecast needed
        processNode(doc);
        
        System.out.println("Done.");
    		
    	return TextBuffer.toString();
    }     
    
    // Writes the parsed text from HTML to a file
    void writeTexttoFile(String htmlText, String fileName) {
    	
    	System.out.println("\nWriting HTML text to output text file " + fileName + "....");
    	try {
    		PrintWriter pw = new PrintWriter(fileName);
    		pw.print(htmlText);
    		pw.close();    	
    	} catch (Exception e) {
    		System.out.println("An exception occurred in writing the html text to file.");
    		e.printStackTrace();
    	}
    	System.out.println("Done.");
    }
    
    // Extracts text from an HTML Document and writes it to a text file
    public static void main(String args[]) {
    	/*
    	if (args.length != 2) {
        	System.out.println("Usage: java HTMLTextParser <InputHTMLFile> <OutputTextFile>");
        	System.exit(1);
        }
        */
    	
    	String htm = "E:/Data_Log/DataSource_Raw/NTCIR-10/DocumentRanking/doc/top-100/0201/a2a6db21a7a5d8ba-1368c369d8c38c60.htm";
		String htm_2 = "E:/Data_Log/DataSource_Raw/NTCIR-10/DocumentRanking/doc/top-100/0004/1f86a7e6678bf8a1-071d026ec8f3fbc0.htm";
		    	
        HTMLTextParser htmlTextParserObj = new HTMLTextParser();
        //String htmlToText = htmlTextParserObj.htmltoText(args[0]);
        String htmlToText = htmlTextParserObj.htmltoText(htm, "GB2312");
        
        if (htmlToText == null) {
        	System.out.println("HTML to Text Conversion failed.");
        }
        else {
        	System.out.println("\nThe text parsed from the HTML Document....\n" + htmlToText);
        	//htmlTextParserObj.writeTexttoFile(htmlToText, args[1]);
        }
    }  
}

