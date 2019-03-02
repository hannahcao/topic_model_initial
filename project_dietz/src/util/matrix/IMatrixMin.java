package util.matrix;

import java.io.Serializable;

/**
 * todo comment 18.11.2006
 *
 * @version $ID$
 */
public interface IMatrixMin extends Serializable {
    void add(int[] coords, double freq);

    void doForAllNonZeros(INonZeroPerformerGeneric performer);

    void add(int[] coordinates, double val, int position) throws IndexOutOfBoundsException;

    IMatrixMin marginalizeMin(int[] margeDims);

    IMatrixMin transposeMin(int[] dimensions);

    int getDimensionSize(int dim);

    int getDimensions();

    /**
     * Converts a IMatrixMin to a IMatrix type. Note that the underlying implementation may choose to either use
     * the instance passed in or may create a completly net instance. It is even allowed to just return "this".
     *
     * @param instance
     * @return not null.
     */
    IMatrix toMatrix(IMatrix instance);

    void set(int[] coordinates, double val, int position) throws IndexOutOfBoundsException;

    double get(int[] coordinates, int position) throws IndexOutOfBoundsException;
}
