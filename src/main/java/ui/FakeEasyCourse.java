package ui;

import io.CourseInputModule;
import io.CourseInputModuleStorage;
import model.obstacles.Water;

public class FakeEasyCourse {

    public static CourseInputModuleStorage build() throws Exception {

        CourseInputModule processor = new CourseInputModule();

        CourseInputModuleStorage course = processor.buildConfig(
            "0.25*sin((x+y)/10)+1",
            "0.08", "0.2",
            "7.0", "8.0",
            "14.0", "1.0",
            "0.1", "0.01"
        );
        
        // Add some water hazards to make the course interesting
        course.addObstacle(new Water(10.0, 10.0, 1.5));  // large water hazard in middle
        course.addObstacle(new Water(5.0, 5.0, 0.8));    // smaller water on left
        course.addObstacle(new Water(12.0, 3.0, 1.0));   // water near the hole
        
        return course;
    }
}
