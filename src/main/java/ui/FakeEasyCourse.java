package ui;

import io.CourseInputModule;
import io.CourseInputModuleStorage;

public class FakeEasyCourse {

    public static CourseInputModuleStorage build() throws Exception {

        CourseInputModule processor = new CourseInputModule();

        return processor.buildConfig(
            "0.25*sin((x+y)/10)+1",
            "0.08", "0.2",
            "7.0", "8.0",
            "14.0", "1.0",
            "0.1", "0.01"
        );
    }
}
