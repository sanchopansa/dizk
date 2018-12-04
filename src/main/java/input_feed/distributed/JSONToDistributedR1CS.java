package input_feed.distributed;

import algebra.curves.barreto_naehrig.bn254a.BN254aFields;
import algebra.fields.AbstractFieldElementExpanded;
import configuration.Configuration;
import org.apache.spark.api.java.JavaPairRDD;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import relations.objects.Assignment;
import relations.objects.LinearTerm;
import relations.objects.R1CSConstraintsRDD;
import relations.r1cs.R1CSRelationRDD;
import scala.Tuple2;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.Iterator;

public class JSONToDistributedR1CS<FieldT extends AbstractFieldElementExpanded<FieldT>>
        extends abstractFileToDistributedR1CS {

    public JSONToDistributedR1CS(final String _filePath, final Configuration _config) {
        super(_filePath, _config);
    }

    @Override
    public R1CSRelationRDD<FieldT> loadR1CS(String fileName) {

        JSONParser parser = new JSONParser();
        JSONObject jsonObject;
        JSONArray header = new JSONArray();
        JSONArray constraintList = new JSONArray();

        try {
            Object obj = parser.parse(new FileReader(this.filePath() + fileName));

            jsonObject = (JSONObject) obj;
            header = (JSONArray) jsonObject.get("header");
            constraintList = (JSONArray) jsonObject.get("constraints");

        } catch (Exception e) {
            e.printStackTrace();
        }

        int numInputs = Integer.parseInt((String) header.get(0));
        int numAuxiliary = Integer.parseInt((String) header.get(1));

        // TODO - do we really need to load this into an ArrayList?
        int numConstraints = constraintList.size();
        JSONArray[] constraintArray = new JSONArray[numConstraints];
        for (int i = 0; i < numConstraints; i++) {
            constraintArray[i] = (JSONArray) constraintList.get(i);
        }

        final ArrayList<Integer> partitions = constructPartitionArray(this.config().numPartitions(), numConstraints);

        // Load Linear Combinations as RDD format
        JavaPairRDD<Long, LinearTerm<FieldT>> combinationA = distributedCombinationFromJSON(
                partitions, constraintArray, 0, numConstraints);

        JavaPairRDD<Long, LinearTerm<FieldT>> combinationB = distributedCombinationFromJSON(
                partitions, constraintArray, 1, numConstraints);

        JavaPairRDD<Long, LinearTerm<FieldT>> combinationC = distributedCombinationFromJSON(
                partitions, constraintArray, 2, numConstraints);

        combinationA.count();
        combinationB.count();
        combinationC.count();

        final R1CSConstraintsRDD<FieldT> constraints = new R1CSConstraintsRDD<>(
                combinationA,
                combinationB,
                combinationC,
                numConstraints);

        final R1CSRelationRDD<FieldT> r1cs = new R1CSRelationRDD<>(
                constraints,
                numInputs,
                numAuxiliary);

        assert (r1cs.isValid());

        return r1cs;
    }

    @Override
    public Tuple2<Assignment<FieldT>, JavaPairRDD<Long, FieldT>> loadWitness(String fileName) {
        JSONParser parser = new JSONParser();
        JSONObject jsonObject;
        JSONArray primaryInputs = new JSONArray();
        JSONArray auxInputs = new JSONArray();

        try {
            Object obj = parser.parse(new FileReader(this.filePath() + fileName));

            jsonObject = (JSONObject) obj;
            primaryInputs = (JSONArray) jsonObject.get("primary_input");
            auxInputs = (JSONArray) jsonObject.get("aux_input");

        } catch (Exception e) {
            e.printStackTrace();
        }

        // Serial assignment may not be necessary.
        final Assignment<FieldT> serialAssignment = new Assignment<>();
        int numInputs = primaryInputs.size();
        for (int i = 0; i < numInputs; i++) {
            final BN254aFields.BN254aFr value = new BN254aFields.BN254aFr((String) primaryInputs.get(i));
            serialAssignment.add((FieldT) value);
        }
        int numAuxiliary = auxInputs.size();
        for (int i = 0; i < numAuxiliary; i++) {
            final BN254aFields.BN254aFr value = new BN254aFields.BN254aFr((String) auxInputs.get(i));
            serialAssignment.add((FieldT) value);
        }

        final long numVariables = numInputs + numAuxiliary;
//        final int numPartitions = config.numPartitions();
//        final ArrayList<Integer> variablePartitions = constructPartitionArray(config.numPartitions(), numVariables);


        final int numExecutors = this.config().numExecutors();
        final ArrayList<Integer> assignmentPartitions = constructPartitionArray(numExecutors, numVariables);

        JavaPairRDD<Long, FieldT> oneFullAssignment = this.config().sparkContext()
                .parallelize(assignmentPartitions, numExecutors).flatMapToPair(part -> {
                    final long startIndex = part * (numVariables / numExecutors);
                    final long partSize = part == numExecutors ? numVariables %
                            (numVariables / numExecutors) : numVariables / numExecutors;

                    final ArrayList<Tuple2<Long, FieldT>> assignment = new ArrayList<>();
                    for (long i = startIndex; i < startIndex + partSize; i++) {
                        assignment.add(new Tuple2<>(i, serialAssignment.get((int) i)));
                    }
                    return assignment.iterator();
                }).persist(this.config().storageLevel());

        final Assignment<FieldT> primary = new Assignment<>(serialAssignment.subList(0, numInputs));

        oneFullAssignment.count();

        return new Tuple2<>(primary, oneFullAssignment);
    }

    private JavaPairRDD<Long, LinearTerm<FieldT>>
    distributedCombinationFromJSON(
            ArrayList<Integer> partitions,
            JSONArray[] constraintArray,
            int constraintArrayIndex,
            int numConstraints
    ){
        final int numPartitions = this.config().numPartitions();
        assert(numConstraints >= numPartitions);

        return this.config().sparkContext()
                .parallelize(partitions, numPartitions).flatMapToPair(part -> {
                    final long partSize = part == numPartitions ?
                            numConstraints % (numConstraints / numPartitions) : numConstraints / numPartitions;

                    final ArrayList<Tuple2<Long, LinearTerm<FieldT>>> T = new ArrayList<>();
                    for (long i = 0; i < partSize; i++) {
                        final long index = part * (numConstraints / numPartitions) + i;

                        JSONObject next = (JSONObject) constraintArray[(int) index].get(constraintArrayIndex);
                        Iterator<String> keys = next.keySet().iterator();
                        while (keys.hasNext()) {
                            String key = keys.next();

                            long columnIndex = Long.parseLong(key);
                            BN254aFields.BN254aFr value;
                            try{
                                value = new BN254aFields.BN254aFr((String) next.get(key));
                            } catch (ClassCastException e){
                                // Handle case when key-value pairs are String: Long
                                value = new BN254aFields.BN254aFr(Long.toString((long) next.get(key)));
                            }
                            T.add(new Tuple2<>(index, new LinearTerm<>(columnIndex, (FieldT) value)));
                        }

                    }
                    return T.iterator();
                });
    }
}
