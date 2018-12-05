package input_feed;

import algebra.curves.barreto_naehrig.bn254a.*;

import input_feed.serial.TextToSerialR1CS;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import relations.objects.Assignment;
import relations.r1cs.R1CSRelation;
import scala.Tuple2;

import java.io.Serializable;

import static org.junit.Assert.assertTrue;

public class serialFromTextTest implements Serializable {
    private TextToSerialR1CS<BN254aFields.BN254aFr> converter;
    private R1CSRelation<BN254aFields.BN254aFr> r1cs;
    private Tuple2<Assignment<BN254aFields.BN254aFr>,
            Assignment<BN254aFields.BN254aFr>> witness;


    @Before
    public void setUp() {
        String textFilePath = "src/test/data/text/";
        converter = new TextToSerialR1CS<>(textFilePath);
    }

    @After
    public void tearDown() {
    }

    @Test
    public void tinySerialR1CSFromTextTest() {
        String fileName = "overflow/overflow";
        r1cs = converter.loadR1CS(fileName);
        assertTrue(r1cs.isValid());

        witness = converter.loadWitness(fileName);
        assertTrue(r1cs.isSatisfied(witness._1(), witness._2()));
    }

    @Test
    public void smallSerialR1CSFromTextTest() {
        String fileName = "contrived/small";
        r1cs = converter.loadR1CS(fileName);
        assertTrue(r1cs.isValid());

        witness = converter.loadWitness(fileName);
        assertTrue(r1cs.isSatisfied(witness._1(), witness._2()));
    }

}
