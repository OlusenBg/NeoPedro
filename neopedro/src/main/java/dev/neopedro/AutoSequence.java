package dev.neopedro;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * An ordered list of {@link Step}s that runs one after another. This is the heart of NeoPedro:
 * list what the robot should do, in order, and tick it once per loop.
 *
 * <pre>{@code
 * AutoSequence sequence = new AutoSequence(
 *     new DriveStep(follower, pathToScore),
 *     new ActionStep(claw::open),
 *     new DriveStep(follower, pathToPark));
 *
 * waitForStart();
 * sequence.start();
 * while (opModeIsActive() && !sequence.isFinished()) {
 *     follower.update();   // always before sequence.update()
 *     sequence.update();
 * }
 * }</pre>
 *
 * <p>A sequence is itself a {@link Step}, so you can nest one inside {@link ParallelSteps},
 * {@link RepeatStep} or {@link BranchStep}.
 */
public class AutoSequence implements Step {
    private static final int NOT_STARTED = -1;

    private final List<Step> steps;
    private int index = NOT_STARTED;

    /**
     * Creates a sequence that will run the given steps in order.
     *
     * @param steps the steps to run, first to last; may be empty (it then finishes at once)
     */
    public AutoSequence(Step... steps) {
        List<Step> copy = new ArrayList<>(Arrays.asList(steps));
        copy.forEach(step -> Objects.requireNonNull(step, "steps must not contain null"));
        this.steps = Collections.unmodifiableList(copy);
    }

    /** Starts (or restarts) the sequence from its first step. Call once after {@code waitForStart()}. */
    @Override
    public void start() {
        index = 0;
        startCurrentStep();
    }

    /**
     * Ticks the current step and moves to the next one when it finishes. Call once per loop,
     * after {@code follower.update()}.
     *
     * <p>A newly started step is not checked for completion until the following tick, so it always
     * sees at least one fresh {@code follower.update()} first.
     *
     * @throws IllegalStateException if {@link #start()} was never called
     */
    @Override
    public void update() {
        if (index == NOT_STARTED) {
            throw new IllegalStateException("AutoSequence.update() called before start()");
        }
        if (isFinished()) {
            return;
        }
        Step current = steps.get(index);
        current.update();
        if (current.isDone()) {
            index++;
            startCurrentStep();
        }
    }

    /**
     * Reports whether every step has completed.
     *
     * @return true after the last step finishes; false before {@link #start()} and while running
     */
    public boolean isFinished() {
        return index >= steps.size();
    }

    /**
     * Same as {@link #isFinished()}; lets a sequence be used as a step inside another sequence.
     *
     * @return true once every step has completed
     */
    @Override
    public boolean isDone() {
        return isFinished();
    }

    /**
     * Abandons the sequence: stops the step that is currently running and marks the sequence finished.
     * Use it to cut an autonomous short, for example when time is nearly up.
     */
    @Override
    public void stop() {
        if (index == NOT_STARTED || isFinished()) {
            return;
        }
        steps.get(index).stop();
        index = steps.size();
    }

    /**
     * Returns the name of the step that is running right now, for telemetry.
     *
     * @return the current step's {@link Step#name()}, {@code "not started"} or {@code "finished"}
     */
    public String currentStepName() {
        if (index == NOT_STARTED) {
            return "not started";
        }
        if (isFinished()) {
            return "finished";
        }
        return steps.get(index).name();
    }

    private void startCurrentStep() {
        if (!isFinished()) {
            steps.get(index).start();
        }
    }
}
