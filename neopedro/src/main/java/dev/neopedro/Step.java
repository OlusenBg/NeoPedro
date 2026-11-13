package dev.neopedro;

/**
 * One unit of autonomous work: drive a path, run an action, wait, hold, or a combination of those.
 *
 * <p>Steps are driven by an {@link AutoSequence} (or by a control-flow step such as
 * {@link ParallelSteps}). The owner calls {@link #start()} once, then calls {@link #update()} once per
 * OpMode loop iteration until {@link #isDone()} is true. A step may be started again after it
 * finishes, which is how {@link RepeatStep} replays its inner step.
 *
 * <p>Implement this interface to add your own step types; anything that fits "start, tick until
 * done" works with every other NeoPedro class.
 */
public interface Step {

    /**
     * Begins the step. Called exactly once before the first {@link #update()}.
     * Use it to send commands (start following a path, run an action, reset a timer).
     */
    void start();

    /**
     * Ticks the step once. Called once per OpMode loop iteration, after {@code follower.update()}.
     * Keep it fast; it runs on the OpMode thread.
     */
    void update();

    /**
     * Reports whether the step has finished.
     *
     * @return true once the step has nothing left to do and the next step may begin
     */
    boolean isDone();

    /**
     * Abandons the step before it finished, for example when it loses a {@link RaceSteps} race.
     * The default does nothing. Steps that leave the robot moving (such as {@link DriveStep}) override
     * it to bring the robot to rest.
     */
    default void stop() {}

    /**
     * Returns a short label for telemetry, shown by {@link AutoSequence#currentStepName()}.
     * Defaults to the class name; wrap a step in {@link NamedStep} to give it a better label.
     *
     * @return a human-readable name for this step
     */
    default String name() {
        return getClass().getSimpleName();
    }
}
