package model;

public class BasinCenterer {

    private static final int SEARCH_STEPS = 15;
    private static final double MAX_SPEED = 5.0;

    public static double[] center(GolfSimulator simulator, double[] startPosition,
                                   double vx, double vy) {
        double centerVx = findCenter(simulator, startPosition, vx, vy, true);
        double centerVy = findCenter(simulator, startPosition, centerVx, vy, false);
        return new double[]{centerVx, centerVy};
    }

    private static double findCenter(GolfSimulator simulator, double[] startPosition,
                                      double vx, double vy, boolean vxAxis) {
        double base = vxAxis ? vx : vy;
        double upper = findEdge(simulator, startPosition, vx, vy, base, vxAxis, true);
        double lower = findEdge(simulator, startPosition, vx, vy, base, vxAxis, false);
        return (upper + lower) / 2.0;
    }

    private static double findEdge(GolfSimulator simulator, double[] startPosition,
                                    double vx, double vy, double knownScore,
                                    boolean vxAxis, boolean positive) {
        double step = positive ? 0.5 : -0.5;
        double knownMiss = knownScore + step;

        // step outward until we find a shot that misses
        for (int i = 0; i < 20; i++) {
            double testVx = vxAxis ? knownMiss : vx;
            double testVy = vxAxis ? vy : knownMiss;
            if (!scores(simulator, startPosition, testVx, testVy)) break;
            knownScore = knownMiss;
            knownMiss += step;
        }

        // binary search between the last known score and the first known miss
        for (int i = 0; i < SEARCH_STEPS; i++) {
            double mid = (knownScore + knownMiss) / 2.0;
            double testVx = vxAxis ? mid : vx;
            double testVy = vxAxis ? vy : mid;
            if (scores(simulator, startPosition, testVx, testVy)) {
                knownScore = mid;
            } else {
                knownMiss = mid;
            }
        }

        return knownScore;
    }

    private static boolean scores(GolfSimulator simulator, double[] startPosition,
                                   double vx, double vy) {
        if (Math.sqrt(vx * vx + vy * vy) > MAX_SPEED) return false;
        try {
            ShotResult result = simulator.simulate(startPosition, new double[]{vx, vy});
            return result.getOutcome() == ShotResult.Outcome.IN_TARGET;
        } catch (Exception e) {
            return false;
        }
    }
}
