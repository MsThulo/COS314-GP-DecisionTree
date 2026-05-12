================================================================
COS314 Assignment 3 - Genetic Programming Decision Tree
Student: Rebone Thulo
Student Number -u26850258
Date: 12 May 2026
================================================================

----------------------------------------------------------------
WHAT THIS PROGRAM DOES (Simple Explanation)
----------------------------------------------------------------
This program looks at breast cancer patient details and tries
to guess if their cancer will come back or not.

It does this by evolving a DECISION TREE - like a game of
YES/NO questions:

   Is tumor size <= 5?
   YES → Is deg_malig <= 2?
         YES → NO CANCER
         NO  → CANCER
   NO  → CANCER

The program starts with 200 random trees and makes them smarter
over 100 rounds by mixing the best ones together (like DNA).
After 30 independent runs it finds and shows the best tree.

----------------------------------------------------------------
WHAT YOU NEED TO RUN THIS
----------------------------------------------------------------
1. Java installed (version 17 or higher)
2. The following files in the SAME folder:
   - GPDecisionTree.java
   - Breast_train.csv
   - Breast_test.csv

----------------------------------------------------------------
HOW TO COMPILE
----------------------------------------------------------------
Open terminal in the folder and type:

   javac GPDecisionTree.java

You should see no errors - just a blank line back.

----------------------------------------------------------------
HOW TO RUN
----------------------------------------------------------------
Type this in terminal:

   java GPDecisionTree

The program will then ask you two questions:

   Enter path to TRAINING file: Breast_train.csv
   Enter path to TEST file: Breast_test.csv

That is all! The program will automatically:
   - Run 30 times with 30 different seeds
   - Print results for each run in a table
   - Show the best run at the end
   - Print the best decision tree found

----------------------------------------------------------------
EXPECTED OUTPUT
----------------------------------------------------------------
Run | Seed  | Train%  | Test%   | F-Measure | Runtime
----|-------|---------|---------|-----------|--------
1   | 33    | 83.61   | 52.33   | 0.3492    | 0.51s
2   | 42    | 83.06   | 52.33   | 0.3051    | 0.44s
... (30 runs total)

================================================================
GP PARAMETERS USED
================================================================
Population Size    : 200
Max Generations    : 100
Max Tree Depth     : 4
Crossover Rate     : 80%
Mutation Rate      : 10%
Tournament Size    : 5
Tree Generation    : Ramped Half-and-Half
Mutation Type      : Point Mutation
Fitness Function   : Accuracy

================================================================
BEST RUN RESULTS (from 30 independent runs)
================================================================
Best Seed          : 7000  ← USE THIS SEED AT THE DEMO
Best Run           : Run 26
Training Accuracy  : 83.61%
Test Accuracy      : 66.28%
F-Measure          : 0.5672
Runtime            : 0.58 seconds

================================================================
ALL 30 RUNS RESULTS
================================================================
Run | Seed  | Train%  | Test%   | F-Measure | Runtime
----|-------|---------|---------|-----------|--------
1   | 33    | 83.61   | 52.33   | 0.3492    | 0.51s
2   | 42    | 83.06   | 52.33   | 0.3051    | 0.44s
3   | 57    | 83.06   | 46.51   | 0.4103    | 0.46s
4   | 99    | 83.06   | 47.67   | 0.4304    | 0.46s
5   | 123   | 83.06   | 52.33   | 0.3051    | 0.66s
6   | 200   | 82.51   | 46.51   | 0.4103    | 0.55s
7   | 314   | 82.51   | 46.51   | 0.4250    | 0.49s
8   | 404   | 81.97   | 48.84   | 0.3333    | 0.51s
9   | 500   | 83.61   | 48.84   | 0.4500    | 0.42s
10  | 612   | 82.51   | 47.67   | 0.4578    | 0.48s
11  | 777   | 82.51   | 47.67   | 0.4000    | 0.65s
12  | 888   | 83.06   | 50.00   | 0.4691    | 0.41s
13  | 999   | 83.61   | 56.98   | 0.4127    | 0.53s
14  | 1024  | 83.06   | 46.51   | 0.4103    | 0.51s
15  | 1337  | 83.06   | 50.00   | 0.3944    | 0.45s
16  | 2048  | 82.51   | 39.53   | 0.2571    | 0.52s
17  | 2501  | 82.51   | 58.14   | 0.4375    | 0.51s
18  | 3000  | 83.06   | 54.65   | 0.3158    | 0.52s
19  | 3567  | 84.15   | 52.33   | 0.3692    | 0.52s
20  | 4096  | 83.06   | 52.33   | 0.3492    | 0.49s
21  | 4500  | 83.06   | 48.84   | 0.3529    | 0.54s
22  | 5000  | 83.06   | 46.51   | 0.4103    | 0.54s
23  | 5678  | 82.51   | 46.51   | 0.4103    | 0.48s
24  | 6000  | 83.61   | 63.95   | 0.5507    | 0.59s
25  | 6543  | 82.51   | 46.51   | 0.4103    | 0.48s
26  | 7000  | 83.61   | 66.28   | 0.5672    | 0.58s  BEST RUN
27  | 7890  | 82.51   | 46.51   | 0.4250    | 0.44s
28  | 8000  | 82.51   | 54.65   | 0.3158    | 0.44s
29  | 8765  | 83.06   | 52.33   | 0.4225    | 0.43s
30  | 9999  | 83.61   | 55.81   | 0.3871    | 0.57s

================================================================
HOW TO REPLICATE BEST RESULT AT DEMO
================================================================
1. Open terminal in the GP_DecisionTree folder
2. Type: java GPDecisionTree
3. Training file: Breast_train.csv
4. Test file: Breast_test.csv
5. Program automatically runs 30 times
6. Best result will be Run 26 with seed 7000

================================================================
DATASET INFORMATION
================================================================
Dataset  : Breast Cancer Wisconsin (Diagnostic)
Features : 9 (age, menopause, tumor_size, inv_nodes,
             node_caps, deg_malig, breast, breast_quad, irradiat)
Classes  : 0 = No recurrence, 1 = Recurrence
Training : 183 patients
Testing  : 86 patients

================================================================
