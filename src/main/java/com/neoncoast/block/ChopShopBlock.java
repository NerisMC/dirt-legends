package com.neoncoast.block;

import com.neoncoast.crime.CrimeHooks;
import com.neoncoast.economy.Money;
import com.neoncoast.vehicle.CarEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Comparator;
import java.util.List;

/** Park a car next to it and right-click: they strip it for parts and pay you. No questions asked. */
public class ChopShopBlock extends Block {
    public ChopShopBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (world.isClient || !(player instanceof ServerPlayerEntity sp)) return ActionResult.success(world.isClient);
        List<CarEntity> cars = world.getEntitiesByClass(CarEntity.class, new Box(pos).expand(6.0), c -> !c.hasPassengers());
        if (cars.isEmpty()) {
            sp.sendMessage(Text.literal("Park a car right next to the chop shop first.").formatted(Formatting.GRAY), true);
            return ActionResult.SUCCESS;
        }
        cars.sort(Comparator.comparingDouble(c -> c.squaredDistanceTo(pos.toCenterPos())));
        CarEntity car = cars.get(0);
        long value = car.isPolice() ? 1500 : car.getCarType().chopValue();
        value = Math.round(value * (0.4 + 0.6 * car.getCarHealth() / car.getCarType().maxHealth()));
        if (car.isPolice()) CrimeHooks.crime(sp, 1, "Chopped a police car");
        car.discard();
        Money.add(sp, value);
        world.playSound(null, pos, SoundEvents.BLOCK_ANVIL_LAND, net.minecraft.sound.SoundCategory.BLOCKS, 0.6f, 0.7f);
        sp.sendMessage(Text.literal("Chopped for parts: +$" + value).formatted(Formatting.GREEN, Formatting.BOLD), true);
        return ActionResult.SUCCESS;
    }
}
