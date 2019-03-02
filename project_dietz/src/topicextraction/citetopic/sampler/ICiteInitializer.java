package topicextraction.citetopic.sampler;

import java.io.Serializable;
import java.util.List;

public interface ICiteInitializer extends Serializable {
    /**
     * Calculates the initial cite for a occurrence
     *
     * @param d          document index
     * @param w          word index
     * @param occurrence number of occurrence of this (d,w) occurrence
     * @param numCites   number of cites from which one has to be returned
     * @return 0 &lt;= result &lt; numTopics
     */
    int initialCite(int d, int w, int occurrence, int numCites);

    /**
     * Calculated the initial cite for a token, given a list of potential cites to choose from
     *
     * @param d            document index
     * @param w            word index
     * @param occurence    this is the i'th occurence of the same token
     * @param bibliography list of allowed cites to choose from
     * @return one entry of bibliography
     */
    int initialCite(int d, int w, int occurence, List<Integer> bibliography);
}
