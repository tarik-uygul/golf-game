package physics;

import io.CourseInputModuleStorage;
import model.ShotResult;
import model.obstacles.Obstacle;
import model.obstacles.Sand;
import model.obstacles.Tree;
import model.obstacles.Water;

// checks collisions with placed obstacle objects (trees, water hazards, sand traps)
// for course-level boundaries (terrain water, out-of-bounds) see CourseCollisionDetector
public class ObstacleCollisionDetector implements CollisionDetector {

    private final CourseInputModuleStorage course;

    public ObstacleCollisionDetector(CourseInputModuleStorage course) {
        this.course = course;
    }

    @Override
    public ShotResult.Outcome checkTerminal(double x, double y) {
        for (Obstacle obstacle : course.getObstacles()) {
            if (!obstacle.contains(x, y)) continue;
            if (obstacle instanceof Water) return ShotResult.Outcome.IN_WATER;
            if (obstacle instanceof Tree)  return ShotResult.Outcome.HIT_TREE;
        }
        return null;
    }

    @Override
    public double[] getSurfaceFriction(double x, double y) {
        for (Obstacle obstacle : course.getObstacles()) {
            if (obstacle instanceof Sand && obstacle.contains(x, y)) {
                Sand sand = (Sand) obstacle;
                return new double[] { sand.getKineticFriction(), sand.getStaticFriction() };
            }
        }
        return new double[] { course.getMuK(), course.getMuS() };
    }
}
