package com.dirtlegends.registry;

import com.dirtlegends.DirtLegends;
import com.dirtlegends.fluid.MudFluid;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.material.FlowingFluid;

public final class ModFluids {
    public static final FlowingFluid STILL_MUD = Registry.register(BuiltInRegistries.FLUID, DirtLegends.id("mud"), new MudFluid.Still());
    public static final FlowingFluid FLOWING_MUD = Registry.register(BuiltInRegistries.FLUID, DirtLegends.id("flowing_mud"), new MudFluid.Flowing());

    private ModFluids() {
    }

    public static void init() {
    }
}
