package input_feed;

import algebra.curves.barreto_naehrig.bn254a.BN254aFields;
import algebra.curves.barreto_naehrig.bn254a.bn254a_parameters.BN254aFrParameters;
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
    private Configuration config;
    private BN254aFrParameters FpParameters;

    @Before
    public void setUp() {
        sc = new JavaSparkContext("local", "ZKSparkTestSuite");
        int numExecutors = 1;
        int numCores = 1;
        int numMemory = 1;
        int numPartitions = 2;

        config = new Configuration(
                numExecutors, numCores, numMemory, numPartitions, sc, StorageLevel.MEMORY_ONLY());

        FpParameters = new BN254aFrParameters();
    }

    @After
    public void tearDown() {
        sc.stop();
        sc = null;
    }

    @Test
    public void distributedR1CSFromJSONTest() {
        String filePath = "src/test/data/json/satisfiable_pepper.json";
        converter = new JSONToDistributedR1CS<>(filePath, config, FpParameters);

        R1CSRelationRDD<BN254aFields.BN254aFr> pepperR1CS = converter.loadR1CS();
        assertTrue(pepperR1CS.isValid());

        Tuple2<Assignment<BN254aFields.BN254aFr>, JavaPairRDD<Long, BN254aFields.BN254aFr>>
                pepperWitness = converter.loadWitness();
        assertTrue(pepperR1CS.isSatisfied(pepperWitness._1(), pepperWitness._2()));
    }


}

