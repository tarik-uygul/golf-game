package physics;

import io.CourseInputModuleStorage;
import model.CourseProfile;

// implements the equations of motion from Appendix B of the manual
// state vector is [x, y, vx, vy]
public class GolfPhysicsFunction implements ODEFunction {

    private static final double G = 9.81;
    private final CourseInputModuleStorage course;

    public GolfPhysicsFunction(CourseInputModuleStorage course) {
        this.course = course;
    }

    @Override
    public double[] compute(double[] state) {
        double x = state[0];
        double y = state[1];
        double vx = state[2];
        double vy = state[3];

        double dhdx = course.getSlopeX(x, y);
        double dhdy = course.getSlopeY(x, y);
        double muK = course.getKineticFriction();
        double speed = Math.sqrt(vx * vx + vy * vy);

        double ax, ay;

        if (speed < 1e-6) {
            // ball nearly stopped — friction term has zero denominator, skip it
            // if slope overcomes static friction, ball starts sliding in slope direction
            double muS = course.getStaticFriction();
            double slopeNorm = Math.sqrt(dhdx * dhdx + dhdy * dhdy);
            if (slopeNorm > muS) {
                // slides: friction opposes slope direction
                ax = -G * dhdx - muK * G * (dhdx / slopeNorm);
                ay = -G * dhdy - muK * G * (dhdy / slopeNorm);
            } else {
                // stays put
                ax = 0;
                ay = 0;
            }
        } else {
            // equations (4) and (5) from the manual
            ax = -G * dhdx - muK * G * (vx / speed);
            ay = -G * dhdy - muK * G * (vy / speed);
        }

        // [dx/dt, dy/dt, dvx/dt, dvy/dt]
        return new double[] { vx, vy, ax, ay };
    }
}
