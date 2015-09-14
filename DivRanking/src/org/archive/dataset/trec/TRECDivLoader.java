package org.archive.dataset.trec;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.archive.dataset.DataSetDiretory;
import org.archive.dataset.trec.doc.CLUEDoc;
import org.archive.dataset.trec.doc.Doc;
import org.archive.dataset.trec.query.TREC68Query;
import org.archive.dataset.trec.query.TRECDivQuery;
import org.archive.dataset.trec.query.TRECQueryAspects;
import org.archive.dataset.trec.query.TRECSubtopic;
import org.archive.util.FileFinder;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

public class TRECDivLoader {
	
	public static enum DivVersion{Div2009, Div2010, Div20092010}
		
	private final static boolean DEBUG = false;
	private final static DecimalFormat _df = new DecimalFormat("#.####");	

	private final static String Div20092010_DOC_DIR	= DataSetDiretory.ROOT+"trec/TREC20092010/ClueWeb-CatB/Clean/OKAPI-Result-Clean";
	private final static String QUERY_FILE_2009     = DataSetDiretory.ROOT+"trec/TREC20092010/wt09.topics.queries-only";
	private final static String QUERY_FILE_2010     = DataSetDiretory.ROOT+"trec/TREC20092010/wt10.topics.queries-only";
	private final static String QUERY_FILE_2009_xml = DataSetDiretory.ROOT+"trec/TREC20092010/wt09.topics.full.xml";
	private final static String QUERY_FILE_2010_xml = DataSetDiretory.ROOT+"trec/TREC20092010/wt2010-topics.xml";
	
	//private final static String ASPECT_FILE 		   = "dataset/trec/TREC20092010/qrels.diversity.all";
	//private final static String aROOT = "/cygdrive/H/v-haiyu/CodeBench/Pool_DataSet/DataSet_DiversifiedRanking/";
	private final static String TREC_DivQRELS_20092010 = DataSetDiretory.ROOT+"trec/TREC20092010/qrels.diversity.0910";
	private final static String TREC_DivQRELS_09       = DataSetDiretory.ROOT+"trec/TREC20092010/09.diversity-qrels.final";
	private final static String TREC_DivQRELS_10       = DataSetDiretory.ROOT+"trec/TREC20092010/10.diversity-qrels.final";	
	//private final static String TREC_DivQRELS_20092010 = aROOT+"trec/TREC20092010/qrels.diversity.0910";
	//private final static String TREC_DivQRELS_09       = aROOT+"trec/TREC20092010/09.diversity-qrels.final";
	//private final static String TREC_DivQRELS_10       = aROOT+"trec/TREC20092010/10.diversity-qrels.final";
	
	// /////////////////////////////////////////////////////////////////////////////
	// Helper Functions
	// /////////////////////////////////////////////////////////////////////////////

	// Note: the TREC Query files have a rather non-standard format
	private static Map<String, TREC68Query> _ReadD0910Queries(List<String> query_files) {
		TreeMap<String, TREC68Query> queries = new TreeMap<String, TREC68Query>();
		BufferedReader br;
		try {
			for (String query_file : query_files) {
				br = new BufferedReader(new FileReader(query_file));
				String line = null;
				//
				while ((line = br.readLine()) != null) {
					line = line.trim();
					String split[] = line.split("[:]");
					TREC68Query query = new TREC68Query(split[0], split[1], "",	"");
					queries.put(query._number, query);
				}
				br.close();
			}
		} catch (Exception e) {
			System.out.println("ERROR: " + e);
			e.printStackTrace();
			System.exit(1);
		}
		//
		return queries;
	}

	public static Map<String, TRECQueryAspects> ReadDivQueryAspects(DivVersion divVersion, String file_root) {
		String aspect_file = null;
		if(DivVersion.Div2009 == divVersion){
			aspect_file = TREC_DivQRELS_09;						
		}else if(DivVersion.Div2010 == divVersion){
			aspect_file = TREC_DivQRELS_10;	
		}else if(DivVersion.Div20092010 == divVersion) {
			aspect_file = TREC_DivQRELS_20092010;	
		}else{
			System.out.println("ERROR: unexpected DivVersion!");
			new Exception().printStackTrace();
			System.exit(1);				
		}

		Map<String, TRECQueryAspects> aspects = new TreeMap<String, TRECQueryAspects>();

		String line = null;
		HashSet<Integer> ids = new HashSet<Integer>();
		for (int i = 1; i <= 100; i++)
			ids.add(i);
		try {
			BufferedReader br = new BufferedReader(new FileReader(aspect_file));
			//doc_name -> relevant set of {subtopic number}
			HashMap<String, TreeSet<Integer>> cur_aspects = new HashMap<String, TreeSet<Integer>>();
			TRECQueryAspects cur_qa = null;
			int max_aspect = -1;
			int last_query_id = -1;

			while ((line = br.readLine()) != null) {
				line = line.trim();
				String[] split = line.split("[\\s]");

				boolean relevant = split[3].equals("1");

				int query_id = new Integer(split[0]);
				int aspect_id = new Integer(split[1]);
				String doc = split[2];

				if (query_id != last_query_id) {
					// We're on a new query, add all aspects for old query

					// Calc stats for old query
					if (cur_qa != null)
						cur_qa.addAllAspects(cur_aspects, max_aspect);

					// Make a new query and rest aspects / max
					max_aspect = -1;
					cur_aspects.clear();
					cur_qa = new TRECQueryAspects((query_id <= 50 ? "wt09-":"wt10-") + query_id, query_id, file_root+"/"+query_id);
					aspects.put(cur_qa._number, cur_qa);
					ids.remove(query_id);
				}
				last_query_id = query_id;

				// Add aspect to current query
				TreeSet<Integer> aspect_set = cur_aspects.get(doc);
				if (aspect_set == null) {
					aspect_set = new TreeSet<Integer>();
					cur_aspects.put(doc, aspect_set);
				}
				if (relevant) {
					aspect_set.add(aspect_id);
					if (aspect_id > max_aspect)
						max_aspect = aspect_id;
				}
			}
			br.close();

			// End of file - add last aspect
			if (cur_qa != null)
				cur_qa.addAllAspects(cur_aspects, max_aspect);

			// Calculate all aspect stats (e.g., needed for Weighted Subtopic
			// Loss)
			for (TRECQueryAspects q : aspects.values())
				q.calcAspectStats();

		} catch (Exception e) {
			System.out.println("ERROR: " + e + "\npossibly at " + line);
			e.printStackTrace();
			System.exit(1);
		}

		// No aspects available
		// aspects.put("wt10-95", new QueryAspects("wt10-95", file_root));
		// aspects.put("wt10-100", new QueryAspects("wt10-100", file_root));

		return aspects;
	}

	///////////////////////////////////////////////////////////////////////////////
	//                              Interface
	///////////////////////////////////////////////////////////////////////////////
	
	public static Map<String, TREC68Query> _loadTrecD0910Queries(){
		// Build Query map
		///*
		Map<String, TREC68Query> queries = 
			_ReadD0910Queries(Arrays.asList(new String[] {QUERY_FILE_2009, QUERY_FILE_2010}));
		//*/
		/*
		Map<String, TRECD0910Query> queries = 
			loadTrecD0910Queries(Arrays.asList(new String[] {QUERY_FILE_2009_xml, QUERY_FILE_2010_xml}));
		*/
		//
		if (DEBUG) {
			for (TREC68Query q : queries.values()){
				System.out.println("TRECD0910-Query: " + q + "\n- content: " + q.getQueryContent()+"\n");				
			}				
		}
		//
		System.out.println("Read " + queries.size() + " queries without filtering!");
		//
		return queries;
	}
	
	public static Map<String, TRECDivQuery> loadTrecDivQueries(DivVersion divVersion){
		List<String> queryFileList = new ArrayList<String>();
		if(DivVersion.Div2009 == divVersion){
			queryFileList.add(QUERY_FILE_2009_xml);				
		}else if(DivVersion.Div2010 == divVersion){
			queryFileList.add(QUERY_FILE_2010_xml);
		}else if(DivVersion.Div20092010 == divVersion) {
			queryFileList.add(QUERY_FILE_2009_xml);
			queryFileList.add(QUERY_FILE_2010_xml);
		}else{
			System.out.println("ERROR: unexpected DivVersion!");
			new Exception().printStackTrace();
			System.exit(1);				
		}
		// Build Query map
		Map<String, TRECDivQuery> queries = loadTrecD0910Queries(queryFileList);
		//
		if (DEBUG) {
			for (TRECDivQuery q : queries.values()){
				System.out.println("TRECDivQuery: " + q + "\n- content: " + q.getQueryContent()+"\n");				
			}				
		}
		//
		System.out.println("Read " + queries.size() + " queries from xml!");
		//
		return queries;
	}

	public static Map<String, TRECQueryAspects> loadTrecDivQueryAspects(DivVersion divVersion){
		// Build the DocAspects
		Map<String, TRECQueryAspects> aspects = ReadDivQueryAspects(divVersion, Div20092010_DOC_DIR);
		
		System.out.println("Read " + aspects.size() + " query aspects");
		///*
		if (DEBUG) {
			int count = 0;
			for (TRECQueryAspects q : aspects.values()) {
				//? merely depends on null==boolean[] or the keys of _aspects
				Set<String> reldocs = q.getRelevantDocsInQRELS();
				//
				Set<String> availdocs = q.getTopNDocs();
				//
				Set<String> avail_and_rel_docs = new HashSet<String>(availdocs);
				//
				avail_and_rel_docs.retainAll(reldocs);
				//
				double frac_avail = avail_and_rel_docs.size() / (double) reldocs.size();
				
				if (avail_and_rel_docs.size() < 1) {
					count ++;
					System.out.print(q._number);
					System.out.println("\t" + _df.format(frac_avail) + "\t"  + avail_and_rel_docs.size() + "\t" + reldocs.size());
				}
			}
			//
			System.out.println(count + " queries will be filtered out!");
		}
		//*/
		//
		return aspects;
	}
	
	public static HashMap<String, String> loadTrecDivDocs(){
		// Build Document map per query
		HashMap<String, String> docs = new HashMap<String, String>();
		ArrayList<File> files = FileFinder.GetAllFiles(Div20092010_DOC_DIR, "", true);
		int count = 0;
		for (File f : files) {
			// System.out.println("Reading: " + f);
			Doc d = new CLUEDoc(f);
			docs.put(d._name, d.getDocContent());
			if (DEBUG)
				System.out.println("CLUEDoc: " + f + " -> " + d + "\n - content: " + d.getDocContent());
			if (++count % 500 == 0)
				System.out.println("Read " + count + " documents");
		}
		System.out.println("Read total of " + count + " (unique: " + docs.size() + ") documents");
		//
		return docs;
	}
	
	public static String getTrecDivQREL(DivVersion divVersion){
		if(DivVersion.Div2009 == divVersion){
			return TREC_DivQRELS_09;						
		}else if(DivVersion.Div2010 == divVersion){
			return TREC_DivQRELS_10;	
		}else if(DivVersion.Div20092010 == divVersion) {
			return TREC_DivQRELS_20092010;	
		}else{
			System.out.println("ERROR: unexpected DivVersion!");
			new Exception().printStackTrace();
			System.exit(1);
			return null;
		}		
	}
	
	private static boolean accept(int number, DivVersion divVersion){
		if(DivVersion.Div2009 == divVersion){
			if(1<=number && number<=50){
				return true;
			}else {
				return false;
			}
		}else if(DivVersion.Div2010 == divVersion){
			if(50<number && number<=100){
				return true;
			}else {
				return false;
			}
		}else if(DivVersion.Div20092010 == divVersion) {
			if(1<=number && number<=100){
				return true;
			}else {
				return false;
			}
		}else{
			System.out.println("ERROR: unexpected DivVersion!");
			new Exception().printStackTrace();
			System.exit(1);	
			return false;
		}
	}
	/**
	 * @return using the relevant documents of the top-n documents as training documents
	 * @param threshold : the least number of relevant documents 
	 * **/
	public static List<String> getDivEvalQueries(DivVersion divVersion,
			Map<String,TRECQueryAspects> divTRECQueryAspects, int threshold){
		//
		List<String> qList = getDivEvalQueries(divVersion);
		List<String> divEvalQueries = new ArrayList<String>();
		//
		for(String number: qList){
			Set<String> relevantDocsInTopNDocs = divTRECQueryAspects.get(number).getRelevantDocsInTopNDocs();
			if(null!=relevantDocsInTopNDocs && relevantDocsInTopNDocs.size()>=threshold){
				divEvalQueries.add(number);
			}
		}
		//
		return divEvalQueries;		 
	}
	
	public static List<String> getDivEvalQueriesWithFiltering(DivVersion divVersion){
		List<String> divEvalQueries = new ArrayList<String>();
		//
		for (int i = 1; i <= 100; i++) {
			if(accept(i, divVersion)){
				divEvalQueries.add((i <= 50 ? "wt09-" : "wt10-") + i);
			}			
		}
		//
		// Remove these results which had < 2 relevant documents in retrieval, i.e., avail_and_rel_docs.size() < 2
		// set
		// wt09-44 0.002 1 598
		// wt10-54 0 0 254
		// wt10-55 0 0 126
		// wt10-59 0 0 46
		// wt10-70 0 0 98
		// wt10-71 0 0 90
		// wt10-72 0 0 83
		// wt10-83 0 0 82
		// wt10-92 0 0 29
		// wt10-95 ? 0 0 <-- no relevant aspects listed
		// wt10-100 ? 0 0 <-- no relevant aspects listed
		divEvalQueries.remove("wt09-44");
		divEvalQueries.remove("wt10-100");
		divEvalQueries.remove("wt10-54");
		divEvalQueries.remove("wt10-55");
		divEvalQueries.remove("wt10-59");
		divEvalQueries.remove("wt10-70");
		divEvalQueries.remove("wt10-71");
		divEvalQueries.remove("wt10-72");
		divEvalQueries.remove("wt10-83");
		divEvalQueries.remove("wt10-92");
		divEvalQueries.remove("wt10-95");
		//		
		System.out.println(divEvalQueries.size() + " queries: " + divEvalQueries);
		return divEvalQueries;
		/*
		public static String[] DivEvaluation_QUERIES_USED = null;
		static {
			ArrayList<String> queries = new ArrayList<String>();
			for (int i = 1; i <= 100; i++) {
				queries.add((i <= 50 ? "wt09-" : "wt10-") + i);
			}

			// Remove these results which had < 2 relevant documents in retrieval, i.e., avail_and_rel_docs.size() < 2
			// set
			// wt09-44 0.002 1 598
			// wt10-54 0 0 254
			// wt10-55 0 0 126
			// wt10-59 0 0 46
			// wt10-70 0 0 98
			// wt10-71 0 0 90
			// wt10-72 0 0 83
			// wt10-83 0 0 82
			// wt10-92 0 0 29
			// wt10-95 ? 0 0 <-- no relevant aspects listed
			// wt10-100 ? 0 0 <-- no relevant aspects listed
			queries.remove("wt09-44");
			queries.remove("wt10-100");
			queries.remove("wt10-54");
			queries.remove("wt10-55");
			queries.remove("wt10-59");
			queries.remove("wt10-70");
			queries.remove("wt10-71");
			queries.remove("wt10-72");
			queries.remove("wt10-83");
			queries.remove("wt10-92");
			queries.remove("wt10-95");

			DivEvaluation_QUERIES_USED = new String[queries.size()];
			DivEvaluation_QUERIES_USED = queries.toArray(DivEvaluation_QUERIES_USED);
			System.out.println(queries.size() + " queries: " + queries);
		}
		*/
	}
	/**
	 * @return the queries used in the diversification task, without filtering
	 * **/
	public static List<String> getDivEvalQueries(DivVersion divVersion){
		List<String> divEvalQueries = new ArrayList<String>();
		//
		for (int i = 1; i <= 100; i++) {
			if(accept(i, divVersion)){
				divEvalQueries.add((i <= 50 ? "wt09-" : "wt10-") + i);
			}			
		}				
		//due to <-- no relevant aspects listed in the official file
		divEvalQueries.remove("wt10-95");
		divEvalQueries.remove("wt10-100");
		//
		System.out.println(divEvalQueries.size() + " queries: " + divEvalQueries);
		//
		return divEvalQueries;		
	}
	
	
	//
	private static Map<String, TRECDivQuery> loadTrecD0910Queries(List<String> query_files){
		TreeMap<String, TRECDivQuery> queries = new TreeMap<String, TRECDivQuery>();
		try{		
			for(String queryFile: query_files){
				SAXBuilder saxBuilder = new SAXBuilder();
				Document xmlDoc = saxBuilder.build(new File(queryFile));
				Element webtrackElement = xmlDoc.getRootElement();
				//topic list
				List topicList = webtrackElement.getChildren("topic");
				for(int i=0; i<topicList.size(); i++){				
					Element topicElement = (Element)topicList.get(i);
					String number = topicElement.getAttributeValue("number");			
					String title = topicElement.getChildText("query");
					String description = topicElement.getChildText("description");
					String type = topicElement.getAttributeValue("type");
					//
					TRECDivQuery trecD0910Query = new TRECDivQuery((Integer.parseInt(number)<=50? "wt09-":"wt10-")+number,
							title, description, type);
					//System.out.println(number+"\t"+type+"\t"+title+"\t"+description);	
					//subtopics
					List subtopicList = topicElement.getChildren("subtopic");
					for(int j=0; j<subtopicList.size(); j++){
						Element subtopicElement = (Element)subtopicList.get(j);
						String sNumber = subtopicElement.getAttributeValue("number");
						String sType = subtopicElement.getAttributeValue("type");
						String sDescription= subtopicElement.getText();
						//
						TRECSubtopic subtopic = new TRECSubtopic(sNumber, sDescription, sType);
						trecD0910Query.addSubtopic(subtopic);
						//System.out.println(sNumber+"\t"+sType+"\t"+sDescription);
					}
					//
					queries.put(trecD0910Query._number, trecD0910Query);						
				}				
			}			
		}catch(Exception e){
			e.printStackTrace();
		}
		//
		return queries;
	}
	//
	public static void main(String[] args) throws Exception {
		
		//Map<String,TRECDivQuery> trecD0910Queries = TRECDivLoader.loadTrecDivQueries(DivVersion.Div20092010);
		
		// No aspects available
		// aspects.put("wt10-95", new QueryAspects("wt10-95", file_root));
		// aspects.put("wt10-100", new QueryAspects("wt10-100", file_root));
		Map<String,TRECQueryAspects> trecD0910QueryAspects = TRECDivLoader.loadTrecDivQueryAspects(DivVersion.Div20092010);
		
		//Map<String,String> trecD0910Docs = TRECDivLoader.loadTrecDivDocs();		
		
	}
}
