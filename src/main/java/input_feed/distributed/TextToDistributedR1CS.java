package input_feed.distributed;

import algebra.curves.barreto_naehrig.bn254a.BN254aFields;
import algebra.fields.AbstractFieldElementExpanded;
import configuration.Configuration;
import org.apache.spark.api.java.JavaPairRDD;
import relations.objects.Assignment;
import relations.objects.LinearCombination;
import relations.objects.LinearTerm;
import relations.objects.R1CSConstraintsRDD;
import relations.r1cs.R1CSRelationRDD;
import scala.Tuple2;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;

public class TextToDistributedR1CS<FieldT extends AbstractFieldElementExpanded<FieldT>>
    extends abstractFileToDistributedR1CS {

    public TextToDistributedR1CS(final String _filePath, final Configuration _config) {
        super(_filePath, _config);
    }

    public R1CSRelationRDD loadR1CS(String fileName) {
        String[] constraintParameters = new String[3];
        try{
            constraintParameters = new BufferedReader(
                    new FileReader(this.filePath() + ".problem_size")).readLine().split(" ");

        } catch (Exception e){
            System.err.println("Error: " + e.getMessage());
        }
        int numInputs = Integer.parseInt(constraintParameters[0]);
        int numAuxiliary = Integer.parseInt(constraintParameters[1]);
        int numConstraints = Integer.parseInt(constraintParameters[2]);

        // Need at least one constraint per partition!
        assert(numConstraints >= this.config().numPartitions());

        final ArrayList<Integer> partitions = constructPartitionArray(this.config().numPartitions(), numConstraints);

        JavaPairRDD<Long, LinearTerm<FieldT>> linearCombinationA = distributedCombination(
                this.filePath() + ".a", partitions, numConstraints);

        JavaPairRDD<Long, LinearTerm<FieldT>> linearCombinationB = distributedCombination(
                this.filePath() + ".b", partitions, numConstraints);

        JavaPairRDD<Long, LinearTerm<FieldT>> linearCombinationC = distributedCombination(
                this.filePath() + ".c", partitions, numConstraints);

        linearCombinationA.count();
        linearCombinationB.count();
        linearCombinationC.count();

        final R1CSConstraintsRDD<FieldT> constraints = new R1CSConstraintsRDD<>(
                linearCombinationA,
                linearCombinationB,
                linearCombinationC,
                numConstraints);

        return new R1CSRelationRDD<>(constraints, numInputs, numAuxiliary);
    }

    @Override
    public Tuple2<Assignment, JavaPairRDD> loadWitness(String fileName) {
        // TODO - not urgent
        return null;
    }

    private <FieldT extends AbstractFieldElementExpanded<FieldT>>
    JavaPairRDD<Long, LinearTerm<FieldT>>
    distributedCombination(
            String fileName,
            ArrayList<Integer> partitions,
            int numConstraints
    ){
        final int numPartitions = this.config().numPartitions();

        // Need at least one constraint per partition!
        assert(numConstraints >= numPartitions);

        JavaPairRDD<Long, LinearTerm<FieldT>> result;
        try {
            final BufferedReader br = new BufferedReader(new FileReader(fileName));
            // Not gonna work. Buffered reader gets reopened for each partition. May need to partition the file.

            result = this.config().sparkContext().parallelize(partitions, numPartitions).flatMapToPair(part -> {
                final long partSize = part == numPartitions ? numConstraints %
                        (numConstraints / numPartitions) : numConstraints / numPartitions;

                final ArrayList<Tuple2<Long, LinearTerm<FieldT>>> T = new ArrayList<>();
                for (long i = 0; i < partSize; i++) {

                    final long index = part * (numConstraints / numPartitions) + i;

                    LinearCombination<FieldT> combinationA = makeRowAt(index, br);

                    for (LinearTerm term: combinationA.terms()) {
                        T.add(new Tuple2<>(index, term));
                    }

                }
                return T.iterator();
            });
            br.close();
            return result;
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
        return null;
    }

    private static <FieldT extends AbstractFieldElementExpanded<FieldT>>
    LinearCombination<FieldT>
    makeRowAt (long index, BufferedReader reader) {
        // Assumes input to be ordered by row and that the last line is blank.
        final LinearCombination<FieldT> L = new LinearCombination<>();

        try {
            String nextLine;
            while ((nextLine = reader.readLine()) != null) {
                String[] tokens = nextLine.split(" ");

                int col = Integer.parseInt(tokens[0]);
                int row = Integer.parseInt(tokens[1]);
                assert (row >= index);

                if (index == row) {
                    reader.mark(100);
                    L.add(new LinearTerm<>(col, (FieldT) new BN254aFields.BN254aFr(tokens[2])));
                } else if (row > index) {
                    reader.reset();
                    return L;
                }
            }
        } catch (Exception e){
            System.err.println("Error: " + e.getMessage());
        }
        return L;
    }
}
