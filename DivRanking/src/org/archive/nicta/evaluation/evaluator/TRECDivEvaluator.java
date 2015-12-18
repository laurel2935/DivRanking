package org.archive.nicta.evaluation.evaluator;

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.Vector;

import org.archive.OutputDirectory;
import org.archive.dataset.trec.TRECDivLoader;
import org.archive.dataset.trec.TRECDivLoader.DivVersion;
import org.archive.dataset.trec.query.TRECDivQuery;
import org.archive.dataset.trec.query.TRECQueryAspects;
import org.archive.dataset.trec.query.TRECSubtopic;
import org.archive.nicta.evaluation.metricfunction.AllUSLoss;
import org.archive.nicta.evaluation.metricfunction.AllWSLoss;
import org.archive.nicta.evaluation.metricfunction.MSnDCG;
import org.archive.nicta.evaluation.metricfunction.Metric;
import org.archive.nicta.evaluation.metricfunction.NDEvalLosses;
import org.archive.nicta.evaluation.metricfunction.SubtopicRecall;
import org.archive.nicta.kernel.BM25Kernel;
import org.archive.nicta.kernel.BM25Kernel_A1;
import org.archive.nicta.kernel.Kernel;
import org.archive.nicta.kernel.LDAKernel;
import org.archive.nicta.kernel.TFIDF;
import org.archive.nicta.kernel.TFIDF_A1;
import org.archive.nicta.ranker.ResultRanker;
import org.archive.util.DevNullPrintStream;
import org.archive.util.VectorUtils;
import org.archive.util.tuple.PairComparatorBySecond_Desc;
import org.archive.util.tuple.StrDouble;

public class TRECDivEvaluator extends Evaluator{
	
	private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss:SS");  
	
	private Map<String,TRECDivQuery> _allTRECDivQueries = null; 
	
	public TRECDivEvaluator(Map<String,TRECDivQuery> allTRECDivQueries, String outputPrefix, String outputFilename){
		super(outputPrefix, outputFilename);
		this._allTRECDivQueries = allTRECDivQueries;
		
		//for computing the times
		this.timeFormat = new SimpleDateFormat("HH:mm:ss:SS");  
        TimeZone timeZone = this.timeFormat.getTimeZone();  
        timeZone.setRawOffset(0);  
        this.timeFormat.setTimeZone(timeZone); 
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
	
	public ArrayList<Double> doEval(List<String> evalQueries, HashMap<String,String> allDocs, Map<String,TRECQueryAspects> stdTRECQueryAspects,
			List<Metric> lossFunctions, List<ResultRanker> resultRankers, int cutoffK) throws Exception {
		
		this.openPrintStreams();		
		// Loop:
		// - go through each test (alg) t (a variant of MMR)
		//     - go through all queries q
		//        - add docs to ranker rRanker for q
		//        - get result list for query q on ranker rRanker
		//            - go through all loss functions l
		//                - evaluate loss
		
		//int ranker_index = 1;
		ArrayList<Double> avgPerRankerList = new ArrayList<>();
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
			
			Long startTime = System.currentTimeMillis();
			
			for (String qNumber : evalQueries) {
				///////////////////////////////////////////////////////////////////////
				// For a test and a query
				
				// Get query relevant info
				++query_serial;
				
				TRECDivQuery trecDivQuery = _allTRECDivQueries.get(qNumber);
				System.out.println(_allTRECDivQueries.keySet());
				TRECQueryAspects trecQueryAspects = stdTRECQueryAspects.get(qNumber);
				
				if (DEBUG) {
					System.out.println("- Processing query '" + qNumber + "'");
					//System.out.println("- Query details: " + trecDivQuery);
					//System.out.println("- Query aspects: " + qa);
				}
				
				rRanker.clearInfoOfTopNDocs();				
				Set<String> topnDocs = trecQueryAspects.getTopNDocs();

				if (DEBUG){
					System.out.println("- Evaluating with " + topnDocs.size() + " docs");
				}					
				
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
				if (DEBUG){
					System.out.println("- Running alg: " + rRanker.getDescription());
				}					
				
				ArrayList<String> resultList = null;
				if(0 == rRanker._indexOfGetResultMethod){
					resultList = rRanker.getResultList(trecDivQuery.getQueryContent(), cutoffK);
				}else{
					resultList = rRanker.getResultList(trecDivQuery, cutoffK);
				}
				
				if(null == resultList){
					continue;
				}
				
				if (DEBUG){
					System.out.println("- Result list: " + resultList);
				}					
				
				// Evaluate all loss functions on the results
				
				for (Metric loss : lossFunctions) {
					String loss_name = loss.getName();
					
					System.out.println("Evaluating: " + loss_name);
					
					Object o = null;
					try {
						o = loss.eval(trecQueryAspects, resultList);						
					} catch (Exception e) {						
						e.printStackTrace();
					}
					
					String loss_result_str = null;
					if (o instanceof double[]) {
						loss_result_str = VectorUtils.GetString((double[])o);
					} else {
						if(null == o){
							System.out.println("null o!");
						}
						loss_result_str = o.toString();
					}
					
					// Display results to screen for now
					System.out.println("==================================================");
					System.out.println("Query: " + trecDivQuery._number + " -> " + trecDivQuery.getQueryContent());
					System.out.println("Result Ranker Alg: " + rRanker.getDescription());
					System.out.println("Loss Function: " + loss.getName());
					System.out.println("Evaluation: " + loss_result_str);
					
					// Maintain averages and export
					if (loss instanceof AllUSLoss) {
						usl_vs_rank = VectorUtils.Sum(usl_vs_rank, (double[])o);
						export(ps_per_SLoss, qNumber, rankerString, "USL", (double[])o, (String [])loss.getMetricArray());
					}
					if (loss instanceof AllWSLoss) {
						wsl_vs_rank = VectorUtils.Sum(wsl_vs_rank, (double[])o);
						export(ps_per_SLoss, qNumber, rankerString, "WSL", (double[])o, (String [])loss.getMetricArray());
					}
					if (loss instanceof NDEvalLosses) {
						if (ndeval == null) {
							ndeval = new double[((double[])o).length];
						}	
						ndeval = VectorUtils.Sum(ndeval, (double[])o);
						export(ps_per_Ndeval, qNumber, rankerString, loss.getName(), (double[])o, (String [])loss.getMetricArray());
					}
					ps_per_SLoss.flush();
					ps_per_Ndeval.flush();
				}				
				///////////////////////////////////////////////////////////////////////
			}
			
			Long endTime = System.currentTimeMillis();  
			System.out.println();
			System.out.println("Time Info ------------------------");
			System.out.println("Time Eclapsed:\t" + timeFormat.format(new Date(endTime - startTime)));
			System.out.println("Topic Size:\t"+evalQueries.size());
			System.out.println("Averaged Time Eclapsed:\t" + timeFormat.format(new Date((endTime-startTime)/evalQueries.size())));
			
			rRanker.clearInfoOfTopNDocs();
			
			usl_vs_rank = VectorUtils.ScalarMultiply(usl_vs_rank, 1d/evalQueries.size());
			wsl_vs_rank = VectorUtils.ScalarMultiply(wsl_vs_rank, 1d/evalQueries.size());
			ndeval =      VectorUtils.ScalarMultiply(ndeval, 1d/evalQueries.size());
			
			System.out.println("==================================================");
			System.out.println("Exporting " + rankerString + ": " + rRanker.getDescription());
			//
			export(ps_avg_SLoss, "Mean", rankerString+"\t", "USL\n", usl_vs_rank, (String [])lossFunctions.get(0).getMetricArray());
			export(ps_avg_SLoss, "Mean", rankerString+"\t", "WSL\n", wsl_vs_rank, (String [])lossFunctions.get(1).getMetricArray());
			export(ps_avg_Ndeval, "Mean", rankerString+"\t", lossFunctions.get(2).getName()+"\n", ndeval, (String [])lossFunctions.get(2).getMetricArray());			
			//for cross evaluation, 5:nERR-IA@20, 11:\alpha-nDCG@20
			avgPerRankerList.add(ndeval[11]);
		}		
		//
		this.closePrintStreams();
		
		return avgPerRankerList;
	}
	/**
	 * using the probability ranking rule ranking the top-n documents with BM25 model
	 * the goal is to examine the subtopic recall of the baseline
	 * which is the basis for testing different diversity strategies
	 * **/
	public static void baselineSubtopicRecall(boolean commonIndri, DivVersion divVersion){
		boolean utility = true;
		
		HashMap<String,String> trecDivDocs = TRECDivLoader.loadTrecDivDocs(commonIndri, divVersion);
		List<String> evalQueries = TRECDivLoader.getDivEvalQueryIDList(commonIndri, divVersion);
		Map<String,TRECDivQuery> allTrecDivQueries = TRECDivLoader.loadTrecDivQueries(commonIndri, divVersion);
		Map<String,TRECQueryAspects> allTrecDivQueryAspects = TRECDivLoader.loadTrecDivQueryAspects(commonIndri, divVersion);
		
		try {
			//output
			String prefix = OutputDirectory.ROOT+"DivEvaluation/";
			String filename = null;
			if(DivVersion.Div2009 == divVersion){
				if(utility){
					filename = "Div2009BaselineUtilityRatio";
				}else{
					filename = "Div2009BaselineSubtopicRecall";
				}							
			}else if(DivVersion.Div2010 == divVersion){
				if(utility){
					filename = "Div2010BaselineUtilityRatio";
				}else{
					filename = "Div2010BaselineSubtopicRecall";
				}				
			}else if(DivVersion.Div20092010 == divVersion) {
				if(utility){
					filename = "Div20092010BaselineUtilityRatio";
				}else{
					filename = "Div20092010BaselineSubtopicRecall";
				}				
			}else{
				System.out.println("ERROR: unexpected DivVersion!");
				new Exception().printStackTrace();
				System.exit(1);				
			}
			//
			PrintStream ps_per_SRecall  = new PrintStream(new FileOutputStream(prefix + filename + "_per.txt"));
			PrintStream ps_avg_SRecall  = new PrintStream(new FileOutputStream(prefix + filename + "_avg.txt"));
			// Maintain average US and WSL vectors
			int cutoffK = 100;			
			double[] avg_vs_rank = new double[cutoffK];
			//k1 - doc TF / k3 - query TF / b - doc length penalty
			double k1, k3, b;
			k1=1.2d; k3=0.5d; b=0.5d; // achieves the best
			//k1=0.5d; k3=0.5d; b=0.5d; //better than the group of b=1000d;
			//k1=1.2d; k3=0.5d; b=1000d;
			BM25Kernel_A1 bm25Kernel = new BM25Kernel_A1(trecDivDocs, k1, k3, b);
			//
			SubtopicRecall srFunction = new SubtopicRecall();
			//
			int query_serial = 0;
			for(String number: evalQueries){
				query_serial++;
				//
				TRECDivQuery trecDivQuery = allTrecDivQueries.get(number);
				TRECQueryAspects trecQueryAspects = allTrecDivQueryAspects.get(number);
				//
				Set<String> topnDocs = trecQueryAspects.getTopNDocs();
				//
				bm25Kernel.clearInfoOfTopNDocs();				
				bm25Kernel.initTonNDocs(topnDocs);
				//
				ArrayList<StrDouble> listForRanking = new ArrayList<StrDouble>();
				//
				String queryText = trecDivQuery.getQueryContent();
				Object queryRepr = bm25Kernel.getNoncachedObjectRepresentation(queryText);
				for(String doc_name: topnDocs){
					Object docRepr = bm25Kernel.getObjectRepresentation(doc_name);
					listForRanking.add(new StrDouble(doc_name, bm25Kernel.sim(queryRepr, docRepr)));
				}
				//
				Collections.sort(listForRanking, new PairComparatorBySecond_Desc<String, Double>());
				//
				List<String> rankedList = new ArrayList<String>();
				for(int i=0; i<listForRanking.size(); i++){
					rankedList.add(listForRanking.get(i).getFirst());
				}
				
				if(utility){
					//utility ratio (part-1) below part-2
					Object srObject = srFunction.utilityEval(trecQueryAspects, rankedList, cutoffK);
					export(ps_per_SRecall, number, "BM25", "UtilityRatio", (double[])srObject, (String [])srFunction.getMetricArray_Ratio());				
					avg_vs_rank = VectorUtils.Sum(avg_vs_rank, (double[])srObject);
				}else{
					//UniformSubtopicRecall (part-1) below part-2
					///*
					Object srObject = srFunction.eval(trecQueryAspects, rankedList, cutoffK);
					export(ps_per_SRecall, number, "BM25", "SubtopicRecall", (double[])srObject, (String [])srFunction.getMetricArray());				
					avg_vs_rank = VectorUtils.Sum(avg_vs_rank, (double[])srObject);	
					//*/
				}		
			}
			//
			avg_vs_rank = VectorUtils.ScalarMultiply(avg_vs_rank, 1d/evalQueries.size());
			
			if(utility){
				//utility ratio (part-2)
				export(ps_avg_SRecall, "Mean", "BM25\t", "UtilityRatio\n", avg_vs_rank, (String [])srFunction.getMetricArray_Ratio());
			}else{
				//UniformSubtopicRecall (part-2)				
				export(ps_avg_SRecall, "Mean", "BM25\t", "SubtopicRecall\n", avg_vs_rank, (String [])srFunction.getMetricArray());				
			}			
			//
			ps_per_SRecall.flush();
			ps_per_SRecall.close();
			ps_avg_SRecall.flush();
			ps_avg_SRecall.close();			
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}
	/**
	 * (1) the goal is to test the effectiveness of each similarity metric, e.g., bm25, tf-idf,
	 * latent topic vector based similarity metric using LDA
	 * 
	 * (2) method: for each subtopic, ranked the top-n documents in decreasing order of the similarity
	 * computed with a specific kernel, compute the nDCG of the obtained ranked list
	 * the averaged nDCG across the subtopics is finally used to evaluate a similarity strategy  
	 * **/
	private static void simMetricAnalysis(boolean commonIndri, DivVersion divVersion, List<Kernel> simMetricKernels){
		//
		List<String> evalQueries = TRECDivLoader.getDivEvalQueryIDList(commonIndri, divVersion);
		Map<String,TRECDivQuery> allTrecDivQueries = TRECDivLoader.loadTrecDivQueries(commonIndri, divVersion);
		Map<String,TRECQueryAspects> allTrecDivQueryAspects = TRECDivLoader.loadTrecDivQueryAspects(commonIndri, divVersion);
		//
		try {
			//output
			String prefix = OutputDirectory.ROOT+"DivEvaluation/";
			String filename = null;
			if(DivVersion.Div2009 == divVersion){
				filename = "Div2009_SimMetric";			
			}else if(DivVersion.Div2010 == divVersion){
				filename = "Div2010_SimMetric";
			}else if(DivVersion.Div20092010 == divVersion) {
				filename = "Div20092010_SimMetric";
			}else{
				System.out.println("ERROR: unexpected DivVersion!");
				new Exception().printStackTrace();
				System.exit(1);				
			}
			//averaged nDCG per query
			PrintStream ps_per_MSnDCG  = new PrintStream(new FileOutputStream(prefix + filename + "_per.txt"));
			//averaged nDCG of the query set
			PrintStream ps_avg_MSnDCG  = new PrintStream(new FileOutputStream(prefix + filename + "_avg.txt"));
			// Maintain average US and WSL vectors
			int cutoffK = 100;
			MSnDCG msnDCGFunction = new MSnDCG();
			//
			for(Kernel simMetric: simMetricKernels){
				double[] avg_vs_rank = new double[cutoffK];
				//
				int query_serial = 0;
				for(String number: evalQueries){
					query_serial++;
					//
					TRECDivQuery trecDivQuery = allTrecDivQueries.get(number);
					TRECQueryAspects trecQueryAspects = allTrecDivQueryAspects.get(number);
					//
					Set<String> topnDocs = trecQueryAspects.getTopNDocs();
					//
					simMetric.clearInfoOfTopNDocs();				
					simMetric.initTonNDocs(topnDocs);
					//across subtopics
					Vector<TRECSubtopic> subtopicList = trecDivQuery.getSubtopicList();
					
					double [] tempMSnDCG = new double[cutoffK];
					//
					for(int s=0; s<subtopicList.size(); s++){						
						//list for ranking top-n documents of a specific subtopic
						ArrayList<org.archive.util.tuple.Pair<String, Double>> listForRanking = 
							new ArrayList<org.archive.util.tuple.Pair<String,Double>>();
						//		
						String subtopicText = subtopicList.get(s).getContent();
						Object subtopicRepr = simMetric.getNoncachedObjectRepresentation(subtopicText);
						for(String doc_name: topnDocs){
							Object docRepr = simMetric.getObjectRepresentation(doc_name);
							listForRanking.add(new org.archive.util.tuple.Pair<String, Double>(doc_name,
									simMetric.sim(subtopicRepr, docRepr)));
						}
						//
						Collections.sort(listForRanking, new PairComparatorBySecond_Desc<String, Double>());
						//
						ArrayList<String> rankedList = new ArrayList<String>();
						for(int i=0; i<listForRanking.size(); i++){
							rankedList.add(listForRanking.get(i).getFirst());
						}
						//
						Object msnDCGObject = msnDCGFunction.calMSnDCG(s, trecQueryAspects, rankedList, cutoffK);
						//
						/*
						double [] array = (double[])msnDCGObject;
						System.out.println();
						for(int m=0; m<array.length; m++){
							System.out.print("  "+array[m]);
						}
						System.out.println();
						*/
						//
						tempMSnDCG = VectorUtils.Sum(tempMSnDCG, (double[])msnDCGObject);
					}
					//get the mean across all subtopics of a query
					tempMSnDCG = VectorUtils.ScalarMultiply(tempMSnDCG, 1d/subtopicList.size());
					//--				
					export(ps_per_MSnDCG, "q-"+serialFormat.format(query_serial), simMetric.getString(), "MSnDCG\n",
							(double[])tempMSnDCG, (String [])msnDCGFunction.getMetricArray());
				
					avg_vs_rank = VectorUtils.Sum(avg_vs_rank, (double[])tempMSnDCG);
					//
				}
				//
				avg_vs_rank = VectorUtils.ScalarMultiply(avg_vs_rank, 1d/evalQueries.size());
				//
				export(ps_avg_MSnDCG, "Mean", simMetric.getString(), "\tMSnDCG\n", avg_vs_rank,
						(String [])msnDCGFunction.getMetricArray());				
			}
			//
			ps_per_MSnDCG.flush();
			ps_per_MSnDCG.close();
			ps_avg_MSnDCG.flush();
			ps_avg_MSnDCG.close();			
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}
	//
	public static void simMetricAnalysis(boolean commonIndri, DivVersion divVersion){
		HashMap<String,String> trecDivDocs = TRECDivLoader.loadTrecDivDocs(commonIndri, divVersion);
		//
		ArrayList<Kernel> kernelList = new ArrayList<Kernel>();
		/////////
		//bm25
		/////////
		double k1, k3, b;
		k1=1.2d; k3=0.5d; b=0.5d; // achieves the best
		//k1=0.5d; k3=0.5d; b=0.5d; //better than the group of b=1000d;
		//k1=1.2d; k3=0.5d; b=1000d;
		BM25Kernel_A1 bm25_A1_Kernel = new BM25Kernel_A1(trecDivDocs, k1, k3, b);
		BM25Kernel bm25Kernel = new BM25Kernel(trecDivDocs, k1, k3, b);
		
		kernelList.add(bm25_A1_Kernel);
		kernelList.add(bm25Kernel);
		
		/////////
		//TF-IDF
		/////////
		TFIDF_A1 tfidf_A1Kernel = new TFIDF_A1(trecDivDocs, false);
		TFIDF tfidfKernel = new TFIDF(trecDivDocs, false);
		
		kernelList.add(tfidf_A1Kernel);
		kernelList.add(tfidfKernel);
		
		////////
		//LDA
		////////
		LDAKernel lda_true_10_Kernel = new LDAKernel(trecDivDocs, 10, true, false);
		LDAKernel lda_false_10_Kernel = new LDAKernel(trecDivDocs, 10, false, false);
		
		kernelList.add(lda_true_10_Kernel);
		kernelList.add(lda_false_10_Kernel);
		//
		LDAKernel lda_true_15_Kernel = new LDAKernel(trecDivDocs, 15, true, false);
		LDAKernel lda_false_15_Kernel = new LDAKernel(trecDivDocs, 15, false, false);
		
		kernelList.add(lda_true_15_Kernel);
		kernelList.add(lda_false_15_Kernel);
		
		
		//metric effectiveness analysis
		simMetricAnalysis(commonIndri, DivVersion.Div20092010, kernelList);
	}
	//
	//
	public static void main(String []args){
		//1 Baseline subtopic recall
		boolean commonIndri = true;
		TRECDivEvaluator.baselineSubtopicRecall(commonIndri, DivVersion.Div2010);
		
		//2 simMetric analysis
		//TRECDivEvaluator.simMetricAnalysis();		
	}
}
