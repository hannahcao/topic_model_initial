package topicextraction.citetopic.sampler;

import java.io.Serializable;
import java.util.List;

/**
 * todo comment 09.11.2006
 *
 * @version $ID$
 */
public class RandomCiteInitializer implements ICiteInitializer, Serializable {
    private static final long serialVersionUID = 2447129317414115907L;

    public int initialCite(int d, int w, int freq, int numCites) {
        return (int) Math.floor(Math.random() * numCites);
    }

    public int initialCite(int d, int w, int freq, List<Integer> bibliography) {
        assert (!bibliography.isEmpty()) : "d=" + d + " w=" + w;
        int index = (int) Math.floor(Math.random() * bibliography.size());
        return bibliography.get(index);
    }
}
