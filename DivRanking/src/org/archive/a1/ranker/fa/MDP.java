package org.archive.a1.ranker.fa;

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.archive.dataset.ntcir.sm.SMTopic;
import org.archive.dataset.trec.query.TREC68Query;
import org.archive.dataset.trec.query.TRECDivQuery;
import org.archive.dataset.trec.query.TRECQuery;
import org.archive.dataset.trec.query.TRECQueryAspects;
import org.archive.nicta.evaluation.metricfunction.AllUSLoss;
import org.archive.nicta.evaluation.metricfunction.AllWSLoss;
import org.archive.nicta.evaluation.metricfunction.Metric;
import org.archive.nicta.evaluation.metricfunction.NDEvalLosses;
import org.archive.nicta.kernel.KLDivergenceKernel;
import org.archive.nicta.kernel.Kernel;
import org.archive.nicta.ranker.ResultRanker;
import org.archive.ntcir.dr.rank.DRRunParameter;
import org.archive.util.DevNullPrintStream;
import org.archive.util.Pair;
import org.archive.util.VectorUtils;
import org.archive.util.tuple.PairComparatorBySecond_Desc;
import org.archive.util.tuple.StrDouble;

/**
 * A common framework which essentially a global optimization!
 * Type-1: argmax{ f(S)=lambda*r(S)+(1-lambda)*d(S)	},
 * 	where r(S) represents the entire relevance, d(S) represents the diversity
 * **/

public class MDP extends ResultRanker {
	
	private static final double MAX_VALUE = 1000000;
	
	private Map<String,TREC68Query> _allTREC68Queries = null; 
	private Map<String,TRECDivQuery> _allTRECDivQueries = null;
	
	public double _dLambda;
	//
	private int _itrThreshold;
	//for computing relevance
	public Kernel _releKernel;	
	//for computing distance
	public Kernel _disKernel;
	
	public KLDivergenceKernel _klDisKernel;	
	private boolean isKLKernel = false;
	
	//buffer similarity values by _sbKernel for two items
	public HashMap<Pair,Double>   _releCache;
	//buffer distance values by _sbKernel for two items
	public HashMap<Pair,Double>   _disCache;
	
	// Constructor
	public MDP(HashMap<String, String> docs, double lambda, int itrThreshold, Kernel releKernel, Kernel disKernel,
			Map<String,TREC68Query> allTREC68Queries, Map<String,TRECDivQuery> allTRECDivQueries) { 
		
		super(docs);		
		this._dLambda = lambda;
		this._itrThreshold = itrThreshold;
		this._releKernel = releKernel;
		this._disKernel = disKernel;
		this._releCache = new HashMap<Pair,Double>();		
		this._disCache = new HashMap<Pair,Double>();	
		//
		if(null != allTREC68Queries){
			this._allTREC68Queries = allTREC68Queries;
		}
		if(null != allTRECDivQueries){
			this._allTRECDivQueries = allTRECDivQueries;
		}		
	}
	
	// Constructor
	public MDP(HashMap<String, String> docs, double lambda, int itrThreshold, Kernel releKernel, KLDivergenceKernel klDisKernel,
			Map<String,TREC68Query> allTREC68Queries, Map<String,TRECDivQuery> allTRECDivQueries) { 
		
		super(docs);		
		this._dLambda = lambda;
		this._itrThreshold = itrThreshold;
		this._releKernel = releKernel;
		this._klDisKernel = klDisKernel;
		isKLKernel = true;
		this._releCache = new HashMap<Pair,Double>();		
		this._disCache = new HashMap<Pair,Double>();	
		//
		if(null != allTREC68Queries){
			this._allTREC68Queries = allTREC68Queries;
		}
		if(null != allTRECDivQueries){
			this._allTRECDivQueries = allTRECDivQueries;
		}		
	}
	
	//be called when a new query comes
	public void addATopNDoc(String doc_name) {
		_docs_topn.add(doc_name);
	}
	//refresh each time for a query
	//_docOrig, i.e., the top-n set of a query
	public void clearInfoOfTopNDocs() {
		_docRepr.clear();	
		_docRepr2.clear();
		_docs_topn.clear();
		_releKernel.clearInfoOfTopNDocs();		
		_disKernel.clearInfoOfTopNDocs();
		// No need to clear sim and div caches, these are local and conditioned
		// on query and we expect that the sim and div kernels will not change.
	}
	//called when a new query come
	public void initTonNDocsForInnerKernels() {
		//for relevance
		// The similarity kernel may need to do pre-processing (e.g., LDA training)
		_releKernel.initTonNDocs(_docs_topn); // LDA should maintain keys for mapping later		
		// Store local representation for later use with kernels
		// (should we let _sim handle everything and just interact with keys?)
		for (String doc : _docs_topn) {
			Object releRepr = _releKernel.getObjectRepresentation(doc);
			_docRepr.put(doc, releRepr);			
		}
		
		//for distance
		_disKernel.initTonNDocs(_docs_topn);
		for(String doc: _docs_topn){
			Object disRepr = _disKernel.getObjectRepresentation(doc);
			_docRepr2.put(doc, disRepr);
		}		
	}
	
	//using the default version: fVersion._dfa
	public ArrayList<String> getResultList(String query, int size) {
		// Intialize document set
		initTonNDocsForInnerKernels();
		
		// Get representation for query
		Object query_repr = _releKernel.getNoncachedObjectRepresentation(query);
		//
		ArrayList<StrDouble> rankedS = hillClimbingSearch_dfa_perBest(query_repr, _docs_topn, size, fVersion._dfa);		
		//
		ArrayList<String> result_list = new ArrayList<String>();
		for(int i=0; i<rankedS.size(); i++){
			result_list.add(rankedS.get(i).getFirst());
		}
		//
		return result_list;	
	}
	
	public ArrayList<String> getResultList(TRECDivQuery trecDivQuery, int size) {
		return null;
	}
	//
	public ArrayList<String> getResultList(String query, int size, fVersion _fVersion) {		
		// Get representation for query
		Object query_repr = _releKernel.getNoncachedObjectRepresentation(query);
		//
		ArrayList<StrDouble> rankedS = hillClimbingSearch_dfa_perBest(query_repr, _docs_topn, size, _fVersion);		
		//
		ArrayList<String> result_list = new ArrayList<String>();
		for(int i=0; i<rankedS.size(); i++){
			result_list.add(rankedS.get(i).getFirst());
		}
		//
		return result_list;		
	}
	//////////////////////////
	//Optimizing Functions f by Maximizing f(S)
	//////////////////////////
	private double f(Object query_repr, Set<String> S, Set<String> D_Minus_S, fVersion _fVersion){
		if(_fVersion == fVersion._dfa){
			return f_dfa(query_repr, S, D_Minus_S);		
		}else if(_fVersion == fVersion._dfa_scaled){
			return f_dfa_scaled(query_repr, S, D_Minus_S);
		}else if(_fVersion == fVersion._md){
			return f_md(query_repr, D_Minus_S);
		}else if(_fVersion == fVersion._md_scaled){
			return f_md_scaled(query_repr, D_Minus_S);
		}else if(_fVersion == fVersion._pdfa){
			return f_pdfa(query_repr, D_Minus_S);
		}else if(_fVersion == fVersion._pdfa_scaled){
			return f_pdfa_scaled(query_repr, D_Minus_S);
		}else if(_fVersion == fVersion._pdfa_scaled_exp){
			return f_pdfa_scaled_exp(query_repr, D_Minus_S);
		}else if(_fVersion == fVersion._pdfa_scaled_exp_head){
			return f_pdfa_scaled_exp_head(query_repr, D_Minus_S);
		}else{
			System.out.println("ERROR: unexpected fVersion!");
			new Exception().printStackTrace();
			System.exit(1);
			return -1d;
		}		
	}
	//////////////////////////
	//Contribution of each doc of S: c()
	//////////////////////////
	private double c(String doc_name_k, Object query_repr, Set<String> S, Set<String> D_Minus_S, fVersion _fVersion){
		if(_fVersion == fVersion._dfa){
			return c_dfa(doc_name_k, query_repr, S, D_Minus_S);
		}else if(_fVersion == fVersion._dfa_scaled){
			return c_dfa_scaled(doc_name_k, query_repr, S, D_Minus_S);
		}else if(_fVersion == fVersion._md){
			return c_md(doc_name_k, query_repr, D_Minus_S);
		}else if(_fVersion == fVersion._md_scaled){
			return c_md_scaled(doc_name_k, query_repr, D_Minus_S);
		}else if(_fVersion == fVersion._pdfa){
			return c_pdfa(doc_name_k, query_repr, D_Minus_S);
		}else if(_fVersion == fVersion._pdfa_scaled){
			return c_pdfa_scaled(doc_name_k, query_repr, D_Minus_S);
		}else if(_fVersion == fVersion._pdfa_scaled_exp){
			return c_pdfa_scaled_exp(doc_name_k, query_repr, D_Minus_S);
		}else if(_fVersion == fVersion._pdfa_scaled_exp_head){
			return c_pdfa_scaled_exp_head(doc_name_k, query_repr, D_Minus_S);
		}else{
			System.out.println("ERROR: unexpected fVersion!");
			new Exception().printStackTrace();
			System.exit(1);
			return -1d;
		}
	}
	//	f versions	//
	public static enum fVersion {_dfa, _dfa_scaled, _md, _md_scaled, 
		_pdfa, _pdfa_scaled, _pdfa_scaled_exp, _pdfa_scaled_exp_head}
	//dfa function: desirable facilities allocation ()
	private double f_dfa(Object query_repr, Set<String> S, Set<String> D_Minus_S){
		return _dLambda*getSumRelevance(query_repr, S)
			+(1 - _dLambda)*(-calDiversity_DFA(S, D_Minus_S));
	}	
	//dfa_scaled function: desirable facilities allocation
	private double f_dfa_scaled(Object query_repr, Set<String> S, Set<String> D_Minus_S){
		return _dLambda*getSumRelevance_Averaged(query_repr, S)
			+(1-_dLambda)*(-calDiversity_DFA_Averaged(S, D_Minus_S));		
	}
	//contribution per doc
	private double c_dfa(String doc_name_k, Object query_repr, Set<String> S, Set<String> D_Minus_S){
		return _dLambda*calRelevance(doc_name_k, query_repr)
			+(1-_dLambda)*(-dContributionOfK_dfa(doc_name_k, S, D_Minus_S));
	}
	private double c_dfa_scaled(String doc_name_k, Object query_repr, Set<String> S, Set<String> D_Minus_S){
		return _dLambda*(1d/S.size())*calRelevance(doc_name_k, query_repr)
			+(1-_dLambda)*(1d/D_Minus_S.size())*(-dContributionOfK_dfa(doc_name_k, S, D_Minus_S));
	}
	//md function: merely maximizing dispersion
	private double f_md(Object query_repr, Set<String> S){
		return _dLambda*getSumRelevance(query_repr, S)
		+(1-_dLambda)*calDiversity_DISP(S);
	}
	//md_scaled function: merely maximizing dispersion, averaged
	private double f_md_scaled(Object query_repr, Set<String> S){
		return _dLambda*getSumRelevance_Averaged(query_repr, S)
			+(1-_dLambda)*calDiversity_DISP_Averaged(S);
	}	
	//contribution per doc
	private double c_md(String doc_name_k, Object query_repr, Set<String> S){
		return _dLambda*calRelevance(doc_name_k, query_repr)
			+(1-_dLambda)*dContributionOfKGivenS_disp(doc_name_k, S);
	}
	private double c_md_scaled(String doc_name_k, Object query_repr, Set<String> S){
		return _dLambda*(1d/S.size())*calRelevance(doc_name_k, query_repr)
			+(1-_dLambda)*(1d/(S.size()*(S.size()-1)))*dContributionOfKGivenS_disp(doc_name_k, S);
	}
	//pdfa function: query-oriented dispersion or proportionally dispersed facilities allocation (pdfa)
	private double f_pdfa(Object query_repr, Set<String> S){
		return _dLambda*getSumRelevance(query_repr, S)
			+(1-_dLambda)*(calDiversity_DISP(S)/calProportionality(query_repr, S));
	}
	//pdfa_scaled function: query-oriented dispersion or proportionally dispersed facilities allocation (pdfa) scaled
	private double f_pdfa_scaled(Object query_repr, Set<String> S){
		return _dLambda*getSumRelevance_Averaged(query_repr, S)
			+(1-_dLambda)*(calDiversity_DISP_Averaged(S)/calProportionality_Scaled(query_repr, S));
	}
	//
	private double c_pdfa(String doc_name_k, Object query_repr, Set<String> S){
		return _dLambda*calRelevance(doc_name_k, query_repr)
			+(1-_dLambda)*(dContributionOfKGivenS_disp(doc_name_k, S)/pContributionOfK(doc_name_k, query_repr));
	}
	private double c_pdfa_scaled(String doc_name_k, Object query_repr, Set<String> S){
		return _dLambda*(1d/S.size())*calRelevance(doc_name_k, query_repr)
		+(1-_dLambda)*((1d/(S.size()*(S.size()-1)))*dContributionOfKGivenS_disp(doc_name_k, S)/pContributionOfK_scaled(doc_name_k, query_repr));
	}
	//f_pdfa_scaled_exp function: query-oriented dispersion or proportionally dispersed facilities allocation (pdfa) scaled and exp
	private double f_pdfa_scaled_exp(Object query_repr, Set<String> S){
		return _dLambda*getSumRelevance_Averaged(query_repr, S)
			+(1-_dLambda)*calDiversity_DISP_Averaged(S)*Math.exp(1-calProportionality_Scaled(query_repr, S));
	}
	private double c_pdfa_scaled_exp(String doc_name_k, Object query_repr, Set<String> S){
		return _dLambda*(1d/S.size())*calRelevance(doc_name_k, query_repr)
			+(1-_dLambda)*((1d/(S.size()*(S.size()-1)))*dContributionOfKGivenS_disp(doc_name_k, S))*Math.exp(1-pContributionOfK_scaled(doc_name_k, query_repr));
	}
	//f_pdfa_scaled_exp_head function: query-oriented dispersion
	private double f_pdfa_scaled_exp_head(Object query_repr, Set<String> S){
		return _dLambda*getSumRelevance_Averaged(query_repr, S)*Math.exp(1-calProportionality_Scaled(query_repr, S))
			+(1-_dLambda)*calDiversity_DISP_Averaged(S);
	}
	//
	private double c_pdfa_scaled_exp_head(String doc_name_k, Object query_repr, Set<String> S){
		return _dLambda*(1d/S.size())*calRelevance(doc_name_k, query_repr)*Math.exp(1-pContributionOfK_scaled(doc_name_k, query_repr))
			+(1-_dLambda)*(1d/(S.size()*(S.size()-1)))*dContributionOfKGivenS_disp(doc_name_k, S);
	}
	
	//////////////////////////
	//HillClimbingSearch
	//////////////////////////	
	//(1)
	private ArrayList<StrDouble> hillClimbingSearch(Object query_repr, Set<String> D, int k, int itrThreshold, fVersion _fVersion){
		//
		ArrayList<StrDouble> topnDoc_rlist =
			new ArrayList<StrDouble>();
		//
		String query_repr_key = query_repr.toString();
		Double releScore = null;
		Object doc_repr = null;
		Pair releKey = null;
		//
		for(String doc_name: D){
			releKey = new Pair(query_repr_key, doc_name );
			if (null == (releScore = _releCache.get(releKey))) {
				doc_repr = _docRepr.get(doc_name);
				releScore = _releKernel.sim(query_repr, doc_repr);
				_releCache.put(releKey, releScore);
			}
			//
			topnDoc_rlist.add(new StrDouble(doc_name, releScore));
		}
		//
		Collections.sort(topnDoc_rlist, new PairComparatorBySecond_Desc<String, Double>());
		//
		HashSet<String> S = new HashSet<String>();
		//initialize with top-k relevant doc
		for(int i=0; i<k; i++){
			S.add(topnDoc_rlist.get(i).getFirst());
		}
		//
		Set<String> D_Minus_S = new HashSet<String>();
		Iterator<String> dItr = D.iterator();
		while(dItr.hasNext()){
			String doc_name = dItr.next();
			if(!S.contains(doc_name)){
				D_Minus_S.add(doc_name);
			}
		}
		//		
		HashSet<String> old_S = new HashSet<String>();	
		HashSet<String> old_D_Minus_S = new HashSet<String>();		
		old_S.addAll(S);			
		old_D_Minus_S.addAll(D_Minus_S);
		//		
		boolean change;
		int count = 0;
		do{	
			change = false;
			if(count > 100000){
				System.out.println("Maximum Itration!");
				break;
			}else {
				count++;
			}			
			//			
			for(String doc_in: old_S){				
				for(String doc_out: old_D_Minus_S){					
					//exchange for S
					S.remove(doc_in);
					S.add(doc_out);
					//exchange for D_Minus_S
					D_Minus_S.remove(doc_out);
					D_Minus_S.add(doc_in);
					//
					if(f(query_repr, S, D_Minus_S, _fVersion) > f(query_repr, old_S, old_D_Minus_S, _fVersion)){
						//change like S
						old_S.remove(doc_in);
						old_S.add(doc_out);
						//change like D_Minus_S
						old_D_Minus_S.remove(doc_out);
						old_D_Minus_S.add(doc_in);
						//
						change = true;
						break;
					}else{
						//back
						S.remove(doc_out);
						S.add(doc_in);
						//
						D_Minus_S.remove(doc_in);
						D_Minus_S.add(doc_out);
					}
				}
				//
				if(change){
					break;
				}
			}			
		}while(change);
		//sorting in order of contribution
		ArrayList<StrDouble> s_c_list =
			new ArrayList<StrDouble>();
		Iterator<String> sItr = S.iterator();
		//
		while(sItr.hasNext()){
			String doc_name_k = sItr.next();			
			//
			s_c_list.add(new StrDouble(doc_name_k, 
					c(doc_name_k, query_repr, S, D_Minus_S, _fVersion)));
		}
		//
		Collections.sort(s_c_list, new PairComparatorBySecond_Desc<String, Double>());
		//
		return s_c_list;
	}
	
	//(2)
	/**
	 * replace with the best possible one
	 * **/
	private ArrayList<StrDouble> hillClimbingSearch_dfa_perBest(Object query_repr, Set<String> D, int k, fVersion _fVersion){
		
		ArrayList<String> S = new ArrayList<String>();		
		ArrayList<String> D_Minus_S = new ArrayList<String>();
		
		//star: elements of S, follower: elements of D_Minus_S
		//star->followers & min-distance value, i.e., max-sim value		
		HashMap<String, HashSet<StrDouble>> starToFollowersMap = new HashMap<String, HashSet<StrDouble>>();
		HashMap<String, StrDouble> followerToStarMap = 	new HashMap<String, StrDouble>();
				
		ArrayList<StrDouble> topnDoc_rlist = new ArrayList<StrDouble>();		
		String query_repr_key = query_repr.toString();
		
		Double releScore = null;
		Object doc_repr = null;
		Pair releKey = null;
		
		for(String doc_name: D){
			releKey = new Pair(query_repr_key, doc_name);
			
			if (null == (releScore = _releCache.get(releKey))) {
				doc_repr = _docRepr.get(doc_name);
				releScore = _releKernel.sim(query_repr, doc_repr);
				_releCache.put(releKey, releScore);
			}
			
			topnDoc_rlist.add(new StrDouble(doc_name, releScore));
		}		
		
		Collections.sort(topnDoc_rlist, new PairComparatorBySecond_Desc<String, Double>());		
		
		//initialize with top-k relevant doc
		for(int i=0; i<k; i++){
			S.add(topnDoc_rlist.get(i).getFirst());
		}
		
		if(DEBUG){
			System.out.println("S size:\t"+S.size());
			System.out.println("D size:\t"+D.size());
		}
		
		//first star follower mapping		
		Iterator<String> theDItr = D.iterator();		
		while(theDItr.hasNext()){
			
			String doc_name = theDItr.next();
			
			if(!S.contains(doc_name)){
				
				D_Minus_S.add(doc_name);
				
				StrDouble theMinPair;
				if(isKLKernel){
					theMinPair = minDisPairGivenS_KL(doc_name, D_Minus_S);
				}else{
					theMinPair = minDisPairGivenS(doc_name, S);
				}
				
				//follower - corresponding star  // (1-_dLambda) is necessary
				followerToStarMap.put(doc_name, new StrDouble(theMinPair.getFirst(), (1-_dLambda)*theMinPair.getSecond()));	
				
				//star - corresponding followers
				if(starToFollowersMap.containsKey(theMinPair.getFirst())){
					
					starToFollowersMap.get(theMinPair.getFirst()).add(new StrDouble(doc_name, (1-_dLambda)*theMinPair.getSecond()));
					
				}else{
					
					HashSet<StrDouble> follwerSet = new HashSet<StrDouble>();					
					follwerSet.add(new StrDouble(doc_name, (1-_dLambda)*theMinPair.getSecond()));
					starToFollowersMap.put(theMinPair.getFirst(), follwerSet);					
				}				
			}else{
				HashSet<StrDouble> follwerSet = new HashSet<StrDouble>();
				starToFollowersMap.put(doc_name, follwerSet);
			}
		}
		
		if(DEBUG){
			System.out.println("D_Minus_S size:\t"+D_Minus_S.size());
			System.out.println("starToFollowersMap size:\t"+starToFollowersMap.size());
		}
		
		//iterating	
		int times = 0;
		do{
			times++;
			boolean change = false;
			
			ArrayList<String> oldS = new ArrayList<String>();
			oldS.addAll(S);
			
			for(int i=0; i<oldS.size(); i++){
				
				String star = oldS.get(i);
				
				if(DEBUG){
					System.out.println("old S:\t"+oldS);
					System.out.println("S:\t"+S);
					System.out.println("Keys:\t"+starToFollowersMap.keySet());
				}		
				
				HashSet<StrDouble> theFollwers = starToFollowersMap.get(star);
				
				double minDelta = 0d;
				String minToBeStar = null;
				
				//finding the toBeStar that results in the minimum delta
				for(int j=0; j<D_Minus_S.size(); j++){
					
					String toBeStar = D_Minus_S.get(j);
					//tempt new S
					ArrayList<String> newS = new ArrayList<String>();
					newS.addAll(S);
					newS.remove(star);
					newS.add(toBeStar);
					
					double delta = 0.0;
					
					//cost for exchanging, the smaller delta, the better
					Iterator<StrDouble> fItr = theFollwers.iterator();
					while(fItr.hasNext()){
						StrDouble follower = fItr.next();
						if(follower.getFirst() == toBeStar){							
							continue;
						}else{
							//change the star of original followers
							StrDouble minPair;
							if(isKLKernel){
								minPair = minDisPairGivenS_KL(follower.getFirst(), newS);
							}else{
								minPair = minDisPairGivenS(follower.getFirst(), newS);
							}
							
							
							delta += ((1-_dLambda)*minPair.second - follower.getSecond());							
						}
					}
					//get the star for the original star
					double minD = (1-_dLambda)*minDisPairGivenS(star, newS).getSecond();
					
					if(isKLKernel){
						minD = (1-_dLambda)*minDisPairGivenS_KL(star, newS).getSecond();
					}else{
						minD = (1-_dLambda)*minDisPairGivenS(star, newS).getSecond();
					}
					
					delta += (minD - followerToStarMap.get(toBeStar).getSecond());					
					
					//relevance part
					delta += ( (0-_dLambda)*calRelevance(toBeStar, query_repr_key) - (0-_dLambda)*calRelevance(star, query_repr_key) );
					
					if(delta < minDelta){
						minDelta = delta;
						minToBeStar = toBeStar;
					}					
				}
				
				if(minDelta < 0){
					change = true;
					
					if(DEBUG){
						System.out.println("Exchange:");
						System.out.println("Original star:\t"+star);
						System.out.println("New star:\t"+minToBeStar);
					}					
					//
					S.remove(star);
					S.add(minToBeStar);
					//
					D_Minus_S.remove(minToBeStar);
					D_Minus_S.add(star);
					
					//update followerToStarMap & starToFollowersMap
					followerToStarMap.clear();
					starToFollowersMap.clear();
										
					Iterator<String> dItr = D.iterator();					
					while(dItr.hasNext()){
						
						String doc_name = dItr.next();
						
						if(!S.contains(doc_name)){											
							StrDouble minPair;
							if(isKLKernel){
								minPair = minDisPairGivenS_KL(doc_name, S);
							}else {
								minPair = minDisPairGivenS(doc_name, S);
							}
							//follower - corresponding star
							followerToStarMap.put(doc_name, new StrDouble(minPair.getFirst(), (1-_dLambda)*minPair.getSecond()));	
							//star - corresponding followers
							if(starToFollowersMap.containsKey(minPair.getFirst())){
								starToFollowersMap.get(minPair.getFirst()).add(new StrDouble(doc_name,(1-_dLambda)*minPair.getSecond()));
							}else{
								HashSet<StrDouble> follwerSet = new HashSet<StrDouble>();					
								follwerSet.add(new StrDouble(doc_name, (1-_dLambda)*minPair.getSecond()));
								starToFollowersMap.put(minPair.getFirst(), follwerSet);
							}				
						}else{
							HashSet<StrDouble> follwerSet = new HashSet<StrDouble>();	
							starToFollowersMap.put(doc_name, follwerSet);
						}
					}					
					//new iterate
					break;					
				}				
			}
			
			if(!change){
				break;
			}	
			
		}while(times < _itrThreshold);	
		
		//sorting in order of contribution
		Set<String> finalS = new HashSet<String>();
		finalS.addAll(S);
		
		Set<String> finalD_minus_S = new HashSet<String>();
		finalD_minus_S.addAll(D_Minus_S);
		
		ArrayList<StrDouble> s_c_list = new ArrayList<StrDouble>();
		Iterator<String> sItr = S.iterator();		
		while(sItr.hasNext()){
			String doc_name_k = sItr.next();	
			s_c_list.add(new StrDouble(doc_name_k, c(doc_name_k, query_repr, finalS, finalD_minus_S, _fVersion)));
		}
		//
		Collections.sort(s_c_list, new PairComparatorBySecond_Desc<String, Double>());
		//
		return s_c_list;
	}
	//////////////////////////
	//Relevance Expression
	//////////////////////////
	/**
	 * @return naive sum relevance of S
	 * */
	private double getSumRelevance(Object query_repr, Set<String> S){
		double sumRelevance = 0.0;				
		for(String doc_name: S){			
			sumRelevance += calRelevance(doc_name, query_repr);
		}
		//
		return sumRelevance;		
	}
	//
	private double calRelevance(String doc_name, Object query_repr){
		String query_repr_key = query_repr.toString();
		Double releScore = null;
		Object doc_repr = null;
		Pair sim_key = null;
		//
		sim_key = new Pair(query_repr_key, doc_name);
		if (null == (releScore = _releCache.get(sim_key))) {
			
			doc_repr = _docRepr.get(doc_name);
			releScore = _releKernel.sim(query_repr, doc_repr);
			_releCache.put(sim_key, releScore);
			
		}
		//
		return releScore;		
	}
	/**
	 * @return averaged sum-relevance
	 * */
	private double getSumRelevance_Averaged(Object query_repr, Set<String> S){
		return getSumRelevance(query_repr, S)/S.size();
	}
	//////////////////////////
	//Diversity Expression
	//////////////////////////
	//	DFA_Diversity	//
	/**
	 * @return get the diversity derived as desirable facility allocation (DFA)
	 * */
	private double calDiversity_DFA(Set<String> S, Set<String> D_Minus_S){
		double sumDistance = 0.0;
		//
		for(String doc_name_i: D_Minus_S){
			sumDistance += minDisGivenS(doc_name_i, S);
		}
		//
		return sumDistance;		
	}
	/**
	 * @return get the averaged diversity derived as desirable facility allocation (DFA)
	 * */
	private double calDiversity_DFA_Averaged(Set<String> S, Set<String> D_Minus_S) {
		return calDiversity_DFA(S, D_Minus_S)/D_Minus_S.size();
	}
	//
	private double minDisGivenS(String doc_name_i, Set<String> S){
		Object doc_repr_i = _docRepr2.get(doc_name_i);
		//
		double minDistance = MAX_VALUE;
		Double dis_score;
		Object doc_repr_j = null;
		Pair dis_key = null;
		for(String doc_name_j: S){
			dis_key = new Pair(doc_name_i, doc_name_j);
			if (null == (dis_score = _disCache.get(dis_key))) {
				doc_repr_j = _docRepr2.get(doc_name_j);
				dis_score = _disKernel.distance(doc_repr_i, doc_repr_j);
				_disCache.put(dis_key, dis_score);
			}
			//
			if (dis_score < minDistance) {
				minDistance = dis_score;
			}
		}
		//
		return minDistance;
	}
	
	private double minDisGivenS_KL(String doc_name_i, Set<String> S){
		Object doc_repr_i = _klDisKernel.getMLELMObjectRepresentation(doc_name_i);
		//
		double minDistance = MAX_VALUE;
		Double dis_score;
		Object doc_repr_j = null;
		Pair dis_key = null;
		for(String doc_name_j: S){
			dis_key = new Pair(doc_name_i, doc_name_j);
			if (null == (dis_score = _disCache.get(dis_key))) {
				doc_repr_j = _klDisKernel.getObjectRepresentation(doc_name_j);
				dis_score = _klDisKernel.distance(doc_repr_i, doc_repr_j);
				_disCache.put(dis_key, dis_score);
			}
			//
			if (dis_score < minDistance) {
				minDistance = dis_score;
			}
		}
		//
		return minDistance;
	}
	
	/**
	 * @return the star element with a minimum distance value in S with respect to doc_name_i
	 * **/
	private double distance(String doc_a, String doc_b){
		Double dis_score = null;
		Pair dis_key = new Pair(doc_a, doc_b);
		
		if (null == (dis_score = _disCache.get(dis_key))) {
			Object doc_aRepr = _docRepr2.get(doc_a);
			Object doc_bRepr = _docRepr2.get(doc_b);
			dis_score = _disKernel.distance(doc_aRepr, doc_bRepr);
			_disCache.put(dis_key, dis_score);
			return dis_score;
		}else{
			return dis_score;
		}
	}
	
	private double distance_KL(String doc_a, String doc_b){
		Double dis_score = null;
		Pair dis_key = new Pair(doc_a, doc_b);
		
		if (null == (dis_score = _disCache.get(dis_key))) {
			Object doc_aRepr = _klDisKernel.getMLELMObjectRepresentation(doc_a);
			Object doc_bRepr = _klDisKernel.getObjectRepresentation(doc_b);
			dis_score = _klDisKernel.distance(doc_aRepr, doc_bRepr);
			_disCache.put(dis_key, dis_score);
			return dis_score;
		}else{
			return dis_score;
		}
	}
	
	private double qdSimilarity_(Object query_repr, String doc_name){		
		String query_repr_key = query_repr.toString();
		
		Double releScore = null;		
		Pair sim_key = new Pair(query_repr_key, doc_name);
		
		if (null == (releScore = _releCache.get(sim_key))) {
			Object doc_repr = _docRepr.get(doc_name);
			releScore = _releKernel.sim(query_repr, doc_repr);
			_releCache.put(sim_key, releScore);
		}
		
		return releScore;			
	}
	/**
	 * find the element is S that has the minimum distance with respect to doc_name_i
	 * **/
	private StrDouble minDisPairGivenS(String doc_name_i, ArrayList<String> S){
		
		Object doc_repr_i = _docRepr2.get(doc_name_i);
		//
		double minDistance = MAX_VALUE;
		String minDoc_name = null;
		
		Double dis_score;
		Object doc_repr_j = null;
		Pair dis_key = null;
		
		for(String doc_name_j: S){
			
			dis_key = new Pair(doc_name_i, doc_name_j);
			
			if (null == (dis_score = _disCache.get(dis_key))) {
				
				doc_repr_j = _docRepr2.get(doc_name_j);
				dis_score = _disKernel.distance(doc_repr_i, doc_repr_j);
				_disCache.put(dis_key, dis_score);
				
			}
			
			if (dis_score < minDistance) {
				minDistance = dis_score;
				minDoc_name = doc_name_j;
			}
		}
		
		return new StrDouble(minDoc_name, minDistance);
	}
	
	private StrDouble minDisPairGivenS_KL(String doc_name_i, ArrayList<String> S){
		
		Object doc_repr_i = _klDisKernel.getMLELMObjectRepresentation(doc_name_i);
		//
		double minDistance = MAX_VALUE;
		String minDoc_name = null;
		
		Double dis_score;
		Object doc_repr_j = null;
		Pair dis_key = null;
		
		for(String doc_name_j: S){
			
			dis_key = new Pair(doc_name_i, doc_name_j);
			
			if (null == (dis_score = _disCache.get(dis_key))) {
				
				doc_repr_j = _klDisKernel.getObjectRepresentation(doc_name_j);
				dis_score = _klDisKernel.distance(doc_repr_i, doc_repr_j);
				_disCache.put(dis_key, dis_score);
				
			}
			
			if (dis_score < minDistance) {
				minDistance = dis_score;
				minDoc_name = doc_name_j;
			}
		}
		
		return new StrDouble(minDoc_name, minDistance);
	}
	//
	private double dContributionOfK_dfa(String doc_name_k, Set<String> S, Set<String> D_Minus_S){
		double cSum = 0.0;
		Iterator<String> itr = D_Minus_S.iterator();
		while(itr.hasNext()){
			cSum += dContributionOfKGivenS_dfa(doc_name_k, S, itr.next());
		}
		//
		return cSum;		
	}
	private double dContributionOfKGivenS_dfa_kl(String doc_name_k, Set<String> S, String doc_name_i){
		Object doc_repr_i = _klDisKernel.getMLELMObjectRepresentation(doc_name_i);
		//
		String mindoc_name = null;
		double minDistance = MAX_VALUE;
		
		Double dis_score;
		Object doc_repr_j = null;
		Pair dis_key = null;
		
		for(String doc_name_j: S){
			
			dis_key = new Pair(doc_name_i, doc_name_j);
			
			if (null == (dis_score = _disCache.get(dis_key))) {
				doc_repr_j = _klDisKernel.getObjectRepresentation(doc_name_j);
				dis_score = _klDisKernel.distance(doc_repr_i, doc_repr_j);
				_disCache.put(dis_key, dis_score);
			}
			//
			if (dis_score < minDistance) {
				minDistance = dis_score;
				mindoc_name = doc_name_j;
			}
		}
		//
		if(mindoc_name.equals(doc_name_k)){
			return minDistance;
		}else{
			//return calDistance(doc_name_i, doc_name_k);
			return 0;
		}		
	}
	
	private double dContributionOfKGivenS_dfa(String doc_name_k, Set<String> S, String doc_name_i){
		Object doc_repr_i = _docRepr2.get(doc_name_i);
		//
		String mindoc_name = null;
		double minDistance = MAX_VALUE;
		
		Double dis_score;
		Object doc_repr_j = null;
		Pair dis_key = null;
		
		for(String doc_name_j: S){
			
			dis_key = new Pair(doc_name_i, doc_name_j);
			
			if (null == (dis_score = _disCache.get(dis_key))) {
				doc_repr_j = _docRepr2.get(doc_name_j);
				dis_score = _disKernel.distance(doc_repr_i, doc_repr_j);
				_disCache.put(dis_key, dis_score);
			}
			//
			if (dis_score < minDistance) {
				minDistance = dis_score;
				mindoc_name = doc_name_j;
			}
		}
		//
		if(mindoc_name.equals(doc_name_k)){
			return minDistance;
		}else{
			//return calDistance(doc_name_i, doc_name_k);
			return 0;
		}		
	}
	
	//
	private double calDistance_kl(String doc_name_i, String doc_name_j){		
		Double dis_score;		
		Pair dis_key = new Pair(doc_name_i, doc_name_j);
		//
		if (null == (dis_score = _disCache.get(dis_key))) {
			Object doc_repr_i = _klDisKernel.getMLELMObjectRepresentation(doc_name_i);
			Object doc_repr_j = _klDisKernel.getObjectRepresentation(doc_name_j);
			dis_score = _klDisKernel.distance(doc_repr_i, doc_repr_j);
			_disCache.put(dis_key, dis_score);
		}
		//
		return dis_score;
	}
	
	private double calDistance(String doc_name_i, String doc_name_j){		
		Double dis_score;		
		Pair dis_key = new Pair(doc_name_i, doc_name_j);
		//
		if (null == (dis_score = _disCache.get(dis_key))) {
			Object doc_repr_i = _docRepr2.get(doc_name_i);
			Object doc_repr_j = _docRepr2.get(doc_name_j);
			dis_score = _disKernel.distance(doc_repr_i, doc_repr_j);
			_disCache.put(dis_key, dis_score);
		}
		//
		return dis_score;
	}
	
	//	Dispersion_Diversity	//
	/**
	 * @return mutually dispersion diversity
	 * */
	private double calDiversity_DISP(Set<String> S){
		double sumDistance = 0.0;
		//
		String [] sArray = S.toArray(new String[0]);		
		Pair dis_key = null;
		Double dis_score;
		Object doc_repr_i = null;
		Object doc_repr_j = null;
		//
		for(int i=0; i<sArray.length; i++){
			String doc_name_i = sArray[i];
			doc_repr_i = _docRepr2.get(doc_name_i);
			for(int j=i+1; j<sArray.length; j++){
				String doc_name_j = sArray[j];
				dis_key = new Pair(doc_name_i, doc_name_j);
				//
				if (null == (dis_score=_disCache.get(dis_key))) {
					doc_repr_j = _docRepr2.get(doc_name_j);
					dis_score = _disKernel.distance(doc_repr_i, doc_repr_j);
					_disCache.put(dis_key, dis_score);
				}
				sumDistance += dis_score;
			}
		}
		//
		return sumDistance;
	}
	/**
	 * @return averaged dispersion diversity
	 * */
	private double calDiversity_DISP_Averaged(Set<String> S){
		return calDiversity_DISP(S)/(S.size()*(S.size()-1));
	}
	//
	private double dContributionOfKGivenS_disp(String doc_name_k, Set<String> S){
		double cSum = 0.0;
		Iterator<String> itr = S.iterator();
		while(itr.hasNext()){
			String doc_name = itr.next();
			if(!doc_name.equals(doc_name_k)){
				cSum += calDistance(doc_name, doc_name_k);
			}
		}
		return cSum;
	}
	//	Proportionality	//
	private double calProportionality(Object query_repr, Set<String> S){
		double[] t_q = (double[])query_repr;
		//
		double [] t_S = new double[t_q.length];		
		for(String doc_name_i: S){
			double [] t_i = (double[])_docRepr.get(doc_name_i);
			for(int k=0; k<t_q.length; k++){
				t_S[k] += t_i[k];
			}
		}
		double [] t_S_NormalizeL1 = VectorUtils.NormalizeL1(t_S);
		//
		double sumDivergence = 0.0;
		for(int k=0; k<t_q.length; k++){
			sumDivergence += Math.pow(t_S_NormalizeL1[k]-t_q[k], 2);
		}
		//
		return sumDivergence;		
	}
	//
	private double calProportionality_Scaled(Object query_repr, Set<String> S){
		double[] t_q = (double[])query_repr;
		return Math.sqrt(1d/t_q.length*calProportionality(query_repr, S));
	}
	//
	private double pContributionOfK(String doc_name_k, Object query_repr){
		double[] t_q = (double[])query_repr;
		double [] t_k = (double[])_docRepr.get(doc_name_k);
		//
		double sumDivergence = 0.0;
		for(int i=0; i<t_q.length; i++){
			sumDivergence += Math.pow(t_k[i]-t_q[i], 2);
		}
		return sumDivergence;
	}
	private double pContributionOfK_scaled(String doc_name_k, Object query_repr){
		double[] t_q = (double[])query_repr;
		return Math.sqrt(1d/t_q.length*pContributionOfK(doc_name_k, query_repr));
	}
	public String getDescription() {
		// TODO Auto-generated method stub
		return "MDP(lambda="+_dLambda+") -ReleKernel: " + _releKernel.getKernelDescription()
									   +"-DisKernel: "+_disKernel.getKernelDescription();
	}
	//
	public String getString(){
		return "MDP="+"["+twoResultFormat.format(_dLambda)+"]"+_releKernel.getString()+"+"+_disKernel.getString();		
	}
	public String getString(fVersion _fVersion){
		return "MDP"+_fVersion.toString();
	}
	
	//////////////////////////
	//Evaluation
	//////////////////////////
	public static final boolean DEBUG = false;
	private static DecimalFormat resultFormat = new DecimalFormat("#.####");
	private static DecimalFormat serialFormat = new DecimalFormat("00");
	//
	public void doEval(
			List<String> evalQueries, 
			HashMap<String,String> docs, 			 
			Map<String,TRECQueryAspects> stdTRECQueryAspects,
			List<Metric> lossFunctions,			
			int cutoffK,			 
			String output_prefix,
			String output_filename,
			fVersion [] fvArray) throws Exception {
		//
		PrintStream ps_per_SLoss  = new PrintStream(new FileOutputStream(output_prefix + output_filename + ".mdp.txt"));
		PrintStream ps_avg_SLoss = new PrintStream(new FileOutputStream(output_prefix + output_filename + ".avg.mdp.txt"));
		PrintStream ps_per_Ndeval = new PrintStream(new FileOutputStream(output_prefix + output_filename + "_ndeval.mdp.txt"));
		PrintStream ps_avg_Ndeval = new PrintStream(new FileOutputStream(output_prefix + output_filename + "_ndeval.avg.mdp.txt"));
		PrintStream err = new DevNullPrintStream(); //new PrintStream(new FileOutputStream(PATH_PREFIX + output_filename + ".errors.txt"));
		//------------
		//for recording the metric values
		Vector<Vector<Double>> usl_vs_rank_vector = new Vector<Vector<Double>>();
		Vector<Vector<Double>> wsl_vs_rank_vector = new Vector<Vector<Double>>();
		Vector<Vector<Double>> ndeval_vector = new Vector<Vector<Double>>();
		//
		for(int k=0; k<fvArray.length; k++){
			//for subtopic loss functions
			Vector<Double> usl_vs_rank = new Vector<Double>();
			Vector<Double> wsl_vs_rank = new Vector<Double>();
			Vector<Double> ndeval = new Vector<Double>();
			for(int j=0; j<cutoffK; j++){
				usl_vs_rank.add(0d);
				wsl_vs_rank.add(0d);
			}
			//for ndeval of 21 metric
			for(int j=0; j<21; j++){
				ndeval.add(0d);
			}
			//
			usl_vs_rank_vector.add(usl_vs_rank);
			wsl_vs_rank_vector.add(wsl_vs_rank);
			ndeval_vector.add(ndeval);
		}		
		//
		int query_serial = 0;
		TRECQuery trecQuery = null;
		for(String qNumber: evalQueries){
			// Get query relevant info
			++query_serial;
			//
			if(null != this._allTREC68Queries){
				trecQuery = _allTREC68Queries.get(qNumber);
			}else if(null != this._allTRECDivQueries){
				trecQuery = _allTRECDivQueries.get(qNumber);				
			}
			//
			TRECQueryAspects qa = stdTRECQueryAspects.get(qNumber);			
			if (DEBUG) {
				System.out.println("- Processing query '" + qNumber + "'");
				System.out.println("- Query details: " + trecQuery);
				System.out.println("- Query aspects: " + qa);
			}
			//refresh D
			clearInfoOfTopNDocs();
			//i.e., the top-n doc
			Set<String> relevant_docs = qa.getTopNDocs();
			if (DEBUG){
				System.out.println("- Evaluating with " + relevant_docs.size() + " docs");
			}			
			//add each new doc to D
			for (String aTopNdoc_name : relevant_docs) {
				if (!docs.containsKey(aTopNdoc_name))
					err.println("ERROR: '" + aTopNdoc_name + "' not found for '" + qNumber + "'");
				else {
					addATopNDoc(aTopNdoc_name);
					if (DEBUG){
						System.out.println("- [" + qNumber + "] Adding " + aTopNdoc_name);
					}					
				}
			}
			// generate the ranked list L			
			// Initialize document set
			initTonNDocsForInnerKernels();
			//
			for(int k=0; k<fvArray.length; k++){
				//k-th type of ranker
				fVersion _fVersion = fvArray[k];
				if (DEBUG){
					System.out.println("\nRunning alg: " + getString(_fVersion));
				}
				ArrayList<String> result_list = getResultList(trecQuery.getQueryContent(), cutoffK, _fVersion);
				//
				for (Metric loss : lossFunctions) {
					String loss_name = loss.getName();
					//System.out.println("Evaluating: " + loss_name);
					Object o = loss.eval(qa, result_list);
					String loss_result_str = null;
					if (o instanceof double[]) {
						loss_result_str = VectorUtils.GetString((double[])o);
					} else {
						loss_result_str = o.toString();
					}					
					// Display results to screen for now
					System.out.println("==================================================");
					System.out.println("Query: " + trecQuery._number + " -> " + trecQuery.getQueryContent());
					System.out.println("MDP Alg: " + getString(_fVersion));
					System.out.println("Loss Function: " + loss.getName());
					System.out.println("Evaluation: " + loss_result_str);
					
					// Maintain averages and export
					if (loss instanceof AllUSLoss) {
						usl_vs_rank_vector.set(k, VectorUtils.Sum(usl_vs_rank_vector.get(k), (double[])o));						
						export(ps_per_SLoss, "q-"+serialFormat.format(query_serial), getString(_fVersion), "USL", (double[])o, (String [])loss.getMetricArray());
					}
					if (loss instanceof AllWSLoss) {
						wsl_vs_rank_vector.set(k, VectorUtils.Sum(wsl_vs_rank_vector.get(k), (double[])o));
						export(ps_per_SLoss, "q-"+serialFormat.format(query_serial), getString(_fVersion), "WSL", (double[])o, (String [])loss.getMetricArray());
					}
					if (loss instanceof NDEvalLosses) {							
						ndeval_vector.set(k, VectorUtils.Sum(ndeval_vector.get(k), (double[])o));
						export(ps_per_Ndeval, "q-"+serialFormat.format(query_serial), getString(_fVersion), "NDEval10\n", (double[])o, (String [])loss.getMetricArray());
					}
					ps_per_SLoss.flush();
					ps_per_Ndeval.flush();
				}
			}			
		}
		clearInfoOfTopNDocs();
		//mean
		for(int k=0; k<fvArray.length; k++){
			usl_vs_rank_vector.set(k, VectorUtils.ScalarMultiply(usl_vs_rank_vector.get(k), 1d/evalQueries.size()));
			wsl_vs_rank_vector.set(k, VectorUtils.ScalarMultiply(wsl_vs_rank_vector.get(k), 1d/evalQueries.size()));
			ndeval_vector.set(k,  VectorUtils.ScalarMultiply(ndeval_vector.get(k), 1d/evalQueries.size()));
		}
		//
		System.out.println("==================================================");
		System.out.println("Exporting ");
		//
		for(int k=0; k<fvArray.length; k++){
			export(ps_avg_SLoss, "Mean", getString(fvArray[k])+"\t", "USL\n", usl_vs_rank_vector.get(k).toArray(new Double[0]), (String [])lossFunctions.get(0).getMetricArray());
			export(ps_avg_SLoss, "Mean", getString(fvArray[k])+"\t", "WSL\n", wsl_vs_rank_vector.get(k).toArray(new Double[0]), (String [])lossFunctions.get(1).getMetricArray());
			export(ps_avg_Ndeval, "Mean", getString(fvArray[k])+"\t", "NDEval10\n", ndeval_vector.get(k).toArray(new Double[0]), (String [])lossFunctions.get(2).getMetricArray());
		}		
		//
		ps_per_SLoss.close();
		ps_avg_SLoss.close();
		ps_per_Ndeval.close();
		ps_avg_Ndeval.close();
		err.close();
		//------------		
	}
	
	public static void export(PrintStream ps, String query_serial, String rankerString, String lossFString, double[] v, String [] metricArray) {
		
		ps.print(query_serial + "\t" + rankerString + "\t" + lossFString);
		for (int i = 0; i < v.length; i++)
			if(metricArray[i].endsWith("\n")){
				ps.print("\t" + metricArray[i].replaceFirst("\n", ":") +resultFormat.format(v[i])+"\n");
			}else {
				ps.print("\t" + metricArray[i] + ":" +resultFormat.format(v[i]));
			}			
		ps.println();		
	}
	
	public static void export(PrintStream ps, String query_serial, String rankerString, String lossFString, Double[] v, String [] metricArray) {
		
		ps.print(query_serial + "\t" + rankerString + "\t" + lossFString);
		for (int i = 0; i < v.length; i++)
			if(metricArray[i].endsWith("\n")){
				ps.print("\t" + metricArray[i].replaceFirst("\n", ":") +resultFormat.format(v[i])+"\n");
			}else {
				ps.print("\t" + metricArray[i] + ":" +resultFormat.format(v[i]));
			}			
		ps.println();		
	}
	
	//--
	public ArrayList<StrDouble> getResultList(DRRunParameter drRunParameter, SMTopic smTopic, ArrayList<String> subtopicList, int cutoff){
		return null;
	}

}
