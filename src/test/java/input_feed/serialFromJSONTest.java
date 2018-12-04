package input_feed;

import algebra.curves.barreto_naehrig.bn254a.*;
import input_feed.serial.JSONToSerialR1CS;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import relations.objects.Assignment;
import relations.r1cs.R1CSRelation;
import scala.Tuple2;

import java.io.Serializable;

import static org.junit.Assert.assertTrue;

public class serialFromJSONTest implements Serializable {
    private JSONToSerialR1CS<BN254aFields.BN254aFr> converter;

    @Before
    public void setUp() {
        String jsonFilePath = "src/test/data/json/";
        converter = new JSONToSerialR1CS<>(jsonFilePath);
    }

    @After
    public void tearDown() {

    }

    @Test
    public void tinyR1CSFromJSONTest() {
        String fileName = "libsnark_tutorial.json";
        R1CSRelation<BN254aFields.BN254aFr> tinyR1CS = converter.loadR1CS(fileName);
        assertTrue(tinyR1CS.isValid());

        Tuple2<Assignment<BN254aFields.BN254aFr>, Assignment<BN254aFields.BN254aFr>>
                tinyWitness = converter.loadWitness(fileName);

        assertTrue(tinyR1CS.isSatisfied(tinyWitness._1(), tinyWitness._2()));
    }

    @Test
    public void smallR1CSFromJSONTest() {
        String fileName = "satisfiable_pepper.json";
        R1CSRelation<BN254aFields.BN254aFr>
                smallR1CS = converter.loadR1CS(fileName);
        assertTrue(smallR1CS.isValid());

        Tuple2<Assignment<BN254aFields.BN254aFr>, Assignment<BN254aFields.BN254aFr>>
                smallWitness = converter.loadWitness(fileName);
        assertTrue(smallR1CS.isSatisfied(smallWitness._1(), smallWitness._2()));

    }

}
