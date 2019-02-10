package interoperability;

import algebra.curves.barreto_naehrig.bn254a.BN254aFields;
import algebra.fields.AbstractFieldElementExpanded;
import relations.objects.*;
import relations.r1cs.R1CSRelation;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import scala.Tuple2;

public class PinocchioReader<FieldT extends AbstractFieldElementExpanded<FieldT>> {

    private static final short ADD_OPCODE = 1;
    private static final short MUL_OPCODE = 2;
    private static final short SPLIT_OPCODE = 3;
    private static final short NONZEROCHECK_OPCODE = 4;
    private static final short PACK_OPCODE = 5;
    private static final short MULCONST_OPCODE = 6;
    private static final short XOR_OPCODE = 7;
    private static final short OR_OPCODE = 8;
    private static final short CONSTRAINT_OPCODE = 9;

    private static final Pattern PINOCCHIO_INSTRUCTION = Pattern.compile("([\\w|-]+) in (\\d+) <([^>]+)> out (\\d+) <([^>]+)>");

    private static int numInputs = 0;
    private static int numNizkInputs = 0;
    private static int numOutputs = 0;

    private String circuitFileName;
    private String inputsFileName;
    private ArrayList<Integer> inputWireIds;
    private ArrayList<Integer> nizkWireIds;
    private ArrayList<Integer> outputWireIds;
    private FieldT fieldParams;

    private HashMap<Integer, FieldT> wireValues;


    public PinocchioReader(final FieldT fieldParams, final String circuitFileName, final String inputsFileName) {
        this.circuitFileName = circuitFileName;
        this.inputsFileName = inputsFileName;
        this.fieldParams = fieldParams;

        this.inputWireIds = new ArrayList<>();
        this.nizkWireIds = new ArrayList<>();
        this.outputWireIds = new ArrayList<>();
        this.wireValues = new HashMap<>();

        this.parseAndEval();
    }

    private void parseAndEval() {
        try (BufferedReader cr = new BufferedReader(new FileReader(circuitFileName))) {
            String line = cr.readLine();
            String[] parts = line.split(" ");
            long totalWires = -1L;

            if (parts.length != 2 || !parts[0].equals("total")) {
                System.err.println("Invalid circuit file format");
                System.exit(1);
            }

            try {
                totalWires = Long.parseLong(parts[1]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid circuit file format");
                System.exit(1);
            }

            System.out.println("Total wires: " + totalWires);

            // Read input values
            try (BufferedReader ir = new BufferedReader(new FileReader(inputsFileName))) {
                while ((line = ir.readLine()) != null) {
                    if (line.isEmpty()) {
                        continue;
                    }
                    parts = line.split(" ");
                    if (parts.length != 2) {
                        System.err.println("Error while reading input values: " + line);
                        System.exit(1);
                    }
                    int wireId = Integer.parseInt(parts[0]);
                    // Wire value is encoded as hex
                    FieldT wireVal = fieldParams.fromHexString(parts[1]);
                    wireValues.put(wireId, wireVal);
                }
            }

            FieldT oneElement = fieldParams.one();
            FieldT zeroElement = fieldParams.zero();

            int numGateInputs;
            int numGateOutputs;

            HashMap<Integer, Integer> wireUseCounters = new HashMap<>();

            while ((line = cr.readLine()) != null) {
                if (line.isEmpty()) {
                    continue;
                }

                if (line.startsWith("#")) {
                    // Ignore comments
                } else if (line.startsWith("input")) {
                    parts = line.split(" ");
                    numInputs++;
                    inputWireIds.add(Integer.parseInt(parts[1].trim()));
                } else if (line.startsWith("nizkinput")) {
                    parts = line.split(" ");
                    if (parts.length != 2) {
                        System.err.println("Error while reading input values: " + line);
                        System.exit(1);
                    }
                    numNizkInputs++;
                    nizkWireIds.add(Integer.parseInt(parts[1].trim()));
                } else if (line.startsWith("output")) {
                    parts = line.split(" ");
                    if (parts.length != 2) {
                        System.err.println("Error while reading input values: " + line);
                        System.exit(1);
                    }
                    int wireId = Integer.parseInt(parts[1].trim());
                    numOutputs++;
                    outputWireIds.add(wireId);
                    wireUseCounters.put(wireId, wireUseCounters.getOrDefault(wireId, 0) + 1);
                } else {
                    Matcher matcher = PINOCCHIO_INSTRUCTION.matcher(line);
                    if (matcher.matches()) {
                        String instruction = matcher.group(1);
                        numGateInputs = Integer.parseInt(matcher.group(2));
                        String inputStr = matcher.group(3);
                        numGateOutputs = Integer.parseInt(matcher.group(4));
                        String outputStr = matcher.group(5);

                        ArrayList<FieldT> inValues = new ArrayList<>();
                        Arrays.stream(inputStr.split(" "))
                                .forEach(x -> {
                                    int wireId = Integer.parseInt(x);
                                    inValues.add(wireValues.get(wireId));
                                    wireUseCounters.put(wireId, wireUseCounters.getOrDefault(wireId,0) + 1);
                                });

                        assert numGateInputs == inValues.size();

                        List<Integer> outWires = Arrays.stream(outputStr.split(" "))
                                .map(Integer::parseInt)
                                .collect(Collectors.toList());

                        assert numGateOutputs == outWires.size();

                        short opcode = 0;
                        FieldT constant = oneElement;

                        if (instruction.equals("add")) {
                            opcode = ADD_OPCODE;
                        } else if (instruction.equals("mul")) {
                            opcode = MUL_OPCODE;
                        } else if (instruction.equals("xor")) {
                            opcode = XOR_OPCODE;
                        } else if (instruction.equals("or")) {
                            opcode = OR_OPCODE;
                        } else if (instruction.equals("assert")) {
                            wireUseCounters.put(outWires.get(0), wireUseCounters.getOrDefault(outWires.get(0), 0));
                            opcode = CONSTRAINT_OPCODE;
                        } else if (instruction.equals("pack")) {
                            opcode = PACK_OPCODE;
                        } else if (instruction.equals("zerop")) {
                            opcode = NONZEROCHECK_OPCODE;
                        } else if (instruction.equals("split")) {
                            opcode = SPLIT_OPCODE;
                        } else if (instruction.startsWith("const-mul-neg-")) {
                            opcode = MULCONST_OPCODE;
                            constant = fieldParams.fromHexString(instruction.substring("const-mul-neg-".length())).negate();
                        } else if (instruction.startsWith("const-mul-")) {
                            opcode = MULCONST_OPCODE;
                            constant = fieldParams.fromHexString(instruction.substring("const-mul-".length()));
                        } else {
                            System.err.println("Unrecognized instruction: " + line);
                            System.exit(1);
                        }

                        if (opcode == ADD_OPCODE) {
                            FieldT sum = zeroElement;
                            for (FieldT val: inValues)
                                sum = sum.add(val);
                            wireValues.put(outWires.get(0), sum);
                        } else if (opcode == MUL_OPCODE) {
                            wireValues.put(outWires.get(0), inValues.get(0).mul(inValues.get(1)));
                        } else if (opcode == XOR_OPCODE) {
                            wireValues.put(outWires.get(0), inValues.get(0).equals(inValues.get(1)) ? zeroElement : oneElement);
                        } else if (opcode == OR_OPCODE) {
                            wireValues.put(outWires.get(0),
                                    inValues.get(0).equals(zeroElement) && inValues.get(1).equals(zeroElement) ? zeroElement : oneElement);
                        } else if (opcode == NONZEROCHECK_OPCODE) {
                            wireValues.put(outWires.get(1), inValues.get(0).equals(zeroElement) ? zeroElement : oneElement);
                        } else if (opcode == PACK_OPCODE) {
                            FieldT sum = zeroElement;
                            FieldT two = oneElement;
                            for (FieldT val: inValues) {
                                sum = sum.add(two.mul(val));
                                two = two.add(two);
                            }
                            wireValues.put(outWires.get(0), sum);
                        } else if (opcode == SPLIT_OPCODE) {
                            int size = outWires.size();
                            BigInteger inVal = inValues.get(0).toBigInteger();
                            for (int i = 0; i < size; i++) {
                                wireValues.put(outWires.get(i), inVal.testBit(i) ? oneElement : zeroElement);
                            }
                        } else if (opcode == MULCONST_OPCODE) {
                            wireValues.put(outWires.get(0), constant.mul(inValues.get(0)));
                        }

                    } else {
                        System.out.println("Invalid line: " + line);
                    }
                }
            }

            System.out.println("Number of public inputs: " + numInputs);
            System.out.println("Number of private inputs: " + numNizkInputs);
            System.out.println("Number of outputs: " + numOutputs);

            for(int wireId: outputWireIds) {
                System.out.println("Wire " + wireId + ": " + wireValues.get(wireId));
            }

        } catch (IOException e) {
            System.err.println("Error while reading file: " +  e.getMessage());
        }
    }

    public R1CSRelation<FieldT> constructR1CSSerial() {
        final R1CSConstraints<FieldT> constraints = new R1CSConstraints<>();

        String line;
        int numGateInputs;
        int numGateOutputs;

        try (BufferedReader cr = new BufferedReader(new FileReader(circuitFileName))) {
            while ((line = cr.readLine()) != null) {
                final LinearCombination<FieldT> A = new LinearCombination<>();
                final LinearCombination<FieldT> B = new LinearCombination<>();
                final LinearCombination<FieldT> C = new LinearCombination<>();

                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                Matcher matcher = PINOCCHIO_INSTRUCTION.matcher(line);
                if (matcher.matches()) {
                    String instruction = matcher.group(1);
                    numGateInputs = Integer.parseInt(matcher.group(2));
                    String inputStr = matcher.group(3);
                    numGateOutputs = Integer.parseInt(matcher.group(4));
                    String outputStr = matcher.group(5);

                    if (instruction.equals("add")) {
                        assert numGateOutputs == 1;
                        Arrays.stream(inputStr.split(" ")).forEach(v -> {
                            long wireId = Long.parseLong(v);
                            A.add(new LinearTerm<>(wireId, fieldParams.one()));
                        });
                        B.add(new LinearTerm<>(0, fieldParams.one()));
                        int outputWire = Integer.parseInt(outputStr);
                        C.add(new LinearTerm<>(outputWire, fieldParams.one()));
                    } else if (instruction.equals("mul")) {
                        assert numGateInputs == 2 && numGateOutputs == 1;
                        String[] inputWires = inputStr.split(" ");
                        int outputWire = Integer.parseInt(outputStr);
                        A.add(new LinearTerm<>(Integer.parseInt(inputWires[0]), fieldParams.one()));
                        B.add(new LinearTerm<>(Integer.parseInt(inputWires[1]), fieldParams.one()));
                        C.add(new LinearTerm<>(outputWire, fieldParams.one()));
                    } else if (instruction.equals("xor")) {
                        assert numGateInputs == 2 && numGateOutputs == 1;
                        String[] inputWires = inputStr.split(" ");
                        int inWire1 = Integer.parseInt(inputWires[0]);
                        int inWire2 = Integer.parseInt(inputWires[1]);
                        int outputWire = Integer.parseInt(outputStr);
                        A.add(new LinearTerm<>(inWire1, fieldParams.one().add(fieldParams.one())));
                        B.add(new LinearTerm<>(inWire2, fieldParams.one()));
                        C.add(new LinearTerm<>(inWire1, fieldParams.one()));
                        C.add(new LinearTerm<>(inWire2, fieldParams.one()));
                        C.add(new LinearTerm<>(outputWire, fieldParams.one().negate()));
                    } else if (instruction.equals("or")) {
                        assert numGateInputs == 2 && numGateOutputs == 1;
                        String[] inputWires = inputStr.split(" ");
                        int inWire1 = Integer.parseInt(inputWires[0]);
                        int inWire2 = Integer.parseInt(inputWires[1]);
                        int outputWire = Integer.parseInt(outputStr);
                        A.add(new LinearTerm<>(inWire1, fieldParams.one()));
                        B.add(new LinearTerm<>(inWire2, fieldParams.one()));
                        C.add(new LinearTerm<>(inWire1, fieldParams.one()));
                        C.add(new LinearTerm<>(inWire2, fieldParams.one()));
                        C.add(new LinearTerm<>(outputWire, fieldParams.one().negate()));
                    } else if (instruction.equals("assert")) {
                        assert numGateInputs == 2 && numGateOutputs == 1;
                        String[] inputWires = inputStr.split(" ");
                        int outputWire = Integer.parseInt(outputStr);
                        A.add(new LinearTerm<>(Integer.parseInt(inputWires[0]), fieldParams.one()));
                        B.add(new LinearTerm<>(Integer.parseInt(inputWires[1]), fieldParams.one()));
                        C.add(new LinearTerm<>(outputWire, fieldParams.one()));
                    } else if (instruction.equals("pack")) {
                        assert numGateOutputs == 1;
                        String[] inputWires = inputStr.split(" ");
                        int outputWire = Integer.parseInt(outputStr);
                        FieldT two = fieldParams.one();
                        for (int i = 0; i < inputWires.length; i++) {
                            A.add(new LinearTerm<>(Integer.parseInt(inputWires[i]), two));
                            two = two.add(two);
                        }
                        B.add(new LinearTerm<>(0, fieldParams.one()));
                        C.add(new LinearTerm<>(outputWire, fieldParams.one()));
                    } else if (instruction.equals("zerop")) {
                        assert numGateInputs == 1 && numGateOutputs == 2;
                    } else if (instruction.equals("split")) {
                        assert numGateInputs == 1;
                        String[] outputWires = outputStr.split(" ");
                        int inputWire = Integer.parseInt(inputStr);
                        FieldT two = fieldParams.one();

                        A.add(new LinearTerm<>(inputWire, fieldParams.one()));
                        B.add(new LinearTerm<>(0, fieldParams.one()));

                        for (int i = 0; i < outputWires.length; i++) {
                            int wireId = Integer.parseInt(outputWires[i]);
                            C.add(new LinearTerm<>(wireId, two));
                            two = two.add(two);

                            // Enforce bitness
                            final LinearCombination<FieldT> Aa = new LinearCombination<>();
                            final LinearCombination<FieldT> Bb = new LinearCombination<>();
                            final LinearCombination<FieldT> Cc = new LinearCombination<>();
                            Aa.add(new LinearTerm<>(wireId, fieldParams.one()));
                            Bb.add(new LinearTerm<>(wireId, fieldParams.one()));
                            Cc.add(new LinearTerm<>(wireId, fieldParams.one()));
                            constraints.add(new R1CSConstraint<>(Aa, Bb, Cc));
                        }
                    } else if (instruction.startsWith("const-mul-neg-")) {
                        assert numGateInputs == 1 && numGateOutputs == 1;
                        FieldT constant = fieldParams
                                .fromHexString(instruction.substring("const-mul-neg-".length()))
                                .negate();
                        A.add(new LinearTerm<>(Integer.parseInt(inputStr), constant));
                        B.add(new LinearTerm<>(0, fieldParams.one()));
                        C.add(new LinearTerm<>(Integer.parseInt(outputStr), fieldParams.one()));
                    } else if (instruction.startsWith("const-mul-")) {
                        assert numGateInputs == 1 && numGateOutputs == 1;
                        FieldT constant = fieldParams
                                .fromHexString(instruction.substring("const-mul-".length()));
                        A.add(new LinearTerm<>(Integer.parseInt(inputStr), constant));
                        B.add(new LinearTerm<>(0, fieldParams.one()));
                        C.add(new LinearTerm<>(Integer.parseInt(outputStr), fieldParams.one()));
                    } else {
                        System.err.println("Unrecognized instruction: " + line);
                        System.exit(1);
                    }
                    constraints.add(new R1CSConstraint<>(A, B, C));
                }
            }
        } catch (IOException e) {
            System.err.println("Error while reading file: " +  e.getMessage());
        }

        return new R1CSRelation<>(constraints, numInputs, numNizkInputs);
    }

    public Tuple2<Assignment, Assignment> getWitness() {
        Assignment<FieldT> primary = new Assignment<>();
        primary.add(fieldParams.one());
        Assignment<FieldT> auxiliary = new Assignment<>();
        for (int i = 1; i < wireValues.size(); i++) {
            auxiliary.add(wireValues.get(i));
        }
        return new Tuple2<>(primary, auxiliary);
    }

    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("You must specify the paths to the arithmetic circuit and the input parameters");
            System.exit(1);
        }

        final BN254aFields.BN254aFr fieldFactory = new BN254aFields.BN254aFr(2L);
        PinocchioReader reader = new PinocchioReader<>(fieldFactory, args[0], args[1]);
        R1CSRelation r1cs = reader.constructR1CSSerial();
        Tuple2<Assignment, Assignment> witness = reader.getWitness();

        System.out.println("R1CS satisfied: " + r1cs.isSatisfied(witness._1, witness._2));
    }
}
