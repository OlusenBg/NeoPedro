package dev.neopedro;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.pedropathing.algorithm.Foresight;
import com.pedropathing.algorithm.ForesightConfig;
import com.pedropathing.follower.Follower;
import com.pedropathing.math.Pose;
import org.junit.jupiter.api.Test;

/** Runs maxPower/decelerate against Pedro's real Foresight config and PathTracker. */
class ForesightKnobsTest {
    private static final Pose A = new Pose(0, 0, 0);
    private static final Pose B = new Pose(24, 0, 0);

    @Test
    void limitsApplyWhileDrivingAndAreRestoredAfterwards() {
        ForesightConfig config = TestConfig.create();
        Follower follower = FakeHardware.follower(new Foresight(config));
        DriveStep step = new DriveStep(follower, Paths.line(A, B)).maxPower(0.5).decelerate(0.4);

        assertEquals(ForesightConfig.Constraint.NONE, config.maxPathSpeed.get());

        step.start();
        assertEquals(0.5, config.maxPathSpeed.get());
        assertEquals(0.4, config.maxDecelerationScale.get());

        follower.stop();
        assertEquals(ForesightConfig.Constraint.NONE, config.maxPathSpeed.get());
        assertEquals(ForesightConfig.Constraint.NONE, config.maxDecelerationScale.get());
    }

    @Test
    void chainAppliesLimitsToTheWholeChain() {
        ForesightConfig config = TestConfig.create();
        Follower follower = FakeHardware.follower(new Foresight(config));
        ChainStep step = new ChainStep(follower, Paths.line(A, B), Paths.line(B, A)).maxPower(0.7);

        step.start();
        assertEquals(0.7, config.maxPathSpeed.get());
    }
}
