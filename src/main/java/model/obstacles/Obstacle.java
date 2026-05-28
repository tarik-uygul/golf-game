package model.obstacles;

// shared API the simulator/renderer can use without caring which obstacle it is
public interface Obstacle {

    // is the given world-coordinate point inside this obstacle?
    boolean contains(double x, double y);

    double getX();

    double getY();

    double getRadius();
}
