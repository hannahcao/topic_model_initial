package util.matrix;

/**
 * todo comment 18.11.2006
 *
 * @version $ID$
 */
public interface IMatrixMin5D extends IMatrixMin {
    void add(int coord0, int coord1, int coord2, int coord3, int coord4, double freq);

    void add(int coord0, int coord1, int coord2, int coord3, int coord4, double val, int position) throws IndexOutOfBoundsException;

    void doForAllNonZeros(INonZeroPerformer5D performer);

}
