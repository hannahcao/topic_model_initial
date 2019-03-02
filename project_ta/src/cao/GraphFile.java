package cao;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.StringTokenizer;
import java.util.TreeMap;

import org.json.JSONArray;
import org.json.JSONObject;


public class GraphFile {
	
	/**
	 * load data to dataraw
	 * @param paperpath      paper path
	 * @param graphEdgeFile  citation relation path
	 * @param dataraw    	 dataraw object
	 */
	public static void loadFile(String paperpath, String graphEdgeFile, String aspectFile, DataRaw dataraw){
		System.out.println(Debugger.getCallerPosition()+" read edge ");
		Set<Integer> docidset = readEdge(graphEdgeFile, dataraw);

		System.out.println(Debugger.getCallerPosition()+" read aspect ");
		Map<Integer, String> aid2aspectMap = readAspectMap(aspectFile, dataraw);

		System.out.println(Debugger.getCallerPosition()+" read doc");
		readDoc(paperpath, docidset, aid2aspectMap, dataraw);
		
	}
	/**
	 * load citation relation
	 * @param graphEdgeFile
	 * @param dataraw
	 * @return
	 */
    public static Set<Integer> readEdge(String graphEdgeFile, DataRaw dataraw){
    	File f;
        FileReader fr;
        BufferedReader br;
        //HashMap<Integer, ArrayList<Integer>> pubId2CiteIds_AL = new HashMap<Integer, ArrayList<Integer>>();
        //document ID set
        Set<Integer> docidset = new HashSet<Integer>();
        //ArrayList<String> documentsNeeded = new ArrayList<String>();
        
    	try{
    		Set<Integer> oSet = new HashSet<Integer>();
    		Set<Integer> oprimeSet = new HashSet<Integer>();
    		
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
    				Integer citer = new Integer(s1.trim());
    				Integer citee = new Integer(s2.trim());
    				
    				oSet.add(citer);
    				oprimeSet.add(citee);
    				
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
    		
    		Constant.oNum = oSet.size();
    		Constant.oprimeNum = oprimeSet.size();
    		
        }catch (FileNotFoundException e) {
			e.printStackTrace();
		}catch (IOException e) {
			e.printStackTrace();
		}
    	return docidset;
    	
    }
    /**
	 * load aspect2aid map
	 * @return
	 */
	public static Map<Integer, String> readAspectMap(String aspectFile, DataRaw dataraw){
		File aFile = new File(aspectFile);
		FileReader fr;
		BufferedReader br;
		Map<Integer, String> aid2aspect = new TreeMap<Integer, String>();
		int id = 0;
		
		try {
			fr = new FileReader(aFile);
			br = new BufferedReader(fr);
			
			String line = br.readLine();
			while(line!=null){
				aid2aspect.put(id, line.trim());
				dataraw.id2Aspect.put(id++, line.trim());
				line = br.readLine();
			}
			
			br.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Constant.aspectNum = id;
		return aid2aspect;
	}
	
    /**
     * read the documents
     * need change for xml format data
     * need change for json format data
     * @param paperpath
     * @param docidset
     * @param aid2aspect
     * @param dataraw
     */
    private static void readDoc(String paperpath, Set<Integer> docidset, Map<Integer, String> aid2aspect, DataRaw dataraw){	
    	for(Integer docid : docidset){
			JSONObject obj = readTweetJson(paperpath, docid);
			JSONArray arr = obj.getJSONArray("user_timeline");
			//text under corresponding aspect
			Map<String, String> aspect2Text = new HashMap<String, String>();
			
			//TODO change here.!!!
			for(int i=0; i<arr.length() && i<100; i++){
				JSONObject tObj = arr.getJSONObject(i);
				String tweet = tObj.getString("tweet");
				String aspect = tObj.getString("aspect");
				
				String text = aspect2Text.get(aspect);
				if(text == null)
					aspect2Text.put(aspect, tweet);
				else
					aspect2Text.put(aspect, text+" "+tweet);
			}
			
			List<String> textList = new ArrayList<String>();
			//since aid2aspect is A TreeMap, so this set is ordered by aspect id.
			for(Integer aid : aid2aspect.keySet()){
				String aspect = aid2aspect.get(aid);
				String text = aspect2Text.get(aspect);
				if(text==null)
					text = "";
				textList.add(text);
			}
			
//			System.out.println(Debugger.getCallerPosition()+" "+obj);
			
			dataraw.id2Docs.put(docid, new Doc(docid, "Document number "+ docid, textList));
        }	
    }
    
    public static JSONObject readTweetJson(String paperpath, int id){
    	JSONObject obj = null;
    	FileReader fr;
		BufferedReader br;
		File target = new File(paperpath+id+".json");
		try {
			fr = new FileReader(target);
			br = new BufferedReader(fr);
			String line = br.readLine();
			String jsonString = "";
			while(line!=null){
				jsonString+=line;
				line = br.readLine();
			}
			
			obj = new JSONObject(jsonString);
			
			br.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    	return obj;
    }
    
    public static void main(String agrs[]){
//    	String test = "<experiment> hello </experiment> <background> world </background>";
//    	System.out.println(test.replaceAll("</?(experiment|background)>", ""));
//    	System.out.println(test.substring(test.indexOf("<experiment>")+"<experiment>".length(), test.indexOf("</experiment>")));
    	
    	Map<Integer, String> m = new TreeMap<Integer, String>();
    	m.put(1, "A");
    	m.put(2, "B");
    	m.put(3, "C");
    	m.put(4, "D");
    	m.put(5, "E");
    	m.put(6, "F");
    	m.put(1, "V");
    	
    	for(Integer i : m.keySet())
    		System.out.println(i+" "+m.get(i));
    		
    }
    
}

