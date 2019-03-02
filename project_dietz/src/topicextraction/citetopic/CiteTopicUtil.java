package topicextraction.citetopic;

//Huiping commented out the incorrect import (those classed do not exist)
//Feb. 14, 2012
//import topicextraction.citetopic.realdata.CitesExperimentResultData;
//import topicextraction.citetopic.realdata.IAnalyzeAbstractModel;
//import topicextraction.citetopic.realdata.IEvaluationReader;
import topicextraction.citetopic.realdata.ITopicModelWrapper;
//import topicextraction.citetopic.realdata.LdaWrapper;
//import topicextraction.querydb.IAuthor;
import topicextraction.querydb.IDocument;
import topicextraction.topicinf.datastruct.DistributionFactory;
import topicextraction.topicinf.datastruct.IDistribution;
import topicextraction.topicinf.datastruct.IDistributionInt;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Utilities for topic based citation analysis.
 */
public class CiteTopicUtil {
    private static final int PRECISION = 2;
    public static final String POST_JENSEN_SHANNON = "POST_JENSEN_SHANNON";
    public static final String POST_POSTERIOR = "POST_POSTERIOR";

    /**
     * Convert the distribution in bugs indices (1...C) to a distribution over elements of citeIds.
     *
     * @param gammaBugsList gammas in bugs indices
     * @param citeIds
     * @param ldaWrapper
     * @return gamma distribution with citeIds indices
     * 
     * Huiping commented out this Feb. 14, 2012
     */
    /*public static Map<Integer, IDistribution<Integer>> bugsIndex2CiteId(List<IDistribution<Integer>> gammaBugsList, List<Integer> citeIds, LdaWrapper ldaWrapper) {
        Map<Integer, IDistribution<Integer>> result = new HashMap<Integer, IDistribution<Integer>>();
        int bugsId = 0;
        for (IDistribution<Integer> gammaBugs : gammaBugsList) {
            int pubId = ldaWrapper.getBugsId2PubId(bugsId);
            IDistribution<Integer> gammaCite = DistributionFactory.<Integer>createDistribution();
            for (int bugsindex : gammaBugs.keySet()) {
                gammaCite.put(citeIds.get(bugsindex), gammaBugs.get(bugsindex));
            }

            result.put(pubId, gammaCite);
            bugsId++;
        }
        return result;
    }*/

    //Huiping commented out this Feb. 14, 2012
    /*public static List<IDistribution<Integer>> readThetaCite(List<Integer> citeIds, Map<Integer, IDocument> pubId2Docs, LdaWrapper ldaWrapper) {

        List<IDistribution<Integer>> thetaCite = new ArrayList<IDistribution<Integer>>();
        for (int citeId : citeIds) {
            IDocument citeDoc = pubId2Docs.get(citeId);

            IDistribution<Integer> distr = ldaWrapper.getTheta(citeDoc);
            thetaCite.add(distr);
        }
        return thetaCite;
    }*/

    //Huiping commented out this Feb. 14, 2012
    /*public static List<Double> generateDocLengths(int dnum, LdaWrapper ldaWrapper, Map<Integer, IDocument> pubId2Docs) {
        List<Double> docLen = new ArrayList<Double>(dnum);
        for (int dbugs = 0; dbugs < dnum; dbugs++) {
            int pubid = ldaWrapper.getBugsId2PubId(dbugs);
            IDocument pubDoc = pubId2Docs.get(pubid);
            int len = ldaWrapper.getDocLengths(pubDoc);
            docLen.add((double) len);
        }
        return docLen;
    }*/

    /**
     * Entropy is maximized, if the distribution is equiprobable. This leads to the fact, that the outcome of an
     * experiment with this distribution is unpredictable.
     *
     * @param distr
     * @return
     */
    public static double entropy(IDistribution<Integer> distr) {
        assert (distr.sum() < 0.0001 || distr.sum() > 0.999 && distr.sum() < 1.0001) :
                "distribution must be normalized, but sum is " + distr.sum();
        double sum = 0;
        for (int key : distr.keySet()) {
            double prob = distr.get(key);
            sum += prob * Math.log10(prob) / Math.log10(2);
        }
        return -sum;
    }

    public static String cut(double value) {
        return CiteTopicUtil.cut(value, PRECISION);
    }

    public static String cut(double value, int precision) {
        double tenPower = Math.pow(10, precision);
        double scaledInt = Math.rint(value * tenPower);
        double cuttedVal = scaledInt / tenPower;
        return "" + cuttedVal;

    }

    public static Double cutDouble(double value, int precision) {
        double tenPower = Math.pow(10, precision);
        double scaledInt = Math.rint(value * tenPower);
        double cuttedVal = scaledInt / tenPower;
        return cuttedVal;

    }

    /**
     * Cuts double to {@link #PRECISION} decimal places, but ensures, that the distribution sums to 1.
     * <p/>
     * This is done by first normalizing, and then adding/substracting the rounding rest to the highest key of the
     * distribution.
     *
     * @param distr != null (this object will be modified)
     * @return pointer to the passed in distr object.
     * 
     * Huiping commented out this Feb. 14, 2012
     */
    
//    public static Distribution<Integer> cut(Distribution<Integer> distr) {
//        distr.normalize();
//        ArrayList<Integer> keys = new ArrayList<Integer>(distr.keySet());
//        for (int key : keys) {
//            double value = distr.get(key);
//            distr.put(key, cutDouble(value, 4));
//        }
//        double roundErr = 1.0 - distr.sum();
//        Integer highKey = distr.highest();
//        distr.add(highKey, roundErr);
//        return distr;
//    }
    public static <T> IDistribution<T> cut(IDistribution<T> distr) {
        distr.normalize();
        ArrayList<T> keys = new ArrayList<T>(distr.keySet());
        for (T key : keys) {
            double value = distr.get(key);
            distr.put(key, cutDouble(value, 4));
        }
        double roundErr = 1.0 - distr.sum();
        T highkey = distr.highest();
        distr.add(highkey, roundErr);
        return distr;
    }

    /**
     * String edit distance (as found on wikipedia)
     *
     * @param s
     * @param t
     * @return
     */
    public static int levenshteinDistance(List<Integer> s, List<Integer> t) {
        // d is a table with m+1 rows and n+1 columns
        final int m = s.size();
        final int n = t.size();
        int[][] d = new int[m + 1][n + 1];

        for (int i = 0; i < m; i++) {
            d[i][0] = i;
        }
        for (int j = 0; j < n; j++) {
            d[0][j] = j;
        }

        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                int cost;
                if (s.get(i) == t.get(j)) {
                    cost = 0;
                } else {
                    cost = 1;
                }
                d[i][j] = Math.min(Math.min(d[i - 1][j] + 1,     // deletion
                        d[i][j - 1] + 1),     // insertion
                        d[i - 1][j - 1] + cost   // substitution
                );
            }
        }
        return d[m][n];
    }

    /**
     * The Kendall tau coefficient (?) has the following properties:
     * <p/>
     * If the agreement between the two rankings is perfect (i.e., the two rankings are the same) the coefficient has value 1.
     * If the disagreement between the two rankings is perfect (i.e., one ranking is the reverse of the other) the coefficient has value -1.
     * For all other arrangements the value lies between -1 and 1, and increasing values imply increasing agreement between the rankings. If the rankings are completely independent, the coefficient has value 0.
     * <p/>
     * Kendall tau coefficient is defined
     * <p/>
     * \tau = \frac{2P}{\frac{1}{2}{n(n-1)}} - 1 = \frac{4P}{n(n-1)} - 1
     *
     * @return -1 &lt;= result &lt;= 1
     * @see <a href="http://en.wikipedia.org/wiki/Kendall%27s_tau">Wikipedia</a>
     */
    public static double kendallTauDistance(List<Integer> s, List<Integer> t) {
        assert (s.size() == t.size()) : "lists must be of same size. s:" + s.size() + " t:" + t.size();
        Map<Integer, Integer> ranks = new HashMap<Integer, Integer>();
        for (int i = 0; i < s.size(); i++) {
            ranks.put(s.get(i), i);
        }

        double p = 0;
        for (int i = 0; i < t.size(); i++) {
            for (int j = i + 1; j < t.size(); j++) {
                final Integer ikey = t.get(i);
                final Integer jkey = t.get(j);
                assert (ranks.get(ikey) != null) : ikey + "," + jkey + " " + ranks + " s=" + s + " t=" + t;
                assert (ranks.get(jkey) != null) : ikey + "," + jkey + " " + ranks + " s=" + s + " t=" + t;
                if (ranks.get(ikey) < ranks.get(jkey)) {
                    p++;
                }
            }
        }

        final double n = s.size();
        double tau = (4.0 * p) / (n * (n - 1)) - 1.0;
        return tau;

    }

    //Huiping commented out this Feb. 14, 2012
//    public static Set<Integer> flattenValues(Map<Integer, List<Integer>> pubId2CiteIds) {
//        Set<Integer> result = new HashSet<Integer>();
//        for (List<Integer> val : pubId2CiteIds.values()) {
//            result.addAll(val);
//        }
//        return result;
//    }

    public static Set<Integer> flattenValues(Map<Integer, ? extends Collection<Integer>> pubId2CiteIds) {
        Set<Integer> result = new HashSet<Integer>();
        for (Collection<Integer> val : pubId2CiteIds.values()) {
            result.addAll(val);
        }
        return result;
    }

    public static Set<String> flattenValuesKeys(Map<String, Map<String, Integer>> signifTests) {
        Set<String> result = new HashSet<String>();
        for (Map<String, Integer> val : signifTests.values()) {
            result.addAll(val.keySet());
        }
        return result;

    }

    public static <T> List<T> readParamList(String propertyPrefix, T classPrototype) {
        return readParamList(propertyPrefix, classPrototype, new ArrayList<T>());
    }

    public static <T> List<T> readParamList(String propertyPrefix, T classPrototype, List<T> defaultVal) {
        List<T> propList = new ArrayList<T>();
        int expIdsNo = 0;
        while (true) {
            String propEntry_ = System.getProperty(propertyPrefix + expIdsNo, "");
            if (propEntry_.length() < 1) {
                break;
            } else {
                if (classPrototype instanceof Integer) {
                    Integer propEntry = Integer.parseInt(propEntry_);
                    propList.add((T) propEntry);
                } else if (classPrototype instanceof Double) {
                    Double propEntry = Double.parseDouble(propEntry_);
                    propList.add((T) propEntry);
                } else if (classPrototype instanceof String) {
                    propList.add((T) propEntry_);
                }
                expIdsNo++;
            }
        }
        if (expIdsNo == 0) {
            return defaultVal;
        }
        return propList;
    }

    public static List<IDistribution<String>> phiInt2PhiWords(final ITopicModelWrapper ldaWrapper) {
        List<IDistribution<Integer>> phiInts = ldaWrapper.getPhis();

        List<IDistribution<String>> phis = new ArrayList<IDistribution<String>>();
        for (int t = 0; t < phiInts.size(); t++) {
            IDistribution<String> phiWord = DistributionFactory.createDistribution();
            IDistribution<Integer> phiInt = phiInts.get(t);
            for (int w : phiInt.keySet()) {
                String word = ldaWrapper.getWordForIndex(w);
                phiWord.put(word, phiInt.get(w));
            }
            phis.add(phiWord);
        }
        return phis;
    }

    // Huiping commented out the following several funcstions on Feb. 14, 2012
    /*public static void analyzeAbstract(int pubid, IAnalyzeAbstractModel model, CitesExperimentResultData chainResult, String experiment, String filename, IEvaluationReader evaluationReader) {
        Map<Integer, String> spreadAbstract = model.analyzeAbstract(pubid);
        Map<String, Object> info = model.getInfo();
        info.putAll(model.getInfoForPub(pubid));


        IDistribution<Integer> gamma = chainResult.getGammaResult().get(pubid);
        SpreadAbstractCsvWriter writer = new SpreadAbstractCsvWriter(experiment, chainResult.getPubId2CiteIds(), chainResult.getPubId2Docs(),
                spreadAbstract, pubid, gamma, evaluationReader);
        writer.addInfo(info);
        try {
            writer.writeToXls(new FileWriter(filename));
        } catch (IOException e) {
            throw new RuntimeException(e);  //todo handle
        }
    }

    public static void analyzeAbstract(int pubid, CitesExperimentResultData chainResult, String experiment, String filename, IEvaluationReader evaluationReader) {
        Map<Integer, Map<Integer, String>> abstracts = chainResult.getSpreadAbstracts();
        Map<Integer, String> spreadAbstract = abstracts.get(pubid);


        IDistribution<Integer> gamma = chainResult.getGammaResult().get(pubid);
        SpreadAbstractCsvWriter writer = new SpreadAbstractCsvWriter(experiment, chainResult.getPubId2CiteIds(), chainResult.getPubId2Docs(),
                spreadAbstract, pubid, gamma, evaluationReader);

        try {
            writer.writeToXls(new FileWriter(filename));
        } catch (IOException e) {
            throw new RuntimeException(e);  //todo handle
        }
    }

    public static void analyzeAbstractHtml(int pubid, CitesExperimentResultData chainResult, String experiment, String filename, IEvaluationReader evaluationReader) {
        Map<Integer, Map<Integer, String>> abstracts = chainResult.getSpreadAbstracts();
        Map<Integer, String> spreadAbstract = abstracts.get(pubid);


        IDistribution<Integer> gamma = chainResult.getGammaResult().get(pubid);
        SpreadAbstractHtmlWriter writer = new SpreadAbstractHtmlWriter(experiment, chainResult.getPubId2CiteIds(), chainResult.getPubId2Docs(),
                spreadAbstract, pubid, gamma, evaluationReader);

        try {
            writer.writeToXls(new FileWriter(filename));
        } catch (IOException e) {
            throw new RuntimeException(e);  //todo handle
        }

    }


    public static String getFirstAuthorEtAlFromIAuthor(List<IAuthor> authors) {
        List<String> anames = new ArrayList<String>();
        for (IAuthor a : authors) {
            anames.add(a.getName());
        }
        return getFirstAuthorEtAl(anames);
    }*/


    public static String getFirstAuthorEtAl(List<String> authorNames) {
        String authorShortString = "";
        if (authorNames.size() > 0) {
            String a = authorNames.get(0);
            String[] splitStr = a.split(" ");
            if (splitStr.length > 0) {
                authorShortString += splitStr[splitStr.length - 1];
            } else {
                authorShortString = a;
            }

        }
        if (authorNames.size() > 1) {
            authorShortString += " et al.";
        }
        return authorShortString;
    }

    // Huiping commented out this function Feb. 14, 2012
    /*public static String getAllAuthorsFromIAuthor(List<IAuthor> authors) {
        List<String> anames = new ArrayList<String>();
        for (IAuthor a : authors) {
            anames.add(a.getName());
        }
        return getAllAuthors(anames);
    }*/


    public static String getAllAuthors(List<String> authorNames) {

        String authorShortString = "";
        for (String authorName : authorNames) {
            if (authorName != null) {
                String[] splitStr = authorName.split(" ");
                if (splitStr.length > 0) {
                    authorShortString += splitStr[splitStr.length - 1];
                } else {
                    authorShortString = authorName;
                }
                if (authorShortString.length() > 1) {
                    authorShortString += ", ";
                }

            }
        }
        // remove last comma.
        if (authorShortString.length() > 2) {
            authorShortString = authorShortString.substring(0, authorShortString.length() - 2);
        }
        return authorShortString;
    }

    /**
     * Simulates a draw from a multinomlial distribution represented by distr (keys are the indices).
     * The array may be unnormalized.
     *
     * @param distr an array containing the probabilities; distr.length == 0 is prohibited
     * @param sum   the sum of the numbers in distr (we do not check this!)
     * @return one of the indices of distr. the large the value at that index, the larger the probability that it gets returned.
     */
    public static int draw(double[] distr, double sum) {
        if (distr.length == 0) {
            throw new IllegalArgumentException("distr.length == 0");
        }

        double seed = Math.random();

        if (Math.abs(sum) < 0.000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000001) {
//            System.err.println("Distribution to draw from is empty, returning random key");
            return (int) Math.floor(seed * distr.length);
        }


        for (int i = 0; i < distr.length; i++) {
            seed -= distr[i] / sum;
            if (seed <= 0) {
                return i;
            }
        }
        return (int) Math.floor(seed * distr.length);

    }


    public static ArrayList<List<Integer>> reverseMaps(List<List<Integer>> maps) {
        ArrayList<List<Integer>> revMap = new ArrayList<List<Integer>>();

        int maxEntry = 0;
        for (int key = 0; key < maps.size(); key++) {
            List<Integer> value = maps.get(key);
            for (int entry : value) {
                while (revMap.size() < entry + 1) {
                    revMap.add(new ArrayList<Integer>());
                }

                revMap.get(entry).add(key);
                if (key > maxEntry) maxEntry = key;
            }
        }

        // make sure that the map contains empty entries for all keys of the original mapping 
        while (revMap.size() < maxEntry + 1) {
            revMap.add(new ArrayList<Integer>());
        }

        return revMap;
    }

    /**
     * Name given to an algorithm for extending a partial order relation to a total order.
     * <p/>
     * The input is a set of ordered pairs i -> j, meaning "i must come before j".
     * The output is a permutation of 1, ..., n such that i is located before j whenever i -> j.
     * Of course, for some inputs (those that specify a loop) this is not possible; the algorithm reports this error condition.
     * <p/>
     * Here's the algorithm:
     * <p/>
     * 1. Compute and store the in degree d[j] for each j (this is the number of i's for which i -&gt; j).
     * <p/>
     * 2. Initialise a collection (typically a queue or a stack, doesn't matter which) C to {j : d[j] == 0}.
     * <p/>
     * 3. While C is not empty:
     * <p/>
     * Remove some element i from C.
     * <p/>
     * Output i.
     * <p/>
     * For each j such that i -> j, decrement d[d]; if this zeros it, add j to C.
     * <p/>
     * 4. If we output n values in the previous step, the sort succeeded; otherwise it failed, and a loop exists in the input.
     * <p/>
     * Properly implemented, the algorithm runs in time complexity O(n2) on a RAM machine. This is quite easy in practice!
     * Just store a linked list of j's for which i -> j for each i; use this step to calculate d[], too.
     * <p/>
     * See http://everything2.com/?node_id=556079 http://everything2.com/?node_id=556079
     *
     * @param dependencies mappings i -&gt; j indicate that "i must come before j"
     * @return
     * @throws topicextraction.citetopic.TopologicalSortException
     *          if there is a conflict
     */
    public static List<Integer> topologicalSort(List<List<Integer>> dependencies, int fromId, int toId) throws TopologicalSortException {

        // collect all ids
        Set<Integer> allIds = new HashSet<Integer>();
        for (int i = 0; i < dependencies.size(); i++) {
            if (i >= fromId && i < toId) {
                allIds.add(i);
            }
            for (int j : dependencies.get(i)) {
                if (j >= fromId && j < toId) {
                    allIds.add(j);
                }
            }
        }

        int[] indegree = new int[toId];
        for (int i = 0; i < toId; i++) {
            indegree[i] = -1;
        }
        for (int i : allIds) {
            indegree[i] = 0;
        }

        for (int from = 0; from < dependencies.size(); from++) {
            if (from >= fromId && from < toId) {
                for (int to : dependencies.get(from)) {
                    if (to >= fromId && to < toId) {
                        indegree[to]++;
                    }
                }
            }
        }

        List<Integer> result = new ArrayList<Integer>();

        boolean changed = false;
        while (!allIds.isEmpty()) {
            changed = false;
            for (int i = 0; i < toId; i++) {
                if (indegree[i] == 0) {
                    allIds.remove(i);
                    result.add(i);
                    indegree[i] = -1;
                    for (int to : dependencies.get(i)) {
                        indegree[to]--;
                        changed = true;
                    }
                }
            }
            if (!changed) throw new TopologicalSortException(result, allIds, indegree);
        }


        return result;
    }

    /**
     * Returns the complete N:M mapping, where N ranges from fromStart (inclusive) to fromEnd (exclusive) and M ranges from toStart to toEnd.
     *
     * @return not null
     */
    public static List<List<Integer>> getManyToMany(int fromStart, int fromEnd, int toStart, int toEnd) {
        List<List<Integer>> result = new ArrayList<List<Integer>>();
        for (int f = fromStart; f < fromEnd; f++) {
            ArrayList<Integer> list = new ArrayList<Integer>();
            for (int t = toStart; t < toEnd; t++) {
                list.add(t);
            }
            result.add(list);
        }
        return result;
    }

    public static Set<Integer> breadthFirstSearch(List<Integer> startIds, Map<Integer, List<Integer>> pubId2CiteIds) {
        final List<Integer> mainNodes = new LinkedList<Integer>();
        for (int id : startIds) {
            if (pubId2CiteIds.containsKey(id)) {
                mainNodes.add(id);
            }
        }
        assert (!mainNodes.isEmpty()) : "startIds = " + startIds;
        Set<Integer> bfsResult = new HashSet<Integer>();
        LinkedList<Integer> q = new LinkedList<Integer>();
        q.addAll(mainNodes);

        bfsResult.addAll(mainNodes);
        while (!q.isEmpty()) {
            Integer p = q.removeFirst();
            if (p != null) {
                try {
                    assert (pubId2CiteIds != null);
                    if (pubId2CiteIds.containsKey(p)) {
                        for (Integer linked : pubId2CiteIds.get(p)) {
//                    for (Iterator iter = p.edges(); iter.hasNext();) {
                            if (linked != null) {
                                if (!bfsResult.contains(linked)) {
                                    q.add(linked);
                                    bfsResult.add(linked);
                                }
                            }
                        }
                    }
                } catch (NullPointerException e) {
                    e.printStackTrace();
                }
            } else {
                System.out.println("CiteTopicUtil.breadthFirstSearch: node null contained in q list");
            }
        }
        return bfsResult;
    }


    public static String toString(Map<Integer, IDistributionInt> map) {
        StringBuffer buf = new StringBuffer();
        for (int key : map.keySet()) {
            buf.append("\n" + key + "->" + map.get(key));
        }
        return buf.toString();
    }

    private final static long[][] stirlingCache = new long[100][100];

    /**
     * Caclulates stirling numbers according to the recurrence scheme given in YWT: Hirarchical Dirichlet Processes.
     * <p/>
     * This method is thread safe, synchronized in the stirling cache.
     *
     * @param n
     * @param m
     * @return
     */
    public static long stirling(int n, int m) throws StirlingOutOfRangeException {
        long result;
        if (n == 0 && m == 0) result = 1;
        else if (n == 1 && m == 1) result = 1;
        else if (m == 0) result = 0;
        else if (m > n) result = 0;
        else if (n == m) result = 1;
        else {
            if (n < 100 && m < 100) {
                long stir = -1;
                synchronized (stirlingCache) {
                    stir = stirlingCache[n][m];
                }
                if (stir > 0) {
                    if (stir < 0) throw new StirlingOutOfRangeException(n, m);
                    result = stir;
                } else {
                    int n_ = n - 1;
                    stir = stirling(n_, m - 1) + n_ * stirling(n_, m);
                    if (n < 100 && m < 100) {
                        synchronized (stirlingCache) {
                            stirlingCache[n][m] = stir;
                        }
                    }
                    if (stir < 0) throw new StirlingOutOfRangeException(n, m);
                    result = stir;
                }
            } else {
                int n_ = n - 1;
                long stir = stirling(n_, m - 1) + n_ * stirling(n_, m);
                if (stir < 0) throw new StirlingOutOfRangeException(n, m);
                result = stir;
            }
        }

        return result;
    }

    public static class StirlingOutOfRangeException extends RuntimeException {
        private int n;
        private int m;

        public StirlingOutOfRangeException(int n, int m) {
            super("stirling(" + n + "," + m + ") got Long overflow");
            this.n = n;
            this.m = m;
        }

    }

    // YWT's stirling routine
//function [ss, lmss]  = stirling(nn);
//
//% returns the unsigned stirling numbers of the first kind.
//% ss(nn,tt) for tt=1:nn
//
//persistent maxnn allss logmaxss
//
//if isempty(maxnn)
//  maxnn = 1;
//  allss = {1};
//  logmaxss = 0;
//end
//
//if nn > maxnn
//  allss{nn} = [];
//  logmaxss(nn) = 0;
//  for mm=maxnn+1:nn
//    allss{mm} = [allss{mm-1}*(mm-1) 0] + [0 allss{mm-1}];
//    mss = max(allss{mm});
//    allss{mm} = allss{mm}/mss;
//    logmaxss(mm) = logmaxss(mm-1) + log(mss);
//  end
//  maxnn = nn;
//end
//
//ss = allss{nn};
//lmss = logmaxss(nn);
//

}

