package topicextraction.citetopic.sampler.citinf;

import topicextraction.citetopic.sampler.VocDocHasher;
import util.matrix.ArrayMatrix1D;
import util.matrix.ArrayMatrix2D;
import util.matrix.IMatrix1D;
import util.matrix.IMatrix2D;
import util.matrix.INonZeroPerformer2D;
import util.matrix.INonZeroPerformer3D;
import util.matrix.INonZeroPerformer5D;
import util.matrix.LineMatrix3D;
import util.matrix.LineMatrix5D;
import org.apache.commons.collections.BidiMap;

import cao.Debugger;
import cern.colt.list.DoubleArrayList;


import java.io.Serializable;
import java.util.List;

/**
 * Data storage for {@link topicextraction.citetopic.sampler.pinkberry.PinkberryAveragingSampler}
 */
public class CitinfData implements Serializable {
    private static final long serialVersionUID = -879969322062107644L;
    public static final int MAX_S = 2;
    private int numTopics;
    private int numCites;
    private int numWords;
    private IMatrix2D wdCited;
    private IMatrix2D wdCiting;
    private IMatrix1D dCiting; // lambda  //N
    private IMatrix1D dCited; // theta
    private List<List<Integer>> bibliographies;
    private int numDocs;
    private int numCitedDocs;
    private int numCitingDocs;
    protected LineMatrix3D dwtCited;
    protected LineMatrix5D dwtscCiting;
    private IMatrix2D wtAll; // phi
    private IMatrix1D tAll;  // phi
    private IMatrix2D tdCited; // theta
    private IMatrix2D tcs0Citing; // theta
    private IMatrix1D cs0Citing; // theta
    private IMatrix2D tds1Citing; // psi
    private IMatrix1D ds1Citing; // psi
    private IMatrix2D dcs0Citing; // gamma
    private IMatrix1D ds0Citing; // gamma
    private IMatrix2D sdCiting; // lambda
    private int averageBy = 1;

    public CitinfData(IMatrix2D cited_wd, IMatrix2D citing_wd, int numTopics, int numCites, List<List<Integer>> bibliographies) {
        this.numTopics = numTopics;
        this.numCites = numCites;
        this.wdCited = cited_wd;
        this.wdCiting = citing_wd;
        this.bibliographies = bibliographies;

        numWords = cited_wd.getDimensionSize(0);
        assert (cited_wd.getDimensionSize(0) == citing_wd.getDimensionSize(0)) :
                "cited=" + cited_wd.getDimensionSize(0) + " citing=" + citing_wd.getDimensionSize(0);

        numCitedDocs = cited_wd.getDimensionSize(1);
        numCitingDocs = citing_wd.getDimensionSize(1);
        numDocs = numCitedDocs + numCitingDocs;
        // these caches won't change during sampling
        //Huiping added: number of words in each citing document
        dCiting = (IMatrix1D) citing_wd.marginalize(0); // gamma, lambda
        //Huiping added: number of words in each cited document
        dCited = (IMatrix1D) cited_wd.marginalize(0); // lambda

         //this added every thing together in cited_wd and citing_wd
        int numTokens = (int) (cited_wd.sum() + citing_wd.sum());


        dwtCited = new LineMatrix3D(numTokens); //[d][w][t][val=]//sample t for each (d,w) combination
        dwtscCiting = new LineMatrix5D(numTokens); //[d][w][t][s][c][val=]//sample t,s,c for each (d,w) combination

        // check bibliographies
        int di = 0;
        for (List<Integer> bib : bibliographies) {
            for (int ci : bib) {
                assert (ci < numCites) : "invalid entry " + ci + " in bibliographies.get(" + di + ") " + bib
                        + " found, which is larger than numCites " + numCites;
            }
            di++;
        }

        // init caches
        wtAll = new ArrayMatrix2D(numWords, numTopics); //N_{z,t},N'_{z,t}
        tAll = new ArrayMatrix1D(numTopics);//N_{z},N'_{z}
        tdCited = new ArrayMatrix2D(numTopics, numCitedDocs);//N'_{o,z}
        tcs0Citing = new ArrayMatrix2D(numTopics, numCitedDocs);//N'_{o',z,b}=NN_{o,z} where b=1
        cs0Citing = new ArrayMatrix1D(numCitedDocs); //N'_{o,b} = NN_{o} where b=1
        tds1Citing = new ArrayMatrix2D(numTopics, numCitingDocs);//N_{o,z,b} where b=0
        ds1Citing = new ArrayMatrix1D(numCitingDocs);//N_{o,b} where b=0
        ds0Citing = new ArrayMatrix1D(numCitingDocs);//N_{o,b} where b=1
        dcs0Citing = new ArrayMatrix2D(numCitingDocs, numCitedDocs);//N_{o,o',b} where b=1
        sdCiting = new ArrayMatrix2D(MAX_S, numCitingDocs);//N_{o,b} -> N_{o}, N'_{o}

    }

    /**
     * The size of the bibliography of di. The bibliography is a collection of ci's that are cited by di.
     *
     * @param di a citing document.
     */
    public int getBibSize(int di) {
        return getBibliography(di).size();
    }

    /**
     * The bibliography of di. The bibliography is a collection of ci's that are cited by di.
     *
     * @param di a citing document.
     * @return not null
     */
    public List<Integer> getBibliography(int di) {
        return bibliographies.get(di);
    }

    /**
     * Used in sampling of initial iteration and Gibbs iteration
     * for citing documents
     * @param d
     * @param w
     * @param t
     * @param s
     * @param c
     * @param freq
     * @param position
     */
    public void addCitingDwtsc(int d, int w, int t, int s, int c, int freq, int position) {
        if (position > -1) {
        	//System.out.println(Debugger.getCallerPosition()+"[1]dwtscCiting");
        	double[] valueArray = dwtscCiting.getValsArray(); 
        	//for(int x=0;x<valueArray.length;x++)System.out.print(" "+valueArray[x]);
        	//System.out.println("\n------");
            dwtscCiting.add(d, w, t, s, c, freq, position);
            //System.out.println(Debugger.getCallerPosition()+"[2]dwtscCiting");
            //for(int x=0;x<valueArray.length;x++)System.out.print(" "+valueArray[x]);
            //System.out.println("\n------");
        } else {
            dwtscCiting.add(d, w, t, s, c, freq);
        }

        // add to caches
        switch (s) {
            case 0:
                tcs0Citing.add(t, c, freq);
                cs0Citing.add(c, freq);
                dcs0Citing.add(d, c, freq);
                ds0Citing.add(d, freq);
                break;
            case 1:
                tds1Citing.add(t, d, freq);
                ds1Citing.add(d, freq);
                break;
            default:
                assert false : "Programming Error: s = " + s + " not expected.";

        }
        wtAll.add(w, t, freq);
        tAll.add(t, freq);
        sdCiting.add(s, d, freq);

    }

    /**
     * Used in sampling of initial iteration and Gibbls iteration
     * for cited documents
     * @param d
     * @param w
     * @param t
     * @param freq
     * @param position
     */
    public void addCitedDwt(int d, int w, int t, int freq, int position) {
        if (position > -1) {
            dwtCited.add(d, w, t, freq, position);
        } else {
            dwtCited.add(d, w, t, freq);
        }
        // add to caches
        wtAll.add(w, t, freq); // phi
        tAll.add(t, freq);  // phi
        tdCited.add(t, d, freq);

    }

    public void assignZero() {
        wtAll.assignZero(); // phi
        tdCited.assignZero(); // theta
        tcs0Citing.assignZero(); // theta
        tAll.assignZero();  // phi
        cs0Citing.assignZero(); // theta
        tds1Citing.assignZero(); // psi*
        ds1Citing.assignZero(); // psi*
        ds0Citing.assignZero();
        dcs0Citing.assignZero(); // gamma
        sdCiting.assignZero(); // lambda
    }

    public void trimToSize() {
        wtAll.trimToSize(); // phi
        tdCited.trimToSize(); // theta
        tcs0Citing.trimToSize(); // theta
        tAll.trimToSize();  // phi
        cs0Citing.trimToSize(); // theta
        tds1Citing.trimToSize(); // psi*
        ds1Citing.trimToSize(); // psi*
        ds0Citing.trimToSize();
        dcs0Citing.trimToSize(); // gamma
        sdCiting.trimToSize(); // lambda
    }

    public int getNumTopics() {
        return numTopics;
    }

    public int getNumCites() {
        return numCites;
    }

    public int getNumWords() {
        return numWords;
    }

    public int getNumDocs() {
        return numDocs;
    }

    public int getNumCitedDocs() {
        return numCitedDocs;
    }

    public int getNumCitingDocs() {
        return numCitingDocs;
    }

    public boolean assertCounts() {
        // todo
        return true;
    }

    public double countWTAll(int w, int t) {
        return wtAll.get(w, t) / averageBy;
    }

    public double countTDCited(int t, int d) {
        assert (d != -1) : t + " " + d;
        return tdCited.get(t, d) / averageBy;
    }

    public double countTCs0Citing(int t, int c) {
        return tcs0Citing.get(t, c) / averageBy;
    }

    public double countTAll(int t) {
        return tAll.get(t) / averageBy;
    }

    public double countCs0Citing(int c) {
        return cs0Citing.get(c) / averageBy;
    }

    public double countTDs1Citing(int t, int d) {
        return tds1Citing.get(t, d) / averageBy;
    }

    public double countDs1Citing(int d) {
        return ds1Citing.get(d) / averageBy;
    }

    public double countDs0Citing(int d) {
        return ds0Citing.get(d) / averageBy;
    }

    public double countDCs0Citing(int d, int c) {
        return dcs0Citing.get(d, c) / averageBy;
    }

    public double countDCited(int d) {
        return dCited.get(d) / averageBy;
    }

    public double countDCiting(int d) {
        return dCiting.get(d) / averageBy;
    }

    public double countSDCiting(int s, int d) {
        return sdCiting.get(s, d) / averageBy;
    }

    public void getTCs0CitingNonZeros(INonZeroPerformer2D performer) {
        tcs0Citing.doForAllNonZeros(performer);
    }

    public void getDTCitedNonZeros(INonZeroPerformer2D performer) {
        tdCited.doForAllNonZeros(performer);
    }

    public void doForAllCitingDwtscNonZeros(INonZeroPerformer5D performer) {
        dwtscCiting.doForAllNonZeros(performer);
    }

    /**
     * Used in sample in each iteration for all cited documents
     * @param performer
     */
    public void doForAllCitedDwtNonZeros(INonZeroPerformer3D performer) {
        dwtCited.doForAllNonZeros(performer);
    }

    /**
     * Used in initial sampling for cited documents
     * @param performer
     */
    public void doForAllCitedWdNonZeros(INonZeroPerformer2D performer) {
        wdCited.doForAllNonZeros(performer);
    }

    /**
     * Used in initial sampling for citing documents
     * @param performer
     */
    public void doForAllCitingWdNonZeros(INonZeroPerformer2D performer) {
        wdCiting.doForAllNonZeros(performer);
    }

    public List<List<Integer>> getBibliographies() {
        return bibliographies;
    }


    public String toString() {
        final StringBuffer citedT = new StringBuffer();
        final StringBuffer citedD = new StringBuffer();
        final StringBuffer citedW = new StringBuffer();
        final StringBuffer citingT = new StringBuffer();
        final StringBuffer citingD = new StringBuffer();
        final StringBuffer citingW = new StringBuffer();
        final StringBuffer citingS = new StringBuffer();
        final StringBuffer citingC = new StringBuffer();

        citedD.append("d:");
        citedW.append("w:");
        citedT.append("t:");
        citingD.append("d:");
        citingW.append("w:");
        citingT.append("t:");
        citingS.append("s:");
        citingC.append("c:");

        doForAllCitedDwtNonZeros(new INonZeroPerformer3D() {
            public void iteration(int d, int w, int t, double val, int position) {
                citedD.append(" " + d);
                citedW.append(" " + w);
                citedT.append(" " + t);
            }
        });
        doForAllCitingDwtscNonZeros(new INonZeroPerformer5D() {
            public void iteration(int d, int w, int t, int s, int c, double val, int position) {
                citingD.append(" " + d);
                citingW.append(" " + w);
                citingT.append(" " + t);
                citingS.append(" " + s);
                citingC.append(" " + c);
            }
        });

        final StringBuffer citedCounts = new StringBuffer();
        citedCounts.append("\nwtAll:"+this.wtAll+"\n");
        citedCounts.append("tAll:"+this.tAll+"\n");
        citedCounts.append("tdCited:"+this.tdCited+"\n");
        
        return "\ncited: "+dwtCited.getEndPosition()+"\n" + citedD + "\n" + citedW + "\n" + citedT 
        + "\nciting: " + dwtscCiting.getEndPosition() + "\n"+citingD + "\n" + citingW + "\n" + citingT + "\n" + citingS + "\n" + citingC
        + citedCounts;
    }

    public String toString(final VocDocHasher vocdoc, final BidiMap citedP2B, final BidiMap citingP2B) {
        final StringBuffer citedT = new StringBuffer();
        final StringBuffer citedD = new StringBuffer();
        final StringBuffer citedW = new StringBuffer();
        final StringBuffer citingT = new StringBuffer();
        final StringBuffer citingD = new StringBuffer();
        final StringBuffer citingW = new StringBuffer();
        final StringBuffer citingS = new StringBuffer();
        final StringBuffer citingC = new StringBuffer();

        citedD.append("d:");
        citedW.append("w:");
        citedT.append("t:");
        citingD.append("d:");
        citingW.append("w:");
        citingT.append("t:");
        citingS.append("s:");
        citingC.append("c:");

        doForAllCitedDwtNonZeros(new INonZeroPerformer3D() {
            public void iteration(int d, int w, int t, double val, int position) {
                citedD.append(" " + citedP2B.getKey(d));
                citedW.append(" " + vocdoc.getVocabInverse().get(w));
                citedT.append("  " + t);
            }
        });
        doForAllCitingDwtscNonZeros(new INonZeroPerformer5D() {
            public void iteration(int d, int w, int t, int s, int c, double val, int position) {
                citingD.append(" " + citingP2B.getKey(d));
                citingW.append(" " + vocdoc.getVocabInverse().get(w));
                citingT.append("  " + t);
                citingS.append("  " + s);
                citingC.append(" " + citedP2B.getKey(c));
            }
        });

        return "\ncited\n" + citedD + "\n" + citedW + "\n" + citedT + "\nciting\n" + citingD + "\n" + citingW + "\n" + citingT + "\n" + citingS + "\n" + citingC;
    }

    public boolean collectedEnoughSamples() {
        return tdCited.sum() > 10;
    }

    public void setAverageBy(int averageBy) {
        this.averageBy = averageBy;
    }

    public void clearPositionCitingDwtsc(int position) {
        dwtscCiting.clearPosition(position);
    }

    public void clearPositionCitedDwt(int position) {
        dwtCited.clearPosition(position);
    }
}