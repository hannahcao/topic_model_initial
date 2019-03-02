package util.matrix;

/**
 * todo comment 18.11.2006
 *
 * @version $ID$
 */
public class LineMatrix4D extends LineMatrix implements IMatrixMin4D {
    public LineMatrix4D(int length) {
        super(4, length);
    }

    public LineMatrix4D(LineMatrix4D matrix) {
        super(matrix);
    }

    public void add(int coord0, int coord1, int coord2, int coord3, double freq) {
        add(new int[]{coord0, coord1, coord2, coord3}, freq);
    }

    public void add(int coord0, int coord1, int coord2, int coord3, double val, int position) throws IndexOutOfBoundsException {
        add(new int[]{coord0, coord1, coord2, coord3}, val, position);
    }


    public void doForAllNonZeros(final INonZeroPerformer4D performer) {
        doForAllNonZeros(new INonZeroPerformerGeneric() {
            public void iteration(int[] coords, double val, int position) {
                assert (coords.length == 4) : "coords.length=" + coords.length;
                performer.iteration(coords[0], coords[1], coords[2], coords[3], val, position);
            }
        });

    }


}
