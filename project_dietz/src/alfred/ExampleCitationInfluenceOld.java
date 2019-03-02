package alfred;

import topicextraction.citetopic.sampler.citinf.CitinfWrapper;
import topicextraction.querydb.IDocument;
//import topicextraction.querydb.Document;
//import topicextraction.querydb.IAuthor;
//import topicextraction.querydb.Relation;
import topicextraction.topicinf.datastruct.IDistribution;
import topicextraction.topicinf.datastruct.DistributionFactory;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.HashMap;
import java.util.Arrays;
import java.util.Date;
import java.util.Collections;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;

import javax.swing.JFileChooser;

/**
 * Created by IntelliJ IDEA.
 * User: dietz
 * Date: 06.11.2008
 * Time: 23:01:28
 * 
 * Usage: 
 * java -cp infdetection.jar nmsu.ExampleCitationInfluence data/relations1.txt
 */
public class ExampleCitationInfluenceOld {
    public static void main(String[] args) {
    	String relationsFile = "";
    	int tnum = 10;	// the number of topics (default 10)
    	String usage = "Usage: ./ExampleCItationInfluence <1.relationship file> [<2. number of topics>]";
    	
    	//argument process
    	if(args.length ==1 ){
        	relationsFile = args[0];
        	System.out.println(relationsFile);
        }else if (args.length==2){
        	relationsFile = args[0];
        	tnum = Integer.parseInt(args[1]);
        }else{
        	System.out.println(usage);
        	return;
        }
    	System.out.println("citation file="+relationsFile+", topic number="+tnum);
    	
    	//Setting multi-threading chains
    	//TODO: why do they use two threads???
    	System.setProperty("torel.allchains0","0");
        System.setProperty("torel.allchains1","1");
        
        //potential scale reduction factor for stopping
        //controlling convergence: 
        //when the rHat value after iterations is smaller than this value
        //it is evaluated as convergence; otherwise, not
        System.setProperty("torel.rhat","1.01"); 
        

        //read map from publication id: citation ids, and publication id: documents
        HashMap<Integer, List<Integer>> pubId2CiteIds = new HashMap<Integer, List<Integer>>();
		HashMap<Integer, IDocument> id2Docs = new HashMap<Integer, IDocument>();
		loadfile(relationsFile, pubId2CiteIds, id2Docs);
		
        
        int cnum=30;

        //hyperparameters 
        double alphaPhi=0.01;
        double alphaPsi=0.1;
        double alphaTheta=0.1;
        double alphaLambdaInherit=0.5;
        double alphaLambdaInnov=0.5;
        double alphaGamma=1.0;
        
        
        int maximalNumIterations=10000;

        //Huiping added the following printf to show the information before calling the wrapper
//        System.out.println("pubId2CiteIds="+pubId2CiteIds);
//        System.out.println("id2Docs size="+id2Docs.size());
//        System.out.println("tnum="+tnum);
//        System.out.println("cnum="+cnum);
//        System.out.println("alphaPhi="+alphaPhi);
//        System.out.println("alphaPsi="+alphaPsi);
//        System.out.println("alphaTheta="+alphaTheta);
//        System.out.println("alphaLambdaInherit="+alphaLambdaInherit);
//        System.out.println("alphaLambdaInnov="+alphaLambdaInnov);
//        System.out.println("alphaGamma="+alphaGamma);
        // end of adding 
        //System.exit(0);
        
        CitinfWrapper sampwrap = new CitinfWrapper(pubId2CiteIds, id2Docs, tnum, cnum, alphaPhi, alphaPsi,
                alphaTheta, alphaLambdaInherit, alphaLambdaInnov, alphaGamma);
        sampwrap.doGibbs(maximalNumIterations);
        

        //System.out.println("citation mixture id 10 "+sampwrap.getGammaForPubId(10));
        /*System.out.println("citation mixture id 11 "+sampwrap.getGammaForPubId(11));
        System.out.println("citation mixture id 12 "+sampwrap.getGammaForPubId(12));
        System.out.println("citation mixture id 13 "+sampwrap.getGammaForPubId(13));

        System.out.println("topic mixture for id 1 "+sampwrap.getThetaByPubId(1));
        System.out.println("topic mixture for id 2 "+sampwrap.getThetaByPubId(2));
        System.out.println("topic mixture for id 3 "+sampwrap.getThetaByPubId(3));
        System.out.println("topic mixture for id 10 "+sampwrap.getThetaByPubId(10));

        System.out.println("degree of innovation for 10 "+sampwrap.getLambdaDistrForPubId(10));
		*/
        
        //Get the posterior of citation mixture, topic mixture, degree innovation, word distribution, etc.
        System.out.println("citation mixture id 425378 "+sampwrap.getGammaForPubId(425378));
        System.out.println("topic mixture for id 500158 "+sampwrap.getThetaByPubId(500158));
        System.out.println("degree of innovation for 425378 "+sampwrap.getLambdaDistrForPubId(425378));
        IDistribution<Integer> wordsForTopic1Number = sampwrap.getPhis().get(1);
        System.out.println("word id distribution for topic 1"+ wordsForTopic1Number);
        IDistribution<String> wordsForTopic1Words = DistributionFactory.createDistribution();
        index2WordDistribution(wordsForTopic1Number, sampwrap, wordsForTopic1Words);
        System.out.println("word distribution for topic 1 = " + wordsForTopic1Words);

    }
    
    private static void loadfile(String str, HashMap<Integer, List<Integer>> pubId2CiteIds, HashMap<Integer, IDocument> id2Docs){
    	
		File f;
        FileReader fr;
        BufferedReader br;
        HashMap<Integer, ArrayList<Integer>> pubId2CiteIds_AL = new HashMap<Integer, ArrayList<Integer>>();
        ArrayList<String> documentsNeeded = new ArrayList<String>();
    	try{

        	f = new File(str);
        	fr = new FileReader(f);
    		br = new BufferedReader(fr);
    		String line;
    		int lineNum = 1;
    		while ((line = br.readLine()) != null){
    			//if line starts with sharp # or line is empty
    			if(line.startsWith("#") || line.length() == 0){
    				
    			}else{
    				int endIndex = line.indexOf(" ");
    				String s1 = line.substring(0, endIndex);
    				String s2 = line.substring(endIndex+1, line.length());
    				Integer citer = new Integer(s1);
    				Integer citee = new Integer(s2);
    				//check to see if pubId2CiteIds already contains the citer
    				if(pubId2CiteIds_AL.containsKey(citer)){
    					//if pubId2CiteIds contains the citer, check to see if the list already has the citee. If not add citee to list.
    					if(!pubId2CiteIds_AL.get(citer).contains(citee)){
    						pubId2CiteIds_AL.get(citer).add(citee);
    					}
    				}//end if
    				//if citer is not inside pubId2CiteIds add citer and citee
    				else{
    					ArrayList<Integer> al = new ArrayList<Integer>();
    					al.add(citee);
    					pubId2CiteIds_AL.put(citer, al);
    					
    				}
    				//check to see if citer and citee numbers need to be added to documentsNeeded
    				if(!documentsNeeded.contains(citer.toString())){
    					documentsNeeded.add(citer.toString());
    				}
    				if(!documentsNeeded.contains(citee.toString())){
    					documentsNeeded.add(citee.toString());
    				}
    				
    				//System.out.println("<"+s1+">" +" <" + s2 +">");
    			}
    			lineNum++;
    		}
        	
        }//end try brackets
		catch (FileNotFoundException e) {
			
			e.printStackTrace();
		}//end catch 
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}//end catch 
		
		for(String s : documentsNeeded){
            try{
            	f = new File("./data/" + s + ".txt");
            	fr = new FileReader(f);
        		br = new BufferedReader(fr);
        		String line;
        		int lineNum = 0;
        		//read lines of file one by one. If the 4th (3rd from 0) line, enter this line into id2Docs. This line is the abstract data for the file. 
        		while ((line = br.readLine()) != null){
        			if(lineNum==3){
	        			//put abstract into id2Docs
        				//id2Docs.put(i, new MyDocument(i, s1.toString(),line));
        				id2Docs.put(new Integer(s), new MyDocument(new Integer(s), "Document number "+ s, line));
        			}
        			lineNum++;
        		}
            }//end try brackets
    		catch (FileNotFoundException e) {
    			
    			e.printStackTrace();
    		}//end catch 
    		catch (IOException e) {
    			// TODO Auto-generated catch block
    			e.printStackTrace();
    		}//end catch 
        	
        }
		for(Integer x : pubId2CiteIds_AL.keySet()){
        	List<Integer> listconvert = pubId2CiteIds_AL.get(x); 
        	pubId2CiteIds.put(x, listconvert);
        }
		
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
        
        
        private class CitationNode{
        	private String name;
        	private List<String> citedPublications; 
        	
        	public CitationNode(String n , List<String> s){
        		name = n;
        		citedPublications = s;
        		
        	}
        	public String getName(){
        		return name;
        	}
        	public List<String> getCitations(){
        		return citedPublications;
        	}
        	public void setName(String n){
        		name = n;
        	}
        	public void setCitations(List<String> s){
        		citedPublications = s;
        	}
        	public void addCitation(String citation){
        		citedPublications.add(citation);
        	}
        	
        	
        }
        
        
        
        
    }

}
