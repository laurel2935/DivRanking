package org.archive.ireval;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

import org.archive.util.tuple.Pair;
import org.archive.util.tuple.PairComparatorBySecond_Desc;

public class Cal {
	private static DecimalFormat resultFormat = new DecimalFormat("#.####");
	
	private static double ALPHA = 0.5;
	private static double GAMA = 0.5;
	
	//synthetic example based comparison
	
	public void com(){
		
		//size of ranked list
		int cutoff = 3;
		//size of subtopic set
		int m = 2;
		int stI = 1, stJ=2;
		
		//subtopic probability vector
		double [] equalStPro = new double [2];
		equalStPro [0] = 0.5;
		equalStPro [1] = 0.5;
		double [] biasedStPro = new double [2];
		biasedStPro [0] = 0.7;
		biasedStPro [1] = 0.3;
				
		//as for g: row: subtopic, i.e., i & j	column: document, i.e., d_1, d_2, d_3
		//case-1		
		int [][] c1_g = new int [2][3];
		c1_g [0][0] = 1;
		c1_g [0][1] = 1;
		c1_g [0][2] = 1;
		
		//case-2
		int [][] c2_g = new int [2][3];
		c2_g[0][0] = 1;
		c2_g[0][1] = 1;
		c2_g[0][2] = 1;
		
		//case-3
		int [][] c3_g = new int [2][3];
		c3_g[0][0] = 1;
		c3_g[0][1] = 1;
		c3_g[1][2] = 1;
		
		//--
		System.out.println("brute force comparison---------------");
		ArrayList<Integer> rankedList_1 = new ArrayList<Integer>();
		rankedList_1.add(1);		rankedList_1.add(2);		rankedList_1.add(3);
		
		ArrayList<Integer> rankedList_2 = new ArrayList<Integer>();
		rankedList_2.add(1);		rankedList_2.add(3);		rankedList_2.add(2);
		
		ArrayList<Integer> rankedList_3 = new ArrayList<Integer>();
		rankedList_3.add(2);		rankedList_3.add(1);		rankedList_3.add(3);
		
		ArrayList<Integer> rankedList_4 = new ArrayList<Integer>();
		rankedList_4.add(2);		rankedList_4.add(3);		rankedList_4.add(1);
		
		ArrayList<Integer> rankedList_5 = new ArrayList<Integer>();
		rankedList_5.add(3);		rankedList_5.add(1);		rankedList_5.add(2);
		
		ArrayList<Integer> rankedList_6 = new ArrayList<Integer>();
		rankedList_6.add(3);		rankedList_6.add(2);		rankedList_6.add(1);
		
		System.out.println("^^^");
		System.out.println(get_Ideal_Alpha_DCG(rankedList_1, m, c1_g));
		System.out.println(get_Ideal_Alpha_DCG(rankedList_1, m, c2_g));
		System.out.println(get_Ideal_Alpha_DCG(rankedList_1, m, c3_g));
		System.out.println("===");
		System.out.println(get_Ideal_Alpha_DCG(rankedList_2, m, c1_g));
		System.out.println(get_Ideal_Alpha_DCG(rankedList_2, m, c2_g));
		System.out.println(get_Ideal_Alpha_DCG(rankedList_2, m, c3_g));
		System.out.println("===");
		System.out.println(get_Ideal_Alpha_DCG(rankedList_3, m, c1_g));
		System.out.println(get_Ideal_Alpha_DCG(rankedList_3, m, c2_g));
		System.out.println(get_Ideal_Alpha_DCG(rankedList_3, m, c3_g));
		System.out.println("===");
		System.out.println(get_Ideal_Alpha_DCG(rankedList_4, m, c1_g));
		System.out.println(get_Ideal_Alpha_DCG(rankedList_4, m, c2_g));
		System.out.println(get_Ideal_Alpha_DCG(rankedList_4, m, c3_g));
		System.out.println("===");
		System.out.println(get_Ideal_Alpha_DCG(rankedList_5, m, c1_g));
		System.out.println(get_Ideal_Alpha_DCG(rankedList_5, m, c2_g));
		System.out.println(get_Ideal_Alpha_DCG(rankedList_5, m, c3_g));
		System.out.println("===");
		System.out.println(get_Ideal_Alpha_DCG(rankedList_6, m, c1_g));
		System.out.println(get_Ideal_Alpha_DCG(rankedList_6, m, c2_g));
		System.out.println(get_Ideal_Alpha_DCG(rankedList_6, m, c3_g));
		//--		
		System.out.println("^^^");
		System.out.println("---------------");
		greedy_alphaNDCG(m, cutoff, c1_g);
		greedy_alphaNDCG(m, cutoff, c2_g);
		greedy_alphaNDCG(m, cutoff, c3_g);
		System.out.println("End of finding the ideal---------------");
		System.out.println();
		//--
		
		//comparison
		
		//S-recall
		cal_SRecall(m, cutoff,stI, stJ, c1_g, c2_g, c3_g);
		//AP-IA
		cal_APIA(m, cutoff, stI,stJ, c1_g, c2_g, c3_g, equalStPro, biasedStPro);
		//ERR-IA
		cal_ERRIA(m, cutoff, stI, stJ, c1_g, c2_g, c3_g, equalStPro, biasedStPro);
		//alpha-nDCG
		cal_AlphaNDCG(m, cutoff, stI, stJ, c1_g, c2_g, c3_g);
		//D#-nDCG
		cal_DSharpNDCG(m, cutoff,stI, stJ, c1_g, c2_g, c3_g, equalStPro, biasedStPro);
		//DIN#-nDCG
		cal_DINSharpNDCG(m, cutoff, stI, stJ, c1_g, c2_g, c3_g, equalStPro, biasedStPro);
	}
	

	/**
	 * mu: 	marginal utility function
	 * U:	subtopic-specific utility function of a ranked list
	 * T:	total utility of a ranked list across all subtopics
	 * **/
	
	////R, I, V:	mapping functions from graded relevance assessments to relevance probabilities or numerical values
	//R:editorial relevance probability
	public double R(int grade, int mode){
		return ((Math.pow(2, grade)-1)/Math.pow(2, mode));		
	}
	//I: binary mapping
	public double I(int grade){
		return grade>0?1:0;
	}
	//V: 
	public double V(int grade){
		return grade;
	}
	
	////S-recall
	public HashSet<Integer> mu_SRecall(int stI, int docK, int [][] gradeMatrix){
		if(0 < gradeMatrix[stI-1][docK-1]){
			HashSet<Integer> stSet = new HashSet<Integer>();
			stSet.add(1);
			return stSet;
		}else{
			return null;
		}
	}
	public double U_SRecall(int stI, int cutoff, int [][] gradeMatrix){
		HashSet<Integer> utilitySet = new HashSet<Integer>();
		for(int k=1; k<=cutoff; k++){
			HashSet<Integer> muSet = mu_SRecall(stI, k, gradeMatrix);
			if(null != muSet){
				utilitySet.addAll(muSet);
			}
		}
		
		return utilitySet.size();		
	}
	public double T_SRecall(int m, int cutoff, int [][] gradeMatrix){
		int stCount = 0;
		for(int i=1; i<=m; i++){
			stCount += U_SRecall(i,cutoff, gradeMatrix);
		}
		
		return (1.0*stCount)/m;		
	}
	
	////AP-IA
	public double mu_APIA(int stI, int docK, int [][] gradeMatrix){
		int c_ik = 0;
		for(int k=1; k<=docK; k++){
			c_ik += I(gradeMatrix[stI-1][k-1]);
		}
		
		return (1.0*I(gradeMatrix[stI-1][docK-1])*c_ik);		
	}
	public double U_APIA(int stI, int cutoff, int [][] gradeMatrix){
		double stUtility = 0.0;
		for(int k=1; k<=cutoff; k++){
			stUtility += (1.0/k)*mu_APIA(stI, k, gradeMatrix);
		}
		return stUtility;
	}
	public double T_APIA(int m, int cutoff, int [][] gradeMatrix, double [] stPro){
		double totalUtility = 0.0;
		for(int i=1; i<=m; i++){
			totalUtility += stPro[i-1]*U_APIA(i, cutoff, gradeMatrix);
		}
		return totalUtility/cutoff;
	}
	
	////ERR-IA
	public double mu_ERRIA(int stI, int docK, int [][] gradeMatrix){
		if(1 == docK){
			return R(gradeMatrix[stI-1][docK-1], 2);
		}else{
			double mu = 1;
			for(int k=1; k<=(docK-1); k++){
				mu *= (1-R(gradeMatrix[stI-1][k-1], 2));
			}
			
			return R(gradeMatrix[stI-1][docK-1], 2)*mu;
		}
	}
	public double U_ERRIA(int stI, int cutoff, int [][] gradeMatrix){
		double stUtility = 0.0;
		for(int k=1; k<=cutoff; k++){
			stUtility += (1.0/k)*mu_ERRIA(stI, k, gradeMatrix);
		}
		return stUtility;
	}
	public double T_ERRIA(int m, int cutoff, int [][] gradeMatrix, double [] stPro){
		double totalUtility = 0.0;
		for(int i=1; i<=m; i++){
			totalUtility += stPro[i-1]*U_ERRIA(i, cutoff, gradeMatrix);
		}
		
		return totalUtility;
	}
	
	////alpha-nDCG
	public double mu_alphaNDCG(int stI, int docK, int [][] gradeMatrix){
		if(1 == docK){
			return 1.0*I(gradeMatrix[stI-1][docK-1]);
		}else{
			int c_ik1 = 0;
			for(int k=1; k<=(docK-1); k++){
				c_ik1 += I(gradeMatrix[stI-1][k-1]);
			}
			
			return I(gradeMatrix[stI-1][docK-1])*Math.pow((1-ALPHA), c_ik1);
		}		
	}
	public double U_alphaNDCG(int stI, int cutoff, int [][] gradeMatrix){
		double stUtility = 0.0;
		for(int k=1; k<=cutoff; k++){
			stUtility += (1.0/(Math.log10(k+1)/Math.log10(2)))*mu_alphaNDCG(stI, k, gradeMatrix);
		}
		return stUtility;
	}
	
	public double T_alphaNDCG(int m, int cutoff, int [][] gradeMatrix, ArrayList<Integer> idealList){
		double totalUtility = 0.0;
		for(int i=1; i<=m; i++){
			totalUtility += U_alphaNDCG(i, cutoff, gradeMatrix);
		}
		//System.out.println("totalUtility:\t"+totalUtility);
		
		return totalUtility/get_Ideal_Alpha_DCG(idealList, m, gradeMatrix);
	}
	
	public void greedy_alphaNDCG(int m, int n, int [][] gradeMatrix){
		HashSet<Integer> selectedSet = new HashSet<Integer>();
		ArrayList<Integer> rankedL = new ArrayList<Integer>();
		
		while(rankedL.size() < n){
			
			ArrayList<Pair<Integer, Double>> candidateList = new ArrayList<Pair<Integer,Double>>();
			
			for(int k=1; k<=n; k++){				
				if(!selectedSet.contains(k)){
					double candidateGain = calGain_alphaNDCG(k, m, rankedL, gradeMatrix);					
					candidateList.add(new Pair<Integer, Double>(k, candidateGain));
				}
			}
			
			Collections.sort(candidateList, new PairComparatorBySecond_Desc<Integer, Double>());
			
			rankedL.add(candidateList.get(0).first);
			selectedSet.add(candidateList.get(0).first);			
		}
		
		//
		System.out.println("Greedy Search Order:\t"+rankedL);
		System.out.println(get_Ideal_Alpha_DCG(rankedL, m, gradeMatrix));
	}
	
	private double calGain_alphaNDCG(int candidateK, int m, ArrayList<Integer> rankedL, int [][] gradeMatrix){
		double gain = 0.0;
		for(int i=1; i<=m; i++){
			gain += I(gradeMatrix[i-1][candidateK-1])*calDiscountFactor(i, rankedL, gradeMatrix);
		}
		return gain;
	}
	
	private double calDiscountFactor(int stI, ArrayList<Integer> rankedL, int [][] gradeMatrix){
		int releCount = 0;
		for(Integer k: rankedL){
			releCount += I(gradeMatrix[stI-1][k-1]);
		}		
		return Math.pow((1-ALPHA), releCount);
	}
		
	//directory compute alphaNDCG given the ranked list
	private double get_Ideal_Alpha_DCG(ArrayList<Integer> rankedList, int m, int [][] gradeMatrix){
		double acc = 0.0;
		for(int k=1; k<=rankedList.size(); k++){
			//position in gradematrix
			int docK = rankedList.get(k-1);
			
			acc += (1.0/(Math.log10(k+1)/Math.log10(2)))*getGain_alphaNDCG(docK, k, m, rankedList, gradeMatrix);
		}
		return acc;
	}
	
	private double getGain_alphaNDCG(int docK, int position, int m, ArrayList<Integer> rankedList, int [][] gradeMatrix){
		double gain = 0.0;
		for(int i=1; i<=m; i++){
			gain += I(gradeMatrix[i-1][docK-1])*getDiscountFactor_alphaNDCG(i, rankedList, position-1, gradeMatrix);
		}
		return gain;
	}
	
	private double getDiscountFactor_alphaNDCG(int stI, ArrayList<Integer> rankedList, int position_KMinus1, int [][] gradeMatrix){
		int releCount = 0;
		for(int j=0; j<position_KMinus1; j++){
			int docK = rankedList.get(j);
			
			releCount += I(gradeMatrix[stI-1][docK-1]);
		}		
		return Math.pow((1-ALPHA), releCount);
	}
	

	
	
	////D-nDCG
	public double mu_DnDCG(int stI, int docK, int [][] gradeMatrix){
		return V(gradeMatrix[stI-1][docK-1]);
	}
	public double U_DnDCG(int stI, int cutoff, int [][] gradeMatrix){
		double stUtility = 0.0;
		for(int k=1; k<=cutoff; k++){
			stUtility += (1.0/(Math.log10(k+1)/Math.log10(2)))*mu_DnDCG(stI, k, gradeMatrix);
		}
		return stUtility;
	}
	
	public double T_DnDCG(int m, int cutoff, double [] stPro, int [][] gradeMatrix){
		double unNormalizedT = 0.0;
		
		for(int i=1; i<=m; i++){
			unNormalizedT += stPro[i-1]*U_DnDCG(i, cutoff, gradeMatrix);
		}
		
		return unNormalizedT/getIdeal_D_DCG(m, cutoff, stPro, gradeMatrix);
	}
	
	public double getIdeal_D_DCG(int m, int cutoff, double [] stPro, int [][] gradeMatrix){
		ArrayList<Integer> idealList = searchIdeal_DDCG(m, cutoff, stPro, gradeMatrix);
		return cal_D_DCG(idealList, m, stPro, gradeMatrix);
	}
	
	public double cal_D_DCG(ArrayList<Integer> rankedList, int m, double [] stPro, int [][] gradeMatrix){
		double acc = 0.0;
		
		for(int k=1; k<=rankedList.size(); k++){
			Integer docK = rankedList.get(k-1);
			acc += (1.0/(Math.log10(k+1)/Math.log10(2)))*calGlobalGain(docK, m, stPro, gradeMatrix);
			
		}
		
		return acc;
	}
	
	private ArrayList<Integer> searchIdeal_DDCG(int m, int cutoff, double [] stPro, int [][] gradeMatrix){
		ArrayList<Pair<Integer, Double>> pairList = new ArrayList<Pair<Integer,Double>>();
		
		for(int k=1; k<=cutoff; k++){
			pairList.add(new Pair<Integer, Double>(k, calGlobalGain(k, m, stPro, gradeMatrix)));
		}
		
		Collections.sort(pairList, new PairComparatorBySecond_Desc<Integer, Double>());
		
		ArrayList<Integer> ideaList = new ArrayList<Integer>();
		for(Pair<Integer, Double> p: pairList){
			ideaList.add(p.getFirst());
		}
		
		return ideaList;
	}
	//global gain value
	private double calGlobalGain(int docK, int m, double [] stPro, int [][] gradeMatrix){
		double gg = 0.0;
		for(int i=1; i<=m; i++){
			gg += stPro[i-1]*V(gradeMatrix[i-1][docK-1]);
		}
		return gg;
	}
	
	//
	public double DSharp_nDCG(int m, int cutoff, double [] stPro, int [][] gradeMatrix){
		return GAMA*T_SRecall(m, cutoff, gradeMatrix)+(1-GAMA)*T_DnDCG(m, cutoff,stPro, gradeMatrix);
	}
	
	////DIN-nDCG
	public double mu_DINnDCG(int stI, int docK, int [][] gradeMatrix, HashSet<Integer> infStSet){
		if(infStSet.contains(stI) || 1==docK){
			return V(gradeMatrix[stI-1][docK-1]);
		}else{
			int c_ik1 = 0;
			for(int k=1; k<=docK-1; k++){
				c_ik1 += I(gradeMatrix[stI-1][k-1]);
			}
			
			if(c_ik1 > 0){
				return 0.0;
			}else{
				return V(gradeMatrix[stI-1][docK-1]);
			}
		}
	}
	public double U_DINnDCG(int stI, int cutoff, int [][] gradeMatrix, HashSet<Integer> infStSet){
		double stUtility = 0.0;
		for(int k=1; k<=cutoff; k++){
			stUtility += (1.0/(Math.log10(k+1)/Math.log10(2)))*mu_DINnDCG(stI, k, gradeMatrix,infStSet);
		}
		return stUtility;
	}
	
	public double T_DINnDCG(int m, int cutoff, double [] stPro, int [][] gradeMatrix, HashSet<Integer> infStSet){
		
		double unNormalizedT = 0.0;
		for(int i=1; i<=m; i++){
			unNormalizedT += stPro[i-1]*U_DINnDCG(i, cutoff, gradeMatrix, infStSet);
		}
		
		return unNormalizedT/getIdeal_D_DCG(m, cutoff, stPro, gradeMatrix);
	}
	
	public double DINSharp_nDCG(int m, int cutoff, double [] stPro, int [][] gradeMatrix, HashSet<Integer> infStSet){
		return GAMA*T_SRecall(m, cutoff, gradeMatrix)+(1-GAMA)*T_DINnDCG(m, cutoff,stPro, gradeMatrix, infStSet);
	}
	
	//S-recall
	public void cal_SRecall(int m, int cutoff, int stI, int stJ, int [][] c1_g, int [][] c2_g, int [][] c3_g){
		////S-recall
		//case-1
		System.out.println("S-recall w.r.t. Case-1");
		//i
		for(int k=1; k<=cutoff; k++){
			System.out.print(mu_SRecall(stI, k, c1_g)+"\t");
		}		
		System.out.print(U_SRecall(stI,cutoff,c1_g)+"\t");
		//j
		for(int k=1; k<=cutoff; k++){
			System.out.print(mu_SRecall(stJ, k, c1_g)+"\t");
		}
		System.out.print(U_SRecall(stJ,cutoff,c1_g)+"\t");
		//total utility
		System.out.println(T_SRecall(m, cutoff, c1_g)+"\t"+T_SRecall(m, cutoff, c1_g));
		
		//case-2
		System.out.println("S-recall w.r.t. Case-2");
		//i
		for(int k=1; k<=cutoff; k++){
			System.out.print(mu_SRecall(stI, k, c2_g)+"\t");
		}		
		System.out.print(U_SRecall(stI,cutoff,c2_g)+"\t");
		//j
		for(int k=1; k<=cutoff; k++){
			System.out.print(mu_SRecall(stJ, k, c2_g)+"\t");
		}
		System.out.print(U_SRecall(stJ,cutoff,c2_g)+"\t");
		//total utility
		System.out.println(T_SRecall(m, cutoff, c2_g)+"\t"+T_SRecall(m, cutoff, c2_g));
		
		//case-3
		System.out.println("S-recall w.r.t. Case-3");
		//i
		for(int k=1; k<=cutoff; k++){
			System.out.print(mu_SRecall(stI, k, c3_g)+"\t");
		}		
		System.out.print(U_SRecall(stI,cutoff,c3_g)+"\t");
		//j
		for(int k=1; k<=cutoff; k++){
			System.out.print(mu_SRecall(stJ, k, c3_g)+"\t");
		}
		System.out.print(U_SRecall(stJ,cutoff,c3_g)+"\t");
		//total utility
		System.out.println(T_SRecall(m, cutoff, c3_g)+"\t"+T_SRecall(m, cutoff, c3_g));
	}
	//AP-IA
	public void cal_APIA(int m, int cutoff, int stI, int stJ, int [][] c1_g, int [][] c2_g, int [][] c3_g, double [] equalStPro, double [] biasedStPro){
		////AP-IA
		System.out.println();
		//case-1
		System.out.println("AP-IA w.r.t. Case-1");
		//i
		for(int k=1; k<=cutoff; k++){
			System.out.print(resultFormat.format(mu_APIA(stI, k, c1_g))+"\t");
		}		
		System.out.print(resultFormat.format(U_APIA(stI,cutoff,c1_g))+"\t");
		//j
		for(int k=1; k<=cutoff; k++){
			System.out.print(resultFormat.format(mu_APIA(stJ, k, c1_g))+"\t");
		}
		System.out.print(resultFormat.format(U_APIA(stJ,cutoff,c1_g))+"\t");
		//total utility
		System.out.println(resultFormat.format(T_APIA(m, cutoff, c1_g, equalStPro))
				+"\t"+resultFormat.format(T_APIA(m, cutoff, c1_g, biasedStPro)));
		
		//case-2
		System.out.println("AP-IA w.r.t. Case-2");
		//i
		for(int k=1; k<=cutoff; k++){
			System.out.print(resultFormat.format(mu_APIA(stI, k, c2_g))+"\t");
		}		
		System.out.print(resultFormat.format(U_APIA(stI,cutoff,c2_g))+"\t");
		//j
		for(int k=1; k<=cutoff; k++){
			System.out.print(resultFormat.format(mu_APIA(stJ, k, c2_g))+"\t");
		}
		System.out.print(resultFormat.format(U_APIA(stJ,cutoff,c2_g))+"\t");
		//total utility
		System.out.println(resultFormat.format(T_APIA(m, cutoff, c2_g, equalStPro))
				+"\t"+resultFormat.format(T_APIA(m, cutoff, c2_g, biasedStPro)));
		
		//case-3
		System.out.println("AP-IA w.r.t. Case-3");
		//i
		for(int k=1; k<=cutoff; k++){
			System.out.print(resultFormat.format(mu_APIA(stI, k, c3_g))+"\t");
		}		
		System.out.print(resultFormat.format(U_APIA(stI,cutoff,c3_g))+"\t");
		//j
		for(int k=1; k<=cutoff; k++){
			System.out.print(resultFormat.format(mu_APIA(stJ, k, c3_g))+"\t");
		}
		System.out.print(resultFormat.format(U_APIA(stJ,cutoff,c3_g))+"\t");
		//total utility
		System.out.println(resultFormat.format(T_APIA(m, cutoff, c3_g, equalStPro))
				+"\t"+resultFormat.format(T_APIA(m, cutoff, c3_g, biasedStPro)));
	}
	//ERR-IA
	public void cal_ERRIA(int m, int cutoff, int stI, int stJ, int [][] c1_g, int [][] c2_g, int [][] c3_g, double [] equalStPro, double [] biasedStPro){
		////ERR-IA
		System.out.println();
		//case-1
		System.out.println("ERR-IA w.r.t. Case-1");
		//i
		for(int k=1; k<=cutoff; k++){
			System.out.print(resultFormat.format(mu_ERRIA(stI, k, c1_g))+"\t");
		}		
		System.out.print(resultFormat.format(U_ERRIA(stI,cutoff,c1_g))+"\t");
		//j
		for(int k=1; k<=cutoff; k++){
			System.out.print(resultFormat.format(mu_ERRIA(stJ, k, c1_g))+"\t");
		}
		System.out.print(resultFormat.format(U_ERRIA(stJ,cutoff,c1_g))+"\t");
		//total utility
		System.out.println(resultFormat.format(T_ERRIA(m, cutoff, c1_g, equalStPro))
				+"\t"+resultFormat.format(T_ERRIA(m, cutoff, c1_g, biasedStPro)));
		
		//case-2
		System.out.println("ERR-IA w.r.t. Case-2");
		//i
		for(int k=1; k<=cutoff; k++){
			System.out.print(resultFormat.format(mu_ERRIA(stI, k, c2_g))+"\t");
		}		
		System.out.print(resultFormat.format(U_ERRIA(stI,cutoff,c2_g))+"\t");
		//j
		for(int k=1; k<=cutoff; k++){
			System.out.print(resultFormat.format(mu_ERRIA(stJ, k, c2_g))+"\t");
		}
		System.out.print(resultFormat.format(U_ERRIA(stJ,cutoff,c2_g))+"\t");
		//total utility
		System.out.println(resultFormat.format(T_ERRIA(m, cutoff, c2_g, equalStPro))
				+"\t"+resultFormat.format(T_ERRIA(m, cutoff, c2_g, biasedStPro)));
		
		//case-3
		System.out.println("ERR-IA w.r.t. Case-3");
		//i
		for(int k=1; k<=cutoff; k++){
			System.out.print(resultFormat.format(mu_ERRIA(stI, k, c3_g))+"\t");
		}		
		System.out.print(resultFormat.format(U_ERRIA(stI,cutoff,c3_g))+"\t");
		//j
		for(int k=1; k<=cutoff; k++){
			System.out.print(resultFormat.format(mu_ERRIA(stJ, k, c3_g))+"\t");
		}
		System.out.print(resultFormat.format(U_ERRIA(stJ,cutoff,c3_g))+"\t");
		//total utility
		System.out.println(resultFormat.format(T_ERRIA(m, cutoff, c3_g, equalStPro))
				+"\t"+resultFormat.format(T_ERRIA(m, cutoff, c3_g, biasedStPro)));
		System.out.println();
	
	}
	//alpha-nDCG
	public void cal_AlphaNDCG(int m, int cutoff, int stI, int stJ, int [][] c1_g, int [][] c2_g, int [][] c3_g){
	////alpha-nDCG
		ArrayList<Integer> c1_idealList = new ArrayList<Integer>();
		c1_idealList.add(1); c1_idealList.add(2); c1_idealList.add(3);
		
		ArrayList<Integer> c2_idealList = new ArrayList<Integer>();
		c2_idealList.add(1); c2_idealList.add(2); c2_idealList.add(3);
		
		ArrayList<Integer> c3_idealList = new ArrayList<Integer>();
		c3_idealList.add(1); c3_idealList.add(3); c3_idealList.add(2);

		//case-1
		System.out.println("alpha-nDCG w.r.t. Case-1");
		//i
		for(int k=1; k<=cutoff; k++){
			System.out.print(resultFormat.format(mu_alphaNDCG(stI, k, c1_g))+"\t");
		}		
		System.out.print(resultFormat.format(U_alphaNDCG(stI,cutoff,c1_g))+"\t");
		//j
		for(int k=1; k<=cutoff; k++){
			System.out.print(resultFormat.format(mu_alphaNDCG(stJ, k, c1_g))+"\t");
		}
		System.out.print(resultFormat.format(U_alphaNDCG(stJ,cutoff,c1_g))+"\t");
		//total utility		
		System.out.println(resultFormat.format(T_alphaNDCG(m, cutoff, c1_g, c1_idealList))
				+"\t"+resultFormat.format(T_alphaNDCG(m, cutoff, c1_g, c1_idealList)));
	
		
		//case-2
		System.out.println("alpha-nDCG w.r.t. Case-2");
		//i
		for(int k=1; k<=cutoff; k++){
			System.out.print(resultFormat.format(mu_alphaNDCG(stI, k, c2_g))+"\t");
		}		
		System.out.print(resultFormat.format(U_alphaNDCG(stI,cutoff,c2_g))+"\t");
		//j
		for(int k=1; k<=cutoff; k++){
			System.out.print(resultFormat.format(mu_alphaNDCG(stJ, k, c2_g))+"\t");
		}
		System.out.print(resultFormat.format(U_alphaNDCG(stJ,cutoff,c2_g))+"\t");
		//total utility
		
		System.out.println(resultFormat.format(T_alphaNDCG(m, cutoff, c2_g, c2_idealList))
				+"\t"+resultFormat.format(T_alphaNDCG(m, cutoff, c2_g, c2_idealList)));
		
		
		//case-3		
		System.out.println("alpha-nDCG w.r.t. Case-3");
		//i
		for(int k=1; k<=cutoff; k++){
			System.out.print(resultFormat.format(mu_alphaNDCG(stI, k, c3_g))+"\t");
		}		
		System.out.print(resultFormat.format(U_alphaNDCG(stI,cutoff,c3_g))+"\t");
		//j
		for(int k=1; k<=cutoff; k++){
			System.out.print(resultFormat.format(mu_alphaNDCG(stJ, k, c3_g))+"\t");
		}
		System.out.print(resultFormat.format(U_alphaNDCG(stJ,cutoff,c3_g))+"\t");
		//total utility
		
		System.out.println(resultFormat.format(T_alphaNDCG(m, cutoff, c3_g, c3_idealList))
				+"\t"+resultFormat.format(T_alphaNDCG(m, cutoff, c3_g, c3_idealList)));
	}
	//D#-nDCG
	public void cal_DSharpNDCG(int m, int cutoff, int stI, int stJ, int [][] c1_g, int [][] c2_g, int [][] c3_g, double [] equalStPro, double [] biasedStPro){
		////D-nDCG
		System.out.println();
		//case-1
		System.out.println("D-nDCG w.r.t. Case-1");
		//i
		for(int k=1; k<=cutoff; k++){
			System.out.print(resultFormat.format(mu_DnDCG(stI, k, c1_g))+"\t");
		}		
		System.out.print(resultFormat.format(U_DnDCG(stI,cutoff,c1_g))+"\t");
		//j
		for(int k=1; k<=cutoff; k++){
			System.out.print(resultFormat.format(mu_DnDCG(stJ, k, c1_g))+"\t");
		}
		System.out.print(resultFormat.format(U_DnDCG(stJ,cutoff,c1_g))+"\t");
		//total utility		
		
		System.out.println(resultFormat.format(T_DnDCG(m, cutoff, equalStPro, c1_g))
				+" (D#-nDCG:"+resultFormat.format(DSharp_nDCG(m, cutoff, equalStPro, c1_g))+")"
				
				+"\t"+resultFormat.format(T_DnDCG(m, cutoff, biasedStPro, c1_g))
				+" (D#-nDCG:"+resultFormat.format(DSharp_nDCG(m, cutoff, biasedStPro, c1_g))+")");
		//
		
		//case-2
		System.out.println("D-nDCG w.r.t. Case-2");
		//i
		for(int k=1; k<=cutoff; k++){
			System.out.print(resultFormat.format(mu_DnDCG(stI, k, c2_g))+"\t");
		}		
		System.out.print(resultFormat.format(U_DnDCG(stI,cutoff,c2_g))+"\t");
		//j
		for(int k=1; k<=cutoff; k++){
			System.out.print(resultFormat.format(mu_DnDCG(stJ, k, c2_g))+"\t");
		}
		System.out.print(resultFormat.format(U_DnDCG(stJ,cutoff,c2_g))+"\t");
		//total utility
		System.out.println(resultFormat.format(T_DnDCG(m, cutoff, equalStPro, c2_g))
				+" (D#-nDCG:"+resultFormat.format(DSharp_nDCG(m, cutoff, equalStPro, c2_g))+")"
				
				+"\t"+resultFormat.format(T_DnDCG(m, cutoff, biasedStPro, c2_g))
				+" (D#-nDCG:"+resultFormat.format(DSharp_nDCG(m, cutoff, biasedStPro, c2_g))+")");
		
		//case-3
		System.out.println("D-nDCG w.r.t. Case-3");
		//i
		for(int k=1; k<=cutoff; k++){
			System.out.print(resultFormat.format(mu_DnDCG(stI, k, c3_g))+"\t");
		}		
		System.out.print(resultFormat.format(U_DnDCG(stI,cutoff,c3_g))+"\t");
		//j
		for(int k=1; k<=cutoff; k++){
			System.out.print(resultFormat.format(mu_DnDCG(stJ, k, c3_g))+"\t");
		}
		System.out.print(resultFormat.format(U_DnDCG(stJ,cutoff,c3_g))+"\t");
		//total utility
		System.out.println(resultFormat.format(T_DnDCG(m, cutoff, equalStPro, c3_g))
				+" (D#-nDCG:"+resultFormat.format(DSharp_nDCG(m, cutoff, equalStPro, c3_g))+")"
				
				+"\t\t"+resultFormat.format(T_DnDCG(m, cutoff, biasedStPro, c3_g))
				+" (D#-nDCG:"+resultFormat.format(DSharp_nDCG(m, cutoff, biasedStPro, c3_g))+")");
		System.out.println();
	}
	//DIN#-nDCG
	public void cal_DINSharpNDCG(int m, int cutoff, int stI, int stJ, int [][] c1_g, int [][] c2_g, int [][] c3_g, double [] equalStPro, double [] biasedStPro){
		////DIN-nDCg
		HashSet<Integer> infStSet_First = new HashSet<Integer>();
		infStSet_First.add(1);		

		//case-1-First
		System.out.println("DIN-nDCG w.r.t. Case-1(inf-i)");
		//i
		for(int k=1; k<=cutoff; k++){
			System.out.print(resultFormat.format(mu_DINnDCG(stI, k, c1_g, infStSet_First))+"\t");
		}		
		System.out.print(resultFormat.format(U_DINnDCG(stI,cutoff,c1_g, infStSet_First))+"\t");
		//j
		for(int k=1; k<=cutoff; k++){
			System.out.print(resultFormat.format(mu_DINnDCG(stJ, k, c1_g, infStSet_First))+"\t");
		}
		System.out.print(resultFormat.format(U_DINnDCG(stJ,cutoff,c1_g, infStSet_First))+"\t");
		//total utility
		System.out.println(resultFormat.format(T_DINnDCG(m, cutoff, equalStPro, c1_g, infStSet_First))
				+" (DIN#-nDCG:"+resultFormat.format(DINSharp_nDCG(m, cutoff, equalStPro, c1_g, infStSet_First))+")"
				+"\t"+resultFormat.format(T_DINnDCG(m, cutoff, biasedStPro, c1_g, infStSet_First))
				+" (DIN#-nDCG:"+resultFormat.format(DINSharp_nDCG(m, cutoff, biasedStPro, c1_g, infStSet_First))+")");
		
		//case-2-First
		System.out.println("DIN-nDCG w.r.t. Case-2(inf-i)");
		//i
		for(int k=1; k<=cutoff; k++){
			System.out.print(resultFormat.format(mu_DINnDCG(stI, k, c2_g, infStSet_First))+"\t");
		}		
		System.out.print(resultFormat.format(U_DINnDCG(stI,cutoff,c2_g, infStSet_First))+"\t");
		//j
		for(int k=1; k<=cutoff; k++){
			System.out.print(resultFormat.format(mu_DINnDCG(stJ, k, c2_g, infStSet_First))+"\t");
		}
		System.out.print(resultFormat.format(U_DINnDCG(stJ,cutoff,c2_g, infStSet_First))+"\t");
		//total utility
		System.out.println(resultFormat.format(T_DINnDCG(m, cutoff, equalStPro, c2_g, infStSet_First))
				+" (DIN#-nDCG:"+resultFormat.format(DINSharp_nDCG(m, cutoff, equalStPro, c2_g, infStSet_First))+")"
				+"\t"+resultFormat.format(T_DINnDCG(m, cutoff, biasedStPro, c2_g, infStSet_First))
				+" (DIN#-nDCG:"+resultFormat.format(DINSharp_nDCG(m, cutoff, biasedStPro, c2_g, infStSet_First))+")");
		
		//case-3-First
		System.out.println("DIN-nDCG w.r.t. Case-3(inf-i)");
		//i
		for(int k=1; k<=cutoff; k++){
			System.out.print(resultFormat.format(mu_DINnDCG(stI, k, c3_g, infStSet_First))+"\t");
		}		
		System.out.print(resultFormat.format(U_DINnDCG(stI,cutoff,c3_g, infStSet_First))+"\t");
		//j
		for(int k=1; k<=cutoff; k++){
			System.out.print(resultFormat.format(mu_DINnDCG(stJ, k, c3_g, infStSet_First))+"\t");
		}
		System.out.print(resultFormat.format(U_DINnDCG(stJ,cutoff,c3_g, infStSet_First))+"\t");
		//total utility
		System.out.println(resultFormat.format(T_DINnDCG(m, cutoff, equalStPro, c3_g, infStSet_First))
				+" (DIN#-nDCG:"+resultFormat.format(DINSharp_nDCG(m, cutoff, equalStPro, c3_g, infStSet_First))+")"
				+"\t\t"+resultFormat.format(T_DINnDCG(m, cutoff, biasedStPro, c3_g, infStSet_First))
				+" (DIN#-nDCG:"+resultFormat.format(DINSharp_nDCG(m, cutoff, biasedStPro, c3_g, infStSet_First))+")");

		////
		HashSet<Integer> infStSet_Second = new HashSet<Integer>();
		//Second-case-1
		System.out.println("DIN-nDCG w.r.t. Case-1(all nav)");
		//i
		for(int k=1; k<=cutoff; k++){
			System.out.print(resultFormat.format(mu_DINnDCG(stI, k, c1_g, infStSet_Second))+"\t");
		}		
		System.out.print(resultFormat.format(U_DINnDCG(stI,cutoff,c1_g, infStSet_Second))+"\t");
		//j
		for(int k=1; k<=cutoff; k++){
			System.out.print(resultFormat.format(mu_DINnDCG(stJ, k, c1_g, infStSet_Second))+"\t");
		}
		System.out.print(resultFormat.format(U_DINnDCG(stJ,cutoff,c1_g, infStSet_Second))+"\t");
		//total utility
		System.out.println(resultFormat.format(T_DINnDCG(m, cutoff, equalStPro, c1_g, infStSet_Second))
				+" (DIN#-nDCG:"+resultFormat.format(DINSharp_nDCG(m, cutoff, equalStPro, c1_g, infStSet_Second))+")"
				+"\t"+resultFormat.format(T_DINnDCG(m, cutoff, biasedStPro, c1_g, infStSet_Second))
				+" (DIN#-nDCG:"+resultFormat.format(DINSharp_nDCG(m, cutoff, biasedStPro, c1_g, infStSet_Second))+")");
		
		//Second-case-2
		System.out.println("DIN-nDCG w.r.t. Case-2(all nav)");
		//i
		for(int k=1; k<=cutoff; k++){
			System.out.print(resultFormat.format(mu_DINnDCG(stI, k, c2_g, infStSet_Second))+"\t");
		}		
		System.out.print(resultFormat.format(U_DINnDCG(stI,cutoff,c2_g, infStSet_Second))+"\t");
		//j
		for(int k=1; k<=cutoff; k++){
			System.out.print(resultFormat.format(mu_DINnDCG(stJ, k, c2_g, infStSet_Second))+"\t");
		}
		System.out.print(resultFormat.format(U_DINnDCG(stJ,cutoff,c2_g, infStSet_Second))+"\t");
		//total utility
		System.out.println(resultFormat.format(T_DINnDCG(m, cutoff, equalStPro, c2_g, infStSet_Second))
				+" (DIN#-nDCG:"+resultFormat.format(DINSharp_nDCG(m, cutoff, equalStPro, c2_g, infStSet_Second))+")"
				+"\t"+resultFormat.format(T_DINnDCG(m, cutoff, biasedStPro, c2_g, infStSet_Second))
				+" (DIN#-nDCG:"+resultFormat.format(DINSharp_nDCG(m, cutoff, biasedStPro, c2_g, infStSet_Second))+")");
		
		//Second-case-3
		System.out.println("DIN-nDCG w.r.t. Case-3(all nav)");
		//i
		for(int k=1; k<=cutoff; k++){
			System.out.print(resultFormat.format(mu_DINnDCG(stI, k, c3_g, infStSet_Second))+"\t");
		}		
		System.out.print(resultFormat.format(U_DINnDCG(stI,cutoff,c3_g, infStSet_Second))+"\t");
		//j
		for(int k=1; k<=cutoff; k++){
			System.out.print(resultFormat.format(mu_DINnDCG(stJ, k, c3_g, infStSet_Second))+"\t");
		}
		System.out.print(resultFormat.format(U_DINnDCG(stJ,cutoff,c3_g, infStSet_Second))+"\t");
		//total utility
		System.out.println(resultFormat.format(T_DINnDCG(m, cutoff, equalStPro, c3_g, infStSet_Second))
				+" (DIN#-nDCG:"+resultFormat.format(DINSharp_nDCG(m, cutoff, equalStPro, c3_g, infStSet_Second))+")"
				+"\t"+resultFormat.format(T_DINnDCG(m, cutoff, biasedStPro, c3_g, infStSet_Second))
				+" (DIN#-nDCG:"+resultFormat.format(DINSharp_nDCG(m, cutoff, biasedStPro, c3_g, infStSet_Second))+")");
	}
	
	
	/////////
	public static void main(String []args){
		//1
		//Cal cal = new Cal();
		//cal.com();
		
		//--
		double logV =  Math.log10(2+1)/Math.log10(2);
		double dom = 2 + 2/logV;
		System.out.println(1/dom);
		System.out.println(2/dom);
		//--		
	}

}
