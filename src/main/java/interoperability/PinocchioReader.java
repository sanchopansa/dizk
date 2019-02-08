package interoperability;

import algebra.curves.barreto_naehrig.bn254b.bn254b_parameters.BN254bFrParameters;
import algebra.fields.Fp;
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

public class PinocchioReader {

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
    private BN254bFrParameters fieldParams;

    private HashMap<Integer, Fp> wireValues;


    private PinocchioReader(String circuitFileName, String inputsFileName) {
        this.circuitFileName = circuitFileName;
        this.inputsFileName = inputsFileName;
        this.fieldParams = new BN254bFrParameters();

        this.inputWireIds = new ArrayList<>();
        this.nizkWireIds = new ArrayList<>();
        this.outputWireIds = new ArrayList<>();
        this.wireValues = new HashMap<>();
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
                    Fp wireVal = new Fp(new BigInteger(parts[1], 16), fieldParams);
                    wireValues.put(wireId, wireVal);
                }
            }

            Fp oneElement = fieldParams.ONE;
            Fp zeroElement = fieldParams.ZERO;

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

                        ArrayList<Fp> inValues = new ArrayList<>();
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
                        Fp constant = oneElement;

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
                            constant = new Fp(
                                    new BigInteger(instruction.substring("const-mul-neg-".length()), 16),
                                    fieldParams).negate();
                        } else if (instruction.startsWith("const-mul-")) {
                            opcode = MULCONST_OPCODE;
                            constant = new Fp(
                                    new BigInteger(instruction.substring("const-mul-".length()), 16),
                                    fieldParams);
                        } else {
                            System.err.println("Unrecognized instruction: " + line);
                            System.exit(1);
                        }

                        if (opcode == ADD_OPCODE) {
                            Fp sum = zeroElement;
                            for (Fp val: inValues)
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
                            Fp sum = zeroElement;
                            Fp two = oneElement;
                            for (Fp val: inValues) {
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

    private R1CSRelation<Fp> constructR1CS() {
        final R1CSConstraints<Fp> constraints = new R1CSConstraints<>();

        String line;
        int numGateInputs;
        int numGateOutputs;

        try (BufferedReader cr = new BufferedReader(new FileReader(circuitFileName))) {
            while ((line = cr.readLine()) != null) {
                final LinearCombination<Fp> A = new LinearCombination<>();
                final LinearCombination<Fp> B = new LinearCombination<>();
                final LinearCombination<Fp> C = new LinearCombination<>();

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
                            A.add(new LinearTerm<>(wireId, fieldParams.ONE));
                        });
                        B.add(new LinearTerm<>(0, fieldParams.ONE));
                        int outputWire = Integer.parseInt(outputStr);
                        C.add(new LinearTerm<>(outputWire, fieldParams.ONE));
                    } else if (instruction.equals("mul")) {
                        assert numGateInputs == 2 && numGateOutputs == 1;
                        String[] inputWires = inputStr.split(" ");
                        int outputWire = Integer.parseInt(outputStr);
                        A.add(new LinearTerm<>(Integer.parseInt(inputWires[0]), fieldParams.ONE));
                        B.add(new LinearTerm<>(Integer.parseInt(inputWires[1]), fieldParams.ONE));
                        C.add(new LinearTerm<>(outputWire, fieldParams.ONE));
                    } else if (instruction.equals("xor")) {
                        assert numGateInputs == 2 && numGateOutputs == 1;
                        String[] inputWires = inputStr.split(" ");
                        int inWire1 = Integer.parseInt(inputWires[0]);
                        int inWire2 = Integer.parseInt(inputWires[1]);
                        int outputWire = Integer.parseInt(outputStr);
                        A.add(new LinearTerm<>(inWire1, fieldParams.ONE.add(fieldParams.ONE)));
                        B.add(new LinearTerm<>(inWire2, fieldParams.ONE));
                        C.add(new LinearTerm<>(inWire1, fieldParams.ONE));
                        C.add(new LinearTerm<>(inWire2, fieldParams.ONE));
                        C.add(new LinearTerm<>(outputWire, fieldParams.ONE.negate()));
                    } else if (instruction.equals("or")) {
                        assert numGateInputs == 2 && numGateOutputs == 1;
                        String[] inputWires = inputStr.split(" ");
                        int inWire1 = Integer.parseInt(inputWires[0]);
                        int inWire2 = Integer.parseInt(inputWires[1]);
                        int outputWire = Integer.parseInt(outputStr);
                        A.add(new LinearTerm<>(inWire1, fieldParams.ONE));
                        B.add(new LinearTerm<>(inWire2, fieldParams.ONE));
                        C.add(new LinearTerm<>(inWire1, fieldParams.ONE));
                        C.add(new LinearTerm<>(inWire2, fieldParams.ONE));
                        C.add(new LinearTerm<>(outputWire, fieldParams.ONE.negate()));
                    } else if (instruction.equals("assert")) {
                        assert numGateInputs == 2 && numGateOutputs == 1;
                        String[] inputWires = inputStr.split(" ");
                        int outputWire = Integer.parseInt(outputStr);
                        A.add(new LinearTerm<>(Integer.parseInt(inputWires[0]), fieldParams.ONE));
                        B.add(new LinearTerm<>(Integer.parseInt(inputWires[1]), fieldParams.ONE));
                        C.add(new LinearTerm<>(outputWire, fieldParams.ONE));
                    } else if (instruction.equals("pack")) {
                        assert numGateOutputs == 1;
                        String[] inputWires = inputStr.split(" ");
                        int outputWire = Integer.parseInt(outputStr);
                        Fp two = fieldParams.ONE;
                        for (int i = 0; i < inputWires.length; i++) {
                            A.add(new LinearTerm<>(Integer.parseInt(inputWires[i]), two));
                            two = two.add(two);
                        }
                        B.add(new LinearTerm<>(0, fieldParams.ONE));
                        C.add(new LinearTerm<>(outputWire, fieldParams.ONE));
                    } else if (instruction.equals("zerop")) {
                        assert numGateInputs == 1 && numGateOutputs == 2;
                    } else if (instruction.equals("split")) {
                        assert numGateInputs == 1;
                        String[] outputWires = outputStr.split(" ");
                        int inputWire = Integer.parseInt(inputStr);
                        Fp two = fieldParams.ONE;

                        A.add(new LinearTerm<>(inputWire, fieldParams.ONE));
                        B.add(new LinearTerm<>(0, fieldParams.ONE));

                        for (int i = 0; i < outputWires.length; i++) {
                            int wireId = Integer.parseInt(outputWires[i]);
                            C.add(new LinearTerm<>(wireId, two));
                            two = two.add(two);

                            // Enforce bitness
                            final LinearCombination<Fp> Aa = new LinearCombination<>();
                            final LinearCombination<Fp> Bb = new LinearCombination<>();
                            final LinearCombination<Fp> Cc = new LinearCombination<>();
                            Aa.add(new LinearTerm<>(wireId, fieldParams.ONE));
                            Bb.add(new LinearTerm<>(wireId, fieldParams.ONE));
                            Cc.add(new LinearTerm<>(wireId, fieldParams.ONE));
                            constraints.add(new R1CSConstraint<>(Aa, Bb, Cc));
                        }
                    } else if (instruction.startsWith("const-mul-neg-")) {
                        assert numGateInputs == 1 && numGateOutputs == 1;
                        Fp constant = new Fp(
                                new BigInteger(instruction.substring("const-mul-neg-".length()), 16),
                                fieldParams).negate();
                        A.add(new LinearTerm<>(Integer.parseInt(inputStr), constant));
                        B.add(new LinearTerm<>(0, fieldParams.ONE));
                        C.add(new LinearTerm<>(Integer.parseInt(outputStr), fieldParams.ONE));
                    } else if (instruction.startsWith("const-mul-")) {
                        assert numGateInputs == 1 && numGateOutputs == 1;
                        Fp constant = new Fp(
                                new BigInteger(instruction.substring("const-mul-".length()), 16),
                                fieldParams);
                        A.add(new LinearTerm<>(Integer.parseInt(inputStr), constant));
                        B.add(new LinearTerm<>(0, fieldParams.ONE));
                        C.add(new LinearTerm<>(Integer.parseInt(outputStr), fieldParams.ONE));
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

    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("You must specify the paths to the arithmetic circuit and the input parameters");
            System.exit(1);
        }
        PinocchioReader reader = new PinocchioReader(args[0], args[1]);
        reader.parseAndEval();

        R1CSRelation r1cs = reader.constructR1CS();

        Assignment<Fp> primary = new Assignment<>();
        primary.add(reader.fieldParams.ONE);
        Assignment<Fp> auxiliary = new Assignment<>();
        for (int i = 1; i < reader.wireValues.size(); i++) {
            auxiliary.add(reader.wireValues.get(i));
        }

        System.out.println("R1CS satisfied: " + r1cs.isSatisfied(primary, auxiliary));
    }
}
