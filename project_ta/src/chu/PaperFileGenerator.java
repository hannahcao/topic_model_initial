package chu;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.json.JSONArray;
import org.json.JSONObject;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

import cao.CmdOption;
import cao.DataParsed;
import cao.DataRaw;
import cao.GraphFile;

public class PaperFileGenerator {
	/**
	 * PaperRoot-
	 * 			|chaomin
	 * 					|clustering
	 * 								|1974
	 * 										|......txt
	 * 			|chu
	 */
	private File paperRoot;
	private String output;
	
	static String whitespace_chars =  //""       /* dummy empty string for homogeneity */
             "\\u0009" // CHARACTER TABULATION
            + "\\u000A" // LINE FEED (LF)
            + "\\u000B" // LINE TABULATION
            + "\\u000C" // FORM FEED (FF)
            + "\\u000D" // CARRIAGE RETURN (CR)
//            + "\\u0020" // SPACE
            + "\\u0085" // NEXT LINE (NEL) 
            + "\\u00A0" // NO-BREAK SPACE
            + "\\u1680" // OGHAM SPACE MARK
            + "\\u180E" // MONGOLIAN VOWEL SEPARATOR
            + "\\u2000" // EN QUAD 
            + "\\u2001" // EM QUAD 
            + "\\u2002" // EN SPACE
            + "\\u2003" // EM SPACE
            + "\\u2004" // THREE-PER-EM SPACE
            + "\\u2005" // FOUR-PER-EM SPACE
            + "\\u2006" // SIX-PER-EM SPACE
            + "\\u2007" // FIGURE SPACE
            + "\\u2008" // PUNCTUATION SPACE
            + "\\u2009" // THIN SPACE
            + "\\u200A" // HAIR SPACE
            + "\\u2028" // LINE SEPARATOR
            + "\\u2029" // PARAGRAPH SEPARATOR
            + "\\u202F" // NARROW NO-BREAK SPACE
            + "\\u205F" // MEDIUM MATHEMATICAL SPACE
            + "\\u3000" // IDEOGRAPHIC SPACE
            ;        
	static String     whitespace_charclass = "["  + whitespace_chars + "]";   

	public PaperFileGenerator(File paperRoot, String output){
		this.paperRoot = paperRoot;
		this.output = output;
	}

	public void convertToExpData(){
		//  citing     set of reference
		Map<String, Set<String>> citeRelation = new TreeMap<String, Set<String>>();
		Map<String, String> paperRefMap = new TreeMap<String, String>();
		Map<String, String> paperTitleMap = new TreeMap<String, String>();
		Map<String, Integer> nameToNumMap = new TreeMap<String, Integer>(); 

		int fileNum = 1;
		File[] dir1 = this.paperRoot.listFiles();
		for(File tem1 : dir1){//tem1 = 403
			if(tem1.isDirectory()){
				File[] dir2 = tem1.listFiles();
				for(File tem2 : dir2){//tem4 = 10.1.1.10.407.txt
					if(tem2.getName().endsWith(".txt")){
						try {
//							String fileName = tem2.getParentFile().getName();
							String fileName = tem2.getName().replaceAll(".txt", "");
//							Integer fileNum = Integer.parseInt(fileName);

							nameToNumMap.put(fileName, fileNum);
							
							FileReader fr = new FileReader(tem2);
							BufferedReader br = new BufferedReader(fr);
							String line = br.readLine();
							String text = "";
							while(line!=null){
								line = line.trim().toLowerCase();
								text+=line;
								line = br.readLine();
							}
							int refBegin = text.indexOf("<reference>")+"<reference>".length();
							int refEnd = text.indexOf("</reference>");
							System.out.println("file name "+tem2.getCanonicalPath()+" begin "+refBegin+" end "+refEnd);
							String refer = text.substring(refBegin, refEnd);
							refer = refer.replaceAll(whitespace_charclass+"*", " ");

							int titleBegin = text.indexOf("<title>")+"<title>".length();
							int titleEnd = text.indexOf("</title>");
							System.out.println("title begin "+titleBegin+" end "+titleEnd);
							String title = text.substring(titleBegin, titleEnd);
							title = title.replaceAll(whitespace_charclass+"*", " ");

							paperRefMap.put(fileName, refer);
							paperTitleMap.put(fileName, title);

							tem2.renameTo(new File("./data/citeseerx_data/paper/"+fileNum+".txt"));//save as num.txt
							fileNum++;
							br.close();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
			}
		}
		
		for(Map.Entry<String, String> paperTitleEntry : paperTitleMap.entrySet()){
			String citedID = paperTitleEntry.getKey();
			String title = paperTitleEntry.getValue();

			for(Map.Entry<String, String> paperRefEntry : paperRefMap.entrySet()){
				String citingID = paperRefEntry.getKey();
				String ref = paperRefEntry.getValue();
				if(ref.contains(title)){
					Set<String> refSet = citeRelation.get(citingID);
					if(refSet==null){
						refSet = new HashSet<String>();
						citeRelation.put(citingID, refSet);
					}
					refSet.add(citedID);
				}
			}
		}
//		System.out.println("citing num "+citeRelation.size());
		for(Map.Entry<String, Set<String>> entry : citeRelation.entrySet())
			for(String cited : entry.getValue())
				System.out.println(entry.getKey()+" "+cited);
		
		for(Map.Entry<String, Integer> entry : nameToNumMap.entrySet())
			System.out.println(entry.getKey()+"\t"+entry.getValue());
			
	}
	
	public void cleanPaper(){
		File[] fs = this.paperRoot.listFiles();
		for(File file : fs){
			FileReader fr;
			if(!file.getName().endsWith(".txt"))
				continue;
			try {
				fr = new FileReader(file);
				BufferedReader br = new BufferedReader(fr);
				BufferedWriter bw = new BufferedWriter(new FileWriter(new File("./data/citeseerx_data/paper_chu/"+file.getName())));
				String line = br.readLine();
				String text = "";
				while(line!=null){
					text+=line+"\n";
					line = br.readLine();
				}
				br.close();
				
				text = text.replaceAll("<conclusion>", "<expanalysis>");
				text = text.replaceAll("</conclusion>", "</expanalysis>");
				
				bw.append(text);
				bw.flush();
				bw.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public void getCitation(){
		Map<Integer, String> refMap = new HashMap<Integer, String>();
		Map<Integer, String> titleMap = new HashMap<Integer, String>();
		
		File[] fs = this.paperRoot.listFiles();
		for(File file : fs){
			FileReader fr;
			if(!file.getName().endsWith(".txt"))
				continue;
			try {
				fr = new FileReader(file);
				Integer num = Integer.parseInt(file.getName().replace(".txt", ""));
				System.out.println(num);
				
				BufferedReader br = new BufferedReader(fr);
				String line = br.readLine();
				String text = "";
				while(line!=null){
					text+=line.trim().toLowerCase()+" ";
					line = br.readLine();
				}
				
				int titleBegin = text.indexOf("<title>");
				int titleEnd = text.indexOf("</title>");
				String title = text.substring(titleBegin+"<title>".length(), titleEnd);
				titleMap.put(num, title);
				
				int refBegin = text.indexOf("<reference>");
				int refEnd = text.indexOf("</reference>");
				String reference = text.substring(refBegin+"<reference>".length(), refEnd);
				refMap.put(num, reference);
				
				br.close();
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		for(Map.Entry<Integer, String> cited : refMap.entrySet()){
			for(Map.Entry<Integer, String> citing : titleMap.entrySet()){
//				System.out.println(citing.getValue());
				if(cited.getValue().contains(citing.getValue())){
					System.out.println(cited.getKey()+" cites "+citing.getKey());
				}
			}
		}
	}

	public void generateLDAPubidcite(){
		FileReader fr;
		try {
			fr = new FileReader(new File("./data/citeseerx_data/pubidcite.txt"));
			BufferedReader br = new BufferedReader(fr);
			BufferedWriter bw = new BufferedWriter(new FileWriter(new File("./data/citeseerx_data/lda_pubidcite.txt")));
			String line = br.readLine();
			Set<String> paper = new TreeSet<String>();
			while(line!=null && line.length()!=0){
				if(line.startsWith("#")){
					line = br.readLine();
					continue;
				}
				int spaceIndex = line.indexOf(" ");
				String citing = line.substring(0, spaceIndex).trim();
				String cited = line.substring(spaceIndex).trim();
				paper.add(cited);paper.add(citing);
				
				line = br.readLine();
			}
			for(String s : paper){
				bw.append("0 "+s+"\n");
			}
			
			bw.flush();
			br.close();
			bw.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void xml2json(){
		File f = new File("./data/citeseerx_data/paper_chu/");
		File aspect = new File("./data/citeseerx_data/aspect.txt");
		Set<String> aspectSet = new HashSet<String>();
		
		BufferedReader br;
		try {
			br = new BufferedReader(new FileReader(aspect));
			String line = br.readLine();
			while(line!=null){
				aspectSet.add(line.trim());
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
		
		File[] xmls = f.listFiles(new FileFilter(){
			@Override
			public boolean accept(File arg0) {
				// TODO Auto-generated method stub
				return !arg0.isDirectory()&&arg0.getName().endsWith(".txt")&&!arg0.getName().startsWith("0");
			}
			
		});
		for(File xml : xmls){
			try {
				System.out.println(xml.getName());
				
				JSONObject obj = new JSONObject();
				JSONArray arr = new JSONArray();
				
				br = new BufferedReader(new FileReader(xml));
				String line = br.readLine();
				String text = "";
				while(line!=null){
					text+=line+" ";
					line = br.readLine();
				}
				
				text = text.replaceAll(whitespace_charclass, "");
				
				String titleBegin = "<title>";
				String titleEnd = "</title>";
				int titleBeginIndex = text.indexOf(titleBegin)+7;
				int titleEndIndex = text.indexOf(titleEnd);
				String title = text.substring(titleBeginIndex, titleEndIndex);
				obj.put("user_name", title);
				
				for(String asp : aspectSet){
					System.out.println(asp);
					
					String begin = "<"+asp+">";
					String end = "</"+asp+">";
					int beginIndex = text.indexOf(begin)+asp.length()+2;
					int endIndex = text.indexOf(end);
					String aspectText = "";
					if(beginIndex==-1 || endIndex==-1 || beginIndex>endIndex)
						aspectText = "";
					else
						aspectText = text.substring(beginIndex, endIndex);
					JSONObject tweet = new JSONObject();
					tweet.put("aspect", asp);
					tweet.put("tweet", aspectText);
					arr.put(tweet);
				}
				
				obj.put("user_timeline", arr);
				
				BufferedWriter bw = new BufferedWriter(new FileWriter(new File("./data/citeseerx_data/paper_chu/"+xml.getName().replace(".txt", ".json"))));
				bw.append(obj.toString());
				bw.flush();
				
				br.close();
				bw.close();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			catch (IOException e){
				e.printStackTrace();
			}
		}
	}
	
	public void generateCrossValidation(CmdOption option, int fold){
		DataRaw rawdata = new DataRaw();
		
		JSONArray partitions = new JSONArray();
		JSONArray[] partition = new JSONArray[fold];
		for(int i=0; i<fold; i++)
			partition[i] = new JSONArray();
		
		Set<Integer> docidset = GraphFile.readEdge(option.graphfile, rawdata);
		int count = 0;
		for(Integer docid : docidset){
			partition[count].put(docid);
			count = (count+1)%fold;
		}
		
		for(JSONArray arr : partition)
			partitions.put(arr);
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(new File(option.graphfile.replaceAll(".txt", "_partition.txt"))));
			bw.append(partitions.toString());
			bw.flush();
			bw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public static void main(String[] args){
		CmdOption option = new CmdOption();
		CmdLineParser parser = new CmdLineParser(option);
		try {
			parser.parseArgument(args);
			PaperFileGenerator p = new PaperFileGenerator(new File("./data/twitter1500/"), "");

			p.generateCrossValidation(option, 10);
		} catch (CmdLineException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
//		p.getCitation();
//		p.generateLDAPubidcite();
//		p.xml2json();
	}
}
