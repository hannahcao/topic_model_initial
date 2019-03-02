package util.matrix;

import cern.colt.function.IntIntDoubleFunction;
import cern.colt.list.DoubleArrayList;
import cern.colt.list.IntArrayList;
import cern.colt.matrix.DoubleFactory2D;
import cern.colt.matrix.DoubleMatrix2D;

import java.util.List;

/**
 * 4D Matrix implementation based in an array of multiple rowcompressed 2D colt matrices. (The last dimension is row compressed)
 * <p/>
 * <h2>Usage Advice</h2>
 * The performance is best, if the matrix is dense in the first two dimensions, but sparse in the last dimension.
 */
public class RowCompressedColtMatrix4D implements IMatrix4D {
    private static final long serialVersionUID = 6250719242507657551L;

    private final DoubleMatrix2D[][] colt2DArray;
    private final int dim0size;
    private final int dim1size;
    private final int dim2size;
    private final int dim3size;
    private final int[] dimsizes;

    public RowCompressedColtMatrix4D(int dim0size, int dim1size, int dim2size, int dim3size) {
        this.dim0size = dim0size;
        this.dim1size = dim1size;
        this.dim2size = dim2size;
        this.dim3size = dim3size;
        dimsizes = new int[]{dim0size, dim1size, dim2size, dim3size};
        colt2DArray = new DoubleMatrix2D[dim0size][dim3size];
        for (int coord0 = 0; coord0 < dim0size; coord0++) {
            for (int coord3 = 0; coord3 < dim3size; coord3++) {
                colt2DArray[coord0][coord3] = DoubleFactory2D.rowCompressed.make(dim1size, dim2size, 0.0);
            }
        }
    }


    public void add(int coord0, int coord1, int coord2, int coord3, double value) {
        DoubleMatrix2D mat = colt2DArray[coord0][coord3];
        double oldVal = mat.get(coord1, coord2);
        mat.set(coord1, coord2, oldVal + value);
    }

    public void set(int coord0, int coord1, int coord2, int coord3, double value) {
        colt2DArray[coord0][coord3].set(coord1, coord2, value);
    }

    public double get(int coord0, int coord1, int coord2, int coord3) {
        return colt2DArray[coord0][coord3].get(coord1, coord2);
    }

    public void doForAllNonZeros(final INonZeroPerformer4D performer) {
        for (int coord0 = 0; coord0 < dim0size; coord0++) {
            for (int coord3 = 0; coord3 < dim3size; coord3++) {
                DoubleMatrix2D mat = colt2DArray[coord0][coord3];
                final int coord0_ = coord0;
                final int coord3_ = coord3;
                mat.forEachNonZero(new IntIntDoubleFunction() {
                    public double apply(int coord1, int coord2, double v) {
                        performer.iteration(coord0_, coord1, coord2, coord3_, v, -1);
                        return v;
                    }
                });
            }
        }
    }

    // /////////////////////////////////////////


    public void add(int[] coordinates, double val, int position) throws IndexOutOfBoundsException {
        add(coordinates, val, -1);
    }

    public void set(int[] coordinates, double val, int position) throws IndexOutOfBoundsException {
        get(coordinates);
    }

    public double get(int[] coordinates, int position) throws IndexOutOfBoundsException {
        return get(coordinates);

    }

    public IMatrixMin marginalizeMin(int[] margeDims) {
        throw new UnsupportedOperationException("todo: implement");
    }

    public IMatrixMin transposeMin(int[] dimensions) {
        throw new UnsupportedOperationException("todo: implement");
    }

    public IMatrix toMatrix(IMatrix instance) {
        return this;
    }

    // ////////////////////////////////////////////////


    public void assignZero() {
        for (int coord0 = 0; coord0 < dim0size; coord0++) {
            for (int coord3 = 0; coord3 < dim3size; coord3++) {
                colt2DArray[coord0][coord3].assign(0.0);
            }
        }
    }

    public int getDimensions() {
        return dimsizes.length;
    }

    public int getDimensionSize(int dimension) {
        return dimsizes[dimension];
    }

    public void trimToSize() {
        for (int coord0 = 0; coord0 < dim0size; coord0++) {
            for (int coord3 = 0; coord3 < dim3size; coord3++) {
                colt2DArray[coord0][coord3].trimToSize();
            }
        }
    }

    public IMatrix marginalize(int margeDimension) {
        // todoDW matrix maginalization
        throw new UnsupportedOperationException("todo: implement");
    }

    public IMatrix marginalize(int[] margeDimensions) {
        throw new UnsupportedOperationException("todo: implement");
    }

    public IMatrix transpose(int[] dimensions) {
        throw new UnsupportedOperationException("todo: implement");
    }

    public void set(int[] coordinates, double val) throws IndexOutOfBoundsException {
        set(coordinates[0], coordinates[1], coordinates[2], coordinates[3], val);
    }

    public void add(int[] coordinates, double val) throws IndexOutOfBoundsException {
        add(coordinates[0], coordinates[1], coordinates[2], coordinates[3], val);
    }

    public double get(int[] coordinates) throws IndexOutOfBoundsException {
        return get(coordinates[0], coordinates[1], coordinates[2], coordinates[3]);
    }

    public void getNonZeros(final List<IntArrayList> keys, final DoubleArrayList values) {
        for (int coord0 = 0; coord0 < dim0size; coord0++) {
            for (int coord3 = 0; coord3 < dim3size; coord3++) {
                DoubleMatrix2D mat = colt2DArray[coord0][coord3];
                final int coord0_ = coord0;
                final int coord3_ = coord3;
                mat.forEachNonZero(new IntIntDoubleFunction() {
                    public double apply(int coord1, int coord2, double v) {
                        keys.get(0).add(coord0_);
                        keys.get(1).add(coord1);
                        keys.get(2).add(coord2);
                        keys.get(3).add(coord3_);
                        values.add(v);
                        return v;
                    }
                });
            }
        }
    }

    public void doForAllNonZeros(final INonZeroPerformerGeneric performer) {
        for (int coord0 = 0; coord0 < dim0size; coord0++) {
            for (int coord3 = 0; coord3 < dim3size; coord3++) {
                DoubleMatrix2D mat = colt2DArray[coord0][coord3];
                final int coord0_ = coord0;
                final int coord3_ = coord3;
                mat.forEachNonZero(new IntIntDoubleFunction() {
                    public double apply(int coord1, int coord2, double v) {
                        performer.iteration(new int[]{coord0_, coord1, coord2, coord3_}, v, -1);
                        return v;
                    }
                });
            }
        }
    }

    public void addQuick(int[] coords, double val) {
        final DoubleMatrix2D mat = colt2DArray[coords[0]][coords[3]];
        double prevVal = mat.getQuick(coords[1], coords[2]);
        mat.setQuick(coords[1], coords[2], val + prevVal);
    }

    public void setQuick(int[] coords, double value) {
        colt2DArray[coords[0]][coords[3]].setQuick(coords[1], coords[2], value);
    }

    public double getQuick(int[] coords) {
        return colt2DArray[coords[0]][coords[3]].getQuick(coords[1], coords[2]);
    }

    public double sum() {
        double result = 0.0;
        for (int coord0 = 0; coord0 < dim0size; coord0++) {
            for (int coord3 = 0; coord3 < dim3size; coord3++) {
                result += colt2DArray[coord0][coord3].zSum();
            }
        }
        return result;
    }

    public IMatrix marginalizeTo(int[] margeDimensions, IMatrix target) {
        throw new UnsupportedOperationException("todo: implement");
    }

    public int size() {
        return dim0size * dim1size * dim2size * dim3size;
    }
}
