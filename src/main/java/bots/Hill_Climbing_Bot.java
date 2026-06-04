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
    private int lastIterationCount = 0;

    public Hill_Climbing_Bot(double dt, double maxTime, String solverType) {
        this.dt = dt;
        this.maxTime = maxTime;
        this.solverType = solverType;
    }

    @Override
    public int getLastIterationCount() { return lastIterationCount; }

    @Override
    public double[] computeShot(double[] currentPosition, CourseInputModuleStorage course) {
        lastIterationCount = 0;
        GolfSimulator simulator = new GolfSimulator(course, solverType, BOT_DT, BOT_MAX_TIME);

        double[] target = course.getTargetPosition();
        double dx = target[0] - currentPosition[0];
        double dy = target[1] - currentPosition[1];
        double distanceToTarget = Math.sqrt(dx * dx + dy * dy);
        double baseAngle = Math.atan2(dy, dx);

        // FIX 1: Smart Speed Scaling. Don't launch short putts at max 5.0 m/s power!
        double currentSpeed = Math.min(5.0, Math.max(0.4, distanceToTarget * 1.3));
        double vx = currentSpeed * Math.cos(baseAngle);
        double vy = currentSpeed * Math.sin(baseAngle);

        double bestScore = evaluateShot(simulator, currentPosition, course, vx, vy);
        double bestVx = vx, bestVy = vy;
        double localBestScore = bestScore;
        double stepSize = 0.5;
        int maxRestarts = 25; // Balanced for high accuracy and sub-second execution
        int restarts = 0;

        while (bestScore > course.getTargetRadius()) {
            // Safety break to prevent full-minute application freezes
            if (lastIterationCount > 1000) {
                break;
            }

            boolean improved = false;

            // Expanded to checking 8 directional neighbors to maneuver smoothly around tight obstacles
            double[][] neighbors = {
                    { vx + stepSize, vy },
                    { vx - stepSize, vy },
                    { vx, vy + stepSize },
                    { vx, vy - stepSize },
                    { vx + stepSize * 0.7, vy + stepSize * 0.7 },
                    { vx - stepSize * 0.7, vy - stepSize * 0.7 },
                    { vx + stepSize * 0.7, vy - stepSize * 0.7 },
                    { vx - stepSize * 0.7, vy + stepSize * 0.7 }
            };

            for (double[] neighbor : neighbors) {
                // Keep neighbor velocities strictly within physical limits
                double speed = Math.hypot(neighbor[0], neighbor[1]);
                if (speed > 5.0) {
                    neighbor[0] = (neighbor[0] / speed) * 5.0;
                    neighbor[1] = (neighbor[1] / speed) * 5.0;
                }

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
                    
                    // Controlled random restart if trapped in an unresolvable local minimum
                    double newAngle = Math.random() * 2 * Math.PI;
                    double newSpeed = 0.2 + Math.random() * 4.8;
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
        lastIterationCount++;
        try {
            ShotResult result = simulator.simulate(currentPosition, new double[] { vx, vy });
            double[] finalPos = result.getFinalState();
            double[] target = course.getTargetPosition();
            double distance = Math.hypot(finalPos[0] - target[0], finalPos[1] - target[1]);
            
            // FIX 2: Check user-placed obstacles directly instead of missing enum HIT_TREE symbol
            boolean hitObstacle = false;
            if (course.getObstacles() != null) {
                for (model.obstacles.Obstacle o : course.getObstacles()) {
                    if (o.contains(finalPos[0], finalPos[1])) {
                        hitObstacle = true;
                        break;
                    }
                }
            }

            // FIX 3: Strict Out of bounds and penalty evaluation
            if (result.getOutcome() == ShotResult.Outcome.IN_WATER
                    || result.getOutcome() == ShotResult.Outcome.OUT_OF_BOUNDS
                    || finalPos[0] < 0 || finalPos[0] > course.getCourseWidth()
                    || finalPos[1] < 0 || finalPos[1] > course.getCourseHeight()
                    || hitObstacle) {
                return 1000.0 + distance; // Heavy penalty forces bot to steer clear
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