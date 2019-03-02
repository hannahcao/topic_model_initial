package util.matrix;

import cern.colt.list.DoubleArrayList;
import cern.colt.list.IntArrayList;
import cern.colt.matrix.DoubleFactory1D;
import cern.colt.matrix.DoubleMatrix1D;

import java.util.ArrayList;
import java.util.List;

/**
 * todo comment 17.11.2006
 *
 * @version $ID$
 */
public class HashMatrix1D extends DoubleHashMatrix implements IMatrix1D {

    public HashMatrix1D(int dim0size) {
        super(new int[]{dim0size});
    }

    public HashMatrix1D(int[] dimSizes) {
        super(dimSizes);
    }

    public int[] createCoords(int coord0) {
        return new int[]{coord0};
    }

    public void addQuick(int coord0, double value) {
        addQuick(createEntry(createCoords(coord0)), value);
    }

    public void add(int coord0, double value) {
        add(createCoords(coord0), value);
    }

    public void setQuick(int coord0, double value) {
        setQuick(createEntry(createCoords(coord0)), value);
    }

    public void set(int coord0, double value) {
        set(createCoords(coord0), value);
    }

    public double getQuick(int coord0) {
        return getQuick(createEntry(createCoords(coord0)));
    }

    public double get(int coord0) {
        return get(createCoords(coord0));
    }


    public void getNonZeros(IntArrayList coordList0, DoubleArrayList values) {
        ArrayList<IntArrayList> keys = new ArrayList<IntArrayList>(1);
        keys.add(coordList0);
        getNonZeros(keys, values);
    }

    public DoubleMatrix1D createColtMatrix() {
        DoubleMatrix1D result = DoubleFactory1D.sparse.make(getDimensionSize(0));
        DoubleArrayList values = new DoubleArrayList();
        List<int[]> nonZeroCoordinates = getNonZeroCoordinates(values);
        for (int i = 0; i < nonZeroCoordinates.size(); i++) {
            result.setQuick(nonZeroCoordinates.get(i)[0], values.get(i));
        }
        return result;
    }

    public boolean equalsColt(DoubleMatrix1D matrix) {
        if (matrix.cardinality() != map.size()) {
            return false;
        } else {
            final IntArrayList matrix0 = new IntArrayList();
            final DoubleArrayList matrixValues = new DoubleArrayList();
            matrix.getNonZeros(matrix0, matrixValues);

            for (int i = 0; i < matrix0.size(); i++) {
                if (get(matrix0.get(i)) != matrixValues.get(i)) {
                    return false;
                }
            }
        }
        return true;
    }


    public double marginalize() {
        double result = 0;
        DoubleArrayList values = new DoubleArrayList();
        getNonZeroCoordinates(values);
        for (int i = 0; i < values.size(); i++) {
            result += values.get(i);
        }
        return result;
    }

    protected boolean equalsCheckFull(IMatrix obj) {
        if (!(obj instanceof IMatrix1D)) {
            return false;
        }
        IMatrix1D mat = (IMatrix1D) obj;
        int[] entry = new int[]{0};
        for (int i = 0; i < getDimensionSize(0); i++) {
            entry[0] = i;
            if (getQuick(entry) != mat.getQuick(entry)) return false;
        }
        return true;
    }


    public void doForAllNonZeros(final INonZeroPerformer1D performer) {
        doForAllNonZeros(new INonZeroPerformerGeneric() {
            public void iteration(int[] coords, double val, int position) {
                performer.iteration(coords[0], val, position);
            }
        });
    }

}
