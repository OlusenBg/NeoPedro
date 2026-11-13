package dev.neopedro;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class WaitStepTest {

    @Test
    void fixedDurationWaitsThenFinishes() throws InterruptedException {
        WaitStep wait = new WaitStep(0.05);
        wait.start();
        wait.update();
        assertFalse(wait.isDone());

        Thread.sleep(80);
        wait.update();
        assertTrue(wait.isDone());
    }

    @Test
    void conditionEndsTheWait() {
        AtomicBoolean ready = new AtomicBoolean();
        WaitStep wait = new WaitStep(ready::get);
        wait.start();
        wait.update();
        assertFalse(wait.isDone());

        ready.set(true);
        wait.update();
        assertTrue(wait.isDone());
    }

    @Test
    void timeoutPreventsHangingForever() throws InterruptedException {
        WaitStep wait = new WaitStep(() -> false, 0.05);
        wait.start();
        wait.update();
        assertFalse(wait.isDone());

        Thread.sleep(80);
        wait.update();
        assertTrue(wait.isDone());
    }

    @Test
    void startResetsTheClock() throws InterruptedException {
        WaitStep wait = new WaitStep(0.05);
        wait.start();
        Thread.sleep(80);
        wait.update();
        assertTrue(wait.isDone());

        wait.start();
        wait.update();
        assertFalse(wait.isDone());
    }

    @Test
    void rejectsNegativeDuration() {
        assertThrows(IllegalArgumentException.class, () -> new WaitStep(-1));
    }

    @Test
    void staysDoneEvenIfTheConditionFlickersBackToFalse() {
        AtomicBoolean sensor = new AtomicBoolean(true);
        WaitStep wait = new WaitStep(sensor::get);
        wait.start();
        wait.update();
        assertTrue(wait.isDone());

        sensor.set(false);
        wait.update();
        assertTrue(wait.isDone());
    }
}
