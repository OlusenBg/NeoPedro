package dev.neopedro;

import com.pedropathing.follower.Follower;
import com.pedropathing.math.Pose;
import com.pedropathing.utils.Timer;
import java.util.Objects;
import java.util.function.BooleanSupplier;

/**
 * Holds the robot at a fixed pose using Pedro's {@code follower.hold(pose)}. Use it to stay put
 * while a mechanism works, for example while an arm scores or a shooter fires.
 *
 * <pre>{@code
 * new HoldStep(follower, scorePose).timeoutSeconds(2)
 * new HoldStep(follower).until(() -> shooter.isEmpty())   // hold wherever the robot is now
 * }</pre>
 *
 * <p>By default a HoldStep never finishes on its own: it holds until the sequence ends. Give it a
 * {@link #timeoutSeconds(double) timeout}, an {@link #until(BooleanSupplier) condition}, or call
 * {@link #finish()} from elsewhere to let the sequence move on. Note that Pedro already holds the end
 * pose after a path; use this step when you want to hold a different pose, or to hold for a while.
 */
public class HoldStep implements Step {
    private final Follower follower;
    private final Pose pose;
    private final Timer timer = new Timer();
    private BooleanSupplier until = () -> false;
    private double timeoutSeconds = Double.POSITIVE_INFINITY;
    private boolean finished;

    /**
     * Creates a step that holds a specific pose.
     *
     * @param follower the follower that will hold the robot
     * @param pose the pose to hold
     */
    public HoldStep(Follower follower, Pose pose) {
        this.follower = Objects.requireNonNull(follower, "follower");
        this.pose = Objects.requireNonNull(pose, "pose");
    }

    /**
     * Creates a step that holds wherever the robot is at the moment the step starts.
     *
     * @param follower the follower that will hold the robot
     */
    public HoldStep(Follower follower) {
        this.follower = Objects.requireNonNull(follower, "follower");
        this.pose = null;
    }

    /**
     * Ends the hold after a set time.
     *
     * @param seconds how long to hold, in seconds; must not be negative
     * @return this step
     */
    public HoldStep timeoutSeconds(double seconds) {
        if (!(seconds >= 0)) {
            throw new IllegalArgumentException("timeoutSeconds must be >= 0 but was " + seconds);
        }
        this.timeoutSeconds = seconds;
        return this;
    }

    /**
     * Ends the hold when a condition becomes true.
     *
     * @param condition checked once per loop; the step is done when it returns true
     * @return this step
     */
    public HoldStep until(BooleanSupplier condition) {
        this.until = Objects.requireNonNull(condition, "condition");
        return this;
    }

    /**
     * Ends the hold from outside, for example from a callback or another step's action.
     * The step reports done immediately; the sequence moves on at its next update.
     */
    public void finish() {
        finished = true;
    }

    /** Tells the follower to hold the pose and starts the clock. */
    @Override
    public void start() {
        follower.hold(pose != null ? pose : follower.pose());
        timer.reset();
        finished = false;
    }

    /** Checks the timeout and the end condition. */
    @Override
    public void update() {
        if (timer.seconds() >= timeoutSeconds || until.getAsBoolean()) {
            finished = true;
        }
    }

    /**
     * Reports whether the hold was ended.
     *
     * @return true only after a timeout, the condition, or {@link #finish()}; otherwise false forever
     */
    @Override
    public boolean isDone() {
        return finished;
    }
}
