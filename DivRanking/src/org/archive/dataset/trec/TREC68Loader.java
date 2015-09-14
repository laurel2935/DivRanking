/** Code to load TREC 6-8 Interactive track
 *   
 * @author Scott Sanner (ssanner@gmail.com)
 */

package org.archive.dataset.trec;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.PrintStream;
import java.util.*;

import org.archive.dataset.trec.doc.Doc;
import org.archive.dataset.trec.doc.TRECDoc;
import org.archive.dataset.trec.query.TREC68Query;
import org.archive.dataset.trec.query.TRECQueryAspects;
import org.archive.util.FileFinder;

///////////////////////////////////////////////////////////////////////////////
// Evaluates Different Diversification Algorithms on TREC 6-8 Interactive Track
///////////////////////////////////////////////////////////////////////////////

public class TREC68Loader {

	private final static boolean DEBUG = false;	
	//location
	private final static String TREC68_DOC_DIR = "dataset/trec/TREC6-8/doc";
	private final static String TREC68_QRELS   = "dataset/trec/TREC6-8/qrels.trec.all";
	private final static String QUERY_FILE     = "dataset/trec/TREC6-8/TRECQuery.txt";
	private final static String ASPECT_FILE    = "dataset/trec/TREC6-8/TRECQueryAspects.txt";
	
	/*
	//queries used for evaluation, i.e., without 
	Three of the original 20 queries were discarded due to
	having small candidate sets, making them un-interesting for our experiments!
	{2008-Predicting Diverse Subsets Using Structural SVMs}
	*/
	private final static String[] TREC68_EvalQUERIES = 
		{ "307", "322", "326", "347", "352", "353", "357", "362", "366", "387",
		  "392", "408", "414", "428", "431", "438", "446" };
	//queries used for evaluation
	private static ArrayList<String> ALL_QUERIES = new ArrayList<String>(Arrays.asList(TREC68_EvalQUERIES));	

	///////////////////////////////////////////////////////////////////////////////
	//                              Helper Functions
	///////////////////////////////////////////////////////////////////////////////
	/**
	 * @return generate the file: qrels.trec.all
	 * **/
	private static void ExportQRels(HashMap<String, TRECQueryAspects> aspects2, String filename) {
		try {
			TreeMap<String, TRECQueryAspects> aspects = new TreeMap<String, TRECQueryAspects>(aspects2);
			PrintStream ps = new PrintStream(new FileOutputStream(filename));
			for (Map.Entry<String, TRECQueryAspects> e : aspects.entrySet()) {
				String query = e.getKey();
				TRECQueryAspects qa = e.getValue();
				qa.calcAspectStats();
				for (Map.Entry<String, boolean[]> e2 : qa._aspects.entrySet()) {
					boolean[] aspect_array = e2.getValue();
					if (aspect_array == null) {
						ps.println(query + " 0 " + e2.getKey() + " 0");
						continue;
					}
					//System.out.println(query + " " + e2.getKey() + " " + QueryAspects.getAspectsAsStr(aspect_array));
					for (int i = 0; i < aspect_array.length; i++) {
						if (aspect_array[i])
							ps.println(query + " " + qa.getContiguousID(i) + " " + e2.getKey() + " 1");
					}
				}
			}
			ps.close();
		} catch (Exception e) {
			System.out.println(e);
			e.printStackTrace();
			System.exit(1);
		}
	}

	public enum FilePos { NOTHING, NUMBER, TITLE, DESC, INST};

	/**
	 * Load trec queries
	 * Note: the TREC Query files have a rather non-standard format
	 * **/ 
	private static HashMap<String, TREC68Query> ReadTRECQueries(String query_file) {
		HashMap<String, TREC68Query> queries = new HashMap<String, TREC68Query>();
		BufferedReader br;
		FilePos last_read = FilePos.NOTHING;
		try {
			br = new BufferedReader(new FileReader(query_file));
			String line = null;
			TREC68Query cur_query = null;
			
			while ((line = br.readLine()) != null) {
				line = line.trim();
				if (line.indexOf("----") >= 0 || line.length() == 0) {
					continue;
				} else if (line.indexOf("Number:") >= 0) {
					if (last_read == FilePos.INST) {
						queries.put(cur_query._number, cur_query);
					}
					cur_query = new TREC68Query("", "", "", "");
					last_read = FilePos.NUMBER;
				} else if (line.indexOf("Title:") >= 0) {
					last_read = FilePos.TITLE;
				} else if (line.indexOf("Description:") >= 0) {
					last_read = FilePos.DESC;
				} else if (line.indexOf("Instances:") >= 0) {
					last_read = FilePos.INST;
				} else {
					switch (last_read) {
						case NOTHING: 
							break;
						case NUMBER:
							cur_query._number = line.substring(0, line.length() - 1); // Should only be one line
							break;
						case TITLE:
							cur_query._title += " " + line;
							break;
						case DESC: 
							cur_query._description += " " + line;
							break;
						case INST:
							cur_query._instance += " " + line;
							break;
					}
				}
			}
			if (last_read == FilePos.INST) {
				queries.put(cur_query._number, cur_query);
			}
			br.close();
		
		} catch (Exception e) {
			System.out.println("ERROR: " + e);
			e.printStackTrace();
			System.exit(1);
		}
		//
		return queries;
	}

	private static HashMap<String,TRECQueryAspects> ReadTRECAspects(String aspect_file, String file_root) {
		
		HashMap<String,TRECQueryAspects> aspects = new HashMap<String,TRECQueryAspects>();
		
		String line = null;
		try {
			BufferedReader br = new BufferedReader(new FileReader(aspect_file));
			
			while ((line = br.readLine()) != null) {
				line = line.trim();
				String[] split = line.split("[\\s]");
				//
				String qNumber = split[0].substring(0, split[0].length() - 1);
				String doc_name = split[1];
				String aspect_str = split[split.length-1];
				//
				TRECQueryAspects qa = aspects.get(qNumber);
				if (qa == null) {
					qa = new TRECQueryAspects(qNumber, new Integer(qNumber), file_root + "/" + qNumber);
					aspects.put(qNumber, qa);
				}
				qa.addAspect(doc_name, aspect_str);
			}
			br.close();
			
			// Calculate all aspect stats (e.g., needed for Weighted Subtopic Loss)
			for (TRECQueryAspects q : aspects.values())
				q.calcAspectStats();

		} catch (Exception e) {
			System.out.println("ERROR: " + e + "\npossibly at " + line);
			e.printStackTrace();
			System.exit(1);
		}		
		return aspects;
	}
	
	///////////////////////////////////////////////////////////////////////////////
	//                              Interface
	///////////////////////////////////////////////////////////////////////////////
	/**
	 * @return HashMap<String,TREC68Query>: number->TREC68Query
	 * **/
	public static HashMap<String,TREC68Query> loadTrec68Queries()
	{
		// Build Query map
		HashMap<String,TREC68Query> trec68Queries = ReadTRECQueries(QUERY_FILE);
		//
		if (DEBUG) {
			for (TREC68Query q : trec68Queries.values()){
				System.out.println("TRECQuery: " + q + "\n - content: " + q.getQueryContent());
			}				
		}
		//
		System.out.println("Read " + trec68Queries.size() + " queries");
		
		return trec68Queries;
	}
	/**
	 * @return HashMap<String,TRECQueryAspects>: number->TRECQueryAspects
	 * **/
	public static HashMap<String,TRECQueryAspects> loadTrec68QAspects()
	{
		// Build the DocAspects
		HashMap<String,TRECQueryAspects> trec68QAspects = ReadTRECAspects(ASPECT_FILE, TREC68_DOC_DIR);
		System.out.println("Read " + trec68QAspects.size() + " query aspects");
		if (DEBUG) {
			for (TRECQueryAspects q : trec68QAspects.values())
				System.out.println(q + "\n");
		}
		//ExportQRels(aspects, TREC_QRELS); System.exit(1);
		
		return trec68QAspects;
	}
	
	public static HashMap<String,String> loadTrec68Docs()
	{
		// Build FT Document map
		//HashSet<String> skipSet = new HashSet<String>();
		HashMap<String,String> trec68Docs = new HashMap<String,String>();
		ArrayList<File> files = FileFinder.GetAllFiles(TREC68_DOC_DIR, "", true);
		for (File f : files) {
			//System.out.println(f.toString());
			String[] filename_split = f.toString().split("[\\\\]");
			String query_num = filename_split[filename_split.length - 2];
			if (!ALL_QUERIES.contains(query_num)) {
				/*
				if(!skipSet.contains(query_num)){
					skipSet.add(query_num);
				}
				*/
				//System.out.println(query_num+"\tNot in queries, skipping...");
				continue;
			}
			//
			Doc d = new TRECDoc(f);
			trec68Docs.put(d._name, d.getDocContent());
			
			//if (DEBUG){
			//	System.out.println("TRECDoc: " + f + " -> " + query_num + "/" + d._name/*+ d + "\n - content: " + d.getDocContent()*/);
			//}
			
		}
		//System.out.println(skipSet.size()+"\t"+skipSet);
		System.out.println("Read " + trec68Docs.size() + " documents");
		//for (Object key : Doc._queryToDocNames.keySet()) {
		//	ArrayList al = Doc._queryToDocNames.getValues(key);
		//	System.out.println(key + " " + al.size() + " : " + al);
		//}
		//System.out.println(Doc._queryToDocNames);	
		
		return trec68Docs;
	}
	
	public static String getTrec68QREL(){
		return TREC68_QRELS;
	}
	
	public static List<String> getTrec68EvalQueries(){
		List<String> evalQueries = new ArrayList<String>();
		for(int i=0; i<TREC68_EvalQUERIES.length; i++){
			evalQueries.add(TREC68_EvalQUERIES[i]);
		}
		return evalQueries;
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		
		//HashMap<String,TREC68Query> trec68Queries = TREC68Loader.loadTrec68Queries();
		
		//HashMap<String,TRECQueryAspects> trec68QueryAspects = TREC68Loader.loadTrec68QAspects();
		
		//HashMap<String,String> trec68Docs = TREC68Loader.loadTrec68Docs();
		
	}
}
