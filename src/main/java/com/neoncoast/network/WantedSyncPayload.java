package com.neoncoast.network;

import com.neoncoast.NeonCoast;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.math.BlockPos;

public record WantedSyncPayload(int stars, boolean evading) implements CustomPayload {
    public static final CustomPayload.Id<WantedSyncPayload> ID = new CustomPayload.Id<>(NeonCoast.id("wanted"));
    public static final PacketCodec<RegistryByteBuf, WantedSyncPayload> CODEC = PacketCodec.tuple(PacketCodecs.VAR_INT, WantedSyncPayload::stars, PacketCodecs.BOOL, WantedSyncPayload::evading, WantedSyncPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
