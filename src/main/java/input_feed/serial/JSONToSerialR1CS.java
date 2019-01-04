package input_feed.serial;

import algebra.fields.AbstractFieldElementExpanded;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import relations.objects.*;
import relations.r1cs.R1CSRelation;
import scala.Tuple2;

import java.io.FileReader;
import java.io.IOException;

public class JSONToSerialR1CS<FieldT extends AbstractFieldElementExpanded<FieldT>> {
    private final int numInputs;
    private final int numAuxiliary;
    private final int numConstraints;
    private final FieldT fieldParameters;
    private final JSONArray constraintList;
    private final JSONArray primaryInputs;
    private final JSONArray auxInputs;

    public JSONToSerialR1CS(
            final String _filePath,
            final FieldT _fieldParameters) throws IOException, ParseException

    {
        fieldParameters = _fieldParameters;

        JSONParser parser = new JSONParser();
        JSONObject jsonObject;
        Object obj = parser.parse(new FileReader(_filePath));
        jsonObject = (JSONObject) obj;

        constraintList = (JSONArray) jsonObject.get("constraints");
        primaryInputs = (JSONArray) jsonObject.get("primary_input");
        auxInputs = (JSONArray) jsonObject.get("aux_input");

        JSONArray header = (JSONArray) jsonObject.get("header");
        numInputs = Integer.parseInt((String) header.get(0));
        numAuxiliary = Integer.parseInt((String) header.get(1));
        numConstraints = Integer.parseInt((String) header.get(2));

        assert (numConstraints == constraintList.size());
    }

    public R1CSRelation<FieldT> loadR1CS() {
        JSONArray[] constraintArray = new JSONArray[numConstraints];
        for (int i = 0; i < numConstraints; i++) {
            constraintArray[i] = (JSONArray) constraintList.get(i);
        }

        final R1CSConstraints<FieldT> constraints = new R1CSConstraints<>();

        for (int i = 0; i < constraintArray.length; i++) {
            final LinearCombination<FieldT>
                    A = serialCombinationFromJSON((JSONObject) constraintArray[i].get(0), false);
            final LinearCombination<FieldT>
                    B = serialCombinationFromJSON((JSONObject) constraintArray[i].get(1), false);
            final LinearCombination<FieldT>
                    C = serialCombinationFromJSON((JSONObject) constraintArray[i].get(2), false);

            constraints.add(new R1CSConstraint<>(A, B, C));
        }

        return new R1CSRelation<>(constraints, numInputs, numAuxiliary);
    }

    public Tuple2<Assignment<FieldT>, Assignment<FieldT>> loadWitness () {
        final Assignment<FieldT> primary = new Assignment<>();
        for (Object element: primaryInputs) {
            final FieldT value = fieldParameters.construct((String) element);
            primary.add(value);
        }

        final Assignment<FieldT> auxiliary = new Assignment<>();
        for (Object element: auxInputs) {
            final FieldT value = fieldParameters.construct((String) element);
            auxiliary.add(value);
        }

        return new Tuple2<>(primary, auxiliary);
    }

    private LinearCombination<FieldT> serialCombinationFromJSON (final JSONObject matrixRow, final boolean negate) {
        final LinearCombination<FieldT> combination = new LinearCombination<>();
        for (Object keyObj: matrixRow.keySet()) {
            String key = (String) keyObj;
            FieldT value;
            try{
                value = fieldParameters.construct((String) matrixRow.get(key));
            } catch (ClassCastException e){
                value = fieldParameters.construct(Long.toString((long) matrixRow.get(key)));
            }
            if (negate) {
                value = value.negate();
            }
            combination.add(new LinearTerm<>(Long.parseLong(key), value));
        }
        return combination;
    }
}

