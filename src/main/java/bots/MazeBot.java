package bots;

import io.CourseInputModuleStorage;
import java.util.List;

public class MazeBot implements GolfBot {

    private final double dt;
    private final double maxTime;
    private final String solverType;

    private List<double[]> plannedPath = null;
    private int currentPathIndex = 0;

    public MazeBot(String solverType, double dt, double maxTime) {
        this.solverType = solverType;
        this.dt = dt;
        this.maxTime = maxTime;
    }

    @Override
    public double[] computeShot(double[] currentPosition, CourseInputModuleStorage course) {
        // 1. Plan path if we don't have one
        if (plannedPath == null) {
            System.out.println("Planning new path...");
            plannedPath = planPath(currentPosition, course.getTargetPosition(), course);
            currentPathIndex = 1;
            System.out.println("Planned path: " + plannedPath.size() + " steps");
        }

        // 2. FIX FOR FREEZING: Prevent IndexOutOfBounds exception!
        if (currentPathIndex >= plannedPath.size()) {
            currentPathIndex = plannedPath.size() - 1;
        }

        // 3. FIX FOR SLOW SHOTS: Line of Sight targeting.
        // Look at the furthest waypoint first. If the path is clear, skip all the waypoints in between!
        for (int i = plannedPath.size() - 1; i > currentPathIndex; i--) {
            if (isLineOfSightClear(currentPosition, plannedPath.get(i), course)) {
                currentPathIndex = i;
                break;
            }
        }

        double[] currentTarget = plannedPath.get(currentPathIndex);
        double distanceToTarget = Math.hypot(currentTarget[0] - currentPosition[0], currentTarget[1] - currentPosition[1]);

        // 4. Advance if we are extremely close and it is NOT the final hole
        if (distanceToTarget < 0.5 && currentPathIndex < plannedPath.size() - 1) {
            currentPathIndex++;
            currentTarget = plannedPath.get(currentPathIndex);
        }

        // We create a fake course where the target is the current target.
        CourseInputModuleStorage fakeCourse = new CourseInputModuleStorage(
                course.heightFunction,
                course.muK, course.muS,
                currentPosition[0], currentPosition[1],
                currentTarget[0], currentTarget[1], 
                0.0, // <-- 0.0 prevents simulator "swallowing" the ball mid-air
                course.stepSize);

        if (course.getObstacles() != null) {
            for (model.obstacles.Obstacle o : course.getObstacles()) {
                fakeCourse.addObstacle(o);
            }
        }

        Newton_Raphson_Bot newtonBot = new Newton_Raphson_Bot(dt, maxTime, solverType);
        return newtonBot.computeShot(currentPosition, fakeCourse);
    }

    // Casts a "ray" from the ball to the target to see if we can shoot straight there
    private boolean isLineOfSightClear(double[] start, double[] end, CourseInputModuleStorage course) {
        double dist = Math.hypot(end[0] - start[0], end[1] - start[1]);
        int steps = (int) Math.ceil(dist / 0.2); // Check every 20cm along the line
        
        for (int i = 1; i <= steps; i++) {
            double t = (double) i / steps;
            double x = start[0] + t * (end[0] - start[0]);
            double y = start[1] + t * (end[1] - start[1]);
            
            // Check if this spot is in water (negative height)
            if (course.getHeight(x, y) < 0) return false;
            
            // Check if this spot hits a placed obstacle
            if (course.getObstacles() != null) {
                for (model.obstacles.Obstacle o : course.getObstacles()) {
                    if (o.contains(x, y)) return false;
                }
            }
        }
        return true; // The path is totally clear!
    }

    private List<double[]> planPath(double[] start, double[] target, CourseInputModuleStorage course) {
        return PathPlanner.findCheckPoints(course, start, target);
    }

    public void reset() {
        plannedPath = null;
        currentPathIndex = 0;
    }
}