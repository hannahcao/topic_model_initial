package util.matrix;

import cao.Debugger;

/**
 * todo comment 18.11.2006
 *
 * @version $ID$
 */
public class LineMatrix5D extends LineMatrix implements IMatrixMin5D {
    public LineMatrix5D(int length) {
        super(5, length);
    }

    public void add(int coord0, int coord1, int coord2, int coord3, int coord4, double freq) {
        add(new int[]{coord0, coord1, coord2, coord3, coord4}, freq);
    }

    public void add(int coord0, int coord1, int coord2, int coord3, int coord4, double val, int position) throws IndexOutOfBoundsException {
        add(new int[]{coord0, coord1, coord2, coord3, coord4}, val, position);
    }


    public void doForAllNonZeros(final INonZeroPerformer5D performer) {
    	//System.out.println(Debugger.getCallerPosition()+"LineMatrix5D::doForAllNonZeros...");
    	//before running this, LineMatrix.doForAllNonZeros is executed first
    	//performer.iteration will execute 
        doForAllNonZeros(new INonZeroPerformerGeneric() {
            public void iteration(int[] coords, double val, int position) {
                assert (coords.length == 5) : "coords.length=" + coords.length;
                //execute the iteration function implemented and embedded in major logic functions 
                performer.iteration(coords[0], coords[1], coords[2], coords[3], coords[4], val, position);
            }
        });

    }


}
