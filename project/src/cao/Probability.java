package cao;

public class Probability {

	private static int averageBy = 1;
	
	/**
     * For objects that we do not consider influence from others (influencing objects)
     * 
     * For obj's token, draw its new z
     * @param obj
     * @param token
     * @return
     */
    public static int getMAPNotInfluenced_z(CmdOption option,SampleData sampleData, int obj, int token)
    {
    	//mapTopicPosteriorDistr.clear();
    	MiniDistribution mapTopicPosteriorDistr = new MiniDistribution(option.znum * (option.oprimeNum + 1));
    	
    	//1. For each latent state, calculate its posterior probability p(z|others)
        for (int latentState = 0; latentState < option.znum; latentState++) {
            double prob = Probability.InfluencingPosterior_z(obj, token, latentState, sampleData,option);
            mapTopicPosteriorDistr.put(latentState, prob);
        }

        //2. make sure the posterior probablity is correct
        if (mapTopicPosteriorDistr.sum() == 0.0) {
            System.err.println("Posterior distribution.sum()==0.0. distr(10):" + mapTopicPosteriorDistr.toString());
            mapTopicPosteriorDistr.initializeEqualDistribution(option.znum);
        } //else {
        //    posteriorDistr.normalize();
        //}

        //3. Draw a new latent state
        int mapK = mapTopicPosteriorDistr.draw();
        //System.out.println(Debugger.getCallerPosition()+"mapTopicPosteriorDistr="+mapTopicPosteriorDistr+",mapK="+mapK);

        return mapK;
    }
    
    /**
     * For object that is influenced by others, 
     * obj's token, draw its new b, new o', and new z
     * 
     * @param obj, an influenced object
     * @param token, a token in the profile of obj
     * @return
     */
    public static int[] getMAPInfluenced_zbaop(CmdOption option, SampleData sampleData, int obj, int token)
	{
    	MiniDistribution mapTopicPosteriorDistr = new MiniDistribution(option.znum * (option.oprimeNum + 1)*option.anum);
        //System.out.println(Debugger.getCallerPosition()+"mapTopicPosteriorDistr\n"+mapTopicPosteriorDistr);
		//mapTopicPosteriorDistr.clear();

		//calculate the probability of p(z=*,oprime=*|b=innovative) when b is innovative
        for (int z = 0; z < option.znum; z++) {//for each latent state
        	for (int a=0; a < option.anum; a++){
        		for (int oprime : sampleData.in_refPubIndexIdList.get(obj)) { 
        			//for each influencing object, i.e., the objects in the reference list 
        			double probGj = Probability.influencedPosterior_zbopa
        					(obj, token, z, Constant.INHERITANCE, a, oprime, sampleData,option);
        			mapTopicPosteriorDistr.put(z, Constant.INHERITANCE, oprime, a, probGj);
        		}
        	}
        }

        //calculate the probability of p(z=*,oprime=*|b= NOT innovative) when b is NOT innovative
        for (int z = 0; z < option.znum; z++) {
            double probGj = Probability.influencedPosterior_zbopa
            		(obj,token, z, Constant.INNOTVATION, -1, -1, sampleData,option);            
            mapTopicPosteriorDistr.put(z, Constant.INNOTVATION, -1, probGj);
        }

        if (mapTopicPosteriorDistr.sum() == 0.0) {
            System.err.println("Posterior distribution.sum()==0.0. distr(10):" + mapTopicPosteriorDistr.toString());
            mapTopicPosteriorDistr.initializeEqualDistribution(option.znum);
        }
        //No need to normalize because the following draw() function will perform normalization
        //else {
        //	posteriorDistr.normalize();
        //}
        
        //randomly draw new z, new b, new o' which follow the joint distribution calculated above
        int mapZ = mapTopicPosteriorDistr.draw(); //mapT
        int mapB = mapTopicPosteriorDistr.getKey2Draw(); //mapS
        int mapOprime = mapTopicPosteriorDistr.getKey3Draw();//mapC
        int mapA = mapTopicPosteriorDistr.getKey4Draw();//mapA
        
        return new int[]{mapZ, mapB, mapA, mapOprime};
	}
    
    //p(z|...)= N1 *N2
	public static double InfluencingPosterior_z(int obj, int token, int latentState,
			final SampleData data,final CmdOption option) {//CitinfSampler.citedPosteriorT
        double theta = theta(latentState, obj, data, option); //N1
        double phi = phi(token, latentState, data, option); //N2
        
        double prob =  theta * phi;
        assert (prob >= 0.0) : "probG0 must be positive but is " + prob + 
        ". obj=" + obj + " token=" + token + " latentState=" + latentState;
        assert (!Double.isNaN(prob));
        assert (!Double.isInfinite(prob));
        
        return prob;
    }
	
	//p(o'|., b=1) = gamma * N1
	//p(zi|...,b=0) = N2 * N3
	//p(zi|...,b=1) = N2 * N1
	//p(bi=0|...) = lambda * N3
	//p(bi=1|...) = lambda * N1
	//citingPosteriorTSC
	/**
	 * b=1 inheritance
	 * p(z,b=1=inhe,op|.) = p(z|.b=1)*p(b=1|.)*p(op|)= (N2 *N1) (N1*lambda)*(N1*gamma)
	 * 
	 * b=0 innovation
	 * p(z,b=0=inno) = p(z|.) * p(b=0|.)= (N2 *N3) (N3*lambda)
	 * @param obj
	 * @param token
	 * @param z
	 * @param b
	 * @param oprime
	 * @param data
	 * @param DistributionParam
	 * @return
	 */
	public static double influencedPosterior_zbopa(int obj, int token, int z, int b, int a, int oprime, 
		 final SampleData data,final CmdOption DistributionParam) 
	{
		double prob = 0.0;
		if (b == Constant.INNOTVATION) {//for b=0, innovation 
			double lambda = beta(b, obj, data,DistributionParam); //p(b|.)
			double N3 = psi(z, obj, data,DistributionParam); //N3
			double N2 = phi(token, z, data,DistributionParam);//N2
			prob = lambda * N3 * N2; //should it be (N2*N3)*(N3*lambda)? TODO
		} else {//b=1, 
			double lambda = beta(b, obj, data,DistributionParam);//p(b|.)
			double N1 = theta(z, oprime, data,DistributionParam);//N1
			double N2 = phi(token, z, data,DistributionParam);
			double eta = eta(obj, a, oprime,  data,  DistributionParam);//aspect
			double gamma = gamma(obj, a, oprime, data,DistributionParam);
			prob = lambda * N1 * N2 * gamma*eta; //should it be (N1*N2)*(N1*lambda)*(N1*gamma) TODO 
		}
		
		assert (prob >= 0.0) : "probG0 must be positive but is " + prob + ". o=" + obj + " t=" + token + " z=" + z;
		assert (!Double.isNaN(prob));
		assert (!Double.isInfinite(prob));;
		return prob;
    }
	 
	 /*
	  * Calculate the right part of formula (16) and (17)
	  * p(b|...)
	  */
	 private static double beta(int b, int obj, final SampleData data,final CmdOption DistributionParam) {
        double num;
        
        //TODO: why not minus 1?
        if (b == Constant.INNOTVATION) {//b=0, p(bi=innovation| b not i, t, o', z), alpha1 = alphaLambdaInnov
            //num = data.count_ob_influenced(obj, b) + DistributionParam.alphaLambdaInnov;
        	num = data.N_ob_influenced[obj][b]/ (double)averageBy  + DistributionParam.alphaLambdaInnov;
        } else {//b=1,p(bi=inherit| b not i, t, o', z), alpha2 = alphaLambdaInherit
            //num = data.count_ob_influenced(obj, b) + DistributionParam.alphaLambdaInherit;
        	num = data.N_ob_influenced[obj][b]/ (double)averageBy + DistributionParam.alphaLambdaInherit;
        }
        
        //double den = data.countObjIncluenced(obj) + DistributionParam.alphaLambdaInherit + DistributionParam.alphaLambdaInnov;
        double den = data.N_o_influenced[obj] / (double)averageBy 
        		+ DistributionParam.alphaLambdaInherit + DistributionParam.alphaLambdaInnov;
        return num / den;
    }
	 
	 /**
	  * full condition probability for latent state part.
	  * @param z
	  * @param oprime
	  * @param data
	  * @param option
	  * @return
	  */
	 private static double theta(int z, int oprime, final SampleData data,final CmdOption option) {
        assert (z != -1) : z + " " + oprime;
        assert (oprime != -1) : z + " " + oprime;
        //TODO: why not minus 1
        
        //double num = data.count_ZOInfluencing(z, oprime) + data.countZOprimeBInnovInfluenced(z, oprime) + option.alphaTheta;
        //double den = data.countOInfluencing(oprime) + data.countOprimeB1Influenced(oprime) + data.getLatentStateNum() * option.alphaTheta;
        
        double num = data.N_oz_influencing[oprime][z]/ (double)averageBy 
        		+ data.NN_opz_bold_influencing[oprime][z]/ (double)averageBy 
        		+ option.alphaTheta;
        double den = data.N_o_influencing[oprime] / (double)averageBy 
        		+ data.NN_op_bold_influencing[oprime]/ (double)averageBy 
        		+ option.znum * option.alphaTheta;
        return num / den;
    }
	
	 //N3 = psi
	 private static double psi(int z, int obj, SampleData data,final CmdOption cmdOption) {
        //double num = data.count_OZ_b0_influenced(obj, z) + cmdOption.alphaPsi;
		//double den = data.count_ob_influenced(obj,0) + data.getLatentStateNum() * cmdOption.alphaPsi;
		
		double num = data.N_oz_bnew_influenced[obj][z]/ (double)averageBy + cmdOption.alphaPsi;
		double den = data.N_ob_influenced[obj][Constant.INNOTVATION]/ (double)averageBy 
				+ cmdOption.znum * cmdOption.alphaPsi;
        
        return num / den;
    }
	 
	//gamma: left side of p(o_i'|.)  formula
    public static double gamma(int objIdx, int aspect, int oprimeIdx, SampleData data,final CmdOption cmdOption) {
        //double num = data.count_OOprime_b1_influenced(obj, oprime) + cmdOption.alphaGamma;
        //double den = data.count_ob_influenced(obj,1) + data.getBibliographySize(obj) * cmdOption.alphaGamma;
    	
    	double num = data.N_oaop_influenced[objIdx][aspect][oprimeIdx]/ (double) averageBy + cmdOption.alphaGamma;
        double den = data.N_oa_influenced[objIdx][aspect]/ (double)averageBy 
        		+ data.getBibliographySize(objIdx) * cmdOption.alphaGamma;
        return num / den;
    }
    /**
     * 
     * @param objIdx
     * @param aspect
     * @param oprimeIdx
     * @param data
     * @param cmdOption
     * @return
     */
    private static double eta(int objIdx, int aspect, int oprimeIdx, SampleData data,final CmdOption cmdOption){
    	double num = data.N_oa_influenced[objIdx][aspect]/(double) averageBy + cmdOption.alphaEta;
    	double den = data.N_ob_influenced[objIdx][Constant.INHERITANCE] + data.N_oa_influenced[0].length*cmdOption.alphaEta;
    	return num/den;
    }
    
    //N2
    /**
     * Full conditional probability for token part.
     * @param token
     * @param z
     * @param data
     * @param option
     * @return
     */
    private static double phi(int token, int z, SampleData data,final CmdOption option) {
    	//count_ZT_All(z, token) = N_{z,t}(z,token) + N'_{z,t}(z,token)
    	//count_Z_All(z) = N_z(z) + N'_z(z)
        //double num = data.count_ZT_All(z, token) + option.alphaPhi;
    	//double den = data.count_Z_All(z) + data.getNumTokens() * option.alphaPhi;
    	double num = data.N_zt_all[z][token]/(double)averageBy + option.alphaPhi;
    	double den = data.N_z_all[z]/ (double)averageBy + data.getNumTokens() * option.alphaPhi;
        
        return num / den;
    }
}
