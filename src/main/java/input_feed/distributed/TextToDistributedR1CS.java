package input_feed.distributed;

import algebra.fields.AbstractFieldElementExpanded;
import configuration.Configuration;
import org.apache.spark.api.java.JavaPairRDD;
import relations.objects.Assignment;
import relations.objects.LinearTerm;
import relations.objects.R1CSConstraintsRDD;
import relations.r1cs.R1CSRelationRDD;
import scala.Tuple2;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.*;

public class TextToDistributedR1CS<FieldT extends AbstractFieldElementExpanded<FieldT>> {
    private final String filePath;
    private final int numInputs;
    private final int numAuxiliary;
    private final int numConstraints;
    private final FieldT fieldParameters;
    private boolean negateCMatrix;

    public TextToDistributedR1CS(
            final String _filePath,
            final FieldT _fieldParameters,
            final boolean _negateCMatrix) {

        filePath = _filePath;
        fieldParameters = _fieldParameters;
        negateCMatrix = _negateCMatrix;

        String[] constraintParameters = new String[3];
        try{
            constraintParameters = new BufferedReader(
                    new FileReader(filePath + ".problem_size")).readLine().split(" ");
        } catch (Exception e){
            System.err.println("Error: " + e.getMessage());
        }
        numInputs = Integer.parseInt(constraintParameters[0]);
        numAuxiliary = Integer.parseInt(constraintParameters[1]);
        numConstraints = Integer.parseInt(constraintParameters[2]);
    }

    public TextToDistributedR1CS(final String _filePath, final FieldT _fieldParameters) {
        this(_filePath, _fieldParameters, false);
    }

    public R1CSRelationRDD<FieldT> loadR1CS(Configuration config) {

        // Need at least one constraint per partition!
        assert(numConstraints >= config.numPartitions());
        config.beginRuntime("Distribute Constraints");
        JavaPairRDD<Long, LinearTerm<FieldT>> linearCombinationA =
                distributedCombination(config, filePath + ".a", false);

        JavaPairRDD<Long, LinearTerm<FieldT>> linearCombinationB =
                distributedCombination(config, filePath + ".b", false);

        JavaPairRDD<Long, LinearTerm<FieldT>> linearCombinationC =
                distributedCombination(config, filePath + ".c", negateCMatrix);
        config.endRuntime("Distribute Constraints");
        final R1CSConstraintsRDD<FieldT> constraints = new R1CSConstraintsRDD<>(
                linearCombinationA,
                linearCombinationB,
                linearCombinationC,
                numConstraints);

        return new R1CSRelationRDD<>(constraints, numInputs, numAuxiliary);
    }

    public Tuple2<Assignment<FieldT>, JavaPairRDD<Long, FieldT>> loadWitness(Configuration config) {

        config.beginRuntime("Distribute Witness");
        config.beginLog("Distribute witness");

        FieldT field = fieldParameters;
        JavaPairRDD<Long, FieldT> auxAssignment = config.sparkContext()
                .textFile(filePath + ".aux").flatMapToPair(line -> {
            final ArrayList<Tuple2<Long, FieldT>> temp = new ArrayList<>();
            String[] splitAux = line.split("\\s+");
            for (int i=0; i < splitAux.length; i++){
                final FieldT value = field.construct(splitAux[i]);
                temp.add(new Tuple2<>((long) i, value));
            }
            return temp.iterator();
        });

        final int numAux = numAuxiliary;
        JavaPairRDD<Long, FieldT> primaryAssignment = config.sparkContext()
                .textFile(filePath + ".primary").flatMapToPair(line -> {
            final ArrayList<Tuple2<Long, FieldT>> temp = new ArrayList<>();
            String[] splitPrimary = line.split("\\s+");
            for (int i=0; i < splitPrimary.length; i++){
                final FieldT value = field.construct(splitPrimary[i]);
                temp.add(new Tuple2<>((long) numAux + i, value));
            }
            return temp.iterator();
        });

        JavaPairRDD<Long, FieldT> fullAssignment = auxAssignment.union(primaryAssignment);

        config.endLog("Distribute witness");
        config.endRuntime("Distribute Witness");

        final Assignment<FieldT> primary = new Assignment<>();
        for (long i=0; i < numInputs; i++){
            List<FieldT> element = fullAssignment.lookup(i);
            assert(element.size() == 1);
            primary.add(element.get(0));
        }
        return new Tuple2<>(primary, fullAssignment);
    }

    private JavaPairRDD<Long, LinearTerm<FieldT>>
    distributedCombination(Configuration config, String fileName, boolean negate) {
        config.beginLog("Distributing constraint matrix " + fileName);
        FieldT field = fieldParameters;
        JavaPairRDD<Long, LinearTerm<FieldT>> res =
                config.sparkContext().textFile(fileName).flatMapToPair(line -> {
            String[] tokens = line.split(" ");
            int col = Integer.parseInt(tokens[0]);
            long row = Long.parseLong(tokens[1]);
            FieldT value = field.construct(tokens[2]);
            if (negate) {
                value = value.negate();
            }
            return Collections.singleton(
                    new Tuple2<>(row, new LinearTerm<>(col, value))).iterator();
        });
        config.endLog("Distributing constraint matrix " + fileName);
        return res;
    }
}
