package com.dirtlegends.item;

import com.dirtlegends.entity.MotoVehicleEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.function.Supplier;

/** Right-click the ground to park the vehicle there, facing the way you look. */
public class VehicleItem extends Item {
    private final Supplier<? extends EntityType<? extends MotoVehicleEntity>> type;
    private final int variant;

    public VehicleItem(Settings settings, Supplier<? extends EntityType<? extends MotoVehicleEntity>> type, int variant) {
        super(settings);
        this.type = type;
        this.variant = variant;
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        if (!(world instanceof ServerWorld serverWorld)) {
            return ActionResult.SUCCESS;
        }
        BlockPos pos = context.getBlockPos().offset(context.getSide());
        MotoVehicleEntity vehicle = type.get().create(serverWorld);
        if (vehicle == null) {
            return ActionResult.FAIL;
        }
        float yaw = context.getPlayerYaw();
        vehicle.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, yaw, 0.0f);
        vehicle.setHeadYaw(yaw);
        vehicle.setBodyYaw(yaw);
        vehicle.setVariant(variant);
        vehicle.setPersistent();
        if (!serverWorld.isSpaceEmpty(vehicle)) {
            return ActionResult.FAIL;
        }
        serverWorld.spawnEntity(vehicle);
        context.getStack().decrementUnlessCreative(1, context.getPlayer());
        return ActionResult.CONSUME;
    }
}
