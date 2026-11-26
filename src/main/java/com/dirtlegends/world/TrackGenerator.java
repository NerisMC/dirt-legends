package com.dirtlegends.world;

import com.dirtlegends.block.FuelPumpBlock;
import com.dirtlegends.block.KickerRampBlock;
import com.dirtlegends.registry.ModBlocks;
import com.dirtlegends.registry.ModFluids;
import com.dirtlegends.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

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

    public static void generate(ServerLevelAccessor world, BlockPos center, RandomSource random) {
        int y = center.getY();
        int cx = center.getX();
        int cz = center.getZ();
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState track = ModBlocks.TRACK_DIRT.defaultBlockState();
        BlockState grass = Blocks.GRASS_BLOCK.defaultBlockState();
        BlockState dirt = Blocks.DIRT.defaultBlockState();

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
                    if (below.canBeReplaced() || !below.getFluidState().isEmpty()) {
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
                    set(world, m.set(x, y, z), ModBlocks.START_FINISH_LINE.defaultBlockState());
                } else if ((dz == 0 && dx < 0) || (dx == 0 && dz > 0)) {
                    set(world, m.set(x, y, z), ModBlocks.CHECKPOINT.defaultBlockState());
                } else if (dz < 0 && (dx == -3 || dx == -2)) {
                    // riders go anticlockwise: east side heading south, so the north straight heads east
                    set(world, m.set(x, y + 1, z), ModBlocks.KICKER_RAMP.defaultBlockState().setValue(KickerRampBlock.FACING, Direction.EAST));
                } else if (dz > 0 && (dx == 6 || dx == 7)) {
                    set(world, m.set(x, y + 1, z), ModBlocks.KICKER_RAMP.defaultBlockState().setValue(KickerRampBlock.FACING, Direction.WEST));
                } else if ((dx - mudX) * (dx - mudX) + (dz - mudZ) * (dz - mudZ) <= 7.5) {
                    set(world, m.set(x, y, z), ModFluids.STILL_MUD.defaultFluidState().createLegacyBlock());
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

    private static void buildGasStation(ServerLevelAccessor world, int cx, int y, int cz, BlockPos.MutableBlockPos m) {
        BlockState pad = Blocks.SMOOTH_STONE.defaultBlockState();
        BlockState roof = Blocks.SMOOTH_STONE_SLAB.defaultBlockState();
        BlockState post = Blocks.IRON_BARS.defaultBlockState();
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
        BlockState pump = ModBlocks.FUEL_PUMP.defaultBlockState();
        set(world, m.set(cx - 1, y + 1, cz), pump.setValue(FuelPumpBlock.FACING, Direction.SOUTH));
        set(world, m.set(cx + 1, y + 1, cz), pump.setValue(FuelPumpBlock.FACING, Direction.NORTH));
        BlockPos chestPos = new BlockPos(cx, y + 1, cz - 2);
        set(world, m.set(chestPos), Blocks.CHEST.defaultBlockState());
        if (world.getBlockEntity(chestPos) instanceof ChestBlockEntity chest) {
            chest.setItem(0, new ItemStack(ModItems.FUEL_CAN, 3));
            chest.setItem(13, new ItemStack(ModItems.MX_HELMET));
        }
    }

    private static void buildBleachersAndCrowd(ServerLevelAccessor world, int cx, int y, int cz, BlockPos.MutableBlockPos m, RandomSource random) {
        BlockState seat = Blocks.OAK_PLANKS.defaultBlockState();
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
            Villager fan = EntityType.VILLAGER.create(world.getLevel(), net.minecraft.world.entity.EntitySpawnReason.STRUCTURE);
            if (fan == null) continue;
            int row = random.nextInt(3);
            double x = cx - 8 + random.nextInt(17) + 0.5;
            double z = cz - B - 4 - row + 0.5;
            fan.snapTo(x, y + 2 + row, z, 0.0f, 0.0f);
            fan.setYHeadRot(0.0f);
            fan.setYBodyRot(0.0f);
            fan.setNoAi(true);
            fan.setPersistenceRequired();
            world.addFreshEntityWithPassengers(fan);
        }
    }

    private static void set(ServerLevelAccessor world, BlockPos pos, BlockState state) {
        world.setBlock(pos, state, Block.UPDATE_CLIENTS);
    }
}
