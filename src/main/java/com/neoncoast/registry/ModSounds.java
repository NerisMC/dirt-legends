package com.neoncoast.registry;

import com.neoncoast.NeonCoast;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public final class ModSounds {
    public static final SoundEvent CAR_ENGINE = register("car_engine");
    public static final SoundEvent SIREN = register("siren");

    private ModSounds() {
    }

    private static SoundEvent register(String name) {
        Identifier id = NeonCoast.id(name);
        return Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
    }

    public static void init() {
    }
}
