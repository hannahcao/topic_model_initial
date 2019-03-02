package cao;

/**
 * The element used to keep the sample for one influenced (citing)
 * object's one position (s) 
 * 
 * @author administrator
 *
 */
public class SampleElementInfluencedOAIM implements Comparable<SampleElementInfluencedOAIM> {
	
	protected int obj; //d
	protected int token; //w
	protected int ta; //ta
	protected int z; //t
	protected int b; //binary value, s
	protected int oprime; //c
	
	public SampleElementInfluencedOAIM(){;}
	public SampleElementInfluencedOAIM(int _obj, int _t, int _ta, int _z, int _b, int _oprime)
	{
		obj = _obj;
		token = _t;
		z = _z;
		b = _b;
		ta = _ta;
		oprime = _oprime; //this is only useful when b = 1-contant.INNOVATION
	}
	
	@Override
	public int compareTo(SampleElementInfluencedOAIM arg0) {
		
		if(obj < arg0.obj) return (-1);
		else if (obj > arg0.obj) return (1);
		else{
			if(ta < arg0.ta)	return (-1);
			else if(ta > arg0.ta) return 1;
			else{
				if(token < arg0.token) return (-1);
				else if (token > arg0.token) return 1;
				else{
					if (z < arg0.z) return (-1);
					else if (z > arg0.z) return 1;
					else {
						if( b < arg0.b) return (-1);
						else if (b > arg0.b) return 1;
						else {
								if(oprime < arg0.oprime) return (-1);
								else if (oprime > arg0.oprime) return (1);
								else return 0;
						}
					}
				}
			}
		}
	}
	
	public String toString()
	{
		String str = "[o"+obj+"][t"+token+"][ta"+ta+"][z"+z+"]"+"[b"+b+"[op"+oprime+"]";
		return str;
	}
	
	/**
	 * @return the obj
	 */
	public int getObj() {
		return obj;
	}

	/**
	 * @param obj the obj to set
	 */
	public void setObj(int obj) {
		this.obj = obj;
	}

	/**
	 * @return the token
	 */
	public int getToken() {
		return token;
	}

	/**
	 * @param token the token to set
	 */
	public void setToken(int token) {
		this.token = token;
	}

	/**
	 * @return the z
	 */
	public int getZ() {
		return z;
	}

	/**
	 * @param z the z to set
	 */
	public void setZ(int z) {
		this.z = z;
	}

	/**
	 * @return the b
	 */
	public int getB() {
		return b;
	}

	/**
	 * @param b the b to set
	 */
	public void setB(int b) {
		this.b = b;
	}

	/**
	 * @return the oprime
	 */
	public int getOprime() {
		return oprime;
	}

	/**
	 * @param oprime the oprime to set
	 */
	public void setOprime(int oprime) {
		this.oprime = oprime;
	}
	public int getTa() {
		return ta;
	}
	public void setTa(int ta) {
		this.ta = ta;
	}


}
