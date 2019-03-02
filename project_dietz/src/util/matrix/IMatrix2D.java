package util.matrix;

import cern.colt.list.DoubleArrayList;
import cern.colt.list.IntArrayList;

/**
 * todo comment 17.11.2006
 *
 * @version $ID$
 */
public interface IMatrix2D extends IMatrix, IMatrixMin2D {
    void add(int coord0, int coord1, double value);

    void set(int coord0, int coord1, double value);

    double get(int coord0, int coord1);

    void getNonZeros(IntArrayList coordList0, IntArrayList coordList1, DoubleArrayList values);

    void doForAllNonZeros(INonZeroPerformer2D performer);

    void doForAllDim0NonZeros(int coord0, INonZeroPerformer1D performer);

    void doForAllDim1NonZeros(int coord1, INonZeroPerformer1D performer);
}
