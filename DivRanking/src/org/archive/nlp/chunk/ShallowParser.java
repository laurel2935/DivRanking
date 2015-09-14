package org.archive.nlp.chunk;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.archive.dataset.ntcir.sm.SMTopic;
import org.archive.dataset.ntcir.sm.TaggedTerm;
import org.archive.dataset.ntcir.sm.TaggedTopic;
import org.archive.util.Language;
import org.archive.util.Language.Lang;

import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.ling.Word;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.stanford.nlp.process.Tokenizer;
import edu.stanford.nlp.process.TokenizerFactory;
import edu.stanford.nlp.process.WordTokenFactory;
import edu.stanford.nlp.trees.EnglishGrammaticalRelations;
import edu.stanford.nlp.trees.GrammaticalRelation;
import edu.stanford.nlp.trees.GrammaticalStructure;
import edu.stanford.nlp.trees.GrammaticalStructureFactory;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations.TreeAnnotation;
import edu.stanford.nlp.trees.TreebankLanguagePack;
import edu.stanford.nlp.trees.TypedDependency;

public class ShallowParser {
	//for debugging
	private static final boolean DEBUG = false;
	//setting
    private static final String[] en_options = { "-maxLength", "80", "-retainTmpSubcategories" };
    private static final String[] ch_options = { "-maxLength", "80"};
    
    private Lang lang = null;
    private String grammar = null;
    private LexicalizedParser lp = null;
    private TreebankLanguagePack tlp = null;
    //private GrammaticalStructureFactory gsf = null;
    
    
    public ShallowParser(Language.Lang lang){
    	if(Language.isEnglish(lang)){
    		this.grammar = "edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz";
    		this.lp = LexicalizedParser.loadModel(grammar, en_options);
    	}else if(Language.isChinese(lang)){
    		//this.grammar = "edu/stanford/nlp/models/lexparser/chinesePCFG.ser.gz";   
    		this.grammar = "edu/stanford/nlp/models/lexparser/chineseFactored.ser.gz";   
    		this.lp = LexicalizedParser.loadModel(grammar, ch_options);
    	}else{
    		new Exception().printStackTrace();
    	}
    	//
    	this.lang = lang;    	
    	this.tlp = lp.getOp().langpack();
    	//this.gsf = tlp.grammaticalStructureFactory();
    }
    //
    public boolean completeSentence(String inputText){    	
    	Tokenizer<? extends HasWord> toke = this.tlp.getTokenizerFactory().getTokenizer(new StringReader(inputText));
        List<? extends HasWord> sentence = toke.tokenize();   
        Tree parse = lp.parse(sentence);
    	GrammaticalStructureFactory gsf = tlp.grammaticalStructureFactory();
    	GrammaticalStructure gs = gsf.newGrammaticalStructure(parse);
    	Collection<TypedDependency> tdCollection = gs.typedDependenciesCollapsed();
    	
    	if(hasEnGramRelation(tdCollection, EnglishGrammaticalRelations.NOMINAL_SUBJECT) && hasEnGramRelation(tdCollection, EnglishGrammaticalRelations.COPULA)
    			|| hasEnGramRelation(tdCollection, EnglishGrammaticalRelations.DIRECT_OBJECT)&&hasEnGramRelation(tdCollection, EnglishGrammaticalRelations.ADVERBIAL_MODIFIER)){
    		return true;
    	}else{
    		return false;
    	}
    }
    public boolean completeSentence(Tree parse){ 
    	GrammaticalStructureFactory gsf = tlp.grammaticalStructureFactory();
    	GrammaticalStructure gs = gsf.newGrammaticalStructure(parse);
    	Collection<TypedDependency> tdCollection = gs.typedDependenciesCollapsed();
    	
    	if(hasEnGramRelation(tdCollection, EnglishGrammaticalRelations.NOMINAL_SUBJECT) && exactCOPEnGramRelation(tdCollection)
    			|| hasEnGramRelation(tdCollection, EnglishGrammaticalRelations.DIRECT_OBJECT) && hasEnGramRelation(tdCollection, EnglishGrammaticalRelations.ADVERBIAL_MODIFIER)){
    		return true;
    	}else{
    		return false;
    	}
    }
    private static boolean hasEnGramRelation(Collection<TypedDependency> tdCollection, GrammaticalRelation egr){
    	for(TypedDependency td : tdCollection) {
      	  if(td.reln().equals(egr)) {
      	    return true;
      	  }
      	}
    	return false;
    }
    private static boolean exactCOPEnGramRelation(Collection<TypedDependency> tdCollection){
    	for(TypedDependency td : tdCollection) {
      	  if(td.reln().equals(EnglishGrammaticalRelations.COPULA)) {
      		return td.dep().nodeString().equals("'s")? false:true;      		
      	  }
      	}
    	return false;
    }
    /////////////////////////
    //for chunk parsing
    /////////////////////////
    /**
     * the tokenized word and its pos tag
     * **/
    private ArrayList<TaggedWord> getTaggedWords(String text){
    	// Use the default tokenizer for this TreebankLanguagePack
        Tokenizer<? extends HasWord> toke = this.tlp.getTokenizerFactory().getTokenizer(new StringReader(text));
        List<? extends HasWord> sentence = toke.tokenize();
        Tree parse = lp.parse(sentence);
        //
        return parse.taggedYield();
    } 
    
    public ArrayList<TaggedTerm> getTaggedTerms(String text){
    	ArrayList<TaggedTerm> taggedTerms = new ArrayList<TaggedTerm>();
    	
    	ArrayList<TaggedWord> taggedWords = getTaggedWords(text);
    	for(TaggedWord word: taggedWords){
    		taggedTerms.add(new TaggedTerm(word.value(), word.tag()));
    	}
    	
    	if(DEBUG){
    		System.out.println();
    		for(TaggedTerm taggedTerm: taggedTerms){
    			System.out.print(taggedTerm.toString()+"\t");
    		}
    		System.out.println();
    	}
    	return taggedTerms;
    }
    //
    private String getTerm(Tree tree){
    	String term = "";
    	ArrayList<Word> words = tree.yieldWords();
    	if(Language.isEnglish(lang)){
    		term += words.get(0).word();
    		for(int i=1; i<words.size(); i++){
    			term = term + " " + words.get(i).word();
    		}
    		return term;
    	}else{    		
    		for(int i=0; i<words.size(); i++){
    			term += words.get(i).word();
    		}
    		return term;
    	}
    }
    //
    public void getTaggedTopic(SMTopic smTopic){
    	TaggedTopic taggedTopic = new TaggedTopic();
    	// Use the default tokenizer for this TreebankLanguagePack
        Tokenizer<? extends HasWord> toke = this.tlp.getTokenizerFactory().getTokenizer(new StringReader(smTopic.getTopicText()));
        List<? extends HasWord> sentence = toke.tokenize();
        Tree parse = lp.parse(sentence);        
        taggedTopic.setPennString(parse.pennString());
        
        smTopic.setSentenceState(completeSentence(parse));
        
        //term        
        ArrayList<TaggedWord> taggedWords = parse.taggedYield(); 
        ArrayList<TaggedTerm> taggedTerms = new ArrayList<TaggedTerm>();
    	for(TaggedWord word: taggedWords){
    		taggedTerms.add(new TaggedTerm(word.value(), word.tag()));
    	}
    	taggedTopic.setTaggedTerms(taggedTerms);
    	
    	//phrase    	
    	ArrayList<HashSet<Tree>>  pList = new ArrayList<HashSet<Tree>>();
    	
        int size = parse.getLeaves().size();
        HashSet<Tree> treeSet = null;
        for(int k=0; k<size; k++){
        	if(null != (treeSet=getTaggedPhrase(parse, k))){
        		if(!include(pList, treeSet)){
        			pList.add(treeSet);
        		}
        	}
        }
        
        ArrayList<ArrayList<TaggedTerm>> taggedPhraseList = new ArrayList<ArrayList<TaggedTerm>>();
        
        for(HashSet<Tree> tSet: pList){
        	ArrayList<TaggedTerm> taggedPhrase = new ArrayList<TaggedTerm>();
    		for(Tree tree: tSet){
    			taggedPhrase.add(new TaggedTerm(getTerm(tree), tree.value()));
    		}
    		taggedPhraseList.add(taggedPhrase);
        }
        if(taggedPhraseList.size() > 0){
        	taggedTopic.setTaggedPhraseList(taggedPhraseList);
        }else{
        	taggedTopic.setTaggedPhraseList(null);
        }
    	
        smTopic.setTaggedTopic(taggedTopic);        
    }
    //
    private boolean include(ArrayList<HashSet<Tree>>  tList, HashSet<Tree> element){
    	for(HashSet<Tree> t: tList){
    		if(t.containsAll(element) && element.containsAll(t)){
    			return true;
    		}
    	}
    	return false;    	
    }
    //
    private HashSet<Tree> getTaggedPhrase(Tree parse, int i){
    	List<Tree> leafSet = parse.getLeaves();
    	
    	Tree iLeaf = leafSet.get(i);
		Tree iParent = iLeaf.ancestor(1, parse);
		Tree iAncestor = iParent.ancestor(1, parse);
		
		if(null!=iAncestor && isSimple2LevelTree(iAncestor)){
			HashSet<Tree> treeSet = new HashSet<Tree>();
			
			for(int j=0; j<i; j++){
				Tree jLeaf = leafSet.get(j);
				Tree jParent = jLeaf.ancestor(1, parse);
				Tree jAncestor = jParent.ancestor(1, parse);
				if(null==jAncestor || jAncestor!=iAncestor){
					treeSet.add(jParent);
				}
			}
			
			for(int j=i+1; j<leafSet.size(); j++){
				Tree jLeaf = leafSet.get(j);
				Tree jParent = jLeaf.ancestor(1, parse);
				Tree jAncestor = jParent.ancestor(1, parse);
				if(null==jAncestor || jAncestor!=iAncestor){
					treeSet.add(jParent);
				}
			}
			
			return treeSet;
		}else{
			return null;
		}
    }
    
    //
    public  ArrayList<ArrayList<TaggedTerm>> getTaggedPhraseList(String text){    	
    	// Use the default tokenizer for this TreebankLanguagePack
        Tokenizer<? extends HasWord> toke = this.tlp.getTokenizerFactory().getTokenizer(new StringReader(text));
        List<? extends HasWord> sentence = toke.tokenize();
        Tree parse = lp.parse(sentence);  
    	
    	//phrase
    	
    	ArrayList<HashSet<Tree>>  pList = new ArrayList<HashSet<Tree>>();
    	
        int size = parse.getLeaves().size();
        HashSet<Tree> treeSet = null;
        for(int k=0; k<size; k++){
        	if(null != (treeSet=getTaggedPhrase(parse, k))){
        		if(!include(pList, treeSet)){
        			pList.add(treeSet);
        		}
        	}
        }
        
        ArrayList<ArrayList<TaggedTerm>> taggedPhraseList = new ArrayList<ArrayList<TaggedTerm>>();
        
        for(HashSet<Tree> tSet: pList){
        	ArrayList<TaggedTerm> taggedPhrase = new ArrayList<TaggedTerm>();
    		for(Tree tree: tSet){
    			taggedPhrase.add(new TaggedTerm(getTerm(tree), tree.value()));
    		}
    		taggedPhraseList.add(taggedPhrase);
        }
        if(taggedPhraseList.size() > 0){
        	return taggedPhraseList;
        }else{
        	return null;
        }    	
    }
    /**
     * It finds that: the leaves are a bare word, namely without pos tag
     * its parent is a merely a tagged word, also a tree!
     * e.g., for the word "a", its parent is just "DT a"
     * **/
    
    /**
     * the tagged word tree, whose child is merely a bare word
     * **/
    private boolean isTaggedWordTree(Tree tree){
    	if(tree.isLeaf()){
    		return false;
    	}
    	List<Tree> childSet = tree.getChildrenAsList();
    	for(Tree child: childSet){
    		if(!child.isLeaf()){
    			return false;
    		}
    	}
    	return true;
    }
    //whose children are all tagged word tree
    /** an example: a sentence to be parsed
     *  leaf:	a
		parent:	(DT a)
		ancestor:	(NP (DT a) (NN sentence))
		leaf:	sentence
		parent:	(NN sentence)
		ancestor:	(NP (DT a) (NN sentence))
		leaf:	to
		parent:	(TO to)
		ancestor:	(VP (TO to) (VP (VB be) (VP (VBN parsed))))
		leaf:	be
		parent:	(VB be)
		ancestor:	(VP (VB be) (VP (VBN parsed)))
		leaf:	parsed
		parent:	(VBN parsed)
		ancestor:	(VP (VBN parsed))  // special case!!!!!!!!!
     * **/
    private boolean isSimple2LevelTree(Tree tree){
    	if(tree.isLeaf() || isTaggedWordTree(tree)){
    		return false;
    	}
    	List<Tree> childSet = tree.getChildrenAsList();
    	if(childSet.size() < 2){
    		return false;
    	}
    	for(Tree child: childSet){
    		if(!isTaggedWordTree(child)){
    			return false;
    		}
    	}
    	return true;
    }    
    /**
     * 
     * **/
    private ArrayList<Tree> getSimple2LevelTrees(String text){
    	// Use the default tokenizer for this TreebankLanguagePack
        Tokenizer<? extends HasWord> toke = this.tlp.getTokenizerFactory().getTokenizer(new StringReader(text));
        List<? extends HasWord> sentence = toke.tokenize();
        Tree rootTree = lp.parse(sentence);
        //
        boolean appear = false;
        ArrayList<Tree> treeSet = new ArrayList<Tree>();
    	//
    	List<Tree> leafSet = rootTree.getLeaves();
    	for(Tree leaf: leafSet){    		
    		//System.out.println("leaf:\t"+leaf.toString());
    		//tagged word tree
    		Tree parent = leaf.ancestor(1, rootTree);
    		//System.out.println("parent:\t"+parent.toString());
    		//
    		Tree ancestor = parent.ancestor(1, rootTree);
    		//System.out.println("ancestor:\t"+ancestor.toString());    		
    		//
    		if(null==ancestor || !isSimple2LevelTree(ancestor)){
    			treeSet.add(parent);
    		}else if(!treeSet.contains(ancestor)){
    			treeSet.add(ancestor);
    			if(!appear){
    				appear = true;
    			}    			
    		}   		
    	}
    	//    	
    	/*
    	for(Tree tree: treeSet){
    		System.out.println(tree.yieldWords());
    		System.out.println(tree.value());
    		System.out.println(tree.taggedYield());
    		//System.out.println(tree.toString());
    	}
    	*/
    	if(appear){
    		//System.out.println(appear);
    		return treeSet;
    	}else{
    		return null;
    	}    	
    }
    
    //////////////////////////////
    //for segmentation
    //////////////////////////////
    //
    public ArrayList<String> segment(String text){
    	ArrayList<String> wordList = new ArrayList<String>();
    	//
    	Tokenizer<? extends HasWord> toke = this.tlp.getTokenizerFactory().getTokenizer(new StringReader(text));
        List<? extends HasWord> sentence = toke.tokenize();
        //
        Iterator<? extends HasWord> itr = sentence.iterator();
        while(itr.hasNext()){
        	HasWord word = itr.next();
        	wordList.add(word.word());
        }
        //
        if(DEBUG){
        	System.out.println(wordList);
        }
        //
        return wordList.size()>0? wordList:null;      
    }
    
    //////////// Test ///////////////
    
    public void test(String testString){    	
    	// Use the default tokenizer for this TreebankLanguagePack
        Tokenizer<? extends HasWord> toke = this.tlp.getTokenizerFactory().getTokenizer(new StringReader(testString));
        List<? extends HasWord> sentence = toke.tokenize();        
        //
        System.out.println("before parsing...");
        System.out.println(sentence.toString());
        //        
        Tree parse = lp.parse(sentence);
        
        parse.dependencies();
        
        parse.pennPrint();        
        System.out.println();
        
		//        GrammaticalStructure gs = gsf.newGrammaticalStructure(parse);
		//        List<TypedDependency> tdl = gs.typedDependenciesCCprocessed();
		//        
		//        System.out.println(tdl);
		//        System.out.println();
		//
		//        System.out.println("The words of the sentence:");
		//        for (Label lab : parse.yield()) {
		//          if (lab instanceof CoreLabel) {
		//            System.out.println(((CoreLabel) lab).toString("{map}"));
		//          } else {
		//            System.out.println(lab);
		//          }
		//        }
        
        System.out.println();
        System.out.println(parse.taggedYield());
        ArrayList<Word> wordList = parse.yieldWords();
        for(Word word: wordList){
        	System.out.print(word.word()+"|");
        }
        System.out.println();        
        ArrayList<TaggedWord> taggedWords = parse.taggedYield();
        for(TaggedWord word: taggedWords){
        	System.out.println(word.tag() + "\t" + word.value());
        }
        System.out.println();
    }
    
    public void dependencyTest(){
    	//String text = "My dog also likes eating sausage";
    	String text = "men's shoe sizes conversion";
    	Tokenizer<? extends HasWord> toke = this.tlp.getTokenizerFactory().getTokenizer(new StringReader(text));
        List<? extends HasWord> sentence = toke.tokenize();        
        //
        System.out.println("before parsing...");
        System.out.println(sentence.toString());
        //        
        Tree parse = lp.parse(sentence);
    	GrammaticalStructureFactory gsf = tlp.grammaticalStructureFactory();
    	GrammaticalStructure gs = gsf.newGrammaticalStructure(parse);
    	Collection<TypedDependency> tdSet = gs.typedDependenciesCollapsed();
    	
    	for(TypedDependency td : tdSet) {
    		System.out.println(td);
      	}
    	System.out.println("-------------");
    	for(TypedDependency td : tdSet) {
    	  if(td.reln().equals(EnglishGrammaticalRelations.COPULA)) {
    		  System.out.println(td.dep().nodeString());
    	    System.out.println(td);
    	  }
    	}
    }
	
	
	//
	public static void main(String []args){
		//1
		//ShallowParser shallowParser = new ShallowParser(Language.Lang.Chinese);
		//shallowParser.test("���׽�Ŀ����Щ");
		//ShallowParser shallowParser = new ShallowParser(Language.Lang.English);
		//shallowParser.test("business insurance commerical general liability");
		//shallowParser.segment("business insurance commerical general liability");
		
		
		//2
		//System.out.println();
		//shallowParser.getSimple2LevelTrees("a sentence to be parsed", "NP");		
		
		//3
		//ShallowParser shallowParser = new ShallowParser(Language.Lang.English);
		//shallowParser.getTaggedTopic("business insurance liability");
		
		//4
		ShallowParser shallowParser = new ShallowParser(Language.Lang.English);
		shallowParser.dependencyTest();
		
	}
}
