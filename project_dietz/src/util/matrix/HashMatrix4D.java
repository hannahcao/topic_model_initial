package util.matrix;

import cern.colt.list.DoubleArrayList;
import cern.colt.list.IntArrayList;

import java.util.ArrayList;

/**
 * todo comment 17.11.2006
 *
 * @version $ID$
 */
public class HashMatrix4D extends DoubleHashMatrix implements IMatrix4D, IMatrixMin4D {
    public HashMatrix4D(int dim0size, int dim1size, int dim2size, int dim3size) {
        super(new int[]{dim0size, dim1size, dim2size, dim3size});
    }

    public HashMatrix4D(int[] dimSizes) {
        super(dimSizes);
    }

    public int[] createCoords(int coord0, int coord1, int coord2, int coord3) {
        return new int[]{coord0, coord1, coord2, coord3};
    }

    public void addQuick(int coord0, int coord1, int coord2, int coord3, double value) {
        addQuick(createEntry(createCoords(coord0, coord1, coord2, coord3)), value);
    }

    public void add(int coord0, int coord1, int coord2, int coord3, double value) {
        add(createCoords(coord0, coord1, coord2, coord3), value);
    }

    public void setQuick(int coord0, int coord1, int coord2, int coord3, double value) {
        setQuick(createEntry(createCoords(coord0, coord1, coord2, coord3)), value);
    }

    public void set(int coord0, int coord1, int coord2, int coord3, double value) {
        set(createCoords(coord0, coord1, coord2, coord3), value);
    }

    public double getQuick(int coord0, int coord1, int coord2, int coord3) {
        return getQuick(createEntry(createCoords(coord0, coord1, coord2, coord3)));
    }

    public double get(int coord0, int coord1, int coord2, int coord3) {
        return get(createCoords(coord0, coord1, coord2, coord3));
    }

    public void getNonZeros(IntArrayList coordList0, IntArrayList coordList1, IntArrayList coordList2, IntArrayList coordList3, DoubleArrayList values) {
        ArrayList<IntArrayList> keys = new ArrayList<IntArrayList>(4);
        keys.add(coordList0);
        keys.add(coordList1);
        keys.add(coordList2);
        keys.add(coordList3);
        getNonZeros(keys, values);
    }

    public void doForAllNonZeros(final INonZeroPerformer4D performer) {
        doForAllNonZeros(new INonZeroPerformerGeneric() {
            public void iteration(int[] coords, double val, int position) {
                performer.iteration(coords[0], coords[1], coords[2], coords[3], val, position);
            }
        });
    }

    public void add(int coord0, int coord1, int coord2, int coord3, double val, int position) throws IndexOutOfBoundsException {
        add(coord0, coord1, coord2, coord3, val);
    }
}
