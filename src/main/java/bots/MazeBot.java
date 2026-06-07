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
        
       
      
        // If there are no obstacles between the ball and the actual hole,
        // throw away the maze logic and take the winning shot immediately!
        double[] finalTarget = course.getTargetPosition();
        if (isLineOfSightClear(currentPosition, finalTarget, course)) {
            System.out.println("Direct line of sight to hole! Taking the winning shot.");
            // We use Hill Climbing here because it handles final approach physics beautifully.
            // Notice we pass the REAL 'course' object so it uses the real target radius!
            Hill_Climbing_Bot finisherBot = new Hill_Climbing_Bot(dt, maxTime, solverType);
            return finisherBot.computeShot(currentPosition, course); 
        }

        // 2. Plan path if we don't have one
        if (plannedPath == null) {
            System.out.println("Planning new path...");
            plannedPath = planPath(currentPosition, course.getTargetPosition(), course);
            currentPathIndex = 1;
            System.out.println("Planned path: " + plannedPath.size() + " steps");
        }

        // Prevent IndexOutOfBounds exception
        if (currentPathIndex >= plannedPath.size()) {
            currentPathIndex = plannedPath.size() - 1;
        }

        // 3. Line of Sight targeting for waypoints
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

        // If our current waypoint is the final hole, use the real radius instead of 0.0
        boolean isFinalWaypoint = (currentPathIndex == plannedPath.size() - 1);
        double radiusToUse = isFinalWaypoint ? course.getTargetRadius() : 0.0;

        CourseInputModuleStorage fakeCourse = new CourseInputModuleStorage(
                course.heightFunction,
                course.muK, course.muS,
                currentPosition[0], currentPosition[1],
                currentTarget[0], currentTarget[1],
                radiusToUse, // 
                course.stepSize);

        if (course.getObstacles() != null) {
            for (model.obstacles.Obstacle o : course.getObstacles()) {
                fakeCourse.addObstacle(o);
            }
        }

        // Use the sub-bot to hit the ball to the waypoint
        Hill_Climbing_Bot waypointBot = new Hill_Climbing_Bot(dt, maxTime, solverType);
        return waypointBot.computeShot(currentPosition, fakeCourse);
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
        return true; // The path is totally clear
    }

    private List<double[]> planPath(double[] start, double[] target, CourseInputModuleStorage course) {
        return PathPlanner.findCheckPoints(course, start, target);
    }

    public void reset() {
        plannedPath = null;
        currentPathIndex = 0;
    }
}