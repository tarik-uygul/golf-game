package ui;

import javax.swing.*;

import physics.EulerSolver;
import physics.ODEFunction;
import physics.ODESystemBuilder;
import physics.RungeKutta4;

import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Small ODE Visualizer
 *
 * Works with your existing classes:
 * - ODEFunction
 * - ODESystemBuilder
 * - EulerSolver
 * - RungeKutta4
 *
 * What it does:
 * - Lets the user type variable names, equations, constants, initial state, dt,
 * steps
 * - Lets the user choose Euler or RK4
 * - Simulates the system
 * - Draws the trajectory of the first two variables
 *
 * Example input:
 * names: x,v
 * equations: v,-x
 * constants:
 * initial: 1,0
 * dt: 0.05
 * steps: 400
 * solver: RK4
 *
 * This example plots x against v, which gives the phase portrait of a harmonic
 * oscillator.
 */
public class SmallODEVisualizer extends JFrame {

    private final JTextField namesField = new JTextField("x,v");
    private final JTextField equationsField = new JTextField("v,-x");
    private final JTextField constantsField = new JTextField("");
    private final JTextField initialStateField = new JTextField("1,0");
    private final JTextField dtField = new JTextField("0.05");
    private final JTextField stepsField = new JTextField("400");
    private final JComboBox<String> solverBox = new JComboBox<>(new String[] { "RK4", "Euler" });
    private final JLabel statusLabel = new JLabel("Enter values and press Run.");

    private final PlotPanel plotPanel = new PlotPanel();

    public SmallODEVisualizer() {
        super("Small ODE Visualizer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        JPanel inputs = new JPanel(new GridBagLayout());
        inputs.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        int row = 0;
        addRow(inputs, gbc, row++, "Variable names:", namesField, "comma-separated, e.g. x,v or x,y");
        addRow(inputs, gbc, row++, "Equations:", equationsField, "comma-separated, same order as names");
        addRow(inputs, gbc, row++, "Constants:", constantsField, "format a=0.25,b=0.15 (or leave empty)");
        addRow(inputs, gbc, row++, "Initial state:", initialStateField, "comma-separated, same order as names");
        addRow(inputs, gbc, row++, "dt:", dtField, "time step, e.g. 0.05");
        addRow(inputs, gbc, row++, "Steps:", stepsField, "number of integration steps");

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        inputs.add(new JLabel("Solver:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        inputs.add(solverBox, gbc);

        JButton runButton = new JButton("Run");
        runButton.addActionListener(e -> runSimulation());

        JButton presetHOButton = new JButton("Preset: Harmonic Oscillator");
        presetHOButton.addActionListener(e -> {
            namesField.setText("x,v");
            equationsField.setText("v,-x");
            constantsField.setText("");
            initialStateField.setText("1,0");
            dtField.setText("0.05");
            stepsField.setText("400");
            solverBox.setSelectedItem("RK4");
        });

        JButton presetLVButton = new JButton("Preset: Lotka-Volterra");
        presetLVButton.addActionListener(e -> {
            namesField.setText("x,y");
            equationsField.setText("a*x-b*x*y,d*x*y-g*y");
            constantsField.setText("a=0.25,b=0.15,d=0.10,g=0.10");
            initialStateField.setText("20,2");
            dtField.setText("0.05");
            stepsField.setText("800");
            solverBox.setSelectedItem("RK4");
        });

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controls.add(runButton);
        controls.add(presetHOButton);
        controls.add(presetLVButton);

        JPanel top = new JPanel(new BorderLayout());
        top.add(inputs, BorderLayout.CENTER);
        top.add(controls, BorderLayout.SOUTH);

        add(top, BorderLayout.NORTH);
        add(plotPanel, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);
    }

    private void addRow(JPanel panel, GridBagConstraints gbc, int row,
            String labelText, JTextField field, String hint) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        panel.add(new JLabel(labelText), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(field, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        JLabel hintLabel = new JLabel(hint);
        hintLabel.setForeground(Color.DARK_GRAY);
        panel.add(hintLabel, gbc);
    }

    private void runSimulation() {
        try {
            String[] names = splitCSV(namesField.getText());
            String[] equations = splitCSV(equationsField.getText());
            double[] initialState = parseDoubleArray(initialStateField.getText());
            Map<String, Double> constants = parseConstants(constantsField.getText());
            double dt = Double.parseDouble(dtField.getText().trim());
            int steps = Integer.parseInt(stepsField.getText().trim());
            String solver = ((String) solverBox.getSelectedItem()).toLowerCase();

            if (names.length < 2) {
                throw new IllegalArgumentException("Please provide at least 2 variables so something can be plotted.");
            }
            if (equations.length != names.length) {
                throw new IllegalArgumentException("Number of equations must match number of variable names.");
            }
            if (initialState.length != names.length) {
                throw new IllegalArgumentException("Initial state length must match number of variable names.");
            }
            if (dt <= 0) {
                throw new IllegalArgumentException("dt must be positive.");
            }
            if (steps <= 0) {
                throw new IllegalArgumentException("steps must be positive.");
            }

            ODESystemBuilder ode = new ODESystemBuilder(equations, names, constants);
            double[][] points = simulate(ode, initialState, dt, steps, solver);
            plotPanel.setData(points, names[0], names[1]);
            statusLabel.setText("Done. Showing trajectory of " + names[0] + " vs " + names[1] + " using "
                    + solver.toUpperCase() + ".");
        } catch (Exception ex) {
            statusLabel.setText("Error: " + ex.getMessage());
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Input Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private double[][] simulate(ODEFunction ode, double[] initialState, double dt, int steps, String solver) {
        double[][] points = new double[steps + 1][2];
        double[] state = initialState.clone();

        points[0][0] = state[0];
        points[0][1] = state[1];

        for (int i = 1; i <= steps; i++) {
            if (solver.equals("euler")) {
                state = EulerSolver.step(state, dt, ode);
            } else {
                state = RungeKutta4.step(state, dt, ode);
            }
            points[i][0] = state[0];
            points[i][1] = state[1];
        }
        return points;
    }

    private String[] splitCSV(String text) {
        String[] raw = text.split(",");
        for (int i = 0; i < raw.length; i++) {
            raw[i] = raw[i].trim();
        }
        return raw;
    }

    private double[] parseDoubleArray(String text) {
        String[] parts = splitCSV(text);
        double[] values = new double[parts.length];
        for (int i = 0; i < parts.length; i++) {
            values[i] = Double.parseDouble(parts[i]);
        }
        return values;
    }

    private Map<String, Double> parseConstants(String text) {
        Map<String, Double> constants = new LinkedHashMap<>();
        if (text == null || text.trim().isEmpty()) {
            return constants;
        }

        String[] pairs = text.split(",");
        for (String pair : pairs) {
            String trimmed = pair.trim();
            if (trimmed.isEmpty())
                continue;

            String[] sides = trimmed.split("=");
            if (sides.length != 2) {
                throw new IllegalArgumentException("Bad constant format: " + trimmed + ". Use a=0.25,b=0.15");
            }
            String name = sides[0].trim();
            double value = Double.parseDouble(sides[1].trim());
            constants.put(name, value);
        }
        return constants;
    }

    private static class PlotPanel extends JPanel {
        private double[][] data;
        private String xName = "x";
        private String yName = "y";

        public PlotPanel() {
            setBackground(Color.WHITE);
            setPreferredSize(new Dimension(900, 450));
        }

        public void setData(double[][] data, String xName, String yName) {
            this.data = data;
            this.xName = xName;
            this.yName = yName;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            int left = 60;
            int right = 20;
            int top = 20;
            int bottom = 50;
            int plotW = w - left - right;
            int plotH = h - top - bottom;

            g2.setColor(Color.WHITE);
            g2.fillRect(0, 0, w, h);

            g2.setColor(new Color(240, 240, 240));
            g2.fillRect(left, top, plotW, plotH);

            g2.setColor(Color.BLACK);
            g2.drawRect(left, top, plotW, plotH);

            g2.drawString(xName, left + plotW / 2, h - 15);
            g2.drawString(yName, 20, top + plotH / 2);

            if (data == null || data.length == 0) {
                g2.drawString("No data yet.", left + 20, top + 20);
                return;
            }

            double minX = data[0][0], maxX = data[0][0];
            double minY = data[0][1], maxY = data[0][1];
            for (double[] p : data) {
                minX = Math.min(minX, p[0]);
                maxX = Math.max(maxX, p[0]);
                minY = Math.min(minY, p[1]);
                maxY = Math.max(maxY, p[1]);
            }

            if (Math.abs(maxX - minX) < 1e-12) {
                maxX += 1;
                minX -= 1;
            }
            if (Math.abs(maxY - minY) < 1e-12) {
                maxY += 1;
                minY -= 1;
            }

            double padX = 0.08 * (maxX - minX);
            double padY = 0.08 * (maxY - minY);
            minX -= padX;
            maxX += padX;
            minY -= padY;
            maxY += padY;

            g2.setColor(Color.GRAY);
            g2.drawString(String.format("%.3f", minX), left, top + plotH + 15);
            g2.drawString(String.format("%.3f", maxX), left + plotW - 35, top + plotH + 15);
            g2.drawString(String.format("%.3f", minY), left - 45, top + plotH);
            g2.drawString(String.format("%.3f", maxY), left - 45, top + 10);

            g2.setColor(new Color(40, 120, 220));
            for (int i = 1; i < data.length; i++) {
                int x1 = mapX(data[i - 1][0], minX, maxX, left, plotW);
                int y1 = mapY(data[i - 1][1], minY, maxY, top, plotH);
                int x2 = mapX(data[i][0], minX, maxX, left, plotW);
                int y2 = mapY(data[i][1], minY, maxY, top, plotH);
                g2.drawLine(x1, y1, x2, y2);
            }

            g2.setColor(new Color(0, 160, 70));
            int sx = mapX(data[0][0], minX, maxX, left, plotW);
            int sy = mapY(data[0][1], minY, maxY, top, plotH);
            g2.fillOval(sx - 4, sy - 4, 8, 8);
            g2.drawString("start", sx + 6, sy - 6);

            g2.setColor(new Color(200, 40, 40));
            int ex = mapX(data[data.length - 1][0], minX, maxX, left, plotW);
            int ey = mapY(data[data.length - 1][1], minY, maxY, top, plotH);
            g2.fillOval(ex - 4, ey - 4, 8, 8);
            g2.drawString("end", ex + 6, ey - 6);
        }

        private int mapX(double x, double minX, double maxX, int left, int plotW) {
            return left + (int) ((x - minX) / (maxX - minX) * plotW);
        }

        private int mapY(double y, double minY, double maxY, int top, int plotH) {
            return top + (int) ((maxY - y) / (maxY - minY) * plotH);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SmallODEVisualizer().setVisible(true));
    }
}
