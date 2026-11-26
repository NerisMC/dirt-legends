package com.dirtlegends.npc;

import com.dirtlegends.entity.MotoVehicleEntity;
import com.dirtlegends.entity.VehicleStats;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * A rival rider. Sits on a dirt bike or ATV at the start line and, once a player rides up, laps the oval
 * (anticlockwise, the same way as the kickers face), aiming a little ahead on the track, levelling the bike in
 * the air and backing off in the corners. Each racer has a different skill level, so some are beatable.
 */
public class RacerEntity extends PathfinderMob {
    public static final int SKINS = 6;
    private static final EntityDataAccessor<Integer> SKIN = SynchedEntityData.defineId(RacerEntity.class, EntityDataSerializers.INT);

    private BlockPos trackCenter;
    private int halfLength = 17, halfWidth = 11;
    private float skill = 0.85f;
    private int stuckTicks, reverseTicks, waitTicks;

    public RacerEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.MOVEMENT_SPEED, 0.25);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SKIN, 0);
    }

    public int getSkin() {
        return Math.floorMod(this.entityData.get(SKIN), SKINS);
    }

    public void setup(BlockPos center, int halfLength, int halfWidth, int skinIndex) {
        this.trackCenter = center;
        this.halfLength = halfLength;
        this.halfWidth = halfWidth;
        this.skill = 0.72f + this.random.nextFloat() * 0.22f;
        this.entityData.set(SKIN, skinIndex);
        this.setCustomName(Component.literal(TrackTalk.pick(TrackTalk.RACER_NAMES, this.random)));
        this.setCustomNameVisible(true);
        this.setPersistenceRequired();
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8.0f));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
    }

    @Override
    public void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putInt("Skin", getSkin());
        out.putFloat("Skill", skill);
        out.putInt("HalfLength", halfLength);
        out.putInt("HalfWidth", halfWidth);
        if (trackCenter != null) {
            out.putInt("TrackX", trackCenter.getX());
            out.putInt("TrackY", trackCenter.getY());
            out.putInt("TrackZ", trackCenter.getZ());
        }
    }

    @Override
    public void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        this.entityData.set(SKIN, in.getIntOr("Skin", 0));
        skill = in.getFloatOr("Skill", 0.85f);
        halfLength = in.getIntOr("HalfLength", 17);
        halfWidth = in.getIntOr("HalfWidth", 11);
        if (in.getIntOr("TrackY", Integer.MIN_VALUE) != Integer.MIN_VALUE) {
            trackCenter = new BlockPos(in.getIntOr("TrackX", 0), in.getIntOr("TrackY", 0), in.getIntOr("TrackZ", 0));
        }
    }

    private String name() {
        return this.getCustomName() != null ? this.getCustomName().getString() : "Racer";
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand == InteractionHand.MAIN_HAND && player instanceof ServerPlayer sp) {
            TrackTalk.say(sp, name(), TrackTalk.pick(TrackTalk.RACER_CHAT, this.random));
        }
        return InteractionResult.SUCCESS;
    }

    /** True while a player on a bike or ATV is near the track, so the racers have someone to race. */
    private boolean raceOn(MotoVehicleEntity bike) {
        Player p = this.level().getNearestPlayer(bike, 56.0);
        return p != null && p.getVehicle() instanceof MotoVehicleEntity;
    }

    /** Called by the bike every server tick. Returns {throttle, steer} in the same form as player input. */
    public float[] drive(MotoVehicleEntity bike) {
        if (trackCenter == null || !raceOn(bike)) {
            waitTicks = 0;
            return new float[]{bike.speed() > 0.02f ? -1.0f : 0.0f, 0.0f};
        }
        if (++waitTicks < 30) return new float[]{0.0f, 0.0f}; // staggered "gate drop"

        if (!bike.onGround()) {
            float pitch = bike.airPitch();   // negative = nose up; forward input pushes the nose down
            return new float[]{pitch < -6.0f ? 1.0f : pitch > 6.0f ? -1.0f : 0.0f, 0.0f};
        }
        if (reverseTicks > 0) {
            reverseTicks--;
            return new float[]{-1.0f, 0.7f};
        }

        double dx = bike.getX() - (trackCenter.getX() + 0.5), dz = bike.getZ() - (trackCenter.getZ() + 0.5);
        double theta = Math.atan2(dz / halfWidth, dx / halfLength);
        double ahead = theta + 0.42 + bike.speed() * 0.45;
        double tx = trackCenter.getX() + 0.5 + halfLength * Math.cos(ahead) - bike.getX();
        double tz = trackCenter.getZ() + 0.5 + halfWidth * Math.sin(ahead) - bike.getZ();
        float wanted = (float) Math.toDegrees(Math.atan2(-tx, tz));
        float diff = Mth.wrapDegrees(wanted - bike.getYRot());
        float side = Mth.clamp(-diff / 18.0f, -1.0f, 1.0f);

        VehicleStats s = bike.getStats();
        float target = s.maxSpeed() * skill * (Math.abs(diff) > 35.0f ? 0.5f : Math.abs(diff) > 18.0f ? 0.75f : 1.0f);
        float throttle = bike.speed() < target ? 1.0f : bike.speed() > target + 0.08f ? -1.0f : 0.0f;

        if (throttle > 0 && Math.abs(bike.speed()) < 0.03f) {
            if (++stuckTicks > 60) {
                stuckTicks = 0;
                reverseTicks = 20;
            }
        } else {
            stuckTicks = 0;
        }
        return new float[]{throttle, side};
    }

    @Override
    public boolean removeWhenFarAway(double distanceSquared) {
        return false;
    }
}
