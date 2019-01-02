package input_feed;

import algebra.curves.barreto_naehrig.bn254a.bn254a_parameters.BN254aFrParameters;
import algebra.fields.Fp;
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


public class DistributedFromJSONTest implements Serializable {
    private transient JavaSparkContext sc;
    private Configuration config;
    private BN254aFrParameters FpParameters;
    private JSONToDistributedR1CS<Fp> converter;
    private R1CSRelationRDD<Fp> r1cs;
    private Tuple2<Assignment<Fp>, JavaPairRDD<Long, Fp>> witness;
    private Fp fieldFactory;


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
        fieldFactory = new Fp(1, FpParameters);
    }

    @After
    public void tearDown() {
        sc.stop();
        sc = null;
    }

    @Test
    public void distributedR1CSFromJSONTest() {
        String filePath = "src/test/data/json/satisfiable_pepper.json";
        converter = new JSONToDistributedR1CS<>(filePath, fieldFactory);

        r1cs = converter.loadR1CS(config);
        assertTrue(r1cs.isValid());

        witness = converter.loadWitness(config);
        assertTrue(r1cs.isSatisfied(witness._1(), witness._2()));
    }

    @Test
    public void distributedR1CSFromJSONTest2() {
        String filePath = "src/test/data/json/libsnark_tutorial.json";
        converter = new JSONToDistributedR1CS<>(filePath, fieldFactory);

        r1cs = converter.loadR1CS(config);
        assertTrue(r1cs.isValid());

        witness = converter.loadWitness(config);
        assertTrue(r1cs.isSatisfied(witness._1(), witness._2()));
    }


}

