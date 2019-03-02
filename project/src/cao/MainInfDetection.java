package cao;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

import cao.Debugger;

/**
 * o=10,op=1,inf=0.20739910313901344
o=10,op=2,inf=0.3501494768310912
o=10,op=3,inf=0.44245142002989535
o=11,op=3,inf=0.2508406186953598
o=11,op=4,inf=0.26899798251513113
o=11,op=5,inf=0.48016139878950914
o=12,op=6,inf=0.26951779563719863
o=12,op=7,inf=0.26521239954075776
o=12,op=8,inf=0.18053960964408727
o=12,op=9,inf=0.2847301951779564
o=13,op=1,inf=0.12343635025754232
o=13,op=2,inf=0.42420897718910966
o=13,op=8,inf=0.1265636497424577
o=13,op=9,inf=0.3257910228108904

CitinfSampler.java:313:doGibbs: Tue Sep 04 11:49:12 MDT 2012 CitinfSampler.java:315:doGibbs: 0 converged
CitinfSampler.java:313:doGibbs: Tue Sep 04 11:49:12 MDT 2012 CitinfSampler.java:315:doGibbs: 1 converged
citation mixture id 10 [(3:0.46615581098339715) (2:0.30622960124875837) (1:0.22761458776784446) ]
citation mixture id 11 [(5:0.43804926352271323) (4:0.28481247679168215) (3:0.2771382596856046) ]
citation mixture id 12 [(9:0.29133601478421567) (7:0.27198608544407) (6:0.24002608979236872) (8:0.19665180997934556) ]
citation mixture id 13 [(2:0.3834177865902916) (9:0.3752662113534553) (8:0.1259455092898583) (1:0.11537049276639495) ]

 * @author hcao
 *
 */

public class MainInfDetection {

	public static void showHelp(CmdLineParser parser){
		System.out.println("infdetection [options ...] [arguments...]");
		parser.printUsage(System.out);
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		CmdOption option = new CmdOption();
		CmdLineParser parser = new CmdLineParser(option);
		
		//1. get command line parameters 
		try {
			parser.parseArgument(args);
		} catch (CmdLineException e) {
			System.out.println(Debugger.getCallerPosition()+"Command line error: " + e.getMessage());
			showHelp(parser);
			return;	
		}
		
		if(option.help==true){
			showHelp(parser);
			return;
		}
		
		System.out.println(Debugger.getCallerPosition()+"graphfile="+option.graphfile
				+"\npaperfolder="+option.paperfolder);
		//System.exit(0);
		
		//2. create a sampler (set the parameters) 
        // Sampler sampler = new Sampler(option);
        
        //3. initialize all the initial structures for sampling 
        // (read and parse documents, create intermediate data structure, etc.) 
        //sampler.init(option.graphfile);
       
        //4. draw the initial sample 
        //sampler.drawInitSample();
        
        //5. Do Gibbs sampling
        //sampler.doGibbs();
        
		SamplerChain samplerChain = new SamplerChain(option);
		samplerChain.doGibbs();
		
        //6. Derive parameters
        Map<Integer, Map<Integer, Map<Integer, Double>>> gamma =samplerChain.getGamma();
        
        
        double[][] N_oa = samplerChain.getOAInfluenced();
        
        double[] N_o = samplerChain.getOInfluenced();
        
        double[][] N_oop = samplerChain.getOOP();
        
        double[][] N_ob = samplerChain.getOB();
        
        
        
//        prtResult2File(option,gamma, psi, N_oa, samplerChain.samplerToGetResult.totalIter,  samplerChain.samplerToGetResult.totalTime);
        prtResult2File(option,gamma, N_oa, N_o, N_oop, N_ob, samplerChain.samplerToGetResult.totalIter,  samplerChain.samplerToGetResult.totalTime);
        
        //System.out.println(Debugger.getCallerPosition()+"gamma:\n"+gamma);
//        if(option.graphfile.trim().length()==0)prtGamma(gamma);
//        prtGamma2File(option,gamma);
        System.out.println("\n"+Debugger.getCallerPosition()+"Finish program\n\n");
	}
	
	
	private static void prtResult2File(CmdOption option, Map<Integer, Map<Integer, Map<Integer, Double>>> gamma, 
		double[][] oa, double[] o, double[][] oop, double[][] ob, int totalIter, long totalTime)
	{
		String outputfile = Constant.DefaultResultOutputFolder+option.SAMPLER_ID+" a="+option.anum+".inf.xls";
		System.out.println(Debugger.getCallerPosition()+"print gamma values to file =" + outputfile);
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(new File(outputfile)));
			bw.append("Graphfile:\t"+option.graphfile+"\n");
			bw.append("Paperfolder:\t"+option.paperfolder+"\n");
			
			bw.append("Influence\n");
			bw.append("Object(influenced) \t aspect(influenced) \t op(influencing) \t influence\n");
			for(Map.Entry<Integer, Map<Integer, Map<Integer, Double>>> entry: gamma.entrySet()){
				int obj = entry.getKey();
				Map<Integer, Map<Integer, Double>> a2opMap = entry.getValue();
				for(Map.Entry<Integer, Map<Integer,Double>> a2opMapEntry: a2opMap.entrySet()){
					int a = a2opMapEntry.getKey();
					Map<Integer, Double> op2probMap = a2opMapEntry.getValue();
					for(Map.Entry<Integer, Double> op2probEntry : op2probMap.entrySet()){
						bw.append(obj+"\t"+a+"\t"+op2probEntry.getKey()+"\t"+String.format(Locale.US, "%1$.5f", op2probEntry.getValue())+"\n");
					}
				}
			}
			bw.append("\n");
			bw.append("N_oa\n");
			for(int i=0; i<oa.length; i++){
				for(int j=0; j<oa[0].length; j++){
					bw.append(String.format(Locale.US, "%1$.5f", oa[i][j])+"\t");
				}
				bw.append("\n");
			}
			bw.append("\n");
			bw.append("o\n");
			for(int i=0; i<o.length; i++){
				bw.append(String.format(Locale.US, "%1$.5f", o[i])+"\t");
			}
			bw.append("\n");
			bw.append("oop\n");
			for(int i=0; i<oop.length; i++){
				for(int j=0; j<oop[0].length; j++){
					bw.append(String.format(Locale.US, "%1$.5f", oop[i][j])+"\t");
				}
				bw.append("\n");
			}
			bw.append("\n");
			bw.append("ob\n");
			for(int i=0; i<ob.length; i++){
				for(int j=0; j<ob[0].length; j++){
					bw.append(String.format(Locale.US, "%1$.5f", ob[i][j])+"\t");
				}
				bw.append("\n");
			}
			
			bw.append("totalIter \t totalTime \n");
			bw.append(totalIter+"\t"+totalTime+"\n");

			bw.flush();
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * print the gamma values
	 * @param gamma
	 */
	private static void prtGamma2File(CmdOption option, Map<Integer, Map<Integer, Map<Integer, Double>>> gamma)
	{
		String outputfile = Constant.DefaultResultOutputFolder+option.SAMPLER_ID+".inf.xls";
		System.out.println(Debugger.getCallerPosition()+"print gamma values to file =" + outputfile);
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(new File(outputfile)));
			bw.append("Graphfile:\t"+option.graphfile+"\n");
			bw.append("Paperfolder:\t"+option.paperfolder+"\n");
			
			bw.append("Influence\n");
			bw.append("Object(influenced) \t aspect(influenced) \t op(influencing) \t influence\n");
			for(Map.Entry<Integer, Map<Integer, Map<Integer, Double>>> entry: gamma.entrySet()){
				int obj = entry.getKey();
				Map<Integer, Map<Integer, Double>> a2opMap = entry.getValue();
				for(Map.Entry<Integer, Map<Integer,Double>> a2opMapEntry: a2opMap.entrySet()){
					int a = a2opMapEntry.getKey();
					Map<Integer, Double> op2probMap = a2opMapEntry.getValue();
					for(Map.Entry<Integer, Double> op2probEntry : op2probMap.entrySet()){
						bw.append(obj+"\t"+a+"\t"+op2probEntry.getKey()+"\t"+op2probEntry.getValue()+"\n");
					}
				}
			}
			bw.flush();
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

}

