package dev.neopedro;

import java.util.function.BooleanSupplier;

/**
 * Runs an action once, the first time a condition is seen true. Used internally by
 * {@link DriveStep} for its mid-path callbacks; call {@link #reset()} before each reuse.
 */
final class Trigger {
    private final BooleanSupplier condition;
    private final Runnable action;
    private boolean fired;

    Trigger(BooleanSupplier condition, Runnable action) {
        this.condition = condition;
        this.action = action;
    }

    /** Re-arms the trigger so it can fire again. */
    void reset() {
        fired = false;
    }

    /** Checks the condition and, the first time it holds, runs the action. */
    void update() {
        if (fired || !condition.getAsBoolean()) {
            return;
        }
        fired = true;
        action.run();
    }
}
