
package org.archive.nicta.evaluation;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.*;

import org.archive.OutputDirectory;
import org.archive.a1.analysis.DivResult;
import org.archive.a1.analysis.ResultAnalyzer;
import org.archive.a1.ranker.fa.DCKUFLRanker;
import org.archive.a1.ranker.fa.DCKUFLRanker.Strategy;
import org.archive.a1.ranker.fa.MDP;
import org.archive.dataset.trec.TRECDivLoader;
import org.archive.dataset.trec.TRECDivLoader.DivVersion;
import org.archive.dataset.trec.query.TRECDivQuery;
import org.archive.dataset.trec.query.TRECQueryAspects;
import org.archive.ml.ufl.DCKUFL.ExemplarType;
import org.archive.nicta.evaluation.evaluator.Evaluator;
import org.archive.nicta.evaluation.evaluator.TRECDivEvaluator;
import org.archive.nicta.evaluation.metricfunction.AllUSLoss;
import org.archive.nicta.evaluation.metricfunction.AllWSLoss;
import org.archive.nicta.evaluation.metricfunction.Metric;
import org.archive.nicta.evaluation.metricfunction.NDEvalLosses;
import org.archive.nicta.kernel.BM25Kernel_A1;
import org.archive.nicta.kernel.KLDivergenceKernel;
import org.archive.nicta.kernel.Kernel;
import org.archive.nicta.kernel.PLSRKernel;
import org.archive.nicta.kernel.PLSRKernelTFIDF;
import org.archive.nicta.kernel.TF;
import org.archive.nicta.kernel.TFIDF_A1;
import org.archive.nicta.kernel.TerrierKernel;
import org.archive.nicta.ranker.BM25BaselineRanker;
import org.archive.nicta.ranker.CommonIndriBaselineRanker;
import org.archive.nicta.ranker.ResultRanker;
import org.archive.nicta.ranker.iaselect.IASelectRanker;
import org.archive.nicta.ranker.mmr.MMR;
import org.archive.nicta.ranker.pm.PM2Ranker;
import org.archive.nicta.ranker.xquad.XQuADRanker;
import org.archive.sigir.explicit.ExpSRDRanker;
import org.archive.sigir.implicit.ImpSRDRanker;
import org.archive.terrier.TrecScorer;
import org.archive.util.io.IOText;
import org.archive.util.tuple.PairComparatorBySecond_Desc;
import org.archive.util.tuple.StrDouble;

////////////////////////////////////////////////////////////////////
// Evaluates Different Diversification Algorithms on ClueWeb Content
////////////////////////////////////////////////////////////////////

public class TRECDivEvaluation {
	
	//BFS, MDP, FL are defined w.r.t. cikm2014
	//ImpSRD, ExpSRD are defined w.r.t. sigir2016
	public static enum RankStrategy{BFS, MDP, FL,	//before
		MMR, DFP, ImpSRD, ExpSRD, XQuAD, PM2, IASelect, CommonIndriBaseline} //sigir2016
	
	//
	private static ArrayList<String> filterDivQuery(List<String> qList, Map<String,TRECDivQuery> divQueryMap, String typeStr){
		ArrayList<String> newQList = new ArrayList<String>();
		
		for(String q: qList){
			if(divQueryMap.get(q)._type.trim().equals(typeStr)){
				newQList.add(q);
			}
		}
		
		return newQList;
	}
	
	private static void trecDivEvaluation(Boolean commonIndri, DivVersion divVersion, RankStrategy rankStrategy){
		//differentiating faceted and ambiguous
		boolean diffFacetedAmbiguous = false;
		boolean acceptFaceted = false;
		String facetedType = "faceted";
		String ambType = "ambiguous";
		String typePrefix = "";	
		
		//cutoff
		int cutoffK = 20;
		//
		List<String> qList = TRECDivLoader.getDivEvalQueryIDList(commonIndri, divVersion);
		HashMap<String,String> trecDivDocs = TRECDivLoader.loadTrecDivDocs(commonIndri, divVersion);		
		Map<String,TRECDivQuery> trecDivQueries = TRECDivLoader.loadTrecDivQueries(commonIndri, divVersion);	
		
		if(diffFacetedAmbiguous){
			if(acceptFaceted){
				qList = filterDivQuery(qList, trecDivQueries, facetedType);
				typePrefix = "Faceted_";
			}else{
				qList = filterDivQuery(qList, trecDivQueries, ambType);
				typePrefix = "Amb_";
			}
		}
		
		Map<String,TRECQueryAspects> trecDivQueryAspects = TRECDivLoader.loadTrecDivQueryAspects(commonIndri, divVersion);
		
		//output
		String output_prefix = OutputDirectory.ROOT+"sigir2016/RunOutput/";
		File outputFile = new File(output_prefix);
		if(!outputFile.exists()){
			outputFile.mkdirs();
		}
		
		String output_filename = null;
		if(DivVersion.Div2009 == divVersion){			
			output_filename = typePrefix+"Div2009"+rankStrategy.toString();			
		}else if(DivVersion.Div2010 == divVersion){			
			output_filename = typePrefix+"Div2010"+rankStrategy.toString();		
		}else if(DivVersion.Div20092010 == divVersion) {			
			output_filename = typePrefix+"Div20092010"+rankStrategy.toString();			
		}else if(DivVersion.Div2011 == divVersion){
			output_filename = typePrefix+"Div2011"+rankStrategy.toString();
		}else if(DivVersion.Div2012 == divVersion){
			output_filename = typePrefix+"Div2012"+rankStrategy.toString();
		}else{
			
			System.out.println("ERROR: unexpected DivVersion!");
			new Exception().printStackTrace();
			System.exit(1);				
		}
		
		output_filename += ("_"+commonIndri.toString());
				
		// Build the Loss functions
		ArrayList<Metric> lossFunctions = new ArrayList<Metric>();
		// loss_functions.add(new USLoss());
		// loss_functions.add(new WSLoss());
		// loss_functions.add(new AvgUSLoss());
		// loss_functions.add(new AvgWSLoss());
		lossFunctions.add(new AllUSLoss());
		lossFunctions.add(new AllWSLoss());
		lossFunctions.add(new NDEvalLosses(TRECDivLoader.getTrecDivQREL(divVersion), divVersion));
		//
		//NDEval10Losses ndEval10Losses = new NDEval10Losses(TRECDivLoader.getTrecDivQREL(divVersion));		
				
		if(rankStrategy == RankStrategy.BFS){
			/*****************
			 * Best First Strategy
			 * ***************/
			//common
			ArrayList<ResultRanker> rankerList = new ArrayList<ResultRanker>();	
			// Build a new result list selectors... all use the greedy MMR approach,
			// each simply selects a different similarity metric				
			// Instantiate all the kernels that we will use with the algorithms below
			
			//////////////////////
			//TF-Kernel
			//////////////////////
			
			/*
			//part-1
			Kernel TF_kernel    = new TF(trecDivDocs, 
					true //query-relevant diversity
					);
			Kernel TFn_kernel    = new TF(trecDivDocs, 
					false //query-relevant diversity
					);
			//part-2
			rankerList.add( new MMR( trecDivDocs, 
					0.5d //lambda: 0d is all weight on query sim
					, TF_kernel // sim 
					, TF_kernel // div 
					));
			rankerList.add( new MMR( trecDivDocs, 
					0.5d //lambda: 0d is all weight on query sim
					, TFn_kernel // sim
					, TFn_kernel // div
					));
			*/
			
			////////////////////////////
			//BM25 baseline
			////////////////////////////
			/*
			String nameFix = "_BM25Baseline";
			rankerList.add(new BM25BaselineRanker(trecDivDocs));	
			*/
			
			////////////////////////////
			//BM25_kernel
			////////////////////////////
			
			/*
			//for doc-doc similarity			
			TFIDF_A1 tfidf_A1Kernel = new TFIDF_A1(trecDivDocs, false);
			//for query-doc similarity
			double k1, k3, b;
			k1=1.2d; k3=0.5d; b=0.5d;   // achieves the best
			//k1=0.5d; k3=0.5d; b=0.5d; //better than the group of b=1000d;
			//k1=1.2d; k3=0.5d; b=1000d;
			BM25Kernel_A1 bm25_A1_Kernel = new BM25Kernel_A1(trecDivDocs, k1, k3, b);	
			
			String nameFix = null;
			boolean singleLambda = true;
			double weightedAvgLambda = Double.NaN;
			
			if(singleLambda){
				//single Lambda evaluation
				nameFix = "_BM25Kernel_A1+TFIDF_A1_SingleLambda";
				
				if(divVersion == DivVersion.Div2009){
					//using description
					//weightedAvgLambda =  0.542d;
					
					//no description derived from WT-2010
					weightedAvgLambda = 0.1813d;
				}else if(divVersion == DivVersion.Div2010){
					//using description
					//weightedAvgLambda =  0.3917d;
					
					//no description derived from WT-2009
					weightedAvgLambda = 0.272d;
				}else {
					System.err.println("Unsupported DivVersion!");
					System.exit(1);
				}
				
				rankerList.add(new MMR(trecDivDocs, 
						weightedAvgLambda //lambda: 0d is all weight on query sim
						, bm25_A1_Kernel // sim
						, tfidf_A1Kernel // div
						));				
			}else{
				//per Lambda evaluation
				//for similarity between documents, as bm25_A1_Kernel does not support				
				nameFix = "_BM25Kernel_A1+TFIDF_A1_PerLambda";
				for(int i=1; i<=11; i++){
					rankerList.add(new MMR(trecDivDocs, (i-1)/(10*1.0)
							, bm25_A1_Kernel // sim
							, tfidf_A1Kernel // div
							));
				}				
			}
			*/
			
			////////////////////////////
			//TFIDF_kernel
			////////////////////////////
			
			/*
			//part-1
			Kernel TFIDF_kernel = new TFIDF(trecD0910Docs,
					true //query-relevant diversity
					);
			Kernel TFIDFn_kernel = new TFIDF(trecD0910Docs, 
					false // query-relevant diversity
					);
			//part-2
			rankers.add( new MMR( trecD0910Docs, 
			0.5d //lambda: 0d is all weight on query sim
			, TFIDF_kernel // sim
			, TFIDF_kernel // div
			));
			rankers.add( new MMR( trecD0910Docs, 
					0.5d //lambda: 0d is all weight on query sim
					, TFIDFn_kernel //sim
					, TFIDFn_kernel //div
					));
			*/
			
			//////////////////////////
			//LDA-Kernel
			//////////////////////////	
			
			/*
			//part-1
			Kernel PLSR_TFIDF_kernel = new PLSRKernelTFIDF(trecDivDocs);
			//
			Kernel PLSR10_kernel  = new PLSRKernel(trecDivDocs
					, 10 // NUM TOPICS - suggest 15
					, false // spherical
					);
			Kernel PLSR15_kernel  = new PLSRKernel(trecDivDocs
					, 15 // NUM TOPICS - suggest 15
					, 2.0
					, 0.5
					, false // spherical
					);
			Kernel PLSR15_sph_kernel  = new PLSRKernel(trecDivDocs
					, 15 // NUM TOPICS - suggest 15
					, true // spherical
					);
			Kernel PLSR20_kernel  = new PLSRKernel(trecDivDocs
					, 20 // NUM TOPICS - suggest 15
					, false // spherical
					);
			//part-2		
			rankerList.add( new MMR( trecDivDocs, 
					0.5d //lambda: 0d is all weight on query sim
					, PLSR_TFIDF_kernel //sim
					, PLSR_TFIDF_kernel //div
					));
			rankerList.add( new MMR( trecDivDocs, 
					0.5d //lambda: 0d is all weight on query sim
					, PLSR15_kernel //sim
					, PLSR15_kernel //div
					));
			*/
			/*
			//part-1
			Kernel LDA10_kernel   = new LDAKernel(trecDivDocs, 
					10 // NUM TOPICS - suggest 15
					, false // spherical
					, false // query-relevant diversity
					);
			
			Kernel LDA15_kernel   = new LDAKernel(trecDivDocs
					, 15 // NUM TOPICS - suggest 15
					, false // spherical
					, false // query-relevant diversity
					);

			Kernel LDA15_sph_kernel   = new LDAKernel(trecDivDocs
					, 15 // NUM TOPICS - suggest 15
					, true // spherical
					, false // query-relevant diversity
					);
			Kernel LDA15_qr_kernel   = new LDAKernel(trecDivDocs
					, 15 // NUM TOPICS - suggest 15
					, false // spherical
					, true // query-relevant diversity
					);
			Kernel LDA15_qr_sph_kernel   = new LDAKernel(trecDivDocs
					, 15 // NUM TOPICS - suggest 15
					, true // spherical
					, true // query-relevant diversity
					);				
			//part-2	
			rankerList.add( new MMR( trecDivDocs, 
					0.5d //lambda: 0d is all weight on query sim
					, LDA10_kernel //sim
					, LDA10_kernel //div
					));
			rankerList.add( new MMR( trecDivDocs, 
					0.5d //lambda: 0d is all weight on query sim
					, LDA15_kernel //sim
					, LDA15_kernel //div
					));
			rankerList.add( new MMR( trecDivDocs, 
					0.5d //lambda: 0d is all weight on query sim
					, LDA15_sph_kernel //sim
					, LDA15_sph_kernel //div
					));
			rankerList.add( new MMR( trecDivDocs, 
					0.5d //lambda: 0d is all weight on query sim
					, LDA15_qr_kernel //sim
					, LDA15_qr_kernel //div
					));
			rankerList.add( new MMR( trecDivDocs, 
					0.5d //lambda: 0d is all weight on query sim
					, LDA15_qr_sph_kernel //sim
					, LDA15_qr_sph_kernel //div
					));		
			*/
			
			//////CIKM2014
			///*
			/*
			
//			Kernel LDA15_kernel   = new LDAKernel(trecDivDocs
//					, 15 // NUM TOPICS - suggest 15
//					, false // spherical
//					, true // query-relevant diversity for reference paper
//					);
			
			//Kernel plsrKernel = new PLSRKernel(trecDivDocs, 15, false);
			
			Kernel plsrKernel = new PLSRKernel(trecDivDocs, 15, 2.0, 0.5, false);			
			
			String nameFix = "_PLSR";			
			
			//single lambda evaluation
			rankerList.add( new MMR( trecDivDocs, 
					0.5d //lambda: 0d is all weight on query sim
					, plsrKernel //sim
					, plsrKernel //div
					));	
			*/
			//common: Add all MMR test variants (vary lambda and kernels)
			
			
			
			
			
			//for testing kl w.r.t. MMR
			//(1) balance relevance and diversity
			double lambda = 0.5;			
			String nameFix = "";						
			//(2)
			//kernel
			double k1, k3, b;
			k1=1.2d; k3=0.5d; b=0.5d;   // achieves the best
			//k1=0.5d; k3=0.5d; b=0.5d; //better than the group of b=1000d;
			//k1=1.2d; k3=0.5d; b=1000d			
			BM25Kernel_A1 releKernel = new BM25Kernel_A1(trecDivDocs, k1, k3, b);
			
			KLDivergenceKernel klDivKernel = new KLDivergenceKernel(trecDivDocs);
			
			output_filename += ("_"+releKernel.getString());
			output_filename += ("_"+klDivKernel.getString());
			
			MMR mmrRanker = new MMR(trecDivDocs, lambda, releKernel, klDivKernel);
			
			rankerList.add(mmrRanker);
			
			// Evaluate results of different query processing algorithms
			Evaluator trecDivEvaluator = new TRECDivEvaluator(trecDivQueries, output_prefix, output_filename+nameFix);
			try {				
				//
				trecDivEvaluator.doEval(qList, trecDivDocs, trecDivQueryAspects, lossFunctions, rankerList, cutoffK);
			} catch (Exception e) {
				// TODO: handle exception
				e.printStackTrace();
			}
			
		}else if(rankStrategy == RankStrategy.MDP){	
			/////////////////////////
			//MDP run - style-1
			////////////////////////
			
			////single lambda evaluation
			/*
			////note the number of topics for LDA training
			//SBKernel _sbKernel = new SBKernel(trecDivDocs, 10);			
			TFIDF_A1 tfidf_A1Kernel = new TFIDF_A1(trecDivDocs, false);
			//
			int itrThreshold = 10000;
			
			String nameFix = "_SingleLambda";
			
			MDP mdp = new MDP(trecDivDocs, 0.5d, itrThreshold, tfidf_A1Kernel, null, trecDivQueries);
			
			Vector<fVersion> mdpRuns = new Vector<MDP.fVersion>();			
			mdpRuns.add(fVersion._dfa);
			//mdpRuns.add(fVersion._dfa_scaled);
			//mdpRuns.add(fVersion._md);
			//mdpRuns.add(fVersion._md_scaled);
			//mdpRuns.add(fVersion._pdfa);
			//mdpRuns.add(fVersion._pdfa_scaled);
			//mdpRuns.add(fVersion._pdfa_scaled_exp);
			//mdpRuns.add(fVersion._pdfa_scaled_exp_head);	
			try {				
				mdp.doEval(TRECDivLoader.getDivEvalQueries(divVersion), trecDivDocs, trecDivQueryAspects,
					lossFunctions, cutoffK, output_prefix, output_filename+nameFix, mdpRuns.toArray(new fVersion[0]));				
			} catch (Exception e) {
				// TODO: handle exception
				e.printStackTrace();
			}
			*/			
						
			/////////////////////////
			//MDP run - style-2
			////////////////////////
			
			///*
			int itrThreshold = 10000;
			//kernel
			double k1, k3, b;
			k1=1.2d; k3=0.5d; b=0.5d;   // achieves the best
			//k1=0.5d; k3=0.5d; b=0.5d; //better than the group of b=1000d;
			//k1=1.2d; k3=0.5d; b=1000d;
			//--1
			BM25Kernel_A1 releKernel = new BM25Kernel_A1(trecDivDocs, k1, k3, b);
			
			boolean isKLKernel = true;	
			ArrayList<ResultRanker> rankerList = new ArrayList<ResultRanker>();
			
			String nameFix = "";
			double weightedAvgLambda = 0.5;
			
			if(!isKLKernel){				
				//--2
				TFIDF_A1 disKernel = new TFIDF_A1(trecDivDocs, false);				
				boolean singleLambda = true;
				
				
				if(singleLambda){		
					//////single lambada evaluation
					nameFix = "_MDP_SingleLambda";
					//
					//double wt2009WeightedAvgLambda = 0.48d;
					//double wt2010WeightedAvgLambda = 0.5646d;
					if(divVersion == DivVersion.Div2009){
						//using description
						//weightedAvgLambda =  0.48d;
						
						//no description derived from wt-2010
						weightedAvgLambda = 0.55d;
						
					}else if(divVersion == DivVersion.Div2010){
						//using description
						//weightedAvgLambda =  0.5646d;
						
						//no description derived from WT-2009
						weightedAvgLambda = 0.43d;
					}else {
						System.err.println("Unsupported DivVersion!");
						System.exit(1);
					}
					
					MDP mdp = new MDP(trecDivDocs, weightedAvgLambda, itrThreshold, releKernel, disKernel, null, trecDivQueries);
					rankerList.add(mdp);
					
				}else{
					//////per Lambda evaluation
					//for similarity between documents, as bm25_A1_Kernel does not support
					nameFix = "_MDP_PerLambda";
					
					for(int i=1; i<=11; i++){
						//(i-1)/(10*1.0)
						rankerList.add(new MDP(trecDivDocs, (i-1)/(10*1.0), itrThreshold, releKernel, disKernel, null, trecDivQueries));
					}
				}
				
			}else{
										
				KLDivergenceKernel klDivKernel = new KLDivergenceKernel(trecDivDocs);
				
				output_filename += ("_"+releKernel.getString());
				output_filename += ("_"+klDivKernel.getString());
				
				MDP dfpRanker = new MDP(trecDivDocs, weightedAvgLambda, itrThreshold, releKernel, klDivKernel, null, trecDivQueries);
				
				rankerList.add(dfpRanker);				
			}			
			
			// Evaluate results of different query processing algorithms			
			Evaluator trecDivEvaluator = new TRECDivEvaluator(trecDivQueries, output_prefix, output_filename+nameFix);
			try {
				trecDivEvaluator.doEval(qList, trecDivDocs, trecDivQueryAspects, lossFunctions, rankerList, cutoffK);
			} catch (Exception e) {
				// TODO: handle exception
				e.printStackTrace();
			}
			//*/
			
		}else if(rankStrategy == RankStrategy.FL){		
			
			//combination			
			ExemplarType exemplarType = ExemplarType.Y;
			Strategy flStrategy = Strategy.Belief;
			
			String nameFix = "_"+exemplarType.toString();
			nameFix += ("_"+flStrategy.toString());
						
			double k1, k3, b;
			k1=1.2d; k3=0.5d; b=0.5d;   // achieves the best
			//k1=0.5d; k3=0.5d; b=0.5d; //better than the group of b=1000d;
			//k1=1.2d; k3=0.5d; b=1000d;
			BM25Kernel_A1 bm25_A1_Kernel = new BM25Kernel_A1(trecDivDocs, k1, k3, b);
			
			//TFIDF_A1 tfidf_A1Kernel = new TFIDF_A1(trecDivDocs, false);
			
			//
			ArrayList<ResultRanker> rankerList = new ArrayList<ResultRanker>();
			
			//1
			double lambda_1 = 0.5;
			int iterationTimes_1 = 5000;
			int noChangeIterSpan_1 = 10; 
			//DCKUFLRanker dckuflRanker = new DCKUFLRanker(trecDivDocs, bm25_A1_Kernel, lambda_1, iterationTimes_1, noChangeIterSpan_1);
			DCKUFLRanker dckuflRanker = new DCKUFLRanker(trecDivDocs, bm25_A1_Kernel, lambda_1, iterationTimes_1, noChangeIterSpan_1, exemplarType, flStrategy);
			
			//2
			/*
			double lambda_2 = 0.5;
			int iterationTimes_2 = 10000;
			int noChangeIterSpan_2 = 10; 
			double SimDivLambda = 0.5;
			K_UFLRanker kuflRanker = new K_UFLRanker(trecDivDocs, tfidf_A1Kernel, lambda_2, iterationTimes_2, noChangeIterSpan_2, SimDivLambda);
			*/
			
			rankerList.add(dckuflRanker);
			//rankers.add(kuflRanker);
			
			// Evaluate results of different query processing algorithms
			Evaluator trecDivEvaluator = new TRECDivEvaluator(trecDivQueries, output_prefix, output_filename+nameFix);
			try {
				trecDivEvaluator.doEval(qList, trecDivDocs, trecDivQueryAspects, lossFunctions, rankerList, cutoffK);
			} catch (Exception e) {
				// TODO: handle exception
				e.printStackTrace();
			}			
			
		}else if(RankStrategy.ImpSRD == rankStrategy){
			
			singleEval_ImpSRD(output_prefix, output_filename, 
					qList, trecDivQueries, trecDivDocs, trecDivQueryAspects, lossFunctions, cutoffK);
	
		}else if(RankStrategy.ExpSRD == rankStrategy){
			
			singleEval_ExpSRD(ReleProVersion.BM25, output_prefix, output_filename, 
					qList, trecDivQueries, trecDivDocs, trecDivQueryAspects, lossFunctions, cutoffK);
			
		}else if(RankStrategy.IASelect == rankStrategy){
			
			singleEval_IASelect(ReleProVersion.COS, output_prefix, output_filename, 
					qList, trecDivQueries, trecDivDocs, trecDivQueryAspects, lossFunctions, cutoffK);
			
		}else if(RankStrategy.PM2 == rankStrategy){
			
			singleEval_PM2(output_prefix, output_filename, 
					qList, trecDivQueries, trecDivDocs, trecDivQueryAspects, lossFunctions, cutoffK);
			
		}else if(RankStrategy.XQuAD == rankStrategy){
			singleEval_XQuAD(output_prefix, output_filename, 
					qList, trecDivQueries, trecDivDocs, trecDivQueryAspects, lossFunctions, cutoffK);
			
		}else if(RankStrategy.CommonIndriBaseline == rankStrategy){
			
			String nameFix = "";
			
			ArrayList<ResultRanker> rankerList = new ArrayList<ResultRanker>();
				
			CommonIndriBaselineRanker comIndriBaselineRanker = new CommonIndriBaselineRanker(divVersion, trecDivDocs);
			
			rankerList.add(comIndriBaselineRanker);
			
			// Evaluate results of different query processing algorithms
			Evaluator trecDivEvaluator = new TRECDivEvaluator(trecDivQueries, output_prefix, output_filename+nameFix);
			
			try {
				trecDivEvaluator.doEval(qList, trecDivDocs, trecDivQueryAspects, lossFunctions, rankerList, cutoffK);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
		}else if(RankStrategy.MMR == rankStrategy){
			singleEval_MMR(output_prefix, output_filename, 
					qList, trecDivQueries, trecDivDocs, trecDivQueryAspects, lossFunctions, cutoffK);
		}else if(RankStrategy.DFP == rankStrategy){
			singleEval_DFP(output_prefix, output_filename, 
					qList, trecDivQueries, trecDivDocs, trecDivQueryAspects, lossFunctions, cutoffK);
		}
	}
	//
	private static PrintStream printer = null; 
	public static void openPrinter(){
		try{
			printer = new PrintStream(new FileOutputStream(new File(OutputDirectory.ROOT+"DivEvaluation/"+"log.txt")));
			System.setOut(printer);			
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	public static void closePrinter(){
		printer.flush();
		printer.close();
	}
	/**
	 * single run
	 * **/
	public static void singleEval_DFP(String output_prefix, String output_filename, 
			List<String> qList, Map<String,TRECDivQuery> trecDivQueries,
			HashMap<String,String> trecDivDocs, Map<String,TRECQueryAspects> trecDivQueryAspects,
			List<Metric> lossFunctions, int cutoffK){
		//(1) balance relevance and diversity
		double optLambda = 0.5;		
		int itrThreshold = 10000;
		String nameFix = "";						
		//(2)
		//kernel
		double k1, k3, b;
		k1=1.2d; k3=0.5d; b=0.5d;   // achieves the best
		//k1=0.5d; k3=0.5d; b=0.5d; //better than the group of b=1000d;
		//k1=1.2d; k3=0.5d; b=1000d;
		
		BM25Kernel_A1 releKernel = new BM25Kernel_A1(trecDivDocs, k1, k3, b);
		TFIDF_A1 disKernel = new TFIDF_A1(trecDivDocs, false);
		
		output_filename += ("_"+releKernel.getString());
		output_filename += ("_"+disKernel.getString());
		
		MDP dfpRanker = new MDP(trecDivDocs, optLambda, itrThreshold, releKernel, disKernel, null, trecDivQueries);
		
		ArrayList<ResultRanker> rankerList = new ArrayList<ResultRanker>();
		rankerList.add(dfpRanker);
		
		// Evaluate results of different query processing algorithms
		Evaluator trecDivEvaluator = new TRECDivEvaluator(trecDivQueries, output_prefix, output_filename+nameFix);
		
		try {
			trecDivEvaluator.doEval(qList, trecDivDocs, trecDivQueryAspects, lossFunctions, rankerList, cutoffK);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	//
	public static void singleEval_MMR(String output_prefix, String output_filename, 
			List<String> qList, Map<String,TRECDivQuery> trecDivQueries,
			HashMap<String,String> trecDivDocs, Map<String,TRECQueryAspects> trecDivQueryAspects,
			List<Metric> lossFunctions, int cutoffK){
		//
		//(1) balance relevance and diversity
		double optLambda = 0.5;			
		String nameFix = "";						
		//(2)
		//kernel
		double k1, k3, b;
		k1=1.2d; k3=0.5d; b=0.5d;   // achieves the best
		//k1=0.5d; k3=0.5d; b=0.5d; //better than the group of b=1000d;
		//k1=1.2d; k3=0.5d; b=1000d			
		BM25Kernel_A1 releKernel = new BM25Kernel_A1(trecDivDocs, k1, k3, b);
		
		TFIDF_A1 disKernel = new TFIDF_A1(trecDivDocs, false);
		
		output_filename += ("_"+releKernel.getString());
		output_filename += ("_"+disKernel.getString());		
		
		
		MMR mmrRanker = new MMR(trecDivDocs, optLambda, releKernel, disKernel);
		
		ArrayList<ResultRanker> rankerList = new ArrayList<ResultRanker>();
		rankerList.add(mmrRanker);
		
		// Evaluate results of different query processing algorithms
		Evaluator trecDivEvaluator = new TRECDivEvaluator(trecDivQueries, output_prefix, output_filename+nameFix);
		
		try {
			trecDivEvaluator.doEval(qList, trecDivDocs, trecDivQueryAspects, lossFunctions, rankerList, cutoffK);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	//
	public static void singleEval_ImpSRD(String output_prefix, String output_filename, 
			List<String> qList, Map<String,TRECDivQuery> trecDivQueries,
			HashMap<String,String> trecDivDocs, Map<String,TRECQueryAspects> trecDivQueryAspects,
			List<Metric> lossFunctions, int cutoffK){
		//
		//(1) balance relevance and diversity
		double SimDivLambda = 0.5;
		
		//(2) combination			
		ExemplarType exemplarType = ExemplarType.Y;
		Strategy flStrategy = Strategy.Belief;			
		String nameFix = "_"+exemplarType.toString();
		nameFix += ("_"+flStrategy.toString());
				
		//(3)
		double k1, k3, b;
		k1=1.2d; k3=0.5d; b=0.5d;   // achieves the best
		//k1=0.5d; k3=0.5d; b=0.5d; //better than the group of b=1000d;
		//k1=1.2d; k3=0.5d; b=1000d;
		
		//kernel-1			
		BM25Kernel_A1 releKernel = new BM25Kernel_A1(trecDivDocs, k1, k3, b);			
		//kernel-2
		TFIDF_A1 divKernel = new TFIDF_A1(trecDivDocs, false);
		
		output_filename += ("_"+releKernel.getString());
		output_filename += ("_"+divKernel.getString());
		
		//1
		double lambda_1 = 0.5;
		int iterationTimes_1 = 5000;
		int noChangeIterSpan_1 = 10;
		
		//kernel-1
		ImpSRDRanker impSRDRanker = new ImpSRDRanker(trecDivDocs, releKernel, divKernel,
				lambda_1, iterationTimes_1, noChangeIterSpan_1, SimDivLambda, exemplarType, flStrategy);

		ArrayList<ResultRanker> rankerList = new ArrayList<ResultRanker>();
		rankerList.add(impSRDRanker);
		
		// Evaluate results of different query processing algorithms
		Evaluator trecDivEvaluator = new TRECDivEvaluator(trecDivQueries, output_prefix, output_filename+nameFix);
		
		try {
			trecDivEvaluator.doEval(qList, trecDivDocs, trecDivQueryAspects, lossFunctions, rankerList, cutoffK);
		} catch (Exception e) {
			e.printStackTrace();
		}		
	}
	//
	public static void singleEval_ImpSRD_dl(String output_prefix, String output_filename, 
			List<String> qList, Map<String,TRECDivQuery> trecDivQueries,
			HashMap<String,String> trecDivDocs, Map<String,TRECQueryAspects> trecDivQueryAspects,
			List<Metric> lossFunctions, int cutoffK){
		//
		//(1) balance relevance and diversity
		double SimDivLambda = 0.5;
		
		//(2) combination			
		ExemplarType exemplarType = ExemplarType.Y;
		Strategy flStrategy = Strategy.Belief;			
		String nameFix = "_"+exemplarType.toString();
		nameFix += ("_"+flStrategy.toString());
		
		double lambda_1 = 0.5;
		int iterationTimes_1 = 5000;
		int noChangeIterSpan_1 = 10;
		
		boolean isKLKernel = true;			
		ArrayList<ResultRanker> rankerList = new ArrayList<ResultRanker>();
		
		if(!isKLKernel){
			//(3)
			double k1, k3, b;
			k1=1.2d; k3=0.5d; b=0.5d;   // achieves the best
			//k1=0.5d; k3=0.5d; b=0.5d; //better than the group of b=1000d;
			//k1=1.2d; k3=0.5d; b=1000d;
			
			//kernel-1			
			BM25Kernel_A1 releKernel = new BM25Kernel_A1(trecDivDocs, k1, k3, b);
			//TFIDF_A1 releKernel = new TFIDF_A1(trecDivDocs, false);
			
			//kernel-2
			//TFIDF_A1 divKernel = new TFIDF_A1(trecDivDocs, false);
			BM25Kernel_A1 divKernel = new BM25Kernel_A1(trecDivDocs, k1, k3, b);
			
			output_filename += ("_"+Double.toString(SimDivLambda));
			output_filename += ("_"+releKernel.getString());
			output_filename += ("_"+divKernel.getString());
			
			//kernel-1
			ImpSRDRanker impSRDRanker = new ImpSRDRanker(trecDivDocs, releKernel, divKernel,
					lambda_1, iterationTimes_1, noChangeIterSpan_1, SimDivLambda, exemplarType, flStrategy);
			
			rankerList.add(impSRDRanker);
			
		}else{
			
			double k1, k3, b;
			k1=1.2d; k3=0.5d; b=0.5d;   // achieves the best
			//k1=0.5d; k3=0.5d; b=0.5d; //better than the group of b=1000d;
			//k1=1.2d; k3=0.5d; b=1000d;				
			//kernel-1			
			BM25Kernel_A1 releKernel = new BM25Kernel_A1(trecDivDocs, k1, k3, b);
			
			KLDivergenceKernel klDivKernel = new KLDivergenceKernel(trecDivDocs);
			
			output_filename += ("_"+Double.toString(SimDivLambda));
			output_filename += ("_"+releKernel.getString());
			output_filename += ("_"+klDivKernel.getString());
			
			//kernel-2
			ImpSRDRanker impSRDRanker = new ImpSRDRanker(trecDivDocs, releKernel, klDivKernel,
					lambda_1, iterationTimes_1, noChangeIterSpan_1, SimDivLambda, exemplarType, flStrategy);
			
			rankerList.add(impSRDRanker);
			
		}		
		
		// Evaluate results of different query processing algorithms
		Evaluator trecDivEvaluator = new TRECDivEvaluator(trecDivQueries, output_prefix, output_filename+nameFix);
		
		try {
			trecDivEvaluator.doEval(qList, trecDivDocs, trecDivQueryAspects, lossFunctions, rankerList, cutoffK);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	//
	public static void singleEval_ExpSRD(ReleProVersion releProVersion, String output_prefix, String output_filename, 
			List<String> qList, Map<String,TRECDivQuery> trecDivQueries,
			HashMap<String,String> trecDivDocs, Map<String,TRECQueryAspects> trecDivQueryAspects,
			List<Metric> lossFunctions, int cutoffK){
		//
		output_prefix += ("E-MP_"+releProVersion.toString());
		File dirFile = new File(output_prefix);
		if(!dirFile.exists()){
			dirFile.mkdirs();
		}
		output_prefix += "/";
		
		////combination			
		ExemplarType exemplarType = ExemplarType.Y;
		Strategy flStrategy = Strategy.Belief;
		
		String nameFix = "_"+exemplarType.toString();
		nameFix += ("_"+flStrategy.toString());
		
		////kernel			
		double k1, k3, b;
		k1=1.2d; k3=0.5d; b=0.5d;   // achieves the best
		//k1=0.5d; k3=0.5d; b=0.5d; //better than the group of b=1000d;
		//k1=1.2d; k3=0.5d; b=1000d;
		
		Kernel usedKernel = null;
		if(releProVersion == ReleProVersion.BM25){
			usedKernel = new BM25Kernel_A1(trecDivDocs, k1, k3, b);
		}else{
			usedKernel = new TFIDF_A1(trecDivDocs, false);
		}			
		
		//			
		output_filename += ("_"+usedKernel.getString());			
		
		//1
		double lambda_1 = 0.5;
		int iterationTimes_1 = 5000;
		int noChangeIterSpan_1 = 10; 
		
		ExpSRDRanker expSRDRanker = new ExpSRDRanker(trecDivDocs, usedKernel, usedKernel,
				lambda_1, iterationTimes_1, noChangeIterSpan_1, exemplarType, flStrategy);
		
		ArrayList<ResultRanker> rankerList = new ArrayList<ResultRanker>();
		rankerList.add(expSRDRanker);
		
		// Evaluate results of different query processing algorithms
		Evaluator trecDivEvaluator = new TRECDivEvaluator(trecDivQueries, output_prefix, output_filename+nameFix);
		
		try {
			trecDivEvaluator.doEval(qList, trecDivDocs, trecDivQueryAspects, lossFunctions, rankerList, cutoffK);
		} catch (Exception e) {
			e.printStackTrace();
		}			
	}
	//
	public static void singleEval_PM2(String output_prefix, String output_filename, 
			List<String> qList, Map<String,TRECDivQuery> trecDivQueries,
			HashMap<String,String> trecDivDocs, Map<String,TRECQueryAspects> trecDivQueryAspects,
			List<Metric> lossFunctions, int cutoffK){
		//
		//(1) balance relevance and diversity
		double optLambda = 0.5;			
		String nameFix = "";						
		//(2)
		
		//1. kernel
		//TFIDF_A1 usedKernel = new TFIDF_A1(trecDivDocs, false);
		
		//2.terrier kernel
		TrecScorer trecScorer = new TrecScorer();						
		TerrierKernel usedKernel = new TerrierKernel(trecDivDocs, trecScorer);	
		
		output_filename += ("_"+usedKernel.getString());
		
		PM2Ranker pm2Ranker = new PM2Ranker(trecDivDocs, optLambda, usedKernel);
		ArrayList<ResultRanker> rankerList = new ArrayList<ResultRanker>();
		rankerList.add(pm2Ranker);
		
		// Evaluate results of different query processing algorithms
		Evaluator trecDivEvaluator = new TRECDivEvaluator(trecDivQueries, output_prefix, output_filename+nameFix);
		
		try {
			trecDivEvaluator.doEval(qList, trecDivDocs, trecDivQueryAspects, lossFunctions, rankerList, cutoffK);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	//
	public static void singleEval_XQuAD(String output_prefix, String output_filename, 
			List<String> qList, Map<String,TRECDivQuery> trecDivQueries,
			HashMap<String,String> trecDivDocs, Map<String,TRECQueryAspects> trecDivQueryAspects,
			List<Metric> lossFunctions, int cutoffK){
		//
		//(1) balance relevance and diversity
		double optLambda = 0.5;			
		String nameFix = "";						
		//(2)
		//kernel
		//TFIDF_A1 usedKernel = new TFIDF_A1(trecDivDocs, false);
		//2.terrier kernel
		//common
		TrecScorer trecScorer = new TrecScorer();						
		TerrierKernel usedKernel = new TerrierKernel(trecDivDocs, trecScorer);		
		
		output_filename += ("_"+usedKernel.getString());
		
		XQuADRanker xQuADRanker = new XQuADRanker(trecDivDocs, optLambda, usedKernel);
		ArrayList<ResultRanker> rankerList = new ArrayList<ResultRanker>();
		rankerList.add(xQuADRanker);
		
		// Evaluate results of different query processing algorithms
		Evaluator trecDivEvaluator = new TRECDivEvaluator(trecDivQueries, output_prefix, output_filename+nameFix);
		
		try {
			trecDivEvaluator.doEval(qList, trecDivDocs, trecDivQueryAspects, lossFunctions, rankerList, cutoffK);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	//
	public static void singleEval_IASelect(ReleProVersion releProVersion, String output_prefix, String output_filename, 
			List<String> qList, Map<String,TRECDivQuery> trecDivQueries,
			HashMap<String,String> trecDivDocs, Map<String,TRECQueryAspects> trecDivQueryAspects,
			List<Metric> lossFunctions, int cutoffK){
		////dir
		output_prefix += ("IASelect_"+releProVersion.toString());
		File dirFile = new File(output_prefix);
		if(!dirFile.exists()){
			dirFile.mkdirs();
		}
		output_prefix += "/";
		
		String nameFix = "";
		
		Kernel usedKernel = null;
		if(releProVersion == ReleProVersion.COS){
			usedKernel = new TFIDF_A1(trecDivDocs, false);
		}else{
			TrecScorer trecScorer = new TrecScorer();						
			usedKernel = new TerrierKernel(trecDivDocs, trecScorer);
		}
		
		output_filename += ("_"+usedKernel.getString());
		
		//kernel
		ArrayList<ResultRanker> rankerList = new ArrayList<ResultRanker>();
		
		//ranker
		IASelectRanker iaSelectRanker = new IASelectRanker(trecDivDocs, usedKernel);
		////
		rankerList.add(iaSelectRanker);
		
		// Evaluate results of different query processing algorithms
		Evaluator trecDivEvaluator = new TRECDivEvaluator(trecDivQueries, output_prefix, output_filename+nameFix);
		
		try {
			trecDivEvaluator.doEval(qList, trecDivDocs, trecDivQueryAspects, lossFunctions, rankerList, cutoffK);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	//
	/**
	 * cross-validation
	 * **/
	private static void crossTrecDivEvaluation(Boolean commonIndri, ArrayList<DivVersion> versionList, RankStrategy rankStrategy){
		//differentiating faceted and ambiguous
		boolean diffFacetedAmbiguous = false;
		boolean acceptFaceted = false;
		String facetedType = "faceted";
		String ambType = "ambiguous";		
		
		//cutoff
		int cutoffK = 20;
		
		////
		List<List<String>> qListArray = new ArrayList<List<String>>();
		List<HashMap<String,String>> trecDivDocsArray = new ArrayList<HashMap<String,String>>();
		List<Map<String,TRECDivQuery>> trecDivQueriesArray = new ArrayList<>();
		List<Map<String,TRECQueryAspects>> trecDivQueryAspectsArray = new ArrayList<>();
		List<String> outputNameArray = new ArrayList<>();
		List<String> typePrefixArray = new ArrayList<>();
		List<ArrayList<Metric>> lostFunctionsArray = new ArrayList<>();
		
		//output
		//sigir
		String output_prefix = OutputDirectory.ROOT+"sigir2016/RunOutput/Cross/"
									+Integer.toString(versionList.size())+"-fold/";
		//tkde
		
		//
		for(int i=0; i<versionList.size(); i++){
			DivVersion divVersion = versionList.get(i);
			//--
			String typePrefix = "";
			List<String> qList = TRECDivLoader.getDivEvalQueryIDList(commonIndri, divVersion);
			
			HashMap<String,String> trecDivDocs = TRECDivLoader.loadTrecDivDocs(commonIndri, divVersion);
			trecDivDocsArray.add(trecDivDocs);
			
			Map<String,TRECDivQuery> trecDivQueries = TRECDivLoader.loadTrecDivQueries(commonIndri, divVersion);
			trecDivQueriesArray.add(trecDivQueries);
			
			if(diffFacetedAmbiguous){
				if(acceptFaceted){
					qList = filterDivQuery(qList, trecDivQueries, facetedType);
					typePrefix = "Faceted_";
				}else{
					qList = filterDivQuery(qList, trecDivQueries, ambType);
					typePrefix = "Amb_";
				}
			}
			qListArray.add(qList);
			typePrefixArray.add(typePrefix);
			
			Map<String,TRECQueryAspects> trecDivQueryAspects = TRECDivLoader.loadTrecDivQueryAspects(commonIndri, divVersion);
			trecDivQueryAspectsArray.add(trecDivQueryAspects);			
			
			//check output directory
			File outputFile = new File(output_prefix);
			if(!outputFile.exists()){
				outputFile.mkdirs();
			}
			
			String output_filename = null;
			if(DivVersion.Div2009 == divVersion){			
				output_filename = typePrefix+"Div2009"+rankStrategy.toString();			
			}else if(DivVersion.Div2010 == divVersion){			
				output_filename = typePrefix+"Div2010"+rankStrategy.toString();		
			}else if(DivVersion.Div20092010 == divVersion) {			
				output_filename = typePrefix+"Div20092010"+rankStrategy.toString();			
			}else if(DivVersion.Div2011 == divVersion){
				output_filename = typePrefix+"Div2011"+rankStrategy.toString();
			}else if(DivVersion.Div2012 == divVersion){
				output_filename = typePrefix+"Div2012"+rankStrategy.toString();
			}else{				
				System.out.println("ERROR: unexpected DivVersion!");
				new Exception().printStackTrace();
				System.exit(1);				
			}
			output_filename += ("_"+commonIndri.toString());
			outputNameArray.add(output_filename);
					
			////Build the Loss functions
			ArrayList<Metric> lossFunctions = new ArrayList<Metric>();
			// loss_functions.add(new USLoss());
			// loss_functions.add(new WSLoss());
			// loss_functions.add(new AvgUSLoss());
			// loss_functions.add(new AvgWSLoss());
			lossFunctions.add(new AllUSLoss());
			lossFunctions.add(new AllWSLoss());
			lossFunctions.add(new NDEvalLosses(TRECDivLoader.getTrecDivQREL(divVersion), divVersion));
			//
			lostFunctionsArray.add(lossFunctions);
			//--
		}	
		
		//-- setting ranking strategy --//
			
				
		if(rankStrategy == RankStrategy.BFS){
			
			crossEval_MMR(versionList, cutoffK, output_prefix,
					qListArray, trecDivDocsArray,
					trecDivQueriesArray, trecDivQueryAspectsArray,
					outputNameArray, typePrefixArray, lostFunctionsArray);
			
		}else if(rankStrategy == RankStrategy.MDP){	
			
			crossEval_DFP(versionList, cutoffK, output_prefix,
					qListArray, trecDivDocsArray,
					trecDivQueriesArray, trecDivQueryAspectsArray,
					outputNameArray, typePrefixArray, lostFunctionsArray);
			
		}else if(RankStrategy.ImpSRD == rankStrategy){
			
			crossEval_ImpSRD(versionList, cutoffK, output_prefix,
					qListArray, trecDivDocsArray,
					trecDivQueriesArray, trecDivQueryAspectsArray,
					outputNameArray, typePrefixArray, lostFunctionsArray);
						
		}else if(RankStrategy.ExpSRD == rankStrategy){
			
			//cross evalidation is not needed, since there is no parameter to be tuned.
			
		}else if(RankStrategy.XQuAD == rankStrategy){
			
			crossEval_XQuAD(versionList, cutoffK, output_prefix,
					qListArray, trecDivDocsArray,
					trecDivQueriesArray, trecDivQueryAspectsArray,
					outputNameArray, typePrefixArray, lostFunctionsArray);
			
		}else if(RankStrategy.PM2 == rankStrategy){
			
			crossEval_PM2(versionList, cutoffK, output_prefix,
					qListArray, trecDivDocsArray,
					trecDivQueriesArray, trecDivQueryAspectsArray,
					outputNameArray, typePrefixArray, lostFunctionsArray);
			
		}
	}
	// suitable for 2-fold, if for 3 or more, there exists duplicate calculation //
	////
	public static void crossEval_ImpSRD(ArrayList<DivVersion> versionList, int cutoffK, String output_prefix,
			List<List<String>> qListArray, List<HashMap<String,String>> trecDivDocsArray,
			List<Map<String,TRECDivQuery>> trecDivQueriesArray, List<Map<String,TRECQueryAspects>> trecDivQueryAspectsArray,
			List<String> outputNameArray, List<String> typePrefixArray, List<ArrayList<Metric>> lostFunctionsArray){
		////--
		ArrayList<Integer> testIDList = new ArrayList<>();
		for(int i=0; i<versionList.size(); i++){
			testIDList.add(i);
		}
		//
		double span = 0.1;
		
		for(Integer testID: testIDList){
			//training
			ArrayList<StrDouble> performancePerLamList = new ArrayList<>();
			for(double lam = 0.0; lam<=1.0; lam+= span){
				//per version
				double avgPerformanceSum = 0.0;
				for(int i=0; i<versionList.size(); i++){
					if(i != testID){
						//----
						List<String> qList = qListArray.get(i);							
						HashMap<String,String> trecDivDocs = trecDivDocsArray.get(i);							
						Map<String,TRECDivQuery> trecDivQueries = trecDivQueriesArray.get(i);
						String typePrefix = typePrefixArray.get(i);
						String output_filename = outputNameArray.get(i);
						output_filename += ("_trainFor_"+versionList.get(testID).toString()+"_"+Evaluator.oneResultFormat.format(lam));
						Map<String,TRECQueryAspects> trecDivQueryAspects = trecDivQueryAspectsArray.get(i);
						ArrayList<Metric> lossFunctions = lostFunctionsArray.get(i);
						//----
						//(1) balance relevance and diversity
						double SimDivLambda = lam;
						
						//(2) combination			
						ExemplarType exemplarType = ExemplarType.Y;
						Strategy flStrategy = Strategy.Belief;			
						String nameFix = "_"+exemplarType.toString();
						nameFix += ("_"+flStrategy.toString());
								
						//(3)
						double k1, k3, b;
						k1=1.2d; k3=0.5d; b=0.5d;   // achieves the best
						//k1=0.5d; k3=0.5d; b=0.5d; //better than the group of b=1000d;
						//k1=1.2d; k3=0.5d; b=1000d;
						
						//kernel-1			
						BM25Kernel_A1 releKernel = new BM25Kernel_A1(trecDivDocs, k1, k3, b);						
						//kernel-2
						TFIDF_A1 divKernel = new TFIDF_A1(trecDivDocs, false);
						
						output_filename += ("_"+releKernel.getString());
						output_filename += ("_"+divKernel.getString());
						
						//1
						double lambda_1 = 0.5;
						int iterationTimes_1 = 5000;
						int noChangeIterSpan_1 = 10;
						
						//kernel-1
						ImpSRDRanker impSRDRanker = new ImpSRDRanker(trecDivDocs, releKernel, divKernel, lambda_1, iterationTimes_1, noChangeIterSpan_1,
								SimDivLambda, exemplarType, flStrategy);
						
						ArrayList<ResultRanker> rankerList = new ArrayList<ResultRanker>();
						rankerList.add(impSRDRanker);
						
						// Evaluate results of different query processing algorithms
						Evaluator trecDivEvaluator = new TRECDivEvaluator(trecDivQueries, output_prefix, output_filename+nameFix);
						
						ArrayList<Double> avgPerRankerList = null;
						try {
							avgPerRankerList = trecDivEvaluator.doEval(qList, trecDivDocs, trecDivQueryAspects, lossFunctions, rankerList, cutoffK);
						} catch (Exception e) {
							e.printStackTrace();
						}
						
						avgPerformanceSum += avgPerRankerList.get(0);
						//----
					}
				}
				//
				performancePerLamList.add(new StrDouble(Double.toString(lam), avgPerformanceSum/(versionList.size()-1)));
			}
			
			//get optimal lambda				
			Collections.sort(performancePerLamList, new PairComparatorBySecond_Desc<>());
			System.out.println("Training optimal lambda selection:");
			for(StrDouble element: performancePerLamList){
				System.out.print(element.toString() + " $ ");
			}
			System.out.println();
			double optimalLam = Double.parseDouble(performancePerLamList.get(0).getFirst());
			
			////testing
			//----
			List<String> qList = qListArray.get(testID);							
			HashMap<String,String> trecDivDocs = trecDivDocsArray.get(testID);							
			Map<String,TRECDivQuery> trecDivQueries = trecDivQueriesArray.get(testID);
			String typePrefix = typePrefixArray.get(testID);
			String output_filename = outputNameArray.get(testID);
			output_filename += ("_test_optimal_"+versionList.get(testID).toString()+"_"+Evaluator.oneResultFormat.format(optimalLam));
			Map<String,TRECQueryAspects> trecDivQueryAspects = trecDivQueryAspectsArray.get(testID);
			ArrayList<Metric> lossFunctions = lostFunctionsArray.get(testID);
			//----
			//(1) balance relevance and diversity
			double SimDivLambda = optimalLam;
			
			//(2) combination			
			ExemplarType exemplarType = ExemplarType.Y;
			Strategy flStrategy = Strategy.Belief;			
			String nameFix = "_"+exemplarType.toString();
			nameFix += ("_"+flStrategy.toString());
					
			//(3)
			double k1, k3, b;
			k1=1.2d; k3=0.5d; b=0.5d;   // achieves the best
			//k1=0.5d; k3=0.5d; b=0.5d; //better than the group of b=1000d;
			//k1=1.2d; k3=0.5d; b=1000d;
			
			//kernel-1			
			BM25Kernel_A1 releKernel = new BM25Kernel_A1(trecDivDocs, k1, k3, b);			
			//kernel-2
			TFIDF_A1 divKernel = new TFIDF_A1(trecDivDocs, false);
			
			output_filename += ("_"+releKernel.getString());
			output_filename += ("_"+divKernel.getString());
			
			//1
			double lambda_1 = 0.5;
			int iterationTimes_1 = 5000;
			int noChangeIterSpan_1 = 10;
			
			//kernel-1
			ImpSRDRanker impSRDRanker = new ImpSRDRanker(trecDivDocs, releKernel, divKernel,
					lambda_1, iterationTimes_1, noChangeIterSpan_1, SimDivLambda, exemplarType, flStrategy);

			ArrayList<ResultRanker> rankerList = new ArrayList<ResultRanker>();
			rankerList.add(impSRDRanker);
			
			// Evaluate results of different query processing algorithms
			Evaluator trecDivEvaluator = new TRECDivEvaluator(trecDivQueries, output_prefix, output_filename+nameFix);
			
			try {
				trecDivEvaluator.doEval(qList, trecDivDocs, trecDivQueryAspects, lossFunctions, rankerList, cutoffK);
			} catch (Exception e) {
				e.printStackTrace();
			}
			//----				
		}
	}
	////
	public static void crossEval_XQuAD(ArrayList<DivVersion> versionList, int cutoffK, String output_prefix,
			List<List<String>> qListArray, List<HashMap<String,String>> trecDivDocsArray,
			List<Map<String,TRECDivQuery>> trecDivQueriesArray, List<Map<String,TRECQueryAspects>> trecDivQueryAspectsArray,
			List<String> outputNameArray, List<String> typePrefixArray, List<ArrayList<Metric>> lostFunctionsArray){
		////

		////--
		ArrayList<Integer> testIDList = new ArrayList<>();
		for(int i=0; i<versionList.size(); i++){
			testIDList.add(i);
		}
		//
		double span = 0.1;
		
		for(Integer testID: testIDList){
			//training
			ArrayList<StrDouble> performancePerLamList = new ArrayList<>();
			for(double lam = 0.0; lam<=1.0; lam+= span){
				//per version
				double avgPerformanceSum = 0.0;
				for(int i=0; i<versionList.size(); i++){
					if(i != testID){
						//----
						List<String> qList = qListArray.get(i);							
						HashMap<String,String> trecDivDocs = trecDivDocsArray.get(i);							
						Map<String,TRECDivQuery> trecDivQueries = trecDivQueriesArray.get(i);
						String typePrefix = typePrefixArray.get(i);
						String output_filename = outputNameArray.get(i);
						output_filename += ("_trainForTesting_"+versionList.get(testID).toString()+"_"+Evaluator.oneResultFormat.format(lam));
						Map<String,TRECQueryAspects> trecDivQueryAspects = trecDivQueryAspectsArray.get(i);
						ArrayList<Metric> lossFunctions = lostFunctionsArray.get(i);
						//----
						//(1) balance relevance and diversity
						double tuneLambda = lam;			
						String nameFix = "";						
						//(2)
						//1.kernel
						//TFIDF_A1 usedKernel = new TFIDF_A1(trecDivDocs, false);
						//2.terrier kernel
						//common
						TrecScorer trecScorer = new TrecScorer();						
						TerrierKernel usedKernel = new TerrierKernel(trecDivDocs, trecScorer);	
						output_filename += ("_"+usedKernel.getString());
						
						XQuADRanker xQuADRanker = new XQuADRanker(trecDivDocs, tuneLambda, usedKernel);
						ArrayList<ResultRanker> rankerList = new ArrayList<ResultRanker>();
						rankerList.add(xQuADRanker);
						
						// Evaluate results of different query processing algorithms
						Evaluator trecDivEvaluator = new TRECDivEvaluator(trecDivQueries, output_prefix, output_filename+nameFix);
						
						ArrayList<Double> avgPerRankerList = null;
						try {
							avgPerRankerList = trecDivEvaluator.doEval(qList, trecDivDocs, trecDivQueryAspects, lossFunctions, rankerList, cutoffK);
						} catch (Exception e) {
							e.printStackTrace();
						}
						
						avgPerformanceSum += avgPerRankerList.get(0);
						//----
					}
				}
				//
				performancePerLamList.add(new StrDouble(Double.toString(lam), avgPerformanceSum/(versionList.size()-1)));
			}
			
			//get optimal lambda				
			Collections.sort(performancePerLamList, new PairComparatorBySecond_Desc<>());
			System.out.println("Training optimal lambda selection:");
			for(StrDouble element: performancePerLamList){
				System.out.print(element.toString() + " $ ");
			}
			System.out.println();
			double optimalLam = Double.parseDouble(performancePerLamList.get(0).getFirst());
			
			////testing
			//----
			List<String> qList = qListArray.get(testID);							
			HashMap<String,String> trecDivDocs = trecDivDocsArray.get(testID);							
			Map<String,TRECDivQuery> trecDivQueries = trecDivQueriesArray.get(testID);
			String typePrefix = typePrefixArray.get(testID);
			String output_filename = outputNameArray.get(testID);
			output_filename += ("_test_optimal_"+versionList.get(testID).toString()+"_"+Evaluator.oneResultFormat.format(optimalLam));
			Map<String,TRECQueryAspects> trecDivQueryAspects = trecDivQueryAspectsArray.get(testID);
			ArrayList<Metric> lossFunctions = lostFunctionsArray.get(testID);
			//----
			//(1) balance relevance and diversity
			double optLambda = optimalLam;			
			String nameFix = "";						
			//(2)
			//kernel
			//TFIDF_A1 usedKernel = new TFIDF_A1(trecDivDocs, false);
			//2.terrier kernel
			//common
			TrecScorer trecScorer = new TrecScorer();						
			TerrierKernel usedKernel = new TerrierKernel(trecDivDocs, trecScorer);		
			
			output_filename += ("_"+usedKernel.getString());
			
			XQuADRanker xQuADRanker = new XQuADRanker(trecDivDocs, optLambda, usedKernel);
			ArrayList<ResultRanker> rankerList = new ArrayList<ResultRanker>();
			rankerList.add(xQuADRanker);
			
			// Evaluate results of different query processing algorithms
			Evaluator trecDivEvaluator = new TRECDivEvaluator(trecDivQueries, output_prefix, output_filename+nameFix);
			
			try {
				trecDivEvaluator.doEval(qList, trecDivDocs, trecDivQueryAspects, lossFunctions, rankerList, cutoffK);
			} catch (Exception e) {
				e.printStackTrace();
			}
			//----				
		}
	}
	////
	public static void crossEval_PM2(ArrayList<DivVersion> versionList, int cutoffK, String output_prefix,
			List<List<String>> qListArray, List<HashMap<String,String>> trecDivDocsArray,
			List<Map<String,TRECDivQuery>> trecDivQueriesArray, List<Map<String,TRECQueryAspects>> trecDivQueryAspectsArray,
			List<String> outputNameArray, List<String> typePrefixArray, List<ArrayList<Metric>> lostFunctionsArray){
		////

		////--
		ArrayList<Integer> testIDList = new ArrayList<>();
		for(int i=0; i<versionList.size(); i++){
			testIDList.add(i);
		}
		//
		double span = 0.1;
		
		for(Integer testID: testIDList){
			//training
			ArrayList<StrDouble> performancePerLamList = new ArrayList<>();
			for(double lam = 0.0; lam<=1.0; lam+= span){
				//per version
				double avgPerformanceSum = 0.0;
				for(int i=0; i<versionList.size(); i++){
					if(i != testID){
						//----
						List<String> qList = qListArray.get(i);							
						HashMap<String,String> trecDivDocs = trecDivDocsArray.get(i);							
						Map<String,TRECDivQuery> trecDivQueries = trecDivQueriesArray.get(i);
						String typePrefix = typePrefixArray.get(i);
						String output_filename = outputNameArray.get(i);
						output_filename += ("_trainFor_"+versionList.get(testID).toString()+"_"+Evaluator.oneResultFormat.format(lam));
						Map<String,TRECQueryAspects> trecDivQueryAspects = trecDivQueryAspectsArray.get(i);
						ArrayList<Metric> lossFunctions = lostFunctionsArray.get(i);
						//----
						//(1) balance relevance and diversity
						double tuneLambda = lam;			
						String nameFix = "";						
						//(2)
						//1. kernel
						//TFIDF_A1 usedKernel = new TFIDF_A1(trecDivDocs, false);
						
						//2.terrier kernel
						//common
						TrecScorer trecScorer = new TrecScorer();						
						TerrierKernel usedKernel = new TerrierKernel(trecDivDocs, trecScorer);						
						
						output_filename += ("_"+usedKernel.getString());
						
						PM2Ranker pm2Ranker = new PM2Ranker(trecDivDocs, tuneLambda, usedKernel);
						ArrayList<ResultRanker> rankerList = new ArrayList<ResultRanker>();
						rankerList.add(pm2Ranker);
						
						// Evaluate results of different query processing algorithms
						Evaluator trecDivEvaluator = new TRECDivEvaluator(trecDivQueries, output_prefix, output_filename+nameFix);
						
						ArrayList<Double> avgPerRankerList = null;
						try {
							avgPerRankerList = trecDivEvaluator.doEval(qList, trecDivDocs, trecDivQueryAspects, lossFunctions, rankerList, cutoffK);
						} catch (Exception e) {
							e.printStackTrace();
						}
						
						avgPerformanceSum += avgPerRankerList.get(0);
						//----
					}
				}
				//
				performancePerLamList.add(new StrDouble(Double.toString(lam), avgPerformanceSum/(versionList.size()-1)));
			}
			
			//get optimal lambda				
			Collections.sort(performancePerLamList, new PairComparatorBySecond_Desc<>());
			System.out.println("Training optimal lambda selection:");
			for(StrDouble element: performancePerLamList){
				System.out.print(element.toString() + " $ ");
			}
			System.out.println();
			double optimalLam = Double.parseDouble(performancePerLamList.get(0).getFirst());
			
			////testing
			//----
			List<String> qList = qListArray.get(testID);							
			HashMap<String,String> trecDivDocs = trecDivDocsArray.get(testID);							
			Map<String,TRECDivQuery> trecDivQueries = trecDivQueriesArray.get(testID);
			String typePrefix = typePrefixArray.get(testID);
			String output_filename = outputNameArray.get(testID);
			output_filename += ("_test_optimal_"+versionList.get(testID).toString()+"_"+Evaluator.oneResultFormat.format(optimalLam));
			Map<String,TRECQueryAspects> trecDivQueryAspects = trecDivQueryAspectsArray.get(testID);
			ArrayList<Metric> lossFunctions = lostFunctionsArray.get(testID);
			//----
			//(1) balance relevance and diversity
			double optLambda = optimalLam;			
			String nameFix = "";						
			//(2)
			
			//1. kernel
			//TFIDF_A1 usedKernel = new TFIDF_A1(trecDivDocs, false);
			
			//2.terrier kernel
			TrecScorer trecScorer = new TrecScorer();						
			TerrierKernel usedKernel = new TerrierKernel(trecDivDocs, trecScorer);	
			
			output_filename += ("_"+usedKernel.getString());
			
			PM2Ranker pm2Ranker = new PM2Ranker(trecDivDocs, optLambda, usedKernel);
			ArrayList<ResultRanker> rankerList = new ArrayList<ResultRanker>();
			rankerList.add(pm2Ranker);
			
			// Evaluate results of different query processing algorithms
			Evaluator trecDivEvaluator = new TRECDivEvaluator(trecDivQueries, output_prefix, output_filename+nameFix);
			
			try {
				trecDivEvaluator.doEval(qList, trecDivDocs, trecDivQueryAspects, lossFunctions, rankerList, cutoffK);
			} catch (Exception e) {
				e.printStackTrace();
			}
			//----				
		}
	}
	////
	public static void crossEval_MMR(ArrayList<DivVersion> versionList, int cutoffK, String output_prefix,
			List<List<String>> qListArray, List<HashMap<String,String>> trecDivDocsArray,
			List<Map<String,TRECDivQuery>> trecDivQueriesArray, List<Map<String,TRECQueryAspects>> trecDivQueryAspectsArray,
			List<String> outputNameArray, List<String> typePrefixArray, List<ArrayList<Metric>> lostFunctionsArray){
		////

		////--
		ArrayList<Integer> testIDList = new ArrayList<>();
		for(int i=0; i<versionList.size(); i++){
			testIDList.add(i);
		}
		//
		double span = 0.1;
		
		for(Integer testID: testIDList){
			//training
			ArrayList<StrDouble> performancePerLamList = new ArrayList<>();
			for(double lam = 0.0; lam<=1.0; lam+= span){
				//per version
				double avgPerformanceSum = 0.0;
				for(int i=0; i<versionList.size(); i++){
					if(i != testID){
						//----
						List<String> qList = qListArray.get(i);							
						HashMap<String,String> trecDivDocs = trecDivDocsArray.get(i);							
						Map<String,TRECDivQuery> trecDivQueries = trecDivQueriesArray.get(i);
						String typePrefix = typePrefixArray.get(i);
						String output_filename = outputNameArray.get(i);
						output_filename += ("_trainFor_"+versionList.get(testID).toString()+"_"+Evaluator.oneResultFormat.format(lam));
						Map<String,TRECQueryAspects> trecDivQueryAspects = trecDivQueryAspectsArray.get(i);
						ArrayList<Metric> lossFunctions = lostFunctionsArray.get(i);
						//----
						//(1) balance relevance and diversity
						double tuneLambda = lam;			
						String nameFix = "";						
						//(2)
						//kernel
						double k1, k3, b;
						k1=1.2d; k3=0.5d; b=0.5d;   // achieves the best
						//k1=0.5d; k3=0.5d; b=0.5d; //better than the group of b=1000d;
						//k1=1.2d; k3=0.5d; b=1000d;						
						BM25Kernel_A1 releKernel = new BM25Kernel_A1(trecDivDocs, k1, k3, b);
						
						TFIDF_A1 disKernel = new TFIDF_A1(trecDivDocs, false);
						
						output_filename += ("_"+releKernel.getString());
						output_filename += ("_"+disKernel.getString());
						
						//TFIDF_A1 divKernel = new TFIDF_A1(trecDivDocs, false);
						
						MMR mmrRanker = new MMR(trecDivDocs, tuneLambda, releKernel, disKernel);
						
						ArrayList<ResultRanker> rankerList = new ArrayList<ResultRanker>();
						rankerList.add(mmrRanker);
						
						// Evaluate results of different query processing algorithms
						Evaluator trecDivEvaluator = new TRECDivEvaluator(trecDivQueries, output_prefix, output_filename+nameFix);
						
						ArrayList<Double> avgPerRankerList = null;
						try {
							avgPerRankerList = trecDivEvaluator.doEval(qList, trecDivDocs, trecDivQueryAspects, lossFunctions, rankerList, cutoffK);
						} catch (Exception e) {
							e.printStackTrace();
						}
						
						avgPerformanceSum += avgPerRankerList.get(0);
						//----
					}
				}
				//
				performancePerLamList.add(new StrDouble(Double.toString(lam), avgPerformanceSum/(versionList.size()-1)));
			}
			
			//get optimal lambda				
			Collections.sort(performancePerLamList, new PairComparatorBySecond_Desc<>());
			System.out.println("Training optimal lambda selection:");
			for(StrDouble element: performancePerLamList){
				System.out.print(element.toString() + " $ ");
			}
			System.out.println();
			double optimalLam = Double.parseDouble(performancePerLamList.get(0).getFirst());
			
			////testing
			//----
			List<String> qList = qListArray.get(testID);							
			HashMap<String,String> trecDivDocs = trecDivDocsArray.get(testID);							
			Map<String,TRECDivQuery> trecDivQueries = trecDivQueriesArray.get(testID);
			String typePrefix = typePrefixArray.get(testID);
			String output_filename = outputNameArray.get(testID);
			output_filename += ("_test_optimal_"+versionList.get(testID).toString()+"_"+Evaluator.oneResultFormat.format(optimalLam));
			Map<String,TRECQueryAspects> trecDivQueryAspects = trecDivQueryAspectsArray.get(testID);
			ArrayList<Metric> lossFunctions = lostFunctionsArray.get(testID);
			//----
			//(1) balance relevance and diversity
			double optLambda = optimalLam;			
			String nameFix = "";						
			//(2)
			//kernel
			double k1, k3, b;
			k1=1.2d; k3=0.5d; b=0.5d;   // achieves the best
			//k1=0.5d; k3=0.5d; b=0.5d; //better than the group of b=1000d;
			//k1=1.2d; k3=0.5d; b=1000d			
			BM25Kernel_A1 releKernel = new BM25Kernel_A1(trecDivDocs, k1, k3, b);
			
			TFIDF_A1 disKernel = new TFIDF_A1(trecDivDocs, false);
			
			output_filename += ("_"+releKernel.getString());
			output_filename += ("_"+disKernel.getString());		
			
			
			MMR mmrRanker = new MMR(trecDivDocs, optLambda, releKernel, disKernel);
			
			ArrayList<ResultRanker> rankerList = new ArrayList<ResultRanker>();
			rankerList.add(mmrRanker);
			
			// Evaluate results of different query processing algorithms
			Evaluator trecDivEvaluator = new TRECDivEvaluator(trecDivQueries, output_prefix, output_filename+nameFix);
			
			try {
				trecDivEvaluator.doEval(qList, trecDivDocs, trecDivQueryAspects, lossFunctions, rankerList, cutoffK);
			} catch (Exception e) {
				e.printStackTrace();
			}
			//----				
		}
	}
	////
	public static void crossEval_DFP(ArrayList<DivVersion> versionList, int cutoffK, String output_prefix,
			List<List<String>> qListArray, List<HashMap<String,String>> trecDivDocsArray,
			List<Map<String,TRECDivQuery>> trecDivQueriesArray, List<Map<String,TRECQueryAspects>> trecDivQueryAspectsArray,
			List<String> outputNameArray, List<String> typePrefixArray, List<ArrayList<Metric>> lostFunctionsArray){
		////

		////--
		ArrayList<Integer> testIDList = new ArrayList<>();
		for(int i=0; i<versionList.size(); i++){
			testIDList.add(i);
		}
		//
		double span = 0.1;
		
		for(Integer testID: testIDList){
			//training
			ArrayList<StrDouble> performancePerLamList = new ArrayList<>();
			for(double lam = 0.0; lam<=1.0; lam+= span){
				//per version
				double avgPerformanceSum = 0.0;
				for(int i=0; i<versionList.size(); i++){
					if(i != testID){
						//----
						List<String> qList = qListArray.get(i);							
						HashMap<String,String> trecDivDocs = trecDivDocsArray.get(i);							
						Map<String,TRECDivQuery> trecDivQueries = trecDivQueriesArray.get(i);
						String typePrefix = typePrefixArray.get(i);
						String output_filename = outputNameArray.get(i);
						output_filename += ("_trainFor_"+versionList.get(testID).toString()+"_"+Evaluator.oneResultFormat.format(lam));
						Map<String,TRECQueryAspects> trecDivQueryAspects = trecDivQueryAspectsArray.get(i);
						ArrayList<Metric> lossFunctions = lostFunctionsArray.get(i);
						//----
						//(1) balance relevance and diversity
						double tuneLambda = lam;	
						int itrThreshold = 10000;
						String nameFix = "";						
						//(2)
						//kernel
						double k1, k3, b;
						k1=1.2d; k3=0.5d; b=0.5d;   // achieves the best
						//k1=0.5d; k3=0.5d; b=0.5d; //better than the group of b=1000d;
						//k1=1.2d; k3=0.5d; b=1000d;
						//--1
						BM25Kernel_A1 releKernel = new BM25Kernel_A1(trecDivDocs, k1, k3, b);
						//--2
						TFIDF_A1 disKernel = new TFIDF_A1(trecDivDocs, false);
						
						output_filename += ("_"+releKernel.getString());
						output_filename += ("_"+disKernel.getString());
						
						MDP dfpRanker = new MDP(trecDivDocs, tuneLambda, itrThreshold, releKernel, disKernel, null, trecDivQueries);
						
						ArrayList<ResultRanker> rankerList = new ArrayList<ResultRanker>();
						rankerList.add(dfpRanker);
												
						// Evaluate results of different query processing algorithms
						Evaluator trecDivEvaluator = new TRECDivEvaluator(trecDivQueries, output_prefix, output_filename+nameFix);
						
						ArrayList<Double> avgPerRankerList = null;
						try {
							avgPerRankerList = trecDivEvaluator.doEval(qList, trecDivDocs, trecDivQueryAspects, lossFunctions, rankerList, cutoffK);
						} catch (Exception e) {
							e.printStackTrace();
						}
						
						avgPerformanceSum += avgPerRankerList.get(0);
						//----
					}
				}
				//
				performancePerLamList.add(new StrDouble(Double.toString(lam), avgPerformanceSum/(versionList.size()-1)));
			}
			
			//get optimal lambda				
			Collections.sort(performancePerLamList, new PairComparatorBySecond_Desc<>());
			System.out.println("Training optimal lambda selection:");
			for(StrDouble element: performancePerLamList){
				System.out.print(element.toString() + " $ ");
			}
			System.out.println();
			double optimalLam = Double.parseDouble(performancePerLamList.get(0).getFirst());
			
			////testing
			//----
			List<String> qList = qListArray.get(testID);							
			HashMap<String,String> trecDivDocs = trecDivDocsArray.get(testID);							
			Map<String,TRECDivQuery> trecDivQueries = trecDivQueriesArray.get(testID);
			String typePrefix = typePrefixArray.get(testID);
			String output_filename = outputNameArray.get(testID);
			output_filename += ("_test_optimal_"+versionList.get(testID).toString()+"_"+Evaluator.oneResultFormat.format(optimalLam));
			Map<String,TRECQueryAspects> trecDivQueryAspects = trecDivQueryAspectsArray.get(testID);
			ArrayList<Metric> lossFunctions = lostFunctionsArray.get(testID);
			//----
			//(1) balance relevance and diversity
			double optLambda = optimalLam;		
			int itrThreshold = 10000;
			String nameFix = "";						
			//(2)
			//kernel
			double k1, k3, b;
			k1=1.2d; k3=0.5d; b=0.5d;   // achieves the best
			//k1=0.5d; k3=0.5d; b=0.5d; //better than the group of b=1000d;
			//k1=1.2d; k3=0.5d; b=1000d;
			
			BM25Kernel_A1 releKernel = new BM25Kernel_A1(trecDivDocs, k1, k3, b);
			TFIDF_A1 disKernel = new TFIDF_A1(trecDivDocs, false);
			
			output_filename += ("_"+releKernel.getString());
			output_filename += ("_"+disKernel.getString());
			
			MDP dfpRanker = new MDP(trecDivDocs, optLambda, itrThreshold, releKernel, disKernel, null, trecDivQueries);
			
			ArrayList<ResultRanker> rankerList = new ArrayList<ResultRanker>();
			rankerList.add(dfpRanker);
			
			// Evaluate results of different query processing algorithms
			Evaluator trecDivEvaluator = new TRECDivEvaluator(trecDivQueries, output_prefix, output_filename+nameFix);
			
			try {
				trecDivEvaluator.doEval(qList, trecDivDocs, trecDivQueryAspects, lossFunctions, rankerList, cutoffK);
			} catch (Exception e) {
				e.printStackTrace();
			}
			//----				
		}
	}
	
	/**
	 * sigir2016
	 * **/
	public static void sigir2016(){
		//////////////
		//sigir w.r.t. 2-fold
		//////////////
		
		////---- part-1 implicit SRD ----////
		
		boolean commonIndri = true;
		
		////single run for efficiency
		//--implicit--
		//(1)
		//TRECDivEvaluation.trecDivEvaluation(commonIndri, DivVersion.Div2009, RankStrategy.MMR);
		//TRECDivEvaluation.trecDivEvaluation(commonIndri, DivVersion.Div2010, RankStrategy.MMR);
		//(2)
		//TRECDivEvaluation.trecDivEvaluation(commonIndri, DivVersion.Div2009, RankStrategy.ImpSRD);
		//TRECDivEvaluation.trecDivEvaluation(commonIndri, DivVersion.Div2010, RankStrategy.ImpSRD);
		//(3)
		//TRECDivEvaluation.trecDivEvaluation(commonIndri, DivVersion.Div2009, RankStrategy.DFP);
		//TRECDivEvaluation.trecDivEvaluation(commonIndri, DivVersion.Div2010, RankStrategy.DFP);
		//--explicit--
		//(4)
		//TRECDivEvaluation.trecDivEvaluation(commonIndri, DivVersion.Div2009, RankStrategy.ExpSRD);
		//TRECDivEvaluation.trecDivEvaluation(commonIndri, DivVersion.Div2010, RankStrategy.ExpSRD);
		
		//(5)PM2-tfidf and lm
		//TRECDivEvaluation.trecDivEvaluation(commonIndri, DivVersion.Div2009, RankStrategy.PM2);
		//TRECDivEvaluation.trecDivEvaluation(commonIndri, DivVersion.Div2010, RankStrategy.PM2);
		
		//(6)xquad-tfidf and lm
		TRECDivEvaluation.trecDivEvaluation(commonIndri, DivVersion.Div2009, RankStrategy.XQuAD);
		TRECDivEvaluation.trecDivEvaluation(commonIndri, DivVersion.Div2010, RankStrategy.XQuAD);
		
		
		
		////baseline
		//TRECDivEvaluation.trecDivEvaluation(commonIndri, DivVersion.Div2009, RankStrategy.CommonIndriBaseline);
		//TRECDivEvaluation.trecDivEvaluation(commonIndri, DivVersion.Div2010, RankStrategy.CommonIndriBaseline);
		//TRECDivEvaluation.trecDivEvaluation(commonIndri, DivVersion.Div2011, RankStrategy.CommonIndriBaseline);
		//TRECDivEvaluation.trecDivEvaluation(commonIndri, DivVersion.Div2012, RankStrategy.CommonIndriBaseline);
		
		//specific lambda and kernels and test
		//implicitSRD, 
		//TRECDivEvaluation.trecDivEvaluation(commonIndri, DivVersion.Div2009, RankStrategy.ImpSRD);
		
		
		////cross validation
		/*
		ArrayList<DivVersion> versionList = new ArrayList<>();
		versionList.add(DivVersion.Div2009);
		versionList.add(DivVersion.Div2010);
		
		//TRECDivEvaluation.crossTrecDivEvaluation(commonIndri, versionList, RankStrategy.ImpSRD);
		//i.e., dfp
		//TRECDivEvaluation.crossTrecDivEvaluation(commonIndri, versionList, RankStrategy.MDP);
		//i.e., mmr
		TRECDivEvaluation.crossTrecDivEvaluation(commonIndri, versionList, RankStrategy.BFS);	
		*/
		
		
		
		////---- part-2 explicit SRD ----////
		
		////proposed one
		//true for sigir
		/*
		//boolean commonIndri = true;
		//TRECDivEvaluation.trecDivEvaluation(commonIndri, DivVersion.Div2009, RankStrategy.ExpSRD);
		//TRECDivEvaluation.trecDivEvaluation(commonIndri, DivVersion.Div2010, RankStrategy.ExpSRD);
		//TRECDivEvaluation.trecDivEvaluation(commonIndri, DivVersion.Div2011, RankStrategy.ExpSRD);
		//TRECDivEvaluation.trecDivEvaluation(commonIndri, DivVersion.Div2012, RankStrategy.ExpSRD);
		*/
		
		////baseline xquad, pm2 
		//there are two types of kernel are used, terrierKernel & tfidf, which should be set up mannually
		/*
		ArrayList<DivVersion> versionList = new ArrayList<>();
		versionList.add(DivVersion.Div2009);
		versionList.add(DivVersion.Div2010);
		
		TRECDivEvaluation.crossTrecDivEvaluation(commonIndri, versionList, RankStrategy.XQuAD);
		TRECDivEvaluation.crossTrecDivEvaluation(commonIndri, versionList, RankStrategy.PM2);
		*/
		
		
		//////////////
		//sigir w.r.t. 4-fold
		//////////////
	}
	
	/**
	 * for tkde
	 * **/
	private static enum ReleProVersion{LM, COS, BM25}
	
	public static void perLambdaForCrossEval_PM2(ReleProVersion releProVersion, ArrayList<DivVersion> versionList, int cutoffK, String output_prefix,
			List<List<String>> qListArray, List<HashMap<String,String>> trecDivDocsArray,
			List<Map<String,TRECDivQuery>> trecDivQueriesArray, List<Map<String,TRECQueryAspects>> trecDivQueryAspectsArray,
			List<String> outputNameArray, List<String> typePrefixArray, List<ArrayList<Metric>> lostFunctionsArray){
		
		////dir
		output_prefix += ("PM2_"+releProVersion.toString());
		File dirFile = new File(output_prefix);
		if(!dirFile.exists()){
			dirFile.mkdirs();
		}		
		output_prefix += "/";
		
		//// per lambda for each version
		double span = 0.1;
		for(int id=0; id<versionList.size(); id++){
			//--
			for(double lam = 0.0; lam<=1.0; lam+= span){
				List<String> qList = qListArray.get(id);							
				HashMap<String,String> trecDivDocs = trecDivDocsArray.get(id);							
				Map<String,TRECDivQuery> trecDivQueries = trecDivQueriesArray.get(id);
				String typePrefix = typePrefixArray.get(id);
				String output_filename = outputNameArray.get(id);
				output_filename += ("_"+Evaluator.oneResultFormat.format(lam));
				Map<String,TRECQueryAspects> trecDivQueryAspects = trecDivQueryAspectsArray.get(id);
				ArrayList<Metric> lossFunctions = lostFunctionsArray.get(id);
				//----
				//(1) balance relevance and diversity
				double tuneLambda = lam;			
				String nameFix = "";						
				//(2)				
				Kernel usedKernel = null;
				if(releProVersion == ReleProVersion.COS){
					usedKernel = new TFIDF_A1(trecDivDocs, false);
				}else{
					TrecScorer trecScorer = new TrecScorer();						
					usedKernel = new TerrierKernel(trecDivDocs, trecScorer);
				}						
				
				output_filename += ("_"+usedKernel.getString());
				
				PM2Ranker pm2Ranker = new PM2Ranker(trecDivDocs, tuneLambda, usedKernel);
				ArrayList<ResultRanker> rankerList = new ArrayList<ResultRanker>();
				rankerList.add(pm2Ranker);
				
				// Evaluate results of different query processing algorithms
				Evaluator trecDivEvaluator = new TRECDivEvaluator(trecDivQueries, output_prefix, output_filename+nameFix);
				
				ArrayList<Double> avgPerRankerList = null;
				try {
					avgPerRankerList = trecDivEvaluator.doEval(qList, trecDivDocs, trecDivQueryAspects, lossFunctions, rankerList, cutoffK);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			//--
		}
	}
	
	public static void perLambdaForCrossEval_XQuAD(ReleProVersion releProVersion, ArrayList<DivVersion> versionList, int cutoffK, String output_prefix,
			List<List<String>> qListArray, List<HashMap<String,String>> trecDivDocsArray,
			List<Map<String,TRECDivQuery>> trecDivQueriesArray, List<Map<String,TRECQueryAspects>> trecDivQueryAspectsArray,
			List<String> outputNameArray, List<String> typePrefixArray, List<ArrayList<Metric>> lostFunctionsArray){
		////dir
		output_prefix += ("XQuAD_"+releProVersion.toString());
		File dirFile = new File(output_prefix);
		if(!dirFile.exists()){
			dirFile.mkdirs();
		}	
		output_prefix += "/";
		
		//// per lambda for each version
		double span = 0.1;
		for(int id=0; id<versionList.size(); id++){
			for(double lam = 0.0; lam<=1.0; lam+= span){
				//----
				List<String> qList = qListArray.get(id);							
				HashMap<String,String> trecDivDocs = trecDivDocsArray.get(id);							
				Map<String,TRECDivQuery> trecDivQueries = trecDivQueriesArray.get(id);
				String typePrefix = typePrefixArray.get(id);
				String output_filename = outputNameArray.get(id);
				output_filename += ("_"+Evaluator.oneResultFormat.format(lam));
				Map<String,TRECQueryAspects> trecDivQueryAspects = trecDivQueryAspectsArray.get(id);
				ArrayList<Metric> lossFunctions = lostFunctionsArray.get(id);
				//----
				//(1) balance relevance and diversity
				double tuneLambda = lam;			
				String nameFix = "";						
				//(2)
				Kernel usedKernel = null;
				if(releProVersion == ReleProVersion.COS){
					usedKernel = new TFIDF_A1(trecDivDocs, false);
				}else{
					TrecScorer trecScorer = new TrecScorer();						
					usedKernel = new TerrierKernel(trecDivDocs, trecScorer);
				}
				
				output_filename += ("_"+usedKernel.getString());
				
				XQuADRanker xQuADRanker = new XQuADRanker(trecDivDocs, tuneLambda, usedKernel);
				ArrayList<ResultRanker> rankerList = new ArrayList<ResultRanker>();
				rankerList.add(xQuADRanker);
				
				// Evaluate results of different query processing algorithms
				Evaluator trecDivEvaluator = new TRECDivEvaluator(trecDivQueries, output_prefix, output_filename+nameFix);
				
				ArrayList<Double> avgPerRankerList = null;
				try {
					avgPerRankerList = trecDivEvaluator.doEval(qList, trecDivDocs, trecDivQueryAspects, lossFunctions, rankerList, cutoffK);
				} catch (Exception e) {
					e.printStackTrace();
				}
				//----
			}
		}
	}
	
	private static void tkdePerLambdaTrecDivEvaluation(Boolean commonIndri, ReleProVersion releProVersion, 
			ArrayList<DivVersion> versionList, RankStrategy rankStrategy){
		//differentiating faceted and ambiguous
		boolean diffFacetedAmbiguous = false;
		boolean acceptFaceted = false;
		String facetedType = "faceted";
		String ambType = "ambiguous";		
		
		//cutoff
		int cutoffK = 20;
		
		////
		List<List<String>> qListArray = new ArrayList<List<String>>();
		List<HashMap<String,String>> trecDivDocsArray = new ArrayList<HashMap<String,String>>();
		List<Map<String,TRECDivQuery>> trecDivQueriesArray = new ArrayList<>();
		List<Map<String,TRECQueryAspects>> trecDivQueryAspectsArray = new ArrayList<>();
		List<String> outputNameArray = new ArrayList<>();
		List<String> typePrefixArray = new ArrayList<>();
		List<ArrayList<Metric>> lostFunctionsArray = new ArrayList<>();
		
		//output
		String output_prefix = OutputDirectory.ROOT+"tkde/RunOutput/";
		
		//
		for(int i=0; i<versionList.size(); i++){
			DivVersion divVersion = versionList.get(i);
			//--
			String typePrefix = "";
			List<String> qList = TRECDivLoader.getDivEvalQueryIDList(commonIndri, divVersion);
			
			HashMap<String,String> trecDivDocs = TRECDivLoader.loadTrecDivDocs(commonIndri, divVersion);
			trecDivDocsArray.add(trecDivDocs);
			
			Map<String,TRECDivQuery> trecDivQueries = TRECDivLoader.loadTrecDivQueries(commonIndri, divVersion);
			trecDivQueriesArray.add(trecDivQueries);
			
			if(diffFacetedAmbiguous){
				if(acceptFaceted){
					qList = filterDivQuery(qList, trecDivQueries, facetedType);
					typePrefix = "Faceted_";
				}else{
					qList = filterDivQuery(qList, trecDivQueries, ambType);
					typePrefix = "Amb_";
				}
			}
			qListArray.add(qList);
			typePrefixArray.add(typePrefix);
			
			Map<String,TRECQueryAspects> trecDivQueryAspects = TRECDivLoader.loadTrecDivQueryAspects(commonIndri, divVersion);
			trecDivQueryAspectsArray.add(trecDivQueryAspects);			
			
			//check output directory
			File outputFile = new File(output_prefix);
			if(!outputFile.exists()){
				outputFile.mkdirs();
			}
			
			String output_filename = null;
			if(DivVersion.Div2009 == divVersion){			
				output_filename = typePrefix+"Div2009"+rankStrategy.toString();			
			}else if(DivVersion.Div2010 == divVersion){			
				output_filename = typePrefix+"Div2010"+rankStrategy.toString();		
			}else if(DivVersion.Div20092010 == divVersion) {			
				output_filename = typePrefix+"Div20092010"+rankStrategy.toString();			
			}else if(DivVersion.Div2011 == divVersion){
				output_filename = typePrefix+"Div2011"+rankStrategy.toString();
			}else if(DivVersion.Div2012 == divVersion){
				output_filename = typePrefix+"Div2012"+rankStrategy.toString();
			}else{				
				System.out.println("ERROR: unexpected DivVersion!");
				new Exception().printStackTrace();
				System.exit(1);				
			}
			output_filename += ("_"+commonIndri.toString());
			outputNameArray.add(output_filename);
					
			////Build the Loss functions
			ArrayList<Metric> lossFunctions = new ArrayList<Metric>();
			// loss_functions.add(new USLoss());
			// loss_functions.add(new WSLoss());
			// loss_functions.add(new AvgUSLoss());
			// loss_functions.add(new AvgWSLoss());
			lossFunctions.add(new AllUSLoss());
			lossFunctions.add(new AllWSLoss());
			lossFunctions.add(new NDEvalLosses(TRECDivLoader.getTrecDivQREL(divVersion), divVersion));
			//
			lostFunctionsArray.add(lossFunctions);
			//--
		}		
		//-- setting ranking strategy --//				
		if(RankStrategy.XQuAD == rankStrategy){
			
			perLambdaForCrossEval_XQuAD(releProVersion, versionList, cutoffK, output_prefix,
					qListArray, trecDivDocsArray,
					trecDivQueriesArray, trecDivQueryAspectsArray,
					outputNameArray, typePrefixArray, lostFunctionsArray);
			
		}else if(RankStrategy.PM2 == rankStrategy){
			
			perLambdaForCrossEval_PM2(releProVersion, versionList, cutoffK, output_prefix,
					qListArray, trecDivDocsArray,
					trecDivQueriesArray, trecDivQueryAspectsArray,
					outputNameArray, typePrefixArray, lostFunctionsArray);
			
		}else if(RankStrategy.IASelect == rankStrategy){
			for(int id=0; id<versionList.size(); id++){
				List<String> qList = qListArray.get(id);							
				HashMap<String,String> trecDivDocs = trecDivDocsArray.get(id);							
				Map<String,TRECDivQuery> trecDivQueries = trecDivQueriesArray.get(id);
				String typePrefix = typePrefixArray.get(id);
				String output_filename = outputNameArray.get(id);
				//output_filename += ("_"+Evaluator.oneResultFormat.format(lam));
				Map<String,TRECQueryAspects> trecDivQueryAspects = trecDivQueryAspectsArray.get(id);
				ArrayList<Metric> lossFunctions = lostFunctionsArray.get(id);
				
				singleEval_IASelect(releProVersion, output_prefix, output_filename, 
						qList, trecDivQueries, trecDivDocs, trecDivQueryAspects, lossFunctions, cutoffK);
			}
		}else if(RankStrategy.ExpSRD == rankStrategy){
			for(int id=0; id<versionList.size(); id++){
				List<String> qList = qListArray.get(id);							
				HashMap<String,String> trecDivDocs = trecDivDocsArray.get(id);							
				Map<String,TRECDivQuery> trecDivQueries = trecDivQueriesArray.get(id);
				String typePrefix = typePrefixArray.get(id);
				String output_filename = outputNameArray.get(id);
				//output_filename += ("_"+Evaluator.oneResultFormat.format(lam));
				Map<String,TRECQueryAspects> trecDivQueryAspects = trecDivQueryAspectsArray.get(id);
				ArrayList<Metric> lossFunctions = lostFunctionsArray.get(id);
				
				singleEval_ExpSRD(releProVersion, output_prefix, output_filename, 
						qList, trecDivQueries, trecDivDocs, trecDivQueryAspects, lossFunctions, cutoffK);
			}
		}else if(RankStrategy.CommonIndriBaseline == rankStrategy) {
			
			output_prefix += "IndriBaseline";
			File dirFile = new File(output_prefix);
			if(!dirFile.exists()){
				dirFile.mkdirs();
			}
			output_prefix += "/";
			
			for(int id=0; id<versionList.size(); id++){
				
				List<String> qList = qListArray.get(id);							
				HashMap<String,String> trecDivDocs = trecDivDocsArray.get(id);							
				Map<String,TRECDivQuery> trecDivQueries = trecDivQueriesArray.get(id);
				String typePrefix = typePrefixArray.get(id);
				String output_filename = outputNameArray.get(id);
				//output_filename += ("_"+Evaluator.oneResultFormat.format(lam));
				Map<String,TRECQueryAspects> trecDivQueryAspects = trecDivQueryAspectsArray.get(id);
				ArrayList<Metric> lossFunctions = lostFunctionsArray.get(id);
				
				//				
				ArrayList<ResultRanker> rankerList = new ArrayList<ResultRanker>();				
				CommonIndriBaselineRanker comIndriBaselineRanker = new CommonIndriBaselineRanker(versionList.get(id), trecDivDocs);
				
				rankerList.add(comIndriBaselineRanker);
				
				String nameFix = "";
				Evaluator trecDivEvaluator = new TRECDivEvaluator(trecDivQueries, output_prefix, output_filename+nameFix);
				try {
					trecDivEvaluator.doEval(qList, trecDivDocs, trecDivQueryAspects, lossFunctions, rankerList, cutoffK);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	public static void tkde_explicit(){
		//1
		boolean commonIndri = true;
		
		ArrayList<DivVersion> versionList = new ArrayList<>();
		versionList.add(DivVersion.Div2009);
		versionList.add(DivVersion.Div2010);
		versionList.add(DivVersion.Div2011);
		versionList.add(DivVersion.Div2012);
		
		//baseline methods
		////common part		
		
		////(1) running methods
		///*
		//tkdePerLambdaTrecDivEvaluation(commonIndri, ReleProVersion.COS, versionList, RankStrategy.PM2);
		//tkdePerLambdaTrecDivEvaluation(commonIndri, ReleProVersion.LM, versionList, RankStrategy.PM2);
		
		//tkdePerLambdaTrecDivEvaluation(commonIndri, ReleProVersion.COS, versionList, RankStrategy.XQuAD);
		//tkdePerLambdaTrecDivEvaluation(commonIndri, ReleProVersion.LM, versionList, RankStrategy.XQuAD);
		
		//tkdePerLambdaTrecDivEvaluation(commonIndri, ReleProVersion.COS, versionList, RankStrategy.IASelect);
		//tkdePerLambdaTrecDivEvaluation(commonIndri, ReleProVersion.LM, versionList, RankStrategy.IASelect);	
		
		//tkdePerLambdaTrecDivEvaluation(commonIndri, ReleProVersion.BM25, versionList, RankStrategy.ExpSRD);
		//tkdePerLambdaTrecDivEvaluation(commonIndri, ReleProVersion.COS, versionList, RankStrategy.ExpSRD);	
		
		tkdePerLambdaTrecDivEvaluation(commonIndri, null, versionList, RankStrategy.CommonIndriBaseline);
		//*/
		
		////(2) cross validation
		/*
		String dir = OutputDirectory.ROOT+"tkde/RunOutput/";
		
		crossValidation(versionList, RankStrategy.XQuAD, ReleProVersion.LM, "TerrierKernel", dir);
		crossValidation(versionList, RankStrategy.XQuAD, ReleProVersion.COS, "TFIDF_A1", dir);
		crossValidation(versionList, RankStrategy.PM2, ReleProVersion.LM, "TerrierKernel", dir);
		crossValidation(versionList, RankStrategy.PM2, ReleProVersion.COS, "TFIDF_A1", dir);
		*/
	}
	
	public static void tkdeCrossResults(ArrayList<DivVersion> versionList, ArrayList<Float> paraList, 
			RankStrategy rankStrategy, ReleProVersion releProVersion, String kernelStr, String dir){		
		////
		if(null != paraList){
			dir += (rankStrategy.toString()+"_"+releProVersion.toString()+"\\");
			for(int i=0; i<versionList.size(); i++){
				DivVersion divVersion = versionList.get(i);
				float para = paraList.get(i);
				
				String avgFile = divVersion.toString()+rankStrategy.toString()+"_true"
						+"_"+Evaluator.oneResultFormat.format(para)
						+"_"+kernelStr+"_ndeval.avg.txt";
				DivResult avgResult = ResultAnalyzer.loadAvgDivResult(dir+avgFile);
				System.out.println(avgResult.nERRIA10.get(0)+"\t"+avgResult.nERRIA20.get(0)+"\t"+avgResult.alphanDCG10.get(0)
						+"\t"+avgResult.alphanDCG20.get(0)+"\t"+avgResult.nNRBP20.get(0));
			}
		}else if(rankStrategy == RankStrategy.CommonIndriBaseline){
			dir += ("IndriBaseline\\");
			for(int i=0; i<versionList.size(); i++){
				DivVersion divVersion = versionList.get(i);
				
				String avgFile = divVersion.toString()+rankStrategy.toString()+"_true"
						+"_ndeval.avg.txt";
				DivResult avgResult = ResultAnalyzer.loadAvgDivResult(dir+avgFile);
				System.out.println(avgResult.nERRIA10.get(0)+"\t"+avgResult.nERRIA20.get(0)+"\t"+avgResult.alphanDCG10.get(0)
						+"\t"+avgResult.alphanDCG20.get(0)+"\t"+avgResult.nNRBP20.get(0));
			}
		}else if(rankStrategy == RankStrategy.ExpSRD){
			dir += ("E-MP_"+releProVersion.toString()+"\\");
			for(int i=0; i<versionList.size(); i++){
				DivVersion divVersion = versionList.get(i);
				//float para = paraList.get(i);
				
				String avgFile = divVersion.toString()+rankStrategy.toString()+"_true"
						+"_"+kernelStr+"_Y_Belief_ndeval.avg.txt";
				DivResult avgResult = ResultAnalyzer.loadAvgDivResult(dir+avgFile);
				System.out.println(avgResult.nERRIA10.get(0)+"\t"+avgResult.nERRIA20.get(0)+"\t"+avgResult.alphanDCG10.get(0)
						+"\t"+avgResult.alphanDCG20.get(0)+"\t"+avgResult.nNRBP20.get(0));
			}
		}else{
			dir += (rankStrategy.toString()+"_"+releProVersion.toString()+"\\");
			for(int i=0; i<versionList.size(); i++){
				DivVersion divVersion = versionList.get(i);
				//float para = paraList.get(i);
				
				String avgFile = divVersion.toString()+rankStrategy.toString()+"_true"
						+"_"+kernelStr+"_ndeval.avg.txt";
				DivResult avgResult = ResultAnalyzer.loadAvgDivResult(dir+avgFile);
				System.out.println(avgResult.nERRIA10.get(0)+"\t"+avgResult.nERRIA20.get(0)+"\t"+avgResult.alphanDCG10.get(0)
						+"\t"+avgResult.alphanDCG20.get(0)+"\t"+avgResult.nNRBP20.get(0));
			}
		}				
	}
	
	public static void tkdeLoadCrossResults(){		
		
		//1 cross-validation
		ArrayList<DivVersion> versionList = new ArrayList<>();
		versionList.add(DivVersion.Div2009);
		versionList.add(DivVersion.Div2010);
		versionList.add(DivVersion.Div2011);
		versionList.add(DivVersion.Div2012);
				
		String dir = OutputDirectory.ROOT+"tkde/RunOutput/";
		
		////baseline
		System.out.println("CommonIndriBaseline");
		tkdeCrossResults(versionList, null, RankStrategy.CommonIndriBaseline, null, null, dir);
		System.out.println();
		////XQuAD_LM
		//Div2009	 optimal lambda=1.0
		//Div2010	 optimal lambda=1.0
		//Div2011	 optimal lambda=1.0
		//Div2012	 optimal lambda=1.0
		ArrayList<Float> XQuAD_lm_paraList = new ArrayList<>();
		XQuAD_lm_paraList.add(1.0f);
		XQuAD_lm_paraList.add(1.0f);
		XQuAD_lm_paraList.add(1.0f);
		XQuAD_lm_paraList.add(1.0f);
		System.out.println("XQuAD_lm");
		tkdeCrossResults(versionList, XQuAD_lm_paraList, RankStrategy.XQuAD, ReleProVersion.LM, "TerrierKernel", dir);
		System.out.println();
		
		////XQuAD_COS
		//Div2009	 optimal lambda=0.0
		//Div2010	 optimal lambda=0.0
		//Div2011	 optimal lambda=0.0
		//Div2012	 optimal lambda=0.0
		ArrayList<Float> XQuAD_COS_paraList = new ArrayList<>();
		XQuAD_COS_paraList.add(0.0f);
		XQuAD_COS_paraList.add(0.0f);
		XQuAD_COS_paraList.add(0.0f);
		XQuAD_COS_paraList.add(0.0f);
		System.out.println("XQuAD_COS");
		tkdeCrossResults(versionList, XQuAD_COS_paraList, RankStrategy.XQuAD, ReleProVersion.COS, "TFIDF_A1", dir);
		System.out.println();
		////PM2_LM
		//Div2009	 optimal lambda=1.0
		//Div2010	 optimal lambda=0.4
		//Div2011	 optimal lambda=0.0
		//Div2012	 optimal lambda=1.0
		ArrayList<Float> pm2_lm_paraList = new ArrayList<>();
		pm2_lm_paraList.add(1.0f);
		pm2_lm_paraList.add(0.4f);
		pm2_lm_paraList.add(0.0f);
		pm2_lm_paraList.add(1.0f);
		System.out.println("PM2_LM");
		tkdeCrossResults(versionList, pm2_lm_paraList, RankStrategy.PM2, ReleProVersion.LM, "TerrierKernel", dir);
		System.out.println();
		////PM2_COS
		//Div2009	 optimal lambda=0.2
		//Div2010	 optimal lambda=0.6
		//Div2011	 optimal lambda=0.9
		//Div2012	 optimal lambda=0.2
		ArrayList<Float> pm2_cos_paraList = new ArrayList<>();
		pm2_cos_paraList.add(0.2f);
		pm2_cos_paraList.add(0.6f);
		pm2_cos_paraList.add(0.9f);
		pm2_cos_paraList.add(0.2f);
		System.out.println("PM2_COS");
		tkdeCrossResults(versionList, pm2_cos_paraList, RankStrategy.PM2, ReleProVersion.COS, "TFIDF_A1", dir);
		System.out.println();
		
		////ia-select
		System.out.println("IA-Select-LM");
		tkdeCrossResults(versionList, null, RankStrategy.IASelect, ReleProVersion.LM, "TerrierKernel", dir);
		System.out.println();
		System.out.println("IA-Select-COS");
		tkdeCrossResults(versionList, null, RankStrategy.IASelect, ReleProVersion.COS, "TFIDF_A1", dir);
		System.out.println();
		////e-mp
		System.out.println("E-MP");
		tkdeCrossResults(versionList, null, RankStrategy.ExpSRD, ReleProVersion.BM25, "BM25Kernel_A1", dir);
		System.out.println();
	}
	
	public static void crossValidation(ArrayList<DivVersion> versionList, RankStrategy rankStrategy, 
			ReleProVersion releProVersion, String kernelStr, String dir){
		
		ArrayList<ArrayList<DivResult>> perLamAvgPerformanceList = new ArrayList<>();
		
		/*
		output_filename = typePrefix+"Div2009"+rankStrategy.toString();
		//output_prefix += ("XQuAD_"+releProVersion.toString());
		output_filename += ("_"+Evaluator.oneResultFormat.format(lam));
		output_filename += ("_"+usedKernel.getString());
		*/
		
		dir += (rankStrategy.toString()+"_"+releProVersion.toString()+"\\");
		
		for(DivVersion divVersion: versionList){
			ArrayList<DivResult> perLamAvgPerformance = new ArrayList<>();
			double span = 0.1;
			for(double lam = 0.0; lam<=1.0; lam+= span){
				String fileName = divVersion.toString()+rankStrategy.toString()+"_true"
						+"_"+Evaluator.oneResultFormat.format(lam)
						+"_"+kernelStr+"_ndeval.avg.txt";
				
				DivResult divResult = ResultAnalyzer.loadAvgDivResult(dir+fileName);
				
				//System.out.println(fileName);
				//System.out.println(divResult.toString());
				
				perLamAvgPerformance.add(divResult);
			}	
			
			perLamAvgPerformanceList.add(perLamAvgPerformance);
		}
		
		ArrayList<Integer> testIDList = new ArrayList<>();
		for(int i=0; i<versionList.size(); i++){
			testIDList.add(i);
		}
		
		ArrayList<Float> paraList = new ArrayList<>();
		paraList.add(0.0f);paraList.add(0.1f);paraList.add(0.2f);paraList.add(0.3f);paraList.add(0.4f);
		paraList.add(0.5f);paraList.add(0.6f);paraList.add(0.7f);paraList.add(0.8f);paraList.add(0.9f);paraList.add(1.0f);
		
		System.out.println(rankStrategy.toString()+"_"+releProVersion.toString());
		for(Integer testID: testIDList){
			
			float maxLambda = Float.NaN;
			float maxAvgsum = Float.MIN_VALUE;
			
			//double span = 0.1;
			for(int paraI=0; paraI<paraList.size(); paraI++){
				//System.out.println(paraList.get(paraI));
				//int index = (int)(lam*10);
				//System.out.println(paraI);
				
				float avgSum = 0.0f;				
				for(int i=0; i<versionList.size(); i++){
					if(i != testID){
						ArrayList<DivResult> perLamAvgPerformance = perLamAvgPerformanceList.get(i);
						avgSum += perLamAvgPerformance.get(paraI).nERRIA20.get(0);
					}					
				}
				
				System.out.println(testID+"\t"+paraI+"\t"+avgSum);
				
				if(avgSum > maxAvgsum){
					maxAvgsum = avgSum;
					maxLambda = paraList.get(paraI);
				}				
			}
			
			//
			System.out.println(versionList.get(testID)+"\t optimal lambda="+maxLambda);	
			//
		}		
		System.out.println();
	}
	
	//generate queries w.r.t. faceted / ambiguous 
	private static void generateQList(){
		boolean commonIndri = true;
		ArrayList<DivVersion> versionList = new ArrayList<>();
		versionList.add(DivVersion.Div2009);
		versionList.add(DivVersion.Div2010);
		
		try {
			BufferedWriter writer = IOText.getBufferedWriter_UTF8(OutputDirectory.ROOT+"sigir2016/Trec20092010QueriesWithType.txt");
			writer.write("ID\tType\tTopicString");
			writer.newLine();
			
			for(DivVersion divVersion: versionList){
				List<String> qList = TRECDivLoader.getDivEvalQueryIDList(commonIndri, divVersion);
				Map<String,TRECDivQuery> trecDivQueries = TRECDivLoader.loadTrecDivQueries(commonIndri, divVersion);
				
				for(String q: qList){
					writer.write(trecDivQueries.get(q)._number
							+"\t"+trecDivQueries.get(q)._type.toString().trim()
							+"\t"+trecDivQueries.get(q)._title);
					writer.newLine();
				}
			}
			writer.flush();
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	//
	private static void generateBaselineRanking(boolean commonIndri, DivVersion divVersion){
		HashMap<String, ArrayList<String>> baselineMap = TRECDivLoader.loadTRECDivBaseline(divVersion);
		List<String> qList = TRECDivLoader.getDivEvalQueryIDList(commonIndri, divVersion);
		
		try {
			BufferedWriter writer = IOText.getBufferedWriter_UTF8(OutputDirectory.ROOT+"sigir2016/RunOutput/Baseline_"+divVersion.toString()+".txt");
			for(String q: qList){
				ArrayList<String> docList = baselineMap.get(q);
				for(String doc: docList){
					writer.write(q+"\t"+doc);
					writer.newLine();
				}
			}
			writer.flush();
			writer.close();
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}
		
	public static void main(String []args){
		
		////1
		/*
		//DivVersion divVersion
		//RankStrategy rankStrategy
		
		//TRECDivEvaluation.openPrinter();		
		
		boolean commonIndri = false;
		TRECDivEvaluation.trecDivEvaluation(commonIndri, DivVersion.Div2009, RankStrategy.FL);
		
		//TRECDivEvaluation.closePrinter();
		*/
		
		
		////	-2009-	////
		//(1) implicit SRD
		//boolean commonIndri = false;
		//TRECDivEvaluation.trecDivEvaluation(commonIndri, DivVersion.Div2009, RankStrategy.ImpSRD);
		
		//(2) explicit SRD
		//boolean commonIndri = false;
		//TRECDivEvaluation.trecDivEvaluation(commonIndri, DivVersion.Div2009, RankStrategy.ExpSRD);
		
		////	-2010-	////
		
		//(2) explicit SRD
		//boolean commonIndri = true;
		//TRECDivEvaluation.trecDivEvaluation(commonIndri, DivVersion.Div2010, RankStrategy.ExpSRD);
		
		//(3) temporal baseline
		//boolean commonIndri = true;
		//TRECDivEvaluation.trecDivEvaluation(commonIndri, DivVersion.Div2010, RankStrategy.Tem);
		
		////	-2011-	////
		//(2) explicit SRD
		//boolean commonIndri = true;
		//TRECDivEvaluation.trecDivEvaluation(commonIndri, DivVersion.Div2011, RankStrategy.ExpSRD);
		
		//(3) temporal baseline
		//boolean commonIndri = true;
		//TRECDivEvaluation.trecDivEvaluation(commonIndri, DivVersion.Div2011, RankStrategy.Tem);
		
		////	-2012-	////
		//(2) explicit SRD
		//boolean commonIndri = true;
		//TRECDivEvaluation.trecDivEvaluation(commonIndri, DivVersion.Div2012, RankStrategy.ExpSRD);
		
		//(3) temporal baseline
		//boolean commonIndri = false;
		//TRECDivEvaluation.trecDivEvaluation(commonIndri, DivVersion.Div2010, RankStrategy.PM2);
		
		
		/////	sigir	////
		TRECDivEvaluation.sigir2016();
		
		/////cross
		/*
		boolean commonIndri = true;
		ArrayList<DivVersion> versionList = new ArrayList<>();
		versionList.add(DivVersion.Div2009);
		versionList.add(DivVersion.Div2010);
		TRECDivEvaluation.crossTrecDivEvaluation(commonIndri, versionList, RankStrategy.ImpSRD);
		*/	
		
		
		////	tkde	////
		//TRECDivEvaluation.tkde_explicit();		
		//TRECDivEvaluation.tkdeLoadCrossResults();
		
		
		//TRECDivEvaluation.generateQList();
		
		////generate baseline ranking
		//TRECDivEvaluation.generateBaselineRanking(true, DivVersion.Div2009);
		//TRECDivEvaluation.generateBaselineRanking(true, DivVersion.Div2010);
	}
}
