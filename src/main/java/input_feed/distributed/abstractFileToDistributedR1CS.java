package input_feed.distributed;

import algebra.fields.AbstractFieldElementExpanded;
import configuration.Configuration;
import org.apache.spark.api.java.JavaPairRDD;
import relations.objects.Assignment;
import relations.r1cs.R1CSRelationRDD;
import scala.Tuple2;

import java.util.ArrayList;

public abstract class abstractFileToDistributedR1CS<FieldT extends AbstractFieldElementExpanded<FieldT>> {

    private final String filePath;
    private final Configuration config;

    public abstractFileToDistributedR1CS(final String _filePath, final Configuration _config) {
        filePath = _filePath;
        config = _config;
    }

    public String filePath() {
        return filePath;
    }

    public Configuration config() {
        return config;
    }

    public abstract R1CSRelationRDD<FieldT> loadR1CS(String fileName);

    public abstract Tuple2<Assignment<FieldT>, JavaPairRDD<Long, FieldT>> loadWitness(String fileName);

    public static ArrayList<Integer>
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
