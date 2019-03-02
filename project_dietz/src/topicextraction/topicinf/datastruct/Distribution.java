package topicextraction.topicinf.datastruct;

//import topicextraction.topicinf.ITopicInferenceFacade;
import org.apache.commons.collections.OrderedMapIterator;
import org.apache.commons.collections.map.AbstractHashedMap;
import org.apache.commons.collections.map.LinkedMap;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Distribution of Objects of Type E.
 * <p/>
 * It stores a mapping of the elements -&gt; probability. It also maintains a sorted list of the elements,
 * which is sorted descending according to their probabilities.
 * <p/>
 * This implementation can also deal with several keys having the same value.
 * <p/>
 * <h2>Usage</h2><pre>
 * ITopicInferenceFacade inferenceResults;
 * Distribution<Integer> topicDistr = inferenceResults.{@link ITopicInferenceFacade#getDoc2TopicMixture() getDoc2TopicMixture()}.get(doc);
 * double probForTopic0 = topicDistr.{@link #get}(0); // probability of the topic 0 in the document
 * double probForTopic1 = topicDistr.get(1); // probability of the topic 0 in the document
 * List<Integer> topics = topicDistr.{@link #descendingElements() }; // the topics in the document, sorted according to likelihood
 * Distribution<Integer> partOfDistr = topicDistr.{@link #elementsAbove}(0.3); // a distribution that contains only topic-&gt;probability mappings, that have at least probability 0.3.
 * List<Integer> highest2 = topicDistr.{@link #highestElements}(2); // the two most probable topics
 * for(OrderedMapIterator iter = topicDistr.{@link #getOrderedMapIterator()}; iter.hasNext();){
 *      iter.next();
 *      System.out.println(iter.getKey()+" -> "+iter.getValue());
 * }
 * </pre>
 *
 * @version $Id: Distribution.java,v 1.16 2007/04/24 16:49:17 dietz Exp $
 */
public class Distribution<E> implements IDistribution<E> {
    private static final long serialVersionUID = -6462375619716109393L;
    private ValueSortedMap distribution;
    private static final double FUDGE = 0.0001;

    /**
     * Instantiates a Distribution in count mode.
     */
    protected Distribution() {
        distribution = new ValueSortedMap();
    }

    private Distribution(ValueSortedMap distribution) {
        this.distribution = distribution;
    }

    /**
     * Copy contructor. Creates a clone of the parameter.
     *
     * @param copyDistr clone-template
     */
    Distribution(IDistribution copyDistr) {
        this.distribution = new ValueSortedMap(((Distribution) copyDistr).distribution);
    }

    /**
     * Returns an distribution (with Integers as keys) which is equally distributed. I.e. distr(0)=distr(1)=...=distr(maxKey-1)
     *
     * @param maxKey boundary of the keys (keys go from 0 to maxKey-1
     * @return !=null if maxKey>0
     */
    public static IDistribution<Integer> equalDistribution(int maxKey) {
        if (maxKey < 1) {
            throw new IndexOutOfBoundsException("maxkey was " + maxKey + " but must be > 0.");
        }

        IDistribution<Integer> result = DistributionFactory.<Integer>createDistribution();
        for (int key = 0; key < maxKey; key++) {
            result.put(key, 1.0);
        }
        result.normalize();
        return result;
    }

    public static IDistribution<Integer> equalDistribution(List<Integer> citeIds) {
        IDistribution<Integer> result = DistributionFactory.<Integer>createDistribution();
        for (int key : citeIds) {
            result.put(key, 1.0);
        }
        result.normalize();
        return result;
    }

    // map wrapper

    /**
     * @param probability 0.0 &gt;= probability &lt;= 1.0,
     * @return the probability
     */
    public double put(E key, double probability) {
        if (Double.isNaN(probability) || Double.isInfinite(probability)) {
            throw new IllegalArgumentException("invalid call to put(" + key + "," + probability + ")");
        }
        if (probability < 0.0) {
            throw new IllegalArgumentException(
                    "invalid call to put(" + key + "," + probability + ") negative params are not allowed.");
        }
        try {
            assert (distribution != null);

            double res;
            Object prevVal = distribution.put(key, probability);
            if (prevVal == null) {
                res = 0.0;
            } else {
                res = (Double) prevVal;
            }

            return res;
        } catch (NullPointerException e) {
            throw new RuntimeException("failure: " + e, e);
        }
    }

    /**
     * Adds the probability to the key. If the key does not exist, it behaves just like {@link topicextraction.topicinf.datastruct.IDistribution#put}.
     *
     * @param key         element
     * @param probability may also be negative.
     * @return the probability
     */
    public double add(E key, double probability) {
        assert (key != null);
//        assert (probability != null);
        double result;
        if (containsKey(key)) {
            double oldProbability = get(key);
            result = put(key, oldProbability + probability);
        } else {
            put(key, probability);
            return 0.0;
        }
        return result;
    }

    /**
     * Returns the probability mass of the key. Returns 0.0 if this key is not contained.
     *
     * @param key element
     */
    public double get(E key) {
        final Double result = (Double) distribution.get(key);
        if (result == null) {
            return 0.0;
        }
        return result;
    }

    /**
     * checks whether there is an element with this probability.
     *
     * @return true if this map maps one or more keys to the specified value.
     */
    public boolean containsValue(double probability) {
        return distribution.containsValue(probability);
    }

    public boolean containsKey(E key) {
        return distribution.containsKey(key);
    }

    public void clear() {
        distribution.clear();
    }

    /**
     * The elements are returned in no special order
     *
     * @return the elements.
     * @see AbstractHashedMap#keySet()
     */
    public Set<E> keySet() {
        return distribution.keySet();
    }

    /**
     * @see AbstractHashedMap#remove(Object)
     */
    public double remove(E key) {
        Double res = (Double) distribution.remove(key);
        return res;
    }

    /**
     * Returns the number of elements
     *
     * @return &gt;=0
     */
    public int size() {
        return distribution.size();
    }

    public boolean isEmpty() {
        return distribution.isEmpty();
    }

    /**
     * Returns a distribution which only contains elements that have a higher probability than lowestElem (including  lowestElem).
     *
     * @return != null
     */
    public IDistribution<E> headMap(E lowestElem) {
        ValueSortedMap result = new ValueSortedMap();
        for (OrderedMapIterator iter = distribution.orderedMapIterator(); iter.hasNext();) {
            iter.next();
            result.put(iter.getKey(), iter.getValue());
            if (iter.getKey().equals(lowestElem)) {
                return new Distribution(result);
            }

        }
        return new Distribution(result);
    }

    /**
     * Returns the element with the highest probability.
     *
     * @return element
     */
    public E highest() {
        return (E) distribution.firstKey();
    }

    /**
     * Returns the element with the lowest probability
     *
     * @return element.
     */
    public E lowest() {
        return (E) distribution.lastKey();
    }

    // special operations

    /**
     * Returns the elements in descending order according to their probability (i.e. most probable entries first)
     * <p/>
     * If you do not need the ordering, use {@link #keySet()}, which is faster.
     *
     * @return a copy of the elements. Modification in this list are not reflected in the data.
     * @see LinkedMap#asList()
     */
    public List<E> descendingElements() {
        return distribution.asList();
    }

    /**
     * Returns only the mappings that have a value above (including) lowestProbability.
     *
     * @param lowestProbability
     * @return != null
     */
    public IDistribution<E> elementsAbove(double lowestProbability) {
        ValueSortedMap result = new ValueSortedMap();
        for (OrderedMapIterator iter = distribution.orderedMapIterator(); iter.hasNext();) {
            iter.next();
            Double value = (Double) iter.getValue();
            if (value < lowestProbability) {
                return new Distribution(result);
            }
            result.put(iter.getKey(), iter.getValue());

        }
        return new Distribution(result);
    }

    /**
     * Returns only the mappings that relate to one of the given keys
     *
     * @param keys
     * @return != null
     */
    public IDistribution<E> getSubMap(List<E> keys) {
        ValueSortedMap result = new ValueSortedMap();
        for (E key : keys) {
            result.put(key, get(key));
        }
        return new Distribution(result);

    }

    /**
     * Returns the map, with values shorted to two columns after the colon. E.g. 0.5432 -> 0.54.
     * This is mainly readability issues for debugging purposes.
     *
     * @return != null
     */
    public IDistribution<E> getShortenedMap() {
        ValueSortedMap result = new ValueSortedMap();
        for (OrderedMapIterator iter = distribution.orderedMapIterator(); iter.hasNext();) {
            iter.next();
            double value = (Double) iter.getValue();
            final String strValue = Double.toString(value);
            final double shortenedValue = Double.parseDouble(strValue.substring(0, Math.min(4, strValue.length())));

            if (shortenedValue >= 0.01) {
                result.put(iter.getKey(), shortenedValue);
            }
        }
        return new Distribution(result);
    }

    /**
     * Returns a list of the maxElements most probable elements.
     *
     * @param maxElements number of highest elements to return
     * @return list of elements. Modification in this list are not reflected in the data.
     */
    public List<E> highestElements(int maxElements) {
        List<E> result = new ArrayList<E>();
        int count = 0;
        for (OrderedMapIterator iter = distribution.orderedMapIterator(); iter.hasNext();) {
            iter.next();
            count++;
            result.add((E) iter.getKey());
            if (count >= maxElements) {
                return result;
            }
        }
        return result;
    }

    /**
     * returns the sum of all probabilies. If you are NOT looking at a subportion (e.g. {@link #getSubMap(java.util.List<E>)}), this sums to 1.
     *
     * @return should be approx 1.0;
     */
    public double sum() {
        double sumCache = distribution.getSumCache();
        assert (Math.abs(sumCache - safesum()) < 0.000000001) : "sumCache " + sumCache + " does not equal the safe calculation of a sum " + safesum() + ".";
        return sumCache;
    }

    private double safesum() {
        double sum = 0;
        for (Object v : distribution.values()) {
            sum += (Double) v;
        }
        return sum;
    }

    /**
     * returns the 10 most probable elements in debug-notation.
     *
     * @return for debug purposes.
     */
    public String toString() {
        String result = "[";
        List<E> elements = highestElements(10);
        for (E elem : elements) {
            result += "(" + elem + ":" + get(elem) + ") ";
        }

        result += "]";
        return result;
    }

    /**
     * returns the 10 most probable elements in debug-notation.
     *
     * @return for debug purposes.
     */
    public String toString(int numelems) {
        String result = "[";
        List<E> elements = highestElements(numelems);
        for (E elem : elements) {
            result += "(" + elem + ":" + get(elem) + ") ";
        }

        result += "]";
        return result;
    }

    /**
     * For iterating the distribution in a descending manner.
     *
     * @see LinkedMap#orderedMapIterator()
     */
    public OrderedMapIterator getOrderedMapIterator() {
        return distribution.orderedMapIterator();
    }

    /**
     * Similar to {@link #normalize()} but leaves this object untouched and returns a copy instead.
     *
     * @return
     */
    public IDistribution<E> getNormalizedDistribution() {
        IDistribution<E> result = DistributionFactory.createDistribution();
        double normC = sum();
        for (E key : keySet()) {
            Double value = get(key);
            result.put(key, value / normC);
        }
        return result;
    }

    /**
     * Normalized the distribution. After the operation it is {@link #sum()} == 1.
     *
     * @throws EmptyDistributionException if sum of entries is 0. (otherwise this would lead to NaN values)
     */
    public void normalize() throws EmptyDistributionException {
        if (keySet().size() < 1) {
            throw new EmptyDistributionException("Distribution contains no keys.");
        }
        final double normalizationC = sum();
        if (normalizationC == 0.0) {
            throw new EmptyDistributionException(keySet() + " / " + toString());
        }
        assert (normalizationC > 0.0) : "invalid normalization: normalizationC=" + normalizationC;
        for (OrderedMapIterator iter = distribution.orderedMapIterator(); iter.hasNext();) {
            iter.next();
            double val = (Double) iter.getValue() / normalizationC;
            assert (!Double.isNaN(val)) : "normalizing causes NaN Values for key " + iter.getKey() + " (normalizationC="
                    + normalizationC + "). distribution=" + this;
            iter.setValue(val);
        }
    }

    public static class EmptyDistributionException extends RuntimeException {
        public EmptyDistributionException() {
            super("This operation does not support empty distributions.");
        }

        public EmptyDistributionException(Set keys) {
            super("This operation does not support empty distributions. (keyset = " + keys + ")");
        }

        public EmptyDistributionException(String message) {
            super("This operation does not support empty distributions. " + message);
        }
    }

    /**
     * draws an element of this distribution according to a categorical process. I.e. elements with a higher value
     * are more probable to draw.
     *
     * @return element of type E
     */
    public E draw() {
        double seed = Math.random();

        for (E key : keySet()) {
            assert (!Double.isNaN(get(key))) : "get(" + key + ") is NaN";
            assert (get(key) >= 0.0) : "Distribution not normalized: 0 > get(" + key + ")=" + get(key);
            assert (get(key) <= 1.0) : "Distribution not normalized: 1 < get(" + key + ")=" + get(key);
            seed -= get(key);
            if (seed <= 0) {
                return key;
            }
        }
        if (hasOnly0Entries()) {
            System.err.println(" Distribution to draw from is empty, returning random key");
        } else {
            System.err.println("warning... returning max distr=" + this + " seed = " + seed);
        }
        return highest();
    }

    private boolean hasOnly0Entries() {
        boolean empty = true;
        for (E key : keySet()) {
            if (get(key) != 0) {
                empty = false;
            }
        }
        return empty;
    }

    public boolean hasOnlyEntriesBelow(double thresh) {
        boolean result = true;
        for (E key : keySet()) {
            if (get(key) > thresh) {
                result = false;
            }
        }
        return result;

    }

    public void overlay(IDistribution<E> distr2) {
        assert (distr2 != null);
        for (E key : distr2.keySet()) {
            add(key, distr2.get(key));
        }
    }

    /**
     * Same as {@link IDistribution#overlay(IDistribution)} with useAbsValue=false.
     */
    public void overlay(IDistribution<E> distr2, double factor) {
        overlay(distr2, factor, false);

    }

    /**
     * overlays the two distributions with a factor, so that
     * this.get(key) = this.get(key)+factor * distr2.get(key)
     *
     * @param distr2      other distr (does not get modified)
     * @param factor      factor with which distr2 is multiplied before overlaying
     * @param useAbsValue if true, the absolute value of the outcome will used, otherwise IllegalArgumentException may be thrown if factor is negative.
     */
    public void overlay(IDistribution<E> distr2, double factor, boolean useAbsValue) {
        assert (distr2 != null);
        ArrayList<E> keys = new ArrayList<E>(distr2.keySet());
        for (E key : keys) {
            double probability = get(key) + factor * distr2.get(key);
            if (useAbsValue && probability < 0.0) {
                probability = -probability;
            }
            put(key, probability);
        }
    }

    public void multiply(double factor) {
        Set<E> keys = new HashSet<E>(keySet());
        for (E key : keys) {
            put(key, factor * get(key));
        }
    }

    public void multiply(IDistribution<E> distr2) {
        Set<E> keys = new HashSet<E>(keySet());
        for (E key : keys) {
            put(key, distr2.get(key) * get(key));
        }
    }

    public boolean checkNonNaN() {
        for (E key : keySet()) {
            if (Double.isNaN(get(key))) {
                throw new RuntimeException(
                        "Distribution contains NaN Value (for key " + key + "). This is not allowed.");
            }
        }
        return true;
    }

    public double geometricMean() {
        double sum = 0.0;
        int n = 0;
        for (E key : keySet()) {
            double val = get(key);
            sum += Math.log(val);
            n++;
        }
        double normSum = sum / n;
        double mean = Math.exp(normSum);
        return mean;
    }


    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Distribution that = (Distribution) o;

        if (distribution != null ? !distribution.equals(that.distribution) : that.distribution != null) return false;

        return true;
    }

    public int hashCode() {
        return (distribution != null ? distribution.hashCode() : 0);
    }
}
