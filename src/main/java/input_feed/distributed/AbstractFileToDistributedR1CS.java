package input_feed.distributed;

import algebra.fields.AbstractFieldElementExpanded;
import algebra.fields.abstractfieldparameters.AbstractFpParameters;
import configuration.Configuration;
import org.apache.spark.api.java.JavaPairRDD;
import relations.objects.Assignment;
import relations.r1cs.R1CSRelationRDD;
import scala.Tuple2;

import java.util.ArrayList;

public abstract class AbstractFileToDistributedR1CS<FieldT extends AbstractFieldElementExpanded<FieldT>> {

    private final String filePath;
    private final Configuration config;
    private final FieldT fieldParameters;
    private boolean negate;

    AbstractFileToDistributedR1CS(final String _filePath, final Configuration _config, final FieldT _fieldParameters) {
        filePath = _filePath;
        config = _config;
        fieldParameters = _fieldParameters;
    }

    AbstractFileToDistributedR1CS(
            final String _filePath,
            final Configuration _config,
            final FieldT _fieldParameters,
            final boolean _negate) {
        filePath = _filePath;
        config = _config;
        fieldParameters = _fieldParameters;
        negate = _negate;
    }

    String filePath() {
        return filePath;
    }

    Configuration config() {
        return config;
    }

    FieldT fieldParameters() { return fieldParameters; }

    boolean negate() { return negate; }

    public abstract R1CSRelationRDD<FieldT> loadR1CS();

    public abstract Tuple2<Assignment<FieldT>, JavaPairRDD<Long, FieldT>> loadWitness();

    static ArrayList<Integer>
    constructPartitionArray(int numPartitions, long numConstraints){
        final ArrayList<Integer> partitions = new ArrayList<>();
        for (int i = 0; i < numPartitions; i++) {
            partitions.add(i);
        }
        if (numConstraints % 2 != 0) {
            partitions.add(numPartitions);
        }
        return partitions;
    }
}
