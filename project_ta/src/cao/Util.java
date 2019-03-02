package cao;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.json.JSONArray;

/**
 * 
 * @author Huiping Cao
 * @date Apr. 2, 2012
 */
public class Util{
	private static final int PRECISION = 2;
	
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
	
	public static int initiaInfluencing(int w, int d, int freq, List<Integer> bibliography) {
        assert (!bibliography.isEmpty()) : "d=" + d + " w=" + w;
        int index = (int) Math.floor(Math.random() * bibliography.size());
        
        return bibliography.get(index);
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
    
    public static void main(String args[]){
    	double a = Math.random();
    	int b = 4;
    	System.out.println((int)Math.floor(a*b));
    	
    	JSONArray arr = new JSONArray();
    	arr.put(1);
    	arr.put(2);
    	
    	System.out.println(arr.get(0));
    }
    /**
     * update 3 dimension Hash map
     * @param map
     * @param key1
     * @param key2
     * @param key3
     * @param value
     */
    public static void update3HashMap(Map<Integer, Map<Integer, Map<Integer, Double>>> map, int key1, int key2, int key3, double value){
    	if(value==0 && get3Map(map, key1, key2, key3)==0)
    		return ;
    	
    	Map<Integer, Map<Integer, Double>> map1 = map.get(key1);
    	if(map1==null){
    		map1 = new HashMap<Integer, Map<Integer, Double>>();//
    		map.put(key1, map1);
    	}
    	Map<Integer, Double> map2 = map1.get(key2);
    	if(map2==null){
    		map2 = new HashMap<Integer, Double>();
    		map1.put(key2, map2);
    	}
    	map2.put(key3, value);
    }
    
    /**
     * update 3 dimension Hash map
     * @param map
     * @param key1
     * @param key2
     * @param key3
     * @param value
     */
    public static void update3TreeMap(Map<Integer, Map<Integer, Map<Integer, Double>>> map, int key1, int key2, int key3, double value){
    	if(value==0 && get3Map(map, key1, key2, key3)==0)
    		return ;
    	
    	Map<Integer, Map<Integer, Double>> map1 = map.get(key1);
    	if(map1==null){
    		map1 = new TreeMap<Integer, Map<Integer, Double>>();//
    		map.put(key1, map1);
    	}
    	Map<Integer, Double> map2 = map1.get(key2);
    	if(map2==null){
    		map2 = new TreeMap<Integer, Double>();
    		map1.put(key2, map2);
    	}
    	map2.put(key3, value);
    }
    /**
     * read value from 3 dimension map
     * @param map
     * @param key1
     * @param key2
     * @param key3
     * @return
     */
    public static double get3Map(Map<Integer, Map<Integer, Map<Integer, Double>>> map, int key1, int key2, int key3){
    	Map<Integer, Map<Integer, Double>> map1 = map.get(key1);
    	if(map1==null)
    		return 0;
    	else{
    		Map<Integer, Double> map2 = map1.get(key2);
    		if(map2==null)
    			return 0;
    		else{
    			Double value = map2.get(key3);
    			if(value==null)
    				return 0;
    			else
    				return value;
    		}
    	}
    }
    
    /**
     * update 2 dimension map
     * @param map
     * @param key1
     * @param key2
     * @param value
     */
    public static void update2Map(Map<Integer, Map<Integer, Double>> map, int key1, int key2,  double value){
    	if(value==0 && get2Map(map, key1, key2)==0)
    		return;
    	
    	Map<Integer, Double> map1 = map.get(key1);
    	if(map1==null){
    		map1 = new HashMap<Integer, Double>();//
    		map.put(key1, map1);
    	}
    	map1.put(key2, value);
    }
    /**
     * read value from 2 dimension map
     * @param map
     * @param key1
     * @param key2
     * @return
     */
    public static double get2Map(Map<Integer, Map<Integer, Double>> map, int key1, int key2){
    	Map<Integer, Double> map1 = map.get(key1);
    	if(map1==null)
    		return 0;
    	else{
    		Double value = map1.get(key2);
    		if(value==null)
    			return 0;
    		else{
    			return value;
    		}
    	}
    }
    /**
     * print 2 dimension map to screeen
     * @param map
     */
    public static void print2Map(Map<Integer, Map<Integer, Double>> map){
    	for(Map.Entry<Integer, Map<Integer, Double>> entry : map.entrySet())
    		for(Map.Entry<Integer, Double> entry2 : entry.getValue().entrySet())
    			System.out.println(entry.getKey()+"\t"+entry2.getKey()+"\t"+entry2.getValue());
    	
    }
    
}
