package util.matrix;

import cern.colt.list.DoubleArrayList;
import cern.colt.list.IntArrayList;

import java.util.List;

/**
 * Wrapper of an {@link util.matrix.ArrayMatrix2D} with translated indices. This is useful, if
 * you do not store elements below a given index.
 */
public class IndexTranslatorMatrix2D implements IMatrix2D {
    private final IMatrix2D wrappee;
    private final int dim0size;
    private final int dim1size;
    private final int offset0;
    private final int offset1;

    public IndexTranslatorMatrix2D(int dim0size, int dim1size, int dim0sizeOffset, int dim1sizeOffset) {
        this(dim0size, dim1size, dim0sizeOffset, dim1sizeOffset, new ArrayMatrix2D(dim0size - dim0sizeOffset, dim1size - dim1sizeOffset));
    }

    public IndexTranslatorMatrix2D(int[] dimSizes, int[] offset) {
        this(dimSizes[0], dimSizes[1], offset[0], offset[1]);
    }

    public IndexTranslatorMatrix2D(int dim0size, int dim1size, int dim0sizeOffset, int dim1sizeOffset, IMatrix2D delegate) {
        this.dim0size = dim0size;
        this.dim1size = dim1size;
        this.offset0 = dim0sizeOffset;
        this.offset1 = dim1sizeOffset;
        wrappee = delegate;
    }

    private int[] offsetCoords(int[] coords) {
        return new int[]{coords[0] - offset0, coords[1] - offset1};
    }

    private int[] offsetCoordsInv(int[] coords) {
        return new int[]{coords[0] + offset0, coords[1] + offset1};
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
        if (dimension == 0) return dim0size;
        if (dimension == 1) return dim1size;

        throw new RuntimeException("dimension " + dimension + " not valid in a Matrix2D");
    }

    public void add(int coord0, int coord1, double value) {
        wrappee.add(coord0 - offset0, coord1 - offset1, value);
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

//    public void doForAllNonZerosGeneric(INonZeroPerformer performer) {
//        wrappee.doForAllNonZerosGeneric(performer);
//    }

    public void set(int coord0, int coord1, double value) {
        wrappee.set(coord0 - offset0, coord1 - offset1, value);
    }

    public double get(int coord0, int coord1) {
        return wrappee.get(coord0 - offset0, coord1 - offset1);
    }

    public void getNonZeros(IntArrayList coordList0, IntArrayList coordList1, DoubleArrayList values) {
        IntArrayList translCoordList0 = new IntArrayList();
        IntArrayList translCoordList1 = new IntArrayList();
        wrappee.getNonZeros(translCoordList0, translCoordList1, values);
        for (int i = 0; i < values.size(); i++) {
            coordList0.add(translCoordList0.get(i) + offset0);
            coordList1.add(translCoordList1.get(i) + offset1);
        }
    }

    public void doForAllNonZeros(INonZeroPerformer2D performer) {
        wrappee.doForAllNonZeros(new IndexTranslatorPerformer2D(performer));
    }

    public void doForAllDim0NonZeros(int coord0, INonZeroPerformer1D performer) {
        wrappee.doForAllDim0NonZeros(coord0 - offset0, new IndexTranslatorPerformer1D(performer, 1));
    }

    public void doForAllDim1NonZeros(int coord1, INonZeroPerformer1D performer) {
        wrappee.doForAllDim1NonZeros(coord1 - offset1, new IndexTranslatorPerformer1D(performer, 0));
    }

    public void getNonZeros(List<IntArrayList> keys, DoubleArrayList values) {
        getNonZeros(keys.get(0), keys.get(1), values);
    }

    public void trimToSize() {
        wrappee.trimToSize();
    }

    public IMatrix marginalize(int margeDimension) {

        IMatrix marged = wrappee.marginalize(margeDimension);
        if (margeDimension == 0) {
            return new IndexTranslatorMatrix1D(dim1size, offset1, (IMatrix1D) marged);
        } else {
            assert (margeDimension == 1) : margeDimension;
            return new IndexTranslatorMatrix1D(dim0size, offset0, (IMatrix1D) marged);
        }
    }

    public IMatrix marginalize(int[] margeDimensions) {
        return marginalize(margeDimensions[0]);
    }

    public IMatrix transpose(int[] dimensions) {
        IMatrix2D transposed = (IMatrix2D) wrappee.transpose(dimensions);
        return new IndexTranslatorMatrix2D(dim1size, dim0size, offset1, offset0, transposed);
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

//    protected boolean equalsCheckFull(IMatrix obj) {
//        return wrappee.equalsCheckFull(obj);
//    }

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
        return marginalize(margeDims);
    }

    public IMatrixMin transposeMin(int[] dimensions) {
        return transpose(dimensions);
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

    protected class IndexTranslatorPerformer2D implements INonZeroPerformer2D {
        private final INonZeroPerformer2D wrapPerform;

        public IndexTranslatorPerformer2D(INonZeroPerformer2D wrapPerform) {
            this.wrapPerform = wrapPerform;
        }

        public void iteration(int coord0, int coord1, double val, int position) {
            wrapPerform.iteration(coord0 + offset0, coord1 + offset1, val, position);
        }
    }

    protected class IndexTranslatorPerformer1D implements INonZeroPerformer1D {
        private final INonZeroPerformer1D wrapPerform;
        private int offset;

        public IndexTranslatorPerformer1D(INonZeroPerformer1D wrapPerform, int dimension) {
            if (dimension == 0) {
                offset = offset0;
            } else {
                assert (dimension == 1);
                offset = offset1;
            }
            this.wrapPerform = wrapPerform;
        }

        public void iteration(int coord0, double val, int position) {
            wrapPerform.iteration(coord0 + offset, val, position);
        }
    }


    public void add(int coord0, int coord1, double val, int position) throws IndexOutOfBoundsException {
        add(coord0, coord1, val);
    }
}
