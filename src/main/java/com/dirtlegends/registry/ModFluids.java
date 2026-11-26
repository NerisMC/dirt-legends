package com.dirtlegends.registry;

import com.dirtlegends.DirtLegends;
import com.dirtlegends.fluid.MudFluid;
import net.minecraft.fluid.FlowableFluid;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public final class ModFluids {
    public static final FlowableFluid STILL_MUD = Registry.register(Registries.FLUID, DirtLegends.id("mud"), new MudFluid.Still());
    public static final FlowableFluid FLOWING_MUD = Registry.register(Registries.FLUID, DirtLegends.id("flowing_mud"), new MudFluid.Flowing());

    private ModFluids() {
    }

    public static void init() {
    }
}
