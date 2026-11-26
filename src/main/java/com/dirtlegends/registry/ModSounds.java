package com.dirtlegends.registry;

import com.dirtlegends.DirtLegends;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

public final class ModSounds {
    public static final SoundEvent ENGINE = register("engine");

    private ModSounds() {
    }

    private static SoundEvent register(String name) {
        Identifier id = DirtLegends.id(name);
        return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
    }

    public static void init() {
    }
}
