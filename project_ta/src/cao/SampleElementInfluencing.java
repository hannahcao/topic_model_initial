package cao;

/**
 * The element used to keep the sample for one influencing object's one position (s) 
 * influencing = cited
 * 
 * @author administrator
 *
 */
public class SampleElementInfluencing implements Comparable<SampleElementInfluencing>{
	protected int obj =0; //d
	protected int token =0; //w
	protected int z=0; //t
	
	public SampleElementInfluencing(){;}
	
	public SampleElementInfluencing(int _o, int _t, int _z)
	{
		obj = _o;
		token = _t;
		z = _z;
	}
	
	@Override
	public int compareTo(SampleElementInfluencing arg0) {
		if(token < arg0.token) return (-1);
		else if (token > arg0.token) return 1;
		else{
			if(obj < arg0.obj) return (-1);
			else if (obj > arg0.obj) return (1);
			else{
				if (z < arg0.z) return (-1);
				else if (z > arg0.z) return 1;
				else return 0;
			}
		}
	}
	
	public String toString()
	{
		String str = "[o"+obj+"][t"+token+"][z"+z+"]";
		return str;
	}
}
