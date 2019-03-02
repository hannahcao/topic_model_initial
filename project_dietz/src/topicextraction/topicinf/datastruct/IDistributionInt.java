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
public interface IDistributionInt extends Serializable {
    double put(int key, double probability);

    double add(int key, double probability);

    double get(int key);

    boolean containsValue(double probability);

    boolean containsKey(int key);

    void clear();

    Set<Integer> keySet();

    double remove(int key);

    int size();

    boolean isEmpty();

    IDistributionInt headMap(int lowestElem);

    int highest();

    int lowest();

    List<Integer> descendingElements();

    IDistributionInt elementsAbove(double lowestProbability);

    IDistributionInt getSubMap(List<Integer> keys);

    IDistributionInt getShortenedMap();

    List<Integer> highestElements(int maxElements);

    double sum();

    String toString(int numelems);

    OrderedMapIterator getOrderedMapIterator();

    IDistributionInt getNormalizedDistribution();

    void normalize() throws Distribution.EmptyDistributionException;

    int draw();

    boolean hasOnlyEntriesBelow(double thresh);

    void overlay(IDistributionInt distr2);

    void overlay(IDistributionInt distr2, double factor);

    void overlay(IDistributionInt distr2, double factor, boolean useAbsValue);

    void multiply(double factor);

    void multiply(IDistributionInt distr2);

    boolean checkNonNaN();

    IDistribution<Integer> convert();

    boolean checkNonNull();

    void setUnmodifyable();

    void retainAllKeys(Set<Integer> integers);
}
