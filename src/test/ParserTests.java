package src.test;

import java.util.HashMap;
import java.util.Map;

import physics.ExpressionParser;
import physics.ODESystemBuilder;

/**
 * Tests for ExpressionParser and ODESystemBuilder.
 *
 * Run this to verify everything works before hooking up the real GUI.
 * Each test prints PASS or FAIL with the expected vs actual value.
 */
public class ParserTests {

    static int passed = 0;
    static int failed = 0;

    public static void main(String[] args) {

        System.out.println("=== ExpressionParser Tests ===\n");

        ExpressionParser p = new ExpressionParser();
        String[] noNames = {};
        double[] noState = {};
        Map<String, Double> noConst = new HashMap<>();

        // ---- Basic arithmetic ----
        test("Addition", p.evaluate("3+4", noNames, noState, noConst), 7.0);
        test("Subtraction", p.evaluate("10-3", noNames, noState, noConst), 7.0);
        test("Multiplication", p.evaluate("3*4", noNames, noState, noConst), 12.0);
        test("Division", p.evaluate("10/4", noNames, noState, noConst), 2.5);

        // ---- Operator precedence ----
        test("Precedence *+", p.evaluate("2+3*4", noNames, noState, noConst), 14.0);
        test("Precedence +-*", p.evaluate("10-2*3", noNames, noState, noConst), 4.0);

        // ---- Parentheses ----
        test("Parentheses", p.evaluate("(2+3)*4", noNames, noState, noConst), 20.0);
        test("Nested parens", p.evaluate("((2+3)*4)", noNames, noState, noConst), 20.0);

        // ---- Unary minus ----
        test("Unary minus", p.evaluate("-5", noNames, noState, noConst), -5.0);
        test("Unary minus expr", p.evaluate("-(2+3)", noNames, noState, noConst), -5.0);

        // ---- Exponentiation ----
        test("Exponent x^2", p.evaluate("3^2", noNames, noState, noConst), 9.0);
        test("Exponent x^3", p.evaluate("2^3", noNames, noState, noConst), 8.0);

        // ---- Math functions ----
        test("sin(0)", p.evaluate("sin(0)", noNames, noState, noConst), 0.0);
        test("cos(0)", p.evaluate("cos(0)", noNames, noState, noConst), 1.0);
        test("sqrt(9)", p.evaluate("sqrt(9)", noNames, noState, noConst), 3.0);
        test("abs(-5)", p.evaluate("abs(-5)", noNames, noState, noConst), 5.0);
        test("exp(0)", p.evaluate("exp(0)", noNames, noState, noConst), 1.0);
        test("log(1)", p.evaluate("log(1)", noNames, noState, noConst), 0.0);

        // ---- Variables ----
        String[] xy = { "x", "y" };
        double[] state = { 3.0, 4.0 };
        test("Variable x", p.evaluate("x", xy, state, noConst), 3.0);
        test("Variable y", p.evaluate("y", xy, state, noConst), 4.0);
        test("x+y", p.evaluate("x+y", xy, state, noConst), 7.0);
        test("x*y", p.evaluate("x*y", xy, state, noConst), 12.0);

        // ---- Constants ----
        Map<String, Double> consts = new HashMap<>();
        consts.put("a", 2.0);
        consts.put("b", 3.0);
        test("Constants a*x", p.evaluate("a*x", xy, state, consts), 6.0);
        test("Constants a*x+b", p.evaluate("a*x+b", xy, state, consts), 9.0);

        // ---- Longer variable names (important - the bug your friend's code had) ----
        String[] vxvy = { "vx", "vy", "x", "y" };
        double[] state2 = { 1.0, 2.0, 3.0, 4.0 }; // vx=1, vy=2, x=3, y=4
        test("Long var vx", p.evaluate("vx", vxvy, state2, noConst), 1.0);
        test("Long var vy", p.evaluate("vy", vxvy, state2, noConst), 2.0);
        // this would fail without the length-sorted substitution:
        test("vx+x no confusion", p.evaluate("vx+x", vxvy, state2, noConst), 4.0);

        // ---- Lotka-Volterra equations (from the project manual) ----
        System.out.println("\n=== Lotka-Volterra ODE Test ===\n");
        String[] lvNames = { "x", "y" };
        String[] lvEqs = { "a*x - b*x*y", "d*x*y - g*y" };
        Map<String, Double> lvConsts = new HashMap<>();
        lvConsts.put("a", 0.25);
        lvConsts.put("b", 0.15);
        lvConsts.put("d", 0.10);
        lvConsts.put("g", 0.10);
        double[] lvState = { 20.0, 2.0 };

        ODESystemBuilder lv = new ODESystemBuilder(lvEqs, lvNames, lvConsts);
        double[] deriv = lv.compute(lvState);

        // dx/dt = 0.25*20 - 0.15*20*2 = 5 - 6 = -1.0
        // dy/dt = 0.10*20*2 - 0.10*2 = 4 - 0.2 = 3.8
        test("Lotka-Volterra dx/dt", deriv[0], -1.0);
        test("Lotka-Volterra dy/dt", deriv[1], 3.8);

        // ---- Harmonic Oscillator (used in the accuracy experiment) ----
        System.out.println("\n=== Harmonic Oscillator ODE Test ===\n");
        String[] hoNames = { "x", "v" };
        String[] hoEqs = { "v", "-1*x" }; // x'=v, v'=-x
        ODESystemBuilder ho = new ODESystemBuilder(hoEqs, hoNames, new HashMap<>());
        double[] hoState = { 1.0, 0.0 }; // x=1, v=0
        double[] hoDeriv = ho.compute(hoState);

        // dx/dt = v = 0.0
        // dv/dt = -x = -1.0
        test("Harmonic dx/dt", hoDeriv[0], 0.0);
        test("Harmonic dv/dt", hoDeriv[1], -1.0);

        // ---- Terrain height function from Phase 2 (sin with division) ----
        System.out.println("\n=== Phase 2 Terrain Function Test ===\n");
        // h(x,y) = 0.25*sin((x+y)/10) + 1 at x=7, y=8
        String[] xyNames = { "x", "y" };
        double[] xyState = { 7.0, 8.0 };
        double expected = 0.25 * Math.sin((7.0 + 8.0) / 10.0) + 1.0;
        test("Terrain h(7,8)", p.evaluate("0.25*sin((x+y)/10)+1", xyNames, xyState, noConst), expected);

        // ---- Summary ----
        System.out.println("\n==============================");
        System.out.println("Results: " + passed + " passed, " + failed + " failed");
        System.out.println("==============================");
    }

    // Checks two doubles are equal within a small tolerance
    static void test(String name, double actual, double expected) {
        double tolerance = 1e-9;
        boolean ok = Math.abs(actual - expected) < tolerance;
        if (ok) {
            System.out.printf("  PASS  %-35s  got %.6f%n", name, actual);
            passed++;
        } else {
            System.out.printf("  FAIL  %-35s  expected %.6f  got %.6f%n",
                    name, expected, actual);
            failed++;
        }
    }
}
