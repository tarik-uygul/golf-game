package bots;

import io.CourseInputModuleStorage;
import model.GolfSimulator;
import model.ShotResult;
import model.ShotResult.Outcome;

public class RuleBasedBot implements GolfBot {
    private static final double MAX_SPEED = 5.0;
    private static final int ANGLE_STEPS = 3;
    private static final int SPEED_STEPS = 3;

    private final double dt;
    private final double maxTime;

    public RuleBasedBot(double dt, double maxTime) {
        this.dt = dt;
        this.maxTime = maxTime;
    }

    @Override
    public double[] computeShot(double[] currentPosition, CourseInputModuleStorage course) {
        GolfSimulator simulator = new GolfSimulator(course, "rk4", dt, maxTime);
        double[] target = course.getTargetPosition();

        double dx = target[0] - currentPosition[0];
        double dy = target[1] - currentPosition[1];
        double baseAngle = Math.atan2(dy, dx);

        ShotChoice best = searchAround(
                simulator, course, currentPosition,
                baseAngle,
                Math.PI / 2,
                ANGLE_STEPS,
                SPEED_STEPS,
                0.8,
                MAX_SPEED);

        return new double[] { best.vx, best.vy };
    }

    private ShotChoice searchAround(GolfSimulator simulator,
            CourseInputModuleStorage course,
            double[] currentPosition,
            double centerAngle,
            double angleSpread,
            int angleSteps,
            int speedSteps,
            double minSpeed,
            double maxSpeed) {

        double bestScore = Double.POSITIVE_INFINITY;
        ShotChoice best = new ShotChoice(0, 0, bestScore);
        double[] target = course.getTargetPosition();

        for (int i = 0; i < angleSteps; i++) {
            double angle = centerAngle - angleSpread / 2 + i * (angleSpread / (angleSteps - 1));

            for (int j = 0; j < speedSteps; j++) {
                double speed = minSpeed + j * ((maxSpeed - minSpeed) / (speedSteps - 1));

                double vx = speed * Math.cos(angle);
                double vy = speed * Math.sin(angle);

                ShotResult result = simulator.simulate(currentPosition, new double[] { vx, vy });

                if (result.getOutcome() == ShotResult.Outcome.IN_TARGET) {
                    return new ShotChoice(vx, vy, -1);
                }

                double distance = calculateDistance(result.getFinalX(), result.getFinalY(), target);
                double score = calculateScore(result.getOutcome(), distance);

                if (score < bestScore) {
                    bestScore = score;
                    best = new ShotChoice(vx, vy, score);
                }
            }
        }

        return best;
    }

    private double calculateDistance(double x, double y, double[] target) {
        double dx = x - target[0];
        double dy = y - target[1];
        return Math.sqrt(dx * dx + dy * dy);
    }

    private double calculateScore(ShotResult.Outcome outcome, double distance) {
        switch (outcome) {
            case IN_WATER:
                return 1000 + distance;
            case TIMEOUT:
                return 500 + distance;
            case STOPPED:
            default:
                return distance;
        }
    }

    private static class ShotChoice {
        final double vx;
        final double vy;
        final double score;

        ShotChoice(double vx, double vy, double score) {
            this.vx = vx;
            this.vy = vy;
            this.score = score;
        }
    }
}