package com.dirtlegends.network;

import com.dirtlegends.DirtLegends;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Rider's client -> server: "I just crashed this hard". The server clamps and applies it. */
public record CrashPayload(float severity) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<CrashPayload> ID = new CustomPacketPayload.Type<>(DirtLegends.id("crash"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CrashPayload> CODEC =
            StreamCodec.composite(ByteBufCodecs.FLOAT, CrashPayload::severity, CrashPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
