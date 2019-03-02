package util.matrix;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Supports only positive coordinates, that do not exceed a given maximum.
 * <p/>
 * To create HashEntrys use {@link Factory#createEntry(int[])}:
 * <pre>
 * entryFactory = new HashEntry.Factory(new int[]{10,10});
 * HashEntry entry1 = entryFactory.createEntry(new int[]{1,2});
 * HashEntry entry2 = entryFactory.createEntry(new int[]{0,5);
 * <p/>
 * </pre>
 * <p/>
 * It is assumed, that the {@link #hashCode()} will be used among Entrys of the same matrix
 * (i.e. they have the same dimension).
 */
public class HashEntry implements Serializable {
    private List<Integer> tuple;
    private List<Integer> maxValues;
    private int cachedHash = -1;


    protected HashEntry(int dimension) {
        tuple = new ArrayList<Integer>(dimension);
        maxValues = new ArrayList<Integer>(dimension);
        for (int d = 0; d < dimension; d++) {
            tuple.add(0);
            maxValues.add(0);
        }
    }

    protected HashEntry(int[] coordinates, int[] maxima) {
        this(coordinates.length);
        assert (coordinates.length == maxima.length) : "both arrays must have the same length " + coordinates.length + " / " + maxima.length;
        for (int dim = 0; dim < coordinates.length; dim++) {
            set(dim, coordinates[dim], maxima[dim]);
        }
    }

    /**
     * Sets the coordinate.
     * <p/>
     * For convenience returns this.
     * <p/>
     * Example: For a tuple (15,13,12) in a 3d-matrix with 20 slices, 20 rows and 20 columns write<br>
     * <pre>
     * HashEntry entry = new HashEntry(3).set(0,15,20).set(1,13,20).set(2,12,20);
     * </pre>
     *
     * @param dimensionIndex the dimensionIndex for which to set the coordinate
     * @param coordinate     the value of the coordinate
     * @return this
     */
    public HashEntry set(int dimensionIndex, int coordinate, int maximum) {
        assert (dimensionIndex < getDimension()) : "dimensionIndex " + dimensionIndex + " must be < the maximum dimensionality of the tuple " + getDimension();
        assert (coordinate < maximum) : "coordinate " + coordinate + " must be < maximum " + maximum;
        assert (coordinate >= 0) : "coordinate " + coordinate + " must be >=0";
        tuple.set(dimensionIndex, coordinate);
        maxValues.set(dimensionIndex, maximum);
        cachedHash = -1;
        return this;
    }

    public int get(int dimensionIndex) {
        return tuple.get(dimensionIndex);
    }

    public int getMaximum(int dimensionIndex) {
        return maxValues.get(dimensionIndex);
    }

    public int getDimension() {
        return tuple.size();
    }


    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final HashEntry hashEntry = (HashEntry) o;

        // first checks null, then dimension, then hashcode, then the coordinates starting with the lowest dimension
        if (tuple != null) {
            if (getDimension() != hashEntry.getDimension()) {
                return false;
            } else {
                if (hashCode() != hashEntry.hashCode()) {
                    return false;
                } else {
                    for (int d = 0; d < getDimension(); d++) {
                        if (get(d) != hashEntry.get(d)) {
                            return false;
                        }
                    }
                    return true;
                }
            }

        } else {
            return (hashEntry.tuple != null);
        }
    }

    public int hashCode() {
        if (cachedHash == -1) {
            cachedHash = calculateHashCode();
        }
        return cachedHash;
    }

    protected int calculateHashCode() {
        if (tuple == null || maxValues == null) {
            return 0;
        }

        boolean largeMatrix = false;
        if (maxEntries() >= Integer.MAX_VALUE || maxEntries() < 0) {
            largeMatrix = true; // we have to normalize the dimensionstrides in order to get a useful hashfunction for the higher dimensionindices
        }

        int hash = 0;
        int dimensionStride = 1;
        for (int d = 0; d < getDimension(); d++) {
            // sliceStride==rows*slices, rowStride==columns, columnStride==1
            hash += dimensionStride + get(d);
            dimensionStride *= largeMatrix ? getMaximum(d) / getDimension() : getMaximum(d);
        }
        return hash;
    }

    private int maxEntries() {
        int mult = 1;
        for (int d = 0; d < getDimension(); d++) {
            mult *= getMaximum(d);
        }
        return mult;
    }

    public String toString() {
        return tuple.toString();
    }

    public static class Factory implements Serializable {
        private int[] maxima;

        public Factory(int[] dimSizes) {
            this.maxima = dimSizes;
        }

        public HashEntry createEntry(int[] coordinates) {
            return new HashEntry(coordinates, maxima);
        }

        /**
         * Returns -1, iff all coordinates are between 0 (inclusive) and the dimension's size (exclusive).
         * Otherwise the first dimension that violates this condition is returned
         *
         * @param coordinates
         * @return -1 iff within dimension size (i.e. everything is okay)
         */
        public int violatesDimensionSizes(int[] coordinates) {
            if (coordinates.length != maxima.length) {
                return Math.max(coordinates.length, maxima.length);
            }
            for (int dim = 0; dim < coordinates.length; dim++) {
                if (coordinates[dim] >= maxima[dim]) {
                    return dim;
                }
                if (coordinates[dim] < 0) {
                    return dim;
                }
            }
            return -1;
        }

        public int getDimensionSize(int dim) {
            if (dim > maxima.length || dim < 0) {
                return -1;
            }
            return maxima[dim];
        }

        public int getDimensions() {
            return maxima.length;
        }
    }
}
