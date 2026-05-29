package ui;

import io.CourseInputModuleStorage;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class GolfApp extends Application {

    private static final double DT = 0.01;
    private static final double MAX_TIME = 60.0;
    private static final double DEFAULT_WIDTH = 1000;
    private static final double DEFAULT_HEIGHT = 600;
    private static final double CONTROL_PANEL_WIDTH = 200;

    private Stage stage;

    @Override
    public void start(Stage mainStage) {
        stage = mainStage;
        stage.setTitle("Crazy Putting");
        stage.setResizable(true);
        stage.setMinWidth(650);
        stage.setMinHeight(400);
        showMainMenu();
        stage.show();
    }

    public void showMainMenu() {
        MainMenu menu = new MainMenu(this);
        Scene scene = new Scene(menu.getLayout());
        if (stage.getScene() == null) { // first time opening -> uses default window size
            stage.setWidth(DEFAULT_WIDTH);
            stage.setHeight(DEFAULT_HEIGHT);
        }
        stage.setScene(scene);
    }

    public void difficultyMenu() {
        DifficultyMenu menu = new DifficultyMenu(this);
        stage.setScene(new Scene(menu.getLayout())); // uses previous window size
    }

    public void difficultyMenuForEditor() {
        DifficultyMenu menu = new DifficultyMenu(this, true);
        stage.setScene(new Scene(menu.getLayout(), DEFAULT_WIDTH, DEFAULT_HEIGHT));
    }

    public void startEditor(String difficulty) {
        try {
            CourseInputModuleStorage course = switch (difficulty) {
                case "Easy"   -> FakeEasyCourse.build();
                case "Medium" -> FakeEasyCourse.build(); // replace later
                case "Hard"   -> FakeEasyCourse.build(); // replace later
                default -> throw new IllegalArgumentException("Invalid difficulty");
            };
            CourseEditorScreen editor = new CourseEditorScreen(this, course);
            stage.setScene(new Scene(editor.getLayout()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void startGameWithCourse(CourseInputModuleStorage course) {
        double initialCanvasW = DEFAULT_WIDTH - CONTROL_PANEL_WIDTH;
        double initialCanvasH = DEFAULT_HEIGHT;

        CourseRenderer renderer = new CourseRenderer(course, initialCanvasW, initialCanvasH);
        Canvas canvas = renderer.getCanvas();

        // wrap canvas in stackpane so it can resize with the window
        StackPane canvasHolder = new StackPane(canvas);
        // gives leftover space to canvas instead of control panel
        HBox.setHgrow(canvasHolder, Priority.ALWAYS);

        ControlPanel controls = new ControlPanel(course);
        controls.getPanel().setMinWidth(CONTROL_PANEL_WIDTH);
        controls.getPanel().setMaxWidth(CONTROL_PANEL_WIDTH);
        controls.setOnReturn(() -> difficultyMenu());

        // NOTHING WORKS WITHOUT ctrl DO NOT TOUCH
        SimulationController ctrl = new SimulationController(course, renderer, controls, DT, MAX_TIME);

        renderer.drawCourse();

        HBox root = new HBox(controls.getPanel(), canvasHolder);
        Scene scene = new Scene(root);

        canvasHolder.layoutBoundsProperty().addListener((obs, oldBounds, newBounds) -> {
            double w = newBounds.getWidth();
            double h = newBounds.getHeight();
            if (w > 0 && h > 0) {
                renderer.resize(w, h, course);
                renderer.drawCourse();
            }
        });

        stage.setScene(scene);
    }

    public void startGame(String difficulty) {
        try {
            CourseInputModuleStorage course = switch (difficulty) {
                case "Easy"   -> FakeEasyCourse.build();
                case "Medium" -> FakeEasyCourse.build(); // change to medium
                case "Hard"   -> FakeEasyCourse.build(); // change to hard
                default -> throw new IllegalArgumentException("Invalid difficulty");
            };
            startGameWithCourse(course);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Stage getStage() { return stage; }

    public static void main(String[] args) {
        launch(args);
    }
}