package relations;

import algebra.curves.barreto_naehrig.bn254a.BN254aFields;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import relations.objects.Assignment;
import relations.r1cs.FileToR1CS;
import relations.r1cs.R1CSRelation;
import scala.Tuple3;

import java.io.Serializable;

import static org.junit.Assert.assertTrue;

public class InputFeedTest implements Serializable {

    private Tuple3<R1CSRelation<BN254aFields.BN254aFr>,
            Assignment<BN254aFields.BN254aFr>,
            Assignment<BN254aFields.BN254aFr>> fromJSONExample;

    @Before
    public void setUp() {

        final BN254aFields.BN254aFr fieldFactory = new BN254aFields.BN254aFr(2L);
        final String filePath = "src/test/data/pepper_out.json";

        fromJSONExample = FileToR1CS.R1CSFromJSON(fieldFactory, filePath);
    }

    @After
    public void tearDown() {

    }

    @Test
    public void runInputFeedTest() {
        final R1CSRelation<BN254aFields.BN254aFr> r1cs = fromJSONExample._1();

        assertTrue(r1cs.isSatisfied(fromJSONExample._2(), fromJSONExample._3()));


    }
}
