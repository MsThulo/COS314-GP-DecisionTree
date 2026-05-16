# COS314 Assignment 3 — Arithmetic GP Classifier

Symbolic Genetic Programming classifier for the Breast Cancer Wisconsin (Diagnostic) dataset, written in plain Java (no external libraries).

This is the **arithmetic classifier** half of the assignment. The function set is `{+, −, ×, ÷ (protected)}`; each evolved tree outputs a real number, and the sign of that number determines the predicted class: `output > 0 → class 1`, otherwise `class 0`.

---

## Files

```
arithmetic-gp/
├── src/
│   ├── Node.java            -- single GP tree node
│   ├── GPTree.java          -- tree wrapper: eval, infix print, prefix save/load
│   ├── Dataset.java         -- CSV loader
│   ├── Metrics.java         -- accuracy, F1, confusion matrix
│   ├── GPEngine.java        -- the GA loop (init, eval, selection, crossover, mutation)
│   ├── TrainGP.java         -- training driver (30 runs, saves best model)
│   ├── TestGP.java          -- testing driver (loads model, classifies test set)
│   └── TeePrintStream.java  -- mirror stdout to a log file
├── build.sh                 -- one-shot build script (macOS / Linux)
├── arithmetic-gp.jar        -- prebuilt executable JAR (Main-Class: TrainGP)
└── README.md
```

---

## Building

```bash
chmod +x build.sh
./build.sh
```

Or manually:

```bash
mkdir -p out
javac -d out src/*.java
jar cfe arithmetic-gp.jar TrainGP -C out .
```

Requires JDK 8+ (developed against OpenJDK 21).

---

## Running

### 1. Training — `TrainGP`

```bash
java -jar arithmetic-gp.jar
```

You will be prompted for, in order:

1. **Seed value** (long integer). This is the base seed; the 30 runs use seeds `base, base+1, …, base+29`.
2. **Training CSV path**.
3. **Test CSV path** (optional — press Enter to skip per-run test evaluation).
4. **GP parameters** (each with a default in `[…]`; press Enter to accept).

Then it performs **30 independent runs** of 100 generations each, prints per-run summaries, and saves:

| File                  | Contents                                                                   |
|-----------------------|----------------------------------------------------------------------------|
| `best_model.txt`      | The best individual across all 30 runs (in prefix form + metadata).        |
| `arithmetic_runs.csv` | Per-run train/test accuracy & F1 — feed this to the t-test / Wilcoxon.     |
| `training_log.txt`    | Full per-generation trace for run 1 (best individual + metrics each gen).  |

Run 1 also prints its full per-generation trace to stdout (this satisfies the "Training Demonstration" requirement). Runs 2–30 print only a one-line summary each to keep the output readable; their per-generation behaviour is identical, just not displayed.

### 2. Testing — `TestGP`

```bash
java -cp arithmetic-gp.jar TestGP
```

Prompts for:

1. Model file path (default `best_model.txt`).
2. Test CSV path.
3. Training CSV path (optional — reproduces training-set metrics if provided).

Outputs accuracy, F1 (class 1), macro-F1, and the confusion matrix, plus per-instance predictions to `test_predictions.csv`.

---

## Example demo session (reproducible)

```bash
$ java -jar arithmetic-gp.jar
Seed value (long integer)            : 42
Training CSV path                    : Breast_train.csv
Test CSV path (or blank to skip eval): Breast_test.csv
(press Enter through all GP parameter prompts to accept defaults)
```

Followed by:

```bash
$ java -cp arithmetic-gp.jar TestGP
Model file path                      : best_model.txt
Test CSV path                        : Breast_test.csv
Training CSV path (...)              : Breast_train.csv
```

With `seed=42` and default parameters the best run on this dataset is **run 4 (seed=45)** giving:

| Metric               | Training | Test  |
|----------------------|---------:|------:|
| Accuracy             |  0.8634  | 0.5349 |
| F-measure (class 1)  |  0.5455  | 0.4872 |
| F-measure (macro)    |  0.7325  | 0.5308 |

Mean across all 30 runs: train acc 0.8463 (std 0.0119), test acc 0.4826 (std 0.0479).

---

## Design choices (the `dd` parameters)

| Parameter                  | Value                          | Rationale                                                            |
|----------------------------|--------------------------------|----------------------------------------------------------------------|
| Initial tree depth         | 2 – 6 (ramped half-and-half)   | Standard Koza setup; covers small and medium starting trees.         |
| Max offspring depth        | 8                              | Allows growth post-crossover without runaway bloat.                  |
| Selection method           | Tournament                     | Cheap, tunable via tournament size, no fitness scaling needed.       |
| Tournament size            | 5                              | Moderate selection pressure (k=2 is too weak, k=10 too greedy).      |
| Function set               | `{+, −, ×, ÷ (protected)}`    | Pure arithmetic per "Symbolic" requirement.                          |
| Crossover rate             | 80%                            | Standard. Remainder are reproduced (copied).                         |
| Mutation rate              | 15% per offspring              | Probability that an offspring undergoes point mutation.              |
| Mutation type              | Point                          | As mandated. Each visited node flipped with prob 0.10.               |
| Mutation offspring depth   | 8                              | Point mutation preserves depth, so equals max offspring depth.       |
| Fitness function           | Training accuracy              | As mandated.                                                         |
| Elitism                    | top-1 unchanged                | Prevents losing the best individual to bad luck.                     |
| Terminal set               | 9 features + constants in [−5, 5] | Constants help fit numeric thresholds.                          |
| Classification threshold   | `output > 0 → class 1`         | Standard symbolic-GP binary classification rule.                     |

---

## A note on the test results (worth a paragraph in the report)

The training set is **heavily imbalanced** (78.7% class 0, 21.3% class 1) while the test set is **balanced** (50/50). Because the assignment mandates *accuracy* as the fitness function, evolution is rewarded for predicting "class 0" most of the time — a model that always predicts class 0 scores 0.787 on training but only 0.500 on the balanced test set. The evolved trees do better than that majority baseline on training (≈ 0.86) but their bias toward class 0 hurts test accuracy. This is **inherent to the fitness specification**, not a bug; alternatives that respect the spec less strictly would be balanced accuracy, weighted F1, or class-weighted accuracy. This is good material for the critical-analysis section of the report.

---

## For the comparison report

For the statistical significance test (t-test / Wilcoxon) you'll compare the 30 runs of the arithmetic classifier against the 30 runs of the decision-tree GP. `arithmetic_runs.csv` has the per-run metrics needed:

```
run,seed,train_accuracy,train_f1,test_accuracy,test_f1,runtime_ms
1,42,0.857923,0.500000,0.488372,0.435897,1048
...
```

Pair it column-for-column with the equivalent CSV from the decision-tree side and run a paired t-test on `test_accuracy`.
