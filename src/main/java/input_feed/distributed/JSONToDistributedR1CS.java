package input_feed.distributed;

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

public class JSONToDistributedR1CS<FieldT extends AbstractFieldElementExpanded<FieldT>> {
    private final String filePath;
    private final int numInputs;
    private final int numAuxiliary;
    private final int numConstraints;
    private final FieldT fieldParameters;

    public JSONToDistributedR1CS(final String _filePath, final FieldT _fieldParameters) {
        filePath = _filePath;
        fieldParameters = _fieldParameters;

        JSONParser parser = new JSONParser();
        JSONObject jsonObject;
        JSONArray header = new JSONArray();

        try {
            Object obj = parser.parse(new FileReader(filePath));
            jsonObject = (JSONObject) obj;
            header = (JSONArray) jsonObject.get("header");
        } catch (Exception e) {
            e.printStackTrace();
        }

        numInputs = Integer.parseInt((String) header.get(0));
        numAuxiliary = Integer.parseInt((String) header.get(1));
        numConstraints = Integer.parseInt((String) header.get(2));
    }

    public R1CSRelationRDD<FieldT> loadR1CS(Configuration config) {

        JSONParser parser = new JSONParser();
        JSONObject jsonObject;
        JSONArray constraintList = new JSONArray();

        try {
            Object obj = parser.parse(new FileReader(filePath));

            jsonObject = (JSONObject) obj;
            constraintList = (JSONArray) jsonObject.get("constraints");

        } catch (Exception e) {
            e.printStackTrace();
        }

        assert (numConstraints == constraintList.size());

        JSONArray[] constraintArray = new JSONArray[numConstraints];
        for (int i = 0; i < numConstraints; i++) {
            constraintArray[i] = (JSONArray) constraintList.get(i);
        }

        final ArrayList<Integer> partitions =
                constructPartitionArray(config.numPartitions(), numConstraints);

        // Load Linear Combinations as RDD format
        JavaPairRDD<Long, LinearTerm<FieldT>> combinationA = distributedCombinationFromJSON(
                partitions, constraintArray, 0, numConstraints, config);

        JavaPairRDD<Long, LinearTerm<FieldT>> combinationB = distributedCombinationFromJSON(
                partitions, constraintArray, 1, numConstraints, config);

        JavaPairRDD<Long, LinearTerm<FieldT>> combinationC = distributedCombinationFromJSON(
                partitions, constraintArray, 2, numConstraints, config);

        combinationA.count();
        combinationB.count();
        combinationC.count();

        final R1CSConstraintsRDD<FieldT> constraints = new R1CSConstraintsRDD<>(
                combinationA, combinationB, combinationC, numConstraints);

        return new R1CSRelationRDD<>(constraints, numInputs, numAuxiliary);
    }

    public Tuple2<Assignment<FieldT>, JavaPairRDD<Long, FieldT>> loadWitness(Configuration config) {
        JSONParser parser = new JSONParser();
        JSONObject jsonObject;
        JSONArray primaryInputs = new JSONArray();
        JSONArray auxInputs = new JSONArray();

        try {
            Object obj = parser.parse(new FileReader(filePath));

            jsonObject = (JSONObject) obj;
            primaryInputs = (JSONArray) jsonObject.get("primary_input");
            auxInputs = (JSONArray) jsonObject.get("aux_input");

        } catch (Exception e) {
            e.printStackTrace();
        }

        // TODO - Serial assignment may not be necessary.
        final Assignment<FieldT> serialAssignment = new Assignment<>();
        for (Object input: primaryInputs) {
            final FieldT value = fieldParameters.construct((String) input);
            serialAssignment.add(value);
        }
        for (Object input: auxInputs) {
            final FieldT value = fieldParameters.construct((String) input);
            serialAssignment.add(value);
        }

        final long numVariables = primaryInputs.size() + auxInputs.size();
        final int numExecutors = config.numExecutors();
        final ArrayList<Integer> assignmentPartitions = constructPartitionArray(numExecutors, numVariables);

        JavaPairRDD<Long, FieldT> oneFullAssignment = config.sparkContext()
                .parallelize(assignmentPartitions, numExecutors).flatMapToPair(part -> {
                    final long startIndex = part * (numVariables / numExecutors);
                    final long partSize = part == numExecutors ? numVariables %
                            (numVariables / numExecutors) : numVariables / numExecutors;

                    final ArrayList<Tuple2<Long, FieldT>> assignment = new ArrayList<>();
                    for (long i = startIndex; i < startIndex + partSize; i++) {
                        assignment.add(new Tuple2<>(i, serialAssignment.get((int) i)));
                    }
                    return assignment.iterator();
                }).persist(config.storageLevel());

        final Assignment<FieldT> primary = new Assignment<>(
                serialAssignment.subList(0, primaryInputs.size()));

        oneFullAssignment.count();

        return new Tuple2<>(primary, oneFullAssignment);
    }

    private JavaPairRDD<Long, LinearTerm<FieldT>>
    distributedCombinationFromJSON(
            ArrayList<Integer> partitions,
            JSONArray[] constraintArray,
            int constraintArrayIndex,
            int numConstraints,
            Configuration config
    ){
        final int numPartitions = config.numPartitions();
        assert(numConstraints >= numPartitions);
        final FieldT fieldParams = fieldParameters;

        return config.sparkContext()
                .parallelize(partitions, numPartitions).flatMapToPair(part -> {
                    final long partSize = part == numPartitions ?
                            numConstraints % (numConstraints / numPartitions) : numConstraints / numPartitions;

                    final ArrayList<Tuple2<Long, LinearTerm<FieldT>>> T = new ArrayList<>();
                    for (long i = 0; i < partSize; i++) {
                        final long index = part * (numConstraints / numPartitions) + i;

                        JSONObject next = (JSONObject) constraintArray[(int) index].get(constraintArrayIndex);

                        for (Object keyObj: next.keySet()) {
                            String key = keyObj.toString();
                            long columnIndex = Long.parseLong(key);
                            FieldT value;
                            try{
                                value = fieldParams.construct((String) next.get(key));
                            } catch (ClassCastException e){
                                // Handle case when key-value pairs are String: Long
                                value = fieldParams.construct(Long.toString((long) next.get(key)));
                            }
                            T.add(new Tuple2<>(index, new LinearTerm<>(columnIndex, value)));
                        }
                    }
                    return T.iterator();
                });
    }

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
