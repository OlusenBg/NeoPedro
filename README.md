# NeoPedro

A flat, readable, declarative way to write FTC autonomous on top of
[Pedro Pathing](https://github.com/Pedro-Pathing/pedropathing).

NeoPedro does **not** reimplement anything from Pedro: no curve math, no localization, no PIDF.
Every drive step hands a Pedro `Path` to Pedro's own `Follower`. NeoPedro only replaces the nested
`if`-chain you would otherwise write around it with a list of steps.

- Java 8, no reflection, no annotations, no background threads
- Everything is ticked once per OpMode loop, exactly like `follower.update()`
- One small class per file, with Javadoc that says what each class does and when to use it

## Raw Pedro vs NeoPedro

**Raw Pedro: the problem**

```java
while (opModeIsActive()) {
    follower.update();
    if (follower.atParametricEnd()) {
        if (!armMoved) { arm.up(); armMoved = true; }
        if (armMoved && !grabbed) { claw.close(); grabbed = true; ... }
        ...
    }
}
```

**NeoPedro: the same autonomous**

```java
AutoSequence sequence = new AutoSequence(
    new DriveStep(follower, pathToScore),
    new ActionStep(arm::up),
    new ActionStep(claw::close),
    new WaitStep(() -> shooter.isReadyToShoot(), 3.0),
    new DriveStep(follower, pathToPark)
);
waitForStart();
sequence.start();
while (opModeIsActive() && !sequence.isFinished()) {
    follower.update();
    sequence.update();
}
```

> **Loop order matters:** call `follower.update()` first, then `sequence.update()`. Pedro's answers
> (such as "am I at the end of the path?") are only fresh after `follower.update()`.

## Install (JitPack)

```groovy
// settings.gradle (or your root build.gradle)
repositories {
    maven { url 'https://jitpack.io' }
}

// your robot module's build.gradle
dependencies {
    implementation 'com.github.OlusenBg.NeoPedro:neopedro:TAG'
}
```

Replace `TAG` with a release tag or commit hash. Because this repository has a `:neopedro` module,
JitPack serves it as `com.github.<USER>.<REPO>:neopedro`; if in doubt, the repo's page on
jitpack.io shows the exact line. NeoPedro declares Pedro Pathing as an `api` dependency, so
`com.pedropathing:core` arrives automatically. You still add Pedro's FTC module yourself, as
the [Pedro docs](https://pedropathing.com/docs) describe.

**Pedro version:** NeoPedro `0.1.x` is built against Pedro Pathing **3.0.2**.

## What is in the box

| Class | What it does |
| --- | --- |
| `Step` | The interface everything implements: `start()`, `update()`, `isDone()` (plus optional `stop()` and `name()`). |
| `AutoSequence` | Runs steps in order. `start()`, `update()`, `isFinished()`, `currentStepName()`. Is itself a `Step`, so it nests. |
| `ActionStep` | Runs a `Runnable` once and is done. |
| `WaitStep` | Waits a fixed time, or until a condition is true, with an optional timeout. |
| `DriveStep` | Follows one path; done at the end of it. |
| `ChainStep` | Follows several paths as one continuous motion with no stop in between. |
| `Paths` / `NeoPath` | `line(...)` and `curve(...)`, with readable heading methods (see below). |
| `ParallelSteps` | Runs steps together; done when **all** are done. |
| `RaceSteps` | Runs steps together; done when **any** is done, and stops the rest. |
| `RepeatStep` | Repeats a step N times, or until a condition is true. |
| `BranchStep` | Chooses between two steps using live data, once, when it starts. |
| `HoldStep` | Holds a pose with `follower.hold(...)`; never done on its own unless you give it a timeout, condition or `finish()`. |
| `NamedStep` | Gives any step a readable name for telemetry. |

### Paths and headings

```java
NeoPath toScore = Paths.line(start, score).facingForward();             // nose along the path
NeoPath toSpike = Paths.curve(score, control, spike)                    // Bezier with a control point
                       .facingConstant(Math.toRadians(90));             // fixed heading
NeoPath turn    = Paths.line(a, b).facingLinear(0, Math.toRadians(90)); // turn while driving
NeoPath aim     = Paths.line(a, b).facingPoint(goalPose);               // keep looking at a spot
NeoPath custom  = Paths.line(a, b).facingCustom(myInterpolator);        // any Pedro Interpolator
```

With no `facing...` call, the robot turns smoothly from the first pose's heading to the last
pose's heading. Headings are in radians.

### Speed limits, timeouts and mid-path actions

`DriveStep` and `ChainStep` share the same builder-style settings:

```java
new DriveStep(follower, toScore)
    .maxPower(0.6)                  // fraction of top speed, for this path only
    .decelerate(0.8)                // gentler braking at the end
    .timeoutSeconds(4)              // give up and hold if it takes too long
    .onReachProgress(0.5, arm::up)  // run an action halfway along the path
    .onReachTime(1500, claw::open)  // ...or 1.5 s after the step starts
```

A `ChainStep` also accepts `onReachProgress(pathIndex, t, action)` to target a point inside one
specific path of the chain.

### Control flow

```java
new ParallelSteps(new DriveStep(follower, toBasket), new ActionStep(shooter::spinUp));

new RaceSteps(new DriveStep(follower, creepToWall), new WaitStep(wallSensor::isTouching));

new RepeatStep(scoreCycle, 3);                  // three times
new RepeatStep(intakeCycle, bucket::isFull);    // until the bucket is full

new BranchStep(vision::sawTag, driveLeft, driveRight);

new HoldStep(follower, scorePose).timeoutSeconds(1.5);
```

A full autonomous using every class is in [`examples/`](examples/dev/neopedro/examples/ExampleAuto.java).
It is compiled by the build and run end to end by the test suite.

## Using it in an OpMode

```java
@Autonomous(name = "My Auto")
public class MyAuto extends LinearOpMode {
    @Override
    public void runOpMode() {
        Follower follower = Constants.createFollower(hardwareMap); // your Pedro setup
        follower.setPose(startPose);

        AutoSequence sequence = new AutoSequence(
            new DriveStep(follower, Paths.line(startPose, scorePose).facingForward()),
            new ActionStep(arm::up));

        waitForStart();
        sequence.start();
        while (opModeIsActive() && !sequence.isFinished()) {
            follower.update();
            sequence.update();
            telemetry.addData("step", sequence.currentStepName());
            telemetry.update();
        }
    }
}
```

## Good to know

- **One follower, one drive at a time.** Do not run two `DriveStep`/`ChainStep`s inside a
  `ParallelSteps`; run a drive alongside actions and waits instead.
- **"Done" means the follower reached the end of the path.** That is Pedro's
  `atParametricEnd()`, the same check raw Pedro code uses. If you need the robot fully settled, follow
  the drive with `new WaitStep(() -> !follower.isBusy(), 1.0)`.
- **Losing a race stops the loser.** `RaceSteps` calls `stop()` on the steps that did not finish; for
  a drive, that holds the robot where it is.
- **`maxPower` / `decelerate` need Pedro's default `Foresight` algorithm.** They are applied as Pedro
  path modifiers (`maxPathSpeed`, `maxDecelerationScale`), so Pedro restores its normal values after
  the path. With a custom algorithm NeoPedro throws a clear error when the step starts.
- **Paths need a heading.** Paths from `Paths.line`/`Paths.curve` always have one. A raw Pedro `Path`
  you build yourself must call a heading method, or Pedro rejects it.
- **Pedro 3 note:** Pedro 3 replaced `PathChain`/`PathBuilder` with immutable paths, and a "chain" is
  now a compound path. `ChainStep` builds one with Pedro's own `Paths.path(...)`.
  Pedro 3 also has no path callback API, so `onReachProgress`/`onReachTime` are checked by NeoPedro each
  loop using Pedro's read-only `follower.pathIndex()` and `follower.parametricCompletion()`. Callbacks
  therefore fire on the first loop after the point is passed, not at the exact instant.

## Building from source

```bash
./gradlew build            # compile (Java 8), test, Javadoc, sources jar
./gradlew publishToMavenLocal
```

## License and credits

BSD 3-Clause, the same license as Pedro Pathing. See [LICENSE](LICENSE).
NeoPedro is an independent project built on top of
[Pedro Pathing](https://github.com/Pedro-Pathing/pedropathing) (BSD-3-Clause,
Copyright (c) Pedro Pathing).
