package topicextraction.topicinf.datastruct;

import cern.colt.list.DoubleArrayList;
import cern.colt.list.IntArrayList;
import org.apache.commons.collections.OrderedMapIterator;

import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * medium fast distribution class.
 * <p/>
 * The object stores key/value as object / double array as well as a full
 * {@link Distribution> object.
 * <p/>
 * when sufficient, the operations work on the array (such as {@link IDistribution#put(Object,double)}  }.
 * <p/>
 * In other cases, the arrays are converted to the full {@link Distribution} object
 * and wrapper methods are used.
 */
public class MediumFastDistributionInt implements IDistributionInt {
    protected DoubleArrayList values = new DoubleArrayList();
    protected IntArrayList keys = new IntArrayList();
    protected double sumCache = 0.0;
    protected Distribution<Integer> distributionCache = new Distribution<Integer>();
    protected boolean distributionValid = true;
    protected boolean arrayValid = true;
    private final StringBuffer commandList = new StringBuffer();
    private boolean unmodifyable = false;

    protected MediumFastDistributionInt(MediumFastDistributionInt medDistr) {
        values = medDistr.values;
        keys = medDistr.keys;
        sumCache = medDistr.sumCache;

        arrayValid = true;
        distributionValid = false;

        addCommand("instantiate(MediumFastDistributionInt)");
    }

    public MediumFastDistributionInt() {
        arrayValid = true;
        distributionValid = true;

        addCommand("instantiate()");
    }

    public MediumFastDistributionInt(IDistribution<Integer> copyDistr) {
        distributionCache = new Distribution<Integer>(copyDistr);
        arrayValid = false;
        distributionValid = true;

        addCommand("instantiate(IDistribution<Integer>)");

    }

    // ///////////////////////////////////

    private void updateDistribution() {
        if (!distributionValid) {
            assert (arrayValid);
            distributionCache.clear();
            for (int i = 0; i < keys.size(); i++) {
                distributionCache.put(keys.get(i), values.get(i));
            }
        }
        distributionValid = true;
    }

    private void updateArray() {
        if (!arrayValid) {
            assert (distributionValid);
            int size = distributionCache.size();
            values.clear();
            values.ensureCapacity(size + 1);
            keys.clear();
            keys.ensureCapacity(size + 1);
            double sum = 0.0;

            for (OrderedMapIterator mapIterator = distributionCache.getOrderedMapIterator(); mapIterator.hasNext();) {
                int key = (Integer) mapIterator.next();
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

    public double put(int key, double probability) {
        if (Double.isNaN(probability) || Double.isInfinite(probability)) {
            throw new IllegalArgumentException("invalid call to put(" + key + "," + probability + ")");
        }
        if (probability < 0.0) {
            throw new IllegalArgumentException(
                    "invalid call to put(" + key + "," + probability + ") negative params are not allowed.");
        }

//        addCommand("put"));
        if (unmodifyable) {
            throw new UnsupportedOperationException("this distribution is unmodifyable.");
        }
        updateArray();

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

    public double get(int key) {
//        addCommand("get"));
        updateArray();

        final int index = keys.indexOf(key);
        if (index == -1) {
            return 0.0;
        } else {
            try {
                return values.get(index);
            } catch (ArrayIndexOutOfBoundsException e) {
                System.err.println("MediumFastDistribution#get(" + key + ") yield OutOfBounds.\n" +
                        "\tkeys = " + Arrays.toString(keys.elements()) + "\n" +
                        "\tvals = " + values.toString());
                throw e;
            }
        }
    }

    public double add(int key, double probability) {
        if (Double.isNaN(probability) || Double.isInfinite(probability)) {
            throw new IllegalArgumentException("invalid call to put(" + key + "," + probability + ")");
        }
        if (probability < 0.0) {
            throw new IllegalArgumentException(
                    "invalid call to put(" + key + "," + probability + ") negative params are not allowed.");
        }

        addCommand("add");
        if (unmodifyable) {
            throw new UnsupportedOperationException("this distribution is unmodifyable.");
        }
        updateArray();

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
            if (probability == -oldVal) {
                remove(key);
            } else {
                values.setQuick(index, probability + oldVal);
                sumCache += probability;
                distributionValid = false;
            }
            return oldVal;
        }

    }


    public boolean containsValue(double probability) {
        addCommand("containsValue");
        updateArray();
        return values.contains(probability);
    }

    public boolean containsKey(int key) {
        addCommand("containsKey");
        updateArray();
        return keys.contains(key);
    }

    public void clear() {
        addCommand("clear");
        if (unmodifyable) {
            throw new UnsupportedOperationException("this distribution is unmodifyable.");
        }
        keys.clear();
        values.clear();
        sumCache = 0.0;
        distributionCache.clear();
        distributionValid = true;
        arrayValid = true;
    }

    public Set<Integer> keySet() {
        addCommand("keySet");
        updateArray();
        return new AbstractSet<Integer>() {
            public int size() {
                return keys.size();
            }


            public Iterator<Integer> iterator() {
                updateArray();
                return new Iterator<Integer>() {
                    private int index = -1;

                    public boolean hasNext() {
                        return index < keys.size() - 1;
                    }

                    public Integer next() {
                        if (!arrayValid) {
                            throw new ConcurrentModificationException();
                        }
                        index++;
                        return keys.get(index);
                    }

                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }

        };
    }

    public double remove(int key) {
        addCommand("remove");
        if (unmodifyable) {
            throw new UnsupportedOperationException("this distribution is unmodifyable.");
        }
        updateArray();

        final int index = keys.indexOf(key);
        if (index == -1) {
            return 0.0;
        } else {
            final double oldVal = values.getQuick(index);
            sumCache -= oldVal;
            keys.remove(index);
            values.remove(index);
            distributionValid = false;
            return oldVal;
        }
    }

    public int size() {
        addCommand("size");
        updateArray();
        return keys.size();
    }

    public boolean isEmpty() {
        addCommand("isEmpty");
        updateArray();
        assert (keys.isEmpty() == values.isEmpty());
        return keys.isEmpty();
    }

    public double sum() {
        addCommand("sum");
        updateArray();
        // todo check
//        assert (Math.abs(sumCache - safeSum()) < 0.000001) : "sums do not match! sumCache=" + sumCache + " safeSum()=" + safeSum();
//        if(Math.abs(sumCache - safeSum()) >= 0.000001){
//            System.err.println(commandList);
//            throw new RuntimeException( "sums do not match! sumCache=" + sumCache + " safeSum()=" + safeSum());
//        }
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
        addCommand("normalize");
        if (unmodifyable) {
            throw new UnsupportedOperationException("this distribution is unmodifyable.");
        }
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
    public IDistributionInt getNormalizedDistribution() {
        final MediumFastDistributionInt distribution = new MediumFastDistributionInt();
        distribution.values = values.copy();
        distribution.keys = new IntArrayList(keys.elements());
        distribution.sumCache = sumCache;
        distribution.normalize();
        return distribution;
    }

    public int draw() {
        addCommand("draw");
        updateArray();

        double seed = Math.random();

        for (int key : keySet()) {
            assert (!Double.isNaN(get(key))) : "get(" + key + ") is NaN";
            assert (get(key) >= 0.0) : "Distribution not normalized: 0 > get(" + key + ")=" + get(key);
            assert (get(key) <= 1.0) : "Distribution not normalized: 1 < get(" + key + ")=" + get(key);
            seed -= get(key);
            if (seed <= 0) {
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

    public int highest() {
        addCommand("highest");
        if (distributionValid) {
            return distributionCache.highest();
        } else if (arrayValid) {
            double highestVal = Double.MIN_VALUE;
            int highestKey = -1;
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

    public int lowest() {
        addCommand("lowest");
        if (distributionValid) {
            return distributionCache.lowest();
        } else if (arrayValid) {
            double lowestVal = Double.MAX_VALUE;
            int lowestKey = -1;
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
        addCommand("hasEntriesBelow");
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


    public IDistributionInt headMap(int lowestElem) {
        addCommand("headMap");
        updateDistribution();
        IDistribution<Integer> distribution = distributionCache.headMap(lowestElem);
        return new MediumFastDistributionInt(distribution);
    }


    public List<Integer> descendingElements() {
        addCommand("descendingElements");
        updateDistribution();
        return distributionCache.descendingElements();
    }

    public IDistributionInt elementsAbove(double lowestProbability) {
        addCommand("elementsAbove");
        updateDistribution();
        return new MediumFastDistributionInt(distributionCache.elementsAbove(lowestProbability));
    }

    public IDistributionInt getSubMap(List<Integer> keys) {
        addCommand("getSubMap");
        updateDistribution();
        return new MediumFastDistributionInt(distributionCache.getSubMap(keys));
    }

    public IDistributionInt getShortenedMap() {
        addCommand("getShortenedMap");
        updateDistribution();
        return new MediumFastDistributionInt(distributionCache.getShortenedMap());
    }

    public List<Integer> highestElements(int maxElements) {
        addCommand("highestElements");
        updateDistribution();
        return distributionCache.highestElements(maxElements);
    }

    public String toString() {
        updateDistribution();
        return distributionCache.toString();
    }

    public String toString(int numelems) {
        updateDistribution();
        return distributionCache.toString(numelems);
    }

    public OrderedMapIterator getOrderedMapIterator() {
        addCommand("getOrderedMapIterator");
        updateDistribution();
        arrayValid = false;
        return distributionCache.getOrderedMapIterator();
    }

    public void overlay(IDistributionInt distr2) {
        addCommand("overlay(IDistributionInt)");
        if (unmodifyable) {
            throw new UnsupportedOperationException("this distribution is unmodifyable.");
        }
        updateDistribution();
        distributionCache.overlay(distr2.convert());
        arrayValid = false;
    }

    public void overlay(IDistributionInt distr2, double factor) {
        addCommand("overlay(IDistributionInt,double)");
        if (unmodifyable) {
            throw new UnsupportedOperationException("this distribution is unmodifyable.");
        }
        updateDistribution();
        distributionCache.overlay(distr2.convert(), factor);
        arrayValid = false;
    }

    public void overlay(IDistributionInt distr2, double factor, boolean useAbsValue) {
        addCommand("overlay(IDistributionInt,double,boolean)");
        if (unmodifyable) {
            throw new UnsupportedOperationException("this distribution is unmodifyable.");
        }
        updateDistribution();
        distributionCache.overlay(distr2.convert(), factor, useAbsValue);
        arrayValid = false;
    }

    public void multiply(double factor) {
        addCommand("multiply(double)");
        if (unmodifyable) {
            throw new UnsupportedOperationException("this distribution is unmodifyable.");
        }
        updateDistribution();
        distributionCache.multiply(factor);
        arrayValid = false;
    }

    public void multiply(IDistributionInt distr2) {
        addCommand("multiply(IdistributionInt)");
        if (unmodifyable) {
            throw new UnsupportedOperationException("this distribution is unmodifyable.");
        }
        updateDistribution();
        distributionCache.multiply(distr2.convert());
        arrayValid = false;
    }

    public boolean checkNonNaN() {
        updateDistribution();
        return distributionCache.checkNonNaN();
    }

    public boolean checkNonNull() {
        updateArray();
        if (values.size() == 0) return false;
        for (double v : values.elements()) {
            if (v != 0.0) return true;
        }
        return false;
    }

    public void setUnmodifyable() {
        updateArray();
        updateDistribution();
        unmodifyable = true;
    }

    public void retainAllKeys(Set<Integer> toRetainKeys) {
        for (int key : new HashSet<Integer>(keySet())) {
            if (!toRetainKeys.contains(key)) {
                remove(key);
            }
        }
    }

    public IDistribution<Integer> convert() {
        addCommand("convert");
        updateDistribution();
        IDistribution<Integer> newDistribution = DistributionFactory.copyDistribution(distributionCache);
        if (newDistribution instanceof MediumFastDistribution) {
            ((MediumFastDistribution) newDistribution).updateArray();
            ((MediumFastDistribution) newDistribution).updateDistribution();
        }
        return newDistribution;
    }

    public static IDistributionInt equalDistribution(Collection<Integer> integers) {
        IDistributionInt result = DistributionFactory.createIntDistribution();
        for (int key : integers) {
            result.put(key, 1.0);
        }
        result.normalize();
        if (result instanceof MediumFastDistribution) {
            ((MediumFastDistribution) result).updateArray();
            ((MediumFastDistribution) result).updateDistribution();
        }
        return result;
    }


    public static List<IDistribution<Integer>> convert(List<IDistributionInt> list) {
        ArrayList<IDistribution<Integer>> result = new ArrayList<IDistribution<Integer>>();
        for (IDistributionInt distr : list) {
            result.add(distr.convert());
        }
        return result;
    }


    public static Map<Integer, IDistribution<Integer>> convert(Map<Integer, IDistributionInt> map) {
        Map<Integer, IDistribution<Integer>> result = new HashMap<Integer, IDistribution<Integer>>();
        for (Integer key : map.keySet()) {
            IDistributionInt distr = map.get(key);
            result.put(key, distr.convert());
        }
        return result;
    }

    /**
     * only needed for debugging purposes.
     *
     * @param command
     * @return
     */
    private boolean addCommand(String command) {
        if (commandList.length() > 120) {
            commandList.delete(0, 100);
        }
        commandList.append(command + "\n");
        return true;
    }

    // equals implementation based on distribution objects

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MediumFastDistributionInt that = (MediumFastDistributionInt) o;
        updateDistribution();
        that.updateDistribution();

        if (distributionCache != null ? !distributionCache.equals(that.distributionCache) : that.distributionCache != null)
            return false;

        return true;
    }

    public int hashCode() {
        updateDistribution();
        return (distributionCache != null ? distributionCache.hashCode() : 0);
    }

    // equals implementation based on arrays
//
//    public boolean equals(Object o) {
//        if (this == o) return true;
//        if (o == null || getClass() != o.getClass()) return false;
//
//        MediumFastDistributionInt that = (MediumFastDistributionInt) o;
//        updateArray();
//        that.updateArray();
//        that.updateDistribution();
//
//        if (keys.size() != that.keys.size()) return false;
//        for (int i = 0; i < keys.size(); i++) {
//            int key = keys.get(i);
//            double val = values.get(i);
//            double thatVal = that.get(key);
//
//            if (val != thatVal) return false;
//
//        }
//        return true;
//    }
//
//    public int hashCode() {
//        updateArray();
//
//        double sum = 0.0;
//        for (int i = 0; i < keys.size(); i++) {
//            int key = keys.get(i);
//            double val = values.get(i);
//            sum += key * val;
//        }
//        return (int) Math.floor(sum * 100.0);
//    }
}
