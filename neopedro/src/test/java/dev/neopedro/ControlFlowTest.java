package dev.neopedro;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ControlFlowTest {

    // ---- ParallelSteps -------------------------------------------------

    @Test
    void parallelWaitsForAllInnerSteps() {
        RecordingStep quick = new RecordingStep("quick", 1);
        RecordingStep slow = new RecordingStep("slow", 3);
        ParallelSteps parallel = new ParallelSteps(quick, slow);

        parallel.start();
        parallel.update();
        assertFalse(parallel.isDone());
        parallel.update();
        parallel.update();
        assertTrue(parallel.isDone());
        assertEquals(1, quick.updates, "a finished step is no longer updated");
        assertEquals(3, slow.updates);
    }

    @Test
    void parallelWithNothingIsDone() {
        assertTrue(new ParallelSteps().isDone());
    }

    @Test
    void parallelStopStopsOnlyUnfinishedSteps() {
        RecordingStep quick = new RecordingStep("quick", 1);
        RecordingStep slow = new RecordingStep("slow", 9);
        ParallelSteps parallel = new ParallelSteps(quick, slow);
        parallel.start();
        parallel.update();
        parallel.stop();
        assertEquals(0, quick.stops);
        assertEquals(1, slow.stops);
    }

    // ---- RaceSteps -----------------------------------------------------

    @Test
    void raceEndsWhenFirstFinishesAndStopsTheLoser() {
        RecordingStep fast = new RecordingStep("fast", 2);
        RecordingStep slow = new RecordingStep("slow", 10);
        RaceSteps race = new RaceSteps(slow, fast);

        race.start();
        race.update();
        assertFalse(race.isDone());
        race.update();
        assertTrue(race.isDone());
        assertEquals(1, slow.stops);
        assertEquals(0, fast.stops);

        race.update();
        assertEquals(2, slow.updates, "nobody is updated after the race is decided");
    }

    @Test
    void raceWithConditionWait() {
        AtomicBoolean sensor = new AtomicBoolean();
        RecordingStep drive = new RecordingStep("drive", 100);
        RaceSteps race = new RaceSteps(drive, new WaitStep(sensor::get));
        race.start();
        race.update();
        assertFalse(race.isDone());
        sensor.set(true);
        race.update();
        assertTrue(race.isDone());
        assertEquals(1, drive.stops);
    }

    @Test
    void stoppingARaceStopsEveryone() {
        RecordingStep a = new RecordingStep("a", 10);
        RecordingStep b = new RecordingStep("b", 10);
        RaceSteps race = new RaceSteps(a, b);
        race.start();
        race.stop();
        assertEquals(1, a.stops);
        assertEquals(1, b.stops);
    }

    // ---- RepeatStep ----------------------------------------------------

    @Test
    void repeatRunsFixedNumberOfTimes() {
        AtomicInteger runs = new AtomicInteger();
        RepeatStep repeat = new RepeatStep(new ActionStep(runs::incrementAndGet), 3);

        repeat.start();
        int ticks = 0;
        while (!repeat.isDone() && ticks++ < 20) {
            repeat.update();
        }
        assertEquals(3, runs.get());
    }

    @Test
    void repeatZeroTimesDoesNothing() {
        AtomicInteger runs = new AtomicInteger();
        RepeatStep repeat = new RepeatStep(new ActionStep(runs::incrementAndGet), 0);
        repeat.start();
        assertTrue(repeat.isDone());
        assertEquals(0, runs.get());
    }

    @Test
    void repeatUntilConditionChecksBeforeEveryRun() {
        AtomicInteger runs = new AtomicInteger();
        RepeatStep repeat = new RepeatStep(new ActionStep(runs::incrementAndGet), () -> runs.get() >= 2);

        repeat.start();
        int ticks = 0;
        while (!repeat.isDone() && ticks++ < 20) {
            repeat.update();
        }
        assertEquals(2, runs.get());
    }

    @Test
    void repeatSkipsEntirelyIfConditionAlreadyTrue() {
        RecordingStep inner = new RecordingStep("inner", 1);
        RepeatStep repeat = new RepeatStep(inner, () -> true);
        repeat.start();
        assertTrue(repeat.isDone());
        assertEquals(0, inner.starts);
    }

    @Test
    void repeatRestartsLongRunningInnerStep() {
        RecordingStep inner = new RecordingStep("inner", 2);
        RepeatStep repeat = new RepeatStep(inner, 2);
        repeat.start();
        for (int i = 0; i < 4; i++) {
            repeat.update();
        }
        assertTrue(repeat.isDone());
        assertEquals(2, inner.starts);
    }

    @Test
    void repeatRejectsNegativeCount() {
        assertThrows(IllegalArgumentException.class, () -> new RepeatStep(new ActionStep(() -> {}), -1));
    }

    @Test
    void repeatStopStopsInnerStep() {
        RecordingStep inner = new RecordingStep("inner", 10);
        RepeatStep repeat = new RepeatStep(inner, 5);
        repeat.start();
        repeat.stop();
        assertEquals(1, inner.stops);
        assertTrue(repeat.isDone());
    }

    // ---- BranchStep ----------------------------------------------------

    @Test
    void branchPicksTrueSide() {
        List<String> log = new ArrayList<>();
        BranchStep branch = new BranchStep(
                () -> true, new ActionStep(() -> log.add("yes")), new ActionStep(() -> log.add("no")));
        branch.start();
        assertEquals(Arrays.asList("yes"), log);
    }

    @Test
    void branchPicksFalseSide() {
        List<String> log = new ArrayList<>();
        BranchStep branch = new BranchStep(
                () -> false, new ActionStep(() -> log.add("yes")), new ActionStep(() -> log.add("no")));
        branch.start();
        assertEquals(Arrays.asList("no"), log);
    }

    @Test
    void branchEvaluatesConditionOnlyOnceAtStart() {
        AtomicBoolean flag = new AtomicBoolean(true);
        RecordingStep yes = new RecordingStep("yes", 2);
        RecordingStep no = new RecordingStep("no", 2);
        BranchStep branch = new BranchStep(flag::get, yes, no);

        branch.start();
        flag.set(false);
        branch.update();
        branch.update();
        assertTrue(branch.isDone());
        assertEquals(2, yes.updates);
        assertEquals(0, no.starts);
    }

    @Test
    void branchWithoutElseDoesNothingWhenFalse() {
        BranchStep branch = new BranchStep(() -> false, new RecordingStep("yes", 1));
        branch.start();
        assertTrue(branch.isDone());
    }

    @Test
    void branchTreatsNullAsFalse() {
        RecordingStep yes = new RecordingStep("yes", 1);
        BranchStep branch = new BranchStep(() -> null, yes);
        branch.start();
        assertEquals(0, yes.starts);
    }

    @Test
    void branchIsNotDoneBeforeStartAndRejectsEarlyUpdate() {
        BranchStep branch = new BranchStep(() -> true, new RecordingStep("yes", 1));
        assertFalse(branch.isDone());
        assertThrows(IllegalStateException.class, branch::update);
    }
}
