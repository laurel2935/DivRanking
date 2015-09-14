/** Simple support class to read in a stopword list and check for 
 *  stopwords.
 *   
 * @author Scott Sanner (ssanner@gmail.com)
 */

package org.archive.nlp.stopword;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashSet;

public class StopWordChecker {

	public static HashSet<String> _ignoreWords = new HashSet<String>();
	public final static String IGNORE_WORDS_ABS = "dic/stopwords_en.txt";

	public StopWordChecker() {
		_ignoreWords = new HashSet<String>();
		loadStopWords(IGNORE_WORDS_ABS);
	}

	public StopWordChecker(String src) {
		_ignoreWords = new HashSet<String>();
		loadStopWords(src);
	}

	public static void loadStopWords(String src) {
        try {
            String line;
            BufferedReader br = new BufferedReader(new FileReader(src));
            while ((line = br.readLine()) != null) {
                _ignoreWords.add(line.trim().toLowerCase());
            }
            br.close();
        } catch (Exception e) {
            System.out.println("File not found");
            e.printStackTrace();
        }

	}
	
    private static boolean malformedWord(String s) {
	    //System.out.println(s);
	    if (!Character.isLetterOrDigit(s.charAt(0))) {
	        return true;
	    }
	    if ((Character.isDigit(s.charAt(0))) && (s.length() <= 3)) {
	        return true;
	    }
	    if (s.length() <= 1) {
	        return true;
	    }
	    return false;
    }

	
	public static boolean isStopWord(String s) {
		if(_ignoreWords.size() == 0){
			loadStopWords(IGNORE_WORDS_ABS);
		}
		return malformedWord(s) || _ignoreWords.contains(s);
	}
	
	public boolean isStopWordExcludingNumbers(String s) {
		return _ignoreWords.contains(s);
	}
	
	public static void main(String args[]) {
		StopWordChecker swc = new StopWordChecker();
		Test(swc, "is");
		Test(swc, "Scott");
		Test(swc, "had");
		Test(swc, "went");
		Test(swc, "crawled");
	}
	
	public static void Test(StopWordChecker swc, String s) {
		System.out.println("'" + s + "' is stopword: " + ((swc.isStopWord(s)) ? "yes" : "no"));
	}
}
