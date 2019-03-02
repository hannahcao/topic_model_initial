package util.matrix;

/**
 * todo comment 18.11.2006
 *
 * @version $ID$
 */
public interface IMatrixMin3D extends IMatrixMin {
    void add(int coord0, int coord1, int coord2, double freq);

    void doForAllNonZeros(INonZeroPerformer3D performer);

    void add(int coord0, int coord1, int coord2, double val, int position) throws IndexOutOfBoundsException;

}
