package src.test;
// SampleAccuracyTestSuite.java

//
// Full test suite using SolverAccuracyTest framework.
// All ODEs are defined via SolverAccuracyTest factory methods which use
// ODESystemBuilder + ExpressionParser internally - no hardcoded Java math.
//
// COMPILE:
//   javac ODEFunction.java EulerSolver.java RungeKutta4.java ExpressionParser.java
//         ODESystemBuilder.java SolverAccuracyTest.java SampleAccuracyTestSuite.java
// RUN:
//   java SampleAccuracyTestSuite

import physics.ODEFunction;

public class SampleAccuracyTestSuite {

    private static int totalTests = 0;
    private static int passedTests = 0;
    private static int failedTests = 0;

    private static final String GREEN = "\u001B[32m";
    private static final String RED = "\u001B[31m";
    private static final String BLUE = "\u001B[34m";
    private static final String RESET = "\u001B[0m";
    private static final String BOLD = "\u001B[1m";

    // -----------------------------------------------------------------------
    // Main
    // -----------------------------------------------------------------------
    public static void main(String[] args) {
        System.out.println(BOLD + BLUE + "\n" + "═".repeat(70) + RESET);
        System.out.println(BOLD + "ODE Solver Accuracy Test Suite" + RESET);
        System.out.println(BLUE + "═".repeat(70) + RESET + "\n");

        testBasicFunctionality();
        testConvergence();
        testEdgeCases();
        printSummary();
    }

    // -----------------------------------------------------------------------
    // SUITE 1: Basic Functionality
    // -----------------------------------------------------------------------
    private static void testBasicFunctionality() {
        System.out.println(BOLD + "SUITE 1: Basic Functionality Tests" + RESET);
        System.out.println("─".repeat(70));

        testSingleODE(
                "Harmonic Oscillator (t=10)",
                SolverAccuracyTest.harmonicOscillator(),
                new double[] { 1.0, 0.0 },
                10.0, 0.01,
                0.3, 1e-5,
                new double[] { Math.cos(10.0), -Math.sin(10.0) });

        testSingleODE(
                "Harmonic Oscillator (t=20)",
                SolverAccuracyTest.harmonicOscillator(),
                new double[] { 1.0, 0.0 },
                20.0, 0.01,
                0.4, 1e-5,
                new double[] { Math.cos(20.0), -Math.sin(20.0) });

        testDampedSpringStability(0.1, 10.0);
        testLotkaVolterraStability(new double[] { 1.0, 1.0 }, 20.0);

        System.out.println();
    }

    // -----------------------------------------------------------------------
    // SUITE 2: Convergence
    // -----------------------------------------------------------------------
    private static void testConvergence() {
        System.out.println(BOLD + "SUITE 2: Convergence Tests (O(h^p) analysis)" + RESET);
        System.out.println("─".repeat(70));

        testMultiStepConvergence(
                "Euler Convergence (O(h))",
                SolverAccuracyTest.harmonicOscillator(),
                new double[] { 1.0, 0.0 },
                20.0,
                new double[] { 0.2, 0.1, 0.05, 0.02, 0.01 },
                1.5, 1e-4,
                new double[] { Math.cos(20.0), -Math.sin(20.0) });

        testMultiStepConvergence(
                "RK4 Convergence (O(h^4))",
                SolverAccuracyTest.harmonicOscillator(),
                new double[] { 1.0, 0.0 },
                10.0,
                new double[] { 0.1, 0.05, 0.02, 0.01 },
                1.0, 1e-6,
                new double[] { Math.cos(10.0), -Math.sin(10.0) });

        System.out.println();
    }

    // -----------------------------------------------------------------------
    // SUITE 3: Edge Cases
    // -----------------------------------------------------------------------
    private static void testEdgeCases() {
        System.out.println(BOLD + "SUITE 3: Edge Cases and Boundary Conditions" + RESET);
        System.out.println("─".repeat(70));

        testSingleODE(
                "Short Integration (t=1)",
                SolverAccuracyTest.harmonicOscillator(),
                new double[] { 1.0, 0.0 },
                1.0, 0.001,
                0.01, 1e-9,
                new double[] { Math.cos(1.0), -Math.sin(1.0) });

        testSingleODE(
                "Long Integration (t=50)",
                SolverAccuracyTest.harmonicOscillator(),
                new double[] { 1.0, 0.0 },
                50.0, 0.05,
                2.5, 1e-4,
                new double[] { Math.cos(50.0), -Math.sin(50.0) });

        testSingleODE(
                "Harmonic Oscillator (v0=0)",
                SolverAccuracyTest.harmonicOscillator(),
                new double[] { 1.0, 0.0 },
                10.0, 0.01,
                0.3, 1e-5,
                new double[] { Math.cos(10.0), -Math.sin(10.0) });

        System.out.println();
    }

    // -----------------------------------------------------------------------
    // Helper: single test
    // -----------------------------------------------------------------------
    private static void testSingleODE(String name,
            ODEFunction ode,
            double[] initialState,
            double endTime,
            double stepSize,
            double eulerTol,
            double rk4Tol,
            double[] exactSolution) {
        totalTests++;
        SolverAccuracyTest test = new SolverAccuracyTest(name);
        test.runTest(ode, initialState, endTime, stepSize, eulerTol, rk4Tol, exactSolution);
        test.printReport();
        try {
            test.assertResults();
            passedTests++;
            System.out.println();
        } catch (AssertionError e) {
            failedTests++;
            System.err.println(RED + "✗ ASSERTION FAILED: " + e.getMessage() + RESET + "\n");
        }
    }

    // -----------------------------------------------------------------------
    // Helper: convergence test
    // -----------------------------------------------------------------------
    private static void testMultiStepConvergence(String name,
            ODEFunction ode,
            double[] initialState,
            double endTime,
            double[] stepSizes,
            double maxEulerError,
            double maxRk4Error,
            double[] exactSolution) {
        totalTests++;
        SolverAccuracyTest test = new SolverAccuracyTest(name);
        SolverAccuracyTest.MultiStepResult result = test.runMultiStepTest(
                ode, initialState, endTime, stepSizes, maxEulerError, maxRk4Error, exactSolution);
        result.print();
        try {
            result.assertAllPass();
            passedTests++;
            System.out.println();
        } catch (AssertionError e) {
            failedTests++;
            System.err.println(RED + "✗ CONVERGENCE TEST FAILED: " + e.getMessage() + RESET + "\n");
        }
    }

    // -----------------------------------------------------------------------
    // Helper: damped spring stability (no exact solution, just check it doesnt blow
    // up)
    // -----------------------------------------------------------------------
    private static void testDampedSpringStability(double damping, double endTime) {
        totalTests++;
        System.out.println("Test: Damped Spring (c=" + damping + ", t=" + endTime + ")");
        System.out.println("─".repeat(70));

        SolverAccuracyTest runner = new SolverAccuracyTest("Damped Spring");
        double[] state = runner.integrate(
                new double[] { 1.0, 0.0 }, 0.01, endTime,
                SolverAccuracyTest.dampedSpring(damping), "rk4");

        double magnitude = Math.sqrt(state[0] * state[0] + state[1] * state[1]);
        boolean stable = magnitude < 1.5;

        System.out.printf("  Final state: x=%.6f, v=%.6f, ||state||=%.6f%n",
                state[0], state[1], magnitude);

        if (stable) {
            System.out.println(GREEN + "  ✓ Solution stable" + RESET);
            passedTests++;
        } else {
            System.out.println(RED + "  ✗ Solution diverged" + RESET);
            failedTests++;
        }
        System.out.println();
    }

    // -----------------------------------------------------------------------
    // Helper: Lotka-Volterra stability (populations should stay positive and
    // bounded)
    // -----------------------------------------------------------------------
    private static void testLotkaVolterraStability(double[] initialState, double endTime) {
        totalTests++;
        System.out.println("Test: Lotka-Volterra Predator-Prey");
        System.out.println("─".repeat(70));

        SolverAccuracyTest runner = new SolverAccuracyTest("Lotka-Volterra");
        double[] state = runner.integrate(
                initialState, 0.01, endTime,
                SolverAccuracyTest.lotkaVolterra(2, 1, 1, 1), "rk4");

        boolean positive = state[0] > 0 && state[1] > 0;
        boolean bounded = state[0] < 10 && state[1] < 10;

        System.out.printf("  Final state: prey=%.6f, predator=%.6f%n", state[0], state[1]);

        if (positive && bounded) {
            System.out.println(GREEN + "  ✓ Solution stable and bounded" + RESET);
            passedTests++;
        } else {
            System.out.println(RED + "  ✗ Solution unstable or diverged" + RESET);
            failedTests++;
        }
        System.out.println();
    }

    // -----------------------------------------------------------------------
    // Summary
    // -----------------------------------------------------------------------
    private static void printSummary() {
        System.out.println(BLUE + "═".repeat(70) + RESET);
        System.out.println(BOLD + "Test Summary" + RESET);
        System.out.println("─".repeat(70));
        System.out.printf("Total tests:   %d%n", totalTests);
        System.out.printf("Passed:        " + GREEN + "%d" + RESET + "%n", passedTests);
        System.out.printf("Failed:        " + RED + "%d" + RESET + "%n", failedTests);
        System.out.println("─".repeat(70));
        if (failedTests == 0) {
            System.out.println(GREEN + BOLD + "✓ ALL TESTS PASSED" + RESET);
        } else {
            System.out.println(RED + BOLD + "✗ " + failedTests + " TEST(S) FAILED" + RESET);
        }
        System.out.println(BLUE + "═".repeat(70) + RESET + "\n");
    }
}
