package com.neoncoast.network;

import com.neoncoast.NeonCoast;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.math.BlockPos;

public record JobSyncPayload(boolean active, BlockPos target) implements CustomPayload {
    public static final CustomPayload.Id<JobSyncPayload> ID = new CustomPayload.Id<>(NeonCoast.id("job"));
    public static final PacketCodec<RegistryByteBuf, JobSyncPayload> CODEC = PacketCodec.tuple(PacketCodecs.BOOL, JobSyncPayload::active, BlockPos.PACKET_CODEC, JobSyncPayload::target, JobSyncPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
