package dev.neopedro;

import com.pedropathing.algorithm.ForesightConfig;
import com.pedropathing.controllers.Controller;
import com.pedropathing.math.Matrix;
import com.pedropathing.math.Vector2D;

/** A minimal but valid Pedro ForesightConfig, so tests can use the real config classes. */
final class TestConfig {
    private TestConfig() {}

    static ForesightConfig create() {
        return new ForesightConfig(c -> {
            c.headingFeedback.set(Controller.pid(1, 0, 0));
            c.forwardTranslational.set(Controller.pid(1, 0, 0));
            c.strafeTranslational.set(Controller.pid(1, 0, 0));
            c.brake.set(Controller.pid(1, 0, 0));
            c.coast.set(Controller.pid(1, 0, 0));
            c.linearBrakeCoefficients.set(Matrix.identity(2));
            c.quadraticBrakeCoefficients.set(Matrix.identity(2));
            c.headingBrakeCoefficients.set(Vector2D.cartesian(1, 1));
            c.maxAchievableForwardVelocity.set(60.0);
            c.maxAchievableStrafeVelocity.set(50.0);
            c.naturalForwardDeceleration.set(40.0);
            c.naturalStrafeDeceleration.set(40.0);
        });
    }
}
