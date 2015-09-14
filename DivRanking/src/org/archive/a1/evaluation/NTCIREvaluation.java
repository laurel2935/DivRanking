package org.archive.a1.evaluation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.archive.dataset.ntcir.NTCIRLoader;
import org.archive.dataset.ntcir.NTCIRTopic;
import org.archive.dataset.ntcir.NTCIRLoader.NTCIR_EVAL_TASK;
import org.archive.nicta.kernel.Kernel;
import org.archive.nicta.kernel.LDAKernel;
import org.archive.nicta.kernel.TF;
import org.archive.nicta.kernel.TFIDF;
import org.archive.nicta.ranker.ResultRanker;
import org.archive.nicta.ranker.mmr.MMR;

public class NTCIREvaluation {
	
	public static void check(){
		String sysRun = "E:/CodeBench/DiversifiedRanking/results/ntcir-10/SM/TUTA1-S-C-1A";
		
		NTCIREvaluator.doEval(NTCIRLoader.loadNTCIR10TopicList(NTCIR_EVAL_TASK.NTCIR10_SM_CH), 
				NTCIRLoader.loadSystemRun(sysRun, NTCIR_EVAL_TASK.NTCIR10_SM_CH), 10, NTCIR_EVAL_TASK.NTCIR10_SM_CH);
	}
	
	public static void check_2(NTCIR_EVAL_TASK eval){
		//
		List<NTCIRTopic> topicList = NTCIRLoader.loadNTCIR10TopicList(eval); 
		HashMap<String,String> docs = NTCIRLoader.loadNTCIR10Docs();
		HashMap<String, ArrayList<String>> poolOfBaseline = NTCIRLoader.loadPoolOfNTCIR10DRBaseline();
		//
		List<ResultRanker> resultRankers = new ArrayList<ResultRanker>();
		
		
		Kernel TF_kernel = new TF(docs, true);
		Kernel TFn_kernel = new TF(docs, false);
		Kernel TFIDF_kernel = new TFIDF(docs, true);
		Kernel TFIDFn_kernel = new TFIDF(docs, false);
		Kernel LDA15_kernel   = new LDAKernel(docs, 15, false, false);

		
		resultRankers.add( new MMR( docs
				, 0.5d //lambda: 0d is all weight on query sim
				, TF_kernel // sim 
				, TF_kernel // div 
				));	
		
		resultRankers.add( new MMR( docs
				, 0.5d //lambda: 0d is all weight on query sim
				, TFn_kernel // sim 
				, TFn_kernel // div 
				));	
		
		resultRankers.add( new MMR( docs
				, 0.5d //lambda: 0d is all weight on query sim
				, TFIDF_kernel // sim 
				, TFIDF_kernel // div 
				));
		
		resultRankers.add( new MMR( docs
				, 0.5d //lambda: 0d is all weight on query sim
				, TFIDFn_kernel // sim 
				, TFIDFn_kernel // div 
				));
		
		resultRankers.add( new MMR( docs
				, 0.5d //lambda: 0d is all weight on query sim
				, LDA15_kernel // sim 
				, LDA15_kernel // div 
				));
		//
		//
		NTCIREvaluator.doEval(topicList, docs, poolOfBaseline, resultRankers, 10, eval);
		
	}
	
	//
	public static void main(String []args){
		//1
		//NTCIREvaluation.check();
		//2
		NTCIREvaluation.check_2(NTCIR_EVAL_TASK.NTCIR10_DR_CH);
		
	}

}
