package dev.neopedro;

import com.pedropathing.algorithm.Foresight;
import com.pedropathing.drivetrain.DrivePowers;
import com.pedropathing.drivetrain.Drivetrain;
import com.pedropathing.localization.MotionState;
import com.pedropathing.math.Pose;
import com.pedropathing.paths.PathTracker;

/**
 * A real Foresight (so maxPower/decelerate find its config) whose driving is replaced by a simple
 * "move a fixed fraction per tick" model, with the same end-of-segment behaviour as the real thing.
 */
class FakeForesight extends Foresight {
    private final double progressPerTick;
    private double closestT;

    FakeForesight(double progressPerTick) {
        super(TestConfig.create());
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
    public double completion() {
        return closestT;
    }

    @Override
    public void reset() {
        closestT = 0.0;
    }
}
