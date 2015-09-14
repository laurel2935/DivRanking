package org.archive.nlp.fetch;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;

public class MultiThreadFetcher {
	/**
	 * fetch html source files given the different urlList files
	 * **/
	private static void parallelFetching(String targetDir, String resultDir, int numPerFile){
		
		ArrayList<FetchThread> threadList = new ArrayList<FetchThread>();
		
		try {
			PrintStream logPrinter = new PrintStream(new FileOutputStream(new File(resultDir+"SystemLog.txt")));
			System.setOut(logPrinter);
			
			
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
			
			logPrinter.flush();
			logPrinter.close();
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}
	
	public static void main(String []agrs){
		//1
		String targetDir = "C:/T/WorkBench/Corpus/DataSource_Analyzed/FilteredBingOrganicSearchLog_AtLeast_2Click/Fetch_Targets/";
		String resultDir = "C:/T/WorkBench/Corpus/DataSource_Analyzed/FilteredBingOrganicSearchLog_AtLeast_2Click/Fetch_Results/";
		MultiThreadFetcher.parallelFetching(targetDir, resultDir, 2000);
	}

}
