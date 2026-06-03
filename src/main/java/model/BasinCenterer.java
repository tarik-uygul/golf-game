package model;

public class BasinCenterer {

    private static final int    SEARCH_STEPS      = 8;
    private static final double MAX_SPEED         = 5.0;
    // velocity basin must be at least this wide in both axes before we bother computing the real basin
    private static final double HYBRID_THRESHOLD  = 0.2;

    public static class Result {
        public final double[] velocity;
        // velocity-only basin
        public final double vxLower, vxUpper, vyLower, vyUpper;
        // position-robust basin (NaN when not computed)
        public final double rxLower, rxUpper, ryLower, ryUpper;
        public final int simulationCount;
        public final boolean usedRealBasin;

        Result(double vx, double vy,
               double vxLower, double vxUpper, double vyLower, double vyUpper,
               double rxLower, double rxUpper, double ryLower, double ryUpper,
               int simulationCount, boolean usedRealBasin) {
            this.velocity        = new double[]{vx, vy};
            this.vxLower         = vxLower;
            this.vxUpper         = vxUpper;
            this.vyLower         = vyLower;
            this.vyUpper         = vyUpper;
            this.rxLower         = rxLower;
            this.rxUpper         = rxUpper;
            this.ryLower         = ryLower;
            this.ryUpper         = ryUpper;
            this.simulationCount = simulationCount;
            this.usedRealBasin   = usedRealBasin;
        }
    }

    public static Result center(GolfSimulator simulator, double[] startPosition,
                                 double vx, double vy, NoiseMode noiseMode) {
        int[] count = {0};
        double posNoise = posNoiseRange(noiseMode);

        // --- velocity-only basin ---
        double vxUpper  = findEdge(simulator, startPosition, vx,       vy,       vx, true,  true,  0.0, count);
        double vxLower  = findEdge(simulator, startPosition, vx,       vy,       vx, true,  false, 0.0, count);
        double centerVx = (vxUpper + vxLower) / 2.0;

        double vyUpper  = findEdge(simulator, startPosition, centerVx, vy,       vy, false, true,  0.0, count);
        double vyLower  = findEdge(simulator, startPosition, centerVx, vy,       vy, false, false, 0.0, count);
        double centerVy = (vyUpper + vyLower) / 2.0;

        // --- real (position-robust) basin — always computed when noise is present so it is
        //     always visible in the UI; only USED for firing when vel basin is wide enough ---
        if (posNoise > 0) {
            if (scores(simulator, startPosition, centerVx, centerVy, posNoise, count)) {
                double rxUpper = findEdge(simulator, startPosition, centerVx, centerVy, centerVx, true,  true,  posNoise, count);
                double rxLower = findEdge(simulator, startPosition, centerVx, centerVy, centerVx, true,  false, posNoise, count);
                double realCVx = (rxUpper + rxLower) / 2.0;

                double ryUpper = findEdge(simulator, startPosition, realCVx, centerVy, centerVy, false, true,  posNoise, count);
                double ryLower = findEdge(simulator, startPosition, realCVx, centerVy, centerVy, false, false, posNoise, count);
                double realCVy = (ryUpper + ryLower) / 2.0;

                double rxWidth = rxUpper - rxLower;
                double ryWidth = ryUpper - ryLower;

                // use real basin center only when vel basin is wide enough and real basin is non-degenerate
                boolean useReal = (vxUpper - vxLower) >= HYBRID_THRESHOLD
                               && (vyUpper - vyLower) >= HYBRID_THRESHOLD
                               && rxWidth > 0 && ryWidth > 0;

                return new Result(useReal ? realCVx : centerVx,
                                  useReal ? realCVy : centerVy,
                                  vxLower, vxUpper, vyLower, vyUpper,
                                  rxLower, rxUpper, ryLower, ryUpper,
                                  count[0], useReal);
            } else {
                // center doesn't survive position noise — real basin is empty, show as zero-width
                return new Result(centerVx, centerVy,
                        vxLower, vxUpper, vyLower, vyUpper,
                        centerVx, centerVx, centerVy, centerVy,
                        count[0], false);
            }
        }

        return new Result(centerVx, centerVy,
                vxLower, vxUpper, vyLower, vyUpper,
                Double.NaN, Double.NaN, Double.NaN, Double.NaN,
                count[0], false);
    }

    /** Position half-range matching ShotNoise's values for each mode. */
    private static double posNoiseRange(NoiseMode mode) {
        switch (mode) {
            case GAUSSIAN: return 0.03;  // ~3σ
            case SMALL:    return 0.02;
            case MEDIUM:   return 0.05;
            case LARGE:    return 0.12;
            default:       return 0.0;
        }
    }

    private static double findEdge(GolfSimulator simulator, double[] startPosition,
                                    double vx, double vy, double knownScore,
                                    boolean vxAxis, boolean positive, double posNoise,
                                    int[] callCount) {
        double step      = positive ? 0.5 : -0.5;
        double knownMiss = knownScore + step;

        for (int i = 0; i < 12; i++) {
            double testVx = vxAxis ? knownMiss : vx;
            double testVy = vxAxis ? vy : knownMiss;
            if (!scores(simulator, startPosition, testVx, testVy, posNoise, callCount)) break;
            knownScore = knownMiss;
            knownMiss += step;
        }

        for (int i = 0; i < SEARCH_STEPS; i++) {
            double mid    = (knownScore + knownMiss) / 2.0;
            double testVx = vxAxis ? mid : vx;
            double testVy = vxAxis ? vy : mid;
            if (scores(simulator, startPosition, testVx, testVy, posNoise, callCount)) {
                knownScore = mid;
            } else {
                knownMiss = mid;
            }
        }

        return knownScore;
    }

    /**
     * Returns true if (vx, vy) scores from startPos AND from all 4 axis-aligned
     * position perturbations (when posNoise > 0).
     */
    private static boolean scores(GolfSimulator simulator, double[] startPos,
                                   double vx, double vy, double posNoise, int[] count) {
        if (Math.sqrt(vx * vx + vy * vy) > MAX_SPEED) return false;
        if (!scoreOnce(simulator, startPos, vx, vy, count)) return false;
        if (posNoise > 0) {
            double[][] perturbed = {
                {startPos[0] + posNoise, startPos[1]},
                {startPos[0] - posNoise, startPos[1]},
                {startPos[0], startPos[1] + posNoise},
                {startPos[0], startPos[1] - posNoise}
            };
            for (double[] pos : perturbed) {
                if (!scoreOnce(simulator, pos, vx, vy, count)) return false;
            }
        }
        return true;
    }

    private static boolean scoreOnce(GolfSimulator simulator, double[] startPos,
                                      double vx, double vy, int[] count) {
        count[0]++;
        try {
            return simulator.simulate(startPos, new double[]{vx, vy})
                            .getOutcome() == ShotResult.Outcome.IN_TARGET;
        } catch (Exception e) {
            return false;
        }
    }
}
