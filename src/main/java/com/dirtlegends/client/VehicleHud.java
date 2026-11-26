package com.dirtlegends.client;

import com.dirtlegends.entity.MotoVehicleEntity;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/** Speedometer, gear indicator and tachometer next to the hotbar while riding. */
public final class VehicleHud {
    private VehicleHud() {
    }

    public static void render(GuiGraphics context, DeltaTracker tickCounter) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.options.hideGui) return;
        if (!(client.player.getVehicle() instanceof MotoVehicleEntity vehicle)) return;

        Font text = client.font;
        int x = context.guiWidth() / 2 + 98;
        int y = context.guiHeight() - 46;
        float rpm = vehicle.getRpm();

        context.drawString(text, Math.round(vehicle.getSpeedKmh()) + " km/h", x, y, 0xFFFFFF);
        context.drawString(text, "Gear " + vehicle.getGear(), x, y + 10, 0xFFD24A);
        int width = 60;
        context.fill(x, y + 21, x + width, y + 26, 0x90000000);
        int color = rpm > 0.9f ? 0xFFFF4040 : rpm > 0.7f ? 0xFFFFC040 : 0xFF40E060;
        context.fill(x, y + 21, x + Math.round(width * clamp01(rpm)), y + 26, color);

        // fuel gauge
        float fuel = clamp01(vehicle.getFuel() / MotoVehicleEntity.MAX_FUEL);
        boolean low = fuel < 0.15f;
        boolean blink = low && (client.player.tickCount / 5) % 2 == 0;
        context.drawString(text, "FUEL", x, y + 30, blink ? 0xFF5050 : 0xFFFFFF);
        context.fill(x + 26, y + 31, x + 26 + 34, y + 37, 0x90000000);
        int fuelColor = fuel < 0.15f ? 0xFFFF4040 : fuel < 0.4f ? 0xFFFFC040 : 0xFF40E060;
        context.fill(x + 26, y + 31, x + 26 + Math.round(34 * fuel), y + 37, fuelColor);
    }

    private static float clamp01(float v) {
        return Math.max(0.0f, Math.min(1.0f, v));
    }
}
