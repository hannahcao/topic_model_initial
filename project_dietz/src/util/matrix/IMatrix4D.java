package util.matrix;

/**
 * todo comment 17.11.2006
 *
 * @version $ID$
 */
public interface IMatrix4D extends IMatrix {
    void add(int coord0, int coord1, int coord2, int coord3, double value);

    void set(int coord0, int coord1, int coord2, int coord3, double value);

    double get(int coord0, int coord1, int coord2, int coord3);

    void doForAllNonZeros(INonZeroPerformer4D performer);

}
