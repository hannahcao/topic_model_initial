package util.matrix;

import cern.colt.list.DoubleArrayList;
import cern.colt.list.IntArrayList;
import cern.colt.matrix.DoubleFactory3D;
import cern.colt.matrix.DoubleMatrix3D;

import java.util.ArrayList;
import java.util.List;

/**
 * todo comment 17.11.2006
 *
 * @version $ID$
 */
public class HashMatrix3D extends DoubleHashMatrix implements IMatrix3D, IMatrixMin3D {
    private static final long serialVersionUID = -3164129717303565907L;

    public HashMatrix3D(int dim0size, int dim1size, int dim2size) {
        super(new int[]{dim0size, dim1size, dim2size});
    }

    public HashMatrix3D(int[] dimSizes) {
        super(dimSizes);
    }

    public HashMatrix3D(IMatrix3D proto) {
        this(proto.getDimensionSize(0), proto.getDimensionSize(1), proto.getDimensionSize(2));
        proto.doForAllNonZeros(new INonZeroPerformer3D() {
            public void iteration(int coord0, int coord1, int coord2, double val, int position) {
                add(coord0, coord1, coord2, val);
            }
        });

    }

    public int[] createCoords(int coord0, int coord1, int coord2) {
        return new int[]{coord0, coord1, coord2};
    }

    public void addQuick(int coord0, int coord1, int coord2, double value) {
        addQuick(createEntry(createCoords(coord0, coord1, coord2)), value);
    }

    public void add(int coord0, int coord1, int coord2, double value) {
        add(createCoords(coord0, coord1, coord2), value);
    }

    public void setQuick(int coord0, int coord1, int coord2, double value) {
        setQuick(createEntry(createCoords(coord0, coord1, coord2)), value);
    }

    public void set(int coord0, int coord1, int coord2, double value) {
        set(createCoords(coord0, coord1, coord2), value);
    }

    public double getQuick(int coord0, int coord1, int coord2) {
        return getQuick(createEntry(createCoords(coord0, coord1, coord2)));
    }

    public double get(int coord0, int coord1, int coord2) {
        return get(createCoords(coord0, coord1, coord2));
    }

    public void getNonZeros(IntArrayList coordList0, IntArrayList coordList1, IntArrayList coordList2, DoubleArrayList values) {
        ArrayList<IntArrayList> keys = new ArrayList<IntArrayList>(3);
        keys.add(coordList0);
        keys.add(coordList1);
        keys.add(coordList2);
        getNonZeros(keys, values);
    }

    public DoubleMatrix3D createColtMatrix() {
        DoubleMatrix3D result = DoubleFactory3D.sparse.make(getDimensionSize(0), getDimensionSize(1),
                getDimensionSize(2));
        DoubleArrayList values = new DoubleArrayList();
        List<int[]> nonZeroCoordinates = getNonZeroCoordinates(values);
        for (int i = 0; i < values.size(); i++) {
            result.setQuick(nonZeroCoordinates.get(i)[0], nonZeroCoordinates.get(i)[1], nonZeroCoordinates.get(i)[2],
                    values.get(i));
        }

        return result;
    }

    public boolean equalsColt(DoubleMatrix3D matrix) {
        if (matrix.cardinality() != map.size()) {
            return false;
        } else {
            final IntArrayList matrix0 = new IntArrayList();
            final IntArrayList matrix1 = new IntArrayList();
            final IntArrayList matrix2 = new IntArrayList();
            final DoubleArrayList matrixValues = new DoubleArrayList();
            matrix.getNonZeros(matrix0, matrix1, matrix2, matrixValues);

            for (int i = 0; i < matrix0.size(); i++) {
                if (get(matrix0.get(i), matrix1.get(i), matrix2.get(i)) != matrixValues.get(i)) {
                    return false;
                }
            }
        }
        return true;
    }

    public void doForAllNonZeros(final INonZeroPerformer3D performer) {
        doForAllNonZeros(new INonZeroPerformerGeneric() {
            public void iteration(int[] coords, double val, int position) {
                performer.iteration(coords[0], coords[1], coords[2], val, position);
            }
        });

    }

    public void add(int[] coordinates, double val, int position) throws IndexOutOfBoundsException {
        add(coordinates, val);
    }

    public IMatrixMin marginalizeMin(int[] margeDims) {
        return marginalize(margeDims);
    }

    public IMatrixMin transposeMin(int[] dimensions) {
        throw new UnsupportedOperationException("to implement"); // todo implement  transposeMin
    }

    public void add(int coord0, int coord1, int coord2, double val, int position) throws IndexOutOfBoundsException {
        add(coord0, coord1, coord2, val);
    }
}
