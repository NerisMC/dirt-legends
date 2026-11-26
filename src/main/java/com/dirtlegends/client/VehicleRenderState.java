package com.dirtlegends.client;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/** Snapshot of a vehicle for one frame: everything the models need to animate. */
public class VehicleRenderState extends LivingEntityRenderState {
    public float steer;
    public float suspension;
    public float bodyPitch;
    public float lean;
    public float wheelSpin;
    public int variant;
}
