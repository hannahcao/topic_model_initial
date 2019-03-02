package topicextraction.citetopic.sampler;

import java.io.Serializable;

/**
 * Double that knows how to sum itself iteratively.
 *
 * @version $ID$
 */
public class Summer implements Serializable {
    private static final long serialVersionUID = -8256195267467588268L;
    private double sum = 0.0;
    private int numberOfSummands = 0;

    public void addToSum(double summand) {
        sum += summand;
        numberOfSummands++;
    }

    public double getSum() {
        return sum;
    }

    public int getNumberOfSummands() {
        return numberOfSummands;
    }

    public String toString() {
        return "" + sum;
    }
}
