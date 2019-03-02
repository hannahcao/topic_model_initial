package chu;

import java.io.File;
import java.io.IOException;

import weka.clusterers.HierarchicalClusterer;
import weka.clusterers.SimpleKMeans;
import weka.core.EditDistance;
import weka.core.Instances;
import weka.core.converters.ArffLoader;

public class MainAspectCluster {

	public static void convertAspectFile(){
		File aspect = new File("./data/twitter3/aspect_ext.arff");
		ArffLoader loader = new ArffLoader();

		try {
			loader.setFile(aspect);
			Instances instances = loader.getDataSet();
				
			HierarchicalClusterer cluster = new HierarchicalClusterer();
			cluster.setNumClusters(5);
			cluster.setDistanceFunction(new EditDistance());	
			
			System.out.println("begin");
			cluster.buildClusterer(instances);
			System.out.println("end");
		
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public static void main(String[] args){
		MainAspectCluster.convertAspectFile();
	}

}
