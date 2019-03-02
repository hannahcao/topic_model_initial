package cao;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class DataRaw {

	HashMap<Integer, List<Integer>> pubId2CiteIds;
	HashMap<Integer, Doc> id2Docs;

	public DataRaw(){
		pubId2CiteIds = new HashMap<Integer, List<Integer>>(); //object id: object id list cited 
		id2Docs = new HashMap<Integer, Doc>();//object id: object document
	}
	
	/**
	 * Manually set a set of data
	 * 
	 * @param pubId2CiteIds: a map from publication-id to the paper-ids that are cited by publication-id 
	 * @param id2Docs: a map from the publication-id to the text of the documents
	 */
	public void getManualData()
	{
		pubId2CiteIds.put(10, Arrays.asList(1,2,3)); //10 cites 1, 2, 3
        pubId2CiteIds.put(11, Arrays.asList(3,4,5)); //11 cites 3, 4, 5
        pubId2CiteIds.put(12, Arrays.asList(6,7,8,9));
        pubId2CiteIds.put(13, Arrays.asList(1,2,8,9));
        
        id2Docs.put(1, new Doc(1, "Document number one","American football, known in the United States and Canada simply as football,[1] is a competitive team sport known for mixing strategy with physical play. The objective of the game is to score points by advancing the ball[2] into the opposing team's end zone. The ball can be advanced by carrying it (a running play) or by throwing it to a teammate (a passing play). Points can be scored in a variety of ways, including carrying the ball over the opponent's goal line; catching a pass from beyond that goal line; kicking the ball through the goal posts at the opponent's end zone; and tackling an opposing ballcarrier within his end zone. The winner is the team with the most points when the time expires."));
        id2Docs.put(2, new Doc(2, "Document number two","The sport is also played outside the United States. National leagues exist in Germany, Italy, Switzerland, Finland, Sweden, Japan, Mexico, Israel, Spain, Austria and several Pacific Island nations."));
        id2Docs.put(3, new Doc(3, "Document number three","The National Football League, the largest professional American football league in the world, ran a developmental league in Europe from 1991�1992 and then from 1995�2006."));
        id2Docs.put(4, new Doc(4, "Document number four","American football is closely related to Canadian football, but with significant differences. Both sports originated from rugby football."));
        id2Docs.put(5, new Doc(5, "Document number five","The history of American football can be traced to early versions of rugby football and soccer. Both games have their origins in varieties of football played in the United Kingdom in the mid-19th century, in which a ball is kicked at a goal and/or run over a line."));
        id2Docs.put(6, new Doc(6, "Document number six","Also like soccer, American football has twenty two players on the field of play. Furthermore, some player position references from soccer are used, such as the term \"halfback\" and \"fullback\"."));
        id2Docs.put(7, new Doc(7, "Document number seven","American football resulted from several major divergences from rugby football, most notably the rule changes instituted by Walter Camp, considered the \"Father of American Football\"."));
        id2Docs.put(8, new Doc(8, "Document number eight","Among these important changes were the introduction of the line of scrimmage and of down-and-distance rules. In the late 19th and early 20th centuries, gameplay developments by college coaches such as Eddie Cochems, Amos Alonzo Stagg, Knute Rockne, and Glenn \"Pop\" Warner helped take advantage of the newly introduced forward pass."));
        id2Docs.put(9, new Doc(9, "Document number nine","The popularity of collegiate football grew as it became the dominant version of the sport for the first half of the twentieth century."));
        id2Docs.put(10, new Doc(10, "Document number ten","Bowl games, a college football tradition, attracted a national audience for collegiate teams. Bolstered by fierce rivalries, college football still holds widespread appeal in the US"));
        id2Docs.put(11, new Doc(11, "Document number eleven","The origin of professional football can be traced back to 1892, with William \"Pudge\" Heffelfinger's $500 contract to play in a game for the Allegheny Athletic Association against the Pittsburgh Athletic Club."));
        id2Docs.put(12, new Doc(12, "Document number twelve","In 1920 the American Professional Football Association was formed. The first game was played in Dayton, Ohio on October 3rd, 1920 with the host Triangles defeating the Columbus Panhandles 14-0."));
        id2Docs.put(13, new Doc(13, "Document number thirteen","The league changed its name to the National Football League (NFL) two years later, and eventually became the major league of American football. Initially a sport of Midwestern, industrial towns in the United States, professional football eventually became a national phenomenon."));

	}
}
