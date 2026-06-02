package model;

public class BasinCenterer {

    private static final int SEARCH_STEPS = 15;
    private static final double MAX_SPEED = 5.0;

    public static class Result {
        public final double[] velocity;
        public final double vxLower, vxUpper, vyLower, vyUpper;
        public final int simulationCount;

        Result(double vx, double vy, double vxLower, double vxUpper,
               double vyLower, double vyUpper, int simulationCount) {
            this.velocity = new double[]{vx, vy};
            this.vxLower = vxLower;
            this.vxUpper = vxUpper;
            this.vyLower = vyLower;
            this.vyUpper = vyUpper;
            this.simulationCount = simulationCount;
        }
    }

    public static Result center(GolfSimulator simulator, double[] startPosition, double vx, double vy) {
        int[] count = {0};

        double vxUpper = findEdge(simulator, startPosition, vx, vy, vx, true, true, count);
        double vxLower = findEdge(simulator, startPosition, vx, vy, vx, true, false, count);
        double centerVx = (vxUpper + vxLower) / 2.0;

        double vyUpper = findEdge(simulator, startPosition, centerVx, vy, vy, false, true, count);
        double vyLower = findEdge(simulator, startPosition, centerVx, vy, vy, false, false, count);
        double centerVy = (vyUpper + vyLower) / 2.0;

        return new Result(centerVx, centerVy, vxLower, vxUpper, vyLower, vyUpper, count[0]);
    }

    private static double findEdge(GolfSimulator simulator, double[] startPosition,
                                    double vx, double vy, double knownScore,
                                    boolean vxAxis, boolean positive, int[] callCount) {
        double step = positive ? 0.5 : -0.5;
        double knownMiss = knownScore + step;

        for (int i = 0; i < 20; i++) {
            double testVx = vxAxis ? knownMiss : vx;
            double testVy = vxAxis ? vy : knownMiss;
            callCount[0]++;
            if (!scores(simulator, startPosition, testVx, testVy)) break;
            knownScore = knownMiss;
            knownMiss += step;
        }

        for (int i = 0; i < SEARCH_STEPS; i++) {
            double mid = (knownScore + knownMiss) / 2.0;
            double testVx = vxAxis ? mid : vx;
            double testVy = vxAxis ? vy : mid;
            callCount[0]++;
            if (scores(simulator, startPosition, testVx, testVy)) {
                knownScore = mid;
            } else {
                knownMiss = mid;
            }
        }

        return knownScore;
    }

    private static boolean scores(GolfSimulator simulator, double[] startPosition, double vx, double vy) {
        if (Math.sqrt(vx * vx + vy * vy) > MAX_SPEED) return false;
        try {
            ShotResult result = simulator.simulate(startPosition, new double[]{vx, vy});
            return result.getOutcome() == ShotResult.Outcome.IN_TARGET;
        } catch (Exception e) {
            return false;
        }
    }
}
