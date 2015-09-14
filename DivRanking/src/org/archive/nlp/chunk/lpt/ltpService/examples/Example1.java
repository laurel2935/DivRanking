package org.archive.nlp.chunk.lpt.ltpService.examples;

import java.util.ArrayList;

import org.archive.nlp.chunk.lpt.ltpService.LTML;
import org.archive.nlp.chunk.lpt.ltpService.LTPOption;
import org.archive.nlp.chunk.lpt.ltpService.LTPService;
import org.archive.nlp.chunk.lpt.ltpService.SRL;
import org.archive.nlp.chunk.lpt.ltpService.Word;

public class Example1 {
    public static void main(String[] args) {

        LTPService ls = new LTPService("yu-haitao@iss.tokushima-u.ac.jp:IF7ynN42"); 
        try {
            ls.setEncoding(LTPOption.UTF8);
            LTML ltml = ls.analyze(LTPOption.PARSER,"我爱北京天安门。");

            int sentNum = ltml.countSentence();
            for(int i = 0; i< sentNum; ++i){
                ArrayList<Word> wordList = ltml.getWords(i);
                System.out.println(ltml.getSentenceContent(i));
                for(int j = 0; j < wordList.size(); ++j){
                    System.out.print("\t" + wordList.get(j).getWS());
                    System.out.print("\t" + wordList.get(j).getPOS());
                    System.out.print("\t" + wordList.get(j).getNE());
                    System.out.print("\t" + wordList.get(j).getParserParent() + 
                            "\t" + wordList.get(j).getParserRelation());
                    if(ltml.hasSRL() && wordList.get(j).isPredicate()){
                        ArrayList<SRL> srls = wordList.get(j).getSRLs();
                        System.out.println();
                        for(int k = 0; k <srls.size(); ++k){
                            System.out.println("\t\t" + srls.get(k).type + 
                                    "\t" + srls.get(k).beg +
                                    "\t" + srls.get(k).end);
                        }
                    }
                    System.out.println();
                }
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            ls.close();
        }
    }
}
