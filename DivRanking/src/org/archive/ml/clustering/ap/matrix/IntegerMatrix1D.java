/* ===========================================================
 * APGraphClusteringPlugin : Java implementation of affinity propagation
 * algorithm as Cytoscape plugin.
 * ===========================================================
 *
 *
 * Project Info:  http://bioputer.mimuw.edu.pl/modevo/
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

public class IntegerMatrix1D extends Matrix1D<Integer> {

    public IntegerMatrix1D(final int N) {
        super(N);
        this.setVector(new Integer[N]);
        for (int i = 0; i < N; i++) {
            setValue(i, Integer.valueOf(0));
        }
    }
    
    public IntegerMatrix1D(final Integer [] vArray){
    	super(vArray);
    }

    public static IntegerMatrix1D range(final int r) {
        IntegerMatrix1D res = new IntegerMatrix1D(r);
        res.setVector(new Integer[r]);
        for (int i = 0; i < r; i++) {
            res.setValue(i, Integer.valueOf(i));
        }
        return res;
    }

    @Override
    public IntegerMatrix1D copy() {
        IntegerMatrix1D res = new IntegerMatrix1D(this.size());
        for (int i = 0; i < this.size(); i++) {
            res.set(i, this.get(i));
        }

        return res;
    }
}
