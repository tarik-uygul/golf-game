package everything;

import everything.GolfSimulator;
import everything.GolfBot;
import everything.ShotResult;

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
    public double[] computeShot(double[] currentPosition, CourseProfile course) {
        GolfSimulator simulator = new GolfSimulator(course, solverType, dt, maxTime);
        double[] target = course.getTargetPosition();

        double dx = target[0] - currentPosition[0];
        double dy = target[1] - currentPosition[1];
        double angle = Math.atan2(dy, dx);
        double vx = 3.0 * Math.cos(angle);
        double vy = 3.0 * Math.sin(angle);

        double epsilon = 0.01;
        int maxIterations = 50;

        for (int i = 0; i < maxIterations; i++) {
            double[] initialLanding = simulateForPosition(simulator, currentPosition, vx, vy);
            double errorX = initialLanding[0] - target[0];
            double errorY = initialLanding[1] - target[1];
            double distance = Math.sqrt(errorX * errorX + errorY * errorY);

            if (distance <= course.getTargetRadius()) {
                return new double[] { vx, vy };
            }

            // Approximate Jacobian
            double[] tweakVyLanding = simulateForPosition(simulator, currentPosition, vx, vy + epsilon);
            double dX_dVy = (tweakVyLanding[0] - initialLanding[0]) / epsilon;
            double dY_dVy = (tweakVyLanding[1] - initialLanding[1]) / epsilon;

            double[] tweakVxLanding = simulateForPosition(simulator, currentPosition, vx + epsilon, vy);
            double dX_dVx = (tweakVxLanding[0] - initialLanding[0]) / epsilon;
            double dY_dVx = (tweakVxLanding[1] - initialLanding[1]) / epsilon;

            double determinant = dX_dVx * dY_dVy - dX_dVy * dY_dVx;

            if (Math.abs(determinant) < 1e-6) {
                vx += (Math.random() - 0.5) * 0.5;
                vy += (Math.random() - 0.5) * 0.5;
                continue;
            }

            // Invert Jacobian and compute step
            double invJ11 = dY_dVy / determinant;
            double invJ12 = -dX_dVy / determinant;
            double invJ21 = -dY_dVx / determinant;
            double invJ22 = dX_dVx / determinant;

            double stepVx = -(invJ11 * errorX + invJ12 * errorY);
            double stepVy = -(invJ21 * errorX + invJ22 * errorY);

            double dampingFactor = 0.8;
            vx = vx - (dampingFactor * stepVx);
            vy = vy - (dampingFactor * stepVy);

            // Cap velocity to max power
            double speed = Math.sqrt(vx * vx + vy * vy);
            if (speed > 5.0) {
                vx = (vx / speed) * 5.0;
                vy = (vy / speed) * 5.0;
            }
        }

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
