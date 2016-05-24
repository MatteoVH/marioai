package competition.cig.robinbaumgarten;

import net.sf.javaml.clustering.Clusterer;
import net.sf.javaml.core.Instance;
import net.sf.javaml.core.DenseInstance;
import net.sf.javaml.core.Dataset;
import net.sf.javaml.core.DefaultDataset;
import net.sf.javaml.tools.InstanceTools;
import java.io.File;
import net.sf.javaml.tools.data.FileHandler;
import net.sf.javaml.clustering.DensityBasedSpatialClustering;
import net.sf.javaml.clustering.SOM;

/**
 * Created by Sebastian on 5/23/2016.
 */


public class ClusterTest {

    public static void main(String[] args) throws Exception{

        /* basic test of instance / dataset functionality */
        double[] values = new double[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
        Instance instance = new DenseInstance(values);
        Dataset data = new DefaultDataset();

        data.add(instance);
        for (int i = 0; i < 10; i++) {
            Instance tmpInstance = InstanceTools.randomInstance(25);
            data.add(tmpInstance);
        }



        /* import csv containing rgb values from test3.png */

        // NOTE:
        // 1st argument to new File is path of csv (from root is easiest)
        // 2nd argument to new File is 0-based index of class label (all "rgb" in this case)
        // 3rd arg is separator term: comma, hence csv

        Dataset rgb_data = FileHandler.loadDataset(new File("C:/Users/Sebastian/Documents/GitHub/marioai/src/competition/cig/robinbaumgarten/rgb_values.csv"), 3, ",");
        System.out.println(rgb_data);

        /* cluster the rgb data */
        Clusterer dbscan =  new DensityBasedSpatialClustering();
        Dataset[] clusters_dbscan = dbscan.cluster(rgb_data);

        /* see what's in the clusters */
        System.out.println("DBSCAN groups:");
        for (int i = 0; i < clusters_dbscan.length; i++) {
            System.out.println(clusters_dbscan[i]);
        }



        /* SOM */

        Clusterer som =  new SOM();
        Dataset[] clusters_som = som.cluster(rgb_data);

        System.out.println("SOM groups:");
        for (int i = 0; i < clusters_dbscan.length; i++) {
            System.out.println(clusters_dbscan[i]);
        }
    }
}
