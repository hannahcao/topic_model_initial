package topicextraction.citetopic.sampler;

import cern.colt.matrix.DoubleMatrix1D;
import topicextraction.querydb.IDocument;
import topicextraction.topicinf.datastruct.DistributionFactory;
import topicextraction.topicinf.datastruct.IDistribution;
import util.matrix.IMatrix2D;
import util.matrix.IMatrixMin2D;
import org.apache.commons.collections.BidiMap;
import org.apache.commons.collections.bidimap.DualHashBidiMap;
import org.apache.lucene.analysis.*;

import java.io.IOException;
import java.io.Reader;
import java.io.Serializable;
import java.io.StringReader;
import java.util.*;

/**
 * Provides matrix generation out of the document collection.
 * <p/>
 * This is an improved version for {@link topicextraction.topicinf.TopicInferenceVocabAndDocs}.
 * <p/>
 * <p/>
 * All words are contained in the origvocabulary, whereas the vocabulary only contains words, that are contained in more
 * than one document.
 *
 * @version $Id: VocDocHasher.java,v 1.3 2007/04/11 18:20:31 dietz Exp $
 */
public class VocDocHasher implements Serializable {
    private static final long serialVersionUID = -6201441084323524139L;
    private Map<String, Integer> vocabulary;
    private final Analyzer stemmer;
    /**
     * holds a list of all words that occur within documents (even those, that occur only in one document
     */
    private List<String> origVocabInverse;
    /**
     * holds a mapping the words in {@link #origVocabInverse} to their index within the list
     */
    private Map<String, Integer> origVocabulary;
    /**
     * each element holds a list of the documents, in which the word occurs. this list uses the same order of elements as in {@link #origVocabInverse}. you can use {@link #origVocabulary} for getting string -> freq.
     * <p/>
     * origVocabInverse2Doc.get(i) contains the document indices of all documents that contain the word origVocabInverse.get(i)
     */
    private List<List<Integer>> origVocabInverse2Doc;
    private List<String> vocabInverse;
    private final Map<Integer, IDocument> docs;
    private Map<IDocument, Integer> docInverse;
    private final int minWordsOccurence;
    private ArrayList<List<Integer>> origVocabInverse2DocFrequency;

    public VocDocHasher(Map<Integer, IDocument> docs) {
        this(docs, 2);
    }

    
    /**
     * Huiping noted 2012-02-29
     * 
     * @param docs:	map in paper_id:paper_content
     * 				from which the vocabulary information will be derived
     * @param minWordsOccurence
     */
    public VocDocHasher(Map<Integer, IDocument> docs, int minWordsOccurence) {
        String stemmingType = System.getProperty("torel.stemm", "nostemm");
        if ("nostemm".equals(stemmingType)) {
            stemmer = new NonStemmingAnalyzer();
        } else {
            stemmer = new PorterAnalyzer();
        }


        this.minWordsOccurence = minWordsOccurence;
        this.docs = docs;
        docInverse = hashDocs(new ArrayList<IDocument>(docs.values()));

        createVocabulary();

    }

    public void clearCaches() {
        origVocabInverse2Doc.clear();
        origVocabInverse2Doc = null;
        origVocabInverse2DocFrequency.clear();
        origVocabInverse2DocFrequency = null;
        docInverse.clear();
        docInverse = null;

        origVocabInverse.clear();
        origVocabInverse = null;
        origVocabulary.clear();
        origVocabulary = null;

    }

    
    /**
     * Huiping noted 2012-02-29
     * 
     * Calculated the reverted list of all the documents
     * 
     * @param docs
     * @return map from IDocument:document_index in the list
     */
    private Map<IDocument, Integer> hashDocs(List<IDocument> docs) {
        HashMap<IDocument, Integer> result = new HashMap<IDocument, Integer>();
        for (int d = 0; d < docs.size(); d++) {
            result.put(docs.get(d), d);
        }
        return result;
    }

    /**
     * Calculates the word document matrix . Words that occur only in one document will not be reflected.
     * <p/>
     * This method also calculates the fields:<ul>
     * <li>{@link #origVocabInverse} containing all vocabulary (including those words that occur only in one document
     * <li>{@link #origVocabulary}  contains a reverse mapping of origVocabInverse: word -&gt; index
     * <li>{@link #origVocabInverse2Doc} contains the number of documents that contain the word
     * <li>{@link #vocabInverse} containing only the words that are used during the inference calculation
     * <li>{@link #vocabulary} reverse mapping of vocabInverse (word -&gt; index)
     * </ul>
     */
    public IMatrix2D calculateWordDocumentMatrix(BidiMap pub2bugsIds, IMatrix2D instance) {
        return (IMatrix2D) calculateWordDocumentMatrixMin(pub2bugsIds, instance);
    }

    public IMatrixMin2D calculateWordDocumentMatrixMin(BidiMap pub2bugsIds, IMatrixMin2D instance) {

        try {
            assert (instance.getDimensionSize(0) == vocabInverse.size()) :
                    "dimensionsizes do not match in dim 0: instance.getDimensionSize(0)=" + instance.getDimensionSize(
                            0);
            assert (instance.getDimensionSize(1) == Collections.<Integer>max(pub2bugsIds.values()) + 1) :
                    "max of pub2bugsIds does not match in dim 0: instance.getDimensionSize(0)=" + instance.getDimensionSize(
                            0) + "  Collections.<Integer>max(pub2bugsIds.values()=" + Collections.<Integer>max(pub2bugsIds.values());


            for (Object pubid_ : pub2bugsIds.keySet()) {
                int pubid = (Integer) pubid_;
                int bugid = getPubid2BugsId(pubid, pub2bugsIds);

                IDocument doc = docs.get(pubid);
                String text = doc.getText();
                assert (text.length() > 1);
                TokenStream tokenStream = getStemmer().tokenStream("", new StringReader(text));
                for (Token tok = tokenStream.next(); tok != null; tok = tokenStream.next()) {
                    final String wordstem = tok.termText();
                    Integer vocabIndex = vocabulary.get(wordstem);
                    if (vocabIndex != null) {
                        instance.add(vocabIndex, bugid, 1.0);
                    }
                }
            }

            return instance;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * Huiping noted 2012-02-29
     * 
     * Extract vocabularies from documents 
     * 
     */
    private void createVocabulary() {
        try {
            // calculate origVocabulary
            origVocabulary = new HashMap<String, Integer>();
            origVocabInverse = new ArrayList<String>();
            origVocabInverse2Doc = new ArrayList<List<Integer>>();
            /** each element holds a list of the frequency with wich a word occurs in the document.*/
            origVocabInverse2DocFrequency = new ArrayList<List<Integer>>();


            assert (docs.size() > 0);

//        for (int d = 0; d < docs.size(); d++) {
            for (int pubid : docs.keySet()) {
                IDocument doc = docs.get(pubid);
                String text = doc.getText();
                assert (text.length() > 1);
                TokenStream tokenStream = getStemmer().tokenStream("", new StringReader(text));
                for (Token tok = tokenStream.next(); tok != null; tok = tokenStream.next()) {
                    final String wordstem = tok.termText();

                    // get the vocab index
                    Integer vocabIndex = origVocabulary.get(wordstem);
                    if (vocabIndex == null) {
                        vocabIndex = origVocabulary.size();
                        origVocabulary.put(wordstem, vocabIndex); // hash
                        origVocabInverse.add(wordstem); // add to list
                        origVocabInverse2Doc.add(
                                new ArrayList<Integer>(Collections.singleton(pubid))); // add to document hash
                        origVocabInverse2DocFrequency.add(
                                new ArrayList<Integer>(Collections.singleton(1))); // add to document hash
                        assert (vocabIndex == origVocabInverse.size() - 1) :
                                "vocabIndex = " + vocabIndex + " origVocabInverse.size()-1=" + (origVocabInverse.size()
                                        - 1);
                        assert (vocabIndex == origVocabInverse2Doc.size() - 1) :
                                "vocabIndex =" + vocabIndex + " origVocabInverse2Doc.size()-1=" + (
                                        origVocabInverse2Doc.size() - 1);
                        assert (origVocabInverse2Doc.get(vocabIndex).size() == 1) :
                                "origVocabInverse2Doc.get(vocabIndex).size()=" + origVocabInverse2Doc.get(
                                        vocabIndex).size();
                        assert (origVocabInverse2DocFrequency.get(vocabIndex).size() == 1) :
                                "origVocabInverse2DocFrequency.get(vocabIndex).size()="
                                        + origVocabInverse2DocFrequency.get(vocabIndex).size();
                    } else {
                        assert (origVocabInverse.get(origVocabulary.get(wordstem)).equals(wordstem)) :
                                "Inverse vocabulary broken. wordstem=" + wordstem + " vocabulary.get(wordstem)="
                                        + origVocabulary.get(wordstem) + " origVocabInverse.get("
                                        + origVocabulary.get(wordstem) + ")="
                                        + origVocabInverse.get(origVocabulary.get(wordstem)) + ")";
                        // add 1 to the frequencyList of (vocabIndex,doc)
                        List<Integer> doclist = origVocabInverse2Doc.get(vocabIndex);
                        List<Integer> freqlist = origVocabInverse2DocFrequency.get(vocabIndex);
                        int docindex1 = doclist.indexOf(pubid);
                        if (docindex1 < 0) {
                            docindex1 = doclist.size();
                            doclist.add(pubid);
                        }

                        while (docindex1 >= freqlist.size()) {
                            freqlist.add(0);
                        }
                        int docindex = docindex1;
                        final int oldFreq = freqlist.get(docindex);
                        freqlist.set(docindex, oldFreq + 1);


                        assert (origVocabInverse2Doc.size() == origVocabInverse2DocFrequency.size());
                        assert (origVocabInverse2DocFrequency.get(vocabIndex).get(docindex) > 0);

                    }

                }


            }

            // calculate (real) vocabulary (use by the inference algorithm)
            // throw out words that occur only within one document (and thus achieve no coupling)

            assert (origVocabInverse2Doc.size() > 1);
            assert (origVocabInverse.size() > 1);

            vocabulary = new HashMap<String, Integer>();
            vocabInverse = new ArrayList<String>();
            for (int w = 0; w < origVocabInverse2Doc.size(); w++) {
                if (origVocabInverse2Doc.get(w).size() >= minWordsOccurence) {
                    int vocaIndex = vocabInverse.size();
                    vocabInverse.add(origVocabInverse.get(w));
                    vocabulary.put(origVocabInverse.get(w), vocaIndex);
                }
            }

            System.out.println(vocabInverse.toArray());
            System.out.println("vocabInverse (" + vocabInverse.size() + ") ");// + vocabInverse);
            System.out.println("origVocabInverse (" + origVocabInverse.size() + ")");// = " + origVocabInverse);

            assert (vocabInverse.size() > 0);
        } catch (IOException e) {
            throw new RuntimeException(e);  //todo handle
        }
    }

    public Analyzer getStemmer() {
        return stemmer;
    }

    public List<IDocument> getDocs() {
        return new ArrayList<IDocument>(docs.values());
    }

    public Map<IDocument, Integer> getDocInverse() {
        return docInverse;
    }

    /**
     * Returns a complex datastructure that stores for each original wordstem, in which documents it is contained.
     * <p/>
     * <code>List<Integer> listOfDocIndices = result.get({@link #getOrigVocabulary()}.get("word"))</code>
     * <code>//listOfDocIndices == [2,4,6,7]</code>
     * <code>boolean wordContainedInDoc4 = listOfDocIndices.contains(4); // true</code>
     *
     * @return vocIndex -&gt; List of docindices
     */
    public List<List<Integer>> getOrigVocabInverse2Doc() {
        return origVocabInverse2Doc;
    }

    public List<String> getOrigVocabInverse() {
        return origVocabInverse;
    }

    public Map<String, Integer> getOrigVocabulary() {
        return origVocabulary;
    }

    public List<String> getVocabInverse() {
        return vocabInverse;
    }

    public Map<String, Integer> getVocabulary() {
        return vocabulary;
    }

    /**
     * Converts the phi matrix to the phi distribution.
     *
     * @param phi
     * @return distribution
     */
    public IDistribution<String> phiToWords(final DoubleMatrix1D phi) {
        IDistribution<String> phiWords = DistributionFactory.createDistribution();
        for (int w = 0; w < phi.size(); w++) {
            phiWords.put(getVocabInverse().get(w), phi.get(w));
        }
        return phiWords;
    }

    public String word2stemm(String word) {
        String wordstem;
        TokenStream tokenStream = getStemmer().tokenStream("", new StringReader(word));
        try {
            Token tok = tokenStream.next();
            wordstem = tok.termText();
            return wordstem;
        } catch (IOException e) {
            throw new RuntimeException("Programming Error:", e);
        }
    }

    public BidiMap createPubId2BugsId(Set<Integer> pubids) {
        return createPubId2BugsId(pubids, 0);
    }

    public BidiMap createPubId2BugsId(Set<Integer> pubids, int startId) {
        BidiMap pubid2bugsid = new DualHashBidiMap();
        int bugsid = startId;
        for (int id : pubids) {
            pubid2bugsid.put(id, bugsid);
            bugsid++;
        }
        return pubid2bugsid;
    }

    public int getPubid2BugsId(int pubid, BidiMap pubid2bugsid) {
    	if (!pubid2bugsid.containsKey(pubid)) {
            throw new IndexOutOfBoundsException("Pubid " + pubid + " not found in pubid2bugsid " + pubid2bugsid);
        }
        int bugsid = (Integer) pubid2bugsid.get(pubid);
        return bugsid;
    }

    /**
     * @throws IndexOutOfBoundsException if pubid is not contained in pubid2doc.
     */
    public int getBugsId2PubId(int bugsid, BidiMap pubid2bugsid) {
        if (!pubid2bugsid.containsValue(bugsid)) {
            throw new IndexOutOfBoundsException("Bugsid " + bugsid + " not found in pubid2bugsid " + pubid2bugsid);
        }
        int pubid = (Integer) pubid2bugsid.getKey(bugsid);
        return pubid;
    }

    public int getVNum() {
        return vocabInverse.size();
    }

    public ArrayList<List<Integer>> getOrigVocabInverse2DocFrequency() {
        return origVocabInverse2DocFrequency;
    }

    // ////////////////////////////////////////////////////
    //   TF-IDF
    // ////////////////////////////////////////////////////

    /**
     * Calculates tf * idf.
     *
     * @param bugsId
     * @param pubId
     * @return Distribution over word indices.
     */
    public IDistribution<Integer> getTfidf(int bugsId, int pubId) {
        IDistribution<Integer> tfi = DistributionFactory.<Integer>createDistribution();
        IDistribution<Integer> idfi = DistributionFactory.<Integer>createDistribution();

        for (int v = 0; v < getVNum(); v++) {
            String word = vocabInverse.get(v);
            int origV = origVocabulary.get(word);
            List<Integer> docsContainingV = getOrigVocabInverse2Doc().get(origV);
            if (docsContainingV.contains(pubId)) {
                int index = docsContainingV.indexOf(pubId);
                tfi.put(v, (double) getOrigVocabInverse2DocFrequency().get(origV).get(index));

                idfi.put(v, Math.log(1.0 * getDocs().size() / docsContainingV.size()));
            }
        }
        if (tfi.sum() > 0.0) {  // steffens variant: first normalize. then multiply. Standard variant: first multiply, then normalize
            tfi.multiply(idfi);
            tfi.normalize();
        }

        return tfi;
    }

    public double getTfidfSimilarity(int bugsId1, int bugsId2, int pubId1, int pubId2) {
        assert (pubId1 != -1) : "implement!!";
        assert (pubId2 != -1) : "implement!!";
        IDistribution<Integer> vec1 = getTfidf(bugsId1, pubId1);
        IDistribution<Integer> vec2 = getTfidf(bugsId2, pubId2);


        if (vec1.sum() == 0.0) {
            System.err.println("VocDocHasher#getTfidfSimilarity: Warning tfidf vector empty " + bugsId1 + ":" + vec1);
            return 0.0;
        }
        if (vec2.sum() == 0.0) {
            System.err.println("VocDocHasher#getTfidfSimilarity: Warning tfidf vector empty " + bugsId2 + ":" + vec2);
            return 0.0;
        }
        return innerProduct(vec1, vec2);
    }

    private static double innerProduct(IDistribution<Integer> vec1, IDistribution<Integer> vec2) {
        double sum = 0.0;
        for (int v : vec1.keySet()) {
            sum += vec1.get(v) * vec2.get(v);
        }

        assert (!Double.isNaN(sum));

        double norm1 = 0.0;
        for (int v : vec1.keySet()) {
            norm1 += vec1.get(v) * vec1.get(v);
        }
        norm1 = Math.sqrt(norm1);
        assert (!Double.isNaN(norm1));
        assert (norm1 != 0.0);

        double norm2 = 0.0;
        for (int v : vec2.keySet()) {
            norm2 += vec2.get(v) * vec2.get(v);
        }
        norm2 = Math.sqrt(norm2);
        assert (!Double.isNaN(norm2));
        assert (norm2 != 0.0);


        return sum / norm1 / norm2;
    }

    // ////////////////////////////////////////////////////
    //   inner classes
    // ////////////////////////////////////////////////////

    public static class NonStemmingAnalyzer extends Analyzer implements Serializable {
        public final TokenStream tokenStream(String fieldName, Reader reader) {
            //                return new StopFilter(new LowerCaseTokenizer(reader), StopAnalyzer.ENGLISH_STOP_WORDS);
            // we do not do porter stemming anymore.
            return new StopFilter(new LowerCaseTokenizer(reader), StopAnalyzer.ENGLISH_STOP_WORDS);
//            return new StopFilter(new PorterStemFilter(new LowerCaseTokenizer(reader)), StopAnalyzer.ENGLISH_STOP_WORDS);
        }
    }

    public static class PorterAnalyzer extends Analyzer implements Serializable {
        public final TokenStream tokenStream(String fieldName, Reader reader) {
            //                return new StopFilter(new LowerCaseTokenizer(reader), StopAnalyzer.ENGLISH_STOP_WORDS);
            // we do not do porter stemming anymore.
            return new StopFilter(new PorterStemFilter(new LowerCaseTokenizer(reader)),
                    StopAnalyzer.ENGLISH_STOP_WORDS);
        }
    }
}
