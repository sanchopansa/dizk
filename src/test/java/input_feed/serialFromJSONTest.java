package input_feed;

import algebra.curves.barreto_naehrig.bn254a.*;
import algebra.curves.barreto_naehrig.bn254a.bn254a_parameters.BN254aFrParameters;
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
    private BN254aFrParameters FpParameters;

    @Before
    public void setUp() {

        FpParameters = new BN254aFrParameters();

    }

    @After
    public void tearDown() {

    }

    @Test
    public void tinyR1CSFromJSONTest() {
        String filePath = "src/test/data/json/libsnark_tutorial.json";
        converter = new JSONToSerialR1CS<>(filePath, FpParameters);

        R1CSRelation<BN254aFields.BN254aFr> tinyR1CS = converter.loadR1CS();
        assertTrue(tinyR1CS.isValid());

        Tuple2<Assignment<BN254aFields.BN254aFr>, Assignment<BN254aFields.BN254aFr>>
                tinyWitness = converter.loadWitness();

        assertTrue(tinyR1CS.isSatisfied(tinyWitness._1(), tinyWitness._2()));
    }

    @Test
    public void smallR1CSFromJSONTest() {
        String filePath = "src/test/data/json/satisfiable_pepper.json";
        converter = new JSONToSerialR1CS<>(filePath, FpParameters);

        R1CSRelation<BN254aFields.BN254aFr>
                smallR1CS = converter.loadR1CS();
        assertTrue(smallR1CS.isValid());

        Tuple2<Assignment<BN254aFields.BN254aFr>, Assignment<BN254aFields.BN254aFr>>
                smallWitness = converter.loadWitness();
        assertTrue(smallR1CS.isSatisfied(smallWitness._1(), smallWitness._2()));

    }

}
