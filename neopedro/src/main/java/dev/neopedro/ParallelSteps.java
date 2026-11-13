package dev.neopedro;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Runs several steps at the same time and finishes when ALL of them are done.
 * Use it to do something while the robot drives: run the intake, spin up the shooter, lift the arm.
 *
 * <pre>{@code
 * new ParallelSteps(
 *     new DriveStep(follower, toScore),
 *     new ActionStep(shooter::spinUp),
 *     new WaitStep(() -> arm.isUp(), 2.0))
 * }</pre>
 *
 * <p>Include at most one drive-like step ({@link DriveStep}, {@link ChainStep}, {@link HoldStep}),
 * since there is only one follower. A step that has finished is not updated again.
 */
public class ParallelSteps implements Step {
    private final List<Step> steps;

    /**
     * Creates a group that runs the given steps together.
     *
     * @param steps the steps to run at once; may be empty (it then finishes at once)
     */
    public ParallelSteps(Step... steps) {
        List<Step> copy = new ArrayList<>(Arrays.asList(steps));
        copy.forEach(step -> Objects.requireNonNull(step, "steps must not contain null"));
        this.steps = Collections.unmodifiableList(copy);
    }

    /** Starts every inner step. */
    @Override
    public void start() {
        steps.forEach(Step::start);
    }

    /** Updates every inner step that is not finished yet. */
    @Override
    public void update() {
        for (Step step : steps) {
            if (!step.isDone()) {
                step.update();
            }
        }
    }

    /**
     * Reports whether every inner step has finished.
     *
     * @return true once all inner steps are done
     */
    @Override
    public boolean isDone() {
        return steps.stream().allMatch(Step::isDone);
    }

    /** Stops every inner step that is still running. */
    @Override
    public void stop() {
        for (Step step : steps) {
            if (!step.isDone()) {
                step.stop();
            }
        }
    }
}
