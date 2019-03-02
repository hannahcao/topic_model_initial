package cao;

import org.apache.commons.collections.BidiMap;
import org.apache.commons.collections.bidimap.DualHashBidiMap;
import org.apache.lucene.analysis.*;
import org.apache.lucene.util.Version;

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
    /**
     * word to word index map
     */
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
    private List<List<Integer>> origVocabInverse2DocFrequency;
    /**
     * apsect to aspect index map
     */
    public static Map<String, Integer> aspectMap = null;
    
    public DocParser()
    {
    	this.minWordsOccurence = 2;
        this.docs = null;
        //??????????
//        String stemmingType = System.getProperty("torel.stemm", "nostemm");
//        if ("nostemm".equals(stemmingType)) {
//            stemmer = new NonStemmingAnalyzer();
//        } else {
        stemmer = new PorterAnalyzer();
//        stemmer = new NonStemmingAnalyzer();
//        }
        vocabularyMap = new HashMap<String, Integer>(); 
//		I set this up manually :(
        
        //TODO  need to change for twitter data !!!
        if(aspectMap==null){
        	aspectMap = new HashMap<String, Integer>();
//        	aspectMap.put("abstract", 0);
//        	aspectMap.put("background", 1);
//        	aspectMap.put("problemdef", 2);
//        	aspectMap.put("solution", 3);
//        	aspectMap.put("expintro", 4);
//        	aspectMap.put("expcomparison", 5);
//        	aspectMap.put("expdata", 6);
//        	aspectMap.put("expanalysis", 7);
        	
        }
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
//        if ("nostemm".equals(stemmingType)) {
//            stemmer = new NonStemmingAnalyzer();
//        } else {
        stemmer = new PorterAnalyzer();
//        }


        this.minWordsOccurence = minWordsOccurence;
        this.docs = docs;
        
        vocabularyMap = new HashMap<String, Integer>();
        
        //createVocabulary(this.docs,this.minWordsOccurence,this.vocabularyMap,this.vocabInverse);
        createVocabulary(this.docs, this.minWordsOccurence, this.vocabInverse);
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
     * @param pub2bugsIds
     * @param dim1size
     * @param dim2size
     * @return
     * @throws IOException 
     */
    public static Map<Integer, Map<Integer, Map<Integer, Double>>> calculateAspectDocumentMap(Map<Integer, Doc> docs,
    		Map<String, Integer> vocabulary,
    		List<String> vocabInverse,
    		BidiMap pub2bugsIds, 
    		final int dim1size, final int dim2size, final int dim3size){
    	
//    	double[][][] apsectDocMatrix = new double[dim1size][dim2size][dim3size];
    	//word -> aspect -> document -> occur number
    	Map<Integer, Map<Integer, Map<Integer, Double>>> w2a2d2num = new HashMap<Integer, Map<Integer, Map<Integer, Double>>>();
    	
    	try{
    		 for (Object pubid_ : pub2bugsIds.keySet()) {
                 int pubid = (Integer) pubid_;
                 //document id
                 int bugid = getPubid2BugsId(pubid, pub2bugsIds);
                 
                 Doc doc = docs.get(pubid);
                 List<String> textList = doc.getText();
                 //aspect id
                 for(int i=0; i<textList.size(); i++){
                	 //TODO:: remove URL, @XXX and non-ASCII characters
                	 //DONE
                	 String text = textList.get(i); 
                	 //remove url
                	 String t1 = text.replaceAll("(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]", "");
                	 //remove non-ascii
                	 String t2 = t1.replaceAll("[^\\x00-\\x7F]", "");
                	 
//               stemmed text token stream
                	 TokenStream tokenStream = getStemmer().tokenStream("", new StringReader(t2));
                	 
                	 	
                	 while (tokenStream.incrementToken()) {
                		 String token = tokenStream.reflectAsString(false);
                		 int termBegin = 5;
                		 int termEnd = token.indexOf(",");
                		 final String wordstem = token.substring(termBegin, termEnd);
                		 
                		 //w id
                		 Integer vocabIndex = vocabulary.get(wordstem);
                		 if (vocabIndex != null) {
                			 //instance.add(vocabIndex, bugid, 1.0);
//                			 apsectDocMatrix[vocabIndex][i][bugid] +=1.0;
                			 
                			 //initialize w2a2d2count map
                			 Double oldValue = Util.get3Map(w2a2d2num, vocabIndex, i, bugid);
                			Util.update3HashMap(w2a2d2num, vocabIndex, i, bugid, oldValue + 1);
                		 }
                	 }
                 }
    		 }
    	}catch (IOException e) {
            throw new RuntimeException(e);
        }
    	return w2a2d2num;
    }

    public static double[][][] calculateAspectDocumentMatrix(Map<Integer, Doc> docs,
    		Map<String, Integer> vocabulary,
    		List<String> vocabInverse,
    		BidiMap pub2bugsIds, 
    		final int dim1size, final int dim2size, final int dim3size){
    	
    	double[][][] apsectDocMatrix = new double[dim1size][dim2size][dim3size];
    	//word -> aspect -> document -> occur number
    	
    	try{
    		 for (Object pubid_ : pub2bugsIds.keySet()) {
                 int pubid = (Integer) pubid_;
                 //document id
                 int bugid = getPubid2BugsId(pubid, pub2bugsIds);
                 
                 Doc doc = docs.get(pubid);
                 List<String> textList = doc.getText();
                 //aspect id
                 for(int i=0; i<textList.size(); i++){
                	 //TODO:: remove URL, @XXX and non-ASCII characters
                	 //DONE
                	 String text = textList.get(i); 
                	 //remove url
                	 String t1 = text.replaceAll("(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]", "");
                	 //remove non-ascii
                	 String t2 = t1.replaceAll("[^\\x00-\\x7F]", "");
                	 
//               stemmed text token stream
                	 TokenStream tokenStream = getStemmer().tokenStream("", new StringReader(t2));

//                	 for (Token tok = tokenStream.next(); tok != null; tok = tokenStream.next()) {
                	 while (tokenStream.incrementToken()) {
                		 String token = tokenStream.reflectAsString(false);
                		 int termBegin = 5;
                		 int termEnd = token.indexOf(",");
                		 final String wordstem = token.substring(termBegin, termEnd);
                		 
                		 //w id
                		 Integer vocabIndex = vocabulary.get(wordstem);
                		 if (vocabIndex != null) {
                			 //instance.add(vocabIndex, bugid, 1.0);
                			 apsectDocMatrix[vocabIndex][i][bugid] +=1.0;
                		 }
                	 }
                 }
    		 }
    	}catch (IOException e) {
            throw new RuntimeException(e);
        }
    	return apsectDocMatrix;
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
    			String text = doc.getFullText();
    			assert (text.length() > 1);

    			//remove url
    			String t1 = text.replaceAll("(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]", "");
    			//remove non-ascii
    			String t2 = t1.replaceAll("[^\\x00-\\x7F]", "");

    			TokenStream tokenStream = getStemmer().tokenStream("", new StringReader(t2));
//    			for (Token tok = tokenStream.next(); tok != null; tok = tokenStream.next()) {
//    				final String wordstem = tok.termText();
    			 while (tokenStream.incrementToken()) {
    				 String token = tokenStream.reflectAsString(false);
    				 int termBegin = 5;
    				 int termEnd = token.indexOf(",");
    				 final String wordstem = token.substring(termBegin, termEnd);
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
    
    public static Map<Integer, Map<Integer, Double>> calculateWordDocumentMap(
    		Map<Integer, Doc> docs,
    		Map<String, Integer> vocabulary,
    		List<String> vocabInverse,
    		BidiMap pub2bugsIds, 
    		final int dim1size, final int dim2size) {

    	Map<Integer, Map<Integer, Double>> wordDocMap = new HashMap<Integer, Map<Integer, Double>>();
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
    			String text = doc.getFullText();
    			assert (text.length() > 1);

    			//remove url
    			String t1 = text.replaceAll("(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]", "");
    			//remove non-ascii
    			String t2 = t1.replaceAll("[^\\x00-\\x7F]", "");

    			TokenStream tokenStream = getStemmer().tokenStream("", new StringReader(t2));
//    			for (Token tok = tokenStream.next(); tok != null; tok = tokenStream.next()) {
//    				final String wordstem = tok.termText();
    			while (tokenStream.incrementToken()) {
    				String token = tokenStream.reflectAsString(false);
    				int termBegin = 5;
    				int termEnd = token.indexOf(",");
    				final String wordstem = token.substring(termBegin, termEnd);
    				Integer vocabIndex = vocabulary.get(wordstem);
    				if (vocabIndex != null) {
    					//instance.add(vocabIndex, bugid, 1.0);
    					Double oldValue = Util.get2Map(wordDocMap, vocabIndex, bugid);
    					Util.update2Map(wordDocMap, vocabIndex, bugid, oldValue+1.0);
    				}
    			}
    		}

    		return wordDocMap;
    	} catch (IOException e) {
    		throw new RuntimeException(e);
    	}
    }
    
    /**
     * Huiping noted 2012-02-29
     * 
     * Extract vocabularies from all documents and Build TF array, DF array
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
    		//Map from vocabularity to its index position
    		origVocabulary = new HashMap<String, Integer>(); 
    		//word ordered by word index 
    		origVocabInverse = new ArrayList<String>();
    		//list of documents containing word, ordered by word index
    		setOrigVocabInverse2Doc(new ArrayList<List<Integer>>());
    		// each element holds a list of the frequency with which a word occurs in the document.
    		// order is the same as above
    		origVocabInverse2DocFrequency = new ArrayList<List<Integer>>();

    		assert (docs.size() > 0);

    		for (int pubid : docs.keySet()) {
    			Doc doc = docs.get(pubid);
    			String text = doc.getFullText();
    			assert (text.length() > 1);

    			//remove url
    			String t1 = text.replaceAll("(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]", "");
    			//remove non-ascii
    			String t2 = t1.replaceAll("[^\\x00-\\x7F]", "");

    			TokenStream tokenStream = getStemmer().tokenStream("", new StringReader(t2));
//    			for (Token tok = tokenStream.next(); tok != null; tok = tokenStream.next()) {
//    				final String wordstem = tok.termText();
    			 while (tokenStream.incrementToken()) {
            		 String token = tokenStream.reflectAsString(false);
            		 int termBegin = 5;
            		 int termEnd = token.indexOf(",");
            		 final String wordstem = token.substring(termBegin, termEnd);
    				// get the vocab index
    				Integer vocabIndex = origVocabulary.get(wordstem);
            		if (vocabIndex == null) {
            			vocabIndex = origVocabulary.size();
            			origVocabulary.put(wordstem, vocabIndex); // hash
            			
            			origVocabInverse.add(wordstem); // add to list
            			getOrigVocabInverse2Doc().add(
            					new ArrayList<Integer>(Collections.singleton(pubid))); // add to document hash
            			origVocabInverse2DocFrequency.add(
            					new ArrayList<Integer>(Collections.singleton(1))); // add to document hash
            			assert (vocabIndex == origVocabInverse.size() - 1) :
            				"vocabIndex = " + vocabIndex + " origVocabInverse.size()-1=" + (origVocabInverse.size()
            						- 1);
            			assert (vocabIndex == getOrigVocabInverse2Doc().size() - 1) :
            				"vocabIndex =" + vocabIndex + " origVocabInverse2Doc.size()-1=" + (
            						getOrigVocabInverse2Doc().size() - 1);
            			assert (getOrigVocabInverse2Doc().get(vocabIndex).size() == 1) :
            				"origVocabInverse2Doc.get(vocabIndex).size()=" + getOrigVocabInverse2Doc().get(
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
            			List<Integer> doclist = getOrigVocabInverse2Doc().get(vocabIndex);
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
            			
            			assert (getOrigVocabInverse2Doc().size() == origVocabInverse2DocFrequency.size());
            			assert (origVocabInverse2DocFrequency.get(vocabIndex).get(docindex) > 0);

            		}
            	}
            }

            // calculate (real) vocabulary (use by the inference algorithm)
            // throw out words that occur only within one document (and thus achieve no coupling)

            assert (getOrigVocabInverse2Doc().size() > 1);
            assert (origVocabInverse.size() > 1);

            //vocabulary = new HashMap<String, Integer>();
            //vocabInverse = new ArrayList<String>();
            for (int w = 0; w < getOrigVocabInverse2Doc().size(); w++) {
            	if (getOrigVocabInverse2Doc().get(w).size() >= minWordsOccurence) {
            		int vocaIndex = vocabInverse.size();
            		vocabInverse.add(origVocabInverse.get(w));
            		vocabularyMap.put(origVocabInverse.get(w), vocaIndex);
            	}
            }

            //            System.out.println(Debugger.getCallerPosition()+"vocabInverse (" + vocabInverse.size() + ") " + vocabInverse);
            //            System.out.println(Debugger.getCallerPosition()+"origVocabInverse (" + origVocabInverse.size() + ")" + origVocabInverse);
            //System.out.println(Debugger.getCallerPosition()+Arrays.toString(vocabInverse.toArray()));

            assert (vocabInverse.size() > 0);
        } catch (IOException e) {
        	throw new RuntimeException(e);  //todo handle
        }
    	
    	Constant.tokenNum = vocabularyMap.size();
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

    public List<List<Integer>> getOrigVocabInverse2DocFrequency() {
        return origVocabInverse2DocFrequency;
    }
    
    public List<List<Integer>> getOrigVocabInverse2Doc() {
		return origVocabInverse2Doc;
	}

	public void setOrigVocabInverse2Doc(List<List<Integer>> origVocabInverse2Doc) {
		this.origVocabInverse2Doc = origVocabInverse2Doc;
	}

	public static class NonStemmingAnalyzer extends Analyzer implements Serializable {
        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public final TokenStream tokenStream(String fieldName, Reader reader) {
            // return new StopFilter(new LowerCaseTokenizer(reader), StopAnalyzer.ENGLISH_STOP_WORDS);
            // we do not do porter stemming anymore.
//            return new StopFilter(new LowerCaseTokenizer(reader), StopAnalyzer.ENGLISH_STOP_WORDS);
//            return new StopFilter(new PorterStemFilter(new LowerCaseTokenizer(reader)), StopAnalyzer.ENGLISH_STOP_WORDS);
        	TokenStream ts = new StopFilter(Version.LUCENE_36, new LowerCaseTokenizer(Version.LUCENE_36, reader),
					StopAnalyzer.ENGLISH_STOP_WORDS_SET);
			
			return ts;
        }
    }

    public static class PorterAnalyzer extends Analyzer implements Serializable {
        /**
		 * 
		 */
		private static final long serialVersionUID = 2L;

		public final TokenStream tokenStream(String fieldName, Reader reader) {
            //                return new StopFilter(new LowerCaseTokenizer(reader), StopAnalyzer.ENGLISH_STOP_WORDS);
            // we do not do porter stemming anymore.
//            return new StopFilter(new PorterStemFilter(new LowerCaseTokenizer(reader)),
//                    StopAnalyzer.ENGLISH_STOP_WORDS);
        	TokenStream ts = new StopFilter(Version.LUCENE_36, new PorterStemFilter(new LowerCaseTokenizer(Version.LUCENE_36, reader)),
					StopAnalyzer.ENGLISH_STOP_WORDS_SET);
			
			return ts;
        }
    }
    
    public static void main(String[] args){
    	
    }
}
