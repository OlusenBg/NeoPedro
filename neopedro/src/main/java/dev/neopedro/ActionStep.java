package dev.neopedro;

import java.util.Objects;

/**
 * Runs a piece of code once, instantly, and is done. Use it for anything that is not driving:
 * raise the arm, close the claw, set a servo, reset an encoder.
 *
 * <pre>{@code
 * new ActionStep(claw::close)
 * }</pre>
 *
 * <p>The step does not wait for the mechanism to finish moving. If it needs time to settle, follow it
 * with a {@link WaitStep}.
 */
public class ActionStep implements Step {
    private final Runnable action;

    /**
     * Creates a step that runs {@code action} once when the step starts.
     *
     * @param action the code to run; must not be null
     */
    public ActionStep(Runnable action) {
        this.action = Objects.requireNonNull(action, "action");
    }

    /** Runs the action. */
    @Override
    public void start() {
        action.run();
    }

    /** Does nothing; the work already happened in {@link #start()}. */
    @Override
    public void update() {}

    /**
     * Always true: an action has no duration.
     *
     * @return true
     */
    @Override
    public boolean isDone() {
        return true;
    }
}
