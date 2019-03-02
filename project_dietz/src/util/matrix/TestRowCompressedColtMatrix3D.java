package util.matrix;

import junit.framework.TestCase;

/**
 * Created by IntelliJ IDEA.
 * User: Administrator
 * Date: Aug 19, 2007
 * Time: 2:24:55 PM
 * To change this template use File | Settings | File Templates.
 */
public class TestRowCompressedColtMatrix3D extends TestCase {
    private RowCompressedColtMatrix3D matrix;

    protected void setUp() throws Exception {
        matrix = new RowCompressedColtMatrix3D(5, 20, 3);
    }

    public void testSetGet() {
        matrix.set(1, 1, 1, 2.0);
        matrix.set(0, 19, 1, 1.0);

        assertEquals(2.0, matrix.get(1, 1, 1));
        assertEquals(1.0, matrix.get(0, 19, 1));


        matrix.set(1, 1, 1, 3.0);
        assertEquals(3.0, matrix.get(1, 1, 1));

        assertEquals(0.0, matrix.get(0, 0, 0));
    }

    public void testAdd() {
        matrix.set(1, 1, 1, 2.0);
        matrix.add(0, 19, 1, 1.0);
        assertEquals(1.0, matrix.get(0, 19, 1));

        matrix.add(1, 1, 1, 3.0);
        assertEquals(5.0, matrix.get(1, 1, 1));

        matrix.add(1, 1, 1, 0.0);
        assertEquals(5.0, matrix.get(1, 1, 1));


        matrix.add(1, 1, 1, -5.0);
        assertEquals(0.0, matrix.get(1, 1, 1));


        matrix.add(0, 0, 0, 0.0);
        assertEquals(0.0, matrix.get(0, 0, 0));
    }

    public void testSetGetAddGeneric() {
        matrix.set(new int[]{1, 1, 1}, 2.0);
        matrix.set(new int[]{0, 19, 1}, 1.0);

        assertEquals(2.0, matrix.get(new int[]{1, 1, 1}));
        assertEquals(1.0, matrix.get(0, 19, 1));


        matrix.set(new int[]{1, 1, 1}, 3.0);
        assertEquals(3.0, matrix.get(new int[]{1, 1, 1}));

        matrix.set(1, 1, 1, -2.0);
        assertEquals(-2.0, matrix.get(new int[]{1, 1, 1}));

        assertEquals(0.0, matrix.get(new int[]{0, 0, 0}));

        matrix.add(new int[]{0, 19, 1}, 1.0);
        assertEquals(2.0, matrix.get(0, 19, 1));

    }

    public void testDoForAllNonzeros() {
        matrix.set(1, 1, 1, 2.0);
        matrix.set(0, 19, 1, 1.0);

        final boolean[] found1 = new boolean[]{false};
        final boolean[] found2 = new boolean[]{false};
        matrix.doForAllNonZeros(new INonZeroPerformer3D() {
            public void iteration(int coord0, int coord1, int coord2, double val, int position) {
                if (coord0 == 1 && coord1 == 1 && coord2 == 1) {
                    assertEquals("[1,1,1] expected to be 2.0, but was " + val, 2.0, val);
                    found1[0] = true;
                } else if (coord0 == 0 && coord1 == 19 && coord2 == 1) {
                    assertEquals("[0,19,1] expected to be 1.0, but was " + val, 1.0, val);
                    found2[0] = true;
                } else {
                    assertFalse("Found unexpected entry [" + coord0 + "," + coord1 + "," + coord2 + "] val " + val + ".", true);
                }
            }
        });
        assertTrue("Entry [1,1,1] not found", found1[0]);
        assertTrue("Entry [0,19,1] not found", found2[0]);
    }

    public void testDoForAllNonzerosGeneric() {
        matrix.set(1, 1, 1, 2.0);
        matrix.set(0, 19, 1, 1.0);

        final boolean[] found1 = new boolean[]{false};
        final boolean[] found2 = new boolean[]{false};
        matrix.doForAllNonZeros(new INonZeroPerformerGeneric() {
            public void iteration(int[] coords, double val, int position) {
                assertEquals(3, coords.length);
                int coord0 = coords[0];
                int coord1 = coords[1];
                int coord2 = coords[2];

                if (coord0 == 1 && coord1 == 1 && coord2 == 1) {
                    assertEquals("[1,1,1] expected to be 2.0, but was " + val, 2.0, val);
                    found1[0] = true;
                } else if (coord0 == 0 && coord1 == 19 && coord2 == 1) {
                    assertEquals("[0,19,1] expected to be 1.0, but was " + val, 1.0, val);
                    found2[0] = true;
                } else {
                    assertFalse("Found unexpected entry [" + coord0 + "," + coord1 + "," + coord2 + "] val " + val + ".", true);
                }
            }
        });
        assertTrue("Entry [1,1,1] not found", found1[0]);
        assertTrue("Entry [0,19,1] not found", found2[0]);
    }

    public void testSum() {
        matrix.set(1, 1, 1, 2.0);
        matrix.set(0, 19, 1, 1.0);
        assertEquals(3.0, matrix.sum());

        matrix.set(0, 19, 1, 10.0);
        assertEquals(12.0, matrix.sum());

        matrix.add(0, 19, 1, -5.0);
        assertEquals(7.0, matrix.sum());
    }

    public void testSize() {
        matrix.set(1, 1, 1, 2.0);
        matrix.set(0, 19, 1, 1.0);

        assertEquals(300, matrix.size());
    }
}
