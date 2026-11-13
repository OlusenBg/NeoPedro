package dev.neopedro;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.pedropathing.follower.Follower;
import dev.neopedro.examples.ExampleAuto;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Runs the whole example autonomous, every phase of the library, against a real Pedro Follower. */
class ExampleAutoTest {

    private static final class StubRobot implements ExampleAuto.Robot {
        final List<String> log = new ArrayList<>();
        private int wallPolls;
        private final boolean tag;

        StubRobot(boolean tag) {
            this.tag = tag;
        }

        public void closeClaw() { log.add("closeClaw"); }
        public void openClaw() { log.add("openClaw"); }
        public void armUp() { log.add("armUp"); }
        public void spinUpShooter() { log.add("spinUp"); }
        public boolean shooterReady() { return true; }
        public void intakeOn() { log.add("intakeOn"); }
        public void intakeOff() { log.add("intakeOff"); }
        public boolean touchingWall() { return ++wallPolls >= 2; }
        public boolean sawTag() { return tag; }

        long count(String entry) {
            return log.stream().filter(entry::equals).count();
        }
    }

    private static StubRobot runAuto(boolean tag) {
        Follower follower = FakeHardware.follower(new FakeForesight(0.2));
        follower.setPose(ExampleAuto.START);
        StubRobot robot = new StubRobot(tag);
        AutoSequence sequence = ExampleAuto.build(follower, robot);

        // Waits use the real clock, so give the simulated loop a wall-clock limit rather than a tick limit.
        long deadline = System.nanoTime() + 10_000_000_000L;
        ExampleAuto.run(follower, sequence, () -> System.nanoTime() < deadline);

        assertTrue(sequence.isFinished(), "autonomous should finish; stuck at: " + sequence.currentStepName());
        return robot;
    }

    @Test
    void runsEveryStageInOrder() {
        StubRobot robot = runAuto(true);

        assertEquals("closeClaw", robot.log.get(0));
        assertEquals(1, robot.count("armUp"), "mid-path callbacks fire exactly once");
        assertEquals(1, robot.count("spinUp"));
        assertEquals(2, robot.count("intakeOn"), "RepeatStep ran the cycle twice");
        assertEquals(2, robot.count("intakeOff"));
        assertEquals(3, robot.count("openClaw"), "once to release, once per cycle");
    }

    @Test
    void bothBranchesComplete() {
        runAuto(true);
        runAuto(false);
    }
}
