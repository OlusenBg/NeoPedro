package dev.neopedro;

import com.pedropathing.algorithm.Algorithm;
import com.pedropathing.drivetrain.DrivePowers;
import com.pedropathing.drivetrain.Drivetrain;
import com.pedropathing.localization.MotionState;
import com.pedropathing.math.Pose;
import com.pedropathing.math.Vector2D;
import com.pedropathing.paths.PathTracker;
import java.util.HashMap;
import java.util.Map;

/**
 * Stands in for Pedro's Foresight: each tick moves {@code progressPerTick} along the current segment.
 * At the end of a segment it advances the real PathTracker and resets, exactly as Foresight does,
 * including the one-tick moment where atParametricEnd() reads false.
 */
class FakeAlgorithm implements Algorithm {
    private final double progressPerTick;
    private double closestT;

    FakeAlgorithm(double progressPerTick) {
        this.progressPerTick = progressPerTick;
    }

    @Override
    public DrivePowers calculatePath(Drivetrain drivetrain, PathTracker tracker, MotionState state, double dt) {
        closestT += progressPerTick;
        if (closestT >= 1.0) {
            tracker.advance();
            closestT = 0.0;
        }
        return DrivePowers.zero();
    }

    @Override
    public DrivePowers calculateHold(Drivetrain d, Pose target, MotionState s, boolean scaling, double dt) {
        return DrivePowers.zero();
    }

    @Override
    public boolean atParametricEnd() {
        return closestT >= 0.975;
    }

    @Override
    public double parametricCompletion() {
        return closestT;
    }

    @Override
    public void reset() {
        closestT = 0.0;
    }

    @Override
    public double completion() {
        return closestT;
    }

    @Override
    public Pose closestPose() {
        return Pose.zero();
    }

    @Override
    public Vector2D closestTangent() {
        return null;
    }

    @Override
    public Vector2D closestNormal() {
        return null;
    }

    @Override
    public double curvature() {
        return 0;
    }

    @Override
    public double remainingDistance() {
        return 0;
    }

    @Override
    public boolean isBusy() {
        return true;
    }

    @Override
    public Map<String, Object> debug() {
        return new HashMap<>();
    }
}
