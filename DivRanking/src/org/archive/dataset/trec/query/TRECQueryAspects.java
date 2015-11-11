/** Query aspect representation for TREC Interactive and CLUEWEB Diversity tracks
 *   
 * @author Scott Sanner (ssanner@gmail.com)
 */

package org.archive.dataset.trec.query;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.archive.util.format.StandardFormat;
import org.archive.util.tuple.Pair;

/**
 * A class data structure to store the standard relevance of documents, 
 * and the top-n real documents
 * **/ 
public class TRECQueryAspects implements Comparable<TRECQueryAspects> {
	public int _intNumber = -1; 
	public String _number = null;
	//number of subtopics
	public int _numAspects = -1;
	/**
	 * doc_name -> relevant aspects(binary array)
	 * Difference among trec68, 2009 and 2010
	 * ->2009, the overview says:
	 * For the diversity task, documents were judged with respect to the subtopics. For each subtopic,
	 * NIST assessors made a binary judgment as to whether or not the document satisfies the information need
	 * associated with the subtopic. However, for the diversity task, a document may not be relevant to 
	 * any subtopic, even if it is relevant to the overall topic.
	 * By this saying, should we think that: once a doc_name appears in the "qrels.final-format", it is relevant,
	 * though it is non-relevant to any subtopics? the same for trec68?
	 * ->2010, no such saying!
	 * 
	 * For consistence, what we do: we merely think:
	 * Iff a document is relevant to a or more subtopics, it is a relevant document!
	 * thus the doc_name with null value of "boolean[]" is not accepted, though it appears in "qrels.final-format"!
	 * **/
	public HashMap<String, boolean[]> _aspects = null;
	//subtopicSerial(from 0) -> count of real relevant documents
	public HashMap<Integer, Integer> _subtopic2ID = null;
	//top-n documents by the initial retrieval
	public Set<String> _topnDocs = null;
	//count of documents relevant to each subtopic or aspect
	public double[] _freq = null;
	//weight of each subtopic or aspect normalized using count of relevant documents
	public double[] _weights = null;
	//for computing nDCG
	public HashMap<Integer, HashSet<String>> _subtopic2ReleSet;
	
	//public TRECQueryAspects(String query_name) {
	//	this(query_name, -1, null);
	//}
	
	public TRECQueryAspects(int intID, String doc_source) {
		if(intID<=100 && intID>=1){
			_number = (intID <= 50 ? "wt09-":"wt10-") + intID;
		}else{
			_number = StandardFormat.serialFormat(intID, "000");
		}		
		_intNumber = intID;
		//doc_name -> relevant aspects
		_aspects = new HashMap<String,boolean[]>();
		_subtopic2ID = new HashMap<Integer, Integer>();
		_topnDocs = new HashSet<String>();
		//
		_subtopic2ReleSet = new HashMap<Integer, HashSet<String>>();
		//read the top-n documents
		if (doc_source != null) {
			File dir = new File(doc_source);
			//System.out.println("Trying dir: " + doc_source);
			String[] available_docs = dir.list();
			for (String s : available_docs) {
				//System.out.println("Adding: " + s);
				_topnDocs.add(s);
				//System.out.println(s);
			}
		}
	}
	
	public TRECQueryAspects(String number, int intID, String doc_source) {	
		_number = number;
		_intNumber = intID;
		//doc_name -> relevant aspects
		_aspects = new HashMap<String,boolean[]>();
		_subtopic2ID = new HashMap<Integer, Integer>();
		_topnDocs = new HashSet<String>();
		//
		_subtopic2ReleSet = new HashMap<Integer, HashSet<String>>();
		//read the top-n documents
		if (doc_source != null) {
			File dir = new File(doc_source);
			//System.out.println("Trying dir: " + doc_source);
			String[] available_docs = dir.list();
			for (String s : available_docs) {
				//System.out.println("Adding: " + s);
				_topnDocs.add(s);
				//System.out.println(s);
			}
		}
	}
	
	public TRECQueryAspects(int intID, HashMap<String, ArrayList<String>> baselineMap) {
		_number = StandardFormat.serialFormat(intID, "000");
		
		/*
		if(intID<=100 && intID>=1){
			_number = (intID <= 50 ? "wt09-":"wt10-") + intID;
		}else{
			_number = StandardFormat.serialFormat(intID, "000");
		}
		*/
		_intNumber = intID;
		//doc_name -> relevant aspects
		_aspects = new HashMap<String,boolean[]>();
		_subtopic2ID = new HashMap<Integer, Integer>();
		_topnDocs = new HashSet<String>();
		//
		_subtopic2ReleSet = new HashMap<Integer, HashSet<String>>();
		//read the top-n documents
		if(null == baselineMap){
			System.out.println("null baseline map");
			System.exit(0);
		}else if(null == baselineMap.get(_number)){
			
			System.out.println(_number);
			System.out.println("non exist error");
			System.exit(0);
		}
		_topnDocs.addAll(baselineMap.get(_number));
	}
	
	/**
	 * @return the real relevant documents judged by human beings
	 * **/
	public Set<String> getRelevantDocsInQRELS() {		
		HashSet<String> qrelsRelevantDocs = new HashSet<String>();
		//
		for(Map.Entry<String, boolean[]> entry: _aspects.entrySet()){
			if (null != entry.getValue()) {
				qrelsRelevantDocs.add(entry.getKey());
			}else{
				//System.out.println(_number+"\t"+entry.getKey());
			}
		}	
		//
		return qrelsRelevantDocs;		
	}
	/**
	 * the top-n documents returned by a baseline IR model
	 * **/
	public Set<String> getTopNDocs() {
		return _topnDocs;
	}
	//
	public Set<String> getRelevantDocsInTopNDocs(){
		HashSet<String> relevantDocsInTopNDocs = new HashSet<String>();
		Iterator<String> itr = _topnDocs.iterator();
		while(itr.hasNext()){
			String doc_name = itr.next();
			if(_aspects.containsKey(doc_name)){
				if(null != _aspects.get(doc_name)){
					relevantDocsInTopNDocs.add(doc_name);
				}
			}
		}
		//
		return relevantDocsInTopNDocs;
	}
	//for trecD0910
	/**
	 * @param doc2aspects doc_name -> relevant set of {subtopic number}
	 * **/
	public void addAllAspects(HashMap<String,TreeSet<Integer>> doc2aspects, int max_aspect) {

		_numAspects = max_aspect;
		for (Map.Entry<String,TreeSet<Integer>> e : doc2aspects.entrySet()) {

			String doc = e.getKey();
			TreeSet<Integer> aspects = e.getValue();
			boolean[] b_aspects = null;
			
			if (aspects != null && aspects.size() > 0) {			
				b_aspects = new boolean[_numAspects];
				for (Integer i : aspects) // aspects are never 0
					b_aspects[i-1] = true;
			}
			
			// Can be null
			_aspects.put(doc, b_aspects);
		}
	}
	//for trec68
	public void addAspect(String doc, String aspect_str) {
		if (_numAspects < 0){
			_numAspects = aspect_str.length();
		}		
		//System.out.println("Adding aspect: " + doc + " :: " + aspect_str);
		boolean[] b_aspects = new boolean[_numAspects];
		//count of relevant times for a specific document
		int aspect_count = 0;
		for (int i = 0; i < _numAspects; i++)
			if ((b_aspects[i] = (aspect_str.charAt(i) == '1')))
				aspect_count++;
		//if relevant
		if (aspect_count > 0)
			_aspects.put(doc, b_aspects);
		else
			_aspects.put(doc, null);
	}
	
	public void calcAspectStats() {
		_freq    = new double[_numAspects];
		_weights = new double[_numAspects];
		for (boolean[] aspects : _aspects.values()) {
			if (aspects != null)
				for (int i = 0; i < _numAspects; i++)
					if (aspects[i])
						_freq[i]++;
		}

		int total = 0;
		for (int i = 0; i < _numAspects; i++)
			total += _freq[i];
		
		int id = 1;
		for (int i = 0; i < _numAspects; i++) {
			if (_freq[i] > 0d)
				_subtopic2ID.put(i, id++);
			_weights[i] = _freq[i] / (double)total;
		}
	}
	
	public Integer getContiguousID(int i) {
		return _subtopic2ID.get(i);
	}
	
	public String toString() {		
		StringBuilder sb = new StringBuilder("Query Aspects for '" + _number + "'\n========================");
		/*
		sb.append("\n- Official Doc Aspects:\n");
		for (Map.Entry<String, boolean[]> e : _aspects.entrySet())
			sb.append(e.getKey() + " -> " + getAspectsAsStr(e.getValue()) + "\n");
		*/
		//
		sb.append("- Freq -> ");
		for (int i = 0; i < _numAspects; i++)
			sb.append(i + ":" + _freq[i] + " ");
		sb.append("\n- Weight -> ");
		//
		for (int i = 0; i < _numAspects; i++)
			sb.append(i + ":" + _weights[i] + " ");
		//
		sb.append("\n- Number of topnDocs: -> "+_topnDocs.size());
		//
		sb.append("\n- Number of relevant docs in top-n: -> "+getRelevantDocsInTopNDocs().size());
		//
		/*
		for(Entry<Integer, Integer> entry: _subtopic2ID.entrySet()){
			if(entry.getValue() == 0){
				sb.append("\n"+entry.getKey()+"-th Subtopic has "+entry.getValue() +" relevant documents!");
			}
		}
		*/
		//		
		return sb.toString();
	}
	
	public static String getAspectsAsStr(boolean[] b) {
		
		if (b == null)
			return "null";
		
		StringBuilder sb = new StringBuilder(" ");
		for (int i = 0; i < b.length; i++)
			if (b[i])
				sb.append(" " + i);
		return sb.toString().substring(1,sb.length());
	}
	/////////////////////////
	//Subtopic Loss & Recall
	/////////////////////////
	public double getUniformSubtopicLoss(List<String> doc_names, int k) {
		boolean[] b_aspects = new boolean[_numAspects];
		int doc_count = 0;
		for (String doc_name : doc_names) {
			if (doc_count++ >= k)
				break;
			boolean[] d_aspects = _aspects.get(doc_name);
			if (d_aspects != null) {
				for (int i = 0; i < _numAspects; i++)
					b_aspects[i] = b_aspects[i] || d_aspects[i];
			}
		}
		int count_aspects = 0;
		for (int i = 0; i < _numAspects; i++)
			if (b_aspects[i])
				count_aspects++;
		
		return count_aspects / (double)_numAspects;
	}
	
	public double getUniformSubtopicLoss_A1(List<String> doc_names, int k) {
		boolean[] b_aspects = new boolean[_numAspects];
		int doc_count = 0;
		for (String doc_name : doc_names) {
			if (doc_count++ >= k)
				break;
			boolean[] d_aspects = _aspects.get(doc_name);
			if (d_aspects != null) {
				for (int i = 0; i < _numAspects; i++)
					b_aspects[i] = b_aspects[i] || d_aspects[i];
			}
		}
		int count_loss_aspects = 0;
		for (int i = 0; i < _numAspects; i++)
			if (!b_aspects[i])
				count_loss_aspects++;
		
		return count_loss_aspects / (double)_numAspects;
	}
	
	public double getUniformSubtopicRecall(List<String> doc_names, int k) {
		boolean[] b_aspects = new boolean[_numAspects];
		int doc_count = 0;
		for (String doc_name : doc_names) {			
			boolean[] d_aspects = _aspects.get(doc_name);
			if (d_aspects != null) {
				for (int i = 0; i < _numAspects; i++)
					b_aspects[i] = b_aspects[i] || d_aspects[i];
			}
			
			if (doc_count++ >= k)
				break;
		}
		int count_aspects = 0;
		for (int i = 0; i < _numAspects; i++)
			if (b_aspects[i])
				count_aspects++;
		
		return count_aspects / (double)_numAspects;
	}
	
	public double getWeightedSubtopicLoss(List<String> doc_names, int k) {
		boolean[] b_aspects = new boolean[_numAspects];
		int doc_count = 0;
		for (String doc_name : doc_names) {
			if (doc_count++ >= k)
				break;
			boolean[] d_aspects = _aspects.get(doc_name);
			if (d_aspects != null) {
				for (int i = 0; i < _numAspects; i++)
					b_aspects[i] = b_aspects[i] || d_aspects[i];
			}
		}
		
		double weight = 0d;
		for (int i = 0; i < _numAspects; i++)
			if (b_aspects[i])
				weight += _weights[i]; // If all aspects on, weight will be 1.0

		return weight;
	}
	
	public double getWeightedSubtopicLoss_A1(List<String> doc_names, int k) {
		boolean[] b_aspects = new boolean[_numAspects];
		int doc_count = 0;
		for (String doc_name : doc_names) {
			if (doc_count++ >= k)
				break;
			boolean[] d_aspects = _aspects.get(doc_name);
			if (d_aspects != null) {
				for (int i = 0; i < _numAspects; i++)
					b_aspects[i] = b_aspects[i] || d_aspects[i];
			}
		}
		
		double weight_loss_aspects = 0d;
		for (int i = 0; i < _numAspects; i++)
			if (!b_aspects[i])
				weight_loss_aspects += _weights[i]; // If all aspects on, weight will be 1.0

		return weight_loss_aspects;
	}
	/////////////////////////
	//subtopic specific nDCG with topn documents
	/////////////////////////
	public void iniSubtopic2ReleSet(){
		for(Entry<String, boolean[]> entry : _aspects.entrySet()){
			String doc_name = entry.getKey();
			boolean[] b_aspects = entry.getValue();
			if(null != b_aspects){
				for(int i=0; i<b_aspects.length; i++){
					if(b_aspects[i]){
						if(_subtopic2ReleSet.containsKey(i)){
							_subtopic2ReleSet.get(i).add(doc_name);
						}else{
							HashSet<String> releSet = new HashSet<String>();
							releSet.add(doc_name);
							_subtopic2ReleSet.put(i, releSet);
						}
					}
				}
			}
		}
	}
	/**
	 * @return the ideal list in order of global gain
	 * **/
	public ArrayList<org.archive.util.tuple.Pair<String, Double>> getIdealListDecByGG (int subtopicIndex){
		ArrayList<org.archive.util.tuple.Pair<String, Double>> idealListDecByGG = 
			new ArrayList<org.archive.util.tuple.Pair<String,Double>>();
		//
		if(_subtopic2ReleSet.size() <= 0){
			//initialize
			iniSubtopic2ReleSet();
		}
		//
		HashSet<String> releSet = _subtopic2ReleSet.get(subtopicIndex);
		if(null == releSet){
			return null;
		}
		for(String doc_name: releSet){
			if(_topnDocs.contains(doc_name)){
				idealListDecByGG.add(new org.archive.util.tuple.Pair<String, Double>(doc_name, 1.0));
			}			
		}
		for(String doc_name: _topnDocs){
			//thus it is a non-relevant document
			if(!releSet.contains(doc_name)){
				idealListDecByGG.add(new org.archive.util.tuple.Pair<String, Double>(doc_name, 0d));
			}
		}
		//
		return idealListDecByGG;
	}
	//
	private double getIdealCGN(ArrayList<org.archive.util.tuple.Pair<String, Double>> idealListDecByGG,
			int cutoff){
		//
		double cgn = 0.0;		
		for(int i=0; i<cutoff; i++){
			cgn += idealListDecByGG.get(i).second/Math.log10(i+2);
		}
		return cgn;
	}
	private double getSysCGN(ArrayList<String> sysRankedItemList, int cutoff,
			ArrayList<org.archive.util.tuple.Pair<String, Double>> idealListDecByGG){
		//		
		double cgn = 0.0;
		for(int k=0; k<cutoff; k++){
			String k_th_item = sysRankedItemList.get(k);
			for(Pair<String, Double> idealRankedItemByGG: idealListDecByGG){
				if(idealRankedItemByGG.first.equals(k_th_item)){
					cgn += idealRankedItemByGG.second/Math.log10(k+2);
					//
					break;
				}
			}
		}		
		return cgn;		
	}
	//
	public Object calMSnDCG(int subtopicIndex, ArrayList<String> sysRankedItemList, int cutoffK){
		double scores[] = new double[cutoffK];
		//
		ArrayList<org.archive.util.tuple.Pair<String, Double>> idealListDecByGG =
			getIdealListDecByGG(subtopicIndex);		
		//all 0 values iff no relevant documents
		if(null == idealListDecByGG || idealListDecByGG.get(0).getSecond()==0){
			return scores;
		}
		//
		//System.out.println(sysRankedItemList);
		//System.out.println(idealListDecByGG);
		//
		int cursor = Math.min(sysRankedItemList.size(), cutoffK);
		if(cursor > 100){
			new Exception().printStackTrace();
			System.out.println("error!");
			System.exit(1);			
		}
		//here k as cutoff, thus k>=1
		for(int k=1; k<=cursor; k++){
			double sysCGN_at_k = getSysCGN(sysRankedItemList, k, idealListDecByGG);			
			double idealCGN_at_k = getIdealCGN(idealListDecByGG, k);
			
			scores [k-1] = sysCGN_at_k/idealCGN_at_k;  
			//
			/*
			if(sysCGN_at_k > 0){
				System.out.println(sysCGN_at_k + "\tscores["+(k-1)+"]="+scores [k-1]);
			}
			*/
		}
		//
		return scores;
	}
	//
	//@Override
	public int compareTo(TRECQueryAspects o) {
		return _number.compareTo(o._number);
	}	
	
	/////////////////////
	//ratio of documents that provide relevant information, with a cutoff k
	/////////////////////
	public double getUtilityRatio(List<String> doc_names, int k){
		boolean[] b_aspects = new boolean[_numAspects];
		int doc_count = 0;
		int releCount = 0;
		
		for (String doc_name : doc_names) {			
			boolean[] d_aspects = _aspects.get(doc_name);
			if (d_aspects != null) {
				for (int i = 0; i < _numAspects; i++){
					b_aspects[i] = b_aspects[i] || d_aspects[i];
					
					if (b_aspects[i]){
						releCount ++;
						break;
					}
				}				
			}
			
			if (doc_count++ >= k){
				break;
			}				
		}			
		
		return releCount / (double)k;
	}
}