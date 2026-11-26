package com.neoncoast.crime;

import com.neoncoast.network.WantedSyncPayload;
import com.neoncoast.npc.PoliceOfficerEntity;
import com.neoncoast.vehicle.CarEntity;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Box;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Wanted level (0-5 stars) per player. Stars go up with crimes. Once no cop can see you
 * for long enough (longer at higher levels), you lose them and the stars clear.
 */
public final class WantedManager {
    public static final int MAX_STARS = 5;
    private static final Map<UUID, State> STATES = new HashMap<>();

    private static final class State {
        int stars;
        long lastSeen;
        boolean evading;
    }

    private WantedManager() {
    }

    public static int getStars(PlayerEntity player) {
        State s = STATES.get(player.getUuid());
        return s == null ? 0 : s.stars;
    }

    public static void addStars(ServerPlayerEntity player, int amount) {
        State s = STATES.computeIfAbsent(player.getUuid(), u -> new State());
        int before = s.stars;
        s.stars = Math.min(MAX_STARS, s.stars + amount);
        s.lastSeen = player.getServerWorld().getTime();
        s.evading = false;
        if (s.stars != before) sync(player, s);
    }

    public static void setAtLeast(ServerPlayerEntity player, int stars) {
        int now = getStars(player);
        if (stars > now) addStars(player, stars - now);
    }

    public static void clear(ServerPlayerEntity player) {
        State s = STATES.get(player.getUuid());
        if (s != null && s.stars > 0) {
            s.stars = 0;
            s.evading = false;
            sync(player, s);
        }
    }

    /** Called every second from the server tick. */
    public static void tick(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            State s = STATES.get(player.getUuid());
            if (s == null || s.stars == 0) continue;
            long now = player.getServerWorld().getTime();
            if (isSeenByPolice(player)) {
                s.lastSeen = now;
                if (s.evading) {
                    s.evading = false;
                    sync(player, s);
                }
            } else {
                long hideFor = (8L + s.stars * 6L) * 20L;
                if (!s.evading && now - s.lastSeen > 60) {
                    s.evading = true;
                    sync(player, s);
                }
                if (now - s.lastSeen > hideFor) {
                    s.stars = 0;
                    s.evading = false;
                    sync(player, s);
                    player.sendMessage(Text.literal("You lost the cops.").formatted(Formatting.GREEN, Formatting.BOLD), true);
                }
            }
            PoliceSpawner.tick(player, s.stars);
        }
    }

    private static boolean isSeenByPolice(ServerPlayerEntity player) {
        Box area = player.getBoundingBox().expand(40.0);
        for (Entity e : player.getWorld().getOtherEntities(player, area, e -> e instanceof PoliceOfficerEntity || e instanceof CarEntity car && car.isPolice())) {
            if (e instanceof PoliceOfficerEntity cop && cop.canSee(player)) return true;
            if (e instanceof CarEntity && e.squaredDistanceTo(player) < 30 * 30) return true;
        }
        return false;
    }

    @Nullable
    public static PlayerEntity nearestWanted(Entity from, double range) {
        PlayerEntity best = null;
        double bestD = range * range;
        for (PlayerEntity p : from.getWorld().getPlayers()) {
            if (getStars(p) <= 0 || p.isSpectator() || p.isCreative()) continue;
            double d = p.squaredDistanceTo(from);
            if (d < bestD) {
                bestD = d;
                best = p;
            }
        }
        return best;
    }

    private static void sync(ServerPlayerEntity player, State s) {
        ServerPlayNetworking.send(player, new WantedSyncPayload(s.stars, s.evading));
    }
}
