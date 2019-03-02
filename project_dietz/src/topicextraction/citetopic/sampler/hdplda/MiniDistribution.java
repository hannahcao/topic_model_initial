package topicextraction.citetopic.sampler.hdplda;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;

/**
 * Minimal implementation of a Distribution. Designed for reuse and highperformance.
 */
public class MiniDistribution implements Serializable {
    private static final long serialVersionUID = 4487678948145657551L;
    private int[] keys;
    private int[] keys2, keys3, keys4;
    private double[] vals;
    int endposition = 0;
    private int key2Draw = 0;
    private int key3Draw = 0;
    private int key4Draw = 0;

    public MiniDistribution(int maxEntries) {
        keys = new int[maxEntries];
        keys2 = new int[maxEntries];
        keys3 = new int[maxEntries];
        keys4 = new int[maxEntries];
        vals = new double[maxEntries];
    }

    public void add(int key, int key2, double val) {
        add(key, key2, 0, val);
    }

    public void add(int key, int key2, int key3, double val) {
        add(key, key2, key3, 0, val);
    }

    public void add(int key, int key2, int key3, int key4, double val) {
        assert (!Double.isNaN(val)) : key + " " + key2 + " " + key3 + " " + key4;
        keys[endposition] = key;
        keys2[endposition] = key2;
        keys3[endposition] = key3;
        keys4[endposition] = key4;
        vals[endposition] = val;
        endposition++;
    }

    public void put(int key, int key2, double val) {
        put(key, key2, 0, val);
    }

    public void put(int key, int key2, int key3, double val) {
        add(key, key2, key3, val);
    }

    public void put(int key, int key2, int key3, int key4, double val) {
        add(key, key2, key3, key4, val);
    }

    public void add(int key, double val) {
        add(key, 0, val);
    }

    public void put(int key, double val) {
        put(key, 0, val);
    }

    public void clear() {
        endposition = 0;
    }

    public boolean isEmpty() {
        return endposition == 0;
    }

    public double sum() {
        double result = 0.0;
        for (int i = 0; i < endposition; i++) {
            assert (!Double.isNaN(vals[i]));
            result += vals[i];
        }
        return result;
    }

    public void normalize() {
        double sum = sum();
        for (int i = 0; i < endposition; i++) {
            assert (!Double.isNaN(vals[i]));
            vals[i] = vals[i] / sum;
            assert (!Double.isNaN(vals[i]));
        }
    }

    /**
     * draw a key from the distribution
     * <p/>
     * the corresponding key2 can be retrieved by a subsequent call to {@link #getKey2Draw()}.
     *
     * @return drawn key
     */
    public int draw() {
        if (isEmpty()) {
            throw new RuntimeException("HdpLdaSampler$MiniDistribution.draw: distribution is Empty!");
        }
        double sum = sum();
        assert (sum > 0) : sum + " \n" + Arrays.toString(keys) + " \n" + Arrays.toString(vals);
        assert (!Double.isNaN(sum));
        double rnd = Math.random();
        if (Double.isInfinite(sum)) {
            System.err.println("Too large values. Sums to infinity. " + Arrays.toString(vals));
        }
        double seed = rnd * sum;
        for (int i = 0; i < endposition; i++) {
            assert (!Double.isNaN(seed));
            assert (!Double.isNaN(vals[i]));
            seed -= vals[i];
            if (Double.isNaN(seed)) {
                int bla = 1;
            }
            if (seed <= 0) {
                key2Draw = keys2[i];
                key3Draw = keys3[i];
                key4Draw = keys4[i];
                return keys[i];
            }
        }
        throw new RuntimeException("something went wrong here.. remaining seed=" + seed);
    }


    public int getKey2Draw() {
        return key2Draw;
    }


    public int getKey3Draw() {
        return key3Draw;
    }


    public int getKey4Draw() {
        return key4Draw;
    }

    public void initializeEqualDistribution(Collection<Integer> keys) {
        clear();
        for (int key : keys) {
            add(key, 0, 0, 0, 1.0);
        }
    }

    public void initializeEqualDistribution(int maxKey) {
        clear();
        for (int key = 0; key < maxKey; key++) {
            add(key, 0, 0, 1.0);
        }
    }

    public int size() {
        return keys.length;
    }

    public double get(int key) {
        int index = Arrays.binarySearch(keys, key);
        assert (index >= 0) : "Search failed: " + index;
        return vals[index];
    }

    public void overlay(MiniDistribution other) {
        assert (other.vals.length == vals.length) : other.vals.length + "!=" + vals.length;
        for (int i = 0; i < other.vals.length; i++) {
            int key = other.keys[i];
            int key2 = other.keys2[i];
            int key3 = other.keys3[i];
            int key4 = other.keys4[i];
            double val = other.vals[i];

            add(key, key2, key3, key4, val);
        }
    }
}
