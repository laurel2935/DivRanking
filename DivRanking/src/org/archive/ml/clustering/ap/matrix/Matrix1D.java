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
import java.util.Vector;

public abstract class Matrix1D<T> implements Matrix1DInterface<T> {

    private T[] vector;
    private int N;

    public Vector<T> getVector() {
        Vector<T> res = new Vector<T>(N);
        for (int i = 0; i < N; i++) {
            res.add(i, vector[i]);
        }
        return res;
    }
    public ArrayList<T> getList() {
        ArrayList<T> res = new ArrayList<T>(N);
        for (int i = 0; i < N; i++) {
            res.add(i, vector[i]);
        }
        return res;
    }

    public void setValue(final int i, final T t) {
        this.vector[i] = t;
    }

    public T getValue(final int i) {
        return this.vector[i];
    }

    @SuppressWarnings("unchecked")
    public void setVector(final T[] v) {
        this.N = v.length;
        this.vector = (T[]) new Object[this.N];
        for (int i = 0; i < this.size(); i++) {
            this.vector[i] = v[i];
        }
    }

    @SuppressWarnings("unchecked")
    public Matrix1D(final int N) {
        this.N = N;
        this.vector = (T[]) new Object[this.N];
    }

    @SuppressWarnings("unchecked")
    public Matrix1D(final T[] v) {
        this.N = v.length;
        this.vector = (T[]) new Object[this.N];
        for (int i = 0; i < this.size(); i++) {
            this.vector[i] = v[i];
        }

    }

    public int size() {
        return N;
    }

    public T get(final int i) {
        return vector[i];
    }

    public void set(final int i, final T t) {
        vector[i] = t;
    }

    public T max() {
        return this.vector[this.maxIndex()];
    }

    public int maxIndex() {
        int maxi = 0;
        for (int i = 0; i < N; i++) {
            @SuppressWarnings("unchecked")
            int compareTo = ((Comparable) (this.getValue(i))).compareTo(this.getValue(maxi));
            if (compareTo == 1) {
                maxi = i;
            }
        }

        return maxi;
    }

    @Override
    public String toString() {
        StringBuffer res = new StringBuffer();
        for (int i = 0; i < N; i++) {
            res.append(vector[i].toString());
            res.append(" ");
        }
        res.append("\n");
        return res.toString();
    }

    public abstract Matrix1D<T> copy();
}
