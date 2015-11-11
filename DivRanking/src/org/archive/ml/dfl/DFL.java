package org.archive.ml.dfl;

import org.archive.ml.clustering.ap.matrix.DoubleMatrix2D;

/**
 * Discrete Facility Location
 * **/

public abstract class DFL {
	protected static final boolean debug = false;
	
	////Basic Parameters with default values ////	
	protected double _lambda = 0.5;
	protected int _iterationTimes = 5000;
	
	//number of customers	0<=i<N
	protected int _N;
	//number of potential facilities	0<=j<M
	protected int _M;
	//C_ij: the cost (or distance) of assigning a customer i to facility j
	protected DoubleMatrix2D _C;
	//Y_j the cost of opening the facility Y_j
	//one-row object
	protected DoubleMatrix2D _Y;
	
	
	protected int getIterationTimes(){
		return this._iterationTimes;
	}
	
	
	protected abstract Integer getCustomerID(String cName);
	protected abstract String  getCustomerName(Integer cID);
	protected abstract Integer getFacilityID(String fName);
	protected abstract String  getFacilityName(Integer fID);
	
	
	
	

}
