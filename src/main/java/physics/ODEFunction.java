package physics;

// so this is basically just a "blueprint" for what a physics function should look like
// to call SOMETHING to get the derivatives, but they dont know what system we are simulating
// so instead of hardcoding the physics into the solver we just say
// "whatever you pass in, it needs to have a compute method"
// thats all this interface does, it just enforces it
public interface ODEFunction {
    // you give it the current state of the system (positions, velocities etc packed
    // into an array)
    // and it spits back how fast everything is changing at that moment
    // so if the ball is at height 10 moving upward at 5 m/s, this tells you
    // "position is increasing at 5, velocity is decreasing at 9.81 because gravity"
    // the solver then uses those rates to figure out where the ball will be
    // stateRightNow is just whatever the ball/system looks like at this exact
    // moment in time
    double[] compute(double[] stateRightNow);
}
