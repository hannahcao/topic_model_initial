package alfred;

import java.math.BigDecimal;
import java.util.Random;

import cern.jet.random.Binomial;
import cern.jet.random.engine.DRand;

public class Multinomial {

		
	/**
	 * TODO: need to add complete commenting
	 * Sample a multinomial distribution with the given parameters.
	 * int n - 
	 * */
public static int[] multinomialSample(int n, double [] prob, int K){
	int[] rN = new int[K];
	double pp;
	BigDecimal p_tot = new BigDecimal(0);
	//ArrayList<Double> rN = new ArrayList<Double>();
	
	for(int k = 0; k < K; k++) {
		pp = prob[k];
		//System.out.println(pp);
		//TODO: implement 
		//if (/*!R_FINITE(pp) ||*/ pp < 0. || pp > 1.) /*ML_ERR_ret_NAN(k)*/;
		//p_tot += pp;
		p_tot = p_tot.add(new BigDecimal(pp));
		//System.out.println("p_tot: "+p_tot);
		rN[k] = 0;
	}
	
	if(Math.abs(p_tot.doubleValue()) - 1. > 1e-7){
		System.out.println("probablity should be 1 but is " + Math.abs(p_tot.doubleValue()));
	}
	//TODO: more informative
	if(n==0){
		System.out.println("Failed at n=0");
		return rN;
	}
	//TODO: more informative
	if (K == 1 && p_tot.doubleValue() == 0.){
		System.out.println("Failed at k == 1 && p_tot == 0");
		return rN;
	}
	for(int k = 0; k < K-1; k++) { /* (p_tot, n) are for "remaining binomial" */
		//TODO: check for validity of this statement
		System.out.print(prob[k] + " ");
		System.out.println(p_tot);
		if(prob[k] > 0 ) {
//			System.out.println(prob[k]);
//			System.out.println(p_tot);
		    pp = prob[k] / p_tot.doubleValue();
		    /* printf("[%d] %.17f\n", k+1, pp); */
		    /*System.out.println("k+1: "+(k+1)+ ", pp: " + pp);*/
//		    rN[k] = ((pp < 1.) ? (int) rbinom((double) n,  pp) :
//			     /*>= 1; > 1 happens because of rounding */
//			     n);
		    Random rnd = new Random();
		    
		    DRand randomGenerator = new DRand(Math.abs(rnd.nextInt()));
		    System.out.println(n +", "+ pp);
		    Binomial b1 = new Binomial(n, pp, randomGenerator);
		    int x = b1.nextInt(n, pp);
		    //System.out.println(x);
		    rN[k] = ((pp < 1.) ? x : n);
		    
		    n -= rN[k];
		}
		else rN[k] = 0;
		if(n <= 0){
			/* we have all*/ return rN;
		}
		
		//p_tot -= prob[k]; /* i.e. = sum(prob[(k+1):K]) */
		p_tot = p_tot.subtract(new BigDecimal(prob[k]));
		
	    }
		rN[K-1] = n;
	
		return rN;
	}
}
