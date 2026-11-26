package com.neoncoast.world;

import com.neoncoast.block.ShopTerminalBlock;
import com.neoncoast.registry.ModBlocks;
import com.neoncoast.registry.ModEntities;
import com.neoncoast.vehicle.CarEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LanternBlock;
import net.minecraft.block.LeavesBlock;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.ServerWorldAccess;

/**
 * One 48x48 city district: a road grid on a 24-block pitch (so neighbouring districts join up
 * into big cities), sidewalks, street lights, and four lots with random buildings
 * (towers, shops, job centres, chop shops, police stations, parks). Parked cars and citizens too.
 */
public final class CityGenerator {
    public static final int SIZE = 48;
    private static final int PITCH = 24;
    private static final int ROAD = 5;
    private static final int CLEAR_HEIGHT = 40;
    /** Shifts the road grid so districts (which start 16 blocks before a chunk that is a multiple of 3) line up with it. */
    public static final int OFF = 16;

    private static final BlockState[] FACADES = {
            Blocks.WHITE_CONCRETE.getDefaultState(), Blocks.LIGHT_GRAY_CONCRETE.getDefaultState(),
            Blocks.GRAY_CONCRETE.getDefaultState(), Blocks.CYAN_TERRACOTTA.getDefaultState(),
            Blocks.PINK_CONCRETE.getDefaultState(), Blocks.LIGHT_BLUE_CONCRETE.getDefaultState(),
            Blocks.SMOOTH_SANDSTONE.getDefaultState(), Blocks.QUARTZ_BLOCK.getDefaultState()};

    private enum Lot { TOWER, TOWER2, SHOP, JOBS, CHOP_SHOP, POLICE, PARK }

    private CityGenerator() {
    }

    /** Snaps a position to the district grid (returns the north-west corner). */
    public static int snap(int v) {
        return Math.floorDiv(v + OFF, SIZE) * SIZE - OFF;
    }

    /** x0/z0 = north-west corner, y = street level (the road surface). */
    public static void generateDistrict(ServerWorldAccess world, int x0, int y, int z0, Random random) {
        BlockPos.Mutable m = new BlockPos.Mutable();
        BlockState asphalt = ModBlocks.ASPHALT.getDefaultState();
        BlockState line = ModBlocks.ROAD_LINE.getDefaultState();
        BlockState walk = ModBlocks.SIDEWALK.getDefaultState();
        BlockState stone = Blocks.STONE.getDefaultState();
        BlockState air = Blocks.AIR.getDefaultState();

        // ground, roads, sidewalks
        for (int dx = 0; dx < SIZE; dx++) {
            for (int dz = 0; dz < SIZE; dz++) {
                int x = x0 + dx, z = z0 + dz;
                int lx = Math.floorMod(x + OFF, PITCH), lz = Math.floorMod(z + OFF, PITCH);
                boolean roadX = lx < ROAD, roadZ = lz < ROAD;
                BlockState surface;
                if (roadX || roadZ) {
                    boolean centre = (roadX && lx == 2 && !roadZ && Math.floorMod(z, 4) < 2)
                            || (roadZ && lz == 2 && !roadX && Math.floorMod(x, 4) < 2);
                    surface = centre ? line : asphalt;
                } else if (lx == ROAD || lz == ROAD || lx == PITCH - 1 || lz == PITCH - 1) {
                    surface = walk;
                } else {
                    surface = Blocks.GRASS_BLOCK.getDefaultState();
                }
                for (int h = 1; h <= CLEAR_HEIGHT; h++) set(world, m.set(x, y + h, z), air);
                set(world, m.set(x, y, z), surface);
                for (int h = 1; h <= 8; h++) {
                    m.set(x, y - h, z);
                    BlockState below = world.getBlockState(m);
                    if (below.isReplaceable() || !below.getFluidState().isEmpty() || below.isOf(Blocks.GRASS_BLOCK)) set(world, m, stone);
                    else break;
                }
                // street lights on the sidewalk corners
                if (!roadX && !roadZ && lx == ROAD && lz == ROAD) streetLight(world, x, y, z, m);
                if (!roadX && !roadZ && lx == PITCH - 1 && lz == PITCH - 1) streetLight(world, x, y, z, m);
            }
        }

        // the four lots in this district
        for (int i = 0; i < SIZE; i += PITCH) {
            for (int j = 0; j < SIZE; j += PITCH) {
                int lotX = Math.floorDiv(x0 + i + OFF, PITCH) * PITCH - OFF + ROAD + 1;
                int lotZ = Math.floorDiv(z0 + j + OFF, PITCH) * PITCH - OFF + ROAD + 1;
                int lotSize = PITCH - ROAD - 2; // 17
                if (lotX < x0 || lotZ < z0 || lotX + lotSize > x0 + SIZE || lotZ + lotSize > z0 + SIZE) continue;
                Lot type = pickLot(lotX, lotZ, random);
                buildLot(world, type, lotX, y, lotZ, lotSize, random, m);
            }
        }

        // parked cars along the roads, and citizens on the sidewalks
        int cars = 2 + random.nextInt(3);
        for (int c = 0; c < cars; c++) {
            boolean alongX = random.nextBoolean();
            int roadStart = (Math.floorDiv(x0 + OFF, PITCH) * PITCH - OFF) + (random.nextBoolean() ? 0 : PITCH);
            int along = 6 + random.nextInt(SIZE - 12);
            double x, z;
            float yaw;
            if (alongX) {
                x = x0 + along + 0.5;
                z = (Math.floorDiv(z0 + OFF, PITCH) * PITCH - OFF) + (random.nextBoolean() ? 0 : PITCH) + 1.0;
                yaw = random.nextBoolean() ? 90.0f : 270.0f;
            } else {
                x = roadStart + 1.0;
                z = z0 + along + 0.5;
                yaw = random.nextBoolean() ? 0.0f : 180.0f;
            }
            if (x < x0 + 1 || x > x0 + SIZE - 2 || z < z0 + 1 || z > z0 + SIZE - 2) continue;
            EntityType<CarEntity> type = random.nextInt(4) == 0 ? ModEntities.SPORTS_CAR : ModEntities.SEDAN;
            CarEntity car = type.create(world.toServerWorld());
            if (car == null) continue;
            car.refreshPositionAndAngles(x, y + 1, z, yaw, 0.0f);
            car.setHeadYaw(yaw);
            car.setBodyYaw(yaw);
            car.initFresh(random.nextInt(CarEntity.COLORS.length));
            car.setCivilian(true);
            world.spawnEntityAndPassengers(car);
        }
        int people = 2 + random.nextInt(4);
        for (int p = 0; p < people; p++) {
            VillagerEntity citizen = EntityType.VILLAGER.create(world.toServerWorld());
            if (citizen == null) continue;
            int cx = x0 + Math.floorMod((Math.floorDiv(x0 + OFF, PITCH) * PITCH - OFF) + ROAD - x0, PITCH) + random.nextInt(PITCH - ROAD) ;
            double px = Math.min(x0 + SIZE - 2, Math.max(x0 + 1, cx)) + 0.5;
            double pz = (Math.floorDiv(z0 + OFF, PITCH) * PITCH - OFF) + ROAD + 0.5;
            if (pz < z0 + 1) pz += PITCH;
            citizen.refreshPositionAndAngles(px, y + 1, pz, random.nextFloat() * 360.0f, 0.0f);
            citizen.setPersistent();
            world.spawnEntityAndPassengers(citizen);
        }
    }

    private static Lot pickLot(int lotX, int lotZ, Random random) {
        int r = random.nextInt(100);
        if (r < 30) return Lot.TOWER;
        if (r < 50) return Lot.TOWER2;
        if (r < 64) return Lot.SHOP;
        if (r < 74) return Lot.JOBS;
        if (r < 82) return Lot.CHOP_SHOP;
        if (r < 88) return Lot.POLICE;
        return Lot.PARK;
    }

    private static void buildLot(ServerWorldAccess world, Lot type, int x0, int y, int z0, int size, Random random, BlockPos.Mutable m) {
        switch (type) {
            case TOWER -> building(world, x0 + 1, y, z0 + 1, size - 2, size - 2, 14 + random.nextInt(18),
                    FACADES[random.nextInt(FACADES.length)], Blocks.LIGHT_BLUE_STAINED_GLASS.getDefaultState(), m);
            case TOWER2 -> building(world, x0 + 2, y, z0 + 2, size - 4, size - 4, 8 + random.nextInt(10),
                    FACADES[random.nextInt(FACADES.length)], Blocks.GLASS.getDefaultState(), m);
            case SHOP -> {
                building(world, x0 + 3, y, z0 + 3, size - 6, size - 6, 5, Blocks.WHITE_CONCRETE.getDefaultState(),
                        Blocks.GLASS.getDefaultState(), m);
                sign(world, x0 + 3, y + 5, z0 + 3, size - 6, Blocks.LIME_CONCRETE.getDefaultState(), m);
                set(world, m.set(x0 + size / 2, y + 1, z0 + size / 2),
                        ModBlocks.SHOP_TERMINAL.getDefaultState().with(ShopTerminalBlock.FACING, Direction.NORTH));
            }
            case JOBS -> {
                building(world, x0 + 3, y, z0 + 3, size - 6, size - 6, 6, Blocks.BRICKS.getDefaultState(),
                        Blocks.GLASS.getDefaultState(), m);
                sign(world, x0 + 3, y + 6, z0 + 3, size - 6, Blocks.ORANGE_CONCRETE.getDefaultState(), m);
                set(world, m.set(x0 + size / 2, y + 1, z0 + size / 2), ModBlocks.JOB_BOARD.getDefaultState());
            }
            case CHOP_SHOP -> {
                // open-fronted garage
                building(world, x0 + 2, y, z0 + 2, size - 4, size - 4, 6, Blocks.GRAY_CONCRETE.getDefaultState(),
                        Blocks.IRON_BARS.getDefaultState(), m);
                for (int dx = 4; dx < size - 4; dx++) {
                    for (int h = 1; h <= 4; h++) set(world, m.set(x0 + dx, y + h, z0 + 2), Blocks.AIR.getDefaultState());
                }
                for (int dx = 2; dx < size - 2; dx++) {
                    for (int dz = 2; dz < size - 2; dz++) set(world, m.set(x0 + dx, y, z0 + dz), ModBlocks.ASPHALT.getDefaultState());
                }
                set(world, m.set(x0 + size / 2, y + 1, z0 + size - 4), ModBlocks.CHOP_SHOP.getDefaultState());
            }
            case POLICE -> {
                building(world, x0 + 2, y, z0 + 2, size - 4, size - 4, 7, Blocks.BLUE_CONCRETE.getDefaultState(),
                        Blocks.GLASS.getDefaultState(), m);
                sign(world, x0 + 2, y + 7, z0 + 2, size - 4, Blocks.WHITE_CONCRETE.getDefaultState(), m);
                CarEntity cruiser = ModEntities.POLICE_CAR.create(world.toServerWorld());
                if (cruiser != null) {
                    cruiser.refreshPositionAndAngles(x0 + size / 2.0, y + 1, z0 - 1.5, 90.0f, 0.0f);
                    cruiser.initFresh(0);
                    world.spawnEntityAndPassengers(cruiser);
                }
            }
            case PARK -> {
                for (int dx = 0; dx < size; dx++) {
                    for (int dz = 0; dz < size; dz++) {
                        if (random.nextInt(9) == 0) set(world, m.set(x0 + dx, y + 1, z0 + dz), Blocks.SHORT_GRASS.getDefaultState());
                        else if (random.nextInt(25) == 0) set(world, m.set(x0 + dx, y + 1, z0 + dz), Blocks.POPPY.getDefaultState());
                    }
                }
                for (int t = 0; t < 3; t++) tree(world, x0 + 3 + random.nextInt(size - 6), y, z0 + 3 + random.nextInt(size - 6), m);
                set(world, m.set(x0 + size / 2, y + 1, z0 + size / 2), Blocks.OAK_STAIRS.getDefaultState());
            }
        }
    }

    /** Hollow box with a door gap on the north side, windows every other floor band, lit inside, flat roof. */
    private static void building(ServerWorldAccess world, int x0, int y, int z0, int w, int d, int h,
                                 BlockState wall, BlockState window, BlockPos.Mutable m) {
        BlockState floor = Blocks.POLISHED_ANDESITE.getDefaultState();
        BlockState light = Blocks.SEA_LANTERN.getDefaultState();
        for (int dx = 0; dx < w; dx++) {
            for (int dz = 0; dz < d; dz++) {
                boolean edge = dx == 0 || dz == 0 || dx == w - 1 || dz == d - 1;
                boolean corner = (dx == 0 || dx == w - 1) && (dz == 0 || dz == d - 1);
                set(world, m.set(x0 + dx, y, z0 + dz), floor);
                for (int hh = 1; hh <= h; hh++) {
                    if (edge) {
                        boolean win = !corner && hh % 4 != 0 && hh % 4 != 1 && (dx + dz) % 3 != 0;
                        set(world, m.set(x0 + dx, y + hh, z0 + dz), win ? window : wall);
                    }
                }
                set(world, m.set(x0 + dx, y + h + 1, z0 + dz), wall);
                if (!edge && dx % 4 == 2 && dz % 4 == 2) set(world, m.set(x0 + dx, y + Math.min(h, 4), z0 + dz), light);
            }
        }
        // front door (north side, middle)
        int door = x0 + w / 2;
        for (int hh = 1; hh <= 3; hh++) {
            set(world, m.set(door, y + hh, z0), Blocks.AIR.getDefaultState());
            set(world, m.set(door + 1, y + hh, z0), Blocks.AIR.getDefaultState());
        }
    }

    private static void sign(ServerWorldAccess world, int x0, int y, int z0, int w, BlockState color, BlockPos.Mutable m) {
        for (int dx = 1; dx < w - 1; dx++) set(world, m.set(x0 + dx, y, z0 - 1), color);
    }

    private static void streetLight(ServerWorldAccess world, int x, int y, int z, BlockPos.Mutable m) {
        for (int h = 1; h <= 4; h++) set(world, m.set(x, y + h, z), Blocks.IRON_BARS.getDefaultState());
        set(world, m.set(x, y + 5, z), Blocks.LANTERN.getDefaultState().with(LanternBlock.HANGING, false));
    }

    private static void tree(ServerWorldAccess world, int x, int y, int z, BlockPos.Mutable m) {
        for (int h = 1; h <= 4; h++) set(world, m.set(x, y + h, z), Blocks.OAK_LOG.getDefaultState());
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int h = 4; h <= 6; h++) {
                    if (Math.abs(dx) + Math.abs(dz) + (h - 4) <= 3 && !(dx == 0 && dz == 0 && h < 5)) {
                        set(world, m.set(x + dx, y + h, z + dz), Blocks.OAK_LEAVES.getDefaultState().with(LeavesBlock.PERSISTENT, true));
                    }
                }
            }
        }
    }

    private static void set(ServerWorldAccess world, BlockPos pos, BlockState state) {
        world.setBlockState(pos, state, Block.NOTIFY_LISTENERS);
    }
}
