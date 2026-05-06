package bots;

import java.util.Random;

import io.CourseInputModuleStorage;
import model.GolfSimulator;
import model.ShotResult;

public class Hill_Climbing_Bot implements GolfBot {
    /*
     * Hill Climbing algorithm.
     * For more information you can read about it here:
     * https://en.wikipedia.org/wiki/Hill_climbing_algorithm
     */

    private final double dt;
    private final double maxTime;
    private final String solverType;
    private final Random random;

    public Hill_Climbing_Bot(double dt, double maxTime, String solverType) {
        this.dt = dt;
        this.maxTime = maxTime;
        this.solverType = solverType;
        this.random = new Random();
    }

    @Override
    public double[] computeShot(double[] currentPosition, CourseInputModuleStorage course) {
        GolfSimulator simulator = new GolfSimulator(course, solverType, dt, maxTime);

        double[] target = course.getTargetPosition();
        double dx = target[0] - currentPosition[0];
        double dy = target[1] - currentPosition[1];
        double baseAngle = Math.atan2(dy, dx);
        // this is the angle from the ball to the target, this is where the search will
        // begin
        // atan2 is calculated with arctan formula, it is angle of the line connecting
        // the ball and the target.

        double currentSpeed = 5.0; // max speed from the manual, we will start with a power shot and adjust from
                                   // there
        double vx = currentSpeed * Math.cos(baseAngle); //
        double vy = currentSpeed * Math.sin(baseAngle);

        double bestScore = evaluateShot(simulator, currentPosition, course, vx, vy);
        double stepSize = 0.5;
        double minStepSize = 0.01;
        int maxIterations = 1000;
        int iterations = 0;

        while (stepSize > minStepSize && iterations < maxIterations) {
            boolean improved = false;

            double[][] neighbors = { // we are checking the 4 neighbors, we'll take the shot with the best score ,
                                     // repeat until we can't find a better shot
                    { vx + stepSize, vy },
                    { vx - stepSize, vy },
                    { vx, vy + stepSize },
                    { vx, vy - stepSize }
            };

            for (double[] neighbor : neighbors) {
                double score = evaluateShot(simulator, currentPosition, course, neighbor[0], neighbor[1]);
                if (score < bestScore) {
                    bestScore = score;
                    vx = neighbor[0];
                    vy = neighbor[1];
                    improved = true;
                }
                if (bestScore <= course.getTargetRadius()) {
                    return new double[] { vx, vy };
                }
            }

            if (!improved) { // if we cannot find a better shot, well reduce the step size by half and search
                             // more in detail
                stepSize *= 0.5;
            }
            iterations++;
        }

        return new double[] { vx, vy };
    }

    private double evaluateShot(GolfSimulator simulator, double[] currentPosition, CourseInputModuleStorage course, double vx,
            double vy) { // we get the scores for shots from this func, the lower the score the better.
        try {
            ShotResult result = simulator.simulate(currentPosition, new double[] { vx, vy });

            if (result.getOutcome() == ShotResult.Outcome.IN_WATER) {
                return 1000.0;
            }
            if (result.getOutcome() == ShotResult.Outcome.TIMEOUT) {
                return 500.0;
            }

            double[] finalPos = result.getFinalState();
            double[] target = course.getTargetPosition();
            double dx = finalPos[0] - target[0];
            double dy = finalPos[1] - target[1];
            return Math.sqrt(dx * dx + dy * dy);
        } catch (Exception e) {
            return 1000.0;
        }
    }
}
