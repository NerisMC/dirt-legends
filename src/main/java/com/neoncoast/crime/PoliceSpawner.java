package com.neoncoast.crime;

import com.neoncoast.npc.PoliceOfficerEntity;
import com.neoncoast.registry.ModEntities;
import com.neoncoast.vehicle.CarEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;

/** Sends cops after wanted players: more stars, more cops, and police cars from 2 stars up. */
public final class PoliceSpawner {
    private PoliceSpawner() {
    }

    static void tick(ServerPlayerEntity player, int stars) {
        ServerWorld world = player.getServerWorld();
        if (stars <= 0 || player.isCreative() || player.isSpectator()) return;
        if (player.getServer().getTicks() % 100 != 0) return; // every 5 seconds
        int officers = world.getEntitiesByClass(PoliceOfficerEntity.class, player.getBoundingBox().expand(64.0), e -> true).size();
        int cars = world.getEntitiesByClass(CarEntity.class, player.getBoundingBox().expand(96.0), CarEntity::isPolice).size();
        Random r = world.getRandom();
        if (officers < Math.min(2 * stars, 10)) {
            BlockPos pos = spot(world, player, r, 18, 28);
            PoliceOfficerEntity cop = ModEntities.POLICE_OFFICER.create(world);
            if (pos != null && cop != null) {
                cop.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, r.nextFloat() * 360.0f, 0.0f);
                cop.initialize(world, world.getLocalDifficulty(pos), SpawnReason.EVENT, null);
                world.spawnEntity(cop);
            }
        }
        if (stars >= 2 && cars < stars - 1) {
            BlockPos pos = spot(world, player, r, 40, 60);
            CarEntity car = ModEntities.POLICE_CAR.create(world);
            PoliceOfficerEntity driver = ModEntities.POLICE_OFFICER.create(world);
            if (pos != null && car != null && driver != null) {
                float yaw = (float) Math.toDegrees(Math.atan2(-(player.getX() - pos.getX()), player.getZ() - pos.getZ()));
                car.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, yaw, 0.0f);
                car.initFresh(0);
                if (world.isSpaceEmpty(car)) {
                    world.spawnEntity(car);
                    driver.refreshPositionAndAngles(car.getX(), car.getY(), car.getZ(), yaw, 0.0f);
                    driver.initialize(world, world.getLocalDifficulty(pos), SpawnReason.EVENT, null);
                    world.spawnEntity(driver);
                    driver.startRiding(car, true);
                }
            }
        }
    }

    private static BlockPos spot(ServerWorld world, ServerPlayerEntity player, Random r, int min, int max) {
        for (int tries = 0; tries < 6; tries++) {
            float a = r.nextFloat() * MathHelper.TAU;
            int d = min + r.nextInt(max - min);
            int x = MathHelper.floor(player.getX() + MathHelper.cos(a) * d);
            int z = MathHelper.floor(player.getZ() + MathHelper.sin(a) * d);
            if (!world.isChunkLoaded(x >> 4, z >> 4)) continue;
            int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos pos = new BlockPos(x, y, z);
            if (world.getFluidState(pos.down()).isEmpty() && Math.abs(y - player.getY()) < 12) return pos;
        }
        return null;
    }
}
