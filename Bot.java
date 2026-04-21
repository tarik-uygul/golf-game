public abstract class Bot extends Thread {
    public abstract Vector2D getNextShot(CourseProfile course, Vector2D ballPosition, Vector2D holePosition);

    // Simulates a golf shot given a course, ball position, and shot vector
    // Returns where the ball lands
    protected Vector2D simulateShot(CourseProfile course, Vector2D ballPosition, Vector2D shotVector) {
        // This is a simplified simulation
        // In a real implementation, this would use physics calculations
        // For now, just return the ball position plus the shot vector
        return ballPosition.add(shotVector);
    }
}
