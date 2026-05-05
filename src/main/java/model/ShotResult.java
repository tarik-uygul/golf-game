package model;

import java.util.List;

public class ShotResult {

    public enum Outcome {
        IN_TARGET, IN_WATER, STOPPED, TIMEOUT, OUT_OF_BOUNDS
    }

    private final List<double[]> path;
    private final Outcome outcome;
    private final double[] finalState;

    public ShotResult(List<double[]> path, Outcome outcome, double[] finalState) {
        this.path = path;
        this.outcome = outcome;
        this.finalState = finalState;
    }

    public List<double[]> getPath() {
        return path;
    }

    public Outcome getOutcome() {
        return outcome;
    }

    public double[] getFinalState() {
        return finalState;
    }

    public double getFinalX() {
        return finalState[0];
    }

    public double getFinalY() {
        return finalState[1];
    }
}
