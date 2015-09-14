package org.archive.ireval.toy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.archive.util.tuple.Pair;
import org.archive.util.tuple.PairComparatorBySecond_Desc;

public class DocSelect {
	//default number of subtopics
	int _subtopicNum=2;
	//default numver of documents
	int _docNum = 10; 
	
	//subtopic list
	float [] _subtopicList;
	//row: subtopic column: document
	float [][] _docList;	
	
	//target set S
	ArrayList<Integer> _S = new ArrayList<Integer>();
	
	//specific for IA-Select algorithm
	float [] Utility_q_S;
	
	
	//composing subtopic and document
	/**
	 * 					0.6				0.4
	 * Document		p(*|q,t_{1})	p(*|q,t_{2})

		d_{1}			 0.4			0.0
d_{2}(duplicate of d_{1} 0.4			0.0
		d_{3}			 0.2			0.0
d_{4},d_{5},d_{6},d_{7}  0.15			0.0
d_{8},d_{9},d_{10}		 0.0			0.2

	 * **/
	private void ini(){
		this._subtopicNum = 2;
		this._docNum = 10;
		
		_subtopicList = new float[2];
		//t_1
		this._subtopicList[0] = 0.6f;
		//t_2
		this._subtopicList[1] = 0.4f;
		
		//initial value of U
		Utility_q_S = new float[2];
		this.Utility_q_S[0] = _subtopicList[0];
		this.Utility_q_S[1] = _subtopicList[1];
		
		
		_docList = new float[2][10];
		
		//documents w.r.t. t_1
		//d_1
		_docList[0][0] = 0.4f;
		//d_2
		_docList[0][1] = 0.4f;
		//d_3
		_docList[0][2] = 0.2f;
		//d_4 - d_7
		_docList[0][3] = 0.15f;
		_docList[0][4] = 0.15f;
		_docList[0][5] = 0.15f;
		_docList[0][6] = 0.15f;
		//d_8 - d_10
		_docList[0][7] = 0.0f;
		_docList[0][8] = 0.0f;
		_docList[0][9] = 0.0f;
		
		//documents w.r.t. t_2
		//d_1
		_docList[1][0] = 0.0f;
		//d_2
		_docList[1][1] = 0.0f;
		//d_3
		_docList[1][2] = 0.0f;
		//d_4 - d_7
		_docList[1][3] = 0.0f;
		_docList[1][4] = 0.0f;
		_docList[1][5] = 0.0f;
		_docList[1][6] = 0.0f;
		//d_8 - d_10
		_docList[1][7] = 0.2f;
		_docList[1][8] = 0.2f;
		_docList[1][9] = 0.2f;		
	}
	/**
	 * greedy algorithm of IA-Select
	 * **/
	private void iaSelect(int k){
		//indicator vector of selected documents
		boolean [] isSelectedVec = new boolean[10];
		
		int step = 1;
		//select
		while(this._S.size()<k){
			int maxIndex = -1;
			float maxMU = 0.0f;
			
			for(int i=0; i<isSelectedVec.length; i++){
				
				if(!isSelectedVec[i]){					
					//marginal utility w.r.t. t_1
					float tempMU = Utility_q_S[0]*_docList[0][i];
					//marginal utility w.r.t. t_2
					tempMU += Utility_q_S[1]*_docList[1][i];
					
					//record
					if(tempMU > maxMU){
						maxMU = tempMU;
						maxIndex = i;
					}
				}
			}
			
			//
			System.out.println("Step-"+(step++));
			if(this._S.size()>0){
				System.out.print("\tSet S:{");
				for(int j=0; j<this._S.size()-1; j++){
					System.out.print("d_"+(this._S.get(j)+1)+" , ");
				}
				System.out.print("d_"+(this._S.get(this._S.size()-1)+1));
				System.out.print("}\t");
			}else{
				System.out.print("\tSet S:{}\t");
			}
			
			System.out.print("U(t_1|q,S):"+Utility_q_S[0]+"\t");
			System.out.print("U(t_2|q,S):"+Utility_q_S[1]);
			System.out.println();
			
			this._S.add(maxIndex);
			System.out.println("\tAdd d_"+(maxIndex+1)+": rel w.r.t. t_1: "+_docList[0][maxIndex]+", rel w.r.t. t_2: "+_docList[1][maxIndex]+"\n\thaving marginal utility of "+maxMU);
			isSelectedVec[maxIndex] = true;
			
			Utility_q_S[0] *= (1-_docList[0][maxIndex]);
			Utility_q_S[1] *= (1-_docList[1][maxIndex]);			
		}
		//output
		System.out.println();
		System.out.println("Final ranked list:");
		System.out.print("\tSet S:{");
		for(int j=0; j<this._S.size()-1; j++){
			System.out.print("d_"+(this._S.get(j)+1)+" , ");
		}
		System.out.print("d_"+(this._S.get(this._S.size()-1)+1));
		System.out.print("}\t");
	}
	/**
	 * PM-2
	 * **/
	private void PM2Select(int k){
		float lambda = 0.5f;
		//seats
		ArrayList<Float> seatVec = new ArrayList<Float>();
		//votes
		ArrayList<Float> voteVec = new ArrayList<Float>();
		//quotient
		ArrayList<Float> quotientVec = new ArrayList<Float>();
		for(int i=0; i<this._subtopicNum; i++){
			quotientVec.add(0.0f);
		}
		//indicator vector of selected documents
		boolean [] isSelectedVec = new boolean[this._docNum];
		
		//ini of setVec
		for(int i=0; i<this._subtopicNum; i++){
			seatVec.add(0.0f);
		}
		//ini of voteVec
		for(int i=0; i<this._subtopicNum; i++){
			voteVec.add(this._subtopicList[i]);
		}
		
		int step = 1;
		//one by one selection
		while(this._S.size()<k){
			
			System.out.println("Step-"+(step++));
			
			for(int i=0; i<this._subtopicNum; i++){
				System.out.print("\tvote["+(i+1)+"]: "+voteVec.get(i)+"\t");
				System.out.println("\tseat["+(i+1)+"]: "+seatVec.get(i));
			}
			System.out.println();
			
			//quotient re-calculation
			for(int i=0; i<this._subtopicNum; i++){
				quotientVec.set(i, voteVec.get(i)/(2*seatVec.get(i)+1));
				
				System.out.println("\tquotient["+(i+1)+"]: "+quotientVec.get(i));
			}
			
			//find i^{*}			
			int maxQuotientIndex = 0;
			float maxQuotient = quotientVec.get(0);
			//
			for(int i=1; i<this._subtopicNum; i++){
				if(quotientVec.get(i) > maxQuotient){
					maxQuotientIndex = i;
					maxQuotient = quotientVec.get(i);
				}
			}
			
			//find d^{*}
			float maxUtilityWithOneDoc = -1.0f;
			int maxDoc = -1;
			for(int docJ=0; docJ<this._docNum; docJ++){
				if(!isSelectedVec[docJ]){
					float utilityValue = lambda*quotientVec.get(maxQuotientIndex)*this._docList[maxQuotientIndex][docJ];
					
					for(int subK=0; subK<this._subtopicNum; subK++){
						if(subK != maxQuotientIndex){
							utilityValue += (1-lambda)*quotientVec.get(subK)*this._docList[subK][docJ];
						}
					}
					
					if(utilityValue > maxUtilityWithOneDoc){
						maxDoc = docJ;
						maxUtilityWithOneDoc = utilityValue;
					}
				}
			}
			
			//
			this._S.add(maxDoc);
			isSelectedVec[maxDoc] = true;
			
			System.out.println();
			System.out.println("\tmaxQuotientIndex: "+(maxQuotientIndex+1));
			System.out.println("\tmaxDoc: "+(maxDoc+1)+"\tValue: "+maxUtilityWithOneDoc);
			
			//alter seats
			float sumOfUtilityWithDoc = 0.0f;
			for(int i=0; i<this._subtopicNum; i++){
				sumOfUtilityWithDoc += this._docList[i][maxDoc];
			}
			for(int i=0; i<this._subtopicNum; i++){
				float newSeat = seatVec.get(i)+(this._docList[i][maxDoc]/sumOfUtilityWithDoc);
				seatVec.set(i, newSeat);
			}
		}
		
		//output
		System.out.println();
		System.out.println("Final ranked list:");
		System.out.print("\tSet S:{");
		for(int j=0; j<this._S.size()-1; j++){
			System.out.print("d_"+(this._S.get(j)+1)+" , ");
		}
		System.out.print("d_"+(this._S.get(this._S.size()-1)+1));
		System.out.print("}\t");
		
	}
	/**
	 * w.r.t. D#-nDCG
	 * **/
	private void getIdealList(){
		//
		this.ini();
		//
		ArrayList<Pair<Integer, Float>> ggList = new ArrayList<Pair<Integer,Float>>();
		for(int dNum=0; dNum<10; dNum++){
			float ggValue = _docList[0][dNum]*_subtopicList[0]+_docList[1][dNum]*_subtopicList[1];
			ggList.add(new Pair<Integer, Float>(dNum, ggValue));
		}
		//
		Collections.sort(ggList, 
				new PairComparatorBySecond_Desc<Integer, Float>());;
		//
		System.out.println("Ideal list by global gain value:");		
		for(Pair<Integer, Float> dPair: ggList){
			System.out.println("d_"+(dPair.getFirst()+1)+"\tGG:"+dPair.getSecond());			
		}
	}
	//
	public static void main(String []args){
		//1 iaSelect
		/*
		IASelect iaSelect = new IASelect();
		iaSelect.ini();
		iaSelect.iaSelect(5);
		*/
		
		//2 ideal list w.r.t. gg value
		/*
		IASelect iaSelect = new IASelect();
		iaSelect.getIdealList();
		
		//PM-2Select
		/*
		DocSelect docSelect = new DocSelect();
		docSelect.ini();
		docSelect.PM2Select(5);
		*/
		String s = System.getProperty("java.library.path");
		for(String t: s.split(";")){
			System.out.println(t);
		}
		
		//System.out.println(s);
		
		
	}

}
