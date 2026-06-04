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

       // 2. Find the FURTHEST visible waypoint on the remaining path
        int furthestVisibleIndex = currentPathIndex;
        for (int i = plannedPath.size() - 1; i >= currentPathIndex; i--) {
            if (isLineOfSightClear(currentPosition, plannedPath.get(i), course)) {
                furthestVisibleIndex = i;
                break;
            }
        }
        
        // Update our official progress
        currentPathIndex = furthestVisibleIndex;
        double[] currentTarget = plannedPath.get(currentPathIndex);

        // 3. The "Peek Around the Corner" Fix
        // If we are extremely close to our target waypoint but STILL can't see the next one,
        // we are trapped on the corner. We project our target 1.5m towards the next waypoint
        // to force the bot to hit a firm shot around the bend.
        if (currentPathIndex < plannedPath.size() - 1) {
            double distanceToTarget = Math.hypot(currentTarget[0] - currentPosition[0], currentTarget[1] - currentPosition[1]);
            
            if (distanceToTarget < 0.5) {
                double[] nextWaypoint = plannedPath.get(currentPathIndex + 1);
                double dx = nextWaypoint[0] - currentPosition[0];
                double dy = nextWaypoint[1] - currentPosition[1];
                double angleToNext = Math.atan2(dy, dx);
                
                // Nudge the temporary target 1.5 meters around the corner
                currentTarget = new double[] {
                    currentPosition[0] + 1.5 * Math.cos(angleToNext),
                    currentPosition[1] + 1.5 * Math.sin(angleToNext)
                };
            }
        }

        // 4. Create the Fake Course
        // Use the actual strict target radius for the final hole, but be loose (0.4m) on intermediate waypoints
        double fakeTolerance = (currentPathIndex == plannedPath.size() - 1) ? course.getTargetRadius() : 0.4;

        CourseInputModuleStorage fakeCourse = new CourseInputModuleStorage(
                course.heightFunction,
                course.muK, course.muS,
                currentPosition[0], currentPosition[1],
                currentTarget[0], currentTarget[1],
                fakeTolerance, 
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
