package bots;

import io.CourseInputModuleStorage;
import model.GolfSimulator;
import model.ShotResult;

public class Newton_Raphson_Bot implements GolfBot {
    /*
     * Newton Raphson method for finding optimal golf shots
     */

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
        // atan2 gives the angle from ball to target (starting point for search)
        double angle = Math.atan2(dy, dx);

        // take initial power as 2.0, it doesn't matter much since we'll do a quick grid
        // search to find a better start point
        double bestInitialPower = 2.0;
        double bestInitialDistance = Double.MAX_VALUE;

        // grid search: we try power from 1 through 5 (max speed allowed by the manual)
        for (double testPower = 1.0; testPower <= 5.0; testPower += 1.0) {
            double testVx = testPower * Math.cos(angle);
            double testVy = testPower * Math.sin(angle);

            double[] testLanding = simulateForPosition(simulator, currentPosition, testVx, testVy);
            double dist = Math.sqrt(Math.pow(testLanding[0] - target[0], 2) + Math.pow(testLanding[1] - target[1], 2)); // distance
                                                                                                                        // from
                                                                                                                        // hole

            if (dist < bestInitialDistance) {
                bestInitialDistance = dist; // minimize distance from first shot to hole
                bestInitialPower = testPower;
            }
        }

        // Start Newton-Raphson with the best power we just found
        double vx = bestInitialPower * Math.cos(angle);
        double vy = bestInitialPower * Math.sin(angle);

        // to calculate derivatives, we need a small h value (limit definition: f'(x) =
        // lim h->0 (f(x+h) - f(x))/h)
        double epsilon = 0.01; // our small h
        int maxIterations = 50; // to prevent infinite loop.
        double damping = 0.8;

        for (int i = 0; i < maxIterations; i++) {

            // Simulate the current shot and calculate the error
            double[] currentLanding = simulateForPosition(simulator, currentPosition, vx, vy);
            double errorX = currentLanding[0] - target[0];
            double errorY = currentLanding[1] - target[1];

            double distanceToHole = Math.sqrt(errorX * errorX + errorY * errorY); // Euclidean distance to hole

            // If we are within the target radius, we found our shot
            if (distanceToHole <= course.getTargetRadius()) {
                return new double[] { vx, vy };
            }

            // Jacobian Approximation: estimate how changing vx,vy affects landing position
            // by tweaking by epsilon
            double[] tweakVxLanding = simulateForPosition(simulator, currentPosition, vx + epsilon, vy); // tweak vx by
                                                                                                         // epsilon
            double dX_dVx = (tweakVxLanding[0] - currentLanding[0]) / epsilon; // change in x landing per unit change in
                                                                               // vx
            double dY_dVx = (tweakVxLanding[1] - currentLanding[1]) / epsilon; // change in y landing per unit change in
                                                                               // vx

            double[] tweakVyLanding = simulateForPosition(simulator, currentPosition, vx, vy + epsilon); // same for vy
            double dX_dVy = (tweakVyLanding[0] - currentLanding[0]) / epsilon; // change in x landing per unit change in
                                                                               // vy
            double dY_dVy = (tweakVyLanding[1] - currentLanding[1]) / epsilon; // change in y landing per unit change in
                                                                               // vy

            // 2D Newton-Raphson: [vx,vy] = [vx,vy] - inverse(Jacobian)*[errorX,errorY]
            // Need determinant to calculate inverse; if near zero, we're stuck
            double determinant = (dX_dVx * dY_dVy) - (dX_dVy * dY_dVx);

            // If determinant is very close to 0, add some noise to escape the flat spot
            if (Math.abs(determinant) < 1e-6) {
                vx += (Math.random() - 0.5) * 0.5;
                vy += (Math.random() - 0.5) * 0.5;
                continue;
            }

            double invJ11 = dY_dVy / determinant; // inverse Jacobian (1,1): how much to change vx for error in x
            double invJ12 = -dX_dVy / determinant; // inverse Jacobian (1,2): how much to change vx for error in y
            double invJ21 = -dY_dVx / determinant; // inverse Jacobian (2,1): how much to change vy for error in x
            double invJ22 = dX_dVx / determinant; // inverse Jacobian (2,2): how much to change vy for error in y

            double stepVx = invJ11 * errorX + invJ12 * errorY; // how much to change vx to reduce error
            double stepVy = invJ21 * errorX + invJ22 * errorY; // how much to change vy to reduce error

            // Damping: take smaller, safer steps. If a step made things worse, reduce
            // damping
           double oldVx = vx;
            double oldVy = vy;
            double oldDistance = distanceToHole;
            boolean stepAccepted = false;

            // INNER LOOP: Try smaller steps until one improves our score, 
            // WITHOUT recalculating the expensive Jacobian derivatives!
            while (damping > 0.01 && !stepAccepted) {
                
                // 1. Calculate proposed step
                vx = oldVx - (damping * stepVx);
                vy = oldVy - (damping * stepVy);

                // 2. Clamp to maximum speed BEFORE evaluating the shot
                double speed = Math.sqrt(vx * vx + vy * vy);
                if (speed > 5.0) {
                    vx = (vx / speed) * 5.0;
                    vy = (vy / speed) * 5.0;
                }

                // 3. Test if this new clamped velocity is actually better
                double[] testLanding = simulateForPosition(simulator, currentPosition, vx, vy);
                double newDistance = Math.sqrt(Math.pow(testLanding[0] - target[0], 2) + Math.pow(testLanding[1] - target[1], 2));

                if (newDistance >= oldDistance) {
                    // Step failed. Cut damping in half and loop again to try a smaller step immediately.
                    damping *= 0.5;
                } else {
                    // Step worked! Accept it and slightly increase damping for the next full iteration.
                    stepAccepted = true;
                    damping = Math.min(0.8, damping * 1.2);
                }
            }

            // If we shrank damping all the way down and still couldn't find a good step, 
            // the math is stuck in a weird local minimum. Bump the ball slightly to escape.
            if (!stepAccepted) {
                vx = oldVx + (Math.random() - 0.5) * 0.5;
                vy = oldVy + (Math.random() - 0.5) * 0.5;
                damping = 0.8; // Reset damping
            }
        }

        // Return the best guess if max iterations are reached
        return new double[] { vx, vy };
    }

    private double[] simulateForPosition(GolfSimulator simulator, double[] startPosition, double vx, double vy) {
        try {
            ShotResult result = simulator.simulate(startPosition, new double[] { vx, vy });
            
            // BUG FIX: If it hits water, treat it as a massive error so the bot rejects it
            if (result.getOutcome() == ShotResult.Outcome.IN_WATER || result.getOutcome() == ShotResult.Outcome.OUT_OF_BOUNDS) {
                return new double[]{9999.0, 9999.0}; 
            }
            return result.getFinalState();
        } catch (Exception e) {
            return new double[]{9999.0, 9999.0};
        }
    }
}
