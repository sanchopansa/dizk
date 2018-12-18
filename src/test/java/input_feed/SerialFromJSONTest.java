package input_feed;

import algebra.curves.barreto_naehrig.bn254a.BN254aFields.BN254aFr;
import algebra.curves.barreto_naehrig.bn254a.bn254a_parameters.BN254aFrParameters;
import algebra.fields.Fp;
import input_feed.serial.JSONToSerialR1CS;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import relations.objects.Assignment;
import relations.r1cs.R1CSRelation;
import scala.Tuple2;

import java.io.Serializable;

import static org.junit.Assert.assertTrue;

public class SerialFromJSONTest implements Serializable {
    private JSONToSerialR1CS<Fp> converter;
    private BN254aFrParameters FpParameters;
    private Fp fieldFactory;
    private R1CSRelation<Fp> r1cs;
    private Tuple2<Assignment<Fp>, Assignment<Fp>> witness;

    @Before
    public void setUp() {

        FpParameters = new BN254aFrParameters();
        fieldFactory = new Fp(1, FpParameters);
    }

    @After
    public void tearDown() {

    }

    @Test
    public void tinyR1CSFromJSONTest() {
        String filePath = "src/test/data/json/libsnark_tutorial.json";
        converter = new JSONToSerialR1CS<>(filePath, fieldFactory);

        r1cs = converter.loadR1CS();
        assertTrue(r1cs.isValid());

        witness = converter.loadWitness();
        assertTrue(r1cs.isSatisfied(witness._1(), witness._2()));
    }

    @Test
    public void smallR1CSFromJSONTest() {
        String filePath = "src/test/data/json/satisfiable_pepper.json";
        converter = new JSONToSerialR1CS<>(filePath, fieldFactory);

        r1cs = converter.loadR1CS();
        assertTrue(r1cs.isValid());

        witness = converter.loadWitness();
        assertTrue(r1cs.isSatisfied(witness._1(), witness._2()));

    }

}
