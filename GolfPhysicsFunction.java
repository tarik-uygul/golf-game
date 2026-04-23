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
        double x  = state[0];
        double y  = state[1];
        double vx = state[2];
        double vy = state[3];

        double dhdx = course.heightFunction.dhdx(x, y);
        double dhdy = course.heightFunction.dhdy(x, y);
        double muK  = course.muK;
        double speed = Math.sqrt(vx * vx + vy * vy);

        double ax, ay;

        if (speed < 1e-6) {
            double muS = course.muS;
            double slopeNorm = Math.sqrt(dhdx * dhdx + dhdy * dhdy);
            if (slopeNorm > muS) {
                ax = -G * dhdx - muK * G * (dhdx / slopeNorm);
                ay = -G * dhdy - muK * G * (dhdy / slopeNorm);
            } else {
                ax = 0;
                ay = 0;
            }
        } else {
            ax = -G * dhdx - muK * G * (vx / speed);
            ay = -G * dhdy - muK * G * (vy / speed);
        }


        // ── DEBUG PRINT ──────────────────────────────────────────────────
    System.out.printf(
        "STATE  x=%.4f  y=%.4f  vx=%.4f  vy=%.4f%n" +
        "SLOPE  dhdx=%.4f  dhdy=%.4f  speed=%.6f%n" +
        "ACCEL  ax=%.4f  ay=%.4f%n" +
        "──────────────────────────────────────%n",
        x, y, vx, vy, dhdx, dhdy, speed, ax, ay
    );
    // ────────────────────────────────────────────────────────────────


        return new double[]{vx, vy, ax, ay};
    }
}