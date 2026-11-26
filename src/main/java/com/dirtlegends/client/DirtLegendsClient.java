package com.dirtlegends.client;

import com.dirtlegends.DirtLegends;
import com.dirtlegends.entity.MotoVehicleEntity;
import com.dirtlegends.network.CrashPayload;
import com.dirtlegends.registry.ModEntities;
import com.dirtlegends.registry.ModFluids;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderingRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintSources;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.resources.Identifier;

public class DirtLegendsClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ModelLayerRegistry.registerModelLayer(DirtBikeModel.LAYER, DirtBikeModel::getTexturedModelData);
        ModelLayerRegistry.registerModelLayer(AtvModel.LAYER, AtvModel::getTexturedModelData);

        EntityRendererRegistry.register(ModEntities.DIRT_BIKE,
                context -> new VehicleRenderer(context, new DirtBikeModel(context.bakeLayer(DirtBikeModel.LAYER)), 0.6f, "dirt_bike"));
        EntityRendererRegistry.register(ModEntities.ATV,
                context -> new VehicleRenderer(context, new AtvModel(context.bakeLayer(AtvModel.LAYER)), 0.8f, "atv"));

        HudElementRegistry.addLast(DirtLegends.id("vehicle_hud"), VehicleHud::render);

        // liquid mud: vanilla water textures tinted dark brown, drawn translucent
        FluidRenderingRegistry.register(ModFluids.STILL_MUD, ModFluids.FLOWING_MUD, new FluidModel.Unbaked(
                new Material(Identifier.withDefaultNamespace("block/water_still"), true),
                new Material(Identifier.withDefaultNamespace("block/water_flow"), true),
                new Material(Identifier.withDefaultNamespace("block/water_overlay"), true),
                BlockTintSources.constant(0xFF5A3B22)));

        MotoVehicleEntity.crashSender = severity -> {
            if (ClientPlayNetworking.canSend(CrashPayload.ID)) {
                ClientPlayNetworking.send(new CrashPayload(severity));
            }
        };
        MotoVehicleEntity.engineSoundStarter = vehicle ->
                Minecraft.getInstance().getSoundManager().play(new EngineSoundInstance(vehicle));
    }
}
