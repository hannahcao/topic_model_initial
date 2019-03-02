package cao;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;


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
     * @param testSet.  Test set
     */
    public Sampler(CmdOption _option, boolean _takeSamplesFromThisChain, DataParsed _parsedData, 
    		List<Integer> _allChainIds, int _chainno, Set<Integer> testSet)
    {
    	cmdOption = _option;
    	takeSamplesFromThisChain = _takeSamplesFromThisChain;
    	allChainIds = _allChainIds;
    	runnableChainNo = _chainno;
    	parsedData = _parsedData; 
    	
    	System.out.println(Debugger.getCallerPosition()+"chain "+runnableChainNo+" init sample data...");
        sampleData = new SampleData(   parsedData.influencing_wd, parsedData.influenced_wd, parsedData.influenced_wTaD, cmdOption.znum,
        		cmdOption.oprimeNum, parsedData.refPubIndexIdList,cmdOption, testSet);
        System.out.println(Debugger.getCallerPosition()+"chain "+runnableChainNo+" init average sample data...");
        sampleAvgData = new SampleData(parsedData.influencing_wd, parsedData.influenced_wd, parsedData.influenced_wTaD, cmdOption.znum, 
        		cmdOption.oprimeNum, parsedData.refPubIndexIdList,cmdOption, testSet);
    }
    /**
	 * Draw the initial sample
	 * @param trainOrTest true: training; false: test
	 */
	public void drawInitSample(boolean trainOrTest)
	{
		sampleAvgData = null;
		sampleAvgData = new SampleData(parsedData.influencing_wd, parsedData.influenced_wd, parsedData.influenced_wTaD, cmdOption.znum, 
	        		cmdOption.oprimeNum, parsedData.refPubIndexIdList,cmdOption, sampleData.testSet);
		//draw initial sample
		System.out.println(Debugger.getCallerPosition()+"chain "+runnableChainNo+"initial sample");
		sampleData.drawInitialSample(trainOrTest);
		//System.out.println(Debugger.getCallerPosition()+" initial SampleData\n"+sampleData);
	}
	
    /**
     * Gibbs sampling process 
     * If there are multiple chains, this function is run concurrently
     * 
     * @param BURN_IN: the number of iterations for burn-in stage
     */
    public void doGibbs(boolean trainOrTest)
	{
		boolean abort = false;
		boolean converged = false;
		
		int iter = 0;
		
		ConvergenceDiagnose convDiag = 
				new ConvergenceDiagnose(cmdOption.SAMPLER_ID,runnableChainNo, allChainIds, sampleData.in_refPubIndexIdList, 
						sampleData.numTa, Constant.oprimeNum, cmdOption);
		ResultStatisticsWriter chainStatWriter = 
				new ResultStatisticsWriter(cmdOption.SAMPLER_ID, runnableChainNo, allChainIds,cmdOption);
        chainStatWriter.initialize();
		
        Date startCheckpoint = new Date();
		for (iter = 0; iter < cmdOption.numIter && !abort && !converged; iter++) {
			Date newCheckPoint = new Date();
			//if(iter%100==0)
			//time is in 0.001 second.
			System.out.println(Debugger.getCallerPosition()+"chain "+runnableChainNo+": Iteration " + iter + "/" + cmdOption.numIter + " time = " + (newCheckPoint.getTime() - startCheckpoint.getTime()));
			
			//(1) draw sample for one iteration
			sampleData.drawOneIterationSample(trainOrTest);
			//System.out.println(Debugger.getCallerPosition()+" Iteration "+i+": SampleData\n"+sampleData);
			
			Map<Integer, Map<Integer, Double>> nobMap = sampleData.N_ob_influenced;
			Util.print2Map(nobMap);
			
			//(2) If this sampler is used to get results, average the counts after the burnning in phase
			if(takeSamplesFromThisChain && iter>=cmdOption.burnin){
				//average Sample counts with this iteration
				assert(this.sampleAvgData!=null);
				sampleAvgData.sumSampleCount(sampleData, iter-cmdOption.burnin+1);
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
		}//finish all the iterations
		//end for loop
		
		//Average the counts from the "to-take-result" sampler
		if(takeSamplesFromThisChain && sampleAvgData!=null &&iter>=cmdOption.burnin){
			sampleAvgData.averageSampleCount(iter-cmdOption.burnin+1);
		}
		
		System.out.println(Debugger.getCallerPosition()+"chain "+runnableChainNo+": Last Iteration: " + iter + ", converged multiple chain = " + converged);
		
		convDiag.finish(sampleData.in_refPubIndexIdList);
        chainStatWriter.shutdown();
	}
	/**
	 * only write the aspect that object have
	 * @param bw
	 */
    public void printGammaToFile(BufferedWriter bw){
    	for(Object obj: parsedData.citingDocsP2B.keySet()){
    		
    		Integer objIdx = (Integer)parsedData.citingDocsP2B.get(obj);
    		List<Integer> oplist = sampleAvgData.in_refPubIndexIdList.get(objIdx);
    		
    		if(cmdOption.model.equals(Constant.oaim))//oaim
        		for(int ta=0 ; ta<sampleAvgData.numTa; ta++){
        			
        			if(Util.get2Map(sampleAvgData.N_ota_influenced, objIdx, ta)==0)//if this object has no this aspect
        				continue;

        			for(int opIdx: oplist){
        				
        				double gamma = Probability.gamma(objIdx, ta, opIdx, sampleAvgData, cmdOption);//why sampleAvgData?
        				//    			double gamma = Probability.gamma(objIdx, a, opIdx, sampleData, cmdOption);//try sampleData
        				Integer opid = (Integer) parsedData.citedDocsP2B.getKey(opIdx);
        				try {
							bw.append(obj+"\t"+ta+"\t"+opid+"\t"+String.format(Locale.US, "%1$.5f", gamma)+"\n");
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
        			}
        		}
        	else
        		for(int a=0 ; a<cmdOption.anum; a++){
        			
        			if(Util.get2Map(sampleAvgData.N_oa_influenced, objIdx, a)==0)//if this object has no this aspect
        				continue; 

        			for(int opIdx: oplist){
        				
        				double gamma = Probability.gamma(objIdx, a, opIdx, sampleAvgData, cmdOption);//why sampleAvgData?
        				//    			double gamma = Probability.gamma(objIdx, a, opIdx, sampleData, cmdOption);//try sampleData
        				Integer opid = (Integer) parsedData.citedDocsP2B.getKey(opIdx);
        				try {
							bw.append(obj+"\t"+a+"\t"+opid+"\t"+String.format(Locale.US, "%1$.5f", gamma)+"\n");
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
        			}
        		}
    	}
    	try {
			bw.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
    	List<Integer> oplist = sampleAvgData.in_refPubIndexIdList.get(objIdx);
//    	List<Integer> oplist = sampleData.in_refPubIndexIdList.get(objIdx);
    	if(cmdOption.model.equals(Constant.oaim))//oaim
    		for(int ta=0 ; ta<sampleAvgData.numTa; ta++){
    			Map<Integer, Double> op2prob = new TreeMap<Integer, Double>();
    			for(int opIdx: oplist){
    				double gamma = Probability.gamma(objIdx, ta, opIdx, sampleAvgData, cmdOption);//why sampleAvgData?
    				//    			double gamma = Probability.gamma(objIdx, a, opIdx, sampleData, cmdOption);//try sampleData
    				Integer opid = (Integer) parsedData.citedDocsP2B.getKey(opIdx);
    				assert (opid != null);
    				op2prob.put(opid, gamma);
    			}
    			result.put(ta, op2prob);
    		}
    	else
    		for(int a=0 ; a<cmdOption.anum; a++){
    			Map<Integer, Double> op2prob = new TreeMap<Integer, Double>();
    			for(int opIdx: oplist){
    				double gamma = Probability.gamma(objIdx, a, opIdx, sampleAvgData, cmdOption);//why sampleAvgData?
    				//    			double gamma = Probability.gamma(objIdx, a, opIdx, sampleData, cmdOption);//try sampleData
    				Integer opid = (Integer) parsedData.citedDocsP2B.getKey(opIdx);
    				assert (opid != null);
    				op2prob.put(opid, gamma);
    			}
    			result.put(a, op2prob);
    		}
        return result;
    }
	
}
