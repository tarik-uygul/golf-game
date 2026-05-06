package ui;

import bots.GolfBot;
import bots.Hill_Climbing_Bot;
import bots.Newton_Raphson_Bot;
import bots.RuleBasedBot;
import io.CourseInputModuleStorage;
import javafx.scene.paint.Color;
import model.GolfSimulator;
import model.ShotResult;

public class SimulationController {

    private int shotCount = 0;
    private double[] currentPosition;
    private double[] positionBeforeShot;
    private final CourseRenderer renderer;
    private final ControlPanel controls;
    private final double dt;
    private final double maxTime;
    private CourseInputModuleStorage course;
    private GolfBot bot = null;
    private boolean isDragging = false;
    private double dragStartPixelX, dragStartPixelY;
    private static final double MAX_DRAG_PIXELS = 150.0;
    private static final double MAX_SPEED = 5.0;

    public SimulationController(CourseInputModuleStorage course, CourseRenderer renderer, ControlPanel controls, double dt,
            double maxTime) {
        this.course = course;
        this.renderer = renderer;
        this.controls = controls;
        this.dt = dt;
        this.maxTime = maxTime;

        // starts with the starting position from the course definition
        // updates the position when a shot is played (by overriding)
        currentPosition = new double[]{course.startX, course.startY};
        positionBeforeShot = new double[]{course.startX, course.startY};

        controls.setOnReset(this::handleReset);
        controls.setOnBotShoot(() -> {
            String selectedBot = controls.getSelectedBot();

            controls.setStatus("Bot is calculating shot...", Color.BLUE);

            switch (selectedBot) {
                case "Hill Climbing":
                    bot = new Hill_Climbing_Bot(dt, maxTime, controls.getSelectedSolver());
                    break;

                case "Newton Raphson":
                    bot = new Newton_Raphson_Bot(dt, maxTime, controls.getSelectedSolver());
                    break;

                case "Rule Based":
                    bot = new RuleBasedBot(dt, maxTime);
                    break;

                default:
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
            double clampedX;
            double clampedY;
            if (dragLength > maxArrowPixels) {
                // keep direction but limit length
                double angle = Math.atan2(dy, dx);
                clampedX = dragStartPixelX + maxArrowPixels * Math.cos(angle);
                clampedY = dragStartPixelY + maxArrowPixels * Math.sin(angle);
            } else {
                clampedX = event.getX();
                clampedY = event.getY();
            }

            renderer.drawArrow(dragStartPixelX, dragStartPixelY, clampedX, clampedY, dragLength, maxArrowPixels);
        });

        renderer.getCanvas().setOnMouseReleased(event -> {
            if (!isDragging) return;
            isDragging = false;

            double dx = event.getX() - dragStartPixelX;
            double dy = event.getY() - dragStartPixelY;
            double dragLength = Math.sqrt(dx*dx + dy*dy);

            // power based on drag length
            double power = Math.min(dragLength, MAX_DRAG_PIXELS) / MAX_DRAG_PIXELS * MAX_SPEED;

            // direction determined by dragging, power determined from the input field
            // dy is negative because the on a computer screen y is at the top but on the course y is at the bottom
            double angle = Math.atan2(-dy, dx);

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

        // animate first, handle outcome after
        renderer.animateBall(result.getPath(), () -> {
            handleOutcome(result);
        });
    }

    private void handleOutcome(ShotResult result) {
        // the result depending on the outcome of the shot
        switch (result.getOutcome()) {
            case IN_TARGET -> {
                String line1 = shotCount == 1 ? "HOLE IN ONE!" : "IN THE HOLE!";
                String line2 = shotCount == 1 ? "Amazing!"
                             : "Completed in " + shotCount + " putts";
                renderer.drawInfoMessage(line1, line2, Color.WHITE, () -> {
                    controls.updateShotCount(0);
                    controls.clearStatus();
                    renderer.drawBall(course.startX, course.startY);
                    handleReset(); // resets everything after game is over
                });
                currentPosition = new double[]{course.startX, course.startY};
                shotCount = 0;
            }
            case IN_WATER -> {
                controls.setStatus("Water!", Color.CORNFLOWERBLUE);
                renderer.drawInfoMessage(
                    "Penalty",
                    "Ball went into the water :( \nReplaying from previous position.",
                    Color.CORNFLOWERBLUE,
                    () -> {
                        // after the message disappears, the ball resets to the location before the shot
                        currentPosition = positionBeforeShot.clone();
                        renderer.clearPaths();
                        renderer.drawBall(currentPosition[0], currentPosition[1]);
                        controls.clearStatus();
                    }
                );
            }
            case OUT_OF_BOUNDS -> {
                controls.setStatus("Out of bounds!", Color.BLACK);
                renderer.drawInfoMessage(
                    "Penalty",
                    "Ball left the course :( \nReplaying from previous position",
                    Color.WHITE,
                    () -> {
                        // after the message disappears, the ball resets to the location before the shot
                        currentPosition = positionBeforeShot.clone();
                        renderer.clearPaths();
                        renderer.drawBall(currentPosition[0], currentPosition[1]);
                        controls.clearStatus();
                    }
                );
            }
            case STOPPED, TIMEOUT -> {
                // draws path, updates position, player shoots from here next
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
            : new double[]{course.startX, course.startY};
        renderer.setCurrentBallPosition(currentPosition);

        // update the course fields directly with the new values from the gui if valid
        if (guiTarget != null && guiFriction != null) {
            course.startX  = currentPosition[0];
            course.startY  = currentPosition[1];
            course.targetX = guiTarget[0];
            course.targetY = guiTarget[1];
            course.muK     = guiFriction[0];
            course.muS     = guiFriction[1];
            renderer.updateCourse(course);
        }

        positionBeforeShot = currentPosition.clone();
        shotCount = 0;
        controls.updateShotCount(0);
        controls.clearStatus();
        renderer.clearPaths();
        renderer.drawBall(currentPosition[0], currentPosition[1]);
    }

    public void setBot(GolfBot bot) {
        this.bot = bot;
    }
}