package input_feed.serial;

import algebra.curves.barreto_naehrig.bn254a.BN254aFields;
import algebra.fields.AbstractFieldElementExpanded;
import relations.objects.*;
import relations.r1cs.R1CSRelation;
import scala.Tuple2;

import java.io.BufferedReader;
import java.io.FileReader;

public class TextToSerialR1CS<FieldT extends AbstractFieldElementExpanded<FieldT>> extends abstractFileToSerialR1CS {

    public TextToSerialR1CS(String _filePath) {
        super(_filePath);
    }

    @Override
    public R1CSRelation<FieldT> loadR1CS(String fileName) {
        final R1CSConstraints<FieldT> constraints = new R1CSConstraints<>();

        int numInputs = -1;
        int numAuxiliary = -1;

        try{
            String[] constraintParameters = new BufferedReader(
                    new FileReader(this.filePath() + fileName + ".problem_size")).readLine().split(" ");

            numInputs = Integer.parseInt(constraintParameters[0]);
            numAuxiliary = Integer.parseInt(constraintParameters[1]);

            int numConstraints = Integer.parseInt(constraintParameters[2]);

            BufferedReader brA = new BufferedReader(new FileReader(this.filePath() + fileName + ".a"));
            BufferedReader brB = new BufferedReader(new FileReader(this.filePath() + fileName + ".b"));
            BufferedReader brC = new BufferedReader(new FileReader(this.filePath() + fileName + ".c"));

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
    public Tuple2<Assignment, Assignment> loadWitness(String fileName) {

        Assignment<FieldT> primary = new Assignment<>();
        Assignment<FieldT> auxiliary = new Assignment<>();

        try{
            String[] constraintParameters = new BufferedReader(
                    new FileReader(this.filePath() + fileName + ".problem_size")).readLine().split(" ");

            int numInputs = Integer.parseInt(constraintParameters[0]);
            int numAuxiliary = Integer.parseInt(constraintParameters[1]);

            BufferedReader br = new BufferedReader(
                    new FileReader(this.filePath() + fileName + ".witness"));

            final Assignment<FieldT> oneFullAssignment = new Assignment<>();

            String nextLine;
            int count = 0;
            while ((nextLine = br.readLine()) != null) {
                final BN254aFields.BN254aFr value = new BN254aFields.BN254aFr(nextLine);
                oneFullAssignment.add((FieldT) value);
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

    private static <FieldT extends AbstractFieldElementExpanded<FieldT>>
    LinearCombination<FieldT>
    makeRowAt (long index, BufferedReader reader) {
        // Assumes input to be ordered by row and that the last line is blank.
        final LinearCombination<FieldT> L = new LinearCombination<>();

        try {
            String nextLine;
            while ((nextLine = reader.readLine()) != null) {
                String[] tokens = nextLine.split(" ");

                int col = Integer.parseInt(tokens[0]);
                int row = Integer.parseInt(tokens[1]);
                assert (row >= index);

                if (index == row) {
                    reader.mark(100);
                    L.add(new LinearTerm<>(col, (FieldT) new BN254aFields.BN254aFr(tokens[2])));
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
