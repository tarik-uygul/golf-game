package ui;

import io.CourseInputModule;
import io.CourseInputModuleStorage;
import model.obstacles.Sand;
import model.obstacles.Tree;
import model.obstacles.Water;

public class FakeEasyCourse {

    // Easy course: start (7,8) → target (11,5).
    //
    // Two water hazards create a gate that the direct path barely threads
    // (only 0.1m clearance on each side). The direct shot works but has a
    // tiny basin — any noise sends the ball into water.
    //
    // The sides of the course are completely open, so a curved shot that goes
    // above the upper hazard (shooting slightly north-east and letting the bowl
    // terrain funnel it south into the hole) has a wide basin with no obstacles
    // in its way. SensitivityRanker finds this and picks it in robust mode,
    // visibly firing at a different angle than the direct shot.
    //
    // Height: bowl centered on target — slopes toward hole from all directions.
    //   h = 1.5 - 0.30*cos((x-11)/2) - 0.30*cos((y-5)/2)
    //   |dh/dx|, |dh/dy| <= 0.15  (at project spec limit)
    //   |d2h/dx2|, |d2h/dy2| <= 0.075  (within spec limit of 0.1/m)
    //
    // Gate geometry (line 3x+4y=53 is the direct path):
    //   perp-distance from each water center to direct path = 0.8m
    //   radius 0.7m  →  clearance = 0.1m  →  total gate width = 0.2m
    public static CourseInputModuleStorage build() throws Exception {
        CourseInputModule processor = new CourseInputModule();

        CourseInputModuleStorage course = processor.buildConfig(
            "1.5-0.30*cos((x-11)/2)-0.30*cos((y-5)/2)",
            "0.08", "0.18",
            "7.0", "8.0",
            "11.0", "5.0",
            "0.1", "0.01"
        );

        // gate: direct path threads exactly between these two, 0.1m clearance each side
        course.addObstacle(new Water(9.0, 7.5, 0.7));  // upper hazard
        course.addObstacle(new Water(9.0, 5.5, 0.7));  // lower hazard

        // decorative — far from all approach paths
        course.addObstacle(new Tree(4.0,  3.0, 0.5));
        course.addObstacle(new Sand(14.0, 2.0, 1.2));

        return course;
    }
}
