package com.dirtlegends.item;

import com.dirtlegends.world.TrackGenerator;
import net.minecraft.item.Item;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;

/** Right-click the ground to build a full motocross track around that spot. */
public class TrackKitItem extends Item {
    public TrackKitItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if (!(context.getWorld() instanceof ServerWorld world)) {
            return ActionResult.SUCCESS;
        }
        TrackGenerator.generate(world, context.getBlockPos(), world.getRandom());
        if (context.getPlayer() != null) {
            context.getPlayer().sendMessage(Text.literal("Track built! Start line is on the east side.").formatted(Formatting.GREEN), true);
        }
        context.getStack().decrementUnlessCreative(1, context.getPlayer());
        return ActionResult.CONSUME;
    }
}
