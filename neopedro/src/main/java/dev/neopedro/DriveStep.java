package dev.neopedro;

import com.pedropathing.follower.Follower;
import com.pedropathing.paths.Path;
import com.pedropathing.utils.Timer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Drives one path with Pedro's {@link Follower} and finishes when the follower reaches the end of it.
 * The robot comes to a stop at the end of the path before the next step begins; for motion that
 * flows through several paths without stopping, use {@link ChainStep}.
 *
 * <pre>{@code
 * new DriveStep(follower, Paths.line(start, score).facingForward())
 *     .maxPower(0.6)
 *     .timeoutSeconds(4)
 *     .onReachProgress(0.5, arm::up)   // raise the arm halfway there
 * }</pre>
 *
 * <p>The optional settings below are all builder-style and return this step. A DriveStep only talks
 * to Pedro through {@code follower.follow(...)}, {@code follower.hold(...)} and read-only getters.
 *
 * <p>Only one drive-like step should be active at a time, because there is only one follower. Do not
 * put two DriveSteps inside a {@link ParallelSteps}.
 */
public class DriveStep implements Step {
    private final Follower follower;
    private final Path path;
    private final List<Trigger> triggers = new ArrayList<>();
    private final Timer timer = new Timer();
    private Double maxPower;
    private Double decelerate;
    private double timeoutSeconds = Double.POSITIVE_INFINITY;
    private int segmentCount = 1;
    private boolean reachedEnd;
    private boolean abandoned;

    /**
     * Creates a step that follows a Pedro path. The path must have a heading (build it with
     * {@link Paths} or call one of Pedro's heading methods), otherwise Pedro rejects it in
     * {@link #start()}.
     *
     * @param follower the follower that will drive the robot
     * @param path the path to follow
     */
    public DriveStep(Follower follower, Path path) {
        this.follower = Objects.requireNonNull(follower, "follower");
        this.path = Objects.requireNonNull(path, "path");
    }

    /**
     * Creates a step that follows a {@link NeoPath}.
     *
     * @param follower the follower that will drive the robot
     * @param path the path to follow
     */
    public DriveStep(Follower follower, NeoPath path) {
        this(follower, Objects.requireNonNull(path, "path").toPedro());
    }

    /**
     * Limits how fast the robot drives this path, as a fraction of its top speed (Pedro's
     * {@code maxPathSpeed}). Use a low value for precise or risky moves. Pedro restores its normal
     * setting when the path ends. Needs Pedro's default Foresight algorithm.
     *
     * @param fraction from just above 0 up to 1, where 1 is full speed
     * @return this step
     */
    public DriveStep maxPower(double fraction) {
        this.maxPower = requireFraction(fraction, "maxPower");
        return this;
    }

    /**
     * Makes the robot slow down more gently at the end of this path, as a fraction of its natural
     * braking ability (Pedro's {@code maxDecelerationScale}). Use it to avoid wheel slip or tipping.
     * Smaller means gentler. Needs Pedro's default Foresight algorithm.
     *
     * @param fraction from just above 0 up to 1, where 1 is the natural deceleration
     * @return this step
     */
    public DriveStep decelerate(double fraction) {
        this.decelerate = requireFraction(fraction, "decelerate");
        return this;
    }

    /**
     * Gives up on the path if it takes too long, so a stuck robot cannot freeze the autonomous.
     * On timeout the robot holds where it is and the step counts as done.
     *
     * @param seconds the longest the step may take, in seconds; must not be negative
     * @return this step
     */
    public DriveStep timeoutSeconds(double seconds) {
        if (!(seconds >= 0)) {
            throw new IllegalArgumentException("timeoutSeconds must be >= 0 but was " + seconds);
        }
        this.timeoutSeconds = seconds;
        return this;
    }

    /**
     * Runs {@code action} once when the robot has covered a fraction of the path, so a mid-path
     * action does not need its own step. For a path made of several pieces, each piece counts
     * equally regardless of its length; use {@link #onReachProgress(int, double, Runnable)} to be exact.
     *
     * @param t how far along, from 0 (start) to 1 (end)
     * @param action the code to run
     * @return this step
     */
    public DriveStep onReachProgress(double t, Runnable action) {
        requireFractionOrZero(t, "t");
        Objects.requireNonNull(action, "action");
        triggers.add(new Trigger(() -> hasReached(t), action));
        return this;
    }

    /**
     * Runs {@code action} once when the robot reaches a point inside one piece of a multi-piece path.
     * Pieces are numbered from 0 in the order they were given. For a single path, the index is 0.
     *
     * @param pathIndex which piece of the path, counting from 0
     * @param t how far along that piece, from 0 (start) to 1 (end)
     * @param action the code to run
     * @return this step
     */
    public DriveStep onReachProgress(int pathIndex, double t, Runnable action) {
        if (pathIndex < 0) {
            throw new IllegalArgumentException("pathIndex must be >= 0 but was " + pathIndex);
        }
        requireFractionOrZero(t, "t");
        Objects.requireNonNull(action, "action");
        triggers.add(new Trigger(() -> hasReached(pathIndex, t), action));
        return this;
    }

    /**
     * Runs {@code action} once, a set time after the step starts, if the step is still running.
     *
     * @param milliseconds delay after the step starts, in milliseconds; must not be negative
     * @param action the code to run
     * @return this step
     */
    public DriveStep onReachTime(long milliseconds, Runnable action) {
        if (milliseconds < 0) {
            throw new IllegalArgumentException("milliseconds must be >= 0 but was " + milliseconds);
        }
        Objects.requireNonNull(action, "action");
        triggers.add(new Trigger(() -> timer.milliseconds() >= milliseconds, action));
        return this;
    }

    /** Applies any speed limits and tells the follower to start driving the path. */
    @Override
    public void start() {
        Path effective = PathKnobs.apply(follower, path, maxPower, decelerate);
        segmentCount = effective.getSegments().size();
        follower.follow(effective);
        triggers.forEach(Trigger::reset);
        timer.reset();
        reachedEnd = false;
        abandoned = false;
    }

    /**
     * Fires any callbacks that are due, notes whether the end of the path was reached, and enforces
     * the timeout. Pedro's answers are only fresh after {@code follower.update()}, so call this after it.
     */
    @Override
    public void update() {
        if (abandoned || reachedEnd) {
            return;
        }
        triggers.forEach(Trigger::update);
        if (follower.atParametricEnd()) {
            reachedEnd = true;
            return;
        }
        if (timer.seconds() >= timeoutSeconds) {
            holdHere();
        }
    }

    /**
     * Reports whether the robot has reached the end of the path (or the step timed out).
     * Never true before the first {@link #update()}, and once true it stays true even if the follower
     * is later given a new path.
     *
     * @return true once the follower was seen at the end of the path or the step gave up
     */
    @Override
    public boolean isDone() {
        return reachedEnd || abandoned;
    }

    /** Stops driving and holds the robot where it is. */
    @Override
    public void stop() {
        holdHere();
    }

    private void holdHere() {
        abandoned = true;
        follower.hold(follower.pose());
    }

    private boolean hasReached(double overallT) {
        double scaled = overallT * segmentCount;
        int index = Math.min((int) scaled, segmentCount - 1);
        return hasReached(index, scaled - index);
    }

    private boolean hasReached(int index, double localT) {
        int current = follower.pathIndex();
        if (current < 0) {
            return true; // follower already released the path, so every point was passed
        }
        if (current != index) {
            return current > index;
        }
        return follower.parametricCompletion() >= localT;
    }

    private static double requireFraction(double value, String what) {
        if (!(value > 0 && value <= 1)) {
            throw new IllegalArgumentException(what + " must be in (0, 1] but was " + value);
        }
        return value;
    }

    private static void requireFractionOrZero(double value, String what) {
        if (!(value >= 0 && value <= 1)) {
            throw new IllegalArgumentException(what + " must be in [0, 1] but was " + value);
        }
    }
}
