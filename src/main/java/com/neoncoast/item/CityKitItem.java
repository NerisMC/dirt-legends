package com.neoncoast.item;

import com.neoncoast.world.CityGenerator;
import net.minecraft.item.Item;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

/** Builds a 2x2 block of city districts (96x96) with its corner where you click. */
public class CityKitItem extends Item {
    public CityKitItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if (!(context.getWorld() instanceof ServerWorld world)) return ActionResult.SUCCESS;
        BlockPos at = context.getBlockPos();
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                CityGenerator.generateDistrict(world, CityGenerator.snap(at.getX()) + i * CityGenerator.SIZE, at.getY(), CityGenerator.snap(at.getZ()) + j * CityGenerator.SIZE, world.getRandom());
            }
        }
        if (context.getPlayer() != null) {
            context.getPlayer().sendMessage(Text.literal("City built!").formatted(Formatting.GREEN, Formatting.BOLD), true);
        }
        context.getStack().decrementUnlessCreative(1, context.getPlayer());
        return ActionResult.CONSUME;
    }
}
