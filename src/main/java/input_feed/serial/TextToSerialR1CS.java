package input_feed.serial;

import algebra.fields.AbstractFieldElementExpanded;
import relations.objects.*;
import relations.r1cs.R1CSRelation;
import scala.Tuple2;

import java.io.BufferedReader;
import java.io.FileReader;

public class TextToSerialR1CS<FieldT extends AbstractFieldElementExpanded<FieldT>> {
    private final String filePath;
    private final int numInputs;
    private final int numAuxiliary;
    private final int numConstraints;
    private final FieldT fieldParameters;
    private boolean negateCMatrix;

    public TextToSerialR1CS(
            final String _filePath,
            final FieldT _fieldParameters,
            final boolean _negateCMatrix) {
        filePath = _filePath;
        fieldParameters = _fieldParameters;
        negateCMatrix = _negateCMatrix;

        String[] parameters = new String[3];
        try{
            parameters = new BufferedReader(
                    new FileReader(filePath + ".problem_size")).readLine().split(" ");
        } catch (Exception e){
            System.err.println("Error: " + e.getMessage());
        }
        numInputs = Integer.parseInt(parameters[0]);
        numAuxiliary = Integer.parseInt(parameters[1]);
        numConstraints = Integer.parseInt(parameters[2]);
    }

    public TextToSerialR1CS(final String _filePath, final FieldT _fieldParameters) {
        this(_filePath, _fieldParameters, false);
    }

    public R1CSRelation<FieldT> loadR1CS() {
        final R1CSConstraints<FieldT> constraints = new R1CSConstraints<>();

        try{

            BufferedReader matrixA = new BufferedReader(new FileReader(filePath + ".a"));
            BufferedReader matrixB = new BufferedReader(new FileReader(filePath + ".b"));
            BufferedReader matrixC = new BufferedReader(new FileReader(filePath + ".c"));

            for (int currRow = 0; currRow < numConstraints; currRow++){
                LinearCombination<FieldT> A = makeRowAt(currRow, matrixA, false);
                LinearCombination<FieldT> B = makeRowAt(currRow, matrixB, false);
                LinearCombination<FieldT> C = makeRowAt(currRow, matrixC, negateCMatrix);

                constraints.add(new R1CSConstraint<>(A, B, C));
            }
            matrixA.close();
            matrixB.close();
            matrixC.close();

        } catch (Exception e){
            System.err.println("Error: " + e.getMessage());
        }

        return new R1CSRelation<>(constraints, numInputs, numAuxiliary);
    }

    public Tuple2<Assignment<FieldT>, Assignment<FieldT>> loadWitness() {

        Assignment<FieldT> primary = new Assignment<>();
        Assignment<FieldT> auxiliary = new Assignment<>();

        try{
            String[] constraintParameters = new BufferedReader(
                    new FileReader(filePath + ".problem_size")).readLine().split(" ");

            int numPrimary = Integer.parseInt(constraintParameters[0]);
            int numAuxiliary = Integer.parseInt(constraintParameters[1]);

            final Assignment<FieldT> fullAssignment = new Assignment<>();

            BufferedReader auxFile = new BufferedReader(new FileReader(filePath + ".aux"));
            int count = 0;
            String nextLine;
            while ((nextLine = auxFile.readLine()) != null) {
                final FieldT value = fieldParameters.construct(nextLine);
                fullAssignment.add(value);
                count++;
            }
            auxFile.close();

            BufferedReader publicFile = new BufferedReader(new FileReader(filePath + ".public"));
            while ((nextLine = publicFile.readLine()) != null) {
                final FieldT value = fieldParameters.construct(nextLine);
                fullAssignment.add(value);
                count++;
            }
            publicFile.close();

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
        final LinearCombination<FieldT> combination = new LinearCombination<>();
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
                    FieldT value = fieldParameters.construct(tokens[2]);
                    if (negate) {
                        value = value.negate();
                    }
                    combination.add(new LinearTerm<>(col, value));
                } else if (row < index) {
                    System.out.format(
                            "[WARNING] Term with index %d after index %d will be ignored.\n", row, index);
                } else {
                    reader.reset();
                    return combination;
                }
            }
        } catch (Exception e){
            System.err.println("Error: " + e.getMessage());
        }
        return combination;
    }
}
