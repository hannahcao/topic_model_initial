package topicextraction.citetopic.sampler;

import java.io.Serializable;

/**
 * Double that knows how to average itself iteratively.
 *
 * @version $ID$
 */
public class Averager implements Serializable {
    private static final long serialVersionUID = -4257841217274580445L;
    private double average = 0.0;
    private int numberOfSummands = 0;

    public Averager() {

    }

    public Averager(double summand) {
        addToAverage(summand);
    }

    /**
     * avg = ((old_avg * old_total_num) + new value)/(new_total_num)
     * @param summand
     */
    public void addToAverage(double summand) {
        average = average * numberOfSummands / (numberOfSummands + 1) + summand / (numberOfSummands + 1);
        numberOfSummands++;
    }

    public double getAverage() {
        return average;
    }

    public int getNumberOfSummands() {
        return numberOfSummands;
    }

    public String toString() {
        return "" + average;
    }

    /**
     * assumes that x summands have been added with "0.0";
     *
     * @param initialSummands x
     * @return this (for convenience)
     */
    public Averager setInitialSummands(int initialSummands) {
        numberOfSummands = initialSummands;
        return this;
    }
}
