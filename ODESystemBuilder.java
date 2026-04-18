 import java.util.Map;

public class ODESystemBuilder implements ODEFunction {

    private String[] equations;
    private String[] names;
    private Map<String, Double> constants;
    private ExpressionParser parser;

    public ODESystemBuilder(String[] equations, String[] names, Map<String, Double> constants) {
        this.equations = equations;
        this.names = names;
        this.constants = constants;
        this.parser = new ExpressionParser();
    }

    @Override
    public double[] compute(double[] stateRightNow) {
        double[] derivatives = new double[equations.length];
        for (int i = 0; i < equations.length; i++) {
            derivatives[i] = parser.evaluate(equations[i], names, stateRightNow, constants);
        }
        return derivatives;
    }
}

