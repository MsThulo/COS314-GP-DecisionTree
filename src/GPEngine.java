import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Genetic Programming engine evolving arithmetic-tree classifiers.
 *
 * Components:
 *   - Initialisation : ramped half-and-half over depths [minInitDepth, maxInitDepth]
 *   - Selection      : tournament selection
 *   - Crossover      : subtree crossover (capped at maxOffspringDepth)
 *   - Mutation       : point mutation (per-node probability when applied)
 *   - Elitism        : top-1 carried over each generation
 *   - Fitness        : training-set classification accuracy
 */
public class GPEngine {

    // parameters 

    public int    populationSize    = 200;
    public int    maxGenerations    = 100;
    public int    tournamentSize    = 5;
    public int    minInitDepth      = 2;
    public int    maxInitDepth      = 6;
    public int    maxOffspringDepth = 8;
    public double crossoverRate     = 0.80;
    public double mutationRate      = 0.15;  // probability offspring undergoes point mutation
    public double perNodeMutateRate = 0.10;  // probability of flipping each node when mutation triggered
    public double constMin          = -5.0;
    public double constMax          =  5.0;
    public double constProb         = 0.30;  

    // state

    private final int numFeatures;
    private final Random rng;

    private GPTree[] population;
    private double[] fitness;

    private GPTree bestEver;
    private double  bestEverFitness;

    public GPEngine(int numFeatures, long seed) {
        this.numFeatures = numFeatures;
        this.rng = new Random(seed);
    }

    // main loop

    /**
     * Runs the GP algorithm and returns the best individual found.
     *
     * @param train       training dataset (fitness = accuracy on this)
     * @param verbose     if true, prints a per-generation summary to `out`
     * @param out         where to print
     */
    public GPTree run(Dataset train, boolean verbose, PrintStream out) {
        initializePopulation();
        evaluatePopulation(train);
        trackBest();

        if (verbose) {
            out.println(" Gen | BestFit  AvgFit  Size Depth | Best individual");
            out.println("-----+--------------------------------+" 
                + "-".repeat(40));
            logGeneration(out, 0);
        }

        for (int gen = 1; gen <= maxGenerations; gen++) {
            GPTree[] next = new GPTree[populationSize];

            // Elitism: carry the best individual unchanged
            next[0] = bestEver.copy();

            for (int i = 1; i < populationSize; i++) {
                GPTree parent1 = tournamentSelect();
                GPTree child;
                if (rng.nextDouble() < crossoverRate) {
                    GPTree parent2 = tournamentSelect();
                    child = crossover(parent1, parent2);
                } else {
                    // Reproduction (copy)
                    child = parent1.copy();
                }
                if (rng.nextDouble() < mutationRate) {
                    pointMutate(child);
                }
                next[i] = child;
            }

            population = next;
            evaluatePopulation(train);
            trackBest();

            if (verbose) logGeneration(out, gen);
        }
        return bestEver;
    }

    private void trackBest() {
        int idx = 0;
        for (int i = 1; i < populationSize; i++) {
            if (fitness[i] > fitness[idx]) idx = i;
        }
        if (bestEver == null || fitness[idx] > bestEverFitness) {
            bestEverFitness = fitness[idx];
            bestEver = population[idx].copy();
        }
    }

    private void logGeneration(PrintStream out, int gen) {
        double avg = 0.0;
        for (double f : fitness) avg += f;
        avg /= fitness.length;

        String expr = bestEver.toInfix();
        if (expr.length() > 60) expr = expr.substring(0, 57) + "...";

        out.printf(" %3d | %.4f   %.4f   %3d   %3d | %s%n",
            gen, bestEverFitness, avg, bestEver.size(), bestEver.depth(), expr);
    }

    // evaluation

    private void evaluatePopulation(Dataset train) {
        for (int i = 0; i < populationSize; i++) {
            fitness[i] = computeFitness(population[i], train);
        }
    }

    public static double computeFitness(GPTree tree, Dataset train) {
        int correct = 0;
        for (int i = 0; i < train.numInstances; i++) {
            int pred = tree.classify(train.X[i]);
            if (pred == train.y[i]) correct++;
        }
        return (double) correct / train.numInstances;
    }

    // initialisation (ramped half-and-half)

    private void initializePopulation() {
        population = new GPTree[populationSize];
        fitness    = new double[populationSize];

        int depthLevels = maxInitDepth - minInitDepth + 1;  
        int idx = 0;

        for (int d = minInitDepth; d <= maxInitDepth; d++) {
            int slotsThisLevel = populationSize / depthLevels;
            if (d == maxInitDepth) {
                // last depth level absorbs any rounding remainder
                slotsThisLevel = populationSize - idx;
            }
            int halfFull = slotsThisLevel / 2;
            int halfGrow = slotsThisLevel - halfFull;
            for (int k = 0; k < halfFull; k++) {
                population[idx++] = new GPTree(buildFull(d));
            }
            for (int k = 0; k < halfGrow; k++) {
                population[idx++] = new GPTree(buildGrow(d));
            }
        }
    }

   
    private Node buildFull(int depth) {
        if (depth == 0) return randomTerminal();
        int fid = rng.nextInt(Node.NUM_FUNCTIONS);
        return Node.func(fid, buildFull(depth - 1), buildFull(depth - 1));
    }

    /** Grow method: branches may stop early with a terminal. */
    private Node buildGrow(int depth) {
        if (depth == 0) return randomTerminal();
        // roughly 50/50 between function and terminal at intermediate depths
        if (rng.nextDouble() < 0.5) {
            return randomTerminal();
        }
        int fid = rng.nextInt(Node.NUM_FUNCTIONS);
        return Node.func(fid, buildGrow(depth - 1), buildGrow(depth - 1));
    }

    private Node randomTerminal() {
        if (rng.nextDouble() < constProb) {
            double v = constMin + rng.nextDouble() * (constMax - constMin);
            return Node.constant(v);
        }
        return Node.feature(rng.nextInt(numFeatures));
    }

    // selection

    private GPTree tournamentSelect() {
        int best = rng.nextInt(populationSize);
        for (int i = 1; i < tournamentSize; i++) {
            int cand = rng.nextInt(populationSize);
            if (fitness[cand] > fitness[best]) best = cand;
        }
        return population[best];
    }

   
    public GPTree crossover(GPTree p1, GPTree p2) {
        GPTree child = p1.copy();
        Node donor = pickRandomNode(p2.root).copy();

        // pick a node in `child` and replace it with the donor subtree
        List<NodeRef> refs = new ArrayList<>();
        collectRefs(child.root, null, -1, refs);
        NodeRef target = refs.get(rng.nextInt(refs.size()));

        if (target.parent == null) {
            child.root = donor;
        } else {
            if (target.childIdx == 0) target.parent.left = donor;
            else target.parent.right = donor;
        }

        if (child.depth() > maxOffspringDepth) {
            return p1.copy();   // bloat control: discard and fall back
        }
        return child;
    }

    //Point mutation
    public void pointMutate(GPTree t) {
        pointMutateRec(t.root);
    }

    private void pointMutateRec(Node n) {
        if (rng.nextDouble() < perNodeMutateRate) {
            mutateNodeInPlace(n);
        }
        if (!n.isTerminal) {
            pointMutateRec(n.left);
            pointMutateRec(n.right);
        }
    }

    private void mutateNodeInPlace(Node n) {
        if (n.isTerminal) {
            Node fresh = randomTerminal();
            n.isConstant    = fresh.isConstant;
            n.featureIndex  = fresh.featureIndex;
            n.constantValue = fresh.constantValue;
        } else {
            n.functionId = rng.nextInt(Node.NUM_FUNCTIONS);
        }
    }

    // node reference helpers (used by crossover)

    private static class NodeRef {
        Node parent;   
        int  childIdx; 
        Node node;
        NodeRef(Node p, int idx, Node n) { parent = p; childIdx = idx; node = n; }
    }

    private static void collectRefs(Node n, Node parent, int idx, List<NodeRef> out) {
        out.add(new NodeRef(parent, idx, n));
        if (!n.isTerminal) {
            collectRefs(n.left, n, 0, out);
            collectRefs(n.right, n, 1, out);
        }
    }

    private Node pickRandomNode(Node root) {
        List<NodeRef> refs = new ArrayList<>();
        collectRefs(root, null, -1, refs);
        return refs.get(rng.nextInt(refs.size())).node;
    }

    //accessors

    public double getBestFitness() { return bestEverFitness; }
    public GPTree getBest()         { return bestEver; }
}
