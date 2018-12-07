package input_feed.serial;

import algebra.fields.AbstractFieldElementExpanded;
import algebra.fields.abstractfieldparameters.AbstractFpParameters;
import relations.objects.Assignment;
import relations.r1cs.R1CSRelation;
import scala.Tuple2;

public abstract class abstractFileToSerialR1CS<FieldT extends AbstractFieldElementExpanded<FieldT>> {
    private final String filePath;
    private final FieldT fieldFactory;

    abstractFileToSerialR1CS(
            final String _filePath, final FieldT _fieldFactory) {
        filePath = _filePath;
        fieldFactory = _fieldFactory;
    }

    String filePath() {
        return filePath;
    }

    FieldT fieldParameters() { return fieldFactory; }

    public abstract R1CSRelation<FieldT> loadR1CS();

    public abstract Tuple2<Assignment<FieldT>, Assignment<FieldT>> loadWitness();
}
