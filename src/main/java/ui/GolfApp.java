package ui;

import io.CourseInputModule;
import io.CourseInputModuleStorage;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class GolfApp extends Application {

    private static final double DT = 0.01;
    private static final double MAX_TIME = 60.0;
    private static final double interfaceWidth = 1000;
    private static final double interfaceHeight = 600;

    private Stage stage;

    @Override
    public void start(Stage mainStage) {
        stage = mainStage;
        stage.setTitle("Crazy Putting");
        stage.setResizable(false);
        showMainMenu();
        stage.show();
    }

    public void showMainMenu() {
        MainMenu menu = new MainMenu(this);
        stage.setScene(new Scene(menu.getLayout(), interfaceWidth, interfaceHeight));
    }

    public void difficultyMenu() {
        DifficultyMenu menu = new DifficultyMenu(this);
        stage.setScene(new Scene(menu.getLayout(), interfaceWidth, interfaceHeight));
    }

    public void startGame(String difficulty) {
        try {
            CourseInputModuleStorage course;

            switch (difficulty) {

                case "Easy":
                    course = FakeEasyCourse.build();
                    break;

                case "Medium":
                    course = FakeEasyCourse.build(); // change to medium
                    break;

                case "Hard":
                    course = FakeEasyCourse.build(); // change to hard
                    break;

                default:
                    throw new IllegalArgumentException("Invalid difficulty");
            }

            CourseRenderer renderer = new CourseRenderer(course, interfaceWidth, interfaceHeight);

            Canvas canvas = renderer.getCanvas();

            canvas.setWidth(interfaceWidth);
            canvas.setHeight(interfaceHeight);

            StackPane canvasHolder = new StackPane(canvas);

            ControlPanel controls = new ControlPanel(course);

            SimulationController ctrl = new SimulationController(course, renderer, controls, DT, MAX_TIME);

            renderer.drawCourse();

            HBox root = new HBox();

            root.getChildren().addAll(controls.getPanel(), canvasHolder);

            Scene scene = new Scene(root, interfaceWidth, interfaceHeight + 60);

            stage.setScene(scene);

        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}