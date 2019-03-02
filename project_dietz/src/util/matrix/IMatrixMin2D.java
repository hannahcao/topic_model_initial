package util.matrix;

/**
 * todo comment 18.11.2006
 *
 * @version $ID$
 */
public interface IMatrixMin2D extends IMatrixMin {
    void add(int coord0, int coord1, double freq);

    void doForAllNonZeros(INonZeroPerformer2D performer);

    void add(int coord0, int coord1, double val, int position) throws IndexOutOfBoundsException;

}
