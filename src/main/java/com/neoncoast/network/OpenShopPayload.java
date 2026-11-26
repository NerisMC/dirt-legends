package com.neoncoast.network;

import com.neoncoast.NeonCoast;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.math.BlockPos;

public record OpenShopPayload(BlockPos pos) implements CustomPayload {
    public static final CustomPayload.Id<OpenShopPayload> ID = new CustomPayload.Id<>(NeonCoast.id("open_shop"));
    public static final PacketCodec<RegistryByteBuf, OpenShopPayload> CODEC = PacketCodec.tuple(BlockPos.PACKET_CODEC, OpenShopPayload::pos, OpenShopPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
