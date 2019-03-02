package util.matrix;

/**
 * todo comment 18.11.2006
 *
 * @version $ID$
 */
public interface INonZeroPerformer2D extends INonZeroPerformer {
    void iteration(int coord0, int coord1, double val, int position);
}
