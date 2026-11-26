package com.dirtlegends.client;

import com.dirtlegends.entity.MotoVehicleEntity;
import com.dirtlegends.registry.ModSounds;
import net.minecraft.client.sound.MovingSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.MathHelper;

/** Looping engine note that follows the vehicle; pitch follows RPM so you hear every gear change. */
public class EngineSoundInstance extends MovingSoundInstance {
    private final MotoVehicleEntity vehicle;

    public EngineSoundInstance(MotoVehicleEntity vehicle) {
        super(ModSounds.ENGINE, SoundCategory.NEUTRAL, SoundInstance.createRandom());
        this.vehicle = vehicle;
        this.repeat = true;
        this.repeatDelay = 0;
        this.volume = 0.3f;
        this.pitch = 0.6f;
        this.x = vehicle.getX();
        this.y = vehicle.getY();
        this.z = vehicle.getZ();
    }

    @Override
    public boolean shouldAlwaysPlay() {
        return true;
    }

    @Override
    public boolean canPlay() {
        return !this.vehicle.isSilent();
    }

    @Override
    public void tick() {
        if (this.vehicle.isRemoved() || !this.vehicle.hasPassengers()) {
            this.vehicle.setEngineRunning(false);
            this.setDone();
            return;
        }
        this.x = this.vehicle.getX();
        this.y = this.vehicle.getY();
        this.z = this.vehicle.getZ();
        float rpm = this.vehicle.getRpm();
        this.volume = 0.3f + 0.55f * rpm;
        this.pitch = MathHelper.clamp(this.vehicle.getStats().enginePitch() * (0.55f + 1.15f * rpm), 0.5f, 2.0f);
    }
}
