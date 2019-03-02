package cao;

import java.io.Serializable;
import java.util.Date;

public class ResultStatisticRecord implements Serializable {
	private static final long serialVersionUID = 1900491202369803233L;
	
	private int iteration;
    private double rHat;
    private long millisecoundsElapsed;
    private Date timestamp;

    public ResultStatisticRecord(int iteration, double rHat, long millisecoundsElapsed, Date timestamp) {
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

