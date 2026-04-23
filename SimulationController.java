import javafx.scene.paint.Color;

public class SimulationController {

    private int shotCount = 0;
    private double[] currentPosition;
    private double[] positionBeforeShot;
    private final CourseInputModuleStorage course;
    private final CourseRenderer renderer;
    private final ControlPanel controls;
    private final double dt;
    private final double maxTime;
    private GolfBot bot = null;

    public SimulationController(CourseInputModuleStorage course, CourseRenderer renderer, ControlPanel controls, double dt, double maxTime) {
        this.course = course;
        this.renderer = renderer;
        this.controls = controls;
        this.dt = dt;
        this.maxTime = maxTime;

        currentPosition = new double[]{course.startX, course.startY};
        positionBeforeShot = new double[]{course.startX, course.startY};

        controls.setOnShoot(this::handleShot);
        controls.setOnReset(this::handleReset);
        controls.setOnBotShoot(() -> {
            if (bot == null) {
                controls.setStatus("No bot loaded.", Color.RED);
                return;
            }
            double[] velocity = bot.computeShot(currentPosition, course);
            handleShot(velocity);
        });
    }

    private void handleShot(double[] velocity) {
        positionBeforeShot = currentPosition.clone();
        controls.clearStatus();

        GolfSimulator sim = new GolfSimulator(course, controls.getSelectedSolver(), dt, maxTime);
        ShotResult result = sim.simulate(currentPosition, velocity);
        shotCount++;

        controls.updateShotCount(shotCount);
        controls.setPosition(result.getFinalX(), result.getFinalY());

        switch (result.getOutcome()) {
            case IN_TARGET -> {
                renderer.drawBallPath(result.getPath());
                String line1 = shotCount == 1 ? "HOLE IN ONE!" : "IN THE HOLE!";
                String line2 = shotCount == 1 ? "Amazing!"
                             : "Completed in " + shotCount + " putts";
                renderer.drawInfoMessage(line1, line2, Color.GREEN, () -> {
                    controls.updateShotCount(0);
                    controls.clearStatus();
                    renderer.drawBall(course.startX, course.startY);
                });
                currentPosition = new double[]{course.startX, course.startY};
                shotCount = 0;
            }
            case IN_WATER -> {
                renderer.drawBallPath(result.getPath());
                controls.setStatus("Water! Penalty.", Color.CORNFLOWERBLUE);
                renderer.drawInfoMessage(
                    "Penalty",
                    "Ball went into the water :( \nReplaying from previous position.",
                    Color.CORNFLOWERBLUE,
                    () -> {
                        currentPosition = positionBeforeShot.clone();
                        renderer.clearPaths();
                        renderer.drawBall(currentPosition[0], currentPosition[1]);
                        controls.clearStatus();
                    }
                );
            }
            case STOPPED, TIMEOUT -> {
                renderer.drawBallPath(result.getPath());
                currentPosition = new double[]{result.getFinalX(), result.getFinalY()};
            }
        }
    }

    private void handleReset() {
        double[] guiStart = controls.getStartPosition();
        currentPosition = (guiStart != null) ? guiStart : new double[]{course.startX, course.startY};
        positionBeforeShot = currentPosition.clone();
        shotCount = 0;
        controls.updateShotCount(0);
        controls.clearStatus();
        renderer.clearPaths();
        renderer.drawBall(currentPosition[0], currentPosition[1]);
    }

    public void setBot(GolfBot bot) { this.bot = bot; }

    public interface GolfBot {
        double[] computeShot(double[] currentPosition, CourseInputModuleStorage course);
    }
}