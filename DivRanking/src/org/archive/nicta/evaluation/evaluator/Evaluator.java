/** Main Evaluator for TREC Interactive and CLUEWEB Diversity tracks
 *   
 * @author Scott Sanner (ssanner@gmail.com)
 */

package org.archive.nicta.evaluation.evaluator;

import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.archive.dataset.trec.query.TRECQueryAspects;
import org.archive.nicta.evaluation.metricfunction.Metric;
import org.archive.nicta.ranker.ResultRanker;

/**
 * Evaluation Results Format:
 * (1)
 * ".txt" for: AllUSLoss & AllWSLoss, calculate the USL/WSL for each cutoff, e.g., 1-20
 * ".avg.txt" is the mean value
 * 
 * (2)
 * "_ndeval.txt", calculate the 21-metric per query
 * "_ndeval.avg.txt" is the mean value
 * 
 * **/

public abstract class Evaluator {
	
	public static final boolean DEBUG = false;
	//output
	public static DecimalFormat fourResultFormat = new DecimalFormat("0.0000");
	public static DecimalFormat twoResultFormat = new DecimalFormat("0.00");
	public static DecimalFormat oneResultFormat = new DecimalFormat("0.0");
	
	public static DecimalFormat serialFormat = new DecimalFormat("00");
	//
	public String _outputPrefix;
	public String _outputFilename;
	PrintStream ps_per_SLoss;
	PrintStream ps_avg_SLoss;
	PrintStream ps_per_Ndeval;
	PrintStream ps_avg_Ndeval;
	PrintStream err;
	
	
	Evaluator(String outputPrefix, String outputFilename){
		this._outputPrefix = outputPrefix;
		this._outputFilename = outputFilename;
	}
	//
	public abstract void openPrintStreams();
	public abstract void closePrintStreams();
	/**
	 * @return perform evaluation.
	 * @param evalQueries: used query set, sometimes not all
	 * @param allDocs: the entire documents
	 * **/
	public abstract ArrayList<Double> doEval(
			List<String> evalQueries, 
			HashMap<String,String> allDocs, 			 
			Map<String,TRECQueryAspects> stdTRECQueryAspects,
			List<Metric> lossFunctions,
			List<ResultRanker> resultRankers,
			int cutoffK) throws Exception;
	//
	public static void export(PrintStream ps, String query_serial, 
			String rankerString, String lossFString, double[] v, String [] metricArray) {
		
		ps.print(query_serial + "\t" + rankerString + "\t" + lossFString);
		for (int i = 0; i < v.length; i++)
			if(metricArray[i].endsWith("\n")){
				ps.print("\t" + metricArray[i].replaceFirst("\n", ":") +fourResultFormat.format(v[i])+"\t");
			}else {
				ps.print("\t" + metricArray[i] + ":" +fourResultFormat.format(v[i]));
			}			
		ps.println();		
	}	
}
