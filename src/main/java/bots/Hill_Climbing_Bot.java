package bots;

import io.CourseInputModuleStorage;
import model.GolfSimulator;
import model.ShotResult;

public class Hill_Climbing_Bot implements GolfBot {

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
        GolfSimulator simulator = new GolfSimulator(course, solverType, dt, maxTime);
        double[] target = course.getTargetPosition();

        // 1. Initial Guess (Aim straight at the hole)
        double angle = Math.atan2(target[1] - currentPosition[1], target[0] - currentPosition[0]);
        double distanceToTarget = Math.hypot(target[0] - currentPosition[0], target[1] - currentPosition[1]);
        double speed = Math.min(5.0, distanceToTarget * 1.2); // Cap speed at physical max 5.0
        
        double vx = speed * Math.cos(angle);
        double vy = speed * Math.sin(angle);

        double bestDistance = evaluateShot(simulator, currentPosition, course, vx, vy);
        double stepSize = 1.0; 

        // Accept a shot if it lands within 20cm of the waypoint (since MazeBot target is 0.0)
        double acceptanceRadius = course.getTargetRadius() > 0 ? course.getTargetRadius() : 0.2;
        int maxIterations = 200; // Strict limit prevents infinite freezing

        // Use acceptanceRadius instead of course.getTargetRadius()
        while (bestDistance > acceptanceRadius && stepSize > 0.01 && lastIterationCount < maxIterations) {
            boolean improved = false;
            double nextVx = vx;
            double nextVy = vy;

            // Test 8 directions (up, down, left, right, diagonals)
            double[][] directions = {
                {1,0}, {-1,0}, {0,1}, {0,-1}, 
                {0.7, 0.7}, {-0.7, -0.7}, {0.7, -0.7}, {-0.7, 0.7}
            };

            for (double[] dir : directions) {
                double testVx = vx + (dir[0] * stepSize);
                double testVy = vy + (dir[1] * stepSize);

                // Don't test illegal speeds
                if (Math.hypot(testVx, testVy) > 5.0) continue;

                double dist = evaluateShot(simulator, currentPosition, course, testVx, testVy);
                
                if (dist < bestDistance) {
                    bestDistance = dist;
                    nextVx = testVx;
                    nextVy = testVy;
                    improved = true;
                }
            }

            if (improved) {
                vx = nextVx;
                vy = nextVy;
            } else {
                // If no direction was better, reduce step size to look closer
                stepSize *= 0.5;
            }
        }

        return new double[] { vx, vy };
    }

    private double evaluateShot(GolfSimulator simulator, double[] currentPosition, CourseInputModuleStorage course, double vx, double vy) {
        lastIterationCount++;
        
        // Use our new ultra-fast bot simulation!
        ShotResult result = simulator.simulateBotShot(currentPosition, new double[] { vx, vy });
        double[] finalPos = result.getFinalState();
        double[] target = course.getTargetPosition();
        
        double distance = Math.hypot(finalPos[0] - target[0], finalPos[1] - target[1]);

        // Heavy penalty for bad outcomes forces the bot to avoid them
        if (result.getOutcome() == ShotResult.Outcome.IN_WATER ||
            result.getOutcome() == ShotResult.Outcome.OUT_OF_BOUNDS ||
            result.getOutcome() == ShotResult.Outcome.HIT_TREE) {
            return distance + 1000.0; 
        }

        return distance;
    }
}