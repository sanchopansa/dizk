package relations.r1cs;

import algebra.curves.barreto_naehrig.bn254a.BN254aFields;
import algebra.fields.AbstractFieldElementExpanded;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import relations.objects.*;
import scala.Tuple3;

import java.io.*;
import java.util.Arrays;
import java.util.Iterator;


public class FileToR1CS {

    public static void main(String[] args) {

    }

    private static <FieldT extends AbstractFieldElementExpanded<FieldT>>
        LinearCombination<FieldT>
    JSONObjectToCombination (
            final JSONObject matrixRow,
            final FieldT fieldFactory) {

        final LinearCombination<FieldT> L = new LinearCombination<>();

        Iterator<String> keys = matrixRow.keySet().iterator();
        while (keys.hasNext()) {
            String key = keys.next();

            long columnIndex = Long.parseLong(key);
            long value = (long) matrixRow.get(key);

            L.add(new LinearTerm<>(columnIndex, fieldFactory.construct(value)));
        }
        return L;
    }

    public static <FieldT extends AbstractFieldElementExpanded<FieldT>>
        Tuple3<R1CSRelation<FieldT>, Assignment<FieldT>, Assignment<FieldT>>
    R1CSFromJSON(final FieldT fieldFactory, String filePath) {
        JSONParser parser = new JSONParser();
        JSONObject jsonObject;
        JSONArray primaryInputs = new JSONArray();
        JSONArray auxInputs = new JSONArray();
        JSONArray constraintList = new JSONArray();

        try {
            // TODO - pass string to input file.
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


            final LinearCombination<FieldT> A = JSONObjectToCombination(nextA, fieldFactory);
            final LinearCombination<FieldT> B = JSONObjectToCombination(nextB, fieldFactory);
            final LinearCombination<FieldT> C = JSONObjectToCombination(nextC, fieldFactory);

            constraints.add(new R1CSConstraint<>(A, B, C));
        }

        final R1CSRelation<FieldT> r1cs = new R1CSRelation<>(constraints, numInputs, numAuxiliary);
        final Assignment<FieldT> primary = new Assignment<>(oneFullAssignment.subList(0, numInputs));
        final Assignment<FieldT> auxiliary = new Assignment<>(oneFullAssignment
                .subList(numInputs, oneFullAssignment.size()));

        assert (r1cs.numInputs() == numInputs);
        assert (r1cs.numVariables() >= numInputs);
        assert (r1cs.numVariables() == oneFullAssignment.size());
        assert (r1cs.numConstraints() == numConstraints);
        assert (r1cs.isSatisfied(primary, auxiliary));

        return new Tuple3<>(r1cs, primary, auxiliary);
    }

    public static <FieldT extends AbstractFieldElementExpanded<FieldT>>
        Tuple3<R1CSRelation<FieldT>, Assignment<FieldT>, Assignment<FieldT>>
    R1CSFromPlainText(final FieldT fieldFactory) {

        final R1CSConstraints<FieldT> constraints = new R1CSConstraints<>();

        final LinearCombination<FieldT> A = new LinearCombination<>();
        final LinearCombination<FieldT> B = new LinearCombination<>();
        final LinearCombination<FieldT> C = new LinearCombination<>();

        final FieldT one = fieldFactory.one();
        final FieldT zero = fieldFactory.zero();

        try{
            FileInputStream fileStreamA = new FileInputStream("src/data/r1cs.medium_a");
            FileInputStream fileStreamB = new FileInputStream("src/data/r1cs.medium_b");
            FileInputStream fileStreamC = new FileInputStream("src/data/r1cs.medium_c");

            DataInputStream inA = new DataInputStream(fileStreamA);
            DataInputStream inB = new DataInputStream(fileStreamB);
            DataInputStream inC = new DataInputStream(fileStreamC);

            BufferedReader brA = new BufferedReader(new InputStreamReader(inA));
            BufferedReader brB = new BufferedReader(new InputStreamReader(inB));
            BufferedReader brC = new BufferedReader(new InputStreamReader(inC));


            // TODO - This construction is wrong. It puts all the elements into the first row.
            String strA;
            while ((strA = brA.readLine()) != null)   {
                String[] tokens = strA.split(" ");
                int colIndex = Integer.parseInt(tokens[0]);
                int rowIndex = Integer.parseInt(tokens[1]);
                long value = Long.parseLong(tokens[2]);

                // TODO - should be able to extend ArrayList better than this. Or just insert wherever.
                while (A.size() < rowIndex) {
                    A.add(new LinearTerm<>((long) colIndex, zero));
                }
                A.add(rowIndex, new LinearTerm<>((long) colIndex, fieldFactory.construct(value)));

            }
            inA.close();

            String strB;
            while ((strB = brB.readLine()) != null)   {
                String[] tokens = strB.split(" ");
                int colIndex = Integer.parseInt(tokens[0]);
                int rowIndex = Integer.parseInt(tokens[1]);
                long value = Long.parseLong(tokens[2]);

                while (B.size() < rowIndex) {
                    B.add(new LinearTerm<>((long) colIndex, zero));
                }
                B.add(rowIndex, new LinearTerm<>((long) colIndex, fieldFactory.construct(value)));

            }
            inB.close();

            String strC;
            while ((strC = brC.readLine()) != null)   {
                String[] tokens = strC.split(" ");
                int colIndex = Integer.parseInt(tokens[0]);
                int rowIndex = Integer.parseInt(tokens[1]);
                long value = Long.parseLong(tokens[2]);

                while (C.size() < rowIndex) {
                    C.add(new LinearTerm<>((long) colIndex, zero));
                }
                C.add(rowIndex, new LinearTerm<>((long) colIndex, fieldFactory.construct(value)));

            }
            inC.close();

            constraints.add(new R1CSConstraint<>(A, B, C));

        } catch (Exception e){
            System.err.println("Error: " + e.getMessage());
        }

        final Assignment<FieldT> oneFullAssignment = new Assignment<>(Arrays.asList(one, zero, zero));

        final int numConstraints = 1; // TODO - set equal to max size of A, B, C.
        final int numInputs = 1;  // TODO - should be specified somewhere as k...
        final int numAuxiliary = 2 + numConstraints - numInputs;  // TODO - Should be max columnIndex + 1 - numInputs.

        final R1CSRelation<FieldT> r1cs = new R1CSRelation<>(constraints, numInputs, numAuxiliary);
        final Assignment<FieldT> primary = new Assignment<>(oneFullAssignment.subList(0, numInputs));
        final Assignment<FieldT> auxiliary = new Assignment<>(oneFullAssignment.subList(numInputs, oneFullAssignment.size()));

        return new Tuple3<>(r1cs, primary, auxiliary);
    }
}
