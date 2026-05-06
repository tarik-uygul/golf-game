package bots;

import io.CourseInputModule;
import io.CourseInputModuleStorage;
import model.CourseProfile;

public interface GolfBot {
    double[] computeShot(double[] currentPosition, CourseInputModuleStorage course);
}
// edited just now
