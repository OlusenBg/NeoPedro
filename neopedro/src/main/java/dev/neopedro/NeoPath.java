package dev.neopedro;

import com.pedropathing.math.Pose;
import com.pedropathing.paths.Path;
import com.pedropathing.paths.interpolator.Interpolator;
import java.util.Objects;

/**
 * A Pedro {@link Path} plus readable methods for choosing which way the robot faces along it.
 * Create one with {@link Paths#line(Pose, Pose)} or {@link Paths#curve(Pose...)}.
 *
 * <pre>{@code
 * NeoPath toScore = Paths.line(start, score).facingForward();
 * NeoPath toSpike = Paths.curve(score, control, spike).facingConstant(Math.toRadians(90));
 * }</pre>
 *
 * <p>A NeoPath is immutable: every {@code facing...} method returns a new NeoPath and leaves the
 * original untouched. Heading values are in radians, like everything in Pedro.
 *
 * <p>All the real work is done by Pedro; each method here is a one-line call to the matching
 * Pedro {@code Path} method.
 */
public final class NeoPath {
    private final Path path;

    NeoPath(Path path) {
        this.path = Objects.requireNonNull(path, "path");
    }

    /**
     * Makes the robot face the direction it is travelling (the path's tangent), nose first.
     * The usual choice for driving from A to B in a straight, natural way.
     *
     * @return a new NeoPath with this heading
     */
    public NeoPath facingForward() {
        return new NeoPath(path.tangent());
    }

    /**
     * Makes the robot face directly away from the direction of travel (tail first), so it drives
     * the path in reverse. Useful for backing into a scoring position.
     *
     * @return a new NeoPath with this heading
     */
    public NeoPath facingBackward() {
        return new NeoPath(path.reverseTangent());
    }

    /**
     * Keeps the robot at one fixed heading for the whole path, whatever direction it travels in.
     * Handy for strafing sideways or driving to a spot while an intake keeps facing the field.
     *
     * @param headingRadians the heading to hold, in radians
     * @return a new NeoPath with this heading
     */
    public NeoPath facingConstant(double headingRadians) {
        return new NeoPath(path.constant(headingRadians));
    }

    /**
     * Turns the robot smoothly from one heading to another over the length of the path.
     * Use it when the robot has to end up facing a different way than it started.
     *
     * @param startHeadingRadians the heading at the start of the path, in radians
     * @param endHeadingRadians the heading at the end of the path, in radians
     * @return a new NeoPath with this heading
     */
    public NeoPath facingLinear(double startHeadingRadians, double endHeadingRadians) {
        return new NeoPath(path.linear(startHeadingRadians, endHeadingRadians));
    }

    /**
     * Keeps the robot pointed at a fixed spot on the field the whole way, for example a goal or a
     * basket, while it drives around.
     *
     * @param target the pose whose position (not heading) the robot should look at
     * @return a new NeoPath with this heading
     */
    public NeoPath facingPoint(Pose target) {
        return new NeoPath(path.facingPoint(target));
    }

    /**
     * Uses any Pedro {@link Interpolator} for the heading. Reach for this when the other
     * {@code facing...} methods are not enough, for example a piecewise heading built with
     * {@code Interpolator.piecewise()}.
     *
     * @param interpolator the heading rule to use; must not be null
     * @return a new NeoPath with this heading
     */
    public NeoPath facingCustom(Interpolator interpolator) {
        return new NeoPath(path.heading(Objects.requireNonNull(interpolator, "interpolator")));
    }

    /**
     * Returns the underlying Pedro path, for when you want to pass it to Pedro directly.
     *
     * @return the Pedro {@link Path} this object wraps
     */
    public Path toPedro() {
        return path;
    }
}
