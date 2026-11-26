package com.dirtlegends.registry;

import com.dirtlegends.DirtLegends;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public final class ModSounds {
    public static final SoundEvent ENGINE = register("engine");

    private ModSounds() {
    }

    private static SoundEvent register(String name) {
        Identifier id = DirtLegends.id(name);
        return Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
    }

    public static void init() {
    }
}
