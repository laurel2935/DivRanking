package org.archive.dataset.ntcir.sm;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;
import java.util.Map.Entry;

import org.archive.OutputDirectory;
import org.archive.nlp.chunk.lpt.addon.LTPPosTag;
import org.archive.nlp.chunk.lpt.addon.LTPRelationTag;
import org.archive.nlp.chunk.lpt.addon.TNode;
import org.archive.nlp.chunk.lpt.addon.TTree;
import org.archive.nlp.chunk.lpt.ltpService.LTML;
import org.archive.nlp.chunk.lpt.ltpService.Word;
import org.archive.nlp.tokenizer.Tokenizer;
import org.archive.ntcir.sm.SMRunParameter;
import org.archive.util.Language.Lang;
import org.archive.util.io.IOText;

//import sun.launcher.resources.launcher;

public class LTPIRAnnotator {
	private final static boolean DEBUG = false;
	
	public static HashMap<String, LTML> ltmlMap = new HashMap<String, LTML>();
	
	
	private static void loadLtmlMap(){
		String inputFile = OutputDirectory.ROOT+"ntcir-11/SM/SubtopicString/UniqueSubtopicStrings.txt";
		String subTDir = OutputDirectory.ROOT+"ntcir-11/SM/SubtopicString/ParsedWithLTP/";
		try {
			ArrayList<String> lineList = IOText.getLinesAsAList_UTF8(inputFile);
			for(String line: lineList){
				String [] array = line.split("\t");
				String topicID = array[0];
				String id = array[1];
				
				String targetString = array[2];					
				String targetFile = subTDir+topicID+"-"+id+".xml";
				File xmlFile = new File(targetFile);
				if(!xmlFile.exists()){
					System.err.println("No Xml File!");
					continue;
				}
				LTML ltml = SMRunParameter.loadLTML(targetFile);
				
				ltmlMap.put(targetString, ltml);
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}	
		
		if(DEBUG){
			for(Entry<String, LTML> entry: ltmlMap.entrySet()){
				System.out.println(entry.getKey());
				
				LTML ltml = entry.getValue();
				if(null != ltml){
					System.out.println("sentence:\t"+ltml.getSentenceContent(0));
					
					ArrayList<Word> wList = ltml.getWords(0);
					for(Word w: wList){
						System.out.print(w.getWS()+"-"+w.getPOS()+"  ");
					}
					System.out.println();
				}
				
			}
		}
	}
	
	public ArrayList<IRAnnotation> irAnnotate(TTree tTree){
		ArrayList<IRAnnotation> results = new ArrayList<IRAnnotation>();
		//
		ArrayList<String> segmentations = parse(tTree);
		if(null != segmentations){
			for(String seg: segmentations){
				results.add(getIRAnnotation(seg, tTree));
			}
			return results;
		}else{
			return null;
		}		
	}
	
	//
	public IRAnnotation getIRAnnotation(String positionStr, TTree tTree){
		String [] array = positionStr.split(" ");
		Vector<TNode> nodeList = tTree.getNodes();
		if(array.length == 1){			
			int koID = Integer.parseInt(positionStr);
			TNode koNode = nodeList.get(koID); 
			KernelObject ko = new KernelObject(koNode.getContent(), koNode.getPosTag());
			//
			ArrayList<Modifier> moSet = new ArrayList<Modifier>();
			for(TNode moNode: nodeList){
				if(moNode.getID() != koID){
					moSet.add(new Modifier(moNode.getContent(), moNode.getPosTag()));
				}
			}
			//
			return new IRAnnotation(ko, moSet);
		}else{
			ArrayList<Integer> componentList = new ArrayList<Integer>();
			for(int i=0; i<array.length; i++){
				componentList.add(Integer.parseInt(array[i]));
			}
			//
			String koStr = "";
			for(Integer c: componentList){
				koStr += nodeList.get(c).getContent();
			}
			//
			KernelObject ko = new KernelObject(koStr, "unk");
			//
			ArrayList<Modifier> moSet = new ArrayList<Modifier>();
			for(TNode moNode: nodeList){
				if(!componentList.contains(moNode.getID())){
					moSet.add(new Modifier(moNode.getContent(), moNode.getPosTag()));
				}
			}
			//
			return new IRAnnotation(ko, moSet);			
		}
	}
	
	//
	public ArrayList<String> parse(TTree tTree){
		TNode root = tTree.getRoot();
		//one child
		if(!root.isLeaf() && 1==root.getChildren().size()){
			TNode child = root.getChildren().get(0);
			//n-ATT-
			if(root.getPosTag().equals(LTPPosTag.n) && child.getRelateTag().equals(LTPRelationTag.ATT)){				
				if(child.isLeaf()){	
					//n-ATT-Nh
					if(child.getPosTag().equals(LTPPosTag.nH)){
						ArrayList<String> koList = new ArrayList<String>();
						koList.add(Integer.toString(child.getID()));
						return koList;
					}else if(child.getPosTag().equals(LTPPosTag.n)){
						ArrayList<String> koList = new ArrayList<String>();
						//i.e., n,n,np
						koList.add("0");koList.add("1");
						koList.add("0 1");
						return koList;
					}else{
						System.out.println("Unexcepted n-ATT-");
						return null;
					}					
				}else{
					//n-ATT-n(tree)
					if(child.getPosTag().equals(LTPPosTag.n)){
						return getNp(child);
					}else{
						System.out.println("Unexcepted n-ATT-(tree)");
						return null;
					}					
				}	
			//v-SBV
			}else if(root.getPosTag().equals(LTPPosTag.v) && child.getRelateTag().equals(LTPRelationTag.SBV)){				
				if(child.isLeaf()){
					//v-SBV-n
					if(child.getPosTag().startsWith(LTPPosTag.n)){
						ArrayList<String> koList = new ArrayList<String>();
						koList.add(Integer.toString(child.getID()));
						return koList;
					}else{
						System.out.println("Unexcepted v-SBV-");
						return null;
					}
				}else{
					//v-SBV-n(tree)
					if(child.getPosTag().startsWith(LTPPosTag.n)){
						return getNp(child);
					}else{
						System.out.println("Unexcepted v-SBV-(tree)");
						return null;
					}					
				}
			//r-ATT-
			}else if(root.getPosTag().equals(LTPPosTag.r) && child.getRelateTag().equals(LTPRelationTag.ATT)){
				if(child.isLeaf()){
					ArrayList<String> koList = new ArrayList<String>();
					koList.add(Integer.toString(child.getID()));
					return koList;
				}else {
					return getNp(child);
				}
			//v-VOB
			}else if(root.getPosTag().equals(LTPPosTag.v) && child.getRelateTag().equals(LTPRelationTag.VOB)){
				if(child.isLeaf()){
					ArrayList<String> koList = new ArrayList<String>();
					//n and vp
					koList.add("1");
					koList.add("0 1");
					return koList;
				}else{
					return getNp(child);
				}
			}else{
				System.out.println("Unexcepted one child case!");
				return null;
			}
		}else if(!root.isLeaf() && 2==root.getChildren().size()){			
			TNode child_1 = root.getChildren().get(0);
			TNode child_2 = root.getChildren().get(1);
			//SBV-v-VOB-r
			if(root.getPosTag().equals(LTPPosTag.v) 
					&& child_1.getRelateTag().equals(LTPRelationTag.SBV) && child_2.getRelateTag().equals(LTPRelationTag.VOB) 
					&& child_2.getPosTag().equals(LTPPosTag.r)){
				//
				if(child_1.isLeaf()){
					if(child_1.getPosTag().startsWith(LTPPosTag.n)){
						ArrayList<String> koList = new ArrayList<String>();
						koList.add(Integer.toString(child_1.getID()));
						return koList;
					}else{
						System.out.println("Unexpected ?-SBV-v-VOB-r");
						return null;
					}
				}else{
					System.out.println("Unexpected ?(tree)-SBV-v-VOB-r");
					return null;
				}
			//r-SBV-v-VOB
			}else if(root.getPosTag().equals(LTPPosTag.v) 
					&& child_1.getRelateTag().equals(LTPRelationTag.SBV) && child_2.getRelateTag().equals(LTPRelationTag.VOB) 
					&& child_1.getPosTag().equals(LTPPosTag.r)){
				//
				if(child_2.isLeaf()){
					if(child_2.getPosTag().startsWith(LTPPosTag.n)){
						ArrayList<String> koList = new ArrayList<String>();
						koList.add(Integer.toString(child_2.getID()));
						return koList;
					}else{
						System.out.println("Unexpected r-SBV-v-VOB-?");
						return null;
					}
				}else{
					System.out.println("Unexpected r-SBV-v-VOB-?(tree)");
					return null;
				}
			//v-n-n
			}else if(root.getPosTag().equals(LTPPosTag.v)){
				if(leaves(root.getChildren())){
					return getVp(root);
				}else {
					System.out.println("Unexcepted v - leaves!");
					return null;
				}
			//n-n-n
			}else if(root.getPosTag().equals(LTPPosTag.n)){
				if(leaves(root.getChildren())){
					return getNp(root);
				}else {
					System.out.println("Unexcepted n - leaves!");
					return null;
				}
			}else{
				System.out.println("Unexpected two child case!");
				return null;
			}
			//ATT & ATT
		}else{
			System.out.println("Unexcepted complex case!");
			return null;
		}		
	}
	//
	public static boolean completeSentence(TTree tTree){
		TNode root = tTree.getRoot();
		//one child
		if(!root.isLeaf() && root.getChildren().size()>1){
			Vector<TNode> children = root.getChildren();
			if(hasLTPRelationTag(children, LTPRelationTag.VOB) && hasLTPRelationTag(children, LTPRelationTag.SBV)){
				return true;
			}else{
				return false;
			}			
		}else{
			return false;
		}
		
	}
	private static boolean hasLTPRelationTag(Vector<TNode> nodeVector, String relationTag){
		for(TNode node: nodeVector){
			if(node.getRelateTag().equals(relationTag)){
				return true;
			}
		}
		return false;
	}
	//
	private static boolean leaves(Vector<TNode> children){
		for(TNode tNode: children){
			if(!tNode.isLeaf()){
				return false;
			}
		}
		return true;
	}
	//
	private ArrayList<String> getNp(TNode tree){
		//person root
		if(tree.getPosTag().equals(LTPPosTag.nH)){
			ArrayList<String> koList = new ArrayList<String>();
			koList.add(Integer.toString(tree.getID()));
			return koList;
		//single character root
		}else if(tree.getContent().length() == 1){
			ArrayList<String> koList = new ArrayList<String>();
			koList.add(Integer.toString(tree.getID())+" "+Integer.toString(tree.getChildren().get(0).getID()));
			return koList;
		}else if(tree.getPosTag().equals(LTPPosTag.n)){
			ArrayList<String> koList = new ArrayList<String>();
			koList.add(Integer.toString(tree.getID()));
			koList.add(Integer.toString(tree.getID())+" "+Integer.toString(tree.getChildren().get(0).getID()));
			return koList;
		}else{
			System.out.println("Unexpected np case!");
		}
		return null;		
	}
	//
	private ArrayList<String> getVp(TNode tree){
		//single character root
		if(tree.getContent().length() == 1){
			ArrayList<String> koList = new ArrayList<String>();
			koList.add(Integer.toString(tree.getID())+" "+Integer.toString(tree.getChildren().get(0).getID()));
			return koList;
		}else if(tree.getPosTag().equals(LTPPosTag.v)){
			ArrayList<String> koList = new ArrayList<String>();			
			koList.add(Integer.toString(tree.getID())+" "+Integer.toString(tree.getChildren().get(0).getID()));
			return koList;
		}else{
			System.out.println("Unexpected np case!");
		}
		return null;
	}
	//
	
	private static boolean include(ArrayList<TaggedTerm> taggedTerms, String reference){
		for(TaggedTerm term: taggedTerms){
			if(term.termStr.equals(reference)){
				return true;
			}
		}
		return false;
	}
	
	public static ArrayList<ArrayList<TaggedTerm>> getTaggedPhraseList(ArrayList<TaggedTerm> taggedTerms){
		ArrayList<ArrayList<TaggedTerm>> taggedPhraseList = new ArrayList<ArrayList<TaggedTerm>>();
		
		ArrayList<TaggedTerm> termList = null;		
		for(int i=1; i<taggedTerms.size(); i++){
			if(null != (termList=getTaggedPhrase(taggedTerms, i))){
				taggedPhraseList.add(termList);
			}
		}
		
		if(taggedPhraseList.size() > 0){
			return taggedPhraseList;
		}else{
			return null;
		}		
	}
	
	private static ArrayList<TaggedTerm> getTaggedPhrase(ArrayList<TaggedTerm> taggedTerms, int i){
		TaggedTerm formerT = taggedTerms.get(i-1);
		TaggedTerm curT = taggedTerms.get(i);
		if(formerT.posTag.startsWith("n") && curT.posTag.startsWith("n")){
			ArrayList<TaggedTerm> termList = new ArrayList<TaggedTerm>();
			for(int j=0; j<i-1; j++){
				termList.add(taggedTerms.get(j));
			}
			
			termList.add(new TaggedTerm(formerT.termStr+curT.termStr, "NP"));
			
			for(int j=i+1; j<taggedTerms.size(); j++){
				termList.add(taggedTerms.get(j));
			}
			return termList;
		}else{
			return null;
		}
	}
	
	public static void getTaggedTopic(SMTopic smTopic){
		String tDir = OutputDirectory.ROOT+"ntcir-11/SM/ParsedTopic/PerFile/";
				
		String topicXMLFile = tDir+smTopic.getID()+".xml";
		LTML topicLTML = SMRunParameter.loadLTML(topicXMLFile);
		
		smTopic.setSentenceState(completeSentence(new TTree(topicLTML.getWords(0))));
		
		ArrayList<TaggedTerm> taggedTerms = new ArrayList<TaggedTerm>();
		
		if(smTopic.getID().equals("0004")){
			for(Word word: topicLTML.getWords(0)){
	    		taggedTerms.add(new TaggedTerm(word.getWS().toLowerCase(), word.getPOS()));
	    	}
			TaggedTopic taggedTopic = new TaggedTopic();
			taggedTopic.setTaggedTerms(taggedTerms);
			
			taggedTopic.setTaggedPhraseList(getTaggedPhraseList(taggedTerms));
			
			smTopic.setTaggedTopic(taggedTopic);
			
			return ;
		}else{
			for(Word word: topicLTML.getWords(0)){
	    		taggedTerms.add(new TaggedTerm(word.getWS(), word.getPOS()));
	    	}
		}
		
		//odd case
		if(Tokenizer.isDirectWord(smTopic.getTopicText(), Lang.Chinese) && !include(taggedTerms, smTopic.getTopicText())){
			if(DEBUG){
				System.out.println(include(taggedTerms, smTopic.getTopicText().trim()));
				System.out.println(">>"+taggedTerms);
				System.out.println(">>"+smTopic.getTopicText());
			}
			smTopic.DirectTermAndNoIRAnnotation = true;
			
			ArrayList<TaggedTerm> unkTaggedTerms = new ArrayList<TaggedTerm>();
			unkTaggedTerms.add(new TaggedTerm(smTopic.getTopicText(), "n"));
			
			TaggedTopic taggedTopic = new TaggedTopic();
			taggedTopic.setTaggedTerms(unkTaggedTerms);
			
			smTopic.setTaggedTopic(taggedTopic);
			
		}else{
			TaggedTopic taggedTopic = new TaggedTopic();
			taggedTopic.setTaggedTerms(taggedTerms);
			
			taggedTopic.setTaggedPhraseList(getTaggedPhraseList(taggedTerms));
			
			smTopic.setTaggedTopic(taggedTopic);
		}		
	}
	
	public static ArrayList<TaggedTerm> getTaggedTerm(SMTopic smTopic, String rq){
		//String subTDir = OutputDirectory.ROOT+"ntcir-11/SM/SubtopicString/ParsedWithLTP/";
		if(ltmlMap.size() == 0){
			loadLtmlMap();
		}
		
		if(smTopic.DirectTermAndNoIRAnnotation){
			if(DEBUG){
				System.out.println("DirectTermAndNoIRAnnotation for "+smTopic.toString());				
			}
			ArrayList<String> subTWords = Tokenizer.adaptiveQuerySegment(Lang.Chinese, rq, smTopic.getTopicText(), true, true);
			if(null == subTWords){
				return null;
			}
			ArrayList<TaggedTerm> taggedTerms = new ArrayList<TaggedTerm>();
			for(String w: subTWords){
				if(w.equals(smTopic.getTopicText())){
					taggedTerms.add(new TaggedTerm(w, "n"));
				}else{
					taggedTerms.add(new TaggedTerm(w, "unk"));
				}				
			}			
			return taggedTerms;
		}else{
			/*
			String subTXMLFile = subTDir+smTopic.getID()+"-"+Integer.toString(subtopicStringID+1)+".xml";
			File xmlFile = new File(subTXMLFile);
			if(!xmlFile.exists()){
				return null;
			}
			LTML subTLTML = SMRunParameter.loadLTML(subTXMLFile);
			*/
			LTML ltml = ltmlMap.get(rq);
			if(null == ltml){
				return null;
			}
			ArrayList<TaggedTerm> taggedTerms = new ArrayList<TaggedTerm>();
			for(Word word: ltml.getWords(0)){
	    		taggedTerms.add(new TaggedTerm(word.getWS(), word.getPOS()));
	    	}
			return taggedTerms;
		}		
	}
	
	private static PrintStream printer = null; 
	public static void openPrinter(){
		try{
			printer = new PrintStream(new FileOutputStream(new File(OutputDirectory.ROOT+"log.txt")));
			System.setOut(printer);			
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public static void closePrinter(){
		printer.flush();
		printer.close();
	}
	
	public static void main(String []args){
		//1
		/*
		String runTitle = "testTitle";
		String runIntroduction = "testIntroduction";
		SMRunParameter runParameter = new SMRunParameter(NTCIR_EVAL_TASK.NTCIR11_SM_CH, runTitle, runIntroduction,
				SimilarityFunction.GregorEditDistance, ClusteringFunction.StandardAP);
		try {
			HashMap<String, LTML> kHashMap = runParameter.loadLTMLForChTopics(runParameter.topicList);
			Set<String> keySet = kHashMap.keySet();
			
			LTPIRAnnotator ltpirAnnotator = new LTPIRAnnotator();
			
			for(String key: keySet){
				LTML ltml = kHashMap.get(key);
				if(null != ltml){
					ArrayList<Word> wList = ltml.getWords(0);
					for(Word w: wList){
						System.out.print(w.getWS()+"-"+w.getPOS()+"  ");
					}
					System.out.println();
					ArrayList<IRAnnotation> aList = ltpirAnnotator.irAnnotate(new TTree(wList));
					if(null != aList){
						for(IRAnnotation a: aList){
							System.out.println(a.toString());
						}
					}
					
				}
			}
			
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}	
		*/
		
		//2
		LTPIRAnnotator.openPrinter();
		LTPIRAnnotator.loadLtmlMap();
		LTPIRAnnotator.closePrinter();
	}
}
