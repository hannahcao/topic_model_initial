package cao;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;


public class ConvergenceDiagnose {

	private CmdOption cmdOption; 
	private String samplerId = "";
	private int chainId = -1;
	private int callNo = -1;
	private int D=0;
	
	//In one chain, keep all the gamma summaries and average gamma summaries in all the iterations
	private List<GammaSummary> gammaSummaryOneChainAllIteration = null; 
	private GammaSummary gammaSummaryOneChainMean = null; //Average gamma summaries for each (o,oprime) pair
	private GammaSummaryStorage summaryStorageOneChain; 
	
	//This is used for checking convergence
	private List<Integer> allChainIds = null;
	public double lastRHat=0.0;
	private boolean allChainsFinished = false;	
	
	public ConvergenceDiagnose(String _samplerId, int runnableChainNo, List<Integer> _allChainIds,
			ArrayList<List<Integer>> bibliographIndexidList, int D, CmdOption _cmdOption){
		
		this.D = D;
		cmdOption = _cmdOption;
		samplerId = _samplerId;
		chainId = runnableChainNo;
		allChainIds = _allChainIds;
		
		gammaSummaryOneChainAllIteration = new ArrayList<GammaSummary>(); 
		if (gammaSummaryOneChainMean == null) {
			gammaSummaryOneChainMean = new GammaSummary(bibliographIndexidList, _cmdOption.anum, cmdOption.oprimeNum);
        }
		
		summaryStorageOneChain = new GammaSummaryStorage(samplerId, chainId); 
	}
	
	 /**
     * Add the gamma value summary in one gibbs iteration to a list
     * Average the gamma value summaries
     */
    public void addOneChainSummary(SampleData sampleData)
    {
    	//(1). get Gamma summary: what is this current summary for? 
		//for each influenced object, create its list of p(o'|o) 
		//for each influenced object, create a list of distributions
		final GammaSummary currentSummary = new GammaSummary(//sampleData.getNumInfluencedObjects(),
                sampleData.getReferencePubIndexIdList(), sampleData.cmdOption.anum, sampleData.in_influencing_wd[0].length);
		
		for(SampleElementInfluenced e : sampleData.sampleInfluenced_otzbop){
			if(e.getB()==Constant.INHERITANCE){ //innovation
				currentSummary.addScalarDistribution(e.getObj(), e.getA(), e.getOprime(),1);
			}
		}
		
		//Normalize the summary values in the range of [0,1]
		currentSummary.normalize();
		
		//(2) Add the gamma value summary in one gibbs iteration to this chain's gamma summary
		gammaSummaryOneChainAllIteration.add(currentSummary);
		
		//(3) For this chain, calculate its statistics
		//(3.1) average all its gamma value summaries for all the iterations
 		//get the total number of summands
 		//System.out.println(Debugger.getCallerPosition()+"chainId="+chainId+",currentSummary\n"+currentSummary);
 		//System.out.println(Debugger.getCallerPosition()+"chainId="+chainId+",[1]summariesMean\n"+summariesMean);
		int numberOfSummands = gammaSummaryOneChainAllIteration.size();
 		gammaSummaryOneChainMean.updAverageSummaries(numberOfSummands, currentSummary);
		//System.out.println(Debugger.getCallerPosition()+"chainId="+chainId+",[1]summariesMean\n"+summariesMean);
		
 		//(3.2) average within-sequence variance 
 		//      (sum_{i=1...iternum} (\psi_{i,j}-\psi_{\dot j})^2)/(iternum) = sj^2 * (iternum)/(iternum-1)
	    double wj = GammaSummary.getWithinSequenceVarPerChain(gammaSummaryOneChainAllIteration,gammaSummaryOneChainMean,false);
	    
	    //(3.3) average within-sequence variance tilde 
	    //      (sum_{i=1...iternum} (\phi_{i,j}-\phi_{j})^2)/(iternum-1) = sj^2
        double wjtilde = GammaSummary.getWithinSequenceVarPerChain(gammaSummaryOneChainAllIteration,gammaSummaryOneChainMean,true);
        //System.out.println(Debugger.getCallerPosition()+"chainId="+chainId+",wj="+wj+",wjtilde="+wjtilde);
        
        //(4) Update this chain's summary storage
        summaryStorageOneChain.update(wj, wjtilde, gammaSummaryOneChainMean,gammaSummaryOneChainAllIteration.size());//, gammaSummariesOverTime.size()-1);
        
        //(5) write to disk this chain's (mean, within-sequence variance, within-sequence variance tilde) 
        summaryStorageOneChain.writeSummaryToDisk(); //write the summary to the disks, however, these files are marked with deleteOnExit
    }
    
    /**
     * Check whether the multiple chains converge or not
     * @return
     */
    public boolean checkConvergence(List<List<Integer>> refPubList)
    {
    	callNo++;
    	
    	//1. only one chain, no need to check the convergence
    	if(allChainIds.size()<=1) 
    		return false;
    	
    	Map<Integer, GammaSummaryStorage> chainId2summaryStorage = null;
    	
    	
    	//2. Read all chains' past summary information from disk
    	//   Get: chainid <--> gammaSummaryStorage (mean, wj, wjtilde)
		try {
			chainId2summaryStorage = GammaSummaryStorage.readSummariesFromDisc(chainId, samplerId, summaryStorageOneChain, allChainIds);
		} catch (Exception e) {
			return false;
		}

		//3. Calculate the potential value
        double rHat = getEstimatedPotentialScaleReduction(chainId2summaryStorage, refPubList);

        //writeChainMeanDebug();
        lastRHat = rHat;

        if ((rHat < cmdOption.R_HAT_THRESH)) {
        	summaryStorageOneChain.finished = true;
            summaryStorageOneChain.writeSummaryToDisk();
            
        	if (!summaryStorageOneChain.finished) {
                System.out.println(Debugger.getCallerPosition()+"Chain CONVERGED. CallNo="+callNo);
            }
            boolean allConverged = checkAllChainsConverged();
            return allConverged;
        } else {
            return false;
        }
    } 
    
    /**
     * Average the withinSeqVarPerChain
     *    w = (1/chainnum) * (\sum_{1}^{chainnum} wj) 
     *    where wj = sj^2 * (iternum)/(iternum-1) = (\sum_{i=1}^{iternum) (\psi_{ij} - \psi_{i})^2)/iternum
     *    I.e., w = W (in the paper)*(iternum)/(iternum-1) = W * n /(n-1)
     * @param chaiId2Summaries
     * @return
     */
    private double avgWithinSequenceVar(Map<Integer, GammaSummaryStorage> chaiId2Summaries) {
        double sumVariance = 0.0;

        for (Integer chainKey : chaiId2Summaries.keySet()) {
        	GammaSummaryStorage summary = chaiId2Summaries.get(chainKey);
            sumVariance += summary.withinSeqVarPerChain;
        }
        
        return (sumVariance/chaiId2Summaries.size());
    }
	
    /**
     * Average the withinSeqVarPerChainTilde, i.e., W in the paper
     *  
     * This function calculates wTidle (in code) = W (in paper) = (1/chainnum) * (\sum_{1}^{chainnum} s^2)
     * where s2 = (1/(iternum-1)) * (\sum_{i=1}^{iternum) (\psi - \psi_{i})^2)
     * @param chain2Summaries
     * @return
     */
    private double avgWithinSequenceVarTilde(Map<Integer, GammaSummaryStorage> chaiId2Summaries) {
    	double sumVariance = 0.0;

        for (int chainKey : chaiId2Summaries.keySet()) {
        	GammaSummaryStorage summary = chaiId2Summaries.get(chainKey);
        	sumVariance +=summary.withinSeqVarPerChainTilde;
        }
        return (sumVariance/chaiId2Summaries.size());
    }
    
    
    /**
     * Check whether each chain's summary is marked with finished.
     * @return
     */
    private boolean checkAllChainsConverged() {
        try {
            // if all chains are finished, remove scalar summaries
            Map<Integer, GammaSummaryStorage> gammaSummaries = 
            		GammaSummaryStorage.readSummariesFromDisc(chainId, samplerId, summaryStorageOneChain, allChainIds);
            allChainsFinished = checkFinishFileExists();
            if (chainId==(allChainIds.get(0)) && !allChainsFinished) {
                allChainsFinished = isAllChainSummariesMarkedAsFinished(gammaSummaries);
                if (allChainsFinished) {
                    try {
                    	String finishFileName = Util.getFinishFileName(samplerId); 
                        FileWriter f = new FileWriter(finishFileName);
                        f.write("");
                    } catch (IOException e) {
                        System.err.println("ConvergenceDiagnosis#checkAllChainsConverged: " + e.toString());
                    }
                }
            }
            return allChainsFinished;
        } catch (Exception e) {
            // one chain has not even been started... which is actually curious, because finish should only be called
            // after *all* chains converged.
            System.err.println("finish should only be called after all chains converged, but could not access "
                    + "summary file for chain " + chainId);
            new RuntimeException(e).printStackTrace();
            return false;
        }
    }
    
    /**
     * Make sure that every chain is finished.
     * @param gammaSummaries
     * @return
     */
    private boolean isAllChainSummariesMarkedAsFinished(Map<Integer, GammaSummaryStorage> gammaSummaries) {
        boolean allFinished = true;
        for (int chainId : gammaSummaries.keySet()) {
            GammaSummaryStorage summary = gammaSummaries.get(chainId);
            allFinished = allFinished && summary.finished;
        }
        return allFinished;
    }
    
    private boolean checkFinishFileExists() {
        return (new File(Util.getFinishFileName(samplerId)).exists());
    }
    
    /**
     * From all chains calculate rHat value
     * 
     * @param allChainSummaries
     * @return
     */
    private double getEstimatedPotentialScaleReduction(Map<Integer, GammaSummaryStorage> allchainId2gammaSummaryStorage, List<List<Integer>> refPubList) {
    	
    	//(1) Average the gamma summaries for all the chains: \psi_{\dot\dot}
    	GammaSummary allChainMean = getAllChainMean(allchainId2gammaSummaryStorage, refPubList);

    	//(2) Average the within sequence variance for all the chains
    	//    w = (1/chainnum) * (\sum_{1}^{chainnum} wj) 
    	//    where wj = = sj^2 * (iternum)/(iternum-1) = (\sum_{i=1}^{iternum) (\psi_{ij} - \psi_{i})^2)/iternum
    	//    I.e., w = W (in the paper)*(iternum)/(iternum-1) = W * n /(n-1)
        double w = avgWithinSequenceVar(allchainId2gammaSummaryStorage);
        
        //    wTidle = (1/chainnum) * (\sum_{j=1}^{chainnum} sj^2)
        //    where sj^2 = wjTilde = (1/(iternum-1)) * (\sum_{i=1}^{iternum) (\psi_{ij} -\psi_{j})^2)
        //    I.e., wTilde = W (in the paper) < w
        double wTilde = avgWithinSequenceVarTilde(allchainId2gammaSummaryStorage);
        
        //(3) Average the between-sequence variance
        //	  bTilde = (iternum *(\sum_{j=1}^{chainnum} (\psi_j - \psi_{\dot\dot})^2))/chainnum-1;
        //    i.e., bTilde = B(in the paper)/iternum
        double bTilde = getBetweenSequenceVar(allchainId2gammaSummaryStorage, allChainMean);

        //(4) Estimation of var(\psi|y)
        //    vHat = (iternum-1)/iternum * W (in the paper) + B(in the paper)/iternum = (iternum-1)/iternum * wTilde + bTilde
        //double vHat = wTilde + bTilde;
        
        // (5) Check that vHat has converged
        //     In Gelman et al. 2004 p.296, rHat = sqrt (vHat/wTilde) = sqrt(\frac{n-1}{n}+B/{n*W})
        //     wTilde/w = \frac{n-1}{n}
        //double rHat = vHat / w;
        double rHat = Math.sqrt(wTilde/w + bTilde/wTilde);

        if (Double.isNaN(rHat)) {
            //if (callNo > 10 && w == 0.0 && vHat == 0.0) 
        	if (callNo > 10 && w == 0.0 && wTilde == 0.0 && bTilde ==0.0)
            {
                rHat = 1.0;
            }
        }

        if ((Double.isNaN(rHat)) || Double.isInfinite(rHat)) {
            System.out.println(Debugger.getCallerPosition()+"rHat = " + rHat);
            System.out.println(Debugger.getCallerPosition()+"  (" + Util.cut(wTilde) + "+" + Util.cut(bTilde) + ")/" + Util.cut(w) + " = (wTilde + bTilde) / w)");
        } else {
            System.out.println(Debugger.getCallerPosition()+"rHat = " + rHat);
        }
        return rHat;
    }
    

    
    /**
     * Average the summary storage from all the past chains
     * @param chainId2summaryStorage
     * @return
     */
    protected GammaSummary getAllChainMean(Map<Integer, GammaSummaryStorage> chainId2summaryStorage, List<List<Integer>> refPubList) {
        // sum
        GammaSummary allChainMean = new GammaSummary(refPubList, this.cmdOption.anum, this.D);
        
        int processedChainNum = 1;
        for (Integer chainKey : chainId2summaryStorage.keySet()) {
        	GammaSummaryStorage storage = chainId2summaryStorage.get(chainKey);
            GammaSummary perChainMean = storage.summariesMean;
            allChainMean.updAverageSummaries(processedChainNum, perChainMean);
            processedChainNum++;
        }
        return allChainMean;
    }


    /**
     * (\sum_{j=1}^{chainnum} (\psi_j - \psi_{\dot\dot})^2)/chainnum;
     * 
     * @param allChainSummaries
     * @param allChainMeans
     * @return B~
     */
    protected double getBetweenSequenceVar(Map<Integer, GammaSummaryStorage> chainid2summaryStorage, 
    		GammaSummary allChainMeans) {

    	assert(allChainMeans.distributions.size()==allChainMeans.distributions.size());
    	
    	double betweenSeqDiffSum = 0.0; //(\sum_{j=1}^{chainnum} (\psi_j - \psi)^2)
    	
    	//Average the variance of gamma summaries on all the chains 
    	for (Integer chainKey : chainid2summaryStorage.keySet()) {
         	GammaSummaryStorage oneChainStorage = chainid2summaryStorage.get(chainKey);
         	GammaSummary oneChainMean = oneChainStorage.summariesMean;
             
             //Calculate the average variance between these two gamma summaries
             //Average (\psi_j - \psi_{\dot\dot})^2 for all the (obj, oprime) pairs
             double avgDiffVarianceOneChainTilde = GammaSummary.avgVarSummaries(oneChainMean, allChainMeans);
             //avgDiffVarianceOneChainTilde = avgDiffVarianceOneChainTilde * oneChainStorage.iterationNumber; //*n factor
             betweenSeqDiffSum += avgDiffVarianceOneChainTilde;
    	 }
    	 
    	int chainnum = chainid2summaryStorage.size();
    	return betweenSeqDiffSum/(chainnum-1);
    }
    
    
    /**
     * Sets this chain as finished. If all other chains are finished as well, their summaryfiles are removed
     * (To be precise: are marked for deletion once this VM is exited)
     *
     * @return true if this chain is the last chain
     * @see #isAllChainsFinished()
     */
    public boolean finish(List<List<Integer>> refPubList) {
        if (!checkConvergence(refPubList)) {
            System.err.println(Debugger.getCallerPosition()+
            		"ConvergenceDiagnosis#finish: chain has not converged. Finish must only be called after convergence.");
        }

        return true;
    }
    
 
}
