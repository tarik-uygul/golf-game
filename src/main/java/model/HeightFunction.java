package model;

import java.util.Map;

import physics.ExpressionParser;

/**
 * Represents the terrain height z = h(x, y)
 * Built from a user-provided expression string through CourseInputProcessor
 */
public class HeightFunction {

    private final String expression;
    private final ExpressionParser parser;

    private static final String[] vars = {"x", "y"};
    private static final Map<String, Double> noConstants = Map.of();
    // I doubt height will involve constants, so empty map it is

    public HeightFunction(String expression) {
        this.expression = expression;
        this.parser = new ExpressionParser();
    }

    public double evaluate(double x, double y) {
        return parser.evaluate(expression, vars, new double[]{x, y}, noConstants);
    }

    // Numerical partial derivatives - physics engine needs these for the slope terms
    // An idea I had over quick research
    // Since we can't do the formal definition of a derivative (lim(h -> 0) etc.)
    // We can use very small h and approximate it in both directions
    public double dhdx(double x, double y) {
        double h = 0.00001;
        return (evaluate(x + h, y) - evaluate(x - h, y)) / (2 * h);
    }

    public double dhdy(double x, double y) {
        double h = 0.00001;
        return (evaluate(x, y + h) - evaluate(x, y - h)) / (2 * h);
    }

    public String getExpression() { return expression; }
}