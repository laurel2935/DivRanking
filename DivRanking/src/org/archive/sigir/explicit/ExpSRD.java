package org.archive.sigir.explicit;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.Vector;

import org.archive.ml.clustering.ap.abs.ConvitsVector2D;
import org.archive.ml.clustering.ap.affinitymain.InteractionData;
import org.archive.ml.clustering.ap.matrix.DoubleMatrix1D;
import org.archive.ml.clustering.ap.matrix.DoubleMatrix2D;
import org.archive.ml.clustering.ap.matrix.IntegerMatrix1D;
import org.archive.ml.clustering.ap.matrix.IntegerMatrix2D;
import org.archive.ml.ufl.DCKUFL.ExemplarType;
import org.archive.ml.ufl.Mat;
import org.archive.util.tuple.BooleanInt;
import org.archive.util.tuple.DoubleInt;
import org.archive.util.tuple.IntInt;
import org.archive.util.tuple.IntIntDouble;
import org.archive.util.tuple.PairComparatorByFirst_Desc;
import org.archive.util.tuple.StrDouble;
import org.archive.util.tuple.TripleComparatorByThird_Desc;

/**
 * Explicit SRD (based on DCKUFL)
 * **/

public class ExpSRD {
	
	private static final boolean debug = false;
	
	////w.r.t. alternative setting
	//tiny impact
	private static final boolean withUtility = true;
	private static final boolean _mplp = true;
	
	//public static enum ExemplarType {X, Y}
		
	//// Basic Parameters with default values ////	
	private double _lambda = 0.5;
	private int _iterationTimes = 5000;
	//thus, the size of iteration-span that without change of exemplar
    protected Integer _noChangeIterSpan = null;    
    
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
         
	
	// the corresponding index in the paper i,j  will be essentially i-1 j-1 in the program!	
    //predefined k, i.e., G_{M+1}_zM
    Integer G_M1_zM;    
    //relevance matrix among subtopic and documents, positive values
    //private boolean _logDomain;
    //private ArrayList<InteractionData> _releMatrix;
    
	
	//number of subtopics
	private int _N;
	//number of documents
	private int _M;
	//relevance matrix r_ij: the relevance of a subtopic i to document j
	private DoubleMatrix2D _R;
	//similarity matrix s_ij: the similarity of subtopic i to subtopic j
	private DoubleMatrix2D _S;
	//1*N one-row popularity vector: the popularity of each subtopic
	private DoubleMatrix1D _P;
	//1*N one-row capacity vector corresponding to each subtopic
	private DoubleMatrix1D _capList;
	//1*M one-row utility vector corresponding to each document
	//the potential usefulness of selecting a document instead of cost
	private DoubleMatrix1D _L;
	
	//if document j is selected for subtopic i, the corresponding score:
	//p_i*r_ij + \sum_{k<>i}{(1 - s_ki) * (p_i*r_ij - p_k*r_kj)}, i.e., W_ij = JforI_ScoreMatrix(i,j)
	private DoubleMatrix2D _JforI_ScoreMatrix;
			
	//N*M
	private DoubleMatrix2D _Eta;
	private DoubleMatrix2D _oldEta;
	
	//1*M
	private DoubleMatrix2D _V;
	private DoubleMatrix2D _oldV;
	
	//N*M
	private DoubleMatrix2D _Alpha;
	private DoubleMatrix2D _oldAlpha;
	
	//(M+1)*M the row corresponds to the state of z_j, the column is the j-th column
	private DoubleMatrix2D _A;
	//private DoubleMatrix2D _oldA;
	private DoubleMatrix2D _B;
	//private DoubleMatrix2D _oldB;
	
	//one-row object
	private DoubleMatrix2D _Gama;
	private DoubleMatrix2D _oldGama;
	
	//N*(M+1)
	private DoubleMatrix2D _H;
	private DoubleMatrix2D _oldH;
	
	
	//one ConvitsVector2D for each potential exemplar
    protected Map<Integer, ConvitsVector2D> _convitsVectorMap = new HashMap<Integer, ConvitsVector2D>();
	//2-row integer matrix, which is used for check convergence�� j-th facility, i-th customer
	//1st-row: column-id
	//2nd-row: target-row-id
    private IntegerMatrix2D _JfForIcMatrix = null;    
    private IntegerMatrix1D _fY = null;	    
    
    //entire [X] list
  	ArrayList<IntIntDouble> _allX = new ArrayList<IntIntDouble>();  	
    //entire [Y] list
    ArrayList<DoubleInt> _allY = new ArrayList<DoubleInt>();
    
    //
    String _topicID;
        
    public ExpSRD(double lambda, int iterationTimes, Integer noChangeIterSpan, Integer preK, int n, int m,
    		DoubleMatrix2D releMatrix, DoubleMatrix2D simMatrix, DoubleMatrix1D p, DoubleMatrix1D capList, DoubleMatrix1D utilityList){
    	//
    	this._lambda = lambda;
    	this._iterationTimes = iterationTimes;
    	this._noChangeIterSpan = noChangeIterSpan;   
    	this.G_M1_zM = preK;    	
    	//
    	this._N = n;
    	this._M = m;
    	this._R = releMatrix;
    	this._capList = capList;
    	this._P = p;
    	this._S = simMatrix;
    	this._L = utilityList;
    	//
    	_JforI_ScoreMatrix = new DoubleMatrix2D(_N, _M, 0);
    	this.getJforI_ScoreMatrix();
    	//
    	this._Eta = new DoubleMatrix2D(this._N, this._M, 0);
        this._Alpha = new DoubleMatrix2D(this._N, this._M, 0);
        this._V = new DoubleMatrix2D(1, this._M, 0);
        this._Gama = new DoubleMatrix2D(1, this._M, 0);
        this._H = new DoubleMatrix2D(this._N,this._M+1, 0);
        
        if(debug){
        	System.out.println("Popularity for subtopics:");
        	System.out.println(_P.toString());  
        	
        	System.out.println("Rele matrix between subtopic <-> documents:");
        	System.out.println(_R.toString());  
        	
        	System.out.println("Sim matrix between subtopics:");
        	System.out.println(_S.toString());  
        	
        	System.out.println("Capacity for matrix:");
        	System.out.println(_capList.toString());  
        }
    }
	
    //pay attention to the positive or negative value of dataPointInteractions &&��fCostList
    public ExpSRD(String topicID, double lambda, int iterationTimes, Integer noChangeIterSpan, Integer preK,
    		ArrayList<InteractionData> releMatrix, ArrayList<InteractionData> subSimMatrix,
    		ArrayList<Double> capList, ArrayList<Double> popList, ArrayList<StrDouble> utilityList){
    	//basic parameters
    	this._lambda = lambda;
    	this._iterationTimes = iterationTimes;
    	this._noChangeIterSpan = noChangeIterSpan;
    	this.G_M1_zM = preK; 
    	//
    	this._cNodeNames = new HashSet<String>();
    	this._fNodeNames = new HashSet<String>();
    	for(InteractionData intData : releMatrix){
        	this._cNodeNames.add(intData.getFrom());
        	this._fNodeNames.add(intData.getTo());
        }
    	this._N = this._cNodeNames.size();
		this._M = this._fNodeNames.size();   
        //rele matrix x_ij
        this._R = new DoubleMatrix2D(this._N, this._M, 0);        
        for (InteractionData intData : releMatrix) {
            //System.out.println(intData.getFrom() + " " + intData.getTo() + " " + intData.getSim());          
            double r_ij = intData.getSim();            
            setRelevance(intData.getFrom(), intData.getTo(), r_ij);
        }   
        //
        this._S = new DoubleMatrix2D(_N, _N, 0.0);
        for (InteractionData intData : subSimMatrix) {
            //System.out.println(intData.getFrom() + " " + intData.getTo() + " " + intData.getSim()); 
            setSubSimilarity(intData.getFrom(), intData.getTo(), intData.getSim());
        } 
        for(int i=0; i<this._N; i++){
        	this._S.set(i, i, 1.0);
        }
        //
        this._capList = new DoubleMatrix1D(capList.toArray(new Double[0]));
        this._P = new DoubleMatrix1D(popList.toArray(new Double[0]));
        
        ////
        Double [] utilityArray = new Double[utilityList.size()];
        for(StrDouble e: utilityList){
        	utilityArray[getFacilityID(e.first)] = e.getSecond();
        }
        this._L = new DoubleMatrix1D(utilityArray);       
        
        //
    	_JforI_ScoreMatrix = new DoubleMatrix2D(_N, _M, 0);
    	this.getJforI_ScoreMatrix();
    	//
    	this._Eta = new DoubleMatrix2D(this._N, this._M, 0);
        this._Alpha = new DoubleMatrix2D(this._N, this._M, 0);
        this._V = new DoubleMatrix2D(1, this._M, 0);
        this._Gama = new DoubleMatrix2D(1, this._M, 0);
        this._H = new DoubleMatrix2D(this._N,this._M+1, 0);
        
        //
        this._topicID = topicID;
        
        if(debug){
        	System.out.println("Rele matrix:");
        	System.out.println(_R.toString());        	
        }
    }
    
    public void run(){
		int itrTimes = getIterationTimes();
		initConvergence();
		
		for(int itr=1; itr<=itrTimes; itr++){
			if(_mplp){
				//this.copyEta();
				this.computeEta_mplp();
				//this.updateEta();
				
				//
				//this.copyAlpha();
				this.computeAlpha_mplp();
				//this.updateAlpha();
				
				//
				//this.copyV();
				this.computeV_mplp();				
				
				//			
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
				this.computeAlpha();
				this.updateAlpha();
				
				//
				this.copyV();
				this.computeV();				
				
				//			
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
		//
		computeBeliefs();
	}
    
    protected Integer getCustomerID(String cName) {
    	if (_customerIDMapper.containsKey(cName)) {
            return _customerIDMapper.get(cName);
        } else {
            Integer id = _customerID;
            _customerIDMapper.put(cName, id);
            _customerIDRevMapper.put(id, cName);
            _customerID++;
            return id;
        }       
    }
    protected String getCustomerName(Integer cID){
    	return this._customerIDRevMapper.get(cID);
    }
    public Integer getFacilityID(String fName){
    	if(_facilityIDMapper.containsKey(fName)){
    		return _facilityIDMapper.get(fName);
    	}else{
    		Integer id = _facilityID;
    		_facilityIDMapper.put(fName, id);
    		_facilityIDRevMapper.put(id, fName);
    		_facilityID++;
    		return id;
    	}    	
    }
    protected String getFacilityName(Integer fID){
    	return this._facilityIDRevMapper.get(fID);
    }
    //
    public void setRelevance(final String from, final String to, final Double rele) {
        Integer cID = getCustomerID(from);
        Integer fID = getFacilityID(to);
        _R.set(cID, fID, rele.doubleValue());
    }
    //
    public void setSubSimilarity(final String from, final String to, final Double sim) {
        Integer iCID = getCustomerID(from);
        Integer jCID = getCustomerID(to);
        _S.set(iCID, jCID, sim);
        _S.set(jCID, iCID, sim);      
    }
	//
	public IntegerMatrix1D getSelectedDocs(){
		return this._fY;
	}	
	//return a negative value
	//cikm function
	///*
	private double Fi(int ithSubtopic, int fNumber){
		if(0 == fNumber){
			return 0;
		}else if(fNumber > 0){
			double cap = this._capList.get(ithSubtopic);
			if(fNumber <= cap){
				return 0;
			}else{
				return -Math.exp(fNumber-cap);
			}			
		}else{
			new Exception("cNumber error!").printStackTrace();
			return Double.NEGATIVE_INFINITY;
		}		
	}
	//*/
	//seems worse
	/*
	private double Fi(int ithSubtopic, int fNumber){
		double cap = this._capList.get(ithSubtopic);
		return -Math.exp(fNumber-cap);
	}
	*/
	//for accelerating
	private void getJforI_ScoreMatrix(){
		for(int j=0; j<this._M; j++){
			for(int i=0; i<this._N; i++){
				_JforI_ScoreMatrix.set(i, j, noIforJcol(i,j));
			}
		}
	}	
	//
	private double noIforJcol(int iRow, int jCol){
		//all 1.0 vector with a size of this._S.getN()-1
		ArrayList<Double> oneUniList = Mat.getUniformList(1.0, this._S.getN()-1);
		//as a symmetric matrix of S 
		ArrayList<Double> sICoL = this._S.getColumn(iRow).getList();
		sICoL.remove(iRow);
		//i.e., 1-s_ki
		ArrayList<Double> minus_1 = Mat.minus(oneUniList, sICoL);
		//
		double PiRij = this._P.get(iRow)*this._R.get(iRow, jCol);
		//all PiRij vector with a size of this._R.getN()-1
		ArrayList<Double> pi_rij_UniList = Mat.getUniformList(PiRij, this._R.getN()-1);
		ArrayList<Double> rJCol = this._R.getColumn(jCol).getList();
		rJCol.remove(iRow);
		ArrayList<Double> pList = this._P.getList();
		pList.remove(iRow);
		//i.e., p_k*r_kj
		ArrayList<Double> pk_rkj_List = Mat.pointwiseMul(pList, rJCol);
		//i.e., p_i*r_ij - p_k*r_kj
		ArrayList<Double> minus_2 = Mat.minus(pi_rij_UniList, pk_rkj_List);
		//i.e., (1-s_ki)*[p_i*r_ij - p_k*r_kj]
		ArrayList<Double> mulList = Mat.pointwiseMul(minus_1, minus_2);
		//noIforJcol
		double noIforJcolSum = Mat.sum(mulList)+PiRij;
		return noIforJcolSum;
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
		DoubleMatrix2D AlphaR = this._Alpha.plus(this._R);
		for(int i=0; i<this._N; i++){
			Vector<Double> IRow = AlphaR.getRow(i).getVector();
			for(int j=0; j<this._M; j++){
				Vector<Double> irow = new Vector<Double>();
				irow.addAll(IRow);
				irow.remove(j);
				//
				ArrayList<DoubleInt> diList = Mat.getDoubleIntList(irow);
				Collections.sort(diList, new PairComparatorByFirst_Desc<Double, Integer>());
				ArrayList<Double> delS = Mat.cumsumDI(diList);
				//
				int m1=0;
				double maxV1 = Fi(i, 1);
				for(int m=1; m<=delS.size(); m++){
					double v1 = delS.get(m-1)+Fi(i, 1+m);
					if(v1 > maxV1){
						maxV1 = v1;
					}
				}
				//
				int m0=0;
				double maxV2 = Fi(i, 0);
				for(int m=1; m<=delS.size(); m++){
					double v2=delS.get(m-1)+Fi(i, m);
					if(v2 > maxV2){
						maxV2 = v2;
					}
				}
				//
				//this._Eta.set(i, j, Math.min(maxV1, maxV1-maxV2));
				this._Eta.set(i, j, maxV1-maxV2);
			}
		}			
	}
	//
	private void updateEta(){
		this._Eta = this._Eta.mul(1-getLambda()).plus(this._oldEta.mul(getLambda()));
	}
	//
	private void computeEta_mplp(){
		DoubleMatrix2D AlphaR = this._Alpha.plus(this._R);
		DoubleMatrix2D AlphaR_2 = this._Alpha.plus(this._R);
		
		for(int i=0; i<this._N; i++){
			Vector<Double> IRow = AlphaR.getRow(i).getVector();
			for(int j=0; j<this._M; j++){
				Vector<Double> irow = new Vector<Double>();
				irow.addAll(IRow);
				irow.remove(j);
				//
				ArrayList<DoubleInt> diList = Mat.getDoubleIntList(irow);
				Collections.sort(diList, new PairComparatorByFirst_Desc<Double, Integer>());
				ArrayList<Double> delS = Mat.cumsumDI(diList);
				//
				int m1=0;
				double maxV1 = Fi(i, 1);
				for(int m=1; m<=delS.size(); m++){
					double v1 = delS.get(m-1)+Fi(i, 1+m);
					if(v1 > maxV1){
						maxV1 = v1;
					}
				}
				//
				int m0=0;
				double maxV2 = Fi(i, 0);
				for(int m=1; m<=delS.size(); m++){
					double v2=delS.get(m-1)+Fi(i, m);
					if(v2 > maxV2){
						maxV2 = v2;
					}
				}
				//
				//this._Eta.set(i, j, Math.min(maxV1, maxV1-maxV2));
				this._Eta.set(i, j, maxV1-maxV2);
			}
		}		
		
		//mplp
		AlphaR_2.mulTo((this._M-1)/this._M);
		this._Eta.mulTo(1.0/this._M);
		this._Eta = this._Eta.minus(AlphaR_2);
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
	private void computeV(){
		DoubleMatrix2D EtaR = this._Eta.plus(this._R);
		//
		for(int j=0; j<this._M; j++){
			double maxVk = Double.NEGATIVE_INFINITY;
			for(int k=0; k<this._N; k++){
				//double vk = commonOperate(k, j)+this._Eta.get(k, j);
				double vk = _JforI_ScoreMatrix.get(k, j) + EtaR.get(k, j);
				if(vk > maxVk){
					maxVk = vk;
				}
			}
			this._V.set(0, j, maxVk);
		}	
	}
	// not used as V is only used for computing beliefs
	/*
	private void updateV(){
		this._V = this._V.mul(1-getLambda()).plus(this._oldV.mul(getLambda()));
	}
	*/
	
	private void computeV_mplp(){
		DoubleMatrix2D EtaR = this._Eta.plus(this._R);
		//
		for(int j=0; j<this._M; j++){
			double maxVk = Double.NEGATIVE_INFINITY;
			for(int k=0; k<this._N; k++){
				//double vk = commonOperate(k, j)+this._Eta.get(k, j);
				double vk = _JforI_ScoreMatrix.get(k, j) + EtaR.get(k, j);
				if(vk > maxVk){
					maxVk = vk;
				}
			}
			this._V.set(0, j, maxVk);
		}	
		
		//mplp
		this._V.mulTo(1.0/(this._N+1));
		DoubleMatrix2D gammaPlusL = this._Gama;
		if(withUtility){
			for(int j=0; j<this._M; j++){
				gammaPlusL.set(0, j, gammaPlusL.get(0, j)+this._L.get(j));
			}
		}
		gammaPlusL.mulTo(1.0*this._N/(this._N+1));
		
		this._V = this._V.minus(gammaPlusL);		
	}
		
	//// Alpha ////
	private void copyAlpha(){
		this._oldAlpha = this._Alpha.copy();
		if(debug){
        	System.out.println("old Alpha:");
        	System.out.println(_oldAlpha.toString());
        }
	}
	private void computeAlpha(){
		for(int j=0; j<this._M; j++){
			double gama_j = this._Gama.get(0, j);
			////alternative
			//document relevance w.r.t. query
			if(withUtility){
				gama_j += this._L.get(j);
			}			
			
			DoubleMatrix2D EtaR = this._Eta.plus(this._R);
			//
			for(int i=0; i<this._N; i++){
				//double iRow_jColumn_sum = commonOperate(i,j);
				double noIforJcolSum = _JforI_ScoreMatrix.get(i, j);
				//
				double maxV2 = Double.NEGATIVE_INFINITY;
				for(int k=0; k<this._N; k++){
					if(k != i){
						//double v2 = commonOperate(k, j)+this._Eta.get(k, j);						
						double noKforJcolSum = _JforI_ScoreMatrix.get(k, j);
						double v2 = noKforJcolSum + EtaR.get(k, j);
						if(v2 > maxV2){
							maxV2 = v2;
						}
					}
				}				
				this._Alpha.set(i, j, Math.min(noIforJcolSum+gama_j, noIforJcolSum-maxV2));
			}
		}	
	}	
	//
	private void updateAlpha(){
		this._Alpha = this._Alpha.mul(1-getLambda()).plus(this._oldAlpha.mul(getLambda()));
	}
	//
	private void computeAlpha_mplp(){
		DoubleMatrix2D EtaR = this._Eta.plus(this._R);
		DoubleMatrix2D EtaR_2 = this._Eta.plus(this._R);
		
		for(int j=0; j<this._M; j++){
			double gama_j = this._Gama.get(0, j);
			////alternative
			//document relevance w.r.t. query
			if(withUtility){
				gama_j += this._L.get(j);
			}			
			
			//DoubleMatrix2D EtaR = this._Eta.plus(this._R);
			//
			for(int i=0; i<this._N; i++){
				//double iRow_jColumn_sum = commonOperate(i,j);
				double noIforJcolSum = _JforI_ScoreMatrix.get(i, j);
				//
				double maxV2 = Double.NEGATIVE_INFINITY;
				for(int k=0; k<this._N; k++){
					if(k != i){
						//double v2 = commonOperate(k, j)+this._Eta.get(k, j);						
						double noKforJcolSum = _JforI_ScoreMatrix.get(k, j);
						double v2 = noKforJcolSum + EtaR.get(k, j);
						if(v2 > maxV2){
							maxV2 = v2;
						}
					}
				}				
				this._Alpha.set(i, j, Math.min(noIforJcolSum+gama_j, noIforJcolSum-maxV2));
			}
		}	
		
		//mplp
		this._Alpha.mulTo(1.0/(this._N+1));
		EtaR_2.mulTo(1.0*this._N/(this._N+1));
		this._Alpha = this._Alpha.minus(EtaR_2);		
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
	    this._A.set(0, 0, 0);
	    for(int j=1; j<=this._M; j++){
	    	//M+1 possible states for z_j
	    	for(int zj=0; zj<=this._M; zj++){
	    		if(0 == zj){
	    			//since there is no status: {-1=zj-1} for z_j
	    			this._A.set(zj, j, this._A.get(zj, j-1));
	    		}else{
	    			if(withUtility){
		    			this._A.set(zj, j, Math.max(this._A.get(zj, j-1), this._A.get(zj-1, j-1)+this._V.get(0, j-1)+this._L.get(j-1)));
	    			}else{
		    			this._A.set(zj, j, Math.max(this._A.get(zj, j-1), this._A.get(zj-1, j-1)+this._V.get(0, j-1)));
	    			}
	    			
	    		}	    		
	    	}	    	
	    }
	    
	    //b-update	    
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
	    			//since there is no status: {M+1=zj_minus_1+1} for z_j
	    			this._B.set(zj_minus_1, j-1, this._B.get(zj_minus_1, j));
	    		}else{
	    			if(withUtility){
	    				this._B.set(zj_minus_1, j-1, Math.max(this._B.get(zj_minus_1, j),
			    				this._B.get(zj_minus_1+1, j)+this._V.get(0, j-1)+this._L.get(j-1)));
	    			}else{
	    				this._B.set(zj_minus_1, j-1, Math.max(this._B.get(zj_minus_1, j),
			    				this._B.get(zj_minus_1+1, j)+this._V.get(0, j-1)));
	    			}	    			
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
	    			//since there is no status: {-1=zj-1} for z_j
	    			this._A.set(zj, j, this._A.get(zj, j-1));
	    		}else{
	    			if(withUtility){
		    			this._A.set(zj, j, Math.max(this._A.get(zj, j-1), this._A.get(zj-1, j-1)+this._V.get(0, j-1)+this._L.get(j-1)));
	    			}else{
		    			this._A.set(zj, j, Math.max(this._A.get(zj, j-1), this._A.get(zj-1, j-1)+this._V.get(0, j-1)));
	    			}
	    			
	    		}	    		
	    	}	    	
	    }
	    
	    //b-update	    
	    //bM(zM)=G_M1_zM, i.e., only the K state is meaningful, which is equal to the predefined K, others are -infinity
	    this._B.set(this.G_M1_zM, this._M, this.G_M1_zM);
	    //
	    for(int j=this._M; j>=1; j--){
	    	for(int zj_minus_1=0; zj_minus_1<=this._M; zj_minus_1++){
	    		//
	    		if(this._M == zj_minus_1){
	    			//since there is no status: {M+1=zj_minus_1+1} for z_j
	    			this._B.set(zj_minus_1, j-1, this._B.get(zj_minus_1, j));
	    		}else{
	    			if(withUtility){
	    				this._B.set(zj_minus_1, j-1, Math.max(this._B.get(zj_minus_1, j),
			    				this._B.get(zj_minus_1+1, j)+this._V.get(0, j-1)+this._L.get(j-1)));
	    			}else{
	    				this._B.set(zj_minus_1, j-1, Math.max(this._B.get(zj_minus_1, j),
			    				this._B.get(zj_minus_1+1, j)+this._V.get(0, j-1)));
	    			}	    			
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
		DoubleMatrix2D VPlusY = this._V;
		if(withUtility){
			for(int j=0; j<this._M; j++){
				VPlusY.set(0, j, VPlusY.get(0, j)+this._L.get(j));
			}
		}
		this._Gama.set(0, 0, this._Gama.get(0, 0)*0.5-0.5*VPlusY.get(0, 0));

		for(int paperJ=2; paperJ<=this._M; paperJ++){
			this._Gama.set(0, paperJ-1, this._Gama.get(0, paperJ-1)*(1.0/3.0)-(2.0/3.0)*VPlusY.get(0, paperJ-1));
		}	
	}
	
	//// H ////
	private void copyH(){
		this._oldH = this._H.copy();
		if(debug){
			System.out.println("old H:");
        	System.out.println(_oldH.toString()); 
		}
	}
	//
	private void computeH(){
		DoubleMatrix2D AlphaR = this._Alpha.plus(this._R);
		for(int i=0; i<this._N; i++){
			Vector<Double> aIRow = AlphaR.getRow(i).getVector();
			ArrayList<DoubleInt> diList = Mat.getDoubleIntList(aIRow);
			Collections.sort(diList, new PairComparatorByFirst_Desc<Double, Integer>());
			ArrayList<Double> S = Mat.cumsumDI(diList);
			//
			this._H.set(i, 0, 0.0);			
			for(int ui=1; ui<=this._M; ui++){
				this._H.set(i, ui, S.get(ui-1));
			}
		}
	}
	
	protected void computeIteratingBeliefs() {
		
		DoubleMatrix2D AlphaEtaR = this._Alpha.plus(this._Eta).plus(this._R);
		
		ArrayList<Integer> colList = new ArrayList<Integer>();
        ArrayList<Integer> rowList = new ArrayList<Integer>();
        
        /*
        //old
		for(int j=0; j<this._M; j++){
			//boolean already = false;
			for(int i=0; i<this._N; i++){
				if(AlphaEtaR.get(i, j) >= 0){
					//if(already){
					//	new Exception("Multiple use error!").printStackTrace();
					//}
					colList.add(j);
					rowList.add(i);
					//already = true;
				}
			}
		}		
		*/
        for(int j=0; j<this._M; j++){        	
			int maxI = -1;
			double maxV =-100000;
			
			for(int i=0; i<this._N; i++){
				if(AlphaEtaR.get(i, j) > maxV){
					maxI = i;
					maxV = AlphaEtaR.get(i, j);
				}
			}
			//for selecting exemplars
			if(maxV >= 0){
				colList.add(j);
				rowList.add(maxI);
			}
		}
        
		this._JfForIcMatrix = new IntegerMatrix2D(2, colList.size(), 0);
		for(int k=0; k<colList.size(); k++){
			this._JfForIcMatrix.set(0, k, colList.get(k));
			this._JfForIcMatrix.set(1, k, rowList.get(k));
		}
		
        if(debug){
        	System.out.println("Iterating ... max Selected Exemplars[X]:");
        	printSDMatrix();
        }		
        //        
        DoubleMatrix1D EY = this._V.plus(this._Gama).getRow(0);
        this._fY = EY.findG(0);
        
        if(debug){
        	System.out.println("Iterating ... >0 Facilities[Y]:");
        	System.out.println(this._fY.toString());
        }
        
        IntegerMatrix1D equalIY = EY.findG_WithEqual(0);
        if(debug){
        	System.out.println("Iterating ... >= 0 Facilities[Y]:");
        	System.out.println(equalIY.toString());
        }
    }
	//
	//
	public void computeBeliefs(){  
		//////////// Way-1: X Matrix
		DoubleMatrix2D AlphaEtaR = this._Alpha.plus(this._Eta).plus(this._R);
		//exemplar list with ij and belief values
		ArrayList<IntIntDouble> eList = new ArrayList<IntIntDouble>();
		
		ArrayList<Integer> colList = new ArrayList<Integer>();
        ArrayList<Integer> rowList = new ArrayList<Integer>();
        /*
        //old
		for(int j=0; j<this._M; j++){
			boolean already = false;
			for(int i=0; i<this._N; i++){
				if(AlphaEtaR.get(i, j) >= 0){
					if(already){
						System.err.println("[X]-datapoint:\tMultiple use error!");						
					}
					colList.add(j);
					rowList.add(i);
					already = true;
					
					//column, row, double
					eList.add(new IntIntDouble(j, i, AlphaEtaR.get(i, j)));
				}
			}
		}	
		*/
        for(int j=0; j<this._M; j++){
        	
			int maxI = -1;
			double maxV =-100000;
			
			for(int i=0; i<this._N; i++){
				if(AlphaEtaR.get(i, j) > maxV){
					maxI = i;
					maxV = AlphaEtaR.get(i, j);
				}
			}
			//for selecting exemplars
			if(maxV >= 0){
				colList.add(j);
				rowList.add(maxI);								
				//column, row, double
				eList.add(new IntIntDouble(j, maxI, maxV));
			}	
			
			//entire [X] list without zero restriction
			_allX.add(new IntIntDouble(j, maxI, maxV));
		}
		
		Collections.sort(eList, new TripleComparatorByThird_Desc<Integer, Integer, Double>());
		
		Collections.sort(_allX, new TripleComparatorByThird_Desc<Integer, Integer, Double>());
		
		//for checking convergence
		this._JfForIcMatrix = new IntegerMatrix2D(2, colList.size(), 0);
		for(int k=0; k<colList.size(); k++){
			this._JfForIcMatrix.set(0, k, colList.get(k));
			this._JfForIcMatrix.set(1, k, rowList.get(k));
		}	
		
        if(debug){
        	System.out.println();
        	System.out.println("Final ... Max Selected Exemplars[X]-"+this._JfForIcMatrix.getM()+":");
        	printSDMatrix();        	
        	System.out.println();        	
        	//
        	System.out.println("Final ... Sorted Max Selected Exemplars[X]-"+eList.size()+":");
        	System.out.print("Col ID:\t\t");
        	for(IntIntDouble element: eList){
        		System.out.print(element.first+"\t");
        	}
        	System.out.println();
        	System.out.print("Row ID:\t\t");
        	for(IntIntDouble element: eList){
        		System.out.print(element.second+"\t");
        	}
        	System.out.println();
        	System.out.println();
        	//
        	System.out.println("Entire X variables:");
        	System.out.print("Col ID:\t\t");
        	for(IntIntDouble element: _allX){
        		System.out.print(element.first+"\t");
        	}
        	System.out.println();
        	System.out.print("Row ID:\t\t");
        	for(IntIntDouble element: _allX){
        		System.out.print(element.second+"\t");
        	}
        	System.out.println();
        	System.out.println();
        }
        
        /////////// Way-2: Y vector
        //
        //DoubleMatrix2D EY = this._V.plus(this._Gama);
        DoubleMatrix2D EY = this._V.plus(this._Gama);
        
        //entire [Y] list        
        for(int j=0; j<this._M; j++){
        	_allY.add(new DoubleInt(EY.get(0, j), j));
        }
        Collections.sort(_allY, new PairComparatorByFirst_Desc<Double, Integer>());
        
        //no sorting
        //this._fY = EY.getRow(0).findG(0);
        //sorted
        this._fY = EY.getRow(0).findG_Sorted(0);
        
        if(debug){
        	if(this._fY.size() < 20){
        		System.err.println("[Y]-selected count:\tsmaller than 20 !");        		
        	}
        	
        	System.out.println("Final ... >0  Facilities[Y]-"+this._fY.size()+":");
        	System.out.print("Col ID:\t\t");
        	for(int fID: this._fY.getVector()){
        		System.out.print(fID+"\t");
        		//System.out.print(fID+"("+getFacilityName(fID)+")"+"\t");
        	}
        	System.out.println();
        	System.out.println();
        	
        	System.out.println("Entire Y variables:");
        	System.out.print("Row ID:\t\t");
        	for(DoubleInt y: _allY){
        		System.out.print(y.second+"\t");
        	}
        	System.out.println();
        	System.out.println();        	
        }
        
        ////////////u_i      
        ArrayList<IntInt> uiList = new ArrayList<IntInt>();  
        
        for(int i=0; i<this._N; i++){
        	
        	int ui = 0; double maxV = this._H.get(i, 0)+Fi(i, 0);
        	//iterate over possible states
        	for(int state=1; state<=this._M; state++){
        		double v = Fi(i, state) + this._H.get(i, state);
        		if(v > maxV){
        			ui = state;
        			maxV = v;
        		}
        	}
        	//
        	uiList.add(new IntInt(i, ui));        	
        }
        
        if(debug){               	
        	System.out.println("Final Capacity Setting[u_i]:"); 
        	for(IntInt intInt: uiList){
        		System.out.println(intInt.toString());
        	} 
        	System.out.println();
        } 
	}
    //
    public void printSDMatrix() {
    	int exeNum = this._JfForIcMatrix.getM();
    	System.out.print("Col ID:\t\t");
        for(int k=0; k<exeNum; k++){
        	System.out.print(this._JfForIcMatrix.get(0, k)+"\t");
        }
        System.out.println();
        System.out.print("Row ID:\t\t");
        for(int k=0; k<exeNum; k++){
        	System.out.print(this._JfForIcMatrix.get(1, k)+"\t");
        }
        System.out.println();
    }
	
	/**
     * initialize the indicator of convergence vectors
     * **/
    protected void initConvergence() {
        //System.out.println("S: " + S.toString());
        if (this._noChangeIterSpan != null) {
            for (int j = 0; j < this._M; j++) {
            	ConvitsVector2D convitsVector = new ConvitsVector2D(this._noChangeIterSpan.intValue(), Integer.valueOf(j));
            	convitsVector.init();                
                this._convitsVectorMap.put(Integer.valueOf(j), convitsVector);
            }
        }
    }
    //
    protected void calculateCovergence() {
        if (this._noChangeIterSpan != null) {
            Vector<Integer>  colExemplars = this._JfForIcMatrix.getRow(0);
            //false cases:
            for (int j = 0; j < this._M; j++) {
                Integer ex = Integer.valueOf(j);
                //after each iteration, examine whether each node is an exemplar,
                //then check the sequential true or false value of each node to determine convergence!
                if (!colExemplars.contains(ex)) {                	
                	this._convitsVectorMap.get(ex).addCovits(new BooleanInt(false, -1));
                	//this._convitsVectorMap.get(ex).addCovits(new BooleanInt(true, second));
                }
            }
            //true cases:
            for(int k=0; k<this._JfForIcMatrix.getM(); k++){
            	this._convitsVectorMap.get(_JfForIcMatrix.get(0, k)).addCovits(new BooleanInt(true, _JfForIcMatrix.get(1, k)));
            }
        }
    }
    /**
     * @return true: notConverged / false:converged, essentially, whether there is no change given a predefined span of iteration
     * **/
    protected boolean checkConvergence() {        
        if (this._noChangeIterSpan == null) {
            return true;
        } else {
            for (ConvitsVector2D convitsVector : _convitsVectorMap.values()) {
                if (convitsVector.checkConvits() == false) {
                    return true;
                }
            }
        }
        return false;
    }	
	//
    /**
     * 
     * **/
    public ArrayList<String> getSelectedFacilities(){
    	ArrayList<String> facilityList = new ArrayList<String>();
    	for(int fID: this._fY.getVector()){
    		facilityList.add(getFacilityName(fID));    		
    	}
    	return facilityList;
    }
    
    /****/
    private static int smallerCnt = 0;
    public ArrayList<String> getSelectedFacilities(ExemplarType exemplarType, int cutoff){
    	//check
    	if(this._fY.size() < cutoff){
    		System.err.println(_topicID+": (count-"+(smallerCnt)+")"+" : [Y]-selected count:"+this._fY.size()+"\tsmaller than\t"+cutoff);        		
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
	public double getLambda(){
		return this._lambda;
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
		Double [] capArray = {6.0, 4.0, 5.0, 5.0}; 
		DoubleMatrix1D capList = new DoubleMatrix1D(capArray);
		
		Double [] utilityArray = new Double[50];
		DoubleMatrix1D uList = new DoubleMatrix1D(utilityArray);
		
		Double [] pArray = {0.4, 0.3, 0.2, 0.1};
		DoubleMatrix1D p = new DoubleMatrix1D(pArray);
		
		DoubleMatrix2D simMatrix = new DoubleMatrix2D(4, 4, 1.0);
		simMatrix.set(0, 1, 0.2);simMatrix.set(0, 2, 0.3);simMatrix.set(0, 3, 0.1);
		simMatrix.set(1, 0, 0.2);simMatrix.set(1, 2, 0.4);simMatrix.set(1, 3, 0.2);
		simMatrix.set(2, 0, 0.3);simMatrix.set(2, 1, 0.4);simMatrix.set(2, 3, 0.3);
		simMatrix.set(3, 0, 0.1);simMatrix.set(3, 1, 0.2);simMatrix.set(3, 2, 0.3);
		//nextInt(int n) ����[0,n)�����
		//��ֵ����[0,1.0)֮��
		Random random = new Random();
		DoubleMatrix2D releMatrix = new DoubleMatrix2D(4, 50, 0.0);
		for(int i=0; i<4; i++){
			for(int j=0; j<50; j++){
				releMatrix.set(i, j, random.nextDouble());
			}
		}
		//		
    	double lambda = 0.5;
    	int iterationTimes = 5000;
    	int noChangeIterSpan = 10; 
    	int preK = 20;    	
    	//double lambda, int iterationTimes, Integer noChangeIterSpan, Integer preK, int n, int m,
    	//DoubleMatrix2D releMatrix, DoubleMatrix2D simMatrix, DoubleMatrix1D p, DoubleMatrix1D capList
    	ExpSRD dckufl = new ExpSRD(lambda, iterationTimes, noChangeIterSpan, preK, 4, 50,
    			releMatrix, simMatrix, p, capList, uList);
    	//    	
    	dckufl.run();    	
    }
	
	//
	public static void main(String []args){
		/**
		 * !!!the convergence check is highly dependent whether C is the same as F
		 * **/
		ExpSRD.testAPExample();
	}
}
