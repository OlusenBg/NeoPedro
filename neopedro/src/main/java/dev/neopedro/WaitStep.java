package dev.neopedro;

import com.pedropathing.utils.Timer;
import java.util.Objects;
import java.util.function.BooleanSupplier;

/**
 * Pauses the sequence, either for a fixed time or until a condition becomes true.
 *
 * <pre>{@code
 * new WaitStep(0.5)                                   // wait half a second
 * new WaitStep(() -> shooter.isReadyToShoot(), 3.0)   // wait for the shooter, give up after 3 s
 * }</pre>
 *
 * <p>Always give a condition wait a timeout unless hanging forever is what you want: a sensor that
 * never triggers would otherwise freeze the rest of your autonomous.
 *
 * <p>The condition is checked once per {@link #update()}, so a sensor is read once per loop.
 * Time is measured with Pedro's own {@link Timer}, which keeps NeoPedro free of any FTC SDK dependency.
 */
public class WaitStep implements Step {
    private final BooleanSupplier condition;
    private final double timeoutSeconds;
    private final Timer timer = new Timer();
    private boolean done;

    /**
     * Creates a step that waits a fixed amount of time.
     *
     * @param seconds how long to wait, in seconds; must not be negative
     */
    public WaitStep(double seconds) {
        this(() -> false, seconds);
    }

    /**
     * Creates a step that waits until {@code condition} is true, with no time limit.
     *
     * @param condition checked once per loop; the step is done when it returns true
     */
    public WaitStep(BooleanSupplier condition) {
        this(condition, Double.POSITIVE_INFINITY);
    }

    /**
     * Creates a step that waits until {@code condition} is true or {@code timeoutSeconds} have
     * passed, whichever comes first.
     *
     * @param condition checked once per loop; the step is done when it returns true
     * @param timeoutSeconds the longest the step may take, in seconds; must not be negative
     */
    public WaitStep(BooleanSupplier condition, double timeoutSeconds) {
        if (!(timeoutSeconds >= 0)) {
            throw new IllegalArgumentException("timeoutSeconds must be >= 0 but was " + timeoutSeconds);
        }
        this.condition = Objects.requireNonNull(condition, "condition");
        this.timeoutSeconds = timeoutSeconds;
    }

    /** Resets the clock and starts waiting. */
    @Override
    public void start() {
        timer.reset();
        done = false;
    }

    /** Checks the condition and the clock. */
    @Override
    public void update() {
        done = condition.getAsBoolean() || timer.seconds() >= timeoutSeconds;
    }

    /**
     * Reports whether the wait is over.
     *
     * @return true once the condition was seen true or the time ran out
     */
    @Override
    public boolean isDone() {
        return done;
    }
}
