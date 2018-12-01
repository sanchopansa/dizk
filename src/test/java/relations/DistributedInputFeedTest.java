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
            JavaPairRDD<Long, BN254aFields.BN254aFr>> r1csFromJSON;
    private R1CSRelationRDD<BN254aFields.BN254aFr> r1csFromText;
    private String jsonFilePath;
    private String textFilePath;


    @Before
    public void setUp() {
        sc = new JavaSparkContext("local", "ZKSparkTestSuite");
        config = new Configuration(1, 1, 1, 2, sc, StorageLevel.MEMORY_ONLY());

        jsonFilePath = "src/test/data/json/";
        textFilePath = "src/test/data/text/cropped_hash";

        r1csFromJSON = FileToR1CS.distributedR1CSFromJSON(jsonFilePath + "libsnark_tutorial.json", config);
        r1csFromText = FileToR1CS.distributedR1CSFromText(textFilePath, config);


    }

    @After
    public void tearDown() {
        sc.stop();
        sc = null;
    }

    @Test
    public void distributedR1CSFromJSONTest() {

        assertTrue(r1csFromJSON._1().isValid());

        assertTrue(r1csFromJSON._1().isSatisfied(r1csFromJSON._2(), r1csFromJSON._3()));
    }

    @Test
    public void distributedR1CSFromTextTest() {

        assertTrue(r1csFromText.isValid());

    }



}
