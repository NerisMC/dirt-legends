package com.neoncoast.client;

import com.neoncoast.vehicle.CarEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.math.MathHelper;

/** Wanted stars and cash (top right), delivery directions (top middle), car gauges (bottom right). */
public final class Hud {
    private static final String[] ARROWS = {"↑", "↗", "→", "↘", "↓", "↙", "←", "↖"};

    private Hud() {
    }

    public static String format(long v) {
        return v < 0 ? "..." : String.format("%,d", v);
    }

    public static void render(DrawContext ctx, RenderTickCounter tickCounter) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.options.hudHidden) return;
        TextRenderer tr = mc.textRenderer;
        int w = ctx.getScaledWindowWidth(), h = ctx.getScaledWindowHeight();

        // wanted level: 5 stars, flashing while you're out of sight of the cops
        boolean flashOff = ClientState.evading && (mc.player.age / 6) % 2 == 0;
        for (int i = 0; i < 5; i++) {
            boolean lit = i < ClientState.stars && !flashOff;
            int color = lit ? 0xFFE040 : 0x50FFFFFF;
            ctx.drawTextWithShadow(tr, "★", w - 14 - (4 - i) * 11, 6, color);
        }
        if (ClientState.money >= 0) {
            String cash = "$" + format(ClientState.money);
            ctx.drawTextWithShadow(tr, cash, w - 6 - tr.getWidth(cash), 18, 0x55FF55);
        }

        // delivery job
        if (ClientState.jobActive) {
            double dx = ClientState.jobTarget.getX() + 0.5 - mc.player.getX();
            double dz = ClientState.jobTarget.getZ() + 0.5 - mc.player.getZ();
            int dist = (int) Math.sqrt(dx * dx + dz * dz);
            float bearing = (float) Math.toDegrees(Math.atan2(-dx, dz));
            float rel = MathHelper.wrapDegrees(bearing - mc.player.getYaw());
            int idx = Math.floorMod(Math.round(rel / 45.0f), 8);
            String text = "DELIVERY  " + ARROWS[idx] + "  " + dist + "m";
            ctx.drawTextWithShadow(tr, text, (w - tr.getWidth(text)) / 2, 6, 0xFFB030);
        }

        // car gauges
        if (mc.player.getVehicle() instanceof CarEntity car) {
            int x = w / 2 + 98, y = h - 46;
            ctx.drawTextWithShadow(tr, Math.round(car.getSpeedKmh()) + " km/h", x, y, 0xFFFFFF);
            ctx.drawTextWithShadow(tr, "Gear " + car.getGear(), x, y + 10, 0xFFD24A);
            float rpm = MathHelper.clamp(car.getRpm(), 0.0f, 1.0f);
            ctx.fill(x, y + 21, x + 60, y + 26, 0x90000000);
            ctx.fill(x, y + 21, x + Math.round(60 * rpm), y + 26, rpm > 0.9f ? 0xFFFF4040 : rpm > 0.7f ? 0xFFFFC040 : 0xFF40E060);
            float hp = MathHelper.clamp(car.getCarHealth() / car.getCarType().maxHealth(), 0.0f, 1.0f);
            ctx.drawTextWithShadow(tr, "CAR", x, y + 30, hp < 0.25f ? 0xFF5050 : 0xFFFFFF);
            ctx.fill(x + 22, y + 31, x + 60, y + 37, 0x90000000);
            ctx.fill(x + 22, y + 31, x + 22 + Math.round(38 * hp), y + 37, hp < 0.25f ? 0xFFFF4040 : hp < 0.5f ? 0xFFFFC040 : 0xFF40A0FF);
        }
    }
}
