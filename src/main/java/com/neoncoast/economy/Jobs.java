package com.neoncoast.economy;

import com.neoncoast.network.JobSyncPayload;
import com.neoncoast.registry.ModItems;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.Heightmap;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Delivery jobs: grab a package at a Job Board, drive it to the marked spot, get paid. */
public final class Jobs {
    private static final Map<UUID, Job> ACTIVE = new HashMap<>();

    private record Job(BlockPos target, long pay) {
    }

    private Jobs() {
    }

    public static void takeJob(ServerPlayerEntity player) {
        Job current = ACTIVE.get(player.getUuid());
        if (current != null) {
            player.sendMessage(Text.literal("You already have a delivery: " + describe(player, current.target)).formatted(Formatting.YELLOW), false);
            return;
        }
        ServerWorld world = player.getServerWorld();
        float angle = world.getRandom().nextFloat() * MathHelper.TAU;
        int dist = 120 + world.getRandom().nextInt(200);
        int x = MathHelper.floor(player.getX() + MathHelper.cos(angle) * dist);
        int z = MathHelper.floor(player.getZ() + MathHelper.sin(angle) * dist);
        int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
        BlockPos target = new BlockPos(x, y, z);
        long pay = 150 + dist;
        ACTIVE.put(player.getUuid(), new Job(target, pay));
        ItemStack box = new ItemStack(ModItems.PACKAGE);
        if (!player.getInventory().insertStack(box)) player.dropItem(box, false);
        player.sendMessage(Text.literal("Delivery job: take the package to " + x + ", " + z + " ($" + pay + ")").formatted(Formatting.GOLD, Formatting.BOLD), false);
        sync(player);
    }

    /** Called once a second. */
    public static void tick(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            Job job = ACTIVE.get(player.getUuid());
            if (job == null) continue;
            double dx = player.getX() - (job.target.getX() + 0.5), dz = player.getZ() - (job.target.getZ() + 0.5);
            if (dx * dx + dz * dz > 6 * 6) continue;
            int slot = player.getInventory().getSlotWithStack(new ItemStack(ModItems.PACKAGE));
            if (slot < 0) {
                player.sendMessage(Text.literal("You lost the package! Job cancelled.").formatted(Formatting.RED), true);
            } else {
                player.getInventory().getStack(slot).decrement(1);
                Money.add(player, job.pay);
                player.playSound(SoundEvents.ENTITY_PLAYER_LEVELUP, 0.8f, 1.2f);
                player.sendMessage(Text.literal("Delivered! +$" + job.pay).formatted(Formatting.GREEN, Formatting.BOLD), true);
            }
            ACTIVE.remove(player.getUuid());
            sync(player);
        }
    }

    public static void sync(ServerPlayerEntity player) {
        Job job = ACTIVE.get(player.getUuid());
        ServerPlayNetworking.send(player, new JobSyncPayload(job != null, job != null ? job.target : BlockPos.ORIGIN));
    }

    private static String describe(ServerPlayerEntity player, BlockPos t) {
        int d = (int) Math.sqrt(player.squaredDistanceTo(t.toCenterPos()));
        return t.getX() + ", " + t.getZ() + " (" + d + "m away)";
    }
}
