package alfred;


import topicextraction.citetopic.CiteTopicUtil;
import topicextraction.citetopic.sampler.ICiteInitializer;
import topicextraction.citetopic.sampler.RandomCiteInitializer;
import topicextraction.citetopic.sampler.VocDocHasher;
import topicextraction.citetopic.sampler.hdplda.MiniDistribution;
import topicextraction.querydb.IDocument;
import topicextraction.topicinf.ITopicInitializer;
import topicextraction.topicinf.RandomTopicInitializer;
import util.matrix.ArrayMatrix2D;
import util.matrix.IMatrix2D;
import util.matrix.LineMatrix3D;
import util.matrix.LineMatrix5D;



import org.apache.commons.collections.BidiMap;

import alfred.CitinfData;

import cern.jet.random.Beta;
import cern.jet.random.Binomial;
import cern.jet.random.engine.DRand;
import cern.jet.random.engine.RandomEngine;

import java.io.Serializable;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Random;

public class NewData {
	
	

	private double alpha_influ_1;
	private double alpha_innov_2;
	double[] oneOverZ;
	double[] oneOverCitedDocs;
	
	
	
    private int[][] n_zt; // phi
    private ArrayList<Integer> n_z;  // phi
    private ArrayList<Integer> n_ob_0;
    private ArrayList<Integer> n_ob_1; // theta
    private int[][][] n_ozb; // theta
    private int[][] n_ooprimeb_1; 
    
    private List<Integer> bSamples_t;
    private List<Integer> zSamples_t;
    private List<Integer> opSamples_t;
    //used token strings in same order as vocdoc
    private List<String> tokenStrings;
    
    private int[][] phi;
    private int[][] theta;
    private int[][] gamma;
    
    private HashMap<Integer, List<Integer>> pubId2CiteIds;
	private HashMap<Integer, IDocument> pubId2Docs;
    private List<List<Integer>> bibliographies;
    private Map<Integer, IDocument> citedDocs;
    private Map<Integer, IDocument> citingDocs;
    private IMatrix2D citing_to;
    private IMatrix2D cited_to;
    //private LineMatrix3D otzCited;
    private int[][] otzCited;
    private BidiMap citedDocsP2B;
    private BidiMap citingDocsP2B;
    
    private ArrayList<ArrayList<Integer>> oToTokenMapping;
    private HashMap<Integer, List<Integer>> objectToTokens;
    
    private final MiniDistribution mapTopicPosteriorDistr;
    
    private final VocDocHasher vocdoc;
	
	private int numTopics;
    private int numCites;
    private int numTokens;
    private int numCitingDocs;
    private int numCitedDocs;
    private double alphaPhi;
    private double alphaTheta;
    private double alphaPsi;
    private double alphaGamma;
//    private double alphaLambdaInherit;
//    private double alphaLambdaInnov;
    private int wMax;
    
    private int averageBy = 1;
	
		
	public NewData(HashMap<Integer, List<Integer>> pubId2CiteIds_pass, HashMap<Integer, IDocument> pubId2Docs_pass, int numTopics_pass, int numCites_pass, double alphaPhi_pass, double alphaPsi_pass, double alphaTheta_pass, double alphaLambdaInherit_pass, double alphaLambdaInnov_pass, double alphaGamma_pass){
		pubId2CiteIds = pubId2CiteIds_pass;
		pubId2Docs = pubId2Docs_pass;
		numTopics = numTopics_pass;
		numCites = numCites_pass;
		alpha_influ_1 = alphaLambdaInherit_pass;
		alpha_innov_2 = alphaLambdaInnov_pass;
		alphaGamma = alphaGamma_pass;
		alphaPsi = alphaPsi_pass;
		alphaPhi = alphaPhi_pass;
		alphaTheta = alphaTheta_pass;
		tokenStrings = new ArrayList<String>();
		
		
		//get citing and cited docs mapping
		citedDocs = initCitedDocs(pubId2CiteIds_pass, pubId2Docs_pass);
		citingDocs = initCitingDocs(pubId2CiteIds_pass, pubId2Docs_pass);
//		for(Integer j :citedDocs.keySet())
//			System.out.println(citedDocs.get(j).getDescription());
		System.out.println("Citing: "+citingDocs.size());
		oToTokenMapping = new ArrayList<ArrayList<Integer>>(citingDocs.size());
		
		
        //Huiping noted: put the document information to vecdoc
        //			from vecdoc, you can get the vocabularies, 
        vocdoc = new VocDocHasher(pubId2Docs, 2);
        
        wMax = vocdoc.getVNum();
        
        //set up mapping between documents.
        citedDocsP2B = vocdoc.createPubId2BugsId(citedDocs.keySet());
        cited_to = vocdoc.calculateWordDocumentMatrix(citedDocsP2B,
                new ArrayMatrix2D(wMax, citedDocs.size()));
        assert (cited_to.getDimensionSize(0) > 0);
        assert (cited_to.getDimensionSize(1) > 0);
        
        citingDocsP2B = vocdoc.createPubId2BugsId(citingDocs.keySet());
        citing_to = vocdoc.calculateWordDocumentMatrix(citingDocsP2B,
                new ArrayMatrix2D(wMax, citingDocs.size()));
        		assert (citing_to.getDimensionSize(0) > 0);
                assert (citing_to.getDimensionSize(1) > 0);
        //TODO: need to put data into bibliographies.
        bibliographies = new ArrayList<List<Integer>>();
        for (int i = 0; i < pubId2CiteIds.size(); i++) {
            bibliographies.add(new ArrayList<Integer>());
        }
        for (int citingPub : pubId2CiteIds.keySet()) {
            int citingBugs = vocdoc.getPubid2BugsId(citingPub, citingDocsP2B);
            List<Integer> bib = bibliographies.get(citingBugs);
            for (int citedPub : pubId2CiteIds.get(citingPub)) {
                int citedBugs = vocdoc.getPubid2BugsId(citedPub, citedDocsP2B);
                bib.add(citedBugs);
            }
        }
        

        //set up standard variables from document mapping.
        numCitingDocs = citingDocs.size();
        numCitedDocs = citedDocs.size();
        numTokens = cited_to.getDimensionSize(0);
        
        //get map for internals
        System.out.println("vocabulary inverse : "+vocdoc.getVocabInverse());
        System.out.println("vocabulary mapping : "+ vocdoc.getVocabulary());
        System.out.println(vocdoc.getDocInverse());
        //vocdoc.getOrigVocabInverse2Doc()
        //for(Object s: citingDocsP2B.mapIterator().)
        makeTokenMapping();
        int numObjects_ = objectToTokens.size();
        int numTokens_ = 0;
        for(Integer x : objectToTokens.keySet()){
        	for(Integer s : objectToTokens.get(x)){
        		numTokens_ ++;
        	}
        }
        //System.out.println("num Tokens: ->"+numTokens_);
        
        //create latent mapping matrix. z_j,k 
        //TODO: need to find out what to do with the sampling.
        //otzCited = new int[][];
        //dwtscCiting = new LineMatrix5D(numTokens);
        
        //set up 1/z distribution for multinomial
		oneOverZ = new double[numTopics];
        for (int i = 0; i < oneOverZ.length; i++ ){
        	oneOverZ[i] = 1.0/numTopics;
        }
        //set up 1/Cited Docs
		oneOverCitedDocs = new double[numCitedDocs];
        for (int i = 0; i < oneOverCitedDocs.length; i++){
        	oneOverCitedDocs[i] = 1.0/numCitedDocs;
        }
        
        //initialize counts
        n_ob_0 = new ArrayList<Integer>(Collections.nCopies(numCitingDocs, 0));
        n_ob_1 = new ArrayList<Integer>(Collections.nCopies(numCitingDocs, 0));
        n_ooprimeb_1 = new int[numCitingDocs][numCitedDocs];
        n_ozb = new int[numCitingDocs][numTopics][2];
        n_z = new ArrayList<Integer>(Collections.nCopies(numTopics, 0));
        n_zt = new int[numTopics][numTokens_];
        
        bSamples_t = new ArrayList<Integer>(Collections.nCopies(numTokens_, 0));
        zSamples_t = new ArrayList<Integer>(Collections.nCopies(numTokens_, 0));
        opSamples_t = new ArrayList<Integer>(Collections.nCopies(numTokens_, 0));

        //System.out.println(numCitingDocs);
        
        
        //step 2 initialize the latent variables.
        initializeData();
        
        printCounts();
        
        //used for gibbs sampling
        mapTopicPosteriorDistr = new MiniDistribution(numTopics * (numCites + 1));
        
	}
	
	/**
	 * Initializing Data
	 * Step 2 of algorithm: Initialize the latent variables.
	 */
	private void initializeData() {
		final ITopicInitializer tinit = new RandomTopicInitializer();
        final ICiteInitializer cinit = new RandomCiteInitializer();
        
        int o = 0;
        for(Integer object_id: objectToTokens.keySet()){
        	int t = 0;
        	for(Integer token_id: objectToTokens.get(object_id)){

	        	
	        	
	        	//TODO: Check here. Javadoc has reverse ordering for t and o.
	        	//int newT = tinit.initialTopic(w, d, 1, kMaxVal);
	        	//int newLatent = tinit.initialTopic(t, o, 1, numTopics);
	        	Random rnd = new Random();
	        	DRand randomGenerator = new DRand(Math.abs(rnd.nextInt()));
			    Binomial bSample = new Binomial(1, alpha_influ_1, randomGenerator);
			    int b = bSample.nextInt();
			    int[] zsample = Multinomial.multinomialSample(1, oneOverZ, numTopics);
			    int z = 0;
			    //b = 0
			    while (zsample[z] <= 0){
			    	z++;
			    }
			    bSamples_t.set(t, b);
			    zSamples_t.set(t, z);
	        	if(b==0){
	        		
	        		
	        		//System.out.print("New Z:");
	        		//int z = 0;
	        		
	        		//System.out.println("while z,i: "+zsample[z] + ", "+ z);
	        		//increment n_ozb=0, n_ob=0
	        		//n_ozb[][][0]
	        		n_ozb[o][z][0]+=1;
	        		n_ob_0.set(o, (n_ob_0.get(o).intValue() + 1));
	        		
	        		
	        	}
	        	//b = 1
	        	else{
	        		//int[] zsample = Multinomial.multinomialSample(1, oneOverZ, numTopics);
	        		//int z = 0;
	        		int[] oPrimeSample = Multinomial.multinomialSample(1, oneOverCitedDocs, numCitedDocs);
	        		
	        		int op = 0;
	        		while (oPrimeSample[op] <= 0){
	        			op++;
	        		}
	        		//set op sample mapping
	        		opSamples_t.set(t, op);
	        		
	        		
	        		n_ooprimeb_1[o][op] += 1;
	        		n_ozb[o][z][1] += 1;
	        		n_ob_1.set(o, (n_ob_1.get(o).intValue() + 1));
	        		//increment nooprimeb=1, n_ozb=1, nob=1
	        	}
	        	n_zt[z][t] += 1;
	        	n_z.set(z, n_z.get(z).intValue() +1);
	        	//System.out.println(bSample[o][t]);
	        	
	        	
	        	
	        	
	        	//n_z.get(index);
	        	//System.out.println("o: "+o+", t: "+t + "newTopic value: " + newT);
	        	//addCited_zbo(samplingBMap[0], samplingBMap[1], samplingBMap[2]);
	        	t++;
        	}
        	o++;
        }
        
//        
//        
//        
//        for(int o = 0; o < numCitingDocs; o++){
//	        for(int t = 0; t < numTokens; t++){
//	        	
//	        	
//	        	
//	        	//TODO: Check here. Javadoc has reverse ordering for t and o.
//	        	//int newT = tinit.initialTopic(w, d, 1, kMaxVal);
//	        	//int newLatent = tinit.initialTopic(t, o, 1, numTopics);
//	        	Random rnd = new Random();
//	        	DRand randomGenerator = new DRand(Math.abs(rnd.nextInt()));
//			    Binomial bSample = new Binomial(1, alpha_influ_1, randomGenerator);
//			    int b = bSample.nextInt();
//			    int[] zsample = Multinomial.multinomialSample(1, oneOverZ, numTopics);
//			    int z = 0;
//			    //b = 0
//			    while (zsample[z] <= 0){
//			    	z++;
//			    }
//			    bSamples_t.set(t, b);
//			    zSamples_t.set(t, z);
//	        	if(b==0){
//	        		
//	        		
//	        		//System.out.print("New Z:");
//	        		//int z = 0;
//	        		
//	        		//System.out.println("while z,i: "+zsample[z] + ", "+ z);
//	        		//increment n_ozb=0, n_ob=0
//	        		//n_ozb[][][0]
//	        		n_ozb[o][z][0]+=1;
//	        		n_ob_0.set(o, (n_ob_0.get(o).intValue() + 1));
//	        		
//	        		
//	        	}
//	        	//b = 1
//	        	else{
//	        		//int[] zsample = Multinomial.multinomialSample(1, oneOverZ, numTopics);
//	        		//int z = 0;
//	        		int[] oPrimeSample = Multinomial.multinomialSample(1, oneOverCitedDocs, numCitedDocs);
//	        		
//	        		int op = 0;
//	        		while (oPrimeSample[op] <= 0){
//	        			op++;
//	        		}
//	        		//set op sample mapping
//	        		opSamples_t.set(t, op);
//	        		
//	        		
//	        		n_ooprimeb_1[o][op] += 1;
//	        		n_ozb[o][z][1] += 1;
//	        		n_ob_1.set(o, (n_ob_1.get(o).intValue() + 1));
//	        		//increment nooprimeb=1, n_ozb=1, nob=1
//	        	}
//	        	n_zt[z][t] += 1;
//	        	n_z.set(z, n_z.get(z).intValue() +1);
//	        	//System.out.println(bSample[o][t]);
//	        	
//	        	
//	        	
//	        	
//	        	//n_z.get(index);
//	        	//System.out.println("o: "+o+", t: "+t + "newTopic value: " + newT);
//	        	//addCited_zbo(samplingBMap[0], samplingBMap[1], samplingBMap[2]);
//	        }//end for
//		}//end for
//        
        
	}
	
	/**
	 * doGibbs - one iteration of Gibbs sampling.
	 */
	public void doGibbs(){
		
		int o = 0;
        for(Integer object_id: objectToTokens.keySet()){
        	int t = 0;
        	for(Integer token_id: objectToTokens.get(object_id)){
	        	int b = bSamples_t.get(t);
	        	int z = zSamples_t.get(t);
        		int op = opSamples_t.get(t);
	        	System.out.println("b: "+b);
	        	System.out.println("z: "+z);
	        	System.out.println("op: "+op);
	        	System.out.println("o: "+o);
	        	System.out.println("t: "+t);
			    
			    n_ozb[o][z][b] -= 1;
			    n_zt[z][t] -= 1;
			    n_z.set(z, n_z.get(z)-1);
			    if(b == 0){
			    	n_ob_0.set(o, n_ob_0.get(0)-1);
			    }else{
			    	n_ob_1.set(o, n_ob_0.get(0)-1);
			    	n_ooprimeb_1[o][op] -= 1;
			    }
			    
			    //sample equations here

		    	int op_new = 0;
		    	int b2 = 0;
		    	int z_new = 0;
		    	
		    	
        		//update equation 16
		    	double b_sample1 = N3(o, z) * LambdaInfluence(o);
        		//update equation 17
        		double b_sample2 = N1(o, z) * LambdaInnovation(o);
        		int[] b2Sample = Multinomial.multinomialSample(1, new double[]{b_sample1,b_sample2 }, 2);
		    	
		    	if(b2Sample[0] == 1){
		    		b2 = 1;
		    	}
		    	double zprime_sample = N2(t,z) * N3(o,z);
		    	oneOverZ[z] = zprime_sample;
		    	int[] zprimeSample = Multinomial.multinomialSample(1, oneOverZ, numTopics);
		    	while (zprimeSample[z_new] <= 0){
			    	z_new++;
			    }
		    	
	        	if(b == 0){

			    	n_ob_0.set(o, n_ob_0.get(0)+1);
			    	//System.out.println(zprime_sample);
	        	}else {
	        		
	        		double oprime_sample = Gamma(o, op, t, z) * N1(o,z);
	        		double [] oprimeSampleOverNumTopics= new double[numCitedDocs];
	        		for(int i = 0; i < oprimeSampleOverNumTopics.length; i++){
			    		oprimeSampleOverNumTopics[i] = oprime_sample;
			    	}
	        		int[] oPrimeSample = Multinomial.multinomialSample(1, oprimeSampleOverNumTopics, numCitedDocs);
	        		
	        		
	        		while (oPrimeSample[op_new] <= 0){
	        			op_new++;
	        		}
	        		
	        		n_ooprimeb_1[o][op_new] += 1;
	        		n_ob_1.set(o, n_ob_0.get(0)+1);
	        	}
	        	n_ozb[o][z_new][b2] += 1;
			    n_zt[z_new][t] += 1;
			    n_z.set(z_new, n_z.get(z_new)+1);
	        	
	        	
			    t++;
        	}
        	o++;
        }
		
	}
	
	private double N1(int o, int z){
		int b = 1;
		int ret = 0;
		
		double num = n_ozb[o][z][0] + n_ozb[o][z][1] + alphaTheta -1;
		double den = n_ob_0.get(o) + n_ob_1.get(o) + numTopics*alphaTheta -1;
		
		double d_ret = num/den;
		return d_ret;
//		ret = (int) d_ret;
//		
//		return ret;		
	}
	
	private double N2(int t, int z){
		int ret = 0;
		double num = n_zt[z][t] + alphaPhi -1;
		double den = n_z.get(z) + numTokens * alphaPsi -1;
		
		double d_ret = num/den;
		return d_ret;
//		ret = (int) d_ret;
//		
//		return ret;
	}
	
	private double N3(int o, int z){
		int ret = 0;
		
		double num = n_ozb[o][z][0] + alphaPhi -1;
		double den = n_ob_0.get(o) + numTokens*alphaPhi -1;
		double d_ret = num/den;
		return d_ret;
//		ret = (int) d_ret;
//		
//		return ret;
		
	}
	
	private double LambdaInfluence(int o){
		int ret = 0;
		
		double num = n_ob_1.get(o) +alpha_innov_2 -1;
		double den = n_ob_0.get(o) + alpha_influ_1 + alpha_innov_2 -1;
		
		double d_ret = num/den;
		return d_ret;
//		ret = (int)d_ret;
//		
//		return ret;
	}
	
	private double LambdaInnovation(int o){
		int ret = 0;
		
		double num = n_ob_0.get(o) +alpha_innov_2 -1;
		double den = n_ob_0.get(o) + alpha_influ_1 + alpha_innov_2 -1;
		
		double d_ret = num/den;
		return d_ret;
//		ret = (int)d_ret;
//		
//		return ret;
	}
	
	private double Gamma(int o,int op, int t, int z){
		int ret = 0;
		final int b = 1;
		
		double num = n_ooprimeb_1[o][op] + alphaGamma -1;
		double den = n_ob_1.get(o) + numCitedDocs*alphaGamma -1;
		
		double d_ret = num/den;
		return d_ret;
//		ret = (int)d_ret;
//		
//		return ret;
	}
	
	
	
	
	private void addCited_zbo(int z, int b, int o){
		
		
	}
	
	
	/**
	 * Get new distribution variables.
	 * @param o
	 * @param t
	 * @return array of 3 variables, T= topics (z), S = new Bernoulli variable (b), C = new citing object (o)
	 * 			zbo
	 */
    private int[] getMAPCitingTSC(int o, int t) {
        mapTopicPosteriorDistr.clear();

        for (int z = 0; z < numTopics; z++) {
            for (int c : bibliographies.get(o)) {
                double probGj = citingPosteriorTSC(o, t, z, 0, c);
                assert (probGj >= 0.0) : "probG0 must be positive but is " + probGj + ". d=" + o + " w=" + t + " t=" + z;
                assert (!Double.isNaN(probGj));
                assert (!Double.isInfinite(probGj));
                mapTopicPosteriorDistr.put(z, 0, c, probGj);
            }
        }

        for (int z = 0; z < numTopics; z++) {
            double probGj = citingPosteriorTSC(o, t, z, 1, -1);
            assert (probGj >= 0.0) : "probG0 must be positive but is " + probGj + ". d=" + o + " w=" + t + " t=" + z;
            assert (!Double.isNaN(probGj));
            assert (!Double.isInfinite(probGj));
            mapTopicPosteriorDistr.put(z, 1, -1, probGj);
        }

        if (mapTopicPosteriorDistr.sum() == 0.0) {
            System.err.println("Posterior distribution.sum()==0.0. distr(10):" + mapTopicPosteriorDistr.toString());
            mapTopicPosteriorDistr.initializeEqualDistribution(numTopics);
        } else {
//            posteriorDistr.normalize();
        }

        int mapT = mapTopicPosteriorDistr.draw();
        int mapS = mapTopicPosteriorDistr.getKey2Draw();
        int mapC = mapTopicPosteriorDistr.getKey3Draw();


        return new int[]{mapT, mapS, mapC};
    }
	
    private double citingPosteriorTSC(int d, int w, int t, int s, int c) {
        if (s == 0) {
            double lambda = lambda(s, d);
            double theta = theta(t, c);
            double phi = phi(w, t);
            //double gamma = gamma(d, c);
            //return lambda * gamma * theta * phi;
            return lambda * theta * phi;
        } else {
            double lambda = lambda(s, d);
            double psi = psi(t, d);
            double phi = phi(w, t);
            return lambda * psi * phi;
        }
    }
    
    private double phi(int t, int z) {
        double num = n_zt[z][t] + alphaPhi;
        double den = n_z.get(z) + numTokens * alphaPhi;
        return num / den;
    }
    
    private double theta(int o, int z){
    	assert (z != -1) : z + " " + o;
        assert (o != -1) : z + " " + o;

        double num = n_ozb[o][z][0] + n_ozb[o][z][1] + alphaTheta;
        double den = n_ob_0.get(o) + n_ob_1.get(o) + numTopics * alphaTheta;
        return num / den;
    }
    
    private double lambda(int b, int o){
    	double num;
        if (b == 0) {
            num = n_ob_0.get(o) + alpha_innov_2;
        } else {
            num = n_ob_1.get(o) + alpha_influ_1;
        }
        double den = n_ob_0.get(o) + alpha_influ_1 + alpha_innov_2;
        return num / den;
    }
    
    private double psi(int o, int z){
    	double num = n_ozb[o][z][0] + alphaPsi;
        double den = n_ob_0.get(o) + numTopics * alphaPsi;
        return num / den;
    }
	
    
    /**
     * TODO: format into normal toString for each variable
     */
	public String printCounts(){
		String str = "";
//		private int[][] n_zt; 
//	    private ArrayList<Integer> n_z;  
//	    private ArrayList<Integer> n_ob_0;
//	    private ArrayList<Integer> n_ob_1;
//	    private int[][][] n_ozb;
//	    private int[][] n_ooprimeb_1; 
		
//		System.out.println("n_zt");
//		for(int i = 0; i < n_zt.length; i++){
//			System.out.println("z:"+ i);
//			for (int j = 0; j < n_zt[0].length; j ++){
//				System.out.print("t:"+j+ " = " + n_zt[i][j]+ " ");
//			}
//			System.out.println();
//		}
//		
//		System.out.println("n_ozb");
//		for(int i = 0; i < n_ozb.length; i++){
//			System.out.println("o:"+i);
//			for(int j = 0; j < n_ozb[0].length; j++){
//				System.out.print("z"+j);
//				for(int k = 0; k < n_ozb[0][0].length; k++){
//					System.out.print("z"+j+",b"+k + " = " + n_ozb[i][j][k]+ " " );
//				}
//				System.out.println();
//			}
//			System.out.println();
//		}
		System.out.println("n_zt");
		System.out.println("z; t ->");
		for(int i = 0; i < n_zt.length; i++){
			//System.out.println("z:"+ i);
			for (int j = 0; j < n_zt[0].length; j ++){
				System.out.print(n_zt[i][j]+ " ");
			}
			System.out.println();
		}
		
		System.out.println("n_ozb");
		for(int i = 0; i < n_ozb.length; i++){
			System.out.println("o:"+i);
			System.out.println("z; b ->");
			for(int j = 0; j < n_ozb[0].length; j++){
				//System.out.print("z"+j);
				for(int k = 0; k < n_ozb[0][0].length; k++){
					System.out.print(n_ozb[i][j][k]+ " " );
				}
				System.out.println();
			}
			System.out.println();
		}
		
		System.out.println("n_ob_0: ");
		System.out.println();
		for(Integer i : n_ob_0){
			System.out.print(i + " ");
		}
		System.out.println();
		
		System.out.println("n_ob_1: ");
		System.out.println();
		for(Integer i : n_ob_1){
			System.out.print(i + " ");
		}
		System.out.println();
		
		System.out.println("n_z: ");
		System.out.println();
		for(Integer i : n_z){
			System.out.print(i + " ");
		}
		System.out.println();
		
		System.out.println("n_ooprimeb_1:");
		System.out.println("o; op ->");
		for(int i = 0; i < n_ooprimeb_1.length; i++){
			//System.out.println("z:"+ i);
			for (int j = 0; j < n_ooprimeb_1[0].length; j ++){
				System.out.print(n_ooprimeb_1[i][j]+ " ");
			}
			System.out.println();
		}
		
		
		return str;
	}
	public void makeTokenMapping(){
		int iz = 0;
		objectToTokens = new HashMap<Integer, List<Integer>>();
		
		for(Integer x : citingDocs.keySet()){
			//System.out.println(x);
			objectToTokens.put(x, new ArrayList<Integer>());
			
		}
		iz= 0;
		//System.out.println("Tokens used more than once mapping back to original doc ids:");
		for(String s: vocdoc.getVocabInverse()){
			//System.out.print(iz +": " + vocdoc.getOrigVocabulary().get(s) + ", ");
			int x = 0;
			//System.out.print("\t"+vocdoc.getVocabInverse().get(iz));
			//System.out.print("\t doc_ids: ");
			for(Integer i : vocdoc.getOrigVocabInverse2Doc().get(iz)){
				//System.out.print("\t" + i + " ");
				if(objectToTokens.containsKey(i)){
					objectToTokens.get(i).add(iz);
					//tokensFromObject.add(iz);
					//System.out.println();
				}
				x++;
				
			}
			//System.out.println();
			iz++;
		}
//		int count = 0;
//		//print out objects to token mapping
//		for(Integer x : objectToTokens.keySet()){
//			System.out.print(x + ": ");
//			for(Integer z: objectToTokens.get(x)){
//				System.out.print(z + " ");
//				count++;
//			}
//			System.out.println();
//		}
//		System.out.println("count: "+count);
	}
	
	public void printTokenMapping(){
		int iz = 0;
		
//    	System.out.println(vocdoc.getOrigVocabInverse2Doc().size());
//      for(int i = 0; i < vocdoc.getOrigVocabInverse2Doc().size(); i ++){
//      	//System.out.println(vocdoc.getOrigVocabulary().keySet());
//      	System.out.println(vocdoc.getOrigVocabInverse2Doc().get(i));
//      }
//      for(String s : vocdoc.getOrigVocabulary().keySet()){
//      	System.out.print(s);
//      	System.out.println(": "+vocdoc.getOrigVocabulary().get(s));
//      }
      
//      System.out.println(vocdoc.getOrigVocabInverse2Doc().size());
//      for(int i = 0; i < vocdoc.getOrigVocabInverse2Doc().size(); i ++){
//      	//System.out.println(vocdoc.getOrigVocabulary().keySet());
//      	System.out.println(vocdoc.getOrigVocabInverse2Doc().get(i));
//      }
//      for(IDocument x:  vocdoc.getDocInverse().keySet()){
//      	System.out.print(x.getId());
//      	System.out.println(": "+vocdoc.getDocInverse().get(x));
//      }
//      for(IDocument x : vocdoc.getDocs()){
//      	System.out.print(iz + ": ");
//      	System.out.println(x.getId());
//      	iz++;
//      }
      
//      for(String s : vocdoc.getVocabulary().keySet()){
//      	System.out.print(s);
//      	System.out.println(": "+vocdoc.getVocabulary().get(s));
//      }
      
//      iz = 0;
//      for(String s: vocdoc.getVocabInverse()){
//      	System.out.println(iz + ": " + s);
//      	iz++;
//      }
//		iz= 0;
//		for(List<Integer> L: vocdoc.getOrigVocabInverse2Doc()){
//			int x = 0;
//			for(Integer i : L){
//				System.out.println("outer: "+iz +", inner: "+ x +", integer: "+ i);
//				x++;
//				
//			}
//			iz++;
//		}
//		System.out.println(vocdoc.getOrigVocabInverse().size());
//		iz= 0;
//		for(String s : vocdoc.getOrigVocabInverse()){
//			System.out.println(iz +": "+s);
//			iz++;
//		}
		

		objectToTokens = new HashMap<Integer, List<Integer>>();
		
		for(Integer x : citingDocs.keySet()){
			//System.out.println(x);
			objectToTokens.put(x, new ArrayList<Integer>());
			
		}
		
		
		
		iz= 0;
		//System.out.println("Tokens used more than once mapping back to original doc ids:");
		for(String s: vocdoc.getVocabInverse()){
			//System.out.print(iz +": " + vocdoc.getOrigVocabulary().get(s) + ", ");
			int x = 0;
			//System.out.print("\t"+vocdoc.getVocabInverse().get(iz));
			//System.out.print("\t doc_ids: ");
			for(Integer i : vocdoc.getOrigVocabInverse2Doc().get(iz)){
				//System.out.print("\t" + i + " ");
				if(objectToTokens.containsKey(i)){
					objectToTokens.get(i).add(iz);
					//tokensFromObject.add(iz);
					//System.out.println();
				}
				x++;
				
			}
			System.out.println();
			iz++;
		}
		
		//print out objects to token mapping
//		for(Integer x : objectToTokens.keySet()){
//			System.out.print(x + ": ");
//			for(Integer z: objectToTokens.get(x)){
//				System.out.print(z + " ");
//			}
//			System.out.println();
//		}
		
		
		//print out docs id
//		iz=0;
//		for(IDocument s : vocdoc.getDocs()){
//			System.out.println(vocdoc.getDocs().get(iz).getId());
//			iz++;
//		}
		
		
		//print out 
//		for ( List<Integer> list : vocdoc.getOrigVocabInverse2Doc()){
//        	System.out.print(iz + ": ");
//        	for(Integer d: list){
//        		System.out.print(d + " ");
//        	}
//        	System.out.println();
//        	iz++;
//        }
	}
	
	
	/**
     * Huiping noted 2012-02-29
     * Get all the cited documents cited_paper_id:cited_paper_content
     *  
     * @param pubId2CiteIds: map for paper_id:cited_paper_id
     * @param pubId2Docs: map for paper_id: paper_content
     * @return the cited documents information
     */
    private Map<Integer, IDocument> initCitedDocs(Map<Integer, List<Integer>> pubId2CiteIds, Map<Integer, IDocument> pubId2Docs) {
        Map<Integer, IDocument> citingDocs = new HashMap<Integer, IDocument>();
        for (List<Integer> cList : pubId2CiteIds.values()) {
            for (int id : cList) {
                if (!citingDocs.containsKey(id)) {
                    IDocument doc = pubId2Docs.get(id);
                    assert (doc != null);
                    citingDocs.put(id, doc);
                }
            }
        }
        return citingDocs;
    }
	
	/**
     * Huiping noted 2012-02-29
     * Get all the information about the papers that cite others
     * 
     * @param pubId2CiteIds: map for paper_id:cited_paper_id
     * @param pubId2Docs: map for paper_id: paper_content
     * @return the citing papers' information in a map citing_paper_id:citing_paper_content
     */
    private Map<Integer, IDocument> initCitingDocs(Map<Integer, List<Integer>> pubId2CiteIds, Map<Integer, IDocument> pubId2Docs) {
        Map<Integer, IDocument> citedDocs = new HashMap<Integer, IDocument>();
        for (int id : pubId2CiteIds.keySet()) {
            IDocument doc = pubId2Docs.get(id);
            assert (doc != null);
            citedDocs.put(id, doc);
        }
        return citedDocs;
    }
	
}
