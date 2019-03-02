package cao;

import org.kohsuke.args4j.Option;

public class CmdOption {

	//@Option(name="-est", usage="Specify whether we want to estimate model from scratch")
	//public boolean est = false;
	
	@Option(name="-help", usage="Print this help info")
	public boolean help = false;
	
	@Option(name="-graphfile", usage="Input graph file name (default: empty)")
	public String graphfile = "";
	//public String graphfile = "data/pubidcite.txt"; 	
	
	@Option(name="-paperfolder", usage="Input paper folder (default: empty)")
	public String paperfolder = "";
	//public String paperfolder = "data/paper/";
	
	//@Option(name="-output", usage="Output file (default: empty)")
	//public String output = "";
	
	@Option(name="-znum", usage="Number of latent states or topics (default: 10)")
	public int znum = 10; 	
	
	@Option(name="-anum", usage="Number of latent aspects (default: 5)")
	public int anum = 5; 	
	
	@Option(name="-oprimeNum", usage="Maximum number of influencing objects (ref list length) for any given object  (default: 30)")
	public int oprimeNum=30;
	
	@Option(name="-numIter", usage="Number of Gibbs sampling iterations  (default: 1000)")
	int numIter = 1000;
	
	@Option(name="-burnin", usage="BURN IN iterations for Gibbs Sampling (default: 10)")
	int burnin = 10;
	
	@Option(name="-chainNum", usage="The number of chains used to judge convergence (default: 2)")
	int chainNum = 2;
	
	@Option(name="-rhat", usage="RHAT value for convergence (default: 1.01)")
	double R_HAT_THRESH = 1.01;

	@Option(name="-samplerId", usage="The sampler id string (default: Cao)")
	String SAMPLER_ID = "Cao";
	
    //Parameters for distributions p(token|latent-state)
	@Option(name="-alphaPhi", usage="Dirichlet parameter alphaPhi for latent variables (Default: 0.01)")
	public double alphaPhi=0.01;
	
	//Dirichlet for p(**|**)?
	@Option(name="-alphaPsi", usage="Dirichlet parameter alphaPsi (Default: 0.1)")
	public double alphaPsi=0.1;
	
	//Dirichlet for p(latent-state|object) 
	@Option(name="-alphaTheta", usage="Dirichlet parameter alphaTheta (Default: 0.1)")
	public double alphaTheta=0.1;
	
	@Option(name="-alphaGamma", usage="Dirichlet parameter alphaGamma for object interaction mixture (Default: 1.0)")
	public double alphaGamma=1.0;
	
	@Option(name="-alphaEta", usage="Dirichlet parameter alphaEta for aspect mixture (Default: 1.0)")
	public double alphaEta=1.0;
	
	
	@Option(name="-alphaLambdaInherit", usage="Dirichlet parameter inherit percentage (Default: 0.5)")
	public double alphaLambdaInherit=0.5;
	
	@Option(name="-alphaLambdaInnov", usage="Dirichlet parameter innovative percentage (Default: 0.5)")
	public double alphaLambdaInnov=0.5;
}
