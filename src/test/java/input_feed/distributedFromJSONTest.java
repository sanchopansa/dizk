package input_feed;

import algebra.curves.barreto_naehrig.bn254a.BN254aFields;
import configuration.Configuration;
import input_feed.distributed.JSONToDistributedR1CS;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.storage.StorageLevel;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import relations.objects.Assignment;
import relations.r1cs.R1CSRelationRDD;
import scala.Tuple2;

import java.io.Serializable;

import static org.junit.Assert.assertTrue;


public class distributedFromJSONTest implements Serializable {
    private transient JavaSparkContext sc;
    private JSONToDistributedR1CS<BN254aFields.BN254aFr> converter;

    @Before
    public void setUp() {
        sc = new JavaSparkContext("local", "ZKSparkTestSuite");
        int numExecutors = 1;
        int numCores = 1;
        int numMemory = 1;
        int numPartitions = 2;

        Configuration config = new Configuration(
                numExecutors, numCores, numMemory, numPartitions, sc, StorageLevel.MEMORY_ONLY());

        String jsonFilePath = "src/test/data/json/";
        converter = new JSONToDistributedR1CS<>(jsonFilePath, config);
    }

    @After
    public void tearDown() {
        sc.stop();
        sc = null;
    }

    @Test
    public void distributedR1CSFromJSONTest() {
        String fileName = "satisfiable_pepper.json";
        R1CSRelationRDD<BN254aFields.BN254aFr> pepperR1CS = converter.loadR1CS(fileName);
        assertTrue(pepperR1CS.isValid());

        Tuple2<Assignment<BN254aFields.BN254aFr>, JavaPairRDD<Long, BN254aFields.BN254aFr>>
                pepperWitness = converter.loadWitness(fileName);
        assertTrue(pepperR1CS.isSatisfied(pepperWitness._1(), pepperWitness._2()));
    }


}

