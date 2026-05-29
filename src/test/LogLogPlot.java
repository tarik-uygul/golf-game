package src.test;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import physics.EulerSolver;
import physics.ODEFunction;
import physics.ODESystemBuilder;
import physics.RungeKutta4;

import java.util.HashMap;

// LogLogPlot.java
//
// Runs the accuracy + timing experiment for EulerSolver and RungeKutta4,
// then displays two side-by-side log-log plots in a JavaFX window:
//
//   Left plot:  Error vs Step Size
//   Right plot: Computation Time vs Step Size
//
// The test ODE is the Simple Harmonic Oscillator: x' = v, v' = -x
// with exact solution x(t) = cos(t), v(t) = -sin(t), starting from x=1, v=0.
//
// Uses ODESystemBuilder + ExpressionParser so the physics is defined as a string
// exactly the same way you would type it into the Visualizer GUI.

public class LogLogPlot extends Application {

    // -----------------------------------------------------------------------
    // Experiment settings
    // -----------------------------------------------------------------------
    private static final double END_TIME = 20.0;
    private static final int TIME_REPS = 5;

    private static final double[] STEP_SIZES = {
            1.0, 0.5, 0.2, 0.1, 0.05, 0.02, 0.01, 0.005, 0.002, 0.001
    };

    // -----------------------------------------------------------------------
    // Colour scheme
    // -----------------------------------------------------------------------
    private static final Color BG_COLOR = Color.web("#1e1e2e");
    private static final Color PANEL_COLOR = Color.web("#2a2a3e");
    private static final Color GRID_COLOR = Color.web("#3a3a5a");
    private static final Color AXIS_COLOR = Color.web("#888888");
    private static final Color EULER_COLOR = Color.web("#ff6b6b");
    private static final Color RK4_COLOR = Color.web("#4ecdc4");
    private static final Color REFERENCE_COLOR = Color.web("#ffd93d");
    private static final Color TEXT_COLOR = Color.web("#e0e0f0");
    private static final Color TITLE_COLOR = Color.web("#ffffff");

    // -----------------------------------------------------------------------
    // JavaFX entry point
    // -----------------------------------------------------------------------
    @Override
    public void start(Stage stage) {

        ExperimentData data = runExperiment();

        VBox root = new VBox(20);
        root.setBackground(new Background(new BackgroundFill(
                BG_COLOR, CornerRadii.EMPTY, Insets.EMPTY)));
        root.setPadding(new Insets(24));
        root.setAlignment(Pos.CENTER);

        Label title = new Label("ODE Solver Comparison — Log-Log Analysis");
        title.setFont(Font.font("Monospace", FontWeight.BOLD, 18));
        title.setTextFill(TITLE_COLOR);

        HBox plots = new HBox(24);
        plots.setAlignment(Pos.CENTER);

        Canvas errorCanvas = new Canvas(560, 430);
        Canvas timeCanvas = new Canvas(560, 430);

        drawLogLogPlot(errorCanvas,
                "Accuracy vs Step Size",
                "Step Size  h",
                "Error  ||numerical - exact||",
                data.stepSizes,
                data.eulerErrors,
                data.rk4Errors,
                true);

        drawLogLogPlot(timeCanvas,
                "Computation Time vs Step Size",
                "Step Size  h",
                "Time (nanoseconds)",
                data.stepSizes,
                data.eulerTimes,
                data.rk4Times,
                false);

        plots.getChildren().addAll(errorCanvas, timeCanvas);

        HBox legend = buildLegend();

        Label subtitle = new Label(
                "Harmonic oscillator  x''= -x  |  integrated to t = " + (int) END_TIME +
                        "  |  exact: x(t)=cos(t)");
        subtitle.setFont(Font.font("Monospace", 12));
        subtitle.setTextFill(AXIS_COLOR);

        root.getChildren().addAll(title, plots, legend, subtitle);

        Scene scene = new Scene(root, 1200, 560);
        stage.setTitle("Solver Log-Log Analysis");
        stage.setScene(scene);
        stage.show();
    }

    // -----------------------------------------------------------------------
    // Run the experiment using YOUR ODESystemBuilder + ExpressionParser
    // The harmonic oscillator equations are written as strings, exactly like
    // you would type them into the Visualizer GUI
    // -----------------------------------------------------------------------
    private ExperimentData runExperiment() {

        // harmonic oscillator: x' = v, v' = -1*x
        // written as strings so they go through ExpressionParser just like in the
        // Visualizer
        String[] names = { "x", "v" };
        String[] eqs = { "v", "-1*x" };
        ODESystemBuilder ode = new ODESystemBuilder(eqs, names, new HashMap<>());

        double[] initialState = { 1.0, 0.0 }; // x(0)=1, v(0)=0

        int n = STEP_SIZES.length;
        double[] eulerErrors = new double[n];
        double[] rk4Errors = new double[n];
        double[] eulerTimes = new double[n];
        double[] rk4Times = new double[n];

        for (int i = 0; i < n; i++) {
            double h = STEP_SIZES[i];

            // Euler - run TIME_REPS times and average the timing for a stable measurement
            long totalTime = 0;
            double[] eulerResult = null;
            for (int rep = 0; rep < TIME_REPS; rep++) {
                long t0 = System.nanoTime();
                eulerResult = integrate(initialState, h, END_TIME, ode, "euler");
                totalTime += System.nanoTime() - t0;
            }
            eulerTimes[i] = totalTime / (double) TIME_REPS;
            eulerErrors[i] = euclideanError(eulerResult, exactSolution(END_TIME));

            // RK4 - same timing approach
            totalTime = 0;
            double[] rk4Result = null;
            for (int rep = 0; rep < TIME_REPS; rep++) {
                long t0 = System.nanoTime();
                rk4Result = integrate(initialState, h, END_TIME, ode, "rk4");
                totalTime += System.nanoTime() - t0;
            }
            rk4Times[i] = totalTime / (double) TIME_REPS;
            rk4Errors[i] = euclideanError(rk4Result, exactSolution(END_TIME));

            System.out.printf("h=%.4f  eulerErr=%.3e  rk4Err=%.3e  eulerTime=%dns  rk4Time=%dns%n",
                    h, eulerErrors[i], rk4Errors[i],
                    (long) eulerTimes[i], (long) rk4Times[i]);
        }

        return new ExperimentData(STEP_SIZES, eulerErrors, rk4Errors, eulerTimes, rk4Times);
    }

    // -----------------------------------------------------------------------
    // Integrate from t=0 to t=endTime using the chosen solver
    // calls EulerSolver.step or RungeKutta4.step - both take the same arguments
    // -----------------------------------------------------------------------
    private double[] integrate(double[] initialState, double h,
            double endTime, ODEFunction ode, String solver) {
        double[] state = initialState.clone();
        double t = 0.0;
        while (t + h <= endTime + 1e-12) {
            if (solver.equals("euler")) {
                state = EulerSolver.step(state, h, ode);
            } else {
                state = RungeKutta4.step(state, h, ode);
            }
            t += h;
        }
        return state;
    }

    // exact solution of the harmonic oscillator x''=-x at time t
    private double[] exactSolution(double t) {
        return new double[] { Math.cos(t), -Math.sin(t) };
    }

    private double euclideanError(double[] numerical, double[] exact) {
        double sum = 0;
        for (int i = 0; i < numerical.length; i++) {
            double d = numerical[i] - exact[i];
            sum += d * d;
        }
        return Math.sqrt(sum);
    }

    // -----------------------------------------------------------------------
    // Drawing - unchanged from original, just draws the plots onto canvases
    // -----------------------------------------------------------------------
    private void drawLogLogPlot(Canvas canvas,
            String plotTitle,
            String xLabel,
            String yLabel,
            double[] xs,
            double[] ys1,
            double[] ys2,
            boolean drawSlopeLines) {

        GraphicsContext gc = canvas.getGraphicsContext2D();
        double W = canvas.getWidth();
        double H = canvas.getHeight();

        double marginLeft = 70;
        double marginRight = 30;
        double marginTop = 50;
        double marginBottom = 60;
        double plotW = W - marginLeft - marginRight;
        double plotH = H - marginTop - marginBottom;

        gc.setFill(PANEL_COLOR);
        gc.fillRoundRect(0, 0, W, H, 16, 16);

        double[] logXs = log10Array(xs);
        double[] logYs1 = log10Array(ys1);
        double[] logYs2 = log10Array(ys2);

        double xMin = min(logXs) - 0.2;
        double xMax = max(logXs) + 0.2;
        double yMin = Math.min(min(logYs1), min(logYs2)) - 0.5;
        double yMax = Math.max(max(logYs1), max(logYs2)) + 0.5;

        gc.setStroke(GRID_COLOR);
        gc.setLineWidth(0.5);
        for (int exp = (int) Math.floor(xMin); exp <= (int) Math.ceil(xMax); exp++) {
            double px = toPixelX(exp, xMin, xMax, marginLeft, plotW);
            gc.strokeLine(px, marginTop, px, marginTop + plotH);
        }
        for (int exp = (int) Math.floor(yMin); exp <= (int) Math.ceil(yMax); exp++) {
            double py = toPixelY(exp, yMin, yMax, marginTop, plotH);
            gc.strokeLine(marginLeft, py, marginLeft + plotW, py);
        }

        gc.setStroke(AXIS_COLOR);
        gc.setLineWidth(1.5);
        gc.strokeLine(marginLeft, marginTop + plotH, marginLeft + plotW, marginTop + plotH);
        gc.strokeLine(marginLeft, marginTop, marginLeft, marginTop + plotH);

        gc.setFill(TEXT_COLOR);
        gc.setFont(Font.font("Monospace", 11));
        for (int exp = (int) Math.floor(xMin) + 1; exp <= (int) Math.ceil(xMax) - 1; exp++) {
            double px = toPixelX(exp, xMin, xMax, marginLeft, plotW);
            gc.setStroke(AXIS_COLOR);
            gc.setLineWidth(1);
            gc.strokeLine(px, marginTop + plotH, px, marginTop + plotH + 5);
            String label = exp == 0 ? "1" : "10" + superscript(exp);
            gc.fillText(label, px - 10, marginTop + plotH + 18);
        }
        for (int exp = (int) Math.floor(yMin) + 1; exp <= (int) Math.ceil(yMax) - 1; exp++) {
            double py = toPixelY(exp, yMin, yMax, marginTop, plotH);
            gc.setStroke(AXIS_COLOR);
            gc.setLineWidth(1);
            gc.strokeLine(marginLeft - 5, py, marginLeft, py);
            String label = exp == 0 ? "1" : "10" + superscript(exp);
            gc.fillText(label, marginLeft - 38, py + 4);
        }

        if (drawSlopeLines) {
            drawReferenceLine(gc, "slope 1 (Euler expected)",
                    xMin, xMax, yMin, yMax, marginLeft, marginTop, plotW, plotH,
                    1.0, logXs[logXs.length / 2], logYs1[logYs1.length / 2] + 0.3);
            drawReferenceLine(gc, "slope 4 (RK4 expected)",
                    xMin, xMax, yMin, yMax, marginLeft, marginTop, plotW, plotH,
                    4.0, logXs[logXs.length / 2], logYs2[logYs2.length / 2] + 0.3);
        }

        drawDataLine(gc, logXs, logYs1, EULER_COLOR, xMin, xMax, yMin, yMax, marginLeft, marginTop, plotW, plotH);
        drawDataLine(gc, logXs, logYs2, RK4_COLOR, xMin, xMax, yMin, yMax, marginLeft, marginTop, plotW, plotH);
        drawDataPoints(gc, logXs, logYs1, EULER_COLOR, xMin, xMax, yMin, yMax, marginLeft, marginTop, plotW, plotH);
        drawDataPoints(gc, logXs, logYs2, RK4_COLOR, xMin, xMax, yMin, yMax, marginLeft, marginTop, plotW, plotH);

        gc.setFill(TITLE_COLOR);
        gc.setFont(Font.font("Monospace", FontWeight.BOLD, 13));
        gc.fillText(plotTitle, marginLeft + plotW / 2.0 - plotTitle.length() * 3.5, 30);

        gc.setFill(TEXT_COLOR);
        gc.setFont(Font.font("Monospace", 12));
        gc.fillText(xLabel, marginLeft + plotW / 2.0 - xLabel.length() * 3.2, H - 10);

        gc.save();
        gc.translate(14, marginTop + plotH / 2.0 + yLabel.length() * 3.2);
        gc.rotate(-90);
        gc.fillText(yLabel, 0, 0);
        gc.restore();
    }

    private void drawReferenceLine(GraphicsContext gc, String label,
            double xMin, double xMax, double yMin, double yMax,
            double marginLeft, double marginTop, double plotW, double plotH,
            double slope, double x0, double y0) {
        gc.setStroke(REFERENCE_COLOR);
        gc.setLineWidth(1.0);
        gc.setLineDashes(6, 4);
        double px1 = toPixelX(xMin + 0.2, xMin, xMax, marginLeft, plotW);
        double py1 = toPixelY(y0 + slope * (xMin + 0.2 - x0), yMin, yMax, marginTop, plotH);
        double px2 = toPixelX(xMax - 0.2, xMin, xMax, marginLeft, plotW);
        double py2 = toPixelY(y0 + slope * (xMax - 0.2 - x0), yMin, yMax, marginTop, plotH);
        gc.strokeLine(px1, py1, px2, py2);
        gc.setLineDashes();
        gc.setFill(REFERENCE_COLOR);
        gc.setFont(Font.font("Monospace", 10));
        gc.fillText(label, px1 + 4, py1 - 4);
    }

    private void drawDataLine(GraphicsContext gc, double[] logXs, double[] logYs, Color color,
            double xMin, double xMax, double yMin, double yMax,
            double marginLeft, double marginTop, double plotW, double plotH) {
        gc.setStroke(color);
        gc.setLineWidth(2.5);
        gc.beginPath();
        for (int i = 0; i < logXs.length; i++) {
            double px = toPixelX(logXs[i], xMin, xMax, marginLeft, plotW);
            double py = toPixelY(logYs[i], yMin, yMax, marginTop, plotH);
            if (i == 0)
                gc.moveTo(px, py);
            else
                gc.lineTo(px, py);
        }
        gc.stroke();
    }

    private void drawDataPoints(GraphicsContext gc, double[] logXs, double[] logYs, Color color,
            double xMin, double xMax, double yMin, double yMax,
            double marginLeft, double marginTop, double plotW, double plotH) {
        double r = 5;
        gc.setFill(color);
        gc.setStroke(PANEL_COLOR);
        gc.setLineWidth(1.5);
        for (int i = 0; i < logXs.length; i++) {
            double px = toPixelX(logXs[i], xMin, xMax, marginLeft, plotW);
            double py = toPixelY(logYs[i], yMin, yMax, marginTop, plotH);
            gc.fillOval(px - r, py - r, 2 * r, 2 * r);
            gc.strokeOval(px - r, py - r, 2 * r, 2 * r);
        }
    }

    private HBox buildLegend() {
        HBox legend = new HBox(32);
        legend.setAlignment(Pos.CENTER);
        legend.getChildren().addAll(
                legendEntry(EULER_COLOR, "— Euler (1st order)"),
                legendEntry(RK4_COLOR, "— RK4 (4th order)"),
                legendEntry(REFERENCE_COLOR, "- - Reference slope"));
        return legend;
    }

    private HBox legendEntry(Color color, String text) {
        Label dot = new Label("●");
        dot.setTextFill(color);
        dot.setFont(Font.font(16));
        Label label = new Label(text);
        label.setTextFill(TEXT_COLOR);
        label.setFont(Font.font("Monospace", 12));
        HBox entry = new HBox(6, dot, label);
        entry.setAlignment(Pos.CENTER_LEFT);
        return entry;
    }

    private double toPixelX(double logX, double xMin, double xMax, double marginLeft, double plotW) {
        return marginLeft + (logX - xMin) / (xMax - xMin) * plotW;
    }

    private double toPixelY(double logY, double yMin, double yMax, double marginTop, double plotH) {
        return marginTop + (1.0 - (logY - yMin) / (yMax - yMin)) * plotH;
    }

    private double[] log10Array(double[] arr) {
        double[] result = new double[arr.length];
        for (int i = 0; i < arr.length; i++) {
            result[i] = arr[i] > 0 ? Math.log10(arr[i]) : -15;
        }
        return result;
    }

    private double min(double[] arr) {
        double m = arr[0];
        for (double v : arr)
            if (v < m)
                m = v;
        return m;
    }

    private double max(double[] arr) {
        double m = arr[0];
        for (double v : arr)
            if (v > m)
                m = v;
        return m;
    }

    private String superscript(int n) {
        String[] sup = { "⁰", "¹", "²", "³", "⁴", "⁵", "⁶", "⁷", "⁸", "⁹" };
        String s = "";
        if (n < 0) {
            s = "⁻";
            n = -n;
        }
        for (char c : String.valueOf(n).toCharArray())
            s += sup[c - '0'];
        return s;
    }

    private static class ExperimentData {
        final double[] stepSizes;
        final double[] eulerErrors, rk4Errors;
        final double[] eulerTimes, rk4Times;

        ExperimentData(double[] stepSizes,
                double[] eulerErrors, double[] rk4Errors,
                double[] eulerTimes, double[] rk4Times) {
            this.stepSizes = stepSizes;
            this.eulerErrors = eulerErrors;
            this.rk4Errors = rk4Errors;
            this.eulerTimes = eulerTimes;
            this.rk4Times = rk4Times;
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
