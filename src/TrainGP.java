import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Scanner;

//Training driver for the Arithmetic GP classifier.
public class TrainGP {

    private static final int NUM_RUNS = 30;

    public static void main(String[] args) throws IOException {
        Scanner sc = new Scanner(System.in);

        
        System.out.println(" COS314 Assignment 3 - Arithmetic GP Classifier (train) ");
        

        long baseSeed = readLong(sc,
            "Seed value (long integer)            : ", 42L);
        String trainPath = readPath(sc,
            "Training CSV path                    : ",
            "Breast_train.csv");
        String testPath = readPath(sc,
            "Test CSV path (or blank to skip eval): ",
            "");

        // Parameters - prompt with defaults
        System.out.println();
        System.out.println("--- GP parameters (press Enter to accept default) ---");
        int popSize     = readInt (sc, "Population size           [200] : ", 200);
        int maxGen      = readInt (sc, "Max generations           [100] : ", 100);
        int tournSize   = readInt (sc, "Tournament size           [  5] : ", 5);
        int minDepth    = readInt (sc, "Min initial tree depth    [  2] : ", 2);
        int maxDepth    = readInt (sc, "Max initial tree depth    [  6] : ", 6);
        int maxOff      = readInt (sc, "Max offspring depth       [  8] : ", 8);
        double crxRate  = readDbl (sc, "Crossover rate            [0.80]: ", 0.80);
        double mutRate  = readDbl (sc, "Mutation rate             [0.15]: ", 0.15);
        double pnodeRate= readDbl (sc, "Per-node mutation prob    [0.10]: ", 0.10);
        System.out.println();

        // Load training data
        Dataset train = Dataset.load(trainPath);
        System.out.println("Loaded training data:");
        System.out.println(train.summary());
        System.out.println();

        Dataset test = null;
        if (!testPath.isEmpty()) {
            test = Dataset.load(testPath);
            System.out.println("Loaded test data:");
            System.out.println(test.summary());
            System.out.println();
        }

        // 30 independent runs
        GPTree overallBest = null;
        double overallBestTrainAcc = -1.0;
        long   overallBestSeed = baseSeed;
        int    overallBestRun  = 0;

        double[] runTrainAcc = new double[NUM_RUNS];
        double[] runTestAcc  = new double[NUM_RUNS];
        double[] runTrainF1  = new double[NUM_RUNS];
        double[] runTestF1   = new double[NUM_RUNS];
        long[]   runSeeds    = new long[NUM_RUNS];
        long[]   runTimes    = new long[NUM_RUNS];

        
        System.out.printf("Starting %d independent runs (base seed = %d)%n",
            NUM_RUNS, baseSeed);
        System.out.println("--------------------------------------------------------");

        PrintStream trainingLog = new PrintStream(
            new java.io.FileOutputStream("training_log.txt"));

        for (int r = 0; r < NUM_RUNS; r++) {
            long seed = baseSeed + r;
            runSeeds[r] = seed;
            long t0 = System.currentTimeMillis();

            GPEngine engine = new GPEngine(train.numFeatures, seed);
            engine.populationSize    = popSize;
            engine.maxGenerations    = maxGen;
            engine.tournamentSize    = tournSize;
            engine.minInitDepth      = minDepth;
            engine.maxInitDepth      = maxDepth;
            engine.maxOffspringDepth = maxOff;
            engine.crossoverRate     = crxRate;
            engine.mutationRate      = mutRate;
            engine.perNodeMutateRate = pnodeRate;

            
            GPTree best;
            if (r == 0) {
                System.out.printf("[Run %2d, seed=%d] FULL per-generation trace:%n", r + 1, seed);
                trainingLog.printf("=== Run %d (seed=%d) full trace ===%n", r + 1, seed);
                // tee output to both stdout and the log file via a multi-stream
                TeePrintStream tee = new TeePrintStream(System.out, trainingLog);
                best = engine.run(train, true, tee);
            } else {
                best = engine.run(train, false, null);
            }

            long elapsed = System.currentTimeMillis() - t0;
            runTimes[r] = elapsed;

            // Evaluate this run's best
            int[] trainPred = predictAll(best, train);
            double trainAcc = Metrics.accuracy(train.y, trainPred);
            double trainF1  = Metrics.f1Positive(train.y, trainPred);
            runTrainAcc[r] = trainAcc;
            runTrainF1[r]  = trainF1;

            if (test != null) {
                int[] testPred = predictAll(best, test);
                runTestAcc[r] = Metrics.accuracy(test.y, testPred);
                runTestF1[r]  = Metrics.f1Positive(test.y, testPred);
            }

            System.out.printf("[Run %2d, seed=%-10d] trainAcc=%.4f  trainF1=%.4f"
                            + (test != null ? "  testAcc=%.4f  testF1=%.4f" : "%s%s")
                            + "  time=%dms%n",
                r + 1, seed, trainAcc, trainF1,
                (test != null ? runTestAcc[r] : 0.0),
                (test != null ? runTestF1[r]  : 0.0),
                elapsed);

            if (trainAcc > overallBestTrainAcc) {
                overallBestTrainAcc = trainAcc;
                overallBest = best.copy();
                overallBestSeed = seed;
                overallBestRun  = r + 1;
            }
        }

        trainingLog.close();
        System.out.println("--------------------------------------------------------");

        // Save the overall best model 
        saveModel("best_model.txt", overallBest, overallBestSeed, overallBestRun,
            popSize, maxGen, tournSize, minDepth, maxDepth, maxOff,
            crxRate, mutRate, pnodeRate, train.numFeatures);

        // Save per-run statistics for the t-test 
        try (BufferedWriter w = new BufferedWriter(new FileWriter("arithmetic_runs.csv"))) {
            w.write("run,seed,train_accuracy,train_f1,test_accuracy,test_f1,runtime_ms");
            w.newLine();
            for (int r = 0; r < NUM_RUNS; r++) {
                w.write(String.format("%d,%d,%.6f,%.6f,%.6f,%.6f,%d",
                    r + 1, runSeeds[r],
                    runTrainAcc[r], runTrainF1[r],
                    runTestAcc[r],  runTestF1[r],
                    runTimes[r]));
                w.newLine();
            }
        }

        // Final report 
        System.out.println();
        System.out.println("=========================== SUMMARY ===========================");
        System.out.printf("Best run         : %d (seed = %d)%n", overallBestRun, overallBestSeed);
        System.out.printf("Best train accuracy: %.4f%n", overallBestTrainAcc);
        System.out.println();
        System.out.println("Best individual (infix):");
        System.out.println("  " + overallBest.toInfix());
        System.out.printf("  size=%d, depth=%d%n", overallBest.size(), overallBest.depth());
        System.out.println();

        // Training-set metrics on best model
        int[] trPred = predictAll(overallBest, train);
        System.out.println("--- Best model on TRAINING set ---");
        System.out.print(Metrics.formatReport(train.y, trPred));

        if (test != null) {
            int[] tePred = predictAll(overallBest, test);
            System.out.println();
            System.out.println("--- Best model on TEST set ---");
            System.out.print(Metrics.formatReport(test.y, tePred));
        }

        System.out.println();
        System.out.println("Outputs written:");
        System.out.println("  best_model.txt        (use with TestGP)");
        System.out.println("  arithmetic_runs.csv   (30-run results for t-test / Wilcoxon)");
        System.out.println("  training_log.txt      (full per-generation trace of run 1)");
        System.out.println();
        System.out.printf("Mean (train acc) over 30 runs: %.4f  (std %.4f)%n",
            mean(runTrainAcc), std(runTrainAcc));
        if (test != null) {
            System.out.printf("Mean (test acc)  over 30 runs: %.4f  (std %.4f)%n",
                mean(runTestAcc), std(runTestAcc));
        }
    }

    // helpers

    private static int[] predictAll(GPTree tree, Dataset d) {
        int[] pred = new int[d.numInstances];
        for (int i = 0; i < d.numInstances; i++) {
            pred[i] = tree.classify(d.X[i]);
        }
        return pred;
    }

    private static void saveModel(String path, GPTree tree, long seed, int runNo,
                                  int popSize, int maxGen, int tournSize,
                                  int minDepth, int maxDepth, int maxOff,
                                  double crxRate, double mutRate, double pnodeRate,
                                  int numFeatures) throws IOException {
        try (BufferedWriter w = new BufferedWriter(new FileWriter(path))) {
            w.write("# Arithmetic GP best model"); w.newLine();
            w.write("# COS314 Assignment 3"); w.newLine();
            w.write("seed=" + seed); w.newLine();
            w.write("run=" + runNo); w.newLine();
            w.write("numFeatures=" + numFeatures); w.newLine();
            w.write("populationSize=" + popSize); w.newLine();
            w.write("maxGenerations=" + maxGen); w.newLine();
            w.write("tournamentSize=" + tournSize); w.newLine();
            w.write("minInitDepth=" + minDepth); w.newLine();
            w.write("maxInitDepth=" + maxDepth); w.newLine();
            w.write("maxOffspringDepth=" + maxOff); w.newLine();
            w.write("crossoverRate=" + crxRate); w.newLine();
            w.write("mutationRate=" + mutRate); w.newLine();
            w.write("perNodeMutateRate=" + pnodeRate); w.newLine();
            w.write("size=" + tree.size()); w.newLine();
            w.write("depth=" + tree.depth()); w.newLine();
            w.write("infix=" + tree.toInfix()); w.newLine();
            w.write("tree=" + tree.toPrefix()); w.newLine();
        }
    }

    private static double mean(double[] xs) {
        double s = 0; for (double v : xs) s += v; return s / xs.length;
    }
    private static double std(double[] xs) {
        double m = mean(xs), s = 0;
        for (double v : xs) s += (v - m) * (v - m);
        return Math.sqrt(s / xs.length);
    }

    private static long readLong(Scanner sc, String prompt, long def) {
        System.out.print(prompt);
        String s = sc.nextLine().trim();
        if (s.isEmpty()) return def;
        try { return Long.parseLong(s); }
        catch (NumberFormatException e) {
            System.out.println("  invalid - using default " + def);
            return def;
        }
    }
    private static int readInt(Scanner sc, String prompt, int def) {
        System.out.print(prompt);
        String s = sc.nextLine().trim();
        if (s.isEmpty()) return def;
        try { return Integer.parseInt(s); }
        catch (NumberFormatException e) {
            System.out.println("  invalid - using default " + def);
            return def;
        }
    }
    private static double readDbl(Scanner sc, String prompt, double def) {
        System.out.print(prompt);
        String s = sc.nextLine().trim();
        if (s.isEmpty()) return def;
        try { return Double.parseDouble(s); }
        catch (NumberFormatException e) {
            System.out.println("  invalid - using default " + def);
            return def;
        }
    }
    private static String readPath(Scanner sc, String prompt, String def) {
        System.out.print(prompt);
        // Use nextLine() so paths with spaces work correctly on macOS
        String s = sc.nextLine().trim();
        // Strip optional wrapping quotes
        if (s.length() >= 2
            && ((s.startsWith("\"") && s.endsWith("\""))
                || (s.startsWith("'") && s.endsWith("'")))) {
            s = s.substring(1, s.length() - 1);
        }
        return s.isEmpty() ? def : s;
    }
}
