package cao;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Added by Chuan. Gibbs sampling chain.  
 * @author cao
 *
 */
class SamplerRunnable implements Runnable{

	int chainNo;
	SamplerChain samplerChain;
	
	/**
	 * Create a runnable sampler
	 * 
	 * @param _chainNo: the chain no of this runnable sampler
	 * @param _samplerChain: the sample chain this runnable belong to
	 */
	public SamplerRunnable(int _chainNo, SamplerChain _samplerChain)
	{
		chainNo = _chainNo;
		samplerChain = _samplerChain;
	}
	
	@Override
	public void run() {
		
		boolean takeSamplesFromThisChain = (chainNo==samplerChain.allChainIds.get(0));
		System.out.println(Debugger.getCallerPosition()+"Chain: "+ chainNo);
		
		//2. create a sampler (set the parameters) 
		Sampler sampler = new Sampler(samplerChain.cmdOption,takeSamplesFromThisChain,samplerChain.parsedData,samplerChain.allChainIds,chainNo);
		
		//3. initialize all the initial structures for sampling 
		// (read and parse documents, create intermediate data structure, etc.) 
		//sampler.init(samplerChain.parsedData);
		
		//4. draw the initial sample 
		sampler.drawInitSample();
		
		if (takeSamplesFromThisChain) {
			samplerChain.samplerToGetResult = sampler;
		}
	
		sampler.doGibbs();
	}
}


public class SamplerChain {
	
	DataParsed parsedData; 		//the data structure after parsing the original data
	CmdOption cmdOption;		//command options (with all the parameters)
	Sampler samplerToGetResult;	//from this sampler to derive the final results
	
	List<Integer> allChainIds;	//the total number of chains used to test the sampler convergence
	
	/**
	 * Initialize sample chain, read sample data
	 * 
	 * Debugged & correct Aug. 17, 2012
	 * 
	 * @param _cmdOption
	 */
	public SamplerChain(CmdOption _cmdOption)
	{
		cmdOption = _cmdOption;
		parsedData = new DataParsed();
		parsedData.init(cmdOption.paperfolder,cmdOption.graphfile);
		
		allChainIds = new ArrayList<Integer>();
		
		for(int i=0;i<cmdOption.chainNum;i++){
			allChainIds.add(i);
		}
	}
	
	/**
	 * Run two threads and do gibbs sample
	 */
	public void doGibbs()
    {
		System.out.println(Debugger.getCallerPosition()+"allChainids size="+ allChainIds.size()+":" +allChainIds);
		
		//Create the sampler threads 
		final List<Thread> threadlist = new ArrayList<Thread>();
		for (int chain : allChainIds) {
			Runnable runnableSampler = new SamplerRunnable(chain,this);
			Thread thread = new Thread(runnableSampler,"train-" + chain);
			
			//if(chain.trim().equals("1")) break;//Huiping added, just use one chain
			threadlist.add(thread);
		}
     
		//Start the sampler threads
		for (Thread thread : threadlist) {
			System.out.println(Debugger.getCallerPosition()+"start thread" + thread.getId()+":" + thread.getName());
			thread.start();
		}
		
		for (Thread thread : threadlist) {
			try {
				thread.join();
			} catch (InterruptedException e) {
				System.err.println(Debugger.getCallerPosition()+"interrupted");
			}
		} 
    }
	
	/**
	 * Get the gamma results
	 * @return map of (object: (influencing object, influence))
	 */
	public Map<Integer, Map<Integer, Map<Integer, Double>>>  getGamma(){ 
		return (samplerToGetResult.getGamma());
	}
	

	public double[][] getOAInfluenced(){
		return samplerToGetResult.getAvgSampleData().N_oa_influenced;
//		return samplerToGetResult.getSampleData().N_oa_influenced;
	}
	
	public double[] getOInfluenced(){
		return samplerToGetResult.getAvgSampleData().N_o_influenced;
	}
	
	public double[][] getOOP(){
		return samplerToGetResult.getAvgSampleData().N_oop_bold_influenced;
	}
	
	public double[][] getOB(){
		return samplerToGetResult.getAvgSampleData().N_ob_influenced;
	}
}
