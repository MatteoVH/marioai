package competition.cig.robinbaumgarten;

import net.sf.javaml.clustering.Clusterer;
import net.sf.javaml.clustering.KMeans;
import net.sf.javaml.core.Instance;
import net.sf.javaml.core.DenseInstance;
import net.sf.javaml.core.Dataset;
import net.sf.javaml.core.DefaultDataset;
import net.sf.javaml.distance.EuclideanDistance;
import net.sf.javaml.tools.InstanceTools;
import java.io.File;
import net.sf.javaml.tools.data.FileHandler;
import net.sf.javaml.clustering.DensityBasedSpatialClustering;
import net.sf.javaml.clustering.SOM;
import net.sf.javaml.clustering.KMedoids;
import net.sf.javaml.distance.EuclideanDistance;
import java.util.Arrays;

import static net.sf.javaml.clustering.SOM.GridType.HEXAGONAL;
//import static net.sf.javaml.clustering.SOM.GridType.RECTANGLES;

/**
 * Created by Sebastian on 5/23/2016.
 */


public class ClusterTest {

    public static void main(String[] args) throws Exception{

        /* basic test of instance / dataset functionality */
        /*
        double[] values = new double[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
        Instance instance = new DenseInstance(values);
        Dataset data = new DefaultDataset();

        data.add(instance);
        for (int i = 0; i < 10; i++) {
            Instance tmpInstance = InstanceTools.randomInstance(25);
            data.add(tmpInstance);
        }
        */


        /* import csv containing rgb values from test3.png */

        // NOTE:
        // 1st argument to new File is path of csv (from root is easiest)
        // 2nd argument to new File is 0-based index of class label (all "rgb" in this case)
        // 3rd arg is separator term: comma, hence csv

        //Dataset rgb_data = FileHandler.loadDataset(new File("C:/Users/Sebastian/Documents/GitHub/marioai/src/competition/cig/robinbaumgarten/newest_test.csv"), 0, ",");
        //System.out.println(rgb_data);

        /* cluster the rgb data */
        //Clusterer dbscan =  new DensityBasedSpatialClustering();
        //Dataset[] clusters_dbscan = dbscan.cluster(rgb_data);

        /* see what's in the clusters */
        /*
        System.out.println("DBSCAN groups:");
        for (int i = 0; i < clusters_dbscan.length; i++) {
            System.out.println("about to print rows");
            System.out.println(clusters_dbscan[i]);
        }
        */



        /* SOM */

        Dataset rgb_data = FileHandler.loadDataset(new File("C:/Users/Sebastian/Documents/GitHub/marioai/src/competition/cig/robinbaumgarten/newest_test.csv"), 0, ",");
        //System.out.println(rgb_data);

        //Clusterer som =  new SOM();
        Clusterer som =  new SOM(3, 3, HEXAGONAL, 1000, 0.1D, 8, SOM.LearningType.LINEAR, SOM.NeighbourhoodFunction.STEP);
        Dataset[] clusters_som = som.cluster(rgb_data);

        Dataset data = new DefaultDataset();

        System.out.println("SOM groups:");
        System.out.println(clusters_som.length);
        for (int i = 0; i < clusters_som.length; i++) {
            //System.out.println(clusters_som[i].size());

            double[] values = new double[305];
            Arrays.fill(values, 0);

            /* add up each element of each instance within cluster */
            for (int j = 0; j < clusters_som[i].size(); j++) {
                //System.out.println(clusters_som[i].instance(j));
                //instance.(clusters_som[i].instance(j));
                for (int k = 0; k < 305; k++) {
                    values[k] += clusters_som[i].instance(j).value(k);
                }
            }

            for (int k = 0; k < 305; k++) {
                values[k] /= clusters_som[i].size();
            }
            Instance instance = new DenseInstance(values);
            System.out.println(instance);
            data.add(instance);
        }


        /* KMEANS */
        /*
        Clusterer kmeans =  new KMedoids(5, 1000, EuclideanDistance);
        Dataset[] clusters_kmeans = kmeans.cluster(rgb_data);

        System.out.println("KMEANS groups:");
        for (int i = 0; i < clusters_kmeans.length; i++) {
            System.out.println(clusters_kmeans[i]);
        }
        */


    }
}
