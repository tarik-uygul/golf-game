# Golf Simulator 🏌️

A physics-based golf game simulator in Java with multiple AI bots that learn to play golf on different courses.

## What This Project Does

This is a golf simulator where:
- **You define courses** with custom height functions (the terrain)
- **Multiple AI bots** compete to reach the hole in fewer shots
- **Physics simulation** models realistic golf ball behavior with friction and gravity
- **Interactive GUI** lets you visualize the course and watch bots play

Three different bot strategies are implemented:
1. **Rule-Based Bot** - Simple heuristic approach (accurate but slow)
2. **Hill Climbing Bot** - Optimization algorithm (fast and good)
3. **Newton-Raphson Bot** - Advanced numerical method (fastest)

---

## Getting Started

### Prerequisites

You need:
- **Java 25** (or Java 21+)
- **Maven 3.9+**
- **JavaFX SDK 25** (already configured in `pom.xml`)

### Quick Start

1. **Clone or download** this project
2. **Open a terminal** in the project folder
3. **Run the GUI app:**

```bash
mvn javafx:run
```

The interactive golf simulator will launch. You can:
- See the 3D course terrain
- Watch AI bots play
- Adjust shot parameters and replay

---

## How to Use

### Running the Interactive GUI

```bash
mvn javafx:run
```

This starts the main golf simulator application with a visual interface where you can:
- View the course in 3D
- Simulate shots with different initial velocities
- Watch how different bot strategies approach the same course

### Running the Bot Performance Benchmark

To compare how fast and accurate each bot is:

```bash
mvn clean test-compile
java -cp target/classes:target/test-classes BotExperiment
```

This runs all three bots on a test course and shows:
- How many shots each bot took
- How long it took (in milliseconds)
- Whether they successfully reached the hole

### Running Individual Tests

If you want to run specific test suites:

```bash
# Compile tests first
mvn test-compile

# Run physics solver accuracy tests
java -cp target/classes:target/test-classes SolverAccuracyTest

# Run parser tests
java -cp target/classes:target/test-classes ParserTests

# Run sample accuracy tests
java -cp target/classes:target/test-classes SampleAccuracyTestSuite
```

---

## Project Structure

```
src/main/java/
├── bots/                  # AI bot implementations
│   ├── GolfBot.java       # Interface for all bots
│   ├── RuleBasedBot.java
│   ├── Hill_Climbing_Bot.java
│   └── Newton_Raphson_Bot.java
├── model/                 # Core data structures
│   ├── GolfSimulator.java # Main physics engine
│   ├── CourseProfile.java
│   ├── ShotResult.java
│   └── ...
├── physics/               # ODE solvers (Euler, Runge-Kutta 4)
│   ├── RungeKutta4.java
│   └── ...
├── io/                    # Input handling and course storage
│   ├── CourseInputModuleStorage.java
│   └── ...
└── ui/                    # GUI components
    ├── GolfApp.java       # Main application
    ├── Visualizer.java
    └── ...

src/test/java/
├── BotExperiment.java     # Bot performance comparison
├── ParserTests.java
└── ...
```

---

## How the Physics Works

The simulator uses **ODE solvers** (Runge-Kutta 4 by default) to simulate a golf ball rolling on a curved surface:

1. You define a **height function** `h(x, y)` that describes the terrain
2. You provide an **initial velocity** (vx, vy)
3. The simulator calculates:
   - Gravity pulling the ball downhill
   - Friction resisting motion
   - Ball trajectory until it stops
4. Returns the **landing position**

Example height function: `0.25 * sin((x+y)/10) + 1` creates a wavy course.

---

## Understanding the Bots

### Rule-Based Bot
- Uses simple rules to estimate the best shot
- Very accurate but computationally slow (~14 seconds per course)
- Good for validation

### Hill Climbing Bot
- Iteratively improves the shot direction
- Fast (~135 ms) and reasonably accurate
- Gets stuck in local minima sometimes

### Newton-Raphson Bot
- Uses advanced calculus to find the optimal shot
- Fastest (~346 ms) with good accuracy
- Most sophisticated approach

Run `BotExperiment` to see how each performs on a test course.

---

## Troubleshooting

### "Command not found: mvn"
Install Maven or ensure it's in your PATH. Check with:
```bash
mvn -v
```

### "JavaFX modules not found"
The `pom.xml` includes JavaFX automatically. Try:
```bash
mvn clean install
mvn javafx:run
```

### Tests don't compile
Make sure you're in the project root directory:
```bash
cd /path/to/team_02
mvn clean test-compile
```

### GUI doesn't appear
The application may be starting—wait a few seconds. If it still fails, check console output for errors.

---

## Development Notes

- **Main entry point**: `ui.GolfApp` (GUI application)
- **Test entry points**: `BotExperiment`, `ParserTests`, etc.
- **Configuration**: Course parameters are in `CourseInputModuleStorage`
- **ODE Solver**: Default is Runge-Kutta 4 (RK4) for accuracy

---

## Questions?

Check the code comments for detailed explanations of:
- How each bot algorithm works
- Physics calculations in `GolfSimulator.java`
- ODE solver implementations in the `physics/` package

Happy golfing! ⛳
