package util.matrix;

/**
 * todo comment 18.11.2006
 *
 * @version $ID$
 */
public class LineMatrix2D extends LineMatrix implements IMatrixMin2D {
    public LineMatrix2D(int length) {
        super(2, length);
    }

    public void add(int coord0, int coord1, double freq) {
        add(new int[]{coord0, coord1}, freq);
    }

    public void add(int coord0, int coord1, double val, int position) throws IndexOutOfBoundsException {
        add(new int[]{coord0, coord1}, val, position);
    }


    public void doForAllNonZeros(final INonZeroPerformer2D performer) {
        doForAllNonZeros(new INonZeroPerformerGeneric() {
            public void iteration(int[] coords, double val, int position) {
                performer.iteration(coords[0], coords[1], val, position);
            }
        });

    }


}
