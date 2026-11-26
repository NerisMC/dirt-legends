package com.dirtlegends.client;

import com.dirtlegends.DirtLegends;
import com.dirtlegends.entity.MotoVehicleEntity;
import com.dirtlegends.fluid.MudFluid;
import com.dirtlegends.registry.ModBlocks;
import com.dirtlegends.registry.ModItems;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * First-person motocross helmet: the goggle frame around the screen, and dirt that lands on the lens while you ride.
 * What hits the lens depends on what you're riding on (mud, dirt, sand, gravel, grass, snow, water) and how fast,
 * plus roost from riders in front of you and big splashes when you land in mud. Mud and water run down the lens,
 * water dries off, and the grime builds up until you pull a tear-off (default key: V).
 */
public final class HelmetVisor {
    private static final Identifier FRAME = DirtLegends.id("textures/gui/helmet_visor.png");
    private static final Identifier[] SPLATS = {
            DirtLegends.id("textures/gui/splat_0.png"), DirtLegends.id("textures/gui/splat_1.png"),
            DirtLegends.id("textures/gui/splat_2.png"), DirtLegends.id("textures/gui/splat_3.png")};
    private static final Identifier DROP = DirtLegends.id("textures/gui/drop.png");
    private static final int MAX_SPLATS = 160;

    /** What the wheels are throwing up. */
    private enum Debris {
        NONE(0, 0, 0, 0f, 0f, false), MUD(70, 46, 26, 1.0f, 1.6f, true), DIRT(110, 76, 46, 0.55f, 1.0f, false),
        SAND(214, 190, 130, 0.6f, 0.6f, false), RED_SAND(190, 100, 50, 0.6f, 0.6f, false), GRAVEL(120, 120, 118, 0.35f, 0.7f, false),
        GRASS(80, 130, 50, 0.3f, 0.7f, false), SNOW(240, 245, 250, 0.5f, 1.0f, true), WATER(180, 210, 230, 0.8f, 0.9f, true);

        final int r, g, b;
        final float rate, size;
        final boolean wet;

        Debris(int r, int g, int b, float rate, float size, boolean wet) {
            this.r = r; this.g = g; this.b = b; this.rate = rate; this.size = size; this.wet = wet;
        }
    }

    private static final class Splat {
        float x, y, size, alpha, vy;
        int color, shape;
        boolean water, wet;
    }

    public static KeyMapping tearOffKey;
    private static final List<Splat> splats = new ArrayList<>();
    private static List<Splat> torn = new ArrayList<>();
    private static float tornGrime;
    private static int tearTicks;
    private static float grime;
    private static boolean wasOnGround = true;
    private static int airTicks;
    private static final Random rng = new Random();

    private HelmetVisor() {
    }

    public static boolean wearingHelmet(LocalPlayer p) {
        return p.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.MX_HELMET);
    }

    private static boolean active(Minecraft mc) {
        return mc.player != null && mc.options.getCameraType().isFirstPerson() && wearingHelmet(mc.player);
    }

    // ------------------------------------------------------------------ simulation (client tick)

    public static void tick(Minecraft mc) {
        if (tearTicks > 0) tearTicks--;
        while (tearOffKey != null && tearOffKey.consumeClick()) tearOff(mc);
        if (mc.player == null || mc.isPaused()) return;
        LocalPlayer p = mc.player;

        // existing splats: wet ones run down, water dries off
        for (int i = splats.size() - 1; i >= 0; i--) {
            Splat s = splats.get(i);
            if (s.wet) {
                s.vy = Math.min(s.vy + 0.00004f, s.water ? 0.004f : 0.0016f);
                s.y += s.vy;
            }
            if (s.water) s.alpha -= 0.004f;
            if (s.alpha <= 0.02f || s.y > 1.15f) splats.remove(i);
        }
        if (!active(mc) || !(p.getVehicle() instanceof MotoVehicleEntity bike)) {
            wasOnGround = true;
            return;
        }

        float speed = Mth.clamp(bike.getSpeedKmh() / 55.0f, 0.0f, 1.6f);
        Debris d = debrisUnder(bike);
        boolean onGround = bike.onGround();

        // our own front wheel and the general dust cloud
        float rate = onGround ? d.rate * speed * 0.6f : 0.0f;
        // roost from riders in front of us
        Vec3 look = Vec3.directionFromRotation(0.0f, bike.getYRot());
        for (MotoVehicleEntity other : p.level().getEntitiesOfClass(MotoVehicleEntity.class, bike.getBoundingBox().inflate(12.0),
                o -> o != bike && o.isVehicle())) {
            Vec3 to = other.position().subtract(bike.position());
            double dist = to.length();
            if (dist < 1.0 || to.normalize().dot(look) < 0.75) continue;
            Debris od = debrisUnder(other);
            float their = Mth.clamp(other.getSpeedKmh() / 50.0f, 0.0f, 1.5f);
            float r = od.rate * their * (float) (1.0 - dist / 12.0) * 2.2f;
            for (int k = 0; k < count(r); k++) spawn(od, 1.0f + rng.nextFloat() * 0.4f);
        }
        for (int k = 0; k < count(rate); k++) spawn(d, 1.0f);

        // landing in the mud: big splash
        if (!onGround) airTicks++;
        if (onGround && !wasOnGround && airTicks > 6 && (d == Debris.MUD || d == Debris.WATER)) {
            int burst = 8 + rng.nextInt(10);
            for (int k = 0; k < burst; k++) spawn(d, 1.3f + rng.nextFloat() * 0.8f);
        }
        if (onGround) airTicks = 0;
        wasOnGround = onGround;

        // rain on the lens
        if (p.level().isRainingAt(p.blockPosition().above()) && rng.nextFloat() < 0.25f + speed * 0.2f) spawn(Debris.WATER, 0.6f);
    }

    private static int count(float rate) {
        int n = (int) rate;
        if (rng.nextFloat() < rate - n) n++;
        return n;
    }

    private static Debris debrisUnder(MotoVehicleEntity bike) {
        Level level = bike.level();
        BlockPos feet = bike.blockPosition();
        if (level.getFluidState(feet).getType() instanceof MudFluid) return Debris.MUD;
        if (level.getFluidState(feet).is(FluidTags.WATER)) return Debris.WATER;
        BlockState s = level.getBlockState(BlockPos.containing(bike.getX(), bike.getY() - 0.2, bike.getZ()));
        if (level.getBlockState(feet).is(Blocks.SNOW) || s.is(Blocks.SNOW_BLOCK) || s.is(Blocks.POWDER_SNOW)) return Debris.SNOW;
        if (s.is(Blocks.MUD) || s.is(Blocks.CLAY) || s.is(Blocks.SOUL_SOIL) || s.is(Blocks.SOUL_SAND) || s.is(Blocks.MUDDY_MANGROVE_ROOTS)) {
            return Debris.MUD;
        }
        if (s.is(Blocks.RED_SAND)) return Debris.RED_SAND;
        if (s.is(BlockTags.SAND)) return Debris.SAND;
        if (s.is(Blocks.GRAVEL) || s.is(Blocks.SUSPICIOUS_GRAVEL)) return Debris.GRAVEL;
        if (s.is(Blocks.GRASS_BLOCK) || s.is(Blocks.MOSS_BLOCK)) return rng.nextInt(3) == 0 ? Debris.GRASS : Debris.DIRT;
        boolean wetTrack = level.isRainingAt(feet.above());
        if (s.is(ModBlocks.TRACK_DIRT) || s.is(ModBlocks.DIRT_RAMP) || s.is(ModBlocks.DIRT_SLAB) || s.is(BlockTags.DIRT)
                || s.is(Blocks.DIRT_PATH) || s.is(Blocks.FARMLAND)) {
            return wetTrack ? Debris.MUD : Debris.DIRT;
        }
        return Debris.NONE;
    }

    private static void spawn(Debris d, float sizeMul) {
        if (d == Debris.NONE) return;
        if (splats.size() >= MAX_SPLATS) splats.remove(0);
        Splat s = new Splat();
        // most dirt lands low and in the middle of the lens
        s.x = Mth.clamp(0.5f + (float) rng.nextGaussian() * 0.24f, 0.04f, 0.96f);
        s.y = Mth.clamp(0.62f + (float) rng.nextGaussian() * 0.22f, 0.05f, 0.95f);
        s.water = d == Debris.WATER;
        s.wet = d.wet;
        s.size = (0.012f + rng.nextFloat() * 0.03f) * d.size * sizeMul * (s.water ? 0.6f : 1.0f);
        s.alpha = s.water ? 0.55f + rng.nextFloat() * 0.25f : 0.8f + rng.nextFloat() * 0.2f;
        int v = rng.nextInt(31) - 15;
        s.color = (Mth.clamp(d.r + v, 0, 255) << 16) | (Mth.clamp(d.g + v, 0, 255) << 8) | Mth.clamp(d.b + v, 0, 255);
        s.shape = rng.nextInt(SPLATS.length);
        splats.add(s);
        if (!s.water) grime = Math.min(1.0f, grime + 0.0025f);
    }

    private static void tearOff(Minecraft mc) {
        if (mc.player == null || !wearingHelmet(mc.player) || (splats.isEmpty() && grime < 0.01f)) return;
        torn = new ArrayList<>(splats);
        tornGrime = grime;
        splats.clear();
        grime = 0.0f;
        tearTicks = 10;
        mc.player.playSound(SoundEvents.BOOK_PAGE_TURN, 0.9f, 1.4f);
    }

    // ------------------------------------------------------------------ drawing (HUD)

    public static void render(GuiGraphicsExtractor g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || !active(mc)) return;
        int w = g.guiWidth(), h = g.guiHeight();

        drawLayer(g, splats, grime, 0, 1.0f, w, h);
        if (tearTicks > 0) {
            float t = 1.0f - (tearTicks - delta.getGameTimeDeltaPartialTick(false)) / 10.0f;   // 0 -> 1
            int shift = (int) (-t * t * w * 1.1f);
            drawLayer(g, torn, tornGrime, shift, 1.0f - t * 0.5f, w, h);
        }
        g.blit(RenderPipelines.GUI_TEXTURED, FRAME, 0, 0, 0.0f, 0.0f, w, h, 512, 288, 512, 288, 0xFFFFFFFF);
    }

    private static void drawLayer(GuiGraphicsExtractor g, List<Splat> list, float grimeLevel, int shiftX, float fade, int w, int h) {
        if (grimeLevel > 0.01f) {
            int a = (int) (grimeLevel * 70 * fade);
            g.fill(shiftX, 0, shiftX + w, h, (a << 24) | 0x4A3520);
        }
        for (Splat s : list) {
            int size = Math.max(2, (int) (s.size * h));
            int x = shiftX + (int) (s.x * w) - size / 2, y = (int) (s.y * h) - size / 2;
            int a = Mth.clamp((int) (s.alpha * fade * 255), 0, 255);
            Identifier tex = s.water ? DROP : SPLATS[s.shape];
            g.blit(RenderPipelines.GUI_TEXTURED, tex, x, y, 0.0f, 0.0f, size, size, 32, 32, 32, 32, (a << 24) | s.color);
            if (s.wet && !s.water && s.vy > 0.0004f) {
                // a muddy streak where it ran
                int streak = (int) (s.vy * 9000);
                g.fill(x + size / 2 - 1, y - streak, x + size / 2 + 1, y + size / 2, ((a / 3) << 24) | s.color);
            }
        }
    }
}
