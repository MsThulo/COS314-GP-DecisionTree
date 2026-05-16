//A single node in a GP Tree
public class Node {

    // Function ids
    public static final int FUNC_ADD = 0;
    public static final int FUNC_SUB = 1;
    public static final int FUNC_MUL = 2;
    public static final int FUNC_DIV = 3;
    public static final int NUM_FUNCTIONS = 4;

    public boolean isTerminal;
    public boolean isConstant;       
    public int functionId;           
    public int featureIndex;         
    public double constantValue;     
    public Node left, right;         

    //factories

    public static Node func(int fid, Node left, Node right) {
        Node n = new Node();
        n.isTerminal = false;
        n.functionId = fid;
        n.left = left;
        n.right = right;
        return n;
    }

    public static Node feature(int idx) {
        Node n = new Node();
        n.isTerminal = true;
        n.isConstant = false;
        n.featureIndex = idx;
        return n;
    }

    public static Node constant(double v) {
        Node n = new Node();
        n.isTerminal = true;
        n.isConstant = true;
        n.constantValue = v;
        return n;
    }

    // helpers

    public Node copy() {
        if (isTerminal) {
            return isConstant ? constant(constantValue) : feature(featureIndex);
        }
        return func(functionId, left.copy(), right.copy());
    }

    public int size() {
        if (isTerminal) return 1;
        return 1 + left.size() + right.size();
    }

    public int depth() {
        if (isTerminal) return 0;
        return 1 + Math.max(left.depth(), right.depth());
    }

    public static String funcSymbol(int fid) {
        switch (fid) {
            case FUNC_ADD: return "+";
            case FUNC_SUB: return "-";
            case FUNC_MUL: return "*";
            case FUNC_DIV: return "/";
            default: return "?";
        }
    }
}
