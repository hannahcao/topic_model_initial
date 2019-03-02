package util.matrix;

import cern.colt.list.DoubleArrayList;
import cern.colt.list.IntArrayList;

import java.io.Serializable;
import java.util.List;

/**
 * todo comment 17.11.2006
 *
 * @version $ID$
 */
public interface IMatrix extends Serializable, IMatrixMin {
    void assignZero();

    int getDimensions();

    int getDimensionSize(int dimension);

    void trimToSize();

    IMatrix marginalize(int margeDimension);

    IMatrix marginalize(int[] margeDimensions);

    IMatrix transpose(int[] dimensions);

    void set(int[] coordinates, double val) throws IndexOutOfBoundsException;

    void add(int[] coordinates, double val) throws IndexOutOfBoundsException;

    double get(int[] coordinates) throws IndexOutOfBoundsException;

    void getNonZeros(List<IntArrayList> keys, DoubleArrayList values);

    void addQuick(int[] coordinates, double val);

    void setQuick(int[] coords, double value);

    double getQuick(int[] coords);

    void doForAllNonZeros(INonZeroPerformerGeneric performer);

    double sum();

    IMatrix marginalizeTo(int[] margeDimensions, IMatrix target);

    int size();
}

