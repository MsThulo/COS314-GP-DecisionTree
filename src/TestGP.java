import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Scanner;

//Testing/classification driver for the Arithmetic GP classifier
public class TestGP {

    public static void main(String[] args) throws IOException {
        Scanner sc = new Scanner(System.in);

        
        System.out.println(" COS314 Assignment 3 - Arithmetic GP Classifier (test)  ");
        

        String modelPath = readPath(sc,
            "Model file path                      : ", "best_model.txt");
        String testPath  = readPath(sc,
            "Test CSV path                        : ", "Breast_test.csv");
        String trainPath = readPath(sc,
            "Training CSV path (for train accuracy, or blank): ", "");

        // Load model
        ModelInfo info = loadModel(modelPath);
        System.out.println();
        System.out.println("Loaded model:");
        System.out.printf("  trained with seed = %d (run %d of 30)%n", info.seed, info.run);
        System.out.printf("  size = %d nodes, depth = %d%n", info.tree.size(), info.tree.depth());
        System.out.println("  expression: " + info.tree.toInfix());
        System.out.println();

        // Load test data
        Dataset test = Dataset.load(testPath);
        System.out.println("Loaded test data:");
        System.out.println(test.summary());
        System.out.println();

        // Classify
        long t0 = System.nanoTime();
        int[] pred = new int[test.numInstances];
        for (int i = 0; i < test.numInstances; i++) {
            pred[i] = info.tree.classify(test.X[i]);
        }
        long elapsedNs = System.nanoTime() - t0;

        System.out.println("--- Test set results ---");
        System.out.print(Metrics.formatReport(test.y, pred));
        System.out.printf("  Classification time : %.3f ms%n", elapsedNs / 1e6);

        if (!trainPath.isEmpty()) {
            Dataset train = Dataset.load(trainPath);
            int[] tp = new int[train.numInstances];
            for (int i = 0; i < train.numInstances; i++) {
                tp[i] = info.tree.classify(train.X[i]);
            }
            System.out.println();
            System.out.println("--- Training set results (with same model) ---");
            System.out.print(Metrics.formatReport(train.y, tp));
        }

        // Per-instance predictions saved for inspection
        try (java.io.BufferedWriter w = new java.io.BufferedWriter(
                 new java.io.FileWriter("test_predictions.csv"))) {
            w.write("index,true,predicted");
            w.newLine();
            for (int i = 0; i < test.numInstances; i++) {
                w.write(i + "," + test.y[i] + "," + pred[i]);
                w.newLine();
            }
        }
        System.out.println();
        System.out.println("Per-instance predictions written to: test_predictions.csv");
    }

    // model loading

    static class ModelInfo {
        GPTree tree;
        long seed;
        int run;
    }

    private static ModelInfo loadModel(String path) throws IOException {
        ModelInfo info = new ModelInfo();
        String prefix = null;
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                int eq = line.indexOf('=');
                if (eq < 0) continue;
                String key = line.substring(0, eq).trim();
                String val = line.substring(eq + 1).trim();
                switch (key) {
                    case "seed": info.seed = Long.parseLong(val); break;
                    case "run":  info.run  = Integer.parseInt(val); break;
                    case "tree": prefix = val; break;
                    default: /* ignore other metadata fields */
                }
            }
        }
        if (prefix == null) {
            throw new IOException("Model file does not contain a 'tree=' line: " + path);
        }
        info.tree = GPTree.fromPrefix(prefix);
        return info;
    }

    private static String readPath(Scanner sc, String prompt, String def) {
        System.out.print(prompt);
        String s = sc.nextLine().trim();
        if (s.length() >= 2
            && ((s.startsWith("\"") && s.endsWith("\""))
                || (s.startsWith("'") && s.endsWith("'")))) {
            s = s.substring(1, s.length() - 1);
        }
        return s.isEmpty() ? def : s;
    }
}
