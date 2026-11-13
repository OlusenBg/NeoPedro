package dev.neopedro;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.pedropathing.math.Pose;
import com.pedropathing.paths.Path;
import com.pedropathing.paths.interpolator.Interpolator;
import org.junit.jupiter.api.Test;

class PathsTest {
    private static final double EPS = 1e-6;
    private static final Pose START = new Pose(0, 0, 0);
    private static final Pose END = new Pose(24, 0, Math.PI / 2);

    @Test
    void defaultHeadingTurnsFromStartPoseToEndPose() {
        Path path = Paths.line(START, END).toPedro();
        assertEquals(0.0, path.heading(0.0), EPS);
        assertEquals(Math.PI / 4, path.heading(0.5), EPS);
        assertEquals(Math.PI / 2, path.heading(1.0), EPS);
    }

    @Test
    void facingForwardFollowsTheTangent() {
        Path path = Paths.line(START, new Pose(0, 24, 0)).facingForward().toPedro();
        assertEquals(Math.PI / 2, path.heading(0.5), EPS);
    }

    @Test
    void facingBackwardIsOppositeTheTangent() {
        Path path = Paths.line(START, END).facingBackward().toPedro();
        assertEquals(Math.PI, Math.abs(path.heading(0.5)), EPS);
    }

    @Test
    void facingConstantNeverChanges() {
        Path path = Paths.line(START, END).facingConstant(1.0).toPedro();
        assertEquals(1.0, path.heading(0.0), EPS);
        assertEquals(1.0, path.heading(1.0), EPS);
    }

    @Test
    void facingLinearInterpolatesBetweenGivenHeadings() {
        Path path = Paths.line(START, END).facingLinear(0.0, 1.0).toPedro();
        assertEquals(0.5, path.heading(0.5), EPS);
    }

    @Test
    void facingPointLooksAtTheTarget() {
        Path path = Paths.line(START, new Pose(24, 0, 0)).facingPoint(new Pose(12, 12, 0)).toPedro();
        assertEquals(Math.PI / 2, path.heading(0.5), EPS);
    }

    @Test
    void facingCustomUsesTheGivenInterpolator() {
        Path path = Paths.line(START, END).facingCustom(Interpolator.constant(0.25)).toPedro();
        assertEquals(0.25, path.heading(0.7), EPS);
    }

    @Test
    void headingMethodsReturnNewObjects() {
        NeoPath original = Paths.line(START, END);
        NeoPath changed = original.facingConstant(1.0);
        assertNotSame(original, changed);
        assertEquals(Math.PI / 4, original.toPedro().heading(0.5), EPS, "original is unchanged");
    }

    @Test
    void curveStartsAndEndsWhereItShould() {
        Path path = Paths.curve(START, new Pose(12, 20, 0), END).toPedro();
        assertEquals(0.0, path.get(0.0).x(), EPS);
        assertEquals(24.0, path.get(1.0).x(), EPS);
        assertEquals(Math.PI / 2, path.heading(1.0), EPS);
    }

    @Test
    void curveNeedsTwoPoses() {
        assertThrows(IllegalArgumentException.class, () -> Paths.curve(START));
    }
}
