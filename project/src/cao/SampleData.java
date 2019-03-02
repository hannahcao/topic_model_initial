package cao;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;


import cao.Debugger;
import cao.Util;

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
	double [][] in_influencing_wd; 	//cited: influencing  T*D
	double [][] in_influenced_wd; 	//citing: influenced  T*1
	ArrayList<List<Integer>> in_refPubIndexIdList; // citing graph
	
	int 	numTokens;
	//sample variables
	//private TreeMap<SampleElementInfluencing,Integer> sampleInfluencing_otz_map;
	private ArrayList<SampleElementInfluencing> sampleInfluencing_otz;
	
	//private TreeMap<SampleElementInfluenced,Integer> sampleInfluenced_otzbop_map;
	public ArrayList<SampleElementInfluenced> sampleInfluenced_otzbop;
	
	//the counts are declared to be double for the purpose of averaging the sample counts in multiple iterations
    public double[][] N_zt_all;// = new int[in_topicNum][numTokens];//wtAll,, N_{z,t}
    public double[]  N_z_all; 
    
    double[][] N_oz_influencing; //N'_{o,z}, tdCited, used in drawing sample for influencing objects
    double[]   N_o_influencing;// = new int[numInfluencingDocs]; //N'_{o}? ,dCited?
    public double[][] NN_opz_bold_influencing;// = new int[numInfluencingDocs][in_topicNum];//N'_{o',z,b}=NN'_{o',z} where b=1, tcs0Citing
    public double[]   NN_op_bold_influencing;// = new int[numInfluencingDocs];//N'_{o',b=1} = NN'_{o'}. cs0Citing
    
    double[][] N_oz_bnew_influenced;// = new int[numInfluencedDocs][in_topicNum]; //N_{o,z,b} where b=0, tds1Citing
    double[][] N_ob_influenced;// = new int[numInfluencedDocs][2];//N_{o,b}, ds1Citing(b=0),ds0Citing(b=1)
    double[][] N_oop_bold_influenced;// = new int[numInfluencedDocs][numInfluencingDocs];////N_{o,o'}=N_{o,o',b=1} where b=1, dcs0Citing
    double[]   N_o_influenced; //= new int[numInfluencedDocs]; dCiting //N_{o} ?
    
    double[][][] N_oaop_influenced; // 1*A*D	N_{o,a,o',b=1}
    double[][] N_oa_influenced;// 1*A	N_{o,a,b=1}
    
	public SampleData(final double [][]influencing_wd , final double [][]influenced_wd, 
			int numLatentStates, int maxnumBibliographListLength, 
			ArrayList<List<Integer>> bibliographIndexidList,
			CmdOption _option)
	{
		cmdOption = _option;
		in_influencing_wd = influencing_wd;
		in_influenced_wd = influenced_wd;
		in_refPubIndexIdList = bibliographIndexidList;
		
		//The length of token vector in both influencing and influenced objects should be the same
		if(in_influencing_wd.length!=in_influenced_wd.length){
			throw new IllegalArgumentException("g_influenceing_wd.length!=g_influenced_wd.length");
		}
		
		init();
	}
	
	/**
	 * Initialize the data structure for Gibbs sampling
	 */
	private void init()
	{
		numTokens = in_influencing_wd.length;
		
		////////////////////////////////////////
		//1. token count for influencing and influenced documents
		int numInfluencingObjects = in_influencing_wd[0].length;
        int numInfluencedObjects = in_influenced_wd[0].length;
        //object, token, latent state
        sampleInfluencing_otz = new ArrayList<SampleElementInfluencing>(); 
        
        //dwtscCiting//[d][w][t][s][c][val=]//sample t,s,c for each (d,w) combination
        //object, token, latent state, b, op
        //sampleInfluenced_otzbop_map = new TreeMap<SampleElementInfluenced,Integer>();//dwtCited
        sampleInfluenced_otzbop = new ArrayList<SampleElementInfluenced>(); 
        
        //In Laura Dietz's code, a cited document can not be a citing document at the same time
        //But this is not realistic. 
        //What about an object is an influencing and an influenced object as well?
        
        /////////////////////////////////////////////////////////////
        //3. Initialize sampling counts
        //Sampling counts
        N_zt_all = new double[cmdOption.znum][numTokens];//wtAll,, N_{z,t}
        N_z_all = new double[cmdOption.znum]; //tAll, N_{z}
        //Nzt_influencing = new int[in_topicNum][numTokens]; //wtAll, N'_{z,t}
        //Nz_influencing = new int[in_topicNum]; //tAll, N'_{z}
        
        ///////////////////////////////
        //for influencing (or cited object) o
        N_oz_influencing = new double[numInfluencingObjects][cmdOption.znum]; //N'_{o,z}, tdCited
        N_o_influencing = new double[numInfluencingObjects]; //N'_{o}
        
        //o is used as an influencing object to generate latent state for other objects
        //this is used only when b=1
        NN_opz_bold_influencing = new double[numInfluencingObjects][cmdOption.znum];//N'_{o',z,b}=NN_{o',z} where b=1, tcs0Citing
        NN_op_bold_influencing = new double[numInfluencingObjects];
        ///////////////////////////////
        
        //for influenced (or citing object) o
        N_oop_bold_influenced = new double[numInfluencedObjects][numInfluencingObjects];//N_{o,o'}=N_{o,o',b=1} where b=1, dcs0Citing
        N_ob_influenced = new double[numInfluencedObjects][2];//N_{o,b}, ds1Citing(b=0),ds0Citing(b=1)
        N_o_influenced = new double[numInfluencedObjects]; //N_{o}, dCiting
        N_oz_bnew_influenced = new double[numInfluencedObjects][cmdOption.znum]; //N_{o,z,b=0} where b=0, tds1Citing
        
        N_oaop_influenced = new double[numInfluencedObjects][cmdOption.anum][numInfluencingObjects];
        N_oa_influenced = new double[numInfluencedObjects][cmdOption.anum];
        //similar to N_ob_influenced
        //Nbo_influenced = new int[2][numInfluencedDocs];//sdCiting, N_{o,b} -> N_{o}, N'_{o}
        /////////////////////////////////////////////////////////////
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
	 
	 public ArrayList<SampleElementInfluenced> getSampleInfluenced_otzbop() {
			return sampleInfluenced_otzbop;
	 }
	
	 /**
	 * Draw the initial sample for influencing and influenced objects
	 */
	public void drawInitialSample()
	{
		drawIntitialSampleInfluencing();
		drawIntitialSampleInfluenced();
	}
	
	/**
	 * Draw initial samples for influencing objects
	 */
	private void drawIntitialSampleInfluencing()
	{
		assert(numTokens==in_influencing_wd.length); //cited_wd
		
		for (int token = 0; token < numTokens; token++) {//w
			for (int obj = 0; obj < in_influencing_wd[token].length; obj++) {//d    
                
				//Get the number of tokens in object "obj"
				final double tokenCount = in_influencing_wd[token][obj];
				
				//when this position does not have any token
				//i.e., this token does not appear in this object?
                if (tokenCount == 0) continue; 
                
                //token occurs "val" times in the profile of object "obj"  
                for (int occ = 0; occ < tokenCount; occ++) {
                    int newZ = cao.Util.initialLatentState(token, obj, 1, cmdOption.znum);
                    
                    //1. update sample matrix
            		SampleElementInfluencing e = new SampleElementInfluencing(obj,token,newZ);
            		sampleInfluencing_otz.add(e);
                    
            		//2.add the sample for [obj],[token],[latent_state] drawing
            		updCountInfluencing_otz(obj,token,newZ,1);
                }
            }
        }
	}
	
	//add A's related count here!!!!!!!!
	private void drawIntitialSampleInfluenced()
	{
		assert(numTokens==in_influenced_wd.length); //cited_wd
		
		for (int token = 0; token < numTokens; token++) {//w
			for (int obj = 0; obj < in_influenced_wd[token].length; obj++) {//d    
                final double tokenCount = in_influenced_wd[token][obj];
                if (tokenCount == 0) continue; //when this position does not have any token
                
                for (int occ = 0; occ < tokenCount; occ++) {
                    int newZ = cao.Util.initialLatentState(token, obj, 1, cmdOption.znum);
                    int newB = cao.Util.initialLatentState(token, obj, 1, 2);
                    int newOprime = -1;
                    int newA = -1;
                    if (newB!=Constant.INNOTVATION){
                    	newOprime = cao.Util.initiaInfluencing(token, obj, 1,in_refPubIndexIdList.get(obj));
                    	newA = cao.Util.initiaAspect(token, obj, 1, cmdOption.anum);
                    }
                    
                    //data.addCitingDwtsc(d, w, newT, newS, newC, 1, position);
                    SampleElementInfluenced e = new SampleElementInfluenced(obj,token,newZ,newB, newA, newOprime);
                    
                    sampleInfluenced_otzbop.add(e);
                    
                    updCountInfluenced_otzbaop(obj,token,newZ,newB, newA, newOprime,1);
                }
            }
        }
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
	    N_zt_all[z][token] +=freq;
		N_z_all[z]+=freq;
		
		//total counts
		N_oz_influencing[obj][z] +=freq;
		N_o_influencing[obj] +=freq;
	}
	
	/**
	 * update the count of influenced document for obj's token
	 * @param obj
	 * @param token
	 * @param z
	 * @param b
	 * @param oprime
	 * @param value
	 */
	private void updCountInfluenced_otzbaop(int obj, int token, int z, int b, int a, int oprime, int value)
	{//add aspect's related count here!!!!!!!!!!!
		if(b==Constant.INHERITANCE){ //b=0; use the inherited data
			N_oop_bold_influenced[obj][oprime] +=value; //o,o'
        	NN_opz_bold_influencing[oprime][z] +=value; //o',z
        	NN_op_bold_influencing[oprime] +=value;	//o'
        	N_oa_influenced[obj][a] +=value;
        	N_oaop_influenced[obj][a][oprime]+=value;
        }else{//b=1 innovative
        	N_oz_bnew_influenced[obj][z]+=value; //o
        }
        N_ob_influenced[obj][b] +=value; //o,b=1
        N_o_influenced[obj] +=value;
        //N_zt_influenced[z][token] +=value;//
        //Nz_influenced[z] +=value;
        
        //total counts
        N_zt_all[z][token] +=value;//
        N_z_all[z] +=value;
        //Nbo_influenced[b][obj] +=value;
	}
	
	public void checkSampleCountConsistency()
	{
		int tmpsum =0 ; 
		
		//1. total N_z_all[i] = \sum_j N_zt_all[i][j]
		for(int z=0;z<cmdOption.znum;z++){
			tmpsum = 0;
			for(int t=0;t<N_zt_all[z].length;t++)tmpsum +=N_zt_all[z][t];
			assert(tmpsum!=N_z_all[z]):"ERROR N_z_all["+z+"]";
		}
		
		//2. influenced: N_o_influenced[i] = \sum_b N_ob_influenced[i][b]
		for(int obj=0;obj<N_o_influenced.length;obj++){
			tmpsum = 0;
			for(int b=0;b<N_ob_influenced[obj].length;b++)
				tmpsum+=N_ob_influenced[obj][b];
			assert(tmpsum!=N_o_influenced[obj]):"ERROR N_o_influenced["+obj+"]";
		}
		
		//3. influenced b0 and b1
		//get b0sample sum and b1 sample sum
		int[] bsum = new int[2];
		bsum[0] = bsum[1] = 0;
		for(int b=0;b<=1;b++){
			for(int obj=0;obj<N_ob_influenced.length;obj++){
				bsum[b]+= N_ob_influenced[obj][b];
			}
		}
		
		//4. influenced b innovative
		//N_oz_bnew_influenced sum should be equal to bsum[innovative]
		tmpsum=0;
		for(int obj=0;obj<N_oz_bnew_influenced.length;obj++){
			for(int z=0;z<cmdOption.znum;z++)
				tmpsum+=N_oz_bnew_influenced[obj][z];
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
		for(int obj=0;obj<N_ob_influenced.length;obj++){
			tmpsum = 0; 
			for(int op=0; op<N_oop_bold_influenced[obj].length;op++){
				tmpsum+=N_oop_bold_influenced[obj][op];
			}
			assert(tmpsum!=N_ob_influenced[obj][Constant.INHERITANCE]):
				"N_ob_influenced["+obj+"]["+Constant.INHERITANCE+"]!="+tmpsum;
		}
	
		//7. influenced NN_op_bold_influencing and NN_opz_bold_influencing
		for(int op=0;op<NN_op_bold_influencing.length;op++){
			tmpsum=0;
			for(int z=0;z<cmdOption.znum;z++){
				tmpsum+=NN_opz_bold_influencing[op][z];
			}
			assert(tmpsum!=NN_op_bold_influencing[op]):
				"NN_op_bold_influencing["+op+"]!="+tmpsum;
		}
		
//		8. Check N_oaop and N_oa
		for(int a=0; a<cmdOption.anum; a++){
			for(int obj=0; obj<in_influenced_wd[0].length; obj++){
				tmpsum = 0;
				for(int op=0; op<in_influencing_wd[0].length; op++){
					tmpsum+=this.N_oaop_influenced[obj][a][op];
				}
				assert(tmpsum!=N_oa_influenced[obj][a]):
					"N_oa_influenced[obj]["+a+"]!="+tmpsum;
			}
		}
		
		//System.out.println(Debugger.getCallerPosition()+"Counts are CONSISTENT.");
	}
	
	/**
	 * Draw sample for one iteration
	 */
	public void drawOneIterationSample() {
    	drawOneIterationInfluencingSample();
    	drawOneIterationInfluencedSample();
    	
    	//check the consistency of the sample counts
		this.checkSampleCountConsistency();
    }
	
	/**
	 * Draw samples for one influencing document, update related counts
	 * Tested, checked the first 10 element's update, correct
	 */
	private void drawOneIterationInfluencingSample() 
	{
		int i =0; 
		//put all the newly added element to this list
		//ArrayList<SampleElementInfluencingCount> newEList = new ArrayList<SampleElementInfluencingCount>();
		for(SampleElementInfluencing e: sampleInfluencing_otz)
		{
			//1. get old sample, update the sample matrix and counts
			//e.updateCount(-1); 
			updCountInfluencing_otz(e.obj, e.token, e.z, -1); //update count
			//System.out.println(Debugger.getCallerPosition()+"[1]e="+e);

			//2. sample new latent state
			int newZ = Probability.getMAPNotInfluenced_z(cmdOption, this, e.obj, e.token);//getMAPCiting_zbop
			e.z = newZ;
			updCountInfluencing_otz(e.obj, e.token, e.z, +1); //update count
			//System.out.println(Debugger.getCallerPosition()+"[2]e="+e);
			
			//System.out.println(Debugger.getCallerPosition()+"[t1] N_zt_all: \n"+Util.Array2DToString(N_zt_all));//z,t
			//System.out.println(Debugger.getCallerPosition()+"[t2] N_z_all: \n"+Arrays.toString(N_z_all)+"\n");//z,t
			//System.out.println(Debugger.getCallerPosition()+"[inf1] N_oz_influencing:\n"+Util.Array2DToString(N_oz_influencing));//o,z
			//System.out.println(Debugger.getCallerPosition()+"[inf2] N_o_influencing:\n"+Arrays.toString(N_o_influencing)+"\n");//o,z
			
			/*if(e.count==0){
				e.z = newZ;
				e.count=1;
				//use the new latent state, update sample matrix and counts
				updCountInfluencing_otz(e.obj, e.token, newZ, +1); //update count
			}else{
				SampleElementInfluencingCount newe = new SampleElementInfluencingCount(e.obj,e.token,newZ,1);
				newEList.add(newe);
				//use the new latent state, update sample matrix and counts
				updCountInfluencing_otz(newe.obj, newe.token, newZ, +1); //update count
			}*/
			//i++;
			//if(i>=10)break;
		}
		
		/*if(newEList.size()>0){
			//Add all the newly added elements
			System.out.println("Influencing::newEList="+newEList);
			sampleInfluencing_otz.addAll(newEList);
		}*/
		
		
		
	}
	
		/**
	 * Draw samples for one influenced document, update related counts
	 */
	private void drawOneIterationInfluencedSample() {
		//ArrayList<SampleElementInfluencedCount> newEList = new ArrayList<SampleElementInfluencedCount>();
		
		int i=0;
		//get the old sample for object e (with old obj, old token, old z, old oprime, old count)
		for(SampleElementInfluenced e: this.sampleInfluenced_otzbop){
		
			//decrease its count
			//e.updateCount(-1);
			
			updCountInfluenced_otzbaop(e.obj, e.token, e.z, e.b, e.a, e.oprime, -1);
			//System.out.println(Debugger.getCallerPosition()+"[3]e="+e);
			
			//get new b, new o', new z (latent state)
			//getMAPCitingTSC(d, w, data);
			int[] new_zbopa = Probability.getMAPInfluenced_zbaop(cmdOption, this,e.obj, e.token);//CitinfSampler.getMAPCiting_zbop
            int newZ = new_zbopa[0];
            int newB = new_zbopa[1];
            int newA = new_zbopa[2];
            int newOprime = new_zbopa[3];
            e.z = newZ;
			e.b = newB;
			e.oprime = newOprime;
			e.a = newA;
			
			//System.out.println(Debugger.getCallerPosition()+"[4]e="+e);
			updCountInfluenced_otzbaop(e.obj, e.token, e.z, e.b, e.a, e.oprime, 1);
			
			/*
			System.out.println(Debugger.getCallerPosition()+"[t1] N_zt_all: \n"+Util.Array2DToString(N_zt_all));//z,t
			System.out.println(Debugger.getCallerPosition()+"[t2] N_z_all: \n"+Arrays.toString(N_z_all)+"\n");//z,t
			System.out.println(Debugger.getCallerPosition()+"[inf3] NN_opz_bold_influencing:\n"+Util.Array2DToString(NN_opz_bold_influencing));//N'_{o',z,b}=NN_{o',z} where b=1, tcs0Citing
			System.out.println(Debugger.getCallerPosition()+"[inf4] NN_op_bold_influencing:\n"+Arrays.toString(NN_op_bold_influencing)+"\n");
	        ///////////////////////////////
	        
	        
			System.out.println(Debugger.getCallerPosition()+"****Influenced counts**** \n");
			System.out.println(Debugger.getCallerPosition()+"[Infed1] N_o_influenced:\n"+Arrays.toString(N_o_influenced)+"\n");
			System.out.println(Debugger.getCallerPosition()+"[Infed2] N_ob_influenced=\n"+Util.Array2DToString(N_ob_influenced));//o,b
			System.out.println(Debugger.getCallerPosition()+"[Infed3] N_oop_bold_influenced=\n"+Util.Array2DToString(N_oop_bold_influenced));//o,op, when b=1
			System.out.println(Debugger.getCallerPosition()+"[Infed4] N_oz_bnew_influenced=\n"+Util.Array2DToString(N_oz_bnew_influenced));//o,op, when b=1
	        */
			
            //update the counts N
            /*if(e.count==0){
				e.z = newZ;
				e.b = newB;
				e.oprime = newOprime;
				e.count=1;
				updCountInfluenced_otzbop(e.obj, e.token, e.z, e.b, e.oprime, e.count);
			}else{
				SampleElementInfluencedCount newe = 
						new SampleElementInfluencedCount(e.obj,e.token,newZ,newB,newOprime,1);
				newEList.add(newe);
				updCountInfluenced_otzbop(newe.obj, newe.token, newe.z, newe.b,newe.oprime, newe.count);
			}*/
			
			//i++;
			//if(i>=3)break;
		}
		
		//if(newEList.size()>0){
			//Add all the newly added elements
		//	System.out.println("Influenced:newEList="+newEList);
		//	sampleInfluenced_otzbop.addAll(newEList);
		//}
	}
	
	
    public String toString() {
    	final StringBuffer str = new StringBuffer();
    	//int size1=0;
    	//for(Map.Entry<SampleElementInfluencing, Integer> entry: sampleInfluencing_otz_map.entrySet()){
    	//	size1+=entry.getValue();
    	//}
    	
    	//int size2=0;
    	//for(Map.Entry<SampleElementInfluenced, Integer> entry: sampleInfluenced_otzbop_map.entrySet()){
    	//	size2+=entry.getValue();
    	//}
    	//for the manual default data:
    	// wordCount_o_Influencing=[30.0, 10.0, 12.0, 9.0, 21.0, 10.0, 14.0, 9.0, 8.0]
    	// wordCount_o_Influenced=[9.0, 10.0, 9.0, 19.0]
    	// So the first size is 123 = 30 + 10 + 12 +...+8; the second size =47 = 9+10+9+19
    	str.append(Debugger.getCallerPosition()+"****Sample**** \n");
        str.append(Debugger.getCallerPosition()+"InfluencingSample[o][t][z]=frequency; size=" +sampleInfluencing_otz.size()
        		+":" + sampleInfluencing_otz);
        str.append(Debugger.getCallerPosition()+"\nInfluencedSample:[o][t][z][b][o']=frequency; size="
        		+sampleInfluenced_otzbop.size()+":"+ sampleInfluenced_otzbop);
        //str.append(Debugger.getCallerPosition()+"\nInfluencedSample:sampleInfluenced_otzbop_map, size="
        //		+sampleInfluenced_otzbop_map.size()+":"+sampleInfluenced_otzbop_map);
        
        str.append(Debugger.getCallerPosition()+"\n****Total counts**** \n");
        str.append(Debugger.getCallerPosition()+"[t1] N_zt_all: \n"+Util.Array2DToString(N_zt_all));//z,t
        str.append(Debugger.getCallerPosition()+"[t2] N_z_all: \n"+Arrays.toString(N_z_all)+"\n");//z,t
        
        str.append(Debugger.getCallerPosition()+"\n****Influencing Counts****\n");
        str.append(Debugger.getCallerPosition()+"[inf1] N_oz_influencing:\n"+Util.Array2DToString(N_oz_influencing));//o,z
        str.append(Debugger.getCallerPosition()+"[inf2] N_o_influencing:\n"+Arrays.toString(N_o_influencing)+"\n");//o,z
        str.append(Debugger.getCallerPosition()+"[inf3] NN_opz_bold_influencing:\n"+Util.Array2DToString(NN_opz_bold_influencing));//N'_{o',z,b}=NN_{o',z} where b=1, tcs0Citing
        str.append(Debugger.getCallerPosition()+"[inf4] NN_op_bold_influencing:\n"+Arrays.toString(NN_op_bold_influencing)+"\n");
        ///////////////////////////////
        
        
        str.append(Debugger.getCallerPosition()+"****Influenced counts**** \n");
        str.append(Debugger.getCallerPosition()+"[Infed1] N_o_influenced:\n"+Arrays.toString(N_o_influenced)+"\n");
        str.append(Debugger.getCallerPosition()+"[Infed2] N_ob_influenced=\n"+Util.Array2DToString(N_ob_influenced));//o,b
        str.append(Debugger.getCallerPosition()+"[Infed3] N_oop_bold_influenced=\n"+Util.Array2DToString(N_oop_bold_influenced));//o,op, when b=1
        str.append(Debugger.getCallerPosition()+"[Infed4] N_oz_bnew_influenced=\n"+Util.Array2DToString(N_oz_bnew_influenced));//o,op, when b=1
        
        
        return str.toString();
    }
    
    /**
     * 
     * @return the number of influenced objects
     */
    public int getNumInfluencedObjects() {
    	if(in_influencing_wd.length==0) return 0;
    	else return in_influencing_wd[0].length;
    }
    
    /**
     * For all the influenced object, get all the objects that influence them. 
     * @return
     */
    public ArrayList<List<Integer>> getReferencePubIndexIdList(){
    	return in_refPubIndexIdList;
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
    			N_zt_all[z][token] += sampleOneIteration.N_zt_all[z][token];
    			//System.out.println(Debugger.getCallerPosition()+"updated value="+N_zt_all[z][token]);
    		}
    		//System.out.println(Debugger.getCallerPosition()+"z="+z+",old value="+N_z_all[z]+",newvalue="+sampleOneIteration.N_z_all[z]);
    		N_z_all[z] += sampleOneIteration.N_z_all[z];
    		//System.out.println(Debugger.getCallerPosition()+"updated value="+N_z_all[z]);
    	}
    	
    	sumInfluencedSampleCount(sampleOneIteration,numIter);  //debugged & correct
    	sumInfluencingSampleCount(sampleOneIteration,numIter); //Debugged & correct
    	
    	this.checkSampleCountConsistency();
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
    	int numInfluencingObjects = in_influencing_wd[0].length;
    	int numInfluencedObjects = in_influenced_wd[0].length;
    	
    	//N_oop_bold_influenced = new int[numInfluencedObjects][numInfluencingObjects];//N_{o,o'}=N_{o,o',b=1} where b=1, dcs0Citing
        //N_ob_influenced = new int[numInfluencedObjects][2];//N_{o,b}, ds1Citing(b=0),ds0Citing(b=1)
        //N_o_influenced = new int[numInfluencedObjects]; //N_{o}, dCiting
    	//N_oz_bnew_influenced = new int[numInfluencedObjects][in_numLatentState];
    	for(int objIdx=0; objIdx<numInfluencedObjects; objIdx++){
    		for(int opIdx=0; opIdx<numInfluencingObjects; opIdx++){//correct
    			//System.out.println(Debugger.getCallerPosition()+"o="+objIdx+",op="+opIdx+",new value="+sampleOneIteration.N_oop_bold_influenced[objIdx][opIdx]+",oldvalue="+N_oop_bold_influenced[objIdx][opIdx]);
    			N_oop_bold_influenced[objIdx][opIdx] += sampleOneIteration.N_oop_bold_influenced[objIdx][opIdx];
    			//System.out.println(Debugger.getCallerPosition()+"o="+objIdx+",op="+opIdx+",updated value="+N_oop_bold_influenced[objIdx][opIdx]);
    		}
    		for (int b=0; b<2; b++){//correct
    			//System.out.println(Debugger.getCallerPosition()+"o="+objIdx+",b="+b+",new value="+sampleOneIteration.N_ob_influenced[objIdx][b]+",oldvalue="+N_ob_influenced[objIdx][b]);
    			N_ob_influenced[objIdx][b] += sampleOneIteration.N_ob_influenced[objIdx][b];
    			//System.out.println(Debugger.getCallerPosition()+"o="+objIdx+",b="+b+",updated value="+N_ob_influenced[objIdx][b]);
    		}
    		for(int z=0; z< cmdOption.znum ; z++){//debugged & correct
    			//System.out.println(Debugger.getCallerPosition()+"numIter="+numIter+",objIdx="+objIdx+",z="+z+",new value="+sampleOneIteration.N_oz_bnew_influenced[objIdx][z]+",oldvalue="+N_oz_bnew_influenced[objIdx][z]);
    			N_oz_bnew_influenced[objIdx][z] += sampleOneIteration.N_oz_bnew_influenced[objIdx][z];
    			//System.out.println(Debugger.getCallerPosition()+"updated value="+N_oz_bnew_influenced[objIdx][z]);
    		}
    		
    		for(int a=0; a<cmdOption.anum ; a++){
//    			System.out.println(Debugger.getCallerPosition()+"numIter="+numIter+",objIdx="+objIdx+",a="+a+",new value="+sampleOneIteration.N_oa_influenced[objIdx][a]+",oldvalue="+N_oa_influenced[objIdx][a]);
    			this.N_oa_influenced[objIdx][a] += sampleOneIteration.N_oa_influenced[objIdx][a];
//    			System.out.println(Debugger.getCallerPosition()+"updated value="+N_oa_influenced[objIdx][a]);
    			
    			for(int op=0; op<numInfluencingObjects; op++){
//    				System.out.println(Debugger.getCallerPosition()+"numIter="+numIter+",objIdx="+objIdx+",a="+a+",op="+op+"new value="+sampleOneIteration.N_oaop_influenced[objIdx][a][op]+",oldvalue="+N_oaop_influenced[objIdx][a][op]);
    				this.N_oaop_influenced[objIdx][a][op] += sampleOneIteration.N_oaop_influenced[objIdx][a][op];
//    				System.out.println(Debugger.getCallerPosition()+"updated value="+N_oaop_influenced[objIdx][a][op]);
    			}
    		}
    		N_o_influenced[objIdx] += sampleOneIteration.N_o_influenced[objIdx];
    	}
    }
    
    /**
     * Debugged & correct
     * @param sampleOneIteration
     * @param numIter
     */
    private void sumInfluencingSampleCount(SampleData sampleOneIteration, int numIter)
    {
    	int numInfluencingObjects = in_influencing_wd[0].length;
    	
    	//Two counts for influencing objects
    	for(int objIdx=0; objIdx<numInfluencingObjects; objIdx++){
    		for(int z=0; z< cmdOption.znum ; z++){
    			//System.out.println(Debugger.getCallerPosition()+"numIter="+numIter+",objIdx="+objIdx+",z="+z+",new value="+sampleOneIteration.N_oz_influencing[objIdx][z]+",oldvalue="+N_oz_influencing[objIdx][z]);
    			N_oz_influencing[objIdx][z] += sampleOneIteration.N_oz_influencing[objIdx][z];
    			//System.out.println(Debugger.getCallerPosition()+"updated value="+N_oz_influencing[objIdx][z]);
    			
    			//System.out.println(Debugger.getCallerPosition()+"numIter="+numIter+",objIdx="+objIdx+",z="+z+",new value="+sampleOneIteration.NN_opz_bold_influencing[objIdx][z]+",oldvalue="+NN_opz_bold_influencing[objIdx][z]);
    			NN_opz_bold_influencing[objIdx][z] += sampleOneIteration.NN_opz_bold_influencing[objIdx][z]; 
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
    			N_zt_all[z][token] /= numIter;
    		}
    		N_z_all[z] /= numIter;
    	}
		
		averageInfluencedSampleCount(numIter);
		averageInfluencingSampleCount(numIter);
    }
    
    private void averageInfluencedSampleCount(int numIter)
    {
    	//for influenced (or citing object) o
    	int numInfluencingObjects = in_influencing_wd[0].length;
    	int numInfluencedObjects = in_influenced_wd[0].length;

    	//N_oop_bold_influenced = new int[numInfluencedObjects][numInfluencingObjects];//N_{o,o'}=N_{o,o',b=1} where b=1, dcs0Citing
        //N_ob_influenced = new int[numInfluencedObjects][2];//N_{o,b}, ds1Citing(b=0),ds0Citing(b=1)
        //N_o_influenced = new int[numInfluencedObjects]; //N_{o}, dCiting
    	//N_oz_bnew_influenced = new int[numInfluencedObjects][in_numLatentState];
    	/*for(int objIdx=0; objIdx<numInfluencedObjects; objIdx++){
    		for(int opIdx=0; opIdx<numInfluencingObjects; opIdx++){
    			N_oop_bold_influenced[objIdx][opIdx] = (N_oop_bold_influenced[objIdx][opIdx]*(numIter-1)+sampleOneIteration.N_oop_bold_influenced[objIdx][opIdx])/numIter;
    		}
    		for (int b=0; b<2; b++){
    			N_ob_influenced[objIdx][b] = (N_ob_influenced[objIdx][b]*(numIter-1)+sampleOneIteration.N_ob_influenced[objIdx][b])/numIter;
    		}
    		for(int z=0; z< cmdOption.znum ; z++){
    			System.out.println("numIter="+numIter+",objIdx="+objIdx+",z="+z+",new value="+sampleOneIteration.N_oz_bnew_influenced[objIdx][z]
    					+",oldvalue="+N_oz_bnew_influenced[objIdx][z]);
    			N_oz_bnew_influenced[objIdx][z] = (N_oz_bnew_influenced[objIdx][z]*(numIter-1)+sampleOneIteration.N_oz_bnew_influenced[objIdx][z])/numIter;
    			System.out.println("updated value="+N_oz_bnew_influenced[objIdx][z]);
    		}
    		N_o_influenced[objIdx] = (N_o_influenced[objIdx] *(numIter-1)+ sampleOneIteration.N_o_influenced[objIdx])/numIter;
    	}*/
    	for(int objIdx=0; objIdx<numInfluencedObjects; objIdx++){
    		for(int opIdx=0; opIdx<numInfluencingObjects; opIdx++){
    			N_oop_bold_influenced[objIdx][opIdx] /= numIter;
    		}
    		for (int b=0; b<2; b++){
    			N_ob_influenced[objIdx][b] /= numIter;
    		}
    		for(int z=0; z< cmdOption.znum ; z++){
    			//System.out.println("numIter="+numIter+",objIdx="+objIdx+",z="+z+",oldvalue="+N_oz_bnew_influenced[objIdx][z]);
    			N_oz_bnew_influenced[objIdx][z] /= numIter;
    			//System.out.println("updated value="+N_oz_bnew_influenced[objIdx][z]);
    		}
    		
    		for(int a=0; a<cmdOption.anum; a++){
    			N_oa_influenced[objIdx][a] /= numIter;
    			for(int op = 0; op <numInfluencingObjects; op++){
    				N_oaop_influenced[objIdx][a][op] /= numIter;
    			}
    		}
    		N_o_influenced[objIdx] /= numIter;
    	}
    	
    }
    
    private void averageInfluencingSampleCount(int numIter)
    {
    	int numInfluencingObjects = in_influencing_wd[0].length;
    	
    	//Two counts for influencing objects
    	/*for(int objIdx=0; objIdx<numInfluencingObjects; objIdx++){
    		for(int z=0; z< cmdOption.znum ; z++){
    			N_oz_influencing[objIdx][z] = (N_oz_influencing[objIdx][z]*(numIter-1)+sampleOneIteration.N_oz_influencing[objIdx][z])/numIter;
    			NN_opz_bold_influencing[objIdx][z] = (NN_opz_bold_influencing[objIdx][z]*(numIter-1)+sampleOneIteration.NN_opz_bold_influencing[objIdx][z])/numIter; 
    		}
    		N_o_influencing[objIdx] =(N_o_influencing[objIdx]*(numIter-1)+sampleOneIteration.N_o_influencing[objIdx])/numIter;
    		NN_op_bold_influencing[objIdx] =(NN_op_bold_influencing[objIdx]*(numIter-1)+sampleOneIteration.NN_op_bold_influencing[objIdx])/numIter;
    	}*/
    	for(int objIdx=0; objIdx<numInfluencingObjects; objIdx++){
    		for(int z=0; z< cmdOption.znum ; z++){
    			N_oz_influencing[objIdx][z] /= numIter;
    			NN_opz_bold_influencing[objIdx][z] /= numIter; 
    		}
    		N_o_influencing[objIdx] /= numIter;
    		NN_op_bold_influencing[objIdx] /= numIter;
    	}
    }
 
    
    /**
	 * @return the sampleInfluenced_otzbop
	 */
	//public TreeMap<SampleElementInfluenced, Integer> getSampleInfluenced_otzbop() {
	//	return sampleInfluenced_otzbop_map;
	//}

   ////////////////////////////////////////////////////////////////////////////////////
   // Counts for objects who can INFLUENCE other objects
	//public double count_ZOInfluencing(int z, int oprime) {//countTDCited
	//	assert (oprime != -1) : z + " " + oprime;
	//   return N_oz_influencing[oprime][z]/ (double)averageBy;//tdCited.get(t, d), N'_{o,z}
	//}
	 
	//public double countOInfluencing(int obj) {//countDCited
	//	return N_o_influencing[obj] / (double)averageBy;//dCited.get(d),N'_{o}?
	//}
	 
	//public double countZOprimeBInnovInfluenced(int z, int oprime) {//countTCs0Citing
	//	return NN_opz_bold_influencing[oprime][z]/ (double)averageBy;
	    //return tcs0Citing.get(t, c) / averageBy;
	//}
	 
	//public double countOprimeB1Influenced(int oprime) {//countCs0Citing
	//    return NN_op_bold_influencing[oprime]/ (double)averageBy;//cs0Citing.get(c) / averageBy;
	//}
	 
    ////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////
	// Counts for objects who are INFLUENCED by some other objects
	// N_o
	//public double countObjIncluenced(int obj) {//countDCiting
	//	 return N_o_influenced[obj] / (double)averageBy;
	//}

	//N_{o,o',b=1}, for inherited drawing
	//public double count_OOprime_b1_influenced(int obj, int oprime) {//countDCs0Citing
	//	return N_oop_bold_influenced[obj][oprime]/ (double) averageBy;//dcs0Citing.get(d, c) 
	//}


	//N_{o,z,b=0}, for innovative drawing
	//public double count_OZ_b0_influenced(int obj, int z) {//countTDs1Citing
	//    return N_oz_bnew_influenced[obj][z]/ (double)averageBy;//tds1Citing.get(t, d) 
	//}
	
	//N_{o,b} 
	//public double count_ob_influenced(int obj, int b) {//countDs1Citing, countDs0Citing
	//    return N_ob_influenced[obj][b]/ (double)averageBy;//ds1Citing, eturn ds0Citing.get(d) 
	//}

	//N_{o,b} when b=1
	//public double countDs0Citing(int d) {//countDs0Citing
    //    return ds0Citing.get(d) / averageBy;//ds0Citing
    //}
	
	//public double count_ob_incluenced(int obj, int b) //countSDCiting
	//{	
		//When did I set N_o and N_ob
		//return Nbo_influenced[b][obj]/(double)averageBy;//return sdCiting.get(s, d) / averageBy;
	//	return N_ob_influenced[obj][b]/(double)averageBy;
    //}

	//N_{z,t}_all
	//public double count_ZT_All(int z, int token) {//countWTAll
	//	return N_zt_all[z][token] / (double)averageBy;//wtAll.get(w, t) 
	//}
	 
	//N_{z}_all
	//public double count_Z_All(int z) {//countTAll
	//    return N_z_all[z]/ (double)averageBy; //tAll.get(t)
	//}


}
