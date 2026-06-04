package physics;

import io.CourseInputModuleStorage;
import model.ShotResult;

// checks course-level boundaries only: terrain water (height < 0) and out-of-bounds
// for obstacle objects (trees, water hazards, sand) see ObstacleCollisionDetector
public class CourseCollisionDetector implements CollisionDetector {

    private final CourseInputModuleStorage course;

    public CourseCollisionDetector(CourseInputModuleStorage course) {
        this.course = course;
    }

    public ShotResult.Outcome checkCourse(double x, double y) {
        if (course.getHeight(x, y) < 0) {
            return ShotResult.Outcome.IN_WATER;
        }
        if (x < 0 || x > course.getCourseWidth() || y < 0 || y > course.getCourseHeight()) {
            return ShotResult.Outcome.OUT_OF_BOUNDS;
        }
        return null;
    }

    @Override
    public ShotResult.Outcome checkTerminal(double x, double y) {
        return checkCourse(x, y);
    }

    @Override
    public double[] getSurfaceFriction(double x, double y) {
        return new double[] { course.getMuK(), course.getMuS() };
    }
}
