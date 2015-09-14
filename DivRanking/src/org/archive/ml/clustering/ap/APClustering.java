package org.archive.ml.clustering.ap;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.archive.dataset.DataSetDiretory;
import org.archive.dataset.trec.TRECDivLoader;
import org.archive.dataset.trec.TRECDivLoader.DivVersion;
import org.archive.dataset.trec.query.TRECDivQuery;
import org.archive.dataset.trec.query.TRECQueryAspects;
import org.archive.ml.clustering.ap.abs.AffinityPropagationAlgorithm.AffinityGraphMode;
import org.archive.ml.clustering.ap.abs.ClusterInteger;
import org.archive.ml.clustering.ap.abs.ClusterString;
import org.archive.ml.clustering.ap.abs.AffinityPropagationAlgorithm.AffinityConnectingMethod;
import org.archive.ml.clustering.ap.affinitymain.InteractionData;
import org.archive.ml.clustering.ap.matrix.MatrixPropagationAlgorithm;
import org.archive.nicta.kernel.TFIDF_A1;
import org.archive.util.format.StandardFormat;
import org.archive.util.io.IOText;


public class APClustering {
	
	private static final boolean debug = false;
	
	//Matrix oriented
	private MatrixPropagationAlgorithm ap = new MatrixPropagationAlgorithm();
    //lambda for damping
    private double lambda;
    //maximum number of iterations to be performed
    private int iterations;
    //given preference
    public double preferences;
    /*
     * algorithm will stop after the n-th{i.e., convits} iteration without any change in centers (exemplars).
     * In this situation, we say that the algorithm converged.
     * If the maximum number of interactions is reached before the stop criterion is satisfied,
     * then the algorithm did not converge. The current clusters can still be retrieved in this situation.
     * */
    private Integer convits;
    //set of node identifier, i.e., names
    private Collection<String> nodeNames = new HashSet<String>();
    private String kind;
    private boolean takeLog;
    private boolean refine;
    //?
    //private Integer steps = null;
    private AffinityConnectingMethod connMode;
    private AffinityGraphMode graphMode;
    
    
	ArrayList<InteractionData> dataPointInteractions;
    
    public APClustering(double lambda, int iterations, Integer convits, double preferences, ArrayList<InteractionData> similarityMatrix){
    	this.lambda = lambda;
        this.iterations = iterations;
        this.preferences = preferences;
        //current case
        this.kind = "centers";
        this.convits = convits;
        this.dataPointInteractions = similarityMatrix;
        //because of max-sum, but negative value
        this.takeLog = false;
        this.refine = false;
        //this.steps = steps;
        //in the context of original AP
        this.connMode = AffinityConnectingMethod.ORIGINAL;
        //current case
        this.graphMode = AffinityGraphMode.UNDIRECTED;
        //
    }
    
    public void setParemeters() {
    	this.ap.setLambda(this.lambda);
    	this.ap.setIterations(this.iterations);
    	this.ap.setConvits(this.convits);
    	//this.ap.setSteps(this.steps);
    	this.ap.setRefine(this.refine);
    	this.ap.setConnectingMode(this.connMode);   
    	this.ap.setGraphMode(this.graphMode);
        //af.addIterationListener(new ConsoleIterationListener(iterations));       
        
        for(InteractionData intData : this.dataPointInteractions){
        	this.nodeNames.add(intData.getFrom());
        	this.nodeNames.add(intData.getTo());
        }
        
        this.ap.setN(this.nodeNames.size());

        this.ap.init();
        
        for (InteractionData intData : this.dataPointInteractions) {
            //System.out.println(intData.getFrom() + " " + intData.getTo() + " " + intData.getSim());
            Double val;
            if (this.takeLog) {
                if (intData.getSim() > 0) {
                    val = Math.log(intData.getSim());
                } else {
                    val = Double.valueOf(0);
                }
            } else {
                val = intData.getSim();
            }            
            this.ap.setSimilarity(intData.getFrom(), intData.getTo(), val);
            //af.setSimilarityInt(target, source, val);
            //af.setSimilarityInt(Integer.valueOf(intData.getFrom()), Integer.valueOf(intData.getTo()), val);
        }
        Double pref;
        if (this.takeLog) {
            if (this.preferences > 0) {
                pref = Math.log(this.preferences);
            } else {
                pref = Double.valueOf(0);
            }
        } else {
            pref = this.preferences;
        }
        System.out.println("pref: " + pref);
        this.ap.setConstPreferences(pref);
    }
    
    public Object run() {
    	 if(debug){
         	System.out.println(this.ap.S.toString());
         	//
         	System.out.println("Size:\t"+this.ap.getN());
         	System.out.println("Preference:\t"+this.preferences);
         }
    	//
    	if (this.kind.equals("centers")) {
            Map<Integer, ClusterInteger> clusters = this.ap.doClusterAssocInt();
            //Map<Integer, Cluster<Integer>> clusters = af.doClusterAssocInt(); 
            if(debug){
            	ArrayList<Integer> rList = new ArrayList<Integer>();
            	rList.addAll(clusters.keySet());
            	Collections.sort(rList);
            	System.out.println(rList);
            }
            //return convertClusters(clusters);
            return clusters;
        } else {
            Map<Integer, Integer> clusters = this.ap.doClusterInt(); 
            if(true){
            	System.out.println("AP results: ");
            	for(Entry<Integer, Integer> entry: clusters.entrySet()){
            		System.out.println(entry.getKey()+"  ->  "+entry.getValue());
            	}
            }
            return clusters;
        }
    }
    
    public static double getSimilarity(ArrayList<Double> aList, ArrayList<Double> bList){
    	double eucDistance = Math.sqrt(Math.pow(aList.get(0)-bList.get(0), 2)
    			+ Math.pow(aList.get(1)-bList.get(1), 2));
    	//
    	return -eucDistance;
    }
    
    private Map<String, ClusterString> convertClusters(Map<Integer, ClusterInteger> clusters){
    	Map<String, ClusterString> newClusters = new HashMap<String, ClusterString>();
    	
    	Iterator<Integer> itr = clusters.keySet().iterator();
    	while(itr.hasNext()){
    		Integer key = itr.next();
    		String exemplarName = this.ap.getNodeName(key);
    		ClusterString cString = new ClusterString(exemplarName);
    		ClusterInteger cInteger = clusters.get(key);     		
    		for(Integer mem: cInteger.getElements()){
    			cString.add(this.ap.getNodeName(mem));
    		}
    		newClusters.put(exemplarName, cString);
    	}
    	
    	return newClusters;    	
    }
    
    public static double getMedian(ArrayList<Double> vList){
    	Collections.sort(vList);
    	if(vList.size() % 2 == 0){
    		return (vList.get(vList.size()/2 - 1)+vList.get(vList.size()/2))/2.0;
    	}else{    		
    		return vList.get(vList.size()/2);
    	}
    }
    
    public static void test(){
    	//4 data points
    	ArrayList<ArrayList<Double>> datapointList = new ArrayList<ArrayList<Double>>();
    	ArrayList<Double> dataPoint_1 = new ArrayList<Double>();
    	dataPoint_1.add(-2.3);
    	dataPoint_1.add(3.7);
    	datapointList.add(dataPoint_1);
    	
    	ArrayList<Double> dataPoint_2 = new ArrayList<Double>();
    	dataPoint_2.add(-1.5);
    	dataPoint_2.add(1.8);
    	datapointList.add(dataPoint_2);
    	
    	ArrayList<Double> dataPoint_3 = new ArrayList<Double>();
    	dataPoint_3.add(2.5);
    	dataPoint_3.add(1.8);
    	datapointList.add(dataPoint_3);
    	
    	ArrayList<Double> dataPoint_4 = new ArrayList<Double>();
    	dataPoint_4.add(4.0);
    	dataPoint_4.add(1.6);
    	datapointList.add(dataPoint_4);
    	//
    	ArrayList<Double> vList = new ArrayList<Double>();
    	ArrayList<InteractionData> dataPointInteractions = new ArrayList<InteractionData>();
    	for(int i=0; i<datapointList.size()-1; i++){
    		for(int j=i+1; j<datapointList.size(); j++){
    			double v = getSimilarity(datapointList.get(i), datapointList.get(j));
    			InteractionData interData = new InteractionData(StandardFormat.serialFormat(i, "00"), 
    					StandardFormat.serialFormat(j, "00"), 
    					v);
    			dataPointInteractions.add(interData);
    			vList.add(v);
    		}
    	}    	
    	//
    	double lambda = 0.5;
    	int iterations = 50000000;
    	int convits = 20;
    	String kind = "";
    	boolean logDomain = false;
    	boolean refine = false;
    	AffinityConnectingMethod connMode = AffinityConnectingMethod.ORIGINAL;
    	AffinityGraphMode graphMode = AffinityGraphMode.UNDIRECTED;
    	//double preferences = getMedian(vList);
    	double preferences = -4.0;
    	APClustering apClustering = new APClustering(lambda, iterations, convits, preferences, dataPointInteractions);
    	//
    	apClustering.setParemeters();
    	apClustering.run();
    }
    //
    public static ArrayList<InteractionData> loadAPExample(){
    	ArrayList<InteractionData> interList = new ArrayList<InteractionData>();
    	//load
    	try {
			String file = DataSetDiretory.ROOT+"APExample/ToyProblemSimilarities.txt";
			BufferedReader reader = IOText.getBufferedReader_UTF8(file);
			String line;
			String [] array;
			
			ArrayList<Double> vList = new ArrayList<Double>();
			while(null != (line=reader.readLine())){
				//System.out.println(line);
				array = line.split("  ");
				//System.out.println(array[0]+"\t"+array[1]+"\t"+array[2]);				
				Double v = Double.parseDouble(array[2]);
				interList.add(new InteractionData(array[0], array[1], v));
				vList.add(v);
			}
			reader.close();
			if(debug){
				System.out.println("inters:\t"+interList.size());
				//System.out.println("Median:\t"+getMedian(vList));
				//System.out.println(interList.get(0).toString());
			}
			
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
    	return interList;
    }
    //
    public static void testAPExample_Topic(){ 
    	String qNumber = "wt09-1";
    	
    	//Map<String,TRECDivQuery> trecDivQueries = TRECDivLoader.loadTrecDivQueries(DivVersion.Div2009);	
    	HashMap<String,String> trecDivDocs = TRECDivLoader.loadTrecDivDocs();
    	Map<String,TRECQueryAspects> trecDivQueryAspects = TRECDivLoader.loadTrecDivQueryAspects(DivVersion.Div2009);
    	    	
    	TRECQueryAspects trecQueryAspects = trecDivQueryAspects.get(qNumber);
    	Set<String> _docs_topn = trecQueryAspects.getTopNDocs();
    	
    	TFIDF_A1 tfidf_A1Kernel = new TFIDF_A1(trecDivDocs, false);
    	tfidf_A1Kernel.initTonNDocs(_docs_topn); 
    	ArrayList<InteractionData> releMatrix = new ArrayList<InteractionData>();			
    	
		String [] topNDocNames = _docs_topn.toArray(new String[0]);
		ArrayList<Double> vList = new ArrayList<Double>();
		for(int i=0; i<topNDocNames.length-1; i++){
			String iDocName = topNDocNames[i]; 
			Object iDocRepr = tfidf_A1Kernel.getObjectRepresentation(iDocName);
			for(int j=i+1; j<topNDocNames.length; j++){
				String jDocName = topNDocNames[j];
				Object jDocRepr = tfidf_A1Kernel.getObjectRepresentation(jDocName);
				//
				double v = tfidf_A1Kernel.sim(iDocRepr, jDocRepr);
				releMatrix.add(new InteractionData(iDocName, jDocName, v));
				//
				vList.add(v);
			}
		}    	
    	//run
    	double lambda = 0.5;
    	int iterations = 5000;
    	int convits = 10;
    	double preferences = getMedian(vList);    	
    	APClustering apClustering = new APClustering(lambda, iterations, convits, preferences, releMatrix);
    	//
    	apClustering.setParemeters();
    	apClustering.run();
    }
    //
    public static void testAPExample(){
    	
    }
    //
    public static void main(String []args){
    	//1
    	//APClustering.test();
    	
    	//2
    	APClustering.testAPExample();
    	
    	//3
    	//[48, 64, 33, 82, 98, 3, 83, 36, 22, 73, 26, 78]
    	//[3, 22, 26, 33, 36, 48, 64, 73, 78, 82, 83, 98]
    	APClustering.testAPExample_Topic();
    	
    	
    }
}
