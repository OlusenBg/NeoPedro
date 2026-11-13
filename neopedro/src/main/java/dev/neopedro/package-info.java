/**
 * NeoPedro: a flat, declarative list of steps for FTC autonomous, built on top of Pedro Pathing.
 *
 * <p>Build your autonomous as an {@link dev.neopedro.AutoSequence} of {@link dev.neopedro.Step}s,
 * then tick it once per OpMode loop, right after {@code follower.update()}. Everything runs
 * synchronously on the OpMode thread: no reflection, no background threads, no hidden state.
 *
 * <p>NeoPedro never does path math, localization or control itself. Every drive step simply hands
 * a Pedro {@code Path} to Pedro's {@code Follower}.
 */
package dev.neopedro;
