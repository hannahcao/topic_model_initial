package util.matrix;

import cern.colt.list.DoubleArrayList;
import cern.colt.list.IntArrayList;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.DoubleMatrix3D;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DoubleHashMatrix implements IMatrix, IMatrixMin {
    private static final long serialVersionUID = 875070467780700594L;
    protected HashMap<HashEntry, Double> map;
    private final HashEntry.Factory entryFactory;
    private final double ZEROVAL;
    private boolean writeProtected = false;

    // ############################################
    // ## Initialization
    // ############################################

    public DoubleHashMatrix(int[] dimSizes) {
        map = new HashMap<HashEntry, Double>(getInitial(dimSizes), getLoad());
        this.ZEROVAL = 0.0;
        entryFactory = new HashEntry.Factory(dimSizes);
    }

    private float getLoad() {
        return 0.75f;
    }

    public int size() {
        return map.size();
    }

    private int getInitial(int[] dimSizes) {
        int maxSize = 1;
        for (int dim : dimSizes) {
            maxSize *= dim;
        }

        int initial = (int) Math.ceil(maxSize / Math.pow(2, (double) dimSizes.length));
        return Math.max(initial, 100);
    }

    public DoubleHashMatrix(int[] dimSizes, double ZEROVAL) {
        map = new HashMap<HashEntry, Double>(getInitial(dimSizes), getLoad());
        this.ZEROVAL = ZEROVAL;
        entryFactory = new HashEntry.Factory(dimSizes);
    }

    public void assignZero() {
        assert (!writeProtected) : " write protection was enabled";
        map.clear();
    }

    /**
     * to be used with the quick variantes ({@link #getQuick(HashEntry)}, {@link #setQuick(HashEntry,double)}, ..)
     *
     * @param coordinates
     * @return the entry. != null
     */
    public HashEntry createEntry(int[] coordinates) {
        return entryFactory.createEntry(coordinates);
    }

    // ############################################
    // ## Sizes..
    // ############################################

    public int getDimensions() {
        return entryFactory.getDimensions();
    }

    public IMatrix toMatrix(IMatrix copyTo) {
        return this;
    }

    public int getDimensionSize(int dimension) {
        return entryFactory.getDimensionSize(dimension);
    }

    public void trimToSize() {
    }

    // ############################################
    // ## Safe methods
    // ############################################

    protected void checkEntry(int[] coordinates) throws IndexOutOfBoundsException {
        int violatedDimension = entryFactory.violatesDimensionSizes(coordinates);
        if (violatedDimension > -1) {
            throw new IndexOutOfBoundsException(
                    "In Dimension " + violatedDimension + " the given coordinates " + getSingleCoordinate(coordinates,
                            violatedDimension) + " violate the Dimensionsize " + entryFactory.getDimensionSize(
                            violatedDimension) + " Coordinates = " + Arrays.toString(coordinates));
        }
    }

    private int getSingleCoordinate(int[] coordinates, int dim) {
        if (dim > coordinates.length) {
            return -1;
        }
        return coordinates[dim];
    }

    public void set(int[] coordinates, double val) throws IndexOutOfBoundsException {
        checkEntry(coordinates);
        setQuick(createEntry(coordinates), val);
    }

    public void add(int[] coordinates, double val) throws IndexOutOfBoundsException {
        checkEntry(coordinates);
        addQuick(createEntry(coordinates), val);
    }

    public double get(int[] coordinates) throws IndexOutOfBoundsException {
        checkEntry(coordinates);
        return getQuick(createEntry(coordinates));
    }

    public boolean contains(int[] coordinates) throws IndexOutOfBoundsException {
        checkEntry(coordinates);
        return containsQuick(createEntry(coordinates));
    }

    // ############################################
    // ## Quick methods
    // ############################################

    /**
     * Sets the value without checking the validity of the entry. Create the entry via {@link #createEntry(int[])}.
     *
     * @param entry != null
     * @param val
     */
    public void setQuick(HashEntry entry, double val) {
        assert (!writeProtected) : " write protection was enabled";
        if (isZero(val)) {
            map.put(entry, val);
        } else {
            map.remove(entry);
        }

    }

    /**
     * Condition on which entries will be removed from the matrix.
     * <p/>
     * This implementation checks for exact equality with {@link #ZEROVAL}.
     * <p/>
     * If you also want to remove values that are in a given environment among {@link #ZEROVAL} overwrite this method.
     *
     * @param val value to be checked
     * @return true if the entry should be removed.
     */
    protected boolean isZero(double val) {
        return val != ZEROVAL;
    }

    /**
     * Adds the value without checking the validity of the entry. Create the entry via {@link #createEntry(int[])}.
     *
     * @param entry != null
     * @param val
     */
    public void addQuick(HashEntry entry, double val) {
        assert (!writeProtected) : " write protection was enabled";
        double oldVal = getQuick(entry);
        map.put(entry, val + oldVal);
    }

    /**
     * Adds the value without checking the validity of the entry. Create the entry via {@link #createEntry(int[])}.
     *
     * @param val
     */
    public void addQuick(int[] coordinates, double val) {
        assert (!writeProtected) : " write protection was enabled";
        HashEntry entry = createEntry(coordinates);
        double oldVal = getQuick(entry);
        map.put(entry, val + oldVal);
    }

    public void setQuick(int[] coordinates, double val) {
        assert (!writeProtected) : " write protection was enabled";
        HashEntry entry = createEntry(coordinates);
        map.put(entry, val);
    }

    public double getQuick(int[] coordinates) {
        assert (!writeProtected) : " write protection was enabled";
        HashEntry entry = createEntry(coordinates);
        final Double val = map.get(entry);
        if (val == null) {
            return ZEROVAL;
        }
        return val;
    }

    /**
     * Returns the value without checking the validity of the entry. Create the entry via {@link #createEntry(int[])}.
     *
     * @param entry != null
     */
    public double getQuick(HashEntry entry) {
        Double result = map.get(entry);
        if (result == null) {
            return ZEROVAL;
        } else {
            return result;
        }
    }

    /**
     * Checks the containment of the entry in the nonzeros of the matrix without checking the validity of the entry. Create the entry via {@link #createEntry(int[])}.
     *
     * @param entry != null
     */
    public boolean containsQuick(HashEntry entry) {
        return map.containsKey(entry);
    }

    // ############################################
    // ## write protection
    // ############################################

    public boolean isWriteProtected() {
        return writeProtected;
    }

    /**
     * Provides a way to mark a matrix as write protected - if assertions are enabled (vm parameter -ea) .
     * If a modifying operation is called while writeProtected is
     * set to true, an assert will throw an {@link junit.framework.AssertionFailedError}.
     *
     * @param writeProtected true for enable write protection.
     */
    public void setWriteProtected(boolean writeProtected) {
        this.writeProtected = writeProtected;
    }

    // ############################################
    // ## NonZeros
    // ############################################

    public List<IntArrayList> createIntArrayListList() {
        List<IntArrayList> result = new ArrayList<IntArrayList>(entryFactory.getDimensions());
        for (int dim = 0; dim < entryFactory.getDimensions(); dim++) {
            result.add(new IntArrayList(map.size()));
        }
        return result;
    }

    /**
     * Get all nonzero entry/value pairs. To access, enter empty (but initialized) datastructures als parameters, they
     * will be filled by this method.
     * <p/>
     * Example:
     * <pre>
     * List<IntArrayList> keys = matrix.createIntArrayListList();
     * keys.set(1, null)  // we are not interested in keys of the secound dimension
     * DoubleArrayList values = new DoubleArrayList();
     * matrix.getNonZeros(keys, values);
     * for(int i=0; i&lt;values.size(); i++){
     *    System.out.println("dim1 = "+keys.get(0).get(i)+" dim3 = "+keys.get(2).get(i)+" value "+values.get(i));
     * }
     * </pre>
     *
     * @param keys   to create an appropriate datastructure for this, use {@link #createIntArrayListList()}
     * @param values array where the values are stored in.
     */
    public void getNonZeros(List<IntArrayList> keys, DoubleArrayList values) {
        entryFactory.getDimensions();
        for (Map.Entry<HashEntry, Double> entry : map.entrySet()) {
            HashEntry key = entry.getKey();
            for (int dim = 0; dim < entryFactory.getDimensions(); dim++) {
                if (keys.get(dim) != null) {
                    keys.get(dim).add(key.get(dim));
                }
            }
            if (values != null) {
                values.add(entry.getValue());
            }
        }
    }

    /**
     * Returns a list of the nonzero coordinates and fills the given values array if not null.
     * the returned list will have the same size as the values.
     *
     * @param values will be filled (like in {@link #getNonZeros} if !=null.
     * @return List of coordinate sets in int[] format.
     */
    public List<int[]> getNonZeroCoordinates(DoubleArrayList values) {
        List<IntArrayList> keys = createIntArrayListList();
        getNonZeros(keys, values);
        assert (values.size() == map.size()) : values.size() + "==" + map.size();
        int number = values.size();

        List<int[]> result = new ArrayList<int[]>(number);

        for (int i = 0; i < number; i++) {
            int[] coordinates = new int[entryFactory.getDimensions()];
            for (int dim = 0; dim < entryFactory.getDimensions(); dim++) {
                coordinates[dim] = keys.get(dim).get(i);
            }
            result.add(coordinates);
        }


        assert (result.size() == number) : result.size() + "==" + number;
        assert (map.size() == number) : map.size() + "==" + number;

        return result;
    }

    // ############################################
    // ## Projections
    // ############################################

    public IMatrix marginalizeTo(final int[] margeDimensions, final IMatrix target) {
        doForAllNonZeros(new INonZeroPerformerGeneric() {
            public void iteration(int[] coords, double val, int position) {
                int[] targetCoords = new int[getDimensions() - margeDimensions.length];
                int c = 0;
                for (int i = 0; i < coords.length; i++) {
                    if (!contains(margeDimensions, i)) {
                        targetCoords[c] = coords[i];
                        c++;
                    }
                }
                target.add(targetCoords, val);
            }
        });


        int[] otherDimensions = createOtherDimensions(margeDimensions);
        int[] otherDimSizes = new int[otherDimensions.length];
        for (int i = 0; i < otherDimensions.length; i++) {
            otherDimSizes[i] = getDimensionSize(otherDimensions[i]);
        }


        IMatrix result = createMatrix(otherDimSizes);
        List<IntArrayList> keys = createIntArrayListList();
        // leave out memory for dims we are not interested in
        for (int margeDim : margeDimensions) {
            keys.set(margeDim, null);
        }
        DoubleArrayList values = new DoubleArrayList();
        getNonZeros(keys, values);

        for (int index = 0; index < values.size(); index++) {
            int[] coordinates = new int[otherDimensions.length];
            for (int i = 0; i < otherDimensions.length; i++) {
                coordinates[i] = keys.get(otherDimensions[i]).get(index);
            }
            result.addQuick(coordinates, values.get(index));
        }
        return result;

    }

    private boolean contains(int[] margeDimensions, int i) {
        for (int dim : margeDimensions) {
            if (dim == i) {
                return true;
            }
        }
        return false;
    }

    /**
     * Projects this matrix to a lower dimensional matrix, where the given margeDimension is marginalized (i.e. summed over)
     * <p/>
     * If the returned matrix has dimensions for which a concrete class implementation exits, the concrete class
     * will be returned (you can access it via cast).
     *
     * @param margeDimension valid dimension of this matrix
     * @return Matrix of the same sizes as the original matrix, but the margeDimension is missing (one Dimension reduced).
     */
    public IMatrix marginalize(int margeDimension) {
        return marginalize(new int[]{margeDimension});

    }

    /**
     * Projects this matrix to a lower dimensional matrix, where the given margeDimensions is marginalized (i.e. summed over)
     * <p/>
     * If the returned matrix has dimensions for which a concrete class implementation exits, the concrete class
     * will be returned (you can access it via cast).
     *
     * @param margeDimensions valid dimension of this matrix
     * @return Matrix of the same sizes as the original matrix, but the margeDimensions is missing (one Dimension reduced).
     */
    public IMatrix marginalize(int[] margeDimensions) {
        int[] otherDimensions = createOtherDimensions(margeDimensions);
        int[] otherDimSizes = new int[otherDimensions.length];
        for (int i = 0; i < otherDimensions.length; i++) {
            otherDimSizes[i] = getDimensionSize(otherDimensions[i]);
        }


        IMatrix result = createMatrix(otherDimSizes);
        List<IntArrayList> keys = createIntArrayListList();
        // leave out memory for dims we are not interested in
        for (int margeDim : margeDimensions) {
            keys.set(margeDim, null);
        }
        DoubleArrayList values = new DoubleArrayList();
        getNonZeros(keys, values);

        for (int index = 0; index < values.size(); index++) {
            int[] coordinates = new int[otherDimensions.length];
            for (int i = 0; i < otherDimensions.length; i++) {
                coordinates[i] = keys.get(otherDimensions[i]).get(index);
            }
            result.addQuick(coordinates, values.get(index));
        }
        return result;

    }

    public IMatrix transpose(int[] dimensions) {
        assert (dimensions.length == getDimensions()) : "dimension sized must match";

        final int[] dimSizes = new int[getDimensions()];
        for (int d = 0; d < dimensions.length; d++) {
            int dim = dimensions[d];
            dimSizes[d] = getDimensionSize(dim);
        }

        final IMatrix matrix = createMatrix(dimSizes);
        final DoubleArrayList values = new DoubleArrayList();
        final List<int[]> coords = getNonZeroCoordinates(values);
        for (int i = 0; i < coords.size(); i++) {
            int[] origcoords = coords.get(i);
            final int[] transposedCoords = new int[getDimensions()];
            for (int d = 0; d < dimensions.length; d++) {
                int dim = dimensions[d];
                transposedCoords[d] = origcoords[dim];
            }

            matrix.add(transposedCoords, values.get(i));
        }
        return matrix;
    }

    /**
     * Creates an array [0, .. getDimensions()] that does not contain the indices listes in leaveOutDimensions
     *
     * @param leaveOutDimensions It is assumed that the array is sorted ascendingly and no duplicates are contained
     * @return int-array of length getDimensions()-leaveOutDimensions.length
     */
    protected int[] createOtherDimensions(int[] leaveOutDimensions) {
        int[] otherDimensions = new int[getDimensions() - leaveOutDimensions.length];
        int index = 0;
        int leaveOutIndex = 0;
        for (int dim = 0; dim < getDimensions(); dim++) {
            if (leaveOutIndex >= leaveOutDimensions.length || dim != leaveOutDimensions[leaveOutIndex]) {
                otherDimensions[index] = dim;
                index++;
            } else {
                leaveOutIndex++;

            }
        }
        return otherDimensions;
    }

    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append(map.size() + " ");
        for (Map.Entry<HashEntry, Double> e : map.entrySet()) {
            buf.append(e.getKey().toString() + ":" + e.getValue() + " ");
        }
        return buf.toString();
    }

    public int hashCode() {
        return map.hashCode();
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof IMatrix)) {
            return false;
        }

        // check dims
        IMatrix mat = (IMatrix) obj;
        if (mat.getDimensions() != getDimensions()) {
            return false;
        }
        for (int d = 0; d < getDimensions(); d++) {
            if (mat.getDimensionSize(d) != getDimensionSize(d)) {
                return false;
            }
        }

        // if hashmatrix, do quickcheck
        if (obj instanceof DoubleHashMatrix) {
            DoubleHashMatrix matrix = (DoubleHashMatrix) obj;
            return map.equals(matrix.map);
        }

        // else do full check
        return equalsCheckFull(mat);
    }

    protected boolean equalsCheckFull(IMatrix obj) {
        throw new UnsupportedOperationException("todo implement");

    }

    // ############################################
    // ## implementations with concrete dimensions
    // ############################################

    public static IMatrix createMatrix(int[] dimSizes) {
        switch (dimSizes.length) {
            case 0:
                throw new RuntimeException("matrices of dimension 0 are not supported. Use a double instead :)");
            case 1:
                return new ArrayMatrix1D(dimSizes);
            case 2:
                return new ArrayMatrix2D(dimSizes);
            case 3:
                return new HashMatrix3D(dimSizes);
            case 4:
                return new HashMatrix4D(dimSizes);
            default:
                return new DoubleHashMatrix(dimSizes);
        }
    }

    public static IMatrix1D createMatrix1D(DoubleMatrix1D coltMatrix) {
        HashMatrix1D result = new HashMatrix1D(coltMatrix.size());
        IntArrayList coord0 = new IntArrayList();
        DoubleArrayList values = new DoubleArrayList();
        coltMatrix.getNonZeros(coord0, values);
        for (int i = 0; i < coord0.size(); i++) {
            result.set(result.createCoords(coord0.get(i)), values.get(i));
        }
        return result;
    }

    public static HashMatrix2D createMatrix2D(DoubleMatrix2D coltMatrix) {
        HashMatrix2D result = new HashMatrix2D(coltMatrix.rows(), coltMatrix.columns());
        IntArrayList coord0 = new IntArrayList();
        IntArrayList coord1 = new IntArrayList();
        DoubleArrayList values = new DoubleArrayList();
        coltMatrix.getNonZeros(coord0, coord1, values);
        for (int i = 0; i < coord0.size(); i++) {
            result.set(result.createCoords(coord0.get(i), coord1.get(i)), values.get(i));
        }

        assert (result.equalsColt(coltMatrix));
        return result;
    }

    public static HashMatrix3D createMatrix3D(DoubleMatrix3D coltMatrix) {
        HashMatrix3D result = new HashMatrix3D(coltMatrix.slices(), coltMatrix.rows(), coltMatrix.columns());
        IntArrayList coord0 = new IntArrayList();
        IntArrayList coord1 = new IntArrayList();
        IntArrayList coord2 = new IntArrayList();
        DoubleArrayList values = new DoubleArrayList();
        coltMatrix.getNonZeros(coord0, coord1, coord2, values);
        for (int i = 0; i < coord0.size(); i++) {
            result.set(result.createCoords(coord0.get(i), coord1.get(i), coord2.get(i)), values.get(i));
        }
        return result;
    }

    public String toString(int[] entry) {
        String res = "[";
        for (int i : entry) {
            res += " " + i;
        }
        return res + "]";
    }

    public void doForAllNonZerosGeneric(INonZeroPerformer performer) {
        switch (getDimensions()) {
            case 1:
                ((IMatrix1D) this).doForAllNonZeros((INonZeroPerformer1D) performer);
                break;
            case 2:
                ((IMatrix2D) this).doForAllNonZeros((INonZeroPerformer2D) performer);
                break;
            case 3:
                ((IMatrix3D) this).doForAllNonZeros((INonZeroPerformer3D) performer);
                break;
            case 4:
                ((IMatrix4D) this).doForAllNonZeros((INonZeroPerformer4D) performer);
                break;
            default:
                throw new UnsupportedOperationException("todo implement");
        }
    }

    public void doForAllNonZeros(final INonZeroPerformerGeneric performer) {

        ArrayList<IntArrayList> keys = new ArrayList<IntArrayList>();
        for (int d = 0; d < getDimensions(); d++) {
            keys.add(new IntArrayList());
        }
        DoubleArrayList vals = new DoubleArrayList();

        getNonZeros(keys, vals);
        int[] coords = new int[getDimensions()];

        for (int index = 0; index < vals.size(); index++) {
            for (int d = 0; d < getDimensions(); d++) {
                coords[d] = keys.get(d).get(index);
            }
            double val = vals.get(index);
            performer.iteration(coords, val, index);
        }
    }

    //void doForAllNonZerosGeneric(INonZeroPerformer performer);
    public double sum() {
        double result = 0.0;
        DoubleArrayList values = new DoubleArrayList();
        getNonZeroCoordinates(values);
        for (int i = 0; i < values.size(); i++) {
            result += values.get(i);
        }
        return result;
    }

    public IMatrixMin marginalizeMin(int[] margeDims) {
        return marginalize(margeDims);
    }

    public IMatrixMin transposeMin(int[] dimensions) {
        return transpose(dimensions);
    }

    public void add(int[] coordinates, double val, int position) throws IndexOutOfBoundsException {
        add(coordinates, val);
    }

    public void set(int[] coordinates, double val, int position) throws IndexOutOfBoundsException {
        set(coordinates, val);
    }

    public double get(int[] coordinates, int position) throws IndexOutOfBoundsException {
        return get(coordinates);

    }

}
