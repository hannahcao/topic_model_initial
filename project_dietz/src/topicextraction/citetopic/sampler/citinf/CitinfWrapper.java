package topicextraction.citetopic.sampler.citinf;

import topicextraction.citetopic.CiteTopicUtil;
import topicextraction.citetopic.realdata.ITopicModelWrapper;
import topicextraction.citetopic.sampler.VocDocHasher;
import topicextraction.querydb.IDocument;
import topicextraction.topicinf.datastruct.DistributionFactory;
import topicextraction.topicinf.datastruct.IDistribution;
import util.matrix.ArrayMatrix2D;
import util.matrix.IMatrix2D;
import util.matrix.INonZeroPerformer5D;
import util.matrix.LineMatrix2D;
import org.apache.commons.collections.BidiMap;

import cao.Debugger;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 */
public class CitinfWrapper implements Serializable, ITopicModelWrapper {
    private static final long serialVersionUID = 8178979117954626591L;
    private LineMatrix2D wd;
    private CitinfSampler sampler;
    private double gammaDP = 0.0001;
    private int gibbsCalls = -1;
    private int dMax;
    private final VocDocHasher vocdoc;
    private Map<Integer, List<Integer>> pubId2CiteIds;
    private Map<Integer, IDocument> pubId2Docs;
    private Map<Integer, IDocument> citedDocs;
    private Map<Integer, IDocument> citingDocs;
    private BidiMap citedDocsP2B;
    private BidiMap citingDocsP2B;
    private IMatrix2D cited_wd;
    private IMatrix2D citing_wd;
    private int wMax;
    private ArrayList<List<Integer>> bibliographies;
    private int numTopics;
    private int numCites;
    private double alphaPhi;
    private double alphaTheta;
    private double alphaPsi;
    private double alphaGamma;
    private double alphaLambdaInherit;
    private double alphaLambdaInnov;


    public CitinfWrapper(Map<Integer, List<Integer>> pubId2CiteIds, Map<Integer, IDocument> pubId2Docs, int numTopics, int numCites, double alphaPhi, double alphaPsi, double alphaTheta, double alphaLambdaInherit, double alphaLambdaInnov, double alphaGamma) {
        this.pubId2CiteIds = pubId2CiteIds;
        this.pubId2Docs = pubId2Docs;
        this.numTopics = numTopics;
        this.numCites = numCites;
        this.alphaPhi = alphaPhi;
        this.alphaTheta = alphaTheta;
        this.alphaPsi = alphaPsi;
        this.alphaGamma = alphaGamma;
        this.alphaLambdaInherit = alphaLambdaInherit;
        this.alphaLambdaInnov = alphaLambdaInnov;


        citedDocs = initCitedDocs(pubId2CiteIds, pubId2Docs);
        citingDocs = initCitingDocs(pubId2CiteIds, pubId2Docs);
        

        //Huiping noted: put the document information to vecdoc
        //			from vecdoc, you can get the vocabularies, 
        vocdoc = new VocDocHasher(pubId2Docs, 2);  
        assert (vocdoc.getDocs().size() > 0);
        wMax = vocdoc.getVNum();


        citedDocsP2B = vocdoc.createPubId2BugsId(citedDocs.keySet());
        cited_wd = vocdoc.calculateWordDocumentMatrix(citedDocsP2B,
                new ArrayMatrix2D(wMax, citedDocs.size()));
        assert (cited_wd.getDimensionSize(0) > 0);
        assert (cited_wd.getDimensionSize(1) > 0);

        citingDocsP2B = vocdoc.createPubId2BugsId(citingDocs.keySet());
        citing_wd = vocdoc.calculateWordDocumentMatrix(citingDocsP2B,
                new ArrayMatrix2D(wMax, citingDocs.size()));
        assert (citing_wd.getDimensionSize(0) > 0);
        assert (citing_wd.getDimensionSize(1) > 0);

        bibliographies = new ArrayList<List<Integer>>();
        for (int i = 0; i < pubId2CiteIds.size(); i++) {
            bibliographies.add(new ArrayList<Integer>());
        }
        for (int citingPub : pubId2CiteIds.keySet()) {
            int citingBugs = vocdoc.getPubid2BugsId(citingPub, citingDocsP2B);
            List<Integer> bib = bibliographies.get(citingBugs);
            for (int citedPub : pubId2CiteIds.get(citingPub)) {
                int citedBugs = vocdoc.getPubid2BugsId(citedPub, citedDocsP2B);
                bib.add(citedBugs);
            }
        }

        vocdoc.clearCaches();
//        new CitinfSampler(cited_wd, citing_wd, this.bibliographies, this.numTopics, this.numCites,
//                numIterations, this.alphaPhi, this.alphaTheta, this.alphaPsi, this.alphaGamma, this.alphaLambdaInherit, this.alphaLambdaInnov, chainId, wMax);

    }

    /**
     * Huiping noted 2012-02-29
     * Get all the cited documents cited_paper_id:cited_paper_content
     *  
     * @param pubId2CiteIds: map for paper_id:cited_paper_id
     * @param pubId2Docs: map for paper_id: paper_content
     * @return the cited documents information
     */
    private Map<Integer, IDocument> initCitedDocs(Map<Integer, List<Integer>> pubId2CiteIds, Map<Integer, IDocument> pubId2Docs) {
        Map<Integer, IDocument> citingDocs = new HashMap<Integer, IDocument>();
        for (List<Integer> cList : pubId2CiteIds.values()) {
            for (int id : cList) {
                if (!citingDocs.containsKey(id)) {
                    IDocument doc = pubId2Docs.get(id);
                    assert (doc != null);
                    citingDocs.put(id, doc);
                }
            }
        }
        return citingDocs;
    }

    /**
     * Huiping noted 2012-02-29
     * Get all the information about the papers that cite others
     * 
     * @param pubId2CiteIds: map for paper_id:cited_paper_id
     * @param pubId2Docs: map for paper_id: paper_content
     * @return the citing papers' information in a map citing_paper_id:citing_paper_content
     */
    private Map<Integer, IDocument> initCitingDocs(Map<Integer, List<Integer>> pubId2CiteIds, Map<Integer, IDocument> pubId2Docs) {
        Map<Integer, IDocument> citedDocs = new HashMap<Integer, IDocument>();
        for (int id : pubId2CiteIds.keySet()) {
            IDocument doc = pubId2Docs.get(id);
            assert (doc != null);
            citedDocs.put(id, doc);
        }
        return citedDocs;
    }

//    public CitinfWrapper(Map<Integer, IDocument> pubid2doc, int kMaxVal, int numIterations, int minWordsOccurence, double alphaT, double beta, double gamma) {
//        this.numIterations = numIterations;
//        vocdoc = new VocDocHasher(pubid2doc, minWordsOccurence);
//
//        pubId2BugsId = vocdoc.createPubId2BugsId(pubid2doc.keySet());
//
//        IMatrix2D wdArray_ = vocdoc.calculateWordDocumentMatrix(pubId2BugsId, new ArrayMatrix2D(vocdoc.getVNum(), pubId2BugsId.size()));
//
////        IMatrix2D wdArray = (IMatrix2D) wd.toMatrix(new ArrayMatrix2D(vocDoc.getVNum(), vocDoc.getDocs().size()));
//
//        wd = new LineMatrix2D(1000);
//        wdArray_.doForAllNonZeros(wd.fillingPerformer());
////        wd = (LineMatrix2D) vocDoc.calculateWordDocumentMatrixMin(pubId2BugsId, new LineMatrix2D(1000));
//        dLength = (IMatrix1D) wdArray_.marginalize(0);
//        dMax = dLength.size();
//        wMax = vocdoc.getVNum();
//        this.kMaxVal = kMaxVal;
//
//
//        alphaTheta = alphaT;
//        alphaPhi = beta;
//        this.gamma = gamma;
//    }


    public void doGibbs(final int numIterations) {
        gibbsCalls++;

        final List<String> ALL_CHAIN_IDS = CiteTopicUtil.readParamList("torel.allchains", "");
        if (ALL_CHAIN_IDS.size() == 0) {
            throw new RuntimeException("please indicate the number (and IDs) of sampling threads by environment params, e.g. \"-Dtorel.allchains0=1 -Dtorel.allchains1=2\"");
        }

        System.out.println("ALL_CHAIN_IDS size="+ ALL_CHAIN_IDS.size()+":" +ALL_CHAIN_IDS);
        final List<Thread> threadlist = new ArrayList<Thread>();
        for (final String chain : ALL_CHAIN_IDS) {
            final String chain_ = chain;
            
            Thread thread = new Thread(new Runnable() {
                public void run() {
                    boolean takeSamplesFromThisChain = chain_.equals(ALL_CHAIN_IDS.get(0));
                    
                    System.out.println(Debugger.getCallerPosition()+"Chain: "+ chain);
//                    System.out.println("\tpubId2CiteIds="+pubId2CiteIds);
//                    System.out.println("\tid2Docs="+pubId2Docs);
//                    System.out.println("\ttnum(changed to numTopics)="+numTopics);
//                    System.out.println("\tcnum(changed to numCites)="+numCites);
//                    System.out.println("\talphaPhi="+alphaPhi);
//                    System.out.println("\talphaPsi="+alphaPsi);
//                    System.out.println("\talphaTheta="+alphaTheta);
//                    System.out.println("\talphaLambdaInherit="+alphaLambdaInherit);
//                    System.out.println("\talphaLambdaInnov="+alphaLambdaInnov);
//                    System.out.println("\talphaGamma="+alphaGamma);
                    
                    
                    CitinfSampler sampler = new CitinfSampler(cited_wd, citing_wd, bibliographies, numTopics, numCites,
                            numIterations, alphaPhi, alphaTheta, alphaPsi, alphaGamma, alphaLambdaInherit, alphaLambdaInnov, chain, wMax, takeSamplesFromThisChain);
                    if (takeSamplesFromThisChain) {
                        CitinfWrapper.this.sampler = sampler;
                    }
                    sampler.doGibbs(numIterations);
                }
            }, "train-" + chain_);
            //if(chain.trim().equals("1")) break;//Huiping added, just use one chain
            threadlist.add(thread);
        }

        for (Thread thread : threadlist) {
            thread.start();
        }
        for (Thread thread : threadlist) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                System.err.println("interrupted");
            }
        }
    }

    // todo cache

    public IDistribution<Integer> getThetaByPubId(int pubid) {
        if (!citingDocsP2B.containsKey(pubid)) return getThetaByCitedPubId(pubid);
        else if (!citedDocsP2B.containsKey(pubid)) return getThetaByCitingPubId(pubid);
        else {
            IDistribution<Integer> thetaCited = getThetaByCitedPubId(pubid);
            IDistribution<Integer> thetaCiting = getThetaByCitingPubId(pubid);
            IDistribution<Integer> theta = DistributionFactory.copyDistribution(thetaCited);
            theta.overlay(thetaCiting);
            return theta;
        }
    }

    public IDistribution<Integer> getThetaByCitingPubId(int pubid) {
        IDistribution<Integer> distribution = sampler.getTheta(vocdoc.getPubid2BugsId(pubid, citingDocsP2B));
        distribution.normalize();
        return distribution;
    }

    public IDistribution<Integer> getThetaByCitedPubId(int pubid) {
        IDistribution<Integer> distribution = sampler.getTheta(vocdoc.getPubid2BugsId(pubid, citedDocsP2B));
        distribution.normalize();
        return distribution;
    }

    // todo gamma, lambda
    public IDistribution<Integer> getGammaForPubId(int pubid) {
        IDistribution<Integer> gammaDistr = sampler.getGamma(vocdoc.getPubid2BugsId(pubid, citingDocsP2B), citedDocsP2B);
        gammaDistr.normalize();
        return gammaDistr;
    }

    public IDistribution<Integer> getLambdaDistrForPubId(int pubid) {
        IDistribution<Integer> lambdaDistr = sampler.getLambdaDistr(vocdoc.getPubid2BugsId(pubid, citingDocsP2B));
        lambdaDistr.normalize();
        return lambdaDistr;
    }

    public double getLambdaForPubId(int pubid) {
        double lambda = sampler.getLambda(vocdoc.getPubid2BugsId(pubid, citingDocsP2B));
        return lambda;
    }

    // todo cache

    public List<IDistribution<Integer>> getPhis() {
        return sampler.getPhis();
    }

    public String getWordForIndex(int wordIndex) {
        return vocdoc.getVocabInverse().get(wordIndex);
    }

    public int getTNum() {
        return sampler.getNumTopics();
    }


    // --------------- analyze abstract -------------
    public Map<Integer, String> analyzeAbstract(int citingPubid) {

        final Map<Integer, String> citingSpreadAbstract = new HashMap<Integer, String>();
        final IDistribution<CitedWord> citingSpreadGlobalDistr = DistributionFactory.createDistribution();
        final int citingBugsId = vocdoc.getPubid2BugsId(citingPubid, citingDocsP2B);
        sampler.doForAllDWTSCCitingAveragedNonZeros(new INonZeroPerformer5D() {
            public void iteration(int d, int w, int t, int s, int c, double val, int position) {
                if (s == 0 && d == citingBugsId) {
                    final int citedPubId = vocdoc.getBugsId2PubId(c, citedDocsP2B);
                    String word = vocdoc.getVocabInverse().get(w);
                    CitedWord cw = new CitedWord(word, citedPubId);
                    citingSpreadGlobalDistr.add(cw, val);
                }
            }
        });

        if (citingSpreadGlobalDistr.isEmpty()) {
            System.err.println("CitinfWrapper#analyzeAbstract: Publication " + citingPubid
                    + " is not associated to any cited publications.");
        } else {
            citingSpreadGlobalDistr.normalize();

            final Map<Integer, IDistribution<String>> citingSpreadDistri = new HashMap<Integer, IDistribution<String>>();
            for (CitedWord cw : citingSpreadGlobalDistr.keySet()) {
                if (!citingSpreadDistri.containsKey(cw.getCitingPubId())) {
                    citingSpreadDistri.put(cw.getCitingPubId(), DistributionFactory.<String>createDistribution());
                }
                IDistribution<String> distr = citingSpreadDistri.get(cw.getCitingPubId());
                distr.add(cw.getWord(), citingSpreadGlobalDistr.get(cw));
            }

            for (int citingPub : citingSpreadDistri.keySet()) {
                IDistribution<String> distr = citingSpreadDistri.get(citingPub);
                String text = "";
                for (String word : distr.highestElements(10)) {
                    text += word + "(" + CiteTopicUtil.cut(distr.get(word), 4) + "), ";
                }
                citingSpreadAbstract.put(citingPub, text);
            }


        }
        return citingSpreadAbstract;
    }

    public Map<String, Object> getInfoForCitingPub(int pubid) {
        HashMap<String, Object> info = new HashMap<String, Object>();

        final int bugsId = vocdoc.getPubid2BugsId(pubid, citingDocsP2B);
        info.put("theta", intDistr2String(getThetaByPubId(pubid)));
        selectorStats(bugsId, info);
        topicStats(bugsId, info);
        return info;
    }


    private HashMap<String, Object> topicStats(final int bugsId, HashMap<String, Object> info) {
        final IDistribution<TopicWord> topicGlobalWords = DistributionFactory.createDistribution();
        sampler.doForAllDWTSCCitingAveragedNonZeros(new INonZeroPerformer5D() {
            public void iteration(int d, int w, int t, int s, int c, double val, int position) {
                if (s == 0 && d == bugsId) {
                    String word = vocdoc.getVocabInverse().get(w);
                    topicGlobalWords.add(new TopicWord(word, t), 1.0);
                }
            }
        });
        if (topicGlobalWords.isEmpty()) {
            System.err.println("PinkberryWrapper#topicStats: no topic words are associated with publication "
                    + vocdoc.getBugsId2PubId(bugsId, citingDocsP2B));
        } else {
            topicGlobalWords.normalize();
            final List<IDistribution<String>> topicWords = new ArrayList<IDistribution<String>>();
            for (int t = 0; t < sampler.getNumTopics(); t++) {
                topicWords.add(DistributionFactory.<String>createDistribution());
            }
            for (TopicWord tw : topicGlobalWords.keySet()) {
                IDistribution<String> distr = topicWords.get(tw.getTopic());
                distr.add(tw.getWord(), topicGlobalWords.get(tw));
            }

            for (int t = 0; t < sampler.getNumTopics(); t++) {
                IDistribution<String> distr = topicWords.get(t);
                String text = "";
                for (String word : distr.highestElements(10)) {
                    text += word + "(" + CiteTopicUtil.cut(distr.get(word), 4) + "), ";
                }

                info.put(t + " Topic Words", text);
            }
        }
        return info;
    }

    private HashMap<String, Object> selectorStats(final int bugsId, HashMap<String, Object> info) {
        final IDistribution<Integer> sDistr = DistributionFactory.createDistribution();
        final IDistribution<String> s0Words = DistributionFactory.createDistribution();
        final IDistribution<String> s1Words = DistributionFactory.createDistribution();
        sampler.doForAllDWTSCCitingAveragedNonZeros(new INonZeroPerformer5D() {
            public void iteration(int d, int w, int t, int s, int c, double val, int position) {
                if (d == bugsId) {
                    if (s == 0) {
                        sDistr.add(1, val);
                        s1Words.add(vocdoc.getVocabInverse().get(w), val);
                    } else {
                        sDistr.add(0, val);
                        s0Words.add(vocdoc.getVocabInverse().get(w), val);
                    }
                }
            }
        });

        if (!sDistr.isEmpty()) {
            sDistr.normalize();
        }
        if (!s0Words.isEmpty()) {
            s0Words.normalize();
        }
        if (!s1Words.isEmpty()) {
            s1Words.normalize();
        }

        System.out.println(
                vocdoc.getBugsId2PubId(bugsId, citingDocsP2B) + ": s0=" + sDistr.get(0) + " s1=" + sDistr.get(1));
        info.put("S0", sDistr.get(0));
        info.put("S1", sDistr.get(1));
        info.put("S0 words",

                stringDistr2String(s0Words)

        );
        info.put("S1 words",

                stringDistr2String(s1Words)

        );
        return info;
    }

    private String stringDistr2String(IDistribution<String> distr) {
        String text = "";
        for (String word : distr.highestElements(10)) {
            text += word + "(" + distr.get(word) + "), ";
        }
        return text;
    }

    private String intDistr2String(IDistribution<Integer> distr) {
        String text = "";
        for (Object topic : distr.elementsAbove(0.01).keySet()) {
            text += topic + "(" + distr.get((Integer) topic) + "), ";
        }
        return text;
    }


    private static class CitedWord {
        private String word;
        private int citingPubId;

        public CitedWord(String word, int citingPubId) {
            this.word = word;
            this.citingPubId = citingPubId;
        }

        public String getWord() {
            return word;
        }

        public int getCitingPubId() {
            return citingPubId;
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            CitedWord citedWord = (CitedWord) o;

            if (citingPubId != citedWord.citingPubId) {
                return false;
            }
            if (word != null ? !word.equals(citedWord.word) : citedWord.word != null) {
                return false;
            }

            return true;
        }

        public int hashCode() {
            int result;
            result = (word != null ? word.hashCode() : 0);
            result = 31 * result + citingPubId;
            return result;
        }
    }

    private static class TopicWord {
        private String word;
        private int topic;

        public TopicWord(String word, int topic) {
            this.word = word;
            this.topic = topic;
        }

        public String getWord() {
            return word;
        }

        public int getTopic() {
            return topic;
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            TopicWord other = (TopicWord) o;

            if (topic != other.topic) {
                return false;
            }
            if (word != null ? !word.equals(other.word) : other.word != null) {
                return false;
            }

            return true;
        }

        public int hashCode() {
            int result;
            result = (word != null ? word.hashCode() : 0);
            result = 31 * result + topic;
            return result;
        }
    }

}
