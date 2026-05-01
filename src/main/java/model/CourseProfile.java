package model;

import javafx.scene.paint.Color;

public interface CourseProfile {
    double getHeight(double x, double y);

    double getSlopeX(double x, double y);

    double getSlopeY(double x, double y);

    double getKineticFriction();

    double getStaticFriction();

    double[] getStartPosition();

    double[] getTargetPosition();

    double getTargetRadius();

    double getCourseWidth();

    double getCourseHeight();

    // the default color for water/grass, can change this by overriding in the
    // method (or change it the default color here)
    default Color getGrassColor() {
        return Color.FORESTGREEN;
    }

    default Color getHighColor() {
        return Color.DARKGREEN;
    }

    default Color getWaterColor() {
        return Color.CORNFLOWERBLUE;
    }
}
