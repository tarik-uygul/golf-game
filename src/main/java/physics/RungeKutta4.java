package physics;

// euler was simple but it only looks at the derivative at the START of the step
// which is a problem because the derivative is changing the whole time during the step

// rk4 fixes this by taking the derivative at FOUR different points within the step
// and then doing a weighted average of all four samples
// and average all those together

// the four samples are called k1 k2 k3 k4
// k1 = derivative at the very start of the step, using current state
// k2 = derivative at the midpoint, but we used k1 to estimate where midpoint is
// k3 = derivative at the midpoint AGAIN but this time we use k2 to estimate it
// k4 = derivative at the end of the step, using k3 to estimate where the end is

// then the final answer weights them as: (k1 + 2*k2 + 2*k3 + k4) / 6
// the middle samples (k2 and k3) get double weight because theyre more representative
// of whats happening in the MIDDLE of the interval which matters more than just the endpoints

public class RungeKutta4 {

    // same signature as euler - currentState, time step size, physics function
    // this is on purpose, the visualizer can swap between solvers without changing
    // anything else
    // both solvers take the same inputs and return the same thing but with
    // different accuracy
    public static double[] step(double[] currentState, double dt, ODEFunction physicsFunc) {

        // sample 1: derivative right at the start
        // this is exactly what euler uses, but rk4 uses it as just one of four samples
        double[] k1 = physicsFunc.compute(currentState);

        // sample 2: derivative at the midpoint of the step
        // but we dont KNOW where the midpoint is yet so we estimate it using k1
        // addScaled just means: currentState + (dt/2) * k1
        double[] k2 = physicsFunc.compute(addScaled(currentState, k1, dt / 2));

        // sample 3: derivative at the midpoint again but using k2 this time
        // k2 was a better estimate of the midpoint than k1 was
        // so using k2 to find the midpoint gives us an even better estimate
        double[] k3 = physicsFunc.compute(addScaled(currentState, k2, dt / 2));

        // sample 4: derivative at the end of the step
        // we use k3 to estimate where the end of the step is
        // note this uses dt not dt/2, were projecting all the way to the end now
        double[] k4 = physicsFunc.compute(addScaled(currentState, k3, dt));

        double[] nextState = new double[currentState.length];

        // now combine all four samples with the weighted average formula
        // k1 and k4 (the endpoints) each count once
        // k2 and k3 (the midpoints) each count twice
        // divide by 6 total (1 + 2 + 2 + 1 = 6) to get the average
        // this weighted average is way more accurate than just using k1 alone (which is
        // euler)
        for (int idx = 0; idx < currentState.length; idx++) {
            nextState[idx] = currentState[idx] + (dt / 6.0) * (k1[idx] + 2 * k2[idx] + 2 * k3[idx] + k4[idx]);
        }

        return nextState;
    }

    // little helper function to avoid repeating ourselves
    // computes: baseState + scaleFactor * derivative
    // this is how we "project" the state forward by some fraction of a step
    // without this wed have to write the same loop four times up above which would
    // be messy
    private static double[] addScaled(double[] baseState, double[] derivative, double scaleFactor) {
        double[] result = new double[baseState.length];
        for (int idx = 0; idx < baseState.length; idx++) {
            result[idx] = baseState[idx] + scaleFactor * derivative[idx];
        }
        return result;
    }
}
