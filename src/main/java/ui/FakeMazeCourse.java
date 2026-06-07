package ui;

import io.CourseInputModule;
import io.CourseInputModuleStorage;
import model.obstacles.Tree;

public class FakeMazeCourse {

    // Maze course: start (2,2) → target (18,18) on a flat 20x20 course.
    //
    // This creates an "S" shaped corridor using densely packed trees.
    // - Wall 1 (x=6) forces the ball to travel UP to y=17 to pass.
    // - Wall 2 (x=14) forces the ball to travel DOWN to y=3 to pass.
    // 
    // Because the line-of-sight is broken, a bot cannot hit the target in 1 or 2 shots.
    // It is physically forced to make at least 3 navigational shots:
    // Shot 1: (2,2) -> (4, 18)    [Navigate to first gap]
    // Shot 2: (4,18) -> (16, 2)   [Navigate through middle corridor to second gap]
    // Shot 3: (16,2) -> (18, 18)  [Navigate to hole]
    public static CourseInputModuleStorage build() throws Exception {

        CourseInputModule processor = new CourseInputModule();

        // 1. Flat terrain ("0.0") with standard grass friction
        CourseInputModuleStorage course = processor.buildConfig(
            "0.0",   // Flat height function
            "0.07",  // muK (kinetic friction)
            "0.14",  // muS (static friction)
            "2.0",   // startX
            "2.0",   // startY
            "18.0",  // targetX
            "18.0",  // targetY
            "0.15",  // targetRadius (slightly larger to ensure bots register the final hit)
            "0.01"   // stepSize
        );

        // 2. Build Wall 1 (Left side, gap at the TOP)
        // Spaced every 0.5 meters to ensure overlapping hitboxes (solid wall)
        for (double y = 0.0; y <= 16.0; y += 0.5) {
            course.addObstacle(new Tree(6.0, y, 0.5));
            // Add thickness to the wall so the bot doesn't try to clip it
            course.addObstacle(new Tree(6.5, y, 0.5)); 
        }

        // 3. Build Wall 2 (Right side, gap at the BOTTOM)
        for (double y = 4.0; y <= 20.0; y += 0.5) {
            course.addObstacle(new Tree(14.0, y, 0.5));
            // Add thickness
            course.addObstacle(new Tree(13.5, y, 0.5));
        }

        // 4. (Optional) Corner cap trees to prevent the bot from trying to bounce 
        // off the absolute map boundaries out-of-bounds to bypass the walls
        course.addObstacle(new Tree(6.0, -1.0, 1.0));
        course.addObstacle(new Tree(14.0, 21.0, 1.0));

        return course;
    }
}
