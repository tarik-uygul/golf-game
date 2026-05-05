package ui;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import java.util.List;

import io.CourseInputModuleStorage;

public class CourseRenderer {

    private final Canvas canvas;
    private final GraphicsContext gc; // for drawing stuff
    private CourseInputModuleStorage course;

    private final double scaleX;
    private final double scaleY;

    private javafx.animation.AnimationTimer ballAnimation;
    private int animationStep = 0;
    private List<double[]> animationPath;

    private boolean MessageOnScreen = false;
    private Runnable afterMessageDismissed = null;

    private double[] currentBallPosition = null;

    // resolution of the terrain (more is nicer but slower)
    private static final int gridResolution = 100;

    public CourseRenderer(CourseInputModuleStorage course, double canvasWidth, double canvasHeight) {
        this.course = course;
        this.canvas = new Canvas(canvasWidth, canvasHeight);
        this.gc = canvas.getGraphicsContext2D();
        this.scaleX = canvasWidth / course.getCourseWidth();
        this.scaleY = canvasHeight / course.getCourseHeight();
    }

    public Canvas getCanvas() {
        return canvas;
    }

    // converts meter to pixels (course uses meter, canvas uses pixels)
    private double toPixelX(double x) {
        return x * scaleX;
    }

    private double toPixelY(double y) {
        return canvas.getHeight() - y * scaleY;
    } // flip y axis

    public void drawCourse() {
        double w = course.getCourseWidth();
        double h = course.getCourseHeight();
        double cellW = canvas.getWidth() / gridResolution;
        double cellH = canvas.getHeight() / gridResolution;

        // look at the heights across the whole course first so we know the full range for color scaling
        double minH = Double.MAX_VALUE;
        double maxH = -Double.MAX_VALUE;
        double[][] heights = new double[gridResolution][gridResolution];
        for (int i = 0; i < gridResolution; i++) {
            for (int j = 0; j < gridResolution; j++) {
                double height = course.getHeight(
                        i * w / gridResolution,
                        j * h / gridResolution);
                heights[i][j] = height;
                if (height < minH)
                    minH = height;
                if (height > maxH)
                    maxH = height;
            }
        }

        // draw each cell with a color based on its height relative to the course's
        // min/max
        for (int i = 0; i < gridResolution; i++) {
            for (int j = 0; j < gridResolution; j++) {
                gc.setFill(heightToColor(heights[i][j], minH, maxH));
                gc.fillRect(i * cellW, canvas.getHeight() - (j + 1) * cellH, cellW, cellH);
            }
        }

        drawTarget();
        drawStartPosition();
    }

    // if the height is negative its water
    private Color heightToColor(double height, double minH, double maxH) {
        if (height < 0)
            return course.getWaterColor();

        double t = (maxH == minH) ? 0.5 : (height - minH) / (maxH - minH);

        return course.getHighColor().interpolate(course.getGrassColor(), t);
    }

    public void drawBall(double x, double y) {
        gc.setFill(Color.WHITE);
        gc.setStroke(Color.BLACK);
        gc.fillOval(toPixelX(x) - 6, toPixelY(y) - 6, 12, 12);
        // gc.strokeOval(toPixelX(x) - 6, toPixelY(y) - 6, 12, 12); //add this for border around ball
    }

        private void drawTarget() {
        double[] t  = course.getTargetPosition();
        double pixX = toPixelX(t[0]);
        double pixY = toPixelY(t[1]);

        // hole/target
        double holeRadius = Math.max(course.getTargetRadius() * scaleX, 6);
        gc.setFill(Color.BLACK);
        gc.fillOval(pixX - holeRadius, pixY - holeRadius, holeRadius * 2, holeRadius * 2);

        // pole
        double poleHeight = 30;  // amount of pixels tall
        double poleWidth  = 2;
        gc.setFill(Color.WHITE);
        gc.fillRect(pixX - poleWidth / 2, pixY - poleHeight, poleWidth, poleHeight);

        // flag
        double flagWidth  = 16;
        double flagHeight = 12;
        gc.setFill(Color.RED);
        gc.fillPolygon(
            new double[]{pixX, pixX + flagWidth, pixX},               // x points
            new double[]{pixY - poleHeight, pixY - poleHeight + flagHeight / 2, pixY - poleHeight + flagHeight}, // y points
            3
        );
    }

    public void setCurrentBallPosition(double[] position) {
        this.currentBallPosition = position;
    }

    private void drawStartPosition() {
        double[] s = (currentBallPosition != null)
        ? currentBallPosition
        : course.getStartPosition();
        gc.setFill(Color.WHITE);
        gc.fillOval(toPixelX(s[0]) - 5, toPixelY(s[1]) - 5, 10, 10);
    }

    // for some messages (so they can't be missed, like when the ball falls in the
    // water or when the ball reaches the target)
    // they go away after some amount of seconds (chosen in simulation controller)
    // automatically but also when you click somewhere on the screen
    public void drawInfoMessage(String line1, String line2, Color color, Runnable onDone) {
        MessageOnScreen = true;
        afterMessageDismissed = onDone;

        double w = canvas.getWidth();
        double h = canvas.getHeight();

        // draw the overlay
        gc.setFill(Color.rgb(0, 0, 0, 0.55));
        gc.fillRect(0, 0, w, h);

        // first message, bigger and bold font
        gc.setFill(color);
        gc.setFont(javafx.scene.text.Font.font("Arial", javafx.scene.text.FontWeight.BOLD, 52));
        gc.setTextAlign(javafx.scene.text.TextAlignment.CENTER);
        gc.fillText(line1, w / 2, h / 2 - 20);

        // second message smaller and normal font
        gc.setFill(Color.WHITE);
        gc.setFont(javafx.scene.text.Font.font("Arial", javafx.scene.text.FontWeight.NORMAL, 24));
        gc.fillText(line2, w / 2, h / 2 + 30);
        gc.fillText("\n(click to continue)", w / 2, h / 2 + 70);
        gc.setTextAlign(javafx.scene.text.TextAlignment.LEFT);
    }

    public void drawArrow(double fromX, double fromY, double toX, double toY, double dragLength, double maxDragPixels) {
        clearPaths();

            // t stores a value between 0 and 1 that resembles the power
            double t = Math.min(dragLength / maxDragPixels, 1.0);

            // changes the colour of the arrow to match the power
            // yellow (low power), orange (medium power), red (high power)
            Color arrowColor;
            if (t < 0.75) {
                // yellow to orange
                arrowColor = Color.YELLOW.interpolate(Color.ORANGE, t/0.75 );
            } else {
                // orange to red
                arrowColor = Color.ORANGE.interpolate(Color.RED, (t - 0.75) / 0.25);
            }

        gc.setStroke(arrowColor);
        gc.setFill(arrowColor);
        gc.setLineWidth(2.5);
        gc.strokeLine(fromX, fromY, toX, toY);

        // create the pointer of the arrow
        double angle = Math.atan2(toY - fromY, toX - fromX);
        double arrowSize = 12;
        gc.setFill(arrowColor);
        double x1 = toX - arrowSize * Math.cos(angle - Math.PI / 6);
        double y1 = toY - arrowSize * Math.sin(angle - Math.PI / 6);
        double x2 = toX - arrowSize * Math.cos(angle + Math.PI / 6);
        double y2 = toY - arrowSize * Math.sin(angle + Math.PI / 6);
        gc.fillPolygon(new double[]{toX, x1, x2}, new double[]{toY, y1, y2}, 3);
    }

    public void animateBall(List<double[]> path, Runnable onFinished) {
        if (ballAnimation != null) ballAnimation.stop();

        animationPath = path;
        animationStep = 0;

        int totalSteps = path.size();
        int stepsPerFrame = Math.max(1, totalSteps / 1000);

        ballAnimation = new javafx.animation.AnimationTimer() {
            @Override
            public void handle(long now) {
                if (animationStep >= animationPath.size()) {
                    stop();
                    if (onFinished != null) onFinished.run();
                    return;
                }

                double[] pos = animationPath.get(animationStep);

                // stop early if ball has essentially stopped moving
                // instead of waiting for all remaining steps to play out
                if (animationStep > 0) {
                    double[] prev = animationPath.get(animationStep - stepsPerFrame < 0
                        ? 0 : animationStep - stepsPerFrame);
                    double dx = pos[0] - prev[0];
                    double dy = pos[1] - prev[1];
                    double distanceMoved = Math.sqrt(dx*dx + dy*dy);
                    if (distanceMoved < 0.001) { // less than 1mm per frame = effectively stopped
                        stop();
                        drawCourse();
                        drawBall(pos[0], pos[1]);
                        if (onFinished != null) onFinished.run();
                        return;
                    }
                }

                drawCourse();
                drawBall(pos[0], pos[1]);
                animationStep += stepsPerFrame;
            }
        };
        ballAnimation.start();
    }

    public boolean isMessageOnScreen() { return MessageOnScreen; }

    public void dismissMessage() {
        MessageOnScreen = false;
        clearPaths();
        Runnable callback = afterMessageDismissed; // callback stores the code to run when the message is dismissed
        afterMessageDismissed = null;
        if (callback != null) callback.run();
    }

    // this converts pixel coordinates back to course coordinates
    public double toWorldX(double pixelX) { return pixelX / scaleX; }
    public double toWorldY(double pixelY) { return (canvas.getHeight() - pixelY) / scaleY; }

    // this converts course coordinates to pixel coordinates
    public double toPixelXPublic(double x) { return toPixelX(x); }
    public double toPixelYPublic(double y) { return toPixelY(y); }

    public void updateCourse(CourseInputModuleStorage course2) {
        this.course = course2;
    }

    public void clearPaths() {
        drawCourse(); // just redraw everything
    }
}
