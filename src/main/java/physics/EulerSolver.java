package physics;

// euler method - simple version of solving differential equations
// in simple words: if you know where something is, and you know which direction its going,
// just. move it a little bit in that direction.

public class EulerSolver {

    // this does ONE step of the euler method
    // currentState - where everything is right now (position + velocity all in one
    // array)
    // dt - how big the time step is, like 0.01 means were jumping 0.01 seconds
    // forward
    // smaller dt = more accurate but also more steps to compute = slower
    // physicsFunc - this is the ODEFunction thing, basically the rules of physics
    // we call compute() on it to ask "how fast is everything changing right now",
    // so the derivatives
    public static double[] step(double[] currentState, double dt, ODEFunction physicsFunc) {
        // we want the detivatives at the point
        // for a golf ball this gives back things like "z is changing at rate vz" and
        // "vz is changing at -9.81"
        double[] ratesOfChange = physicsFunc.compute(currentState);

        // make a new array to store where everything will be after the step
        // we dont modify currentState directly because that would mess up the
        // calculation halfway through
        double[] nextState = new double[currentState.length];

        // now for each variable (x position, y position, z position, vx, vy, vz)
        // we do the same thing: new value = old value + (how fast its changing * how
        // much time passed)
        // which is: new_position = old_position + velocity * time
        // except were doing it for ALL variables at once including the velocities
        // themselves
        for (int idx = 0; idx < currentState.length; idx++) {
            nextState[idx] = currentState[idx] + dt * ratesOfChange[idx];
        }

        // hand back the new state - the solver doesnt touch the original, always
        // returns fresh array
        return nextState;
    }
}