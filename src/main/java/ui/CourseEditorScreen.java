package ui;

import io.CourseInputModuleStorage;
import javafx.geometry.Insets;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import model.obstacles.Sand;
import model.obstacles.Tree;
import model.obstacles.Water;

import java.util.ArrayDeque;
import java.util.Deque;

public class CourseEditorScreen {

    //what obstacle type the user is currently placing
    public enum PlacementType { WATER, SAND, TREE }
    //place or erase mode
    public enum EditMode { PLACE, ERASE }

    private final HBox layout;
    private final CourseInputModuleStorage course;
    private final CourseRenderer renderer;
    private Canvas canvas;

    //undo stack - stores recently added obstacles so the user can remove them
    private final Deque<model.obstacles.Obstacle> undoStack = new ArrayDeque<>();

    //current editor state
    private PlacementType placementType = PlacementType.WATER;
    private EditMode editMode = EditMode.PLACE;
    private double currentRadius = 1.0;

    //sand friction (user-editable in the panel)
    private double sandMuK = Sand.DEFAULT_MU_K;
    private double sandMuS = Sand.DEFAULT_MU_S;

    public CourseEditorScreen(GolfApp app, CourseInputModuleStorage course) {
        this.course = course;

        //canvas side
        renderer = new CourseRenderer(course, 800, 600);
        canvas = renderer.getCanvas();
        renderer.drawCourse();

        StackPane canvasHolder = new StackPane(canvas);
        HBox.setHgrow(canvasHolder, Priority.ALWAYS);

        //side panel
        VBox sidePanel = buildSidePanel(app);

        layout = new HBox(sidePanel, canvasHolder);

        canvasHolder.layoutBoundsProperty().addListener((obs, oldBounds, newBounds) -> {
            double w = newBounds.getWidth();
            double h = newBounds.getHeight();
            if (w > 0 && h > 0) {
                canvas.setWidth(w);
                canvas.setHeight(h);
                renderer.resize(w, h, course);
                renderer.drawCourse();
            }
        });

        canvas.setOnMouseMoved(e -> {
            double wx = renderer.toWorldX(e.getX());
            double wy = renderer.toWorldY(e.getY());
            renderer.setGhostPreview(wx, wy, currentRadius);
            renderer.drawCourse();
        });

        canvas.setOnMouseExited(e -> {
            renderer.hideGhostPreview();
            renderer.drawCourse();
        });

        canvas.setOnMousePressed(e -> handleCanvasClick(e.getX(), e.getY()));
    }

    private void handleCanvasClick(double pixelX, double pixelY) {
        double wx = renderer.toWorldX(pixelX);
        double wy = renderer.toWorldY(pixelY);

        if (editMode == EditMode.ERASE) {
            //remove any obstacle whose circle contains this point
            course.getObstacles().removeIf(o -> o.contains(wx, wy));
            //erase doesn't touch the undo stack - undo only undoes placements
        } else {
            model.obstacles.Obstacle o = switch (placementType) {
                case WATER -> new Water(wx, wy, currentRadius);
                case SAND  -> new Sand(wx, wy, currentRadius, sandMuK, sandMuS);
                case TREE  -> new Tree(wx, wy, currentRadius);
            };
            course.addObstacle(o);
            undoStack.push(o);
        }

        renderer.drawCourse();
    }

    private VBox buildSidePanel(GolfApp app) {

        //obstacle type
        ComboBox<String> typePicker = new ComboBox<>();
        typePicker.getItems().addAll("Water", "Sand", "Tree");
        typePicker.setValue("Water");
        typePicker.setMaxWidth(150);
        typePicker.setOnAction(e -> {
            switch (typePicker.getValue()) {
                case "Water" -> placementType = PlacementType.WATER;
                case "Sand"  -> placementType = PlacementType.SAND;
                case "Tree"  -> placementType = PlacementType.TREE;
            }
        });

        //edit mode
        ComboBox<String> modePicker = new ComboBox<>();
        modePicker.getItems().addAll("Place", "Erase");
        modePicker.setValue("Place");
        modePicker.setMaxWidth(150);
        modePicker.setOnAction(e ->
                editMode = modePicker.getValue().equals("Erase") ? EditMode.ERASE : EditMode.PLACE
        );

        //radius slider
        Label radiusValue = new Label("1.00 m");
        radiusValue.setMaxWidth(150);

        Slider radiusSlider = new Slider(0.3, 4.0, 1.0);
        radiusSlider.setShowTickLabels(true);
        radiusSlider.setShowTickMarks(true);
        radiusSlider.setMajorTickUnit(1.0);
        radiusSlider.setMaxWidth(150);
        radiusSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            currentRadius = newVal.doubleValue();
            radiusValue.setText(String.format("%.2f m", currentRadius));
        });

        //sand friction fields
        TextField muKField = new TextField(String.valueOf(Sand.DEFAULT_MU_K));
        TextField muSField = new TextField(String.valueOf(Sand.DEFAULT_MU_S));
        muKField.setMaxWidth(130);
        muSField.setMaxWidth(130);

        muKField.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused) parseSandFriction(muKField, muSField);
        });
        muSField.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused) parseSandFriction(muKField, muSField);
        });

        //undo / clear
        Button undoBtn  = new Button("Undo last");
        Button clearBtn = new Button("Clear all");
        undoBtn.setMaxWidth(Double.MAX_VALUE);
        clearBtn.setMaxWidth(Double.MAX_VALUE);

        undoBtn.setOnAction(e -> {
            if (!undoStack.isEmpty()) {
                course.removeObstacle(undoStack.pop());
                renderer.drawCourse();
            }
        });
        clearBtn.setOnAction(e -> {
            course.clearObstacles();
            undoStack.clear();
            renderer.drawCourse();
        });

        //status label (what is being placed)
        Label statusLabel = new Label("");
        statusLabel.setWrapText(true);
        statusLabel.setMaxWidth(150);

        //update status label on type/mode change so the user knows what's active
        typePicker.setOnAction(e -> {
            switch (typePicker.getValue()) {
                case "Water" -> placementType = PlacementType.WATER;
                case "Sand"  -> placementType = PlacementType.SAND;
                case "Tree"  -> placementType = PlacementType.TREE;
            }
        });
        modePicker.setOnAction(e -> {
            editMode = modePicker.getValue().equals("Erase") ? EditMode.ERASE : EditMode.PLACE;
            statusLabel.setText(modePicker.getValue() + " mode");
        });

        //navigation
        Button playBtn = new Button("Play course");
        Button backBtn = new Button("Back");
        playBtn.setMaxWidth(Double.MAX_VALUE);
        backBtn.setMaxWidth(Double.MAX_VALUE);

        playBtn.setOnAction(e -> {
            canvas.widthProperty().unbind();
            canvas.heightProperty().unbind();
            app.startGameWithCourse(course);
        });
        backBtn.setOnAction(e -> app.showMainMenu());

        VBox panel = new VBox(10,
                new Label("Obstacle:"),   typePicker,
                new Separator(),
                new Label("Mode:"),       modePicker,
                new Separator(),
                new Label("Radius:"),     radiusSlider, radiusValue,
                new Separator(),
                new Label("friction:"),
                new HBox(5, new Label("µk"), muKField),
                new HBox(5, new Label("µs"), muSField),
                new Separator(),
                undoBtn,
                clearBtn,
                new Separator(),
                playBtn,
                backBtn,
                new Separator(),
                statusLabel
        );
        panel.setPadding(new Insets(10));
        panel.setMinWidth(170);
        panel.setMaxWidth(170);  // add this line
        return panel;
    }
    private void parseSandFriction(TextField muKField, TextField muSField) {
        try {
            double k = Double.parseDouble(muKField.getText());
            double s = Double.parseDouble(muSField.getText());
            if (k > 0 && s > 0 && k < s) {
                sandMuK = k;
                sandMuS = s;
            }
            //if invalid, keep the last valid values
        } catch (NumberFormatException ignored) {}
    }

    public HBox getLayout() { return layout; }
}