
public class GPTree {

    public Node root;

    public GPTree(Node root) {
        this.root = root;
    }

    //evaluation

    public double eval(double[] features) {
        return evalRec(root, features);
    }

    private static double evalRec(Node n, double[] x) {
        if (n.isTerminal) {
            return n.isConstant ? n.constantValue : x[n.featureIndex];
        }
        double a = evalRec(n.left, x);
        double b = evalRec(n.right, x);
        switch (n.functionId) {
            case Node.FUNC_ADD: return a + b;
            case Node.FUNC_SUB: return a - b;
            case Node.FUNC_MUL: return a * b;
            case Node.FUNC_DIV:
                // Protected division: if divisor is too small, return 1.0
                if (Math.abs(b) < 1e-6) return 1.0;
                return a / b;
            default:
                throw new IllegalStateException("Unknown function id: " + n.functionId);
        }
    }

    public int classify(double[] features) {
        return eval(features) > 0.0 ? 1 : 0;
    }

    // structural

    public int size()  { return root.size(); }
    public int depth() { return root.depth(); }

    public GPTree copy() {
        return new GPTree(root.copy());
    }

    

    //human readable infix notation
    public String toInfix() {
        return toInfixRec(root);
    }

    private static String toInfixRec(Node n) {
        if (n.isTerminal) {
            if (n.isConstant) return formatConst(n.constantValue);
            return "x" + n.featureIndex;
        }
        return "(" + toInfixRec(n.left) + " "
                + Node.funcSymbol(n.functionId) + " "
                + toInfixRec(n.right) + ")";
    }

    private static String formatConst(double v) {
        if (v == Math.floor(v) && !Double.isInfinite(v)) {
            return String.format("%.1f", v);
        }
        return String.format("%.3f", v);
    }

    // prefix serialization (for save / load) 

    public String toPrefix() {
        StringBuilder sb = new StringBuilder();
        toPrefixRec(root, sb);
        return sb.toString();
    }

    private static void toPrefixRec(Node n, StringBuilder sb) {
        if (n.isTerminal) {
            if (n.isConstant) {
                sb.append("C:").append(n.constantValue);
            } else {
                sb.append("X:").append(n.featureIndex);
            }
        } else {
            sb.append("F:").append(n.functionId).append("(");
            toPrefixRec(n.left, sb);
            sb.append(",");
            toPrefixRec(n.right, sb);
            sb.append(")");
        }
    }

    public static GPTree fromPrefix(String s) {
        int[] pos = {0};
        Node root = parsePrefix(s, pos);
        return new GPTree(root);
    }

    private static Node parsePrefix(String s, int[] pos) {
        if (s.startsWith("F:", pos[0])) {
            pos[0] += 2;
            int fid = readInt(s, pos);
            expect(s, pos, '(');
            Node left = parsePrefix(s, pos);
            expect(s, pos, ',');
            Node right = parsePrefix(s, pos);
            expect(s, pos, ')');
            return Node.func(fid, left, right);
        } else if (s.startsWith("X:", pos[0])) {
            pos[0] += 2;
            int idx = readInt(s, pos);
            return Node.feature(idx);
        } else if (s.startsWith("C:", pos[0])) {
            pos[0] += 2;
            double v = readDouble(s, pos);
            return Node.constant(v);
        } else {
            throw new IllegalArgumentException(
                "Unexpected token at " + pos[0] + " in: " + s);
        }
    }

    private static int readInt(String s, int[] pos) {
        int start = pos[0];
        while (pos[0] < s.length()) {
            char c = s.charAt(pos[0]);
            if (c == '-' || (c >= '0' && c <= '9')) pos[0]++;
            else break;
        }
        return Integer.parseInt(s.substring(start, pos[0]));
    }

    private static double readDouble(String s, int[] pos) {
        int start = pos[0];
        while (pos[0] < s.length()) {
            char c = s.charAt(pos[0]);
            if (c == '-' || c == '+' || c == '.' || c == 'e' || c == 'E'
                    || (c >= '0' && c <= '9')) {
                pos[0]++;
            } else break;
        }
        return Double.parseDouble(s.substring(start, pos[0]));
    }

    private static void expect(String s, int[] pos, char ch) {
        if (pos[0] >= s.length() || s.charAt(pos[0]) != ch) {
            throw new IllegalArgumentException(
                "Expected '" + ch + "' at " + pos[0] + " in: " + s);
        }
        pos[0]++;
    }
}
