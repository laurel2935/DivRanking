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

import java.util.ArrayList;
import java.util.Collections;

import org.archive.util.tuple.DoubleInt;
import org.archive.util.tuple.PairComparatorByFirst_Desc;

public class DoubleMatrix1D extends Matrix1D<Double> implements DoubleMatrix1DInterface {

    public DoubleMatrix1D(final Double[] vector) {
        super(vector.length);
        this.setVector(new Double[this.size()]);
        for (int i = 0; i < this.size(); i++) {
            this.setValue(i, vector[i]);
        }
    }

    public DoubleMatrix1D(final int N) {
        super(N);
    }

    public DoubleMatrix1D(final int N, final double t) {
        super(N);
        this.setVector(new Double[N]);
        for (int i = 0; i < N; i++) {
            this.setValue(i, Double.valueOf(t));
        }
    }

    public void set(final int i, final double t) {
        super.set(i, Double.valueOf(t));
    }
    /**
     * @return A Matrix1D that records the sequential indexes the corresponding value of which is larger than the given {x}
     * */
    public IntegerMatrix1D findG(final double x) {
        int count = 0;
        for (int i = 0; i < this.size(); i++) {
            if (this.getValue(i).doubleValue() > x) {
                count++;
            }
        }
        IntegerMatrix1D res = new IntegerMatrix1D(count);
        count = 0;
        for (int i = 0; i < this.size(); i++) {
            if (this.getValue(i).doubleValue() > x) {
                res.set(count, Integer.valueOf(i));
                count++;
            }
        }
        return res;
    }
    /**
     * @return A Matrix1D that records the sequential indexes (sorted in decreasing order of Double value) the corresponding value of which is larger than the given {x}
     * */
    public IntegerMatrix1D findG_Sorted(final double x) {
        int count = 0;
        ArrayList<DoubleInt> eList = new ArrayList<DoubleInt>();
        
        for (int i = 0; i < this.size(); i++) {
            if (this.getValue(i).doubleValue() > x) {
            	eList.add(new DoubleInt(this.getValue(i).doubleValue(), i));
                count++;
            }
        }
        
        Collections.sort(eList, new PairComparatorByFirst_Desc<Double, Integer>());
        
        IntegerMatrix1D res = new IntegerMatrix1D(count);
        
        for(int i=0; i<count; i++){
        	res.set(i, eList.get(i).second);
        }        
        //--
        /*
        count = 0;
        for (int i = 0; i < this.size(); i++) {
            if (this.getValue(i).doubleValue() > x) {
                res.set(count, Integer.valueOf(i));
                count++;
            }
        }
        */
        return res;
    }
    
    public IntegerMatrix1D findG_WithEqual(final double x) {
        int count = 0;
        for (int i = 0; i < this.size(); i++) {
            if (this.getValue(i).doubleValue() >= x) {
                count++;
            }
        }
        IntegerMatrix1D res = new IntegerMatrix1D(count);
        count = 0;
        for (int i = 0; i < this.size(); i++) {
            if (this.getValue(i).doubleValue() >= x) {
                res.set(count, Integer.valueOf(i));
                count++;
            }
        }
        return res;
    }

    @Override
    public DoubleMatrix1D copy() {
        DoubleMatrix1D res = new DoubleMatrix1D(this.size());
        for (int i = 0; i < this.size(); i++) {
            res.set(i, this.get(i));
        }

        return res;
    }
}
