package com.neoncoast.vehicle;

/**
 * Handling numbers for each kind of car. Speeds are in blocks per tick (x72 = km/h).
 *
 * @param maxSpeed    top speed on a road
 * @param accel       base acceleration per tick (bigger in low gears)
 * @param brake       braking deceleration per tick
 * @param reverse     top speed in reverse
 * @param steerLow    max wheel angle (degrees) when slow
 * @param steerHigh   max wheel angle (degrees) at top speed
 * @param wheelbase   axle-to-axle length in blocks
 * @param grip        tyre grip multiplier (sports cars stick better)
 * @param maxHealth   damage it can take before it catches fire and blows up
 * @param enginePitch base pitch of the engine sound
 * @param chopValue   what the chop shop pays for it
 */
public record CarType(String name, float maxSpeed, float accel, float brake, float reverse,
                      float steerLow, float steerHigh, float wheelbase, float grip,
                      float maxHealth, float enginePitch, int chopValue) {
    public static final CarType SEDAN = new CarType("sedan", 1.25f, 0.024f, 0.045f, 0.25f, 34.0f, 7.0f, 2.6f, 1.0f, 100.0f, 0.75f, 400);
    public static final CarType SPORTS = new CarType("sports", 1.65f, 0.036f, 0.055f, 0.25f, 32.0f, 6.0f, 2.5f, 1.08f, 80.0f, 0.95f, 1200);
    public static final CarType POLICE = new CarType("police", 1.5f, 0.032f, 0.055f, 0.25f, 34.0f, 7.0f, 2.6f, 1.05f, 140.0f, 0.8f, 0);
}
