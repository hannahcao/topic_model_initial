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
public class ArrayMatrix1D implements IMatrix1D {
    private static final long serialVersionUID = -3133717999022236770L;
    double[] data;
    private int dim0size;

    public ArrayMatrix1D(int dim0size) {
        this.dim0size = dim0size;
        data = new double[dim0size];
        assignZero();

    }

    public ArrayMatrix1D(int[] dimSizes) {
        this(dimSizes[0]);
    }

    public void add(int coord0, double value) {
        set(coord0, get(coord0) + value);
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

    public void set(int coord0, double value) {
        data[coord0] = value;
    }

    public double get(int coord0) {
        return data[coord0];
    }

    public void assignZero() {
        Arrays.fill(data, 0);
    }

    public int getDimensions() {
        return 1;
    }

    public IMatrix toMatrix(IMatrix instance) {
        return this;
    }

    public int getDimensionSize(int dimension) {
        if (dimension == 0) {
            return dim0size;
        }
        throw new RuntimeException("dimension " + dimension + " not valid in a Matrix1D");
    }

    public void trimToSize() {
    }

    public void getNonZeros(IntArrayList coordList0, DoubleArrayList values) {
        for (int i = 0; i < dim0size; i++) {
            final double val = get(dim0size);
            if (val != 0) {
                coordList0.add(i);
                values.add(val);
            }
        }
    }

    public double marginalize() {
        double sum = 0;
        for (double val : data) {
            sum += val;
        }
        return sum;
    }

    public String toString() {
        return Arrays.toString(data);
    }

    public IMatrix marginalize(int margeDimension) {
        throw new UnsupportedOperationException("use marginalizeMin() on a Matrix1D");
    }

    public IMatrix marginalize(int[] margeDimensions) {
        throw new UnsupportedOperationException("use marginalizeMin() on a Matrix1D");
    }

    public IMatrix transpose(int[] dimensions) {
        return this;
    }

    public void set(int[] coordinates, double val) throws IndexOutOfBoundsException {
        set(coordinates[0], val);
    }

    public void add(int[] coordinates, double val) throws IndexOutOfBoundsException {
        add(coordinates[0], val);
    }

    public double get(int[] coordinates) throws IndexOutOfBoundsException {
        return get(coordinates[0]);
    }

    public void getNonZeros(List<IntArrayList> keys, DoubleArrayList values) {
        getNonZeros(keys.get(0), values);
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
        if (!(obj instanceof IMatrix1D)) {
            return false;
        }
        IMatrix1D mat = (IMatrix1D) obj;
        int[] entry = new int[]{0};
        for (int i = 0; i < getDimensionSize(0); i++) {
            entry[0] = i;
            if (getQuick(entry) != mat.getQuick(entry)) {
                return false;
            }
        }
        return true;
    }

    public void doForAllNonZeros(INonZeroPerformer1D performer) {
        for (int i = 0; i < dim0size; i++) {
            final double val = get(i);
            if (val != 0) {
                performer.iteration(i, val, -1);
            }
        }
    }

    public void doForAllNonZeros(final INonZeroPerformerGeneric performer) {
        doForAllNonZeros(new INonZeroPerformer1D() {
            public void iteration(int coord0, double val, int position) {
                performer.iteration(new int[]{coord0}, val, position);
            }
        });
    }

    //void doForAllNonZerosGeneric(INonZeroPerformer performer);
    public double sum() {
        return marginalize();
    }

    public IMatrix marginalizeTo(int[] margeDimensions, IMatrix target) {
        throw new UnsupportedOperationException("to implement"); // todo implement  marginalizeTo
    }

    public int size() {
        int s = 0;
        for (int i = 0; i < data.length; i++) {
            if (data[i] != 0) {
                s++;
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


}
