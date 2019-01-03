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
    private boolean useFileSlash;
    private final int numInputs;
    private final int numAuxiliary;
    private final int numConstraints;
    private final FieldT fieldParameters;
    private boolean negateCMatrix;

    public TextToDistributedR1CS(
            final String _filePath,
            final FieldT _fieldParameters,
            final boolean _negateCMatrix,
            final boolean _useFileSlash) {

        filePath = _filePath;
        useFileSlash = _useFileSlash;
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
        this(_filePath, _fieldParameters, false, false);
    }

    public R1CSRelationRDD<FieldT> loadR1CS(Configuration config) {

        // Need at least one constraint per partition!
        assert(numConstraints >= config.numPartitions());
        config.beginRuntime("Distribute Constraints");
        JavaPairRDD<Long, LinearTerm<FieldT>> combinationA =
                distributedCombination(config, filePath + ".a", false);

        JavaPairRDD<Long, LinearTerm<FieldT>> combinationB =
                distributedCombination(config, filePath + ".b", false);

        JavaPairRDD<Long, LinearTerm<FieldT>> combinationC =
                distributedCombination(config, filePath + ".c", negateCMatrix);
        config.endRuntime("Distribute Constraints");

        config.beginRuntime("Count combinations");

        config.beginLog("Count A");
        combinationA.count();
        config.endLog("Count A");

        config.beginLog("Count B");
        combinationB.count();
        config.endLog("Count B");

        config.beginLog("Count C");
        combinationC.count();
        config.endLog("Count C");

        config.endRuntime("Count combinations");

        final R1CSConstraintsRDD<FieldT> constraints = new R1CSConstraintsRDD<>(
                combinationA,
                combinationB,
                combinationC,
                numConstraints
        );

        return new R1CSRelationRDD<>(constraints, numInputs, numAuxiliary);
    }

    public Tuple2<Assignment<FieldT>, JavaPairRDD<Long, FieldT>> loadWitness(Configuration config) {

        config.beginRuntime("Distribute Witness");
        config.beginLog("Distribute witness");

        FieldT field = fieldParameters;
        String path = filePath;
        if (useFileSlash) { path = "file://" + path; }

        final int numAux = numAuxiliary;
        JavaPairRDD<Long, FieldT> auxAssignment =
                config.sparkContext().textFile(path + ".aux").zipWithIndex().flatMapToPair(
                        line -> Collections.singleton(new Tuple2<>(line._2, field.construct(line._1))).iterator()
                ).persist(config.storageLevel());

        JavaPairRDD<Long, FieldT> primaryAssignment =
                config.sparkContext().textFile(path + ".public").zipWithIndex().flatMapToPair(
                        line -> Collections.singleton(new Tuple2<>(numAux + line._2, field.construct(line._1))).iterator()
        ).persist(config.storageLevel());

        JavaPairRDD<Long, FieldT> fullAssignment = auxAssignment.union(primaryAssignment);

        config.beginLog("Count assignment");
        fullAssignment.count();
        config.endLog("Count assignment");

        config.endLog("Distribute witness");
        config.endRuntime("Distribute Witness");

        config.beginLog("Serialize Primary");
        config.beginRuntime("Serialize Primary");
        final Assignment<FieldT> primary = new Assignment<>();
        for (long i=0; i < numInputs; i++){
            List<FieldT> element = fullAssignment.lookup(i);
            assert(element.size() == 1);
            primary.add(element.get(0));
        }
        config.endLog("Serialize Primary");
        config.endRuntime("Serialize Primary");

        return new Tuple2<>(primary, fullAssignment);
    }

    private JavaPairRDD<Long, LinearTerm<FieldT>>
    distributedCombination(Configuration config, String fileName, boolean negate) {
        FieldT field = fieldParameters;
        String path = fileName;
        if (useFileSlash) { path = "file://" + path; }

        config.beginLog("Distributing constraint matrix " + fileName);
        JavaPairRDD<Long, LinearTerm<FieldT>> res =
                config.sparkContext().textFile(path).flatMapToPair(line -> {
            String[] tokens = line.split(" ");
            int col = Integer.parseInt(tokens[0]);
            long row = Long.parseLong(tokens[1]);
            FieldT value = field.construct(tokens[2]);
            if (negate) { value = value.negate(); }
            return Collections.singleton(
                    new Tuple2<>(row, new LinearTerm<>(col, value))).iterator();
        }).persist(config.storageLevel());
        config.endLog("Distributing constraint matrix " + fileName);
        return res;
    }
}
