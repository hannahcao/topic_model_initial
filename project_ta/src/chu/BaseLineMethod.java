package chu;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.LowerCaseTokenizer;
import org.apache.lucene.analysis.PorterStemFilter;
import org.apache.lucene.analysis.StopAnalyzer;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.index.TermFreqVector;
import org.apache.lucene.search.DefaultSimilarity;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.util.Version;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

import cao.CmdOption;
import cao.DataParsed;
import cao.DataRaw;
import cao.Debugger;
import cao.Doc;
import cao.Util;

class TFIDFVector{
	/**
	 * lucene default similarity class.  Used to compute TF/IDF
	 */
	static DefaultSimilarity sim = new DefaultSimilarity();

	public static List<String> vocabulary = null;
	
	/**
	 * TF/IDF vector
	 */
	private Map<String, Float> tfidfMap = null;
	
	private String[] terms;
	private int[] tfArr;
	
	private int[] dfArr;

	/**
	 * Constructor
	 * @param terms array of terms
	 * @param tfArr array of term frequency
	 * @param dfArr array of document frequency
	 * @param numDocs num of total documents
	 */
	TFIDFVector(String[] terms, int[] tfArr, int[] dfArr, int numDocs){
		tfidfMap = new HashMap<String, Float>();
		this.terms = terms;
		this.tfArr = tfArr;
		this.dfArr = dfArr;
		
		for(int i=0; i<terms.length; i++){
			tfidfMap.put(terms[i], sim.tf(tfArr[i])*sim.idf(dfArr[i], numDocs));
			
		}
	}
	/**
	 * use TF/IDF value as term's probability and normalize all terms'
	 */
	public void normalize(){
		float sum = this.sum();
		//compute sum TF/IDF value
		//normalize every term's value
		for(Map.Entry<String, Float> entry : tfidfMap.entrySet())
			entry.setValue(entry.getValue()/sum);
	}
	/**
	 * 
	 * @return
	 */
	public float sum(){
		float sum = 0;
		//compute sum TF/IDF value
		
		for(int i=0; i<tfArr.length; i++){
			sum += tfArr[i]*tfidfMap.get(terms[i]);
		}
//		for(Map.Entry<String, Float> entry : tfidfMap.entrySet())
//			sum+=entry.getValue();
		return sum;
	}
	
	/**
	 * 
	 * @param factor
	 */
	public void normalizedBy(float factor){
		//normalize every term's value
		for(Map.Entry<String, Float> entry : tfidfMap.entrySet())
			entry.setValue(entry.getValue()/factor);
	}
	
	/**
	 * get one term's probability in this vector
	 * @param term
	 * @return
	 */
	public double getTermPro(String term){
		Float pro = tfidfMap.get(term);
		if(pro==null)
			pro = new Float(0);
		return pro;
	}
	
	
	/**
	 * Compute and return prior log likelihood of this doc
	 * @return
	 */
	public double priorLLH(){
		double llh = 0;
		
		for(int i=0; i<tfArr.length; i++){
			int count = tfArr[i];
			llh+=  (count*Math.log(tfidfMap.get(terms[i])));
		}
		
		return llh;
	}
	/**
	 * compute posterior log llh of this doc with its cited docs, aspect cosine similarity, lambda
	 * @param citedVector
	 * @param aspectSim
	 * @return
	 */
	public double posteriorLLH(TFIDFVector[] opVector, double[] opSimArr, double lambda){
		double llh = 0;
		for(int i=0; i<this.tfArr.length; i++){
			int count = tfArr[i];
			String term = terms[i];
			
			double citingPro = this.getTermPro(term);
			double citedPro = 0;
			for(int j=0; j<opVector.length; j++){
				citedPro += (opVector[j].getTermPro(term)*opSimArr[j]);
//				if(citedPro!=0)
//					System.out.println(term+" "+opVector.length+" "+j+" "+opSimArr[j]+" "+citingPro+" "+citedPro);
			}
			
			llh += count*Math.log(lambda*citingPro+(1-lambda)*citedPro);
		}
		return llh;
	}
	
	/**
	 * compute cosine similarity of two TFIDF vectors.
	 * @param v1
	 * @param v2
	 * @return
	 */
	public static double computeCosineSim(TFIDFVector v1, TFIDFVector v2){
		double sim=0, module1=0, module2=0, product=0;

		for(String term : TFIDFVector.vocabulary){
			float w1=0, w2=0;
			if(v1.tfidfMap.keySet().contains(term))
				w1 = v1.tfidfMap.get(term);
			if(v2.tfidfMap.keySet().contains(term))
				w2 = v2.tfidfMap.get(term);
			product += (w1*w2);
			module1 += Math.pow(w1, 2);
			module2 += Math.pow(w2, 2);
		}
		sim = product/(Math.sqrt(module1)*Math.sqrt(module2));
		return Double.isNaN(sim)?0 : sim;
	}

}

public class BaseLineMethod {
	/**
	 * command options (with all the parameters)
	 */
	CmdOption cmdOption;		

	/**
	 * 
	 */
	static DataRaw rawdata = new DataRaw();
	
	public BaseLineMethod(CmdOption _cmdOption){
		cmdOption = _cmdOption;
		DataParsed parsedData = new DataParsed();
		rawdata = parsedData.initBaseLine(cmdOption.paperfolder,cmdOption.graphfile, cmdOption.aspectfile);
	}
	/**
	 * 
	 * @param indexDir
	 */
	public void buildIndex(String indexDir){
		
//		System.out.println(Debugger.getCallerPosition()+" is RawData null "+rawdata==null);
		
		IndexWriterConfig conf = new IndexWriterConfig(Version.LUCENE_36, new PorterAnalyzer());
		IndexWriter w;
		try {
			w = new IndexWriter(FSDirectory.open(new File(indexDir)), conf);
			w.deleteAll();
			w.commit();
			
			System.out.println(rawdata.id2Docs.size());
			
			for(Map.Entry<Integer, Doc> entry : rawdata.id2Docs.entrySet()){
				Document document = BaseLineMethod.convertDoc2Document(entry.getValue());
				w.addDocument(document);
			}
			
			w.commit();
			w.close();
			
		} catch (CorruptIndexException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (LockObtainFailedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * 
	 * @param lamda
	 */
	public void calLikelihoodFromIndex(String indexDir, double lambda){
		try {
			IndexReader ir = IndexReader.open(FSDirectory.open(new File(indexDir)));
			IndexSearcher is = new IndexSearcher(ir);
			int numDocs = ir.maxDoc();
			
			double LLH = 0;
			
			//vocabulary list
			List<String> vocab = new ArrayList<String>();
			
			TermEnum te = ir.terms();
			//create vocabulary
			while(te.next()){
				String term = te.term().text();
//				System.out.println(term);
				vocab.add(term);
			}
			TFIDFVector.vocabulary = vocab;
			
			//dataset id to index id
			Map<Integer, Integer> idMap = new HashMap<Integer, Integer>();
			
			for(int i=0; i<numDocs; i++){
				Document doc = ir.document(i);
				idMap.put(Integer.parseInt(doc.get("docid")), i);
			}
			
			//o -> a -> o'
			Map<Integer, Map<Integer, Map<Integer, Double>>> cosineSimMap = new HashMap<Integer, Map<Integer, Map<Integer, Double>>>();
			// (o | o') dataset id -> tfidf vector
			Map<Integer, TFIDFVector> docVectorMap = new HashMap<Integer, TFIDFVector>();
			// o -> a -> vector
			Map<Integer, Map<Integer, TFIDFVector>> docAspectVectorMap = new HashMap<Integer, Map<Integer, TFIDFVector>>();
			
			
			Set<Integer> citedSet = new HashSet<Integer>();
			//for all citing document
			for(Map.Entry<Integer, List<Integer>> entry : rawdata.pubId2CiteIds.entrySet()){//llh for citing documents
				int citingDatasetID = entry.getKey();
				int citingIndexID = idMap.get(citingDatasetID);
				
				//set up citing document vector
				TFIDFVector citingVector = BaseLineMethod.getFullTextTFIDFVector(docVectorMap, ir, citingDatasetID, citingIndexID, numDocs);
				float sum = citingVector.sum();
				
//				System.out.println(Debugger.getCallerPosition()+" "+citingDatasetID);
				
				List<Integer> refList = entry.getValue();
				//for all aspects
				for(Integer aspectID : rawdata.id2Aspect.keySet()){
					String aspect = rawdata.id2Aspect.get(aspectID);
					//set up citing document aspect vector
					double aspectSim = 0;
					if(rawdata.id2Docs.get(citingDatasetID).getText().get(aspectID).length()!=0){
						TFIDFVector citingAspectVector = BaseLineMethod.getAspectTFIDFVector(docAspectVectorMap, ir, citingDatasetID, citingIndexID, aspectID, numDocs);
						citingAspectVector.normalizedBy(sum);
						
						int refSize = refList.size();
						TFIDFVector[] citedVectors = new TFIDFVector[refSize];
						double[] cosineSims = new double[refSize];
						int count = 0;

						//for all cited documents of this citing document
						for(Integer citedDatasetID : refList){
							citedSet.add(citedDatasetID);
							//set up cited document vector
							int citedIndexID = idMap.get(citedDatasetID);
							TFIDFVector citedVector = BaseLineMethod.getFullTextTFIDFVector(docVectorMap, ir, citedDatasetID, citedIndexID, numDocs);
							citedVector.normalize();
							
							aspectSim = TFIDFVector.computeCosineSim(citedVector, citingAspectVector);
//							System.out.println(Debugger.getCallerPosition()+"\t\t"+aspectSim);
							citedVectors[count] = citedVector;
							cosineSims[count] = aspectSim;
							count++;
						}
						double aspectLLH = citingAspectVector.posteriorLLH(citedVectors, cosineSims, lambda);
						LLH+=aspectLLH;
					}
					//						Util.update3Map(cosineSimMap, citingDatasetID, aspectID, citedDatasetID, aspectSim);
				}
			}
			
			for(Integer citedDatasetID : citedSet){
				int citedIndexID = idMap.get(citedDatasetID);
				TFIDFVector citedVector = BaseLineMethod.getFullTextTFIDFVector(docVectorMap, ir, citedDatasetID, citedIndexID, numDocs);
				citedVector.normalize();
				LLH += citedVector.priorLLH();
			}
			
			System.out.println(LLH);
			is.close();
			ir.close();
			
		} catch (CorruptIndexException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	/**
	 * 
	 * @param docVectorMap
	 * @param ir
	 * @param datasetID
	 * @param indexID
	 * @param numDocs
	 * @return
	 */
	public static TFIDFVector getFullTextTFIDFVector(Map<Integer, TFIDFVector> docVectorMap, IndexReader ir, int datasetID, int indexID, int numDocs){
		TFIDFVector vector = null;
		try{
			if( (vector=docVectorMap.get(datasetID))==null ){
				TermFreqVector termFreqVector = ir.getTermFreqVector(indexID, "fulltext");
				int[] tf = termFreqVector==null?new int[0] : termFreqVector.getTermFrequencies();
				String[] terms = termFreqVector==null?new String[0] : termFreqVector.getTerms();
				
				int[] df = new int[tf.length];
				for(int j=0; j<terms.length; j++)
					df[j] = ir.docFreq(new Term(terms[j]));

				vector = new TFIDFVector(terms, tf, df, numDocs);
//				docVectorMap.put(datasetID, vector);
			}
		}
		catch(IOException e){
			e.printStackTrace();
		}
		
		return vector;
	}
	/**
	 * 
	 * @param docAspectVectorMap
	 * @param ir
	 * @param datasetID
	 * @param indexID
	 * @param aspectID
	 * @param numDocs
	 * @return
	 */
	public static TFIDFVector getAspectTFIDFVector(Map<Integer, Map<Integer, TFIDFVector>> docAspectVectorMap, IndexReader ir, int datasetID, int indexID, int aspectID, int numDocs){
		TFIDFVector vector = null;
		try{
			Map<Integer, TFIDFVector> aspectVectorMap = docAspectVectorMap.get(datasetID);
			if(aspectVectorMap==null){
				aspectVectorMap = new HashMap<Integer, TFIDFVector>();
				docAspectVectorMap.put(datasetID, aspectVectorMap);
				
				TermFreqVector termFreqVector = ir.getTermFreqVector(indexID, rawdata.id2Aspect.get(aspectID));
				int[] tf = termFreqVector==null?new int[0] : termFreqVector.getTermFrequencies();
				String[] terms = termFreqVector==null?new String[0] : termFreqVector.getTerms();
				
				int[] df = new int[tf.length];
				for(int j=0; j<terms.length; j++)
					df[j] = ir.docFreq(new Term(terms[j]));

				vector = new TFIDFVector(terms, tf, df, numDocs);
//				aspectVectorMap.put(aspectID, vector);
			}
			else
				if((vector=aspectVectorMap.get(aspectID))==null){
					TermFreqVector termFreqVector = ir.getTermFreqVector(indexID, rawdata.id2Aspect.get(aspectID));
					int[] tf = termFreqVector==null?new int[0] : termFreqVector.getTermFrequencies();
					String[] terms = termFreqVector==null?new String[0] : termFreqVector.getTerms();
					
					int[] df = new int[tf.length];
					for(int j=0; j<terms.length; j++)
						df[j] = ir.docFreq(new Term(terms[j]));

					vector = new TFIDFVector(terms, tf, df, numDocs);
//					aspectVectorMap.put(aspectID, vector);
				}
		}
		catch(IOException e){
			e.printStackTrace();
		}
		
		return vector;
	}
	
	
	/**
	 * 
	 * @param doc
	 * @return
	 */
	public static Document convertDoc2Document(Doc doc){
		Document document = new Document();
		
		document.add(new Field("docid", String.valueOf(doc.getId()), Field.Store.YES, Field.Index.ANALYZED, Field.TermVector.YES));
		
		for(int i=0; i<rawdata.id2Aspect.size(); i++){
			String aspect = rawdata.id2Aspect.get(i);
			String text = doc.getText().get(i);
			
//			System.out.println(aspect+"\t"+text);x
			
			//remove url
			String t1 = text.replaceAll("(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]", "");
			//remove non-ascii
			String t2 = t1.replaceAll("[^\\x00-\\x7F]", "");
			
			Field aspectText = new Field(aspect, t2, Field.Store.NO, Field.Index.ANALYZED, Field.TermVector.YES);
			document.add(aspectText);
		}

		Field fullText = new Field("fulltext", doc.getFullText().replaceAll("(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]", "").replaceAll("[^\\x00-\\x7F]", ""), Field.Store.NO, Field.Index.ANALYZED , Field.TermVector.YES);
		document.add(fullText);
		
//		System.out.println(doc.getId()+" "+doc.getFullText());
		
		return document;
	}
	
	
	/**
	 * Analyser that remove stop words and stem words.
	 * @author chu
	 *
	 */
	public static class PorterAnalyzer extends Analyzer implements Serializable {
		public final TokenStream tokenStream(String fieldName, Reader reader) {
			//                return new StopFilter(new LowerCaseTokenizer(reader), StopAnalyzer.ENGLISH_STOP_WORDS);
			// we do not do porter stemming anymore.
			//	            return new StopFilter(new PorterStemFilter(new LowerCaseTokenizer(reader)),
			//	                    StopAnalyzer.ENGLISH_STOP_WORDS_SET);

			TokenStream ts = new StopFilter(Version.LUCENE_36, new PorterStemFilter(new LowerCaseTokenizer(Version.LUCENE_36, reader)),
					StopAnalyzer.ENGLISH_STOP_WORDS_SET);
			
			return ts;
		}
	}
	
	public static void main(String[] args){
		CmdOption option = new CmdOption();
		CmdLineParser parser = new CmdLineParser(option);
		
		try {
			parser.parseArgument(args);
		} catch (CmdLineException e) {
			// TODO Auto-generated catch block
			System.out.println(Debugger.getCallerPosition()+"Command line error: " + e.getMessage());
			e.printStackTrace();
			return;
		}
		
		BaseLineMethod blm = new BaseLineMethod(option);
//		blm.buildIndex("./data/index/"+option.SAMPLER_ID);
		blm.calLikelihoodFromIndex("./data/index/"+option.SAMPLER_ID, option.lambda);
	}
}
