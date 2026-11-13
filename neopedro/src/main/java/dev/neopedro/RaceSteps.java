package dev.neopedro;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Runs several steps at the same time and finishes as soon as ANY ONE of them is done.
 * The steps that did not finish are stopped. Use it for "do this until that happens", such as
 * driving until a sensor triggers, or doing something with a time limit.
 *
 * <pre>{@code
 * new RaceSteps(
 *     new DriveStep(follower, creepForward),
 *     new WaitStep(() -> distanceSensor.seesWall()))
 * }</pre>
 *
 * <p>If the losing step was a drive, it is stopped with {@link Step#stop()}, which holds the robot
 * where it is, so it does not carry on driving.
 */
public class RaceSteps implements Step {
    private final List<Step> steps;
    private boolean decided;

    /**
     * Creates a race between the given steps.
     *
     * @param steps the competitors; may be empty (it then finishes at once)
     */
    public RaceSteps(Step... steps) {
        List<Step> copy = new ArrayList<>(Arrays.asList(steps));
        copy.forEach(step -> Objects.requireNonNull(step, "steps must not contain null"));
        this.steps = Collections.unmodifiableList(copy);
    }

    /** Starts every competitor. */
    @Override
    public void start() {
        decided = false;
        steps.forEach(Step::start);
    }

    /** Updates the competitors in order until one finishes, then stops the others. */
    @Override
    public void update() {
        if (decided) {
            return;
        }
        for (Step step : steps) {
            step.update();
            if (step.isDone()) {
                stopEveryoneExcept(step);
                return;
            }
        }
    }

    /**
     * Reports whether any competitor has finished.
     *
     * @return true as soon as one inner step is done (or if there are none)
     */
    @Override
    public boolean isDone() {
        return decided || steps.isEmpty();
    }

    /** Stops every competitor that is still running. */
    @Override
    public void stop() {
        if (!decided) {
            stopEveryoneExcept(null);
        }
    }

    private void stopEveryoneExcept(Step winner) {
        decided = true;
        for (Step step : steps) {
            if (step != winner && !step.isDone()) {
                step.stop();
            }
        }
    }
}
