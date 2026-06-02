package bots;

import io.CourseInputModuleStorage;

public interface GolfBot {
    double[] computeShot(double[] currentPosition, CourseInputModuleStorage course);
    default int getLastIterationCount() { return -1; }
}
// edited just now
