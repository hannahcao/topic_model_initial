package alfred;

/**
 * 
 * @author Huiping Cao
 * @date Apr. 2, 2012
 */
public class Util{

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
}
