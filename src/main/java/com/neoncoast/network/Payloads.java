package com.neoncoast.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public final class Payloads {
    private Payloads() {
    }

    public static void register() {
        PayloadTypeRegistry.playS2C().register(WantedSyncPayload.ID, WantedSyncPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(MoneySyncPayload.ID, MoneySyncPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(JobSyncPayload.ID, JobSyncPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(OpenShopPayload.ID, OpenShopPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(BuyPayload.ID, BuyPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(CarCrashPayload.ID, CarCrashPayload.CODEC);
    }
}
