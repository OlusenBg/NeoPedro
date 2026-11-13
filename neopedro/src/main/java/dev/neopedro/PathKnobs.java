package dev.neopedro;

import com.pedropathing.algorithm.Foresight;
import com.pedropathing.algorithm.ForesightConfig;
import com.pedropathing.config.Modifier;
import com.pedropathing.follower.Follower;
import com.pedropathing.paths.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Turns {@link DriveStep}'s {@code maxPower} and {@code decelerate} settings into Pedro path
 * modifiers. A modifier changes a Pedro setting only while that path is being followed and restores
 * it afterwards, so one slow path never leaks into the next.
 */
final class PathKnobs {
    private PathKnobs() {}

    /**
     * Returns {@code path} carrying the requested limits.
     *
     * @param maxPower fraction (0, 1] of the robot's top speed, or null to leave Pedro's setting alone
     * @param decelerate fraction (0, 1] of the natural deceleration, or null to leave it alone
     * @throws IllegalStateException if a limit is requested but the follower does not use Pedro's
     *     default {@link Foresight} algorithm
     */
    static Path apply(Follower follower, Path path, Double maxPower, Double decelerate) {
        if (maxPower == null && decelerate == null) {
            return path;
        }
        ForesightConfig config = configOf(follower);
        List<Modifier> modifiers = new ArrayList<>();
        if (maxPower != null) {
            modifiers.add(config.maxPathSpeed.at(maxPower));
        }
        if (decelerate != null) {
            modifiers.add(config.maxDecelerationScale.at(decelerate));
        }
        return path.with(modifiers);
    }

    private static ForesightConfig configOf(Follower follower) {
        if (follower.algorithm() instanceof Foresight) {
            return ((Foresight) follower.algorithm()).config;
        }
        throw new IllegalStateException("maxPower()/decelerate() need Pedro's Foresight algorithm, but this "
                + "follower uses " + follower.algorithm().getClass().getName());
    }
}
