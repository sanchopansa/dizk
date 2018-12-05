package input_feed.serial;

import algebra.fields.AbstractFieldElementExpanded;
import relations.objects.Assignment;
import relations.r1cs.R1CSRelation;
import scala.Tuple2;

public abstract class abstractFileToSerialR1CS<FieldT extends AbstractFieldElementExpanded<FieldT>> {
    private final String filePath;

    public abstractFileToSerialR1CS(final String _filePath) {
        filePath = _filePath;
    }

    public String filePath() {
        return filePath;
    }

    public abstract R1CSRelation<FieldT> loadR1CS(String fileName);

    public abstract Tuple2<Assignment<FieldT>, Assignment<FieldT>> loadWitness(String fileName);
}
