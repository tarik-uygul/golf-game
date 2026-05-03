package physics;

import java.util.Map;

/**
 * Builds an ODE system from human-readable equation strings.
 *
 * Implements ODEFunction so it can be passed directly to EulerSolver or
 * RungeKutta4
 * without either solver needing to know what equations are being used.
 *
 * Example:
 * String[] names = {"x", "y"};
 * String[] equations = {"a*x - b*x*y", "d*x*y - g*y"};
 * Map<String,Double> constants = Map.of("a",0.25, "b",0.15, "d",0.10,
 * "g",0.10);
 * ODESystemBuilder ode = new ODESystemBuilder(equations, names, constants);
 *
 * // plug straight into your solvers:
 * double[] next = EulerSolver.step(currentState, dt, ode);
 * double[] next = RungeKutta4.step(currentState, dt, ode);
 */
public class ODESystemBuilder implements ODEFunction {

    private final String[] equations; // e.g. {"a*x - b*x*y", "d*x*y - g*y"}
    private final String[] names; // e.g. {"x", "y"}
    private final Map<String, Double> constants; // e.g. {"a"->0.25, "b"->0.15}
    private final ExpressionParser parser;

    public ODESystemBuilder(String[] equations,
            String[] names,
            Map<String, Double> constants) {
        this.equations = equations;
        this.names = names;
        this.constants = constants;
        this.parser = new ExpressionParser();
    }

    /**
     * Computes the derivatives at the current state.
     * Called by EulerSolver and RungeKutta4 on every step.
     *
     * @param stateRightNow current values of all variables, in the same order as
     *                      names[]
     * @return array of derivatives dx/dt, dy/dt, ...
     */
    @Override
    public double[] compute(double[] stateRightNow) {
        double[] derivatives = new double[equations.length];
        for (int i = 0; i < equations.length; i++) {
            derivatives[i] = parser.evaluate(
                    equations[i], names, stateRightNow, constants);
        }
        return derivatives;
    }

    // Getters - useful for the GUI to display current settings
    public String[] getEquations() {
        return equations;
    }

    public String[] getNames() {
        return names;
    }

    public Map<String, Double> getConstants() {
        return constants;
    }
}
