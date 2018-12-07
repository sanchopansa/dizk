package input_feed.serial;

import algebra.fields.AbstractFieldElementExpanded;
import algebra.fields.Fp;
import algebra.fields.abstractfieldparameters.AbstractFpParameters;
import relations.objects.*;
import relations.r1cs.R1CSRelation;
import scala.Tuple2;

import java.io.BufferedReader;
import java.io.FileReader;

public class TextToSerialR1CS<FieldT extends AbstractFieldElementExpanded<FieldT>> extends abstractFileToSerialR1CS<FieldT> {

    public TextToSerialR1CS(String _filePath, FieldT _fieldFactory) {
        super(_filePath, _fieldFactory);
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
                LinearCombination<FieldT> A = makeRowAt(currRow, brA);
                LinearCombination<FieldT> B = makeRowAt(currRow, brB);
                LinearCombination<FieldT> C = makeRowAt(currRow, brC);

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

            int numInputs = Integer.parseInt(constraintParameters[0]);
            int numAuxiliary = Integer.parseInt(constraintParameters[1]);

            BufferedReader br = new BufferedReader(
                    new FileReader(this.filePath() + ".witness"));

            final Assignment<FieldT> oneFullAssignment = new Assignment<>();

            String nextLine;
            int count = 0;
            while ((nextLine = br.readLine()) != null) {
                final FieldT value = this.fieldParameters().construct(nextLine);
                oneFullAssignment.add(value);
                count++;
            }

            assert (count == numInputs + numAuxiliary);

            primary = new Assignment<>(oneFullAssignment.subList(0, numInputs));
            auxiliary = new Assignment<>(oneFullAssignment.subList(numInputs, oneFullAssignment.size()));

        } catch (Exception e){
            System.err.println("Error: " + e.getMessage());
        }

        return new Tuple2<>(primary, auxiliary);
    }

    private LinearCombination<FieldT> makeRowAt (long index, BufferedReader reader) {
        final LinearCombination<FieldT> L = new LinearCombination<>();

        try {
            final int readAheadLimit = 100;
            String nextLine;
            reader.mark(readAheadLimit);
            while ((nextLine = reader.readLine()) != null) {
                String[] tokens = nextLine.split(" ");

                int col = Integer.parseInt(tokens[0]);
                int row = Integer.parseInt(tokens[1]);
                assert (row >= index);

                if (index == row) {
                    reader.mark(readAheadLimit);
                    final FieldT value = this.fieldParameters().construct(tokens[2]);
                    L.add(new LinearTerm<>(col, value));
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
