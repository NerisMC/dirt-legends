package com.dirtlegends.world;

import com.dirtlegends.block.FuelPumpBlock;
import com.dirtlegends.block.KickerRampBlock;
import com.dirtlegends.registry.ModBlocks;
import com.dirtlegends.registry.ModFluids;
import com.dirtlegends.registry.ModItems;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.ServerWorldAccess;

/**
 * Builds an oval motocross track centred on {@code center} (the ground block):
 * track dirt loop, start/finish line (east), checkpoints, two kickers, a liquid mud pit,
 * a gas station in the infield with fuel pumps and a chest of fuel cans, and bleachers
 * on the north side with a crowd of villagers watching.
 *
 * Everything stays within 21 blocks of the centre so it fits inside world generation limits.
 */
public final class TrackGenerator {
    private static final int A = 17;            // half-length of the oval (east-west)
    private static final int B = 11;            // half-width of the oval (north-south)
    private static final double HALF_WIDTH = 2.2;

    private TrackGenerator() {
    }

    public static void generate(ServerWorldAccess world, BlockPos center, Random random) {
        int y = center.getY();
        int cx = center.getX();
        int cz = center.getZ();
        BlockPos.Mutable m = new BlockPos.Mutable();
        BlockState air = Blocks.AIR.getDefaultState();
        BlockState track = ModBlocks.TRACK_DIRT.getDefaultState();
        BlockState grass = Blocks.GRASS_BLOCK.getDefaultState();
        BlockState dirt = Blocks.DIRT.getDefaultState();

        // ---- ground: flatten the loop, infield and a strip around it
        for (int dx = -A - 4; dx <= A + 4; dx++) {
            for (int dz = -B - 7; dz <= B + 4; dz++) {
                double t = signedDistance(dx, dz);
                boolean onTrack = Math.abs(t) <= HALF_WIDTH;
                boolean inside = t < -HALF_WIDTH;
                boolean apron = t > HALF_WIDTH && t <= HALF_WIDTH + 1.5;
                boolean bleachers = isBleacher(dx, dz);
                if (!onTrack && !inside && !apron && !bleachers) continue;
                int x = cx + dx, z = cz + dz;
                for (int h = 1; h <= 10; h++) {
                    set(world, m.set(x, y + h, z), air);
                }
                set(world, m.set(x, y, z), onTrack ? track : grass);
                for (int h = 1; h <= 6; h++) {
                    m.set(x, y - h, z);
                    BlockState below = world.getBlockState(m);
                    if (below.isReplaceable() || !below.getFluidState().isEmpty()) {
                        set(world, m, dirt);
                    } else {
                        break;
                    }
                }
            }
        }

        // ---- track features
        double mudX = A * Math.cos(Math.toRadians(135));
        double mudZ = B * Math.sin(Math.toRadians(135));
        for (int dx = -A - 3; dx <= A + 3; dx++) {
            for (int dz = -B - 3; dz <= B + 3; dz++) {
                if (Math.abs(signedDistance(dx, dz)) > HALF_WIDTH) continue;
                int x = cx + dx, z = cz + dz;
                if (dz == 0 && dx > 0) {
                    set(world, m.set(x, y, z), ModBlocks.START_FINISH_LINE.getDefaultState());
                } else if ((dz == 0 && dx < 0) || (dx == 0 && dz > 0)) {
                    set(world, m.set(x, y, z), ModBlocks.CHECKPOINT.getDefaultState());
                } else if (dz < 0 && (dx == -3 || dx == -2)) {
                    // riders go anticlockwise: east side heading south, so the north straight heads east
                    set(world, m.set(x, y + 1, z), ModBlocks.KICKER_RAMP.getDefaultState().with(KickerRampBlock.FACING, Direction.EAST));
                } else if (dz > 0 && (dx == 6 || dx == 7)) {
                    set(world, m.set(x, y + 1, z), ModBlocks.KICKER_RAMP.getDefaultState().with(KickerRampBlock.FACING, Direction.WEST));
                } else if ((dx - mudX) * (dx - mudX) + (dz - mudZ) * (dz - mudZ) <= 7.5) {
                    set(world, m.set(x, y, z), ModFluids.STILL_MUD.getDefaultState().getBlockState());
                    set(world, m.set(x, y - 1, z), dirt);
                }
            }
        }

        buildGasStation(world, cx, y, cz, m);
        buildBleachersAndCrowd(world, cx, y, cz, m, random);
    }

    /** Signed distance (in blocks, roughly) from the centre line of the oval. Negative = infield. */
    private static double signedDistance(int dx, int dz) {
        double nx = dx / (double) A, nz = dz / (double) B;
        double d = Math.sqrt(nx * nx + nz * nz);
        if (d < 1.0E-6) return -B;
        double r = Math.sqrt(dx * dx + dz * dz);
        return (d - 1.0) * (r / d);
    }

    private static boolean isBleacher(int dx, int dz) {
        return dx >= -8 && dx <= 8 && dz <= -B - 4 && dz >= -B - 6;
    }

    private static void buildGasStation(ServerWorldAccess world, int cx, int y, int cz, BlockPos.Mutable m) {
        BlockState pad = Blocks.SMOOTH_STONE.getDefaultState();
        BlockState roof = Blocks.SMOOTH_STONE_SLAB.getDefaultState();
        BlockState post = Blocks.IRON_BARS.getDefaultState();
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                set(world, m.set(cx + dx, y, cz + dz), pad);
                set(world, m.set(cx + dx, y + 4, cz + dz), roof);
            }
        }
        for (int dx : new int[]{-3, 3}) {
            for (int dz : new int[]{-2, 2}) {
                for (int h = 1; h <= 3; h++) {
                    set(world, m.set(cx + dx, y + h, cz + dz), post);
                }
            }
        }
        BlockState pump = ModBlocks.FUEL_PUMP.getDefaultState();
        set(world, m.set(cx - 1, y + 1, cz), pump.with(FuelPumpBlock.FACING, Direction.SOUTH));
        set(world, m.set(cx + 1, y + 1, cz), pump.with(FuelPumpBlock.FACING, Direction.NORTH));
        BlockPos chestPos = new BlockPos(cx, y + 1, cz - 2);
        set(world, m.set(chestPos), Blocks.CHEST.getDefaultState());
        if (world.getBlockEntity(chestPos) instanceof ChestBlockEntity chest) {
            chest.setStack(0, new ItemStack(ModItems.FUEL_CAN, 3));
            chest.setStack(13, new ItemStack(ModItems.MX_HELMET));
        }
    }

    private static void buildBleachersAndCrowd(ServerWorldAccess world, int cx, int y, int cz, BlockPos.Mutable m, Random random) {
        BlockState seat = Blocks.OAK_PLANKS.getDefaultState();
        for (int dx = -8; dx <= 8; dx++) {
            for (int row = 0; row < 3; row++) {
                int dz = -B - 4 - row;
                for (int h = 0; h <= row; h++) {
                    set(world, m.set(cx + dx, y + 1 + h, cz + dz), seat);
                }
            }
        }
        // the crowd: villagers standing on the bleachers, facing the track (south)
        int fans = 5 + random.nextInt(4);
        for (int i = 0; i < fans; i++) {
            VillagerEntity fan = EntityType.VILLAGER.create(world.toServerWorld());
            if (fan == null) continue;
            int row = random.nextInt(3);
            double x = cx - 8 + random.nextInt(17) + 0.5;
            double z = cz - B - 4 - row + 0.5;
            fan.refreshPositionAndAngles(x, y + 2 + row, z, 0.0f, 0.0f);
            fan.setHeadYaw(0.0f);
            fan.setBodyYaw(0.0f);
            fan.setAiDisabled(true);
            fan.setPersistent();
            world.spawnEntityAndPassengers(fan);
        }
    }

    private static void set(ServerWorldAccess world, BlockPos pos, BlockState state) {
        world.setBlockState(pos, state, Block.NOTIFY_LISTENERS);
    }
}
