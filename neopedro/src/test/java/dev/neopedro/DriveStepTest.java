package dev.neopedro;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.pedropathing.follower.Follower;
import com.pedropathing.math.Pose;
import com.pedropathing.paths.Path;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class DriveStepTest {
    private static final Pose A = new Pose(0, 0, 0);
    private static final Pose B = new Pose(24, 0, 0);

    /** Mirrors the OpMode loop: follower first, then the step. */
    private static void tick(Follower follower, Step step) {
        follower.update(0.02);
        step.update();
    }

    @Test
    void startsFollowingAndFinishesAtTheEnd() {
        Follower follower = FakeHardware.follower(0.25);
        DriveStep step = new DriveStep(follower, Paths.line(A, B));

        step.start();
        assertTrue(follower.following());
        assertFalse(step.isDone(), "not done before the first tick");

        int ticks = 0;
        while (!step.isDone() && ticks < 50) {
            tick(follower, step);
            ticks++;
        }
        assertTrue(step.isDone());
        assertTrue(ticks > 1, "must take real ticks, not finish instantly");
    }

    @Test
    void secondStepIsNotFooledByStaleStateFromTheFirst() {
        Follower follower = FakeHardware.follower(0.5);
        DriveStep first = new DriveStep(follower, Paths.line(A, B));
        DriveStep second = new DriveStep(follower, Paths.line(B, A));
        AutoSequence sequence = new AutoSequence(first, new NamedStep("second", second));

        sequence.start();
        int ticks = 0;
        while (!"second".equals(sequence.currentStepName()) && ticks++ < 50) {
            follower.update(0.02);
            sequence.update();
        }
        // first just finished and second just started in the same tick
        assertTrue(first.isDone(), "a finished step stays finished even after the follower moves on");
        assertFalse(second.isDone());
        assertTrue(follower.following());

        follower.update(0.02);
        sequence.update();
        assertFalse(second.isDone(), "second needs real progress, not first's leftovers");
    }

    @Test
    void progressCallbackFiresOnceAtTheRightPoint() {
        Follower follower = FakeHardware.follower(0.25);
        AtomicInteger fired = new AtomicInteger();
        DriveStep step = new DriveStep(follower, Paths.line(A, B)).onReachProgress(0.5, fired::incrementAndGet);

        step.start();
        tick(follower, step); // 0.25
        assertEquals(0, fired.get());
        tick(follower, step); // 0.50
        assertEquals(1, fired.get());
        while (!step.isDone()) {
            tick(follower, step);
        }
        assertEquals(1, fired.get(), "fires once only");
    }

    @Test
    void progressCallbackAtTheEndStillFires() {
        Follower follower = FakeHardware.follower(0.25);
        AtomicInteger fired = new AtomicInteger();
        DriveStep step = new DriveStep(follower, Paths.line(A, B)).onReachProgress(1.0, fired::incrementAndGet);

        step.start();
        for (int i = 0; i < 10; i++) {
            tick(follower, step);
        }
        assertEquals(1, fired.get());
    }

    @Test
    void timeCallbackFires() throws InterruptedException {
        Follower follower = FakeHardware.follower(0.0001);
        AtomicInteger fired = new AtomicInteger();
        DriveStep step = new DriveStep(follower, Paths.line(A, B)).onReachTime(40, fired::incrementAndGet);

        step.start();
        tick(follower, step);
        assertEquals(0, fired.get());
        Thread.sleep(70);
        tick(follower, step);
        assertEquals(1, fired.get());
    }

    @Test
    void timeoutHoldsTheRobotAndEndsTheStep() throws InterruptedException {
        Follower follower = FakeHardware.follower(0.0001);
        AtomicInteger fired = new AtomicInteger();
        DriveStep step = new DriveStep(follower, Paths.line(A, B))
                .timeoutSeconds(0.04)
                .onReachProgress(0.9, fired::incrementAndGet);

        step.start();
        tick(follower, step);
        assertFalse(step.isDone());

        Thread.sleep(70);
        tick(follower, step);
        assertTrue(step.isDone());
        assertTrue(follower.holding());
        tick(follower, step);
        assertEquals(0, fired.get(), "a timed-out drive must not fire its callbacks");
    }

    @Test
    void stopHoldsTheRobot() {
        Follower follower = FakeHardware.follower(0.1);
        DriveStep step = new DriveStep(follower, Paths.line(A, B));
        step.start();
        tick(follower, step);
        step.stop();
        assertTrue(follower.holding());
    }

    @Test
    void canBeRestarted() {
        Follower follower = FakeHardware.follower(0.5);
        DriveStep step = new DriveStep(follower, Paths.line(A, B));
        for (int run = 0; run < 2; run++) {
            step.start();
            assertFalse(step.isDone());
            int ticks = 0;
            while (!step.isDone() && ticks++ < 50) {
                tick(follower, step);
            }
            assertTrue(step.isDone());
        }
    }

    @Test
    void pathWithoutHeadingIsRejectedByPedroOnStart() {
        Path noHeading = com.pedropathing.api.Paths.line(A, B);
        DriveStep step = new DriveStep(FakeHardware.follower(0.5), noHeading);
        assertThrows(IllegalStateException.class, step::start);
    }

    @Test
    void validatesArguments() {
        DriveStep step = new DriveStep(FakeHardware.follower(0.5), Paths.line(A, B));
        assertThrows(IllegalArgumentException.class, () -> step.maxPower(0));
        assertThrows(IllegalArgumentException.class, () -> step.maxPower(1.5));
        assertThrows(IllegalArgumentException.class, () -> step.decelerate(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> step.timeoutSeconds(-1));
        assertThrows(IllegalArgumentException.class, () -> step.onReachProgress(1.1, () -> {}));
        assertThrows(IllegalArgumentException.class, () -> step.onReachProgress(-1, 0.5, () -> {}));
        assertThrows(IllegalArgumentException.class, () -> step.onReachTime(-5, () -> {}));
    }

    @Test
    void speedLimitsNeedTheForesightAlgorithm() {
        DriveStep step = new DriveStep(FakeHardware.follower(0.5), Paths.line(A, B)).maxPower(0.5);
        IllegalStateException error = assertThrows(IllegalStateException.class, step::start);
        assertTrue(error.getMessage().contains("Foresight"));
    }
}
