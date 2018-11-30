package relations.r1cs;

import algebra.curves.barreto_naehrig.bn254a.BN254aFields;
import algebra.fields.AbstractFieldElementExpanded;
import configuration.Configuration;
import org.apache.spark.api.java.JavaPairRDD;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import relations.objects.*;
import scala.Tuple2;
import scala.Tuple3;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;


public class FileToR1CS {

    public static <FieldT extends AbstractFieldElementExpanded<FieldT>>
        Tuple3<R1CSRelation<FieldT>, Assignment<FieldT>, Assignment<FieldT>>
    serialR1CSFromJSON(String filePath) {
        JSONParser parser = new JSONParser();
        JSONObject jsonObject;
        JSONArray primaryInputs = new JSONArray();
        JSONArray auxInputs = new JSONArray();
        JSONArray constraintList = new JSONArray();

        try {
            Object obj = parser.parse(new FileReader(filePath));

            jsonObject = (JSONObject) obj;
            primaryInputs = (JSONArray) jsonObject.get("primary_input");
            auxInputs = (JSONArray) jsonObject.get("aux_input");
            constraintList = (JSONArray) jsonObject.get("constraints");

        } catch (Exception e) {
            e.printStackTrace();
        }

        final Assignment<FieldT> oneFullAssignment = new Assignment<>();

        int numInputs = primaryInputs.size();
        for (int i = 0; i < numInputs; i++) {
            final BN254aFields.BN254aFr value = new BN254aFields.BN254aFr((String) primaryInputs.get(i));
            oneFullAssignment.add((FieldT) value);
        }

        int numAuxiliary = auxInputs.size();
        for (int i = 0; i < numAuxiliary; i++) {
            final BN254aFields.BN254aFr value = new BN254aFields.BN254aFr((String) auxInputs.get(i));
            oneFullAssignment.add((FieldT) value);
        }

        int numConstraints = constraintList.size();
        JSONArray[] constraintArray = new JSONArray[numConstraints];
        for (int i = 0; i < numConstraints; i++) {
            constraintArray[i] = (JSONArray) constraintList.get(i);
        }

        final R1CSConstraints<FieldT> constraints = new R1CSConstraints<>();

        for (int i = 0; i < constraintArray.length; i++) {

            JSONObject nextA = (JSONObject) constraintArray[i].get(0);
            JSONObject nextB = (JSONObject) constraintArray[i].get(1);
            JSONObject nextC = (JSONObject) constraintArray[i].get(2);


            final LinearCombination<FieldT> A = JSONObjectToCombination(nextA);
            final LinearCombination<FieldT> B = JSONObjectToCombination(nextB);
            final LinearCombination<FieldT> C = JSONObjectToCombination(nextC);

            constraints.add(new R1CSConstraint<>(A, B, C));
        }

        final R1CSRelation<FieldT> r1cs = new R1CSRelation<>(constraints, numInputs, numAuxiliary);
        final Assignment<FieldT> primary = new Assignment<>(oneFullAssignment.subList(0, numInputs));
        final Assignment<FieldT> auxiliary = new Assignment<>(oneFullAssignment
                .subList(numInputs, oneFullAssignment.size()));

//        assert (r1cs.numInputs() == numInputs);
//        assert (r1cs.numVariables() >= numInputs);
//        assert (r1cs.numVariables() == oneFullAssignment.size());
//        assert (r1cs.numConstraints() == numConstraints);
//        assert (r1cs.isSatisfied(primary, auxiliary));

        return new Tuple3<>(r1cs, primary, auxiliary);
    }

    public static <FieldT extends AbstractFieldElementExpanded<FieldT>>
        R1CSRelation<FieldT>
    serialR1CSFromPlainText(String filePath) {

        final R1CSConstraints<FieldT> constraints = new R1CSConstraints<>();

        int numInputs = -1;
        int numAuxiliary = -1;

        try{
            String[] constraintParameters = new BufferedReader(
                    new FileReader(filePath + "cropped_hash_problem_size")).readLine().split(" ");

            numInputs = Integer.parseInt(constraintParameters[1]);
            numAuxiliary = Integer.parseInt(constraintParameters[2]);

            int numConstraints = Integer.parseInt(constraintParameters[2]);

            BufferedReader brA = new BufferedReader(new FileReader(filePath + "cropped_hash_a"));
            BufferedReader brB = new BufferedReader(new FileReader(filePath + "cropped_hash_b"));
            BufferedReader brC = new BufferedReader(new FileReader(filePath + "cropped_hash_b"));

            for (int currRow = 0; currRow < numConstraints; currRow++){
                //  Start a fresh row for each of A, B, C
                Tuple2<LinearCombination<FieldT>, BufferedReader> resA = makeRowAt(currRow, brA);
                final LinearCombination<FieldT> A = resA._1();
                brA = resA._2();

                Tuple2<LinearCombination<FieldT>, BufferedReader> resB = makeRowAt(currRow, brB);
                final LinearCombination<FieldT> B = resB._1();
                brB = resB._2();

                Tuple2<LinearCombination<FieldT>, BufferedReader> resC = makeRowAt(currRow, brC);
                final LinearCombination<FieldT> C = resC._1();
                brC = resC._2();

                constraints.add(new R1CSConstraint<>(A, B, C));
            }

            brA.close();
            brB.close();
            brC.close();

        } catch (Exception e){
            System.err.println("Error: " + e.getMessage());
        }

        return new R1CSRelation<>(constraints, numInputs, numAuxiliary);
    }

    public static <FieldT extends AbstractFieldElementExpanded<FieldT>>
        Tuple3<R1CSRelationRDD<FieldT>, Assignment<FieldT>, JavaPairRDD<Long, FieldT>>
    distributedR1CSFromJSON(
            final String filePath,
            final Configuration config) {

        JSONParser parser = new JSONParser();
        JSONObject jsonObject;
        JSONArray primaryInputs = new JSONArray();
        JSONArray auxInputs = new JSONArray();
        JSONArray constraintList = new JSONArray();

        try {
            Object obj = parser.parse(new FileReader(filePath));

            jsonObject = (JSONObject) obj;
            primaryInputs = (JSONArray) jsonObject.get("primary_input");
            auxInputs = (JSONArray) jsonObject.get("aux_input");
            constraintList = (JSONArray) jsonObject.get("constraints");

        } catch (Exception e) {
            e.printStackTrace();
        }

        // TODO - do we really need to load this into an ArrayList?
        int numConstraints = constraintList.size();
        JSONArray[] constraintArray = new JSONArray[numConstraints];
        for (int i = 0; i < numConstraints; i++) {
            constraintArray[i] = (JSONArray) constraintList.get(i);
        }

        // instantiate partition array
        final int numPartitions = config.numPartitions();
        final ArrayList<Integer> partitions = new ArrayList<>();
        for (int i = 0; i < numPartitions; i++) {
            partitions.add(i);
        }
        if (numConstraints % 2 != 0) {
            partitions.add(numPartitions);
        }

        // Make combinations A, B and C. TODO - remove duplicate code
        JavaPairRDD<Long, LinearTerm<FieldT>> linearCombinationA = config.sparkContext()
                .parallelize(partitions, numPartitions).flatMapToPair(part -> {
                    final long partSize = part == numPartitions ?
                            numConstraints % (numConstraints / numPartitions) : numConstraints / numPartitions;

                    final ArrayList<Tuple2<Long, LinearTerm<FieldT>>> A = new ArrayList<>();
                    for (long i = 0; i < partSize; i++) {
                        final long index = part * (numConstraints / numPartitions) + i;

                        JSONObject nextA = (JSONObject) constraintArray[(int) index].get(0);
                        Iterator<String> keys = nextA.keySet().iterator();
                        while (keys.hasNext()) {
                            String key = keys.next();

                            long columnIndex = Long.parseLong(key);
                            final BN254aFields.BN254aFr value = new BN254aFields.BN254aFr(Long.toString((long) nextA.get(key)));

                            A.add(new Tuple2<>(index, new LinearTerm<>(columnIndex, (FieldT) value)));
                        }

                    }
                    return A.iterator();
                });
        JavaPairRDD<Long, LinearTerm<FieldT>> linearCombinationB = config.sparkContext()
                .parallelize(partitions, numPartitions).flatMapToPair(part -> {
                    final long partSize = part == numPartitions ?
                            numConstraints % (numConstraints / numPartitions) : numConstraints / numPartitions;

                    final ArrayList<Tuple2<Long, LinearTerm<FieldT>>> B = new ArrayList<>();
                    for (long i = 0; i < partSize; i++) {
                        final long index = part * (numConstraints / numPartitions) + i;

                        JSONObject nextB = (JSONObject) constraintArray[(int) index].get(1);
                        Iterator<String> keys = nextB.keySet().iterator();
                        while (keys.hasNext()) {
                            String key = keys.next();

                            long columnIndex = Long.parseLong(key);
                            final BN254aFields.BN254aFr value = new BN254aFields.BN254aFr(Long.toString((long) nextB.get(key)));

                            B.add(new Tuple2<>(index, new LinearTerm<>(columnIndex, (FieldT) value)));
                        }

                    }
                    return B.iterator();
                });
        JavaPairRDD<Long, LinearTerm<FieldT>> linearCombinationC = config.sparkContext()
                .parallelize(partitions, numPartitions).flatMapToPair(part -> {
                    final long partSize = part == numPartitions ?
                            numConstraints % (numConstraints / numPartitions) : numConstraints / numPartitions;

                    final ArrayList<Tuple2<Long, LinearTerm<FieldT>>> C = new ArrayList<>();
                    for (long i = 0; i < partSize; i++) {
                        final long index = part * (numConstraints / numPartitions) + i;

                        JSONObject nextC = (JSONObject) constraintArray[(int) index].get(2);
                        Iterator<String> keys = nextC.keySet().iterator();
                        while (keys.hasNext()) {
                            String key = keys.next();

                            long columnIndex = Long.parseLong(key);
                            final BN254aFields.BN254aFr value = new BN254aFields.BN254aFr(Long.toString((long) nextC.get(key)));
                            C.add(new Tuple2<>(index, new LinearTerm<>(columnIndex, (FieldT) value)));
                        }

                    }
                    return C.iterator();
                });

        // Make a function for this if it is necessary.
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

        final ArrayList<Integer> variablePartitions = new ArrayList<>();
        for (int i = 0; i < numPartitions; i++) {
            variablePartitions.add(i);
        }
        if (numVariables % 2 != 0) {
            variablePartitions.add(numPartitions);
        }

        final int numExecutors = config.numExecutors();
        final ArrayList<Integer> assignmentPartitions = new ArrayList<>();
        for (int i = 0; i < numExecutors; i++) {
            assignmentPartitions.add(i);
        }
        if (numVariables % 2 != 0) {
            assignmentPartitions.add(numExecutors);
        }

        JavaPairRDD<Long, FieldT> oneFullAssignment = config.sparkContext()
                .parallelize(assignmentPartitions, numExecutors).flatMapToPair(part -> {
                    final ArrayList<Tuple2<Long, FieldT>> assignment = new ArrayList<>();


                    final long startIndex = part * (numVariables / numExecutors);
                    final long partSize = part == numExecutors ? numVariables %
                            (numVariables / numExecutors) : numVariables / numExecutors;

                    for (long i = startIndex; i < startIndex + partSize; i++) {
                        assignment.add(new Tuple2<>(i, serialAssignment.get((int) i)));
                    }
                    return assignment.iterator();

                }).persist(config.storageLevel());


        final Assignment<FieldT> primary = new Assignment<>(serialAssignment.subList(0, numInputs));

        final long oneFullAssignmentSize = oneFullAssignment.count();
        linearCombinationA.count();
        linearCombinationB.count();
        linearCombinationC.count();

        final R1CSConstraintsRDD<FieldT> constraints = new R1CSConstraintsRDD<>(
                linearCombinationA,
                linearCombinationB,
                linearCombinationC,
                numConstraints);

        final R1CSRelationRDD<FieldT> r1cs = new R1CSRelationRDD<>(
                constraints,
                numInputs,
                numAuxiliary);

        assert (r1cs.numInputs() == numInputs);
        assert (r1cs.numVariables() >= numInputs);
        assert (r1cs.numVariables() == oneFullAssignmentSize);
        assert (r1cs.numConstraints() == numConstraints);
        assert (r1cs.isSatisfied(primary, oneFullAssignment));

        return new Tuple3<>(r1cs, primary, oneFullAssignment);
    }

//    public static <FieldT extends AbstractFieldElementExpanded<FieldT>>
//        R1CSRelation<FieldT>
//    distributedR1CSFromPlainText(String filePath) {
//
//        return new R1CSRelationRDD<>(constraints, numInputs, numAuxiliary);
//    }

    private static <FieldT extends AbstractFieldElementExpanded<FieldT>>
        LinearCombination<FieldT>
    JSONObjectToCombination (final JSONObject matrixRow) {

        final LinearCombination<FieldT> L = new LinearCombination<>();

        Iterator<String> keys = matrixRow.keySet().iterator();
        while (keys.hasNext()) {
            String key = keys.next();
            BN254aFields.BN254aFr value;
            try{
                value = new BN254aFields.BN254aFr((String) matrixRow.get(key));
            } catch (ClassCastException e){
                // Handle case when key-value pairs are String: Long
                value = new BN254aFields.BN254aFr(Long.toString((long) matrixRow.get(key)));
            }
            L.add(new LinearTerm<>(Long.parseLong(key), (FieldT) value));
        }
        return L;
    }

    private static <FieldT extends AbstractFieldElementExpanded<FieldT>>
        Tuple2<LinearCombination<FieldT>, BufferedReader>
    makeRowAt (int index, BufferedReader reader) {
        // Assumes input to be ordered by row and that the last line is blank.
        final LinearCombination<FieldT> L = new LinearCombination<>();

        try {
            String nextLine;
            reader.mark(1);  // TODO - this is very strange.
            while ((nextLine = reader.readLine()) != null) {
                String[] tokens = nextLine.split(" ");

                int col = Integer.parseInt(tokens[0]);
                int row = Integer.parseInt(tokens[1]);
                final BN254aFields.BN254aFr value = new BN254aFields.BN254aFr(tokens[2]);

                if (row == index) {
                    L.add(new LinearTerm<>(col, (FieldT) value));
                } else if (row > index) {
                    reader.reset();
                    return new Tuple2<>(L, reader);
                }
            }
        } catch (Exception e){
            System.err.println("Error: " + e.getMessage());
        }
        return new Tuple2<>(L, reader);
    }
}
