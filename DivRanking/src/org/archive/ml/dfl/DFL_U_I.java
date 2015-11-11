package org.archive.ml.dfl;

import java.util.Map;
import java.util.TreeMap;

import org.archive.ml.clustering.ap.matrix.DoubleMatrix1D;
import org.archive.ml.clustering.ap.matrix.DoubleMatrix2D;
import org.archive.ml.ufl.UFL.UFLMode;

/**
 * Implicit Uncapacitated Discrete Facility Location
 * **/
public class DFL_U_I extends DFL_U {
	// for the case of C is the same as F    
    private Integer _cfID = 0;
    protected Map<String, Integer> _cfIDMapper = new TreeMap<String, Integer>();
    protected Map<Integer, String> _cfIDRevMapper = new TreeMap<Integer, String>();
    
    
    ////message variables
    //Eta_{MM}
    //V_{1M}
    //Alpha_{MM}
    
    
    public void run(){
		int itrTimes = getIterationTimes();
		initConvergence();
		
		for(int itr=1; itr<=itrTimes; itr++){
			//
			this.copyEta();
			this.computeEta();
			this.updateEta();
			
			//
			this.copyAlpha();
			this.computeAlpha();
			this.updateAlpha();
			
			//
			this.copyV();
			this.computeV();
			//
			computeIteratingBeliefs();
			//
			calculateCovergence();
			//
			if(!checkConvergence()){
				break;
			}
		}		
		//
		computeBeliefs();
	}
    
    ////
    protected void computeV(){
    	computeV_CFSameCase();		
	}
    private void computeV_CFSameCase(){
		DoubleMatrix2D Eta_minus_C = this._Eta.minus(this._C);
		DoubleMatrix2D maxZero = Eta_minus_C.max(0);
		for(int j=0; j<this._M; j++){
			maxZero.set(j, j, 0.0);
		}
		DoubleMatrix2D cSum = maxZero.sumEachColumn();
		this._V = new DoubleMatrix2D(1, this._M, 0.0);
		for(int j=0; j<this._M; j++){
			this._V.set(0, j, 
					cSum.get(0, j)+this._Eta.get(j, j)-this._C.get(j, j));
		}		
	}
    //
    protected void computeAlpha(){
    	computeAlpha_CFSameCase();			
	}
    private void computeAlpha_CFSameCase(){
		DoubleMatrix2D Eta_minus_C = this._Eta.minus(this._C);
		DoubleMatrix2D maxZero = Eta_minus_C.max(0);		
		for(int j=0; j<this._M; j++){
			maxZero.set(j, j, 0.0);
		}
		DoubleMatrix2D maxZeroNoJJ = maxZero.sumEachColumn();
		//for i=j
		DoubleMatrix2D IisJ = maxZeroNoJJ.minus(this._Y);
		//--for i<>j		
		double [] row = new double [this._M];
		DoubleMatrix1D etaJJ = this._Eta.diag();
		DoubleMatrix1D cJJ = this._C.diag();
		for(int j=0; j<this._M; j++){
			row[j] = maxZeroNoJJ.get(0, j)+etaJJ.get(j)-cJJ.get(j)-this._Y.get(0, j);
		}		
		DoubleMatrix2D reversedM = new DoubleMatrix2D(this._N, row);
		DoubleMatrix2D rightM = reversedM.transpose();		
		DoubleMatrix2D rep = rightM.minus(maxZero);
		this._Alpha = rep.min(0);
		//
		for(int i=0; i<this._N; i++){
			this._Alpha.set(i, i, IisJ.get(0, i));
		}				
	}

    ////
    protected Integer getCustomerID(String cName) {
    	if(_cfIDMapper.containsKey(cName)){
			return _cfIDMapper.get(cName);
		}else{
			Integer id = _cfID;
			_cfIDMapper.put(cName, id);
			_cfIDRevMapper.put(id, cName);
			_cfID++;
			return id;
		}       
    }
    protected String getCustomerName(Integer cID){
    	return this._cfIDRevMapper.get(cID);
    }
    protected Integer getFacilityID(String fName){
    	if(_cfIDMapper.containsKey(fName)){
			return _cfIDMapper.get(fName);
		}else{
			Integer id = _cfID;
			_cfIDMapper.put(fName, id);
			_cfIDRevMapper.put(id, fName);
			_cfID++;
			return id;
		}   	
    }
    protected String getFacilityName(Integer fID){
    	return this._cfIDRevMapper.get(fID);
    }

}
