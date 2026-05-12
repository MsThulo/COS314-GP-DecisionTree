import java.io.*;
import java.util.*;

public class GPDecisionTree {

    static String[] FEATURE_NAMES = {
        "age", "menopause", "tumor_size", "inv_nodes",
        "node_caps", "deg_malig", "breast", "breast_quad", "irradiat"
    };
    static int NUM_FEATURES = 9;

    static int    POP_SIZE        = 200;
    static int    MAX_GENERATIONS = 100;
    static int    MAX_DEPTH       = 4;
    static double CROSSOVER_RATE  = 0.8;
    static double MUTATION_RATE   = 0.1;
    static int    TOURNAMENT_SIZE = 5;

    static class Node {
        boolean isLeaf;
        int leafValue;
        int featureIndex;
        double threshold;
        Node left;
        Node right;

        Node(int value) {
            this.isLeaf    = true;
            this.leafValue = value;
        }

        Node(int featureIndex, double threshold) {
            this.isLeaf       = false;
            this.featureIndex = featureIndex;
            this.threshold    = threshold;
        }

        Node deepCopy(int depthLeft) {
            if (depthLeft == 0) {
                return new Node(this.isLeaf ? this.leafValue : 0);
            }
            if (this.isLeaf) {
                return new Node(this.leafValue);
            }
            Node copy = new Node(this.featureIndex, this.threshold);
            if (this.left  != null) copy.left  = this.left.deepCopy(depthLeft - 1);
            if (this.right != null) copy.right = this.right.deepCopy(depthLeft - 1);
            return copy;
        }
    }

    static Random rng;

    // STEP 1: READ CSV
    static double[][] loadCSV(String filepath) throws IOException {
        List<double[]> rows = new ArrayList<>();
        BufferedReader br = new BufferedReader(new FileReader(filepath));
        String line = br.readLine();
        while ((line = br.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty()) continue;
            String[] parts = line.split(",");
            double[] row = new double[parts.length];
            for (int i = 0; i < parts.length; i++) {
                row[i] = Double.parseDouble(parts[i].trim());
            }
            rows.add(row);
        }
        br.close();
        return rows.toArray(new double[0][]);
    }

    // STEP 2: BUILD RANDOM TREES
    static Node buildRandomTree(int maxDepth, String method) {
        if (maxDepth == 0 || (method.equals("grow") && rng.nextDouble() < 0.3)) {
            return new Node(rng.nextInt(2));
        }
        int feature = rng.nextInt(NUM_FEATURES);
        double threshold = rng.nextInt(11);
        Node node = new Node(feature, threshold);
        node.left  = buildRandomTree(maxDepth - 1, method);
        node.right = buildRandomTree(maxDepth - 1, method);
        return node;
    }

    static Node[] buildInitialPopulation() {
        Node[] population = new Node[POP_SIZE];
        for (int i = 0; i < POP_SIZE; i++) {
            int depth = 2 + (i % (MAX_DEPTH - 1));
            String method = (i < POP_SIZE / 2) ? "full" : "grow";
            population[i] = buildRandomTree(depth, method);
        }
        return population;
    }

    // STEP 3: CLASSIFY AND SCORE
    static int classify(Node node, double[] patient) {
        if (node.isLeaf) return node.leafValue;
        double value = patient[node.featureIndex + 1];
        if (value <= node.threshold) {
            return classify(node.left, patient);
        } else {
            return classify(node.right, patient);
        }
    }

    static double getAccuracy(Node tree, double[][] data) {
        int correct = 0;
        for (double[] patient : data) {
            int actual    = (int) patient[0];
            int predicted = classify(tree, patient);
            if (predicted == actual) correct++;
        }
        return (double) correct / data.length;
    }

    static double getFMeasure(Node tree, double[][] data) {
        int tp = 0, fp = 0, fn = 0;
        for (double[] patient : data) {
            int actual    = (int) patient[0];
            int predicted = classify(tree, patient);
            if (predicted == 1 && actual == 1) tp++;
            if (predicted == 1 && actual == 0) fp++;
            if (predicted == 0 && actual == 1) fn++;
        }
        double precision = (tp + fp == 0) ? 0 : (double) tp / (tp + fp);
        double recall    = (tp + fn == 0) ? 0 : (double) tp / (tp + fn);
        if (precision + recall == 0) return 0;
        return 2 * precision * recall / (precision + recall);
    }

    // STEP 4: TOURNAMENT, CROSSOVER, MUTATION
    static Node tournamentSelect(Node[] population, double[][] data) {
        Node best = null;
        double bestScore = -1;
        for (int i = 0; i < TOURNAMENT_SIZE; i++) {
            Node competitor = population[rng.nextInt(POP_SIZE)];
            double score    = getAccuracy(competitor, data);
            if (score > bestScore) {
                bestScore = score;
                best      = competitor;
            }
        }
        return best;
    }

    static Node crossover(Node parent1, Node parent2) {
        Node child = parent1.deepCopy(MAX_DEPTH);
        if (rng.nextBoolean()) {
            child.left  = parent2.deepCopy(MAX_DEPTH - 1);
        } else {
            child.right = parent2.deepCopy(MAX_DEPTH - 1);
        }
        return child;
    }

    static Node mutate(Node node, int depth) {
        if (node == null) return null;
        if (!node.isLeaf && rng.nextDouble() < MUTATION_RATE) {
            node.featureIndex = rng.nextInt(NUM_FEATURES);
            node.threshold    = rng.nextInt(11);
        }
        if (!node.isLeaf && depth > 0) {
            node.left  = mutate(node.left,  depth - 1);
            node.right = mutate(node.right, depth - 1);
        }
        return node;
    }

    static Node evolve(Node[] population, double[][] trainData) {
        Node bestEver    = null;
        double bestScore = -1;

        for (int gen = 0; gen < MAX_GENERATIONS; gen++) {
            Node[] newPopulation = new Node[POP_SIZE];
            for (int i = 0; i < POP_SIZE; i++) {
                Node parent1 = tournamentSelect(population, trainData);
                Node child;
                if (rng.nextDouble() < CROSSOVER_RATE) {
                    Node parent2 = tournamentSelect(population, trainData);
                    child = crossover(parent1, parent2);
                } else {
                    child = parent1.deepCopy(MAX_DEPTH);
                }
                child = mutate(child, MAX_DEPTH);
                newPopulation[i] = child;
            }
            population = newPopulation;

            Node bestThisGen    = null;
            double bestGenScore = -1;
            for (Node tree : population) {
                double score = getAccuracy(tree, trainData);
                if (score > bestGenScore) {
                    bestGenScore = score;
                    bestThisGen  = tree;
                }
            }
            if (bestGenScore > bestScore) {
                bestScore = bestGenScore;
                bestEver  = bestThisGen.deepCopy(MAX_DEPTH);
            }
        }
        return bestEver;
    }

    // PRINT TREE AS TEXT FOR DEMO
    static void printTree(Node node, String indent) {
        if (node == null) return;
        if (node.isLeaf) {
            System.out.println(indent + "-> " + (node.leafValue == 1 ? "CANCER (1)" : "NO CANCER (0)"));
        } else {
            String feature = FEATURE_NAMES[node.featureIndex];
            System.out.println(indent + "If " + feature + " <= " + (int)node.threshold);
            printTree(node.left,  indent + "  |YES| ");
            System.out.println(indent + "Else");
            printTree(node.right, indent + "  |NO|  ");
        }
    }

    // ─────────────────────────────────────────────
    // RUN ONCE — runs full GP with one seed
    // returns [trainAcc, testAcc, fMeasure, runtime]
    // ─────────────────────────────────────────────
    static double[] runOnce(long seed, double[][] trainData, double[][] testData) {
        rng = new Random(seed);
        Node[] population = buildInitialPopulation();
        long start = System.currentTimeMillis();
        Node bestTree = evolve(population, trainData);
        long end = System.currentTimeMillis();

        double trainAcc = getAccuracy(bestTree, trainData);
        double testAcc  = getAccuracy(bestTree, testData);
        double fMeasure = getFMeasure(bestTree, testData);
        double runtime  = (end - start) / 1000.0;

        return new double[]{trainAcc, testAcc, fMeasure, runtime, seed};
    }

    // MAIN
    public static void main(String[] args) throws IOException {
        Scanner scanner = new Scanner(System.in);

        System.out.print("Enter path to TRAINING file: ");
        String trainPath = scanner.next();

        System.out.print("Enter path to TEST file: ");
        String testPath = scanner.next();

        System.out.println("\nLoading data...");
        double[][] trainData = loadCSV(trainPath);
        double[][] testData  = loadCSV(testPath);
        System.out.println("Loaded " + trainData.length + " training, " + testData.length + " test patients.\n");

        // 30 unique seeds for 30 independent runs
        long[] seeds = {
            33, 42, 57, 99, 123, 200, 314, 404, 500, 612,
            777, 888, 999, 1024, 1337, 2048, 2501, 3000, 3567, 4096,
            4500, 5000, 5678, 6000, 6543, 7000, 7890, 8000, 8765, 9999
        };

        // Store results for all 30 runs
        double[][] results = new double[30][];

        // Print table header
        System.out.println("Run | Seed  | Train%  | Test%   | F-Measure | Runtime");
        System.out.println("----|-------|---------|---------|-----------|--------");

        double bestTestAcc   = -1;
        int    bestRunIndex  = -1;
        double[] bestResult  = null;

        // Do all 30 runs
        for (int i = 0; i < 30; i++) {
            System.out.print("Running run " + (i + 1) + "/30 ... ");
            results[i] = runOnce(seeds[i], trainData, testData);

            double trainAcc = results[i][0];
            double testAcc  = results[i][1];
            double fMeasure = results[i][2];
            double runtime  = results[i][3];
            long   seed     = (long) results[i][4];

            // Print this run's results in table
            System.out.printf("%-3d | %-5d | %-7.2f | %-7.2f | %-9.4f | %.2fs%n",
                (i + 1), seed,
                trainAcc * 100, testAcc * 100,
                fMeasure, runtime);

            // Track best test accuracy across all runs
            if (testAcc > bestTestAcc) {
                bestTestAcc  = testAcc;
                bestRunIndex = i;
                bestResult   = results[i];
            }
        }

        // Print summary
        System.out.println("\n========== BEST RUN SUMMARY ==========");
        System.out.println("Best run        : Run " + (bestRunIndex + 1));
        System.out.println("Best seed       : " + (long) bestResult[4]);
        System.out.printf ("Training Acc    : %.2f%%%n", bestResult[0] * 100);
        System.out.printf ("Test Accuracy   : %.2f%%%n", bestResult[1] * 100);
        System.out.printf ("F-Measure       : %.4f%n",   bestResult[2]);
        System.out.printf ("Runtime         : %.2fs%n",  bestResult[3]);

        // Show best tree using the best seed
        System.out.println("\n========== BEST TREE ==========");
        rng = new Random((long) bestResult[4]);
        Node[] pop = buildInitialPopulation();
        Node bestTree = evolve(pop, trainData);
        printTree(bestTree, "");
    }
}