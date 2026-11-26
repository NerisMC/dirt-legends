package com.dirtlegends.npc;

import com.dirtlegends.entity.MotoVehicleEntity;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** A fan in the grandstand. Watches the riders, jumps and cheers at big air, groans at crashes, chats when you talk to them. */
public class SpectatorEntity extends PathfinderMob {
    public static final int SKINS = 12;
    private static final EntityDataAccessor<Integer> SKIN = SynchedEntityData.defineId(SpectatorEntity.class, EntityDataSerializers.INT);

    public enum Moment { AIR, LAP, CRASH }

    private int cheerTicks;
    private int shoutCooldown;

    public SpectatorEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 16.0).add(Attributes.MOVEMENT_SPEED, 0.25);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SKIN, 0);
    }

    public int getSkin() {
        return Math.floorMod(this.entityData.get(SKIN), SKINS);
    }

    public void randomize() {
        this.entityData.set(SKIN, this.random.nextInt(SKINS));
        this.setCustomName(Component.literal(TrackTalk.pick(TrackTalk.FAN_NAMES, this.random)));
        this.setPersistenceRequired();
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new WatchRidersGoal());
        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 8.0f));
    }

    @Override
    public void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putInt("Skin", getSkin());
    }

    @Override
    public void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        this.entityData.set(SKIN, in.getIntOr("Skin", 0));
    }

    private String name() {
        return this.getCustomName() != null ? this.getCustomName().getString() : "Fan";
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand == InteractionHand.MAIN_HAND && player instanceof ServerPlayer sp) {
            TrackTalk.say(sp, name(), TrackTalk.pick(TrackTalk.FAN_CHAT, this.random));
        }
        return InteractionResult.SUCCESS;
    }

    /** Something happened on the track: fans within 40 blocks react. */
    public static void react(ServerLevel level, Vec3 at, Moment moment) {
        List<SpectatorEntity> fans = level.getEntitiesOfClass(SpectatorEntity.class, new AABB(at, at).inflate(40.0), f -> true);
        boolean someoneShouted = false;
        for (SpectatorEntity fan : fans) {
            fan.cheerTicks = moment == Moment.CRASH ? 0 : 30 + fan.random.nextInt(20);
            if (!someoneShouted && fan.shoutCooldown <= 0 && fan.random.nextInt(3) == 0) {
                String[] lines = moment == Moment.AIR ? TrackTalk.FAN_CHEER_AIR : moment == Moment.LAP ? TrackTalk.FAN_CHEER_LAP : TrackTalk.FAN_CRASH;
                String line = TrackTalk.pick(lines, fan.random);
                for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, fan.getBoundingBox().inflate(48.0), p -> true)) {
                    TrackTalk.say(p, fan.name(), line);
                }
                fan.shoutCooldown = 200;
                someoneShouted = true;
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) return;
        if (shoutCooldown > 0) shoutCooldown--;
        if (cheerTicks > 0) {
            cheerTicks--;
            if (this.onGround() && this.random.nextInt(6) == 0) this.jumpFromGround();
            if (cheerTicks % 10 == 0 && this.level() instanceof ServerLevel level) {
                level.sendParticles(ParticleTypes.NOTE, getX(), getY() + 2.3, getZ(), 1, 0.2, 0.1, 0.2, 0.0);
            }
        }
    }

    @Override
    public boolean removeWhenFarAway(double distanceSquared) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    /** Heads follow the nearest rider on the track. */
    private class WatchRidersGoal extends Goal {
        private MotoVehicleEntity watching;

        WatchRidersGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            List<MotoVehicleEntity> bikes = level().getEntitiesOfClass(MotoVehicleEntity.class, getBoundingBox().inflate(40.0),
                    MotoVehicleEntity::isVehicle);
            watching = null;
            double best = Double.MAX_VALUE;
            for (MotoVehicleEntity b : bikes) {
                double d = distanceToSqr(b);
                if (d < best) {
                    best = d;
                    watching = b;
                }
            }
            return watching != null;
        }

        @Override
        public boolean canContinueToUse() {
            return watching != null && watching.isAlive() && watching.isVehicle() && distanceToSqr(watching) < 1600;
        }

        @Override
        public void tick() {
            getLookControl().setLookAt(watching, 30.0f, 30.0f);
        }
    }
}
