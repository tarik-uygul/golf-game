package model;

import java.util.ArrayList;
import java.util.List;

import io.CourseInputModuleStorage;
import physics.EulerSolver;
import physics.GolfPhysicsFunction;
import physics.ODEFunction;
import physics.RungeKutta4;
import physics.CourseCollisionDetector;
import physics.ObstacleCollisionDetector;

public class GolfSimulator {

    // which solver to use — "euler" or "rk4"

    private final String solverType;
    private final ODEFunction physicsFunc;
    private final CourseInputModuleStorage course;
    private final CourseCollisionDetector courseDetector;
    private final ObstacleCollisionDetector obstacleDetector;
    private final double dt;
    private final double maxTime;

    public GolfSimulator(CourseInputModuleStorage course, String solverType, double dt, double maxTime) {
        this.course = course;
        this.solverType = solverType;
        this.dt = dt;
        this.maxTime = maxTime;
        this.courseDetector = new CourseCollisionDetector(course);
        this.obstacleDetector = new ObstacleCollisionDetector(course);
        this.physicsFunc = new GolfPhysicsFunction(course, this.courseDetector);
    }

    // initialVelocity = [vx, vy], starting from currentPosition = [x, y]
    public ShotResult simulate(double[] currentPosition, double[] initialVelocity) {
        double[] state = {
                currentPosition[0], currentPosition[1],
                initialVelocity[0], initialVelocity[1]
        };

        List<double[]> path = new ArrayList<>();
        path.add(state.clone());

        double time = 0;

        while (time < maxTime) {
            state = doStep(state);
            path.add(state.clone());
            time += dt;

            // Check for collisions with course boundaries (terrain water, out-of-bounds)
            ShotResult.Outcome courseCollision = courseDetector.checkCourse(state[0], state[1]);
            if (courseCollision != null) {
                return new ShotResult(path, courseCollision, state);
            }

            // Check for collisions with placed obstacles (trees, water hazards, sand)
            ShotResult.Outcome obstacleCollision = obstacleDetector.checkTerminal(state[0], state[1]);
            if (obstacleCollision != null) {
                return new ShotResult(path, obstacleCollision, state);
            }

            // check target reached
            double[] target = course.getTargetPosition();
            double dx = state[0] - target[0];
            double dy = state[1] - target[1];
            if (Math.sqrt(dx * dx + dy * dy) <= course.getTargetRadius()) {
                return new ShotResult(path, ShotResult.Outcome.IN_TARGET, state);
            }

            // check if ball has stopped
            if (hasStopped(state)) {
                return new ShotResult(path, ShotResult.Outcome.STOPPED, state);
            }
        }

        return new ShotResult(path, ShotResult.Outcome.TIMEOUT, state);
    }
    // Fast simulation for bots - does not record the visual path to save memory/CPU
    public ShotResult simulateBotShot(double[] currentPosition, double[] initialVelocity) {
        double[] state = {
                currentPosition[0], currentPosition[1],
                initialVelocity[0], initialVelocity[1]
        };

        double time = 0;

        while (time < maxTime) {
            state = doStep(state);
            time += dt;

            // Check boundaries
            ShotResult.Outcome courseCollision = courseDetector.checkCourse(state[0], state[1]);
            if (courseCollision != null) return new ShotResult(null, courseCollision, state);

            // Check obstacles
            ShotResult.Outcome obstacleCollision = obstacleDetector.checkTerminal(state[0], state[1]);
            if (obstacleCollision != null) return new ShotResult(null, obstacleCollision, state);

            // Check target
            double[] target = course.getTargetPosition();
            double dx = state[0] - target[0];
            double dy = state[1] - target[1];
            if (Math.sqrt(dx * dx + dy * dy) <= course.getTargetRadius()) {
                return new ShotResult(null, ShotResult.Outcome.IN_TARGET, state);
            }

            // Check if ball stopped
            if (hasStopped(state)) {
                return new ShotResult(null, ShotResult.Outcome.STOPPED, state);
            }
        }
        return new ShotResult(null, ShotResult.Outcome.TIMEOUT, state);
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
        double speed = Math.sqrt(vx * vx + vy * vy);
        if (speed > 1e-4)
            return false;

        // check static friction holds
        double dhdx = course.getSlopeX(state[0], state[1]);
        double dhdy = course.getSlopeY(state[0], state[1]);
        double slopeNorm = Math.sqrt(dhdx * dhdx + dhdy * dhdy);
        return slopeNorm <= course.getStaticFriction();
    }
}
