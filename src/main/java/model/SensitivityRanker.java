package model;

import java.util.ArrayList;
import java.util.List;

public class SensitivityRanker {

    private static final double EPSILON                  = 0.01;
    private static final double MAX_SPEED                = 5.0;
    private static final double MIN_SPEED                = 0.3;
    private static final int    ANGLE_STEPS              = 24;  // was 36
    private static final int    SPEED_STEPS              = 8;   // was 10
    private static final int    MAX_SENSITIVITY_CANDIDATES = 5; // only run sensitivity on the most promising ones

    public static class Candidate {
        public final double vx, vy;
        public final double sensitivity; // lower = spongier / more noise-tolerant

        Candidate(double vx, double vy, double sensitivity) {
            this.vx          = vx;
            this.vy          = vy;
            this.sensitivity = sensitivity;
        }
    }

    /**
     * Scans velocity space for hole-in-one candidates, pre-filters to the closest
     * ones to the hole center, then ranks those by sensitivity gradient. Running
     * sensitivity only on the top MAX_SENSITIVITY_CANDIDATES keeps simulation count low
     * while still finding the spongiest trajectory among the most central shots.
     */
    public static List<Candidate> rankCandidates(GolfSimulator simulator,
                                                  double[] startPos,
                                                  double seedVx, double seedVy,
                                                  double[] targetPos) {
        // grid sweep — store {vx, vy, distFromHoleCenter} for each hole-in-one
        List<double[]> valid = new ArrayList<>();

        for (int ai = 0; ai < ANGLE_STEPS; ai++) {
            double angle = -Math.PI + ai * (2.0 * Math.PI / ANGLE_STEPS);
            for (int si = 0; si < SPEED_STEPS; si++) {
                double speed = MIN_SPEED + si * (MAX_SPEED - MIN_SPEED) / (SPEED_STEPS - 1);
                double vx = speed * Math.cos(angle);
                double vy = speed * Math.sin(angle);
                double[] entry = tryCandidate(simulator, startPos, vx, vy, targetPos);
                if (entry != null) valid.add(entry);
            }
        }

        // always include the bot's own shot as a fallback
        double[] seed = tryCandidate(simulator, startPos, seedVx, seedVy, targetPos);
        if (seed != null) valid.add(seed);

        // keep only the top MAX_SENSITIVITY_CANDIDATES closest to the hole center —
        // those are most likely to have wide basins
        valid.sort((a, b) -> Double.compare(a[2], b[2]));
        List<double[]> top = valid.subList(0, Math.min(MAX_SENSITIVITY_CANDIDATES, valid.size()));

        List<Candidate> ranked = new ArrayList<>();
        for (double[] v : top) {
            double sens = computeSensitivity(simulator, startPos, v[0], v[1]);
            ranked.add(new Candidate(v[0], v[1], sens));
        }
        ranked.sort((a, b) -> Double.compare(a.sensitivity, b.sensitivity));
        return ranked;
    }

    // Returns {vx, vy, distFromHoleCenter} if the shot is a hole-in-one, null otherwise.
    private static double[] tryCandidate(GolfSimulator sim, double[] startPos,
                                          double vx, double vy, double[] targetPos) {
        if (Math.sqrt(vx * vx + vy * vy) > MAX_SPEED) return null;
        try {
            ShotResult result = sim.simulate(startPos, new double[]{vx, vy});
            if (result.getOutcome() != ShotResult.Outcome.IN_TARGET) return null;
            double[] landing = result.getFinalState();
            double dist = Math.sqrt(Math.pow(landing[0] - targetPos[0], 2)
                                  + Math.pow(landing[1] - targetPos[1], 2));
            return new double[]{vx, vy, dist};
        } catch (Exception e) {
            return null;
        }
    }

    private static double computeSensitivity(GolfSimulator sim, double[] startPos,
                                              double vx, double vy) {
        try {
            double[] base  = land(sim, startPos, vx,           vy          );
            double[] pertX = land(sim, startPos, vx + EPSILON, vy          );
            double[] pertY = land(sim, startPos, vx,           vy + EPSILON);
            return dist(base, pertX) / EPSILON + dist(base, pertY) / EPSILON;
        } catch (Exception e) {
            return Double.MAX_VALUE;
        }
    }

    private static double[] land(GolfSimulator sim, double[] startPos, double vx, double vy) {
        return sim.simulate(startPos, new double[]{vx, vy}).getFinalState();
    }

    private static double dist(double[] a, double[] b) {
        double dx = a[0] - b[0];
        double dy = a[1] - b[1];
        return Math.sqrt(dx * dx + dy * dy);
    }
}
