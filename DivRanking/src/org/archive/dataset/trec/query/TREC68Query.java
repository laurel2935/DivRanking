/** Query representation for TREC Interactive and CLUEWEB Diversity tracks
 *   
 * @author Scott Sanner (ssanner@gmail.com)
 */

package org.archive.dataset.trec.query;


public class TREC68Query extends TRECQuery implements Comparable<TRECQuery> {
	//Usage cases
	private static final boolean INCLUDE_QUERY_TITLE       = true;
	//
	private static final boolean INCLUDE_QUERY_DESCRIPTION = false;
	//
	private static final boolean INCLUDE_QUERY_INSTANCE    = false;
	
	//
	public String _instance;	
	
	// Constructors
	public TREC68Query(String number, String title, String description, String instance) {
		super(number, title, description);		
		_instance = instance;
	}
	
	public String toString() {
		return _number + " -> [ " + _title + ", " + _description + ", " + _instance + " ]"; 
	}
	
	public String getQueryContent() {
		StringBuilder sb = new StringBuilder();
		//
		if (INCLUDE_QUERY_TITLE) {
			sb.append(_title);
		} 
		if (INCLUDE_QUERY_DESCRIPTION) {
			sb.append((sb.length() > 0 ? " " : "") + _description);			
		}
		if (INCLUDE_QUERY_INSTANCE) {
			sb.append((sb.length() > 0 ? " " : "") + _instance);
		}
		return sb.toString();
	}

	//@Override
	public int compareTo(TRECQuery o) {
		// TODO Auto-generated method stub
		return toString().compareTo(o.toString());
	}	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}
}
