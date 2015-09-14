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

public class DoubleMatrix2D implements java.io.Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 1L;
    private double[][] matrix;
    private int N,  M;
    /**
     * @return A matrix with M columns, v.length rows, each row has the same value of v[i], i.e., each column vector is the same as {v}
     * */
    public DoubleMatrix2D(final int M, final double[] v) {
        this.N = v.length;
        this.M = M;
        this.matrix = new double[N][M];
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < M; j++) {
                this.matrix[i][j] = v[i];
            }
        }
    }

    public DoubleMatrix2D(final int N, final int M, final double[][] m) {
        this.N = N;
        this.M = M;
        this.matrix = new double[N][M];
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < M; j++) {
                this.matrix[i][j] = m[i][j];
            }
        }
    }

    public DoubleMatrix2D(final int N, final int M, final double v) {
        this.N = N;
        this.M = M;
        this.matrix = new double[N][M];
        setValues(v);
    }

    private DoubleMatrix2D(final int N, final int M) {
        this.N = N;
        this.M = M;
        this.matrix = new double[N][M];
    }

    private DoubleMatrix2D(final DoubleMatrix2D m) {
        this.N = m.N;
        this.M = m.M;
        this.matrix = new double[N][M];
        set(m.matrix);
    }
    /**
     * @return a one-row Matrix2D that sums of each column
     * */
    public DoubleMatrix2D sumEachColumn() {
        DoubleMatrix2D res = new DoubleMatrix2D(1, M, 0);
        for (int j = 0; j < M; j++) {
            double s = 0;
            for (int i = 0; i < N; i++) {
                s += this.matrix[i][j];

            }
            res.matrix[0][j] = s;
        }
        return res;
    }
   
    /**
     * @return get the diagose matrix1D
     * */
    public DoubleMatrix1D diag() {
        DoubleMatrix1D res = new DoubleMatrix1D(N, 0);
        for (int i = 0; i < N; i++) {
            res.set(i, Double.valueOf(this.matrix[i][i]));
        }

        return res;
    }
    //
    
    public DoubleMatrix2D transpose() {
        DoubleMatrix2D res = new DoubleMatrix2D(M, N);

        for (int i = 0; i < N; i++) {
            for (int j = 0; j < M; j++) {
                res.matrix[j][i] = this.matrix[i][j];
            }
        }

        return res;
    }

    public void set(final int i, final int j, final double v) {
        matrix[i][j] = v;
    }

    public double get(final int i, final int j) {
        return matrix[i][j];
    }

    public double[] getVector(final int i) {
        return matrix[i];
    }

    /*   public double[][] get() {
    return matrix;
    }*/
    public void set(final int n, final int m, final double[][] matrix) {
        this.N = n;
        this.M = m;
        set(matrix);
    }

    public void set(final double[][] matrix) {
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < M; j++) {
                this.matrix[i][j] = matrix[i][j];
            }
        }
    }

    public void setValues(final double v) {
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < M; j++) {
                matrix[i][j] = v;
            }
        }
    }

    public void plusTo(final DoubleMatrix2D m) {
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < M; j++) {
                matrix[i][j] += m.get(i, j);
            }
        }
    }
    /**
     * @return a new DoubleMatrix2D corresponding to the sum
     * **/
    public DoubleMatrix2D plus(final DoubleMatrix2D m) {
        DoubleMatrix2D result = new DoubleMatrix2D(N, M);
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < M; j++) {
                result.set(i, j, this.matrix[i][j] + m.get(i, j));
            }
        }
        return result;
    }

    public void minusTo(final DoubleMatrix2D m) {
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < M; j++) {
                matrix[i][j] -= m.get(i, j);
            }
        }
    }
    /**
     * @return a new DoubleMatrix2D corresponding to the minus value
     * **/
    public DoubleMatrix2D minus(final DoubleMatrix2D m) {
        DoubleMatrix2D result = new DoubleMatrix2D(N, M);
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < M; j++) {
                result.set(i, j, this.matrix[i][j] - m.get(i, j));
            }
        }
        return result;
    }
    /**
     * @return a DoubleMatrix2D corresponding to the mul value
     * **/
    public DoubleMatrix2D mul(final double c) {
        DoubleMatrix2D result = new DoubleMatrix2D(N, M);
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < M; j++) {
                result.set(i, j, this.matrix[i][j] * c);
            }
        }
        return result;
    }

    public void plusTo(final double[][] m) {
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < M; j++) {
                matrix[i][j] += m[i][j];
            }
        }
    }

    public void mulTo(final double c) {
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < M; j++) {
                matrix[i][j] *= c;
            }
        }
    }

    public DoubleMatrix2D copy() {
        return new DoubleMatrix2D(this);
    }

    public int getM() {
        return M;
    }

    public void setM(final int M) {
        this.M = M;
    }

    public int getN() {
        return N;
    }

    public void setN(final int N) {
        this.N = N;
    }
    /**
     * @return A two column matrix, the 1st column records the index of the maximum element in the row;
     * the 2nd column record the value of the maximum element 
     * */
    public DoubleMatrix2D maxr() {
        DoubleMatrix2D res = new DoubleMatrix2D(N, 2, 0);

        for (int i = 0; i < N; i++) {
            int maxj = 0;
            for (int j = 0; j < M; j++) {
                if (this.get(i, j) > this.get(i, maxj)) {
                    maxj = j;
                }
            }
            //the first column records the column-position of the maximum element 
            res.set(i, 0, maxj);
            //the second column records the value of the maximum element
            res.set(i, 1, this.matrix[i][maxj]);
        }
        return res;
    }
    /**
     * @return A two row matrix, the 1st row records the index of the maximum element in the column;
     * the 2nd row record the value of the maximum element 
     * */
    public DoubleMatrix2D maxc() {
        DoubleMatrix2D res = new DoubleMatrix2D(2, M, 0);

        for (int j = 0; j < M; j++) {
            int maxi = 0;
            for (int i=0; i<N; i++) {
                if (this.get(i, j) > this.get(maxi, j)) {
                	maxi = i;
                }
            }
            //the first row records the row-position of the maximum element 
            res.set(0, j, maxi);
            //the second row records the value of the maximum element
            res.set(1, j, this.matrix[maxi][j]);
        }
        return res;
    }

    public IntegerMatrix1D maxrIndexes() {
        IntegerMatrix1D res = new IntegerMatrix1D(N);

        for (int i = 0; i < N; i++) {
            int maxj = 0;
            for (int j = 0; j < M; j++) {
                if (this.matrix[i][j] > this.matrix[i][maxj]) {
                    maxj = j;
                }
            }
            res.set(i, Integer.valueOf(maxj));
        }
        return res;
    }
    //
    public IntegerMatrix1D maxcIndexes() {
        IntegerMatrix1D res = new IntegerMatrix1D(M);

        for (int j=0; j<M; j++) {
            int maxi = 0;
            for (int i=0; i<N; i++) {
                if (this.matrix[i][j] > this.matrix[maxi][j]) {
                	maxi = i;
                }
            }
            res.set(j, Integer.valueOf(maxi));
        }
        return res;
    }
    /**
     * @return merely keep the values that are larger or equal to "v", and replace the values with given v if it is smaller than v
     * */
    public DoubleMatrix2D max(final double v) {
        DoubleMatrix2D res = new DoubleMatrix2D(this);

        for (int i = 0; i < N; i++) {
            for (int j = 0; j < M; j++) {
                if (v > this.matrix[i][j]) {
                    res.matrix[i][j] = v;
                }
            }
        }
        return res;
    }
    /**
     * @return merely keep the values that are larger or equal to "v", and replace the values with replaceV if it is smaller than v
     * */
    public DoubleMatrix2D max(final double v, final double replaceV) {
        DoubleMatrix2D res = new DoubleMatrix2D(this);

        for (int i = 0; i < N; i++) {
            for (int j = 0; j < M; j++) {
                if (v > this.matrix[i][j]) {
                    res.matrix[i][j] = replaceV;
                }
            }
        }
        return res;
    }

    public DoubleMatrix2D getColumns(final IntegerMatrix1D indexes) {
        DoubleMatrix2D res = new DoubleMatrix2D(this.N, indexes.size());
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < indexes.size(); j++) {
                res.set(i, j, this.matrix[i][indexes.get(j).intValue()]);
            }
        }

        return res;
    }

    public DoubleMatrix1D getColumn(final int j) {
        DoubleMatrix1D res = new DoubleMatrix1D(N);
        for (int i = 0; i < N; i++) {
            res.set(i, this.matrix[i][j]);
        }

        return res;
    }

    public DoubleMatrix1D getRow(final int i) {
        DoubleMatrix1D res = new DoubleMatrix1D(M);
        for (int j = 0; j < M; j++) {
            res.set(j, this.matrix[i][j]);
        }

        return res;
    }
    /**
     * @return merely keep the values that are smaller or equal to "v", and replace the values with given v if it is larger than v
     * **/
    public DoubleMatrix2D min(final double v) {
        DoubleMatrix2D res = new DoubleMatrix2D(this);

        for (int i = 0; i < N; i++) {
            for (int j = 0; j < M; j++) {
                if (v < this.matrix[i][j]) {
                    res.matrix[i][j] = v;
                }
            }
        }
        return res;
    }

    @Override
    public String toString() {
        StringBuffer res = new StringBuffer();
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < M; j++) {
                res.append(this.matrix[i][j]);
                res.append(" ");
            }
            res.append("\n");

        }
        return res.toString();
    }
}