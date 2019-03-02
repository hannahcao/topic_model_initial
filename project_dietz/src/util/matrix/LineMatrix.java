package util.matrix;

import cao.Debugger;
import cern.colt.list.DoubleArrayList;
import cern.colt.list.IntArrayList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * todo comment 18.11.2006
 *
 * @version $ID$
 */
public class LineMatrix implements IMatrixMin {
    protected int[][] coords;
    protected double[] vals;
    private int dimensions;
    private int length;
    private int endPosition = 0;

    public LineMatrix(int dimensions, int length) {
        this.dimensions = dimensions;
        this.length = length;
        coords = new int[dimensions][length];
        vals = new double[length];
        assignZero();
    }

    /**
     * copy constructor
     *
     * @param matrix
     */
    public LineMatrix(LineMatrix matrix) {
        dimensions = matrix.dimensions;
        length = matrix.length;
        setEndPosition(matrix.getEndPosition());
        coords = new int[dimensions][length];
        vals = new double[length];
        System.arraycopy(matrix.vals, 0, vals, 0, length);
        for (int d = 0; d < dimensions; d++) {
            System.arraycopy(matrix.coords[d], 0, coords[d], 0, length);
        }
    }

    public INonZeroPerformerGeneric fillingPerformer() {
        return new INonZeroPerformerGeneric() {
            public void iteration(int[] coords, double val, int position) {
                assert (val >= 0.0);
                for (int f = 0; f < val; f++) {
                    add(coords, 1.0);
                }
            }
        };
    }

    public void enlargeBy(int additionalLength) {
        length += additionalLength;
        vals = cern.colt.Arrays.ensureCapacity(vals, length);
        for (int d = 0; d < dimensions; d++) {
            coords[d] = cern.colt.Arrays.ensureCapacity(coords[d], length);
        }

    }


    public void assignZero() {
        for (int d = 0; d < dimensions; d++) {
            Arrays.fill(coords[d], 0);
        }
        Arrays.fill(vals, 0.0);
        setEndPosition(0);
    }

    public int[] getCoordArray(int dimension) {
        return coords[dimension];
    }

    public double[] getValsArray() {
        return vals;
    }

    public double get(int[] coordinates, int position) throws IndexOutOfBoundsException {
        for (int d = 0; d < dimensions; d++) {
            if (coords[d][position] != coordinates[d])
                throw new RuntimeException("dimension " + d + ": token at position " + position + " was expected to be " + coordinates[d] + " but was " + coords[d][position] + " (coords = " + Arrays.toString(coordinates) + ")");
        }
        return vals[position];
    }

    public void add(int[] coordinates, double val, int position) throws IndexOutOfBoundsException {
        if (vals[position] == 0.0) {//reuse the space when the frequency becomes 0
            for (int d = 0; d < dimensions; d++) {
                coords[d][position] = coordinates[d];
            }
            vals[position] = val;
            if (getEndPosition() <= position) setEndPosition(position + 1);
        } else {
            for (int d = 0; d < dimensions; d++) {//make sure the index is correct
                if (coords[d][position] != coordinates[d]) {
                    throw new RuntimeException("dimension " + d + ": token at position " + position + " was expected to be " + coordinates[d] + " but was " + coords[d][position] + " (coords = " + Arrays.toString(coordinates) + " val=" + vals[position] + ")");
                }
            }
            vals[position] += val;//then add the new frequency
            if (getEndPosition() <= position) setEndPosition(position + 1);
        }
        assert (vals[position] >= 0.0) : position + " " + Arrays.toString(coordinates);

    }

    public void set(int[] coordinates, double val, int position) throws IndexOutOfBoundsException {
        for (int d = 0; d < dimensions; d++) {
            if (coords[d][position] != coordinates[d]) {
                throw new RuntimeException("dimension " + d + ": token at position " + position + " was expected to be " + coordinates[d] + " but was " + coords[d][position] + " (coords = " + Arrays.toString(coordinates) + ")");
            }
        }
        vals[position] = val;
        if (getEndPosition() <= position) setEndPosition(position + 1);

    }

    public void add(int[] coordinates, double val) throws IndexOutOfBoundsException {
        if (getEndPosition() >= length) {
            enlargeBy(1000);
//            throw new RuntimeException("This matrix is full. (Exceeding length " + length + " during add)");
        }
        if (val >= 0.0) {
            for (int d = 0; d < dimensions; d++) {
                coords[d][getEndPosition()] = coordinates[d];
            }
            vals[getEndPosition()] = val;
            setEndPosition(getEndPosition() + 1);
        } else {
            //  for negative vals: find entry in line and remove
            // this implementation assumes, that only positive vals are contained in the list.
            List<Integer> list = positionsFor(coordinates);
            double toRemove = -val;
            for (int i : list) {
                if (toRemove > 0.0) {
                    if (vals[i] > toRemove) {
                        add(coordinates, -toRemove, i);
                        toRemove -= vals[i];
                        break;
                    } else {
                        add(coordinates, -vals[i], i);
                        toRemove -= vals[i];
                    }
                }
            }

        }
    }


    public int getDimensions() {
        return dimensions;
    }

    public IMatrix toMatrix(final IMatrix instance) {
        if (instance.getDimensions() != getDimensions()) throw new RuntimeException("dimensions do not match");
        int[] maxCoords = getMaxCoords();
        for (int d = 0; d < getDimensions(); d++) {
            if (maxCoords[d] >= instance.getDimensionSize(d))
                throw new RuntimeException("maxCoords[" + d + "]=" + maxCoords[d] + "(" + Arrays.toString(maxCoords) + ") exceeds target matrix dimension " + instance.getDimensionSize(d));
        }
        doForAllNonZeros(new INonZeroPerformerGeneric() {
            public void iteration(int[] coords, double val, int position) {
                instance.add(coords, val);
            }
        });
        return instance;
    }

    public int getDimensionSize(int dimension) {
        return length;
    }

    public void trimToSize() {

    }

    public void trimToSizeX() {
        final int[] maxCoords = getMaxCoords();
        IMatrix allTokens = toMatrix(new DoubleHashMatrix(maxCoords, 0.0));
        assignZero();
        allTokens.doForAllNonZeros(new INonZeroPerformerGeneric() {
            public void iteration(int[] coords, double val, int position) {
                add(coords, val);
            }
        });
    }

    public int[] getMaxCoords() {
        final int[] maxCoords = new int[dimensions];
        doForAllNonZeros(new INonZeroPerformerGeneric() {
            public void iteration(int[] coords, double val, int position) {
                for (int d = 0; d < dimensions; d++) {
                    if (maxCoords[d] < coords[d]) {
                        maxCoords[d] = coords[d];
                    }
                }
            }
        });
        return maxCoords;
    }

    public IMatrixMin marginalizeMin(int[] margeDimensions) {
        int newDims = dimensions - margeDimensions.length;
        if (newDims < 1) {
            throw new RuntimeException("Matrix with dimension " + dimensions + " can not be marginalized by " + margeDimensions.length + " dimensions. margedims = " + margeDimensions);
        }
        int[] copyDims = new int[newDims];
        int i = 0;
        for (int d = 0; d < dimensions; d++) {
            boolean margeThis = true;
            for (int m : margeDimensions) {
                if (m == d) {
                    margeThis = false;
                    break;
                }
            }
            if (margeThis) {
                copyDims[i] = d;
                i++;
            }

        }
        return copyDimensions(copyDims);

    }

    private LineMatrix copyDimensions(int[] dims) {
        LineMatrix marged = new LineMatrix(dims.length, length);
        int count = 0;
        for (int d : dims) {
            marged.coords[count] = new int[length];
            System.arraycopy(coords[d], 0, marged.coords[count], 0, length);
            count++;
        }
        System.arraycopy(vals, 0, marged.vals, 0, length);
        marged.setEndPosition(getEndPosition());

        return marged;
    }

    public IMatrixMin marginalize(int margeDimensions) {
        return marginalizeMin(new int[]{margeDimensions});
    }

    public IMatrixMin transposeMin(int[] dimensions) {
        return copyDimensions(dimensions);
    }

    public void doForAllNonZeros(INonZeroPerformerGeneric performer) {
    	//System.out.println(Debugger.getCallerPosition()+"LineMatrix::doForAllNonZeros...dimensions="+dimensions);
        int[] coordinates = new int[dimensions];
        for (int index = 0; index < getEndPosition(); index++) {
            for (int d = 0; d < dimensions; d++) {
                coordinates[d] = coords[d][index];
                //System.out.println("d="+d+",index="+index+",coords[d][index]="+coords[d][index]);
            }
            //System.out.println("index="+index+",vals[index]="+vals[index]+",vals.length="+vals.length);
            if (vals[index] != 0.0) {
                performer.iteration(coordinates, vals[index], index); //this is implemented in LineMatrix5D
            }
//            System.err.println("LineMatrix.doForAllNonZeros: coordinates "+Arrays.toString(coordinates)+" with val 0.0 at position "+index);
            
            
        }
    }

    public void getNonZeros(List<IntArrayList> keys, DoubleArrayList values) {
        if (keys.size() != dimensions) {
            throw new RuntimeException("dimension of keys (" + dimensions + ") does not match matrix dimension " + dimensions);
        }
        for (int d = 0; d < dimensions; d++) {
            keys.get(d).addAllOf(new IntArrayList(coords[d]));
        }
        values.addAllOf(new DoubleArrayList(vals));
    }

    public void addQuick(int[] coordinates, double val) {
        add(coordinates, val);
    }

    /**
     * Returns a list of positions, where values for the given coordinates are defined.
     *
     * @param coordinates
     * @return
     */
    public List<Integer> positionsFor(final int[] coordinates) {
        if ((coordinates.length != dimensions)) {
            throw new IllegalArgumentException("length of coordinate array does not match dimension. coords=" + coordinates.length + " (" + Arrays.toString(coordinates) + ") dimensions=" + dimensions);
        }
        final List<Integer> result = new ArrayList<Integer>();
        doForAllNonZeros(new INonZeroPerformerGeneric() {
            public void iteration(int[] coords, double val, int position) {
                assert (coordinates.length == dimensions) : "length of coordinate array does not match dimension. coords=" + coords.length + " (" + coords + ") dimensions=" + dimensions;
                for (int d = 0; d < dimensions; d++) {
                    if (coordinates[d] != coords[d]) {
                        return;
                    }
                }
                result.add(position);
            }
        });
        return result;
    }

    public int sum() {
        return getEndPosition();
    }

    public void clearPosition(int position) {
        vals[position] = 0.0;
    }

    public int getEndPosition() {
        return endPosition;
    }

    void setEndPosition(int endPosition) {
        this.endPosition = endPosition;
    }
}
