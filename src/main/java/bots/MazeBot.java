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
        // 1. Generate path from A* if empty
        if (plannedPath == null) {
            System.out.println("MazeBot: Planning new path around obstacles...");
            plannedPath = planPath(currentPosition, course.getTargetPosition(), course);
            currentPathIndex = 1;
            System.out.println("MazeBot: Path found with " + plannedPath.size() + " waypoints.");
        }

        // Auto-advance checking: if we are already close enough to the active checkpoint, pop to next
        while (currentPathIndex < plannedPath.size() - 1) {
            double[] wp = plannedPath.get(currentPathIndex);
            double distToWp = Math.hypot(wp[0] - currentPosition[0], wp[1] - currentPosition[1]);
            if (distToWp < 0.6) {
                currentPathIndex++;
            } else {
                break;
            }
        }

       // 2. Lookahead scanner: Find furthest visible waypoint on remaining path
        int furthestVisibleIndex = currentPathIndex;
        for (int i = plannedPath.size() - 1; i >= currentPathIndex; i--) {
            double[] wp = plannedPath.get(i);
            double distToWp = Math.hypot(wp[0] - currentPosition[0], wp[1] - currentPosition[1]);
            
            // FIX 4: Look-ahead distance cap. Stops the bot from taking crazy shortcuts across the map
            if (i != plannedPath.size() - 1 && distToWp > 4.0) {
                continue;
            }

            if (isLineOfSightClear(currentPosition, wp, course)) {
                furthestVisibleIndex = i;
                break;
            }
        }
        
        currentPathIndex = furthestVisibleIndex;
        double[] currentTarget = plannedPath.get(currentPathIndex);

        // 3. Setup Fake Course Container to isolate intermediate waypoint targets
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

        // FIX 5: Full integration with Hill Climbing Engine
        System.out.println("MazeBot: Firing Hill-Climbing towards checkpoint index " + currentPathIndex);
        Hill_Climbing_Bot hcBot = new Hill_Climbing_Bot(dt, maxTime, solverType);
        return hcBot.computeShot(currentPosition, fakeCourse);
    }

    private boolean isLineOfSightClear(double[] start, double[] end, CourseInputModuleStorage course) {
        double dist = Math.hypot(end[0] - start[0], end[1] - start[1]);
        int steps = (int) Math.ceil(dist / 0.1); // High-density check step (every 10cm)

        for (int i = 1; i <= steps; i++) {
            double t = (double) i / steps;
            double x = start[0] + t * (end[0] - start[0]);
            double y = start[1] + t * (end[1] - start[1]);

            // FIX 6: Explicit map boundary checks with a 35cm Wall Buffer!
            if (x < 0.35 || x > course.getCourseWidth() - 0.35 || y < 0.35 || y > course.getCourseHeight() - 0.35) {
                return false;
            }

            // Check math function water levels
            if (course.getHeight(x, y) < 0) return false;

            // FIX 7: Thick Laser Raycasting. Adds a 35cm safety radius buffer around trees
            // to stop the path scanner from picking a path that dangerously grazes an edge.
            if (course.getObstacles() != null) {
                for (model.obstacles.Obstacle o : course.getObstacles()) {
                    double dx = x - o.getX();
                    double dy = y - o.getY();
                    if (Math.hypot(dx, dy) < o.getRadius() + 0.35) {
                        return false; 
                    }
                }
            }
        }
        return true; 
    }

    private List<double[]> planPath(double[] start, double[] target, CourseInputModuleStorage course) {
        return PathPlanner.findCheckPoints(course, start, target);
    }

    public void reset() {
        plannedPath = null;
        currentPathIndex = 0;
    }
}