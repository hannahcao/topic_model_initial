package cao;

/**
 * Not used, defined for object pairs
 * 
 * @author hcao
 *
 */
public class ObjPair implements Comparable<ObjPair>{

	int objInfluenced;
	int objInfluencing;
	
	@Override
	public int compareTo(ObjPair arg0) {
		
		if(objInfluenced<arg0.objInfluenced){
			return (-1);
		}else if (objInfluenced > arg0.objInfluenced){
			return 1;
		}else{
			if(objInfluencing < arg0.objInfluencing) return (-1);
			else if (objInfluencing > arg0.objInfluencing) return (1);
			else return 0; 
		}
	}

	public String toString(){
		return "("+objInfluenced+" <- " +objInfluencing+")"; 
	}
}
