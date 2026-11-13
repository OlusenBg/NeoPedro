package dev.neopedro;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Chooses between two steps while the autonomous is running. The choice is made once, at the moment
 * the branch starts, using live data. Use it when the plan depends on something the robot only
 * learns mid-match, such as a camera detection or whether a game piece was picked up.
 *
 * <pre>{@code
 * new BranchStep(() -> vision.sawTag(),
 *     new DriveStep(follower, toTagSide),    // if the tag was seen
 *     new DriveStep(follower, toOtherSide))  // if not
 * }</pre>
 *
 * <p>The condition is not re-checked afterwards: whichever step was chosen runs to completion.
 */
public class BranchStep implements Step {
    private final Supplier<Boolean> condition;
    private final Step ifTrue;
    private final Step ifFalse;
    private Step chosen;

    /**
     * Creates a branch with a step for each outcome.
     *
     * @param condition evaluated once when the branch starts; a null result counts as false
     * @param ifTrue the step to run when the condition is true
     * @param ifFalse the step to run when the condition is false
     */
    public BranchStep(Supplier<Boolean> condition, Step ifTrue, Step ifFalse) {
        this.condition = Objects.requireNonNull(condition, "condition");
        this.ifTrue = Objects.requireNonNull(ifTrue, "ifTrue");
        this.ifFalse = Objects.requireNonNull(ifFalse, "ifFalse");
    }

    /**
     * Creates a branch that does nothing when the condition is false.
     *
     * @param condition evaluated once when the branch starts; a null result counts as false
     * @param ifTrue the step to run when the condition is true
     */
    public BranchStep(Supplier<Boolean> condition, Step ifTrue) {
        this(condition, ifTrue, new ActionStep(() -> {}));
    }

    /** Evaluates the condition, picks a step, and starts it. */
    @Override
    public void start() {
        chosen = Boolean.TRUE.equals(condition.get()) ? ifTrue : ifFalse;
        chosen.start();
    }

    /**
     * Updates the chosen step.
     *
     * @throws IllegalStateException if {@link #start()} was never called
     */
    @Override
    public void update() {
        if (chosen == null) {
            throw new IllegalStateException("BranchStep.update() called before start()");
        }
        chosen.update();
    }

    /**
     * Reports whether the chosen step has finished.
     *
     * @return true once the chosen step is done; false before the branch has started
     */
    @Override
    public boolean isDone() {
        return chosen != null && chosen.isDone();
    }

    /** Stops the chosen step if it is still running. */
    @Override
    public void stop() {
        if (chosen != null && !chosen.isDone()) {
            chosen.stop();
        }
    }
}
