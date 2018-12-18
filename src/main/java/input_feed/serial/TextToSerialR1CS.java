package input_feed.serial;

import algebra.fields.AbstractFieldElementExpanded;
import relations.objects.*;
import relations.r1cs.R1CSRelation;
import scala.Tuple2;

import java.io.BufferedReader;
import java.io.FileReader;

public class TextToSerialR1CS<FieldT extends AbstractFieldElementExpanded<FieldT>> extends AbstractFileToSerialR1CS<FieldT> {

    public TextToSerialR1CS(
            final String _filePath,
            final FieldT _fieldFactory) {
        super(_filePath, _fieldFactory);
    }

    public TextToSerialR1CS(
            final String _filePath,
            final FieldT _fieldFactory,
            final boolean _negate) {
        super(_filePath, _fieldFactory, _negate);
    }

    @Override
    public R1CSRelation<FieldT> loadR1CS() {
        final R1CSConstraints<FieldT> constraints = new R1CSConstraints<>();

        int numInputs = -1;
        int numAuxiliary = -1;

        try{
            String[] constraintParameters = new BufferedReader(
                    new FileReader(this.filePath() + ".problem_size")).readLine().split(" ");

            numInputs = Integer.parseInt(constraintParameters[0]);
            numAuxiliary = Integer.parseInt(constraintParameters[1]);

            int numConstraints = Integer.parseInt(constraintParameters[2]);

            BufferedReader brA = new BufferedReader(new FileReader(this.filePath() + ".a"));
            BufferedReader brB = new BufferedReader(new FileReader(this.filePath() + ".b"));
            BufferedReader brC = new BufferedReader(new FileReader(this.filePath() + ".c"));

            for (int currRow = 0; currRow < numConstraints; currRow++){
                LinearCombination<FieldT> A = makeRowAt(currRow, brA, false);
                LinearCombination<FieldT> B = makeRowAt(currRow, brB, false);
                LinearCombination<FieldT> C = makeRowAt(currRow, brC, this.negate());

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

    @Override
    public Tuple2<Assignment<FieldT>, Assignment<FieldT>> loadWitness() {

        Assignment<FieldT> primary = new Assignment<>();
        Assignment<FieldT> auxiliary = new Assignment<>();

        try{
            String[] constraintParameters = new BufferedReader(
                    new FileReader(this.filePath() + ".problem_size")).readLine().split(" ");

            int numPrimary = Integer.parseInt(constraintParameters[0]);
            int numAuxiliary = Integer.parseInt(constraintParameters[1]);

            BufferedReader brA = new BufferedReader(
                    new FileReader(this.filePath() + ".aux"));

            BufferedReader brP = new BufferedReader(
                    new FileReader(this.filePath() + ".primary"));

            final Assignment<FieldT> fullAssignment = new Assignment<>();

            int count = 0;
            String[] splitAux = brA.readLine().split("\\s+");
            for (String next: splitAux) {
                final FieldT value = this.fieldParameters().construct(next);
                fullAssignment.add(value);
                count++;
            }
            brA.close();
            String[] splitPrimary = brP.readLine().split("\\s+");
            for (String next: splitPrimary) {
                final FieldT value = this.fieldParameters().construct(next);
                fullAssignment.add(value);
                count++;
            }
            brP.close();
            assert (count == numPrimary + numAuxiliary);

//            auxiliary = new Assignment<>(fullAssignment.subList(0, numAuxiliary));
//            primary = new Assignment<>(fullAssignment.subList(numAuxiliary, fullAssignment.size()));
            primary = new Assignment<>(fullAssignment.subList(0, numPrimary));
            auxiliary = new Assignment<>(fullAssignment.subList(numPrimary, fullAssignment.size()));

        } catch (Exception e){
            System.err.println("Error: " + e.getMessage());
        }

        return new Tuple2<>(primary, auxiliary);
    }

    private LinearCombination<FieldT> makeRowAt (long index, BufferedReader reader, boolean negate) {
        final LinearCombination<FieldT> L = new LinearCombination<>();

        try {
            final int readAheadLimit = 100;
            String nextLine;
            reader.mark(readAheadLimit);
            while ((nextLine = reader.readLine()) != null) {
                String[] tokens = nextLine.split(" ");

                int col = Integer.parseInt(tokens[0]);
                int row = Integer.parseInt(tokens[1]);

                if (index == row) {
                    reader.mark(readAheadLimit);
                    FieldT value = this.fieldParameters().construct(tokens[2]);
                    if (negate) {
                        value = value.negate();
                    }
                    L.add(new LinearTerm<>(col, value));
                } else if (row < index) {
                    System.out.format(
                            "[WARNING] found term with index %d after index %d. This term will be ignored.\n", row, index);
                } else if (row > index) {
                    reader.reset();
                    return L;
                }
            }
        } catch (Exception e){
            System.err.println("Error: " + e.getMessage());
        }
        return L;
    }
}
