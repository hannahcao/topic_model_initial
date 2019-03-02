package chu;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.TreeSet;

import org.json.JSONObject;

import cao.GraphFile;
import cao.Util;

/**
 * Result reader for OAIM
 * @author chuanhu
 *
 */
public class ResultReader {
	/**
	 * summary result of OAIM
	 */
	public static void summaryOAIMResult(int size){
//		File output = new File(cao.Constant.DefaultResultOutputFolder);
		File output = new File("./result/");
		File[] results = output.listFiles();
		System.out.println("twitter"+size+" oaim");
		System.out.println("znum,iteration,time(ms),log likelihood,log_file,cmd file");
		
		Map<String, String> llhMap = new TreeMap<String, String>();
		
		for(File res : results){
//			if(!res.getName().startsWith("twitter_"+size+"_oaim") || !res.getName().endsWith("inf.xls"))
//				continue;
			if(!res.getName().startsWith("citeseerx_data_ta") || !res.getName().endsWith("inf.xls"))
				continue;
			try {
				String name = res.getName();
				int zStart = name.indexOf("znum")+5;
				int zEnd = zStart+3;
				String znum = name.substring(zStart, zEnd).trim();
				
				String llhStr = "";
				
				FileReader fr;
				BufferedReader br;
				fr = new FileReader(res);
				br = new BufferedReader(fr);
				String line = br.readLine();
				while(line!=null){
					if(line.contains("likelihood")){
						StringTokenizer tool = new StringTokenizer(line, "\t\n");
						tool.nextToken();
						llhStr += ","+tool.nextToken().trim()+","+res.getName().replaceAll(" ", "_")+",command_twitter_"+size+"_oaim_z_"+znum+".sh";
						llhMap.put(znum, llhStr);
						break;
					}
					if(line.contains("totalIter")){
						line = br.readLine();
						StringTokenizer tool = new StringTokenizer(line, "\t\n");
						String iter = tool.nextToken().trim();
						String time = tool.nextToken().trim();
						llhStr = iter+","+time;
					}
					line = br.readLine();
				}
				br.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		for(Map.Entry<String, String> entry : llhMap.entrySet())
			System.out.println(entry.getKey()+","+entry.getValue());
		
	}
	/**
	 * summary result of LAIM
	 */
	public static void summaryLAIMResult(int size){
		File output = new File("./result/");
		File[] results = output.listFiles();
		System.out.println("twitter"+size+" laim");
		System.out.println("anum,znum,iteration,time(ms),log likelihoodlog,file,cmd file");
		
		//anum -> znum -> llh
		Map<String, Map<String, String>> llhMap = new TreeMap<String, Map<String, String>>();
		
		for(File res : results){
			if(!res.getName().startsWith("twitter_"+size+"_laim") || !res.getName().endsWith("inf.xls"))
				continue;
			
//			if(!res.getName().startsWith("citeseerx_data-a") || !res.getName().endsWith("inf.xls"))
//				continue;
			
			try {
				String name = res.getName();
				
				int zStart = name.indexOf("z_")+2;
				int zEnd = zStart+2;
				String znum = name.substring(zStart, zEnd).trim();
				
				int aStart = name.indexOf("a_")+2;
				int aEnd = aStart+3;
				String anum = name.substring(aStart, aEnd).trim();

//				System.out.println(zStart+" "+zEnd+" "+aStart+" "+aEnd);
//				System.out.println(znum+" "+anum);
				
				String llhStr = "";
				
				FileReader fr;
				BufferedReader br;
				fr = new FileReader(res);
				br = new BufferedReader(fr);
				
				String line = br.readLine();
				while(line!=null){
					if(line.contains("likelihood")){
						StringTokenizer tool = new StringTokenizer(line, "\t\n");
						tool.nextToken();
						String llh = tool.nextToken().trim();
						llhStr += ","+llh+","+res.getName().replaceAll(" ", "_")+",command_twitter_"+size+"_laim_z_"+znum+"_a_"+anum+".sh";;
//						System.out.println(para+""+llhStr);
						Map<String, String> temMap = llhMap.get(anum);
						if(temMap==null){
							temMap = new TreeMap<String, String>();
							llhMap.put(anum, temMap);
						}
						
						temMap.put(znum, llhStr);
						break;
					}
					
					if(line.contains("totalIter")){
						line = br.readLine();
						StringTokenizer tool = new StringTokenizer(line, "\t\n");
						String iter = tool.nextToken().trim();
						String time = tool.nextToken().trim();
						llhStr = iter+","+time;
					}
					
					line = br.readLine();
				}
				br.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		for(Map.Entry<String, Map<String, String>> entry : llhMap.entrySet()){
			String anum = entry.getKey();
			for(Map.Entry<String, String> entry2 : entry.getValue().entrySet()){
				String znum = entry2.getKey();
				String llh = entry2.getValue();
				System.out.println(anum+","+znum+","+llh);
			}
		}
	}
	/**
	 * 
	 * @param file
	 */
	public static void citeseerOAIMResultRead(String file){
		FileReader fr;
		BufferedReader br;
		
		Map<Integer, Map<Integer, Map<Integer, Double>>> infMap = new TreeMap<Integer, Map<Integer, Map<Integer, Double>>>();
		Map<Integer, String> titleMap = new HashMap<Integer, String>();
		
		try {
			fr = new FileReader(new File(file));
			br = new BufferedReader(fr);
			String line = br.readLine();
			boolean isNum = false;
			
			while(line!=null){
				if(line.startsWith("Object")){
					isNum = true;
					line = br.readLine();
					continue;
				}
				if(isNum){
					if(line==null||line.trim().length()==0)
						break;
					StringTokenizer tool = new StringTokenizer(line, "\t\n");
					int o = Integer.parseInt(tool.nextToken());
					int a = Integer.parseInt(tool.nextToken());
					int op = Integer.parseInt(tool.nextToken());
					double inf = Double.parseDouble(tool.nextToken());
					Util.update3TreeMap(infMap, o, a, op, inf);
					
					JSONObject citing = GraphFile.readTweetJson("./data/citeseerx_data/paper_chu/", o);
					JSONObject cited = GraphFile.readTweetJson("./data/citeseerx_data/paper_chu/", op);
					String citingTitle = citing.getString("user_name");
					String citedTitle = cited.getString("user_name");

					titleMap.put(o, citingTitle);
					titleMap.put(op, citedTitle);
					
				}
				line = br.readLine();
			}
			
			br.close();
			
			for(Map.Entry<Integer, Map<Integer, Map<Integer, Double>>> entry : infMap.entrySet()){
				int o = entry.getKey();
				for(Map.Entry<Integer, Map<Integer, Double>> entry2 : entry.getValue().entrySet()){
					int a = entry2.getKey();
					for(Map.Entry<Integer, Double> entry3 : entry2.getValue().entrySet()){
						int op = entry3.getKey();
						double inf = entry3.getValue();
						System.out.println(o+","+a+","+op+","+inf+","+titleMap.get(o)+","+titleMap.get(op));
					}
				}
			}
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	/**
	 * 
	 * @param file
	 */
	public static void citeseerLAIMResultRead(String file){
		FileReader fr;
		BufferedReader br;
		
		Map<Integer, Map<Integer, Map<Integer, Double>>> infMap = new TreeMap<Integer, Map<Integer, Map<Integer, Double>>>();
		Map<Integer, String> titleMap = new HashMap<Integer, String>();
		
		try {
			fr = new FileReader(new File(file));
			br = new BufferedReader(fr);
			String line = br.readLine();
			boolean isNum = false;
			
			while(line!=null){
				if(line.startsWith("Object")){
					isNum = true;
					line = br.readLine();
					continue;
				}
				if(isNum){
					if(line==null||line.trim().length()==0)
						break;
					StringTokenizer tool = new StringTokenizer(line, "\t\n");
					int o = Integer.parseInt(tool.nextToken());
					int a = Integer.parseInt(tool.nextToken());
					int op = Integer.parseInt(tool.nextToken());
					double inf = Double.parseDouble(tool.nextToken());
					Util.update3TreeMap(infMap, o, a, op, inf);
					
					JSONObject citing = GraphFile.readTweetJson("./data/citeseerx_data/paper_chu/", o);
					JSONObject cited = GraphFile.readTweetJson("./data/citeseerx_data/paper_chu/", op);
					String citingTitle = citing.getString("user_name");
					String citedTitle = cited.getString("user_name");

					titleMap.put(o, citingTitle);
					titleMap.put(op, citedTitle);
					
				}
				line = br.readLine();
			}
			
			br.close();
			
			for(Map.Entry<Integer, Map<Integer, Map<Integer, Double>>> entry : infMap.entrySet()){
				int o = entry.getKey();
				for(Map.Entry<Integer, Map<Integer, Double>> entry2 : entry.getValue().entrySet()){
					int a = entry2.getKey();
					for(Map.Entry<Integer, Double> entry3 : entry2.getValue().entrySet()){
						int op = entry3.getKey();
						double inf = entry3.getValue();
						System.out.println(o+","+a+","+op+","+inf+","+titleMap.get(o)+","+titleMap.get(op));
					}
				}
			}
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * 
	 * @param file
	 */
	public static void twitterOAIMResultRead(String file){
		FileReader fr;
		BufferedReader br;
		
		Map<Integer, Map<Integer, Map<Integer, Double>>> infMap = new TreeMap<Integer, Map<Integer, Map<Integer, Double>>>();
		Map<Integer, String> titleMap = new HashMap<Integer, String>();
		Map<Integer, String> aspectMap = new HashMap<Integer, String>();
		
		try {
			fr = new FileReader(new File("./data/twitter100/aspect.txt"));
			br = new BufferedReader(fr);
			String line = br.readLine();	
			int aspectId = 0;
			while(line!=null){
				aspectMap.put(aspectId, line);
				aspectId++;
				line = br.readLine();
			}
			br.close();
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		try {
			fr = new FileReader(new File(file));
			br = new BufferedReader(fr);
			String line = br.readLine();
			boolean isNum = false;
			
			while(line!=null){
				if(line.startsWith("Object")){
					isNum = true;
					line = br.readLine();
					continue;
				}
				if(isNum){
					if(line==null||line.trim().length()==0)
						break;
					StringTokenizer tool = new StringTokenizer(line, "\t\n");
					int o = Integer.parseInt(tool.nextToken());
					int a = Integer.parseInt(tool.nextToken());
					int op = Integer.parseInt(tool.nextToken());
					double inf = Double.parseDouble(tool.nextToken());
					Util.update3TreeMap(infMap, o, a, op, inf);
					
					JSONObject citing = GraphFile.readTweetJson("./data/twitter100/tweet/", o);
					JSONObject cited = GraphFile.readTweetJson("./data/twitter100/tweet/", op);
					String citingTitle = citing.getString("user_name");
					String citedTitle = cited.getString("user_name");

					titleMap.put(o, citingTitle);
					titleMap.put(op, citedTitle);
					
				}
				line = br.readLine();
			}
			
			br.close();
			
			for(Map.Entry<Integer, Map<Integer, Map<Integer, Double>>> entry : infMap.entrySet()){
				int o = entry.getKey();
				for(Map.Entry<Integer, Map<Integer, Double>> entry2 : entry.getValue().entrySet()){
					int a = entry2.getKey();
					for(Map.Entry<Integer, Double> entry3 : entry2.getValue().entrySet()){
						int op = entry3.getKey();
						double inf = entry3.getValue();
						System.out.println(o+","+a+","+op+","+inf+","+titleMap.get(o)+","+aspectMap.get(a)+","+titleMap.get(op));
					}
				}
			}
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	/**
	 * 
	 * @param file
	 */
	public static void twitterLAIMResultRead(String file){
		FileReader fr;
		BufferedReader br;
		
		Map<Integer, Map<Integer, Map<Integer, Double>>> infMap = new TreeMap<Integer, Map<Integer, Map<Integer, Double>>>();
		Map<Integer, String> titleMap = new HashMap<Integer, String>();
		
		try {
			fr = new FileReader(new File(file));
			br = new BufferedReader(fr);
			String line = br.readLine();
			boolean isNum = false;
			
			while(line!=null){
				if(line.startsWith("Object")){
					isNum = true;
					line = br.readLine();
					continue;
				}
				if(isNum){
					if(line==null||line.trim().length()==0)
						break;
					StringTokenizer tool = new StringTokenizer(line, "\t\n");
					int o = Integer.parseInt(tool.nextToken());
					int a = Integer.parseInt(tool.nextToken());
					int op = Integer.parseInt(tool.nextToken());
					double inf = Double.parseDouble(tool.nextToken());
					Util.update3TreeMap(infMap, o, a, op, inf);
					
					JSONObject citing = GraphFile.readTweetJson("./data/twitter100/tweet/", o);
					JSONObject cited = GraphFile.readTweetJson("./data/twitter100/tweet/", op);
					String citingTitle = citing.getString("user_name");
					String citedTitle = cited.getString("user_name");
					
					titleMap.put(o, citingTitle);
					titleMap.put(op, citedTitle);
					
				}
				line = br.readLine();
			}
			
			br.close();
			
			for(Map.Entry<Integer, Map<Integer, Map<Integer, Double>>> entry : infMap.entrySet()){
				int o = entry.getKey();
				for(Map.Entry<Integer, Map<Integer, Double>> entry2 : entry.getValue().entrySet()){
					int a = entry2.getKey();
					for(Map.Entry<Integer, Double> entry3 : entry2.getValue().entrySet()){
						int op = entry3.getKey();
						double inf = entry3.getValue();
						System.out.println(o+","+a+","+op+","+inf+","+titleMap.get(o)+","+titleMap.get(op));
					}
				}
			}
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void main(String agrs[]){
//		ResultReader.citeseerOAIMResultRead("./output/citeseerx_data_ta z10 duplicate=yes znum 10 time=2013-03-17-15-33-29.inf.xls");
//		ResultReader.twitterOAIMResultRead("./output/twitter_100_oaim z 10 a 10 duplicate=yes znum 10 time=2013-05-15-20-54-26.inf.xls");
//		ResultReader.summaryOAIMResult(500);
//		ResultReader.summaryOAIMResult(1000);
//		ResultReader.summaryOAIMResult(1500);
//		ResultReader.summaryOAIMResult(2000);
		
//		ResultReader.citeseerLAIMResultRead("./output/citeseerx_data-a 5 z 10 anum 5 znum 10 time=2013-03-16-02-47-22.inf.xls");
		ResultReader.twitterLAIMResultRead("./output/twitter_100_laim_z_10_a_100 duplicate=yes znum 10 time=2013-05-14-09-57-56.inf.xls");
//		ResultReader.summaryLAIMResult(500);
//		ResultReader.summaryLAIMResult(100);
//		ResultReader.summaryLAIMResult(1000);
//		ResultReader.summaryLAIMResult(1500);
//		ResultReader.summaryLAIMResult(2000);
	}
}
