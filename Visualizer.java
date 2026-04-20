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

// this is the main GUI window for the ODE simulator
// it lets the user type in any ODE system as equation strings,
// pick a solver, set step size and duration, then plots the result
//
// the GUI does three things:
//   1. collects equation strings, variable names, constants, initial conditions
//   2. builds an ODESystemBuilder from those inputs (which implements ODEFunction)
//   3. runs the solver loop and plots whatever variables the user picks
//
// the solvers (euler and rk4) know nothing about what equations are being solved
// the GUI knows nothing about how the solvers work
// all the physics/math is in the equation strings the user types in
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

        // ---- SCREEN 1: choose solver and number of variables ----
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

            int numVars;
            try {
                numVars = Integer.parseInt(numVarsField.getText().trim());
            } catch (NumberFormatException ex) {
                numVarsField.setText("2");
                return;
            }

            // ---- SCREEN 2: enter equations, constants, initial conditions ----
            GridPane screen2 = new GridPane();
            screen2.setHgap(8);
            screen2.setVgap(8);
            int row = 0;

            // variable name fields
            screen2.addRow(row++, new Label("--- Variable names ---"));
            TextField[] nameFields = new TextField[numVars];
            for (int i = 0; i < numVars; i++) {
                nameFields[i] = new TextField("var" + (i + 1));
                screen2.addRow(row++,
                    new Label("Name for variable " + (i + 1) + ":"),
                    nameFields[i]);
            }

            // equation fields - user types the right-hand side of each d/dt equation
            // supports basic arithmetic, ^ for powers, and sin/cos/sqrt/etc
            // example: "a*x - b*x*y" for the first Lotka-Volterra equation
            screen2.addRow(row++, new Label("--- Equations (type the right-hand side) ---"));
            TextField[] equationFields = new TextField[numVars];
            for (int i = 0; i < numVars; i++) {
                equationFields[i] = new TextField();
                equationFields[i].setPrefWidth(300);
                equationFields[i].setPromptText("e.g. a*x - b*x*y");
                screen2.addRow(row++,
                    new Label("d(var" + (i + 1) + ")/dt ="),
                    equationFields[i]);
            }

            // constants - one per line in "name=value" format
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

            // axis selectors - user picks what to plot on each axis
            // "t" means plot against time, otherwise pick a variable
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
                try {
                    theChart.getData().clear();

                    // collect variable names from the input fields
                    String[] names = new String[numVars];
                    for (int i = 0; i < numVars; i++) {
                        names[i] = nameFields[i].getText().trim();
                    }
                    System.out.println("Variable names: " + java.util.Arrays.toString(names));

                    // collect equation strings
                    String[] equations = new String[numVars];
                    for (int i = 0; i < numVars; i++) {
                        equations[i] = equationFields[i].getText().trim();
                    }
                    System.out.println("Equations: " + java.util.Arrays.toString(equations));

                    // parse constants from the text area
                    // each line should look like "a=0.25"
                    Map<String, Double> constants = new HashMap<>();
                    for (String line : constantsArea.getText().split("\n")) {
                        line = line.trim();
                        if (line.contains("=")) {
                            String[] parts = line.split("=");
                            try {
                                constants.put(parts[0].trim(),
                                    Double.parseDouble(parts[1].trim()));
                            } catch (NumberFormatException ex) {
                                System.out.println("skipping bad constant line: " + line);
                            }
                        }
                    }
                    System.out.println("Constants: " + constants);

                    // collect initial conditions
                    double[] currentState = new double[numVars];
                    for (int i = 0; i < numVars; i++) {
                        currentState[i] = Double.parseDouble(initFields[i].getText().trim());
                    }
                    System.out.println("Initial state: " + java.util.Arrays.toString(currentState));

                    double dt = Double.parseDouble(timeStep.getText().trim());
                    double totalTime = Double.parseDouble(simDuration.getText().trim());
                    System.out.println("dt=" + dt + ", totalTime=" + totalTime);

                    // build the ODE system from the strings the user typed
                    // this is what connects the GUI inputs to the solvers
                    ODESystemBuilder ode = new ODESystemBuilder(equations, names, constants);

                    // run the simulation loop
                    // we store every state so we can plot the full trajectory afterwards
                    // note: we clone() each state before storing because arrays are references,
                    // without clone() every entry would just point to the final state
                    List<double[]> allStates = new ArrayList<>();
                    List<Double> allTimes = new ArrayList<>();
                    double currentTime = 0;
                    int stepCount = 0;

                    while (currentTime <= totalTime) {
                        allStates.add(currentState.clone());
                        allTimes.add(currentTime);

                        if (solverBox.getValue().equals("Euler")) {
                            currentState = EulerSolver.step(currentState, dt, ode);
                        } else {
                            currentState = RungeKutta4.step(currentState, dt, ode);
                        }
                        currentTime += dt;
                        stepCount++;
                    }
                    System.out.println("Simulation complete. Steps: " + stepCount + ", States collected: " + allStates.size());
                    System.out.println("Final state: " + java.util.Arrays.toString(allStates.get(allStates.size()-1)));

                    // pull out the two variables the user wants to see and plot them
                    String xChoice = plotXAxis.getValue();
                    String yChoice = plotYAxis.getValue();
                    System.out.println("Plotting: X=" + xChoice + ", Y=" + yChoice);

                    XYChart.Series<Number, Number> plotLine = new XYChart.Series<>();
                    plotLine.setName(yChoice + " vs " + xChoice);

                    for (int i = 0; i < allStates.size(); i++) {
                        double[] snap = allStates.get(i);
                        double t = allTimes.get(i);

                        double xVal = getVal(xChoice, snap, t, names);
                        double yVal = getVal(yChoice, snap, t, names);

                        plotLine.getData().add(new XYChart.Data<>(xVal, yVal));
                    }

                    System.out.println("Plot points added: " + plotLine.getData().size());
                    xAxisLabel.setLabel(xChoice);
                    yAxisLabel.setLabel(yChoice);
                    theChart.getData().add(plotLine);
                    System.out.println("Chart updated successfully!");
                    
                    // Print results as a table
                    System.out.println("\n" + "=".repeat(100));
                    System.out.println("SIMULATION RESULTS");
                    System.out.println("=".repeat(100));
                    
                    // Print header
                    StringBuilder header = new StringBuilder();
                    header.append(String.format("%-8s", "Time"));
                    for (String name : names) {
                        header.append(String.format("%-15s", name));
                    }
                    System.out.println(header);
                    System.out.println("-".repeat(100));
                    
                    // Print every 10th state to avoid too much output
                    int printInterval = Math.max(1, allStates.size() / 20);
                    for (int i = 0; i < allStates.size(); i += printInterval) {
                        StringBuilder rowBuilder = new StringBuilder();
                        rowBuilder.append(String.format("%-8.4f", allTimes.get(i)));
                        for (double val : allStates.get(i)) {
                            rowBuilder.append(String.format("%-15.6f", val));
                        }
                        System.out.println(rowBuilder);
                    }
                    
                    // Print final state
                    if ((allStates.size() - 1) % printInterval != 0) {
                        StringBuilder finalRowBuilder = new StringBuilder();
                        finalRowBuilder.append(String.format("%-8.4f", allTimes.get(allStates.size() - 1)));
                        for (double val : allStates.get(allStates.size() - 1)) {
                            finalRowBuilder.append(String.format("%-15.6f", val));
                        }
                        System.out.println(finalRowBuilder);
                    }
                    
                    System.out.println("=".repeat(100));
                    System.out.println("FINAL STATE:");
                    for (int i = 0; i < names.length; i++) {
                        System.out.printf("  %s = %.6f%n", names[i], allStates.get(allStates.size() - 1)[i]);
                    }
                    System.out.println("=".repeat(100) + "\n");
                    
                    // Remove any existing evaluation panels before adding new one
                    mainLayout.getChildren().removeIf(node -> node instanceof VBox && 
                        ((VBox)node).getStyle().contains("-fx-border-color"));
                    
                    // Add evaluation section to the layout
                    addEvaluationPanel(mainLayout, allStates, allTimes, names);
                    
                } catch (Exception ex) {
                    System.err.println("ERROR during simulation: " + ex.getMessage());
                    ex.printStackTrace();
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Error");
                    alert.setContentText("Error: " + ex.getMessage());
                    alert.showAndWait();
                }
            });
        });
    }

    // maps a variable name (like "var1", "x", or "t") to its value in the state array
    // "t" returns current time, everything else is looked up by matching the names array
    private double getVal(String name, double[] state, double t, String[] names) {
        if (name.equals("t")) return t;
        
        // Try to match the variable name directly
        for (int i = 0; i < names.length; i++) {
            if (name.equals(names[i])) {
                return state[i];
            }
        }
        
        // Also try matching "var1", "var2", etc. format
        for (int i = 0; i < names.length; i++) {
            if (name.equals("var" + (i + 1))) {
                return state[i];
            }
        }
        
        System.err.println("Warning: Variable '" + name + "' not found. Available: " + java.util.Arrays.toString(names));
        return 0;
    }
    
    // =========================================================================
    // Add evaluation panel to query function values at specific times
    // =========================================================================
    private void addEvaluationPanel(VBox layout, List<double[]> allStates, List<Double> allTimes, String[] names) {
        VBox evalPanel = new VBox(10);
        evalPanel.setStyle("-fx-border-color: #ccc; -fx-border-radius: 5; -fx-padding: 15;");
        
        Label evalTitle = new Label("Evaluate at Specific Time");
        evalTitle.setStyle("-fx-font-size: 14; -fx-font-weight: bold;");
        
        HBox inputBox = new HBox(10);
        Label timeLabel = new Label("Time t =");
        TextField timeField = new TextField("0.0");
        timeField.setPrefWidth(100);
        Button evalBtn = new Button("Evaluate");
        TextArea resultArea = new TextArea();
        resultArea.setPrefRowCount(8);
        resultArea.setEditable(false);
        resultArea.setStyle("-fx-font-family: 'Monospace'; -fx-font-size: 10;");
        
        inputBox.getChildren().addAll(timeLabel, timeField, evalBtn);
        
        evalBtn.setOnAction(e -> {
            try {
                double queryTime = Double.parseDouble(timeField.getText().trim());
                
                // Find the closest time in the simulation
                int closestIdx = 0;
                double minDiff = Math.abs(allTimes.get(0) - queryTime);
                
                for (int i = 0; i < allTimes.size(); i++) {
                    double diff = Math.abs(allTimes.get(i) - queryTime);
                    if (diff < minDiff) {
                        minDiff = diff;
                        closestIdx = i;
                    }
                }
                
                double actualTime = allTimes.get(closestIdx);
                double[] state = allStates.get(closestIdx);
                
                StringBuilder result = new StringBuilder();
                result.append("TIME EVALUATION RESULT\n");
                result.append("=".repeat(60)).append("\n");
                result.append("Requested time: t = ").append(String.format("%.6f", queryTime)).append("\n");
                result.append("Actual time:    t = ").append(String.format("%.6f", actualTime));
                if (minDiff > 1e-6) {
                    result.append(" (diff: ").append(String.format("%.8f", minDiff)).append(")");
                }
                result.append("\n").append("-".repeat(60)).append("\n");
                
                result.append("STATE VALUES:\n");
                for (int i = 0; i < names.length; i++) {
                    result.append(String.format("  %-20s = %15.10f%n", names[i], state[i]));
                }
                
                result.append("\n-".repeat(60)).append("\n");
                result.append("SIMULATION INFO:\n");
                result.append(String.format("  Total states: %d%n", allStates.size()));
                result.append(String.format("  Time range: [%.6f, %.6f]%n", allTimes.get(0), allTimes.get(allTimes.size()-1)));
                result.append(String.format("  Sample rate: ~%.4f%n", allTimes.size() > 1 ? (allTimes.get(1) - allTimes.get(0)) : 0));
                result.append("=".repeat(60)).append("\n");
                
                resultArea.setText(result.toString());
                
                // Also print to console
                System.out.println("\n" + "=".repeat(70));
                System.out.println("POINT EVALUATION AT t = " + String.format("%.6f", actualTime));
                System.out.println("=".repeat(70));
                System.out.println(result.toString());
                System.out.println("=".repeat(70) + "\n");
                
            } catch (NumberFormatException ex) {
                resultArea.setText("Error: Invalid time value. Please enter a number.");
            }
        });
        
        evalPanel.getChildren().addAll(evalTitle, inputBox, new Label("Result:"), resultArea);
        layout.getChildren().add(evalPanel);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
