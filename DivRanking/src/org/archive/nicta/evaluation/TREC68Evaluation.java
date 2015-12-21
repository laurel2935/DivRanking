package org.archive.nicta.evaluation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Vector;

import org.archive.a1.kernel.SBKernel;
import org.archive.a1.ranker.fa.MDP;
import org.archive.a1.ranker.fa.MDP.fVersion;
import org.archive.dataset.trec.TREC68Loader;
import org.archive.dataset.trec.query.TREC68Query;
import org.archive.dataset.trec.query.TRECQueryAspects;
import org.archive.nicta.evaluation.metricfunction.AllUSLoss;
import org.archive.nicta.evaluation.metricfunction.AllWSLoss;
import org.archive.nicta.evaluation.metricfunction.Metric;
import org.archive.nicta.evaluation.metricfunction.NDEvalLosses;
import org.archive.nicta.kernel.*;
import org.archive.nicta.ranker.ResultRanker;



public class TREC68Evaluation {
	
	//
	private static void trec68Evaluation(){
		
		String output_prefix = "results/trec68/";
		
		String output_filename = "Trec68";
		
		int cutoffK = 20;
		
		HashMap<String,String> trec68Docs = TREC68Loader.loadTrec68Docs();		
		HashMap<String,TREC68Query> trec68Queries = TREC68Loader.loadTrec68Queries();		
		HashMap<String,TRECQueryAspects> trec68QueryAspects = TREC68Loader.loadTrec68QAspects();		
		
		// Build the Loss functions
		ArrayList<Metric> lossFunctions = new ArrayList<Metric>();
		//loss_functions.add(new USLoss());
		//loss_functions.add(new WSLoss());
		//loss_functions.add(new AvgUSLoss());
		//loss_functions.add(new AvgWSLoss());
		lossFunctions.add(new AllUSLoss());
		lossFunctions.add(new AllWSLoss());
		lossFunctions.add(new NDEvalLosses(TREC68Loader.getTrec68QREL(), null));
		
		// Build the TREC tests
		// Build a new result list selectors... all use the greedy MMR approach,
		// each simply selects a different similarity metric
		ArrayList<ResultRanker> rankers = new ArrayList<ResultRanker>();
		
		// Instantiate all the kernels that we will use with the algorithms below
		//////////////////////
		//TF-Kernel
		//////////////////////
		/*
		Kernel TF_kernel    = new TF(trec68Docs, 
				true //query-relevant diversity
				);
		Kernel TFn_kernel    = new TF(trec68Docs, 
				false //query-relevant diversity
				);
		//-----
		rankers.add( new MMR( trec68Docs, 
				0.5d //lambda: 0d is all weight on query sim
				, TF_kernel // sim 
				, TF_kernel // div 
				));
		rankers.add( new MMR( trec68Docs, 
				0.5d //lambda: 0d is all weight on query sim
				, TFn_kernel // sim
				, TFn_kernel // div
				));
		*/
		////////////////////////////
		//TFIDF_kernel
		////////////////////////////
		/*
		Kernel TFIDF_kernel = new TFIDF(trec68Docs,
				true //query-relevant diversity
				);
		Kernel TFIDFn_kernel = new TFIDF(trec68Docs, 
				false // query-relevant diversity
				);
		//-----
		rankers.add( new MMR( trec68Docs, 
		0.5d //lambda: 0d is all weight on query sim
		, TFIDF_kernel // sim
		, TFIDF_kernel // div
		));
		rankers.add( new MMR( trec68Docs, 
				0.5d //lambda: 0d is all weight on query sim
				, TFIDFn_kernel //sim
				, TFIDFn_kernel //div
				));
		*/
		//////////////////////////
		//LDA-Kernel
		//////////////////////////		
		//check result
		Kernel PLSR_TFIDF_kernel = new PLSRKernelTFIDF(trec68Docs);
		//
		Kernel PLSR10_kernel  = new PLSRKernel(trec68Docs
				, 10 // NUM TOPICS - suggest 15 */
				, false // spherical */
				);
		Kernel PLSR15_kernel  = new PLSRKernel(trec68Docs
				, 15 // NUM TOPICS - suggest 15 */
				, 2.0
				, 0.5
				, false // spherical */
				);
		Kernel PLSR15_sph_kernel  = new PLSRKernel(trec68Docs
				, 15 // NUM TOPICS - suggest 15 */
				, true // spherical */
				);
		Kernel PLSR20_kernel  = new PLSRKernel(trec68Docs
				, 20 // NUM TOPICS - suggest 15 */
				, false // spherical */
				);
		
		//-----
		/*
		rankers.add( new MMR( trec68Docs, 
				0.5d //lambda: 0d is all weight on query sim
				, PLSR_TFIDF_kernel //sim
				, PLSR_TFIDF_kernel //div
				));
		rankers.add( new MMR( trec68Docs, 
				0.5d //lambda: 0d is all weight on query sim
				, PLSR15_kernel //sim
				, PLSR15_kernel //div
				));
		*/
		
		//---------		
		Kernel LDA10_kernel   = new LDAKernel(trec68Docs, 
				10 // NUM TOPICS - suggest 15 */
				, false // spherical */
				, false // query-relevant diversity */
				);
		
		Kernel LDA15_kernel   = new LDAKernel(trec68Docs
				, 15 // NUM TOPICS - suggest 15
				, false // spherical
				, false // query-relevant diversity
				);

		Kernel LDA15_sph_kernel   = new LDAKernel(trec68Docs
				, 15 // NUM TOPICS - suggest 15 */
				, true // spherical */
				, false // query-relevant diversity */
				);
		Kernel LDA15_qr_kernel   = new LDAKernel(trec68Docs
				, 15 // NUM TOPICS - suggest 15 */
				, false // spherical */
				, true // query-relevant diversity */
				);
		Kernel LDA15_qr_sph_kernel   = new LDAKernel(trec68Docs
				, 15 // NUM TOPICS - suggest 15 */
				, true // spherical */
				, true // query-relevant diversity */
				);
		
		Kernel LDA20_kernel   = new LDAKernel(trec68Docs
				, 20 // NUM TOPICS - suggest 15 */
				, false // spherical */
				, false // query-relevant diversity */
				);
		
		Kernel BM25_kernel  = new BM25Kernel(trec68Docs 
				/* 0 for any disables effect */
				, 0.5d // k1 - doc TF */
				, 0.5d // k3 - query TF */
				, 0.5d // b - doc length penalty */
				);
		//
		/*
		rankers.add( new MMR( trec68Docs, 
				0.5d //lambda: 0d is all weight on query sim
				, LDA10_kernel //sim
				, LDA10_kernel //div
				));
		*/
		
		// Add all MMR test variants (vary lambda and kernels)
		//2
		
		///////////////////////////////
		//MDP based evaluation
		///////////////////////////////
		SBKernel _sbKernel = new SBKernel(trec68Docs, 5);
		TFIDF_A1 disKernel = new TFIDF_A1(trec68Docs, false);
		
		int itrThreshold = 10000;
		MDP mdp = new MDP(trec68Docs, 0.5d, itrThreshold, _sbKernel, disKernel, trec68Queries, null);
		Vector<fVersion> mdpRuns = new Vector<MDP.fVersion>();
		mdpRuns.add(fVersion._dfa);
		mdpRuns.add(fVersion._dfa_scaled);
		mdpRuns.add(fVersion._md);
		mdpRuns.add(fVersion._md_scaled);
		mdpRuns.add(fVersion._pdfa);
		mdpRuns.add(fVersion._pdfa_scaled);
		mdpRuns.add(fVersion._pdfa_scaled_exp);
		mdpRuns.add(fVersion._pdfa_scaled_exp_head);					
		// Evaluate results of different query processing algorithms
		try {
			mdp.doEval(TREC68Loader.getTrec68EvalQueries(), trec68Docs, trec68QueryAspects,
					lossFunctions, cutoffK, output_prefix, output_filename, mdpRuns.toArray(new fVersion[0]));
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}		
	}

	//
	public static void main(String []args){
		TREC68Evaluation.trec68Evaluation();
	}
}
