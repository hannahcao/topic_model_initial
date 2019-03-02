package util.matrix;

/**
 * todo comment 18.11.2006
 *
 * @version $ID$
 */
public interface IMatrixMin4D extends IMatrixMin {
    void add(int coord0, int coord1, int coord2, int coord3, double freq);

    void doForAllNonZeros(INonZeroPerformer4D performer);

    void add(int coord0, int coord1, int coord2, int coord3, double val, int position) throws IndexOutOfBoundsException;

}
