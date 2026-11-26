package com.neoncoast.item;

import com.neoncoast.vehicle.CarEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;

import java.util.function.Supplier;

/** Right-click the ground to park your car there. It's yours, so driving it isn't a crime. */
public class CarKeyItem extends Item {
    private final Supplier<EntityType<CarEntity>> type;

    public CarKeyItem(Settings settings, Supplier<EntityType<CarEntity>> type) {
        super(settings);
        this.type = type;
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if (!(context.getWorld() instanceof ServerWorld world)) return ActionResult.SUCCESS;
        BlockPos pos = context.getBlockPos().offset(context.getSide());
        CarEntity car = type.get().create(world);
        if (car == null) return ActionResult.FAIL;
        float yaw = context.getPlayerYaw();
        car.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, yaw, 0.0f);
        car.setHeadYaw(yaw);
        car.setBodyYaw(yaw);
        car.initFresh(world.getRandom().nextInt(CarEntity.COLORS.length));
        if (context.getPlayer() != null) car.setOwner(context.getPlayer().getUuid());
        if (!world.isSpaceEmpty(car)) return ActionResult.FAIL;
        world.spawnEntity(car);
        context.getStack().decrementUnlessCreative(1, context.getPlayer());
        return ActionResult.CONSUME;
    }
}
