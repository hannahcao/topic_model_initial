package topicextraction.topicinf.datastruct;

import cern.colt.list.IntArrayList;
import org.apache.commons.collections.map.LinkedMap;

import java.util.Map;

/**
 * A hashmap, which is traversed by the descending order of the values
 * <p/>
 * If we have a mapping {A -> 0.3, B-> 0.2, C->0.1} and a new mapping E->0.15 is added we get
 * {A -> 0.3, B-> 0.2, E->0.15, C->0.1}
 * <p/>
 * {A -> 0.3, B-> 0.2, C->0.2, D->0.1} to enter E->0.22
 * {A -> 0.3, B-> 0.2, C->0.2, E->0.22, D->0.1}
 * <p/>
 * {A -> 0.3, B-> 0.2, C->0.2, D->0.1} to enter E->0.2
 * {A -> 0.3, B-> 0.2, C->0.2, E->0.2, D->0.1}
 */
public class ValueSortedMap extends LinkedMap {
    //    private static final long serialVersionUID = -2951351118150839815L;
    private static final long serialVersionUID = 5257135751154177586L;
    private double sumCache = 0.0;

    public ValueSortedMap() {
        super();
        calculateSafeSum();
    }

    public ValueSortedMap(ValueSortedMap copyConst) {
        super(copyConst);
        calculateSafeSum();
    }

    protected void init() {
        header = new MyLinkEntry(null, -1, null, null);
        MyLinkEntry header = (MyLinkEntry) this.header;
        header.setBefore(header);
        header.setAfter(header);
    }

    public Object put(Object key, Object value) {
        Object previousValue = remove(key);
        super.put(key, value);
        if (previousValue != null && previousValue instanceof Number) {
            sumCache -= ((Number) previousValue).doubleValue();
        }
        if (value instanceof Number) {
            sumCache += ((Number) value).doubleValue();
        }
//        calculateSafeSum();
        return previousValue;
    }


    protected void addEntry(HashEntry hashEntry, int hashIndex) {
        MyLinkEntry link = (MyLinkEntry) hashEntry;
        MyLinkEntry insertPoint = entryOfNearestValue(link.getNumberValue());

        link.setAfter(insertPoint);
        link.setBefore(insertPoint.getBefore());
        insertPoint.getBefore().setAfter(link);
        insertPoint.setBefore(link);
        data[hashIndex] = hashEntry;
    }

    /**
     * Gets the index of the specified value.
     *
     * @param val the value to find the index of
     * @return the index, or -1 if not found
     */
    public int indexOfNearestValue(Double val) {
        if (val == null) {
            throw new NullPointerException("value must != null");
        }
        int i = 0;

        // assumption: descending
        for (MyLinkEntry entry = getHeader().getAfter(); entry != getHeader(); entry = entry.getAfter(), i++) {
            if (entry.getNumberValue() < val) {
                return i;
            }
        }
        return size();
    }

    /**
     * Use the returned value to insert a new entry before.
     *
     * @param val
     * @return
     */
    public MyLinkEntry entryOfNearestValue(Double val) {
        if (val == null) {
            throw new NullPointerException("value must != null");
        }
        int i = 0;

        // assumption: descending
        for (MyLinkEntry entry = getHeader().getAfter(); entry != getHeader(); entry = entry.getAfter(), i++) {
            if (entry.getNumberValue() < val) {
                return entry;
            }
        }
        return getHeader();
    }

    public IntArrayList indexListOfValue(Object val) {
        IntArrayList result = new IntArrayList();
        if (val == null) {
            throw new NullPointerException("value must != null");
        }
        int i = 0;

        for (MyLinkEntry entry = getHeader().getAfter(); entry != getHeader(); entry = entry.getAfter(), i++) {
            if (isEqualValue(val, entry.getValue())) {
                result.add(i);
                // todo stopping criterion. (All entries with the same values are neighbors)
                // todo optimization. (if <0.5 search backwards, if >0.5 search forwards)
            }
        }
        return result;
    }

    public MyLinkEntry getHeader() {
        return (MyLinkEntry) header;
    }

    protected HashEntry createEntry(HashEntry next, int hashCode, Object key, Object value) {
        return new MyLinkEntry(next, hashCode, key, value);
    }

    public int numberOfStrictlyBelowElems(double val) {
        int count = 0;
        for (ValueSortedMap.MyLinkEntry entry = getHeader().getAfter(); entry != getHeader(); entry = entry.getAfter())
        {
            if (entry.getNumberValue() < val) {
                count++;
            }
        }

        return count;
    }

    public int numberOfStrictlyAboveElems(double val) {
        int count = 0;
        for (ValueSortedMap.MyLinkEntry entry = getHeader().getAfter(); entry != getHeader(); entry = entry.getAfter())
        {
            if (entry.getNumberValue() > val) {
                count++;
            }
        }

        return count;
    }

    public int numberEqualElems(double val) {
        int count = 0;
        for (ValueSortedMap.MyLinkEntry entry = getHeader().getAfter(); entry != getHeader(); entry = entry.getAfter())
        {
            if (entry.getNumberValue() == val) {
                count++;
            }
        }

        return count;
    }

    protected class MyLinkEntry extends LinkEntry {
        /**
         * Constructs a new entry.
         *
         * @param next     the next entry in the hash bucket sequence
         * @param hashCode the hash code
         * @param key      the key
         * @param value    the value
         */
        protected MyLinkEntry(HashEntry next, int hashCode, Object key, Object value) {
            super(next, hashCode, key, value);
            Double x = (Double) value;
        }

        protected void setBefore(MyLinkEntry b) {
            before = b;
        }

        protected MyLinkEntry getBefore() {
            return (MyLinkEntry) before;
        }

        protected void setAfter(MyLinkEntry a) {
            after = a;
        }

        protected MyLinkEntry getAfter() {
            return (MyLinkEntry) after;
        }

        public Object setValue(Object value) {
            Double n = (Double) value;
            return super.setValue(value);
        }

        public Double getNumberValue() {
            return (Double) getValue();
        }


    }

    // //////////////////////////////////////////////////////
    // extra precautions for keeping the sumCache up to date.
    // /////////////////////////////////////////////////////


    public double getSumCache() {
        return sumCache;
    }

    public Object remove(int i) {
        Object previousValue = super.remove(i);
        if (previousValue instanceof Number) {
            sumCache -= ((Number) previousValue).doubleValue();
        }
        return previousValue;
    }

    public Object remove(Object object) {
        Object previousValue = super.remove(object);
        if (previousValue instanceof Number) {
            sumCache -= ((Number) previousValue).doubleValue();
        }
        return previousValue;
    }


    public void putAll(Map map) {
        super.putAll(map);
        calculateSafeSum();
    }

    private void calculateSafeSum() {
        sumCache = 0.0;
        for (Object value : values()) {
            if (value instanceof Number) {
                sumCache += ((Number) value).doubleValue();
            }
        }

    }

    public void clear() {
        super.clear();
        sumCache = 0.0;
    }


}
