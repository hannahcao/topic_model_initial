package topicextraction.topicinf.datastruct;

import org.apache.commons.collections.OrderedMapIterator;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: Administrator
 * Date: Aug 19, 2007
 * Time: 10:22:07 PM
 * To change this template use File | Settings | File Templates.
 */
public interface IDistribution<E> extends Serializable {
    double put(E key, double probability);

    double add(E key, double probability);

    double get(E key);

    boolean containsValue(double probability);

    boolean containsKey(E key);

    void clear();

    Set<E> keySet();

    double remove(E key);

    int size();

    boolean isEmpty();

    IDistribution<E> headMap(E lowestElem);

    E highest();

    E lowest();

    List<E> descendingElements();

    IDistribution<E> elementsAbove(double lowestProbability);

    IDistribution<E> getSubMap(List<E> keys);

    IDistribution<E> getShortenedMap();

    List<E> highestElements(int maxElements);

    double sum();

    String toString(int numelems);

    OrderedMapIterator getOrderedMapIterator();

    IDistribution<E> getNormalizedDistribution();

    void normalize() throws Distribution.EmptyDistributionException;

    E draw();

    boolean hasOnlyEntriesBelow(double thresh);

    void overlay(IDistribution<E> distr2);

    void overlay(IDistribution<E> distr2, double factor);

    void overlay(IDistribution<E> distr2, double factor, boolean useAbsValue);

    void multiply(double factor);

    void multiply(IDistribution<E> distr2);

    boolean checkNonNaN();

    double geometricMean();
}
