package cao;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GammaSummaryStorage implements Serializable{
		private static final long serialVersionUID = -2268924766057656844L;
		
		private String samplerId;
	    private int chainId;
	    //private int summaryNumber = 0; //the number of gamma summaries averaged in summariesMean
	    //private int sampleIndex = -1;
	    private int iterationNumber = 0;
	    
	    public GammaSummary summariesMean = null;
	    //public Map<Integer, Map<Integer, Double> > scalarSummariesMean = null;
	    
	    public double withinSeqVarPerChain = 0.0;
	    public double withinSeqVarPerChainTilde = 0.0;
	    protected boolean finished;
	    
	    public GammaSummaryStorage(String _samplerId, int _chainId) {
	        chainId = _chainId;
	        samplerId = _samplerId;
	        finished = false;
	    }
	    
	    public void update(double withinSeqVarPerChain, double withinSeqVarPerChainTilde, 
	    		GammaSummary newSummariesMean, int newIterationNumber)//int _summaryNumber) 
	    {
	    	this.withinSeqVarPerChain = withinSeqVarPerChain;
	        this.withinSeqVarPerChainTilde = withinSeqVarPerChainTilde;
	        //this.sampleIndex = sampleIndex;
	        this.summariesMean = newSummariesMean;
	        //this.summaryNumber = _summaryNumber;
	        this.iterationNumber = newIterationNumber;
	    }
	
	    /**
	     * Write Gamma summary to disk
	     */
	    public void writeSummaryToDisk() {
	        ObjectOutputStream w = null;
	        try {
	        	String fname = Util.getSummaryFileName(samplerId,chainId);
	            File sumFile = new File(fname);
	            
	            //System.out.println(Debugger.getCallerPosition()+"sumFile = "+ sumFile);
	            w = new ObjectOutputStream(new FileOutputStream(sumFile));
	            w.writeObject(this);
	            w.flush();
	            w.close();
	            if(Constant.DeleteTemporaryFile){
	            	sumFile.deleteOnExit();
	            }
	        } catch (IOException e) {
	            throw new RuntimeException(e);
	        }
	    }
	    
	  /**
	   * Read all chain's past summaries from disks
	   * And add current summaries 
	   * 
	   * @param curChainId
	   * @param curSamplerId
	   * @param curSummaryStorage
	   * @param allChainIds
	   * @return
	   * @throws Exception
	   */
	    public static Map<Integer, GammaSummaryStorage> readSummariesFromDisc(
	    		int curChainId,String curSamplerId,GammaSummaryStorage curSummaryStorage,
	    		List<Integer> allChainIds) 
	    		throws Exception
	    {
	        Map<Integer, GammaSummaryStorage> chainId2summaryStorage = new HashMap<Integer, GammaSummaryStorage>();
	        
	        //Add all chain's to summary storage 
	        for (int chain : allChainIds) {
	            if(chain==curChainId){
	            	//(1) For the current Chain: add the map of the current chainId to summary storage 
	    	        chainId2summaryStorage.put(curChainId, curSummaryStorage);
	            }else{
	            	//(2) For all the other chains: read the summary files from the disk
	                boolean readCompleted = false;
	                int retryNo = 0;
	                while (!readCompleted && retryNo < 3) {
	                    String filename = Util.getSummaryFileName(curSamplerId, chain);//"summaries." + Util.fileNamePrefix(curSamplerId, chain);
	                	File sumFile = new File(filename);
	                	System.out.println(Debugger.getCallerPosition()+"Read file "+sumFile);
	                    try {
	                        ObjectInputStream r = new ObjectInputStream(new FileInputStream(sumFile));
	                        Object summary_ = r.readObject();
	                        r.close();
	                        GammaSummaryStorage summary = (GammaSummaryStorage) summary_;
	                        chainId2summaryStorage.put(chain, summary);
	                        readCompleted = true;
	                    } catch (IOException e) {
	                        System.err.println(Debugger.getCallerPosition()+"unable to read " + sumFile + " retrying...");
	                        try {
	                            retryNo++;
	                            Thread.sleep(100);
	                        } catch (InterruptedException e1) {

	                        }
	                    } catch (ClassNotFoundException e) {
	                        throw new RuntimeException(e);
	                    }
	                }
	                if (!readCompleted) {
	                    throw new Exception(Debugger.getCallerPosition()+"Could not access summary file of chain " + curChainId);
	                }
	            }
	        }
	        return chainId2summaryStorage;
	    }
}
