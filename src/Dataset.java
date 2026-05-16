import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


 //Holds a labelled tabular dataset.

public class Dataset {

    public double[][] X;    // shape [numInstances][numFeatures]
    public int[] y;         // length numInstances, values in {0, 1}
    public int numFeatures;
    public int numInstances;
    public String[] featureNames;

    public static Dataset load(String path) throws IOException {
        Dataset d = new Dataset();
        List<double[]> rows = new ArrayList<>();
        List<Integer> labels = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String header = br.readLine();
            if (header == null) {
                throw new IOException("Empty CSV: " + path);
            }
            String[] cols = header.trim().split(",");
            d.numFeatures = cols.length - 1;
            d.featureNames = new String[d.numFeatures];
            System.arraycopy(cols, 1, d.featureNames, 0, d.numFeatures);

            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] parts = line.split(",");
                if (parts.length != cols.length) {
                    throw new IOException("Malformed row (expected "
                        + cols.length + " cols): " + line);
                }
                int label = Integer.parseInt(parts[0].trim());
                double[] features = new double[d.numFeatures];
                for (int i = 0; i < d.numFeatures; i++) {
                    features[i] = Double.parseDouble(parts[i + 1].trim());
                }
                labels.add(label);
                rows.add(features);
            }
        }

        d.numInstances = rows.size();
        d.X = new double[d.numInstances][];
        d.y = new int[d.numInstances];
        for (int i = 0; i < d.numInstances; i++) {
            d.X[i] = rows.get(i);
            d.y[i] = labels.get(i);
        }
        return d;
    }

    public String summary() {
        int pos = 0;
        for (int v : y) if (v == 1) pos++;
        return String.format(
            "  instances = %d, features = %d, class 1 = %d (%.1f%%), class 0 = %d (%.1f%%)",
            numInstances, numFeatures,
            pos, 100.0 * pos / numInstances,
            numInstances - pos, 100.0 * (numInstances - pos) / numInstances);
    }
}
