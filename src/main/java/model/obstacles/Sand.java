package model.obstacles;

// rolling onto sand replaces the surface friction with higher values,
// so the ball decelerates much faster (handled by the simulator/physics)
public class Sand implements Obstacle {

    // grass uses µK 0.05–0.1 / µS 0.1–0.2; sand is significantly grippier
    public static final double DEFAULT_MU_K = 0.3;
    public static final double DEFAULT_MU_S = 0.4;

    private final double x;
    private final double y;
    private final double radius;
    private final double muK;
    private final double muS;

    public Sand(double x, double y, double radius) {
        this(x, y, radius, DEFAULT_MU_K, DEFAULT_MU_S);
    }

    public Sand(double x, double y, double radius, double muK, double muS) {
        if (radius <= 0) {
            throw new IllegalArgumentException("Sand radius must be positive (got " + radius + ")");
        }
        if (muK <= 0 || muS <= 0) {
            throw new IllegalArgumentException("Sand friction must be positive (got µK=" + muK + ", µS=" + muS + ")");
        }
        if (muK >= muS) {
            throw new IllegalArgumentException("µS must be greater than µK (got µK=" + muK + ", µS=" + muS + ")");
        }
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.muK = muK;
        this.muS = muS;
    }

    @Override
    public boolean contains(double px, double py) {
        double dx = px - x;
        double dy = py - y;
        return dx * dx + dy * dy <= radius * radius;
    }

    @Override
    public double getX() {
        return x;
    }

    @Override
    public double getY() {
        return y;
    }

    @Override
    public double getRadius() {
        return radius;
    }

    public double getKineticFriction() {
        return muK;
    }

    public double getStaticFriction() {
        return muS;
    }
}
