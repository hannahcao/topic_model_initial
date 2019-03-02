package util.matrix;

import cern.colt.list.DoubleArrayList;
import cern.colt.list.IntArrayList;
import cern.colt.matrix.DoubleFactory2D;
import cern.colt.matrix.DoubleMatrix2D;

import java.util.ArrayList;
import java.util.List;

/**
 * todo comment 17.11.2006
 *
 * @version $ID$
 */
public class HashMatrix2D extends DoubleHashMatrix implements IMatrix2D {

    public HashMatrix2D(int dim0size, int dim1size) {
        super(new int[]{dim0size, dim1size});
    }

    public HashMatrix2D(int dim0size, int dim1size, double ZEROVAL) {
        super(new int[]{dim0size, dim1size}, ZEROVAL);
    }

    public HashMatrix2D(int[] dimSizes) {
        super(dimSizes);
    }

    public HashMatrix2D(int[] dimSizes, double ZEROVAL) {
        super(dimSizes, ZEROVAL);
    }

    public int[] createCoords(int coord0, int coord1) {
        return new int[]{coord0, coord1};
    }

    public void addQuick(int coord0, int coord1, double value) {
        addQuick(createEntry(createCoords(coord0, coord1)), value);
    }

    public void add(int coord0, int coord1, double value) {
        add(createCoords(coord0, coord1), value);
    }

    public void setQuick(int coord0, int coord1, double value) {
        setQuick(createEntry(createCoords(coord0, coord1)), value);
    }

    public void set(int coord0, int coord1, double value) {
        set(createCoords(coord0, coord1), value);
    }

    public double getQuick(int coord0, int coord1) {
        return getQuick(createEntry(createCoords(coord0, coord1)));
    }

    public double get(int coord0, int coord1) {
        return get(createCoords(coord0, coord1));
    }

    public void getNonZeros(IntArrayList coordList0, IntArrayList coordList1, DoubleArrayList values) {
        ArrayList<IntArrayList> keys = new ArrayList<IntArrayList>(2);
        keys.add(coordList0);
        keys.add(coordList1);
        getNonZeros(keys, values);
    }


    public DoubleMatrix2D createColtMatrix() {
        DoubleMatrix2D result = DoubleFactory2D.sparse.make(getDimensionSize(0), getDimensionSize(1));
        DoubleArrayList values = new DoubleArrayList();
        List<int[]> nonZeroCoordinates = getNonZeroCoordinates(values);
        for (int i = 0; i < nonZeroCoordinates.size(); i++) {
            result.setQuick(nonZeroCoordinates.get(i)[0], nonZeroCoordinates.get(i)[1], values.get(i));
        }

        return result;
    }


    public boolean equalsColt(DoubleMatrix2D matrix) {
        if (matrix.cardinality() != map.size()) {
            return false;
        } else {
            final IntArrayList matrix0 = new IntArrayList();
            final IntArrayList matrix1 = new IntArrayList();
            final DoubleArrayList matrixValues = new DoubleArrayList();
            matrix.getNonZeros(matrix0, matrix1, matrixValues);

            for (int i = 0; i < matrix0.size(); i++) {
                if (get(matrix0.get(i), matrix1.get(i)) != matrixValues.get(i)) {
                    return false;
                }
            }
        }
        return true;
    }

    protected boolean equalsCheckFull(IMatrix obj) {
        if (obj == null) return false;
        if (!(obj instanceof IMatrix2D)) {
            return false;
        }
        IMatrix2D mat = (IMatrix2D) obj;
        int[] entry = new int[]{0, 0};
        for (int i = 0; i < getDimensionSize(0); i++) {
            entry[0] = i;
            for (int j = 0; j < getDimensionSize(1); j++) {
                entry[1] = j;
                if (getQuick(entry) != mat.getQuick(entry)) return false;
            }
        }
        return true;
    }

    public void doForAllNonZeros(final INonZeroPerformer2D performer) {
        doForAllNonZeros(new INonZeroPerformerGeneric() {
            public void iteration(int[] coords, double val, int position) {
                performer.iteration(coords[0], coords[1], val, position);
            }
        });

    }

    public void doForAllDim0NonZeros(int coord0, INonZeroPerformer1D performer) {
        throw new UnsupportedOperationException("not implemented");
    }

    public void doForAllDim1NonZeros(int coord1, INonZeroPerformer1D performer) {
        throw new UnsupportedOperationException("not implemented");
    }


    public void add(int coord0, int coord1, double val, int position) throws IndexOutOfBoundsException {
        add(coord0, coord1, val);
    }
}
