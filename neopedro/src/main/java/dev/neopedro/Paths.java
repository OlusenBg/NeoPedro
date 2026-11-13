package dev.neopedro;

import com.pedropathing.math.Pose;

/**
 * Starting points for building a {@link NeoPath}: straight lines and Bezier curves.
 *
 * <p>This class is a thin wrapper over Pedro's own {@code com.pedropathing.api.Paths}; the curve
 * math is entirely Pedro's. If you import both, refer to one of them by its full name.
 *
 * <p>Unless you call a {@code facing...} method, the robot turns smoothly from the first pose's
 * heading to the last pose's heading, which is what most autonomous paths want.
 *
 * <pre>{@code
 * NeoPath line  = Paths.line(startPose, scorePose);
 * NeoPath curve = Paths.curve(scorePose, controlPose, parkPose).facingForward();
 * }</pre>
 */
public final class Paths {
    private Paths() {}

    /**
     * Creates a straight line between two poses.
     *
     * @param start where the line begins
     * @param end where the line ends
     * @return a path that turns from {@code start}'s heading to {@code end}'s heading; change it with
     *     the {@code facing...} methods
     */
    public static NeoPath line(Pose start, Pose end) {
        return withPoseHeadings(com.pedropathing.api.Paths.line(start, end), start, end);
    }

    /**
     * Creates a Bezier curve. The first and last poses are the start and end; any poses in between
     * are control points that bend the curve towards them without the robot passing through them.
     *
     * @param poses at least two poses: start, optional control points, end
     * @return a path that turns from the first pose's heading to the last pose's heading; change it
     *     with the {@code facing...} methods
     * @throws IllegalArgumentException if fewer than two poses are given
     */
    public static NeoPath curve(Pose... poses) {
        if (poses.length < 2) {
            throw new IllegalArgumentException("A curve needs at least 2 poses but got " + poses.length);
        }
        Pose start = poses[0];
        Pose end = poses[poses.length - 1];
        return withPoseHeadings(com.pedropathing.api.Paths.curve(poses), start, end);
    }

    private static NeoPath withPoseHeadings(com.pedropathing.paths.Path path, Pose start, Pose end) {
        return new NeoPath(path.linear(start, end));
    }
}
