package profiler.profiling;

import algebra.curves.barreto_naehrig.bn254a.*;
import algebra.curves.barreto_naehrig.bn254a.bn254a_parameters.BN254aFrParameters;
import algebra.curves.barreto_naehrig.bn254a.bn254a_parameters.BN254aG1Parameters;
import algebra.curves.barreto_naehrig.bn254a.bn254a_parameters.BN254aG2Parameters;
import configuration.Configuration;
import input_feed.distributed.JSONToDistributedR1CS;
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
    private TextToDistributedR1CS<BN254aFields.BN254aFr> converter;
    private BN254aFrParameters FpParameters;

    public void distributedSetupProfiler(final Configuration config) {

        final BN254aFields.BN254aFr fieldFactory = new BN254aFields.BN254aFr(2L);
        final BN254aG1 g1Factory = new BN254aG1Parameters().ONE();
        final BN254aG2 g2Factory = new BN254aG2Parameters().ONE();
        final BN254aPairing pairing = new BN254aPairing();

//        String filePath = "/input";
//        FpParameters = new BN254aFrParameters();
//        converter = new TextToDistributedR1CS<>(filePath, config, FpParameters);
//
//        R1CSRelationRDD r1cs = converter.loadR1CS();
//
//        Tuple2<Assignment<BN254aFields.BN254aFr>, JavaPairRDD<Long, BN254aFields.BN254aFr>>
//                witness = converter.loadWitness();
//
//        Assignment primary = witness._1();
//        JavaPairRDD fullAssignment = witness._2();
//
//        long numConstraints = r1cs.numConstraints();
//
//        config.setContext("Setup");
//        config.beginRuntimeMetadata("Size (inputs)", numConstraints);
//
//        config.beginLog(config.context());
//        config.beginRuntime("Setup");
//        final CRS<BN254aFields.BN254aFr, BN254aG1, BN254aG2, BN254aGT> CRS =
//                DistributedSetup.generate(r1cs, fieldFactory, g1Factory, g2Factory, pairing, config);
//        config.endLog(config.context());
//        config.endRuntime("Setup");
//
//        config.writeRuntimeLog(config.context());
//
//        config.setContext("Prover");
//        config.beginRuntimeMetadata("Size (inputs)", numConstraints);
//
//        config.beginLog(config.context());
//        config.beginRuntime("Prover");
//        final Proof<BN254aG1, BN254aG2> proof =
//                DistributedProver.prove(CRS.provingKeyRDD(), primary, fullAssignment, fieldFactory, config);
//        config.endLog(config.context());
//        config.endRuntime("Prover");
//
//        config.writeRuntimeLog(config.context());
//
//        config.setContext("Verifier-for-");
//        config.beginRuntimeMetadata("Size (inputs)", numConstraints);
//
//        config.beginLog(config.context());
//        config.beginRuntime("Verifier");
//        final boolean isValid = Verifier.verify(CRS.verificationKey(), primary, proof, pairing, config);
//        config.beginRuntimeMetadata("isValid", isValid ? 1L : 0L);
//        config.endLog(config.context());
//        config.endRuntime("Verifier");
//
//        config.writeRuntimeLog(config.context());
//
//        System.out.println(isValid);
//        assert (isValid);
    }
}
