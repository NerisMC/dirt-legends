package com.dirtlegends.entity;

import com.dirtlegends.block.KickerRampBlock;
import com.dirtlegends.registry.ModItems;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PlayerRideableJumping;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Shared code for every rideable vehicle.
 *
 * Movement is simulated on the rider's own client (the same way vanilla horses and boats work) and the
 * server just receives the position. Physics used:
 *  - kinematic "bicycle" steering: yaw rate = speed * tan(steer angle) / wheelbase
 *  - surface traction: velocity slides toward the heading by the surface's grip each tick
 *  - 5-speed gearbox: strong pull in low gears, less in high gears
 *  - air pitch control and landing / wall-impact crashes
 */
public abstract class MotoVehicleEntity extends Mob implements PlayerRideableJumping {
    private static final EntityDataAccessor<Integer> VARIANT = SynchedEntityData.defineId(MotoVehicleEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> FUEL = SynchedEntityData.defineId(MotoVehicleEntity.class, EntityDataSerializers.FLOAT);
    public static final float MAX_FUEL = 100.0f;
    /** Fuel used per block travelled: a full tank lasts roughly 1,500 blocks. */
    private static final float FUEL_PER_BLOCK = 0.065f;
    public static final float WHEEL_RADIUS = 0.3125f;
    private static final float DEG = Mth.DEG_TO_RAD;
    private static final double DEFAULT_BASE_GRAVITY = 0.08;
    /** Fraction of top speed at which each gear starts (gear 1..5). */
    private static final float[] GEARS = {0.0f, 0.2f, 0.38f, 0.56f, 0.76f};

    /** Set by the client entrypoint: sends a crash report to the server. */
    public static Consumer<Float> crashSender = severity -> { };
    /** Set by the client entrypoint: starts the looping engine sound for a vehicle. */
    public static Consumer<MotoVehicleEntity> engineSoundStarter = vehicle -> { };

    // ---- physics, only meaningful on the side that controls movement (the rider's client)
    private float flyDist;
    private double velX, velZ;
    private float steer;
    private float throttle;
    private float jumpCharge;
    private double lastControlX, lastControlY, lastControlZ;
    private boolean lastControlOnGround = true;
    private int airTicks;
    private float airPitch;
    private float lastSpeed;
    private int crashCooldown;
    private boolean controlledLocally;

    // ---- visuals, computed on every client
    private float wheelSpin, prevWheelSpin;
    private float bodyPitch, prevBodyPitch;
    private float lean, prevLean;
    private float steerVisual, prevSteerVisual;
    private float suspension, prevSuspension;
    private float displaySpeed, prevDisplaySpeed;
    private float rpm = 0.15f;
    private int gear = 1;
    private boolean visualOnGround = true;
    private double lastVisualDy;
    private boolean engineRunning;

    // ---- race timing + landing damage, server only
    private boolean lapActive;
    private long lapStart;
    private long bestLap = -1;
    private int lapNumber;
    private boolean wasOnLine;
    private final List<BlockPos> checkpoints = new ArrayList<>();
    private int messageHold;
    private int serverAirTicks;
    private double peakY;
    private long lastCrashTick = -1000;
    private double serverLastX, serverLastZ;
    private boolean warnedEmpty;
    private int refuelMessageCooldown;

    protected MotoVehicleEntity(EntityType<? extends MotoVehicleEntity> type, Level world) {
        super(type, world);
    }

    public abstract VehicleStats getStats();

    protected abstract ItemStack getVariantStack(int variant);

    public static AttributeSupplier.Builder createVehicleAttributes() {
        // Max health 1 keeps the vanilla HUD showing your food bar instead of "mount hearts".
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 1.0)
                .add(Attributes.MOVEMENT_SPEED, 0.1)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    // ------------------------------------------------------------------ data

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(VARIANT, 0);
        builder.define(FUEL, MAX_FUEL);
    }

    public int getVariant() {
        return Mth.clamp(this.entityData.get(VARIANT), 0, ModItems.COLORS.length - 1);
    }

    public void setVariant(int variant) {
        this.entityData.set(VARIANT, variant);
    }

    public float getFuel() {
        return this.entityData.get(FUEL);
    }

    public void setFuel(float fuel) {
        this.entityData.set(FUEL, Mth.clamp(fuel, 0.0f, MAX_FUEL));
    }

    @Override
    public void addAdditionalSaveData(ValueOutput nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putInt("Variant", getVariant());
        nbt.putFloat("Fuel", getFuel());
        if (bestLap > 0) nbt.putLong("BestLap", bestLap);
    }

    @Override
    public void readAdditionalSaveData(ValueInput nbt) {
        super.readAdditionalSaveData(nbt);
        setVariant(nbt.getIntOr("Variant", 0));
        setFuel(nbt.getFloatOr("Fuel", MAX_FUEL));
        bestLap = nbt.getLongOr("BestLap", bestLap);
    }

    // ------------------------------------------------------------------ basic vehicle behaviour

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (held.is(ModItems.FUEL_CAN)) {
            if (getFuel() >= MAX_FUEL - 0.5f) return InteractionResult.PASS;
            if (!this.level().isClientSide()) {
                setFuel(getFuel() + com.dirtlegends.item.FuelCanItem.FUEL_AMOUNT);
                held.consume(1, player);
                this.playSound(SoundEvents.BUCKET_EMPTY, 1.0f, 0.8f);
                player.sendOverlayMessage(Component.literal("Refueled: " + Math.round(getFuel()) + "%").withStyle(ChatFormatting.GREEN));
                warnedEmpty = false;
            }
            return InteractionResult.SUCCESS;
        }
        if (player.isSecondaryUseActive() || this.isVehicle()) {
            return InteractionResult.PASS;
        }
        if (!this.level().isClientSide()) {
            player.startRiding(this);
        }
        return InteractionResult.SUCCESS;
    }

    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        return this.getFirstPassenger() instanceof Player player ? player : null;
    }

    @Override
    public boolean hurtServer(ServerLevel serverLevel, DamageSource source, float amount) {
        if (source.getEntity() instanceof Player player) {
            if (this.isRemoved()) return true;
            if (this.hasPassenger(player)) return false;
            if (!player.getAbilities().instabuild) {
                this.spawnAtLocation(serverLevel, getVariantStack(getVariant()));
            }
            this.playSound(SoundEvents.ITEM_BREAK.value(), 0.8f, 0.8f);
            this.ejectPassengers();
            this.discard();
            return true;
        }
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return super.hurtServer(serverLevel, source, amount);
        }
        return false; // fire, cactus, mobs, falling etc. don't wreck the vehicle
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float damageMultiplier, DamageSource damageSource) {
        return false; // hard landings are handled in serverTick()
    }

    @Override
    public ItemStack getPickResult() {
        return getVariantStack(getVariant());
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distanceSquared) {
        return false;
    }

    @Override
    public boolean startRiding(Entity entity, boolean force, boolean sendEvent) {
        return false; // vehicles can't be scooped into boats or minecarts
    }

    @Override
    protected void positionRider(Entity passenger, Entity.MoveFunction positionUpdater) {
        super.positionRider(passenger, positionUpdater);
        if (passenger instanceof LivingEntity living) {
            living.yBodyRot = this.getYRot();
        }
    }

    @Override
    public void onPassengerTurned(Entity passenger) {
        passenger.setYBodyRot(this.getYRot());
        float f = Mth.wrapDegrees(passenger.getYRot() - this.getYRot());
        float g = Mth.clamp(f, -105.0f, 105.0f);
        passenger.yRotO += g - f;
        passenger.setYRot(passenger.getYRot() + g - f);
        passenger.setYHeadRot(passenger.getYRot());
    }

    // ------------------------------------------------------------------ jump bar = suspension preload

    @Override
    public void onPlayerJump(int strength) {
        if (strength <= 0) return;
        jumpCharge = strength >= 90 ? 1.0f : 0.35f + 0.65f * strength / 90.0f;
    }

    @Override
    public boolean canJump() {
        return true;
    }

    @Override
    public void handleStartJump(int height) {
    }

    @Override
    public void handleStopJump() {
    }

    // ------------------------------------------------------------------ riding physics

    @Override
    protected Vec3 getRiddenInput(Player controllingPlayer, Vec3 movementInput) {
        return Vec3.ZERO; // we set the velocity ourselves in tickControlled
    }

    @Override
    protected float getRiddenSpeed(Player controllingPlayer) {
        return 0.1f;
    }

    @Override
    protected void tickRidden(Player rider, Vec3 movementInput) {
        super.tickRidden(rider, movementInput);
        if (!this.isLocalInstanceAuthoritative()) return;

        float fwdIn = rider.zza > 0.01f ? 1.0f : rider.zza < -0.01f ? -1.0f : 0.0f;
        if (fwdIn > 0 && getFuel() <= 0.0f) fwdIn = 0.0f; // out of gas: you can only coast and brake
        float sideIn = rider.xxa > 0.01f ? 1.0f : rider.xxa < -0.01f ? -1.0f : 0.0f; // + is left
        physics(fwdIn, sideIn, rider);
    }

    /**
     * One tick of bike/ATV physics. Players run it on their own client (tickRidden); AI racers run it on the
     * server (travel). {@code rider} turns with the vehicle when given.
     */
    protected void physics(float fwdIn, float sideIn, @Nullable Entity rider) {
        VehicleStats s = getStats();
        throttle = fwdIn;
        if (crashCooldown > 0) crashCooldown--;

        boolean onGround = this.onGround();
        double movedX = getX() - lastControlX, movedZ = getZ() - lastControlZ;
        double moved = Math.sqrt(movedX * movedX + movedZ * movedZ);
        double climbed = getY() - lastControlY;
        Surface surf = Surface.of(this);
        float top = s.maxSpeed() * surf.top();
        if (this.isInWater()) top *= 0.3f;
        if (this.isInLava()) top *= 0.2f;

        // --- wall impact: we were fast, hit something, and nearly stopped
        if (this.horizontalCollision && lastSpeed > 0.55f && moved < lastSpeed * 0.35f) {
            crash(4.0f + (lastSpeed - 0.55f) * 25.0f);
            flyDist *= 0.2f;
            velX *= 0.2;
            velZ *= 0.2;
        } else if (this.horizontalCollision && Math.abs(flyDist) > 0.05f && moved < Math.abs(flyDist) * 0.5f) {
            flyDist *= 0.6f;
        }

        // --- landing
        if (onGround && !lastControlOnGround && airTicks > 8) {
            if (Math.abs(airPitch) > 42.0f) {
                crash(5.0f + airTicks * 0.1f);
                flyDist *= 0.3f;
            } else {
                flyDist *= 1.0f - Math.abs(airPitch) / 200.0f; // sloppy landings scrub speed
            }
        }

        // --- engine, brakes, rolling resistance
        if (onGround) {
            if (fwdIn > 0) {
                if (flyDist < 0) {
                    flyDist = Math.min(0.0f, flyDist + s.brake());
                } else if (flyDist < top) {
                    int g = gearFor(flyDist / s.maxSpeed());
                    float pull = s.accel() * surf.accel() * (1.35f - 0.13f * g);
                    flyDist = Math.min(top, flyDist + pull * (1.0f - 0.5f * flyDist / top));
                }
            } else if (fwdIn < 0) {
                if (flyDist > 0.01f) {
                    flyDist = Math.max(0.0f, flyDist - s.brake() * (0.35f + 0.65f * surf.grip()));
                } else {
                    flyDist = Math.max(-s.reverseSpeed(), flyDist - 0.01f);
                }
            } else {
                flyDist *= 0.985f;
                flyDist -= Math.signum(flyDist) * Math.min(Math.abs(flyDist), 0.003f);
            }
            if (flyDist > top) flyDist = Math.max(top, flyDist - 0.025f);
        } else {
            flyDist *= 0.998f;
        }

        // --- steering (bicycle model)
        float speedFrac = Mth.clamp(Math.abs(flyDist) / s.maxSpeed(), 0.0f, 1.0f);
        float maxSteer = Mth.lerp(speedFrac, s.steerLow(), s.steerHigh());
        float targetSteer = -sideIn * maxSteer;
        steer += (targetSteer - steer) * 0.35f;
        float dYaw;
        if (onGround) {
            double yawRate = flyDist * Math.tan(steer * DEG) / s.wheelbase();
            dYaw = (float) Math.toDegrees(yawRate) * (0.45f + 0.55f * surf.grip());
        } else {
            dYaw = -sideIn * 1.5f; // a little "scrub" in the air
        }
        if (dYaw != 0.0f) {
            this.setYRot(this.getYRot() + dYaw);
            if (rider != null) rider.setYRot(rider.getYRot() + dYaw);
        }

        // --- traction: actual velocity slides toward where the wheels point
        float yawRad = this.getYRot() * DEG;
        double fx = -Mth.sin(yawRad), fz = Mth.cos(yawRad);
        if (onGround) {
            double grip = surf.grip();
            velX += (fx * flyDist - velX) * grip;
            velZ += (fz * flyDist - velZ) * grip;
            double lateral = velX * -fz + velZ * fx;
            flyDist -= (float) Math.abs(lateral) * 0.04f * Math.signum(flyDist);
        } else {
            velX *= 0.995;
            velZ *= 0.995;
        }

        // --- vertical: ramps, kickers, preload hop, air control
        double vy = this.getDeltaMovement().y;
        if (onGround) {
            if (lastControlOnGround && climbed > 0.2 && climbed < 1.1 && flyDist > 0.3f) {
                vy = Math.max(vy, Math.min(0.7, climbed * flyDist));
                airPitch = (float) -Math.min(35.0, vy * 55.0);
            }
            BlockState feet = this.level().getBlockState(this.blockPosition());
            if (feet.getBlock() instanceof KickerRampBlock && flyDist > 0.25f) {
                Direction face = feet.getValue(KickerRampBlock.FACING);
                double along = fx * face.getStepX() + fz * face.getStepZ();
                if (along > 0.5) {
                    vy = 0.38 + flyDist * 0.5;
                    airPitch = -28.0f;
                }
            }
            if (jumpCharge > 0.0f) {
                vy = Math.max(vy, 0.22 + 0.3 * jumpCharge);
                airPitch = -10.0f;
            }
            if (vy <= 0.0) airPitch = 0.0f;
        } else {
            float natural = (float) Mth.clamp(-vy * 60.0, -40.0, 40.0);
            airPitch += (natural - airPitch) * 0.05f;
            airPitch += fwdIn * 2.4f; // W = nose down, S = nose up
            airPitch = Mth.clamp(airPitch, -80.0f, 80.0f);
        }
        jumpCharge = 0.0f;
        airTicks = onGround ? 0 : airTicks + 1;

        this.setDeltaMovement(velX, vy, velZ);

        lastSpeed = Math.abs(flyDist);
        lastControlX = getX();
        lastControlY = getY();
        lastControlZ = getZ();
        lastControlOnGround = onGround;
    }

    private void crash(float severity) {
        if (crashCooldown > 0 || !this.level().isClientSide()) return;
        crashCooldown = 40;
        crashSender.accept(severity);
    }

    static int gearFor(float speedFraction) {
        int g = 1;
        for (int i = 1; i < GEARS.length; i++) {
            if (speedFraction >= GEARS[i]) g = i + 1;
        }
        return g;
    }

    // ------------------------------------------------------------------ AI racers

    @Override
    public void travel(Vec3 movementInput) {
        if (!this.level().isClientSide() && !this.hasControllingPassenger()
                && this.getFirstPassenger() instanceof com.dirtlegends.npc.RacerEntity racer) {
            float[] in = racer.drive(this);
            physics(in[0], in[1], null);
            super.travel(Vec3.ZERO);
            return;
        }
        super.travel(movementInput);
    }

    /** Forward speed in blocks per tick (signed). */
    public float speed() {
        return flyDist;
    }

    public float airPitch() {
        return airPitch;
    }

    // ------------------------------------------------------------------ ticking

    @Override
    public void tick() {
        super.tick();
        this.setYBodyRot(this.getYRot());
        this.setYHeadRot(this.getYRot());
        if (this.level().isClientSide()) {
            controlledLocally = this.isLocalInstanceAuthoritative() && this.hasControllingPassenger();
            if (!this.isVehicle()) {
                flyDist = 0.0f;
                velX = velZ = 0.0;
            }
            clientVisualTick();
        } else {
            serverTick();
        }
    }

    private void clientVisualTick() {
        VehicleStats s = getStats();
        prevWheelSpin = wheelSpin;
        prevBodyPitch = bodyPitch;
        prevLean = lean;
        prevSteerVisual = steerVisual;
        prevSuspension = suspension;
        prevDisplaySpeed = displaySpeed;

        double dx = getX() - xo, dy = getY() - yo, dz = getZ() - zo;
        float yawRad = getYRot() * DEG;
        double fwd = -Mth.sin(yawRad) * dx + Mth.cos(yawRad) * dz;
        wheelSpin += (float) (fwd / WHEEL_RADIUS);
        displaySpeed += ((float) Math.sqrt(dx * dx + dz * dz) - displaySpeed) * 0.35f;

        boolean grounded = this.onGround();
        float dYawRad = Mth.wrapDegrees(getYRot() - yRotO) * DEG;

        // steering angle: exact when we're riding it, otherwise worked back out from the turn rate
        float steerTarget = controlledLocally ? steer
                : displaySpeed > 0.03f ? (float) Math.toDegrees(Math.atan(dYawRad * s.wheelbase() / displaySpeed)) : steerVisual;
        steerVisual += (Mth.clamp(steerTarget, -40.0f, 40.0f) - steerVisual) * 0.5f;

        // lean angle from centripetal acceleration: tan(lean) = v * omega / g
        float leanTarget = grounded ? (float) Math.toDegrees(Math.atan(displaySpeed * dYawRad / DEFAULT_BASE_GRAVITY)) * s.leanFactor() : lean * 0.97f;
        lean += (Mth.clamp(leanTarget, -38.0f, 38.0f) - lean) * 0.3f;

        boolean onThrottle = controlledLocally ? throttle > 0 : displaySpeed - prevDisplaySpeed > 0.002f;
        float pitchTarget;
        if (!grounded) {
            pitchTarget = controlledLocally ? airPitch : (float) Mth.clamp(-dy * 60.0, -40.0, 40.0);
        } else if (s.wheelies() && onThrottle && displaySpeed > 0.04f && displaySpeed < 0.3f && displaySpeed - prevDisplaySpeed > 0.008f) {
            pitchTarget = -14.0f; // wheelie off the line
        } else {
            pitchTarget = 0.0f;
        }
        bodyPitch += (pitchTarget - bodyPitch) * (grounded ? 0.3f : 0.5f);

        if (grounded && !visualOnGround && lastVisualDy < -0.1) {
            suspension = Math.min(1.0f, (float) -lastVisualDy * 2.0f);
        }
        suspension *= 0.78f;
        visualOnGround = grounded;
        lastVisualDy = dy;

        // gearbox + rpm
        float frac = displaySpeed / s.maxSpeed();
        gear = gearFor(frac);
        float lo = GEARS[gear - 1];
        float hi = gear < GEARS.length ? GEARS[gear] : 1.05f;
        float rpmTarget = 0.18f + 0.82f * Mth.clamp((frac - lo) / (hi - lo), 0.0f, 1.0f);
        if (onThrottle) rpmTarget = Math.max(rpmTarget, 0.45f);
        if (!grounded && onThrottle) rpmTarget = 1.0f;
        if (!this.isVehicle()) rpmTarget = 0.0f;
        rpm += (rpmTarget - rpm) * 0.3f;

        if (this.isVehicle() && !engineRunning) {
            engineRunning = true;
            engineSoundStarter.accept(this);
        }

        // roost: dirt flies off the rear tyre under power or when sliding
        if (grounded && displaySpeed > 0.12f && this.random.nextFloat() < displaySpeed * (onThrottle ? 1.2f : 0.3f)) {
            BlockState under = this.level().getBlockState(BlockPos.containing(getX(), getY() - 0.2, getZ()));
            if (!under.isAir() && under.getRenderShape() != RenderShape.INVISIBLE) {
                double bx = getX() + Mth.sin(yawRad) * 0.8;
                double bz = getZ() - Mth.cos(yawRad) * 0.8;
                for (int i = 0; i < 2; i++) {
                    this.level().addParticle(new BlockParticleOption(ParticleTypes.BLOCK, under),
                            bx + (random.nextDouble() - 0.5) * 0.4, getY() + 0.1, bz + (random.nextDouble() - 0.5) * 0.4,
                            Mth.sin(yawRad) * displaySpeed * 1.5, 0.2 + random.nextDouble() * 0.2, -Mth.cos(yawRad) * displaySpeed * 1.5);
                }
            }
        }
    }

    private void serverTick() {
        this.fallDistance = 0.0;
        if (messageHold > 0) messageHold--;
        if (refuelMessageCooldown > 0) refuelMessageCooldown--;
        double mx = getX() - serverLastX, mz = getZ() - serverLastZ;
        double moved = this.tickCount > 1 ? Math.min(2.0, Math.sqrt(mx * mx + mz * mz)) : 0.0;
        serverLastX = getX();
        serverLastZ = getZ();
        if (this.tickCount % 5 == 0) tickFuelPumps();
        if (!(this.getControllingPassenger() instanceof ServerPlayer rider)) {
            serverAirTicks = 0;
            return;
        }

        // hard landings (flat drops) hurt even if you land straight; gear helps a lot
        boolean grounded = !this.level().noCollision(this, this.getBoundingBox().deflate(0.05, 0.0, 0.05).move(0.0, -0.12, 0.0));
        if (!grounded) {
            peakY = serverAirTicks == 0 ? getY() : Math.max(peakY, getY());
            serverAirTicks++;
        } else {
            if (serverAirTicks > 0) {
                double drop = peakY - getY();
                if (serverAirTicks >= 14) {
                    com.dirtlegends.npc.SpectatorEntity.react(rider.level(), this.position(), com.dirtlegends.npc.SpectatorEntity.Moment.AIR);
                    showMessage(rider, Component.literal(String.format("Airtime %.2fs", serverAirTicks / 20.0f)).withStyle(ChatFormatting.AQUA), 30);
                }
                if (drop > 12.0) {
                    float dmg = (float) (drop - 12.0) * 0.5f;
                    if (MotocrossGear.isWearingFullSet(rider)) dmg *= 0.3f;
                    if (dmg >= 1.0f) rider.hurt(this.damageSources().fall(), dmg);
                }
            }
            serverAirTicks = 0;
        }

        // burn fuel while riding
        if (getFuel() > 0.0f) {
            setFuel(getFuel() - (float) moved * FUEL_PER_BLOCK - 0.0005f);
            if (getFuel() <= 0.0f && !warnedEmpty) {
                warnedEmpty = true;
                showMessage(rider, Component.literal("OUT OF FUEL! Find a gas station or use a Fuel Can").withStyle(ChatFormatting.RED, ChatFormatting.BOLD), 60);
            } else if (getFuel() < 15.0f && getFuel() + (float) moved * FUEL_PER_BLOCK + 0.0005f >= 15.0f) {
                showMessage(rider, Component.literal("Low fuel!").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), 40);
            }
        }

        tickRaceTiming(rider);
    }

    /** Parked within 2 blocks of a Fuel Pump: fill up (about 5 seconds for a full tank). */
    private void tickFuelPumps() {
        if (getFuel() >= MAX_FUEL) return;
        BlockPos base = this.blockPosition();
        boolean nearPump = false;
        for (BlockPos p : BlockPos.betweenClosed(base.offset(-2, -1, -2), base.offset(2, 2, 2))) {
            if (this.level().getBlockState(p).is(com.dirtlegends.registry.ModBlocks.FUEL_PUMP)) {
                nearPump = true;
                break;
            }
        }
        if (!nearPump) return;
        setFuel(getFuel() + 5.0f);
        warnedEmpty = false;
        if (this.getControllingPassenger() instanceof ServerPlayer rider && refuelMessageCooldown == 0) {
            showMessage(rider, Component.literal("REFUELING  " + Math.round(getFuel()) + "%").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD), 6);
            refuelMessageCooldown = 5;
        }
        if (this.tickCount % 20 == 0) {
            this.playSound(SoundEvents.BREWING_STAND_BREW, 0.5f, 1.4f);
        }
    }

    /** Called on the server when the rider's client reports a crash. */
    public void serverCrash(ServerPlayer rider, float severity) {
        long now = this.level().getGameTime();
        if (now - lastCrashTick < 30 || !this.hasPassenger(rider)) return;
        lastCrashTick = now;
        severity = Mth.clamp(severity, 0.0f, 14.0f);
        boolean geared = MotocrossGear.isWearingFullSet(rider);
        float dmg = geared ? severity * 0.3f : severity;
        rider.stopRiding();
        float yawRad = this.getYRot() * DEG;
        rider.setDeltaMovement(-Mth.sin(yawRad) * 0.6, 0.45, Mth.cos(yawRad) * 0.6);
        rider.hurtMarked = true;
        if (dmg >= 0.5f) rider.hurt(this.damageSources().flyIntoWall(), dmg);
        this.level().playSound(null, getX(), getY(), getZ(), SoundEvents.ANVIL_LAND, SoundSource.NEUTRAL, 0.5f, 0.6f);
        rider.sendOverlayMessage(Component.literal(geared ? "CRASHED! Your gear saved you." : "CRASHED! Wear full Motocross gear!").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
        lapActive = false;
        com.dirtlegends.npc.SpectatorEntity.react(rider.level(), this.position(), com.dirtlegends.npc.SpectatorEntity.Moment.CRASH);
    }

    private void tickRaceTiming(ServerPlayer rider) {
        long now = this.level().getGameTime();
        BlockState under = this.level().getBlockState(BlockPos.containing(getX(), getY() - 0.3, getZ()));
        boolean onLine = under.is(com.dirtlegends.registry.ModBlocks.START_FINISH_LINE);
        if (onLine && !wasOnLine) {
            if (!lapActive) {
                lapActive = true;
                lapStart = now;
                lapNumber = 1;
                checkpoints.clear();
                showMessage(rider, Component.literal("GO! Lap 1").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD), 40);
            } else if (now - lapStart > 40) {
                if (checkpoints.isEmpty()) {
                    showMessage(rider, Component.literal("Missed the checkpoints! Lap restarted").withStyle(ChatFormatting.RED), 50);
                } else {
                    long lap = now - lapStart;
                    boolean best = bestLap < 0 || lap < bestLap;
                    if (best) bestLap = lap;
                    showMessage(rider, Component.literal("Lap " + lapNumber + ": " + formatTime(lap)
                                    + (best ? "  NEW BEST!" : "  (best " + formatTime(bestLap) + ")"))
                            .withStyle(best ? ChatFormatting.GOLD : ChatFormatting.WHITE, ChatFormatting.BOLD), 70);
                    lapNumber++;
                    int prize = best ? 3 : 1;
                    ItemStack cash = new ItemStack(net.minecraft.world.item.Items.EMERALD, prize);
                    if (!rider.getInventory().add(cash)) rider.drop(cash, false);
                    com.dirtlegends.npc.SpectatorEntity.react(rider.level(), this.position(), com.dirtlegends.npc.SpectatorEntity.Moment.LAP);
                }
                lapStart = now;
                checkpoints.clear();
            }
        }
        wasOnLine = onLine;

        if (lapActive && under.is(com.dirtlegends.registry.ModBlocks.CHECKPOINT)) {
            BlockPos p = BlockPos.containing(getX(), getY() - 0.3, getZ());
            if (checkpoints.stream().noneMatch(q -> q.closerThan(p, 6.0))) {
                checkpoints.add(p);
                showMessage(rider, Component.literal("Checkpoint " + checkpoints.size() + "  " + formatTime(now - lapStart)).withStyle(ChatFormatting.YELLOW), 35);
            }
        }

        if (lapActive && messageHold == 0 && now % 4 == 0) {
            rider.sendOverlayMessage(Component.literal("Lap " + lapNumber + "  " + formatTime(now - lapStart)
                    + "  CP " + checkpoints.size()
                    + (bestLap > 0 ? "  Best " + formatTime(bestLap) : "")).withStyle(ChatFormatting.GRAY));
        }
    }

    private void showMessage(ServerPlayer rider, Component text, int holdTicks) {
        rider.sendOverlayMessage(text);
        messageHold = holdTicks;
    }

    public static String formatTime(long ticks) {
        long ms = ticks * 50;
        return String.format("%d:%02d.%02d", ms / 60000, (ms / 1000) % 60, (ms % 1000) / 10);
    }

    // ------------------------------------------------------------------ getters for rendering / HUD / sound

    public float getWheelSpin(float tickDelta) {
        return Mth.lerp(tickDelta, prevWheelSpin, wheelSpin);
    }

    public float getBodyPitch(float tickDelta) {
        return Mth.lerp(tickDelta, prevBodyPitch, bodyPitch);
    }

    public float getLean(float tickDelta) {
        return Mth.lerp(tickDelta, prevLean, lean);
    }

    public float getSteerAngle(float tickDelta) {
        return Mth.lerp(tickDelta, prevSteerVisual, steerVisual);
    }

    public float getSuspension(float tickDelta) {
        return Mth.lerp(tickDelta, prevSuspension, suspension);
    }

    public float getSpeedKmh() {
        return displaySpeed * 72.0f; // blocks/tick * 20 ticks/s * 3.6
    }

    public float getRpm() {
        return rpm;
    }

    public int getGear() {
        return gear;
    }

    public boolean isEngineRunning() {
        return engineRunning;
    }

    public void setEngineRunning(boolean running) {
        this.engineRunning = running;
    }
}
