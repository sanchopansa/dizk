package input_feed;

import algebra.curves.barreto_naehrig.bn254a.BN254aFields;
import configuration.Configuration;
import input_feed.distributed.TextToDistributedR1CS;
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


public class distributedFromTextTest implements Serializable {
    private transient JavaSparkContext sc;
    private TextToDistributedR1CS<BN254aFields.BN254aFr> converter;
    private R1CSRelationRDD<BN254aFields.BN254aFr> r1cs;
    private Tuple2<Assignment<BN254aFields.BN254aFr>,
            JavaPairRDD<Long, BN254aFields.BN254aFr>> witness;

    @Before
    public void setUp() {
        sc = new JavaSparkContext("local", "ZKSparkTestSuite");
        int numExecutors = 1;
        int numCores = 1;
        int numMemory = 1;
        int numPartitions = 2;

        Configuration config = new Configuration(
                numExecutors, numCores, numMemory, numPartitions, sc, StorageLevel.MEMORY_ONLY());

        String textFilePath = "src/test/data/text/contrived/";
        converter = new TextToDistributedR1CS<>(textFilePath, config);
    }

    @After
    public void tearDown() {
        sc.stop();
        sc = null;
    }

    @Test
    public void distributedR1CSFromTextTest() {
        String fileName = "small";
        r1cs = converter.loadR1CS(fileName);
        assertTrue(r1cs.isValid());

        witness = converter.loadWitness(fileName);
        assertTrue(r1cs.isSatisfied(witness._1(), witness._2()));
    }

}
