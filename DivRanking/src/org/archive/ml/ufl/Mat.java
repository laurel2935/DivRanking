package org.archive.ml.ufl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Vector;

import org.archive.util.tuple.DoubleInt;
import org.archive.util.tuple.PairComparatorByFirst_Asc;
import org.archive.util.tuple.PairComparatorByFirst_Desc;

public class Mat {
	
	//double and its original index
	public static ArrayList<DoubleInt> getDoubleIntList(double [] dArray){
		ArrayList<DoubleInt> res = new ArrayList<DoubleInt>();
		for(int i=0; i<dArray.length; i++){
			res.add(new DoubleInt(dArray[i], i));
		}
		return res;
	}
	public static ArrayList<DoubleInt> getDoubleIntList(Vector<Double> dVector){
		ArrayList<DoubleInt> res = new ArrayList<DoubleInt>();
		for(int i=0; i<dVector.size(); i++){
			res.add(new DoubleInt(dVector.get(i), i));
		}
		return res;
	}
	//cumulative sum
	public static ArrayList<Double> cumsumDI(ArrayList<DoubleInt> diList){
		double sum = 0.0;
		ArrayList<Double> res = new ArrayList<Double>();
		for(DoubleInt di: diList){
			sum += di.getFirst();
			res.add(sum);
		}
		return res;
	}
	public static ArrayList<Double> cumsumD(ArrayList<Double> dList){
		double sum = 0.0;
		ArrayList<Double> res = new ArrayList<Double>();
		for(Double d: dList){
			sum += d;
			res.add(sum);
		}
		return res;
	}
	//
	public static ArrayList<Integer> getIntList(ArrayList<DoubleInt> diList){
		ArrayList<Integer> res = new ArrayList<Integer>();
		for(DoubleInt di: diList){
			res.add(di.getSecond());
		}
		return res;
	}
	//
	public static ArrayList<Double> getPointList(ArrayList<Integer> indexList, int keepInt){
		ArrayList<Double> res = new ArrayList<Double>();
		for(Integer index: indexList){
			if(index == keepInt){
				res.add(1.0);
			}else{
				res.add(0.0);
			}
		}
		return res;
	}
	//
	public static ArrayList<Double> mul(ArrayList<Double> dList, double v){
		ArrayList<Double> res = new ArrayList<Double>();
		for(Double d: dList){
			res.add(d*v);
		}
		return res;
	}
	//
	public static ArrayList<Double> minus(ArrayList<Double> aList, ArrayList<Double> bList){
		ArrayList<Double> res = new ArrayList<Double>();
		if(aList.size() != bList.size()){
			new Exception("Un-matched error!").printStackTrace();
			return null;
		}
		for(int i=0; i<aList.size(); i++){
			res.add(aList.get(i) - bList.get(i));
		}
		return res;		
	}
	/**
	 * @return A list with the same value of the given size
	 * **/
	public static ArrayList<Double> getUniformList(Double v, int size){
		ArrayList<Double> res = new ArrayList<Double>();
		for(int i=0; i<size; i++){
			res.add(v);
		}
		return res;
	}
	//
	public static ArrayList<Double> pointwiseMul(ArrayList<Double> aList, ArrayList<Double> bList){
		ArrayList<Double> res = new ArrayList<Double>();
		if(aList.size() != bList.size()){
			new Exception("Un-matched error!").printStackTrace();
			return null;
		}
		for(int i=0; i<aList.size(); i++){
			res.add(aList.get(i)*bList.get(i));
		}
		return res;	
	}
	//
	public static double sum(ArrayList<Double> dList){
		double sum = 0.0;
		for(Double d: dList){
			sum += d.doubleValue();
		}
		return sum;
	}
	
	//
	public static void main(String []args){
		//1
		double [] array = {2.0, 1.5, 3.4};
		ArrayList<DoubleInt> list = getDoubleIntList(array);
		//Collections.sort(list, new PairComparatorByFirst_Asc<Double, Integer>());
		//for(DoubleInt di: list){
		//	System.out.println(di.toString());
		//}
		ArrayList<Double> cSum = cumsumDI(list);
		System.out.println(cSum);
	}

}
