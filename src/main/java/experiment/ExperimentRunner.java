package experiment;

import bots.*;
import io.CourseInputModuleStorage;
import model.*;
import ui.FakeEasyCourse;
import ui.FakeMediumCourse;
import ui.FakeHardCourse;

import java.util.Random;

/**
 * Headless experiment runner — no GUI required.
 *
 * Runs N shots for every combination of course × bot × noise_mode × robust,
 * writing one CSV row per shot to stdout.
 *
 * Usage (from project root):
 *   mvn compile exec:java > results.csv
 *   mvn compile exec:java -Dexec.args="200" > results.csv   (200 runs per config)
 *
 * The resulting CSV can be read directly with:
 *   df = pd.read_csv("results.csv")
 */
public class ExperimentRunner {

    private static final int    DEFAULT_RUNS = 100;
    private static final double SCAN_DT      = 0.05;   // coarse dt for SensitivityRanker
    private static final double REAL_DT      = 0.01;   // accurate dt for basin + actual shot
    private static final double MAX_TIME     = 20.0;
    private static final String SOLVER       = "rk4";
    private static final long   RNG_SEED     = 42;

    public static void main(String[] args) throws Exception {
        int runs = DEFAULT_RUNS;
        if (args.length > 0) {
            try { runs = Integer.parseInt(args[0]); }
            catch (NumberFormatException e) { /* keep default */ }
        }

        Random rng = new Random(RNG_SEED);

        System.out.println(
            "course,bot,noise_mode,robust,run," +
            "raw_vx,raw_vy," +
            "centered_vx,centered_vy," +
            "fired_vx,fired_vy," +
            "fired_from_x,fired_from_y," +
            "final_x,final_y," +
            "dist_to_hole,outcome,hole_in_one," +
            "vel_basin_vx_width,vel_basin_vy_width," +
            "real_basin_vx_width,real_basin_vy_width"
        );

        String[]    courses  = {"Easy", "Medium", "Hard"};
        String[]    bots     = {"HillClimbing", "NewtonRaphson", "RuleBased"};
        NoiseMode[] modes    = {NoiseMode.NONE, NoiseMode.GAUSSIAN,
                                NoiseMode.SMALL, NoiseMode.MEDIUM, NoiseMode.LARGE};
        boolean[]   robustOpts = {false, true};

        int total = courses.length * bots.length * modes.length * robustOpts.length * runs;
        System.err.printf("Running %d total shots across %d configs × %d runs each...%n",
                total, courses.length * bots.length * modes.length * robustOpts.length, runs);

        for (String courseName : courses) {
            CourseInputModuleStorage course = buildCourse(courseName);
            double[] startPos = {course.startX, course.startY};
            for (String botName : bots) {
                for (NoiseMode mode : modes) {
                    for (boolean robust : robustOpts) {
                        System.err.printf("  %s / %s / %s / robust=%b%n",
                                courseName, botName, mode, robust);
                        runConfig(courseName, botName, mode, robust, course, startPos, rng, runs);
                    }
                }
            }
        }
    }

    private static void runConfig(String courseName, String botName,
                                   NoiseMode mode, boolean robust,
                                   CourseInputModuleStorage course,
                                   double[] startPos, Random rng, int runs) {
        GolfSimulator scanSim = new GolfSimulator(course, SOLVER, SCAN_DT, MAX_TIME);
        GolfSimulator realSim = new GolfSimulator(course, SOLVER, REAL_DT, MAX_TIME);

        for (int run = 1; run <= runs; run++) {
            GolfBot bot = buildBot(botName);

            // Step 1: bot computes ideal velocity
            double[] rawVelocity = bot.computeShot(startPos, course);
            double[] velocity    = rawVelocity.clone();

            double velBasinVxW = Double.NaN, velBasinVyW = Double.NaN;
            double realBasinVxW = Double.NaN, realBasinVyW = Double.NaN;

            // Step 2-3: robust pipeline
            if (robust) {
                try {
                    // SensitivityRanker: pick lowest-sensitivity candidate
                    var candidates = SensitivityRanker.rankCandidates(
                            scanSim, startPos, velocity[0], velocity[1],
                            course.getTargetPosition());
                    if (!candidates.isEmpty()) {
                        var best = candidates.get(0);
                        velocity = new double[]{best.vx, best.vy};
                    }

                    // BasinCenterer: centre within the basin
                    ShotResult test = realSim.simulate(startPos, velocity);
                    if (test.getOutcome() == ShotResult.Outcome.IN_TARGET) {
                        BasinCenterer.Result basin = BasinCenterer.center(
                                realSim, startPos, velocity[0], velocity[1], mode);

                        velBasinVxW  = basin.vxUpper - basin.vxLower;
                        velBasinVyW  = basin.vyUpper - basin.vyLower;
                        if (!Double.isNaN(basin.rxLower)) {
                            realBasinVxW = basin.rxUpper - basin.rxLower;
                            realBasinVyW = basin.ryUpper - basin.ryLower;
                        }

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
            ShotResult result = realSim.simulate(firedFrom, firedVelocity);

            double[] target = course.getTargetPosition();
            double dist = Math.hypot(result.getFinalX() - target[0],
                                     result.getFinalY() - target[1]);
            int hit = result.getOutcome() == ShotResult.Outcome.IN_TARGET ? 1 : 0;

            System.out.printf(
                "%s,%s,%s,%b,%d," +
                "%.6f,%.6f," +
                "%.6f,%.6f," +
                "%.6f,%.6f," +
                "%.6f,%.6f," +
                "%.6f,%.6f," +
                "%.6f,%s,%d," +
                "%s,%s," +
                "%s,%s%n",
                courseName, botName, mode, robust, run,
                rawVelocity[0], rawVelocity[1],
                centeredVelocity[0], centeredVelocity[1],
                firedVelocity[0], firedVelocity[1],
                firedFrom[0], firedFrom[1],
                result.getFinalX(), result.getFinalY(),
                dist, result.getOutcome(), hit,
                fmt(velBasinVxW),  fmt(velBasinVyW),
                fmt(realBasinVxW), fmt(realBasinVyW)
            );
        }
    }

    private static String fmt(double v) {
        return Double.isNaN(v) ? "" : String.format("%.6f", v);
    }

    private static GolfBot buildBot(String name) {
        switch (name) {
            case "HillClimbing":  return new Hill_Climbing_Bot(REAL_DT, MAX_TIME, SOLVER);
            case "NewtonRaphson": return new Newton_Raphson_Bot(REAL_DT, MAX_TIME, SOLVER);
            default:              return new RuleBasedBot(REAL_DT, MAX_TIME);
        }
    }

    private static CourseInputModuleStorage buildCourse(String name) throws Exception {
        switch (name) {
            case "Easy":   return FakeEasyCourse.build();
            case "Medium": return FakeMediumCourse.build();
            default:       return FakeHardCourse.build();
        }
    }
}
