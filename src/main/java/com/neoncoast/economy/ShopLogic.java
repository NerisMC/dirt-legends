package com.neoncoast.economy;

import com.neoncoast.crime.CrimeHooks;
import com.neoncoast.crime.WantedManager;
import com.neoncoast.registry.ModBlocks;
import com.neoncoast.registry.ModItems;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;

public final class ShopLogic {
    private static final Map<BlockPos, Long> LAST_ROBBED = new HashMap<>();

    private ShopLogic() {
    }

    public static void buy(ServerPlayerEntity player, int index, BlockPos shop) {
        if (index < 0 || index >= ShopCatalog.NAMES.length) return;
        if (player.squaredDistanceTo(shop.toCenterPos()) > 64.0 || !player.getWorld().getBlockState(shop).isOf(ModBlocks.SHOP_TERMINAL)) return;

        if (index == ShopCatalog.ROB) {
            long now = player.getWorld().getTime();
            Long last = LAST_ROBBED.get(shop);
            if (last != null && now - last < 6000) {
                say(player, "The register is empty. Come back later.", Formatting.GRAY);
                return;
            }
            LAST_ROBBED.put(shop.toImmutable(), now);
            long take = 300 + player.getRandom().nextInt(500);
            Money.add(player, take);
            player.playSound(SoundEvents.BLOCK_CHAIN_BREAK, 1.0f, 1.2f);
            CrimeHooks.crime(player, 2, "Armed robbery (+$" + take + ")");
            return;
        }
        if (index == ShopCatalog.PAY_OFF && WantedManager.getStars(player) == 0) {
            say(player, "Nobody's looking for you.", Formatting.GRAY);
            return;
        }
        long price = ShopCatalog.PRICES[index];
        if (!Money.spend(player, price)) {
            say(player, "Not enough cash ($" + price + ")", Formatting.RED);
            return;
        }
        switch (index) {
            case 0 -> give(player, new ItemStack(ModItems.SEDAN_KEY));
            case 1 -> give(player, new ItemStack(ModItems.SPORTS_KEY));
            case 2 -> {
                ItemStack armor = new ItemStack(Items.IRON_CHESTPLATE);
                armor.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Body Armor").formatted(Formatting.AQUA));
                give(player, armor);
            }
            case 3 -> give(player, new ItemStack(ModItems.REPAIR_KIT));
            case 4 -> give(player, new ItemStack(Items.COOKED_BEEF, 8));
            case 5 -> WantedManager.clear(player);
            default -> { }
        }
        player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.2f);
        say(player, "Bought " + ShopCatalog.NAMES[index] + " for $" + price, Formatting.GREEN);
    }

    private static void give(ServerPlayerEntity player, ItemStack stack) {
        if (!player.getInventory().insertStack(stack)) player.dropItem(stack, false);
    }

    private static void say(ServerPlayerEntity player, String msg, Formatting color) {
        player.sendMessage(Text.literal(msg).formatted(color), true);
    }
}
