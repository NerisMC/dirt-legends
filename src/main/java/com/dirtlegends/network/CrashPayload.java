package com.dirtlegends.network;

import com.dirtlegends.DirtLegends;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/** Rider's client -> server: "I just crashed this hard". The server clamps and applies it. */
public record CrashPayload(float severity) implements CustomPayload {
    public static final CustomPayload.Id<CrashPayload> ID = new CustomPayload.Id<>(DirtLegends.id("crash"));
    public static final PacketCodec<RegistryByteBuf, CrashPayload> CODEC =
            PacketCodec.tuple(PacketCodecs.FLOAT, CrashPayload::severity, CrashPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
