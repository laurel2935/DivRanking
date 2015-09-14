package org.archive.a1.evaluation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.archive.dataset.ntcir.NTCIRLoader.NTCIR_EVAL_TASK;
import org.archive.dataset.ntcir.NTCIRTopic;
import org.archive.nicta.ranker.ResultRanker;



public class NTCIREvaluator {
	
	public static final boolean DEBUG = false;
	//	
	
	private static boolean acceptedTopic_NTCIR10_DR_CH(String topicid){
		int intID = Integer.parseInt(topicid);
		if(intID>200 && intID<=300 
				&& !topicid.equals("0266")
				&& !topicid.equals("0272")
				&& !topicid.equals("0300")){
			return true;
		}else {
			return false;
		}
	}
	
	private static boolean acceptedTopic_NTCIR10_SM_CH(String topicid){
		int intID = Integer.parseInt(topicid);
		if(intID>200 && intID<=300 				
				&& !topicid.equals("0272")
				&& !topicid.equals("0300")){
			return true;
		}else {
			return false;
		}
	}
	
	public static void doEval(List<NTCIRTopic> topicList, 
			HashMap<String, ArrayList<String>> sysRun, int cutoff, NTCIR_EVAL_TASK task){
		//
		//I-rec, D-nDCG, D#-nDCG per topic
		ArrayList<ArrayList<Double>> IRec_DnDCG_DSharp_TripleList = new ArrayList<ArrayList<Double>>();
		ArrayList<Double> avg_IRec_DnDCG_DSharp_Triple = new ArrayList<Double>();
		//
		int topic_count = 0;
		for(NTCIRTopic topic: topicList){
			if(task == NTCIR_EVAL_TASK.NTCIR10_SM_CH){
				if(!acceptedTopic_NTCIR10_SM_CH(topic.getTopicID())){
					continue;
				}
			}else if(task == NTCIR_EVAL_TASK.NTCIR10_DR_CH) {
				if(!acceptedTopic_NTCIR10_DR_CH(topic.getTopicID())){
					continue;
				}				
			}
			ArrayList<Double> triple = topic.cal(sysRun.get(topic.getTopicID()), cutoff);
			if (DEBUG) {
				System.out.println(topic.getTopicID()+"\t with respect to\t"+cutoff);
				System.out.println(triple.get(0)+"\t"+triple.get(1)+"\t"+triple.get(2));
			}
			IRec_DnDCG_DSharp_TripleList.add(triple);
			topic_count++;
		}
		//
		double sumIRec, sumDnDCG, sumDSharp;
		sumIRec = sumDnDCG = sumDSharp = 0;			
		for(ArrayList<Double> triple: IRec_DnDCG_DSharp_TripleList){
			sumIRec += triple.get(0);
			sumDnDCG += triple.get(1);
			sumDSharp += triple.get(2);
		}
		System.out.println("topic count\t"+topic_count);
		avg_IRec_DnDCG_DSharp_Triple.add(sumIRec/topic_count);
		avg_IRec_DnDCG_DSharp_Triple.add(sumDnDCG/topic_count);
		avg_IRec_DnDCG_DSharp_Triple.add(sumDSharp/topic_count);
		//
		if (DEBUG) {
			System.out.println(avg_IRec_DnDCG_DSharp_Triple);
		}		
	}
	
	public static void doEval(
			List<NTCIRTopic> topicList, 
			HashMap<String,String> docs,
			HashMap<String, ArrayList<String>> poolOfBaseline,  
			List<ResultRanker> resultRankers,
			int cutoff, NTCIR_EVAL_TASK task){		
		
		// Loop:
		// - go through each ranker: a variant of MMR
		//     - go through all topics: topic
		//        - add docs to ranker rRanker corresponding to topic
		//        - get result list for topic with ranker rRanker

		
		int ranker_index = 1;
		for (ResultRanker rRanker : resultRankers) {
			
			if (DEBUG){
				System.out.println("- Processing test '" + rRanker.getDescription() + "'");
			}			
			
			//I-rec, D-nDCG, D#-nDCG per topic
			ArrayList<ArrayList<Double>> IRec_DnDCG_DSharp_TripleList = new ArrayList<ArrayList<Double>>();
			ArrayList<Double> avg_IRec_DnDCG_DSharp_Triple = new ArrayList<Double>();
			// Build a cache for reuse of top-docs
			//HashMap<String, HashSet<String>> top_docs = new HashMap<String, HashSet<String>>();
			
			int topic_count = 0;
			for (NTCIRTopic topic : topicList) {
				// For a pair of ranker and a topic	
				if(task == NTCIR_EVAL_TASK.NTCIR10_SM_CH){
					if(!acceptedTopic_NTCIR10_SM_CH(topic.getTopicID())){
						continue;
					}
				}else if(task == NTCIR_EVAL_TASK.NTCIR10_DR_CH) {
					if(!acceptedTopic_NTCIR10_DR_CH(topic.getTopicID())){
						continue;
					}				
				}
				++topic_count;				
				
				if (DEBUG) {
					System.out.println("- Processing topic '" + topic.getTopicID() + "'");
					System.out.println("- Query details: " + topic.getTopicText());
					//System.out.println("- Query aspects: " + qa);
				}

				// Add docs for query to test
				rRanker.clearInfoOfTopNDocs();
				//Set<String> relevant_docs = qa.getRelevantDocs();
				ArrayList<String> relevant_docs = poolOfBaseline.get(topic.getTopicID());

				if (DEBUG)
					System.out.println("- Evaluating with " + relevant_docs.size() + " docs");
				
				//check doc's segments length 
				for (String doc_name : relevant_docs) {
					//case-1:
					if (!docs.containsKey(doc_name)){
						if (DEBUG){
							System.out.println("ERROR case-1: '" + doc_name + "' of baseline is not in html pool for '" + topic.getTopicText() + "'");
						}
					}else if((null==docs.get(doc_name)) || docs.get(doc_name).length() < 10){
						if (DEBUG) {
							System.out.println("ERROR case-2: '" + doc_name + "' contains little words for '" + topic.getTopicText() + "'");
						}
					}else {
						rRanker.addATopNDoc(doc_name);
						//if (DEBUG)
						//	System.out.println("- [" + query + "] Adding " + doc_name);
					}
				}
				
				// Get the results
				if (DEBUG){
					System.out.println("- Running alg: " + rRanker.getDescription());
				}					
				
				ArrayList<String> result_list = rRanker.getResultList(topic.getTopicRepresentation(), cutoff);
				
				if (DEBUG)
					System.out.println("- Result list: " + result_list);
				
				ArrayList<Double> triple = topic.cal(result_list, cutoff);
				IRec_DnDCG_DSharp_TripleList.add(triple);
				
				// Evaluate all loss functions on the results
				/*
				for (AspectLoss loss : loss_functions) {
					String loss_name = loss.getName();
					System.out.println("Evaluating: " + loss_name);
					Object o = loss.eval(qa, result_list);
					String loss_result_str = null;
					if (o instanceof double[]) {
						loss_result_str = VectorUtils.GetString((double[])o);
					} else {
						loss_result_str = o.toString();
					}
					
					// Display results to screen for now
					System.out.println("==================================================");
					System.out.println("Query: " + q._name + " -> " + q.getQueryContent());
					System.out.println("MMR Alg: " + rRanker.getDescription());
					System.out.println("Loss Function: " + loss.getName());
					System.out.println("Evaluation: " + loss_result_str);
					
					// Maintain averages and export
					if (loss instanceof AllUSLoss) {
						usl_vs_rank = VectorUtils.Sum(usl_vs_rank, (double[])o);
						export(ps_per_SLoss, query_num, test_num, 1, (double[])o);
					}
					if (loss instanceof AllWSLoss) {
						wsl_vs_rank = VectorUtils.Sum(wsl_vs_rank, (double[])o);
						export(ps_per_SLoss, query_num, test_num, 2, (double[])o);
					}
					if (loss instanceof NDEval10Losses) {
						if (ndeval == null) {
							ndeval = new double[((double[])o).length];
						}	
						ndeval = VectorUtils.Sum(ndeval, (double[])o);
						export(ps_per_Ndeval, query_num, test_num, 3, (double[])o);
					}
					ps_per_SLoss.flush();
					ps_per_Ndeval.flush();
				}	
				*/
				///////////////////////////////////////////////////////////////////////
			}
			rRanker.clearInfoOfTopNDocs();
			
			double sumIRec, sumDnDCG, sumDSharp;
			sumIRec = sumDnDCG = sumDSharp = 0;			
			for(ArrayList<Double> triple: IRec_DnDCG_DSharp_TripleList){
				sumIRec += triple.get(0);
				sumDnDCG += triple.get(1);
				sumDSharp += triple.get(2);
			}
			avg_IRec_DnDCG_DSharp_Triple.add(sumIRec/topic_count);
			avg_IRec_DnDCG_DSharp_Triple.add(sumDnDCG/topic_count);
			avg_IRec_DnDCG_DSharp_Triple.add(sumDSharp/topic_count);
			
			System.out.println("topic count\t"+topic_count);	
			System.out.println(avg_IRec_DnDCG_DSharp_Triple);
			if (DEBUG) {
				System.out.println(avg_IRec_DnDCG_DSharp_Triple);
			}	
						
			++ranker_index;
		}
		/*
		ps_per_SLoss.close();
		ps_avg_SLoss.close();
		ps_per_Ndeval.close();
		ps_avg_Ndeval.close();
		*/
		//err.close();
		
	}

}
