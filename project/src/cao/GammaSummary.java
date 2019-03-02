package cao;

import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

/**
 * Class to hold probabilities p(o'|o)
 * @author Huiping Cao
 *
 */
public class GammaSummary implements Serializable{
	
	private static final long serialVersionUID = -4691182351864516876L;
	/**
	 * obj --> {a --> (oprime -> frequency)}
	 */
	Map<Integer, Map<Integer, Map<Integer, Double>>> distributions = null;
	
	int A;
	int D;
	
	public GammaSummary(int A, int D)
	{
		this.A = A;
		this.D = D;
		distributions = new TreeMap<Integer, Map<Integer, Map<Integer, Double>>>();
		for(int objIdx=0; objIdx<1; objIdx++){
//        	a --> o' --> freq
        	Map<Integer, Map<Integer, Double>> a2oprime = new TreeMap<Integer, Map<Integer, Double>>(); 
        	for(int a =0; a<this.A ;a++){
//        		o' --> freq
        		Map<Integer, Double> op2freq = new TreeMap<Integer, Double>();
        		for(int oprimeIdx=0; oprimeIdx<this.D; oprimeIdx++){
        			op2freq.put(oprimeIdx, 0.0);
        		}
        		a2oprime.put(a, op2freq);
        	}
        	distributions.put(objIdx, a2oprime);
        }
	}
	/**
	 * 
	 * @param numObjects
	 * @param refPubList
	 */
	public GammaSummary(List<List<Integer>> refPubList, int A, int D)
	{
		this.A = A;
		this.D = D;
		distributions = new TreeMap<Integer, Map<Integer, Map<Integer, Double>>>();
		
		for(int objIdx=0; objIdx< refPubList.size(); objIdx++){
        	List<Integer> oprimeIdxList = refPubList.get(objIdx);
//        	a --> o' --> freq
        	Map<Integer, Map<Integer, Double>> a2oprime = new TreeMap<Integer, Map<Integer, Double>>(); 
        	for(int a =0; a<A ;a++){
//        		o' --> freq
        		Map<Integer, Double> op2freq = new TreeMap<Integer, Double>();
        		for(int oprimeIdx=0; oprimeIdx<D; oprimeIdx++){
        			op2freq.put(oprimeIdx, 0.0);
        		}
        		a2oprime.put(a, op2freq);
        	}
        	distributions.put(objIdx, a2oprime);
        }
//		
//		for(int objIdx=0; objIdx< refPubList.size(); objIdx++){
//        	List<Integer> oprimeIdxList = refPubList.get(objIdx);
////        	a --> o' --> freq
//        	Map<Integer, Map<Integer, Double>> a2oprime = new TreeMap<Integer, Map<Integer, Double>>(); 
//        	for(int a =0; a<A ;a++){
////        		o' --> freq
//        		Map<Integer, Double> op2freq = new TreeMap<Integer, Double>();
//        		for(int oprimeIdx: oprimeIdxList){
//        			op2freq.put(oprimeIdx, 0.0);
//        		}
//        		a2oprime.put(a, op2freq);
//        	}
//        	distributions.put(objIdx, a2oprime);
//        }
	}
	
	public String toString(){
		String dist = "";
		for(Map.Entry<Integer, Map<Integer, Map<Integer, Double>>> entry: distributions.entrySet()){
         	int obj = entry.getKey();
         	Map<Integer, Map<Integer, Double>> a2oprime = entry.getValue();
        	for(Map.Entry<Integer, Map<Integer, Double>> a2oprimeEntry: a2oprime.entrySet()){
        		int a = a2oprimeEntry.getKey();
        		Map<Integer, Double> op2freq = a2oprimeEntry.getValue();
        		for(Map.Entry<Integer, Double> op2freqEntry : op2freq.entrySet()){
        			int op = op2freqEntry.getKey();
        			double freq = op2freqEntry.getValue();
        		dist += "("+obj+","+a+","+op+")="+freq+", ";
        		}
        	}
         }
		return dist+"\n";
	}
	/**
	 * Add the influence time to the scalar distribution
	 * 
	 * Checked, correct
	 * @param o: object
	 * @param oprime: object that influences o
	 * @param frequency: the number of times that oprime influences o
	 */
	public void addScalarDistribution(int o, int a, int oprime, double frequency) {
		
		assert(oprime>=0);
        //distributions.get(o).add(oprime, aspect);
		Map<Integer, Map<Integer, Double>> a2oprime = distributions.get(o);
		if(a2oprime==null){//this branch, checked correct
			a2oprime = new TreeMap<Integer, Map<Integer, Double>>();
			Map<Integer, Double> oprime2freq = new TreeMap<Integer, Double>();
			oprime2freq.put(oprime, frequency);
			a2oprime.put(a, oprime2freq);
			distributions.put(o, a2oprime);
		}else{
			Double oldfrequency = a2oprime.get(a).get(oprime);
			if(oldfrequency==null){
				a2oprime.get(a).put(oprime, frequency); //checked, correct
			}else{
				a2oprime.get(a).put(oprime, frequency+oldfrequency);//checked, correct
			}
		}
    }
	/**
	 * Normalize the citation frequency to the range of [0,1]
	 */
	public void normalize()
	{
		for(Map.Entry<Integer, Map<Integer,Map<Integer,Double>>> entry: distributions.entrySet()){
			Map<Integer,Map<Integer,Double>> a2oprime = entry.getValue();
			
			for(Map.Entry<Integer, Map<Integer,Double>> a2oprimeEntry : a2oprime.entrySet()){
				//get the normalized frequency
				Map<Integer, Double> oprime2freq = a2oprimeEntry.getValue();
				double objsum = sum(oprime2freq);
				
				for(Map.Entry<Integer, Double> oprime2freqEntry: oprime2freq.entrySet()){
					double avgFreq = oprime2freqEntry.getValue()/objsum;
					if(Double.isNaN(avgFreq))
						oprime2freqEntry.setValue(0.0);
					else{
						oprime2freqEntry.setValue(avgFreq);
					}
				} 
			}
		}
	}
	
	/**
	 * Summarize the total citation frequency
	 * 
	 * @param op2freq
	 * @return
	 */
	private double sum(Map<Integer,Double> op2freq){
		
		double sumFreq = 0.0;
		for(Map.Entry<Integer, Double> entry: op2freq.entrySet()){
			sumFreq += entry.getValue();
		}
		return sumFreq;
	}
	
	/**
     * Calculate the average variance between theses two gamma summaries
     * 
     * @param perChainMean
     * @param allChainMeans
     * @return
     */
    public static double avgVarSummaries(GammaSummary perChainMean, GammaSummary allChainMean)
    {
    	 double sumVariance= 0.0;
    	 
         int numObjPair = 0;
         for(Map.Entry<Integer, Map<Integer, Map<Integer, Double>>> entry: perChainMean.distributions.entrySet()){
         	int obj = entry.getKey();
        	Map<Integer, Map<Integer, Double>> a2oprimeMap = entry.getValue();
        	for(Map.Entry<Integer, Map<Integer, Double>> a2oprimeEntry: a2oprimeMap.entrySet()){
        		int a = a2oprimeEntry.getKey();
        		Map<Integer, Double> oprime2freqMap = a2oprimeEntry.getValue();
        		
        		for(Map.Entry<Integer, Double> op2freqEntry : oprime2freqMap.entrySet()){
        			int oprime = op2freqEntry.getKey();
        			double perChainProb = op2freqEntry.getValue();
        			double diff = perChainProb-allChainMean.distributions.get(obj).get(a).get(oprime);
        			sumVariance = diff*diff;
        			numObjPair++;
        		}
        	}
         }
         
         double avgDiffPerChain = sumVariance/numObjPair;
         
         return avgDiffPerChain;
    }
    
 	/**
 	 * Use the current summary to update the overall average summary
 	 * @param totalNumberOfSummands: the total number of summands (which include the new summary)
 	 *                               This number is used to calculate the average
 	 * @param currentSummary
 	 */
 	public void updAverageSummaries(int totalNumberOfSummands, GammaSummary newSummary) {
 		assert(totalNumberOfSummands>0);
 		
 		//loop the current summary distributions: loop obj
 		for(Map.Entry<Integer, Map<Integer, Map<Integer, Double>>> entry : newSummary.distributions.entrySet()){
 			int obj = entry.getKey();
 			Map<Integer, Map<Integer, Double>> a2opMap = entry.getValue();
 			if(a2opMap == null)continue;
 			//loop the current summary distributions: fix obj, loop a
 			for(Map.Entry<Integer, Map<Integer, Double>> a2opEntry: a2opMap.entrySet()){
 				int a = a2opEntry.getKey();
 				Map<Integer, Double> op2freqMap = a2opEntry.getValue();
 				
 				if(op2freqMap==null)continue;
 				
 				for(Map.Entry<Integer, Double> op2freqEntry : op2freqMap.entrySet()){
 					int oprime = op2freqEntry.getKey();
 					Double newProb = op2freqEntry.getValue();
 					if(newProb==null)newProb = 0.0;
 					
 					Map<Integer, Map<Integer,Double>> existSummaryA2OpMap = distributions.get(obj);
 	 				if(existSummaryA2OpMap==null){
 	 					existSummaryA2OpMap = new TreeMap<Integer, Map<Integer, Double>>();
 	 					Map<Integer, Double> op2freq = new TreeMap<Integer, Double>();
 	 					op2freq.put(oprime, 0.0);
 	 					existSummaryA2OpMap.put(a, op2freq);
 	 					distributions.put(obj,existSummaryA2OpMap);
 	 				}
 	 				
 	 				Double oldMean = 0.0;
 	 				if(distributions==null||(distributions.get(obj)==null)||(distributions.get(obj).get(a)==null)||(distributions.get(obj).get(a).get(oprime)==null)){
// 	 					System.out.println("distribution null? "+distributions==null);
// 	 					System.out.println("obj "+obj+" ->a null? "+(distributions.get(obj)==null));
// 	 					System.out.println("a "+a+" ->oprime null? "+(distributions.get(obj).get(a)==null));
// 	 					System.out.println("oprime "+oprime+" ->freq null? "+(distributions.get(obj).get(a).get(oprime)==null));
 	 					oldMean = 0.0;
 	 				}else
 	 					oldMean = distributions.get(obj).get(a).get(oprime);
 					
 	 				if(oldMean ==null){
 	 					double newMean = (newProb) / (totalNumberOfSummands);
 	 					distributions.get(obj).get(a).put(oprime, newMean);
 	 				}else{
 	 					double newMean = (oldMean * (totalNumberOfSummands-1) + newProb) / (totalNumberOfSummands);
 	 					if(oldMean!=newMean)
 	 						distributions.get(obj).get(a).put(oprime, newMean);
 	 				}
 				}
 			}
 		}
 	}
 	
 	/**
     * 
     * Return the variance average of all the (o,oprime) pairs
     * TODO: give an example 
     * @param tildeVersion
     * @return when tildeVersion=true, return sj^2; 
     *         when tildeVersion=false, return sj^2 *(iternum)/(iternum-1)
     */
    public static double getWithinSequenceVarPerChain(List<GammaSummary> summariesAllIterations, 
    		GammaSummary summaryMean, boolean tildeVersion){
    	
    	double totalAverage = 0.0;
    	int numberOfIterations = summariesAllIterations.size();
    	double sumVariance = 0.0;
    	int totalPairNumber = 0;
    	for(Map.Entry<Integer, Map<Integer, Map<Integer, Double>>> meanEntry: summaryMean.distributions.entrySet()){
    		
    		int obj = meanEntry.getKey();
			Map<Integer, Map<Integer, Double>> a2oprimeMeanMap = meanEntry.getValue();
			assert(a2oprimeMeanMap == null);
			
			//loop the current summary distributions: fix obj, loop oprime:prob, to update mean
			for(Map.Entry<Integer, Map<Integer, Double>> a2oprimeEntry: a2oprimeMeanMap.entrySet()){
				int a = a2oprimeEntry.getKey();
				Map<Integer, Double> op2meanProb = a2oprimeEntry.getValue();
				
				for(Map.Entry<Integer, Double> op2ProbEntry : op2meanProb.entrySet()){
					int oprime = op2ProbEntry.getKey();
					Double meanProb = op2ProbEntry.getValue();
					
					totalPairNumber++;
					double sumDiffs = 0.0;
					for(int iteration = 0; iteration<numberOfIterations; iteration++){
						double diff = (summariesAllIterations.get(iteration).distributions.get(obj)).get(a).get(oprime)-meanProb;
						sumDiffs += (diff*diff);
					}
					if(tildeVersion){//true
						sumVariance += sumDiffs/(numberOfIterations -1);
					}else{
						sumVariance += sumDiffs/(numberOfIterations);
					}
				}
			}
    	}
    	totalAverage = sumVariance/totalPairNumber;
    	
    	return totalAverage;
    }
}
