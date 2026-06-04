import bots.*;
import io.CourseInputModuleStorage;
import model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Headless experiment runner — no GUI required.
 * Runs N shots for every combination of (bot × noise mode × robust on/off),
 * then prints one CSV row per shot to stdout.
 *
 * Usage:
 *   Redirect stdout to a file:  java ExperimentRunner > results.csv
 *   Then load in Python:        df = pd.read_csv("results.csv")
 */
public class ExperimentRunner {

    // ── Experiment settings ────────────────────────────────────────────────
    private static final int    RUNS_PER_CONFIG = 50;
    private static final double DT              = 0.05;  // pipeline dt (fast)
    private static final double REAL_DT         = 0.01;  // actual shot dt (accurate)
    private static final double MAX_TIME        = 20.0;
    private static final String SOLVER          = "rk4";
    private static final long   RNG_SEED        = 42;
    // ───────────────────────────────────────────────────────────────────────

    public static void main(String[] args) {
        CourseInputModuleStorage course = buildCourse();
        double[] startPos = {course.startX, course.startY};
        Random rng = new Random(RNG_SEED);

        // CSV header
        System.out.println("bot,noise_mode,robust,run," +
                           "raw_vx,raw_vy," +
                           "centered_vx,centered_vy," +
                           "fired_vx,fired_vy," +
                           "fired_from_x,fired_from_y," +
                           "final_x,final_y," +
                           "dist_to_hole,outcome,hole_in_one");

        String[] botNames  = {"HillClimbing", "NewtonRaphson", "RuleBased"};
        NoiseMode[] modes  = {NoiseMode.NONE, NoiseMode.GAUSSIAN,
                              NoiseMode.SMALL, NoiseMode.MEDIUM, NoiseMode.LARGE};
        boolean[] robustOptions = {false, true};

        for (String botName : botNames) {
            for (NoiseMode mode : modes) {
                for (boolean robust : robustOptions) {
                    runConfig(botName, mode, robust, course, startPos, rng);
                }
            }
        }
    }

    private static void runConfig(String botName, NoiseMode mode, boolean robust,
                                   CourseInputModuleStorage course, double[] startPos,
                                   Random rng) {
        for (int run = 1; run <= RUNS_PER_CONFIG; run++) {
            GolfBot bot = buildBot(botName);

            // Step 1: bot computes ideal shot
            double[] rawVelocity = bot.computeShot(startPos, course);
            double[] velocity    = rawVelocity.clone();

            // Step 2 & 3: robust pipeline (SensitivityRanker + BasinCenterer)
            if (robust) {
                GolfSimulator scanSim = new GolfSimulator(course, SOLVER, DT, MAX_TIME);
                GolfSimulator realSim = new GolfSimulator(course, SOLVER, REAL_DT, MAX_TIME);
                try {
                    List<SensitivityRanker.Candidate> candidates =
                            SensitivityRanker.rankCandidates(scanSim, startPos,
                                                              velocity[0], velocity[1],
                                                              course.getTargetPosition());
                    if (!candidates.isEmpty()) {
                        SensitivityRanker.Candidate best = candidates.get(0);
                        velocity = new double[]{best.vx, best.vy};
                    }
                    ShotResult test = realSim.simulate(startPos, velocity);
                    if (test.getOutcome() == ShotResult.Outcome.IN_TARGET) {
                        BasinCenterer.Result basin = BasinCenterer.center(
                                realSim, startPos, velocity[0], velocity[1], mode);
                        ShotResult verify = realSim.simulate(startPos, basin.velocity);
                        if (verify.getOutcome() == ShotResult.Outcome.IN_TARGET) {
                            velocity = basin.velocity;
                        }
                    }
                } catch (Exception ignored) {}
            }

            double[] centeredVelocity = velocity.clone();

            // Step 4: apply noise
            double[] firedFrom     = ShotNoise.applyToPosition(startPos, mode, rng);
            double[] firedVelocity = ShotNoise.applyToVelocity(velocity, mode, rng);

            // Step 5: simulate actual shot
            GolfSimulator realSim = new GolfSimulator(course, SOLVER, REAL_DT, MAX_TIME);
            ShotResult result = realSim.simulate(firedFrom, firedVelocity);

            double[] target = course.getTargetPosition();
            double dist = Math.sqrt(Math.pow(result.getFinalX() - target[0], 2)
                                  + Math.pow(result.getFinalY() - target[1], 2));
            boolean hit = result.getOutcome() == ShotResult.Outcome.IN_TARGET;

            System.out.printf("%s,%s,%b,%d," +
                              "%.6f,%.6f," +
                              "%.6f,%.6f," +
                              "%.6f,%.6f," +
                              "%.6f,%.6f," +
                              "%.6f,%.6f," +
                              "%.6f,%s,%d%n",
                    botName, mode, robust, run,
                    rawVelocity[0], rawVelocity[1],
                    centeredVelocity[0], centeredVelocity[1],
                    firedVelocity[0], firedVelocity[1],
                    firedFrom[0], firedFrom[1],
                    result.getFinalX(), result.getFinalY(),
                    dist, result.getOutcome(), hit ? 1 : 0);
        }
    }

    private static GolfBot buildBot(String name) {
        switch (name) {
            case "HillClimbing":  return new Hill_Climbing_Bot(REAL_DT, MAX_TIME, SOLVER);
            case "NewtonRaphson": return new Newton_Raphson_Bot(REAL_DT, MAX_TIME, SOLVER);
            default:              return new RuleBasedBot(REAL_DT, MAX_TIME);
        }
    }

    private static CourseInputModuleStorage buildCourse() {
        HeightFunction h = new HeightFunction("0.35 * sin(x/5) + 0.25 * cos(y/5) + 1.5");
        return new CourseInputModuleStorage(
                h,
                0.08,   // muK
                0.2,    // muS
                7.0,    // startX
                8.0,    // startY
                14.0,   // targetX
                1.0,    // targetY
                0.4,    // targetRadius (wider = wider basin for meaningful experiment)
                0.01    // stepSize
        );
    }
}
