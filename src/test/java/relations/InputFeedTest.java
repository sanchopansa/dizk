package relations;

import algebra.curves.barreto_naehrig.bn254a.*;
import algebra.curves.barreto_naehrig.bn254a.bn254a_parameters.BN254aG1Parameters;
import algebra.curves.barreto_naehrig.bn254a.bn254a_parameters.BN254aG2Parameters;
import configuration.Configuration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import relations.objects.Assignment;
import relations.r1cs.FileToR1CS;
import relations.r1cs.R1CSRelation;
import scala.Tuple3;
import zk_proof_systems.zkSNARK.SerialProver;
import zk_proof_systems.zkSNARK.SerialSetup;
import zk_proof_systems.zkSNARK.Verifier;
import zk_proof_systems.zkSNARK.objects.CRS;
import zk_proof_systems.zkSNARK.objects.Proof;

import java.io.Serializable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class InputFeedTest implements Serializable {

    private Tuple3<R1CSRelation<BN254aFields.BN254aFr>,
            Assignment<BN254aFields.BN254aFr>,
            Assignment<BN254aFields.BN254aFr>> fromJSONExample;
    private R1CSRelation<BN254aFields.BN254aFr> fromFileExample;
    private BN254aFields.BN254aFr fieldFactory;
    private BN254aG1 g1Factory;
    private BN254aG2 g2Factory;
    private R1CSRelation<BN254aFields.BN254aFr> r1cs;
    private Assignment<BN254aFields.BN254aFr> primary;
    private Assignment<BN254aFields.BN254aFr> auxiliary;
    private BN254aPairing pairing;
    private Configuration config;
    private CRS<BN254aFields.BN254aFr, BN254aG1, BN254aG2, BN254aGT> CRS;
    private Proof<BN254aG1, BN254aG2> proof;


    @Before
    public void setUp() {
        // Dummy Configuration
        config = new Configuration();

        fieldFactory = new BN254aFields.BN254aFr(2L);
        g1Factory = new BN254aG1Parameters().ONE();
        g2Factory = new BN254aG2Parameters().ONE();
        pairing = new BN254aPairing();

//        final String filePath = "src/test/data/satisfiable_pepper.json";
//        final String filePath = "src/test/data/satisfiable_pepper.json";

        fromJSONExample = FileToR1CS.serialR1CSFromJSON("src/test/data/json/satisfiable_pepper.json");

        fromFileExample = FileToR1CS.serialR1CSFromPlainText("src/test/data/text/");

    }

    @After
    public void tearDown() {

    }

    @Test
    public void inputSatisfiedTest() {
        final R1CSRelation<BN254aFields.BN254aFr> r1cs = fromJSONExample._1();

        assertTrue(r1cs.isSatisfied(fromJSONExample._2(), fromJSONExample._3()));
    }

    @Test
    public void plainInputTest() {
        assertTrue(fromFileExample.isValid());
    }

    @Test
    public void serialCRSTest() {

        r1cs = fromJSONExample._1();
        primary = fromJSONExample._2();
        auxiliary = fromJSONExample._3();

        CRS = SerialSetup.generate(r1cs, fieldFactory, g1Factory, g2Factory, pairing, config);

        proof = SerialProver.prove(CRS.provingKey(), primary, auxiliary, fieldFactory, config);

        assertTrue(Verifier.verify(CRS.verificationKey(), primary, proof, pairing, config));
    }

}
