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

public class TextToDistributedR1CS<FieldT extends AbstractFieldElementExpanded<FieldT>>
    extends AbstractFileToDistributedR1CS<FieldT> {
    final private int numInputs;
    final private int numAuxiliary;
    final private int numConstraints;

    public TextToDistributedR1CS(
            final String _filePath,
            final Configuration _config,
            final FieldT _fieldParameters) {
        super(_filePath, _config, _fieldParameters);

        String[] constraintParameters = new String[3];
        try{
            constraintParameters = new BufferedReader(
                    new FileReader(this.filePath() + ".problem_size")).readLine().split(" ");
        } catch (Exception e){
            System.err.println("Error: " + e.getMessage());
        }
        numInputs = Integer.parseInt(constraintParameters[0]);
        numAuxiliary = Integer.parseInt(constraintParameters[1]);
        numConstraints = Integer.parseInt(constraintParameters[2]);
    }

    public TextToDistributedR1CS(
            final String _filePath,
            final Configuration _config,
            final FieldT _fieldParameters,
            final boolean _negate) {

        super(_filePath, _config, _fieldParameters, _negate);

        String[] constraintParameters = new String[3];
        try{
            constraintParameters = new BufferedReader(
                    new FileReader(this.filePath() + ".problem_size")).readLine().split(" ");
        } catch (Exception e){
            System.err.println("Error: " + e.getMessage());
        }
        numInputs = Integer.parseInt(constraintParameters[0]);
        numAuxiliary = Integer.parseInt(constraintParameters[1]);
        numConstraints = Integer.parseInt(constraintParameters[2]);
    }

    public R1CSRelationRDD<FieldT> loadR1CS() {

        // Need at least one constraint per partition!
        assert(numConstraints >= this.config().numPartitions());

        this.config().beginLog("Constraint matrix A");
        JavaPairRDD<Long, LinearTerm<FieldT>>
                linearCombinationA = distributedCombination(this.filePath() + ".a", false);
        this.config().endLog("Constraint matrix A");
        this.config().beginLog("Constraint matrix B");
        JavaPairRDD<Long, LinearTerm<FieldT>>
                linearCombinationB = distributedCombination(this.filePath() + ".b", false);
        this.config().endLog("Constraint matrix B");

        this.config().beginLog("Constraint matrix C");
        JavaPairRDD<Long, LinearTerm<FieldT>>
                linearCombinationC = distributedCombination(this.filePath() + ".c", this.negate());
        this.config().endLog("Constraint matrix C");

        // doesn't appear to be necessary.
//        this.config().beginLog("Count combinations");
//        linearCombinationA.count();
//        linearCombinationB.count();
//        linearCombinationC.count();
//        this.config().endLog("Count combinations");

        final R1CSConstraintsRDD<FieldT> constraints = new R1CSConstraintsRDD<>(
                linearCombinationA,
                linearCombinationB,
                linearCombinationC,
                numConstraints);

        return new R1CSRelationRDD<>(constraints, numInputs, numAuxiliary);
    }

    @Override
    public Tuple2<Assignment<FieldT>, JavaPairRDD<Long, FieldT>> loadWitness() {

        this.config().beginRuntime("Distribute Witness");
        this.config().beginLog("Distribute witness");

        String filePath = "file://" + this.filePath();
//        String filePath = this.filePath();

        FieldT field = this.fieldParameters();
        JavaPairRDD<Long, FieldT> auxAssignment = this.config().sparkContext()
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
        JavaPairRDD<Long, FieldT> primaryAssignment = this.config().sparkContext()
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


        this.config().endLog("Distribute witness");
        this.config().endRuntime("Distribute Witness");

        final Assignment<FieldT> primary = new Assignment<>();

        for (long i=0; i < numInputs; i++){
            List<FieldT> element = fullAssignment.lookup(i);
            assert(element.size() == 1);
            primary.add(element.get(0));
        }

        return new Tuple2<>(primary, fullAssignment);
    }

    private JavaPairRDD<Long, LinearTerm<FieldT>>
    distributedCombination(String fileName, boolean negate) {
        FieldT field = this.fieldParameters();

        return this.config().sparkContext().textFile("file://" + fileName).flatMapToPair(line -> {
            String[] tokens = line.split(" ");
            int col = Integer.parseInt(tokens[0]);
            long row = Long.parseLong(tokens[1]);
            FieldT value = field.construct(tokens[2]);
            if (negate) {
                value = value.negate();
            }
            return Collections.singleton(new Tuple2<>(row, new LinearTerm<>(col, value))).iterator();
        });
    }
}
