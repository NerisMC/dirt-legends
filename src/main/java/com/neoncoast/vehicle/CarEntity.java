package com.neoncoast.vehicle;

import com.neoncoast.crime.CrimeHooks;
import com.neoncoast.crime.WantedManager;
import com.neoncoast.npc.PoliceOfficerEntity;
import com.neoncoast.registry.ModBlocks;
import com.neoncoast.registry.ModEntities;
import com.neoncoast.registry.ModItems;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
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
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * A drivable car. Driving is simulated on the driver's own client (like vanilla horses and boats);
 * police cars with no player inside are driven by the server towards the nearest wanted player.
 *
 * Physics: kinematic bicycle steering (yaw rate = v * tan(steer) / wheelbase), tyre grip per surface
 * (low grip = the car slides), a 6-speed gearbox, handbrake drifts, crash damage and run-over damage.
 */
public class CarEntity extends MobEntity implements JumpingMount {
    private static final TrackedData<Integer> VARIANT = DataTracker.registerData(CarEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Float> CAR_HEALTH = DataTracker.registerData(CarEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Boolean> SIREN = DataTracker.registerData(CarEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    public static final String[] COLORS = {"red", "blue", "black", "white", "yellow"};
    public static final float WHEEL_RADIUS = 0.375f;
    private static final float DEG = MathHelper.RADIANS_PER_DEGREE;
    private static final float[] GEARS = {0.0f, 0.12f, 0.26f, 0.42f, 0.6f, 0.8f};

    /** Set by the client entrypoint. */
    public static Consumer<Float> crashSender = f -> { };
    public static Consumer<CarEntity> soundStarter = c -> { };

    // physics (driving side)
    private float speed;
    private double velX, velZ;
    private float steer;
    private float throttle;
    private int handbrakeTicks;
    private float lastSpeed;
    private double lastX, lastZ;
    private int crashCooldown;
    private boolean controlledLocally;

    // visuals (every client)
    private float wheelSpin, prevWheelSpin, steerVisual, prevSteerVisual, roll, prevRoll, pitch, prevPitch;
    private float displaySpeed, prevDisplaySpeed, rpm;
    private int gear = 1;
    private boolean soundRunning;

    // server
    @Nullable private UUID owner;
    private boolean civilian;
    private double serverLastX, serverLastZ;
    private int idleTicks;
    private int aiStuckTicks;

    public CarEntity(EntityType<? extends CarEntity> type, World world) {
        super(type, world);
    }

    public static DefaultAttributeContainer.Builder createCarAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 1.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.1)
                .add(EntityAttributes.GENERIC_STEP_HEIGHT, 1.0)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1.0);
    }

    public CarType getCarType() {
        return ModEntities.carTypeOf(this.getType());
    }

    public boolean isPolice() {
        return this.getType() == ModEntities.POLICE_CAR;
    }

    // ------------------------------------------------------------------ data

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(VARIANT, 0);
        builder.add(CAR_HEALTH, 100.0f);
        builder.add(SIREN, false);
    }

    public int getVariant() {
        return MathHelper.clamp(this.dataTracker.get(VARIANT), 0, COLORS.length - 1);
    }

    public void setVariant(int v) {
        this.dataTracker.set(VARIANT, v);
    }

    public float getCarHealth() {
        return this.dataTracker.get(CAR_HEALTH);
    }

    public void setCarHealth(float h) {
        this.dataTracker.set(CAR_HEALTH, MathHelper.clamp(h, 0.0f, getCarType().maxHealth()));
    }

    public boolean isSirenOn() {
        return this.dataTracker.get(SIREN);
    }

    public void setOwner(@Nullable UUID owner) {
        this.owner = owner;
    }

    public void setCivilian(boolean civilian) {
        this.civilian = civilian;
    }

    public void initFresh(int variant) {
        setVariant(variant);
        setCarHealth(getCarType().maxHealth());
        setPersistent();
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putInt("Variant", getVariant());
        nbt.putFloat("CarHealth", getCarHealth());
        nbt.putBoolean("Civilian", civilian);
        if (owner != null) nbt.putUuid("Owner", owner);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        setVariant(nbt.getInt("Variant"));
        if (nbt.contains("CarHealth")) setCarHealth(nbt.getFloat("CarHealth"));
        civilian = nbt.getBoolean("Civilian");
        if (nbt.containsUuid("Owner")) owner = nbt.getUuid("Owner");
    }

    // ------------------------------------------------------------------ interaction

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        ItemStack held = player.getStackInHand(hand);
        if (held.isOf(ModItems.REPAIR_KIT)) {
            if (!this.getWorld().isClient) {
                setCarHealth(getCarType().maxHealth());
                held.decrementUnlessCreative(1, player);
                this.playSound(SoundEvents.BLOCK_ANVIL_USE, 0.6f, 1.4f);
                player.sendMessage(Text.literal("Car repaired").formatted(Formatting.GREEN), true);
            }
            return ActionResult.success(this.getWorld().isClient);
        }
        if (player.shouldCancelInteraction() || this.hasPassengers()) {
            return ActionResult.PASS;
        }
        if (!this.getWorld().isClient && player instanceof ServerPlayerEntity sp) {
            boolean mine = owner != null && owner.equals(player.getUuid());
            if (!mine) {
                if (isPolice()) {
                    CrimeHooks.crime(sp, 2, "Stole a police car");
                } else if (civilian || owner != null) {
                    CrimeHooks.witnessedCrime(sp, 1, "Car theft");
                }
                owner = player.getUuid();
                civilian = false;
            }
            player.startRiding(this);
        }
        return ActionResult.success(this.getWorld().isClient);
    }

    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        return this.getFirstPassenger() instanceof PlayerEntity p ? p : null;
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        if (this.getWorld().isClient || this.isRemoved()) return false;
        if (source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY)) return super.damage(source, amount);
        Entity attacker = source.getAttacker();
        if (attacker instanceof PlayerEntity player) {
            if (this.hasPassenger(player)) return false;
            if (player.getAbilities().creativeMode && player.isSneaking()) {
                this.discard();
                return true;
            }
            boolean mine = owner != null && owner.equals(player.getUuid());
            if (mine && player.isSneaking()) {
                // owners can pocket their car back into a key
                this.dropStack(new ItemStack(ModItems.keyFor(this.getType())));
                this.discard();
                return true;
            }
            damageCar(4.0f);
            if (isPolice() && player instanceof ServerPlayerEntity sp) CrimeHooks.crime(sp, 1, "Vandalised a police car");
            return true;
        }
        if (source.isIn(DamageTypeTags.IS_EXPLOSION)) {
            damageCar(amount * 2.0f);
            return true;
        }
        if (source.isIn(DamageTypeTags.IS_PROJECTILE)) {
            damageCar(amount);
            return true;
        }
        return false;
    }

    public void damageCar(float amount) {
        setCarHealth(getCarHealth() - amount);
        this.playSound(SoundEvents.ENTITY_IRON_GOLEM_DAMAGE, 0.6f, 0.8f + this.random.nextFloat() * 0.3f);
        if (getCarHealth() <= 0.0f) explode();
    }

    private void explode() {
        if (this.getWorld().isClient || this.isRemoved()) return;
        this.removeAllPassengers();
        this.getWorld().createExplosion(this, getX(), getY() + 0.5, getZ(), 2.5f, World.ExplosionSourceType.NONE);
        this.discard();
    }

    @Override
    public boolean handleFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource) {
        if (fallDistance > 4.0f && !this.getWorld().isClient) damageCar((fallDistance - 4.0f) * 4.0f);
        return false;
    }

    @Override
    public ItemStack getPickBlockStack() {
        return new ItemStack(ModItems.keyFor(this.getType()));
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
        return false;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return this.getPassengerList().isEmpty();
    }

    @Override
    protected void updatePassengerPosition(Entity passenger, Entity.PositionUpdater positionUpdater) {
        super.updatePassengerPosition(passenger, positionUpdater);
        if (passenger instanceof LivingEntity living) living.bodyYaw = this.getYaw();
    }

    @Override
    public void onPassengerLookAround(Entity passenger) {
        passenger.setBodyYaw(this.getYaw());
        float f = MathHelper.wrapDegrees(passenger.getYaw() - this.getYaw());
        float g = MathHelper.clamp(f, -120.0f, 120.0f);
        passenger.prevYaw += g - f;
        passenger.setYaw(passenger.getYaw() + g - f);
        passenger.setHeadYaw(passenger.getYaw());
    }

    // ------------------------------------------------------------------ handbrake (space bar)

    @Override
    public void setJumpStrength(int strength) {
        if (strength > 0) handbrakeTicks = 8 + strength / 6;
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

    // ------------------------------------------------------------------ driving

    @Override
    protected Vec3d getControlledMovementInput(PlayerEntity controllingPlayer, Vec3d movementInput) {
        return Vec3d.ZERO;
    }

    @Override
    protected float getSaddledSpeed(PlayerEntity controllingPlayer) {
        return 0.1f;
    }

    @Override
    protected void tickControlled(PlayerEntity rider, Vec3d movementInput) {
        super.tickControlled(rider, movementInput);
        if (!this.isLogicalSideForUpdatingMovement()) return;
        float fwd = rider.forwardSpeed > 0.01f ? 1.0f : rider.forwardSpeed < -0.01f ? -1.0f : 0.0f;
        float side = rider.sidewaysSpeed > 0.01f ? 1.0f : rider.sidewaysSpeed < -0.01f ? -1.0f : 0.0f;
        float dYaw = simulate(fwd, side, true);
        if (dYaw != 0.0f) rider.setYaw(rider.getYaw() + dYaw);
    }

    /** One tick of car physics. Returns how much the car turned (to turn the driver's camera with it). */
    private float simulate(float fwdIn, float sideIn, boolean reportCrashes) {
        CarType s = getCarType();
        throttle = fwdIn;
        if (crashCooldown > 0) crashCooldown--;
        boolean onGround = this.isOnGround();
        double mx = getX() - lastX, mz = getZ() - lastZ;
        double moved = Math.sqrt(mx * mx + mz * mz);
        CarSurface surf = CarSurface.of(this);
        float health = getCarHealth() / s.maxHealth();
        float top = s.maxSpeed() * surf.top() * (health < 0.25f ? 0.6f : 1.0f);
        if (this.isTouchingWater()) top *= 0.25f;

        // crash into something solid
        if (this.horizontalCollision && lastSpeed > 0.45f && moved < lastSpeed * 0.4f) {
            float impact = lastSpeed;
            if (reportCrashes && crashCooldown == 0 && this.getWorld().isClient) {
                crashCooldown = 20;
                crashSender.accept(impact);
            }
            speed *= 0.15f;
            velX *= 0.15;
            velZ *= 0.15;
        } else if (this.horizontalCollision && Math.abs(speed) > 0.05f && moved < Math.abs(speed) * 0.5f) {
            speed *= 0.5f;
        }

        if (onGround) {
            if (fwdIn > 0) {
                if (speed < -0.02f) {
                    speed = Math.min(0.0f, speed + s.brake());
                } else if (speed < top) {
                    int g = gearFor(speed / s.maxSpeed());
                    float pull = s.accel() * surf.accel() * (1.5f - 0.17f * g);
                    speed = Math.min(top, speed + pull * (1.0f - 0.55f * speed / top));
                }
            } else if (fwdIn < 0) {
                if (speed > 0.02f) speed = Math.max(0.0f, speed - s.brake() * (0.4f + 0.6f * surf.grip()));
                else speed = Math.max(-s.reverse(), speed - 0.012f);
            } else {
                speed *= 0.988f;
                speed -= Math.signum(speed) * Math.min(Math.abs(speed), 0.0025f);
            }
            if (speed > top) speed = Math.max(top, speed - 0.03f);
        }

        boolean handbrake = handbrakeTicks > 0;
        if (handbrake) {
            handbrakeTicks--;
            if (onGround) speed *= 0.975f;
        }

        // steering (bicycle model)
        float frac = MathHelper.clamp(Math.abs(speed) / s.maxSpeed(), 0.0f, 1.0f);
        float maxSteer = MathHelper.lerp(frac, s.steerLow(), s.steerHigh()) * (handbrake ? 1.6f : 1.0f);
        steer += (-sideIn * maxSteer - steer) * 0.3f;
        float dYaw = 0.0f;
        if (onGround) {
            double yawRate = speed * Math.tan(steer * DEG) / s.wheelbase();
            dYaw = (float) Math.toDegrees(yawRate) * (0.5f + 0.5f * surf.grip());
            if (dYaw != 0.0f) this.setYaw(this.getYaw() + dYaw);
        }

        // tyre grip: the real velocity is pulled toward where the car points
        float yawRad = this.getYaw() * DEG;
        double fx = -MathHelper.sin(yawRad), fz = MathHelper.cos(yawRad);
        if (onGround) {
            double grip = Math.min(1.0, surf.grip() * s.grip()) * (handbrake ? 0.12 : 1.0);
            velX += (fx * speed - velX) * grip;
            velZ += (fz * speed - velZ) * grip;
            double lateral = velX * -fz + velZ * fx;
            speed -= (float) Math.abs(lateral) * 0.03f * Math.signum(speed);
        } else {
            velX *= 0.99;
            velZ *= 0.99;
        }
        this.setVelocity(velX, this.getVelocity().y, velZ);

        lastSpeed = Math.abs(speed);
        lastX = getX();
        lastZ = getZ();
        return dYaw;
    }

    static int gearFor(float frac) {
        int g = 1;
        for (int i = 1; i < GEARS.length; i++) {
            if (frac >= GEARS[i]) g = i + 1;
        }
        return g;
    }

    /** Police AI and any other server-driven movement. */
    @Override
    public void travel(Vec3d movementInput) {
        if (!this.getWorld().isClient && !this.hasControllingPassenger() && isPolice()) {
            float[] input = policeDriver();
            simulate(input[0], input[1], false);
            super.travel(Vec3d.ZERO);
            return;
        }
        if (!this.hasControllingPassenger()) {
            // coasting / parked
            speed *= 0.9f;
            velX *= 0.8;
            velZ *= 0.8;
            if (this.isOnGround()) this.setVelocity(velX, this.getVelocity().y, velZ);
        }
        super.travel(movementInput);
    }

    /** Returns {throttle, steer} towards the nearest wanted player. */
    private float[] policeDriver() {
        PlayerEntity target = WantedManager.nearestWanted(this, 96.0);
        boolean chasing = target != null;
        if (this.dataTracker.get(SIREN) != chasing) this.dataTracker.set(SIREN, chasing);
        if (!chasing) {
            idleTicks++;
            return new float[]{0.0f, 0.0f};
        }
        idleTicks = 0;
        double dx = target.getX() - getX(), dz = target.getZ() - getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);
        float wanted = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float diff = MathHelper.wrapDegrees(wanted - getYaw());
        float steerIn = MathHelper.clamp(-diff / 25.0f, -1.0f, 1.0f);
        float throttleIn;
        if (dist < 7.0) {
            throttleIn = speed > 0.05f ? -1.0f : 0.0f;
            // close enough: officers jump out
            if (Math.abs(speed) < 0.15f && this.hasPassengers()) this.removeAllPassengers();
        } else if (Math.abs(diff) > 100.0f && dist < 20.0) {
            throttleIn = -1.0f; // target is behind us: reverse around
            steerIn = -steerIn;
        } else {
            throttleIn = 1.0f;
        }
        if (lastSpeed < 0.03f && throttleIn > 0) {
            if (++aiStuckTicks > 40) {
                handbrakeTicks = 10;
                aiStuckTicks = 0;
            }
        } else {
            aiStuckTicks = 0;
        }
        return new float[]{throttleIn, steerIn};
    }

    // ------------------------------------------------------------------ ticking

    @Override
    public void tick() {
        super.tick();
        this.setBodyYaw(this.getYaw());
        this.setHeadYaw(this.getYaw());
        if (this.getWorld().isClient) {
            controlledLocally = this.isLogicalSideForUpdatingMovement() && this.hasControllingPassenger();
            clientTick();
        } else {
            serverTick();
        }
    }

    private void clientTick() {
        prevWheelSpin = wheelSpin;
        prevSteerVisual = steerVisual;
        prevRoll = roll;
        prevPitch = pitch;
        prevDisplaySpeed = displaySpeed;
        CarType s = getCarType();
        double dx = getX() - prevX, dz = getZ() - prevZ;
        float yawRad = getYaw() * DEG;
        double fwd = -MathHelper.sin(yawRad) * dx + MathHelper.cos(yawRad) * dz;
        wheelSpin += (float) (fwd / WHEEL_RADIUS);
        displaySpeed += ((float) Math.sqrt(dx * dx + dz * dz) - displaySpeed) * 0.35f;
        float dYawRad = MathHelper.wrapDegrees(getYaw() - prevYaw) * DEG;
        float steerTarget = controlledLocally ? steer
                : displaySpeed > 0.03f ? (float) Math.toDegrees(Math.atan(dYawRad * s.wheelbase() / displaySpeed)) : steerVisual;
        steerVisual += (MathHelper.clamp(steerTarget, -35.0f, 35.0f) - steerVisual) * 0.5f;
        // body roll leans OUT of corners, nose dips under braking and squats under power
        float lateralG = (float) Math.toDegrees(Math.atan(displaySpeed * dYawRad / 0.08));
        roll += (MathHelper.clamp(lateralG * 0.08f, -5.0f, 5.0f) - roll) * 0.25f;
        float accel = displaySpeed - prevDisplaySpeed;
        pitch += (MathHelper.clamp(-accel * 120.0f, -4.0f, 4.0f) - pitch) * 0.25f;

        float frac = displaySpeed / s.maxSpeed();
        gear = gearFor(frac);
        float lo = GEARS[gear - 1];
        float hi = gear < GEARS.length ? GEARS[gear] : 1.05f;
        float rpmTarget = 0.15f + 0.85f * MathHelper.clamp((frac - lo) / (hi - lo), 0.0f, 1.0f);
        boolean onThrottle = controlledLocally ? throttle > 0 : accel > 0.002f;
        if (onThrottle) rpmTarget = Math.max(rpmTarget, 0.4f);
        boolean running = this.hasPassengers() || isSirenOn();
        if (!running) rpmTarget = 0.0f;
        rpm += (rpmTarget - rpm) * 0.3f;
        if (running && !soundRunning) {
            soundRunning = true;
            soundStarter.accept(this);
        }

        // damage smoke and fire
        float health = getCarHealth() / s.maxHealth();
        double hx = getX() - MathHelper.sin(yawRad) * 1.4, hz = getZ() + MathHelper.cos(yawRad) * 1.4;
        if (health < 0.45f && this.random.nextFloat() < (0.5f - health)) {
            this.getWorld().addParticle(health < 0.2f ? ParticleTypes.LARGE_SMOKE : ParticleTypes.SMOKE, hx, getY() + 1.0, hz, 0.0, 0.05, 0.0);
        }
        if (health < 0.15f && this.random.nextFloat() < 0.3f) {
            this.getWorld().addParticle(ParticleTypes.FLAME, hx, getY() + 1.0, hz, 0.0, 0.02, 0.0);
        }
        // tyre smoke when sliding / doing burnouts
        if (this.isOnGround() && controlledLocally && (handbrakeTicks > 0 || (throttle > 0 && displaySpeed < 0.15f && gear == 1))) {
            double rx = getX() + MathHelper.sin(yawRad) * 1.3, rz = getZ() - MathHelper.cos(yawRad) * 1.3;
            this.getWorld().addParticle(ParticleTypes.CLOUD, rx, getY() + 0.1, rz, 0.0, 0.02, 0.0);
        }
    }

    private void serverTick() {
        this.fallDistance = 0.0f;
        double mx = getX() - serverLastX, mz = getZ() - serverLastZ;
        double moved = this.age > 2 ? Math.min(3.0, Math.sqrt(mx * mx + mz * mz)) : 0.0;
        serverLastX = getX();
        serverLastZ = getZ();

        // run people over
        if (moved > 0.3) {
            Entity driver = this.getFirstPassenger();
            Box box = this.getBoundingBox().expand(0.4, 0.0, 0.4);
            List<Entity> hit = this.getWorld().getOtherEntities(this, box,
                    e -> e instanceof LivingEntity && !(e instanceof CarEntity) && !this.hasPassenger(e) && e.isAlive());
            for (Entity e : hit) {
                LivingEntity victim = (LivingEntity) e;
                if (victim.hurtTime > 0) continue;
                DamageSource src = driver instanceof PlayerEntity p ? this.getDamageSources().playerAttack(p) : this.getDamageSources().mobAttack(this);
                victim.damage(src, (float) moved * 16.0f);
                Vec3d push = new Vec3d(mx, 0.0, mz).normalize().multiply(moved * 1.2);
                victim.addVelocity(push.x, 0.35, push.z);
                victim.velocityModified = true;
                if (driver instanceof ServerPlayerEntity sp) CrimeHooks.hitWithCar(sp, victim);
                if (moved > 0.8) damageCar(2.0f);
            }
        }

        // abandoned police cars clean themselves up
        if (isPolice() && !this.hasPassengers() && idleTicks > 1200) {
            this.discard();
        }
    }

    /** Server side: rider's client reported a crash into a wall. */
    public void serverCrash(ServerPlayerEntity driver, float impact) {
        impact = MathHelper.clamp(impact, 0.0f, 2.0f);
        damageCar(impact * 30.0f);
        if (impact > 0.9f) {
            driver.damage(this.getDamageSources().flyIntoWall(), (impact - 0.9f) * 12.0f);
        }
        this.getWorld().playSound(null, getX(), getY(), getZ(), SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.NEUTRAL, 0.3f, 1.6f);
    }

    // ------------------------------------------------------------------ getters for render/HUD/sound

    public float getWheelSpin(float td) {
        return MathHelper.lerp(td, prevWheelSpin, wheelSpin);
    }

    public float getSteerAngle(float td) {
        return MathHelper.lerp(td, prevSteerVisual, steerVisual);
    }

    public float getRoll(float td) {
        return MathHelper.lerp(td, prevRoll, roll);
    }

    public float getBodyPitch(float td) {
        return MathHelper.lerp(td, prevPitch, pitch);
    }

    public float getSpeedKmh() {
        return displaySpeed * 72.0f;
    }

    public float getRpm() {
        return rpm;
    }

    public int getGear() {
        return gear;
    }

    public void setSoundRunning(boolean running) {
        this.soundRunning = running;
    }

    public boolean isOnRoad() {
        BlockState under = this.getWorld().getBlockState(BlockPos.ofFloored(getX(), getY() - 0.2, getZ()));
        return under.isOf(ModBlocks.ASPHALT) || under.isOf(ModBlocks.ROAD_LINE);
    }

    static boolean isIce(BlockState s) {
        return s.isIn(BlockTags.ICE);
    }

    static boolean isSoftGround(BlockState s) {
        return s.isIn(BlockTags.DIRT) || s.isOf(Blocks.DIRT_PATH) || s.isOf(Blocks.FARMLAND);
    }
}
