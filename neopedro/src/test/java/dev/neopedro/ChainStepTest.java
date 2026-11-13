package dev.neopedro;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.pedropathing.follower.Follower;
import com.pedropathing.math.Pose;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ChainStepTest {
    private static final Pose A = new Pose(0, 0, 0);
    private static final Pose B = new Pose(24, 0, 0);
    private static final Pose C = new Pose(24, 24, 0);

    private static void tick(Follower follower, Step step) {
        follower.update(0.02);
        step.update();
    }

    @Test
    void drivesEveryPathBeforeFinishing() {
        Follower follower = FakeHardware.follower(0.5);
        ChainStep step = new ChainStep(follower, Paths.line(A, B), Paths.line(B, C));

        step.start();
        List<Integer> seenIndexes = new ArrayList<>();
        int ticks = 0;
        while (!step.isDone() && ticks++ < 50) {
            tick(follower, step);
            if (follower.following()) {
                seenIndexes.add(follower.pathIndex());
            }
        }
        assertTrue(step.isDone());
        assertTrue(seenIndexes.contains(0) && seenIndexes.contains(1), "visited both pieces: " + seenIndexes);
    }

    @Test
    void overallProgressCountsEachPieceEqually() {
        Follower follower = FakeHardware.follower(0.25);
        List<String> log = new ArrayList<>();
        ChainStep step = new ChainStep(follower, Paths.line(A, B), Paths.line(B, C))
                .onReachProgress(0.5, () -> log.add("half"))
                .onReachProgress(1.0, () -> log.add("end"));

        step.start();
        int ticks = 0;
        while (!step.isDone() && ticks++ < 50) {
            tick(follower, step);
        }
        assertEquals(2, log.size());
        assertEquals("half", log.get(0));
        assertEquals("end", log.get(1));
    }

    @Test
    void indexedProgressTargetsOnePiece() {
        Follower follower = FakeHardware.follower(0.25);
        List<Integer> indexAtFire = new ArrayList<>();
        ChainStep step = new ChainStep(follower, Paths.line(A, B), Paths.line(B, C))
                .onReachProgress(1, 0.5, () -> indexAtFire.add(follower.pathIndex()));

        step.start();
        int ticks = 0;
        while (!step.isDone() && ticks++ < 50) {
            tick(follower, step);
        }
        assertEquals(1, indexAtFire.size());
        assertEquals(1, indexAtFire.get(0).intValue());
    }

    @Test
    void acceptsRawPedroPaths() {
        Follower follower = FakeHardware.follower(0.5);
        ChainStep step = new ChainStep(follower, Paths.line(A, B).toPedro(), Paths.line(B, C).toPedro());
        step.start();
        assertTrue(follower.following());
    }

    @Test
    void stopHolds() {
        Follower follower = FakeHardware.follower(0.1);
        ChainStep step = new ChainStep(follower, Paths.line(A, B), Paths.line(B, C));
        step.start();
        step.stop();
        assertTrue(follower.holding());
    }

    @Test
    void notDoneBeforeFirstTick() {
        ChainStep step = new ChainStep(FakeHardware.follower(0.5), Paths.line(A, B), Paths.line(B, C));
        step.start();
        assertFalse(step.isDone());
    }

    @Test
    void emptyChainIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new ChainStep(FakeHardware.follower(0.5), new NeoPath[0]));
    }
}
