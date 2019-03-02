package util.matrix;

import cern.colt.list.DoubleArrayList;
import cern.colt.list.IntArrayList;

import java.util.List;

/**
 * Wrapper of an {@link util.matrix.ArrayMatrix1D} with translated indices. This is useful, if
 * you do not store elements below a given index.
 */
public class IndexTranslatorMatrix1D implements IMatrix1D {
    private final IMatrix1D wrappee;
    private final int dim0size;
    private final int offset0;

    public IndexTranslatorMatrix1D(int dim0size, int dim0sizeOffset) {
        this(dim0size, dim0sizeOffset, new ArrayMatrix1D(dim0size - dim0sizeOffset));
    }

    public IndexTranslatorMatrix1D(int[] dimSizes, int[] offset) {
        this(dimSizes[0], offset[0]);
    }

    public IndexTranslatorMatrix1D(int dim0size, int dim0sizeOffset, IMatrix1D delegate) {
        this.dim0size = dim0size;
        this.offset0 = dim0sizeOffset;
        wrappee = delegate;
    }

    private int[] offsetCoords(int[] coords) {
        return new int[]{coords[0] - offset0};
    }

    private int[] offsetCoordsInv(int[] coords) {
        return new int[]{coords[0] + offset0};
    }


    public void add(int coord0, double value) {
        wrappee.add(coord0 - offset0, value);
    }

    public void addQuick(int[] coordinates, double val) {
        wrappee.addQuick(offsetCoords(coordinates), val);
    }

    public void setQuick(int[] coordinates, double val) {
        wrappee.setQuick(offsetCoords(coordinates), val);
    }

    public double getQuick(int[] coordinates) {
        return wrappee.getQuick(offsetCoords(coordinates));
    }

    public void set(int coord0, double value) {
        wrappee.set(coord0 - offset0, value);
    }

    public double get(int coord0) {
        return wrappee.get(coord0 - offset0);
    }

    public void assignZero() {
        wrappee.assignZero();
    }

    public int getDimensions() {
        return wrappee.getDimensions();
    }

    public IMatrix toMatrix(IMatrix instance) {
        return this;
    }

    public int getDimensionSize(int dimension) {
        return dim0size;
    }

    public void trimToSize() {
        wrappee.trimToSize();
    }

    public void getNonZeros(IntArrayList coordList0, DoubleArrayList values) {
        IntArrayList translCoordList0 = new IntArrayList();
        wrappee.getNonZeros(translCoordList0, values);
        for (int i = 0; i < values.size(); i++) {
            coordList0.add(translCoordList0.get(i) + offset0);
        }

    }

    public double marginalize() {
        return wrappee.marginalize();
    }

    public IMatrix marginalize(int margeDimension) {
        return wrappee.marginalize(margeDimension);
    }

    public IMatrix marginalize(int[] margeDimensions) {
        return wrappee.marginalize(margeDimensions);
    }

    public IMatrix transpose(int[] dimensions) {
        return this;
    }

    public void set(int[] coordinates, double val) throws IndexOutOfBoundsException {
        wrappee.set(offsetCoords(coordinates), val);
    }

    public void add(int[] coordinates, double val) throws IndexOutOfBoundsException {
        wrappee.add(offsetCoords(coordinates), val);
    }

    public double get(int[] coordinates) throws IndexOutOfBoundsException {
        return wrappee.get(offsetCoords(coordinates));
    }

    public void getNonZeros(List<IntArrayList> keys, DoubleArrayList values) {
        getNonZeros(keys.get(0), values);
    }

    public void doForAllNonZeros(INonZeroPerformer1D performer) {
        wrappee.doForAllNonZeros(new IndexTranslatorPerformer1D(performer));
    }

    public void doForAllNonZeros(INonZeroPerformerGeneric performer) {
        wrappee.doForAllNonZeros(new IndexTranslatorPerformer(performer));
    }

    public double sum() {
        return wrappee.sum();
    }

    public IMatrix marginalizeTo(int[] margeDimensions, IMatrix target) {
        return wrappee.marginalizeTo(margeDimensions, target);
    }

    public int size() {
        return wrappee.size();
    }

    public IMatrixMin marginalizeMin(int[] margeDims) {
        return this;
    }

    public IMatrixMin transposeMin(int[] dimensions) {
        return this;
    }

    public void add(int[] coordinates, double val, int position) throws IndexOutOfBoundsException {
        wrappee.add(offsetCoords(coordinates), val, position);
    }

    public void set(int[] coordinates, double val, int position) throws IndexOutOfBoundsException {
        wrappee.set(offsetCoords(coordinates), val, position);
    }

    public double get(int[] coordinates, int position) throws IndexOutOfBoundsException {
        return get(coordinates);

    }

    protected class IndexTranslatorPerformer implements INonZeroPerformerGeneric {
        private final INonZeroPerformerGeneric wrapPerform;

        public IndexTranslatorPerformer(INonZeroPerformerGeneric wrapPerform) {
            this.wrapPerform = wrapPerform;
        }

        public void iteration(int[] coords, double val, int position) {
            wrapPerform.iteration(offsetCoordsInv(coords), val, position);
        }
    }


    protected class IndexTranslatorPerformer1D implements INonZeroPerformer1D {
        private final INonZeroPerformer1D wrapPerform;

        public IndexTranslatorPerformer1D(INonZeroPerformer1D wrapPerform) {
            this.wrapPerform = wrapPerform;
        }

        public void iteration(int coord0, double val, int position) {
            wrapPerform.iteration(coord0 + offset0, val, position);
        }
    }

}
