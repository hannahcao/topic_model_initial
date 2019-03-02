package alfred;

/**
 * Test multinomial function
 * 
 * @author Huiping Cao
 * @date Apr. 3, 2012
 */
public class MultinomialTest {

	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		int K=10;
		int n=1000; // 89 108 116 100 89 108 85 121 87 97
		double[] prob = new double[K];
		
		for(int i=0;i<K;i++) 
			prob[i] = (((double)1)/K);
		int[] sample = Multinomial.multinomialSample(n, prob, K);
		
		//wrapper
		Integer[] sampleType = new Integer[sample.length];
		for(int i=0;i<sample.length;i++)sampleType[i]=sample[i];
		
		//print the sample
		Util.printArray(sampleType);
	}

	
	
}
