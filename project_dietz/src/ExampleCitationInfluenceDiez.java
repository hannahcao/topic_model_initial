

import topicextraction.citetopic.sampler.citinf.CitinfWrapper;
import topicextraction.querydb.IDocument;
//import topicextraction.querydb.Document;
//import topicextraction.querydb.IAuthor;
//import topicextraction.querydb.Relation;
import topicextraction.topicinf.datastruct.IDistribution;
import topicextraction.topicinf.datastruct.DistributionFactory;

import java.util.List;
import java.util.HashMap;
import java.util.Arrays;
import java.util.Date;
import java.util.Collections;
import java.sql.SQLException;

/**
 * Created by IntelliJ IDEA.
 * User: dietz
 * Date: 06.11.2008
 * Time: 23:01:28
 */
public class ExampleCitationInfluenceDiez {
    public static void main(String[] args) {
        System.setProperty("torel.allchains0","0");
        System.setProperty("torel.allchains1","1");
        System.setProperty("torel.rhat","1.01"); // potential scale reduction factor for stopping


        HashMap<Integer, List<Integer>> pubId2CiteIds = new HashMap<Integer, List<Integer>>();
        pubId2CiteIds.put(10, Arrays.asList(1,2,3));
        pubId2CiteIds.put(11, Arrays.asList(3,4,5));
        pubId2CiteIds.put(12, Arrays.asList(6,7,8,9));
        pubId2CiteIds.put(13, Arrays.asList(1,2,8,9));
        HashMap<Integer, IDocument> id2Docs = new HashMap<Integer, IDocument>();
        id2Docs.put(1, new MyDocument(1, "Document number one","American football, known in the United States and Canada simply as football,[1] is a competitive team sport known for mixing strategy with physical play. The objective of the game is to score points by advancing the ball[2] into the opposing team's end zone. The ball can be advanced by carrying it (a running play) or by throwing it to a teammate (a passing play). Points can be scored in a variety of ways, including carrying the ball over the opponent's goal line; catching a pass from beyond that goal line; kicking the ball through the goal posts at the opponent's end zone; and tackling an opposing ballcarrier within his end zone. The winner is the team with the most points when the time expires."));
        id2Docs.put(2, new MyDocument(2, "Document number two","The sport is also played outside the United States. National leagues exist in Germany, Italy, Switzerland, Finland, Sweden, Japan, Mexico, Israel, Spain, Austria and several Pacific Island nations."));
        id2Docs.put(3, new MyDocument(3, "Document number three","The National Football League, the largest professional American football league in the world, ran a developmental league in Europe from 1991�1992 and then from 1995�2006."));
        id2Docs.put(4, new MyDocument(4, "Document number four","American football is closely related to Canadian football, but with significant differences. Both sports originated from rugby football."));
        id2Docs.put(5, new MyDocument(5, "Document number five","The history of American football can be traced to early versions of rugby football and soccer. Both games have their origins in varieties of football played in the United Kingdom in the mid-19th century, in which a ball is kicked at a goal and/or run over a line."));
        id2Docs.put(6, new MyDocument(6, "Document number six","Also like soccer, American football has twenty two players on the field of play. Furthermore, some player position references from soccer are used, such as the term \"halfback\" and \"fullback\"."));
        id2Docs.put(7, new MyDocument(7, "Document number seven","American football resulted from several major divergences from rugby football, most notably the rule changes instituted by Walter Camp, considered the \"Father of American Football\"."));
        id2Docs.put(8, new MyDocument(8, "Document number eight","Among these important changes were the introduction of the line of scrimmage and of down-and-distance rules. In the late 19th and early 20th centuries, gameplay developments by college coaches such as Eddie Cochems, Amos Alonzo Stagg, Knute Rockne, and Glenn \"Pop\" Warner helped take advantage of the newly introduced forward pass."));
        id2Docs.put(9, new MyDocument(9, "Document number nine","The popularity of collegiate football grew as it became the dominant version of the sport for the first half of the twentieth century."));
        id2Docs.put(10, new MyDocument(10, "Document number ten","Bowl games, a college football tradition, attracted a national audience for collegiate teams. Bolstered by fierce rivalries, college football still holds widespread appeal in the US"));
        id2Docs.put(11, new MyDocument(11, "Document number eleven","The origin of professional football can be traced back to 1892, with William \"Pudge\" Heffelfinger's $500 contract to play in a game for the Allegheny Athletic Association against the Pittsburgh Athletic Club."));
        id2Docs.put(12, new MyDocument(12, "Document number twelve","In 1920 the American Professional Football Association was formed. The first game was played in Dayton, Ohio on October 3rd, 1920 with the host Triangles defeating the Columbus Panhandles 14-0."));
        id2Docs.put(13, new MyDocument(13, "Document number thirteen","The league changed its name to the National Football League (NFL) two years later, and eventually became the major league of American football. Initially a sport of Midwestern, industrial towns in the United States, professional football eventually became a national phenomenon."));

        int tnum = 10; 	//noted by Huiping: number of topics
        int cnum=30;	//noted by Huiping: numCites, the maximum number of articles in the reference list of one object 

        double alphaPhi=0.01;
        double alphaPsi=0.1;
        double alphaTheta=0.1;

        double alphaLambdaInherit=0.5;
        double alphaLambdaInnov=0.5;
        double alphaGamma=1.0;
        int maximalNumIterations=1000;
        
        //Huiping added the following printf to show the information before calling the wrapper
        System.out.println("pubId2CiteIds="+pubId2CiteIds);
        System.out.println("id2Docs size: "+id2Docs.size());
        System.out.println("tnum="+tnum);
        System.out.println("cnum="+cnum); 
        System.out.println("alphaPhi="+alphaPhi);
        System.out.println("alphaPsi="+alphaPsi);
        System.out.println("alphaTheta="+alphaTheta);
        System.out.println("alphaLambdaInherit="+alphaLambdaInherit);
        System.out.println("alphaLambdaInnov="+alphaLambdaInnov);
        System.out.println("alphaGamma="+alphaGamma);
        //System.exit(0);
        // end of adding 
        
        
        CitinfWrapper sampwrap = new CitinfWrapper(pubId2CiteIds, id2Docs, tnum, cnum, alphaPhi, alphaPsi,
                alphaTheta, alphaLambdaInherit, alphaLambdaInnov, alphaGamma);
        System.out.println("Finish CitinfWrapper ... \n");
        sampwrap.doGibbs(maximalNumIterations);

        System.out.println("citation mixture id 10 "+sampwrap.getGammaForPubId(10));
        System.out.println("citation mixture id 11 "+sampwrap.getGammaForPubId(11));
        System.out.println("citation mixture id 12 "+sampwrap.getGammaForPubId(12));
        System.out.println("citation mixture id 13 "+sampwrap.getGammaForPubId(13));

        System.out.println("topic mixture for id 1 "+sampwrap.getThetaByPubId(1));
        System.out.println("topic mixture for id 2 "+sampwrap.getThetaByPubId(2));
        System.out.println("topic mixture for id 3 "+sampwrap.getThetaByPubId(3));
        System.out.println("topic mixture for id 10 "+sampwrap.getThetaByPubId(10));

        System.out.println("degree of innovation for 10 "+sampwrap.getLambdaDistrForPubId(10));

        IDistribution<Integer> wordsForTopic1Number = sampwrap.getPhis().get(1);
        System.out.println("word id distribution for topic 1"+ wordsForTopic1Number);
        IDistribution<String> wordsForTopic1Words = DistributionFactory.createDistribution();
        index2WordDistribution(wordsForTopic1Number, sampwrap, wordsForTopic1Words);
        System.out.println("word distribution for topic 1 = " + wordsForTopic1Words);

    }

    private static void index2WordDistribution(IDistribution<Integer> wordsForTopic1Number, CitinfWrapper sampwrap, IDistribution<String> wordsForTopic1Words) {
        for(int wordIndex:wordsForTopic1Number.keySet()){
            String word = sampwrap.getWordForIndex(wordIndex);
            double prob = wordsForTopic1Number.get(wordIndex);
            wordsForTopic1Words.put(word, prob);
        }
        wordsForTopic1Words.normalize();
    }

    private static class MyDocument implements IDocument{
        private final int id;
        private final String title;
        private final String description;

        public MyDocument(int id, String title, String description){

            this.id = id;
            this.title = title;
            this.description = description;
        }

        public int getId() {
            return id;
        }

        public Date getDate() {
            return null;
        }

        public String getTitle() {
            return title;
        }

        public String getDescription() {
            return description;
        }

        public String getContributor() throws SQLException {
            return "";
        }

        public String getFormat() throws SQLException {
            return "";
        }

        public String getSource() throws SQLException {
            return "";
        }

        public String getLanguage() throws SQLException {
            return "";
        }

        //Huiping commented out the following three functions
        //Feb. 14, 2012
        //public List<IAuthor> getAuthors() {
        //	return Collections.emptyList();
        //}

        //public List<Relation> getRelations() throws SQLException {
        //   return Collections.emptyList();
        //}

        //public List<Relation> getRelationsByType(int type) throws SQLException {
        //    return Collections.emptyList();
        //}

        public String getText() {
            return getTitle()+" "+getDescription();
        }
    }

}
