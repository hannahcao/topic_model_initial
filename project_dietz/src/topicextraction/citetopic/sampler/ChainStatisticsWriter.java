package topicextraction.citetopic.sampler;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Writes statistics of sampling chains. Provides an online-per chain excel sheet as well as a full-stats-overview.
 * The full stats overview provides
 * <ul>
 * <li> rhat over iteration number for each chain (ready to be plottet with a scatterplot in excel)
 * <li> summary stats such as average time per iteration, average iterations until convergence, average sampling time
 * </ul>
 *
 * @version $ID$
 */
public class ChainStatisticsWriter implements Serializable {
    private String experiment;
    private static final long serialVersionUID = 7039143622833961160L;
    private transient FileWriter writer = null;
    private transient File file;
    private Date firstDate = null;
    private Map<Integer, Record> records = new LinkedHashMap<Integer, Record>();
    private final String samplerId;
    private final String chainId;
    private final List<String> allChainIds;
    private int rowNumber = 1;

    private static enum Language {
        DE, EN
    }

    private Language lang = Language.EN;


    public ChainStatisticsWriter(String samplerid, String chainid, List<String> allChainIds) {
        this.samplerId = samplerid;
        this.chainId = chainid;
        this.allChainIds = allChainIds;
        this.experiment = samplerid + "-" + chainid;
        file = new File(experiment + ".stat.xls");
        writer = openWriter(true);
        System.out.println("file.getAbsolutePath() = " + file.getAbsolutePath());

    }

    // --------------------------------------------
    //             interface methods
    // --------------------------------------------

    public void initialize() {

        FileWriter out = openWriter(true);
        writePrologToXls(samplerId, chainId, out);
        closeWriter();

    }

    public void addRecord(int iteration, double rhat) {
        if (firstDate == null) {
            firstDate = new Date();
        }
        Date now = new Date();
        long millisecsElapsed = (now.getTime() - firstDate.getTime());
        records.put(iteration, new Record(iteration, rhat, millisecsElapsed, now));

        FileWriter out = openWriter(false);
        writeSingleChainDataToXls(rhat, iteration, now, millisecsElapsed, out);
        closeWriter();

    }

    public void shutdown() {
        FileWriter out = openWriter(false);
        writeEpilogToXls(out);
        try {
            out.close();
        } catch (IOException e) {
            System.err.println("ChainStatisticsWriter#writeEpilogToXls: " + e.toString());
        }

        writeFullStatistics();
    }

    // -------------------------------------------------------
    //     online excel export
    // -------------------------------------------------------

    private FileWriter openWriter(boolean create) {
        if (create) {
            try {
                writer = new FileWriter(file, !create);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return writer;
    }

    private void closeWriter() {
        try {
            writer.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void writePrologToXls(String samplerId, String chainId, final FileWriter out) {
        try {
            out.write("ChainStatisticsWriter " + samplerId + "-" + chainId + newline());
            out.write("Iteration \trhat \tmilliseconds elapsed \ttimestamp" + newline());
        } catch (IOException e) {
            System.err.println("ChainStatisticsWriter#writePrologToXls: " + e.toString());
        }
    }

    private void writeGlobalChainDataToXls(double rhat, int iteration, Date now, long millisecsElapsed, final FileWriter out) {
        try {
            String rHatString = cut(rhat);
            if (Double.isNaN(rhat)) {
                rHatString = "";
            }
            String timestamp = formatDate(now);
            out.write(iteration + "\t" + rHatString + "\t" + millisecsElapsed + "\t" + timestamp + newline());
        } catch (IOException e) {
            System.err.println("ChainStatisticsWriter#writeGlobalChainDataToXls: " + e.toString());
        }
    }

    private String formatDate(Date date) {
        SimpleDateFormat form = dateFormat();
        String timestamp = form.format(date);
        return timestamp;
    }

    private void writeSingleChainDataToXls(double rhat, int iteration, Date now, long millisecsElapsed, final FileWriter out) {
        try {
            String rHatString = cut(rhat);
            if (Double.isNaN(rhat)) {
                rHatString = "";
            }
            SimpleDateFormat form = dateFormat();
            out.write(iteration + "\t" + rHatString + "\t" + millisecsElapsed + "\t" + form.format(now) + newline());
        } catch (IOException e) {
            System.err.println("ChainStatisticsWriter#writeGlobalChainDataToXls: " + e.toString());
        }
    }

    private SimpleDateFormat dateFormat() {
        if (lang == Language.DE) {
            return new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
        } else if (lang == Language.EN) {
            return new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
        } else {
            throw new UnsupportedOperationException("language " + lang + " not supported");
        }
    }

    private void writeEpilogToXls(final FileWriter out) {
    }

    private void writeOther(final FileWriter out) {
        try {
            String iterEpi = "";
            String rHatEpi = "";
            String milliEpi = "";
            String timestampEpi = "";
            out.write(iterEpi + "\t" + rHatEpi + "\t" + milliEpi + "\t" + timestampEpi + newline());
        } catch (IOException e) {
            System.err.println("ChainStatisticsWriter#writeEpilogToXls: " + e.toString());
        }

    }

    // --------------------------------------------
    //     full statistics
    // --------------------------------------------

    public void writeFullStatistics() {
        writeStatisticSummaryToDisc();
        try {
            Map<String, Map<Integer, Record>> summaries = readStatisticSummariesFromDisc();
            writeSummariesToFullStat(summaries);
            if(cao.Constant.DeleteTemporaryFile)
            	deleteStatistiscSummariesFromDisc();
        } catch (AccessSummaryFileException e) {
            System.out.println("other statistic summaries are not ready yet, relying on others.");
        }


    }

    private void writeStatisticSummaryToDisc() {
        String storageName = samplerId + "-" + chainId + ".stat.object";
//        String storageName = RealDiscDataExperiment.getClassName(model.getModelName(), AUTHOR_ID);
        try {
            ObjectOutputStream w = new ObjectOutputStream(new FileOutputStream(storageName));
            w.writeObject(records);
            w.flush();
            w.close();
        } catch (IOException e) {
            throw new RuntimeException(e);  //todo handle
        }
    }

    private void deleteStatistiscSummariesFromDisc() {
        for (String chain : allChainIds) {
            String filename = samplerId + "-" + chain + ".stat.object";
            File f = new File(filename);
            f.delete();
        }
    }

    protected Map<String, Map<Integer, Record>> readStatisticSummariesFromDisc() throws AccessSummaryFileException {
        Map<String, Map<Integer, Record>> summaries = new HashMap<String, Map<Integer, Record>>();

        for (String chain : allChainIds) {
            if (!chain.equals(chainId)) {
                boolean readCompleted = false;
                int retryNo = 0;
                while (!readCompleted && retryNo < 3) {
                    String filename = samplerId + "-" + chain + ".stat.object";
                    try {
                        ObjectInputStream r = new ObjectInputStream(new FileInputStream(filename));
                        Object summary_ = r.readObject();
                        r.close();
                        Map<Integer, Record> summary = (Map<Integer, Record>) summary_;
                        summaries.put(chain, summary);
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
        summaries.put(chainId, records);

        return summaries;
    }

    private void writeSummariesToFullStat(Map<String, Map<Integer, Record>> summaries) {
        resetNewline();
        if (summaries.size() == 1) {
            System.out.println("ChainStatisticsWriter.writeSummariesToFullStat: only one chain available. Skipping stats output.");
            return;
        }

        Map<String, Integer> beginBlocks = new HashMap<String, Integer>();
        Map<String, Integer> endBlocks = new HashMap<String, Integer>();


        try {
            File f = new File(samplerId + ".fullstat.xls");
            FileWriter out = new FileWriter(f);

            // write per chain summary

            Map<Integer, Record> summMap0 = summaries.get(allChainIds.get(0));
            writeGlobalPrologToXls(out);


            beginBlocks.put(chainId, rowNumber);

            for (int iterkey : summMap0.keySet()) {
                out.write(iterkey + "\t");
                for (String chainId : allChainIds) {

                    Map<Integer, Record> map = summaries.get(chainId);
//               for (int iterkey : map.keySet()) {
                    Record rec = map.get(iterkey);
                    if (rec != null && !Double.isNaN(rec.getRHat())) {
                        out.write(rec.getRHat() + "\t");
                    } else {
                        out.write("\t");
                    }
                }
                out.write(newline());
            }
            endBlocks.put(chainId, rowNumber - 1);
            writeEpilogToXls(out);
            out.write(newline() + newline() + newline());


            Summer sumDuration = new Summer();

            // write overview summary
            out.write(
                    "Chain id\tIterations until convergence\tlast rhat\taverage millisecs per iter\tfrom date\tto date"
                            + newline());
            int beginOverview = rowNumber;
            for (String chainId : allChainIds) {
                Integer endRow = endBlocks.get(chainId);
                Integer beginRow = beginBlocks.get(chainId);
                Map<Integer, Record> map = summaries.get(chainId);


                int itersUntilConv = 0;
                for (int iter : map.keySet()) {
                    Record record = map.get(iter);
                    if (record.getRHat() < ConvergenceDiagnosis.R_HAT_THRESH) {
                        itersUntilConv = iter;
                        break;
                    }
                }
                if (itersUntilConv == 0 && !map.keySet().isEmpty()) {
                    itersUntilConv = Collections.max(map.keySet()); // chain did not converge
                }
                String maxIter = "" + itersUntilConv;

                if (!map.isEmpty()) {
                    Integer maxIter_ = Collections.max(map.keySet());
                    String lastRhat = "" + cut(map.get(maxIter_).getRHat());

                    long duration = map.get(maxIter_).getMillisecoundsElapsed();
                    double avgTimePerIter_ = 1.0 * duration / maxIter_;
                    String avgTimePerIter = "" + cut(avgTimePerIter_);

                    int minIter_ = Collections.min(map.keySet());
                    Date beginDate = map.get(minIter_).getTimestamp();
                    Date endDate = map.get(maxIter_).getTimestamp();
                    String fromDate = formatDate(beginDate);
                    String todate = formatDate(endDate);

                    sumDuration.addToSum(avgTimePerIter_ * itersUntilConv);

                    String ganttchart = "=A" + rowNumber + "\t=E" + rowNumber + "\t=F" + rowNumber + "-E" + rowNumber;
                    out.write(chainId + "\t" + maxIter + "\t" + lastRhat + "\t" + avgTimePerIter + "\t" + fromDate + "\t"
                            + todate + "\t\t\t" + ganttchart + newline());
                } else {
                    out.write(chainId + "\tnot enough data collected.");
                }
            }
            int endOverview = rowNumber - 1;
            out.write("Average\t" + vMittel("B", beginOverview, endOverview) + "\t"
                    + vMittel("C", beginOverview, endOverview) + "\t" + vMittel("D", beginOverview, endOverview)
                    + newline());

            int averageRow = rowNumber - 1;
            out.write(newline());
            out.write("Average Duration\thours\tmins\tsecs" + newline());
            int thisRow = rowNumber;
            // average duration
            String avgDurInMillis = "" + (sumDuration.getSum() / sumDuration.getNumberOfSummands());

            String hours = ganzzahl("$A" + thisRow + "/1000/60/60");
//            String hours = "=GANZZAHL($A" + thisRow + "/1000/60/60)";
            String mins = ganzzahlRest("$A" + thisRow + "/1000/60", 60);
//            String mins = "=GANZZAHL(REST($A" + thisRow + "/1000/60;60))";
            String secs = ganzzahlRest("$A" + thisRow + "/1000", 60);
//            String secs = "=GANZZAHL(REST($A" + thisRow + "/1000;60))";
            out.write(avgDurInMillis + "\t" + hours + "\t" + mins + "\t" + secs + newline());

            // klappe zu, affe tot.
            out.flush();
            out.close();

        } catch (IOException e) {
            System.err.println("ChainStatisticsWriter#writeSummariesToFullStat: " + e.toString());
        }


    }

    private String ganzzahlRest(String value, int divisor) {
        if (lang == Language.DE) {
            return "=GANZZAHL(REST(" + value + ";" + divisor + "))";
        } else if (lang == Language.EN) {
            return "=INT(MOD(" + value + "," + divisor + "))";
        } else {
            throw new UnsupportedOperationException("language " + lang + " not supported");
        }

    }

    private String ganzzahl(String value) {
        if (lang == Language.DE) {
            return "=GANZZAHL(" + value + ")";
        } else if (lang == Language.EN) {
            return "=INT(" + value + ")";
        } else {
            throw new UnsupportedOperationException("language " + lang + " not supported");
        }

    }

    private void writeGlobalPrologToXls(FileWriter out) throws IOException {
        out.write("ChainStatisticsWriter " + samplerId + newline());
        out.write("Rhat over iterations");
        out.write("Iteration \t");
        for (String chainId : allChainIds) {
            out.write(chainId + "\t");
        }
        out.write(newline());
    }

    private String vMittel(String col, int beginRow, int endRow) {
        if (lang == Language.DE) {
            return "=mittelwert(" + col + beginRow + ":" + col + endRow + ")";
        } else if (lang == Language.EN) {
            return "=average(" + col + beginRow + ":" + col + endRow + ")";
        } else {
            throw new UnsupportedOperationException("language " + lang + " not supported");
        }
    }

    // --------------------------------------------
    //             util
    // --------------------------------------------

    private String newline() {
        rowNumber++;
        return "\n";
    }

    private void resetNewline() {
        rowNumber = 1;
    }

    public static String cut(double value) {
        double tenPower = Math.pow(10, 4);
        double scaledInt = Math.rint(value * tenPower);
        double cuttedVal = scaledInt / tenPower;
        return "" + cuttedVal;

    }

    private static class Record implements Serializable {
        private int iteration;
        private double rHat;
        private long millisecoundsElapsed;
        private Date timestamp;
        private static final long serialVersionUID = 7039778622833961160L;

        public Record(int iteration, double rHat, long millisecoundsElapsed, Date timestamp) {
            this.iteration = iteration;
            this.rHat = rHat;
            this.millisecoundsElapsed = millisecoundsElapsed;
            this.timestamp = timestamp;
        }

        public int getIteration() {
            return iteration;
        }

        public double getRHat() {
            return rHat;
        }

        public long getMillisecoundsElapsed() {
            return millisecoundsElapsed;
        }

        public Date getTimestamp() {
            return timestamp;
        }
    }

    protected static class AccessSummaryFileException extends Exception {
        private String chainId;

        public AccessSummaryFileException(String chainId) {
            super("Could not access statistic summary file of chain " + chainId);
            this.chainId = chainId;
        }

        public String getChainId() {
            return chainId;
        }
    }


}
