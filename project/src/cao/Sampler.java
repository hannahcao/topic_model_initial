package cao;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import cao.Debugger;

/**
 * Sampler that do Gibbs sampling.
 * @author cao
 *
 */
public class Sampler {
	
	//input parameters for the sampler
	//private Parameter in_DistributionParam;
	private CmdOption cmdOption;
	//private String in_InputGraphFileName;
	int runnableChainNo =-1;
	List<Integer> allChainIds; 
	
	public DataParsed parsedData; //internal structures of the parsed data 
	
    boolean takeSamplesFromThisChain; 	//the final results is calculated from this sampler
    									//the data structure sampleAvgData needs to be calculated
    
    public int totalIter = 0;
    
    public long totalTime = 0;
    
//	BidiMap citedDocsP2B;
	//BidiMap citingDocsP2B; 
	
	//Internal data structure after reading the data
	//private Map<Integer, MyDocument> citedPubId2Docs; //map from cited_pub_id -> pub_doc_content
    //private Map<Integer, MyDocument> citingPubId2Docs; //map from citing_pub_id -> pub_doc_content
    
    private SampleData sampleData = null;
    private SampleData sampleAvgData = null;
    
    /**
     * return the avgerage SampleData.
     * @return
     */
    public SampleData getAvgSampleData(){
    	return this.sampleAvgData;
    }
    /**
     * return last SampleData
     * @return
     */
    public SampleData getSampleData(){
    	return this.sampleData;
    }
    
    //private MiniDistribution mapTopicPosteriorDistr;
    /**
     * Create the internal data structure that would be used in the sampling process.
     * (1) Sample structure
	 * (2) Sample drawing for each document  
	 * 
     * @param _option
     * @param _takeSamplesFromThisChain
     * @param _parsedData
     * @param _chainno
     */
    public Sampler(CmdOption _option, boolean _takeSamplesFromThisChain, DataParsed _parsedData, List<Integer> _allChainIds, int _chainno)
    {
    	cmdOption = _option;
    	takeSamplesFromThisChain = _takeSamplesFromThisChain;
    	allChainIds = _allChainIds;
    	runnableChainNo = _chainno;
    	parsedData = _parsedData; 
    	
    	System.out.println(Debugger.getCallerPosition()+"chain "+runnableChainNo+" init sample data...");
        sampleData = new SampleData(parsedData.influencing_wd, parsedData.influenced_wd, cmdOption.znum,
        		cmdOption.oprimeNum,parsedData.refPubIndexIdList,cmdOption);
        System.out.println(Debugger.getCallerPosition()+"chain "+runnableChainNo+" init average sample data...");
        sampleAvgData = new SampleData(parsedData.influencing_wd, parsedData.influenced_wd, cmdOption.znum, 
        		cmdOption.oprimeNum, parsedData.refPubIndexIdList,cmdOption);
    }
    
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
	 * (3) Sample structure
	 * (4) Sample drawing for each document  
	 */
//	public void init(DataParsed _parsedData)
//	{
//		System.out.println("\n"+Debugger.getCallerPosition()+"chain "+runnableChainNo+": enter...");
//		
//		parsedData = _parsedData;
//		//DataRaw rawdata = new DataRaw(); //Manually set the data
//		
//		//1. Get raw data 
//        //rawdata.getManualData();
//        //System.out.println(Debugger.getCallerPosition()+"PubId2CiteIds="+rawdata.pubId2CiteIds);
//        //System.out.println(Debugger.getCallerPosition()+"id2Docs size: "+rawdata.id2Docs.size());
//        
//        //2. Get all the cited documents cited_paper_id:cited_paper_content
//        //   Get all the citing documents citing_paper_id:citing_paper_content
//        //citedPubId2Docs = initCitedDocs(rawdata.pubId2CiteIds, rawdata.id2Docs);
//        //citingPubId2Docs = initCitingDocs(rawdata.pubId2CiteIds, rawdata.id2Docs);
//        //System.out.println(Debugger.getCallerPosition()+"citedDocs size="+citedPubId2Docs.size()+", keys set="+citedPubId2Docs.keySet()+"\t"+citedPubId2Docs);
//        //System.out.println(Debugger.getCallerPosition()+"citingDocs size="+citingPubId2Docs.size()+
//        //		", keys set="+citingPubId2Docs.keySet()+"\t"+citingPubId2Docs);
//        //Problem with Laura Dietz's program: citing and cited docs DO NOT overlap
//        //////////////
//        //tested till to here
//        //////////////
//       
//        //////////Similar to CitinfWrapper constructor
//        //3. get vocabulary
//        //assert (rawdata.id2Docs.size() > 0);
//        ////Map<String, Integer> vocabularymap = new HashMap<String, Integer>();  //vocabulary: index
//		//List<String> vocab = new ArrayList<String>(); //all the vocabularies in the documents
////		DocParser vocdoc = new DocParser(); //the design needs to be refined
////        vocdoc.createVocabulary(rawdata.id2Docs,2,vocab);
////        Map<String, Integer> vocabularymap = vocdoc.getVocabularymap();
////        System.out.println(Debugger.getCallerPosition()+"vocabularymap="+vocabularymap);
////        System.out.println(Debugger.getCallerPosition()+"vocab="+vocab);
////        
////        //the maximum number of words in the vocabulary list
////        int maxTokenNum = vocab.size();//vocdoc.getVNum(); 
////        System.out.println(Debugger.getCallerPosition()+"maxTokenNum="+maxTokenNum);
////
////        //bidirection map cited_pub_id(int) <--> index_id(int) 
////        citedDocsP2B = DocParser.createOid2OIdx(citedPubId2Docs.keySet());
////        System.out.println(Debugger.getCallerPosition()+"citedDocsP2B="+citedDocsP2B);
////        //Sampler.java:100:init: citedDocsP2B={1=0, 2=1, 3=2, 4=3, 5=4, 6=5, 7=6, 8=7, 9=8}
////        //Change from obj id [1...9] to [0...8]
////        
////        //cited_wd
////        double [][]influencing_wd = DocParser.calculateWordDocumentMatrix(
////        		rawdata.id2Docs,vocabularymap,vocab,
////        		citedDocsP2B,maxTokenNum,citedPubId2Docs.size());
////        //System.out.println(Debugger.getCallerPosition()+"influencing_wd\n"+cao.Util.toString(influencing_wd));
////        
////        //bidirection map citing_pub_id(int) <--> index_id(int)
////        citingDocsP2B = DocParser.createOid2OIdx(citingPubId2Docs.keySet());
////        System.out.println(Debugger.getCallerPosition()+"citingDocsP2B="+citingDocsP2B);
////        //Sampler.java:110:init: citingDocsP2B={10=0, 11=1, 12=2, 13=3}
////        //Change from obj id [10...13] to [0...3]
////        
////        double [][]influenced_wd = DocParser.calculateWordDocumentMatrix(//citing_wd
////        		rawdata.id2Docs,vocabularymap,vocab,
////        		citingDocsP2B,maxTokenNum,citingPubId2Docs.size());
////        //System.out.println(Debugger.getCallerPosition()+"influenced_wd\n"+cao.Util.toString(influenced_wd));
////
////        //get bibliographies 
////        //refPubIndexIdList[i]: Given a publication with index i
////        //						Get the list of index ids for the publications in the
////        //                      reference list of the given publication
////        // This is used to draw oPrime
////        ArrayList<List<Integer>> refPubIndexIdList = new ArrayList<List<Integer>>();
////        for (int i = 0; i < rawdata.pubId2CiteIds.size(); i++) {
////        	refPubIndexIdList.add(new ArrayList<Integer>());
////        }
////        
////        for (int citingPubId : rawdata.pubId2CiteIds.keySet()) {
////            int citingBugs = vocdoc.getPubid2BugsId(citingPubId, citingDocsP2B);
////            List<Integer> bib = refPubIndexIdList.get(citingBugs);
////            for (int citedPubId : rawdata.pubId2CiteIds.get(citingPubId)) {
////                int citedBugs = vocdoc.getPubid2BugsId(citedPubId, citedDocsP2B);
////                bib.add(citedBugs);
////            }
////        }
////        System.out.println(Debugger.getCallerPosition()+"refPubIndexIdList:"+refPubIndexIdList);
//        //////////Similar to CitinfWrapper constructor
//        
//        //similar to CitinfSampler constructor
//        System.out.println(Debugger.getCallerPosition()+"init sample data...");
//        sampleData = new SampleData(parsedData.influencing_wd, parsedData.influenced_wd, cmdOption.znum,
//        		cmdOption.oprimeNum,parsedData.refPubIndexIdList,cmdOption);
//        System.out.println("\n"+Debugger.getCallerPosition()+"init average sample data...");
//        sampleAvgData = new SampleData(parsedData.influencing_wd, parsedData.influenced_wd, 
//        		cmdOption.znum, cmdOption.oprimeNum, parsedData.refPubIndexIdList,cmdOption);
//        //System.out.println(Debugger.getCallerPosition()+"\nlSampleData\n"+lSampleData);
//        
//        //move this to getMAPCiting_zbop
//        //mapTopicPosteriorDistr = 
//        //	new MiniDistribution(in_DistributionParam.tnum * (in_DistributionParam.cnum + 1));
//        //System.out.println(Debugger.getCallerPosition()+"\nmapTopicPosteriorDistr\n"+mapTopicPosteriorDistr);
//        
//        
//        System.out.println(Debugger.getCallerPosition()+"chain "+runnableChainNo+"leave init...\n");
//	}
//	
	/**
	 * Draw the initial sample
	 */
	public void drawInitSample()
	{
		//draw initial sample
		System.out.println(Debugger.getCallerPosition()+"chain "+runnableChainNo+"initial sample");
		sampleData.drawInitialSample();
		//System.out.println(Debugger.getCallerPosition()+" initial SampleData\n"+sampleData);
	}
	
    /**
     * Gibbs sampling process 
     * If there are multiple chains, this function is run concurrently
     * 
     * @param BURN_IN: the number of iterations for burn-in stage
     */
    public void doGibbs()
	{
		boolean abort = false;
		boolean converged = false;
		int iter = 0;
		
		ConvergenceDiagnose convDiag = 
				new ConvergenceDiagnose(cmdOption.SAMPLER_ID,runnableChainNo, allChainIds,sampleData.in_refPubIndexIdList, 
						sampleData.in_influencing_wd[0].length, cmdOption);
		ResultStatisticsWriter chainStatWriter = 
				new ResultStatisticsWriter(cmdOption.SAMPLER_ID, runnableChainNo, allChainIds,cmdOption);
        chainStatWriter.initialize();
		
        Date startCheckpoint = new Date();
		for (iter = 0; iter < cmdOption.numIter && !abort && !converged; iter++) {
			
			Date newCheckPoint = new Date();
			//if(iter%100==0)
			System.out.println(Debugger.getCallerPosition()+"chain "+runnableChainNo+": Iteration " + iter + "/" + cmdOption.numIter + " time = " + (newCheckPoint.getTime() - startCheckpoint.getTime()));
			
			//(1) draw sample for one iteration
			sampleData.drawOneIterationSample();
			//System.out.println(Debugger.getCallerPosition()+" Iteration "+i+": SampleData\n"+sampleData);
			
			//(2) If this sampler is used to get results, average the counts after the burnning in phase
			if(takeSamplesFromThisChain && iter>=cmdOption.burnin){
				//average Sample counts with this iteration
				assert(this.sampleAvgData!=null);
				sampleAvgData.sumSampleCount(sampleData,iter-cmdOption.burnin+1);
			}
			
			//(3) Check the convergence of two chains after the burnning in phase
			if(iter>=cmdOption.burnin){
				convDiag.addOneChainSummary(sampleData); //add this summary data to monitor
				
				//check convergence of multiple chains
				converged = convDiag.checkConvergence(sampleData.in_refPubIndexIdList);
				
				chainStatWriter.addResultStatisticRecord(iter, convDiag.lastRHat);
                //abort = killFileExists(killFile);
			}
			totalIter = iter;
			totalTime = newCheckPoint.getTime() - startCheckpoint.getTime();
			
			//break;
		}//finish all the iterations
		
		//Average the counts from the "to-take-result" sampler
		if(takeSamplesFromThisChain && sampleAvgData!=null &&iter>=cmdOption.burnin){
			sampleAvgData.averageSampleCount(iter-cmdOption.burnin+1);
		}
		
		System.out.println(Debugger.getCallerPosition()+"chain "+runnableChainNo+": Last Iteration: " + iter + ", converged multiple chain = " + converged);
		
		convDiag.finish(sampleData.in_refPubIndexIdList);
        chainStatWriter.shutdown();
	}
	
    /**
     * Get map from o->(o'--- gamma value)
     * @return
     */
	public Map<Integer, Map<Integer, Map<Integer, Double>>> getGamma()
	{
		Map<Integer, Map<Integer, Map<Integer, Double>>> o2a2opgamma = 
				new TreeMap<Integer, Map<Integer, Map<Integer, Double>>>();
		
		//System.out.println(Debugger.getCallerPosition()+"sampleAvgData:\n"+sampleAvgData);
		for(Object obj: parsedData.citingDocsP2B.keySet()){
			Integer objIdx = (Integer)parsedData.citingDocsP2B.get(obj);
			Map<Integer, Map<Integer,Double>> a2op2gamma = getGammaForOneObj(objIdx);
			//Integer oid = (Integer)citingDocsP2B.getKey(objIdx);
			o2a2opgamma.put((Integer)obj, a2op2gamma);
		}
		return o2a2opgamma;
	}
	/**
	 * Get the gamma values for one objects 
	 * 
	 * @param objIdx
	 * @return Map (o' -- gamma value)
	 */
	private Map<Integer, Map<Integer, Double>> getGammaForOneObj(int objIdx){
    	
		Map<Integer, Map<Integer, Double>> result = new TreeMap<Integer, Map<Integer, Double>>();
    	//assert (takeSamplesFromThisChain);
    	List<Integer> oplist = sampleData.in_refPubIndexIdList.get(objIdx);
    	for(int a=0 ; a<cmdOption.anum; a++){
    		Map<Integer, Double> op2prob = new TreeMap<Integer, Double>();
    		for(int opIdx: oplist){
    			double gamma = Probability.gamma(objIdx, a, opIdx,sampleAvgData,cmdOption);
    			Integer opid = (Integer) parsedData.citedDocsP2B.getKey(opIdx);
    			assert (opid != null);
    			op2prob.put(opid, gamma);
    		}
    		result.put(a, op2prob);
        }
        return result;
    }
 }
