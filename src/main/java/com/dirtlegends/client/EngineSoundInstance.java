package com.dirtlegends.client;

import com.dirtlegends.entity.MotoVehicleEntity;
import com.dirtlegends.registry.ModSounds;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;

/** Looping engine note that follows the vehicle; pitch follows RPM so you hear every gear change. */
public class EngineSoundInstance extends AbstractTickableSoundInstance {
    private final MotoVehicleEntity vehicle;

    public EngineSoundInstance(MotoVehicleEntity vehicle) {
        super(ModSounds.ENGINE, SoundSource.NEUTRAL, SoundInstance.createUnseededRandom());
        this.vehicle = vehicle;
        this.looping = true;
        this.delay = 0;
        this.volume = 0.3f;
        this.pitch = 0.6f;
        this.x = vehicle.getX();
        this.y = vehicle.getY();
        this.z = vehicle.getZ();
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    @Override
    public boolean canPlaySound() {
        return !this.vehicle.isSilent();
    }

    @Override
    public void tick() {
        if (this.vehicle.isRemoved() || !this.vehicle.isVehicle()) {
            this.vehicle.setEngineRunning(false);
            this.stop();
            return;
        }
        this.x = this.vehicle.getX();
        this.y = this.vehicle.getY();
        this.z = this.vehicle.getZ();
        float rpm = this.vehicle.getRpm();
        this.volume = 0.3f + 0.55f * rpm;
        this.pitch = Mth.clamp(this.vehicle.getStats().enginePitch() * (0.55f + 1.15f * rpm), 0.5f, 2.0f);
    }
}
