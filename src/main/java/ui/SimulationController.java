package ui;

import bots.GolfBot;
import bots.Hill_Climbing_Bot;
import bots.Newton_Raphson_Bot;
import bots.RuleBasedBot;
import bots.MazeBot;
import io.CourseInputModuleStorage;
import javafx.scene.input.MouseButton;
import javafx.scene.paint.Color;
import model.BasinCenterer;
import model.SensitivityRanker;
import model.GolfSimulator;
import model.NoiseMode;
import model.ShotNoise;
import model.ShotResult;
import model.obstacles.Obstacle;
import model.obstacles.Sand;
import model.obstacles.Tree;
import model.obstacles.Water;
import ui.ControlPanel.PlacementMode;
import java.util.List;
import java.util.Random;

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
    private final Random rng = new Random();
    private String pendingBotDiag = "";

    // per-type default radii in world units (meters)
    private static final double TREE_RADIUS = 0.5;
    private static final double SAND_RADIUS = 1.5;
    private static final double WATER_RADIUS = 2.0;

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
        controls.setOnClearObstacles(() -> {
            course.clearObstacles();
            refreshObstacleCounts();
            renderer.clearPaths();
            renderer.drawBall(currentPosition[0], currentPosition[1]);
        });
        controls.setOnBotShoot(() -> {
            String selectedBot = controls.getSelectedBot();

            controls.setStatus("Bot is calculating shot...", Color.BLUE);

            boolean robust = controls.isRobustShotEnabled();
            String solver = controls.getSelectedSolver();
            NoiseMode robustNoiseMode = controls.getNoiseMode();

            switch (selectedBot) {
                case "Hill Climbing":
                    bot = new Hill_Climbing_Bot(dt, maxTime, solver);
                    break;

                case "Newton Raphson":
                    bot = new Newton_Raphson_Bot(dt, maxTime, solver);
                    break;

                case "Rule Based":
                    bot = new RuleBasedBot(dt, maxTime);
                    break;
                case "MazeBot":
                    bot = new MazeBot(solver, dt, maxTime);
                    break;

                default:
                    controls.setStatus("No bot loaded.", Color.RED);
                    return;
            }
            // disables the button while thinking so the user doesnt keep clicking
            controls.setBotEnabled(false);
            controls.setStatus("Bot is thinking...", Color.RED);
            controls.setDiagnostics("");

            // run the bot on the background thread so gui doesnt freeze
            Thread botThread = new Thread(() -> {
                double[] velocity = bot.computeShot(currentPosition, course);
                int iterations = bot.getLastIterationCount();
                String botName = bot.getClass().getSimpleName();
                double[] rawVelocity = velocity.clone();

                double vxLower = Double.NaN, vxUpper = Double.NaN;
                double vyLower = Double.NaN, vyUpper = Double.NaN;
                int basinIters = 0;

                if (robust) {
                    double pipelineDt = Math.max(dt, 0.05); // coarser dt only for the fast grid scan
                    GolfSimulator scanSim = new GolfSimulator(course, solver, pipelineDt, maxTime);
                    GolfSimulator realSim = new GolfSimulator(course, solver, dt, maxTime);
                    try {
                        // Step 1 & 2: fast grid scan with coarse dt to find candidates
                        List<SensitivityRanker.Candidate> candidates =
                                SensitivityRanker.rankCandidates(scanSim, currentPosition, velocity[0], velocity[1], course.getTargetPosition());
                        if (!candidates.isEmpty()) {
                            SensitivityRanker.Candidate best = candidates.get(0);
                            velocity = new double[]{best.vx, best.vy};
                        }
                        // Step 3: verify candidate and find basin using real dt so obstacles are detected properly
                        ShotResult test = realSim.simulate(currentPosition, velocity);
                        if (test.getOutcome() == ShotResult.Outcome.IN_TARGET) {
                            BasinCenterer.Result basin = BasinCenterer.center(realSim, currentPosition, velocity[0], velocity[1], robustNoiseMode);
                            ShotResult verify = realSim.simulate(currentPosition, basin.velocity);
                            if (verify.getOutcome() == ShotResult.Outcome.IN_TARGET) {
                                velocity = basin.velocity;
                            }
                            vxLower = basin.vxLower; vxUpper = basin.vxUpper;
                            vyLower = basin.vyLower; vyUpper = basin.vyUpper;
                            basinIters = basin.simulationCount;
                        }
                    } catch (Exception ignored) {}
                }

                // columns: bot, bot_iters, basin_iters, raw_vx, raw_vy, center_vx, center_vy, vx_lower, vx_upper, vy_lower, vy_upper
                System.out.printf("BOT|%s|%d|%d|%.4f|%.4f|%.4f|%.4f|%.4f|%.4f|%.4f|%.4f%n",
                        botName, iterations, basinIters,
                        rawVelocity[0], rawVelocity[1],
                        velocity[0], velocity[1],
                        vxLower, vxUpper, vyLower, vyUpper);

                // build sidebar diagnostics string
                StringBuilder diag = new StringBuilder();
                diag.append(String.format("Iters: %d", iterations));
                diag.append(String.format("%nRaw:   vx=%.3f vy=%.3f", rawVelocity[0], rawVelocity[1]));
                diag.append(String.format("%nFired: vx=%.3f vy=%.3f", velocity[0], velocity[1]));
                if (!Double.isNaN(vxLower)) {
                    diag.append(String.format("%nBasin(%d sims):", basinIters));
                    diag.append(String.format("%n  vx [%.3f, %.3f]", vxLower, vxUpper));
                    diag.append(String.format("%n  vy [%.3f, %.3f]", vyLower, vyUpper));
                }
                final String diagText = diag.toString();
                final double[] finalVelocity = velocity;
                // bring result back to javafx and update gui
                javafx.application.Platform.runLater(() -> {
                    controls.setBotEnabled(true);
                    controls.clearStatus();
                    pendingBotDiag = diagText;
                    handleShot(finalVelocity);
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
            // intercept right-click → remove an obstacle under the cursor
            if (event.getButton() == MouseButton.SECONDARY) {
                double wx = renderer.toWorldX(event.getX());
                double wy = renderer.toWorldY(event.getY());
                Obstacle hit = renderer.obstacleAtWorld(wx, wy);
                if (hit != null) {
                    course.removeObstacle(hit);
                    refreshObstacleCounts();
                    renderer.clearPaths();
                    renderer.drawBall(currentPosition[0], currentPosition[1]);
                }
                return;
            }
            // intercept left-click while a placement toggle is active → drop a new obstacle
            PlacementMode mode = controls.getPlacementMode();
            if (mode != PlacementMode.OFF) {
                double wx = renderer.toWorldX(event.getX());
                double wy = renderer.toWorldY(event.getY());
                Obstacle created = switch (mode) {
                    case TREE -> new Tree(wx, wy, TREE_RADIUS);
                    case SAND -> new Sand(wx, wy, SAND_RADIUS);
                    case WATER -> new Water(wx, wy, WATER_RADIUS);
                    case OFF -> null;
                };
                if (created != null) {
                    course.addObstacle(created);
                    refreshObstacleCounts();
                    renderer.clearPaths();
                    renderer.drawBall(currentPosition[0], currentPosition[1]);
                }
                return; // don't start shot drag in placement mode
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
        controls.clearStatus();
        controls.setDiagnostics(pendingBotDiag);
        pendingBotDiag = "";

        NoiseMode noiseMode = controls.getNoiseMode();
        double[] firedFrom     = ShotNoise.applyToPosition(currentPosition, noiseMode, rng);
        double[] firedVelocity = ShotNoise.applyToVelocity(velocity, noiseMode, rng);

        double dvx = firedVelocity[0] - velocity[0];
        double dvy = firedVelocity[1] - velocity[1];

        System.out.printf("SHOT|%.4f|%.4f|%.4f|%.4f%n",
                firedFrom[0] - currentPosition[0], firedFrom[1] - currentPosition[1],
                dvx, dvy);

        // append noise to whatever the bot diagnostics already set
        if (noiseMode != NoiseMode.NONE) {
            String base = controls.getDiagnostics();
            String noiseStr = String.format("Noise dvx=%.3f dvy=%.3f", dvx, dvy);
            controls.setDiagnostics(base.isEmpty() ? noiseStr : base + "\n" + noiseStr);
        }

        GolfSimulator sim = new GolfSimulator(course, controls.getSelectedSolver(), dt, maxTime);
        ShotResult result = sim.simulate(firedFrom, firedVelocity);
        shotCount++;

        controls.updateShotCount(shotCount);
        controls.setPosition(result.getFinalX(), result.getFinalY());

        // animate first, handle outcome after
        renderer.animateBall(result.getPath(), dt, () -> {
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
            case HIT_TREE -> {
                controls.setStatus("Hit a tree!", Color.DARKGREEN);
                renderer.drawInfoMessage(
                    "Penalty",
                    "Ball hit a tree :(\nReplaying from previous position.",
                    Color.WHITE,
                    () -> {
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

    // recount obstacles by type and push to the sidebar labels
    private void refreshObstacleCounts() {
        int trees = 0, sand = 0, water = 0;
        for (Obstacle o : course.getObstacles()) {
            if (o instanceof Tree) trees++;
            else if (o instanceof Sand) sand++;
            else if (o instanceof Water) water++;
        }
        controls.updateObstacleCounts(trees, sand, water);
    }
}