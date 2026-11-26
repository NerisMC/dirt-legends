package com.neoncoast.economy;

import com.neoncoast.network.MoneySyncPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;

public final class Money {
    public static final long STARTING_CASH = 500;

    private Money() {
    }

    private static MoneyState state(ServerPlayerEntity player) {
        return player.getServer().getOverworld().getPersistentStateManager().getOrCreate(MoneyState.TYPE, "neoncoast_money");
    }

    public static long get(ServerPlayerEntity player) {
        return state(player).balances.getOrDefault(player.getUuid(), STARTING_CASH);
    }

    public static void add(ServerPlayerEntity player, long delta) {
        MoneyState s = state(player);
        long now = Math.max(0, s.balances.getOrDefault(player.getUuid(), STARTING_CASH) + delta);
        s.balances.put(player.getUuid(), now);
        s.markDirty();
        sync(player);
    }

    /** Takes the money if the player has it. */
    public static boolean spend(ServerPlayerEntity player, long amount) {
        if (get(player) < amount) return false;
        add(player, -amount);
        return true;
    }

    public static void sync(ServerPlayerEntity player) {
        ServerPlayNetworking.send(player, new MoneySyncPayload(get(player)));
    }
}
