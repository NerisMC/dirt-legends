package com.neoncoast.crime;

import com.neoncoast.npc.PoliceOfficerEntity;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Box;

/** Turns things you do into wanted stars. */
public final class CrimeHooks {
    private CrimeHooks() {
    }

    public static void register() {
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (!world.isClient && player instanceof ServerPlayerEntity sp && !sp.isCreative()) {
                if (entity instanceof PoliceOfficerEntity) crime(sp, 2, "Assaulting an officer");
                else if (entity instanceof VillagerEntity) witnessedCrime(sp, 1, "Assault");
            }
            return ActionResult.PASS;
        });
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayerEntity dead) {
                int stars = WantedManager.getStars(dead);
                WantedManager.clear(dead);
                long bill = Math.min(1000, Math.max(50, com.neoncoast.economy.Money.get(dead) / 10));
                com.neoncoast.economy.Money.add(dead, -bill);
                dead.sendMessage(Text.literal("Hospital bill: $" + bill + (stars > 0 ? " (wanted level cleared)" : "")).formatted(Formatting.RED), false);
                return;
            }
            if (source.getAttacker() instanceof ServerPlayerEntity killer && !killer.isCreative()) {
                if (entity instanceof PoliceOfficerEntity) {
                    WantedManager.setAtLeast(killer, 3);
                    crime(killer, 1, "Killed an officer");
                } else if (entity instanceof VillagerEntity) {
                    crime(killer, 1, "Murder");
                }
            }
        });
    }

    /** A crime the police always hear about. */
    public static void crime(ServerPlayerEntity player, int stars, String what) {
        if (player.isCreative()) return;
        WantedManager.addStars(player, stars);
        player.sendMessage(Text.literal(what + "! Wanted level: " + WantedManager.getStars(player))
                .formatted(Formatting.RED, Formatting.BOLD), true);
    }

    /** A crime that only counts if a cop or a citizen saw it. */
    public static void witnessedCrime(ServerPlayerEntity player, int stars, String what) {
        if (hasWitness(player)) crime(player, stars, what);
    }

    public static void hitWithCar(ServerPlayerEntity driver, LivingEntity victim) {
        if (victim instanceof PoliceOfficerEntity) crime(driver, 2, "Ran over an officer");
        else if (victim instanceof VillagerEntity) witnessedCrime(driver, 1, "Hit and run");
    }

    private static boolean hasWitness(ServerPlayerEntity player) {
        Box near = player.getBoundingBox().expand(16.0);
        Box far = player.getBoundingBox().expand(32.0);
        for (Entity e : player.getWorld().getOtherEntities(player, far, e -> e instanceof PoliceOfficerEntity || e instanceof VillagerEntity)) {
            if (e instanceof PoliceOfficerEntity) return true;
            if (near.intersects(e.getBoundingBox()) && e instanceof LivingEntity l && l.canSee(player)) return true;
        }
        return false;
    }
}
