package everything;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Visualizer extends Application {

    @Override
    public void start(Stage primaryStage) {

        NumberAxis xAxisLabel = new NumberAxis();
        NumberAxis yAxisLabel = new NumberAxis();
        LineChart<Number, Number> theChart = new LineChart<>(xAxisLabel, yAxisLabel);
        theChart.setAnimated(false);

        VBox mainLayout = new VBox(10);
        Scene scene = new Scene(new ScrollPane(mainLayout), 900, 700);
        primaryStage.setScene(scene);
        primaryStage.setTitle("ODE Simulator");
        primaryStage.show();

        // ---- SCREEN 1 ----
        ComboBox<String> solverBox = new ComboBox<>();
        solverBox.getItems().addAll("Euler", "RK4");
        solverBox.setValue("Euler");

        TextField numVarsField = new TextField("2");
        Button confirmSetup = new Button("Next");

        GridPane screen1 = new GridPane();
        screen1.setHgap(8);
        screen1.setVgap(8);
        screen1.addRow(0, new Label("Choose solver:"), solverBox);
        screen1.addRow(1, new Label("Number of variables:"), numVarsField);
        screen1.add(confirmSetup, 0, 2);

        mainLayout.getChildren().add(screen1);

        confirmSetup.setOnAction(e -> {

            mainLayout.getChildren().removeIf(node -> node != screen1);

            int numVars = Integer.parseInt(numVarsField.getText());

            // ---- SCREEN 2 ----
            GridPane screen2 = new GridPane();
            screen2.setHgap(8);
            screen2.setVgap(8);

            int row = 0;

            // variable name input boxes
            screen2.addRow(row++, new Label("--- Variable names ---"));
            TextField[] nameFields = new TextField[numVars];
            for (int i = 0; i < numVars; i++) {
                nameFields[i] = new TextField("var" + (i + 1));
                screen2.addRow(row++,
                        new Label("Name for variable " + (i + 1) + ":"),
                        nameFields[i]);
            }

            // equation input boxes
            screen2.addRow(row++, new Label("--- Equations ---"));
            TextField[] equationFields = new TextField[numVars];
            for (int i = 0; i < numVars; i++) {
                equationFields[i] = new TextField();
                equationFields[i].setPrefWidth(300);
                equationFields[i].setPromptText("e.g. a*x - b*x*y");
                screen2.addRow(row++,
                        new Label("d(var" + (i + 1) + ")/dt ="),
                        equationFields[i]);
            }

            // constants
            screen2.addRow(row++, new Label("--- Constants (name=value, one per line) ---"));
            TextArea constantsArea = new TextArea();
            constantsArea.setPromptText("a=0.25\nb=0.15\nd=0.10\ng=0.10");
            constantsArea.setPrefRowCount(4);
            constantsArea.setPrefWidth(300);
            screen2.addRow(row++, constantsArea);

            // initial conditions
            screen2.addRow(row++, new Label("--- Initial conditions ---"));
            TextField[] initFields = new TextField[numVars];
            for (int i = 0; i < numVars; i++) {
                initFields[i] = new TextField("1.0");
                screen2.addRow(row++,
                        new Label("var" + (i + 1) + "(0):"),
                        initFields[i]);
            }

            // time settings
            screen2.addRow(row++, new Label("--- Time settings ---"));
            TextField timeStep = new TextField("0.1");
            TextField simDuration = new TextField("10");
            screen2.addRow(row++,
                    new Label("Step size:"), timeStep,
                    new Label("Total time:"), simDuration);

            // axis selectors
            screen2.addRow(row++, new Label("--- Plot settings ---"));
            ComboBox<String> plotXAxis = new ComboBox<>();
            ComboBox<String> plotYAxis = new ComboBox<>();
            plotXAxis.getItems().add("t");
            for (int i = 0; i < numVars; i++) {
                plotXAxis.getItems().add("var" + (i + 1));
                plotYAxis.getItems().add("var" + (i + 1));
            }
            plotXAxis.setValue("t");
            plotYAxis.setValue("var1");

            screen2.addRow(row++,
                    new Label("X axis:"), plotXAxis,
                    new Label("Y axis:"), plotYAxis);

            Button runButton = new Button("Run Simulation");
            screen2.add(runButton, 0, row);

            mainLayout.getChildren().addAll(screen2, theChart);

            runButton.setOnAction(ev -> {

                theChart.getData().clear();

                // collect actual variable names from user input
                String[] names = new String[numVars];
                for (int i = 0; i < numVars; i++) {
                    names[i] = nameFields[i].getText().trim();
                }

                // collect equations
                String[] equations = new String[numVars];
                for (int i = 0; i < numVars; i++) {
                    equations[i] = equationFields[i].getText().trim();
                }

                // parse constants from text area
                Map<String, Double> constants = new HashMap<>();
                for (String line : constantsArea.getText().split("\n")) {
                    line = line.trim();
                    if (line.contains("=")) {
                        String[] parts = line.split("=");
                        try {
                            constants.put(parts[0].trim(),
                                    Double.parseDouble(parts[1].trim()));
                        } catch (NumberFormatException ex) {
                            System.out.println("bad constant: " + line);
                        }
                    }
                }

                // collect initial conditions
                double[] currentState = new double[numVars];
                for (int i = 0; i < numVars; i++) {
                    currentState[i] = Double.parseDouble(
                            initFields[i].getText().trim());
                }

                double dt = Double.parseDouble(timeStep.getText().trim());
                double totalTime = Double.parseDouble(simDuration.getText().trim());

                // build ODE system with user provided names and equations
                ODESystemBuilder ode = new ODESystemBuilder(equations, names, constants);

                // run simulation
                List<double[]> allStates = new ArrayList<>();
                List<Double> allTimes = new ArrayList<>();
                double currentTime = 0;

                while (currentTime <= totalTime) {
                    allStates.add(currentState.clone());
                    allTimes.add(currentTime);

                    if (solverBox.getValue().equals("Euler")) {
                        currentState = EulerSolver.step(currentState, dt, ode);
                    } else {
                        currentState = RungeKutta4.step(currentState, dt, ode);
                    }

                    currentTime += dt;
                }

                // figure out which variable index the user selected
                String xChoice = plotXAxis.getValue();
                String yChoice = plotYAxis.getValue();

                XYChart.Series<Number, Number> plotLine = new XYChart.Series<>();
                plotLine.setName(yChoice + " vs " + xChoice);

                for (int i = 0; i < allStates.size(); i++) {
                    double[] snap = allStates.get(i);
                    double t = allTimes.get(i);

                    double xVal = getVal(xChoice, snap, t, numVars);
                    double yVal = getVal(yChoice, snap, t, numVars);

                    plotLine.getData().add(new XYChart.Data<>(xVal, yVal));
                }

                xAxisLabel.setLabel(xChoice);
                yAxisLabel.setLabel(yChoice);
                theChart.getData().add(plotLine);
            });
        });
    }

    // extracts value by var name like "var1", "var2", or "t"
    private double getVal(String name, double[] state, double t, int numVars) {
        if (name.equals("t")) return t;
        for (int i = 0; i < numVars; i++) {
            if (name.equals("var" + (i + 1))) return state[i];
        }
        return 0;
    }

    public static void main(String[] args) {
        launch(args);
    }
}

