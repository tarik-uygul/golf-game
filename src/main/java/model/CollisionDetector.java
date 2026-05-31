package model;

import io.CourseInputModuleStorage;
import model.obstacles.Obstacle;

public class CollisionDetector {

    private final CourseInputModuleStorage course;

    public CollisionDetector(CourseInputModuleStorage course) {
        this.course = course;
    }

    public Obstacle getCollidingObstacle(double x, double y) {
        for (Obstacle obstacle : course.getObstacles()) {
            if (obstacle.contains(x, y)) {
                return obstacle;
            }
        }
        return null;
    }

    public boolean isOutOfBounds(double x, double y) {
        return x < 0
                || y < 0
                || x > course.getCourseWidth()
                || y > course.getCourseHeight();
    }
}
