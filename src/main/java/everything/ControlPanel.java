package everything;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import java.util.function.Consumer;

public class ControlPanel {

    private final VBox panel;
    private final TextField vxField;
    private final TextField vyField;
    private final ComboBox<String> solverPicker;
    private final Button shootButton;
    private final Button resetButton;
    private final Label statusLabel;
    private final Label shotCountLabel;
    private final Label positionLabel;
    private TextField startXField;
    private TextField startYField;
    private Button botButton;

    public ControlPanel(CourseProfile course) {
        vxField = new TextField("0.0");
        vyField = new TextField("0.0");
        // to make sure the interface of the game doesn't move because the textfields become bigger/smaller
        vxField.setPrefWidth(150);
        vxField.setMinWidth(150);
        vxField.setMaxWidth(150);
        vyField.setPrefWidth(150);
        vyField.setMinWidth(150);
        vyField.setMaxWidth(150);

        // use this to hardcode the textfield (when course input didnt give starting position)
        // also change public ControlPanel(CourseProfile course) to public ControlPanel()
        // and ControlPanel controls = new ControlPanel(course); to ControlPanel controls = new ControlPanel(); (in GolfApp.java)
        // startXField = new TextField("7.0"); // starting position when user didn't input an x value (yet)
        // startYField = new TextField("8.0"); // starting position when user didn't input a y value (yet)
        double[] start = course.getStartPosition();
        startXField = new TextField(String.valueOf(start[0])); //default starting position if in the courseprofile
        startYField = new TextField(String.valueOf(start[1])); // default starting position if in the courseprofile
        startXField.setMaxWidth(150);
        startYField.setMaxWidth(150);

        solverPicker = new ComboBox<>();
        solverPicker.getItems().addAll("rk4", "euler");
        solverPicker.setValue("rk4");
        solverPicker.setMaxWidth(150);

        shootButton   = new Button("Shoot");
        resetButton   = new Button("Reset");
        botButton = new Button("Bot shot");
        shootButton.setMaxWidth(Double.MAX_VALUE);
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

        // show labels and textfields
        panel = new VBox(10,
                new Label("vx:"), vxField,
                new Label("vy:"), vyField,
                new Label("Solver:"), solverPicker,
                new Separator(),
                shootButton,
                resetButton,
                botButton,
                new Separator(),
                new Label("Start Position:"),
                new HBox(5, new Label("x"), startXField),
                new HBox(5, new Label("y"), startYField),
                new Separator(),
                shotCountLabel,
                statusLabel,
                positionLabel
        );
        panel.setPadding(new Insets(10));
    }

    public VBox getPanel() {return panel;}

    public String getSelectedSolver() { return solverPicker.getValue(); }

    public void setOnShoot(Consumer<double[]> handler) {
        shootButton.setOnAction(e -> {
            try {
                double vx = Double.parseDouble(vxField.getText());
                double vy = Double.parseDouble(vyField.getText());
                double speed = Math.sqrt(vx*vx + vy*vy);
                double maxSpeed = 5.0;
                if (speed > maxSpeed) {
                    vx = vx / speed * maxSpeed;
                    vy = vy / speed * maxSpeed;
                }
                handler.accept(new double[]{vx, vy});
            } catch (NumberFormatException ex) {
                setStatus("Invalid input", Color.RED);
            }
        });
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
            return new double[]{
                    Double.parseDouble(startXField.getText()),
                    Double.parseDouble(startYField.getText())
            };
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public void setShootEnabled(boolean enabled) {
        shootButton.setDisable(!enabled);
        botButton.setDisable(!enabled);
    }
}

