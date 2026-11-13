package dev.neopedro;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.pedropathing.follower.Follower;
import com.pedropathing.math.Pose;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class HoldStepTest {
    private static final Pose TARGET = new Pose(10, 10, 0);

    @Test
    void holdsThePoseAndNeverFinishesByDefault() {
        Follower follower = FakeHardware.follower(0.5);
        HoldStep hold = new HoldStep(follower, TARGET);

        hold.start();
        assertTrue(follower.holding());
        for (int i = 0; i < 100; i++) {
            hold.update();
        }
        assertFalse(hold.isDone());
    }

    @Test
    void holdsCurrentPoseWhenNoneGiven() {
        Follower follower = FakeHardware.follower(0.5);
        follower.setPose(new Pose(3, 4, 0));
        new HoldStep(follower).start();
        assertTrue(follower.holding());
    }

    @Test
    void timeoutEndsTheHold() throws InterruptedException {
        HoldStep hold = new HoldStep(FakeHardware.follower(0.5), TARGET).timeoutSeconds(0.04);
        hold.start();
        hold.update();
        assertFalse(hold.isDone());
        Thread.sleep(70);
        hold.update();
        assertTrue(hold.isDone());
    }

    @Test
    void conditionEndsTheHold() {
        AtomicBoolean empty = new AtomicBoolean();
        HoldStep hold = new HoldStep(FakeHardware.follower(0.5), TARGET).until(empty::get);
        hold.start();
        hold.update();
        assertFalse(hold.isDone());
        empty.set(true);
        hold.update();
        assertTrue(hold.isDone());
    }

    @Test
    void finishEndsTheHold() {
        HoldStep hold = new HoldStep(FakeHardware.follower(0.5), TARGET);
        hold.start();
        hold.finish();
        assertTrue(hold.isDone());
    }

    @Test
    void startRearmsAFinishedHold() {
        HoldStep hold = new HoldStep(FakeHardware.follower(0.5), TARGET);
        hold.start();
        hold.finish();
        hold.start();
        assertFalse(hold.isDone());
    }

    @Test
    void namedStepReportsItsLabel() {
        assertEquals("pose lock", new NamedStep("pose lock", new HoldStep(FakeHardware.follower(0.5))).name());
    }
}
