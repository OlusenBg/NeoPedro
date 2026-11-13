package dev.neopedro;

import java.util.Objects;
import java.util.function.BooleanSupplier;

/**
 * Runs an inner step again and again, either a fixed number of times or until a condition is met.
 * Use it for cycles such as "score three samples" or "keep intaking until the bucket is full".
 *
 * <pre>{@code
 * new RepeatStep(new AutoSequence(driveToPile, intake, driveToBasket, score), 3)
 * new RepeatStep(intakeCycle, () -> bucket.isFull())
 * }</pre>
 *
 * <p>The condition is checked before every run, including the first, and never interrupts a run that
 * is already in progress. The inner step is started afresh each time, so it must be safe to start
 * again (every NeoPedro step is).
 */
public class RepeatStep implements Step {
    private final Step inner;
    private final BooleanSupplier until;
    private int completedRuns;
    private boolean done;

    /**
     * Creates a step that repeats {@code inner} a fixed number of times.
     *
     * @param inner the step to repeat
     * @param times how many runs to do; must not be negative (0 does nothing)
     */
    public RepeatStep(Step inner, int times) {
        this.inner = Objects.requireNonNull(inner, "inner");
        if (times < 0) {
            throw new IllegalArgumentException("times must be >= 0 but was " + times);
        }
        this.until = () -> completedRuns >= times;
    }

    /**
     * Creates a step that repeats {@code inner} until a condition becomes true.
     * Give the inner step a way to end, or the condition must eventually turn true,
     * otherwise this never finishes.
     *
     * @param inner the step to repeat
     * @param until checked before each run; repeating stops as soon as it returns true
     */
    public RepeatStep(Step inner, BooleanSupplier until) {
        this.inner = Objects.requireNonNull(inner, "inner");
        this.until = Objects.requireNonNull(until, "until");
    }

    /** Starts the first run (or finishes at once if there is nothing to run). */
    @Override
    public void start() {
        completedRuns = 0;
        done = false;
        startNextRun();
    }

    /** Ticks the current run and starts the next one when it finishes. */
    @Override
    public void update() {
        if (done) {
            return;
        }
        inner.update();
        if (inner.isDone()) {
            completedRuns++;
            startNextRun();
        }
    }

    /**
     * Reports whether all repeats are complete.
     *
     * @return true once the count was reached or the condition became true
     */
    @Override
    public boolean isDone() {
        return done;
    }

    /** Stops the run in progress and prevents further repeats. */
    @Override
    public void stop() {
        if (!done) {
            inner.stop();
        }
        done = true;
    }

    private void startNextRun() {
        if (until.getAsBoolean()) {
            done = true;
            return;
        }
        inner.start();
    }
}
