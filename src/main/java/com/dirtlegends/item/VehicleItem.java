package com.dirtlegends.item;

import com.dirtlegends.entity.MotoVehicleEntity;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/** Right-click the ground to park the vehicle there, facing the way you look. */
public class VehicleItem extends Item {
    private final Supplier<? extends EntityType<? extends MotoVehicleEntity>> type;
    private final int variant;

    public VehicleItem(Properties settings, Supplier<? extends EntityType<? extends MotoVehicleEntity>> type, int variant) {
        super(settings);
        this.type = type;
        this.variant = variant;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level world = context.getLevel();
        if (!(world instanceof ServerLevel serverWorld)) {
            return InteractionResult.SUCCESS;
        }
        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        MotoVehicleEntity vehicle = type.get().create(serverWorld, net.minecraft.world.entity.EntitySpawnReason.SPAWN_ITEM_USE);
        if (vehicle == null) {
            return InteractionResult.FAIL;
        }
        float yaw = context.getRotation();
        vehicle.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, yaw, 0.0f);
        vehicle.setYHeadRot(yaw);
        vehicle.setYBodyRot(yaw);
        vehicle.setVariant(variant);
        vehicle.setPersistenceRequired();
        if (!serverWorld.noCollision(vehicle)) {
            return InteractionResult.FAIL;
        }
        serverWorld.addFreshEntity(vehicle);
        context.getItemInHand().consume(1, context.getPlayer());
        return InteractionResult.CONSUME;
    }
}
