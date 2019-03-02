package cao;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
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
				+"\npaperfolder="+option.paperfolder+"\naspectfile="+option.aspectfile);
		//System.exit(0);

		
		//2. do Gibbs sampling to estimate parameters on all data.
		Set<Integer> emptyTestSet = new HashSet<Integer>();
		SamplerChain samplerChain = new SamplerChain(option, emptyTestSet);
		samplerChain.doGibbs();
		
		Map<Integer, Map<Integer, Double>> N_oa;
		
		if(option.model.equals("oaim"))
			N_oa= samplerChain.getOTAInfluenced();
		else
			N_oa = samplerChain.getATAInfluenced();
		
		double[] N_o = samplerChain.getOInfluenced();
		Map<Integer, Map<Integer, Double>> N_oop = samplerChain.getOOP();
		Map<Integer, Map<Integer, Double>> N_ob = samplerChain.getOB();
		Map<Integer, Map<Integer, Map<Integer, Double>>> N_oaop = samplerChain.getOTAOPInfluenced();
		Map<Integer, Map<Integer, Map<Integer, Double>>> gamma =samplerChain.getGamma();
		
//        if(option.graphfile.trim().length()==0)prtGamma(gamma);
		
		//3. split the data set to training and test.  Use a 10-fold cross-validation and average the likelihood 
		//on test data
        
		
//		double likelihood = samplerChain.getLogLikelihoodAll();
		
		double likelihood = 0;
		//TODO: mark here
//		List<Set<Integer>> crossValiSet = crossValidation(option, 10);
//		for(Set<Integer> test : crossValiSet){
//			SamplerChain c = new SamplerChain(option, test);
//			c.doGibbs();
//			double llh = c.getLogLikelihoodAll();
//			Map<Integer, Map<Integer, Double>> N_oa;
//			N_oa = c.getOTAInfluenced();
//			double[] N_o = c.getOInfluenced();
//			Map<Integer, Map<Integer, Double>> N_oop = c.getOOP();
//			Map<Integer, Map<Integer, Double>> N_ob = c.getOB();
//			Map<Integer, Map<Integer, Map<Integer, Double>>> N_oaop = c.getOTAOPInfluenced();
//			
//			likelihood+=llh;
//			
//			prtResult2File(option, c, N_oa, N_o, N_oop, N_ob, N_oaop, c.samplerToGetResult.totalIter,  c.samplerToGetResult.totalTime, likelihood);
//			
//			break;//TODO comment later
//		}
//		likelihood /= 10;
		
		prtResult2File(option, samplerChain, N_oa, N_o, N_oop, N_ob, N_oaop, samplerChain.samplerToGetResult.totalIter,  samplerChain.samplerToGetResult.totalTime, likelihood);
        
        System.out.println("\n"+Debugger.getCallerPosition()+"Finish program\n\n");
	}
	
	/**
	 * print the gamma values
	 * @param gamma
	 */
	private static void prtGamma(Map<Integer, Map<Integer, Map<Integer, Double>>> gamma)
	{
		System.out.println(Debugger.getCallerPosition()+"print gamma values...");
		for(Map.Entry<Integer, Map<Integer, Map<Integer, Double>>> entry: gamma.entrySet()){
			
			int obj = entry.getKey();
			Map<Integer, Map<Integer, Double>> a2opMap = entry.getValue();
			for(Map.Entry<Integer, Map<Integer,Double>> a2opEntry: a2opMap.entrySet()){
				int a = a2opEntry.getKey();
				Map<Integer,Double> op2probMap = a2opEntry.getValue();				
				for(Map.Entry<Integer, Double> op2probEntry : op2probMap.entrySet()){
					System.out.println("o="+obj+",a="+a+",op="+op2probEntry.getKey()+",inf="+op2probEntry.getValue());
				}
			}
		}
	}
	
	/**
	 * print the gamma values
	 * @param gamma
	 */
	private static void prtResult2File(CmdOption option, SamplerChain chain, 
			Map<Integer, Map<Integer, Double>> psi, double[] o, Map<Integer, Map<Integer, Double>> oop, 
			Map<Integer, Map<Integer, Double>> ob, 
			Map<Integer, Map<Integer, Map<Integer, Double>>> oaop,
			int totalIter, long totalTime, double llh)
	{
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
		Date date = new Date();
		
		String outputfile = Constant.DefaultResultOutputFolder+option.SAMPLER_ID+" duplicate="+(option.duplicate.equals("yes")?"yes":"no")+" znum "+option.znum+" time="+dateFormat.format(date)+".inf.xls";
		System.out.println(Debugger.getCallerPosition()+"print gamma values to file =" + outputfile);
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(new File(outputfile)));
			bw.append("Graphfile:\t"+option.graphfile+"\n");
			bw.append("Paperfolder:\t"+option.paperfolder+"\n");
			
			bw.append("Influence\n");
			bw.append("Object(influenced) \t aspect(influenced) \t op(influencing) \t influence\n");
			chain.samplerToGetResult.printGammaToFile(bw);
			
			bw.append("\n");
			if(option.model.equals("oaim"))
				bw.append("O \t TA \t freq \n");
			else
				bw.append("A \t TA \t freq \n");
			for(Map.Entry<Integer, Map<Integer, Double>> entry : psi.entrySet()){
				int i=entry.getKey();
				Map<Integer, Double> value = entry.getValue();
				for(Map.Entry<Integer, Double> entry2 : value.entrySet()){
					int j = entry2.getKey();
					Double value2 = entry2.getValue();
					bw.append(i+"\t"+j+"\t"+value2+"\n");
				}
			}
			
			bw.append("\n");
			bw.append("\n");
			bw.append("O\n");
			for(int i=0; i<o.length; i++){
				bw.append(String.format(Locale.US, "%1$.5f", o[i])+"\t");
			}
			
			bw.append("\n");
			bw.append("O \t OP \t freq\n");
			for(Map.Entry<Integer, Map<Integer, Double>> entry : oop.entrySet()){
				int i=entry.getKey();
				Map<Integer, Double> value = entry.getValue();
				for(Map.Entry<Integer, Double> entry2 : value.entrySet()){
					int j = entry2.getKey();
					Double value2 = entry2.getValue();
					bw.append(i+"\t"+j+"\t"+value2+"\n");
				}
			}
			
			bw.append("\n");
			bw.append("O \t B \t freq\n");
			for(Map.Entry<Integer, Map<Integer, Double>> entry : ob.entrySet()){
				int i=entry.getKey();
				Map<Integer, Double> value = entry.getValue();
				for(Map.Entry<Integer, Double> entry2 : value.entrySet()){
					int j = entry2.getKey();
					Double value2 = entry2.getValue();
					bw.append(i+"\t"+j+"\t"+value2+"\n");
				}
			}
			bw.append("\n");

//			bw.append("O \t TA \t OP\n");
//			for(int i=0; i<Constant.oNum; i++){
//				bw.append("obj "+i+"\n");
//				for(int j=0; j<Constant.aspectNum; j++){
//					bw.append("ta "+j+"\n");
//					for(int t=0; t<Constant.oprimeNum; t++){
//						double count =  Util.get3Map(oaop, i, j, t);
//						if(count==0)
//							continue;
//						bw.append(String.format(Locale.US, "%1$.5f", count)+"\t");
//					}
//					bw.append("\n");
//				}
//				bw.append("\n");
//			}
			
			bw.append("totalIter \t totalTime \n");
			bw.append(totalIter+"\t"+totalTime+"\n");
			
			bw.append("\n");
			bw.append("likelihood of unseen data(10 fold cross validation) \t "+llh);
			
			bw.flush();
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void prtResult(CmdOption option, Map<Integer, Map<Integer, Map<Integer, Double>>> gamma, 
			  double[] o, double[][] oop, double[][] ob, double[][][] oaop, int totalIter, long totalTime)
	{
		System.out.println(Debugger.getCallerPosition()+"print gamma values to file on screen");

		StringBuffer bw = new StringBuffer();

		bw.append("totalIter \t totalTime \n");
		bw.append(totalIter+"\t"+totalTime+"\n");

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
		
		bw.append("\n");
		bw.append("oaop\n");
		for(int i=0; i<oaop.length; i++){
			bw.append("obj "+i+"\n");
			for(int j=0; j<oaop[0].length; j++){
				bw.append("aspect "+j+"\n");
				for(int t=0; t<oaop[0][0].length; t++){
					bw.append(String.format(Locale.US, "%1$.5f", oaop[i][j][t])+"\t");
				}
				bw.append("\n");
			}
			bw.append("\n");
		}
		System.out.println(Debugger.getCallerPosition()+"print tem information"+bw.toString());
	}
	/**
	 * generate cross validation separation test sets.
	 * @param refList
	 * @param fold
	 * @return
	 */
	private static List<Set<Integer>> crossValidation(CmdOption option, int fold){
		List<Set<Integer>> crossValiSet = new ArrayList<Set<Integer>>();
		
		try {
			BufferedReader bw = new BufferedReader(new FileReader(new File(option.graphfile.replaceAll(".txt", "_partition.txt"))));
			String line = bw.readLine();
			JSONArray partitions = new JSONArray(line);
			for(int i=0; i<fold; i++){
				JSONArray partition = partitions.getJSONArray(i);
				Set<Integer> cross = new HashSet<Integer>();
				for(int j=0; j<partition.length(); j++){
					cross.add(partition.getInt(j));
				}
				crossValiSet.add(cross);
			}
			bw.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return crossValiSet;
	}
	
}

