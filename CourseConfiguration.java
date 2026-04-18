/**
 * All the containers we need I think
 * If not at least this class can be easily changed
 */

public class CourseConfiguration {

    public HeightFunction heightFunction;
    public double muK;
    public double muS;
    public double startX, startY;
    public double targetX, targetY;
    public double targetRadius;
    public double stepSize;

    public CourseConfiguration(HeightFunction heightFunction, double muK, double muS, double startX, double startY,
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
}