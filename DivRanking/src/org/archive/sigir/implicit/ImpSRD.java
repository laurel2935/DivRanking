package org.archive.sigir.implicit;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;

import org.archive.dataset.trec.TRECDivLoader;
import org.archive.dataset.trec.TRECDivLoader.DivVersion;
import org.archive.dataset.trec.query.TRECQueryAspects;
import org.archive.ml.clustering.ap.APClustering;
import org.archive.ml.clustering.ap.abs.ConvitsVector;
import org.archive.ml.clustering.ap.affinitymain.InteractionData;
import org.archive.ml.clustering.ap.matrix.DoubleMatrix1D;
import org.archive.ml.clustering.ap.matrix.DoubleMatrix2D;
import org.archive.ml.clustering.ap.matrix.IntegerMatrix1D;
import org.archive.ml.ufl.DCKUFL.ExemplarType;
import org.archive.nicta.kernel.TFIDF_A1;
import org.archive.util.tuple.DoubleInt;
import org.archive.util.tuple.IntIntDouble;
import org.archive.util.tuple.PairComparatorByFirst_Desc;
import org.archive.util.tuple.StrDouble;
import org.archive.util.tuple.TripleComparatorByThird_Desc;

/**
 * Implicit SRD (based on K_UFL)
 * i.e., the case of C_Same_F, while the case of C_Differ_F is not checked.
 * 
 * **/

public class ImpSRD {
	
	private static final boolean debug = false;
	
	private static final boolean _mplp = true;
	//C is the same as F or not
	public enum UFLMode {C_Same_F, C_Differ_F}
	
	//// Basic Parameters with default values ////	
	private double _lambda = 0.5;
	private boolean _noise = false;
	private Random _noiseGenerator = new Random();
	private final double _epsilon = 0.0000001;
	private int _iterationTimes = 5000;
	//thus, the size of iteration-span that without change of exemplar
    protected Integer _noChangeIterSpan = null;
    //given preference for the case of C is the same as F
    private double preferenceCost;    
    private UFLMode _uflMode;
    
    //set of node identifier, i.e., names
    private Collection<String> _cNodeNames;
    private Collection<String> _fNodeNames;
    //
    private Integer _customerID = 0;
    protected Map<String, Integer> _customerIDMapper = new TreeMap<String, Integer>();
    protected Map<Integer, String> _customerIDRevMapper = new TreeMap<Integer, String>();
    //
    private Integer _facilityID = 0;
    protected Map<String, Integer> _facilityIDMapper = new TreeMap<String, Integer>();
    protected Map<Integer, String> _facilityIDRevMapper = new TreeMap<Integer, String>();
    //for the case of C is the same as F
    private Integer _cfID = 0;
    protected Map<String, Integer> _cfIDMapper = new TreeMap<String, Integer>();
    protected Map<Integer, String> _cfIDRevMapper = new TreeMap<Integer, String>();
    
    protected Map<Integer, ConvitsVector> convitsVectors = new HashMap<Integer, ConvitsVector>();
    
    
    
	///////////////////////
	// the corresponding index in the paper i,j  will be essentially i-1 j-1 in the program!
	///////////////////////
    //predefined k
    Integer G_M1_zM;
    
    //as the cost matrix takes the negative value of similarity matrix, thus ...
    //private boolean _logDomain;
    private ArrayList<InteractionData> _costMatrix;
    //f_j, i.e., negative value of d-q relevance
    //private ArrayList<Double> _fList;
    ArrayList<StrDouble> _facilityCostList;
	
	//number of customers	0<=i<N
	private int _N;
	//number of potential facilities	0<=j<M
	private int _M;
	//C_ij: the cost of assigning a customer i to facility j
	private DoubleMatrix2D _C;
	//Y_j the cost of opening the facility Y_j
	//one-row object
	public DoubleMatrix2D _Y;
	
	//N��M
	private DoubleMatrix2D _Eta;
	private DoubleMatrix2D _oldEta;
	
	//1��M
	private DoubleMatrix2D _V;
	private DoubleMatrix2D _oldV;
	
	//N��M
	private DoubleMatrix2D _Alpha;
	private DoubleMatrix2D _oldAlpha;
	
	//(M+1)��M the row corresponds to the state of z_j, the column is the j-th column
	private DoubleMatrix2D _A;
	//private DoubleMatrix2D _oldA;
	private DoubleMatrix2D _B;
	//private DoubleMatrix2D _oldB;
	
	//one-row object
	private DoubleMatrix2D _Gama;
	private DoubleMatrix2D _oldGama;
	
	//exemplar vector, i.e., the size of I equals the number of exemplars,
    //the value of each element is the exemplar index
    //i.e., the index of the positive element of the diagonal of R+A 
    private IntegerMatrix1D IX = null;    
    private IntegerMatrix1D IY = null;
    
    //entire [X] list
  	ArrayList<IntIntDouble> _allX = new ArrayList<IntIntDouble>();  	
    //entire [Y] list
    ArrayList<DoubleInt> _allY = new ArrayList<DoubleInt>();
	
    //the number of exemplar
    private int clustersNumber = 0;
    
    //
    String _topicID;
    
    //pay attention to the positive or negative value of dataPointInteractions && fCostList
    public ImpSRD(String topicID, double lambda, int iterationTimes, Integer noChangeIterSpan, double preferenceCost,
    		Integer preK, UFLMode uflMode, ArrayList<InteractionData> costMatrix, ArrayList<StrDouble> facilityCostList){
    	//1
    	//dataPointSimilarities, for cost, e.g., c_ij, it would be the negative value of each similarity
    	//relevanceList, for facility f_j, it would be the negative value of each one
    	//
    	this._lambda = lambda;
    	this._iterationTimes = iterationTimes;
    	this._noChangeIterSpan = noChangeIterSpan;
    	this.preferenceCost = preferenceCost;
    	this._costMatrix = costMatrix;
    	this._facilityCostList = facilityCostList;
    	this.G_M1_zM = preK;
    	this._uflMode = uflMode;
    	//this._logDomain = logDomain;       
    	this._topicID = topicID;    	
       	//2
    	this.ini();
    }
    
    private void ini(){    	
    	this._cNodeNames = new HashSet<String>();
    	this._fNodeNames = new HashSet<String>();
    	for(InteractionData intData : this._costMatrix){
        	this._cNodeNames.add(intData.getFrom());
        	this._fNodeNames.add(intData.getTo());
        }
    	if(UFLMode.C_Same_F == _uflMode){
    		HashSet<String> nodeSet = new HashSet<String>();
    		nodeSet.addAll(_cNodeNames);
    		nodeSet.addAll(_fNodeNames);
    		this._N = nodeSet.size();
    	    this._M = nodeSet.size();
    	}else{
    		this._N = this._cNodeNames.size();
    		this._M = this._fNodeNames.size();
    	}       
        
        //cost matrix c_ij
        this._C = new DoubleMatrix2D(this._N, this._M, 0);
        
        for (InteractionData intData : this._costMatrix) {
            //System.out.println(intData.getFrom() + " " + intData.getTo() + " " + intData.getSim());          
            double c_ij = intData.getSim();            
            setCost(intData.getFrom(), intData.getTo(), c_ij);
        }        
        if(UFLMode.C_Same_F == _uflMode){
        	System.out.println("pref: " + preferenceCost);        
            for (int i = 0; i < this._N; i++) {
            	double c_ii = preferenceCost;
            	this._C.set(i, i, c_ii);
            }
        }
        
        //
        this._Eta = new DoubleMatrix2D(this._N, this._M, 0);
        this._Alpha = new DoubleMatrix2D(this._N, this._M, 0);
        this._V = new DoubleMatrix2D(1, this._M, 0);
        this._Gama = new DoubleMatrix2D(1, this._M, 0);
        
        this._Y = new DoubleMatrix2D(1, this._M, 0);
        for(StrDouble e: _facilityCostList){
        	this._Y.set(0, getFacilityID(e.first), e.getSecond());
        }
        
        if(debug){
        	System.out.println("Cost matrix:");
        	System.out.println(_C.toString());
        	System.out.println("Y matrix:");
        	System.out.println(_Y.toString());
        }
    }	
    
    public void run(){
		int itrTimes = getIterationTimes();
		
		initConvergence();
		
		//
        if (_noise) {
            generateNoise();
        }
		
		if(UFLMode.C_Same_F == _uflMode){
			for(int itr=1; itr<=itrTimes; itr++){
				if(_mplp){
					//
					//this.copyEta();
					this.computeEta_mplp();
					//this.updateEta();
					
					//
					//this.copyAlpha();
					this.computeAlpha_CFSameCase_mplp();
					//this.updateAlpha();
					
					//
					//this.copyV();
					this.computeV_CFSameCase_mplp();		
					//this.updateV();
					
					//
					//this.copyAB();				
					this.updateAB_mplp();
					
					//
					//this.copyGama();
					this.computeGama_mplp();
					//this.updateGama();
				}else{
					//
					this.copyEta();
					this.computeEta();
					this.updateEta();
					
					//
					this.copyAlpha();
					this.computeAlpha_CFSameCase();
					this.updateAlpha();
					
					//
					this.copyV();
					this.computeV_CFSameCase();		
					this.updateV();
					
					//
					//this.copyAB();				
					this.updateAB();
					
					//
					this.copyGama();
					this.computeGama();
					this.updateGama();
					
				}				
				//
				computeIteratingBeliefs();
				
				calculateCovergence();
				
				if(!checkConvergence()){
					break;
				}
			}
		}else{
			for(int itr=1; itr<=itrTimes; itr++){
				//
				this.copyEta();
				this.computeEta();
				this.updateEta();
				
				//
				this.copyAlpha();
				this.computeAlpha_CFDiferCase();
				this.updateAlpha();
				
				//
				this.copyV();
				this.computeV_CFDifferCase();				
				
				//
				//this.copyAB();				
				this.updateAB();
				
				//
				this.copyGama();
				this.computeGama();
				this.updateGama();
				
				//
				computeIteratingBeliefs();
				
				calculateCovergence();
				
				if(!checkConvergence()){
					break;
				}
			}
		}
		//
		computeBeliefs();
	}
    
    protected void generateNoise() {
    	if(this._uflMode == UFLMode.C_Same_F){
    		for (int i = 0; i < _N; i++) {
                double s = _C.get(i, i);
                s = generateNoiseHelp(s);
                _C.set(i, i, s);

            }
    	}        
    }
    protected Double generateNoiseHelp(Double sim) {
        double ran_tmp = _noiseGenerator.nextDouble();
        double noise_tmp = Math.abs(sim) * _epsilon * ran_tmp;
        return sim - noise_tmp;
    }
    
    protected Integer getCustomerID(String cName) {
    	if(UFLMode.C_Differ_F == this._uflMode){
    		if (_customerIDMapper.containsKey(cName)) {
                return _customerIDMapper.get(cName);
            } else {
                Integer id = _customerID;
                _customerIDMapper.put(cName, id);
                _customerIDRevMapper.put(id, cName);
                _customerID++;
                return id;
            }
    	}else{
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
    }
    protected String getCustomerName(Integer cID){
    	if(UFLMode.C_Differ_F == this._uflMode){    		
    		return this._customerIDRevMapper.get(cID);
    	}else{
    		return this._cfIDRevMapper.get(cID);
    	}
    }
    public Integer getFacilityID(String fName){
    	if(UFLMode.C_Differ_F == _uflMode){
    		if(_facilityIDMapper.containsKey(fName)){
        		return _facilityIDMapper.get(fName);
        	}else{
        		Integer id = _facilityID;
        		_facilityIDMapper.put(fName, id);
        		_facilityIDRevMapper.put(id, fName);
        		_facilityID++;
        		return id;
        	}
    	}else{
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
    }
    public String getFacilityName(Integer fID){
    	if(UFLMode.C_Differ_F == this._uflMode){
    		return this._facilityIDRevMapper.get(fID);
    	}else{
    		return this._cfIDRevMapper.get(fID);
    	}
    }
    //
    public void setCost(final String from, final String to, final Double cost) {

        Integer cID = getCustomerID(from);
        Integer fID = getFacilityID(to);
        if (UFLMode.C_Differ_F == _uflMode) {
        	_C.set(cID, fID, cost.doubleValue());
        } else {
        	_C.set(cID, fID, cost.doubleValue());
        	_C.set(fID, cID, cost.doubleValue());
        }
    }
	
	//// Eta ////
	private void copyEta(){
		this._oldEta = this._Eta.copy();
		if(debug){
        	System.out.println("old Eta:");
        	System.out.println(_oldEta.toString());
        }
	}
	//
	private void computeEta(){
		//alpha-c
		DoubleMatrix2D Alpha_minus_C;
		//maximum element of each row of Alpha_minus_C
		DoubleMatrix2D rMax;
		//for 2nd-maximum element of each row used for the exact maximum element
		DoubleMatrix2D rMax2;
		
		Alpha_minus_C = this._Alpha.minus(this._C);
		rMax = Alpha_minus_C.maxr();		
		for(int i=0; i<this._N; i++){
			Alpha_minus_C.set(i, (int)rMax.get(i, 0), Double.NEGATIVE_INFINITY);
		}
		rMax2 = Alpha_minus_C.maxr();
		
		double [] maxElements = new double[this._N];
		for(int i=0; i<this._N; i++){
			maxElements[i] = rMax.get(i, 1);
		}
		DoubleMatrix2D maxMatrix = new DoubleMatrix2D(this._M, maxElements);
		
		DoubleMatrix2D zeroMatrix = new DoubleMatrix2D(this._N, this._M, 0);
		//the case that k!=j
		this._Eta = zeroMatrix.minus(maxMatrix);
		//the case that k==j
		for(int i=0; i<this._N; i++){
			this._Eta.set(i, (int)rMax.get(i, 0), 0.0-rMax2.get(i, 1));
		}		
	}
	// together with computeEta()
	private void updateEta(){
		this._Eta = this._Eta.mul(1-getLambda()).plus(this._oldEta.mul(getLambda()));
	}
	//
	private void computeEta_mplp(){
		//alpha-c
		DoubleMatrix2D Alpha_minus_C;
		DoubleMatrix2D Alpha_minus_C_2;
		//maximum element of each row of Alpha_minus_C
		DoubleMatrix2D rMax;
		//for 2nd-maximum element of each row used for the exact maximum element
		DoubleMatrix2D rMax2;
		
		Alpha_minus_C = this._Alpha.minus(this._C);
		Alpha_minus_C_2 = this._Alpha.minus(this._C);
		
		rMax = Alpha_minus_C.maxr();		
		for(int i=0; i<this._N; i++){
			Alpha_minus_C.set(i, (int)rMax.get(i, 0), Double.NEGATIVE_INFINITY);
		}
		rMax2 = Alpha_minus_C.maxr();
		
		double [] maxElements = new double[this._N];
		for(int i=0; i<this._N; i++){
			maxElements[i] = rMax.get(i, 1);
		}
		DoubleMatrix2D maxMatrix = new DoubleMatrix2D(this._M, maxElements);
		
		DoubleMatrix2D zeroMatrix = new DoubleMatrix2D(this._N, this._M, 0);
		//the case that k!=j
		this._Eta = zeroMatrix.minus(maxMatrix);
		//the case that k==j
		for(int i=0; i<this._N; i++){
			this._Eta.set(i, (int)rMax.get(i, 0), 0.0-rMax2.get(i, 1));
		}		
		
		//mplp update
		Alpha_minus_C_2.mulTo((this._M-1)/this._M);
		this._Eta.mulTo(1.0/this._M);
		this._Eta = this._Eta.minus(Alpha_minus_C_2);
	}
	
	//// V ////
	private void copyV(){
		this._oldV = this._V.copy();
		if(debug){
        	System.out.println("old V:");
        	System.out.println(_oldV.toString());
        }
	}
	//
	private void computeV_CFDifferCase(){
		DoubleMatrix2D Eta_minus_C = this._Eta.minus(this._C);
		DoubleMatrix2D maxZero = Eta_minus_C.max(0);
		this._V = maxZero.sumEachColumn();		
	}
	private void computeV_CFSameCase(){
		DoubleMatrix2D Eta_minus_C = this._Eta.minus(this._C);
		DoubleMatrix2D maxZero = Eta_minus_C.max(0);
		//no jj
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
	// not used as V is only used for computing beliefs
	private void updateV(){
		this._V = this._V.mul(1-getLambda()).plus(this._oldV.mul(getLambda()));
	}
	
	private void computeV_CFSameCase_mplp(){
		DoubleMatrix2D Eta_minus_C = this._Eta.minus(this._C);
		DoubleMatrix2D maxZero = Eta_minus_C.max(0);
		//no jj
		for(int j=0; j<this._M; j++){
			maxZero.set(j, j, 0.0);
		}
		DoubleMatrix2D cSum = maxZero.sumEachColumn();
		this._V = new DoubleMatrix2D(1, this._M, 0.0);
		for(int j=0; j<this._M; j++){
			this._V.set(0, j, 
					cSum.get(0, j)+this._Eta.get(j, j)-this._C.get(j, j));
		}	
		
		//mplp
		this._V.mulTo(1.0/(this._M+1));
		DoubleMatrix2D Gama_minus_Y = this._Gama.minus(this._Y);
		Gama_minus_Y.mulTo(this._M/(this._M+1));
		this._V = this._V.minus(Gama_minus_Y);
	}
	
	//// Alpha ////
	private void copyAlpha(){
		this._oldAlpha = this._Alpha.copy();
		if(debug){
        	System.out.println("old Alpha:");
        	System.out.println(_oldAlpha.toString());
        }
	}
	private void computeAlpha_CFDiferCase(){
		DoubleMatrix2D Eta_minus_C = this._Eta.minus(this._C);
		DoubleMatrix2D maxZero = Eta_minus_C.max(0);
		DoubleMatrix2D columnSum = maxZero.sumEachColumn();
		//
		//DoubleMatrix2D
		DoubleMatrix2D Gama_minus_Y = this._Gama.minus(this._Y);
		//the value at ij should not be added
		DoubleMatrix2D sameColumnVector = Gama_minus_Y.plus(columnSum);
		double [] sameColumnArray = new double [this._M];
		for(int j=0; j<this._M; j++){
			sameColumnArray[j] = sameColumnVector.get(0, j);
		}
		DoubleMatrix2D transposedTarget = new DoubleMatrix2D(this._N, sameColumnArray);
		DoubleMatrix2D target = transposedTarget.transpose();
		DoubleMatrix2D rightMatrix = target.minus(maxZero);
		this._Alpha = rightMatrix.min(0);		
	}
	private void computeAlpha_CFSameCase(){
		DoubleMatrix2D Eta_minus_C = this._Eta.minus(this._C);
		DoubleMatrix2D maxZero = Eta_minus_C.max(0);		
		for(int j=0; j<this._M; j++){
			maxZero.set(j, j, 0.0);
		}
		DoubleMatrix2D maxZeroNoJJ = maxZero.sumEachColumn();
		
		//for i==j
		////cost function should be specified again
		DoubleMatrix2D IisJ = maxZeroNoJJ.minus(this._Y).plus(this._Gama);
		
		//for i!=j		
		double [] row = new double [this._M];
		DoubleMatrix1D etaJJ = this._Eta.diag();
		DoubleMatrix1D cJJ = this._C.diag();
		////cost function should be specified again
		for(int j=0; j<this._M; j++){
			row[j] = maxZeroNoJJ.get(0, j)+etaJJ.get(j)-cJJ.get(j)-this._Y.get(0, j)+this._Gama.get(0, j);
		}		
		DoubleMatrix2D reversedM = new DoubleMatrix2D(this._N, row);
		DoubleMatrix2D rightM = reversedM.transpose();
		//realize no plus \alpha_ij, i.e., element-wise minus \alpha_ij
		DoubleMatrix2D rep = rightM.minus(maxZero);
		this._Alpha = rep.min(0);
		//
		for(int i=0; i<this._N; i++){
			this._Alpha.set(i, i, IisJ.get(0, i));
		}				
	}
	//
	private void updateAlpha(){
		this._Alpha = this._Alpha.mul(1-getLambda()).plus(this._oldAlpha.mul(getLambda()));
	}
	//
	private void computeAlpha_CFSameCase_mplp(){
		DoubleMatrix2D Eta_minus_C = this._Eta.minus(this._C);
		DoubleMatrix2D Eta_minus_C_2 = this._Eta.minus(this._C);
		
		DoubleMatrix2D maxZero = Eta_minus_C.max(0);		
		for(int j=0; j<this._M; j++){
			maxZero.set(j, j, 0.0);
		}
		DoubleMatrix2D maxZeroNoJJ = maxZero.sumEachColumn();
		
		//for i==j
		////cost function should be specified again
		DoubleMatrix2D IisJ = maxZeroNoJJ.minus(this._Y).plus(this._Gama);
		
		//for i!=j		
		double [] row = new double [this._M];
		DoubleMatrix1D etaJJ = this._Eta.diag();
		DoubleMatrix1D cJJ = this._C.diag();
		////cost function should be specified again
		for(int j=0; j<this._M; j++){
			row[j] = maxZeroNoJJ.get(0, j)+etaJJ.get(j)-cJJ.get(j)-this._Y.get(0, j)+this._Gama.get(0, j);
		}		
		DoubleMatrix2D reversedM = new DoubleMatrix2D(this._N, row);
		DoubleMatrix2D rightM = reversedM.transpose();
		//realize no plus \alpha_ij, i.e., element-wise minus \alpha_ij
		DoubleMatrix2D rep = rightM.minus(maxZero);
		this._Alpha = rep.min(0);
		//
		for(int i=0; i<this._N; i++){
			this._Alpha.set(i, i, IisJ.get(0, i));
		}	
		
		//mplp
		this._Alpha.mulTo(1.0/(this._M+1));
		Eta_minus_C_2.mulTo(1.0*this._M/(this._M+1));
		/*
		for(int i=0; i<this._N; i++){
			for(int j=0; j<this._M; j++){
				this._Alpha.set(i,j, this._Alpha.get(i, j)-Eta_minus_C_2.get(i, j));
			}
		}
		*/
		this._Alpha = this._Alpha.minus(Eta_minus_C_2);
	}
	
	////  (a,b) update////
	/*
	private void copyAB(){
		this._oldA = this._A.copy();
		this._oldB = this._B.copy();
	}
	*/
	private void updateAB(){
		this._A = new DoubleMatrix2D(this._M+1, this._M+1, Double.NEGATIVE_INFINITY);
	    this._B = new DoubleMatrix2D(this._M+1, this._M+1, Double.NEGATIVE_INFINITY);
	    //a-update
	    //a0(z0)=0
	    //(1)
	    /*
	    for(int z0=0; z0<=this._M; z0++){
	    	this._A.set(z0, 0, 0);
	    }
	    */
	    //(2)
	    this._A.set(0, 0, 0);
	    
	    for(int j=1; j<=this._M; j++){
	    	//M+1 possible states for z_j
	    	for(int zj=0; zj<=this._M; zj++){
	    		if(0 == zj){
	    			this._A.set(zj, j, this._A.get(zj, j-1));
	    		}else{
	    			this._A.set(zj, j, Math.max(this._A.get(zj, j-1), this._A.get(zj-1, j-1)+this._V.get(0, j-1)-this._Y.get(0, j-1)));
	    		}	    		
	    	}	    	
	    }
	    //b-update
	    //bM(zM)=G_M1_zM
	    //(1)
	    /*
	    for(int zM=0; zM<=this._M; zM++){
	    	this._B.set(zM, this._M, this.G_M1_zM);
	    }
	    */
	    //bM(zM)=G_M1_zM, i.e., only the K state is meaningful, which is equal to the predefined K, others are -infinity
	    this._B.set(this.G_M1_zM, this._M, this.G_M1_zM);
	    //
	    for(int j=this._M; j>=1; j--){
	    	for(int zj_minus_1=0; zj_minus_1<=this._M; zj_minus_1++){
	    		//
	    		if(this._M == zj_minus_1){
	    			this._B.set(zj_minus_1, j-1, this._B.get(zj_minus_1, j));
	    		}else{
	    			this._B.set(zj_minus_1, j-1, Math.max(this._B.get(zj_minus_1, j),
		    				this._B.get(zj_minus_1+1, j)+this._V.get(0, j-1)-this._Y.get(0, j-1)));
	    		}	    		
	    	}	    	
	    }	
	    //
	    if(debug){
        	System.out.println("AB update:");
        	System.out.println(_A.toString());
        	System.out.println(_B.toString());
        }
	}
	//
	/*
	private void updateAB(){
		this._A = this._A.mul(1-getLambda()).plus(this._oldA.mul(getLambda()));
		this._B = this._B.mul(1-getLambda()).plus(this._oldB.mul(getLambda()));
	}
	*/
	
	//
	private void updateAB_mplp(){
		this._A = new DoubleMatrix2D(this._M+1, this._M+1, Double.NEGATIVE_INFINITY);
	    this._B = new DoubleMatrix2D(this._M+1, this._M+1, Double.NEGATIVE_INFINITY);
	    //a-update
	    //a0(z0)=0
	    this._A.set(0, 0, 0);
	    
	    for(int j=1; j<=this._M; j++){
	    	//M+1 possible states for z_j
	    	for(int zj=0; zj<=this._M; zj++){
	    		if(0 == zj){
	    			this._A.set(zj, j, this._A.get(zj, j-1));
	    		}else{
	    			this._A.set(zj, j, Math.max(this._A.get(zj, j-1), this._A.get(zj-1, j-1)+this._V.get(0, j-1)-this._Y.get(0, j-1)));
	    		}	    		
	    	}	    	
	    }
	    //b-update
	    //bM(zM)=G_M1_zM
	    //bM(zM)=G_M1_zM, i.e., only the K state is meaningful, which is equal to the predefined K, others are -infinity
	    this._B.set(this.G_M1_zM, this._M, this.G_M1_zM);
	    //
	    for(int j=this._M; j>=1; j--){
	    	for(int zj_minus_1=0; zj_minus_1<=this._M; zj_minus_1++){
	    		//
	    		if(this._M == zj_minus_1){
	    			this._B.set(zj_minus_1, j-1, this._B.get(zj_minus_1, j));
	    		}else{
	    			this._B.set(zj_minus_1, j-1, Math.max(this._B.get(zj_minus_1, j),
		    				this._B.get(zj_minus_1+1, j)+this._V.get(0, j-1)-this._Y.get(0, j-1)));
	    		}	    		
	    	}	    	
	    }
	    
	    //mplp update
	    for(int j=1; j<=this._M; j++){
	    	if(1==j){
	    		for(int zj=0; zj<=this._M; zj++){
	    			this._A.set(zj, j, this._A.get(zj, j)*0.5-0.5*this._B.get(zj, j));
	    		}
	    	}else{
	    		for(int zj=0; zj<=this._M; zj++){
	    			this._A.set(zj, j, this._A.get(zj, j)*(1.0/3.0)-(2.0/3.0)*this._B.get(zj, j));
	    		}
	    	}
	    }
	    for(int j=0; j<=this._M-1; j++){
	    	if(0==j){
	    		for(int zj=0; zj<=this._M; zj++){
	    			this._B.set(zj, j, this._B.get(zj, j)*0.5-0.5*this._A.get(zj, j));
	    		}	    		
	    	}else{
	    		for(int zj=0; zj<=this._M; zj++){
	    			this._B.set(zj, j, this._B.get(zj, j)*(1.0/3.0)-(2.0/3.0)*this._A.get(zj, j));
	    		}
	    	}
	    }
	    
	    //
	    if(debug){
        	System.out.println("AB update:");
        	System.out.println(_A.toString());
        	System.out.println(_B.toString());
        }
	}
	
	//// Gama ////
	private void copyGama(){
		this._oldGama = this._Gama.copy();
		if(debug){
        	System.out.println("old Gama:");
        	System.out.println(_oldGama.toString());        	
        }
	}
	
	public ArrayList<String> getSelectedFacilities(){
    	ArrayList<String> facilityList = new ArrayList<String>();
    	for(int fID: this.IY.getVector()){
    		facilityList.add(getFacilityName(fID));    		
    	}
    	System.out.println(facilityList);
    	//
    	ArrayList<Integer> rList = new ArrayList<Integer>();
    	rList.addAll(IY.getVector());
    	Collections.sort(rList);
    	System.out.println("Selected facility number:\t"+rList.size());
    	System.out.println(rList);
    	//
    	return facilityList;
    }
	private static int smallerCnt = 0;
	public ArrayList<String> getSelectedFacilities(ExemplarType exemplarType, int cutoff){
    	//check
    	if(this.IY.size() < cutoff){
    		smallerCnt++;
    		System.err.println(_topicID+": (count-"+(smallerCnt)+")"+" : [Y]-selected count:"+this.IY.size()+"\tsmaller than\t"+cutoff);        		
    	}
    	
    	ArrayList<String> reList = new ArrayList<String>();
    	
    	if(exemplarType == ExemplarType.Y){
    		for(int i=0; i<cutoff; i++){
    			reList.add(getFacilityName(_allY.get(i).second));
    		}    		   		
        	return reList;
    	}else{
    		for(int i=0; i<cutoff; i++){
    			reList.add(getFacilityName(_allX.get(i).first));
    		}    		   		
        	return reList;
    	}
    }
	
	//
	private void computeGama(){
		for(int paperJ=1; paperJ<=this._M; paperJ++){
			//1st factor
			double maxMinuend = Double.NEGATIVE_INFINITY;
			//2nd factor
			double  maxSubtrahend = Double.NEGATIVE_INFINITY; 
			for(int z=0; z<=this._M; z++){
				if(z > 0){
					double minuendSum = this._A.get(z-1, paperJ-1)+this._B.get(z, paperJ);
					if(minuendSum > maxMinuend){
						maxMinuend = minuendSum;
					}
				}
				//
				double subtrahendSum = this._A.get(z, paperJ-1)+this._B.get(z, paperJ);
				if(subtrahendSum > maxSubtrahend){
					maxSubtrahend = subtrahendSum;
				}				
			}
			//
			this._Gama.set(0, paperJ-1, maxMinuend-maxSubtrahend);
		}
	}
	//
	private void updateGama(){
		this._Gama = this._Gama.mul(1-getLambda()).plus(this._oldGama.mul(getLambda()));
	}
		
	//
	private void computeGama_mplp(){
		for(int paperJ=1; paperJ<=this._M; paperJ++){
			//1st factor
			double maxMinuend = Double.NEGATIVE_INFINITY;
			//2nd factor
			double  maxSubtrahend = Double.NEGATIVE_INFINITY; 
			for(int z=0; z<=this._M; z++){
				if(z > 0){
					double minuendSum = this._A.get(z-1, paperJ-1)+this._B.get(z, paperJ);
					if(minuendSum > maxMinuend){
						maxMinuend = minuendSum;
					}
				}
				//
				double subtrahendSum = this._A.get(z, paperJ-1)+this._B.get(z, paperJ);
				if(subtrahendSum > maxSubtrahend){
					maxSubtrahend = subtrahendSum;
				}				
			}
			//
			this._Gama.set(0, paperJ-1, maxMinuend-maxSubtrahend);
		}
		
		//mplp
		DoubleMatrix2D VMinusY = this._V.minus(this._Y);
		this._Gama.set(0, 0, this._Gama.get(0, 0)*0.5-0.5*VMinusY.get(0, 0));

		for(int paperJ=2; paperJ<=this._M; paperJ++){
			this._Gama.set(0, paperJ-1, this._Gama.get(0, paperJ-1)*(1.0/3.0)-(2.0/3.0)*VMinusY.get(0, paperJ-1));
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
        
        if(false){
        	System.out.println("Iterating ... >0 exemplars[X]:");
        	System.out.println(IX.toString());
        }
        //
        DoubleMatrix1D EY;
        EY = this._V.minus(this._Y).plus(this._Gama).getRow(0);
        IY = EY.findG(0);
        if(debug){
        	System.out.println("Iterating ... >0 Facilities[Y]:");
        	System.out.println(IY.toString());
        }
        
        IntegerMatrix1D equalIY = EY.findG_WithEqual(0);
        if(debug){
        	System.out.println("Iterating ... >= 0 Facilities[Y]:");
        	System.out.println(equalIY.toString());
        }
    }
	//
	public void computeBeliefs_old(){
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
        	System.out.println("Final ... >0 Selected Exemplars:");
        	for(Integer cID: IX.getVector()){
        		System.out.print(cID+"("+getCustomerName(cID)+")"+"\t");
        	}        	
        	System.out.println();
        }
        //
        DoubleMatrix2D EY;
        EY = this._V.minus(this._Y).plus(this._Gama);
        IY = EY.getRow(0).findG(0);
        if(debug){
        	System.out.println("Final ... >0  Facilities:");
        	for(int fID: IY.getVector()){
        		System.out.print(fID+"("+getFacilityName(fID)+")"+"\t");
        	}
        	System.out.println();
        }
	}
	
	public void computeBeliefs(){
		/////Way-1: X Matrix
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
		
		for(int j=0; j<this._M; j++){
			//entire [X] list without zero restriction
			_allX.add(new IntIntDouble(j, j, AlphaEtaC.get(j, j)));
		}
		Collections.sort(_allX, new TripleComparatorByThird_Desc<Integer, Integer, Double>());
		
        if(debug){
        	System.out.println("Final ... >0 Selected Exemplars:");
        	for(Integer cID: IX.getVector()){
        		System.out.print(cID+"("+getCustomerName(cID)+")"+"\t");
        	}        	
        	System.out.println();
        }
        
        
        
        ////
        DoubleMatrix2D EY;
        EY = this._V.minus(this._Y).plus(this._Gama);
        IY = EY.getRow(0).findG(0);        
        
        //entire [Y] list        
        for(int j=0; j<this._M; j++){
        	_allY.add(new DoubleInt(EY.get(0, j), j));
        }
        Collections.sort(_allY, new PairComparatorByFirst_Desc<Double, Integer>());
        
        if(debug){
        	System.out.println("Final ... >0  Facilities:");
        	for(int fID: IY.getVector()){
        		System.out.print(fID+"("+getFacilityName(fID)+")"+"\t");
        	}
        	System.out.println();
        }
	}
	
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
    //
    protected void calculateCovergence() {
        if (this._noChangeIterSpan != null) {
            Vector<Integer> c = IX.getVector();
            for (int j = 0; j < this._M; j++) {
                Integer ex = Integer.valueOf(j);
                //after each iteration, examine whether each node is an exemplar,
                //then check the sequential true or false value of each node to determine convergence!
                if (c.contains(ex)) {
                	this.convitsVectors.get(ex).addCovits(true);
                } else {
                	this.convitsVectors.get(ex).addCovits(false);
                }
            }
        }
    }
    /**
     * @return true: notConverged / false:converged, essentially, whether there is no change given a predefined span of iteration
     * **/
    protected boolean checkConvergence() {
    	//no cluster generated!
        if (getClustersNumber() == 0) {
            return true;
        }
        //
        if (this._noChangeIterSpan == null) {
            return true;
        } else {
            for (ConvitsVector vec : convitsVectors.values()) {
                if (vec.checkConvits() == false) {
                    return true;
                }
            }
        }
        return false;
    }
	
	
	//
	public double getLambda(){
		return this._lambda;
	}
	//
	public int getClustersNumber(){
		return this.clustersNumber;
	}
	//
	public int getIterationTimes(){
		return this._iterationTimes;
	}
	//
	public static ArrayList<InteractionData> getCostMatrix(ArrayList<InteractionData> interList){
		ArrayList<InteractionData> costMatrix = new ArrayList<InteractionData>();
		for(InteractionData itr: interList){
			costMatrix.add(new InteractionData(itr.getFrom(), itr.getTo(), -itr.getSim()));
		}
		return costMatrix;		
	}
	//
	public static void testAPExample(){
		//similarity values
		ArrayList<InteractionData> costMatrix = getCostMatrix(APClustering.loadAPExample());
		//ArrayList<InteractionData> costMatrix = APClustering.loadAPExample();
    	//
    	double lambda = 0.5;
    	int iterationTimes = 500;
    	int noChangeIterSpan = 20;    	
    	//double preferences = getMedian(vList);
    	//positive value as a cost value
    	double costPreferences = 15.561256;
    	int preK = 7;
    	ArrayList<StrDouble> fList = new ArrayList<StrDouble>();
    	for(int j=0; j<25; j++){
    		fList.add(new StrDouble(Integer.toString(j), 0.0));
    	}
    	////
    	ImpSRD kUFL = new ImpSRD("", lambda, iterationTimes, noChangeIterSpan, costPreferences,
    			preK, UFLMode.C_Same_F, costMatrix, fList);
    	//    	
    	kUFL.run();    	
    	//
    	kUFL.getSelectedFacilities();
    }
	//
	public static void testAPExample_Topic(boolean commonIndri, DivVersion divVersion){ 
    	String qNumber = "wt09-2";
    	
    	//Map<String,TRECDivQuery> trecDivQueries = TRECDivLoader.loadTrecDivQueries(DivVersion.Div2009);	
    	HashMap<String,String> trecDivDocs = TRECDivLoader.loadTrecDivDocs(commonIndri, divVersion);
    	Map<String,TRECQueryAspects> trecDivQueryAspects = TRECDivLoader.loadTrecDivQueryAspects(commonIndri, divVersion);
    	    	
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
		
    	ArrayList<StrDouble> fList = new ArrayList<StrDouble>();
		for(int j=0; j<topNDocNames.length; j++){
			fList.add(new StrDouble(topNDocNames[j], 0.0));
    	}
		
		ArrayList<InteractionData> costMatrix = getCostMatrix(releMatrix);
		
    	//run
    	double lambda = 0.5;
    	int iterations = 5000;
    	int convits = 10;
    	double preferences = 0-APClustering.getMedian(vList);
    	int preK = 15;
    	ImpSRD kUFL = new ImpSRD(qNumber, lambda, iterations, convits, preferences, preK, UFLMode.C_Same_F, costMatrix, fList);
    	//    	
    	kUFL.run();    	
    	//
    	kUFL.getSelectedFacilities();
    }
	
	//
	public static void main(String []args){
		//1
		//K_UFL.testAPExample();
		
		//2
		boolean commonIndri = false;
		DivVersion divVersion = DivVersion.Div2009;
		ImpSRD.testAPExample_Topic(commonIndri, divVersion);
		
	}
}
