/**
 * boilerpipe
 *
 * Copyright (c) 2009 Christian Kohlschtter
 *
 * The author licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.archive.nlp.htmlparser.boilerpipe;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.URL;

import org.archive.nlp.htmlparser.hit.HITHtmlExtractor;
import org.xml.sax.InputSource;

import de.l3s.boilerpipe.extractors.ArticleExtractor;

/**
 * Demonstrates how to use Boilerpipe to get the main content as plain text.
 * Note: In real-world cases, you'd probably want to download the file first using a fault-tolerant crawler.
 * 
 * @author Christian Kohlschtter
 * @see HTMLHighlightDemo if you need HTML as well.
 */
public class Oneliner {
    public static void main(final String[] args) throws Exception {
        final URL url = new URL(
//                "http://www.l3s.de/web/page11g.do?sp=page11g&link=ln104g&stu1g.LanguageISOCtxParam=en"
        		"http://www.dn.se/nyheter/vetenskap/annu-godare-choklad-med-hjalp-av-dna-teknik"
        		);

        // This can also be done in one line:
        
        //String htm = "E:/Data_Log/DataSource_Raw/NTCIR-10/DocumentRanking/doc/top-100/0201/a2a6db21a7a5d8ba-1368c369d8c38c60.htm";
		 String htm_2 = "E:/Data_Log/DataSource_Raw/NTCIR-10/DocumentRanking/doc/top-100/0004/1f86a7e6678bf8a1-071d026ec8f3fbc0.htm";
		 BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(new File(htm_2))));
		  StringBuffer s = new StringBuffer();
		  String l = "";
		  while((l = br.readLine()) != null){
		   s.append(l);
		  }
		  br.close();
		 //System.out.println(ArticleExtractor.INSTANCE.getText(htm_2));
		 //InputSource inputSource = new InputSource(new FileInputStream(new File(htm_2)));
		 //inputSource.setEncoding("GB2312");
		 System.out.println(ArticleExtractor.INSTANCE.getText(s.toString()));

        // Also try other extractors!
//        System.out.println(DefaultExtractor.INSTANCE.getText(url));
//       System.out.println(CommonExtractors.CANOLA_EXTRACTOR.getText(url));
    }
}
