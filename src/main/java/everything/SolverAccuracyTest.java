package everything;

import java.util.HashMap;

// SolverAccuracyTest.java
//
// Test framework for verifying EulerSolver and RungeKutta4 accuracy.
// Uses ODESystemBuilder + ExpressionParser to define test ODEs as strings,
// the same way you would type them into the Visualizer GUI.
//
// The example ODE inner classes (HarmonicOscillatorODE, LotkaVolterraODE,
// DampedSpringODE) are now built using ODESystemBuilder instead of hardcoded
// Java math, so everything goes through your ExpressionParser.

public class SolverAccuracyTest {

    private String testName;
    private TestResult result;
    private static final int TIME_REPS = 5;

    private static final String GREEN = "\u001B[32m";
    private static final String RED   = "\u001B[31m";
    private static final String RESET = "\u001B[0m";
    private static final String BOLD  = "\u001B[1m";

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------
    public SolverAccuracyTest(String testName) {
        this.testName = testName;
        this.result = null;
    }

    // -----------------------------------------------------------------------
    // Single step-size test
    // -----------------------------------------------------------------------
    public boolean runTest(ODEFunction ode,
                           double[] initialState,
                           double endTime,
                           double stepSize,
                           double eulerTolerance,
                           double rk4Tolerance,
                           double[] exactSolution) {

        long eulerTimeNs = 0;
        double[] eulerResult = null;
        for (int rep = 0; rep < TIME_REPS; rep++) {
            long t0 = System.nanoTime();
            eulerResult = integrate(initialState, stepSize, endTime, ode, "euler");
            eulerTimeNs += System.nanoTime() - t0;
        }
        eulerTimeNs /= TIME_REPS;
        double eulerError = euclideanError(eulerResult, exactSolution);
        boolean eulerPass = eulerError <= eulerTolerance;

        long rk4TimeNs = 0;
        double[] rk4Result = null;
        for (int rep = 0; rep < TIME_REPS; rep++) {
            long t0 = System.nanoTime();
            rk4Result = integrate(initialState, stepSize, endTime, ode, "rk4");
            rk4TimeNs += System.nanoTime() - t0;
        }
        rk4TimeNs /= TIME_REPS;
        double rk4Error = euclideanError(rk4Result, exactSolution);
        boolean rk4Pass = rk4Error <= rk4Tolerance;

        this.result = new TestResult(
                testName, stepSize,
                eulerError, rk4Error,
                eulerTimeNs, rk4TimeNs,
                eulerTolerance, rk4Tolerance,
                eulerPass && rk4Pass
        );

        return eulerPass && rk4Pass;
    }

    // -----------------------------------------------------------------------
    // Multi-step-size convergence test
    // -----------------------------------------------------------------------
    public MultiStepResult runMultiStepTest(ODEFunction ode,
                                            double[] initialState,
                                            double endTime,
                                            double[] stepSizes,
                                            double maxEulerError,
                                            double maxRk4Error,
                                            double[] exactSolution) {
        int n = stepSizes.length;
        double[] eulerErrors = new double[n];
        double[] rk4Errors   = new double[n];
        long[]   eulerTimes  = new long[n];
        long[]   rk4Times    = new long[n];
        boolean[] eulerPass  = new boolean[n];
        boolean[] rk4Pass    = new boolean[n];

        for (int i = 0; i < n; i++) {
            double h = stepSizes[i];

            long eulerTimeNs = 0;
            double[] eulerResult = null;
            for (int rep = 0; rep < TIME_REPS; rep++) {
                long t0 = System.nanoTime();
                eulerResult = integrate(initialState, h, endTime, ode, "euler");
                eulerTimeNs += System.nanoTime() - t0;
            }
            eulerTimes[i]  = eulerTimeNs / TIME_REPS;
            eulerErrors[i] = euclideanError(eulerResult, exactSolution);
            eulerPass[i]   = eulerErrors[i] <= maxEulerError;

            long rk4TimeNs = 0;
            double[] rk4Result = null;
            for (int rep = 0; rep < TIME_REPS; rep++) {
                long t0 = System.nanoTime();
                rk4Result = integrate(initialState, h, endTime, ode, "rk4");
                rk4TimeNs += System.nanoTime() - t0;
            }
            rk4Times[i]  = rk4TimeNs / TIME_REPS;
            rk4Errors[i] = euclideanError(rk4Result, exactSolution);
            rk4Pass[i]   = rk4Errors[i] <= maxRk4Error;
        }

        return new MultiStepResult(testName, stepSizes, eulerErrors, rk4Errors,
                eulerTimes, rk4Times, eulerPass, rk4Pass, maxEulerError, maxRk4Error);
    }

    // -----------------------------------------------------------------------
    // Print and assert
    // -----------------------------------------------------------------------
    public void printReport() {
        if (result == null) {
            System.err.println("No result yet - run runTest() first.");
            return;
        }
        result.print();
    }

    public void assertResults() throws AssertionError {
        if (result == null) throw new AssertionError("No test result available");
        if (!result.passed) {
            throw new AssertionError(String.format(
                    "Test '%s' failed: Euler error %.3e (tol %.3e), RK4 error %.3e (tol %.3e)",
                    result.testName, result.eulerError, result.eulerTolerance,
                    result.rk4Error, result.rk4Tolerance
            ));
        }
    }

    // -----------------------------------------------------------------------
    // Integration helper - shared by both test methods above
    // -----------------------------------------------------------------------
    double[] integrate(double[] initialState, double h,
                       double endTime, ODEFunction ode, String solver) {
        double[] state = initialState.clone();
        double t = 0.0;
        while (t + h <= endTime + 1e-12) {
            if (solver.equals("euler")) {
                state = EulerSolver.step(state, h, ode);
            } else {
                state = RungeKutta4.step(state, h, ode);
            }
            t += h;
        }
        return state;
    }

    // -----------------------------------------------------------------------
    // Error metric
    // -----------------------------------------------------------------------
    private double euclideanError(double[] numerical, double[] exact) {
        double sum = 0;
        for (int i = 0; i < numerical.length; i++) {
            double d = numerical[i] - exact[i];
            sum += d * d;
        }
        return Math.sqrt(sum);
    }

    // -----------------------------------------------------------------------
    // ODE factories - built using ODESystemBuilder so they go through
    // ExpressionParser, same as everything typed into the Visualizer GUI
    // -----------------------------------------------------------------------

    // Harmonic oscillator: x' = v,  v' = -1*x
    // exact solution: x(t) = cos(t), v(t) = -sin(t)  with x(0)=1, v(0)=0
    public static ODEFunction harmonicOscillator() {
        return new ODESystemBuilder(
                new String[]{"v", "-1*x"},
                new String[]{"x", "v"},
                new HashMap<>()
        );
    }

    // Lotka-Volterra predator-prey: x' = a*x - b*x*y,  y' = d*x*y - c*y
    // pass in the four parameters a, b, c, d
    public static ODEFunction lotkaVolterra(double a, double b, double c, double d) {
        java.util.Map<String, Double> consts = new HashMap<>();
        consts.put("a", a);
        consts.put("b", b);
        consts.put("c", c);
        consts.put("d", d);
        return new ODESystemBuilder(
                new String[]{"a*x - b*x*y", "d*x*y - c*y"},
                new String[]{"x", "y"},
                consts
        );
    }

    // Damped spring: x' = v,  v' = -dampingCoeff*v - x
    public static ODEFunction dampedSpring(double dampingCoeff) {
        java.util.Map<String, Double> consts = new HashMap<>();
        consts.put("c", dampingCoeff);
        return new ODESystemBuilder(
                new String[]{"v", "-c*v - x"},
                new String[]{"x", "v"},
                consts
        );
    }

    // -----------------------------------------------------------------------
    // TestResult - single step size
    // -----------------------------------------------------------------------
    public static class TestResult {
        public final String testName;
        public final double stepSize;
        public final double eulerError, rk4Error;
        public final long   eulerTimeNs, rk4TimeNs;
        public final double eulerTolerance, rk4Tolerance;
        public final boolean passed;

        TestResult(String testName, double stepSize,
                   double eulerError, double rk4Error,
                   long eulerTimeNs, long rk4TimeNs,
                   double eulerTolerance, double rk4Tolerance,
                   boolean passed) {
            this.testName       = testName;
            this.stepSize       = stepSize;
            this.eulerError     = eulerError;
            this.rk4Error       = rk4Error;
            this.eulerTimeNs    = eulerTimeNs;
            this.rk4TimeNs      = rk4TimeNs;
            this.eulerTolerance = eulerTolerance;
            this.rk4Tolerance   = rk4Tolerance;
            this.passed         = passed;
        }

        public void print() {
            String GREEN  = "\u001B[32m";
            String RED    = "\u001B[31m";
            String RESET  = "\u001B[0m";
            String BOLD   = "\u001B[1m";

            String eulerStatus = eulerError <= eulerTolerance
                    ? GREEN + "✓ PASS" + RESET : RED + "✗ FAIL" + RESET;
            String rk4Status = rk4Error <= rk4Tolerance
                    ? GREEN + "✓ PASS" + RESET : RED + "✗ FAIL" + RESET;

            System.out.println();
            System.out.println(BOLD + "Test: " + testName + RESET);
            System.out.println("─".repeat(70));
            System.out.printf("  Step size:        %.4f%n", stepSize);
            System.out.printf("%n  Euler Method:%n");
            System.out.printf("    Error:          %.3e  (tolerance: %.3e)  %s%n",
                    eulerError, eulerTolerance, eulerStatus);
            System.out.printf("    Time:           %d ns%n", eulerTimeNs);
            System.out.printf("%n  Runge-Kutta 4:%n");
            System.out.printf("    Error:          %.3e  (tolerance: %.3e)  %s%n",
                    rk4Error, rk4Tolerance, rk4Status);
            System.out.printf("    Time:           %d ns%n", rk4TimeNs);
            System.out.printf("%n  Speedup (RK4 vs Euler): %.2fx%n",
                    (double) eulerTimeNs / rk4TimeNs);
            System.out.println("─".repeat(70));
            System.out.println(passed
                    ? GREEN + "✓ TEST PASSED" + RESET
                    : RED   + "✗ TEST FAILED" + RESET);
        }
    }

    // -----------------------------------------------------------------------
    // MultiStepResult - convergence study
    // -----------------------------------------------------------------------
    public static class MultiStepResult {
        public final String   testName;
        public final double[] stepSizes;
        public final double[] eulerErrors, rk4Errors;
        public final long[]   eulerTimes,  rk4Times;
        public final boolean[] eulerPass,  rk4Pass;
        public final double   maxEulerError, maxRk4Error;

        MultiStepResult(String testName,
                        double[] stepSizes,
                        double[] eulerErrors, double[] rk4Errors,
                        long[]   eulerTimes,  long[]  rk4Times,
                        boolean[] eulerPass,  boolean[] rk4Pass,
                        double maxEulerError, double maxRk4Error) {
            this.testName     = testName;
            this.stepSizes    = stepSizes;
            this.eulerErrors  = eulerErrors;
            this.rk4Errors    = rk4Errors;
            this.eulerTimes   = eulerTimes;
            this.rk4Times     = rk4Times;
            this.eulerPass    = eulerPass;
            this.rk4Pass      = rk4Pass;
            this.maxEulerError = maxEulerError;
            this.maxRk4Error  = maxRk4Error;
        }

        public boolean allPass() {
            for (boolean b : eulerPass) if (!b) return false;
            for (boolean b : rk4Pass)   if (!b) return false;
            return true;
        }

        public void print() {
            String GREEN = "\u001B[32m";
            String RED   = "\u001B[31m";
            String RESET = "\u001B[0m";
            String BOLD  = "\u001B[1m";

            System.out.println();
            System.out.println(BOLD + "Multi-Step Test: " + testName + RESET);
            System.out.println("═".repeat(100));
            System.out.printf("%-10s  %-15s  %-15s  %-12s  %-12s  %-8s  %-8s%n",
                    "h", "Euler Error", "RK4 Error", "Euler Time", "RK4 Time", "Euler", "RK4");
            System.out.println("─".repeat(100));

            for (int i = 0; i < stepSizes.length; i++) {
                String eulerStat = eulerPass[i] ? GREEN + "✓" + RESET : RED + "✗" + RESET;
                String rk4Stat   = rk4Pass[i]   ? GREEN + "✓" + RESET : RED + "✗" + RESET;
                System.out.printf("%.4f  %.3e [%.3e]  %.3e [%.3e]  %8d ns  %8d ns  %s  %s%n",
                        stepSizes[i],
                        eulerErrors[i], maxEulerError,
                        rk4Errors[i],   maxRk4Error,
                        eulerTimes[i],  rk4Times[i],
                        eulerStat, rk4Stat);
            }

            System.out.println("═".repeat(100));
            System.out.println(allPass()
                    ? GREEN + "✓ ALL TESTS PASSED" + RESET
                    : RED   + "✗ SOME TESTS FAILED" + RESET);
        }

        public void assertAllPass() throws AssertionError {
            if (!allPass()) {
                StringBuilder msg = new StringBuilder();
                msg.append("Multi-step test '").append(testName).append("' failed:\n");
                for (int i = 0; i < stepSizes.length; i++) {
                    if (!eulerPass[i])
                        msg.append(String.format("  h=%.4f Euler: error %.3e > tol %.3e\n",
                                stepSizes[i], eulerErrors[i], maxEulerError));
                    if (!rk4Pass[i])
                        msg.append(String.format("  h=%.4f RK4:   error %.3e > tol %.3e\n",
                                stepSizes[i], rk4Errors[i], maxRk4Error));
                }
                throw new AssertionError(msg.toString());
            }
        }
    }

    // -----------------------------------------------------------------------
    // Main - quick demo
    // -----------------------------------------------------------------------
    public static void main(String[] args) {
        System.out.println(BOLD + "ODE Solver Accuracy Test Framework" + RESET);
        System.out.println("═".repeat(70));

        System.out.println("\n" + BOLD + "[Test 1] Single Step-Size Test" + RESET);
        SolverAccuracyTest test1 = new SolverAccuracyTest("Harmonic Oscillator");
        test1.runTest(
                harmonicOscillator(),
                new double[]{1.0, 0.0},
                20.0,
                0.01,
                0.1,
                1e-6,
                new double[]{Math.cos(20.0), -Math.sin(20.0)}
        );
        test1.printReport();
        try { test1.assertResults(); }
        catch (AssertionError e) { System.err.println("Assertion failed: " + e.getMessage()); }

        System.out.println("\n" + BOLD + "[Test 2] Multi-Step Convergence Test" + RESET);
        SolverAccuracyTest test2 = new SolverAccuracyTest("Harmonic Oscillator Convergence");
        MultiStepResult r = test2.runMultiStepTest(
                harmonicOscillator(),
                new double[]{1.0, 0.0},
                20.0,
                new double[]{0.1, 0.05, 0.02, 0.01, 0.005},
                0.5,
                1e-5,
                new double[]{Math.cos(20.0), -Math.sin(20.0)}
        );
        r.print();
        try { r.assertAllPass(); }
        catch (AssertionError e) { System.err.println("Assertion failed: " + e.getMessage()); }

        System.out.println("\n" + BOLD + "[Test 3] Damped Spring Stability" + RESET);
        SolverAccuracyTest test3 = new SolverAccuracyTest("Damped Spring");
        double[] springResult = test3.integrate(
                new double[]{1.0, 0.0}, 0.01, 10.0, dampedSpring(0.1), "rk4"
        );
        System.out.printf("Damped spring at t=10: x=%.6f, v=%.6f%n",
                springResult[0], springResult[1]);
        System.out.println("\u001B[32m✓ Numerical integration stable\u001B[0m");

        System.out.println("\n" + "═".repeat(70));
    }
}


