package com.dirtlegends.world;

import com.dirtlegends.block.FuelPumpBlock;
import com.dirtlegends.block.KickerRampBlock;
import com.dirtlegends.entity.MotoVehicleEntity;
import com.dirtlegends.npc.RacerEntity;
import com.dirtlegends.npc.SpectatorEntity;
import com.dirtlegends.npc.VendorEntity;
import com.dirtlegends.registry.ModBlocks;
import com.dirtlegends.registry.ModEntities;
import com.dirtlegends.registry.ModFluids;
import com.dirtlegends.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;

/**
 * Builds a full motocross venue centred on {@code center} (the ground block). Riders go round the oval
 * clockwise on the map: down the east end (start/finish), west along the south straight, up the west end,
 * east along the north straight.
 *
 * Track: start/finish gantry with a checkered banner, two checkpoints, a tabletop jump on the north straight,
 * a whoops section and a kicker on the south straight, banked berms in both turns, a liquid mud pit,
 * tyre walls on the outside of the turns and hay bales on the infield.
 * Venue: covered grandstand full of fans, two concession stands with vendors and picnic tables,
 * porta-potties, light towers, a pit area with team tents, a mechanic and parked bikes,
 * the infield gas station, and three rival racers waiting on the grid.
 *
 * Everything stays within 23 blocks of the centre so it fits inside world generation limits.
 */
public final class TrackGenerator {
    public static final int A = 17;             // half-length of the oval (east-west)
    public static final int B = 11;             // half-width of the oval (north-south)
    private static final double HALF_WIDTH = 2.2;
    private static final int MIN_X = -22, MAX_X = 22, MIN_Z = -20, MAX_Z = 19;

    private TrackGenerator() {
    }

    public static void generate(ServerLevelAccessor world, BlockPos center, RandomSource random) {
        int y = center.getY(), cx = center.getX(), cz = center.getZ();
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState track = ModBlocks.TRACK_DIRT.defaultBlockState();
        BlockState dirt = Blocks.DIRT.defaultBlockState();

        // ---- ground: one flat venue
        for (int dx = MIN_X; dx <= MAX_X; dx++) {
            for (int dz = MIN_Z; dz <= MAX_Z; dz++) {
                int x = cx + dx, z = cz + dz;
                for (int h = 1; h <= 12; h++) set(world, m.set(x, y + h, z), air);
                boolean onTrack = Math.abs(signedDistance(dx, dz)) <= HALF_WIDTH;
                boolean pits = dz >= B + 4 && Math.abs(dx) <= 15;
                set(world, m.set(x, y, z), onTrack ? track : pits ? Blocks.GRAVEL.defaultBlockState() : Blocks.GRASS_BLOCK.defaultBlockState());
                for (int h = 1; h <= 6; h++) {
                    m.set(x, y - h, z);
                    BlockState below = world.getBlockState(m);
                    if (below.canBeReplaced() || !below.getFluidState().isEmpty()) set(world, m, dirt);
                    else break;
                }
            }
        }

        buildTrackFeatures(world, cx, y, cz, m);
        buildBarriers(world, cx, y, cz, m);
        buildStartGantry(world, cx, y, cz, m);
        buildGasStation(world, cx, y, cz, m);
        buildGrandstand(world, cx, y, cz, m, random);
        buildConcessionStand(world, cx - 18, y, cz - 17, m, random);
        buildConcessionStand(world, cx + 16, y, cz - 17, m, random);
        buildPortaPotties(world, cx, y, cz, m);
        buildLightTowers(world, cx, y, cz, m);
        buildPits(world, cx, y, cz, m, random);
        spawnRacers(world, cx, y, cz, random);
    }

    // ------------------------------------------------------------------ the track

    private static void buildTrackFeatures(ServerLevelAccessor world, int cx, int y, int cz, BlockPos.MutableBlockPos m) {
        BlockState track = ModBlocks.TRACK_DIRT.defaultBlockState();
        BlockState slab = ModBlocks.DIRT_SLAB.defaultBlockState();
        double mudX = A * Math.cos(Math.toRadians(135)), mudZ = B * Math.sin(Math.toRadians(135));
        for (int dx = -A - 3; dx <= A + 3; dx++) {
            for (int dz = -B - 3; dz <= B + 3; dz++) {
                double t = signedDistance(dx, dz);
                if (Math.abs(t) > HALF_WIDTH) continue;
                int x = cx + dx, z = cz + dz;
                if (dz == 0 && dx > 0) {
                    set(world, m.set(x, y, z), ModBlocks.START_FINISH_LINE.defaultBlockState());
                } else if ((dz == 0 && dx < 0) || (dx == 0 && dz > 0)) {
                    set(world, m.set(x, y, z), ModBlocks.CHECKPOINT.defaultBlockState());
                } else if (dz < 0 && dx >= -7 && dx <= 3) {
                    // tabletop on the north straight (riders head east): up-ramp, 7-block deck, down-ramp
                    if (dx == -7) set(world, m.set(x, y + 1, z), ramp(Direction.EAST));
                    else if (dx == -6) { set(world, m.set(x, y + 1, z), track); set(world, m.set(x, y + 2, z), ramp(Direction.EAST)); }
                    else if (dx <= 1) { set(world, m.set(x, y + 1, z), track); set(world, m.set(x, y + 2, z), track); }
                    else if (dx == 2) { set(world, m.set(x, y + 1, z), track); set(world, m.set(x, y + 2, z), ramp(Direction.WEST)); }
                    else set(world, m.set(x, y + 1, z), ramp(Direction.WEST));
                } else if (dz > 0 && dx >= -9 && dx <= -2 && Math.floorMod(dx, 2) == 0) {
                    set(world, m.set(x, y + 1, z), slab);   // whoops on the south straight
                } else if (dz > 0 && (dx == 6 || dx == 7)) {
                    set(world, m.set(x, y + 1, z), ModBlocks.KICKER_RAMP.defaultBlockState().setValue(KickerRampBlock.FACING, Direction.WEST));
                } else if ((dx - mudX) * (dx - mudX) + (dz - mudZ) * (dz - mudZ) <= 7.5) {
                    set(world, m.set(x, y, z), ModFluids.STILL_MUD.defaultFluidState().createLegacyBlock());
                    set(world, m.set(x, y - 1, z), Blocks.DIRT.defaultBlockState());
                } else if (Math.abs(dx) >= A - 6 && Math.abs(dz) > 1 && t > 0.8) {
                    // berms: the outside of each turn is banked up
                    set(world, m.set(x, y + 1, z), t > 1.6 ? track : slab);
                }
            }
        }
    }

    private static BlockState ramp(Direction up) {
        return ModBlocks.DIRT_RAMP.defaultBlockState().setValue(StairBlock.FACING, up);
    }

    /** Tyre walls round the outside of the turns, hay bales along the infield of the straights. */
    private static void buildBarriers(ServerLevelAccessor world, int cx, int y, int cz, BlockPos.MutableBlockPos m) {
        for (int dx = -A - 4; dx <= A + 4; dx++) {
            for (int dz = -B - 4; dz <= B + 4; dz++) {
                double t = signedDistance(dx, dz);
                boolean turn = Math.abs(dx) >= A - 5;
                if (turn && t > HALF_WIDTH + 0.6 && t <= HALF_WIDTH + 1.4 && Math.abs(dz) > 2) {
                    set(world, m.set(cx + dx, y + 1, cz + dz), Math.floorMod(dx + dz, 3) == 0
                            ? Blocks.WHITE_CONCRETE.defaultBlockState() : Blocks.BLACK_CONCRETE.defaultBlockState());
                } else if (!turn && t < -HALF_WIDTH - 0.6 && t >= -HALF_WIDTH - 1.4 && Math.floorMod(dx, 3) == 0) {
                    set(world, m.set(cx + dx, y + 1, cz + dz), Blocks.HAY_BLOCK.defaultBlockState());
                }
            }
        }
    }

    /** Gantry over the start/finish line with a checkered banner. */
    private static void buildStartGantry(ServerLevelAccessor world, int cx, int y, int cz, BlockPos.MutableBlockPos m) {
        int x0 = cx + A - 4, x1 = cx + A + 4;
        for (int h = 1; h <= 5; h++) {
            set(world, m.set(x0, y + h, cz), Blocks.IRON_BARS.defaultBlockState());
            set(world, m.set(x1, y + h, cz), Blocks.IRON_BARS.defaultBlockState());
        }
        for (int x = x0; x <= x1; x++) {
            for (int h = 6; h <= 7; h++) {
                boolean black = Math.floorMod(x + h, 2) == 0;
                set(world, m.set(x, y + h, cz), black ? Blocks.BLACK_CONCRETE.defaultBlockState() : Blocks.WHITE_CONCRETE.defaultBlockState());
            }
        }
    }

    // ------------------------------------------------------------------ the venue

    private static void buildGrandstand(ServerLevelAccessor world, int cx, int y, int cz, BlockPos.MutableBlockPos m, RandomSource random) {
        BlockState frame = Blocks.STONE_BRICKS.defaultBlockState();
        BlockState seat = Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH);
        int front = -B - 5;                          // dz of the first row
        for (int dx = -11; dx <= 11; dx++) {
            for (int row = 0; row < 4; row++) {
                int z = cz + front - row;
                for (int h = 1; h <= row; h++) set(world, m.set(cx + dx, y + h, z), frame);
                set(world, m.set(cx + dx, y + 1 + row, z), seat);
            }
            for (int h = 1; h <= 8; h++) set(world, m.set(cx + dx, y + h, cz + front - 4), frame);   // back wall
            for (int dz = front - 4; dz <= front + 1; dz++) {                                          // striped roof
                set(world, m.set(cx + dx, y + 9, cz + dz), Math.floorMod(dx, 4) < 2
                        ? Blocks.RED_CONCRETE.defaultBlockState() : Blocks.WHITE_CONCRETE.defaultBlockState());
            }
        }
        for (int dx : new int[]{-11, 0, 11}) {
            for (int h = 1; h <= 8; h++) set(world, m.set(cx + dx, y + h, cz + front + 1), Blocks.IRON_BARS.defaultBlockState());
        }
        // announcer booth hanging under the roof
        for (int dx = -2; dx <= 2; dx++) set(world, m.set(cx + dx, y + 8, cz + front - 3), Blocks.GLASS.defaultBlockState());

        int fans = 12 + random.nextInt(6);
        for (int i = 0; i < fans; i++) {
            int row = random.nextInt(4);
            spectator(world, cx - 10 + random.nextInt(21) + 0.5, y + 2 + row, cz + front - row + 0.5, 0.0f);
        }
    }

    private static void buildConcessionStand(ServerLevelAccessor world, int x0, int y, int z0, BlockPos.MutableBlockPos m, RandomSource random) {
        // 5 wide (x0..x0+4), 3 deep (z0..z0+2), counter on the south side facing the crowd
        BlockState wall = Blocks.SPRUCE_PLANKS.defaultBlockState();
        for (int dx = 0; dx < 5; dx++) {
            for (int dz = 0; dz < 3; dz++) {
                set(world, m.set(x0 + dx, y, z0 + dz), Blocks.SMOOTH_STONE.defaultBlockState());
                boolean side = dx == 0 || dx == 4;
                boolean back = dz == 0;
                boolean counter = dz == 2;
                for (int h = 1; h <= 3; h++) {
                    if (back || (side && h <= 3)) set(world, m.set(x0 + dx, y + h, z0 + dz), wall);
                }
                if (counter && !side) {
                    set(world, m.set(x0 + dx, y + 1, z0 + dz), wall);
                    set(world, m.set(x0 + dx, y + 2, z0 + dz), Blocks.SMOOTH_STONE_SLAB.defaultBlockState());
                }
            }
            // striped awning reaching out over the customers, with a menu board above
            for (int dz = 0; dz <= 4; dz++) {
                set(world, m.set(x0 + dx, y + 4, z0 + dz), Math.floorMod(dx, 2) == 0
                        ? Blocks.RED_WOOL.defaultBlockState() : Blocks.WHITE_WOOL.defaultBlockState());
            }
            set(world, m.set(x0 + dx, y + 5, z0 + 2), Blocks.YELLOW_CONCRETE.defaultBlockState());
        }
        set(world, m.set(x0 + 1, y + 1, z0 + 1), Blocks.SMOKER.defaultBlockState().setValue(AbstractFurnaceBlock.FACING, Direction.SOUTH));
        set(world, m.set(x0 + 3, y + 1, z0 + 1), Blocks.BARREL.defaultBlockState());
        set(world, m.set(x0 + 3, y + 2, z0 + 1), Blocks.LANTERN.defaultBlockState());

        VendorEntity vendor = ModEntities.VENDOR.create(world.getLevel(), EntitySpawnReason.STRUCTURE);
        if (vendor != null) {
            vendor.setup(VendorEntity.FOOD);
            place(world, vendor, x0 + 2.5, y + 1, z0 + 1.5, 0.0f);
        }
        // picnic table and a bin, and a couple of hungry fans in line
        set(world, m.set(x0 + 1, y + 1, z0 + 6), Blocks.SPRUCE_FENCE.defaultBlockState());
        set(world, m.set(x0 + 1, y + 2, z0 + 6), Blocks.SPRUCE_SLAB.defaultBlockState());
        set(world, m.set(x0, y + 1, z0 + 6), Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.WEST));
        set(world, m.set(x0 + 2, y + 1, z0 + 6), Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST));
        set(world, m.set(x0 + 4, y + 1, z0 + 6), Blocks.COMPOSTER.defaultBlockState());
        int line = 1 + random.nextInt(2);
        for (int i = 0; i < line; i++) spectator(world, x0 + 2.5, y + 1, z0 + 3.6 + i, 180.0f);
    }

    private static void buildPortaPotties(ServerLevelAccessor world, int cx, int y, int cz, BlockPos.MutableBlockPos m) {
        for (int i = 0; i < 3; i++) {
            int x = cx + 21, z = cz - 12 + i * 2;
            set(world, m.set(x, y + 1, z), Blocks.BLUE_TERRACOTTA.defaultBlockState());
            set(world, m.set(x, y + 2, z), Blocks.BLUE_TERRACOTTA.defaultBlockState());
            set(world, m.set(x, y + 3, z), Blocks.SMOOTH_QUARTZ_SLAB.defaultBlockState());
        }
    }

    private static void buildLightTowers(ServerLevelAccessor world, int cx, int y, int cz, BlockPos.MutableBlockPos m) {
        for (int[] p : new int[][]{{-21, -14}, {21, -15}, {-21, 13}, {21, 13}}) {
            int x = cx + p[0], z = cz + p[1];
            for (int h = 1; h <= 9; h++) set(world, m.set(x, y + h, z), Blocks.IRON_BARS.defaultBlockState());
            int towardTrack = p[0] < 0 ? 1 : -1;
            for (int k = -1; k <= 1; k++) {
                set(world, m.set(x + towardTrack, y + 10, z + k), Blocks.SEA_LANTERN.defaultBlockState());
                set(world, m.set(x + towardTrack, y + 11, z + k), Blocks.SMOOTH_STONE_SLAB.defaultBlockState());
            }
        }
    }

    /** Team tents along the south side: bikes on stands, tool chests, tyre stacks, and the pit mechanic. */
    private static void buildPits(ServerLevelAccessor world, int cx, int y, int cz, BlockPos.MutableBlockPos m, RandomSource random) {
        Block[] colors = {Blocks.RED_WOOL, Blocks.BLUE_WOOL, Blocks.YELLOW_WOOL, Blocks.ORANGE_WOOL, Blocks.LIME_WOOL};
        int z0 = cz + B + 5;
        for (int tent = 0; tent < 3; tent++) {
            int x0 = cx - 14 + tent * 10;
            Block color = colors[random.nextInt(colors.length)];
            for (int[] post : new int[][]{{0, 0}, {6, 0}, {0, 3}, {6, 3}}) {
                for (int h = 1; h <= 3; h++) set(world, m.set(x0 + post[0], y + h, z0 + post[1]), Blocks.IRON_BARS.defaultBlockState());
            }
            for (int dx = 0; dx <= 6; dx++) {
                for (int dz = 0; dz <= 3; dz++) {
                    set(world, m.set(x0 + dx, y + 4, z0 + dz), dz == 1 ? Blocks.WHITE_WOOL.defaultBlockState() : color.defaultBlockState());
                }
            }
            BlockPos chest = new BlockPos(x0 + 1, y + 1, z0 + 3);
            set(world, chest, Blocks.CHEST.defaultBlockState());
            if (world.getBlockEntity(chest) instanceof ChestBlockEntity c) {
                c.setItem(0, new ItemStack(ModItems.FUEL_CAN, 2));
                c.setItem(1, new ItemStack(ModItems.TIRE, 2));
                c.setItem(2, new ItemStack(ModItems.HOT_DOG, 2));
            }
            set(world, m.set(x0 + 2, y + 1, z0 + 3), Blocks.ANVIL.defaultBlockState().setValue(AnvilBlock.FACING, Direction.EAST));
            for (int h = 1; h <= 2; h++) set(world, m.set(x0 + 5, y + h, z0 + 3), Blocks.BLACK_CONCRETE.defaultBlockState());   // tyre stack
            if (tent == 1) {
                VendorEntity mechanic = ModEntities.VENDOR.create(world.getLevel(), EntitySpawnReason.STRUCTURE);
                if (mechanic != null) {
                    mechanic.setup(VendorEntity.MECHANIC);
                    place(world, mechanic, x0 + 3.5, y + 1, z0 + 2.5, 180.0f);
                }
            } else {
                EntityType<? extends MotoVehicleEntity> type = random.nextBoolean() ? ModEntities.DIRT_BIKE : ModEntities.ATV;
                MotoVehicleEntity bike = type.create(world.getLevel(), EntitySpawnReason.STRUCTURE);
                if (bike != null) {
                    bike.setVariant(random.nextInt(ModItems.COLORS.length));
                    place(world, bike, x0 + 3.5, y + 1, z0 + 1.5, 180.0f);
                }
            }
        }
        // a couple of fans hanging round the pits
        spectator(world, cx - 4.5, y + 1, z0 - 1.5, 180.0f);
        spectator(world, cx + 6.5, y + 1, z0 - 1.5, 180.0f);
    }

    private static void buildGasStation(ServerLevelAccessor world, int cx, int y, int cz, BlockPos.MutableBlockPos m) {
        BlockState pad = Blocks.SMOOTH_STONE.defaultBlockState();
        BlockState roof = Blocks.SMOOTH_STONE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
        BlockState post = Blocks.IRON_BARS.defaultBlockState();
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                set(world, m.set(cx + dx, y, cz + dz), pad);
                set(world, m.set(cx + dx, y + 4, cz + dz), roof);
            }
        }
        for (int dx : new int[]{-3, 3}) {
            for (int dz : new int[]{-2, 2}) {
                for (int h = 1; h <= 3; h++) set(world, m.set(cx + dx, y + h, cz + dz), post);
            }
        }
        BlockState pump = ModBlocks.FUEL_PUMP.defaultBlockState();
        set(world, m.set(cx - 1, y + 1, cz), pump.setValue(FuelPumpBlock.FACING, Direction.SOUTH));
        set(world, m.set(cx + 1, y + 1, cz), pump.setValue(FuelPumpBlock.FACING, Direction.NORTH));
        BlockPos chestPos = new BlockPos(cx, y + 1, cz - 2);
        set(world, chestPos, Blocks.CHEST.defaultBlockState());
        if (world.getBlockEntity(chestPos) instanceof ChestBlockEntity chest) {
            chest.setItem(0, new ItemStack(ModItems.FUEL_CAN, 3));
            chest.setItem(13, new ItemStack(ModItems.MX_HELMET));
        }
    }

    // ------------------------------------------------------------------ people

    /** Three rival racers lined up behind the start line on the east end, facing the way the race goes. */
    private static void spawnRacers(ServerLevelAccessor world, int cx, int y, int cz, RandomSource random) {
        double[] thetas = {-0.30, -0.48, -0.66};
        int[] skins = {0, 1, 2, 3, 4, 5};
        for (int i = 0; i < thetas.length; i++) {
            double th = thetas[i];
            double px = cx + 0.5 + A * Math.cos(th) + (i % 2 == 0 ? -0.8 : 0.8);
            double pz = cz + 0.5 + B * Math.sin(th);
            double tx = -A * Math.sin(th), tz = B * Math.cos(th);   // direction of travel
            float yaw = (float) Math.toDegrees(Math.atan2(-tx, tz));
            EntityType<? extends MotoVehicleEntity> type = i == 2 ? ModEntities.ATV : ModEntities.DIRT_BIKE;
            MotoVehicleEntity bike = type.create(world.getLevel(), EntitySpawnReason.STRUCTURE);
            RacerEntity racer = ModEntities.RACER.create(world.getLevel(), EntitySpawnReason.STRUCTURE);
            if (bike == null || racer == null) continue;
            bike.setVariant(random.nextInt(ModItems.COLORS.length));
            bike.snapTo(px, y + 1, pz, yaw, 0.0f);
            bike.setYHeadRot(yaw);
            bike.setYBodyRot(yaw);
            bike.setPersistenceRequired();
            racer.setup(new BlockPos(cx, y, cz), A, B, skins[(i * 2 + random.nextInt(2)) % skins.length]);
            racer.snapTo(px, y + 1, pz, yaw, 0.0f);
            racer.startRiding(bike, true, true);
            world.addFreshEntityWithPassengers(bike);   // in world generation the riders must be on before it's added
        }
    }

    private static void spectator(ServerLevelAccessor world, double x, double y, double z, float yaw) {
        SpectatorEntity fan = ModEntities.SPECTATOR.create(world.getLevel(), EntitySpawnReason.STRUCTURE);
        if (fan == null) return;
        fan.randomize();
        place(world, fan, x, y, z, yaw);
    }

    private static void place(ServerLevelAccessor world, Mob mob, double x, double y, double z, float yaw) {
        mob.snapTo(x, y, z, yaw, 0.0f);
        mob.setYHeadRot(yaw);
        mob.setYBodyRot(yaw);
        mob.setPersistenceRequired();
        world.addFreshEntity(mob);
    }

    // ------------------------------------------------------------------ helpers

    /** Signed distance (in blocks, roughly) from the centre line of the oval. Negative = infield. */
    private static double signedDistance(int dx, int dz) {
        double nx = dx / (double) A, nz = dz / (double) B;
        double d = Math.sqrt(nx * nx + nz * nz);
        if (d < 1.0E-6) return -B;
        double r = Math.sqrt(dx * dx + dz * dz);
        return (d - 1.0) * (r / d);
    }

    private static void set(ServerLevelAccessor world, BlockPos pos, BlockState state) {
        world.setBlock(pos, state, Block.UPDATE_CLIENTS);
    }
}
