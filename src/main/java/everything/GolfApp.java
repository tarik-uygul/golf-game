package everything;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class GolfApp extends Application {

    // change according to assignment
    private static final double DT = 0.01; // time step in seconds
    private static final double MAX_TIME = 60.0; // max simulation time per shot
    private static final double interfaceWidth = 1000;
    private static final double interfaceHeight = 600;

    @Override
    public void start(Stage stage) {
        try {
            CourseInputProcessing processor = new CourseInputProcessing();
            CourseConfiguration configuration = processor.buildConfig(
                    "1.0",        // temporarily used these functions since the expression parses cannot handle sin/cos yet
                    "0.08", "0.2",
                    "7.0", "8.0",
                    "14.0", "1.0",
                    "0.1", "0.01"
                    // "0.25*sin((x+y)/10)+1", // height expression
                    // "0.08", "0.2", // muK, muS
                    // "7.0", "8.0", // startX, startY
                    // "14.0", "1.0", // targetX, targetY
                    // "0.1", "0.01" // radius, stepSize
            );

            CourseProfile course = new CourseConfigurationProfile(configuration);

            // the course renderer handles all drawings on the canvas
            CourseRenderer renderer = new CourseRenderer(course, interfaceWidth, interfaceHeight);
            Canvas canvas = renderer.getCanvas();
            canvas.setWidth(interfaceWidth);
            canvas.setHeight(interfaceHeight);

            // StackPane locks the canvas to a size so it doesn't shift/move
            StackPane canvasHolder = new StackPane(canvas);
            canvasHolder.setMinSize(interfaceWidth, interfaceHeight);
            canvasHolder.setMaxSize(interfaceWidth, interfaceHeight);
            ControlPanel controls = new ControlPanel(course);
            // controller wires the buttons to the simulator and renderer, nothing works without this
            SimulationController ctrl = new SimulationController(
                    course, renderer, controls, DT, MAX_TIME
            );

            renderer.drawCourse();

            HBox root = new HBox();
            root.getChildren().addAll(controls.getPanel(), canvasHolder);

            stage.setScene(new Scene(root, interfaceWidth, interfaceHeight + 60));
            stage.setTitle("Putting game");
            stage.setResizable(false);
            stage.sizeToScene();
            stage.show();

            // for using the bots
            //ctrl.setBot(new RuleBasedBot());
            //ctrl.setBot(new MLBot());
            //HERE IT BECOMES
            //SimulationController ctrl = new SimulationController(
            //    course, renderer, controls, DT, MAX_TIME
            //);
            //
            //ctrl.setBot(new RuleBasedBot(DT, MAX_TIME, "rk4"));
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
