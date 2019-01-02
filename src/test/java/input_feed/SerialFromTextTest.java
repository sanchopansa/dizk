package input_feed;

import algebra.curves.barreto_naehrig.bn254a.bn254a_parameters.BN254aFrParameters;
import algebra.fields.Fp;
import input_feed.serial.TextToSerialR1CS;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import relations.objects.Assignment;
import relations.r1cs.R1CSRelation;
import scala.Tuple2;

import java.io.Serializable;

import static org.junit.Assert.assertTrue;

public class SerialFromTextTest implements Serializable {
    private TextToSerialR1CS<Fp> converter;
    private R1CSRelation<Fp> r1cs;
    private Tuple2<Assignment<Fp>, Assignment<Fp>> witness;
    private BN254aFrParameters FpParameters;
    private Fp fieldFactory;


    @Before
    public void setUp() {
        FpParameters = new BN254aFrParameters();
        fieldFactory = new Fp(1, FpParameters);
    }

    @After
    public void tearDown() {
    }

    @Test
    public void tinySerialR1CSFromTextTest() {
        String fileName = "src/test/data/text/overflow/overflow";
        converter = new TextToSerialR1CS<>(fileName, fieldFactory);

        r1cs = converter.loadR1CS();
        assertTrue(r1cs.isValid());

        witness = converter.loadWitness();
        assertTrue(r1cs.isSatisfied(witness._1(), witness._2()));
    }

    @Test
    public void smallSerialR1CSFromTextTest() {
        String fileName = "src/test/data/text/contrived/small";
        converter = new TextToSerialR1CS<>(fileName, fieldFactory);

        r1cs = converter.loadR1CS();
        assertTrue(r1cs.isValid());

        witness = converter.loadWitness();
        assertTrue(r1cs.isSatisfied(witness._1(), witness._2()));
    }


}
