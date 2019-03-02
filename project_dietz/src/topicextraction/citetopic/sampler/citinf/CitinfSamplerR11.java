package topicextraction.citetopic.sampler.citinf;

import topicextraction.citetopic.CiteTopicUtil;
import topicextraction.citetopic.sampler.ChainStatisticsWriter;
import topicextraction.citetopic.sampler.ConvergenceDiagnosis;
import topicextraction.citetopic.sampler.GammaScalarSummaries;
import topicextraction.citetopic.sampler.ICiteInitializer;
import topicextraction.citetopic.sampler.RandomCiteInitializer;
import topicextraction.citetopic.sampler.hdplda.MiniDistribution;
import topicextraction.topicinf.ITopicInitializer;
import topicextraction.topicinf.RandomTopicInitializer;
import topicextraction.topicinf.datastruct.DistributionFactory;
import topicextraction.topicinf.datastruct.IDistribution;
import util.matrix.IMatrix2D;
import util.matrix.INonZeroPerformer2D;
import util.matrix.INonZeroPerformer3D;
import util.matrix.INonZeroPerformer5D;
import org.apache.commons.collections.BidiMap;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Samples the Citation-Influence Model.
 */
public class CitinfSamplerR11 implements Serializable {
    private static final long serialVersionUID = 2457478948145657551L;
    private double alphaPhi;
    private double gamma;
    private double alphaTheta;
    private double alphaGamma;
    private double alphaPsi;
    private double alphaLambdaInherit;
    private double alphaLambdaInnov;
    private final int kMaxVal;
    private final int numCites;
    private final int numIterations;
    private final int wMax;
    private CitinfData data;
    // average
    private CitinfData averageData;
    //  convdiag
    private final int SAMPLE_LAG = Integer.parseInt(System.getProperty("torel.samplelag", "20"));
    private int BURN_IN = Integer.parseInt(System.getProperty("torel.burnin", "50"));
    private final String SAMPLER_ID = System.getProperty("torel.samplerid", "test");
    private final String CHAIN_ID;
    private final boolean takeSamplesFromThisChain;
    private final List<String> ALL_CHAIN_IDS = CiteTopicUtil.readParamList("torel.allchains", "");
    private long lastCheck = new Date().getTime();
    private static final long MINIMUM_MILLISECS_CONV_SYNC = 500; // 0.5 secs
    private int gibbsCalls = 0;
    private final MiniDistribution mapTopicPosteriorDistr;
    private final ArrayList<List<Integer>> bibliographies;

    // /////////////////////////////////////////////////////////////////////////////////
    //       INIT
    // /////////////////////////////////////////////////////////////////////////////////


    public CitinfSamplerR11(IMatrix2D cited_wd, IMatrix2D citing_wd, ArrayList<List<Integer>> bibliographies, int numTopics, int numCites, int numIterations, double alphaPhi, double alphaTheta, double alphaPsi, double alphaGamma, double alphaLambdaInherit, double alphaLambdaInnov, String chainId, int wMax, boolean takeSamplesFromThisChain) {
        this.alphaLambdaInnov = alphaLambdaInnov;
        this.alphaLambdaInherit = alphaLambdaInherit;
        this.alphaGamma = alphaGamma;
        this.alphaPsi = alphaPsi;
        this.alphaTheta = alphaTheta;
        this.alphaPhi = alphaPhi;

        this.bibliographies = bibliographies;
        this.kMaxVal = numTopics;
        this.wMax = wMax;
        this.numCites = numCites;
        this.numIterations = numIterations;
        this.CHAIN_ID = chainId;
        this.takeSamplesFromThisChain = takeSamplesFromThisChain;

        this.data = new CitinfData(cited_wd, citing_wd, numTopics, numCites, bibliographies);
        if (takeSamplesFromThisChain) {
            this.averageData = new CitinfData(cited_wd, citing_wd, numTopics, numCites, bibliographies);
        }

        initialize(data);

        mapTopicPosteriorDistr = new MiniDistribution(kMaxVal * (numCites + 1));
    }

    private void initialize(final CitinfData data) {
        final ITopicInitializer tinit = new RandomTopicInitializer();
        final ICiteInitializer cinit = new RandomCiteInitializer();
        data.doForAllCitedWdNonZeros(new INonZeroPerformer2D() {
            public void iteration(int w, int d, double val, int position) {
                for (int occ = 0; occ < val; occ++) {
                    int newT = tinit.initialTopic(w, d, 1, kMaxVal);
                    data.addCitedDwt(d, w, newT, 1, position);
                }
            }
        });
        data.doForAllCitingWdNonZeros(new INonZeroPerformer2D() {
            public void iteration(int w, int d, double val, int position) {
                for (int occ = 0; occ < val; occ++) {
                    int newT = tinit.initialTopic(w, d, 1, kMaxVal);
                    int newS = tinit.initialTopic(w, d, 1, CitinfData.MAX_S);
                    int newC = cinit.initialCite(d, w, 1, bibliographies.get(d));
                    data.addCitingDwtsc(d, w, newT, newS, newC, 1, position);
                }
            }
        });
    }


    // /////////////////////////////////////////////////////////////////////////////////
    //       SAMPLING
    // /////////////////////////////////////////////////////////////////////////////////
    public void doGibbs(int numIterations) {
        System.out.println("Entering gibbs sampling with paramters: data: "+ data + ", numIterations: " + numIterations + ", (unknown entry of zero)" + 0 + ", CHAIN_ID: " + CHAIN_ID +", averageData: " + averageData);
        doGibbs(data, numIterations, 0, CHAIN_ID, averageData);
    }

    /**
     * @param data          read only
     * @param numIterations
     * @param callNo
     * @param chain_id
     * @param averageData
     */
    private void doGibbs(final CitinfData data, final int numIterations, final int callNo, final String chain_id, final CitinfData averageData) {
    	System.out.println("Internal doGibbs: ");
    	
        //  convdiag
        ConvergenceDiagnosis convDiag = new ConvergenceDiagnosis(SAMPLER_ID, chain_id, ALL_CHAIN_IDS, callNo);
        ChainStatisticsWriter chainStatWriter = new ChainStatisticsWriter(SAMPLER_ID, chain_id, ALL_CHAIN_IDS);
        chainStatWriter.initialize();

        int killNo = (int) Math.rint(Math.random() * 100000000);
        String killFile = killNo + ".kill";
        System.out.println(chain_id + " " + " touch " + killFile + " to stop the process.");
        boolean abort = false;
        Date startCheckpoint = new Date();
        boolean converged = false;
        int numSamples = 0;
       
        //Changed temporarily by Huiping "numIterations"
        for (int i = 0; i < 10 && !abort && !converged; i++) {
            //if (i % 100 == 0) {
                Date newCheckPoint = new Date();
                long diffChecks = newCheckPoint.getTime() - startCheckpoint.getTime();
                if (i == 0) {
                    System.out.println(chain_id + " " + newCheckPoint + "Iteration " + i + "/" + numIterations);
                } else {
                    int itersToGo = numIterations - i;
                    long estimateDiff = (long) (1.0 * diffChecks / i * itersToGo);
                    Date estimateTime = new Date(newCheckPoint.getTime() + estimateDiff);
                    System.out.println(chain_id + " " +
                            newCheckPoint + "Iteration " + i + "/" + numIterations + " ETA = " + estimateTime);
                }
                data.trimToSize();
                abort = killFileExists(killFile);
//                assert (data.assertCounts());
            //}


            if (i >= BURN_IN && i % SAMPLE_LAG == 0) {
                //   convdiag
                // ------------
                final GammaScalarSummaries currentSummary = new GammaScalarSummaries(data.getNumCitingDocs(),
                        data.getBibliographies());


                data.doForAllCitingDwtscNonZeros(new INonZeroPerformer5D() {
                    public void iteration(int d, int w, int t, int s, int c, double val, int position) {
                        if (s == 0) {
                            currentSummary.addScalarDistribution(d, c, val);
                        }
                    }
                });

                currentSummary.distrToSummary();
                convDiag.addSampleToMonitor(currentSummary);
                // -------

                numSamples++;
                if (takeSamplesFromThisChain) {
                    //  average
                    if (averageData != null) {
                        data.doForAllCitingDwtscNonZeros(new INonZeroPerformer5D() {
                            public void iteration(int d, int w, int t, int s, int c, double val, int position) {
                                averageData.clearPositionCitingDwtsc(position);
                                averageData.addCitingDwtsc(d, w, t, s, c, (int) val, position);
                            }
                        });
                        data.doForAllCitedDwtNonZeros(new INonZeroPerformer3D() {
                            public void iteration(int d, int w, int t, double val, int position) {
                                averageData.clearPositionCitedDwt(position);
                                averageData.addCitedDwt(d, w, t, (int) val, position);
                            }
                        });
                    }
                }

                if (minimumTimeHasPassed()) {
                    converged = convDiag.checkForConvergence();
//                    if(Double.isNaN(convDiag.getRHat())){
//                    System.out.println("currentSummary = " + currentSummary.subList(0, Math.min(100, currentSummary.size())));
                    chainStatWriter.addRecord(i, convDiag.getRHat());
                    abort = killFileExists(killFile);
                }
            }
            boolean collectGammaSamples = ((i + 1) >= BURN_IN && (i + 1) % SAMPLE_LAG == 0);
            
            System.out.println("Data at interation "+ i +": " + data);
            oneIteration(data);
        }//end for loop numIterations
        System.out.print(new Date() + " ");
        if (converged) {
            System.out.println(chain_id + " converged");
        } else if (abort) {
            System.out.println(chain_id + " stopped by killfile (" + killFile + ") ");
        } else {
            System.out.println(chain_id + " last Iteration. (" + numIterations + ") ");
        }

        // normalize average data
        if (takeSamplesFromThisChain) {
            if ((averageData != null) && (numSamples > 0)) {
                //           todo sample K
                //  simulAllMjk(averageData);
                averageData.setAverageBy(numSamples);
            }
        }

        // todo sample K
//        buildTopic2KTable();

        //  convdiag
        convDiag.finish();
        chainStatWriter.shutdown();

    }

    private boolean killFileExists(String killFile) {
        File killFileHandle = new File(killFile);
        if (killFileHandle.exists()) {
            killFileHandle.delete();
            return true;
        }
        return false;
    }

    // todo sample K
//    private void simulAllMjk(CitinfData data) {
//        for (int j = 0; j < dMax; j++) {
//            for (int k : data.getUsedTopics()) {
//                int mjk = simulMJK(data.countNJK(j, k), alphaTheta);
//                data.replaceMJK(j, k, mjk);
//            }
//        }
//    }

    private boolean minimumTimeHasPassed() {
        boolean result = true;
        long now = new Date().getTime();
        if (now - lastCheck < MINIMUM_MILLISECS_CONV_SYNC) {
            result = false;
        } else {
            lastCheck = now;
        }
        return result;
    }


    private void oneIteration(final CitinfData data) {
        oneCitedIteration(data);
        oneCitingIteration(data);
    }

    private void oneCitedIteration(final CitinfData data) {
        data.doForAllCitedDwtNonZeros(new INonZeroPerformer3D() {
            public void iteration(int d, int w, int t, double n_, int position) {
                assert (n_ == 1.0) : n_;

                // todo sample K
//                assert (!data.isUnusedTopic(k)) : "k " + k + " is marked as unused! ";
//                assert (data.getUsedTopics().contains(k)) : "k " + k + " is not marked as used! ";
                // interpretation: the word w occurs n times in document s

                // remove old token
                data.addCitedDwt(d, w, t, -1, position);

                // calculate new topic
//                    todo: sample K int[] newK_Type = getMAPTopic(j, i, data);
                int newT = getMAPCitedTopic(d, w, data);

                // add new token
                data.addCitedDwt(d, w, newT, +1, position);

                // todo sample K
//                    // ------- guess new mjk counts -----
////                    // safe simulmjk version
////                    if (k != newT) {
////                        int mjk = simulMJK(data.countNJK(j, k), alphaTheta);
////                        data.replaceMJK(j, k, mjk);
////                    }
//
//                    // update mjk values when a topic is drawn from G0 or H
//                    if ((type != GJ_TOPIC_TYPE) && (k != newT)) {
//                        data.addMJK(j, newT, 1.0);
//                    }
//
//                    // rerun simulMJK when we go into a new doc (i.e. j != oldJ)
//                    if (oldJ[0] != j) {
//                        for (int k_ : data.getUsedTopics()) {
//                            int mjk = simulMJK(data.countNJK(j, k_), alphaTheta);
//                            data.replaceMJK(j, k_, mjk);
//                        }
//                        oldJ[0] = j;
//                    }
            }


        });
    }

    private void oneCitingIteration(final CitinfData data) {
        data.doForAllCitingDwtscNonZeros(new INonZeroPerformer5D() {
            public void iteration(int d, int w, int t, int s, int c, double n_, int position) {
                assert (n_ == 1.0) : n_;

                // todo sample K
//                assert (!data.isUnusedTopic(k)) : "k " + k + " is marked as unused! ";
//                assert (data.getUsedTopics().contains(k)) : "k " + k + " is not marked as used! ";
                // interpretation: the word w occurs n times in document s

                // remove old token
                data.addCitingDwtsc(d, w, t, s, c, -1, position);

                // calculate new topic
//                    todo: sample K int[] newK_Type = getMAPTopic(j, i, data);
                int[] newTSC = getMAPCitingTSC(d, w, data);
                int newT = newTSC[0];
                int newS = newTSC[1];
                int newC = newTSC[2];

                // add new token
                data.addCitingDwtsc(d, w, newT, newS, newC, +1, position);

                // todo sample K
//                    // ------- guess new mjk counts -----
////                    // safe simulmjk version
////                    if (k != newT) {
////                        int mjk = simulMJK(data.countNJK(j, k), alphaTheta);
////                        data.replaceMJK(j, k, mjk);
////                    }
//
//                    // update mjk values when a topic is drawn from G0 or H
//                    if ((type != GJ_TOPIC_TYPE) && (k != newT)) {
//                        data.addMJK(j, newT, 1.0);
//                    }
//
//                    // rerun simulMJK when we go into a new doc (i.e. j != oldJ)
//                    if (oldJ[0] != j) {
//                        for (int k_ : data.getUsedTopics()) {
//                            int mjk = simulMJK(data.countNJK(j, k_), alphaTheta);
//                            data.replaceMJK(j, k_, mjk);
//                        }
//                        oldJ[0] = j;
//                    }
            }


        });
    }

    /**
     * Calculate mjk (tables in document j serving topic k) by simulating the DP process.
     *
     * @param njk   number of words in document j being associated with topic k
     * @param alpha alpha of the DP
     * @return mjk >=0
     */
    private int simulMJK(double njk, double alpha) {
        if (njk == 0) return 0;
        else if (njk == 1) return 1;
        else {
            int mjk = 1;
            for (int i = 1; i < njk; i++) {
                double rnd = Math.random();
                double dpTableArrival = (alpha / (i + alpha));
                if (rnd < dpTableArrival) {
                    mjk++;
                }
            }
            return mjk;
        }
    }

    private int getMAPCitedTopic(int d, int w, CitinfData data) {
        mapTopicPosteriorDistr.clear();

        for (int t = 0; t < kMaxVal; t++) {
            double prob = citedPosteriorT(d, w, t, data);
            assert (prob >= 0.0) : "probG0 must be positive but is " + prob + ". d=" + d + " w=" + w + " t=" + t;
            assert (!Double.isNaN(prob));
            assert (!Double.isInfinite(prob));
            mapTopicPosteriorDistr.put(t, prob);
        }

        if (mapTopicPosteriorDistr.sum() == 0.0) {
            System.err.println("Posterior distribution.sum()==0.0. distr(10):" + mapTopicPosteriorDistr.toString());
            mapTopicPosteriorDistr.initializeEqualDistribution(kMaxVal);
        } else {
//            posteriorDistr.normalize();
        }

        int mapK = mapTopicPosteriorDistr.draw();


        return mapK;
    }


    private int[] getMAPCitingTSC(int d, int w, CitinfData data) {
        mapTopicPosteriorDistr.clear();

        for (int t = 0; t < kMaxVal; t++) {
            for (int c : bibliographies.get(d)) {
                double probGj = citingPosteriorTSC(d, w, t, 0, c, data);
                assert (probGj >= 0.0) : "probG0 must be positive but is " + probGj + ". d=" + d + " w=" + w + " t=" + t;
                assert (!Double.isNaN(probGj));
                assert (!Double.isInfinite(probGj));
                mapTopicPosteriorDistr.put(t, 0, c, probGj);
            }
        }

        for (int t = 0; t < kMaxVal; t++) {
            double probGj = citingPosteriorTSC(d, w, t, 1, -1, data);
            assert (probGj >= 0.0) : "probG0 must be positive but is " + probGj + ". d=" + d + " w=" + w + " t=" + t;
            assert (!Double.isNaN(probGj));
            assert (!Double.isInfinite(probGj));
            mapTopicPosteriorDistr.put(t, 1, -1, probGj);
        }

        if (mapTopicPosteriorDistr.sum() == 0.0) {
            System.err.println("Posterior distribution.sum()==0.0. distr(10):" + mapTopicPosteriorDistr.toString());
            mapTopicPosteriorDistr.initializeEqualDistribution(kMaxVal);
        } else {
//            posteriorDistr.normalize();
        }

        int mapT = mapTopicPosteriorDistr.draw();
        int mapS = mapTopicPosteriorDistr.getKey2Draw();
        int mapC = mapTopicPosteriorDistr.getKey3Draw();


        return new int[]{mapT, mapS, mapC};
    }

//        // todo sample K
//    private int getMAPTopic(int j, int i, CitinfData data) {
//        mapTopicPosteriorDistr.clear();
//
//        //        IDistribution<Integer> posteriorDistr = DistributionFactory.<Integer>createDistribution();
//        for (int k : data.getUsedTopics()) {
//            double probGj = topicGjPosterior(j, i, k, data);
//            assert (probGj >= 0.0) : "probG0 must be positive but is " + probGj + ". j=" + j + " i=" + i + " k=" + k;
//            assert (!Double.isNaN(probGj));
//            assert (!Double.isInfinite(probGj));
//            mapTopicPosteriorDistr.put(k, GJ_TOPIC_TYPE, probGj);
//
//
//            double probG0 = topicG0Posterior(j, i, k, data);
//            assert (probG0 >= 0.0) : "probG0 must be positive but is " + probG0 + ". j=" + j + " i=" + i + " k=" + k;
//            assert (!Double.isNaN(probG0));
//            assert (!Double.isInfinite(probG0));
//            mapTopicPosteriorDistr.put(k, G0_TOPIC_TYPE, probG0);
//        }
//        try {
//            int k = data.getUnusedTopic();
//            double probH = topicHPosterior(j, i, k, data);
//            assert (probH >= 0.0) : "probH must be positive but is " + probH + ". j=" + j + " i=" + i + " k=" + k;
//            assert (!Double.isNaN(probH));
//            assert (!Double.isInfinite(probH));
//            mapTopicPosteriorDistr.put(k, H_TOPIC_TYPE, probH);
//        } catch (CitinfData.NoMoreUnusedTopicsException e) {
//            // if no more unused topics are available, we can either augment the data structures or simply do not allow any more topics.
//            // here we decide to disallow new topics.
//            System.err.println("CitinfWrapper#getMAPTopic: All topics are used up (those are " + kMaxVal + "). Prohibiting new topics.");
//        }
//
//        if (mapTopicPosteriorDistr.sum() == 0.0) {
//            System.err.println("Posterior distribution.sum()==0.0. distr(10):" + mapTopicPosteriorDistr.toString());
//            mapTopicPosteriorDistr.initializeEqualDistribution(data.getUsedTopics());
//        } else {
////            posteriorDistr.normalize();
//        }
//
//        int mapT = mapTopicPosteriorDistr.draw();
//        int type = mapTopicPosteriorDistr.getKey2Draw();
//
//
//        return new int[]{mapT, type};
//    }

    //

    private double citedPosteriorT(int d, int w, int t, CitinfData data) {
        double theta = theta(t, d, data);
        double phi = phi(w, t, data);
        return theta * phi;
    }

    private double citingPosteriorTSC(int d, int w, int t, int s, int c, CitinfData data) {
        if (s == 0) {
            double lambda = lambda(s, d, data);
            double theta = theta(t, c, data);
            double phi = phi(w, t, data);
            double gamma = gamma(d, c, data);
            return lambda * gamma * theta * phi;
        } else {
            double lambda = lambda(s, d, data);
            double psi = psi(t, d, data);
            double phi = phi(w, t, data);
            return lambda * psi * phi;
        }
    }

    // todo sample K
//    private double topicGjPosterior(int j, int i, int k, CitinfData data) {
//        double thetaGj = thetaGj(j, k, data);
//        double phi = phi(i, k, data);
//        return thetaGj * phi;
//    }
//
//    private double topicG0Posterior(int j, int i, int k, CitinfData data) {
//        double thetaG0 = thetaG0(j, k, data);
//        double phi = phi(i, k, data);
//        return thetaG0 * phi;
//    }
//
//    private double topicHPosterior(int j, int i, int k, CitinfData data) {
//        double thetaH = thetaH(j, k, data);
//        double phi = phi(i, k, data);
//        return thetaH * phi;
//    }
//
//    /**
//     * p(k_{ji}=k,\textrm{draw from }G_{j}|w_{ji},k_{-ji})\propto\frac{n_{j\cdot k}}{n_{j\cdot\cdot}+\alpha}\cdot\phi_{k}(w_{ij})
//     *
//     * @param j
//     * @param k
//     * @param data
//     * @return
//     */
//    private double thetaGj(int j, int k, CitinfData data) {
//        double num = data.countNJK(j, k);
//        double den = data.countNJ(j) + alphaTheta;
//        assert (num >= 0.0) : num;
//        assert (den >= 0.0) : den;
//        return num / den;
//    }
//
//    /**
//     * p(k_{ji}=k,\textrm{draw from }G_{0}|w_{ji},k_{-ji})\propto\frac{\alpha m_{k}}{(n_{j\cdot\cdot}+\alpha)\cdot(m_{\cdot}+\gamma)}\cdot\phi_{k}(w_{ij})
//     *
//     * @param j
//     * @param k
//     * @param data
//     * @return
//     */
//    private double thetaG0(int j, int k, CitinfData data) {
//        double num = alphaTheta * data.countMK(k);
//        double den = (data.countNJ(j) + alphaTheta) * (data.countM() + gamma);
//        assert (num >= 0.0) : num;
//        assert (den >= 0.0) : den;
//        return num / den;
//    }
//
//    /**
//     * p(k_{ji}=k^{new},\textrm{draw from }H|w_{ji},k_{-ji})\propto\frac{\alpha\gamma}{(n_{j\cdot\cdot}+\alpha)\cdot(m_{\cdot}+\gamma)}\cdot\phi_{k}(w_{ij})
//     *
//     * @param j
//     * @param k
//     * @param data
//     * @return
//     */
//    private double thetaH(int j, int k, CitinfData data) {
//        double num = alphaTheta * gamma;
//        double den = (data.countNJ(j) + alphaTheta) * (data.countM() + gamma);
//        assert (num >= 0.0) : num;
//        assert (den >= 0.0) : den;
//        return num / den;
//    }
//
//    /**
//     * \frac{n_{\cdot ik}+H}{n_{\cdot\cdot k}+VH}
//     *
//     * @param i
//     * @param k
//     * @param data
//     * @return
//     */
//    private double phi(int i, int k, CitinfData data) {
//        double num = data.countNIK(i, k) + alphaPhi;
//        double den = data.countNK(k) + wMax * alphaPhi;
//        assert (num >= 0.0) : num;
//        assert (den >= 0.0) : den;
//        return num / den;
//    }


    private double phi(int w, int t, CitinfData data) {
        double num = data.countWTAll(w, t) + alphaPhi;
        double den = data.countTAll(t) + data.getNumWords() * alphaPhi;
        return num / den;
    }

    private double theta(int t, int c, CitinfData data) {
        assert (t != -1) : t + " " + c;
        assert (c != -1) : t + " " + c;

        double num = data.countTDCited(t, c) + data.countTCs0Citing(t, c) + alphaTheta;
        double den = data.countDCited(c) + data.countCs0Citing(c) + data.getNumTopics() * alphaTheta;
        return num / den;
    }

    private double psi(int t, int d, CitinfData data) {
        double num = data.countTDs1Citing(t, d) + alphaPsi;
        double den = data.countDs1Citing(d) + data.getNumTopics() * alphaPsi;
        return num / den;
    }

    private double gamma(int d, int c, CitinfData data) {
        double num = data.countDCs0Citing(d, c) + alphaGamma;
        double den = data.countDs0Citing(d) + data.getBibSize(d) * alphaGamma;
        return num / den;
    }

    private double lambda(int s, int d, CitinfData data) {
        double num;
        if (s == 0) {
            num = data.countSDCiting(s, d) + alphaLambdaInherit;
        } else {
            num = data.countSDCiting(s, d) + alphaLambdaInnov;
        }
        double den = data.countDCiting(d) + alphaLambdaInherit + alphaLambdaInnov;
        return num / den;
    }

    // ///////////////////////////////////////////////////////////////////////////////
    //           GET / SET / OTHER
    // ///////////////////////////////////////////////////////////////////////////////


    public String toString() {
        return data.toString();
    }

    public CitinfData getData() {
        return data;
    }

    public int getIterations() {
        return numIterations;
    }


    public double getAlphaPhi() {
        return alphaPhi;
    }


    public double getGamma() {
        return gamma;
    }

    public double getAlphaTheta() {
        return alphaTheta;
    }


    public boolean collectedEnoughSamples() {
        return averageData.collectedEnoughSamples();
    }

    // todo sample K
//    private void buildTopic2KTable() {
//        topic2KTable = new DualHashBidiMap();
//        int topic = 0;
//        for (int k : averageData.getUsedTopics()) {
//            assert (!averageData.isUnusedTopic(k));
//            topic2KTable.put(topic, k);
//            topic++;
//        }
//    }
//

    public int getNumTopics() {
        return kMaxVal;
        // todo sample K
//        if (topic2KTable == null) {
//            throw new RuntimeException("call buildTopic2KTable first!");
//        }
//        return topic2KTable.size();
    }

    // todo sample K


    /**
     * Calculate word distributions for each topic c.
     * <p/>
     * result.get(k).get(i) == phi_k(w==i)
     *
     * @return returns empty distributions for k that are not used.
     */
    public List<IDistribution<Integer>> getPhis() {
        assert (takeSamplesFromThisChain);

        ArrayList<IDistribution<Integer>> result = new ArrayList<IDistribution<Integer>>();
        for (int t = 0; t < getNumTopics(); t++) {
            IDistribution<Integer> distr = DistributionFactory.<Integer>createDistribution();
            result.add(distr);
        }
        for (int t = 0; t < kMaxVal; t++) {
            int topic = (Integer) t;
            IDistribution<Integer> distr = result.get(topic);
            for (int i = 0; i < wMax; i++) {
                double phi = phi(i, t, averageData);
                assert (phi >= 0);
                assert (!Double.isNaN(phi));
                assert (!Double.isInfinite(phi));
                distr.put(i, phi);
            }
        }
        return result;
    }

    public IDistribution<Integer> getTheta(int d) {
        assert (takeSamplesFromThisChain);
        IDistribution<Integer> result = DistributionFactory.createDistribution();
        for (int t = 0; t < kMaxVal; t++) {
            int topic = (Integer) t;

            double theta = theta(t, d, averageData);
            result.put(topic, theta);
        }

        return result;
    }

    /**
     * @param d
     * @param citedDocsP2B used for resolving c-bugsid back to the pubid
     * @return
     */
    public IDistribution<Integer> getGamma(int d, BidiMap citedDocsP2B) {
        assert (takeSamplesFromThisChain);
        IDistribution<Integer> result = DistributionFactory.createDistribution();
        for (int c : bibliographies.get(d)) {
            double gamma = gamma(d, c, averageData);
            Integer cid = (Integer) citedDocsP2B.getKey(c);
            assert (cid != null);
            result.put(cid, gamma);
        }

        return result;
    }

    public IDistribution<Integer> getLambdaDistr(int d) {
        assert (takeSamplesFromThisChain);
        IDistribution<Integer> result = DistributionFactory.createDistribution();
        result.put(0, lambda(0, d, averageData));
        result.put(1, lambda(1, d, averageData));
        return result;
    }

    public double getLambda(int d) {
        assert (takeSamplesFromThisChain);
        return lambda(0, d, averageData);
    }

    public void doForAllDWTSCCitingAveragedNonZeros(INonZeroPerformer5D performer) {
        assert (takeSamplesFromThisChain);
        averageData.doForAllCitingDwtscNonZeros(performer);
    }
}
