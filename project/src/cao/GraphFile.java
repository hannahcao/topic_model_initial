package cao;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import java.util.List;
import java.util.Set;
import java.util.HashSet;


public class GraphFile {
	
	public static void loadFile(String paperpath,String graphEdgeFile,DataRaw dataraw){
		 Set<Integer> docidset = readEdge(graphEdgeFile, dataraw);
		 readDoc(paperpath,docidset,dataraw);
	}
	
    private static Set<Integer> readEdge(String graphEdgeFile,DataRaw dataraw){
    	
		File f;
        FileReader fr;
        BufferedReader br;
        //HashMap<Integer, ArrayList<Integer>> pubId2CiteIds_AL = new HashMap<Integer, ArrayList<Integer>>();
        Set<Integer> docidset = new HashSet<Integer>();
        //ArrayList<String> documentsNeeded = new ArrayList<String>();
        
    	try{
        	f = new File(graphEdgeFile);
        	fr = new FileReader(f);
        	br = new BufferedReader(fr);
    		
        	String line;
    		while ((line = br.readLine()) != null){
    			if(line.startsWith("#") || line.trim().length() == 0){ //if line starts with sharp # or line is empty
    				continue;
    			}else{
    				int endIndex = line.indexOf(" ");
    				String s1 = line.substring(0, endIndex);
    				String s2 = line.substring(endIndex+1, line.length());
    				Integer citer = new Integer(s1);
    				Integer citee = new Integer(s2);
    				
    				
    				List<Integer> citeids = dataraw.pubId2CiteIds.get(citer);
    				if(citeids==null){
    					citeids = new ArrayList<Integer>();
    					dataraw.pubId2CiteIds.put(citer, citeids);
    				}
    				
    				//citee is not in the list yet, add this paper in the citee list
    				if(!citeids.contains(citee)){
    					citeids.add(citee);
					}
    				
    				docidset.add(citer);
    				docidset.add(citee);
    			}	
    		}
    		br.close();
    		fr.close();
        }catch (FileNotFoundException e) {
			e.printStackTrace();
		}catch (IOException e) {
			e.printStackTrace();
		}
    	return docidset;
    	
    }
    
    /**
     * read the documents
     * @param paperpath
     * @param docidset
     * @param dataraw
     */
    private static void readDoc(String paperpath, Set<Integer> docidset, DataRaw dataraw)
    {	
    	File f;
        FileReader fr;
        BufferedReader br;
        
		for(Integer docid : docidset){
            try{
            	f = new File(paperpath + docid + ".txt");
            	fr = new FileReader(f);
        		br = new BufferedReader(fr);
        		String line;
        		 
        		while ((line = br.readLine()) != null){
        			if(line.startsWith("#") || line.trim().length() == 0){ //if line starts with sharp # or line is empty
        				continue;
        			}else{
	        			dataraw.id2Docs.put(docid, new Doc(docid, "Document number "+ docid, line));
        			}
        		}
        		br.close();
        		fr.close();
            }catch (FileNotFoundException e) {
    			e.printStackTrace();
    		}catch (IOException e) {
    			e.printStackTrace();
    		} 
        }	
    }
}

