package bots;

import io.CourseInputModuleStorage;
import java.util.List;
import bots.PathPlanner;

public class MazeBot implements GolfBot {

    private final double dt;
    private final double maxTime;
    private final String solverType;

    private List<double[]> plannedPath = null;
    private int currentPathIndex;

    public MazeBot(String solverType, double dt, double maxTime) {
        this.solverType = solverType;
        this.dt = dt;
        this.maxTime = maxTime;
    }
    @Override
    public double[] computeShot(double[] currentPosition, CourseInputModuleStorage course) {
        if (plannedPath == null || currentPathIndex >= plannedPath.size()) {
            System.out.println("Planning new path...");
            plannedPath = planPath(currentPosition, course.getTargetPosition(), course);
            currentPathIndex = 1; // skip the current position
            System.out.println("Planned path: " + plannedPath.size() + " steps");
        }
        double[] currentTarget = plannedPath.get(currentPathIndex);

        double distanceToTarget = Math.hypot(currentTarget[0] - currentPosition[0], currentTarget[1] - currentPosition[1]);
        if (distanceToTarget < 0.5 || currentPathIndex >= plannedPath.size() - 1) { // if we are close to the target or at the end of the path
            currentPathIndex++; // move to the next point in the path
            currentTarget = plannedPath.get(currentPathIndex);
            System.out.println("Reached checkpoint, moving to next target: " + currentPathIndex);
        }
        
        // We create a fake course where the target is the current target.
        CourseInputModuleStorage fakeCourse = new CourseInputModuleStorage(
                course.heightFunction, 
                course.muK, course.muS, 
                currentPosition[0], currentPosition[1], 
                currentTarget[0], currentTarget[1], // <-- Trick Newton Bot into aiming here
                0.2, // Waypoint tolerance radius
                course.stepSize);

            if (course.getObstacles() != null) {
            for (model.obstacles.Obstacle o : course.getObstacles()) {
                fakeCourse.addObstacle(o);
            }
        }

        Newton_Raphson_Bot newtonBot = new Newton_Raphson_Bot(dt, maxTime, solverType);
        return newtonBot.computeShot(currentPosition, fakeCourse);
    }

    private List<double[]> planPath(double[] start, double[] target, CourseInputModuleStorage course) {
        return PathPlanner.findCheckPoints(course, start, target);
    }

    public void reset() {
        plannedPath = null;
        currentPathIndex = 0;
    }
}
