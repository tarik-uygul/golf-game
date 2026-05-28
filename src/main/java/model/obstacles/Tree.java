package model.obstacles;

// hitting a tree triggers a penalty stroke (handled by the simulator)
public class Tree implements Obstacle {

    private final double x;
    private final double y;
    private final double radius;

    public Tree(double x, double y, double radius) {
        if (radius <= 0) {
            throw new IllegalArgumentException("Tree radius must be positive (got " + radius + ")");
        }
        this.x = x;
        this.y = y;
        this.radius = radius;
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
}
