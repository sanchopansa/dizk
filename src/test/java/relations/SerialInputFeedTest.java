package relations;

import algebra.curves.barreto_naehrig.bn254a.*;
import algebra.curves.barreto_naehrig.bn254a.bn254a_parameters.BN254aG1Parameters;
import algebra.curves.barreto_naehrig.bn254a.bn254a_parameters.BN254aG2Parameters;
import configuration.Configuration;
import org.apache.spark.api.java.JavaSparkContext;
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

public class SerialInputFeedTest implements Serializable {
    private transient JavaSparkContext sc;
    private Configuration config;
    private Tuple3<R1CSRelation<BN254aFields.BN254aFr>,
            Assignment<BN254aFields.BN254aFr>,
            Assignment<BN254aFields.BN254aFr>> r1csFromJSON;
    private R1CSRelation<BN254aFields.BN254aFr> r1csFromText;
    private BN254aFields.BN254aFr fieldFactory;
    private BN254aG1 g1Factory;
    private BN254aG2 g2Factory;
    private R1CSRelation<BN254aFields.BN254aFr> r1cs;
    private Assignment<BN254aFields.BN254aFr> primary;
    private Assignment<BN254aFields.BN254aFr> auxiliary;
    private BN254aPairing pairing;
    private CRS<BN254aFields.BN254aFr, BN254aG1, BN254aG2, BN254aGT> CRS;
    private Proof<BN254aG1, BN254aG2> proof;
    private String jsonFilePath;
    private String textFilePath;


    @Before
    public void setUp() {
        // Dummy Configuration
        config = new Configuration();

        jsonFilePath = "src/test/data/json/";
        textFilePath = "src/test/data/text/cropped_hash/";

        r1csFromJSON = FileToR1CS.serialR1CSFromJSON(jsonFilePath + "satisfiable_pepper.json");

        // Notice that FromText returns only the constraints.
        r1csFromText = FileToR1CS.serialR1CSFromPlainText(textFilePath + "cropped_hash");
    }

    @After
    public void tearDown() {
    }

    @Test
    public void serialR1CSFromJSONTest() {
        assertTrue(r1csFromJSON._1().isValid());
        assertTrue(r1csFromJSON._1().isSatisfied(r1csFromJSON._2(), r1csFromJSON._3()));
    }


    @Test
    public void serialR1CSFromTextTest() {
        assertTrue(r1csFromText.isValid());

    }

    @Test
    public void serialCRSTest() {
        // TODO - clean this up.
        fieldFactory = new BN254aFields.BN254aFr(2L);
        g1Factory = new BN254aG1Parameters().ONE();
        g2Factory = new BN254aG2Parameters().ONE();
        pairing = new BN254aPairing();

        r1cs = r1csFromJSON._1();
        primary = r1csFromJSON._2();
        auxiliary = r1csFromJSON._3();

        CRS = SerialSetup.generate(r1cs, fieldFactory, g1Factory, g2Factory, pairing, config);

        proof = SerialProver.prove(CRS.provingKey(), primary, auxiliary, fieldFactory, config);

        assertTrue(Verifier.verify(CRS.verificationKey(), primary, proof, pairing, config));
    }

}
