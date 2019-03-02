package topicextraction.citetopic.realdata;


import topicextraction.topicinf.datastruct.IDistribution;

import java.io.Serializable;
import java.util.List;

/**
 * todo comment 22.03.2007
 *
 * @version $ID$
 */
public interface ITopicModelWrapper extends Serializable {

    IDistribution<Integer> getThetaByPubId(int pubid);

    List<IDistribution<Integer>> getPhis();

    String getWordForIndex(int wordIndex);
}
