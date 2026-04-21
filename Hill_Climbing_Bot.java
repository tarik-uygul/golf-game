public class Hill_Climbing_Bot extends Bot {

    private static final double MAX_POWER = 100.0;
    private static final double POWER_STEP = 1.0;
    private static final double ANGLE_STEP = Math.toRadians(1.0); // 1 degree in radians

    @Override
    public Vector2D getNextShot(CourseProfile course, Vector2D ballPosition, Vector2D holePosition) {
        double bestPower = 0;
        double bestAngle = 0;
        double bestDistance = Double.MAX_VALUE;

        for (double power = 0; power <= MAX_POWER; power += POWER_STEP) {
            for (double angle = 0; angle < 2 * Math.PI; angle += ANGLE_STEP) {
                Vector2D shotVector = new Vector2D(Math.cos(angle) * power, Math.sin(angle) * power);
                Vector2D landingPosition = simulateShot(course, ballPosition, shotVector);
                double distanceToHole = landingPosition.distanceTO(holePosition).magnitude();

                if (distanceToHole < bestDistance) {
                    bestDistance = distanceToHole;
                    bestPower = power;
                    bestAngle = angle;
                }
            }
        }

        return new Vector2D(Math.cos(bestAngle) * bestPower, Math.sin(bestAngle) * bestPower);
    }

    @Override
    protected Vector2D simulateShot(CourseProfile course, Vector2D startPosition, Vector2D shotVector) {
        // Implement a simple physics simulation to determine where the ball lands
        // This is a placeholder and should be replaced with actual physics calculations
        return startPosition.add(shotVector); // Simplified: assumes no friction or slope
    }

}
