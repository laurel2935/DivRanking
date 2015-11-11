package org.archive.ml.dfl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.archive.ml.clustering.ap.abs.ConvitsVector;
import org.archive.ml.clustering.ap.matrix.DoubleMatrix1D;
import org.archive.ml.clustering.ap.matrix.DoubleMatrix2D;
import org.archive.ml.clustering.ap.matrix.IntegerMatrix1D;
import org.archive.ml.ufl.UFL.UFLMode;

/**
 * Uncapacitated Discrete Facility Location
 * **/

public abstract class DFL_U extends DFL {
	
	
	//thus, the size of iteration-span that without change of exemplar
	protected Integer _noChangeIterSpan = null;
	protected Map<Integer, ConvitsVector> convitsVectors = new HashMap<Integer, ConvitsVector>();
	
	/**
	 * Implicit DFL_U and Explicit DFL_U have the same message-variables, i.e., \eta, \v, \alpha
	 * The differences are: (1) different iteration formula; (2) customer is different from facility 
	 * **/
	
	protected DoubleMatrix2D _Eta;
	protected DoubleMatrix2D _oldEta;
	
	//
	protected DoubleMatrix2D _V;
	protected DoubleMatrix2D _oldV;
	
	//
	protected DoubleMatrix2D _Alpha;
	protected DoubleMatrix2D _oldAlpha;
	
	//i.e., the index of positive elements in the binary matrix
	protected IntegerMatrix1D IX = null;    
    //i.e., the index of positive elements in the y vector
	protected IntegerMatrix1D IY = null;
    
	
	
	
	////
	/**
     * initialize the indicator of convergence vectors
     * **/
    protected void initConvergence() {
        //System.out.println("S: " + S.toString());
        if (this._noChangeIterSpan != null) {
            for (int j = 0; j < this._M; j++) {
                ConvitsVector vec = new ConvitsVector(this._noChangeIterSpan.intValue(), Integer.valueOf(j));
                vec.init();
                this.convitsVectors.put(Integer.valueOf(j), vec);
            }
        }
    }
    
    protected void computeIteratingBeliefs() {
		DoubleMatrix2D AlphaEtaC = this._Alpha.plus(this._Eta).minus(this._C);
        //the indexes of potential exemplars
		if(UFLMode.C_Same_F == this._uflMode){
			this.IX = AlphaEtaC.diag().findG(0);
		}else{
			ArrayList<Integer> fList = new ArrayList<Integer>();
			for(int j=0; j<this._M; j++){
				for(int i=0; i<this._N; i++){
					if(AlphaEtaC.get(i, j) >= 0){
						fList.add(j);
					}
				}
			}
			//
			this.IX = new IntegerMatrix1D(fList.toArray(new Integer[0]));
		}
        if(debug){
        	System.out.println("Iterating ... >0 exemplars[X]:");
        	System.out.println(IX.toString());
        }
                
        DoubleMatrix1D EY;
        EY = this._V.minus(this._Y).getRow(0);
        IY = EY.findG(0);
        if(debug){
        	System.out.println("Iterating ... >0 Facilities[Y]:");
        	System.out.println(IY.toString());
        }
        IntegerMatrix1D equalIY = EY.findG_WithEqual(0);
        if(debug){
        	System.out.println("Iterating ... >=0 Facilities[Y]:");
        	System.out.println(equalIY.toString());
        }
    }
	
	////Eta ////
	protected void copyEta(){
		this._oldEta = this._Eta.copy();
		if(debug){
       	System.out.println("old Eta:");
       	System.out.println(_oldEta.toString());
       }
	}
	//
	protected void computeEta(){
		DoubleMatrix2D Alpha_minus_C = this._Alpha.minus(this._C);
		DoubleMatrix2D max = Alpha_minus_C.maxr();
		for(int i=0; i<this._N; i++){
			Alpha_minus_C.set(i, (int)max.get(i, 0), Double.NEGATIVE_INFINITY);
		}
		DoubleMatrix2D max2 = Alpha_minus_C.maxr();
		//
		double [] sameRow = new double [this._N];
		for(int i=0; i<this._N; i++){
			sameRow[i] = max.get(i, 1);
		}
		//
		DoubleMatrix2D maxElements = new DoubleMatrix2D(this._M, sameRow);
		DoubleMatrix2D zeroM = new DoubleMatrix2D(this._N, this._M, 0.0);
		//before real eta
		this._Eta = zeroM.minus(maxElements);
		//real eta
		for(int i=0; i<this._N; i++){
			this._Eta.set(i, (int)max.get(i, 0), 0.0-max2.get(i, 1));
		}			
	}
	//
	protected void updateEta(){
		this._Eta = this._Eta.mul(1-getLambda()).plus(this._oldEta.mul(getLambda()));
	}
	////V ////
	protected void copyV(){
		this._oldV = this._V.copy();
		if(debug){
       	System.out.println("old V:");
       	System.out.println(_oldV.toString());
       }
	}
	////Alpha ////
	protected void copyAlpha(){
		this._oldAlpha = this._Alpha.copy();
		if(debug){
       	System.out.println("old Alpha:");
       	System.out.println(_oldAlpha.toString());
       }
	}
	//
	protected void updateAlpha(){
		this._Alpha = this._Alpha.mul(1-getLambda()).plus(this._oldAlpha.mul(getLambda()));
	}
	
    //vary w.r.t. U & I
	protected abstract void computeV();
	protected abstract void computeAlpha();
	
	
	////
	public double getLambda(){
		return this._lambda;
	}

}
