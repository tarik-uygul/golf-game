package src.test;

import bots.GolfBot;
import bots.Hill_Climbing_Bot;
import bots.Newton_Raphson_Bot;
import bots.RuleBasedBot;
import io.CourseInputModuleStorage;
import model.CourseConfiguration;
import model.CourseConfigurationProfile;
import model.GolfSimulator;
import model.ShotResult;

public class BotExperiment {

    public static void main(String[] args) {
        System.out.println("Starting Bot Performance Experiment...\n");

        // Simulation Parameters
        double dt = 0.01;
        double maxTime = 15.0;
        String solverType = "rk4";

        // Use the explicit test course mentioned in the manual: h(x,y) =
        // 0.25*sin((x+y)/10) + 1
        CourseInputModuleStorage testCourse = createTestCourse();
        double[] startPos = { 7.0, 8.0 }; // From the manual

        // Initialize the Bots
        GolfBot ruleBased = new RuleBasedBot(dt, maxTime);
        GolfBot hillClimbing = new Hill_Climbing_Bot(dt, maxTime, solverType);
        GolfBot newtonRaphson = new Newton_Raphson_Bot(dt, maxTime, solverType);

        System.out.println(String.format("%-20s | %-15s | %-20s | %-15s",
                "Bot Name", "Calc Time (ms)", "Final Distance (m)", "Result"));
        System.out.println("----------------------------------------------------------------------------------");

        // Run the Experiment for each bot
        runTest("Rule-Based Bot", ruleBased, testCourse, startPos, dt, maxTime, solverType);
        runTest("Hill Climbing Bot", hillClimbing, testCourse, startPos, dt, maxTime, solverType);
        runTest("Newton-Raphson Bot", newtonRaphson, testCourse, startPos, dt, maxTime, solverType);
    }

    private static void runTest(String botName, GolfBot bot, CourseInputModuleStorage course, double[] startPos,
            double dt, double maxTime, String solverType) {

        // Start timing
        long startTime = System.nanoTime();

        // Ask the bot to compute the shot
        double[] velocity = bot.computeShot(startPos, course);

        // Stop timing
        long endTime = System.nanoTime();
        double calcTimeMs = (endTime - startTime) / 1_000_000.0;

        // Run the shot through the simulator to verify where it actually lands
        GolfSimulator simulator = new GolfSimulator(course, solverType, dt, maxTime);
        ShotResult result = simulator.simulate(startPos, velocity);

        // Calculate the distance to the hole
        double[] target = course.getTargetPosition();
        double finalDist = Math.sqrt(Math.pow(result.getFinalX() - target[0], 2) +
                Math.pow(result.getFinalY() - target[1], 2));

        String outcome = (finalDist <= course.getTargetRadius()) ? "HOLE IN ONE!" : "Missed";

        // Print the formatted results
        System.out.println(String.format("%-20s | %-15.2f | %-20.4f | %-15s",
                botName, calcTimeMs, finalDist, outcome));
    }

    // Helper to create the specific test course from the Phase 2 Manual
    private static CourseInputModuleStorage createTestCourse() {
        CourseConfiguration config = new CourseConfiguration();
        config.setHeightProfile("0.25 * sin((x+y)/10) + 1");
        config.setFrictionCoefficient(0.08); // mu_K
        config.setStaticFriction(0.2); // mu_S
        config.setTargetPosition(new double[] { 14.0, 1.0 }); // (x_t, y_t)
        config.setTargetRadius(0.1);

        return new CourseConfigurationProfile(config);
    }
}