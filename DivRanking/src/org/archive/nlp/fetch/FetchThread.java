package org.archive.nlp.fetch;

import java.io.BufferedWriter;
import java.util.ArrayList;

import org.archive.util.format.StandardFormat;
import org.archive.util.io.IOText;

public class FetchThread extends Thread {
	String _threadID;
	//
	HttpDownloader downloader = new HttpDownloader(0, 100);
	//
	ArrayList<String> urlList = null;
	String _outputDir;
	int _numPerFile;
	
	public FetchThread(String threadID, String urlFile, String outputDir, int numPerFile){
		this._threadID = threadID;
		this._outputDir = outputDir+"/";
		this._numPerFile = numPerFile;
		
		try {
			urlList = IOText.getLinesAsAList_UTF8(urlFile);
		} catch (Exception e) {
			e.printStackTrace();
		}		
	}
	
	public void run(){
		
		int txtFileID = 0, outputCount = 0;
		String txtFile = null;
		StringBuffer htmlBuf = null;
		
		//statistic
		int successCount = 0;
		int failedCount = 0;		
		//int testCount = 53;
		
		try {

			BufferedWriter logWriter = IOText.getBufferedWriter_UTF8(this._outputDir+_threadID+"-"+"FetchLog.txt");
						
			txtFile = this._outputDir+_threadID+"-"+StandardFormat.serialFormat(++txtFileID, "00000000")+".txt";			
			BufferedWriter htmlWriter = IOText.getBufferedWriter_UTF8(txtFile);
			
			BufferedWriter failWriter = IOText.getBufferedWriter_UTF8(this._outputDir+_threadID+"-"+"FailedUrlList.txt");
			
			for(int i=0; i<urlList.size(); i++){
				logWriter.write(i+1);
				logWriter.newLine();
				
				/*
				if(i> testCount){
					break;
				}
				*/
				
				String url = urlList.get(i);
				
				////---
				try {
					htmlBuf=downloader.getContent(url);
				} catch (Exception e) {
					// TODO: handle exception
					e.printStackTrace();
					htmlBuf = null;
				}
				
				if(null != htmlBuf){
					successCount++;
					
					String html = htmlBuf.toString();
					
					if((outputCount>0) && (0 == (outputCount%this._numPerFile))){
						htmlWriter.flush();
						htmlWriter.close();
						htmlWriter = null;
						
						txtFile = this._outputDir+_threadID+"-"+StandardFormat.serialFormat(++txtFileID, "00000000")+".txt";
						htmlWriter = IOText.getBufferedWriter_UTF8(txtFile);
						
						htmlWriter.write("<doc>");			 	htmlWriter.newLine();
						htmlWriter.write("<url>"+url+"</url>"); htmlWriter.newLine();
						htmlWriter.write(html);					htmlWriter.newLine();
						htmlWriter.write("</doc>");				htmlWriter.newLine();
					
					}else{					
						
						htmlWriter.write("<doc>");			 	htmlWriter.newLine();
						htmlWriter.write("<url>"+url+"</url>"); htmlWriter.newLine();
						htmlWriter.write(html);					htmlWriter.newLine();
						htmlWriter.write("</doc>");				htmlWriter.newLine();						
					}
					
					outputCount++;
				}else{	
					failedCount++;
					
					failWriter.write(url);
					failWriter.newLine();
				}
				////---
			}
			
			//final
			htmlWriter.flush();
			htmlWriter.close();
			
			failWriter.flush();
			failWriter.close();
			
			logWriter.write("SuccessCount:\t"+successCount);
			logWriter.newLine();
			logWriter.write("FailedCount:\t"+failedCount);
			logWriter.newLine();
			logWriter.flush();
			logWriter.close();			
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}		
	}

}
