package util.matrix;

/**
 * todo comment 18.11.2006
 *
 * @version $ID$
 */
public class LineMatrix3D extends LineMatrix implements IMatrixMin3D {
    public LineMatrix3D(int length) {
        super(3, length);
    }

    public void add(int coord0, int coord1, int coord2, double freq) {
        add(new int[]{coord0, coord1, coord2}, freq);
    }

    public void add(int coord0, int coord1, int coord2, double val, int position) throws IndexOutOfBoundsException {
        add(new int[]{coord0, coord1, coord2}, val, position);
    }


    public void doForAllNonZeros(final INonZeroPerformer3D performer) {
        doForAllNonZeros(new INonZeroPerformerGeneric() {
            public void iteration(int[] coords, double val, int position) {
                performer.iteration(coords[0], coords[1], coords[2], val, position);
            }
        });

    }


}
