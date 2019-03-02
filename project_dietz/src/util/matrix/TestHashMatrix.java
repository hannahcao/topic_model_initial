package util.matrix;

import cern.colt.list.DoubleArrayList;
import cern.colt.matrix.DoubleFactory3D;
import cern.colt.matrix.DoubleMatrix3D;
import junit.framework.TestCase;

import java.util.List;

public class TestHashMatrix extends TestCase {
    public void testEntry() {
        HashEntry entry1 = new HashEntry(3).set(0, 1, 5).set(1, 2, 5).set(2, 3, 4);
        HashEntry entry1_ = new HashEntry(3).set(0, 1, 5).set(1, 2, 5).set(2, 3, 4);
        HashEntry entry2 = new HashEntry(3).set(0, 1, 5).set(1, 2, 5).set(2, 2, 4);
        HashEntry entry3 = new HashEntry(2).set(0, 1, 5).set(1, 2, 5);

        assertNotSame(entry1, entry1_);

        assertEquals(entry1, entry1_);
        assertFalse(entry1.equals(entry2));
        assertFalse(entry1.equals(entry3));

        assertTrue(entry1.hashCode() == entry1_.hashCode());
        assertFalse(entry1.hashCode() == entry2.hashCode());
        assertFalse(entry1.hashCode() == entry3.hashCode());
    }

    public void testEntryLargeMatrixHalf() {
        int largeDim = Integer.MAX_VALUE / 2;
        HashEntry entry1 = new HashEntry(3).set(0, 1, largeDim).set(1, 2, largeDim).set(2, 3, largeDim);
        HashEntry entry1_ = new HashEntry(3).set(0, 1, largeDim).set(1, 2, largeDim).set(2, 3, largeDim);
        HashEntry entry2 = new HashEntry(3).set(0, 1, largeDim).set(1, 140, largeDim).set(2, 3, largeDim);

        assertNotSame(entry1, entry1_);

        assertEquals(entry1, entry1_);
        assertFalse(entry1.equals(entry2));

        assertTrue(entry1.hashCode() == entry1_.hashCode());
        assertFalse(entry1.hashCode() == entry2.hashCode());

        assertTrue(entry1.hashCode() < Integer.MAX_VALUE);
        assertTrue(entry2.hashCode() < Integer.MAX_VALUE * 0.8);
        assertTrue(entry1.hashCode() + " / " + entry2.hashCode(), Math.abs(entry2.hashCode() - entry1.hashCode()) > 50);

    }

    public void testEntryLargeMatrixFull() {
        int largeDim = Integer.MAX_VALUE;
        HashEntry entry1 = new HashEntry(3).set(0, 1, largeDim).set(1, 2, largeDim).set(2, 3, largeDim);
        HashEntry entry1_ = new HashEntry(3).set(0, 1, largeDim).set(1, 2, largeDim).set(2, 3, largeDim);
        HashEntry entry2 = new HashEntry(3).set(0, 3, largeDim).set(1, 2, largeDim).set(2, 100, largeDim);

        assertNotSame(entry1, entry1_);

        assertEquals(entry1, entry1_);
        assertFalse(entry1.equals(entry2));

        assertTrue(entry1.hashCode() == entry1_.hashCode());
        assertFalse(entry1.hashCode() == entry2.hashCode());

        assertTrue(entry1.hashCode() < Integer.MAX_VALUE);
        assertTrue(entry2.hashCode() < Integer.MAX_VALUE * 0.8);
        assertTrue(entry1.hashCode() + " / " + entry2.hashCode(), Math.abs(entry2.hashCode() - entry1.hashCode()) > 50);

    }

    public void testMatrixSetAdd() {
        DoubleHashMatrix matrix = new DoubleHashMatrix(new int[]{5, 13, 200});
        matrix.set(new int[]{0, 0, 0}, 20.0);
        assertEquals(20.0, matrix.get(new int[]{0, 0, 0}));
        matrix.set(new int[]{0, 0, 0}, 10.0);
        assertEquals(10.0, matrix.get(new int[]{0, 0, 0}));
        matrix.add(new int[]{0, 0, 0}, 5.0);
        assertEquals(15.0, matrix.get(new int[]{0, 0, 0}));
        matrix.add(new int[]{0, 0, 0}, -4.0);
        assertEquals(11.0, matrix.get(new int[]{0, 0, 0}));
        matrix.add(new int[]{0, 0, 0}, -20.0);
        assertEquals(-9.0, matrix.get(new int[]{0, 0, 0}));
        matrix.set(new int[]{0, 0, 0}, 10.0);
        assertEquals(10.0, matrix.get(new int[]{0, 0, 0}));
    }

    public void testMatrixSetZeros() {
        DoubleHashMatrix matrix = new DoubleHashMatrix(new int[]{5, 13, 200});
        assertEquals(0.0, matrix.get(new int[]{0, 0, 0}));
        matrix.set(new int[]{0, 0, 0}, 0.0);
        assertEquals(0.0, matrix.get(new int[]{0, 0, 0}));
        matrix.set(new int[]{0, 1, 0}, 10.0);
        assertEquals(10.0, matrix.get(new int[]{0, 1, 0}));
        matrix.add(new int[]{0, 1, 0}, -10.0);
        assertEquals(0.0, matrix.get(new int[]{0, 1, 0}));

        matrix.set(new int[]{0, 0, 1}, 10.0);
        assertEquals(10.0, matrix.get(new int[]{0, 0, 1}));

    }

    public void testMatrixNonZeros() {
        DoubleHashMatrix matrix = new DoubleHashMatrix(new int[]{5, 13, 200});
        matrix.set(new int[]{0, 1, 0}, 1.0);
        assertEquals(1.0, matrix.get(new int[]{0, 1, 0}));
        matrix.set(new int[]{0, 0, 1}, 10.0);
        assertEquals(10.0, matrix.get(new int[]{0, 0, 1}));
        matrix.set(new int[]{0, 1, 1}, 11.0);
        assertEquals(11.0, matrix.get(new int[]{0, 1, 1}));

        DoubleArrayList values = new DoubleArrayList();
        List<int[]> keys = matrix.getNonZeroCoordinates(values);

        assertTrue(contains(keys, values, new int[]{0, 1, 0}, 1.0));
        assertTrue(contains(keys, values, new int[]{0, 0, 1}, 10.0));
        assertTrue(contains(keys, values, new int[]{0, 1, 1}, 11.0));
        assertTrue(notContains(keys, new int[]{0, 0, 0}));

        // now set one to zero, this should not show up anymore

        matrix.set(new int[]{0, 1, 1}, 0.0);
        assertEquals(0.0, matrix.get(new int[]{0, 1, 1}));
        DoubleArrayList values_ = new DoubleArrayList();
        List<int[]> keys_ = matrix.getNonZeroCoordinates(values_);
        assertTrue(contains(keys, values, new int[]{0, 1, 0}, 1.0));
        assertTrue(contains(keys, values, new int[]{0, 0, 1}, 10.0));
        assertTrue(notContains(keys_, new int[]{0, 1, 1}));
        assertTrue(notContains(keys_, new int[]{0, 0, 0}));

    }

    private boolean notContains(List<int[]> keys, int[] coordinates) {
        for (int i = 0; i < keys.size(); i++) {
            int[] key = keys.get(i);
            boolean found = true;
            for (int c = 0; c < coordinates.length; c++) {
                if (coordinates[c] != key[c]) {
                    found = false;
                }
            }
            if (found) {
                return false;
            }
        }
        return true;
    }

    private boolean contains(List<int[]> keys, DoubleArrayList values, int[] coordinates, double value) {
        for (int i = 0; i < keys.size(); i++) {
            int[] key = keys.get(i);
            boolean found = true;
            for (int c = 0; c < coordinates.length; c++) {
                if (coordinates[c] != key[c]) {
                    found = false;
                }
            }
            if (found) {
                if (values.get(i) == value) {
                    return true;
                }
            }
        }
        return false;
    }

    public void testMatrixDimError() {
        DoubleHashMatrix matrix = new DoubleHashMatrix(new int[]{5, 13, 200});
        boolean found = false;
        try {
            matrix.set(new int[]{6, 0, 0}, 20.0);
        } catch (IndexOutOfBoundsException e) {
            found = true;
        }
        assertTrue("IndexOutOfBoundsException not thrown", found);

        found = false;

        try {
            matrix.set(new int[]{-4, 0, 0}, 20.0);
        } catch (IndexOutOfBoundsException e) {
            found = true;
        }
        assertTrue("IndexOutOfBoundsException not thrown", found);

        try {
            matrix.set(new int[]{0, 0, 0, 0}, 20.0);
        } catch (IndexOutOfBoundsException e) {
            found = true;
        }
        assertTrue("IndexOutOfBoundsException not thrown", found);

    }

    public void testMarginalize2D1() {
        HashMatrix2D matrix = new HashMatrix2D(10, 15);
        matrix.set(5, 5, 1);
        matrix.set(0, 5, 2);
        matrix.set(4, 14, 2.0);
        matrix.set(5, 0, 1);

        HashMatrix1D projection = (HashMatrix1D) matrix.marginalize(new int[]{0});
        assertEquals(3.0, projection.get(5));
        assertEquals(1.0, projection.get(0));
        assertEquals(2.0, projection.get(14));
        assertEquals(15, projection.getDimensionSize(0));

    }

    public void testMarginalize3D1() {
        HashMatrix3D matrix = new HashMatrix3D(10, 15, 7);
        matrix.set(5, 5, 1, 1);
        matrix.set(0, 5, 1, 2);
        matrix.set(4, 14, 1, 2.0);
        matrix.set(5, 0, 1, 1);

        HashMatrix2D projection = (HashMatrix2D) matrix.marginalize(2);
        assertEquals(1.0, projection.get(5, 5));
        assertEquals(2.0, projection.get(0, 5));
        assertEquals(2.0, projection.get(4, 14));
        assertEquals(1.0, projection.get(5, 0));

        assertEquals(10, projection.getDimensionSize(0));
        assertEquals(15, projection.getDimensionSize(1));

    }

    public void testMarginalize3D2() {
        HashMatrix3D matrix = new HashMatrix3D(10, 15, 7);
        matrix.set(5, 5, 1, 1);
        matrix.set(0, 5, 1, 2);
        matrix.set(4, 14, 1, 2.0);
        matrix.set(5, 0, 1, 1);

        HashMatrix1D projection = (HashMatrix1D) matrix.marginalize(new int[]{0, 2});
        assertEquals(3.0, projection.get(5));
        assertEquals(1.0, projection.get(0));
        assertEquals(2.0, projection.get(14));
        assertEquals(15, projection.getDimensionSize(0));

    }

    public void testColtConversion() {
        DoubleMatrix3D colt = DoubleFactory3D.sparse.make(3, 4, 5);
        colt.set(0, 0, 0, 1.0);
        colt.set(0, 3, 0, 2.0);
        colt.set(0, 3, 3, 3.0);

        HashMatrix3D matrix3D = DoubleHashMatrix.createMatrix3D(colt);
        matrix3D.add(0, 3, 3, 1.0);
        matrix3D.add(1, 1, 1, 1.0);

        DoubleMatrix3D coltMatrix_ = matrix3D.createColtMatrix();
        double[][][] doubles = colt.toArray();
        doubles[0][3][3] = 4.0;
        doubles[1][1][1] = 1.0;

        DoubleMatrix3D coltMatrix__ = DoubleFactory3D.sparse.make(doubles);

        assertEquals(coltMatrix__, coltMatrix_);
    }


}
