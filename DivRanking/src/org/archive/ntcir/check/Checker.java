package org.archive.ntcir.check;

import java.util.ArrayList;
import java.util.HashSet;

import org.archive.OutputDirectory;
import org.archive.dataset.ntcir.NTCIRLoader.NTCIR_EVAL_TASK;
import org.archive.util.format.StandardFormat;
import org.archive.util.io.IOText;

public class Checker {
	
	
	//
	public static void checkNoResultTopic(NTCIR_EVAL_TASK eval, String resultFile){
		ArrayList<String> totalIDList = new ArrayList<String>();
		//
		if(NTCIR_EVAL_TASK.NTCIR11_SM_CH==eval || NTCIR_EVAL_TASK.NTCIR11_DR_CH==eval){
			for(int t=1; t<=50; t++){
				totalIDList.add(StandardFormat.serialFormat(t, "0000"));
			}
		}else if(NTCIR_EVAL_TASK.NTCIR11_SM_EN==eval || NTCIR_EVAL_TASK.NTCIR11_DR_EN==eval){
			for(int t=51; t<=100; t++){
				totalIDList.add(StandardFormat.serialFormat(t, "0000"));
			}
		}else{
			System.err.println("NTCIR_EVAL_TASK Input Error!");
			System.exit(1);
		}
		
		HashSet<String> resultIDSet = new HashSet<String>();
		
		ArrayList<String> lineList = IOText.getLinesAsAList_UTF8(resultFile);
		for(String line: lineList){
			String resultID = null;
			if(NTCIR_EVAL_TASK.NTCIR11_SM_CH==eval || NTCIR_EVAL_TASK.NTCIR11_SM_EN==eval){
				String [] fields = line.split(";");
				resultID = fields[0];
			}else if(NTCIR_EVAL_TASK.NTCIR11_DR_CH==eval || NTCIR_EVAL_TASK.NTCIR11_DR_EN==eval){
				String [] fields = line.split("\\s");
				resultID = fields[0];
			}else{				
				System.err.println("NTCIR_EVAL_TASK Input Error!");
				System.exit(1);
			}
			
			if(!resultIDSet.contains(resultID)){
				resultIDSet.add(resultID);
			}			
		}
		
		System.out.println(eval.toString());
		int count = 0;
		for(String topicID: totalIDList){
			if(!resultIDSet.contains(topicID)){
				System.err.println("No Result TopicID:\t"+topicID);
				count ++;
			}
		}	
		System.out.println("Count:\t"+count);
		System.out.println();
	}
	
	
	public static void main(String []args){
		String smResultDir = OutputDirectory.ROOT+"SMResult/";
		String drResultDir = OutputDirectory.ROOT+"DRResult/";
		
		//1 sm-ch-check
		//String smResult_ch = "TUTA1-S-C-1A.txt";
		//Checker.checkNoResultTopic(NTCIR_EVAL_TASK.NTCIR11_SM_CH, smResultDir+smResult_ch);
		
		//2 sm-en-check
		//String smResult_en = "TUTA1-S-E-1A.txt";
		//Checker.checkNoResultTopic(NTCIR_EVAL_TASK.NTCIR11_SM_EN, smResultDir+smResult_en);
		
		//3 dr-ch-check
		//String drResult_ch = "TUTA1-D-C-1B.txt";
		//Checker.checkNoResultTopic(NTCIR_EVAL_TASK.NTCIR11_DR_CH, drResultDir+drResult_ch);
		
		//4 dr-en-check
		String drResult_en = "TUTA1-D-E-1B.txt";
		Checker.checkNoResultTopic(NTCIR_EVAL_TASK.NTCIR11_DR_EN, drResultDir+drResult_en);
	}

}
