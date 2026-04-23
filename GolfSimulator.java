import java.util.ArrayList;
import java.util.List;

public class GolfSimulator {
    private final String solverType;
    private final ODEFunction physicsFunc;
    private final CourseInputModuleStorage course;
    private final double dt;
    private final double maxTime;

    public GolfSimulator(CourseInputModuleStorage course, String solverType, double dt, double maxTime) {
        this.course = course;
        this.solverType = solverType;
        this.physicsFunc = new GolfPhysicsFunction(course);
        this.dt = dt;
        this.maxTime = maxTime;
    }

    public ShotResult simulate(double[] currentPosition, double[] initialVelocity) {
        double[] state = {
            currentPosition[0], currentPosition[1],
            initialVelocity[0], initialVelocity[1]
        };

        List<double[]> path = new ArrayList<>();
        path.add(state.clone());

        double time = 0;

        while (time < maxTime) {
            double[] prevState = state.clone();
            state = doStep(state);

            // if velocity crossed zero this step, the ball has stopped - don't let it bounce
            if (prevState[2] * state[2] < 0) state[2] = 0;
            if (prevState[3] * state[3] < 0) state[3] = 0;

            path.add(state.clone());
            time += dt;

            // check water (negative height)
            if (course.heightFunction.evaluate(state[0], state[1]) < 0) {
                System.out.println(">>> NEW SIMULATOR - outcome: WATER <<<");
                return new ShotResult(path, ShotResult.Outcome.IN_WATER, state);
            }

            // check target reached
            double dx = state[0] - course.targetX;
            double dy = state[1] - course.targetY;
            if (Math.sqrt(dx*dx + dy*dy) <= course.targetRadius) {
                System.out.println(">>> NEW SIMULATOR - outcome: IN_TARGET <<<");
                return new ShotResult(path, ShotResult.Outcome.IN_TARGET, state);
            }

            // check if ball has stopped
            if (hasStopped(state)) {
                System.out.println(">>> NEW SIMULATOR - outcome: STOPPED <<<");
                return new ShotResult(path, ShotResult.Outcome.STOPPED, state);
            }
        }

        System.out.println(">>> NEW SIMULATOR - outcome: TIMEOUT <<<");
        return new ShotResult(path, ShotResult.Outcome.TIMEOUT, state);
    }

    private double[] doStep(double[] state) {
        if (solverType.equals("rk4")) {
            return RungeKutta4.step(state, dt, physicsFunc);
        } else {
            return EulerSolver.step(state, dt, physicsFunc);
        }
    }

    private boolean hasStopped(double[] state) {
        double vx = state[2];
        double vy = state[3];
        double speed = Math.sqrt(vx*vx + vy*vy);
        if (speed > 0.01) return false;
        double dhdx = course.heightFunction.dhdx(state[0], state[1]);
        double dhdy = course.heightFunction.dhdy(state[0], state[1]);
        double slopeNorm = Math.sqrt(dhdx*dhdx + dhdy*dhdy);
        return slopeNorm <= course.muS;
    }
}