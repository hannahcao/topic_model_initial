package cao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.BidiMap;

/**
 * Class which represents a raw data.
 * @author cao
 *
 */
public class DataParsed {
	//Internal data structure after reading the data
	public Map<Integer, Doc> citedPubId2Docs; //map from cited_pub_id -> pub_doc_content
	public Map<Integer, Doc> citingPubId2Docs; //map from citing_pub_id -> pub_doc_content
	
	public BidiMap citedDocsP2B;
	public BidiMap citingDocsP2B;
	
	double [][]influencing_wd;
	double [][]influenced_wd;
	
	ArrayList<List<Integer>> refPubIndexIdList;

	/**
	 * Initialize the sample's internal structure. 
	 * Read the input graph, and the documents for the nodes
	 * If the passed graphFileName is empty, the program will set a manual set of data
	 * otherwise, the sampler will read the file and get the internal structures 
	 * Create the internal data structure that would be used in the sampling process.
	 *  
	 * These internal structures include
	 * (1) topological structure of influence
	 * (2) Vocabulary
	 * @param paperFolder
	 * @param InputGraphFileName
	 */
	public void init(String paperFolder, String graphEdgeFile)
	{
		System.out.println("\n"+Debugger.getCallerPosition()+"enter...");
		
		DataRaw rawdata = new DataRaw(); //Manually set the data
		
		//1. Get raw data
		if(graphEdgeFile.trim().length()==0)
			rawdata.getManualData();
		else
			GraphFile.loadFile(paperFolder, graphEdgeFile,rawdata);
		
        System.out.println(Debugger.getCallerPosition()+"PubId2CiteIds="+rawdata.pubId2CiteIds);
        System.out.println(Debugger.getCallerPosition()+"id2Docs size: "+rawdata.id2Docs.size());
        
        //2. Get all the cited documents cited_paper_id:cited_paper_content
        //   Get all the citing documents citing_paper_id:citing_paper_content
        citedPubId2Docs = initCitedDocs(rawdata.pubId2CiteIds, rawdata.id2Docs);
        citingPubId2Docs = initCitingDocs(rawdata.pubId2CiteIds, rawdata.id2Docs);
        System.out.println(Debugger.getCallerPosition()+"citedDocs size="+citedPubId2Docs.size()+
        		", keys set="+citedPubId2Docs.keySet()+"\t"+citedPubId2Docs);
        System.out.println(Debugger.getCallerPosition()+"citingDocs size="+citingPubId2Docs.size()+
        		", keys set="+citingPubId2Docs.keySet()+"\t"+citingPubId2Docs);
        //Problem with Laura Dietz's program: citing and cited docs DO NOT overlap
        //////////////
        //tested till to here
        //////////////
       
        //////////Similar to CitinfWrapper constructor
        //3. get vocabulary
        assert (rawdata.id2Docs.size() > 0);
        //Map<String, Integer> vocabularymap = new HashMap<String, Integer>();  //vocabulary: index
		List<String> vocab = new ArrayList<String>(); //all the vocabularies in the documents
		DocParser vocdoc = new DocParser(); //the design needs to be refined
        vocdoc.createVocabulary(rawdata.id2Docs,2,vocab);
        Map<String, Integer> vocabularymap = vocdoc.getVocabularymap();
        System.out.println(Debugger.getCallerPosition()+"vocabularymap="+vocabularymap);
        System.out.println(Debugger.getCallerPosition()+"vocab="+vocab);
        
        //the maximum number of words in the vocabulary list
        int maxTokenNum = vocab.size();//vocdoc.getVNum(); 
        System.out.println(Debugger.getCallerPosition()+"maxTokenNum="+maxTokenNum);

        //bidirection map cited_pub_id(int) <--> index_id(int) 
        citedDocsP2B = DocParser.createOid2OIdx(citedPubId2Docs.keySet());
        System.out.println(Debugger.getCallerPosition()+"citedDocsP2B="+citedDocsP2B);
        //Sampler.java:100:init: citedDocsP2B={1=0, 2=1, 3=2, 4=3, 5=4, 6=5, 7=6, 8=7, 9=8}
        //Change from obj id [1...9] to [0...8]
        
        //cited_wd
        influencing_wd = DocParser.calculateWordDocumentMatrix(
        		rawdata.id2Docs,vocabularymap,vocab,
        		citedDocsP2B,maxTokenNum,citedPubId2Docs.size());
        //System.out.println(Debugger.getCallerPosition()+"influencing_wd\n"+cao.Util.toString(influencing_wd));
        
        //bidirection map citing_pub_id(int) <--> index_id(int)
        citingDocsP2B = DocParser.createOid2OIdx(citingPubId2Docs.keySet());
        System.out.println(Debugger.getCallerPosition()+"citingDocsP2B="+citingDocsP2B);
        //Sampler.java:110:init: citingDocsP2B={10=0, 11=1, 12=2, 13=3}
        //Change from obj id [10...13] to [0...3]
        
        influenced_wd = DocParser.calculateWordDocumentMatrix(//citing_wd
        		rawdata.id2Docs,vocabularymap,vocab,
        		citingDocsP2B,maxTokenNum,citingPubId2Docs.size());
        //System.out.println(Debugger.getCallerPosition()+"influenced_wd\n"+cao.Util.toString(influenced_wd));

        //get bibliographies 
        //refPubIndexIdList[i]: Given a publication with index i
        //						Get the list of index ids for the publications in the
        //                      reference list of the given publication
        // This is used to draw oPrime
        refPubIndexIdList = new ArrayList<List<Integer>>();
        for (int i = 0; i < rawdata.pubId2CiteIds.size(); i++) {
        	refPubIndexIdList.add(new ArrayList<Integer>());
        }
        
        for (int citingPubId : rawdata.pubId2CiteIds.keySet()) {
            int citingBugs = vocdoc.getPubid2BugsId(citingPubId, citingDocsP2B);
            List<Integer> bib = refPubIndexIdList.get(citingBugs);
            for (int citedPubId : rawdata.pubId2CiteIds.get(citingPubId)) {
                int citedBugs = vocdoc.getPubid2BugsId(citedPubId, citedDocsP2B);
                bib.add(citedBugs);
            }
        }
        System.out.println(Debugger.getCallerPosition()+"refPubIndexIdList:"+refPubIndexIdList);
        //////////Similar to CitinfWrapper constructor
        
        System.out.println(Debugger.getCallerPosition()+"leave init...\n");
	}
	
	/**
     * Huiping noted 2012-02-29
     * Get all the cited documents cited_paper_id:cited_paper_content
     *  
     * @param pubId2CiteIds: map for paper_id:cited_paper_id
     * @param pubId2Docs: map for paper_id: paper_content
     * @return the cited documents information
     */
    private Map<Integer, Doc> initCitedDocs(Map<Integer, List<Integer>> pubId2CiteIds, Map<Integer, Doc> pubId2Docs) {
        Map<Integer, Doc> citingDocs = new HashMap<Integer, Doc>();
        for (List<Integer> cList : pubId2CiteIds.values()) {
            for (int id : cList) {
                if (!citingDocs.containsKey(id)) {
                	Doc doc = pubId2Docs.get(id);
                    assert (doc != null);
                    citingDocs.put(id, doc);
                }
            }
        }
        return citingDocs;
    }

    /**
     * Huiping noted 2012-02-29
     * Get all the information about the papers that cite others
     * 
     * @param pubId2CiteIds: map for paper_id:cited_paper_id
     * @param pubId2Docs: map for paper_id: paper_content
     * @return the citing papers' information in a map citing_paper_id:citing_paper_content
     */
    private Map<Integer, Doc> initCitingDocs(Map<Integer, List<Integer>> pubId2CiteIds, Map<Integer, Doc> pubId2Docs) {
        Map<Integer, Doc> citingDocs = new HashMap<Integer, Doc>();
        for (int id : pubId2CiteIds.keySet()) {
        	Doc doc = pubId2Docs.get(id);
            assert (doc != null);
            citingDocs.put(id, doc);
        }
        return citingDocs;
    }
	
}

