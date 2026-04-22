import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import java.util.List;

public class CourseRenderer {

    private final Canvas canvas;
    private final GraphicsContext gc; // for drawing stuff
    private CourseProfile course;

    private final double scaleX;
    private final double scaleY;

    private boolean MessageOnScreen = false;
    private Runnable afterMessageDismissed = null;

    private double[] currentBallPosition = null;

    // resolution of the terrain (more is nicer but slower)
    private static final int gridResolution = 100;

    public CourseRenderer(CourseProfile course, double canvasWidth, double canvasHeight) {
        this.course = course;
        this.canvas = new Canvas(canvasWidth, canvasHeight);
        this.gc = canvas.getGraphicsContext2D();
        this.scaleX = canvasWidth  / course.getCourseWidth();
        this.scaleY = canvasHeight / course.getCourseHeight();
    }

    public Canvas getCanvas() { return canvas; }

    // converts meter to pixels (course uses meter, canvas uses pixels)
    private double toPixelX(double x) { return x * scaleX; }
    private double toPixelY(double y) { return canvas.getHeight() - y * scaleY; } // flip y axis

    public void drawCourse() {
        double w = course.getCourseWidth();
        double h = course.getCourseHeight();
        double cellW = canvas.getWidth()  / gridResolution;
        double cellH = canvas.getHeight() / gridResolution;

        // look at the heights across the whole course first so we know the full range for color scaling
        double minH = Double.MAX_VALUE, maxH = -Double.MAX_VALUE;
        double[][] heights = new double[gridResolution][gridResolution];
        for (int i = 0; i < gridResolution; i++) {
            for (int j = 0; j < gridResolution; j++) {
                double height = course.getHeight(
                    i * w / gridResolution,
                    j * h / gridResolution
                );
                heights[i][j] = height;
                if (height < minH) minH = height;
                if (height > maxH) maxH = height;
            }
        }

        // draw each cell with a color based on its height relative to the course's min/max
        for (int i = 0; i < gridResolution; i++) {
            for (int j = 0; j < gridResolution; j++) {
                gc.setFill(heightToColor(heights[i][j], minH, maxH));
                gc.fillRect(i * cellW, canvas.getHeight() - (j+1) * cellH, cellW, cellH);
            }
        }

    drawTarget();
    drawStartPosition();
    }

    // if the height is negative its water
    private Color heightToColor(double height, double minH, double maxH) {
        if (height < 0) return course.getWaterColor();

        double t = (maxH == minH) ? 0.5 : (height - minH) / (maxH - minH);

        return course.getHighColor().interpolate(course.getGrassColor(), t);
    }

    public void drawBall(double x, double y) {
        gc.setFill(Color.WHITE);
        gc.setStroke(Color.BLACK);
        gc.fillOval(toPixelX(x) - 6, toPixelY(y) - 6, 12, 12);
        gc.strokeOval(toPixelX(x) - 6, toPixelY(y) - 6, 12, 12);
    }

    public void drawBallPath(List<double[]> path) {
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(1.5);
        for (int i = 1; i < path.size(); i++) {
            double[] prev = path.get(i - 1);
            double[] curr = path.get(i);
            gc.strokeLine(toPixelX(prev[0]), toPixelY(prev[1]),
                          toPixelX(curr[0]), toPixelY(curr[1]));
        }
        // draw final ball position
        double[] last = path.get(path.size() - 1);
        drawBall(last[0], last[1]);
    }



    private void drawTarget() {
        double[] t  = course.getTargetPosition();
        double r    = course.getTargetRadius();
        double pixR = Math.max(r * scaleX, 6); // at least 6px so it's always visible

        gc.setFill(Color.RED);
        gc.fillOval(
            toPixelX(t[0]) - pixR,
            toPixelY(t[1]) - pixR,
            pixR * 2,
            pixR * 2
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

    // for some messages (so they can't be missed, like when the ball falls in the water or when the ball reaches the target)
    // they go away after some amount of seconds (chosen in simulation controller) automatically but also when you click somewhere on the screen
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

    public void drawArrow(double fromX, double fromY, double toX, double toY) {
        clearPaths();
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(2.5);
        gc.strokeLine(fromX, fromY, toX, toY);

        // create the pointer of the arrow
        double angle = Math.atan2(toY - fromY, toX - fromX);
        double arrowSize = 12;
        gc.setFill(Color.WHITE);
        double x1 = toX - arrowSize * Math.cos(angle - Math.PI / 6);
        double y1 = toY - arrowSize * Math.sin(angle - Math.PI / 6);
        double x2 = toX - arrowSize * Math.cos(angle + Math.PI / 6);
        double y2 = toY - arrowSize * Math.sin(angle + Math.PI / 6);
        gc.fillPolygon(new double[]{toX, x1, x2}, new double[]{toY, y1, y2}, 3);
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

    public void updateCourse(CourseProfile course) {
        this.course = course;
    }

    public void clearPaths() {
        drawCourse(); // just redraw everything
    }
}