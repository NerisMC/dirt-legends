package com.neoncoast.client;

import com.neoncoast.registry.ModSounds;
import com.neoncoast.vehicle.CarEntity;
import net.minecraft.client.sound.MovingSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.MathHelper;

/** Engine note (pitch follows RPM) or police siren, following the car. */
public class CarSoundInstance extends MovingSoundInstance {
    private final CarEntity car;
    private final boolean siren;

    public CarSoundInstance(CarEntity car, boolean siren) {
        super(siren ? ModSounds.SIREN : ModSounds.CAR_ENGINE, SoundCategory.NEUTRAL, SoundInstance.createRandom());
        this.car = car;
        this.siren = siren;
        this.repeat = true;
        this.repeatDelay = 0;
        this.volume = 0.2f;
        this.x = car.getX();
        this.y = car.getY();
        this.z = car.getZ();
    }

    @Override
    public boolean shouldAlwaysPlay() {
        return true;
    }

    @Override
    public void tick() {
        boolean running = car.hasPassengers() || car.isSirenOn();
        if (car.isRemoved() || !running) {
            if (!siren) car.setSoundRunning(false);
            this.setDone();
            return;
        }
        this.x = car.getX();
        this.y = car.getY();
        this.z = car.getZ();
        if (siren) {
            this.volume = car.isSirenOn() ? 1.0f : 0.0f;
            this.pitch = 1.0f;
        } else {
            float rpm = car.getRpm();
            this.volume = 0.35f + 0.5f * rpm;
            this.pitch = MathHelper.clamp(car.getCarType().enginePitch() * (0.6f + 1.2f * rpm), 0.5f, 2.0f);
        }
    }
}
