import javafx.scene.paint.Color;

public class SimulationController {

    private int shotCount = 0;
    private double[] currentPosition;
    private double[] positionBeforeShot;
    private CourseProfile course;
    private final CourseRenderer renderer;
    private final ControlPanel controls;
    private final double dt;
    private final double maxTime;
    private GolfBot bot = null;
    private boolean isDragging = false;
    private double dragStartPixelX, dragStartPixelY;

    public SimulationController(CourseProfile course, CourseRenderer renderer, ControlPanel controls, double dt, double maxTime) {
        this.course = course;
        this.renderer = renderer;
        this.controls = controls;
        this.dt = dt;
        this.maxTime = maxTime;

        // starts with the starting position from the course definition
        // updates the position when a shot is played (by overriding)
        currentPosition = course.getStartPosition().clone();
        positionBeforeShot = course.getStartPosition().clone();

        controls.setOnReset(this::handleReset);
        controls.setOnBotShoot(() -> {
            if (bot == null) {
                controls.setStatus("No bot loaded.", Color.RED);
                return;
            }
            // disables the button while thinking so the user doesnt keep clicking
            controls.setBotEnabled(false);
            controls.setStatus("Bot is thinking...", Color.RED);
            
            // run the bot on the background thread so gui doesnt freeze
            Thread botThread = new Thread(() -> {
                double[] velocity = bot.computeShot(currentPosition, course);

                // bring result back to javafx and update gui
                javafx.application.Platform.runLater(() -> {
                    controls.setBotEnabled(true);
                    controls.clearStatus();
                    handleShot(velocity);
                });
            });
            botThread.setDaemon(true); // thread stops when the app closes
            botThread.start();
        });

        renderer.getCanvas().setOnMousePressed(event -> {
            if (renderer.isMessageOnScreen()) {
                renderer.dismissMessage();
                isDragging = false;
                return;
            }
        // start dragging when the clicking near the ball
        double ballPixelX = renderer.toPixelXPublic(currentPosition[0]);
        double ballPixelY = renderer.toPixelYPublic(currentPosition[1]);
        double dx = event.getX() - ballPixelX;
        double dy = event.getY() - ballPixelY;
        if (Math.sqrt(dx*dx + dy*dy) < 20) { // can start dragging within 20px of ball
            isDragging = true;
            dragStartPixelX = ballPixelX;
            dragStartPixelY = ballPixelY;
        }
    });

    renderer.getCanvas().setOnMouseDragged(event -> {
        if (!isDragging) return;

        double dx = event.getX() - dragStartPixelX;
        double dy = event.getY() - dragStartPixelY;
        double dragLength = Math.sqrt(dx*dx + dy*dy);

        // set a max to the length of the arrow
        double maxArrowPixels = 50.0;
        double maxX;
        double maxY;
        if (dragLength > maxArrowPixels) {
            // keep direction but limit length
            double angle = Math.atan2(dy, dx);
            maxX = dragStartPixelX + maxArrowPixels * Math.cos(angle);
            maxY = dragStartPixelY + maxArrowPixels * Math.sin(angle);
        } else {
            maxX = event.getX();
            maxY = event.getY();
        }

        renderer.drawArrow(dragStartPixelX, dragStartPixelY, maxX, maxY);
    });

    renderer.getCanvas().setOnMouseReleased(event -> {
        if (!isDragging) return;
        isDragging = false;

        double dx = event.getX() - dragStartPixelX;
        double dy = event.getY() - dragStartPixelY;

        // direction determined by dragging, power determined from the input field
        // dy is negative because the on a computer screen y is at the top but on the course y is at the bottom
        double angle = Math.atan2(-dy, dx);
        
        double[] powerInput = controls.getPower();
        double power = (powerInput != null) ? powerInput[0] : 1.0;
        power = Math.max(0, Math.min(5.0, power)); // power is a value between 0 and 5

        double vx = power * Math.cos(angle);
        double vy = power * Math.sin(angle);

        handleShot(new double[]{vx, vy});
    });
    }

    private void handleShot(double[] velocity) {
        positionBeforeShot = currentPosition.clone();
        controls.clearStatus(); // clears both status and position label

        GolfSimulator sim = new GolfSimulator(course, controls.getSelectedSolver(), dt, maxTime);
        ShotResult result = sim.simulate(currentPosition, velocity);
        shotCount++;

        controls.updateShotCount(shotCount);
        controls.setPosition(result.getFinalX(), result.getFinalY());

        // the result depending on the outcome of the shot
        switch (result.getOutcome()) {
            case IN_TARGET -> {
                renderer.drawBallPath(result.getPath()); // draws a path to target
                String line1 = shotCount == 1 ? "HOLE IN ONE!" : "IN THE HOLE!";
                String line2 = shotCount == 1 ? "Amazing!" 
                             : "Completed in " + shotCount + " putts";
                renderer.drawInfoMessage(line1, line2, Color.WHITE, () -> {
                    controls.updateShotCount(0);
                    controls.clearStatus();
                    renderer.drawBall(
                        course.getStartPosition()[0],
                        course.getStartPosition()[1]
                    );
                });
                currentPosition = course.getStartPosition().clone();
                shotCount = 0;
            }
            case IN_WATER -> {
                renderer.drawBallPath(result.getPath()); // show path into water first
                controls.setStatus("Water!", Color.CORNFLOWERBLUE);
                    renderer.drawInfoMessage(
                        "Penalty",
                        "Ball went into the water :( \nReplaying from previous position.",
                        Color.CORNFLOWERBLUE,
                        () -> {
                            // after the message disappears,  the ball resets to the location before the shot
                            currentPosition = positionBeforeShot.clone();
                            renderer.clearPaths();
                            renderer.drawBall(currentPosition[0], currentPosition[1]);
                            controls.clearStatus();
                        }
                    );
            }
            case OUT_OF_BOUNDS -> {
                renderer.drawBallPath(result.getPath()); // show that the ball goes out of bounds
                controls.setStatus("Out of bounds!", Color.BLACK);
                    renderer.drawInfoMessage(
                    "Penalty",
                    "Ball left the course :( \nReplaying from previous position",
                    Color.WHITE,
                    () -> {
                            // after the message disappears,  the ball resets to the location before the shot
                            currentPosition = positionBeforeShot.clone();
                            renderer.clearPaths();
                            renderer.drawBall(currentPosition[0], currentPosition[1]);
                            controls.clearStatus();
                        }
                );
            }
            case STOPPED, TIMEOUT -> {
                // draws path, updates position, player shoots from here next
                // after most of the shots
                renderer.drawBallPath(result.getPath());
                currentPosition = new double[]{result.getFinalX(), result.getFinalY()};
                renderer.setCurrentBallPosition(currentPosition);
            }
        }
    }

    private void handleReset() {
        // this takes input from the gui (overrides the default values)
        double[] guiStart = controls.getStartPosition();
        double[] guiTarget = controls.getTargetPosition();
        double[] guiFriction = controls.getFriction();

        currentPosition = (guiStart != null) 
        ? guiStart 
        : course.getStartPosition().clone();
        renderer.setCurrentBallPosition(currentPosition);

        //rebuild the course with the new target position and friction if valid
        if (guiTarget != null && guiFriction != null) {
            try {
                CourseInputProcessing processor = new CourseInputProcessing();
                CourseConfiguration config = processor.buildConfig(
                    // keep existing height expression so the shape of terrains stays the same
                    ((CourseConfigurationProfile) course).getHeightExpression(),
                    String.valueOf(guiFriction[0]),
                    String.valueOf(guiFriction[1]),
                    String.valueOf(currentPosition[0]),
                    String.valueOf(currentPosition[1]),
                    String.valueOf(guiTarget[0]),
                    String.valueOf(guiTarget[1]),
                    String.valueOf(course.getTargetRadius()),
                    String.valueOf(((CourseConfigurationProfile) course).getStepSize())
                );
                
                //update the course reference
                course = new CourseConfigurationProfile(config);
                renderer.updateCourse(course);
            } catch (IllegalArgumentException e) {
                controls.setStatus(e.getMessage(), Color.RED);
                return;
            }
        }

        positionBeforeShot = currentPosition.clone();
        shotCount = 0;
        controls.updateShotCount(0);
        controls.clearStatus();
        renderer.clearPaths();
        renderer.drawBall(currentPosition[0], currentPosition[1]);
    }

    public void setBot(GolfBot bot) { this.bot = bot; }

    public interface GolfBot {
        double[] computeShot(double[] currentPosition, CourseProfile course);
    }
}