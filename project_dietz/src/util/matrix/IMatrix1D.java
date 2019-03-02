package util.matrix;

import cern.colt.list.DoubleArrayList;
import cern.colt.list.IntArrayList;

/**
 * todo comment 17.11.2006
 *
 * @version $ID$
 */
public interface IMatrix1D extends IMatrix {
    void add(int coord0, double value);

    void set(int coord0, double value);

    double get(int coord0);

    void getNonZeros(IntArrayList coordList0, DoubleArrayList values);

    double marginalize();

    void doForAllNonZeros(INonZeroPerformer1D performer);
}
