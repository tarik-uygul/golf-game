package everything;

import javafx.scene.paint.Color;

public class CourseConfigurationProfile implements CourseProfile {
    private final CourseConfiguration config;

    public CourseConfigurationProfile(CourseConfiguration config) {
        this.config = config;
    }

    @Override public double getHeight(double x, double y)  { return config.heightFunction.evaluate(x, y); }
    @Override public double getSlopeX(double x, double y)  { return config.heightFunction.dhdx(x, y); }
    @Override public double getSlopeY(double x, double y)  { return config.heightFunction.dhdy(x, y); }
    @Override public double getKineticFriction()           { return config.muK; }
    @Override public double getStaticFriction()            { return config.muS; }
    @Override public double[] getStartPosition()           { return new double[]{config.startX, config.startY}; }
    @Override public double[] getTargetPosition()          { return new double[]{config.targetX, config.targetY}; }
    @Override public double getTargetRadius()              { return config.targetRadius; }
    @Override public double getCourseWidth()               { return 20.0; } // adjust as needed
    @Override public double getCourseHeight()              { return 20.0; }
}
