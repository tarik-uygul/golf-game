package bots;

import io.CourseInputModuleStorage;
import model.GolfSimulator;
import model.ShotResult;

public class Hill_Climbing_Bot implements GolfBot {

    private static final double BOT_DT = 0.01;
    private static final double BOT_MAX_TIME = 20.0;

    private final double dt;
    private final double maxTime;
    private final String solverType;

    public Hill_Climbing_Bot(double dt, double maxTime, String solverType) {
        this.dt = dt;
        this.maxTime = maxTime;
        this.solverType = solverType;
    }

    @Override
    public double[] computeShot(double[] currentPosition, CourseInputModuleStorage course) {
        GolfSimulator simulator = new GolfSimulator(course, solverType, BOT_DT, BOT_MAX_TIME);

        double[] target = course.getTargetPosition();
        double dx = target[0] - currentPosition[0];
        double dy = target[1] - currentPosition[1];
        double baseAngle = Math.atan2(dy, dx);

        double currentSpeed = 5.0;
        double vx = currentSpeed * Math.cos(baseAngle);
        double vy = currentSpeed * Math.sin(baseAngle);

        double bestScore = evaluateShot(simulator, currentPosition, course, vx, vy);
        double bestVx = vx, bestVy = vy;
        double localBestScore = bestScore;
        double stepSize = 0.5;
        int maxRestarts = 50;
        int restarts = 0;

        while (bestScore > course.getTargetRadius()) {
            boolean improved = false;

            double[][] neighbors = {
                    { vx + stepSize, vy },
                    { vx - stepSize, vy },
                    { vx, vy + stepSize },
                    { vx, vy - stepSize }
            };

            for (double[] neighbor : neighbors) {
                double score = evaluateShot(simulator, currentPosition, course, neighbor[0], neighbor[1]);
                if (score < localBestScore) {
                    localBestScore = score;
                    vx = neighbor[0];
                    vy = neighbor[1];
                    improved = true;
                }
                if (localBestScore < bestScore) {
                    bestScore = localBestScore;
                    bestVx = vx;
                    bestVy = vy;
                }
                if (bestScore <= course.getTargetRadius()) {
                    return new double[] { bestVx, bestVy };
                }
            }

            if (!improved) {
                stepSize *= 0.5;
                if (stepSize < 0.01) {
                    if (restarts++ >= maxRestarts) break;
                    double newAngle = Math.random() * 2 * Math.PI;
                    double newSpeed = 0.5 + Math.random() * 4.5;
                    vx = newSpeed * Math.cos(newAngle);
                    vy = newSpeed * Math.sin(newAngle);
                    localBestScore = evaluateShot(simulator, currentPosition, course, vx, vy);
                    if (localBestScore < bestScore) {
                        bestScore = localBestScore;
                        bestVx = vx;
                        bestVy = vy;
                    }
                    stepSize = 0.5;
                }
            }
        }

        return new double[] { bestVx, bestVy };
    }

    private double evaluateShot(GolfSimulator simulator, double[] currentPosition,
                                 CourseInputModuleStorage course, double vx, double vy) {
        try {
            ShotResult result = simulator.simulate(currentPosition, new double[] { vx, vy });
            double[] finalPos = result.getFinalState();
            double[] target = course.getTargetPosition();
            double dx = finalPos[0] - target[0];
            double dy = finalPos[1] - target[1];
            double distance = Math.sqrt(dx * dx + dy * dy);
            if (result.getOutcome() == ShotResult.Outcome.IN_WATER
                    || result.getOutcome() == ShotResult.Outcome.HIT_TREE
                    || result.getOutcome() == ShotResult.Outcome.OUT_OF_BOUNDS) {
                return 1000.0 + distance;
            }
            if (result.getOutcome() == ShotResult.Outcome.TIMEOUT) {
                return 500.0 + distance;
            }
            return distance;
        } catch (Exception e) {
            return 1000.0;
        }
    }
}
