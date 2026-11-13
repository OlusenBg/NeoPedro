package dev.neopedro;

import com.pedropathing.algorithm.Algorithm;
import com.pedropathing.drivetrain.DrivePowers;
import com.pedropathing.drivetrain.Drivetrain;
import com.pedropathing.follower.Follower;
import com.pedropathing.localization.Localizer;
import com.pedropathing.localization.MotionState;
import com.pedropathing.math.Pose;
import java.util.HashMap;
import java.util.Map;

/** Builds a real Pedro Follower that drives no motors, for tests. */
final class FakeHardware {
    private FakeHardware() {}

    static Follower follower(Algorithm algorithm) {
        return new Follower(new FixedLocalizer(), new SilentDrivetrain(), algorithm);
    }

    static Follower follower(double progressPerTick) {
        return follower(new FakeAlgorithm(progressPerTick));
    }

    private static final class FixedLocalizer implements Localizer {
        private MotionState state = MotionState.zero();

        @Override
        public void setPose(Pose pose) {
            state = state.withPose(pose);
        }

        @Override
        public MotionState state() {
            return state;
        }

        @Override
        public void update() {}

        @Override
        public void reset() {}
    }

    private static final class SilentDrivetrain implements Drivetrain {
        @Override
        public void drive(DrivePowers powers, boolean manual) {}

        @Override
        public double maxScaling(DrivePowers current, DrivePowers delta) {
            return 1;
        }

        @Override
        public void stop() {}

        @Override
        public void stop(boolean brake) {}

        @Override
        public Map<String, Object> debug() {
            return new HashMap<>();
        }

        @Override
        public double interpolateVelocity(double xRadius, double yRadius, double theta) {
            return 1;
        }
    }
}
