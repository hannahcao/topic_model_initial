package cao;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * 
 * Influencing objects: that have the potential to influence others
 * Influenced objects: that have the potential to be influenced by others
 * 
 * Example: o1->o2, o2->o3, o2->o4
 * Influencing objects: o1,o2
 * Influenced objects: o2,o3,o4
 * 
 * @author Huiping Cao
 */
public class SampleData {
	CmdOption cmdOption;
	
	//TODO change to map
//	double [][] in_influencing_wd; 	//cited: influencing  T*D
//	double [][] in_influenced_wd; 	//citing: influenced  T*D
	
	Map<Integer, Map<Integer, Double>> in_influencing_wd;
	Map<Integer, Map<Integer, Double>> in_influenced_wd; 
	Map<Integer, Map<Integer, Map<Integer, Double>>> in_influenced_wTaD; //citing: influenced  T*TA*D
	 
	Set<Integer> testSet; //citing:influenced test set
	
	ArrayList<List<Integer>> in_refPubIndexIdList; // citing graph
//    List<Integer> allPaper;//set of all paper
	
	int numTokens;
	int numTa;
	//sample variables
	//private TreeMap<SampleElementInfluencing,Integer> sampleInfluencing_otz_map;
	private ArrayList<SampleElementInfluencing> sampleInfluencing_otz;
	
	//private TreeMap<SampleElementInfluenced,Integer> sampleInfluenced_otzbop_map;
	private ArrayList<SampleElementInfluencedOAIM> sampleInfluenced_otzbtaop;
	
	private ArrayList<SampleElementInfluencedLAIM> sampleInfluenced_otzbataop;
	
	//the counts are declared to be double for the purpose of averaging the sample counts in multiple iterations
    public Map<Integer, Map<Integer, Double>> N_zt_all;// = new int[in_topicNum][numTokens];//wtAll,, N_{z,t}
    public double[]  N_z_all; 
    
    Map<Integer, Map<Integer, Double>> N_oz_influencing; //N'_{o,z}, tdCited, used in drawing sample for influencing objects
    double[]   N_o_influencing;// = new int[numInfluencingDocs]; //N'_{o}? ,dCited?
    public Map<Integer, Map<Integer, Double>> NN_opz_bold_influencing;// = new int[numInfluencingDocs][in_topicNum];//N'_{o',z,b}=NN'_{o',z} where b=1, tcs0Citing
    public double[]   NN_op_bold_influencing;// = new int[numInfluencingDocs];//N'_{o',b=1} = NN'_{o'}. cs0Citing
    
    Map<Integer, Map<Integer, Double>> N_oz_bnew_influenced;// = new int[numInfluencedDocs][in_topicNum]; //N_{o,z,b} where b=0, tds1Citing
    Map<Integer, Map<Integer, Double>> N_ob_influenced;// = new int[numInfluencedDocs][2];//N_{o,b}, ds1Citing(b=0),ds0Citing(b=1)
    Map<Integer, Map<Integer, Double>> N_oop_bold_influenced;// = new int[numInfluencedDocs][numInfluencingDocs];////N_{o,o'}=N_{o,o',b=1} where b=1, dcs0Citing
    double[]   N_o_influenced; //= new int[numInfluencedDocs]; dCiting //N_{o} ?
    
    //OAIM specific structure
    Map<Integer, Map<Integer, Map<Integer, Double>>> N_otaop_influenced;// D*Ta*D	N_{o,ta,o',b=1}
    Map<Integer, Map<Integer, Double>> N_ota_influenced;// D*A	N_{o,ta,b=1}
    
    //LAIM specific structure
    Map<Integer, Map<Integer, Map<Integer, Double>>> N_oaop_influenced; // D*A*D	N_{o,a,o',b=1}
    Map<Integer, Map<Integer, Double>> N_oa_influenced;// D*A	N_{o,a,b=1}
    Map<Integer, Map<Integer, Double>> N_ata_influenced;//A*TA	N_{o,a,ta,b=1}
    double[] N_a_all; //A, N_a

    public SampleData(final  Map<Integer, Map<Integer, Double>>influencing_wd , 
    		final Map<Integer, Map<Integer, Double>>influenced_wd, 
    		final Map<Integer, Map<Integer, Map<Integer, Double>>>in_influenced_wtad,
			int numLatentStates, int maxnumBibliographListLength, 
			ArrayList<List<Integer>> bibliographIndexidList,
			CmdOption _option, Set<Integer> testSet)
	{
		this.cmdOption = _option;
		this.in_influencing_wd = influencing_wd;
		this.in_influenced_wd = influenced_wd;
		this.in_influenced_wTaD = in_influenced_wtad;
		this.in_refPubIndexIdList = bibliographIndexidList;
		this.testSet = testSet;
		
		//The length of token vector in both influencing and influenced objects should be the same
//		if(in_influencing_wd.length!=in_influenced_wd.length){
//			throw new IllegalArgumentException("g_influenceing_wd.length!=g_influenced_wd.length");
//		}
		
		init();
	}
    
    /**
	 * Initialize the data structure for Gibbs sampling
	 */
	private void init()
	{
		numTokens = Constant.tokenNum;
		numTa = Constant.aspectNum;
		
		////////////////////////////////////////
		//1. token count for influencing and influenced documents
		int numInfluencingObjects = Constant.oprimeNum;
		int numInfluencedObjects = Constant.oNum;
        
        System.out.println(Debugger.getCallerPosition()+" num of tokens "+numTokens+" num of Ta "+numTa+" num of obj "
        +numInfluencedObjects+" num of oprime "+numInfluencingObjects);
        
        this.sampleInfluencing_otz = new ArrayList<SampleElementInfluencing>(); 
        this.sampleInfluenced_otzbtaop = new ArrayList<SampleElementInfluencedOAIM>(); 
        this.sampleInfluenced_otzbataop = new ArrayList<SampleElementInfluencedLAIM>();
        
        /////////////////////////////////////////////////////////////
        
        //3. Initialize sampling counts
        //Sampling counts
        N_zt_all = new HashMap<Integer, Map<Integer, Double>>();//wtAll,, N_{z,t}
        N_z_all = new double[cmdOption.znum]; //tAll, N_{z}
        ///////////////////////////////
        //for influencing (or cited object) o
        N_oz_influencing =  new HashMap<Integer, Map<Integer, Double>>(); //N'_{o,z}, tdCited
        N_o_influencing = new double[numInfluencingObjects]; //N'_{o}
        
        //o is used as an influencing object to generate latent state for other objects
        //this is used only when b=1
        NN_opz_bold_influencing =  new HashMap<Integer, Map<Integer, Double>>();//N'_{o',z,b}=NN_{o',z} where b=1, tcs0Citing
        NN_op_bold_influencing = new double[numInfluencingObjects];
        ///////////////////////////////
        
        //for influenced (or citing object) o
        N_oop_bold_influenced =  new HashMap<Integer, Map<Integer, Double>>();//N_{o,o'}=N_{o,o',b=1} where b=1, dcs0Citing
        N_ob_influenced =  new HashMap<Integer, Map<Integer, Double>>();//N_{o,b}, ds1Citing(b=0),ds0Citing(b=1)
        N_o_influenced = new double[numInfluencedObjects]; //N_{o}, dCiting
        N_oz_bnew_influenced =  new HashMap<Integer, Map<Integer, Double>>(); //N_{o,z,b=0} where b=0, tds1Citing
        
        //OAIM
        N_otaop_influenced = new HashMap<Integer, Map<Integer, Map<Integer, Double>>>();
        N_ota_influenced =  new HashMap<Integer, Map<Integer, Double>>();
        
        //LAIM
        N_oaop_influenced = new HashMap<Integer, Map<Integer, Map<Integer, Double>>>();
        N_oa_influenced = new HashMap<Integer, Map<Integer, Double>>();
        N_ata_influenced = new HashMap<Integer, Map<Integer, Double>>();
        N_a_all = new double[cmdOption.anum];
        
//		System.out.println(Debugger.getCallerPosition()+" Size of all paper set: "+allPaper.size());
	}

	/**
	  * Get total number of tokens in the input data collection
	  * @return the total number of tokens in the input data collection
	  */
	 public int getNumTokens() {
		 return numTokens;
	 }
	
	/**
	 * For object "obj", get the number of objects that influece it.
	 * @param obj
	 * @return he number of objects that influece the given object "obj".
	 */
	 public int getBibliographySize(int obj) {//getBibSize
		 return in_refPubIndexIdList.get(obj).size();
	 }
	 
	 public ArrayList<SampleElementInfluencedOAIM> getSampleInfluenced_otzbtaop() {
			return sampleInfluenced_otzbtaop;
	 }
	
	 /**
	 * Draw the initial sample for influencing and influenced objects
	 * @param trainOrTest true: training; false: test
	 */
	public void drawInitialSample(boolean trainOrTest)
	{
		drawIntitialSampleInfluencing(trainOrTest);
		drawIntitialSampleInfluenced(trainOrTest);
	}
	
	/**
	 * Draw initial samples for influencing objects
	 * @param trainOrTest true: training; false: test
	 */
	private void drawIntitialSampleInfluencing(boolean trainOrTest)
	{
//		assert(numTokens==in_influencing_wd.length); //cited_wd
		
		for (int token = 0; token < numTokens; token++) {//w
			for (int obj = 0; obj < Constant.oprimeNum; obj++) {//d    
				if(testSet.contains(obj)&&trainOrTest)//in training step and obj in testSet. Continue
					continue;
				if(!testSet.contains(obj)&&!trainOrTest)//in test step and obj is not in testSet.  Continue
					continue;
				
				//Get the number of tokens in object "obj"
				final double tokenCount = Util.get2Map(in_influencing_wd, token, obj);
				
				//when this position does not have any token
				//i.e., this token does not appear in this object?
				if (tokenCount == 0) continue; 

				//token occurs "val" times in the profile of object "obj"  
				for (int occ = 0; occ < tokenCount; occ++) {
					int newZ = cao.Util.initialLatentState(token, obj, 1, cmdOption.znum);

					//1. update sample matrix
					SampleElementInfluencing e = new SampleElementInfluencing(obj,token,newZ);
					getSampleInfluencing_otz().add(e);

					//2.add the sample for [obj],[token],[latent_state] drawing
					updCountInfluencing_otz(obj,token,newZ,1);
				}
			}
        }
		System.out.println(Debugger.getCallerPosition()+" influencing sample size "+this.getSampleInfluencing_otz().size());
	}
	
	//add A's related count here!!!!!!!!
	/**
	 * draw initial sample for influenced documents.
	 * @param trainOrTest true: training; false: test
	 */
	private void drawIntitialSampleInfluenced(boolean trainOrTest)
	{
		//TODO change to iteration over map entry
//		assert(numTokens==in_influenced_wd.length); //cited_wd
			for (int token = 0; token < numTokens; token++) {//w
				for(int ta=0; ta < numTa; ta++){
					for (int obj = 0; obj < Constant.oNum; obj++) {//d
						if(testSet.contains(obj)&&trainOrTest)//in training step and obj in testSet. Continue
							continue;
						if(!testSet.contains(obj)&&!trainOrTest)//in test step and obj is not in testSet.  Continue
							continue;

						final Double tokenCount = Util.get3Map(in_influenced_wTaD, token, ta, obj);

						for (int occ = 0; occ < tokenCount; occ++) {
							
							int newZ = cao.Util.initialLatentState(token, obj, 1, cmdOption.znum);
							int newB = cao.Util.initialLatentState(token, obj, 1, 2);
							int newOprime = -1;
							int newA = -1;
							if (newB!=Constant.INNOTVATION){
								//							draw initial Oprime from its reference
								newOprime = cao.Util.initiaInfluencing(token, obj, 1, in_refPubIndexIdList.get(obj));
								newA = cao.Util.initialLatentState(token, obj, 1, cmdOption.anum);
							}
							
							if(cmdOption.model.equals(Constant.oaim)){//oaim
								SampleElementInfluencedOAIM e = new SampleElementInfluencedOAIM(obj,token, ta, newZ,newB, newOprime);
								getSampleInfluenced_otzbtaop().add(e);
								updCountInfluenced_ottazbaop(obj,token, ta, newZ,newB, newA, newOprime, 1);
							}
							else{//laim
								SampleElementInfluencedLAIM e = new SampleElementInfluencedLAIM(obj,token, ta, newZ,newB, newA, newOprime);
								getSampleInfluenced_otzbataop().add(e);
								updCountInfluenced_ottazbaop(obj,token, ta, newZ,newB, newA, newOprime, 1);
							}
						}
					}
				}
			}
			if(cmdOption.model.equals(Constant.oaim))
				System.out.println(Debugger.getCallerPosition()+" influenced sample size "+getSampleInfluenced_otzbtaop().size());
			else
				System.out.println(Debugger.getCallerPosition()+" influenced sample size "+getSampleInfluenced_otzbataop().size());
				
	}
	
	/**
	 * Update the sample for object, for its token, latent state, with the new frequency
	 * 
	 * @param obj: the given object
	 * @param token: the given object's token
	 * @param z: the latent state assigned to the given object's given token
	 * @param freq: new frequency (positive means add count, negative means reduce count)
	 */
	private void updCountInfluencing_otz(int obj, int token, int z, int freq)
	{
		//update counts
		double oldFreq = Util.get2Map(N_zt_all, z, token);
		Util.update2Map(N_zt_all, z, token, oldFreq+freq);
//	    N_zt_all[z][token] +=freq;
		N_z_all[z]+=freq;
		
		//total counts
		oldFreq = Util.get2Map(N_oz_influencing, obj, z);
		Util.update2Map(N_oz_influencing, obj, z, oldFreq+freq);
//		N_oz_influencing[obj][z] +=freq;
		N_o_influencing[obj] +=freq;
	}
	
	/**
	 * update the count of influenced document for obj's token
	 * @param obj
	 * @param token
	 * @param ta
	 * @param z
	 * @param b
	 * @param a
	 * @param oprime
	 * @param value
	 */
	private void updCountInfluenced_ottazbaop(int obj, int token, int ta, int z, int b, int a, int oprime, int value)
	{//add aspect's related count here!!!!!!!!!!!
		double oldValue;
		if(b==Constant.INHERITANCE){ //b=0; use the inherited data
			oldValue = Util.get2Map(N_oop_bold_influenced, obj, oprime);
			Util.update2Map(N_oop_bold_influenced, obj, oprime, oldValue+value);
			
			oldValue = Util.get2Map(NN_opz_bold_influencing, oprime, z);
			Util.update2Map(NN_opz_bold_influencing, oprime, z, oldValue+value);
			NN_op_bold_influencing[oprime] +=value;	//o'

			if(cmdOption.model.equals(Constant.oaim)){//oaim
				oldValue = Util.get2Map(N_ota_influenced, obj, ta);
				Util.update2Map(N_ota_influenced, obj, ta, oldValue+value);

				oldValue = Util.get3Map(N_otaop_influenced, obj, ta, oprime);
				Util.update3HashMap(N_otaop_influenced, obj, ta, oprime, oldValue+value);
			}
			else{//laim
				oldValue = Util.get2Map(N_oa_influenced, obj, a);
				Util.update2Map(N_oa_influenced, obj, a, oldValue+value);
				
				oldValue = Util.get3Map(N_oaop_influenced, obj, a, oprime);
				Util.update3HashMap(N_oaop_influenced, obj, a, oprime, oldValue+value);
				
				N_a_all[a]+=value;
			
				oldValue = Util.get2Map(N_ata_influenced, a, ta);
				Util.update2Map(N_ata_influenced, a, ta, oldValue+value);
			}
			
		}else{//b=1 innovative
			oldValue = Util.get2Map(N_oz_bnew_influenced, obj, z);
			Util.update2Map(N_oz_bnew_influenced, obj, z, oldValue+value);
		}
		oldValue = Util.get2Map(N_ob_influenced, obj, b);
		Util.update2Map(N_ob_influenced, obj, b, oldValue+value);
		
		N_o_influenced[obj] +=value;
		
		//total counts
		oldValue = Util.get2Map(N_zt_all, z, token);
		Util.update2Map(N_zt_all, z, token, oldValue+value);
		N_z_all[z] +=value;
	}
	
	
	
	public void checkSampleCountConsistency()
	{
		int tmpsum =0 ; 
		
		//1. total N_z_all[i] = \sum_j N_zt_all[i][j]
		for(int z=0;z<cmdOption.znum;z++){
			tmpsum = 0;
			for(int t=0;t<Constant.tokenNum;t++)tmpsum += Util.get2Map(N_zt_all, z, t);
			assert(tmpsum!=N_z_all[z]):"ERROR N_z_all["+z+"]";
		}
		
		//2. influenced: N_o_influenced[i] = \sum_b N_ob_influenced[i][b]
		for(int obj=0;obj<N_o_influenced.length;obj++){
			tmpsum = 0;
			for(int b=0;b<2;b++)
				tmpsum+=Util.get2Map(N_ob_influenced, obj, b);
			assert(tmpsum!=N_o_influenced[obj]):"ERROR N_o_influenced["+obj+"]";
		}
		
		//3. influenced b0 and b1
		//get b0sample sum and b1 sample sum
		int[] bsum = new int[2];
		bsum[0] = bsum[1] = 0;
		for(int b=0;b<=1;b++){
			for(int obj=0;obj<Constant.oNum;obj++){
				bsum[b]+= Util.get2Map(N_ob_influenced, obj, b);
			}
		}
		
		//4. influenced b innovative
		//N_oz_bnew_influenced sum should be equal to bsum[innovative]
		tmpsum=0;
		for(int obj=0;obj<Constant.oNum;obj++){
			for(int z=0;z<cmdOption.znum;z++)
				tmpsum+=Util.get2Map(N_oz_bnew_influenced, obj, z);
		}
		assert(tmpsum!=bsum[Constant.INNOTVATION]):"bsum["+Constant.INNOTVATION+"]!="+tmpsum;
		
		//5. influenced b inheritance
		tmpsum=0;
		//NN_op_bold_influencing sum should be equal to bsum[inheritance]
		for(int op=0;op<NN_op_bold_influencing.length;op++){
			tmpsum+=NN_op_bold_influencing[op];
		}	
		assert(tmpsum!=bsum[Constant.INHERITANCE]):"bsum["+Constant.INHERITANCE+"]!="+tmpsum;
		
		//6. influenced N_ob_influenced and N_oop_bold_influenced
		for(int obj=0;obj<Constant.oNum;obj++){
			tmpsum = 0; 
			for(int op=0; op<Constant.oprimeNum;op++){
				tmpsum+=Util.get2Map(N_oop_bold_influenced, obj, op);
			}
			assert(tmpsum!=Util.get2Map(N_ob_influenced, obj, Constant.INHERITANCE)):
				"N_ob_influenced["+obj+"]["+Constant.INHERITANCE+"]!="+tmpsum;
		}
	
		//7. influenced NN_op_bold_influencing and NN_opz_bold_influencing
		for(int op=0;op<NN_op_bold_influencing.length;op++){
			tmpsum=0;
			for(int z=0;z<cmdOption.znum;z++){
				tmpsum+=Util.get2Map(NN_opz_bold_influencing, op, z);
			}
			assert(tmpsum!=NN_op_bold_influencing[op]):
				"NN_op_bold_influencing["+op+"]!="+tmpsum;
		}
		
		for(int obj=0; obj<Constant.oNum; obj++){
			int temSum = 0;
			for(int ta=0; ta<numTa; ta++){
				tmpsum = 0;
				for(int op=0; op<NN_op_bold_influencing.length; op++){
//					tmpsum+=this.N_otaop_influenced[obj][ta][op];
					tmpsum+=Util.get3Map(N_otaop_influenced, obj, ta, op);
				}
				assert(tmpsum!=Util.get2Map(N_ota_influenced, obj, ta));
				temSum+=tmpsum;
			}
			assert(temSum!=this.N_o_influenced[obj]);
		}
		
		//System.out.println(Debugger.getCallerPosition()+"Counts are CONSISTENT.");
	}
	
	/**
	 * Draw sample for one iteration
	 */
	public void drawOneIterationSample(boolean trainOrTest) {
    	drawOneIterationInfluencingSample(trainOrTest);
    	drawOneIterationInfluencedSample(trainOrTest);
    	
    	//check the consistency of the sample counts
//		this.checkSampleCountConsistency();
    }
	
	/**
	 * Draw samples for one influencing document, update related counts
	 * Tested, checked the first 10 element's update, correct
	 */
	private void drawOneIterationInfluencingSample(boolean trainOrTest) 
	{
		//put all the newly added element to this list
		//ArrayList<SampleElementInfluencingCount> newEList = new ArrayList<SampleElementInfluencingCount>();
		for(SampleElementInfluencing e : getSampleInfluencing_otz())
		{
//			System.out.println(Debugger.getCallerPosition()+" influencing size "+getSampleInfluencing_otz().size());
//			Date d1 = new Date();
			//1. get old sample, update the sample matrix and counts
			//e.updateCount(-1); 
			updCountInfluencing_otz(e.obj, e.token, e.z, -1); //update count
			//System.out.println(Debugger.getCallerPosition()+"[1]e="+e);

			//2. sample new latent state
			int newZ = Probability.getMAPInfluencing_z(cmdOption, this, e.obj, e.token, trainOrTest);//getMAPCiting_zbop
			e.z = newZ;
			updCountInfluencing_otz(e.obj, e.token, e.z, +1); //update count
			
			//TODO remove this later
//			Date d2 = new Date();
//			System.out.println(Debugger.getCallerPosition()+" influencing sample time "+(d2.getTime()-d1.getTime()));
//			break;
		}
	}
	
		/**
	 * Draw samples for one influenced document, update related counts
	 */
	private void drawOneIterationInfluencedSample(boolean trainOrTest) {
		if(cmdOption.model.equals(Constant.oaim))//oaim
			for(SampleElementInfluencedOAIM e : this.getSampleInfluenced_otzbtaop()){
				//get the old sample for object e (with old obj, old token, old z, old oprime, old count)
				//decrease its count
				updCountInfluenced_ottazbaop(e.obj, e.token, e.ta, e.z, e.b, -1, e.oprime, -1);

				//			draw use step by step Gibbs sampling
				int[] new_zbop = Probability.getMAPInfluenced_zbaop(cmdOption, this, e.obj, e.token, e.ta, trainOrTest);
				int newZ = new_zbop[0];
				int newB = new_zbop[1];
				int newOprime = new_zbop[2];
				e.z = newZ;
				e.b = newB;
				e.oprime = newOprime;

				//System.out.println(Debugger.getCallerPosition()+"[4]e="+e);
				updCountInfluenced_ottazbaop(e.obj, e.token, e.ta, e.z, e.b, -1, e.oprime, 1);
			}
		else//laim
			for(SampleElementInfluencedLAIM e : this.getSampleInfluenced_otzbataop()){
				//get the old sample for object e (with old obj, old token, old z, old oprime, old count)
				//decrease its count
				updCountInfluenced_ottazbaop(e.obj, e.token, e.ta, e.z, e.b, e.a, e.oprime, -1);

				//			draw use step by step Gibbs sampling
				int[] new_zbop = Probability.getMAPInfluenced_zbaop(cmdOption, this, e.obj, e.token, e.ta, trainOrTest);
				int newZ = new_zbop[0];
				int newB = new_zbop[1];
				int newOprime = new_zbop[2];
				int newA = new_zbop[3];
				e.z = newZ;
				e.b = newB;
				e.oprime = newOprime;
				e.a = newA;
				//System.out.println(Debugger.getCallerPosition()+"[4]e="+e);
				updCountInfluenced_ottazbaop(e.obj, e.token, e.ta, e.z, e.b, e.a, e.oprime, 1);
			}
	}
	
    public String toString() {
    	final StringBuffer str = new StringBuffer();

    	str.append(Debugger.getCallerPosition()+"****Sample**** \n");
        str.append(Debugger.getCallerPosition()+"InfluencingSample[o][t][z]=frequency; size=" +getSampleInfluencing_otz().size()
        		+":" + getSampleInfluencing_otz());
        str.append(Debugger.getCallerPosition()+"\nInfluencedSample:[o][t][z][b][o']=frequency; size="
        		+getSampleInfluenced_otzbtaop().size()+":"+ getSampleInfluenced_otzbtaop());
        //str.append(Debugger.getCallerPosition()+"\nInfluencedSample:sampleInfluenced_otzbop_map, size="
        //		+sampleInfluenced_otzbop_map.size()+":"+sampleInfluenced_otzbop_map);
        //TODO later on change this
//        str.append(Debugger.getCallerPosition()+"\n****Total counts**** \n");
//        str.append(Debugger.getCallerPosition()+"[t1] N_zt_all: \n"+Util.Array2DToString(N_zt_all));//z,t
//        str.append(Debugger.getCallerPosition()+"[t2] N_z_all: \n"+Arrays.toString(N_z_all)+"\n");//z,t
//        
//        str.append(Debugger.getCallerPosition()+"\n****Influencing Counts****\n");
//        str.append(Debugger.getCallerPosition()+"[inf1] N_oz_influencing:\n"+Util.Array2DToString(N_oz_influencing));//o,z
//        str.append(Debugger.getCallerPosition()+"[inf2] N_o_influencing:\n"+Arrays.toString(N_o_influencing)+"\n");//o,z
//        str.append(Debugger.getCallerPosition()+"[inf3] NN_opz_bold_influencing:\n"+Util.Array2DToString(NN_opz_bold_influencing));//N'_{o',z,b}=NN_{o',z} where b=1, tcs0Citing
//        str.append(Debugger.getCallerPosition()+"[inf4] NN_op_bold_influencing:\n"+Arrays.toString(NN_op_bold_influencing)+"\n");
//        ///////////////////////////////
//        
//        str.append(Debugger.getCallerPosition()+"****Influenced counts**** \n");
//        str.append(Debugger.getCallerPosition()+"[Infed1] N_o_influenced:\n"+Arrays.toString(N_o_influenced)+"\n");
//        str.append(Debugger.getCallerPosition()+"[Infed2] N_ob_influenced=\n"+Util.Array2DToString(N_ob_influenced));//o,b
//        str.append(Debugger.getCallerPosition()+"[Infed3] N_oop_bold_influenced=\n"+Util.Array2DToString(N_oop_bold_influenced));//o,op, when b=1
//        str.append(Debugger.getCallerPosition()+"[Infed4] N_oz_bnew_influenced=\n"+Util.Array2DToString(N_oz_bnew_influenced));//o,op, when b=1
        
        return str.toString();
    }
    
    /**
     * 
     * @return the number of influenced objects
     */
    public int getNumInfluencedObjects() {
//    	if(in_influencing_wd.length==0) return 0;
//    	else return in_influencing_wd[0].length;
    	return Constant.oNum;
    }
    
    
    /**
     * Add one iteration's sample count to the average sample counts, which will be averaged later.
     * 
     * Debugged & correct
     * @param sampleOneIteration
     * @param numIter
     */
    public void sumSampleCount(SampleData sampleOneIteration, int numIter){
    	
    	//two total counts
    	//newavg = newSum/numIter
		//newSum = oldSum + this_iteration_value
		//oldSum = old avg value * oldNumIter
    	
    	//tested correct (initial sum and later adding)
		for(int z=0;z<cmdOption.znum;z++){ //debugged & correct
    		for(int token=0;token<numTokens;token++){
    			//System.out.println(Debugger.getCallerPosition()+"z="+z+",token="+token+",old value="+N_zt_all[z][token]+",newvalue="+sampleOneIteration.N_zt_all[z][token]);
//    			N_zt_all[z][token] += sampleOneIteration.N_zt_all[z][token];
    			double selfValue = Util.get2Map(N_zt_all, z, token);
    			double otherValue = Util.get2Map(sampleOneIteration.N_zt_all, z, token);
    			Util.update2Map(N_zt_all, z, token, selfValue+otherValue);
    			//System.out.println(Debugger.getCallerPosition()+"updated value="+N_zt_all[z][token]);
    		}
    		//System.out.println(Debugger.getCallerPosition()+"z="+z+",old value="+N_z_all[z]+",newvalue="+sampleOneIteration.N_z_all[z]);
    		N_z_all[z] += sampleOneIteration.N_z_all[z];
    		//System.out.println(Debugger.getCallerPosition()+"updated value="+N_z_all[z]);
    	}
    	
    	sumInfluencingSampleCount(sampleOneIteration,numIter); //Debugged & correct
    	sumInfluencedSampleCount(sampleOneIteration,numIter);  //debugged & correct
    	
//    	this.checkSampleCountConsistency();
    }
    
    /**
     * Summarize the counts for influenced objects
     * 
     * Debugged & correct
     * @param sampleOneIteration
     * @param numIter
     */
    private void sumInfluencedSampleCount(SampleData sampleOneIteration,int numIter)
    {
    	//for influenced (or citing object) 
    	int numInfluencingObjects = Constant.oprimeNum;
    	int numInfluencedObjects = Constant.oNum;
    	
    	//N_oop_bold_influenced = new int[numInfluencedObjects][numInfluencingObjects];//N_{o,o'}=N_{o,o',b=1} where b=1, dcs0Citing
        //N_ob_influenced = new int[numInfluencedObjects][2];//N_{o,b}, ds1Citing(b=0),ds0Citing(b=1)
        //N_o_influenced = new int[numInfluencedObjects]; //N_{o}, dCiting
    	//N_oz_bnew_influenced = new int[numInfluencedObjects][in_numLatentState];
    	for(int objIdx=0; objIdx<numInfluencedObjects; objIdx++){
    		for(int opIdx=0; opIdx<numInfluencingObjects; opIdx++){//correct
    			//System.out.println(Debugger.getCallerPosition()+"o="+objIdx+",op="+opIdx+",new value="+sampleOneIteration.N_oop_bold_influenced[objIdx][opIdx]+",oldvalue="+N_oop_bold_influenced[objIdx][opIdx]);
//    			N_oop_bold_influenced[objIdx][opIdx] += sampleOneIteration.N_oop_bold_influenced[objIdx][opIdx];
    			double selfValue = Util.get2Map(N_oop_bold_influenced, objIdx, opIdx);
    			double otherValue = Util.get2Map(sampleOneIteration.N_oop_bold_influenced, objIdx, opIdx);
    			Util.update2Map(N_oop_bold_influenced, objIdx, opIdx, selfValue+otherValue);
    			//System.out.println(Debugger.getCallerPosition()+"o="+objIdx+",op="+opIdx+",updated value="+N_oop_bold_influenced[objIdx][opIdx]);
    		}
    		for (int b=0; b<2; b++){//correct
    			//System.out.println(Debugger.getCallerPosition()+"o="+objIdx+",b="+b+",new value="+sampleOneIteration.N_ob_influenced[objIdx][b]+",oldvalue="+N_ob_influenced[objIdx][b]);
//    			N_ob_influenced[objIdx][b] += sampleOneIteration.N_ob_influenced[objIdx][b];
    			double selfValue = Util.get2Map(N_ob_influenced, objIdx, b);
    			double otherValue = Util.get2Map(sampleOneIteration.N_ob_influenced, objIdx, b);
    			Util.update2Map(N_ob_influenced, objIdx, b, selfValue+otherValue);
    			
    			//System.out.println(Debugger.getCallerPosition()+"o="+objIdx+",b="+b+",updated value="+N_ob_influenced[objIdx][b]);
    		}
    		for(int z=0; z< cmdOption.znum ; z++){//debugged & correct
    			//System.out.println(Debugger.getCallerPosition()+"numIter="+numIter+",objIdx="+objIdx+",z="+z+",new value="+sampleOneIteration.N_oz_bnew_influenced[objIdx][z]+",oldvalue="+N_oz_bnew_influenced[objIdx][z]);
//    			N_oz_bnew_influenced[objIdx][z] += sampleOneIteration.N_oz_bnew_influenced[objIdx][z];
    			double selfValue = Util.get2Map(N_oz_bnew_influenced, objIdx, z);
    			double otherValue = Util.get2Map(sampleOneIteration.N_oz_bnew_influenced, objIdx, z);
    			Util.update2Map(N_oz_bnew_influenced, objIdx, z, selfValue+otherValue);
    			//System.out.println(Debugger.getCallerPosition()+"updated value="+N_oz_bnew_influenced[objIdx][z]);
    		}
    		
    		if(cmdOption.model.equals(Constant.oaim))//oaim
    			for(int ta=0; ta<numTa ; ta++){
    				//    			System.out.println(Debugger.getCallerPosition()+"numIter="+numIter+",objIdx="+objIdx+",a="+a+",new value="+sampleOneIteration.N_oa_influenced[objIdx][a]+",oldvalue="+N_oa_influenced[objIdx][a]);
    				//    			this.N_ota_influenced[objIdx][a] += sampleOneIteration.N_ota_influenced[objIdx][a];
    				double selfValue = Util.get2Map(N_ota_influenced, objIdx, ta);
    				double otherValue = Util.get2Map(sampleOneIteration.N_ota_influenced, objIdx, ta);
    				Util.update2Map(N_ota_influenced, objIdx, ta, selfValue+otherValue);
    				//    			System.out.println(Debugger.getCallerPosition()+"updated value="+N_oa_influenced[objIdx][a]);

    				for(int op=0; op<numInfluencingObjects; op++){
    					//    				System.out.println(Debugger.getCallerPosition()+"numIter="+numIter+",objIdx="+objIdx+",a="+a+",op="+op+"new value="+sampleOneIteration.N_oaop_influenced[objIdx][a][op]+",oldvalue="+N_oaop_influenced[objIdx][a][op]);
    					//    				this.N_otaop_influenced[objIdx][a][op] += sampleOneIteration.N_otaop_influenced[objIdx][a][op];
    					double selfValue2 = Util.get3Map(N_otaop_influenced, objIdx, ta, op);
    					double otherValue2 = Util.get3Map(sampleOneIteration.N_otaop_influenced, objIdx, ta, op);
    					Util.update3HashMap(N_otaop_influenced, objIdx, ta, op, selfValue2+otherValue2);
    					
    					//    				System.out.println(Debugger.getCallerPosition()+"updated value="+N_oaop_influenced[objIdx][a][op]);
    				}
    			}
    		else//laim
    			for(int a=0; a<cmdOption.anum ; a++){
    				//    			System.out.println(Debugger.getCallerPosition()+"numIter="+numIter+",objIdx="+objIdx+",a="+a+",new value="+sampleOneIteration.N_oa_influenced[objIdx][a]+",oldvalue="+N_oa_influenced[objIdx][a]);
//    				this.N_oa_influenced[objIdx][a] += sampleOneIteration.N_oa_influenced[objIdx][a];
    				
    				double selfValue = Util.get2Map(this.N_oa_influenced, objIdx, a);
    				double otherValue = Util.get2Map(sampleOneIteration.N_oa_influenced, objIdx, a);
    				Util.update2Map(this.N_oa_influenced, objIdx, a, selfValue+otherValue);
    				//    			System.out.println(Debugger.getCallerPosition()+"updated value="+N_oa_influenced[objIdx][a]);

    				for(int op=0; op<numInfluencingObjects; op++){
    					//    				System.out.println(Debugger.getCallerPosition()+"numIter="+numIter+",objIdx="+objIdx+",a="+a+",op="+op+"new value="+sampleOneIteration.N_oaop_influenced[objIdx][a][op]+",oldvalue="+N_oaop_influenced[objIdx][a][op]);
//    					this.N_oaop_influenced[objIdx][a][op] += sampleOneIteration.N_oaop_influenced[objIdx][a][op];
    					double selfValue2 = Util.get3Map(this.N_oaop_influenced, objIdx, a, op);
    					double otherValue2 = Util.get3Map(sampleOneIteration.N_oaop_influenced, objIdx, a, op);
    					Util.update3HashMap(this.N_oaop_influenced, objIdx, a, op, selfValue2+otherValue2);
    					//    				System.out.println(Debugger.getCallerPosition()+"updated value="+N_oaop_influenced[objIdx][a][op]);
    				}
    			}

    		N_o_influenced[objIdx] += sampleOneIteration.N_o_influenced[objIdx];
    	}
    	if(!cmdOption.model.equals(Constant.oaim))//laim
    		for(int a=0; a<cmdOption.anum ; a++){
    			this.N_a_all[a] += sampleOneIteration.N_a_all[a];

    			for(int ta=0; ta<numTa; ta++){
    				double selfValue = Util.get2Map(this.N_ata_influenced, a, ta);
    				double otherValue = Util.get2Map(sampleOneIteration.N_ata_influenced, a, ta);
    				Util.update2Map(this.N_ata_influenced, a, ta, selfValue+otherValue);
    			}
    		}
    }
    
    /**
     * Debugged & correct
     * @param sampleOneIteration
     * @param numIter
     */
    private void sumInfluencingSampleCount(SampleData sampleOneIteration, int numIter)
    {
    	int numInfluencingObjects = Constant.oprimeNum;
    	
    	//Two counts for influencing objects
    	for(int objIdx=0; objIdx<numInfluencingObjects; objIdx++){
    		for(int z=0; z< cmdOption.znum ; z++){
    			//System.out.println(Debugger.getCallerPosition()+"numIter="+numIter+",objIdx="+objIdx+",z="+z+",new value="+sampleOneIteration.N_oz_influencing[objIdx][z]+",oldvalue="+N_oz_influencing[objIdx][z]);
//    			N_oz_influencing[objIdx][z] += sampleOneIteration.N_oz_influencing[objIdx][z];
    			double selfValue = Util.get2Map(N_oz_influencing, objIdx, z);
    			double otherValue = Util.get2Map(sampleOneIteration.N_oz_influencing, objIdx, z);
    			Util.update2Map(N_oz_influencing, objIdx, z, selfValue+otherValue);
    			//System.out.println(Debugger.getCallerPosition()+"updated value="+N_oz_influencing[objIdx][z]);
    			
    			//System.out.println(Debugger.getCallerPosition()+"numIter="+numIter+",objIdx="+objIdx+",z="+z+",new value="+sampleOneIteration.NN_opz_bold_influencing[objIdx][z]+",oldvalue="+NN_opz_bold_influencing[objIdx][z]);
//    			NN_opz_bold_influencing[objIdx][z] += sampleOneIteration.NN_opz_bold_influencing[objIdx][z]; 
    			selfValue = Util.get2Map(NN_opz_bold_influencing, objIdx, z);
    			otherValue = Util.get2Map(sampleOneIteration.NN_opz_bold_influencing, objIdx, z);
    			Util.update2Map(NN_opz_bold_influencing, objIdx, z, selfValue+otherValue);
    			//System.out.println(Debugger.getCallerPosition()+"updated value="+NN_opz_bold_influencing[objIdx][z]);
    		}
    		
    		//System.out.println(Debugger.getCallerPosition()+"numIter="+numIter+",objIdx="+objIdx+",new value="+sampleOneIteration.N_o_influencing[objIdx]+",oldvalue="+N_o_influencing[objIdx]);
    		N_o_influencing[objIdx] +=sampleOneIteration.N_o_influencing[objIdx];
			//System.out.println(Debugger.getCallerPosition()+"updated value="+N_o_influencing[objIdx]);
    		
    		//System.out.println(Debugger.getCallerPosition()+"numIter="+numIter+",objIdx="+objIdx+",new value="+sampleOneIteration.NN_op_bold_influencing[objIdx]+",oldvalue="+NN_op_bold_influencing[objIdx]);
    		NN_op_bold_influencing[objIdx] +=+sampleOneIteration.NN_op_bold_influencing[objIdx];
    		//System.out.println(Debugger.getCallerPosition()+"updated value="+NN_op_bold_influencing[objIdx]);
    	}
    }
 

    /**
     * Averaget the sample counts in the past iterations
     * @param numIter
     */
    public void averageSampleCount(int numIter){
    	
    	if(numIter<=0){
    		System.err.println(Debugger.getCallerPosition()+"Average sample count, but numIter="+numIter+"...");
    		System.exit(-1);
    	}
    	//two total counts
    	//newavg = newSum/numIter
		//newSum = oldSum + this_iteration_value
		//oldSum = old avg value * oldNumIter
    	/*for(int z=0;z<cmdOption.znum;z++){
    		for(int token=0;token<numTokens;token++){
    			N_zt_all[z][token] = (N_zt_all[z][token]*(numIter-1)+sampleOneIteration.N_zt_all[z][token])/numIter;
    		}
    		N_z_all[z] = (N_z_all[z]*(numIter-1)+sampleOneIteration.N_z_all[z])/numIter;
    	}*/
    	for(int z=0;z<cmdOption.znum;z++){
    		for(int token=0;token<numTokens;token++){
//    			N_zt_all[z][token] /= numIter;
    			double oldValue = Util.get2Map(N_zt_all, z, token);
    			Util.update2Map(N_zt_all, z, token, oldValue/numIter);
    		}
    		N_z_all[z] /= numIter;
    	}
		
		averageInfluencedSampleCount(numIter);
		averageInfluencingSampleCount(numIter);
    }
    
    private void averageInfluencedSampleCount(int numIter)
    {
    	//for influenced (or citing object) o
    	int numInfluencingObjects = Constant.oprimeNum;
    	int numInfluencedObjects = Constant.oNum;
    	
    	for(int objIdx=0; objIdx<numInfluencedObjects; objIdx++){
    		for(int opIdx=0; opIdx<numInfluencingObjects; opIdx++){
//    			N_oop_bold_influenced[objIdx][opIdx] /= numIter;
    			double oldValue = Util.get2Map(N_oop_bold_influenced, objIdx, opIdx);
    			Util.update2Map(N_oop_bold_influenced, objIdx, opIdx, oldValue/numIter);
    		}
    		for (int b=0; b<2; b++){
//    			N_ob_influenced[objIdx][b] /= numIter;
    			double oldValue = Util.get2Map(N_ob_influenced, objIdx, b);
    			Util.update2Map(N_ob_influenced, objIdx, b, oldValue/numIter);
    		}
    		for(int z=0; z< cmdOption.znum ; z++){
    			//System.out.println("numIter="+numIter+",objIdx="+objIdx+",z="+z+",oldvalue="+N_oz_bnew_influenced[objIdx][z]);
//    			N_oz_bnew_influenced[objIdx][z] /= numIter;
    			double oldValue = Util.get2Map(N_oz_bnew_influenced, objIdx, z);
    			Util.update2Map(N_oz_bnew_influenced, objIdx, z, oldValue/numIter);
    			
    			//System.out.println("updated value="+N_oz_bnew_influenced[objIdx][z]);
    		}
    		if(cmdOption.model.equals(Constant.oaim))//oaim
    			for(int a=0; a<numTa; a++){
    				//    			N_ota_influenced[objIdx][a] /= numIter;
    				double old = Util.get2Map(N_ota_influenced, objIdx, a);
    				Util.update2Map(N_ota_influenced, objIdx, a, old/numIter);
    				for(int op = 0; op <numInfluencingObjects; op++){
    					double oldValue = Util.get3Map(N_otaop_influenced, objIdx, a, op);
    					Util.update3HashMap(N_otaop_influenced, objIdx, a, op, oldValue/numIter);
    					//    				N_otaop_influenced[objIdx][a][op] /= numIter;
    				}
    			}
    		else//laim
    			for(int a=0; a<cmdOption.anum; a++){
    				double oldValue = Util.get2Map(N_oa_influenced, objIdx, a);
    				Util.update2Map(N_oa_influenced, objIdx, a, oldValue/numIter);
//    				N_oa_influenced[objIdx][a] /= numIter;
    				for(int op = 0; op <numInfluencingObjects; op++){
//    					N_oaop_influenced[objIdx][a][op] /= numIter;
    					double oldValue2 = Util.get3Map(N_oaop_influenced, objIdx, a, op);
    					Util.update3HashMap(N_oaop_influenced, objIdx, a, op, oldValue2/numIter);
    				}
    			}
    		
    		N_o_influenced[objIdx] /= numIter;
    	}
    	
    	if(!cmdOption.model.equals(Constant.oaim))//laim
    		for(int a=0; a < cmdOption.anum; a++){
    			this.N_a_all[a] /= numIter;
    			for(int ta=0; ta<numTa; ta++){
//    				this.N_ata_influenced[a][ta] /= numIter;
    				double oldValue = Util.get2Map(N_ata_influenced, a, ta);
    				Util.update2Map(N_ata_influenced, a, ta, oldValue/numIter);
    			}
    		}
    }
    
    private void averageInfluencingSampleCount(int numIter)
    {
    	int numInfluencingObjects = Constant.oprimeNum;
    	
    	for(int objIdx=0; objIdx<numInfluencingObjects; objIdx++){
    		for(int z=0; z< cmdOption.znum ; z++){
//    			N_oz_influencing[objIdx][z] /= numIter;
    			double oldValue = Util.get2Map(N_oz_influencing, objIdx, z);
				Util.update2Map(N_oz_influencing, objIdx, z, oldValue/numIter);
    			
//    			NN_opz_bold_influencing[objIdx][z] /= numIter; 
    			oldValue = Util.get2Map(NN_opz_bold_influencing, objIdx, z);
				Util.update2Map(NN_opz_bold_influencing, objIdx, z, oldValue/numIter);
    		}
    		N_o_influencing[objIdx] /= numIter;
    		NN_op_bold_influencing[objIdx] /= numIter;
    	}
    }
    
    /**
	 * get the Topic-Token mixture
	 * @return
	 */
	public double[][] getPhi(){
		double[][] phi = new double[cmdOption.znum][numTokens];
		
//		double[][] N_zt = N_zt_all;
		double[] N_z = N_z_all;
		
		for(int i=0; i<phi.length; i++){
			double den = N_z[i] + numTokens*cmdOption.alphaPhi;
			for(int j=0; j<phi[0].length; j++){
				double num = Util.get2Map(N_zt_all, i, j)+cmdOption.alphaPhi;
				phi[i][j] = num/den;
//				System.out.println(Debugger.getCallerPosition()+" test final Phi "+phi[i][j]);
			}
		}
		
		return phi;
	}
	/**
	 * get topic distribution of influenced documents.
	 * @return
	 */
	public double[][] getTheta(){
		double[][] theta = new double[Constant.oNum][cmdOption.znum];
		
//		double[][] N_ozb0 = N_oz_bnew_influenced;
//		double[][] N_ob = N_ob_influenced;
		
		for(int i=0; i<theta.length; i++){
			double den = Util.get2Map(N_ob_influenced, i, Constant.INNOTVATION) +cmdOption.znum*cmdOption.alphaTheta;
			for(int j=0; j<theta[0].length; j++){
				double num = Util.get2Map(N_oz_bnew_influenced, i, j) +cmdOption.alphaTheta;
				theta[i][j] = num/den;
//				System.out.println(Debugger.getCallerPosition()+" test final Theta "+theta[i][j]);
			}
		}
		
		return theta;
	}
	/**
	 * get topic distribution for influencing documents.
	 * @return
	 */
	public double[][] getThetaPrime(){
		double[][] theta = new double[Constant.oprimeNum][cmdOption.znum];
		
//		double[][] N_oopz = NN_opz_bold_influencing;
//		double[][] N_opz = N_oz_influencing;
		double[] N_oop = NN_op_bold_influencing;
		double[] N_op = N_o_influencing;
		
		for(int i=0; i<theta.length; i++){
			double den = N_oop[i]+N_op[i]+cmdOption.znum*cmdOption.alphaTheta;
			for(int j=0; j<theta[0].length; j++){
				double num = Util.get2Map(NN_opz_bold_influencing, i, j)  +Util.get2Map(N_oz_influencing, i, j) +cmdOption.alphaTheta;
				theta[i][j] = num/den;
//				System.out.println(Debugger.getCallerPosition()+" test final Theta "+theta[i][j]);
			}
		}
		return theta;
	}

    /**
     * For all the influenced object, get all the objects that influence them. 
     * @return
     */
    public ArrayList<List<Integer>> getReferencePubIndexIdList(){
    	return in_refPubIndexIdList;
    }

	public ArrayList<SampleElementInfluencing> getSampleInfluencing_otz() {
		return sampleInfluencing_otz;
	}


	public ArrayList<SampleElementInfluencedLAIM> getSampleInfluenced_otzbataop() {
		return sampleInfluenced_otzbataop;
	}

    
}
