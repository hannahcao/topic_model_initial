package util.matrix;

/**
 * todo comment 18.11.2006
 *
 * @version $ID$
 */
public interface INonZeroPerformer4D extends INonZeroPerformer {
    void iteration(int coord0, int coord1, int coord2, int coord3, double val, int position);
}
