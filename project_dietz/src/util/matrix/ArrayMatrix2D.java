package util.matrix;

import cern.colt.list.DoubleArrayList;
import cern.colt.list.IntArrayList;

import java.util.Arrays;
import java.util.List;

/**
 * todo comment 17.11.2006
 *
 * @version $ID$
 */
public class ArrayMatrix2D implements IMatrix2D {
    private static final long serialVersionUID = 205563074542624861L;
    protected double[][] data;
    private int dim0size, dim1size;

    public ArrayMatrix2D(int dim0size, int dim1size) {
        this.dim1size = dim1size;
        this.dim0size = dim0size;
        data = new double[dim0size][dim1size];
        assignZero();
    }

    public ArrayMatrix2D(int[] dimSizes) {
        this(dimSizes[0], dimSizes[1]);
    }

    public void assignZero() {
        for (int i = 0; i < dim0size; i++) {
            for (int j = 0; j < dim1size; j++) {
                data[i][j] = 0;
            }
        }
    }

    public int getDimensions() {
        return 2;
    }

    public IMatrix toMatrix(IMatrix instance) {
        return this;
    }

    public int getDimensionSize(int dimension) {
        if (dimension == 0) {
            return dim0size;
        } else if (dimension == 1) {
            return dim1size;
        }
        throw new RuntimeException("dimension " + dimension + " not valid in a Matrix2D");
    }

    public void add(int coord0, int coord1, double value) {
        set(coord0, coord1, value + get(coord0, coord1));
    }

    public void addQuick(int[] coordinates, double val) {
        add(coordinates, val);
    }

    public void setQuick(int[] coordinates, double val) {
        set(coordinates, val);
    }

    public double getQuick(int[] coordinates) {
        return get(coordinates);
    }

    public void doForAllNonZerosGeneric(INonZeroPerformer performer) {
        doForAllNonZeros((INonZeroPerformer2D) performer);

    }

    public void set(int coord0, int coord1, double value) {
        if (coord0 >= dim0size) {
            throw new RuntimeException(
                    "dim 0 of entry " + coord0 + "," + coord1 + " violates dimensionsize " + dim0size);
        }
        if (coord1 >= dim1size) {
            throw new RuntimeException(
                    "dim 1 of entry " + coord0 + "," + coord1 + " violates dimensionsize " + dim1size);
        }
        data[coord0][coord1] = value;
    }

    public double get(int coord0, int coord1) {
        if (coord0 >= dim0size) {
            throw new RuntimeException(
                    "dim 0 of entry " + coord0 + "," + coord1 + " violates dimensionsize " + dim0size);
        }
        if (coord1 >= dim1size) {
            throw new RuntimeException(
                    "dim 1 of entry " + coord0 + "," + coord1 + " violates dimensionsize " + dim1size);
        }
        return data[coord0][coord1];
    }

    public void getNonZeros(IntArrayList coordList0, IntArrayList coordList1, DoubleArrayList values) {
        for (int i = 0; i < dim0size; i++) {
            for (int j = 0; j < dim1size; j++) {
                final double val = get(i, j);
                if (val != 0) {
                    coordList0.add(i);
                    coordList1.add(j);
                    values.add(val);
                }
            }
        }
    }

    public void doForAllNonZeros(INonZeroPerformer2D performer) {
        for (int i = 0; i < dim0size; i++) {
            for (int j = 0; j < dim1size; j++) {
                final double val = get(i, j);
                if (val != 0) {
                    performer.iteration(i, j, val, -1);
                }
            }
        }
    }

    public void doForAllDim0NonZeros(int coord0, INonZeroPerformer1D performer) {
        for (int j = 0; j < dim1size; j++) {
            final double val = get(coord0, j);
            if (val != 0) {
                performer.iteration(j, val, -1);
            }
        }
    }

    public void doForAllDim1NonZeros(int coord1, INonZeroPerformer1D performer) {
        for (int i = 0; i < dim0size; i++) {
            final double val = get(i, coord1);
            if (val != 0) {
                performer.iteration(i, val, -1);
            }
        }
    }

    public void getNonZeros(List<IntArrayList> keys, DoubleArrayList values) {
        getNonZeros(keys.get(0), keys.get(1), values);
    }

    public void trimToSize() {
    }

    public IMatrix marginalize(int margeDimension) {
        ArrayMatrix1D marged = new ArrayMatrix1D(getDimensionSize(1 - margeDimension));
        if (margeDimension == 1) {
            for (int i = 0; i < dim0size; i++) {
                double sum = 0.0;
                for (int j = 0; j < dim1size; j++) {
                    sum += data[i][j];
                }
                marged.data[i] = sum;
            }
        } else if (margeDimension == 0) {
            for (int j = 0; j < dim1size; j++) {
                double sum = 0.0;
                for (int i = 0; i < dim0size; i++) {
                    sum += data[i][j];
                }
                marged.data[j] = sum;
            }
        } else {
            throw new RuntimeException("Matrix2D can not marginalizeMin dimension " + margeDimension);
        }
        return marged;
    }

    public IMatrix marginalize(int[] margeDimensions) {
        if (margeDimensions.length == 0) {
            return this;
        } else if (margeDimensions.length == 1) {
            return marginalize(margeDimensions[0]);
        } else {
            throw new RuntimeException(
                    "Matrix2D can only be marginalized for 1 dimensions; margeDimensions = " + margeDimensions);
        }
    }

    public IMatrix transpose(int[] dimensions) {
        ArrayMatrix2D t = new ArrayMatrix2D(dim1size, dim0size);
        for (int i = 0; i < dim0size; i++) {
            for (int j = 0; j < dim1size; j++) {
                t.data[j][i] = data[i][j];
            }
        }
        return t;
    }

    public void set(int[] coordinates, double val) throws IndexOutOfBoundsException {
        set(coordinates[0], coordinates[1], val);
    }

    public void add(int[] coordinates, double val) throws IndexOutOfBoundsException {
        add(coordinates[0], coordinates[1], val);
    }

    public double get(int[] coordinates) throws IndexOutOfBoundsException {
        return get(coordinates[0], coordinates[1]);
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof IMatrix)) {
            return false;
        }

        // check dims
        IMatrix mat = (IMatrix) obj;
        if (mat.getDimensions() != getDimensions()) {
            return false;
        }
        for (int d = 0; d < getDimensions(); d++) {
            if (mat.getDimensionSize(d) != getDimensionSize(d)) {
                return false;
            }
        }

        // if arraymatrix, do quickcheck
        if (obj instanceof ArrayMatrix2D) {
            ArrayMatrix2D matrix = (ArrayMatrix2D) obj;
            data.equals(matrix.data);
        }

        // else do full check
        return equalsCheckFull(mat);
    }

    protected boolean equalsCheckFull(IMatrix obj) {
        if (!(obj instanceof IMatrix2D)) {
            return false;
        }
        IMatrix2D mat = (IMatrix2D) obj;
        int[] entry = new int[]{0, 0};
        for (int i = 0; i < getDimensionSize(0); i++) {
            entry[0] = i;
            for (int j = 0; j < getDimensionSize(1); j++) {
                entry[1] = j;
                if (getQuick(entry) != mat.getQuick(entry)) {
                    return false;
                }
            }
        }
        return true;
    }

    public void doForAllNonZeros(final INonZeroPerformerGeneric performer) {
        doForAllNonZeros(new INonZeroPerformer2D() {
            public void iteration(int coord0, int coord1, double val, int position) {
                performer.iteration(new int[]{coord0, coord1}, val, position);
            }
        });
    }

    //void doForAllNonZerosGeneric(INonZeroPerformer performer);
    public double sum() {
        double result = 0.0;
        for (int i = 0; i < dim0size; i++) {
            for (int j = 0; j < dim1size; j++) {
                result += data[i][j];
            }
        }
        return result;
    }

    public IMatrix marginalizeTo(int[] margeDimensions, IMatrix target) {
        throw new UnsupportedOperationException("to implement"); // todo implement  marginalizeTo
    }

    public int size() {
        int s = 0;
        for (int i = 0; i < data.length; i++) {
            for (int j = 0; j < data[i].length; j++) {
                if (data[i][j] != 0) {
                    s++;
                }
            }
        }
        return s;
    }

    public IMatrixMin marginalizeMin(int[] margeDims) {
        return marginalize(margeDims);
    }

    public IMatrixMin transposeMin(int[] dimensions) {
        return transpose(dimensions);
    }

    public void add(int[] coordinates, double val, int position) throws IndexOutOfBoundsException {
        add(coordinates, val);
    }

    public void set(int[] coordinates, double val, int position) throws IndexOutOfBoundsException {
        set(coordinates, val);
    }

    public double get(int[] coordinates, int position) throws IndexOutOfBoundsException {
        return get(coordinates);
    }

    public String toString() {
        StringBuffer buf = new StringBuffer();
        
        //added by Huiping
        buf.append("row num = word num="+dim0size+"\n");
        if(dim0size>0) buf.append("col num = doc num = "+data[0].length+"\n");
        //end of adding
        
        for (int c0 = 0; c0 < dim0size; c0++) {
            double[] row = data[c0];
            buf.append(Arrays.toString(row)).append("\n");
        }
        return buf.toString();
        //return Arrays.deepToString(data);
    }


    public void add(int coord0, int coord1, double val, int position) throws IndexOutOfBoundsException {
        add(coord0, coord1, val);
    }
}
