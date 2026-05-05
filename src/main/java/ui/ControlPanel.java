package ui;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import io.CourseInputModuleStorage;

public class ControlPanel {

    private final VBox panel;
    private final ComboBox<String> solverPicker;
    private final ComboBox<String> botPicker;
    private final Button resetButton;
    private final Label statusLabel;
    private final Label shotCountLabel;
    private final Label positionLabel;
    private TextField startXField;
    private TextField startYField;
    private Button botButton;
    private TextField targetXField;
    private TextField targetYField;
    private TextField muKField;
    private TextField muSField;

    public ControlPanel(CourseInputModuleStorage course) {
        double[] start = course.getStartPosition();
        startXField = new TextField(String.valueOf(start[0])); // default starting position
        startYField = new TextField(String.valueOf(start[1])); // default starting position
        startXField.setMaxWidth(150);
        startYField.setMaxWidth(150);

        solverPicker = new ComboBox<>();
        solverPicker.getItems().addAll("rk4", "euler");
        solverPicker.setValue("rk4");
        solverPicker.setMaxWidth(150);

        botPicker = new ComboBox<>();
        botPicker.getItems().addAll("Rule Based", "Hill Climbing", "Newton Raphson");
        botPicker.setValue("Newton Raphson");
        botPicker.setMaxWidth(150);

        resetButton = new Button("Reset");
        botButton = new Button("Bot shot");
        resetButton.setMaxWidth(Double.MAX_VALUE);
        botButton.setMaxWidth(Double.MAX_VALUE);

        shotCountLabel = new Label("Shots: 0");
        shotCountLabel.setWrapText(true);
        shotCountLabel.setMaxWidth(150);

        // shows the status/messages, such as invalid input, etc.
        statusLabel = new Label("");
        statusLabel.setWrapText(true);
        statusLabel.setMaxWidth(150);

        positionLabel = new Label("");
        positionLabel.setWrapText(true);
        positionLabel.setMaxWidth(150);

        // initialize the textfields for the position of the target
        double[] target = course.getTargetPosition();
        targetXField = new TextField(String.valueOf(target[0]));
        targetYField = new TextField(String.valueOf(target[1]));
        targetXField.setMinWidth(150);
        targetXField.setMaxWidth(150);
        targetYField.setMaxWidth(150);

        // initialize the textfields for the friction
        muKField = new TextField(String.valueOf(course.getMuK()));
        muSField = new TextField(String.valueOf(course.getMuS()));
        muKField.setMaxWidth(130);
        muSField.setMaxWidth(130);

        // show labels and textfields
        panel = new VBox(10,
            new Label("Solver:"), solverPicker,
            new Separator(),
            new Label("Bot button:"), botPicker,
            botButton,
            new Separator(),
            resetButton,
            new Separator(),
            new Label("Start Position:"),
            new HBox(5, new Label("x"), startXField),
            new HBox(5, new Label("y"), startYField),
            new Separator(),
            new Label("Target Position:"),
            new HBox(5, new Label("x"), targetXField),
            new HBox(5, new Label("y"), targetYField),
            new Separator(),
            new Label("Friction:"),
            new HBox(5, new Label("\u00B5k"), muKField), // label µk
            new HBox(5, new Label("\u00B5s"), muSField), // label µs
            new Separator(),
            shotCountLabel,
            statusLabel,
            positionLabel
        );
        panel.setPadding(new Insets(10));
    }

    public VBox getPanel() {
        return panel;
    }

    public String getSelectedSolver() {
        return solverPicker.getValue();
    }

    public void setOnReset(Runnable handler) {
        resetButton.setOnAction(e -> handler.run());
    }

    public void setOnBotShoot(Runnable handler) {
        botButton.setOnAction(e -> handler.run());
    }

    public void updateShotCount(int shots) {
        shotCountLabel.setText("Shots: " + shots);
    }

    public void setStatus(String message, Color color) {
        statusLabel.setTextFill(color);
        statusLabel.setText(message);
    }

    public void setPosition(double x, double y) {
        positionLabel.setText(String.format("x: %.2f\ny: %.2f", x, y));
    }

    public void clearStatus() {
        statusLabel.setText("");
        positionLabel.setText("");
    }

    // reads input from textfieldss
    public double[] getStartPosition() {
        try {
            return new double[] {
                    Double.parseDouble(startXField.getText()),
                    Double.parseDouble(startYField.getText())
            };
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public double[] getTargetPosition() {
        try {
            return new double[]{
                Double.parseDouble(targetXField.getText()),
                Double.parseDouble(targetYField.getText())
            };
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public double[] getFriction() {
        try {
            return new double[]{
                Double.parseDouble(muKField.getText()),
                Double.parseDouble(muSField.getText())
            };
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public void setBotEnabled(boolean enabled) {
        botButton.setDisable(!enabled);
    }

    public String getSelectedBot() {
        return botPicker.getValue();
    }
}
