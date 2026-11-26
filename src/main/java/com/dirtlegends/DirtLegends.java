package com.dirtlegends;

import com.dirtlegends.entity.MotoVehicleEntity;
import com.dirtlegends.network.CrashPayload;
import com.dirtlegends.registry.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DirtLegends implements ModInitializer {
    public static final String MOD_ID = "dirtlegends";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        ModSounds.init();
        ModEntities.init();
        ModBlocks.init();
        ModArmor.init();
        ModItems.init();
        ModItemGroup.init();

        PayloadTypeRegistry.playC2S().register(CrashPayload.ID, CrashPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(CrashPayload.ID, (payload, context) -> {
            if (context.player().getVehicle() instanceof MotoVehicleEntity vehicle) {
                vehicle.serverCrash(context.player(), payload.severity());
            }
        });
        LOGGER.info("Dirt Legends loaded - brap brap");
    }
}
