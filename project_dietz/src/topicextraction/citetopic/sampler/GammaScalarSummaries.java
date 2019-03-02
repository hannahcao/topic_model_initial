package topicextraction.citetopic.sampler;

import topicextraction.topicinf.datastruct.DistributionFactory;
import topicextraction.topicinf.datastruct.IDistribution;

import java.util.ArrayList;
import java.util.List;

/**
 * Scalar Summaries measuring the strength of influence associated to citations.
 *
 * @version $ID$
 */
public class GammaScalarSummaries extends ArrayList<Double> {
    private static final long serialVersionUID = 6085411148797657551L;
    private int numDocs;
    private List<List<Integer>> bibliographies;
    private List<Integer> docOffsets = new ArrayList<Integer>();
    private List<IDistribution<Integer>> distributions = new ArrayList<IDistribution<Integer>>();

    public GammaScalarSummaries(int numDocs, List<List<Integer>> bibliographies) {
        this.numDocs = numDocs;
        this.bibliographies = bibliographies;
        assert (bibliographies.size() == numDocs);

        int count = 0;
        for (List<Integer> bib : bibliographies) {
            docOffsets.add(count);
            for (int c : bib) {
                super.add(0.0);
                count++;
            }
        }

        for (int d = 0; d < numDocs; d++) {
            distributions.add(DistributionFactory.<Integer>createDistribution());
        }
    }

    /**
     * Huiping commented on Aug. 13, 2012
     * 
     * Let the number of citing articles be m
     * distributions has m elements: distributions[0],...,distributions[m-1]
     * Each distributions[i] consists of 
     * (1) a map: cited_doc_id:frequency of using this cited document id in generating article <i>i</i>'s information
     * (2) Total number of frequencies in using cited documents "sumCache"  
     * E.g., [[(2:1.0) ], [(3:2.0) ], [(8:2.0) (5:1.0) ], [(1:3.0) (0:1.0) ]]
     * It means there are 4 citing articles:
     * [(2:1.0) ] Citing article no. 0: cites cited article no. 2 once (frequency 1.0)
     * [(3:2.0) ] Citing article no. 1: cites cited article no.  3 twice (frequency 2.0)
     * [(8:2.0) (5:1.0) ] Citing article no. 2: cites cited article no.  8 twice and cites cited article no.  5 once
     * [(1:3.0) (0:1.0) ] Citing article no. 4: cites cited article no.  1 three times and cites cited article no. 0 once
     * Note that citing article no. and cited article no. are in different space. 
     * I.e., even the same number represent different articles.  
     * 
     * @param d
     * @param c
     * @param aspect
     */
    public void addScalarDistribution(int d, int c, double aspect) {
        distributions.get(d).add(c, aspect); //add is implemented in MediumFastDistribution
    }

    /**
     * Set the distribution summary
     * d--> c: probability (normalized to the range of [0,1])
     */
    public void distrToSummary() {
        for (int d = 0; d < distributions.size(); d++) {
            IDistribution<Integer> distr = distributions.get(d); //get all the cited documents:frequency list
            if (distr.sum() > 0.0) {//get the total citation frequency
                distr.normalize(); //for each cited document, calculate its percentage of citation, i.e., frequency of one doc/sum
            }
            for (int c : distr.keySet()) {
                setScalarSummary(d, c, distr.get(c));
            }
        }
    }

    /**
     * sets a scalar summary
     *
     * @param d       bugsid of citing doc
     * @param c       bugsid of cited doc
     * @param summary new value
     */
    public void setScalarSummary(int d, int c, double summary) {
        int index = convertToIndex(d, c); //get the index of d cites bibliograph c in a linea list
        super.set(index, summary); //put the probability there
    }

    public void addScalarSummary(int d, int c, double summary) {
        int index = convertToIndex(d, c);
        Double oldSumm = super.get(index);
        super.set(index, summary + oldSumm);
    }

    /**
     * gets a scalar summary
     *
     * @param d bugsid of citing doc
     * @param c bugsid of cited doc
     * @return the summary value
     */
    public double getScalarSummary(int d, int c) {
        int index = convertToIndex(d, c);
        return super.get(index);
    }

    /**
     * converts semantic coordinates to list index
     *
     * @param d bugsid of citing doc
     * @param c bugsid of cited doc
     * @return corresponding index in list
     */
    protected int convertToIndex(int d, int c) {
        assert (d < numDocs);
        assert (bibliographies.get(d).contains(c)) : "bibliographies do not contain mapping d=" + d + " c=" + c + "  key for d=" + bibliographies.get(d);

        int docOffset = docOffsets.get(d);
        List<Integer> bib = bibliographies.get(d);
        int citeOffset = bib.indexOf(c);
        assert (citeOffset > -1) : "d=" + d + " c=" + c + " bibliography=" + bib + " docOffset=" + docOffset;
        int listIndex = docOffset + citeOffset;
        assert (listIndex < super.size()) : "listindex=" + listIndex + " exceeds list size (" + super.size() + ")";
        return listIndex;
    }

}
