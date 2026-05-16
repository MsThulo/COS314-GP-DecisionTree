
public class Metrics {

    public static double accuracy(int[] yTrue, int[] yPred) {
        int correct = 0;
        for (int i = 0; i < yTrue.length; i++) {
            if (yTrue[i] == yPred[i]) correct++;
        }
        return (double) correct / yTrue.length;
    }

    /** [TP, FP, TN, FN] with class 1 treated as positive. */
    public static int[] confusion(int[] yTrue, int[] yPred) {
        int tp = 0, fp = 0, tn = 0, fn = 0;
        for (int i = 0; i < yTrue.length; i++) {
            if (yPred[i] == 1 && yTrue[i] == 1) tp++;
            else if (yPred[i] == 1 && yTrue[i] == 0) fp++;
            else if (yPred[i] == 0 && yTrue[i] == 0) tn++;
            else fn++;
        }
        return new int[]{tp, fp, tn, fn};
    }

    /** F1 score for the positive class (class 1). */
    public static double f1Positive(int[] yTrue, int[] yPred) {
        int[] c = confusion(yTrue, yPred);
        int tp = c[0], fp = c[1], fn = c[3];
        if (tp == 0) return 0.0;
        double precision = (double) tp / (tp + fp);
        double recall    = (double) tp / (tp + fn);
        if (precision + recall == 0) return 0.0;
        return 2.0 * precision * recall / (precision + recall);
    }

    /** F1 averaged across both classes (macro-F1). */
    public static double f1Macro(int[] yTrue, int[] yPred) {
        double f1pos = f1Positive(yTrue, yPred);

        // F1 for the negative class: swap roles
        int[] yTrueFlip = new int[yTrue.length];
        int[] yPredFlip = new int[yPred.length];
        for (int i = 0; i < yTrue.length; i++) {
            yTrueFlip[i] = 1 - yTrue[i];
            yPredFlip[i] = 1 - yPred[i];
        }
        double f1neg = f1Positive(yTrueFlip, yPredFlip);
        return (f1pos + f1neg) / 2.0;
    }

    public static String formatReport(int[] yTrue, int[] yPred) {
        int[] c = confusion(yTrue, yPred);
        double acc  = accuracy(yTrue, yPred);
        double f1p  = f1Positive(yTrue, yPred);
        double f1m  = f1Macro(yTrue, yPred);
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("  Accuracy        : %.4f%n", acc));
        sb.append(String.format("  F1 (class 1)    : %.4f%n", f1p));
        sb.append(String.format("  F1 (macro avg)  : %.4f%n", f1m));
        sb.append(String.format("  Confusion matrix (rows=true, cols=pred):%n"));
        sb.append(String.format("                pred=0    pred=1%n"));
        sb.append(String.format("    true=0   %7d   %7d%n", c[2], c[1]));
        sb.append(String.format("    true=1   %7d   %7d%n", c[3], c[0]));
        return sb.toString();
    }
}
