package org.archive.ml.clustering.ap.abs;

import java.util.Vector;

import org.archive.util.tuple.BooleanInt;

/**
 * A 2d vector to record the change (i.e., exemplar or non-exemplar) within an iterative span
 * each ConvitsVector2D is used for tracking one data-point
 * **/

public class ConvitsVector2D implements Comparable<ConvitsVector2D> {
	//boolean value: the exemplar-state (exemplar or non-exemplar)
	//integer value: target index
    private Vector<BooleanInt> convits;
    //the current iterative position
    private int current;
    //the iterative span
    private int len;
    //indicates whether performs at least len times of iteration
    private boolean ready;
    //the index of the data pint
    private Integer name;

    public ConvitsVector2D(int len, Integer n) {
        this.len = len;
        this.convits = new Vector<BooleanInt>(len);
        this.current = 0;
        this.ready = false;
        this.name = n;
    }
    //set the current state
    public void addCovits(BooleanInt booInt) {
        convits.set(current, booInt);
        if (current == len - 1) {
            this.ready = true;
        }
        current = (current + 1) % len;
    }

    public boolean checkConvits() {
        if (ready == false) {
        	//do not meet the least iteration
            return false;
        } else {
        	BooleanInt firstBooInt = convits.firstElement();

            for (BooleanInt booInt : convits) {
                if (!booInt.equals(firstBooInt)) {
                	//System.out.println("Not equal");
                	//System.out.println(booInt.toString());
                	//System.out.println(firstBooInt.toString());
                	//System.out.println();
                    return false;
                }
            }
            return true;
        }
    }
    //
    public void init() {
        for (int i = 0; i < len; i++) {
            this.convits.add(new BooleanInt(false, -1));
        }
    }

    public int compareTo(ConvitsVector2D o) {
        return this.name.compareTo(o.name);
    }
    //
    public static void main(String []args){
    	//1 test
    	
    }
}
