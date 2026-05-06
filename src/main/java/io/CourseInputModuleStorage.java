package io;

import javafx.scene.paint.Color;
import model.HeightFunction;

/**
 * All the containers we need I think
 * If not at least this class can be easily changed
 */

public class CourseInputModuleStorage {

    public HeightFunction heightFunction;
    public double muK;
    public double muS;
    public double startX, startY;
    public double targetX, targetY;
    public double targetRadius;
    public double stepSize;

    public CourseInputModuleStorage(HeightFunction heightFunction, double muK, double muS, double startX, double startY,
                        double targetX, double targetY, double targetRadius,
                        double stepSize) {
        this.heightFunction = heightFunction;
        this.muK = muK;
        this.muS = muS;
        this.startX = startX;
        this.startY = startY;
        this.targetX = targetX;
        this.targetY = targetY;
        this.targetRadius = targetRadius;
        this.stepSize = stepSize;
    }

    public double getHeight(double x, double y) {
        return heightFunction.evaluate(x, y);
    }

    public double getSlopeX(double x, double y) {
        // numerical derivative in x direction using central difference
        return heightFunction.dhdx(x, y);
    }

    public double getSlopeY(double x, double y) {
        // numerical derivative in y direction using central difference
        return heightFunction.dhdy(x, y);
    }

    public double getKineticFriction() {
        return muK;
    }

    public double getStaticFriction() {
        return muS;
    }

    public double getMuK() {
        return muK;
    }

    public double getMuS() {
        return muS;
    }

    public double[] getStartPosition() {
        return new double[]{startX, startY};
    }

    public double[] getTargetPosition() {
        return new double[]{targetX, targetY};
    }

    public double getTargetRadius() {
        return targetRadius;
    }

    public double getCourseWidth() {
        return 20.0;
    }

    public double getCourseHeight() {
        return 20.0;
    }

    // the default color for water/grass, can change this by overriding in the
    // method (or change it the default color here)
    public Color getGrassColor() {
        return Color.LIMEGREEN;
    }

    public Color getHighColor() {
        return Color.DARKGREEN;
    }

    public Color getWaterColor() {
        return Color.CORNFLOWERBLUE;
    }
}