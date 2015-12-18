package org.archive.nicta.evaluation.evaluator;

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.archive.dataset.trec.query.TREC68Query;
import org.archive.dataset.trec.query.TRECQueryAspects;
import org.archive.nicta.evaluation.metricfunction.AllUSLoss;
import org.archive.nicta.evaluation.metricfunction.AllWSLoss;
import org.archive.nicta.evaluation.metricfunction.Metric;
import org.archive.nicta.evaluation.metricfunction.NDEvalLosses;
import org.archive.nicta.ranker.ResultRanker;
import org.archive.util.DevNullPrintStream;
import org.archive.util.VectorUtils;

public class TREC68Evaluator extends Evaluator {
	
	private Map<String,TREC68Query> _allTREC68Queries = null; 
	
	public TREC68Evaluator(Map<String,TREC68Query>allTREC68Queries, String outputPrefix, String outputFilename){
		super(outputPrefix, outputFilename);
		this._allTREC68Queries = allTREC68Queries;
	}	
	
	public void openPrintStreams(){
		try {
			this.ps_per_SLoss  = new PrintStream(new FileOutputStream(_outputPrefix + _outputFilename + ".txt"));
			this.ps_avg_SLoss = new PrintStream(new FileOutputStream(_outputPrefix + _outputFilename + ".avg.txt"));
			this.ps_per_Ndeval = new PrintStream(new FileOutputStream(_outputPrefix + _outputFilename + "_ndeval.txt"));
			this.ps_avg_Ndeval = new PrintStream(new FileOutputStream(_outputPrefix + _outputFilename + "_ndeval.avg.txt"));
			this.err = new DevNullPrintStream(); //new PrintStream(new FileOutputStream(PATH_PREFIX + output_filename + ".errors.txt"));
			
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}
	
	public void closePrintStreams(){
		try {
			this.ps_per_SLoss.flush();
			this.ps_per_SLoss.close();
			
			this.ps_avg_SLoss.flush();
			this.ps_avg_SLoss.close();
			
			this.ps_per_Ndeval.flush();
			this.ps_per_Ndeval.close();
			
			this.ps_avg_Ndeval.flush();
			this.ps_avg_Ndeval.close();	
			
			this.err.flush();
			this.err.close();
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}
	
	// TODO: Need to optimize number of topics
	// TODO: Verify consistency of rankers when used multiple times with clearDocs
	// TODO: Need to do code profiling, can improve code with caching (e.g., similarity metrics, LDA)	
	
	public ArrayList<Double> doEval(
			List<String> evalQueries, 
			HashMap<String,String> allDocs, 			 
			Map<String,TRECQueryAspects> stdTRECQueryAspects,
			List<Metric> lossFunctions,
			List<ResultRanker> resultRankers,
			int cutoffK) throws Exception {
		//
		this.openPrintStreams();		
		// Loop:
		// - go through each test (alg) t (a variant of MMR)
		//     - go through all queries q
		//        - add docs to ranker rRanker for q
		//        - get result list for query q on ranker rRanker
		//            - go through all loss functions l
		//                - evaluate loss
		
		//int ranker_index = 1;
		for (ResultRanker rRanker : resultRankers) {
			
			String rankerString = rRanker.getString();
			
			if (DEBUG)
				System.out.println("- Processing test '" + rRanker.getDescription() + "'");

			// Maintain average US and WSL vectors
			double[] usl_vs_rank = new double[cutoffK];
			double[] wsl_vs_rank = new double[cutoffK];
			double[] ndeval = null;
			
			// Build a cache for reuse of top-docs
			//HashMap<String, HashSet<String>> top_docs = new HashMap<String, HashSet<String>>();
			
			int query_serial = 0;
			for (String qNumber : evalQueries) {
				///////////////////////////////////////////////////////////////////////
				// For a test and a query
				
				// Get query relevant info
				++query_serial;
				TREC68Query trec68Query = _allTREC68Queries.get(qNumber);
				TRECQueryAspects trecQueryAspects = stdTRECQueryAspects.get(qNumber);
				
				if (DEBUG) {
					System.out.println("- Processing query '" + qNumber + "'");
					System.out.println("- Query details: " + trec68Query);
					//System.out.println("- Query aspects: " + qa);
				}

				// Add docs for query to test
				rRanker.clearInfoOfTopNDocs();
				//Set<String> relevant_docs = qa.getRelevantDocs();
				Set<String> topnDocs = trecQueryAspects.getTopNDocs();

				if (DEBUG)
					System.out.println("- Evaluating with " + topnDocs.size() + " docs");
				
				for (String doc_name : topnDocs) {
					if (!allDocs.containsKey(doc_name))
						err.println("ERROR: '" + doc_name + "' not found for '" + qNumber + "'");
					else {
						rRanker.addATopNDoc(doc_name);
						//if (DEBUG)
						//	System.out.println("- [" + query + "] Adding " + doc_name);
					}
				}
				
				// Get the results
				if (DEBUG)
					System.out.println("- Running alg: " + rRanker.getDescription());
				
				ArrayList<String> resultList = rRanker.getResultList(trec68Query.getQueryContent(), cutoffK);
				
				if (DEBUG)
					System.out.println("- Result list: " + resultList);
				
				// Evaluate all loss functions on the results
				for (Metric loss : lossFunctions) {
					String loss_name = loss.getName();
					System.out.println("Evaluating: " + loss_name);
					Object o = loss.eval(trecQueryAspects, resultList);
					String loss_result_str = null;
					if (o instanceof double[]) {
						loss_result_str = VectorUtils.GetString((double[])o);
					} else {
						loss_result_str = o.toString();
					}
					
					// Display results to screen for now
					System.out.println("==================================================");
					System.out.println("Query: " + trec68Query._number + " -> " + trec68Query.getQueryContent());
					System.out.println("MMR Alg: " + rRanker.getDescription());
					System.out.println("Loss Function: " + loss.getName());
					System.out.println("Evaluation: " + loss_result_str);
					
					// Maintain averages and export
					if (loss instanceof AllUSLoss) {
						usl_vs_rank = VectorUtils.Sum(usl_vs_rank, (double[])o);
						export(ps_per_SLoss, "q-"+serialFormat.format(query_serial), rankerString, "USL", (double[])o, (String [])loss.getMetricArray());
					}
					if (loss instanceof AllWSLoss) {
						wsl_vs_rank = VectorUtils.Sum(wsl_vs_rank, (double[])o);
						export(ps_per_SLoss, "q-"+serialFormat.format(query_serial), rankerString, "WSL", (double[])o, (String [])loss.getMetricArray());
					}
					if (loss instanceof NDEvalLosses) {
						if (ndeval == null) {
							ndeval = new double[((double[])o).length];
						}	
						ndeval = VectorUtils.Sum(ndeval, (double[])o);
						export(ps_per_Ndeval, "q-"+serialFormat.format(query_serial), rankerString, "NDEval10\n", (double[])o, (String [])loss.getMetricArray());
					}
					ps_per_SLoss.flush();
					ps_per_Ndeval.flush();
				}				
				///////////////////////////////////////////////////////////////////////
			}
			rRanker.clearInfoOfTopNDocs();
			
			usl_vs_rank = VectorUtils.ScalarMultiply(usl_vs_rank, 1d/evalQueries.size());
			wsl_vs_rank = VectorUtils.ScalarMultiply(wsl_vs_rank, 1d/evalQueries.size());
			ndeval =      VectorUtils.ScalarMultiply(ndeval, 1d/evalQueries.size());
			
			System.out.println("==================================================");
			System.out.println("Exporting " + rankerString + ": " + rRanker.getDescription());
			//
			export(ps_avg_SLoss, "Mean", rankerString+"\t", "USL\n", usl_vs_rank, (String [])lossFunctions.get(0).getMetricArray());
			export(ps_avg_SLoss, "Mean", rankerString+"\t", "WSL\n", wsl_vs_rank, (String [])lossFunctions.get(1).getMetricArray());
			export(ps_avg_Ndeval, "Mean", rankerString+"\t", "NDEval10\n", ndeval, (String [])lossFunctions.get(2).getMetricArray());			
		}		
		//
		this.closePrintStreams();
		
		return null;
	}
}
