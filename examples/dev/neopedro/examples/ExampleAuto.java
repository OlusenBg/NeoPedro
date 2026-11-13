package dev.neopedro.examples;

import com.pedropathing.follower.Follower;
import com.pedropathing.math.Pose;
import dev.neopedro.ActionStep;
import dev.neopedro.AutoSequence;
import dev.neopedro.BranchStep;
import dev.neopedro.ChainStep;
import dev.neopedro.DriveStep;
import dev.neopedro.HoldStep;
import dev.neopedro.NamedStep;
import dev.neopedro.ParallelSteps;
import dev.neopedro.Paths;
import dev.neopedro.RaceSteps;
import dev.neopedro.RepeatStep;
import dev.neopedro.Step;
import dev.neopedro.WaitStep;
import java.util.function.BooleanSupplier;

/**
 * A complete autonomous using every NeoPedro feature. Not part of the library.
 *
 * <p>On a real robot the {@link #run} loop below is the body of your {@code LinearOpMode}'s
 * {@code runOpMode()}: create the follower, call {@code follower.setPose(START)}, build the sequence with
 * {@link #build}, call {@code waitForStart()}, then call {@link #run}. See the README for the full OpMode.
 */
public final class ExampleAuto {
    // Field positions in inches; headings in radians (Pedro's convention).
    public static final Pose START = new Pose(8, 60, Math.toRadians(0));
    public static final Pose SCORE = new Pose(40, 70, Math.toRadians(45));
    static final Pose PILE_CONTROL = new Pose(30, 30, 0);
    static final Pose PILE = new Pose(50, 20, Math.toRadians(-90));
    static final Pose WALL = new Pose(50, 8, Math.toRadians(-90));
    static final Pose PARK_LEFT = new Pose(10, 70, Math.toRadians(180));
    static final Pose PARK_RIGHT = new Pose(10, 20, Math.toRadians(180));

    /** Everything the autonomous needs from your robot, so the example runs without hardware. */
    public interface Robot {
        void closeClaw();

        void openClaw();

        void armUp();

        void spinUpShooter();

        boolean shooterReady();

        void intakeOn();

        void intakeOff();

        boolean touchingWall();

        boolean sawTag();
    }

    private ExampleAuto() {}

    /** Lists the whole autonomous as a flat sequence of steps. */
    public static AutoSequence build(Follower follower, Robot robot) {
        return new AutoSequence(
                new ActionStep(robot::closeClaw),
                driveToScore(follower, robot),
                new NamedStep("release", new ActionStep(robot::openClaw)),
                new NamedStep("wait for shooter", new WaitStep(robot::shooterReady, 3.0)),
                new RepeatStep(new NamedStep("pile cycle", pileCycle(follower, robot)), 2),
                new RaceSteps(
                        new DriveStep(follower, Paths.line(PILE, WALL).facingConstant(Math.toRadians(-90))),
                        new WaitStep(robot::touchingWall)),
                new BranchStep(robot::sawTag, park(follower, PARK_LEFT), park(follower, PARK_RIGHT)),
                new HoldStep(follower, PARK_LEFT).timeoutSeconds(0.3));
    }

    /** Runs the sequence the way every NeoPedro autonomous runs: follower first, then the sequence. */
    public static void run(Follower follower, AutoSequence sequence, BooleanSupplier opModeIsActive) {
        sequence.start();
        while (opModeIsActive.getAsBoolean() && !sequence.isFinished()) {
            follower.update();
            sequence.update();
        }
    }

    /** One path, slowed down, with the arm lifting halfway there while the shooter spins up. */
    private static Step driveToScore(Follower follower, Robot robot) {
        return new ParallelSteps(
                new DriveStep(follower, Paths.line(START, SCORE).facingLinear(0, Math.toRadians(45)))
                        .maxPower(0.7)
                        .decelerate(0.8)
                        .timeoutSeconds(5)
                        .onReachProgress(0.5, robot::armUp)
                        .onReachTime(2000, robot::armUp),
                new ActionStep(robot::spinUpShooter));
    }

    /** Out to the pile and back as one smooth motion, intaking on the way. */
    private static Step pileCycle(Follower follower, Robot robot) {
        return new AutoSequence(
                new ChainStep(
                                follower,
                                Paths.curve(SCORE, PILE_CONTROL, PILE).facingForward(),
                                Paths.line(PILE, SCORE).facingBackward())
                        .maxPower(0.8)
                        .onReachProgress(0, 0.8, robot::intakeOn)
                        .onReachProgress(1, 0.5, robot::intakeOff),
                new ActionStep(robot::openClaw),
                new WaitStep(0.05));
    }

    private static Step park(Follower follower, Pose spot) {
        return new DriveStep(follower, Paths.line(WALL, spot).facingForward()).timeoutSeconds(4);
    }
}
