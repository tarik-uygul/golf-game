package physics;

import model.ShotResult;

public interface CollisionDetector {

    /**
     * Returns a terminal outcome if the ball at (x, y) ends the shot
     * (water, tree, out-of-bounds), or null if no terminal collision.
     */
    ShotResult.Outcome checkTerminal(double x, double y);

    /**
     * Returns {muK, muS} for the surface at (x, y).
     * Overrides course defaults when the ball is inside a Sand obstacle.
     */
    double[] getSurfaceFriction(double x, double y);
}
