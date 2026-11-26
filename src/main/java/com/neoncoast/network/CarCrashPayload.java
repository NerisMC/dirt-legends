package com.neoncoast.network;

import com.neoncoast.NeonCoast;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.math.BlockPos;

public record CarCrashPayload(float impact) implements CustomPayload {
    public static final CustomPayload.Id<CarCrashPayload> ID = new CustomPayload.Id<>(NeonCoast.id("car_crash"));
    public static final PacketCodec<RegistryByteBuf, CarCrashPayload> CODEC = PacketCodec.tuple(PacketCodecs.FLOAT, CarCrashPayload::impact, CarCrashPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
