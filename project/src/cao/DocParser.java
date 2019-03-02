package cao;

import cao.Debugger;
import cao.Doc;
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
 * This is an improved version for 
 * All words are contained in the origvocabulary, whereas the vocabulary only contains words, that are contained in more
 * than one document. 
 */
public class DocParser implements Serializable {
    private static final long serialVersionUID = -6201441084323524139L;
    private Map<String, Integer> vocabularyMap;
    private static Analyzer stemmer;
    /**
     * holds a list of all words that occur within documents (even those, that occur only in one document
     */
    private List<String> origVocabInverse;
    /**
     * holds a mapping the words in {@link #origVocabInverse} to their index(position) within the list
     */
    private Map<String, Integer> origVocabulary;
    /**
     * each element holds a list of the documents, in which the word occurs. this list uses the same order of elements as in {@link #origVocabInverse}. you can use {@link #origVocabulary} for getting string -> freq.
     * <p/>
     * origVocabInverse2Doc.get(i) contains the document indices of all documents that contain the word origVocabInverse.get(i)
     */
    private List<List<Integer>> origVocabInverse2Doc;
    private List<String> vocabInverse;
    private final Map<Integer, Doc> docs;
    private Map<Doc, Integer> docInverse;
    private final int minWordsOccurence;
    private ArrayList<List<Integer>> origVocabInverse2DocFrequency;
    
    public DocParser()
    {
    	this.minWordsOccurence = 2;
        this.docs = null;
        String stemmingType = System.getProperty("torel.stemm", "nostemm");
        if ("nostemm".equals(stemmingType)) {
            stemmer = new NonStemmingAnalyzer();
        } else {
            stemmer = new PorterAnalyzer();
        }
        vocabularyMap = new HashMap<String, Integer>(); 
    }
    
    public DocParser(Map<Integer, Doc> docs) {
        this(docs, 2);
        vocabularyMap = new HashMap<String, Integer>();
    }

    
    /**
     * Huiping noted 2012-02-29
     * 
     * @param docs:	map in paper_id:paper_content
     * 				from which the vocabulary information will be derived
     * @param minWordsOccurence
     */
    public DocParser(Map<Integer, Doc> docs, int minWordsOccurence) {
        String stemmingType = System.getProperty("torel.stemm", "nostemm");
        if ("nostemm".equals(stemmingType)) {
            stemmer = new NonStemmingAnalyzer();
        } else {
            stemmer = new PorterAnalyzer();
        }


        this.minWordsOccurence = minWordsOccurence;
        this.docs = docs;
        
        vocabularyMap = new HashMap<String, Integer>();
        
        //createVocabulary(this.docs,this.minWordsOccurence,this.vocabularyMap,this.vocabInverse);
        createVocabulary(this.docs,this.minWordsOccurence,this.vocabInverse);
    }
    
    public Map<String,Integer> getVocabularymap(){
    	return vocabularyMap;
    }

    
    /**
     * Huiping noted 2012-02-29
     * 
     * Calculated the reverted list of all the documents
     * 
     * @param docs
     * @return map from MyDocument:document_index in the list
     */
    private Map<Doc, Integer> hashDocs(List<Doc> docs) {
        HashMap<Doc, Integer> result = new HashMap<Doc, Integer>();
        for (int d = 0; d < docs.size(); d++) {
            result.put(docs.get(d), d);
        }
        return result;
    }


    /**
     * 
     * @param docs
     * @param vocabulary
     * @param vocabInverse
     * @param pub2bugsIds
     * @param dim1size
     * @param dim2size
     * @return
     */
    public static double[][] calculateWordDocumentMatrix(
    		Map<Integer, Doc> docs,
    		Map<String, Integer> vocabulary,
    		List<String> vocabInverse,
    		BidiMap pub2bugsIds, 
    		final int dim1size, final int dim2size) {

    	double[][] wordDocMatrix = new double[dim1size][dim2size];
        try {
            assert (dim1size == vocabInverse.size()) :
                    "dimensions do not match in dim 1: dim1size=" + dim1size;
            //assert (dim2size == Collections.<Integer>max(pub2bugsIds.values()) + 1) :
            //        "max of pub2bugsIds does not match in dim 2: dim1size=" + dim2size + 
            //        "  Collections.<Integer>max(pub2bugsIds.values()=" + Collections.<Integer>max(pub2bugsIds.values());


            for (Object pubid_ : pub2bugsIds.keySet()) {
                int pubid = (Integer) pubid_;
                int bugid = getPubid2BugsId(pubid, pub2bugsIds);
                
                Doc doc = docs.get(pubid);
                String text = doc.getText();
                assert (text.length() > 1);
                TokenStream tokenStream = getStemmer().tokenStream("", new StringReader(text));
                for (Token tok = tokenStream.next(); tok != null; tok = tokenStream.next()) {
                    final String wordstem = tok.termText();
                    Integer vocabIndex = vocabulary.get(wordstem);
                    if (vocabIndex != null) {
                        //instance.add(vocabIndex, bugid, 1.0);
                    	wordDocMatrix[vocabIndex][bugid] +=1.0;
                    }
                }
            }

            return wordDocMatrix;
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
    public void createVocabulary(
    		Map<Integer, Doc> docs, int minWordsOccurence,
    		//Map<String, Integer> vocabularyMap,
    		List<String> vocabInverse) 
    {
    	vocabularyMap.clear();
    	docInverse = hashDocs(new ArrayList<Doc>(docs.values()));

        try {
            // calculate origVocabulary
            origVocabulary = new HashMap<String, Integer>(); //Map from vocabularity to its position
            origVocabInverse = new ArrayList<String>();
            origVocabInverse2Doc = new ArrayList<List<Integer>>();

            // each element holds a list of the frequency with which a word occurs in the document.
            origVocabInverse2DocFrequency = new ArrayList<List<Integer>>();

            assert (docs.size() > 0);

            for (int pubid : docs.keySet()) {
            	Doc doc = docs.get(pubid);
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

            //vocabulary = new HashMap<String, Integer>();
            //vocabInverse = new ArrayList<String>();
            for (int w = 0; w < origVocabInverse2Doc.size(); w++) {
                if (origVocabInverse2Doc.get(w).size() >= minWordsOccurence) {
                    int vocaIndex = vocabInverse.size();
                    vocabInverse.add(origVocabInverse.get(w));
                    vocabularyMap.put(origVocabInverse.get(w), vocaIndex);
                }
            }

            System.out.println(Debugger.getCallerPosition()+"vocabInverse (" + vocabInverse.size() + ") " + vocabInverse);
            System.out.println(Debugger.getCallerPosition()+"origVocabInverse (" + origVocabInverse.size() + ")" + origVocabInverse);
            //System.out.println(Debugger.getCallerPosition()+Arrays.toString(vocabInverse.toArray()));
            
            assert (vocabInverse.size() > 0);
        } catch (IOException e) {
            throw new RuntimeException(e);  //todo handle
        }
    }

    public static Analyzer getStemmer() {
        return stemmer;
    }

    //bidirection map cited_pub_id(int) <--> index_id(int)
    public static BidiMap createOid2OIdx(Set<Integer> pubids) {
        //return createPubId2BugsId(pubids, 0);
    	BidiMap pubid2bugsid = new DualHashBidiMap();
        int idx = 0;
        for (int id : pubids) {
            pubid2bugsid.put(id, idx);
            idx++;
        }
        return pubid2bugsid;
    }

    public static int getPubid2BugsId(int pubid, BidiMap pubid2bugsid) {
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

    public ArrayList<List<Integer>> getOrigVocabInverse2DocFrequency() {
        return origVocabInverse2DocFrequency;
    }
    
    public static class NonStemmingAnalyzer extends Analyzer implements Serializable {
        public final TokenStream tokenStream(String fieldName, Reader reader) {
            // return new StopFilter(new LowerCaseTokenizer(reader), StopAnalyzer.ENGLISH_STOP_WORDS);
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
