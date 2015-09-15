package org.archive.nlp.fetch;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;

import org.archive.util.format.StandardFormat;
import org.archive.util.io.IOText;

public class MultiThreadFetcher {
	/**
	 * fetch html source files given the different urlList files
	 * **/
	private static void parallelFetching(String targetDir, String resultDir, int numPerFile){
		
		ArrayList<FetchThread> threadList = new ArrayList<FetchThread>();
		
		try {
			PrintStream sysLogPrinter = new PrintStream(new FileOutputStream(new File(resultDir+"SysLog.txt")));
			System.setOut(sysLogPrinter);
			PrintStream errLogPrinter = new PrintStream(new FileOutputStream(new File(resultDir+"ErrLog.txt")));
			System.setErr(errLogPrinter);
			
			
			File _targetDir = new File(targetDir);
			File [] _targetFiles = _targetDir.listFiles();
			
			for(File _file: _targetFiles){
				String name = _file.getName();
				File _outputDir = new File(resultDir+"/"+name+"/");
				if(!_outputDir.exists()){
					_outputDir.mkdirs();
				}
				
				threadList.add(new FetchThread(name, _file.getAbsolutePath(), _outputDir.getAbsolutePath(), numPerFile));				
			}
			
			for(FetchThread thread: threadList){
				thread.start();
			}
			
			sysLogPrinter.flush();
			sysLogPrinter.close();
			
			errLogPrinter.flush();
			errLogPrinter.close();
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}
	
	private static void singleFetching(String targetDir, String resultDir, int numPerFile){
		try {
			PrintStream logPrinter = new PrintStream(new FileOutputStream(new File(resultDir+"SystemLog.txt")));
			System.setOut(logPrinter);
			
			PrintStream errLogPrinter = new PrintStream(new FileOutputStream(new File(resultDir+"ErrLog.txt")));
			System.setErr(errLogPrinter);
			
			File _targetDir = new File(targetDir);
			File [] _targetFiles = _targetDir.listFiles();
			
			for(File _file: _targetFiles){
				String name = _file.getName();
				File _outputDir = new File(resultDir+"/"+name+"/");
				if(!_outputDir.exists()){
					_outputDir.mkdirs();
				}
				
				//threadList.add(new FetchThread(name, _file.getAbsolutePath(), _outputDir.getAbsolutePath(), numPerFile));		
				sinRun(name, _file.getAbsolutePath(), _outputDir.getAbsolutePath(), numPerFile);
				
				System.out.println("Finish one file!");
			}
			
			logPrinter.flush();
			logPrinter.close();			
			
			errLogPrinter.flush();
			errLogPrinter.close();
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}		
	}
	
	private static void sinRun(String _threadID, String urlFile, String _outputDir, int _numPerFile){
		HttpDownloader downloader = null;
		ArrayList<String> urlList = null;
		try {
			downloader = new HttpDownloader(0, 100);
			
			urlList = IOText.getLinesAsAList_UTF8(urlFile);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		_outputDir = _outputDir+"/";
		
		int txtFileID = 0, outputCount = 0;
		String txtFile = null;
		StringBuffer htmlBuf = null;
		
		//statistic
		int successCount = 0;
		int failedCount = 0;		
		//int testCount = 53;
		
		try {

			BufferedWriter logWriter = IOText.getBufferedWriter_UTF8(_outputDir+_threadID+"-"+"FetchLog.txt");
						
			txtFile = _outputDir+_threadID+"-"+StandardFormat.serialFormat(++txtFileID, "00000000")+".txt";			
			BufferedWriter htmlWriter = IOText.getBufferedWriter_UTF8(txtFile);
			
			BufferedWriter failWriter = IOText.getBufferedWriter_UTF8(_outputDir+_threadID+"-"+"FailedUrlList.txt");
			
			for(int i=0; i<urlList.size(); i++){
				////--
				System.out.println(_threadID +"-"+ i);
				//logWriter.write("--"+(i+1));
				//logWriter.newLine();
				
				/*
				if(i> testCount){
					break;
				}
				*/
				
				String url = urlList.get(i);
				//System.out.println(url);
				
				////---
				try {
					htmlBuf=downloader.getContent(url);
				} catch (Exception e) {
					// TODO: handle exception
					//e.printStackTrace();
					System.out.println("!!!");
					htmlBuf = null;
				}
				////---
				
				if(null != htmlBuf){
					successCount++;
					
					String html = htmlBuf.toString();
					
					if((outputCount>0) && (0 == (outputCount%_numPerFile))){
						htmlWriter.flush();
						htmlWriter.close();
						htmlWriter = null;
						
						txtFile = _outputDir+_threadID+"-"+StandardFormat.serialFormat(++txtFileID, "00000000")+".txt";
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
			
			downloader = null;
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}	
		
	}
	
	public static void main(String []agrs){
		//1
		/*
		String targetDir = "../../../../Corpus/DataSource_Analyzed/WeekOrganicLog/AtLeast_2Clicks/Fetch_Targets_5h/";
		String resultDir = "../../../../Corpus/DataSource_Analyzed/WeekOrganicLog/AtLeast_2Clicks/Fetch_Results_5h/";
		MultiThreadFetcher.parallelFetching(targetDir, resultDir, 2000);
		*/
		
		//2
		//bad case:http://calm1.calmradio.com:10708/
		///*
		String targetDir = "../../../../Corpus/DataSource_Analyzed/WeekOrganicLog/AtLeast_2Clicks/Fetch_Targets_7th/";
		String resultDir = "../../../../Corpus/DataSource_Analyzed/WeekOrganicLog/AtLeast_2Clicks/Fetch_Results_7th/";
		MultiThreadFetcher.singleFetching(targetDir, resultDir, 2000);
		//*/
	}

}
