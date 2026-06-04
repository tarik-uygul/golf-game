package ui;

import io.CourseInputModule;
import io.CourseInputModuleStorage;
import model.obstacles.Tree;

public class FakeMediumCourse {

    // Medium course: start (3,5) → target (16,13) on a 20×20 course.
    //
    // Three trees form a cluster that completely blocks the direct path.
    // The two main blockers (9.5,9.0) and (11.5,9.5) sit right on the line
    // from start to target. The upper cap tree (10.5,11.0) sits just above
    // the direct path, so the ball cannot slip between the main blockers and
    // the cap — it must arc high enough to clear all three (y > 12.2 near
    // x=10.5), then descend into the bowl around the target.
    //
    // A southern flank tree (8.0,6.5) discourages the far-under bypass.
    //
    // Bowl terrain near target gives moderate forgiveness:
    //   h = 1.4 + 0.10*sin(x/4) - 0.32*exp(-((x-16)²+(y-13)²)/12)
    //   max |dh/dx| ≈ 0.025 + 0.079 = 0.104  (spec limit 0.15)  ✓
    //   max |dh/dy| ≈ 0.079                                       ✓
    //   max curvature at target centre: 0.32*2/12 ≈ 0.053         ✓
    //
    // The real basin under MEDIUM noise (±0.05 m position) is moderate —
    // a decent but not guaranteed chance of scoring under noise.
    public static CourseInputModuleStorage build() throws Exception {

        CourseInputModule processor = new CourseInputModule();

        CourseInputModuleStorage course = processor.buildConfig(
            "1.4+0.10*sin(x/4)-0.32*exp(-((x-16)*(x-16)+(y-13)*(y-13))/12)",
            "0.07", "0.14",
            "3.0", "5.0",
            "16.0", "13.0",
            "0.1", "0.01"
        );

        // tree cluster blocking direct path — ball must arc above all three
        course.addObstacle(new Tree(9.5,  9.0,  1.3));  // main left blocker  (on direct path)
        course.addObstacle(new Tree(11.5, 9.5,  1.3));  // main right blocker (on direct path)
        course.addObstacle(new Tree(10.5, 11.0, 1.2));  // upper cap — limits how high the arc can go

        // southern flank — discourages far-under bypass
        course.addObstacle(new Tree(8.0, 6.5, 1.0));

        return course;
    }
}
