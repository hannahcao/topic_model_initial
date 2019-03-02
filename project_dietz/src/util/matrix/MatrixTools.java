package util.matrix;

import java.util.Arrays;

/**
 * todo comment 19.11.2006
 *
 * @version $ID$
 */
public class MatrixTools {
    public static boolean equals(IMatrix obj1, IMatrix obj2) {
        int[] entry = new int[obj1.getDimensions()];
        return check(obj1, obj2, 0, entry);
    }

    private static boolean check(IMatrix obj1, IMatrix obj2, int dim, int[] entry) {
        for (int i = 0; i < obj1.getDimensionSize(dim); i++) {
            entry[dim] = i;
            if ((dim + 1) < obj1.getDimensions()) {
                boolean res = check(obj1, obj2, dim + 1, entry);
                if (!res) return false;
            } else {
                if (obj1.get(entry) != obj2.get(entry)) return false;
            }
        }
        return true;
    }

    /**
     * @param from
     * @param to
     * @param fromToDimensions arr[toD] = fromD; special case if fromD == -1, then it is not altered.
     * @return
     */
    public static LineMatrix copyInto(LineMatrix from, LineMatrix to, int[] fromToDimensions) {
        if (to.getDimensions() != fromToDimensions.length) {
            throw new RuntimeException("Dimensions don't match " + to.getDimensions() + " " + Arrays.toString(fromToDimensions));
        }

        to.enlargeBy(from.vals.length);

        for (int toD = 0; toD < fromToDimensions.length; toD++) {
            int fromD = fromToDimensions[toD];
            if (fromD != -1) {
                assert (fromD <= from.getDimensions()) : fromD + " " + from.getDimensions();
                System.arraycopy(from.coords[fromD], 0, to.coords[toD], 0, from.coords[fromD].length);
            }
        }
        System.arraycopy(from.vals, 0, to.vals, 0, from.vals.length);
        to.setEndPosition(from.vals.length);
        return to;
    }

}
