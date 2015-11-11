package org.archive.ml.dfl;

import org.archive.ml.clustering.ap.matrix.DoubleMatrix2D;

/**
 * Explicit Uncapacitated Discrete Facility Location
 * **/
public class DFL_U_E extends DFL_U {
	
	
	
	
	
	
	
	private void computeV_CFDifferCase(){
		DoubleMatrix2D Eta_minus_C = this._Eta.minus(this._C);
		DoubleMatrix2D maxZero = Eta_minus_C.max(0);
		this._V = maxZero.sumEachColumn();		
	}
	
	private void computeAlpha_CFDifferCase(){
		DoubleMatrix2D Eta_minus_C = this._Eta.minus(this._C);
		DoubleMatrix2D maxZero = Eta_minus_C.max(0);
		DoubleMatrix2D columnSum = maxZero.sumEachColumn();		
		//the value at ij should not be added
		DoubleMatrix2D sameColumnVector = columnSum.minus(this._Y);
		double [] sameColumnArray = new double [this._M];
		for(int j=0; j<this._M; j++){
			sameColumnArray[j] = sameColumnVector.get(0, j);
		}
		DoubleMatrix2D transposedTarget = new DoubleMatrix2D(this._N, sameColumnArray);
		DoubleMatrix2D target = transposedTarget.transpose();
		DoubleMatrix2D rightMatrix = target.minus(maxZero);
		this._Alpha = rightMatrix.min(0);		
	}

}
