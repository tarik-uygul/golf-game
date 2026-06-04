package ui;

import io.CourseInputModule;
import io.CourseInputModuleStorage;

public class FakeHardCourse {

    // Hard course: undulating terrain with a central lake that blocks the
    // direct path from start (3,4) to target (14,12). The direct angle passes
    // through water, so the player must arc north of the lake to reach the hole.
    // The real basin under noise is essentially zero-width — there is barely
    // one trajectory that reliably scores under position noise.
    public static CourseInputModuleStorage build() throws Exception {

        CourseInputModule processor = new CourseInputModule();

        return processor.buildConfig(
            "1.2+0.5*sin(x/5)*cos(y/5)-2.2*exp(-((x-10)*(x-10)+(y-8)*(y-8))/10)",
            "0.08", "0.18",
            "3.0", "4.0",
            "14.0", "12.0",
            "0.1", "0.01"
        );
    }
}
