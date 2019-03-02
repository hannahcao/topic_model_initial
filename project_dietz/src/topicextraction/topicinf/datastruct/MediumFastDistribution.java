package topicextraction.topicinf.datastruct;

import cern.colt.function.DoubleProcedure;
import cern.colt.list.DoubleArrayList;
import org.apache.commons.collections.OrderedMapIterator;

import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * medium fast distribution class.
 * <p/>
 * The object stores key/value as object / double array as well as a full
 * {@link topicextraction.topicinf.datastruct.Distribution> object.
 * <p/>
 * when sufficient, the operations work on the array (such as {@link IDistribution#put(Object,double)}  }.
 * <p/>
 * In other cases, the arrays are converted to the full {@link topicextraction.topicinf.datastruct.Distribution} object
 * and wrapper methods are used.
 */
public class MediumFastDistribution<E> implements IDistribution<E> {
    private static final long serialVersionUID = -4101480968191545786L;
    protected DoubleArrayList values = new DoubleArrayList();
    protected ArrayList<E> keys = new ArrayList<E>();
    protected double sumCache = 0.0;
    protected IDistribution<E> distributionCache = new Distribution<E>();
    protected boolean distributionValid = true;
    protected boolean arrayValid = true;

    public MediumFastDistribution(IDistribution<E> copyDistr) {
        if (copyDistr instanceof MediumFastDistribution) {
            MediumFastDistribution<E> mcopyDistr = (MediumFastDistribution<E>) copyDistr;
            distributionCache = new Distribution<E>(mcopyDistr.distributionCache);

        } else {
            distributionCache = new Distribution<E>(copyDistr);

        }
        arrayValid = false;
    }

    public MediumFastDistribution() {

    }

    // ///////////////////////////////////

    protected void updateDistribution() {
        if (!distributionValid) {
            assert (arrayValid);
            distributionCache.clear();
            for (int i = 0; i < keys.size(); i++) {
                distributionCache.put(keys.get(i), values.get(i));
            }
        }
        distributionValid = true;
    }

    protected void updateArray() {
        if (!arrayValid) {
            assert (distributionValid);
            int size = distributionCache.size();
            values.clear();
            values.ensureCapacity(size + 1);
            keys.clear();
            keys.ensureCapacity(size + 1);
            double sum = 0.0;

            for (OrderedMapIterator mapIterator = distributionCache.getOrderedMapIterator(); mapIterator.hasNext();) {
                E key = (E) mapIterator.next();
                double value = (Double) mapIterator.getValue();
                keys.add(key);
                values.add(value);
                sum += value;
            }

            sumCache = sum;
        }
        arrayValid = true;
    }
    // ///////////////////////////////////

    public double put(E key, double probability) {
        updateArray();
        assert (!Double.isNaN(probability)) : key + ":NaN";
        assert (!Double.isInfinite(probability)) : key + "Infinity";

        final int index = keys.indexOf(key);
        if (index == -1) {
            if (probability != 0.0) { // do not create entry when it results in 0.0
                keys.add(key);
                values.add(probability);
                sumCache += probability;
                distributionValid = false;
            }
            return 0.0;
        } else {
            final double oldVal = values.getQuick(index);
            if (probability == 0.0) {
                remove(key);
            } else {
                values.setQuick(index, probability);
                sumCache -= oldVal;
                sumCache += probability;
                distributionValid = false;
            }
            return oldVal;
        }
    }

    public double get(E key) {
        updateArray();

        final int index = keys.indexOf(key);
        if (index == -1) {
            return 0.0;
        } else {
            try {
                double result = values.get(index);
                assert (!Double.isNaN(result));
                if (Double.isNaN(result)) result = 0.0;
                return result;
            } catch (ArrayIndexOutOfBoundsException e) {
                System.err.println("MediumFastDistribution#get(" + key + ") yield OutOfBounds.\n" +
                        "\tkeys = " + Arrays.toString(keys.toArray()) + "\n" +
                        "\tvals = " + values.toString());
                throw e;
            }
        }
    }

    /**
     * Huiping put the comments on Aug. 13, 2012
     */
    public double add(E key, double probability) {
        updateArray();

        final int index = keys.indexOf(key); //the ids of documents that are cited by the same document
        if (index == -1) {
            if (probability != 0.0) { // do not create entry when it results in 0.0
                keys.add(key);
                values.add(probability);
                sumCache += probability;
                distributionValid = false;
            }
            return 0.0;
        } else {
            final double oldVal = values.getQuick(index); //oldVal = values[index]
            if (probability == -oldVal) {//probability+oldVal=0, so this key can be removed
                remove(key);
            } else {//this key cannot be removed 
                values.setQuick(index, probability + oldVal);//values[index]=probability+oldVal
                sumCache += probability; //sumCache = \sum_i values[i]
                distributionValid = false;
            }
            return oldVal;
        }

    }


    public boolean containsValue(double probability) {
        updateArray();
        return values.contains(probability);
    }

    public boolean containsKey(E key) {
        updateArray();
        return keys.contains(key);
    }

    public void clear() {
        keys.clear();
        values.clear();
        sumCache = 0.0;
        distributionCache.clear();
        distributionValid = true;
        arrayValid = true;
    }

    public Set<E> keySet() {
        updateArray();
        return new AbstractSet<E>() {
            public int size() {
                return keys.size();
            }


            public Iterator<E> iterator() {
                return keys.iterator();
            }

        };
    }

    public double remove(E key) {
        updateArray();

        final int index = keys.indexOf(key);
        if (index == -1) {
            return 0.0;
        } else {
            final double oldVal = values.getQuick(index);
            sumCache -= oldVal;
            final E oldKey = keys.remove(index);
            values.remove(index);
            distributionValid = false;
            return oldVal;
        }
    }

    public int size() {
        updateArray();
        return keys.size();
    }

    public boolean isEmpty() {
        updateArray();
        assert (keys.isEmpty() == values.isEmpty());
        return keys.isEmpty();
    }

    public double sum() {
        updateArray();
        // todo check
        assert (Math.abs(sumCache - safeSum()) < 0.000001) : "sums do not match! sumCache=" + sumCache + " safeSum()=" + safeSum();
        return sumCache;
//        return safeSum();
    }

    private double safeSum() {
        double result = 0.0;
        for (int i = 0; i < values.size(); i++) {
            result += values.get(i);
        }
        return result;
    }

    public void normalize() throws Distribution.EmptyDistributionException {
        updateArray();

        double normalizationConst = sum();
        for (int i = 0; i < values.size(); i++) {
            final double oldVal = values.getQuick(i);
            values.setQuick(i, oldVal / normalizationConst);
        }
        distributionValid = false;
        sumCache = 1.0;
    }


    // todo --> returns IDistribution
    public IDistribution<E> getNormalizedDistribution() {
        final MediumFastDistribution<E> distribution = new MediumFastDistribution<E>();
        distribution.values = values.copy();
        distribution.keys = new ArrayList<E>(keys);
        distribution.sumCache = sumCache;
        distribution.normalize();
        return distribution;
    }


    public E draw() {
        updateArray();

        double seed = Math.random();
        assert (!Double.isNaN(seed));

        for (E key : keySet()) {
            double prob = get(key);
            assert (!Double.isNaN(prob)) : "get(" + key + ") is NaN";
            assert (!Double.isInfinite(prob)) : "get(" + key + ") is Infin";
            assert (prob >= 0.0) : "Distribution not normalized: 0 > get(" + key + ")=" + prob;
            assert (prob <= 1.0) : "Distribution not normalized: 1 < get(" + key + ")=" + prob;
            seed -= prob;
            assert (!Double.isNaN(seed));
            if (seed <= 0 || Double.isNaN(seed)) {
                return key;
            }
        }
        if (isEmpty()) {
//            System.err.println("Distribution to draw from is empty, returning random key");
        } else {
            System.err.println("warning... returning max distr=" + this + " seed = " + seed);
        }
        return highest();
    }

    // //////////////////////////////////////////////
    // //////////////////////////////////////////////
    // //////////////////////////////////////////////
    // //////////////////////////////////////////////
    // //////////////////////////////////////////////

    public E highest() {
        if (distributionValid) {
            return distributionCache.highest();
        } else if (arrayValid) {
            double highestVal = Double.MIN_VALUE;
            E highestKey = null;
            for (int i = 0; i < keys.size(); i++) {
                double v = values.get(i);
                if (v > highestVal) {
                    highestVal = v;
                    highestKey = keys.get(i);
                }
            }
            return highestKey;
        } else {
            throw new RuntimeException("programming error: neither distributionValid not arrayValid are true");
        }

    }

    public E lowest() {
        if (distributionValid) {
            return distributionCache.lowest();
        } else if (arrayValid) {
            double lowestVal = Double.MAX_VALUE;
            E lowestKey = null;
            for (int i = 0; i < keys.size(); i++) {
                double v = values.get(i);
                if (v < lowestVal) {
                    lowestVal = v;
                    lowestKey = keys.get(i);
                }
            }
            return lowestKey;
        } else {
            throw new RuntimeException("programming error: neither distributionValid not arrayValid are true");
        }
    }

    public boolean hasOnlyEntriesBelow(double thresh) {
        if (distributionValid) {
            return distributionCache.hasOnlyEntriesBelow(thresh);
        } else if (arrayValid) {
            for (int i = 0; i < keys.size(); i++) {
                double v = values.get(i);
                if (v >= thresh) {
                    return false;
                }
            }
            return true;
        } else {
            throw new RuntimeException("programming error: neither distributionValid not arrayValid are true");
        }

    }

    // //////////////////////////////////////////////
    // //////////////////////////////////////////////
    // //////////////////////////////////////////////
    // //////////////////////////////////////////////
    // //////////////////////////////////////////////


    public IDistribution<E> headMap(E lowestElem) {
        updateDistribution();
        return distributionCache.headMap(lowestElem);
    }


    public List<E> descendingElements() {
        updateDistribution();
        return distributionCache.descendingElements();
    }

    public IDistribution<E> elementsAbove(double lowestProbability) {
        updateDistribution();
        return distributionCache.elementsAbove(lowestProbability);
    }

    public IDistribution<E> getSubMap(List<E> keys) {
        updateDistribution();
        return distributionCache.getSubMap(keys);
    }

    public IDistribution<E> getShortenedMap() {
        updateDistribution();
        return distributionCache.getShortenedMap();
    }

    public List<E> highestElements(int maxElements) {
        updateDistribution();
        return distributionCache.highestElements(maxElements);
    }


    public String toString(int numelems) {
        updateDistribution();
        return distributionCache.toString(numelems);
    }

    public String toString() {
        updateDistribution();
        return distributionCache.toString();
    }

    public OrderedMapIterator getOrderedMapIterator() {
        updateDistribution();
        arrayValid = false;
        return distributionCache.getOrderedMapIterator();
    }

    public void overlay(IDistribution<E> distr2) {
        updateDistribution();
        distributionCache.overlay(distr2);
        arrayValid = false;
    }

    public void overlay(IDistribution<E> distr2, double factor) {
        updateDistribution();
        distributionCache.overlay(distr2, factor);
        arrayValid = false;
    }

    public void overlay(IDistribution<E> distr2, double factor, boolean useAbsValue) {
        updateDistribution();
        distributionCache.overlay(distr2, factor, useAbsValue);
        arrayValid = false;
    }

    public void multiply(double factor) {
        updateDistribution();
        distributionCache.multiply(factor);
        arrayValid = false;
    }

    public void multiply(IDistribution<E> distr2) {
        updateDistribution();
        distributionCache.multiply(distr2);
        arrayValid = false;
    }

    public boolean checkNonNaN() {
        updateDistribution();
        return distributionCache.checkNonNaN();
    }

    public double geometricMean() {
        final double[] sum = new double[]{0.0};
        final int[] n = new int[]{0};

        values.forEach(new DoubleProcedure() {
            public boolean apply(double val) {
                sum[0] += Math.log(val);
                n[0]++;
                return true;
            }
        });
        double normSum = sum[0] / n[0];
        double mean = Math.exp(normSum);
        return mean;

    }
}
