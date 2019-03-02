package topicextraction.topicinf;

import java.io.Serializable;

public class RandomTopicInitializer implements ITopicInitializer, Serializable {
    /**
     * Calculates the initial topic for a token
     *
     * @param d         document index
     * @param w         word index
     * @param token     number of occurance of this (d,w) token
     * @param numTopics number of topics from which one has to be returned
     * @return 0 &lt;= result &lt; numTopics
     */
    public int initialTopic(int d, int w, int token, int numTopics) {
        return (int) Math.floor(Math.random() * numTopics);
    }
}
