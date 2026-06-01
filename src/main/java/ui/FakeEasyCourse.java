package ui;

import io.CourseInputModule;
import io.CourseInputModuleStorage;
import model.obstacles.Sand;
import model.obstacles.Tree;
import model.obstacles.Water;

public class FakeEasyCourse {

    // Easy course: start (7,8) → target (14,1), ~10m shot at -45°.
    // Same distance as the original so all bots can reliably score.
    // Two water pools flank the direct path creating a narrow "channel",
    // plus a tree and sand patch for visual variety.
    public static CourseInputModuleStorage build() throws Exception {
        CourseInputModule processor = new CourseInputModule();

        CourseInputModuleStorage course = processor.buildConfig(
            "0.25*sin((x+y)/10)+1",
            "0.08", "0.18",
            "7.0", "8.0",
            "14.0", "1.0",
            "0.1", "0.01"
        );

        // water pools above and below the direct -45° path (path y = -x+15):
        // path at x=10 is y=5, so centers at y=7.5 and y=2.5 give 2.5m clearance each side
        course.addObstacle(new Water(10.0, 7.5, 1.5));
        course.addObstacle(new Water(10.0, 2.5, 1.5));
        // decorative obstacles that don't block any bot angle
        course.addObstacle(new Tree(4.0,   3.0, 0.5));
        course.addObstacle(new Tree(17.0,  6.0, 0.5));
        course.addObstacle(new Sand(16.0, 12.0, 1.5));

        return course;
    }
}
