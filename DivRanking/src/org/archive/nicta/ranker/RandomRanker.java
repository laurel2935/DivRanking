/** Algorithm for ranking documents similarity score with query
 *  (requires a similarity kernel)	
 * 
 * @author Scott Sanner (ssanner@gmail.com)
 */

package org.archive.nicta.ranker;

import java.util.*;

import org.archive.dataset.ntcir.sm.SMTopic;
import org.archive.dataset.trec.query.TRECDivQuery;
import org.archive.ntcir.dr.rank.DRRunParameter;
import org.archive.util.Permutation;
import org.archive.util.tuple.StrDouble;

public class RandomRanker extends ResultRanker {
	
	// Constructor
	public RandomRanker(HashMap<String, String> docs) { 
		super(docs);
	}
	
	public void addATopNDoc(String doc_name) {
		_docs_topn.add(doc_name);
	}
	
	public void clearInfoOfTopNDocs() {
		_docRepr.clear();
		_docs_topn.clear();
	}
	
	@Override
	public void initTonNDocsForInnerKernels() {
		
	}
	
	public ArrayList<String> getResultList(TRECDivQuery trecDivQuery, int size) {
		return null;
	}

	@Override
	public ArrayList<String> getResultList(String query, int list_size) {
		
		ArrayList<String> result_list = new ArrayList<String>();

		// Get the list of doc names and a random index permutation
		int num_docs = _docRepr.size();
		String[] docs = new String[num_docs];
		int[] permutation = Permutation.permute(num_docs);
		
		// Return the permutation
		for (int i = 0; i < list_size && i < num_docs; i++)
			result_list.add(docs[permutation[i]]);
		
		return result_list;		
	}
	
	@Override
	public String getDescription() {
		return "RandomRanker";
	}
	public String getString(){
		return "RandomRanker";
	}
	
	//--
	public ArrayList<StrDouble> getResultList(DRRunParameter drRunParameter, SMTopic smTopic, ArrayList<String> subtopicList, int cutoff){
		return null;
	}
}
