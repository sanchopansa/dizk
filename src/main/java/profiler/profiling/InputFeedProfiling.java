package profiler.profiling;

import algebra.curves.barreto_naehrig.bn254a.BN254aFields.BN254aFr;
import algebra.curves.barreto_naehrig.bn254a.BN254aG1;
import algebra.curves.barreto_naehrig.bn254a.BN254aG2;
import algebra.curves.barreto_naehrig.bn254a.BN254aGT;
import algebra.curves.barreto_naehrig.bn254a.BN254aPairing;
import algebra.curves.barreto_naehrig.bn254a.bn254a_parameters.BN254aG1Parameters;
import algebra.curves.barreto_naehrig.bn254a.bn254a_parameters.BN254aG2Parameters;
import configuration.Configuration;
import input_feed.distributed.TextToDistributedR1CS;
import input_feed.serial.TextToSerialR1CS;
import org.apache.spark.api.java.JavaPairRDD;
import relations.objects.Assignment;
import relations.r1cs.R1CSRelation;
import relations.r1cs.R1CSRelationRDD;
import scala.Tuple2;
import zk_proof_systems.zkSNARK.*;
import zk_proof_systems.zkSNARK.objects.CRS;
import zk_proof_systems.zkSNARK.objects.Proof;

public class InputFeedProfiling {

    public static void distributedZKSnarkProfiler(final Configuration config, String filePath) {

        final BN254aFr fieldFactory = new BN254aFr(2L);
        final BN254aG1 g1Factory = new BN254aG1Parameters().ONE();
        final BN254aG2 g2Factory = new BN254aG2Parameters().ONE();
        final BN254aPairing pairing = new BN254aPairing();

        TextToDistributedR1CS<BN254aFr>
                converter = new TextToDistributedR1CS<>(filePath, fieldFactory, true, true);

        config.setContext("Load R1CS");

        config.beginLog(config.context());
        config.beginRuntime("Load R1CS");
        R1CSRelationRDD<BN254aFr> r1cs = converter.loadR1CS(config);
        config.endLog(config.context());
        config.endRuntime("Load R1CS");

        config.writeRuntimeLog(config.context());

        config.setContext("Load Witness");

        config.beginLog(config.context());
        config.beginRuntime("Load Witness");
        Tuple2<Assignment<BN254aFr>, JavaPairRDD<Long, BN254aFr>>
                witness = converter.loadWitness(config);
        config.endLog(config.context());
        config.endRuntime("Load Witness");

        config.writeRuntimeLog(config.context());

        Assignment<BN254aFr> primary = witness._1();
        JavaPairRDD<Long, BN254aFr> fullAssignment = witness._2();

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

    public static void serialZKSnarkProfiler(final Configuration config, String filePath) {

        final BN254aFr fieldFactory = new BN254aFr(2L);
        final BN254aG1 g1Factory = new BN254aG1Parameters().ONE();
        final BN254aG2 g2Factory = new BN254aG2Parameters().ONE();
        final BN254aPairing pairing = new BN254aPairing();

        final TextToSerialR1CS<BN254aFr>
                converter = new TextToSerialR1CS<>(filePath, fieldFactory, true);

        config.setContext("Load R1CS");

        config.beginLog(config.context());
        config.beginRuntime("Load R1CS");
        final R1CSRelation<BN254aFr> r1cs = converter.loadR1CS();
        config.endLog(config.context());
        config.endRuntime("Load R1CS");

        config.writeRuntimeLog(config.context());

        config.setContext("Load Witness");

        config.beginLog(config.context());
        config.beginRuntime("Load Witness");
        final Tuple2<Assignment<BN254aFr>, Assignment<BN254aFr>> witness = converter.loadWitness();
        config.endLog(config.context());
        config.endRuntime("Load Witness");

        config.writeRuntimeLog(config.context());

        final Assignment<BN254aFr> primary = witness._1();
        final Assignment<BN254aFr> auxiliary = witness._2();

        long numConstraints = r1cs.numConstraints();

        config.setContext("Setup");
        config.beginRuntimeMetadata("Size (inputs)", numConstraints);

        config.beginLog(config.context());
        config.beginRuntime("Setup");
        final CRS<BN254aFr, BN254aG1, BN254aG2, BN254aGT>
                CRS = SerialSetup.generate(r1cs, fieldFactory, g1Factory, g2Factory, pairing, config);
        config.endLog(config.context());
        config.endRuntime("Setup");

        config.writeRuntimeLog(config.context());

        config.setContext("Prover");
        config.beginRuntimeMetadata("Size (inputs)", numConstraints);

        config.beginLog(config.context());
        config.beginRuntime("Prover");
        final Proof<BN254aG1, BN254aG2>
                proof = SerialProver.prove(CRS.provingKey(), primary, auxiliary, fieldFactory, config);
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
