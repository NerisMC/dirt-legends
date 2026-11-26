package com.dirtlegends.entity;

/**
 * Handling numbers for a vehicle. Speeds are in blocks per tick (multiply by 72 for km/h).
 *
 * @param maxSpeed     top speed on good dirt
 * @param accel        base acceleration per tick (scaled by gear and surface)
 * @param brake        braking deceleration per tick
 * @param reverseSpeed how fast it can back up (bikes only paddle backwards)
 * @param steerLow     max steering angle in degrees when slow
 * @param steerHigh    max steering angle in degrees at top speed
 * @param wheelbase    distance between axles in blocks
 * @param leanFactor   1 = leans into turns like a bike, negative = body rolls outward like a quad
 * @param enginePitch  base pitch of the engine sound
 * @param wheelies     whether it pops wheelies off the line
 */
public record VehicleStats(float maxSpeed, float accel, float brake, float reverseSpeed,
                           float steerLow, float steerHigh, float wheelbase, float leanFactor,
                           float enginePitch, boolean wheelies) {
    public static final VehicleStats DIRT_BIKE = new VehicleStats(0.95f, 0.030f, 0.055f, 0.06f, 32.0f, 10.0f, 1.25f, 1.0f, 1.1f, true);
    public static final VehicleStats ATV = new VehicleStats(0.80f, 0.028f, 0.065f, 0.15f, 30.0f, 14.0f, 1.25f, -0.12f, 0.85f, false);
}
