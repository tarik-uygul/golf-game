package everything;

import java.util.HashMap;
import java.util.Map;

public class FakeGUI {

    public static void main(String[] args) {

        // fake GUI inputs - these will come from real GUI input boxes later
        String[] names = {"x", "y"};
        String[] equations = {"a*x - b*x*y", "d*x*y - g*y"};

        Map<String, Double> constants = new HashMap<>();
        constants.put("a", 0.25);
        constants.put("b", 0.15);
        constants.put("d", 0.10);
        constants.put("g", 0.10);

        double[] initialConditions = {20.0, 2.0};
        double stepSize = 1.0;
        double startTime = 0.0;
        double endTime = 10.0;

        // now pass everything to the ODE system
        ODESystemBuilder ode = new ODESystemBuilder(equations, names, constants);

        // test it worked by asking for derivatives at initial state
        double[] derivatives = ode.compute(initialConditions);

        System.out.println("dx/dt = " + derivatives[0]); // should be -1.0
        System.out.println("dy/dt = " + derivatives[1]); // should be 3.8
    }
}
