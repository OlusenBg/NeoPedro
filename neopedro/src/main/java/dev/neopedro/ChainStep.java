package dev.neopedro;

import com.pedropathing.follower.Follower;
import com.pedropathing.paths.Path;
import java.util.Objects;

/**
 * Drives several paths as one smooth, continuous motion, with no stop between them. This is
 * Pedro's compound path (the successor of the old {@code PathChain}), built for you.
 *
 * <pre>{@code
 * new ChainStep(follower,
 *         Paths.curve(start, control, spike).facingForward(),
 *         Paths.line(spike, score).facingConstant(Math.toRadians(135)))
 *     .maxPower(0.8)
 *     .onReachProgress(1, 0.5, intake::stop)   // halfway along the second path
 * }</pre>
 *
 * <p>Contrast with putting several {@link DriveStep}s in an {@link AutoSequence}: those stop
 * completely at the end of each path, which is slower but lets you do something at every stop.
 *
 * <p>It offers the same settings and callbacks as {@link DriveStep}; it is a thin wrapper around one.
 */
public class ChainStep implements Step {
    private final DriveStep drive;

    /**
     * Creates a continuous chain from {@link NeoPath}s, driven in the order given.
     *
     * @param follower the follower that will drive the robot
     * @param paths one or more paths, first to last
     */
    public ChainStep(Follower follower, NeoPath... paths) {
        this(follower, toPedro(paths));
    }

    /**
     * Creates a continuous chain from raw Pedro paths, driven in the order given.
     *
     * @param follower the follower that will drive the robot
     * @param paths one or more paths with headings, first to last
     */
    public ChainStep(Follower follower, Path... paths) {
        if (paths.length == 0) {
            throw new IllegalArgumentException("A chain needs at least one path");
        }
        this.drive = new DriveStep(follower, com.pedropathing.api.Paths.path(paths));
    }

    /**
     * Limits speed along the whole chain; see {@link DriveStep#maxPower(double)}.
     *
     * @param fraction from just above 0 up to 1, where 1 is full speed
     * @return this step
     */
    public ChainStep maxPower(double fraction) {
        drive.maxPower(fraction);
        return this;
    }

    /**
     * Softens braking at the end of the chain; see {@link DriveStep#decelerate(double)}.
     *
     * @param fraction from just above 0 up to 1, where 1 is the natural deceleration
     * @return this step
     */
    public ChainStep decelerate(double fraction) {
        drive.decelerate(fraction);
        return this;
    }

    /**
     * Gives up on the chain if it takes too long; see {@link DriveStep#timeoutSeconds(double)}.
     *
     * @param seconds the longest the step may take, in seconds
     * @return this step
     */
    public ChainStep timeoutSeconds(double seconds) {
        drive.timeoutSeconds(seconds);
        return this;
    }

    /**
     * Runs an action once at a fraction of the whole chain, counting every path in it equally;
     * see {@link DriveStep#onReachProgress(double, Runnable)}.
     *
     * @param t how far along the chain, from 0 to 1
     * @param action the code to run
     * @return this step
     */
    public ChainStep onReachProgress(double t, Runnable action) {
        drive.onReachProgress(t, action);
        return this;
    }

    /**
     * Runs an action once at a point inside one path of the chain, counting paths from 0; see
     * {@link DriveStep#onReachProgress(int, double, Runnable)}.
     *
     * @param pathIndex which path in the chain, counting from 0
     * @param t how far along that path, from 0 to 1
     * @param action the code to run
     * @return this step
     */
    public ChainStep onReachProgress(int pathIndex, double t, Runnable action) {
        drive.onReachProgress(pathIndex, t, action);
        return this;
    }

    /**
     * Runs an action once, a set time after the chain starts; see
     * {@link DriveStep#onReachTime(long, Runnable)}.
     *
     * @param milliseconds delay after the step starts, in milliseconds
     * @param action the code to run
     * @return this step
     */
    public ChainStep onReachTime(long milliseconds, Runnable action) {
        drive.onReachTime(milliseconds, action);
        return this;
    }

    /** Starts driving the chain. */
    @Override
    public void start() {
        drive.start();
    }

    /** Fires due callbacks and enforces the timeout. */
    @Override
    public void update() {
        drive.update();
    }

    /**
     * Reports whether the robot finished the last path in the chain.
     *
     * @return true once the chain is complete or timed out
     */
    @Override
    public boolean isDone() {
        return drive.isDone();
    }

    /** Stops and holds the robot where it is. */
    @Override
    public void stop() {
        drive.stop();
    }

    private static Path[] toPedro(NeoPath[] paths) {
        Path[] result = new Path[paths.length];
        for (int i = 0; i < paths.length; i++) {
            result[i] = Objects.requireNonNull(paths[i], "paths must not contain null").toPedro();
        }
        return result;
    }
}
