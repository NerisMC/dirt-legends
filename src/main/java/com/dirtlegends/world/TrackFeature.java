package com.dirtlegends.world;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** Places a motocross track in the world now and then (see data/dirtlegends/worldgen/placed_feature). */
public class TrackFeature extends Feature<NoneFeatureConfiguration> {
    public TrackFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel world = context.level();
        BlockPos origin = context.origin();
        int x = (origin.getX() & ~15) + 8;
        int z = (origin.getZ() & ~15) + 8;
        int ground = world.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
        BlockPos center = new BlockPos(x, ground, z);
        if (!world.getFluidState(center).isEmpty() || !world.getBlockState(center).isSolidRender()) {
            return false; // don't build on water or on top of plants/trees
        }
        // skip very hilly spots so the track doesn't cut through mountains
        int[][] probes = {{-17, 0}, {17, 0}, {0, -11}, {0, 11}};
        for (int[] p : probes) {
            int h = world.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x + p[0], z + p[1]) - 1;
            if (Math.abs(h - ground) > 4) return false;
        }
        TrackGenerator.generate(world, center, context.random());
        return true;
    }
}
