package com.neoncoast.npc;

import com.neoncoast.crime.WantedManager;
import com.neoncoast.economy.Money;
import com.neoncoast.vehicle.CarEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/** Cop on foot. Ignores you unless you're wanted; at 1-2 stars a hit when you're hurt means you're busted. */
public class PoliceOfficerEntity extends PathAwareEntity {
    private int noTargetTicks;

    public PoliceOfficerEntity(EntityType<? extends PathAwareEntity> type, World world) {
        super(type, world);
    }

    public static DefaultAttributeContainer.Builder createPoliceAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 24.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.32)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 3.0)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 48.0)
                .add(EntityAttributes.GENERIC_ARMOR, 4.0);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(2, new MeleeAttackGoal(this, 1.25, true));
        this.goalSelector.add(6, new WanderAroundFarGoal(this, 0.8));
        this.goalSelector.add(7, new LookAtEntityGoal(this, PlayerEntity.class, 10.0f));
        this.goalSelector.add(8, new LookAroundGoal(this));
        this.targetSelector.add(1, new ActiveTargetGoal<>(this, PlayerEntity.class, 5, true, false,
                p -> p instanceof PlayerEntity pl && WantedManager.getStars(pl) > 0));
    }

    @Nullable
    @Override
    public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason, @Nullable EntityData entityData) {
        this.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.STICK));
        this.setEquipmentDropChance(EquipmentSlot.MAINHAND, 0.0f);
        return super.initialize(world, difficulty, spawnReason, entityData);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.getWorld().isClient) return;
        if (this.getTarget() instanceof PlayerEntity p && WantedManager.getStars(p) <= 0) {
            this.setTarget(null);
        }
        // hop out of the cruiser once we're close
        if (this.getVehicle() instanceof CarEntity && this.getTarget() != null && this.squaredDistanceTo(this.getTarget()) < 64.0) {
            this.stopRiding();
        }
        if (this.getTarget() == null && !this.hasVehicle()) {
            if (++noTargetTicks > 1200) this.discard(); // off duty
        } else {
            noTargetTicks = 0;
        }
    }

    @Override
    public boolean tryAttack(Entity target) {
        if (target instanceof ServerPlayerEntity player && !player.hasVehicle()) {
            int stars = WantedManager.getStars(player);
            if (stars > 0 && stars <= 2 && player.getHealth() <= 10.0f) {
                bust(player);
                return true;
            }
        }
        return super.tryAttack(target);
    }

    private void bust(ServerPlayerEntity player) {
        long fine = Math.max(100, Money.get(player) / 5);
        Money.add(player, -fine);
        WantedManager.clear(player);
        this.setTarget(null);
        player.setHealth(Math.max(player.getHealth(), 6.0f));
        player.playSound(SoundEvents.BLOCK_IRON_DOOR_CLOSE, 1.0f, 0.8f);
        player.sendMessage(Text.literal("BUSTED! Fine: $" + fine).formatted(Formatting.DARK_AQUA, Formatting.BOLD), true);
    }

    @Override
    public boolean canImmediatelyDespawn(double distanceSquared) {
        return this.getTarget() == null && distanceSquared > 128 * 128;
    }
}
