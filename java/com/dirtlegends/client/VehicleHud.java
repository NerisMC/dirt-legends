package com.dirtlegends.client;

import com.dirtlegends.entity.MotoVehicleEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

/** Speedometer, gear indicator and tachometer next to the hotbar while riding. */
public final class VehicleHud {
    private VehicleHud() {
    }

    public static void render(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden) return;
        if (!(client.player.getVehicle() instanceof MotoVehicleEntity vehicle)) return;

        TextRenderer text = client.textRenderer;
        int x = context.getScaledWindowWidth() / 2 + 98;
        int y = context.getScaledWindowHeight() - 36;
        float rpm = vehicle.getRpm();

        context.drawTextWithShadow(text, Math.round(vehicle.getSpeedKmh()) + " km/h", x, y, 0xFFFFFF);
        context.drawTextWithShadow(text, "Gear " + vehicle.getGear(), x, y + 10, 0xFFD24A);
        int width = 60;
        context.fill(x, y + 21, x + width, y + 26, 0x90000000);
        int color = rpm > 0.9f ? 0xFFFF4040 : rpm > 0.7f ? 0xFFFFC040 : 0xFF40E060;
        context.fill(x, y + 21, x + Math.round(width * clamp01(rpm)), y + 26, color);
    }

    private static float clamp01(float v) {
        return Math.max(0.0f, Math.min(1.0f, v));
    }
}
