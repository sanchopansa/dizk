package input_feed.serial;

import algebra.curves.barreto_naehrig.bn254a.BN254aFields;
import algebra.fields.AbstractFieldElementExpanded;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import relations.objects.*;
import relations.r1cs.R1CSRelation;
import scala.Tuple2;

import java.io.FileReader;
import java.util.Iterator;

public class JSONToSerialR1CS<FieldT extends AbstractFieldElementExpanded<FieldT>> extends abstractFileToSerialR1CS {

    public JSONToSerialR1CS(String _filePath) {
        super(_filePath);
    }

    @Override
    public R1CSRelation<FieldT> loadR1CS(String fileName) {
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

        int numConstraints = constraintList.size();
        JSONArray[] constraintArray = new JSONArray[numConstraints];
        for (int i = 0; i < numConstraints; i++) {
            constraintArray[i] = (JSONArray) constraintList.get(i);
        }

        final R1CSConstraints<FieldT> constraints = new R1CSConstraints<>();

        for (int i = 0; i < constraintArray.length; i++) {
            final LinearCombination<FieldT> A = serialCombinationFromJSON(
                    (JSONObject) constraintArray[i].get(0));
            final LinearCombination<FieldT> B = serialCombinationFromJSON(
                    (JSONObject) constraintArray[i].get(1));
            final LinearCombination<FieldT> C = serialCombinationFromJSON(
                    (JSONObject) constraintArray[i].get(2));

            constraints.add(new R1CSConstraint<>(A, B, C));
        }

        final R1CSRelation<FieldT> r1cs = new R1CSRelation<>(constraints, numInputs, numAuxiliary);

        assert (r1cs.isValid());

        return r1cs;
    }

    public Tuple2<Assignment<FieldT>, Assignment<FieldT>> loadWitness (String fileName) {
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

        final Assignment<FieldT> primary = new Assignment<>(oneFullAssignment.subList(0, numInputs));
        final Assignment<FieldT> auxiliary = new Assignment<>(oneFullAssignment
                .subList(numInputs, oneFullAssignment.size()));

        return new Tuple2<>(primary, auxiliary);
    }

    private static <FieldT extends AbstractFieldElementExpanded<FieldT>>
    LinearCombination<FieldT>
    serialCombinationFromJSON (final JSONObject matrixRow) {
        final LinearCombination<FieldT> L = new LinearCombination<>();

        Iterator<String> keys = matrixRow.keySet().iterator();
        while (keys.hasNext()) {
            String key = keys.next();
            // TODO - don't hardcode the field!
            BN254aFields.BN254aFr value;
            try{
                value = new BN254aFields.BN254aFr((String) matrixRow.get(key));
            } catch (ClassCastException e){
                value = new BN254aFields.BN254aFr(Long.toString((long) matrixRow.get(key)));
            }
            L.add(new LinearTerm<>(Long.parseLong(key), (FieldT) value));
        }
        return L;
    }
}

