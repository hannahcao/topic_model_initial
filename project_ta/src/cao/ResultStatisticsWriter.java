package cao;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ResultStatisticsWriter {
	
	CmdOption cmdOption;
	
	private transient FileWriter writer = null;
    private transient File file;
    private Date firstDate = null;
    SimpleDateFormat dateFormat = null; 
//    			iteration		rHat
    private Map<Integer, ResultStatisticRecord> records = new LinkedHashMap<Integer, ResultStatisticRecord>();
    
	private final String samplerId;
    private final int chainId;
    private final List<Integer> allChainIds;
    private int rowNumber = 1;
    
	public ResultStatisticsWriter(String _samplerId, int _chainId, List<Integer> _allChainIds, CmdOption _cmdOption)
	{
		cmdOption = _cmdOption;
		samplerId = _samplerId;
		chainId = _chainId;
		allChainIds = _allChainIds;
		
		String filename = Util.getStatFileName(samplerId,chainId);
		file = new File(filename);
		dateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss"); 
		
	    System.out.println(Debugger.getCallerPosition()+"file.getAbsolutePath() = " + file.getAbsolutePath());
	}
	
	public void initialize()
	{
		 try {
			 writer = new FileWriter(file, false);
			 writer.write("StatisticsWriter " + samplerId + "-" + chainId + "\n");
			 rowNumber++;
			 writer.write("Iteration \trhat \tmilliseconds elapsed \ttimestamp" + "\n");
	         rowNumber++;
	         writer.flush();
        } catch (IOException e) {
        	System.err.println("StatisticsWriter#writePrologToXls: " + e.toString());
        	throw new RuntimeException(e);
        }
	}
	
	public void addResultStatisticRecord(int iteration, double rhat) {
        if (firstDate == null) {
            firstDate = new Date();
        }
        Date now = new Date();
        long millisecsElapsed = (now.getTime() - firstDate.getTime());
        records.put(iteration, new ResultStatisticRecord(iteration, rhat, millisecsElapsed, now));

        //FileWriter out = openWriter(false);
        //writeSingleChainDataToXls(rhat, iteration, now, millisecsElapsed, out);
        try {
            String rHatString = Util.cut(rhat);
            if (Double.isNaN(rhat)) {
                rHatString = "";
            }
            SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss"); //dateFormat();
            writer.write(iteration + "\t" + rHatString + "\t" + millisecsElapsed + "\t" + dateFormat.format(now) + "\n");
            rowNumber++;
            writer.flush();
        } catch (IOException e) {
            System.err.println("ChainStatisticsWriter#writeGlobalChainDataToXls: " + e.toString());
            throw new RuntimeException(e);
        }
    }
	
	public void shutdown() {
		//writeEpilogToXls(out);
        try {
            writer.close();
        } catch (IOException e) {
            System.err.println("ChainStatisticsWriter#writeEpilogToXls: " + e.toString());
        }
        
        //write this chain's information
      	writeOneChainStatisticSummaryToDisc(); 
        
      	//write all the chains' summary information
        writeFullStatistics();
    }
	
	/**
	 * Write all the chains' statistics information out
	 */
	private void writeFullStatistics() {
        //read all chains' information from their individual files
        //merge them together and write them out
        //delete individual chain's summary files
        try {
            Map<Integer, Map<Integer, ResultStatisticRecord>> summaries = readStatisticSummariesFromDisc();
            writeSummariesToFullStatText(summaries);
            if(Constant.DeleteTemporaryFile) deleteOneChainStatistiscSummariesFromDisc();
            
        } catch (Exception e) {
            System.out.println(Debugger.getCallerPosition()+"other statistic summaries are not ready yet, relying on others.");
            //e.printStackTrace();
        }
    }
	
	private void writeOneChainStatisticSummaryToDisc() {
		String storageName = Util.getStatFileNameBinary(samplerId,chainId);
        try {
            ObjectOutputStream w = new ObjectOutputStream(new FileOutputStream(storageName));
            w.writeObject(records);
            w.flush();
            w.close();
        } catch (IOException e) {
            throw new RuntimeException(e);  
        }
	 }
	 
	 private void deleteOneChainStatistiscSummariesFromDisc() {
        for (int chain : allChainIds) {
            String filename = Util.getStatFileNameBinary(samplerId,chainId); 
            		//Util.fileNamePrefix(samplerId,chain) + Constant.ResultStatisticsFileSuffix;
            File f = new File(filename);
            f.delete();
        }
    }
	 
	
	/**
	 * Read the given chain' result statistic summary
	 *  
	 * @param chain
	 * @return
	 * @throws Exception
	 */
	private Map<Integer, ResultStatisticRecord> readOneChainStatisticSummaryFromDisc(int chain)
		throws Exception
	{
		 Map<Integer, ResultStatisticRecord> summary = null;

		 String filename = Util.getStatFileNameBinary(samplerId,chain); //samplerId + "-" + chain +  Constant.ResultStatisticsFileSuffix;
		 
		 boolean readCompleted = false;
         int retryNo = 0;
         
         while (!readCompleted && retryNo < 3) {
        	 
             try {
                 ObjectInputStream r = new ObjectInputStream(new FileInputStream(filename));
                 Object summary_ = r.readObject();
                 r.close();
                 summary = (Map<Integer, ResultStatisticRecord>) summary_;
                 //summaries.put(chain, summary);
                 readCompleted = true;
             } catch (IOException e) {
                 System.err.println(Debugger.getCallerPosition()+"unable to read " + filename + " retrying...");
                 try {
                     retryNo++;
                     Thread.sleep(100);
                 } catch (InterruptedException e1) {
                	 throw new RuntimeException(e1);
                 }
             } catch (ClassNotFoundException e) {
                 throw new RuntimeException(e);
             }
         }
         
         if (!readCompleted) {
        	 System.err.println(Debugger.getCallerPosition()+"Chain"+chain +" read one chain statistics incomplete");
             throw new Exception("Chain"+chain +" read incomplete");
         }
         return summary;
	 }
	
	 /**
	  * Read all chains' result statistics summary information and put them into one map
	  * @return
	  * @throws Exception
	  */
	 private Map<Integer, Map<Integer, ResultStatisticRecord>> readStatisticSummariesFromDisc() throws Exception{
        Map<Integer, Map<Integer, ResultStatisticRecord>> summaries = 
        		new HashMap<Integer, Map<Integer, ResultStatisticRecord>>();

        //add all the chains' statistic summary
        for (int chain : allChainIds) {
        	if(chain == chainId){
        	    //add the current chain's summary
                summaries.put(chainId, records);
        	}else{
        		//add the other chains' summaries
            	try{
            		Map<Integer, ResultStatisticRecord> summary =  readOneChainStatisticSummaryFromDisc(chain);
            		if(summary!=null){
	            		summaries.put(chain, summary);
	            	}
            	}catch(Exception e){
            		throw e;
            	}
            	
            }
        }
        
   

        return summaries;
    }

	/**
	 * Write all chains' statistics information to a text file
	 * 
	 * @param summaries: map: chainid --> (Map: iteration: summary)
	 */
    private void writeSummariesToFullStatText(Map<Integer, Map<Integer, ResultStatisticRecord>> chainId2summaries) {
    	rowNumber = 1;
    	
    	//If all the chains are not finished yet
        if (chainId2summaries.size() !=allChainIds.size()) {
            System.out.println("ChainStatisticsWriter.writeSummariesToFullStat: only one chain available. Skipping stats output.");
            return;
        }
        
        Map<Integer, Integer> beginBlocks = new HashMap<Integer, Integer>();
        Map<Integer, Integer> endBlocks = new HashMap<Integer, Integer>();
         
        try {
        	String fname = Util.getAllStatFileName(samplerId); //samplerId + Constant.ResultStatisticsFileAllChainSuffix
            File f = new File(fname);//".fullstat.xls");
            FileWriter out = new FileWriter(f);
          
            //(1) write statistic file header
            //writeGlobalPrologToXls(out);
            out.write("ResultStatisticsWriter " + samplerId + "\n");rowNumber++;
            
            //This row output: Rhat over iterations: Iteration \t 1 \t 2 \t ...\n
            out.write("Rhat over iterations");
            out.write("Iteration \t");
            for (Integer chainId : allChainIds) {
                out.write(chainId + "\t");
            }
            out.write("\n");rowNumber++;
            
            //(2) write per chain summary
            beginBlocks.put(chainId, rowNumber);
            Map<Integer, ResultStatisticRecord> summMap0 = chainId2summaries.get(allChainIds.get(0));
            //Each row output: iter \t   rHat-chain1	\t rhat-chain2	\t	... \n
            for (int iterkey : summMap0.keySet()) {
                out.write(iterkey + "\t");
                for (int chainId : allChainIds) {
                    Map<Integer, ResultStatisticRecord> map = chainId2summaries.get(chainId);
                    ResultStatisticRecord rec = map.get(iterkey);
                    if (rec != null && !Double.isNaN(rec.getRHat())) {
                        out.write(rec.getRHat() + "\t");
                    } else {
                        out.write("\t");
                    }
                }
                out.write("\n");
                rowNumber++;
            }
            endBlocks.put(chainId, rowNumber - 1);
            //writeEpilogToXls(out);
            out.write("\n\n\n");
            rowNumber+=3;


            //Summer sumDuration = new Summer();
            double sumDuration = 0.0;
            int summands = 0; 

            // write overview summary
            out.write("Chain id\tIterations until convergence" +
            		"\tlast rhat" +
            		"\taverage millisecs per iter" +
            		"\tfrom date" +
            		"\tto date"+ 
            		"\n");
            
            rowNumber++;
            int beginOverview = rowNumber;
            for (Integer chainId : allChainIds) {
                //Integer endRow = endBlocks.get(chainId);
                //Integer beginRow = beginBlocks.get(chainId);
                Map<Integer, ResultStatisticRecord> oneChainResultStat = chainId2summaries.get(chainId);

                int itersUntilConv = 0;
                for (int iter : oneChainResultStat.keySet()) {
                	ResultStatisticRecord record = oneChainResultStat.get(iter);
                    if (record.getRHat() < cmdOption.R_HAT_THRESH) {
                        itersUntilConv = iter;
                        break;
                    }
                }
                if (itersUntilConv == 0 && !oneChainResultStat.keySet().isEmpty()) {
                    itersUntilConv = Collections.max(oneChainResultStat.keySet()); // chain did not converge
                }
                String maxIter = "" + itersUntilConv;

                if (!oneChainResultStat.isEmpty()) {
                    Integer maxIter_ = Collections.max(oneChainResultStat.keySet());
                    String lastRhat = "" + Util.cut(oneChainResultStat.get(maxIter_).getRHat());

                    long duration = oneChainResultStat.get(maxIter_).getMillisecoundsElapsed();
                    double avgTimePerIter_ = 1.0 * duration / maxIter_;
                    String avgTimePerIter = "" + Util.cut(avgTimePerIter_);

                    int minIter_ = Collections.min(oneChainResultStat.keySet());
                    Date beginDate = oneChainResultStat.get(minIter_).getTimestamp();
                    Date endDate = oneChainResultStat.get(maxIter_).getTimestamp();
                    String fromDate = dateFormat.format(beginDate);
                    String todate = dateFormat.format(endDate);

                    sumDuration +=avgTimePerIter_ * itersUntilConv;
                    summands++;
                    //sumDuration.addToSum(avgTimePerIter_ * itersUntilConv);

                    String ganttchart = "=A" + rowNumber + "\t=E" + rowNumber + "\t=F" + rowNumber + "-E" + rowNumber;
                    out.write(chainId + "\t" + maxIter + "\t" + lastRhat + "\t" + avgTimePerIter + "\t" + fromDate + "\t"
                            + todate + "\t\t\t" + ganttchart + "\n");
                    rowNumber++;
                } else {
                    out.write(chainId + "\tnot enough data collected.");
                }
            }
            int endOverview = rowNumber - 1;
            out.write("Average\t" + vMittel("B", beginOverview, endOverview) + "\t"
                    + vMittel("C", beginOverview, endOverview) + "\t" + vMittel("D", beginOverview, endOverview)
                    + "\n");
            rowNumber++;
            
            //int averageRow = rowNumber - 1;
            out.write("\n");rowNumber++;
            out.write("Average Duration\thours\tmins\tsecs" + "\n");rowNumber++;
            int thisRow = rowNumber;
            // average duration
            //String avgDurInMillis = "" + (sumDuration.getSum() / sumDuration.getNumberOfSummands());
            String avgDurInMillis = "" + sumDuration/ summands;
            
            String hours = ganzzahl("$A" + thisRow + "/1000/60/60");
            String mins = ganzzahlRest("$A" + thisRow + "/1000/60", 60);
            String secs = ganzzahlRest("$A" + thisRow + "/1000", 60);
            out.write(avgDurInMillis + "\t" + hours + "\t" + mins + "\t" + secs + "\n");
            rowNumber++;
            
            out.flush();
            out.close();

        } catch (IOException e) {
            System.err.println("ChainStatisticsWriter#writeSummariesToFullStat: " + e.toString());
        }
    }
    
    private String ganzzahlRest(String value, int divisor) {
        return "=INT(MOD(" + value + "," + divisor + "))";
    }

    private String ganzzahl(String value) {
        return "=INT(" + value + ")";
      
    }
    
    private String vMittel(String col, int beginRow, int endRow) {
       return "=average(" + col + beginRow + ":" + col + endRow + ")";
    }
}