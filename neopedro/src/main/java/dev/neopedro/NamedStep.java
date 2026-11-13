package dev.neopedro;

import java.util.Objects;

/**
 * Gives any step a readable label for telemetry, without changing how it behaves.
 *
 * <p>Use it when {@link AutoSequence#currentStepName()} would otherwise show the same generic name
 * (for example "ActionStep") for several different steps.
 *
 * <pre>{@code
 * new NamedStep("close claw", new ActionStep(claw::close))
 * }</pre>
 */
public class NamedStep implements Step {
    private final String label;
    private final Step inner;

    /**
     * Wraps {@code inner} under a new name.
     *
     * @param label the name to report; must not be null
     * @param inner the step that does the actual work; must not be null
     */
    public NamedStep(String label, Step inner) {
        this.label = Objects.requireNonNull(label, "label");
        this.inner = Objects.requireNonNull(inner, "inner");
    }

    /** Starts the wrapped step. */
    @Override
    public void start() {
        inner.start();
    }

    /** Updates the wrapped step. */
    @Override
    public void update() {
        inner.update();
    }

    /**
     * Reports whether the wrapped step is done.
     *
     * @return the wrapped step's answer
     */
    @Override
    public boolean isDone() {
        return inner.isDone();
    }

    /** Stops the wrapped step. */
    @Override
    public void stop() {
        inner.stop();
    }

    /**
     * Returns the custom label.
     *
     * @return the label given to the constructor
     */
    @Override
    public String name() {
        return label;
    }
}
