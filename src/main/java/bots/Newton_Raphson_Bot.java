package bots;

import io.CourseInputModuleStorage;
import model.GolfSimulator;
import model.ShotResult;

public class Newton_Raphson_Bot implements GolfBot {
    
    private final double dt;
    private final double maxTime;
    private final String solverType;

    public Newton_Raphson_Bot(double dt, double maxTime, String solverType) {
        this.dt = dt;
        this.maxTime = maxTime;
        this.solverType = solverType;
    }

    @Override
    public double[] computeShot(double[] currentPosition, CourseInputModuleStorage course) {
        GolfSimulator simulator = new GolfSimulator(course, solverType, dt, maxTime);
        double[] target = course.getTargetPosition();

        double dx = target[0] - currentPosition[0];
        double dy = target[1] - currentPosition[1];
        double distanceToTarget = Math.sqrt(dx * dx + dy * dy);
        double angle = Math.atan2(dy, dx);

        double bestInitialVx = 2.0 * Math.cos(angle);
        double bestInitialVy = 2.0 * Math.sin(angle);
        double bestInitialDistance = Double.MAX_VALUE;
        
        // Define tolerance
        double tolerance = Math.max(course.getTargetRadius(), 0.15);

        // --- DYNAMIC GRID SEARCH ---
        // Prevents the game from freezing by only doing dense scans on short putts!
        
        if (distanceToTarget <= 1.5) {
            // MICRO-PUTT: Dense Grid (250 simulations, but they stop fast)
            for (double testPower = 0.1; testPower <= 5.0; testPower += 0.1) {
                for (double angleOffset = -0.3; angleOffset <= 0.3; angleOffset += 0.15) {
                    double testVx = testPower * Math.cos(angle + angleOffset);
                    double testVy = testPower * Math.sin(angle + angleOffset);

                    double[] testLanding = simulateForPosition(simulator, currentPosition, testVx, testVy);
                    double dist = Math.sqrt(Math.pow(testLanding[0] - target[0], 2) + Math.pow(testLanding[1] - target[1], 2));

                    if (dist < bestInitialDistance) {
                        bestInitialDistance = dist;
                        bestInitialVx = testVx;
                        bestInitialVy = testVy;
                    }

                    if (dist <= tolerance) {
                        return new double[] { testVx, testVy };
                    }
                }
            }
        } else {
            // LONG SHOT: Sparse Grid (Only 5 simulations, lightning fast!)
            for (double testPower = 1.0; testPower <= 5.0; testPower += 1.0) {
                double testVx = testPower * Math.cos(angle);
                double testVy = testPower * Math.sin(angle);

                double[] testLanding = simulateForPosition(simulator, currentPosition, testVx, testVy);
                double dist = Math.sqrt(Math.pow(testLanding[0] - target[0], 2) + Math.pow(testLanding[1] - target[1], 2));

                if (dist < bestInitialDistance) {
                    bestInitialDistance = dist;
                    bestInitialVx = testVx;
                    bestInitialVy = testVy;
                }
                
                if (dist <= tolerance) {
                    return new double[] { testVx, testVy };
                }
            }
        }

        // --- FALLBACK: NEWTON RAPHSON ---
        double vx = bestInitialVx;
        double vy = bestInitialVy;
        double epsilon = 0.01; 
        int maxIterations = 50; 
        double damping = 0.8;

        for (int i = 0; i < maxIterations; i++) {

            double[] currentLanding = simulateForPosition(simulator, currentPosition, vx, vy);
            double errorX = currentLanding[0] - target[0];
            double errorY = currentLanding[1] - target[1];

            double distanceToHole = Math.sqrt(errorX * errorX + errorY * errorY); 

            if (distanceToHole <= tolerance) {
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
                double newDistance = Math.sqrt(Math.pow(testLanding[0] - target[0], 2) + Math.pow(testLanding[1] - target[1], 2));

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

        return new double[] { vx, vy };
    }

    private double[] simulateForPosition(GolfSimulator simulator, double[] startPosition, double vx, double vy) {
        try {
            ShotResult result = simulator.simulate(startPosition, new double[] { vx, vy });
            
            if (result.getOutcome() == ShotResult.Outcome.IN_WATER || result.getOutcome() == ShotResult.Outcome.OUT_OF_BOUNDS) {
                return new double[]{9999.0, 9999.0}; 
            }
            return result.getFinalState();
        } catch (Exception e) {
            return new double[]{9999.0, 9999.0};
        }
    }
}