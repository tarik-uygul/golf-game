package bots;

import io.CourseInputModuleStorage;
import model.GolfSimulator;
import model.ShotResult;

public class Newton_Raphson_Bot implements GolfBot {

    private static final double BOT_DT = 0.01;
    private static final double BOT_MAX_TIME = 20.0;

    private final double dt;
    private final double maxTime;
    private final String solverType;
    private int lastIterationCount = 0;

    public Newton_Raphson_Bot(double dt, double maxTime, String solverType) {
        this.dt = dt;
        this.maxTime = maxTime;
        this.solverType = solverType;
    }

    @Override
    public int getLastIterationCount() { return lastIterationCount; }

    @Override
    public double[] computeShot(double[] currentPosition, CourseInputModuleStorage course) {
        GolfSimulator simulator = new GolfSimulator(course, solverType, BOT_DT, BOT_MAX_TIME);
        double[] target = course.getTargetPosition();

        double dx = target[0] - currentPosition[0];
        double dy = target[1] - currentPosition[1];
        double angle = Math.atan2(dy, dx);

        double bestVx = 2.0 * Math.cos(angle);
        double bestVy = 2.0 * Math.sin(angle);
        double bestInitialDistance = Double.MAX_VALUE;
        int angleSteps = 9;
        double[] testPowers = { 2.0, 3.5, 5.0 };

        for (int ai = 0; ai < angleSteps; ai++) {
            double testAngle = angle - Math.PI / 2 + ai * (Math.PI / (angleSteps - 1));
            for (double testPower : testPowers) {
                double testVx = testPower * Math.cos(testAngle);
                double testVy = testPower * Math.sin(testAngle);
                double[] landing = simulateForPosition(simulator, currentPosition, testVx, testVy);
                double dist = Math.sqrt(Math.pow(landing[0] - target[0], 2) + Math.pow(landing[1] - target[1], 2));
                if (dist < bestInitialDistance) {
                    bestInitialDistance = dist;
                    bestVx = testVx;
                    bestVy = testVy;
                }
            }
        }

        lastIterationCount = 0;
        double vx = bestVx;
        double vy = bestVy;
        // ... (inside computeShot)
        double epsilon = 0.01;
        int maxIterations = 15; // REDUCED from 50
        int maxRestarts = 3;    // REDUCED from 20
        double damping = 0.8;
        double globalBestDist = Double.MAX_VALUE;
        double globalBestVx = vx, globalBestVy = vy;

        // If MazeBot gives us a 0.0 radius, accept any shot that stops within 20cm of the waypoint
        double acceptanceRadius = course.getTargetRadius() > 0 ? course.getTargetRadius() : 0.2;

        for (int restart = 0; restart <= maxRestarts; restart++) {
            for (int i = 0; i < maxIterations; i++) {
                lastIterationCount++;

                double[] currentLanding = simulateForPosition(simulator, currentPosition, vx, vy);
                double errorX = currentLanding[0] - target[0];
                double errorY = currentLanding[1] - target[1];
                double distanceToHole = Math.sqrt(errorX * errorX + errorY * errorY);

                if (distanceToHole < globalBestDist) {
                    globalBestDist = distanceToHole;
                    globalBestVx = vx;
                    globalBestVy = vy;
                }

                // FIXED: Use acceptanceRadius so it actually breaks!
                if (distanceToHole <= acceptanceRadius) {
                    return new double[] { vx, vy };
                }
               

                double[] tweakVxLanding = simulateForPosition(simulator, currentPosition, vx + epsilon, vy);
                double dX_dVx = (tweakVxLanding[0] - currentLanding[0]) / epsilon;
                double dY_dVx = (tweakVxLanding[1] - currentLanding[1]) / epsilon;

                double[] tweakVyLanding = simulateForPosition(simulator, currentPosition, vx, vy + epsilon);
                double dX_dVy = (tweakVyLanding[0] - currentLanding[0]) / epsilon;
                double dY_dVy = (tweakVyLanding[1] - currentLanding[1]) / epsilon;

                double determinant = (dX_dVx * dY_dVy) - (dX_dVy * dY_dVx);

                if (Math.abs(determinant) < 1e-6) {
                    vx += (Math.random() - 0.5) * 0.5;
                    vy += (Math.random() - 0.5) * 0.5;
                    continue;
                }

                double invJ11 = dY_dVy / determinant;
                double invJ12 = -dX_dVy / determinant;
                double invJ21 = -dY_dVx / determinant;
                double invJ22 = dX_dVx / determinant;

                double stepVx = invJ11 * errorX + invJ12 * errorY;
                double stepVy = invJ21 * errorX + invJ22 * errorY;

                double oldVx = vx;
                double oldVy = vy;
                double oldDistance = distanceToHole;
                boolean stepAccepted = false;

                while (damping > 0.01 && !stepAccepted) {
                    vx = oldVx - (damping * stepVx);
                    vy = oldVy - (damping * stepVy);
                    double speed = Math.sqrt(vx * vx + vy * vy);
                    if (speed > 5.0) {
                        vx = (vx / speed) * 5.0;
                        vy = (vy / speed) * 5.0;
                    }
                    double[] testLanding = simulateForPosition(simulator, currentPosition, vx, vy);
                    double newDistance = Math.sqrt(
                            Math.pow(testLanding[0] - target[0], 2) + Math.pow(testLanding[1] - target[1], 2));
                    if (newDistance >= oldDistance) {
                        damping *= 0.5;
                    } else {
                        stepAccepted = true;
                        damping = Math.min(0.8, damping * 1.2);
                    }
                }

                if (!stepAccepted) {
                    vx = oldVx + (Math.random() - 0.5) * 0.5;
                    vy = oldVy + (Math.random() - 0.5) * 0.5;
                    damping = 0.8;
                }
            }

if (globalBestDist <= acceptanceRadius) break;

            if (restart < maxRestarts) {
                double randAngle = Math.random() * 2 * Math.PI;
                double randSpeed = 0.5 + Math.random() * 4.5;
                vx = randSpeed * Math.cos(randAngle);
                vy = randSpeed * Math.sin(randAngle);
                damping = 0.8;
            }
        }

        return new double[] { globalBestVx, globalBestVy };
    }

    private double[] simulateForPosition(GolfSimulator simulator, double[] startPosition, double vx, double vy) {
        try {
            // FIXED: Use the memory-free fast-forward simulation!
            ShotResult result = simulator.simulateBotShot(startPosition, new double[] { vx, vy });
            
            // FIXED: If the shot hits a tree, water, or goes out of bounds, return a massive error
            // This tricks the Newton-Raphson math into aggressively steering away from hazards!
            if (result.getOutcome() == ShotResult.Outcome.IN_WATER ||
                result.getOutcome() == ShotResult.Outcome.OUT_OF_BOUNDS ||
                result.getOutcome() == ShotResult.Outcome.HIT_TREE) {
                return new double[] { 9999.0, 9999.0 }; 
            }
            
            return result.getFinalState();
        } catch (Exception e) {
            return new double[] { 9999.0, 9999.0 };
        }
    }
}
