/* ===========================================================
 * APGraphClusteringPlugin : Java implementation of affinity propagation
 * algorithm as Cytoscape plugin.
 * ===========================================================
 *
 *
 * Project Info:  http://bioputer.mimuw.edu.pl/modevo/
 * Sources: http://code.google.com/p/misiek/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *
 * [Java is a trademark or registered trademark of Sun Microsystems, Inc.
 * in the United States and other countries.]
 *
 * APGraphClusteringPlugin  Copyright (C) 2008-2010
 * Authors:  Michal Wozniak (code) (m.wozniak@mimuw.edu.pl)
 *           Janusz Dutkowski (idea) (j.dutkowski@mimuw.edu.pl)
 *           Jerzy Tiuryn (supervisor) (tiuryn@mimuw.edu.pl)
 */
package org.archive.ml.clustering.ap.matrix;

import java.util.Collection;
import java.util.TreeSet;
import java.util.Vector;

import org.archive.ml.clustering.ap.abs.AffinityPropagationAlgorithm;
import org.archive.ml.clustering.ap.abs.ConvitsVector;

public class MatrixPropagationAlgorithm extends AffinityPropagationAlgorithm {
	
	private static final boolean debug = false;

	//number of data points
    private int N;
    //availability
    private DoubleMatrix2D A;
    //exemplar vector, i.e., the size of I equals the number of exemplars,
    //the value of each element is the exemplar index
    //i.e., the index of the positive element of the diagonal of R+A 
    private IntegerMatrix1D I = null;
    //responsibility
    private DoubleMatrix2D R;
    //previous iteration's availability
    private DoubleMatrix2D aold = null;
    //previous iteration's responsibility
    private DoubleMatrix2D rold = null;
    //similarity matrix
    public DoubleMatrix2D S;    
    //the number of exemplar
    private int clustersNumber = 0;

    @Override
    public void init() {
        A = new DoubleMatrix2D(N, N, 0);
        R = new DoubleMatrix2D(N, N, 0);
        S = new DoubleMatrix2D(N, N, Double.NEGATIVE_INFINITY);
    }

    public int getN() {
        return N;
    }

    public void setN(final int N) {
        this.N = N;
    }

    public IntegerMatrix1D idx(final IntegerMatrix1D C, final IntegerMatrix1D I) {

        IntegerMatrix1D res = new IntegerMatrix1D(C.size());

        for (int i = 0; i < C.size(); i++) {
            res.set(i, I.get(C.get(i).intValue()));
        }

        return res;
    }

    public IntegerMatrix1D tmp(final IntegerMatrix1D C, final IntegerMatrix1D I) {
        IntegerMatrix1D res = C.copy();
        for (int i = 0; i < I.size(); i++) {
            res.set(I.get(i), Integer.valueOf(i));
        }

        return res;
    }

    public void setSimilarities(final double[][] similarities) {
        N = similarities.length;
        this.S = new DoubleMatrix2D(N, N, similarities);
    }
    //set similarity
    @Override
    public void setSimilarityInt(final Integer x, final Integer y, final Double sim) {
    	if(x >= N || y >= N) {
            System.out.println("ROZMIAR: "+N+ "query: "+x +" "+y);
        }else{
        	if (graphMode == AffinityGraphMode.DIRECTED) {    	        
    	        S.set(x, y, sim.doubleValue());
            } else {
                S.set(x, y, sim.doubleValue());
                S.set(y, x, sim.doubleValue());
            }
        }    	
    }

    @Override
    public void setSimilarity(final String from, final String to, final Double sim) {

        Integer x = getExamplarID(from);
        Integer y = getExamplarID(to);
        if (graphMode == AffinityGraphMode.DIRECTED) {
        	S.set(x, y, sim.doubleValue());
        } else {
            S.set(x, y, sim.doubleValue());
            S.set(y, x, sim.doubleValue());
        }
    }

    @Override
    /**
     * R -> rold, i.e., buffer the previous R
     * **/
    protected void copyResponsibilies() {
        rold = R.copy();
        if(debug){
        	System.out.println("old R:");
        	System.out.println(rold.toString());
        }
    }

    @Override
    protected void computeResponsibilities() {
    	//maximum element of each row of AS corresponding to the case of exemplar point
        DoubleMatrix2D YI2;
        //maximum element of each row of AS
        DoubleMatrix2D YI;
        //sum of A and S
        DoubleMatrix2D AS;

        //
        double[] pom = new double[N];
        //availability plus similarity, i.e., a(i,k^')+s(i,k^')
        AS = A.plus(S);
        //i.e., max{a(i,k^')+s(i,k^')}
        YI = AS.maxr();
        
        //for the case of the k' that corresponds to the maximum sum value,
        //the r() value should be s()- "the second maximum sum"
        for (int i = 0; i < N; i++) {
            int y = (int) YI.get(i, 0);
            AS.set(i, y, Double.NEGATIVE_INFINITY);
        }
        YI2 = AS.maxr();

        for (int i = 0; i < N; i++) {
            pom[i] = YI.get(i, 1);
        }
        DoubleMatrix2D Rep = new DoubleMatrix2D(N, pom);
        //for common r(i,k), where i is not candidate exemplar
        R = S.minus(Rep);
        //for the case of exemplar, thus the maximum should not be itself
        //for point of this kind, it is: r(i,k)=s(i,k)- "the second maximum sum"
        for (int i = 0; i < N; i++) {
            R.set(i, (int) YI.get(i, 0), S.get(i, (int) YI.get(i, 0)) - YI2.get(i, 1));
        }
    }

    @Override
    protected void avgResponsibilies() {
        R = R.mul(1 - getLambda()).plus(rold.mul(getLambda()));
        // System.out.println("R: "+R.toString());
    }

    @Override
    /**
     * A -> aold, i.e., buffer the previous A
     * **/
    protected void copyAvailabilities() {
        aold = A.copy();
        if(debug){
        	System.out.println("old A:");
        	System.out.println(aold.toString());
        }
    }
    //?     
    protected void computeAvailabilities_change() {
        DoubleMatrix1D dA;
        DoubleMatrix2D rp;

        //System.out.println("R: " + R.toString());
        //sum_max equation, which should not include: r(i,k) & r(k,k)
        rp = R.max(0);
        //set correct r(k,k) thus corresponds to r(k,k)+sum_max equation when sum()
        //however should not include r(i,k)
        //??? having changed R_ii, but [ dA = A.diag();] is later used for set A_ii
        for (int i = 0; i < N; i++) {
            rp.set(i, i, R.get(i, i));
        }
        // System.out.println("rp: "+rp.toString());
        //(1):[(new DoubleMatrix2D(N, rp.sum().getVector(0))).transpose()] = r(k,k)+{sum_max equation}        
        //here minus(rp) because of max{0,r(i,k)} is wrongly included
        //because r(i,k) should not be added, thus when r(i,k)>0, it will be wrongly added, thus should be deleted
        A = (new DoubleMatrix2D(N, rp.sumEachColumn().getVector(0))).transpose().minus(rp);
        //  System.out.println("A-pom: "+A.toString());
        dA = A.diag();
        
        A = A.min(0);
        //specific case of a(k,k), i.e., the sum of max(0, r(i,k)) without i=k
        for (int i = 0; i < N; i++) {
            A.set(i, i, dA.get(i));
        }
        // System.out.println("A-last: "+A.toString());
    }
    //new
    protected void computeAvailabilities() {
    	DoubleMatrix2D maxZero = R.max(0);
    	for(int k=0; k<N; k++){
    		maxZero.set(k, k, 0.0);
    	}
    	//no kk for both
    	DoubleMatrix2D cSumNoKK = maxZero.sumEachColumn();
    	//
    	double [] row = new double[N];
    	for(int k=0; k<N; k++){
    		row[k] = cSumNoKK.get(0, k)+R.get(k, k);
    	}
    	DoubleMatrix2D IK = new DoubleMatrix2D(N, row);
    	this.A = IK.transpose().minus(maxZero).min(0);
    	for(int k=0; k<N; k++){
    		this.A.set(k, k, cSumNoKK.get(0, k));
    	}    	
    }

    @Override
    protected void avgAvailabilities() {
        //  System.out.println("Aold: "+aold.toString());
        A = A.mul((1 - getLambda())).plus(aold.mul(getLambda()));
        // System.out.println("A: "+A.toString());
    }

    @Override
    protected void computeCenters() {
        DoubleMatrix2D RA = R.plus(A);
        //
        DoubleMatrix2D maxAR = RA.maxr();
        if(debug){        	
        	System.out.println("Maximum centers:");
        	for(int i=0; i<maxAR.getN(); i++){
        		//System.out.println(i+" -> "+(int)AR.get(i, 0));
        		if(i == (int)maxAR.get(i, 0)){
        			System.out.print(i+"("+this.idRevMapper.get(i)+")"+"\t");
        		}
        	}
        	System.out.println();
        }
        //
        //the indexes of potential exemplars
        
        I = RA.diag().findG(0);
        clustersNumber = I.size(); 
        
        //System.out.println("Ap Cluster Number:\t"+clustersNumber);
        
        if(debug){
        	System.out.println("Bigger Zero centers:");
        	for(int eID: I.getVector()){
        		System.out.print(eID+"("+this.idRevMapper.get(eID)+")"+"\t");
        	}
        	System.out.println();
        }
        
        IntegerMatrix1D equalI = RA.diag().findG_WithEqual(0);
        if(debug){
        	System.out.println("BiggerAndEqual Zero centers:");
        	for(int eID: equalI.getVector()){
        		System.out.print(eID+"("+this.idRevMapper.get(eID)+")"+"\t");
        	}
        	System.out.println();
        }
    }

    @Override
    public int getClustersNumber() {
        return I.size();
    }
    //set the preference value with a given constant
    @Override
    public void setConstPreferences(Double preferences) {
        for (int i = 0; i < N; i++) {
            S.set(i, i, preferences);
        }
    }

    @Override
    public Collection<Integer> getCentersAlg() {
        Collection<Integer> res = new TreeSet<Integer>();
        for (int i = 0; i < I.size(); i++) {
            res.add(Integer.valueOf(I.get(i)));
        }

        return res;
    }

    @Override
    /**
     * essentially all the data points
     * **/
    protected Collection<Integer> getAllExamplars() {
        Collection<Integer> res = new TreeSet<Integer>();
        for (int i = 0; i < N; i++) {
            res.add(Integer.valueOf(i));
        }

        return res;
    }
    /**
     * @return get the similarity value, e.g., the way as the original paper or other variant ways
     * **/
    protected Double tryGetSimilarityInt(Integer i, Integer j) {
        double sim = S.get(i.intValue(), j.intValue());
        if (sim > Double.NEGATIVE_INFINITY) {
            return sim;
        } else {
            return null;
        }
    }

    @Override
    protected Double tryGetSimilarity(String from, String to) {
        double sim = S.get(idMapper.get(from), idMapper.get(to));
        if (sim > Double.NEGATIVE_INFINITY) {
            return sim;
        } else {
            return null;
        }
    }

    @Override
    protected void calculateCovergence() {
        if (convits != null) {
            Vector<Integer> c = I.getVector();
            for (int i = 0; i < N; i++) {
                Integer ex = Integer.valueOf(i);
                //after each iteration, examine whether each node is an exemplar,
                //then check the sequential true or false value of each node to determine convergence!
                if (c.contains(ex)) {
                    convitsVectors.get(ex).addCovits(true);
                } else {
                    convitsVectors.get(ex).addCovits(false);
                }
            }
        }
    }

    @Override
    /**
     * initialize the indicator of convergence vectors
     * **/
    protected void initConvergence() {
        //System.out.println("S: " + S.toString());
        if (convits != null) {
            for (int i = 0; i < N; i++) {
                ConvitsVector vec = new ConvitsVector(convits.intValue(), Integer.valueOf(i));
                vec.init();
                convitsVectors.put(Integer.valueOf(i), vec);
            }
        }
    }

    @Override
    protected void generateNoise() {
        for (int i = 0; i < N; i++) {
            double s = S.get(i, i);
            s = generateNoiseHelp(s);
            S.set(i, i, s);

        }
    }

    @Override
    protected void showInfo() {
    }
    
    //
}
