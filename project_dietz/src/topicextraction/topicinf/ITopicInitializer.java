package topicextraction.topicinf;

public interface ITopicInitializer {
    /**
     * Calculates the initial topic for a occurrence
     *
     * @param d          document index
     * @param w          word index
     * @param occurrence this is the i'th occurence of the same token
     * @param numTopics  number of topics from which one has to be returned
     * @return 0 &lt;= result &lt; numTopics
     */
    int initialTopic(int d, int w, int occurrence, int numTopics);
}
