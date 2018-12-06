package input_feed.serial;

import algebra.fields.AbstractFieldElementExpanded;
import algebra.fields.Fp;
import algebra.fields.abstractfieldparameters.AbstractFpParameters;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import relations.objects.*;
import relations.r1cs.R1CSRelation;
import scala.Tuple2;

import java.io.FileReader;

public class JSONToSerialR1CS<FieldT extends AbstractFieldElementExpanded<FieldT>>
        extends abstractFileToSerialR1CS {

    public JSONToSerialR1CS(String _filePath, AbstractFpParameters _fieldParameters) {
        super(_filePath, _fieldParameters);
    }

    @Override
    public R1CSRelation<FieldT> loadR1CS() {
        JSONParser parser = new JSONParser();
        JSONObject jsonObject;
        JSONArray header = new JSONArray();
        JSONArray constraintList = new JSONArray();

        try {
            Object obj = parser.parse(new FileReader(this.filePath()));
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

        return new R1CSRelation<>(constraints, numInputs, numAuxiliary);
    }

    public Tuple2<Assignment<FieldT>, Assignment<FieldT>> loadWitness () {
        JSONParser parser = new JSONParser();
        JSONObject jsonObject;
        JSONArray primaryInputs = new JSONArray();
        JSONArray auxInputs = new JSONArray();

        try {
            Object obj = parser.parse(new FileReader(this.filePath()));

            jsonObject = (JSONObject) obj;
            primaryInputs = (JSONArray) jsonObject.get("primary_input");
            auxInputs = (JSONArray) jsonObject.get("aux_input");

        } catch (Exception e) {
            e.printStackTrace();
        }

        final Assignment<FieldT> primary = new Assignment<>();
        for (Object element: primaryInputs) {
            final Fp value = new Fp((String) element, this.fieldParameters());
            primary.add((FieldT) value);
        }

        final Assignment<FieldT> auxiliary = new Assignment<>();
        for (Object element: auxInputs) {
            final Fp value = new Fp((String) element, this.fieldParameters());
            auxiliary.add((FieldT) value);
        }

        return new Tuple2<>(primary, auxiliary);
    }

    private <FieldT extends AbstractFieldElementExpanded<FieldT>>
    LinearCombination<FieldT>
    serialCombinationFromJSON (final JSONObject matrixRow) {
        final LinearCombination<FieldT> L = new LinearCombination<>();

        for (Object keyObj: matrixRow.keySet()) {
            String key = (String) keyObj;
            Fp value;
            try{
                value = new Fp((String) matrixRow.get(key), this.fieldParameters());
            } catch (ClassCastException e){
                value = new Fp(Long.toString((long) matrixRow.get(key)), this.fieldParameters());
            }
            L.add(new LinearTerm<>(Long.parseLong(key), (FieldT) value));
        }
        return L;
    }
}

