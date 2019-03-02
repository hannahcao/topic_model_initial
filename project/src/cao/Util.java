package cao;

import java.util.Arrays;
import java.util.List;

/**
 * 
 * @author Huiping Cao
 * @date Apr. 2, 2012
 */
public class Util{
	private static final int PRECISION = 2;
	
	//////////////////////////////////////////////////////////////////
	//Unified way to form temporary and result file names
    //////////////////////////////////////////////////////////////////
	//private static String fileNamePrefix(String samplerId, int chainId){
	//	return (samplerId + "-" + chainId);
	//}
	
	//temporary file for gammasum in each iterations
	public static String getSummaryFileName(String samplerId, int chainId){
		return(Constant.DefaultResultOutputFolder+ samplerId + "-chain" + chainId +".gammasum.tmp");
	}
	
	public static String getStatFileName(String samplerId, int chainId)
	{
		String fname = Constant.DefaultResultOutputFolder+samplerId + "-chain" + chainId + ".stat.xls";
		return fname;
	}
	
	public static String getStatFileNameBinary(String samplerId, int chainId)
	{
		String fname = Constant.DefaultResultOutputFolder+
				samplerId + "-chain" + chainId+ ".stat.tmp"; //Constant.ResultStatisticsFileSuffix;
		return fname;
	}
	
	public static String getAllStatFileName(String samplerId)
	{
		String fname = Constant.DefaultResultOutputFolder+samplerId 
				+ "-allchain.stat.xls"; //Constant.ResultStatisticsFileAllChainSuffix;
		return fname;
	}
	
	public static String getFinishFileName(String samplerId){
		String finishFileName = Constant.DefaultResultOutputFolder+samplerId+".finished.txt";
		return finishFileName;
	}
	
    //////////////////////////////////////////////////////////////////
	
	/**
	 * A generic function to print an array
	 * @param array
	 */
	public static <E> void printArray(E[] array)
	{
		for(int i=0;i<array.length;i++)
			System.out.print(" "+array[i]);
		System.out.println();
	}
	
	/**
	 * print 2D array to string (matrix)
	 * @param array
	 * @return
	 */
	public static String Array2DToString(double[][] array)
	{
		final StringBuffer str = new StringBuffer();
		int i=0, j=0;  
        for(i=0;i<array.length;i++){
        	for(j=0;j<array[i].length;j++){
        		 str.append(" "+array[i][j]);
        	}
        	str.append("\n");
        }
        str.append("\n");
        
        return str.toString();
	}
	
	/**
	 * Convert a 2D array to string
	 * @param data
	 * @return
	 */
	public static String toString(double[][] data) {
        StringBuffer buf = new StringBuffer();
        
        //added by Huiping
        buf.append("row num = word num="+data.length+"\n");
        if(data.length>0) buf.append("col num = doc num = "+data[0].length+"\n");
        //end of adding
        
        for (int c0 = 0; c0 < data.length; c0++) {
            double[] row = data[c0];
            buf.append(Arrays.toString(row)).append("\n");
        }
        return buf.toString();
        //return Arrays.deepToString(data);
    }
	
	/**
	 * add the number of elements in one dimension together
	 * E.g., from 2D array
	 * [[1],[2]]
	 * [[3],[4]]
	 * [[5],[6]] dim0size = 3, dim1size=2
	 * sumArrayDim(array,0) should return array with size dim1size 2  
	 * [9=1+3+5, 12=2+4+6]
	 * sumArrayDim(array,1) should return array with size dim1size 3
	 * [3=1+2, 7=3+4, 11=5+6]
	 * @param array
	 * @param dimToSummarize
	 * @return
	 */
	public static double[] sumArrayDim(double [][] array,int dimToSummarize)
	{
		double[] sum = null; 
		int dim0size = array.length;
		int dim1size = array[0].length;
		
		if(dimToSummarize==0) sum = new double[dim1size];
		else 				sum = new double[dim0size];
	
		for(int i=0;i<dim0size;i++){
			for(int j=0;j<dim1size;j++){
				if(dimToSummarize==0) sum[j] += array[i][j];
				else 				sum[i] += array[i][j];
			}
		}
		return sum;
	}
	
	/**
	 * Add the number of the elements inside the array
	 * @param array
	 * @return
	 */
	public static double sumArray(double[] array)
	{
		double sum=0.0;
		for(int i=0;i<array.length;i++)
			sum+=array[i];
		return sum;
	}
	
	/**
	 * randomly generate the the latent state
	 * @param token
	 * @param obj: object
	 * @param freq
	 * @param numLatentState
	 * @return
	 */
	public static int initialLatentState(int token, int object, int freq, int numLatentState) {
        return (int) Math.floor(Math.random() * numLatentState);
    } 
	
	
	public static int initiaInfluencing(int w, int d, int freq, int numCites) {
        return (int) Math.floor(Math.random() * numCites);
    }
	
    public static int initiaInfluencing(int w, int d, int freq, List<Integer> bibliography) {
        assert (!bibliography.isEmpty()) : "d=" + d + " w=" + w;
        int index = (int) Math.floor(Math.random() * bibliography.size());
        return bibliography.get(index);
    }
    
    public static int initiaAspect(int w, int d, int freq, int A){
    	return (int)Math.floor(Math.random() * A);
    }
    
    public static String cut(double value) {
        double tenPower = Math.pow(10, PRECISION);
        double scaledInt = Math.rint(value * tenPower);
        double cuttedVal = scaledInt / tenPower;
        return "" + cuttedVal;
    }

    public static String cut(double value, int precision) {
        double tenPower = Math.pow(10, precision);
        double scaledInt = Math.rint(value * tenPower);
        double cuttedVal = scaledInt / tenPower;
        return "" + cuttedVal;

    }
}
