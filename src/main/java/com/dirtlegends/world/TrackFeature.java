package com.dirtlegends.world;

import com.mojang.serialization.Codec;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;

/** Places a motocross track in the world now and then (see data/dirtlegends/worldgen/placed_feature). */
public class TrackFeature extends Feature<DefaultFeatureConfig> {
    public TrackFeature(Codec<DefaultFeatureConfig> codec) {
        super(codec);
    }

    @Override
    public boolean generate(FeatureContext<DefaultFeatureConfig> context) {
        StructureWorldAccess world = context.getWorld();
        BlockPos origin = context.getOrigin();
        int x = (origin.getX() & ~15) + 8;
        int z = (origin.getZ() & ~15) + 8;
        int ground = world.getTopY(Heightmap.Type.WORLD_SURFACE_WG, x, z) - 1;
        BlockPos center = new BlockPos(x, ground, z);
        if (!world.getFluidState(center).isEmpty() || !world.getBlockState(center).isOpaqueFullCube(world, center)) {
            return false; // don't build on water or on top of plants/trees
        }
        // skip very hilly spots so the track doesn't cut through mountains
        int[][] probes = {{-17, 0}, {17, 0}, {0, -11}, {0, 11}};
        for (int[] p : probes) {
            int h = world.getTopY(Heightmap.Type.WORLD_SURFACE_WG, x + p[0], z + p[1]) - 1;
            if (Math.abs(h - ground) > 4) return false;
        }
        TrackGenerator.generate(world, center, context.getRandom());
        return true;
    }
}
