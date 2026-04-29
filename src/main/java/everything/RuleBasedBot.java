package everything;

public class RuleBasedBot implements GolfBot {
//manual gives maximum speed of 5 m/s for the ball, prevents the ball from suggesting unrealistic shots
    private static final double MAX_SPEED = 5.0;

    //simulation settings
    private final double dt;
    private final double maxTime;

    public RuleBasedBot(double dt, double maxTime) {
        this.dt = dt;
        this.maxTime = maxTime;
    }
//this method returns chosen shot as a 2d velocity vector [vx,vy]
    //basically it returns the shot the bot wants to play
    @Override
    public double[] computeShot(double[] currentPosition, CourseProfile course) {

        GolfSimulator simulator = new GolfSimulator(course, "rk4", dt, maxTime);

        double[] target = course.getTargetPosition();
        //computes direction from the ball to the target
        double dx = target[0] - currentPosition[0];//horizontal
        double dy = target[1] - currentPosition[1]; //vertical difference

        double baseAngle = Math.atan2(dy, dx);//gives the angle of two points from current position to the target


 //best score starts at infinity, best shot start at zero velocity
 //the lower the score the better
        double bestScore = Double.POSITIVE_INFINITY;
        double[] bestShot = new double[]{0, 0};

        // try different angles around the target direction
        int angleSteps = 25; //25 different directions
        int speedSteps = 15;// bot tries 15 different speeds


//it searches within a 90-degree range to the target
        double angleSpread = Math.PI / 2; // +- 90 degrees
//tries angles
        for (int i = 0; i < angleSteps; i++) {

            double angle = baseAngle - angleSpread / 2
                    + i * (angleSpread / (angleSteps - 1));
//tries speeds
            for (int j = 1; j <= speedSteps; j++) {

                double speed = j * (MAX_SPEED / speedSteps);
//convert angle and speed into velocity
                double vx = speed * Math.cos(angle);
                double vy = speed * Math.sin(angle);

//simulation - so bot hits the ball from current position with this velocity
//and checks where it ends up
                ShotResult result = simulator.simulate(currentPosition, new double[]{vx, vy});

                // if we hit the target → perfect shot
                if (result.getOutcome() == ShotResult.Outcome.IN_TARGET) {
                    return new double[]{vx, vy};
                }
//if the ball didn't get into the target, it gives a score
                double score = score(result, course);
//keeps the score
                if (score < bestScore) {
                    bestScore = score;
                    bestShot = new double[]{vx, vy};
                }
            }
        }

        return bestShot;
    }

    private double score(ShotResult result, CourseProfile course) {

        double[] target = course.getTargetPosition();

        double dx = result.getFinalX() - target[0];
        double dy = result.getFinalY() - target[1];
        double distance = Math.sqrt(dx * dx + dy * dy);

        // simple rules
        switch (result.getOutcome()) {
            case IN_WATER:
                return 1000 + distance; // big penalty
            case TIMEOUT:
                return 500 + distance;
            case STOPPED:
                return distance; // closer is better
            default:
                return distance;
        }
    }
}
