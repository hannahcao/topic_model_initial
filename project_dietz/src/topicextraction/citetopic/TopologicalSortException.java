package topicextraction.citetopic;

import java.util.List;
import java.util.Set;

/**
 * Exception for {@link de.hu_berlin.wm.torel.topicextraction.citetopic.CiteTopicUtil#topologicalSort(java.util.List,int,int)}.
 */
public class TopologicalSortException extends Throwable {
    private final List<Integer> resultSoFar;
    private final Set<Integer> remainingIds;
    private final int[] remainingIndegree;

    public TopologicalSortException(List<Integer> resultSoFar, Set<Integer> remainingIds, int[] remainingIndegree) {
        this.resultSoFar = resultSoFar;
        this.remainingIds = remainingIds;
        this.remainingIndegree = remainingIndegree;
    }


    public List<Integer> getResultSoFar() {
        return resultSoFar;
    }

    public Set<Integer> getRemainingIds() {
        return remainingIds;
    }

    public int[] getRemainingIndegree() {
        return remainingIndegree;
    }


    public String toString() {
        return "TopologicalSortException. Sorted " + resultSoFar + ". The following Ids remained: " + remainingIds;
    }
}
