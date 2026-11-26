package com.dirtlegends.entity;

import com.dirtlegends.block.KickerRampBlock;
import com.dirtlegends.registry.ModItems;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.JumpingMount;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

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
public abstract class MotoVehicleEntity extends MobEntity implements JumpingMount {
    private static final TrackedData<Integer> VARIANT = DataTracker.registerData(MotoVehicleEntity.class, TrackedDataHandlerRegistry.INTEGER);
    public static final float WHEEL_RADIUS = 0.3125f;
    private static final float DEG = MathHelper.RADIANS_PER_DEGREE;
    private static final double GRAVITY = 0.08;
    /** Fraction of top speed at which each gear starts (gear 1..5). */
    private static final float[] GEARS = {0.0f, 0.2f, 0.38f, 0.56f, 0.76f};

    /** Set by the client entrypoint: sends a crash report to the server. */
    public static Consumer<Float> crashSender = severity -> { };
    /** Set by the client entrypoint: starts the looping engine sound for a vehicle. */
    public static Consumer<MotoVehicleEntity> engineSoundStarter = vehicle -> { };

    // ---- physics, only meaningful on the side that controls movement (the rider's client)
    private float speed;
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

    protected MotoVehicleEntity(EntityType<? extends MotoVehicleEntity> type, World world) {
        super(type, world);
    }

    public abstract VehicleStats getStats();

    protected abstract ItemStack getVariantStack(int variant);

    public static DefaultAttributeContainer.Builder createVehicleAttributes() {
        // Max health 1 keeps the vanilla HUD showing your food bar instead of "mount hearts".
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 1.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.1)
                .add(EntityAttributes.GENERIC_STEP_HEIGHT, 1.0);
    }

    // ------------------------------------------------------------------ data

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(VARIANT, 0);
    }

    public int getVariant() {
        return MathHelper.clamp(this.dataTracker.get(VARIANT), 0, ModItems.COLORS.length - 1);
    }

    public void setVariant(int variant) {
        this.dataTracker.set(VARIANT, variant);
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putInt("Variant", getVariant());
        if (bestLap > 0) nbt.putLong("BestLap", bestLap);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        setVariant(nbt.getInt("Variant"));
        if (nbt.contains("BestLap")) bestLap = nbt.getLong("BestLap");
    }

    // ------------------------------------------------------------------ basic vehicle behaviour

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        if (player.shouldCancelInteraction() || this.hasPassengers()) {
            return ActionResult.PASS;
        }
        if (!this.getWorld().isClient) {
            player.startRiding(this);
        }
        return ActionResult.success(this.getWorld().isClient);
    }

    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        return this.getFirstPassenger() instanceof PlayerEntity player ? player : null;
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        if (source.getAttacker() instanceof PlayerEntity player) {
            if (this.getWorld().isClient || this.isRemoved()) return true;
            if (this.hasPassenger(player)) return false;
            if (!player.getAbilities().creativeMode) {
                this.dropStack(getVariantStack(getVariant()));
            }
            this.playSound(SoundEvents.ENTITY_ITEM_BREAK, 0.8f, 0.8f);
            this.removeAllPassengers();
            this.discard();
            return true;
        }
        if (source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return super.damage(source, amount);
        }
        return false; // fire, cactus, mobs, falling etc. don't wreck the vehicle
    }

    @Override
    public boolean handleFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource) {
        return false; // hard landings are handled in serverTick()
    }

    @Override
    public ItemStack getPickBlockStack() {
        return getVariantStack(getVariant());
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canImmediatelyDespawn(double distanceSquared) {
        return false;
    }

    @Override
    public boolean startRiding(Entity entity, boolean force) {
        return false; // vehicles can't be scooped into boats or minecarts
    }

    @Override
    protected void updatePassengerPosition(Entity passenger, Entity.PositionUpdater positionUpdater) {
        super.updatePassengerPosition(passenger, positionUpdater);
        if (passenger instanceof LivingEntity living) {
            living.bodyYaw = this.getYaw();
        }
    }

    @Override
    public void onPassengerLookAround(Entity passenger) {
        passenger.setBodyYaw(this.getYaw());
        float f = MathHelper.wrapDegrees(passenger.getYaw() - this.getYaw());
        float g = MathHelper.clamp(f, -105.0f, 105.0f);
        passenger.prevYaw += g - f;
        passenger.setYaw(passenger.getYaw() + g - f);
        passenger.setHeadYaw(passenger.getYaw());
    }

    // ------------------------------------------------------------------ jump bar = suspension preload

    @Override
    public void setJumpStrength(int strength) {
        if (strength <= 0) return;
        jumpCharge = strength >= 90 ? 1.0f : 0.35f + 0.65f * strength / 90.0f;
    }

    @Override
    public boolean canJump() {
        return true;
    }

    @Override
    public void startJumping(int height) {
    }

    @Override
    public void stopJumping() {
    }

    // ------------------------------------------------------------------ riding physics

    @Override
    protected Vec3d getControlledMovementInput(PlayerEntity controllingPlayer, Vec3d movementInput) {
        return Vec3d.ZERO; // we set the velocity ourselves in tickControlled
    }

    @Override
    protected float getSaddledSpeed(PlayerEntity controllingPlayer) {
        return 0.1f;
    }

    @Override
    protected void tickControlled(PlayerEntity rider, Vec3d movementInput) {
        super.tickControlled(rider, movementInput);
        if (!this.isLogicalSideForUpdatingMovement()) return;

        VehicleStats s = getStats();
        float fwdIn = rider.forwardSpeed > 0.01f ? 1.0f : rider.forwardSpeed < -0.01f ? -1.0f : 0.0f;
        float sideIn = rider.sidewaysSpeed > 0.01f ? 1.0f : rider.sidewaysSpeed < -0.01f ? -1.0f : 0.0f; // + is left
        throttle = fwdIn;
        if (crashCooldown > 0) crashCooldown--;

        boolean onGround = this.isOnGround();
        double movedX = getX() - lastControlX, movedZ = getZ() - lastControlZ;
        double moved = Math.sqrt(movedX * movedX + movedZ * movedZ);
        double climbed = getY() - lastControlY;
        Surface surf = Surface.of(this);
        float top = s.maxSpeed() * surf.top();
        if (this.isTouchingWater()) top *= 0.3f;
        if (this.isInLava()) top *= 0.2f;

        // --- wall impact: we were fast, hit something, and nearly stopped
        if (this.horizontalCollision && lastSpeed > 0.55f && moved < lastSpeed * 0.35f) {
            crash(4.0f + (lastSpeed - 0.55f) * 25.0f);
            speed *= 0.2f;
            velX *= 0.2;
            velZ *= 0.2;
        } else if (this.horizontalCollision && Math.abs(speed) > 0.05f && moved < Math.abs(speed) * 0.5f) {
            speed *= 0.6f;
        }

        // --- landing
        if (onGround && !lastControlOnGround && airTicks > 8) {
            if (Math.abs(airPitch) > 42.0f) {
                crash(5.0f + airTicks * 0.1f);
                speed *= 0.3f;
            } else {
                speed *= 1.0f - Math.abs(airPitch) / 200.0f; // sloppy landings scrub speed
            }
        }

        // --- engine, brakes, rolling resistance
        if (onGround) {
            if (fwdIn > 0) {
                if (speed < 0) {
                    speed = Math.min(0.0f, speed + s.brake());
                } else if (speed < top) {
                    int g = gearFor(speed / s.maxSpeed());
                    float pull = s.accel() * surf.accel() * (1.35f - 0.13f * g);
                    speed = Math.min(top, speed + pull * (1.0f - 0.5f * speed / top));
                }
            } else if (fwdIn < 0) {
                if (speed > 0.01f) {
                    speed = Math.max(0.0f, speed - s.brake() * (0.35f + 0.65f * surf.grip()));
                } else {
                    speed = Math.max(-s.reverseSpeed(), speed - 0.01f);
                }
            } else {
                speed *= 0.985f;
                speed -= Math.signum(speed) * Math.min(Math.abs(speed), 0.003f);
            }
            if (speed > top) speed = Math.max(top, speed - 0.025f);
        } else {
            speed *= 0.998f;
        }

        // --- steering (bicycle model)
        float speedFrac = MathHelper.clamp(Math.abs(speed) / s.maxSpeed(), 0.0f, 1.0f);
        float maxSteer = MathHelper.lerp(speedFrac, s.steerLow(), s.steerHigh());
        float targetSteer = -sideIn * maxSteer;
        steer += (targetSteer - steer) * 0.35f;
        float dYaw;
        if (onGround) {
            double yawRate = speed * Math.tan(steer * DEG) / s.wheelbase();
            dYaw = (float) Math.toDegrees(yawRate) * (0.45f + 0.55f * surf.grip());
        } else {
            dYaw = -sideIn * 1.5f; // a little "scrub" in the air
        }
        if (dYaw != 0.0f) {
            this.setYaw(this.getYaw() + dYaw);
            rider.setYaw(rider.getYaw() + dYaw);
        }

        // --- traction: actual velocity slides toward where the wheels point
        float yawRad = this.getYaw() * DEG;
        double fx = -MathHelper.sin(yawRad), fz = MathHelper.cos(yawRad);
        if (onGround) {
            double grip = surf.grip();
            velX += (fx * speed - velX) * grip;
            velZ += (fz * speed - velZ) * grip;
            double lateral = velX * -fz + velZ * fx;
            speed -= (float) Math.abs(lateral) * 0.04f * Math.signum(speed);
        } else {
            velX *= 0.995;
            velZ *= 0.995;
        }

        // --- vertical: ramps, kickers, preload hop, air control
        double vy = this.getVelocity().y;
        if (onGround) {
            if (lastControlOnGround && climbed > 0.2 && climbed < 1.1 && speed > 0.3f) {
                vy = Math.max(vy, Math.min(0.7, climbed * speed));
                airPitch = (float) -Math.min(35.0, vy * 55.0);
            }
            BlockState feet = this.getWorld().getBlockState(this.getBlockPos());
            if (feet.getBlock() instanceof KickerRampBlock && speed > 0.25f) {
                Direction face = feet.get(KickerRampBlock.FACING);
                double along = fx * face.getOffsetX() + fz * face.getOffsetZ();
                if (along > 0.5) {
                    vy = 0.38 + speed * 0.5;
                    airPitch = -28.0f;
                }
            }
            if (jumpCharge > 0.0f) {
                vy = Math.max(vy, 0.22 + 0.3 * jumpCharge);
                airPitch = -10.0f;
            }
            if (vy <= 0.0) airPitch = 0.0f;
        } else {
            float natural = (float) MathHelper.clamp(-vy * 60.0, -40.0, 40.0);
            airPitch += (natural - airPitch) * 0.05f;
            airPitch += fwdIn * 2.4f; // W = nose down, S = nose up
            airPitch = MathHelper.clamp(airPitch, -80.0f, 80.0f);
        }
        jumpCharge = 0.0f;
        airTicks = onGround ? 0 : airTicks + 1;

        this.setVelocity(velX, vy, velZ);

        lastSpeed = Math.abs(speed);
        lastControlX = getX();
        lastControlY = getY();
        lastControlZ = getZ();
        lastControlOnGround = onGround;
    }

    private void crash(float severity) {
        if (crashCooldown > 0 || !this.getWorld().isClient) return;
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

    // ------------------------------------------------------------------ ticking

    @Override
    public void tick() {
        super.tick();
        this.setBodyYaw(this.getYaw());
        this.setHeadYaw(this.getYaw());
        if (this.getWorld().isClient) {
            controlledLocally = this.isLogicalSideForUpdatingMovement() && this.hasControllingPassenger();
            if (!this.hasPassengers()) {
                speed = 0.0f;
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

        double dx = getX() - prevX, dy = getY() - prevY, dz = getZ() - prevZ;
        float yawRad = getYaw() * DEG;
        double fwd = -MathHelper.sin(yawRad) * dx + MathHelper.cos(yawRad) * dz;
        wheelSpin += (float) (fwd / WHEEL_RADIUS);
        displaySpeed += ((float) Math.sqrt(dx * dx + dz * dz) - displaySpeed) * 0.35f;

        boolean grounded = this.isOnGround();
        float dYawRad = MathHelper.wrapDegrees(getYaw() - prevYaw) * DEG;

        // steering angle: exact when we're riding it, otherwise worked back out from the turn rate
        float steerTarget = controlledLocally ? steer
                : displaySpeed > 0.03f ? (float) Math.toDegrees(Math.atan(dYawRad * s.wheelbase() / displaySpeed)) : steerVisual;
        steerVisual += (MathHelper.clamp(steerTarget, -40.0f, 40.0f) - steerVisual) * 0.5f;

        // lean angle from centripetal acceleration: tan(lean) = v * omega / g
        float leanTarget = grounded ? (float) Math.toDegrees(Math.atan(displaySpeed * dYawRad / GRAVITY)) * s.leanFactor() : lean * 0.97f;
        lean += (MathHelper.clamp(leanTarget, -38.0f, 38.0f) - lean) * 0.3f;

        boolean onThrottle = controlledLocally ? throttle > 0 : displaySpeed - prevDisplaySpeed > 0.002f;
        float pitchTarget;
        if (!grounded) {
            pitchTarget = controlledLocally ? airPitch : (float) MathHelper.clamp(-dy * 60.0, -40.0, 40.0);
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
        float rpmTarget = 0.18f + 0.82f * MathHelper.clamp((frac - lo) / (hi - lo), 0.0f, 1.0f);
        if (onThrottle) rpmTarget = Math.max(rpmTarget, 0.45f);
        if (!grounded && onThrottle) rpmTarget = 1.0f;
        if (!this.hasPassengers()) rpmTarget = 0.0f;
        rpm += (rpmTarget - rpm) * 0.3f;

        if (this.hasPassengers() && !engineRunning) {
            engineRunning = true;
            engineSoundStarter.accept(this);
        }

        // roost: dirt flies off the rear tyre under power or when sliding
        if (grounded && displaySpeed > 0.12f && this.random.nextFloat() < displaySpeed * (onThrottle ? 1.2f : 0.3f)) {
            BlockState under = this.getWorld().getBlockState(BlockPos.ofFloored(getX(), getY() - 0.2, getZ()));
            if (!under.isAir() && under.getRenderType() != BlockRenderType.INVISIBLE) {
                double bx = getX() + MathHelper.sin(yawRad) * 0.8;
                double bz = getZ() - MathHelper.cos(yawRad) * 0.8;
                for (int i = 0; i < 2; i++) {
                    this.getWorld().addParticle(new BlockStateParticleEffect(ParticleTypes.BLOCK, under),
                            bx + (random.nextDouble() - 0.5) * 0.4, getY() + 0.1, bz + (random.nextDouble() - 0.5) * 0.4,
                            MathHelper.sin(yawRad) * displaySpeed * 1.5, 0.2 + random.nextDouble() * 0.2, -MathHelper.cos(yawRad) * displaySpeed * 1.5);
                }
            }
        }
    }

    private void serverTick() {
        this.fallDistance = 0.0f;
        if (messageHold > 0) messageHold--;
        if (!(this.getControllingPassenger() instanceof ServerPlayerEntity rider)) {
            serverAirTicks = 0;
            return;
        }

        // hard landings (flat drops) hurt even if you land straight; gear helps a lot
        boolean grounded = !this.getWorld().isSpaceEmpty(this, this.getBoundingBox().contract(0.05, 0.0, 0.05).offset(0.0, -0.12, 0.0));
        if (!grounded) {
            peakY = serverAirTicks == 0 ? getY() : Math.max(peakY, getY());
            serverAirTicks++;
        } else {
            if (serverAirTicks > 0) {
                double drop = peakY - getY();
                if (serverAirTicks >= 14) {
                    showMessage(rider, Text.literal(String.format("Airtime %.2fs", serverAirTicks / 20.0f)).formatted(Formatting.AQUA), 30);
                }
                if (drop > 12.0) {
                    float dmg = (float) (drop - 12.0) * 0.5f;
                    if (MotocrossGear.isWearingFullSet(rider)) dmg *= 0.3f;
                    if (dmg >= 1.0f) rider.damage(this.getDamageSources().fall(), dmg);
                }
            }
            serverAirTicks = 0;
        }

        tickRaceTiming(rider);
    }

    /** Called on the server when the rider's client reports a crash. */
    public void serverCrash(ServerPlayerEntity rider, float severity) {
        long now = this.getWorld().getTime();
        if (now - lastCrashTick < 30 || !this.hasPassenger(rider)) return;
        lastCrashTick = now;
        severity = MathHelper.clamp(severity, 0.0f, 14.0f);
        boolean geared = MotocrossGear.isWearingFullSet(rider);
        float dmg = geared ? severity * 0.3f : severity;
        rider.stopRiding();
        float yawRad = this.getYaw() * DEG;
        rider.setVelocity(-MathHelper.sin(yawRad) * 0.6, 0.45, MathHelper.cos(yawRad) * 0.6);
        rider.velocityModified = true;
        if (dmg >= 0.5f) rider.damage(this.getDamageSources().flyIntoWall(), dmg);
        this.getWorld().playSound(null, getX(), getY(), getZ(), SoundEvents.BLOCK_ANVIL_LAND, SoundCategory.NEUTRAL, 0.5f, 0.6f);
        rider.sendMessage(Text.literal(geared ? "CRASHED! Your gear saved you." : "CRASHED! Wear full Motocross gear!").formatted(Formatting.RED, Formatting.BOLD), true);
        lapActive = false;
    }

    private void tickRaceTiming(ServerPlayerEntity rider) {
        long now = this.getWorld().getTime();
        BlockState under = this.getWorld().getBlockState(BlockPos.ofFloored(getX(), getY() - 0.3, getZ()));
        boolean onLine = under.isOf(com.dirtlegends.registry.ModBlocks.START_FINISH_LINE);
        if (onLine && !wasOnLine) {
            if (!lapActive) {
                lapActive = true;
                lapStart = now;
                lapNumber = 1;
                checkpoints.clear();
                showMessage(rider, Text.literal("GO! Lap 1").formatted(Formatting.GREEN, Formatting.BOLD), 40);
            } else if (now - lapStart > 40) {
                if (checkpoints.isEmpty()) {
                    showMessage(rider, Text.literal("Missed the checkpoints! Lap restarted").formatted(Formatting.RED), 50);
                } else {
                    long lap = now - lapStart;
                    boolean best = bestLap < 0 || lap < bestLap;
                    if (best) bestLap = lap;
                    showMessage(rider, Text.literal("Lap " + lapNumber + ": " + formatTime(lap)
                                    + (best ? "  NEW BEST!" : "  (best " + formatTime(bestLap) + ")"))
                            .formatted(best ? Formatting.GOLD : Formatting.WHITE, Formatting.BOLD), 70);
                    lapNumber++;
                }
                lapStart = now;
                checkpoints.clear();
            }
        }
        wasOnLine = onLine;

        if (lapActive && under.isOf(com.dirtlegends.registry.ModBlocks.CHECKPOINT)) {
            BlockPos p = BlockPos.ofFloored(getX(), getY() - 0.3, getZ());
            if (checkpoints.stream().noneMatch(q -> q.isWithinDistance(p, 6.0))) {
                checkpoints.add(p);
                showMessage(rider, Text.literal("Checkpoint " + checkpoints.size() + "  " + formatTime(now - lapStart)).formatted(Formatting.YELLOW), 35);
            }
        }

        if (lapActive && messageHold == 0 && now % 4 == 0) {
            rider.sendMessage(Text.literal("Lap " + lapNumber + "  " + formatTime(now - lapStart)
                    + "  CP " + checkpoints.size()
                    + (bestLap > 0 ? "  Best " + formatTime(bestLap) : "")).formatted(Formatting.GRAY), true);
        }
    }

    private void showMessage(ServerPlayerEntity rider, Text text, int holdTicks) {
        rider.sendMessage(text, true);
        messageHold = holdTicks;
    }

    public static String formatTime(long ticks) {
        long ms = ticks * 50;
        return String.format("%d:%02d.%02d", ms / 60000, (ms / 1000) % 60, (ms % 1000) / 10);
    }

    // ------------------------------------------------------------------ getters for rendering / HUD / sound

    public float getWheelSpin(float tickDelta) {
        return MathHelper.lerp(tickDelta, prevWheelSpin, wheelSpin);
    }

    public float getBodyPitch(float tickDelta) {
        return MathHelper.lerp(tickDelta, prevBodyPitch, bodyPitch);
    }

    public float getLean(float tickDelta) {
        return MathHelper.lerp(tickDelta, prevLean, lean);
    }

    public float getSteerAngle(float tickDelta) {
        return MathHelper.lerp(tickDelta, prevSteerVisual, steerVisual);
    }

    public float getSuspension(float tickDelta) {
        return MathHelper.lerp(tickDelta, prevSuspension, suspension);
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
