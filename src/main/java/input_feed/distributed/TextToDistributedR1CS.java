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
import java.util.Map;
import java.util.HashMap;

public class TextToDistributedR1CS<FieldT extends AbstractFieldElementExpanded<FieldT>>
    extends abstractFileToDistributedR1CS {

    public TextToDistributedR1CS(final String _filePath, final Configuration _config) {
        super(_filePath, _config);
    }

    public R1CSRelationRDD<FieldT> loadR1CS(String fileName) {
        String[] constraintParameters = new String[3];
        try{
            constraintParameters = new BufferedReader(
                    new FileReader(this.filePath() + fileName + ".problem_size")).readLine().split(" ");

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
                this.filePath() + fileName + ".a", partitions, numConstraints);
        JavaPairRDD<Long, LinearTerm<FieldT>> linearCombinationB = distributedCombination(
                this.filePath() + fileName + ".b", partitions, numConstraints);
        JavaPairRDD<Long, LinearTerm<FieldT>> linearCombinationC = distributedCombination(
                this.filePath() + fileName + ".c", partitions, numConstraints);

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
    public Tuple2<Assignment<FieldT>, JavaPairRDD<Long, FieldT>> loadWitness(String fileName) {

        final Assignment<FieldT> serialAssignment = new Assignment<>();
        int numInputs = -1;
        int numAuxiliary = -1;

        try{
            String[] constraintParameters = new BufferedReader(
                    new FileReader(this.filePath() + fileName + ".problem_size")).readLine().split(" ");

            numInputs = Integer.parseInt(constraintParameters[0]);
            numAuxiliary = Integer.parseInt(constraintParameters[1]);

            BufferedReader br = new BufferedReader(
                    new FileReader(this.filePath() + fileName + ".witness"));

            String nextLine;
            int count = 0;
            while ((nextLine = br.readLine()) != null) {
                final BN254aFields.BN254aFr value = new BN254aFields.BN254aFr(nextLine);
                serialAssignment.add((FieldT) value);
                count++;
            }
            assert (count == numInputs + numAuxiliary);
            br.close();
        } catch (Exception e){
            System.err.println("Error: " + e.getMessage());
        }


        int totalSize = numInputs + numAuxiliary;

        final int numExecutors = this.config().numExecutors();
        final ArrayList<Integer> assignmentPartitions = new ArrayList<>();
        for (int i = 0; i < numExecutors; i++) {
            assignmentPartitions.add(i);
        }
        if (totalSize % 2 != 0) {
            assignmentPartitions.add(numExecutors);
        }

        JavaPairRDD<Long, FieldT> distributedAssignment = this.config().sparkContext()
                .parallelize(assignmentPartitions, numExecutors).flatMapToPair(part -> {
                    final long startIndex = part * (totalSize / numExecutors);
                    final long partSize = part == numExecutors ?
                            totalSize % (totalSize / numExecutors) : totalSize / numExecutors;

                    final ArrayList<Tuple2<Long, FieldT>> assignment = new ArrayList<>();
                    for (long i = startIndex; i < startIndex + partSize; i++) {
                        assignment.add(new Tuple2<>(i, serialAssignment.get((int) i)));
                    }
                    return assignment.iterator();
                }).persist(this.config().storageLevel());


        final Assignment<FieldT> primary = new Assignment<>(serialAssignment.subList(0, numInputs));


        return new Tuple2<>(primary, distributedAssignment);
    }



    private JavaPairRDD<Long, LinearTerm<FieldT>>
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
            // TODO - this is not the best for performance
            Map<Integer, LinearCombination<FieldT>> constraintMap = serializeReader(br, numConstraints);

            result = this.config().sparkContext().parallelize(partitions, numPartitions).flatMapToPair(part -> {
                final long partSize = part == numPartitions ? numConstraints %
                        (numConstraints / numPartitions) : numConstraints / numPartitions;

                final ArrayList<Tuple2<Long, LinearTerm<FieldT>>> T = new ArrayList<>();
                for (long i = 0; i < partSize; i++) {

                    final long index = part * (numConstraints / numPartitions) + i;

                    if (constraintMap.containsKey((int) index)){
                        LinearCombination<FieldT> currRow = constraintMap.get((int) index);
                        for (LinearTerm term: currRow.terms()) {
                            T.add(new Tuple2<>(index, term));
                        }
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

    public <FieldT extends AbstractFieldElementExpanded<FieldT>> Map<Integer, LinearCombination<FieldT>>
    serializeReader(BufferedReader br, int numConstraints) {
        int index = 0;
//        ArrayList<LinearCombination<FieldT>> constraintList = new ArrayList<>();
        Map<Integer, LinearCombination<FieldT>> constraintMap = new HashMap<>();
        LinearCombination<FieldT> L = new LinearCombination<>();
        try {
            String nextLine;
            br.mark(100);
            while ((nextLine = br.readLine()) != null) {
                String[] tokens = nextLine.split(" ");
                int col = Integer.parseInt(tokens[0]);
                int row = Integer.parseInt(tokens[1]);
                BN254aFields.BN254aFr value = new BN254aFields.BN254aFr(tokens[2]);
                assert (row >= index);

                if (row == index) {
                    L.add(new LinearTerm<>(col, (FieldT) value));
                    br.mark(100);
                } else if (row > index) {
                    constraintMap.put(index, L);
//                    constraintList.add(index, L);
                    L = new LinearCombination<>();
                    br.reset();
                    index++;
                }
            }
        } catch (Exception e){
            System.err.println("Error: " + e.getMessage());
        }
        // In case the last few rows are zero.
//        while (index < numConstraints) {
//            constraintList.add(new LinearCombination<>());
//        }
        return constraintMap;
    }
}
