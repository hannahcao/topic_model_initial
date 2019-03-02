package topicextraction.citetopic.sampler;

import topicextraction.citetopic.CiteTopicUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cao.Debugger;

/**
 * Object for monitoring convergence of various sampling chains.
 * <p/>
 * This object takes care of communication with other chains.
 * <p/>
 * See <a href="pinkberry/doc-files/monitoring-pinkberry-javadoc.html"> Implementation of Gibbs Convergence Monitoring</a> for details on the monitoring method.
 * <p/>
 * <h2>Beispiel</h2>
 * Wir haben 3 Ketten, mit meheren Sampling Iterationen (oder
 * States) jede Iteration wird jetzt mal durch ein i dargestellt. Es gibt
 * eine Burn-in periode, die ich mal mit B kennzeichne. Dann sieht das Bild
 * zu einem Zeitpunkt t (zu dem wir die Konvergenz feststellen wollen) so aus:
 * <p/>
 * chain 1: BBBBB i11 i12 i13 i14 i15
 * chain 2: BBBBB i21 i22 i23 i24 i25
 * chain 3: BBBBB i31 i32 i33 i34 i35
 * <p/>
 * fuer jedes i gibt es einen skalaren wert, der den zustand der kette in
 * dieser iteration symbolisiert.
 * <p/>
 * Der ansatz misst nun fuer jede kette die within varianz der skalaren
 * werte, z.B. fuer chain 1 die varianz von summary(i11), summary(i12), ...
 * summary(i15). Und vergleicht sie mit der varianz wenn man alle i's
 * zusammen wirft, also summary(i11), summary(i21), summary(i31),
 * summary(i12), summary(i22), summary(i32), ... ,  summary(i15),
 * summary(i25), summary(i35).
 * <p/>
 * Wenn diese varianzen etwa gleich gross sind, schliessen wir, dass die
 * ketten konvergiert sind. Anschliessend erhaelt man die gesuchte
 * verteilung wenn man alle zustaende mittelt.
 * <p/>
 * Soviel zur Theorie. In meinem Fall ist es nun so, dass ich mehrere
 * zufallsvariablen habe (Z1, Z2, ... Z10) die zu einer iteration gehoeren.
 * Jetzt muss in summary(i_kj) die Belegungen der Zufallsvariablen Zmkj
 * (Variable m im zustand j der kette k) eingehen. Was ich nun mache ist
 * fuer jede zufallsvariable das summary einzeln zu speichern, d.h. ich
 * habe nicht nur eine zahl pro i, sondern eine Liste (unten heisst sie
 * currentSummary). Im Monitor wird dann die withing-chain und
 * inbetween-chain varianzen fuer jede variable einzeln berechnet und
 * anschliessend die varianzen gemittelt.
 * <p/>
 * <p/>
 * Mein ConvergenceDiagnosis bedient man nun so. Fuer jede Kette wird einer
 * instanziiert, man sagt ihm fuer welche ketter er steht und welche
 * anderen ketten es noch gibt. Jetzt hier mal ein Beispiel fuer chain 2.
 * <p/>
 * convDiag = new ConvergenceDiagnosis("mysampler", 2, {1,2,3});
 * <p/>
 * Nach jeder iteration (oder nach je 20 iterationen), steckt man die
 * zufallsvariablen in den monitor rein. Also wenn wir chain 2 im zustand j
 * reinstecken wollen, merken wir uns fuer alle m=1..10 die belegungen
 * Zmj2 in der Liste currentSummary. Diese packen wir so in den Monitor.
 * (Achtung, die Reihenfolge der Zufallsvariablen in der Liste muss immer
 * gleich bleiben! Also immer zuerst Z1kj dann Z2kj, ...)
 * <p/>
 * convDiag.addSampleToMonitor(currentSummary);
 * <p/>
 * Der Monitor legt eine Datei an, die mysampler.2 heisst und speichert
 * hier die belegungen (sowie alle vorigen gespeicherten belegungen)
 * <p/>
 * <p/>
 * Wenn wir jetzt auf konvergenz pruefen wollen, geht das so.
 * <p/>
 * converged = convDiag.checkForConvergence();
 * <p/>
 * In dieser Methode werden die dateien mysampler.1 und mysampler.3 gelesen
 * und daraus within-chain und between-chain varianzen bestimmt. Die
 * staerke der Konvergenz r^ kann man mit -Dtorel.rhat=1.1 setzen. Ich
 * glaube default ist 1.2.
 *
 * @version $ID$
 */
public class ConvergenceDiagnosis implements Serializable {
    private static final long serialVersionUID = -1757475258702997531L;
    private List<List<Double>> scalarSummariesOverTime = new ArrayList<List<Double>>();
    private List<Averager> scalarSummariesMean = null;
    private String samplerId;
    private String chainId;
    private ScalarSummaryStorage summaryStorage;
    private List<String> allChainIds;
    private boolean allChainsFinished = WRITE_DEBUG;
    public static final double R_HAT_THRESH = Double.parseDouble(System.getProperty("torel.rhat", "1.2"));
    private static final boolean WRITE_DEBUG = false;
    private double lastRHat = Double.POSITIVE_INFINITY;
    private int callNo = -1;

    public ConvergenceDiagnosis(String samplerId, String chainId, List<String> allChainIds, int callNo) {
        this.samplerId = samplerId;
        // there might be problems with spaces etc. originated from shell parameter passing
        this.chainId = chainId.trim().toLowerCase() + "#" + callNo;
        if (this.chainId.length() < 1) {
            throw new IllegalArgumentException("chainId not given");
        }
        this.allChainIds = new ArrayList<String>();
        for (String chain : allChainIds) {
            String tidyChain = chain.trim().toLowerCase();
            if (tidyChain.length() < 1) {
                throw new IllegalArgumentException(
                        "entry in allChainIds is empty: " + Arrays.toString(allChainIds.toArray()));
            }
            this.allChainIds.add(tidyChain + "#" + callNo);
        }
        System.out.println(Debugger.getCallerPosition()+"samplerId = " + samplerId +",chainId = " + chainId+",allChainIds = " + Arrays.toString(this.allChainIds.toArray()));

        deleteScaarSummaryFromDisc();
        summaryStorage = new ScalarSummaryStorage(samplerId, chainId);
    }

    public void addSampleToMonitor(List<Double> scalarSummaries) {
        addScalarSummaries(scalarSummaries);
        double wj = getWithinSequenceVarPerChain(false);
        double wjtilde = getWithinSequenceVarPerChain(true);

        // todo check first if wj has converged

        summaryStorage.update(wj, wjtilde, scalarSummariesMean, scalarSummariesOverTime.size() - 1);
        writeScalarSummaryToDisc(); //write the summary to the disks, however, these files are marked with deleteOnExit
    }

    /**
     * If this returns true, all chains have converged and we can abort the sampling process.
     * <p/>
     * <h2>Implementation</h2>
     * <ol>
     * <li> calculate rhat
     * <li> if rhat < R_HAT_THRESH, then this chain has converged.
     * <li> if converged, this is marked in the summaries file (which is written to disc)
     * <li> then we continue sampling, until all other chains are converged as well (this is done in {@link #checkAllChainsConverged()} .
     * <li> if all chains are converged, true is returned in order to stop each chain.
     * </ol>
     *
     * @return
     */
    public boolean checkForConvergence() {
        callNo++;
        try {
            Map<String, ScalarSummaryStorage> allChainSummaries = readScalarSummariesFromDisc();

            double rHat = getEstimatedPotentialScaleReduction(allChainSummaries);


            writeChainMeanDebug();
            lastRHat = rHat;


            if ((rHat < R_HAT_THRESH)) {
                if (!summaryStorage.getFinished()) {
                    System.out.println(Debugger.getCallerPosition()+"Chain CONVERGED."+"chainId="+chainId+", callNo="+callNo+",R_HAT_THRESH="+R_HAT_THRESH);
                }
                summaryStorage.setFinished(true);
                writeScalarSummaryToDisc();
                boolean allConverged = checkAllChainsConverged();
                return allConverged;
            } else {
                return false;
            }
        } catch (AccessSummaryFileException e) {
            // if a summary file could not be read, we assume that the chain is still in burn in (or hasnt started yet),
            // to we go on calculating...
            return false;
        }
    }

    private transient FileWriter debugWriter = null;

    private void writeChainMeanDebug() {
        if (WRITE_DEBUG) {
            try {
                Map<String, ScalarSummaryStorage> allChainSummaries = readScalarSummariesFromDisc();
                List<Double> allChainMeans = getAllChainMean(allChainSummaries);

                if (debugWriter == null) {
                    File oldFile = new File("chain-debug" + samplerId + "-" + chainId + ".xls");
                    if (oldFile.exists()) {
                        oldFile.delete();
                    }

                    debugWriter = new FileWriter("chain-debug" + samplerId + "-" + chainId + ".xls");
                    for (int index = 0; index < allChainMeans.size(); index++) {
                        debugWriter.write("\t" + index);
                    }
                    debugWriter.write("\n");
                    debugWriter.flush();
                }


                for (int index = 0; index < allChainMeans.size(); index++) {
                    double scalarVariance = getBetweenSequenceVarPerEntry(allChainSummaries, index, allChainMeans);
                    debugWriter.write("\t" + CiteTopicUtil.cut(scalarVariance, 8));
                }


                debugWriter.write("\n");


                debugWriter.flush();

            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (AccessSummaryFileException e) {
                // ignore
            }
        }
    }

    protected double getEstimatedPotentialScaleReduction(Map<String, ScalarSummaryStorage> allChainSummaries) {
        List<Double> allChainMean = getAllChainMean(allChainSummaries);

        //w = W * n/(n-1)
        double w = getWithinSequenceVar(allChainSummaries);
        
        //wTitle = W
        double wTilde = getWithingSequenceVarTilde(allChainSummaries);


        //bTitle = B/(n-1)
        double bTilde = getBetweenSequenceVar(allChainSummaries, allChainMean);

        //vHat = W + B/(n-1)
        double vHat = wTilde + bTilde;
        // todo check that vHat has converged

        //rHat = (W + B/n-1)/w =(W + W/n-1)/(B * n/(n-1)) = \frac{n-1}{n} + B/(n*W)  
        double rHat = vHat / w;

        if (Double.isNaN(rHat)) {
            if (callNo > 10 && w == 0.0 && vHat == 0.0) {
                rHat = 1.0;
            }
        }

        if ((Double.isNaN(rHat)) || Double.isInfinite(rHat)) {
            System.out.print(Debugger.getCallerPosition()+"L247: rHat = " + rHat);
            System.out.println("  (" + CiteTopicUtil.cut(wTilde) + "+" + CiteTopicUtil.cut(bTilde) + ")/" + CiteTopicUtil.cut(w) + " = (wTilde + bTilde) / w)");
        } else {
//            System.out.println(Debugger.getCallerPosition()+"L250: rHat = " + rHat+",allChainIds="+allChainIds);
//            System.out.println("  (" + CiteTopicUtil.cut(wTilde) + "+" + CiteTopicUtil.cut(bTilde) + ")/" + CiteTopicUtil.cut(w) + " = (wTilde + bTilde) / w)");
        }
        return rHat;
    }

    private double getWithingSequenceVarTilde(Map<String, ScalarSummaryStorage> allChainSummaries) {
        Averager wTilde = new Averager();

        for (String chainKey : allChainSummaries.keySet()) {
            ScalarSummaryStorage summary = allChainSummaries.get(chainKey);
            wTilde.addToAverage(summary.getWithinSeqVarPerChainTilde());
        }
        return wTilde.getAverage();
    }

    private double getWithinSequenceVar(Map<String, ScalarSummaryStorage> allChainSummaries) {
        Averager w = new Averager();

        for (String chainKey : allChainSummaries.keySet()) {
            ScalarSummaryStorage summary = allChainSummaries.get(chainKey);
            w.addToAverage(summary.getWithinSeqVarPerChain());
        }
        return w.getAverage();
    }

    protected List<Double> getAllChainMean(Map<String, ScalarSummaryStorage> allChainSummaries) {
        List<Double> allChainMean = new ArrayList<Double>();

        if (scalarSummariesMean != null) {

            // initialize data structure
            for (int index = 0; index < scalarSummariesMean.size(); index++) {
                allChainMean.add(0.0);
            }

            // sum
            for (String chainKey : allChainSummaries.keySet()) {
                ScalarSummaryStorage storage = allChainSummaries.get(chainKey);
                List<Averager> perChainMean = storage.getScalarSummariesMean();
                for (int index = 0; index < scalarSummariesMean.size(); index++) {
                    allChainMean.set(index, allChainMean.get(index) + perChainMean.get(index).getAverage());
                }
            }

            // average
            for (int index = 0; index < scalarSummariesMean.size(); index++) {
                allChainMean.set(index, allChainMean.get(index) / allChainSummaries.size());
            }

        }
        return allChainMean;
    }

    /**
     * Add the gamma value summary in one gibbs iteration to a list and average the gamma value summaries
     * Commented: Aug. 13, 2012
     *  
     * @param scalarSummaries
     */
    protected void addScalarSummaries(List<Double> scalarSummaries) {
        // add to list
        scalarSummariesOverTime.add(scalarSummaries);

        // initialize data structure for mean
        if (scalarSummariesMean == null) {
            scalarSummariesMean = new ArrayList<Averager>();
            for (int i = 0; i < scalarSummaries.size(); i++) {
                scalarSummariesMean.add(new Averager());
            }
        }

        // add to mean
        for (int i = 0; i < scalarSummaries.size(); i++) {
            double scal = scalarSummaries.get(i);
            scalarSummariesMean.get(i).addToAverage(scal);
        }
    }

    /**
     * @param tildeVersion if true, Wj~ is returned (otherwise Wj)
     * @return either Wj or Wj~
     */
    protected double getWithinSequenceVarPerChain(boolean tildeVersion) {

        Averager withinSeqSum = new Averager();

        for (int index = 0; index < scalarSummariesMean.size(); index++) {//Huiping: each (o,op) pair
            Summer scalarVarianceSummer = new Summer();

            for (List<Double> scalarSummary : scalarSummariesOverTime) {
                double diff = scalarSummary.get(index) - scalarSummariesMean.get(index).getAverage();
                scalarVarianceSummer.addToSum(diff * diff);
            }

            double scalarVariance;
            if (tildeVersion) {//Huiping: sum of each (o,op) pair; scalarVarianceSummer.getNumberOfSummands() = iternation number 
                scalarVariance = scalarVarianceSummer.getSum() / (scalarVarianceSummer.getNumberOfSummands() - 1);
            } else {
                scalarVariance = scalarVarianceSummer.getSum() / (scalarVarianceSummer.getNumberOfSummands());
            }

            withinSeqSum.addToAverage(scalarVariance);
        }
        return withinSeqSum.getAverage(); //either wj =sj^2 * (iternum/iternum-1) or wjTilde = sj^2
    }

    /**
     * @param allChainSummaries
     * @param allChainMeans
     * @return B~ (1/(m-1)) * sum_{j=1}^{m}(\psi_{\dot j} - \psi_{\dot\dot})^2 * (1/(n-1))?
     */
    protected double getBetweenSequenceVar(Map<String, ScalarSummaryStorage> allChainSummaries, List<Double> allChainMeans) {

        Summer betweenSeqSum = new Summer();
        if (scalarSummariesMean == null) {
            return 1.0;
        } else {
            for (int index = 0; index < scalarSummariesMean.size(); index++) {//sum_index=0...(o,op)-pair-number
                double scalarVariance = getBetweenSequenceVarPerEntry(allChainSummaries, index, allChainMeans);

                betweenSeqSum.addToSum(scalarVariance);
            }
            //Huiping: betweenSeqSum.getNumberOfSummands() = number of (o,op) pairs, not the number of iterations
            return betweenSeqSum.getSum() / (betweenSeqSum.getNumberOfSummands() - 1); // originally it should be like this, 
        }
    }

    /**
     * 
     * @param allChainSummaries
     * @param index
     * @param allChainMeans
     * @return (1/(m-1)) * sum_{j=1}^{m}(\psi_{\dot j} - \psi_{\dot\dot})^2 (noted by Huiping)
     */
    private double getBetweenSequenceVarPerEntry(Map<String, ScalarSummaryStorage> allChainSummaries, int index, List<Double> allChainMeans) {
        if (scalarSummariesMean == null) {
            return 1.0;
        } else {
            Summer scalarVarianceSummer = new Summer(); // sum_j=1..m(chainMean_index-allMean_index)^2
            for (String chainKey : allChainSummaries.keySet()) {
                ScalarSummaryStorage summary = allChainSummaries.get(chainKey);
                double diff = scalarSummariesMean.get(index).getAverage() - allChainMeans.get(index);
                scalarVarianceSummer.addToSum(diff * diff);
            }

            // 1/m-1 * scalarVarianceSummer
            //Huiping: scalarVarianceSummer.getNumberOfSummands() = number of chains
            double scalarVariance = scalarVarianceSummer.getSum() / (scalarVarianceSummer.getNumberOfSummands() - 1);
            return scalarVariance;
        }
    }


    private void deleteScaarSummaryFromDisc() {
        File sumFile = new File("scalarSummaries." + samplerId + "." + chainId);
        sumFile.delete();
    }

    private void writeScalarSummaryToDisc() {
        ObjectOutputStream w = null;
        try {
            File sumFile = new File("scalarSummaries." + samplerId + "." + chainId);
            w = new ObjectOutputStream(new FileOutputStream(sumFile));
            w.writeObject(summaryStorage);
            w.flush();
            w.close();
            if(cao.Constant.DeleteTemporaryFile)
            	sumFile.deleteOnExit();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * This is not needed, because all chains clean the summary files on their own via {@link File#deleteOnExit()}.
     *
     * @param scalarSummaries
     */
    private void deleteScalarSummaryFromDisc(Map<String, ScalarSummaryStorage> scalarSummaries) {
        for (ScalarSummaryStorage summary : scalarSummaries.values()) {
            File summaryFile = new File("scalarSummaries." + summary.getSamplerId() + "." + summary.getChainId());
            summaryFile.delete();
        }
    }

    /**
     * Reads a map of scalar summaries of all chains from disc. The summary for this chain is not read from disc
     * but returned directly (so do not modify the summaries!!!)
     * <p/>
     * If a chain result could not be read (which is tested repeatedly), assume that this chain is either in burnin
     * phase or hasnt been started at all. In either case, the chains are considered to be *not converged*.
     * Just continue calculating the chain for a while an try again...
     *
     * @return a map of scalar summaries of all chains (including this one)
     * @throws AccessSummaryFileException if summaries of a chain could not be read.
     */
    protected Map<String, ScalarSummaryStorage> readScalarSummariesFromDisc() throws AccessSummaryFileException {
        Map<String, ScalarSummaryStorage> result = new HashMap<String, ScalarSummaryStorage>();

        for (String chain : allChainIds) {
            if (!chain.equals(chainId)) {
                boolean readCompleted = false;
                int retryNo = 0;
                while (!readCompleted && retryNo < 3) {
                    String filename = "scalarSummaries." + samplerId + "." + chain;
                    try {
                        ObjectInputStream r = new ObjectInputStream(new FileInputStream(filename));
                        Object summary_ = r.readObject();
                        r.close();
                        ScalarSummaryStorage summary = (ScalarSummaryStorage) summary_;
                        result.put(chain, summary);
                        readCompleted = true;
                    } catch (IOException e) {
                        System.err.println("unable to read " + filename + " retrying...");
                        try {
                            retryNo++;
                            Thread.sleep(100);
                        } catch (InterruptedException e1) {

                        }
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }

                if (!readCompleted) {
                    throw new AccessSummaryFileException(chain);
                }
            }
        }
        result.put(chainId, summaryStorage);

        return result;
    }

    /**
     * Checks if all chains have marked themselves as converged.
     * <p/>
     * <h2>Implementation</h2>
     * The first chain (indicated by chainId.equals(allChainIds.get(0)==true),
     * checks all summaries for the finished flag (of course, summary files for all chains must exist),
     * if they are all finished, it creates the file named finished.samplerId and terminates
     * <p/>
     * All other chain continue sampling, until the finished file exists.
     *
     * @return
     */
    private boolean checkAllChainsConverged() {
        try {
            // if all chains are finished, remove scalar summaries
            Map<String, ScalarSummaryStorage> scalarSummaries = null;
            scalarSummaries = readScalarSummariesFromDisc();
            allChainsFinished = checkFinishFileExists();
            if (chainId.equals(allChainIds.get(0)) && !allChainsFinished) {
                allChainsFinished = isAllChainSummariesMarkedAsFinished(scalarSummaries);
                if (allChainsFinished) {
                    try {
                        FileWriter f = new FileWriter("finished." + samplerId);
                        f.write("");
                    } catch (IOException e) {
                        System.err.println("ConvergenceDiagnosis#checkAllChainsConverged: " + e.toString());
                    }
                }
            }
            return allChainsFinished;
        } catch (AccessSummaryFileException e) {
            // one chain has not even been started... which is actually curious, because finish should only be called
            // after *all* chains converged.
            System.err.println("finish should only be called after all chains converged, but could not access "
                    + "summary file for chain " + e.getChainId());
            new RuntimeException(e).printStackTrace();
            return false;
        }
    }

    /**
     * Sets this chain as finished. If all other chains are finished as well, they summaryfiles are removed
     * (To be precise: are marked for deletion once this VM is exited)
     *
     * @return true if this chain is the last chain
     * @see #isAllChainsFinished()
     */
    public boolean finish() {
        if (!checkForConvergence()) {
            System.err.println(
                    "ConvergenceDiagnosis#finish: chain has not converged. Finish must only be called after convergence.");
        }

        return true;


    }

    private boolean checkFinishFileExists() {
        return (new File("finished." + samplerId).exists());
    }

    /**
     * Returns true iff all chains (including this one) have finished. Once true is returned, the sampling results may be aggregated.
     *
     * @return true iff all chains have finished
     */
    public boolean isAllChainsFinished() {
        return allChainsFinished;
    }

    private boolean isAllChainSummariesMarkedAsFinished(Map<String, ScalarSummaryStorage> scalarSummaries) {
        boolean allFinished = true;
        for (String chainId : scalarSummaries.keySet()) {
            ScalarSummaryStorage summary = scalarSummaries.get(chainId);
            allFinished = allFinished && summary.isFinished();
        }
        return allFinished;
    }

    private boolean allChainSummariesDeleted() {
        boolean allDeleted = true;
        for (String cid : allChainIds) {
            if (!cid.equals(chainId)) {
                File f = new File("scalarSummaries." + samplerId + "." + chainId);
                allDeleted = allDeleted && !f.exists();
            }
        }
        return allDeleted;
    }

    /**
     * rhat computed in the last call to {@link #checkForConvergence()}.
     * <p/>
     * -1.0 if {@link #checkForConvergence()} has never been called.
     *
     * @return approximating 1.0 from above.
     */
    public double getRHat() {
        return lastRHat;
    }

    public static class ScalarSummaryStorage implements Serializable {
        private static final long serialVersionUID = -4257883617184580445L;
        private String samplerId;
        private String chainId;
        private int sampleIndex = -1;
        private List<Averager> scalarSummariesMean = null;
        private double withinSeqVarPerChain = 0.0;
        private double withinSeqVarPerChainTilde = 0.0;
        private boolean finished;

        public ScalarSummaryStorage(String samplerId, String chainId) {
            this.chainId = chainId;
            this.samplerId = samplerId;
        }

        public void update(double withinSeqVarPerChain, double withinSeqVarPerChainTilde, List<Averager> scalarSummariesMean, int sampleIndex) {
            this.sampleIndex = sampleIndex;
            this.scalarSummariesMean = scalarSummariesMean;
            this.withinSeqVarPerChain = withinSeqVarPerChain;
            this.withinSeqVarPerChainTilde = withinSeqVarPerChainTilde;
        }

        public String getSamplerId() {
            return samplerId;
        }

        public String getChainId() {
            return chainId;
        }

        //public int getSampleIndex() {
        //    return sampleIndex;
        //}

        public List<Averager> getScalarSummariesMean() {
            return scalarSummariesMean;
        }

        public double getWithinSeqVarPerChain() {
            return withinSeqVarPerChain;
        }

        public double getWithinSeqVarPerChainTilde() {
            return withinSeqVarPerChainTilde;
        }

        public boolean isFinished() {
            return finished;
        }

        public void setFinished(boolean finished) {
            this.finished = finished;
        }

        public boolean getFinished() {
            return finished;
        }
    }

    protected static class AccessSummaryFileException extends Exception {
        private String chainId;

        public AccessSummaryFileException(String chainId) {
            super("Could not access summary file of chain " + chainId);
            this.chainId = chainId;
        }

        public String getChainId() {
            return chainId;
        }
    }


}


