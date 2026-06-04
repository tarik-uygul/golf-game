package model;

import java.util.Random;

public class ShotNoise {

    private static final double MAX_SPEED = 5.0;

    // Gaussian (realistic): sigma ~1% of max speed, models a skilled human's execution error
    private static final double SIGMA_GAUSSIAN = 0.05;

    // Uniform half-ranges for small / medium / large perturbation tiers
    private static final double RANGE_SMALL  = 0.10;
    private static final double RANGE_MEDIUM = 0.35;
    private static final double RANGE_LARGE  = 0.90;

    // Position half-ranges (meters) — kept small so the shift is physically plausible
    private static final double POS_SIGMA_GAUSSIAN = 0.01;
    private static final double POS_RANGE_SMALL    = 0.02;
    private static final double POS_RANGE_MEDIUM   = 0.05;
    private static final double POS_RANGE_LARGE    = 0.12;

    /**
     * Returns a new velocity vector with noise added according to the chosen mode.
     * The result is clamped so its magnitude never exceeds vmax = 5 m/s (Table 1).
     * Returns the original array unchanged when mode is NONE.
     */
    public static double[] applyToVelocity(double[] velocity, NoiseMode mode, Random rng) {
        if (mode == NoiseMode.NONE) return velocity;

        double dvx, dvy;
        switch (mode) {
            case GAUSSIAN:
                dvx = rng.nextGaussian() * SIGMA_GAUSSIAN;
                dvy = rng.nextGaussian() * SIGMA_GAUSSIAN;
                break;
            case SMALL:
                dvx = (rng.nextDouble() * 2 - 1) * RANGE_SMALL;
                dvy = (rng.nextDouble() * 2 - 1) * RANGE_SMALL;
                break;
            case MEDIUM:
                dvx = (rng.nextDouble() * 2 - 1) * RANGE_MEDIUM;
                dvy = (rng.nextDouble() * 2 - 1) * RANGE_MEDIUM;
                break;
            case LARGE:
                dvx = (rng.nextDouble() * 2 - 1) * RANGE_LARGE;
                dvy = (rng.nextDouble() * 2 - 1) * RANGE_LARGE;
                break;
            default:
                return velocity;
        }

        double vx = velocity[0] + dvx;
        double vy = velocity[1] + dvy;

        double speed = Math.sqrt(vx * vx + vy * vy);
        if (speed > MAX_SPEED) {
            vx = vx / speed * MAX_SPEED;
            vy = vy / speed * MAX_SPEED;
        }

        return new double[] { vx, vy };
    }

    /**
     * Returns a new position vector with a small random offset applied.
     * Returns the original array unchanged when mode is NONE.
     */
    public static double[] applyToPosition(double[] position, NoiseMode mode, Random rng) {
        if (mode == NoiseMode.NONE) return position;

        double dx, dy;
        switch (mode) {
            case GAUSSIAN:
                dx = rng.nextGaussian() * POS_SIGMA_GAUSSIAN;
                dy = rng.nextGaussian() * POS_SIGMA_GAUSSIAN;
                break;
            case SMALL:
                dx = (rng.nextDouble() * 2 - 1) * POS_RANGE_SMALL;
                dy = (rng.nextDouble() * 2 - 1) * POS_RANGE_SMALL;
                break;
            case MEDIUM:
                dx = (rng.nextDouble() * 2 - 1) * POS_RANGE_MEDIUM;
                dy = (rng.nextDouble() * 2 - 1) * POS_RANGE_MEDIUM;
                break;
            case LARGE:
                dx = (rng.nextDouble() * 2 - 1) * POS_RANGE_LARGE;
                dy = (rng.nextDouble() * 2 - 1) * POS_RANGE_LARGE;
                break;
            default:
                return position;
        }

        return new double[] { position[0] + dx, position[1] + dy };
    }
}
