package dev.neopedro;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class AutoSequenceTest {

    @Test
    void runsStepsInOrder() {
        List<String> log = new ArrayList<>();
        AutoSequence sequence = new AutoSequence(
                new ActionStep(() -> log.add("a")),
                new ActionStep(() -> log.add("b")),
                new ActionStep(() -> log.add("c")));

        sequence.start();
        while (!sequence.isFinished()) {
            sequence.update();
        }

        assertEquals(Arrays.asList("a", "b", "c"), log);
    }

    @Test
    void newStepIsNotCheckedInTheTickItStartedIn() {
        RecordingStep first = new RecordingStep("first", 1);
        RecordingStep second = new RecordingStep("second", 1);
        AutoSequence sequence = new AutoSequence(first, second);

        sequence.start();
        sequence.update(); // first finishes, second starts
        assertEquals(1, second.starts);
        assertEquals(0, second.updates);
        assertEquals("second", sequence.currentStepName());

        sequence.update(); // second gets its first update
        assertTrue(sequence.isFinished());
    }

    @Test
    void reportsNameBeforeAndAfter() {
        AutoSequence sequence = new AutoSequence(new NamedStep("only", new ActionStep(() -> {})));
        assertEquals("not started", sequence.currentStepName());
        sequence.start();
        assertEquals("only", sequence.currentStepName());
        sequence.update();
        assertEquals("finished", sequence.currentStepName());
    }

    @Test
    void emptySequenceFinishesOnStart() {
        AutoSequence sequence = new AutoSequence();
        assertFalse(sequence.isFinished());
        sequence.start();
        assertTrue(sequence.isFinished());
    }

    @Test
    void updateBeforeStartIsAnError() {
        assertThrows(IllegalStateException.class, () -> new AutoSequence(new RecordingStep("x", 1)).update());
    }

    @Test
    void stopAbandonsCurrentStep() {
        RecordingStep running = new RecordingStep("running", 100);
        AutoSequence sequence = new AutoSequence(running, new RecordingStep("never", 1));
        sequence.start();
        sequence.update();
        sequence.stop();
        assertEquals(1, running.stops);
        assertTrue(sequence.isFinished());
    }

    @Test
    void canBeRestarted() {
        RecordingStep step = new RecordingStep("s", 1);
        AutoSequence sequence = new AutoSequence(step);
        sequence.start();
        sequence.update();
        sequence.start();
        assertFalse(sequence.isFinished());
        assertEquals(2, step.starts);
    }
}
