package com.neoncoast.client;

import com.neoncoast.economy.ShopCatalog;
import com.neoncoast.network.BuyPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

public class ShopScreen extends Screen {
    private final BlockPos shop;

    public ShopScreen(BlockPos shop) {
        super(Text.literal("24/7 Store"));
        this.shop = shop;
    }

    @Override
    protected void init() {
        int w = 220, h = 20, gap = 4;
        int x = (this.width - w) / 2;
        int y = this.height / 2 - (ShopCatalog.NAMES.length * (h + gap)) / 2 + 10;
        for (int i = 0; i < ShopCatalog.NAMES.length; i++) {
            final int index = i;
            Text label = i == ShopCatalog.ROB
                    ? Text.literal("ROB THE STORE  (+2 wanted stars)").formatted(Formatting.RED, Formatting.BOLD)
                    : Text.literal(ShopCatalog.NAMES[i] + "  -  $" + ShopCatalog.PRICES[i]);
            this.addDrawableChild(ButtonWidget.builder(label, b -> {
                ClientPlayNetworking.send(new BuyPayload(index, shop));
                if (index == ShopCatalog.ROB) this.close();
            }).dimensions(x, y + i * (h + gap), w, h).build());
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        int top = this.height / 2 - (ShopCatalog.NAMES.length * 24) / 2 - 22;
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, top, 0xFFD23F);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Cash: $" + Hud.format(ClientState.money)), this.width / 2, top + 12, 0x55FF55);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
