package com.dirtlegends.item;

import com.dirtlegends.world.TrackGenerator;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

/** Right-click the ground to build a full motocross track around that spot. */
public class TrackKitItem extends Item {
    public TrackKitItem(Properties settings) {
        super(settings);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel world)) {
            return InteractionResult.SUCCESS;
        }
        TrackGenerator.generate(world, context.getClickedPos(), world.getRandom());
        if (context.getPlayer() != null) {
            context.getPlayer().displayClientMessage(Component.literal("Track built! Start line is on the east side.").withStyle(ChatFormatting.GREEN), true);
        }
        context.getItemInHand().consume(1, context.getPlayer());
        return InteractionResult.CONSUME;
    }
}
