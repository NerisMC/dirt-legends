package com.neoncoast.client;

import net.minecraft.util.math.BlockPos;

/** What the server has told this client about its own player. */
public final class ClientState {
    public static int stars;
    public static boolean evading;
    public static long money = -1;
    public static boolean jobActive;
    public static BlockPos jobTarget = BlockPos.ORIGIN;

    private ClientState() {
    }
}
