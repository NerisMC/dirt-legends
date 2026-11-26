package com.dirtlegends.client;

import com.dirtlegends.DirtLegends;
import com.dirtlegends.entity.MotoVehicleEntity;
import com.dirtlegends.npc.RacerEntity;
import com.dirtlegends.npc.SpectatorEntity;
import com.dirtlegends.npc.VendorEntity;
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
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
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

        EntityRendererRegistry.register(ModEntities.SPECTATOR,
                ctx -> new NpcRenderer<>(ctx, "spectator", "fan", SpectatorEntity.SKINS, SpectatorEntity::getSkin));
        EntityRendererRegistry.register(ModEntities.RACER,
                ctx -> new NpcRenderer<>(ctx, "racer", "racer", RacerEntity.SKINS, RacerEntity::getSkin));
        EntityRendererRegistry.register(ModEntities.VENDOR,
                ctx -> new NpcRenderer<>(ctx, "vendor", "vendor", 2, VendorEntity::getKind));

        HudElementRegistry.addFirst(DirtLegends.id("helmet_visor"), HelmetVisor::render);
        HudElementRegistry.addLast(DirtLegends.id("vehicle_hud"), VehicleHud::render);
        HelmetVisor.tearOffKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.dirtlegends.tear_off",
                InputConstants.KEY_V, KeyMapping.Category.GAMEPLAY));
        ClientTickEvents.END_CLIENT_TICK.register(HelmetVisor::tick);

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
