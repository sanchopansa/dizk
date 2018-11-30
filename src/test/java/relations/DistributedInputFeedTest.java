package relations;

import algebra.curves.barreto_naehrig.bn254a.*;
import configuration.Configuration;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.storage.StorageLevel;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import relations.objects.Assignment;
import relations.r1cs.FileToR1CS;

import relations.r1cs.R1CSRelationRDD;
import scala.Tuple3;
import java.io.Serializable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DistributedInputFeedTest implements Serializable {
    private transient JavaSparkContext sc;
    private Configuration config;
    private Tuple3<R1CSRelationRDD<BN254aFields.BN254aFr>,
            Assignment<BN254aFields.BN254aFr>,
            JavaPairRDD<Long, BN254aFields.BN254aFr>> distributedFromJSON;
    private String jsonFilePath;
    private String textFilePath;


    @Before
    public void setUp() {
        sc = new JavaSparkContext("local", "ZKSparkTestSuite");
        config = new Configuration(1, 1, 1, 2, sc, StorageLevel.MEMORY_ONLY());

        jsonFilePath = "src/test/data/json/";
        textFilePath = "src/test/data/text/";

        distributedFromJSON = FileToR1CS.distributedR1CSFromJSON(jsonFilePath + "pepper_out.json", config);
//        distributedFromFileExample = FileToR1CS.distributedR1CSFromPlainText(textFilePath);


    }

    @After
    public void tearDown() {
        sc.stop();
        sc = null;
    }

    @Test
    public void distributedR1CSFromJSONTest() {

        assertTrue(distributedFromJSON._1().isValid());

        assertTrue(distributedFromJSON._1().isSatisfied(distributedFromJSON._2(), distributedFromJSON._3()));
    }




}
