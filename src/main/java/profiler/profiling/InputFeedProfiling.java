package profiler.profiling;

import algebra.curves.barreto_naehrig.bn254a.BN254aFields.BN254aFr;
import algebra.curves.barreto_naehrig.bn254a.BN254aG1;
import algebra.curves.barreto_naehrig.bn254a.BN254aG2;
import algebra.curves.barreto_naehrig.bn254a.BN254aGT;
import algebra.curves.barreto_naehrig.bn254a.BN254aPairing;
import algebra.curves.barreto_naehrig.bn254a.bn254a_parameters.BN254aFrParameters;
import algebra.curves.barreto_naehrig.bn254a.bn254a_parameters.BN254aG1Parameters;
import algebra.curves.barreto_naehrig.bn254a.bn254a_parameters.BN254aG2Parameters;
import algebra.fields.Fp;
import configuration.Configuration;
import input_feed.distributed.TextToDistributedR1CS;
import org.apache.spark.api.java.JavaPairRDD;
import relations.objects.Assignment;
import relations.r1cs.R1CSRelationRDD;
import scala.Tuple2;
import zk_proof_systems.zkSNARK.DistributedProver;
import zk_proof_systems.zkSNARK.DistributedSetup;
import zk_proof_systems.zkSNARK.Verifier;
import zk_proof_systems.zkSNARK.objects.CRS;
import zk_proof_systems.zkSNARK.objects.Proof;

public class InputFeedProfiling {
//    private TextToDistributedR1CS<BN254aFr> converter;
//    private BN254aFrParameters FpParameters;
//    private Fp fieldFactory;
//    private R1CSRelationRDD<BN254aFr> r1cs;
//    private Tuple2<Assignment<BN254aFr>, JavaPairRDD<Long, BN254aFr>> witness;
//    private CRS<BN254aFr, BN254aG1, BN254aG2, BN254aGT> CRS;
//    private Proof<BN254aG1, BN254aG2> proof;

    public static void distributedSetupProfiler(final Configuration config, String filePath) {

        final BN254aFr fieldFactory = new BN254aFr(2L);
        final BN254aG1 g1Factory = new BN254aG1Parameters().ONE();
        final BN254aG2 g2Factory = new BN254aG2Parameters().ONE();
        final BN254aPairing pairing = new BN254aPairing();
//        FpParameters = new BN254aFrParameters();
//        fieldFactory = new Fp(1, FpParameters);

        TextToDistributedR1CS<BN254aFr>
                converter = new TextToDistributedR1CS<>(filePath, config, fieldFactory, true);

        R1CSRelationRDD<BN254aFr> r1cs = converter.loadR1CS();

        Tuple2<Assignment<BN254aFr>, JavaPairRDD<Long, BN254aFr>>
                witness = converter.loadWitness();

        Assignment primary = witness._1();
        JavaPairRDD fullAssignment = witness._2();

        long numConstraints = r1cs.numConstraints();

        config.setContext("Setup");
        config.beginRuntimeMetadata("Size (inputs)", numConstraints);

        config.beginLog(config.context());
        config.beginRuntime("Setup");
        final CRS<BN254aFr, BN254aG1, BN254aG2, BN254aGT>
                CRS = DistributedSetup.generate(r1cs, fieldFactory, g1Factory, g2Factory, pairing, config);
        config.endLog(config.context());
        config.endRuntime("Setup");

        config.writeRuntimeLog(config.context());

        config.setContext("Prover");
        config.beginRuntimeMetadata("Size (inputs)", numConstraints);

        config.beginLog(config.context());
        config.beginRuntime("Prover");
        Proof<BN254aG1, BN254aG2>
                proof = DistributedProver.prove(CRS.provingKeyRDD(), primary, fullAssignment, fieldFactory, config);
        config.endLog(config.context());
        config.endRuntime("Prover");

        config.writeRuntimeLog(config.context());

        config.setContext("Verifier-for-");
        config.beginRuntimeMetadata("Size (inputs)", numConstraints);

        config.beginLog(config.context());
        config.beginRuntime("Verifier");
        final boolean isValid = Verifier.verify(CRS.verificationKey(), primary, proof, pairing, config);
        config.beginRuntimeMetadata("isValid", isValid ? 1L : 0L);
        config.endLog(config.context());
        config.endRuntime("Verifier");

        config.writeRuntimeLog(config.context());

        System.out.println(isValid);
        assert (isValid);
    }
}
