package com.dirtlegends.client;

import com.dirtlegends.entity.MotoVehicleEntity;
import com.dirtlegends.network.CrashPayload;
import com.dirtlegends.registry.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import com.dirtlegends.registry.ModFluids;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry;
import net.fabricmc.fabric.api.client.render.fluid.v1.SimpleFluidRenderHandler;

public class DirtLegendsClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityModelLayerRegistry.registerModelLayer(DirtBikeModel.LAYER, DirtBikeModel::getTexturedModelData);
        EntityModelLayerRegistry.registerModelLayer(AtvModel.LAYER, AtvModel::getTexturedModelData);

        EntityRendererRegistry.<MotoVehicleEntity>register(ModEntities.DIRT_BIKE,
                context -> new VehicleRenderer<>(context, new DirtBikeModel(context.bakeLayer(DirtBikeModel.LAYER)), 0.6f, "dirt_bike"));
        EntityRendererRegistry.<MotoVehicleEntity>register(ModEntities.ATV,
                context -> new VehicleRenderer<>(context, new AtvModel(context.bakeLayer(AtvModel.LAYER)), 0.8f, "atv"));

        HudRenderCallback.EVENT.register(VehicleHud::render);

        // liquid mud: vanilla water textures tinted dark brown
        FluidRenderHandlerRegistry.INSTANCE.register(ModFluids.STILL_MUD, ModFluids.FLOWING_MUD,
                new SimpleFluidRenderHandler(ResourceLocation.withDefaultNamespace("block/water_still"), ResourceLocation.withDefaultNamespace("block/water_flow"), 0x5A3B22));
        BlockRenderLayerMap.INSTANCE.putFluids(RenderType.translucent(), ModFluids.STILL_MUD, ModFluids.FLOWING_MUD);

        MotoVehicleEntity.crashSender = severity -> {
            if (ClientPlayNetworking.canSend(CrashPayload.ID)) {
                ClientPlayNetworking.send(new CrashPayload(severity));
            }
        };
        MotoVehicleEntity.engineSoundStarter = vehicle ->
                Minecraft.getInstance().getSoundManager().play(new EngineSoundInstance(vehicle));
    }
}
