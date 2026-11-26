package com.neoncoast.network;

import com.neoncoast.NeonCoast;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.math.BlockPos;

public record BuyPayload(int index, BlockPos pos) implements CustomPayload {
    public static final CustomPayload.Id<BuyPayload> ID = new CustomPayload.Id<>(NeonCoast.id("buy"));
    public static final PacketCodec<RegistryByteBuf, BuyPayload> CODEC = PacketCodec.tuple(PacketCodecs.VAR_INT, BuyPayload::index, BlockPos.PACKET_CODEC, BuyPayload::pos, BuyPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
