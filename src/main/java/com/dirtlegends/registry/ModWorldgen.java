package com.dirtlegends.registry;

import com.dirtlegends.DirtLegends;
import com.dirtlegends.world.TrackFeature;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.PlacedFeature;

public final class ModWorldgen {
    public static final Feature<DefaultFeatureConfig> MOTOCROSS_TRACK = Registry.register(Registries.FEATURE,
            DirtLegends.id("motocross_track"), new TrackFeature(DefaultFeatureConfig.CODEC));

    public static final RegistryKey<PlacedFeature> MOTOCROSS_TRACK_PLACED =
            RegistryKey.of(RegistryKeys.PLACED_FEATURE, DirtLegends.id("motocross_track"));

    private ModWorldgen() {
    }

    public static void init() {
        BiomeModifications.addFeature(
                BiomeSelectors.includeByKey(BiomeKeys.PLAINS, BiomeKeys.SUNFLOWER_PLAINS, BiomeKeys.SAVANNA,
                        BiomeKeys.DESERT, BiomeKeys.BADLANDS, BiomeKeys.MEADOW),
                GenerationStep.Feature.SURFACE_STRUCTURES,
                MOTOCROSS_TRACK_PLACED);
    }
}
