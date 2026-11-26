package com.dirtlegends.registry;

import com.dirtlegends.DirtLegends;
import com.dirtlegends.world.TrackFeature;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public final class ModWorldgen {
    public static final Feature<NoneFeatureConfiguration> MOTOCROSS_TRACK = Registry.register(BuiltInRegistries.FEATURE,
            DirtLegends.id("motocross_track"), new TrackFeature(NoneFeatureConfiguration.CODEC));

    public static final ResourceKey<PlacedFeature> MOTOCROSS_TRACK_PLACED =
            ResourceKey.create(Registries.PLACED_FEATURE, DirtLegends.id("motocross_track"));

    private ModWorldgen() {
    }

    public static void init() {
        BiomeModifications.addFeature(
                BiomeSelectors.includeByKey(Biomes.PLAINS, Biomes.SUNFLOWER_PLAINS, Biomes.SAVANNA,
                        Biomes.DESERT, Biomes.BADLANDS, Biomes.MEADOW),
                GenerationStep.Decoration.SURFACE_STRUCTURES,
                MOTOCROSS_TRACK_PLACED);
    }
}
