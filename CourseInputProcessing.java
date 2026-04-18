import java.util.Map;

/**
 * Usage for now, might look slightly different for a functional gui interface:
 *   CourseInputProcessor p = new CourseInputProcessor();
 *   CourseConfig config = p.buildConfig(
 *       "0.25*sin((x+y)/10)+1",
 *       "0.08", "0.2",
 *       "7.0", "8.0",
 *       "14.0", "1.0",
 *       "0.1", "0.01"
 *   );
 *   Basically all the sanity checks babes
 *   Later this should evaluate strings as inputs from the GUI (text fields?)
 */
public class CourseInputProcessing {

    private final ExpressionParser parser = new ExpressionParser();
    private static final String[] vars = {"x", "y"};
    private static final Map<String, Double> noConstants = Map.of();

    public CourseConfiguration buildConfig(String heightExpr,
                                    String muKStr, String muSStr,
                                    String startXStr, String startYStr,
                                    String targetXStr, String targetYStr,
                                    String radiusStr, String stepSizeStr) {

        double muK = parsePositive(muKStr, "µK");
        double muS = parsePositive(muSStr, "µS");
        double startX = parseDouble(startXStr, "start X");
        double startY = parseDouble(startYStr, "start Y");
        double targetX = parseDouble(targetXStr, "target X");
        double targetY = parseDouble(targetYStr, "target Y");
        double radius = parsePositive(radiusStr, "target radius");
        double stepSize = parsePositive(stepSizeStr, "step size");

        // Constraints from the project manual
        if (muK >= muS)
            throw new IllegalArgumentException(
                    "µS must be greater than µK (got µK=" + muK + ", µS=" + muS + ")");
        if (muK < 0.05 || muK > 0.1)
            throw new IllegalArgumentException(
                    "µK should be between 0.05 and 0.1 for grass (got " + muK + ")");
        if (muS < 0.1 || muS > 0.2)
            throw new IllegalArgumentException(
                    "µS should be between 0.1 and 0.2 for grass (got " + muS + ")");
        if (radius < 0.05 || radius > 0.15)
            throw new IllegalArgumentException(
                    "Target radius should be between 0.05 and 0.15 (got " + radius + ")");

        // Validate height expression by test-evaluating at two points
        String cleanExpr = heightExpr.replace(" ", "");
        try {
            parser.evaluate(cleanExpr, vars, new double[]{0.0, 0.0}, noConstants);
            parser.evaluate(cleanExpr, vars, new double[]{1.0, 1.0}, noConstants);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Invalid height expression: " + e.getMessage());
        }

        return new CourseConfiguration(new HeightFunction(cleanExpr),
                muK, muS, startX, startY, targetX, targetY,
                radius, stepSize);
    }

    // Helpers
    private double parseDouble(String s, String fieldName) {
        try {
            return Double.parseDouble(s.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Invalid value for " + fieldName + ": '" + s + "'");
        }
    }

    private double parsePositive(String s, String fieldName) {
        double val = parseDouble(s, fieldName);
        if (val <= 0)
            throw new IllegalArgumentException(
                    fieldName + " must be positive (got " + val + ")");
        return val;
    }
}