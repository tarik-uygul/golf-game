import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import java.util.List;

public class CourseRenderer {

    private final Canvas canvas;
    private final GraphicsContext gc;
    private final CourseInputModuleStorage course;

    private final double scaleX;
    private final double scaleY;

    private static final int gridResolution = 100;
    private static final double COURSE_WIDTH = 20.0;
    private static final double COURSE_HEIGHT = 20.0;

    public CourseRenderer(CourseInputModuleStorage course, double canvasWidth, double canvasHeight) {
        this.course = course;
        this.canvas = new Canvas(canvasWidth, canvasHeight);
        this.gc = canvas.getGraphicsContext2D();
        this.scaleX = canvasWidth  / COURSE_WIDTH;
        this.scaleY = canvasHeight / COURSE_HEIGHT;
    }

    public Canvas getCanvas() { return canvas; }

    private double toPixelX(double x) { return x * scaleX; }
    private double toPixelY(double y) { return canvas.getHeight() - y * scaleY; }

    public void drawCourse() {
        double cellW = canvas.getWidth()  / gridResolution;
        double cellH = canvas.getHeight() / gridResolution;

        double minH = Double.MAX_VALUE, maxH = -Double.MAX_VALUE;
        double[][] heights = new double[gridResolution][gridResolution];
        for (int i = 0; i < gridResolution; i++) {
            for (int j = 0; j < gridResolution; j++) {
                double height = course.heightFunction.evaluate(
                    i * COURSE_WIDTH  / gridResolution,
                    j * COURSE_HEIGHT / gridResolution
                );
                heights[i][j] = height;
                if (height < minH) minH = height;
                if (height > maxH) maxH = height;
            }
        }

        for (int i = 0; i < gridResolution; i++) {
            for (int j = 0; j < gridResolution; j++) {
                gc.setFill(heightToColor(heights[i][j], minH, maxH));
                gc.fillRect(i * cellW, canvas.getHeight() - (j+1) * cellH, cellW, cellH);
            }
        }

        drawTarget();
        drawStartPosition();
    }

    private Color heightToColor(double height, double minH, double maxH) {
        if (height < 0) return Color.CORNFLOWERBLUE;
        double t = (maxH == minH) ? 0.5 : (height - minH) / (maxH - minH);
        return Color.DARKGREEN.interpolate(Color.FORESTGREEN, t);
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
        double[] last = path.get(path.size() - 1);
        drawBall(last[0], last[1]);
    }

    private void drawTarget() {
        double pixR = Math.max(course.targetRadius * scaleX, 6);
        gc.setFill(Color.RED);
        gc.fillOval(
            toPixelX(course.targetX) - pixR,
            toPixelY(course.targetY) - pixR,
            pixR * 2,
            pixR * 2
        );
    }

    private void drawStartPosition() {
        gc.setFill(Color.YELLOW);
        gc.fillOval(toPixelX(course.startX) - 5, toPixelY(course.startY) - 5, 10, 10);
    }

    public void drawInfoMessage(String line1, String line2, Color color, Runnable onDone) {
        double w = canvas.getWidth();
        double h = canvas.getHeight();

        gc.setFill(Color.rgb(0, 0, 0, 0.55));
        gc.fillRect(0, 0, w, h);

        gc.setFill(color);
        gc.setFont(javafx.scene.text.Font.font("Arial", javafx.scene.text.FontWeight.BOLD, 52));
        gc.setTextAlign(javafx.scene.text.TextAlignment.CENTER);
        gc.fillText(line1, w / 2, h / 2 - 20);

        gc.setFill(Color.WHITE);
        gc.setFont(javafx.scene.text.Font.font("Arial", javafx.scene.text.FontWeight.NORMAL, 24));
        gc.fillText(line2, w / 2, h / 2 + 30);
        gc.fillText("\n(click to continue)", w / 2, h / 2 + 70);
        gc.setTextAlign(javafx.scene.text.TextAlignment.LEFT);

        canvas.setOnMouseClicked(event -> {
            clearPaths();
            canvas.setOnMouseClicked(null);
            if (onDone != null) onDone.run();
        });
    }

    public void clearPaths() {
        drawCourse();
    }
}