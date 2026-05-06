package bots;

import bots.GolfBot;
import io.CourseInputModule;
import io.CourseInputModuleStorage;
import model.CourseProfile;
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
        double angle = Math.atan2(dy, dx); // this is the angle from the ball to the target, this is where the search
                                           // will begin
        // atan2 is calculated with arctan formula (arctan(dy/dx)), it is angle of the
        // line connecting the ball and the target.

        // take initial power as 2.0, it doesn't matter much since we'll do a quick grid
        // search to find a better start point
        double bestInitialPower = 2.0;
        double bestInitialDistance = Double.MAX_VALUE;

        // grid search: we try power from 1 through 5 (max speed allowed by the manual)
        for (double testPower = 1.0; testPower <= 5.0; testPower += 1.0) {
            double testVx = testPower * Math.cos(angle);
            double testVy = testPower * Math.sin(angle);

            double[] testLanding = simulateForPosition(simulator, currentPosition, testVx, testVy);
            double dist = Math.sqrt(Math.pow(testLanding[0] - target[0], 2) + Math.pow(testLanding[1] - target[1], 2)); // squared
                                                                                                                        // distance
                                                                                                                        // from
                                                                                                                        // the
                                                                                                                        // landing
                                                                                                                        // spot
                                                                                                                        // to
                                                                                                                        // the
                                                                                                                        // hole

            if (dist < bestInitialDistance) {
                bestInitialDistance = dist; // distance from the first shot to the hole, we want to minimize this
                bestInitialPower = testPower;
            }
        }

        // Start Newton-Raphson with the best power we just found
        double vx = bestInitialPower * Math.cos(angle);
        double vy = bestInitialPower * Math.sin(angle);

        // to calculate derivatives, we need a small h value from the limit def of
        // derivative.
        // remember f'(x) =lim h -> 0 (f(x+h) - f(x))/h.
        double epsilon = 0.01; // our small h
        int maxIterations = 50; // to prevent infinite loop.
        double damping = 0.8;

        for (int i = 0; i < maxIterations; i++) {

            // Simulate the current shot and calculate the error
            double[] currentLanding = simulateForPosition(simulator, currentPosition, vx, vy);
            double errorX = currentLanding[0] - target[0];
            double errorY = currentLanding[1] - target[1];

            double distanceToHole = Math.sqrt(errorX * errorX + errorY * errorY); // Euclidean distance from the landing
                                                                                  // spot to the hole

            // If we are within the target radius, we found our shot
            if (distanceToHole <= course.getTargetRadius()) {
                return new double[] { vx, vy };
            }

            // Jacobian Approximation: We need to know how changing the vx,vy will affect
            // the landing position (x,y).
            // We change (tweak) vx and vy by a small epsilon and see how the landing
            // position changes to estimate the derivatives.
            double[] tweakVxLanding = simulateForPosition(simulator, currentPosition, vx + epsilon, vy); // tweak vx by
                                                                                                         // a small
                                                                                                         // amount
                                                                                                         // (epsilon)
                                                                                                         // and see
                                                                                                         // where we
                                                                                                         // land
            double dX_dVx = (tweakVxLanding[0] - currentLanding[0]) / epsilon; // how much the x landing position
                                                                               // changes when we tweak velocity in x
                                                                               // direction
            double dY_dVx = (tweakVxLanding[1] - currentLanding[1]) / epsilon; // how much the y landing position
                                                                               // changes when we tweak velocity in x
                                                                               // direction

            double[] tweakVyLanding = simulateForPosition(simulator, currentPosition, vx, vy + epsilon); // same for vy
            double dX_dVy = (tweakVyLanding[0] - currentLanding[0]) / epsilon; // repeat for velocity in y direction
            double dY_dVy = (tweakVyLanding[1] - currentLanding[1]) / epsilon;

            // We need to calculate the inverse of the Jacobian matrix to know how to adjust
            // vx and vy to reduce the error in x and y.
            // Normally, for 1D Newton-Raphson we just do new_guess = old_guess -
            // f(old_guess)/f'(old_guess).
            // However since its 2D, we have to do a matrix operation involving the inverse
            // of the Jacobian.
            // Matrix equivalent for 2D Newton Raphson is: [vx,vy] = [vx,vy] -
            // inverse(Jacobian matrix)*[errorX,errorY].
            // The reason we take the inverse is we cannot directly divide like in 1D.
            // determninant of the matrix. We need it to calculate the inverse. If its close
            // to zero, it means we are stuck and need some noise to escape.
            double determinant = (dX_dVx * dY_dVy) - (dX_dVy * dY_dVx);

            // If determinant is very close to 0, add some noise to escape the flat spot
            if (Math.abs(determinant) < 1e-6) {
                vx += (Math.random() - 0.5) * 0.5;
                vy += (Math.random() - 0.5) * 0.5;
                continue;
            }

            double invJ11 = dY_dVy / determinant; // (1,1) element of the inverse Jacobian, this tells us how much we
                                                  // should change vx to reduce the error in x, ignoring y for a moment
            double invJ12 = -dX_dVy / determinant; // (1,2) element of the inverse Jacobian, this tells us how much we
                                                   // should change vx to reduce the error in y, ignoring x for a moment
            double invJ21 = -dY_dVx / determinant;// (2,1) element of the inverse Jacobian, this tells us how much we
                                                  // should change vy to reduce the error in x, ignoring y for a moment
            double invJ22 = dX_dVx / determinant;// (2,2) element of the inverse Jacobian, this tells us how much we
                                                 // should change vy to reduce the error in y, ignoring x for a moment

            double stepVx = invJ11 * errorX + invJ12 * errorY; // how much we should change vx to reduce the error in x
                                                               // and y
            double stepVy = invJ21 * errorX + invJ22 * errorY; // how much we should change vy to reduce the error in x
                                                               // and y

            // Damping or learning rate: Since the course is not flat, we might overshoot or
            // go in the wrong direction.
            // Damping will help us take smaller and safer steps. If we find that a step
            // made things worse, we can reduce the damping to take an even smaller step
            // next time.
            double oldVx = vx;
            double oldVy = vy;
            double oldDistance = distanceToHole;

            // Apply the step with current damping
            vx = oldVx - (damping * stepVx);
            vy = oldVy - (damping * stepVy);

            // Test if this new velocity is actually better
            double[] testLanding = simulateForPosition(simulator, currentPosition, vx, vy);
            double newDistance = Math
                    .sqrt(Math.pow(testLanding[0] - target[0], 2) + Math.pow(testLanding[1] - target[1], 2));

            if (newDistance > oldDistance) {
                // Oops. Our math pushed us in a worse direction.
                // Go back to the old velocity and cut damping in half for a smaller, safer step
                // next time.
                vx = oldVx;
                vy = oldVy;
                damping *= 0.5;
            } else {
                // It worked! We can slightly increase damping back toward 0.8 for faster
                // learning

                damping = Math.min(0.8, damping * 1.1);
            }

            // maximum speed allowed by the manual (5.0 m/s)
            double speed = Math.sqrt(vx * vx + vy * vy);
            if (speed > 5.0) {
                vx = (vx / speed) * 5.0;
                vy = (vy / speed) * 5.0;
            }
        }

        // Return the best guess if max iterations are reached
        return new double[] { vx, vy };
    }

    private double[] simulateForPosition(GolfSimulator simulator, double[] startPosition, double vx, double vy) {
        try {
            ShotResult result = simulator.simulate(startPosition, new double[] { vx, vy });
            return result.getFinalState();
        } catch (Exception e) {
            return startPosition;
        }
    }
}
