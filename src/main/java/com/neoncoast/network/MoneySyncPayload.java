package com.neoncoast.network;

import com.neoncoast.NeonCoast;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.math.BlockPos;

public record MoneySyncPayload(long money) implements CustomPayload {
    public static final CustomPayload.Id<MoneySyncPayload> ID = new CustomPayload.Id<>(NeonCoast.id("money"));
    public static final PacketCodec<RegistryByteBuf, MoneySyncPayload> CODEC = PacketCodec.tuple(PacketCodecs.VAR_LONG, MoneySyncPayload::money, MoneySyncPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
