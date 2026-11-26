#!/usr/bin/env python3
"""Generates textures, entity models (Java), sounds and JSON data for the Dirt Legends mod."""
import json, math, os, random, subprocess, sys
import numpy as np
from PIL import Image

ROOT = sys.argv[1]
MOD = "dirtlegends"
PKG = "com.dirtlegends"
A = f"{ROOT}/src/main/resources/assets/{MOD}"
D = f"{ROOT}/src/main/resources/data"
J = f"{ROOT}/src/main/java/com/dirtlegends"
random.seed(7)

COLORS = {
    "red": (214, 32, 32),
    "blue": (30, 80, 214),
    "green": (70, 190, 40),
    "yellow": (240, 196, 12),
}
COLOR_NAMES = list(COLORS)


def mk(path):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    return path


def wjson(path, obj):
    with open(mk(path), "w") as f:
        json.dump(obj, f, indent=2)


def clamp(c):
    return tuple(max(0, min(255, int(v))) for v in c)


def shade(c, d):
    return clamp((c[0] + d, c[1] + d, c[2] + d))


def noisy(c, amt=10):
    d = random.randint(-amt, amt)
    return shade(c, d) + (255,)

# ---------------------------------------------------------------- entity models
# cuboid: (x, y, z, w, h, d, role, dilation)  -- integer sizes; root space: ground y=0, up is -y, front is -z
WHEEL_BIKE = [(-1, -5, -3, 2, 10, 6, "tire", 0), (-1, -3, -5, 2, 6, 10, "tire", 0), (-1, -3, -3, 2, 6, 6, "hub", 0.2)]
WHEEL_ATV = [(-2, -5, -3, 4, 10, 6, "tire", 0), (-2, -3, -5, 4, 6, 10, "tire", 0), (-2, -3, -3, 4, 6, 6, "hub", 0.2)]

BIKE = {
    "class": "DirtBikeModel", "layer": "dirt_bike", "tex": (128, 64),
    "parts": [
        ("frame", "root", (0, 0, 0), [
            (-2, -10, -3, 4, 4, 6, "metal", 0),
            (-3, -8, -2, 6, 2, 4, "dark", 0),
            (-3, -14, -7, 6, 3, 6, "color", 0),
            (-4, -13, -8, 1, 3, 4, "color", 0),
            (3, -13, -8, 1, 3, 4, "color", 0),
            (-2, -16, -2, 4, 2, 10, "seat", 0),
            (-3, -14, 2, 6, 3, 5, "color", 0),
            (-2, -14, 8, 4, 1, 7, "color", 0),
            (-1, -11, -6, 2, 5, 2, "metal", 0),
            (-2, -6, 0, 4, 1, 11, "metal", 0),
            (3, -11, 0, 2, 2, 12, "metal", 0),
        ]),
        ("fork", "root", (0, -16, -10), [
            (-3, 0, -1, 1, 11, 2, "metal", 0),
            (2, 0, -1, 1, 11, 2, "metal", 0),
            (-3, -1, -1, 6, 1, 3, "dark", 0),
            (-7, -2, 0, 14, 1, 1, "black", 0),
            (-2, 0, -3, 4, 4, 1, "plate", 0),
            (-2, 5, -6, 4, 1, 9, "color", 0),
        ]),
        ("front_wheel", "fork", (0, 11, 0), WHEEL_BIKE),
        ("rear_wheel", "root", (0, -5, 10), WHEEL_BIKE),
    ],
}

ATV = {
    "class": "AtvModel", "layer": "atv", "tex": (128, 128),
    "parts": [
        ("frame", "root", (0, 0, 0), [
            (-5, -9, -11, 10, 3, 22, "dark", 0),
            (-3, -11, -4, 6, 3, 8, "metal", 0),
            (-9, -12, -15, 18, 2, 9, "color", 0),
            (-9, -12, 5, 18, 2, 10, "color", 0),
            (-4, -14, -8, 8, 3, 7, "color", 0),
            (-3, -14, -1, 6, 2, 9, "seat", 0),
            (-3, -12, -1, 6, 1, 6, "dark", 0),
            (-6, -13, -15, 12, 1, 5, "dark", 0),
            (-6, -13, 11, 12, 1, 5, "dark", 0),
            (-5, -9, -17, 10, 2, 2, "dark", 0),
            (-1, -13, 15, 2, 2, 1, "plate", 0),
        ]),
        ("bars", "root", (0, -14, -7), [
            (-1, -3, -1, 2, 4, 2, "metal", 0),
            (-7, -4, 0, 14, 1, 1, "black", 0),
        ]),
        ("front_left", "root", (8, -5, -10), WHEEL_ATV),
        ("front_right", "root", (-8, -5, -10), WHEEL_ATV),
        ("rear_left", "root", (8, -5, 10), WHEEL_ATV),
        ("rear_right", "root", (-8, -5, 10), WHEEL_ATV),
    ],
}


def pack(model):
    """Assign UV offsets with a shelf packer. Returns list of (part, cuboid, u, v)."""
    tw, th = model["tex"]
    items = []
    for pname, _, _, cubs in model["parts"]:
        for c in cubs:
            items.append((pname, c))
    # reuse UVs for identical wheel cuboids: wheels share texture
    out = []
    cache = {}
    x = y = shelf = 0
    for pname, c in items:
        key = c if c[6] in ("tire", "hub") else None
        if key is not None and key in cache:
            out.append((pname, c) + cache[key])
            continue
        w, h, d = c[3], c[4], c[5]
        fw, fh = 2 * (w + d), d + h
        if x + fw > tw:
            x, y, shelf = 0, y + shelf, 0
        if y + fh > th:
            raise SystemExit(f"texture too small for {model['class']}")
        out.append((pname, c, x, y))
        if key is not None:
            cache[key] = (x, y)
        x += fw
        shelf = max(shelf, fh)
    return out


def role_color(role, base):
    return {
        "color": base, "dark": (48, 48, 54), "metal": (150, 152, 160), "black": (28, 28, 30),
        "tire": (34, 34, 34), "hub": (185, 185, 190), "seat": (22, 22, 24), "plate": (236, 236, 236),
    }[role]


def paint_cuboid(img, u, v, w, h, d, role, base):
    px = img.load()
    col = role_color(role, base)
    faces = {
        "top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d),
        "side1": (u, v + d, d, h), "front": (u + d, v + d, w, h),
        "side2": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h),
    }
    for name, (fx, fy, fw, fh) in faces.items():
        for i in range(fw):
            for j in range(fh):
                c = col
                if name == "top":
                    c = shade(c, 22)
                elif name == "bottom":
                    c = shade(c, -25)
                if role == "tire" and (i + j) % 3 == 0:
                    c = shade(c, 18)
                if role == "color" and name in ("side1", "side2") and fh >= 3 and j == fh // 2:
                    c = (245, 245, 245)
                if role == "plate" and name in ("front", "back") and 0 < i < fw - 1 and 0 < j < fh - 1 and (i == fw // 2):
                    c = (20, 20, 20)
                if i == 0 or j == 0 or i == fw - 1 or j == fh - 1:
                    c = shade(c, -14)
                px[fx + i, fy + j] = noisy(c, 6 if role in ("color", "metal") else 4)


def gen_model(model):
    placed = pack(model)
    tw, th = model["tex"]
    for cname, base in COLORS.items():
        img = Image.new("RGBA", (tw, th), (0, 0, 0, 0))
        random.seed(hash(cname) & 0xffff)
        for pname, c, u, v in placed:
            paint_cuboid(img, u, v, c[3], c[4], c[5], c[6], base)
        img.save(mk(f"{A}/textures/entity/{model['layer']}_{cname}.png"))

    def f(v):
        return f"{float(v)}f"

    lines = []
    for pname, parent, pivot, cubs in model["parts"]:
        chain = "ModelPartBuilder.create()"
        for (pp, c, u, v) in placed:
            if pp != pname:
                continue
            x, y, z, w, h, d, role, dil = c
            if dil:
                chain += f".uv({u}, {v}).cuboid({f(x)}, {f(y)}, {f(z)}, {f(w)}, {f(h)}, {f(d)}, new Dilation({f(dil)}))"
            else:
                chain += f".uv({u}, {v}).cuboid({f(x)}, {f(y)}, {f(z)}, {f(w)}, {f(h)}, {f(d)})"
        pv = pname.replace("-", "_")
        lines.append(f"        ModelPartData {pv} = {parent}.addChild(\"{pname}\", {chain},\n"
                     f"                ModelTransform.pivot({f(pivot[0])}, {f(pivot[1])}, {f(pivot[2])}));")
    return "\n".join(lines)


def write_bike_model():
    body = gen_model(BIKE)
    src = f'''package {PKG}.client;

import {PKG}.DirtLegends;
import {PKG}.entity.MotoVehicleEntity;
import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

/** Blocky motocross bike. Generated by tools/gen.py (shapes and texture layout must match). */
public class DirtBikeModel extends SinglePartEntityModel<MotoVehicleEntity> {{
    public static final EntityModelLayer LAYER = new EntityModelLayer(Identifier.of(DirtLegends.MOD_ID, "dirt_bike"), "main");
    private final ModelPart top;
    private final ModelPart root;
    private final ModelPart frame;
    private final ModelPart fork;
    private final ModelPart frontWheel;
    private final ModelPart rearWheel;

    public DirtBikeModel(ModelPart top) {{
        this.top = top;
        this.root = top.getChild("root");
        this.frame = root.getChild("frame");
        this.fork = root.getChild("fork");
        this.frontWheel = fork.getChild("front_wheel");
        this.rearWheel = root.getChild("rear_wheel");
    }}

    @SuppressWarnings("unused")
    public static TexturedModelData getTexturedModelData() {{
        ModelData data = new ModelData();
        ModelPartData base = data.getRoot();
        ModelPartData root = base.addChild("root", ModelPartBuilder.create(), ModelTransform.pivot(0.0f, 24.0f, 0.0f));
{body}
        return TexturedModelData.of(data, {BIKE['tex'][0]}, {BIKE['tex'][1]});
    }}

    @Override
    public ModelPart getPart() {{
        return top;
    }}

    @Override
    public void setAngles(MotoVehicleEntity entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {{
        float tickDelta = MathHelper.clamp(animationProgress - entity.age, 0.0f, 1.0f);
        float comp = entity.getSuspension(tickDelta) * 2.5f;
        root.pitch = entity.getBodyPitch(tickDelta) * MathHelper.RADIANS_PER_DEGREE;
        root.roll = -entity.getLean(tickDelta) * MathHelper.RADIANS_PER_DEGREE;
        fork.yaw = entity.getSteerAngle(tickDelta) * MathHelper.RADIANS_PER_DEGREE;
        frame.pivotY = comp;
        fork.pivotY = -16.0f + comp;
        frontWheel.pivotY = 11.0f - comp;
        float spin = entity.getWheelSpin(tickDelta);
        frontWheel.pitch = spin;
        rearWheel.pitch = spin;
    }}
}}
'''
    with open(mk(f"{J}/client/DirtBikeModel.java"), "w") as fh:
        fh.write(src)


def write_atv_model():
    body = gen_model(ATV)
    src = f'''package {PKG}.client;

import {PKG}.DirtLegends;
import {PKG}.entity.MotoVehicleEntity;
import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

/** Blocky four-wheel ATV. Generated by tools/gen.py (shapes and texture layout must match). */
public class AtvModel extends SinglePartEntityModel<MotoVehicleEntity> {{
    public static final EntityModelLayer LAYER = new EntityModelLayer(Identifier.of(DirtLegends.MOD_ID, "atv"), "main");
    private final ModelPart top;
    private final ModelPart root;
    private final ModelPart frame;
    private final ModelPart bars;
    private final ModelPart frontLeft;
    private final ModelPart frontRight;
    private final ModelPart rearLeft;
    private final ModelPart rearRight;

    public AtvModel(ModelPart top) {{
        this.top = top;
        this.root = top.getChild("root");
        this.frame = root.getChild("frame");
        this.bars = root.getChild("bars");
        this.frontLeft = root.getChild("front_left");
        this.frontRight = root.getChild("front_right");
        this.rearLeft = root.getChild("rear_left");
        this.rearRight = root.getChild("rear_right");
    }}

    @SuppressWarnings("unused")
    public static TexturedModelData getTexturedModelData() {{
        ModelData data = new ModelData();
        ModelPartData base = data.getRoot();
        ModelPartData root = base.addChild("root", ModelPartBuilder.create(), ModelTransform.pivot(0.0f, 24.0f, 0.0f));
{body}
        return TexturedModelData.of(data, {ATV['tex'][0]}, {ATV['tex'][1]});
    }}

    @Override
    public ModelPart getPart() {{
        return top;
    }}

    @Override
    public void setAngles(MotoVehicleEntity entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {{
        float tickDelta = MathHelper.clamp(animationProgress - entity.age, 0.0f, 1.0f);
        float steer = entity.getSteerAngle(tickDelta) * MathHelper.RADIANS_PER_DEGREE;
        float comp = entity.getSuspension(tickDelta) * 2.0f;
        root.pitch = entity.getBodyPitch(tickDelta) * MathHelper.RADIANS_PER_DEGREE;
        root.roll = -entity.getLean(tickDelta) * MathHelper.RADIANS_PER_DEGREE;
        frame.pivotY = comp;
        bars.pivotY = -14.0f + comp;
        bars.yaw = steer;
        frontLeft.yaw = steer;
        frontRight.yaw = steer;
        float spin = entity.getWheelSpin(tickDelta);
        frontLeft.pitch = spin;
        frontRight.pitch = spin;
        rearLeft.pitch = spin;
        rearRight.pitch = spin;
    }}
}}
'''
    with open(mk(f"{J}/client/AtvModel.java"), "w") as fh:
        fh.write(src)

# ---------------------------------------------------------------- 16x16 sprites

def canvas(n=16):
    return Image.new("RGBA", (n, n), (0, 0, 0, 0))


def put(img, x, y, c):
    if 0 <= x < img.width and 0 <= y < img.height:
        img.putpixel((x, y), tuple(c) + ((255,) if len(c) == 3 else ()))


def rect(img, x0, y0, x1, y1, c):
    for x in range(x0, x1 + 1):
        for y in range(y0, y1 + 1):
            put(img, x, y, c)


def wheel(img, cx, cy, r, tire=(30, 30, 30), hub=(170, 170, 175)):
    for x in range(cx - r - 1, cx + r + 2):
        for y in range(cy - r - 1, cy + r + 2):
            dist = math.hypot(x - cx, y - cy)
            if dist <= r + 0.3:
                put(img, x, y, tire if dist > r - 1.4 else hub if dist < 1.2 else (60, 60, 64))


def bike_sprite(base):
    img = canvas()
    wheel(img, 3, 12, 3)
    wheel(img, 12, 12, 3)
    dark = shade(base, -50)
    # swingarm and frame
    for i in range(4, 9):
        put(img, i, 11, (140, 140, 148))
    rect(img, 6, 8, 8, 10, (120, 120, 128))  # engine
    rect(img, 5, 6, 10, 7, base)  # tank / shroud
    rect(img, 8, 5, 13, 5, (25, 25, 25))  # seat
    rect(img, 11, 6, 14, 6, base)  # rear fender
    put(img, 15, 6, dark)
    # fork + bars
    for i in range(0, 5):
        put(img, 4 - i // 2, 5 + i + 1, (150, 150, 158))
    rect(img, 3, 4, 5, 4, (25, 25, 25))
    put(img, 5, 3, (25, 25, 25))
    rect(img, 1, 8, 4, 8, base)  # front fender
    put(img, 7, 7, (245, 245, 245))
    put(img, 9, 9, (140, 140, 148))
    rect(img, 9, 9, 12, 9, (150, 150, 158))  # exhaust
    return img


def atv_sprite(base):
    img = canvas()
    wheel(img, 3, 12, 3)
    wheel(img, 12, 12, 3)
    rect(img, 1, 7, 14, 8, base)
    rect(img, 0, 8, 5, 8, shade(base, -40))
    rect(img, 10, 8, 15, 8, shade(base, -40))
    rect(img, 4, 9, 11, 10, (50, 50, 56))
    rect(img, 7, 5, 11, 6, (25, 25, 25))  # seat
    rect(img, 4, 5, 6, 6, base)
    rect(img, 3, 3, 6, 3, (25, 25, 25))  # bars
    put(img, 5, 4, (150, 150, 158))
    rect(img, 1, 6, 3, 6, (60, 60, 66))
    rect(img, 12, 6, 14, 6, (60, 60, 66))
    return img


def gear_sprites():
    red, white, black = (210, 34, 34), (240, 240, 240), (28, 28, 30)
    out = {}
    img = canvas()  # helmet
    for x in range(2, 14):
        for y in range(2, 13):
            if math.hypot(x - 7.5, y - 7.5) < 6.2:
                put(img, x, y, red if y < 6 or x < 5 else white)
    rect(img, 8, 6, 13, 9, (40, 40, 48))
    rect(img, 9, 7, 12, 8, (120, 170, 220))
    rect(img, 3, 2, 12, 2, black)
    rect(img, 12, 3, 14, 4, red)  # peak
    out["mx_helmet"] = img
    img = canvas()  # jersey
    rect(img, 4, 2, 11, 14, red)
    rect(img, 1, 3, 3, 9, red)
    rect(img, 12, 3, 14, 9, red)
    rect(img, 1, 9, 3, 9, white)
    rect(img, 12, 9, 14, 9, white)
    rect(img, 6, 2, 9, 3, black)
    rect(img, 6, 6, 7, 11, white)
    rect(img, 8, 6, 9, 6, white)
    rect(img, 9, 7, 9, 8, white)
    rect(img, 8, 8, 8, 8, white)
    rect(img, 4, 13, 11, 14, black)
    out["mx_jersey"] = img
    img = canvas()  # pants
    rect(img, 3, 2, 12, 4, black)
    rect(img, 3, 5, 7, 14, white)
    rect(img, 8, 5, 12, 14, white)
    rect(img, 3, 7, 4, 14, red)
    rect(img, 11, 7, 12, 14, red)
    rect(img, 7, 6, 8, 14, (0, 0, 0, 0))
    rect(img, 3, 12, 12, 12, black)
    out["mx_pants"] = img
    img = canvas()  # boots
    for ox in (1, 8):
        rect(img, ox + 1, 3, ox + 4, 11, white)
        rect(img, ox + 1, 11, ox + 6, 13, black)
        rect(img, ox + 1, 5, ox + 4, 5, (150, 150, 158))
        rect(img, ox + 1, 8, ox + 4, 8, (150, 150, 158))
        rect(img, ox + 1, 3, ox + 4, 3, red)
    out["mx_boots"] = img
    img = canvas()  # engine
    rect(img, 3, 5, 12, 12, (120, 120, 128))
    for y in range(5, 12, 2):
        rect(img, 3, y, 12, y, (90, 90, 96))
    rect(img, 5, 2, 10, 4, (150, 150, 158))
    rect(img, 6, 1, 9, 1, (60, 60, 66))
    rect(img, 12, 8, 14, 9, (180, 50, 30))
    rect(img, 2, 12, 13, 13, (50, 50, 56))
    out["engine"] = img
    img = canvas()  # tire
    wheel(img, 7, 7, 6, tire=(32, 32, 32), hub=(170, 170, 175))
    for a in range(0, 360, 30):
        x = 7 + round(6 * math.cos(math.radians(a)))
        y = 7 + round(6 * math.sin(math.radians(a)))
        put(img, x, y, (60, 60, 60))
    out["tire"] = img
    return out

# ---------------------------------------------------------------- block textures

def tex_track_dirt(top=False):
    img = canvas()
    for x in range(16):
        for y in range(16):
            c = (120, 84, 54)
            r = random.random()
            if r < 0.18:
                c = (98, 66, 40)
            elif r < 0.3:
                c = (140, 100, 66)
            if top and x in (4, 5, 11, 12) and random.random() < 0.7:
                c = (88, 60, 38)  # ruts
            put(img, x, y, noisy(c, 6)[:3])
    return img


def tex_checker():
    img = canvas()
    for x in range(16):
        for y in range(16):
            put(img, x, y, (240, 240, 240) if ((x // 4) + (y // 4)) % 2 == 0 else (22, 22, 22))
    return img


def tex_checkpoint():
    img = canvas()
    for x in range(16):
        for y in range(16):
            band = ((abs(x - 7.5) + y) // 3) % 2
            put(img, x, y, (245, 200, 20) if band == 0 else (25, 25, 25))
    return img


def tex_kicker(top):
    img = canvas()
    for x in range(16):
        for y in range(16):
            c = (176, 132, 82) if (y // 4) % 2 == 0 else (160, 118, 72)
            if y % 4 == 3:
                c = (110, 80, 48)
            put(img, x, y, noisy(c, 5)[:3])
    if top:
        for x in range(16):
            for y in range(0, 2):
                put(img, x, y, (245, 200, 20) if (x // 2) % 2 == 0 else (25, 25, 25))
    return img


def tex_line_side():
    img = tex_track_dirt()
    for x in range(16):
        for y in range(0, 3):
            put(img, x, y, (240, 240, 240) if ((x // 4) + y // 3) % 2 == 0 else (22, 22, 22))
    return img


def tex_cp_side():
    img = tex_track_dirt()
    for x in range(16):
        for y in range(0, 3):
            put(img, x, y, (245, 200, 20) if (x // 2) % 2 == 0 else (25, 25, 25))
    return img

# ---------------------------------------------------------------- armor layers (64x32)

def armor_textures():
    red, white, black, grey = (210, 34, 34), (240, 240, 240), (28, 28, 30), (150, 150, 158)

    def box(img, u, v, w, h, d, fn):
        faces = [(u + d, v, w, d, "top"), (u + d + w, v, w, d, "bottom"), (u, v + d, d, h, "right"),
                 (u + d, v + d, w, h, "front"), (u + d + w, v + d, d, h, "left"), (u + 2 * d + w, v + d, w, h, "back")]
        for fx, fy, fw, fh, name in faces:
            for i in range(fw):
                for j in range(fh):
                    c = fn(name, i, j, fw, fh)
                    if c is not None:
                        put(img, fx + i, fy + j, noisy(c, 4)[:3])

    l1 = Image.new("RGBA", (64, 32), (0, 0, 0, 0))

    def helmet(name, i, j, fw, fh):
        if name == "front" and 2 <= j <= 4 and 1 <= i <= 6:
            return (40, 40, 48) if j != 3 else (120, 170, 220)
        if name == "front" and j >= 6 and 2 <= i <= 5:
            return grey  # chin bar vent
        if name in ("right", "left") and j == 4:
            return white
        if name == "top" and i in (3, 4):
            return white
        return red

    box(l1, 0, 0, 8, 8, 8, helmet)  # head

    def jersey(name, i, j, fw, fh):
        if name == "front" and 3 <= i <= 4 and 3 <= j <= 8:
            return white
        if name == "back" and 2 <= i <= 5 and 2 <= j <= 7 and (i in (2, 5) or j in (2, 7)):
            return white
        if j >= fh - 1 and name not in ("top", "bottom"):
            return black
        return red

    box(l1, 16, 16, 8, 12, 4, jersey)  # body

    def sleeve(name, i, j, fw, fh):
        if name not in ("top", "bottom") and j in (6, 7):
            return white
        if name not in ("top", "bottom") and j >= 10:
            return black
        return red

    box(l1, 40, 16, 4, 12, 4, sleeve)  # arms

    def boots(name, i, j, fw, fh):
        if name == "top":
            return None
        if name == "bottom":
            return black
        if j < 5:
            return None
        if j >= 10:
            return black
        if j in (6, 8):
            return grey
        return white

    box(l1, 0, 16, 4, 12, 4, boots)  # legs region on layer 1 -> boots
    l1.save(mk(f"{A}/textures/models/armor/motocross_layer_1.png"))

    l2 = Image.new("RGBA", (64, 32), (0, 0, 0, 0))

    def pants(name, i, j, fw, fh):
        if name in ("right", "left") and i in (1, 2) and j >= 2:
            return red
        if j >= 10:
            return black
        return white

    box(l2, 0, 16, 4, 12, 4, pants)

    def belt(name, i, j, fw, fh):
        if name in ("top",):
            return None
        if j >= fh - 3:
            return black
        return None

    box(l2, 16, 16, 8, 12, 4, belt)
    l2.save(mk(f"{A}/textures/models/armor/motocross_layer_2.png"))

# ---------------------------------------------------------------- sound

def engine_sound():
    """Seamless 1-second loop of a single-cylinder 4-stroke idle (pitched up/down in game for RPM)."""
    sr = 44100
    n = sr
    t = np.arange(n) / sr
    f0 = 41.0
    phase = (t * f0) % 1.0
    pulses = np.exp(-phase * 9.0)
    sig = 0.6 * pulses
    for k, amp in [(1, 0.30), (2, 0.28), (3, 0.16), (4, 0.12), (6, 0.07), (9, 0.04)]:
        sig += amp * np.sin(2 * np.pi * f0 * k * t + 0.7 * k)
    rng = np.random.default_rng(3)
    noise = rng.normal(0, 1, n)
    kern = np.ones(8) / 8
    noise = np.real(np.fft.ifft(np.fft.fft(noise) * np.fft.fft(kern, n)))
    sig += 0.35 * noise * (0.3 + pulses)
    sig = np.tanh(sig * 1.6)
    sig -= sig.mean()
    sig *= 0.85 / np.max(np.abs(sig))
    pcm = (sig * 32767).astype(np.int16)
    raw = f"/tmp/engine.raw"
    pcm.tofile(raw)
    out = mk(f"{A}/sounds/engine.ogg")
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-f", "s16le", "-ar", str(sr), "-ac", "1", "-i", raw,
                    "-c:a", "libvorbis", "-q:a", "5", out], check=True)
    os.remove(raw)


# ---------------------------------------------------------------- JSON data

def stairs_blockstate(name):
    base_y = {"east": 0, "south": 90, "west": 180, "north": 270}
    variants = {}
    for facing, by in base_y.items():
        for half in ("bottom", "top"):
            for shape in ("straight", "inner_left", "inner_right", "outer_left", "outer_right"):
                model = f"{MOD}:block/{name}" + ("_inner" if shape.startswith("inner") else "_outer" if shape.startswith("outer") else "")
                if half == "bottom":
                    y = by - 90 if shape.endswith("left") else by
                else:
                    y = by + 90 if shape.endswith("right") else by
                y %= 360
                v = {"model": model}
                if half == "top":
                    v["x"] = 180
                if y:
                    v["y"] = y
                if half == "top" or y:
                    v["uvlock"] = True
                variants[f"facing={facing},half={half},shape={shape}"] = v
    return {"variants": variants}


def data_files():
    tex = lambda n: f"{MOD}:block/{n}"
    # simple cube blocks
    cubes = {
        "track_dirt": {"parent": "minecraft:block/cube_bottom_top",
                       "textures": {"top": tex("track_dirt_top"), "bottom": tex("track_dirt"), "side": tex("track_dirt")}},
        "start_finish_line": {"parent": "minecraft:block/cube_bottom_top",
                              "textures": {"top": tex("start_finish_line"), "bottom": tex("track_dirt"), "side": tex("start_finish_line_side")}},
        "checkpoint": {"parent": "minecraft:block/cube_bottom_top",
                       "textures": {"top": tex("checkpoint"), "bottom": tex("track_dirt"), "side": tex("checkpoint_side")}},
    }
    for name, model in cubes.items():
        wjson(f"{A}/models/block/{name}.json", model)
        wjson(f"{A}/blockstates/{name}.json", {"variants": {"": {"model": f"{MOD}:block/{name}"}}})
        wjson(f"{A}/models/item/{name}.json", {"parent": f"{MOD}:block/{name}"})

    dirt_tex = {"bottom": tex("track_dirt"), "top": tex("track_dirt_top"), "side": tex("track_dirt")}
    # ramp (stairs)
    wjson(f"{A}/models/block/dirt_ramp.json", {"parent": "minecraft:block/stairs", "textures": dirt_tex})
    wjson(f"{A}/models/block/dirt_ramp_inner.json", {"parent": "minecraft:block/inner_stairs", "textures": dirt_tex})
    wjson(f"{A}/models/block/dirt_ramp_outer.json", {"parent": "minecraft:block/outer_stairs", "textures": dirt_tex})
    wjson(f"{A}/blockstates/dirt_ramp.json", stairs_blockstate("dirt_ramp"))
    wjson(f"{A}/models/item/dirt_ramp.json", {"parent": f"{MOD}:block/dirt_ramp"})
    # slab
    wjson(f"{A}/models/block/dirt_slab.json", {"parent": "minecraft:block/slab", "textures": dirt_tex})
    wjson(f"{A}/models/block/dirt_slab_top.json", {"parent": "minecraft:block/slab_top", "textures": dirt_tex})
    wjson(f"{A}/blockstates/dirt_slab.json", {"variants": {
        "type=bottom": {"model": f"{MOD}:block/dirt_slab"},
        "type=top": {"model": f"{MOD}:block/dirt_slab_top"},
        "type=double": {"model": f"{MOD}:block/track_dirt"}}})
    wjson(f"{A}/models/item/dirt_slab.json", {"parent": f"{MOD}:block/dirt_slab"})
    # kicker: 4 steps rising toward north (z=0)
    elements = []
    for i in range(4):
        a, b, h = i * 4, i * 4 + 4, 2 + i * 2
        faces = {
            "up": {"texture": "#top"},
            "down": {"texture": "#side", "cullface": "down"},
            "north": {"texture": "#side"}, "south": {"texture": "#side"},
            "east": {"texture": "#side"}, "west": {"texture": "#side"},
        }
        if i == 0:
            faces["south"]["cullface"] = "south"
        if i == 3:
            faces["north"]["cullface"] = "north"
        elements.append({"from": [0, 0, 16 - b], "to": [16, h, 16 - a], "faces": faces})
    wjson(f"{A}/models/block/kicker_ramp.json", {
        "parent": "minecraft:block/block",
        "textures": {"particle": tex("kicker_ramp_side"), "top": tex("kicker_ramp_top"), "side": tex("kicker_ramp_side")},
        "elements": elements})
    wjson(f"{A}/blockstates/kicker_ramp.json", {"variants": {
        "facing=north": {"model": f"{MOD}:block/kicker_ramp"},
        "facing=east": {"model": f"{MOD}:block/kicker_ramp", "y": 90},
        "facing=south": {"model": f"{MOD}:block/kicker_ramp", "y": 180},
        "facing=west": {"model": f"{MOD}:block/kicker_ramp", "y": 270}}})
    wjson(f"{A}/models/item/kicker_ramp.json", {"parent": f"{MOD}:block/kicker_ramp"})

    # item models
    flat = [f"dirt_bike_{c}" for c in COLOR_NAMES] + [f"atv_{c}" for c in COLOR_NAMES] + \
           ["mx_helmet", "mx_jersey", "mx_pants", "mx_boots", "engine", "tire"]
    for n in flat:
        wjson(f"{A}/models/item/{n}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"{MOD}:item/{n}"}})

    # loot tables
    for b in ["track_dirt", "dirt_ramp", "kicker_ramp", "start_finish_line", "checkpoint"]:
        wjson(f"{D}/{MOD}/loot_table/blocks/{b}.json", {
            "type": "minecraft:block",
            "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"{MOD}:{b}"}],
                       "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
    wjson(f"{D}/{MOD}/loot_table/blocks/dirt_slab.json", {
        "type": "minecraft:block",
        "pools": [{"rolls": 1, "entries": [{
            "type": "minecraft:item", "name": f"{MOD}:dirt_slab",
            "functions": [{"function": "minecraft:set_count", "count": 2, "add": False,
                           "conditions": [{"condition": "minecraft:block_state_property", "block": f"{MOD}:dirt_slab",
                                           "properties": {"type": "double"}}]},
                          {"function": "minecraft:explosion_decay"}]}]}]})
    wjson(f"{D}/minecraft/tags/block/mineable/shovel.json",
          {"replace": False, "values": [f"{MOD}:track_dirt", f"{MOD}:dirt_ramp", f"{MOD}:dirt_slab"]})
    wjson(f"{D}/minecraft/tags/block/mineable/axe.json", {"replace": False, "values": [f"{MOD}:kicker_ramp"]})
    wjson(f"{D}/minecraft/tags/block/mineable/pickaxe.json",
          {"replace": False, "values": [f"{MOD}:start_finish_line", f"{MOD}:checkpoint"]})
    wjson(f"{D}/minecraft/tags/block/slabs.json", {"replace": False, "values": [f"{MOD}:dirt_slab"]})
    wjson(f"{D}/minecraft/tags/block/stairs.json", {"replace": False, "values": [f"{MOD}:dirt_ramp"]})

    # recipes
    R = f"{D}/{MOD}/recipe"

    def item(i):
        return {"tag": i[1:]} if i.startswith("#") else {"item": i}

    def shaped(name, pattern, key, result, count=1, cat="misc"):
        wjson(f"{R}/{name}.json", {"type": "minecraft:crafting_shaped", "category": cat, "pattern": pattern,
                                    "key": {k: item(v) for k, v in key.items()},
                                    "result": {"id": result, "count": count}})

    shaped("engine", ["IRI", "IPI", "III"], {"I": "minecraft:iron_ingot", "R": "minecraft:redstone", "P": "minecraft:piston"}, f"{MOD}:engine")
    shaped("tire", [" K ", "KNK", " K "], {"K": "minecraft:dried_kelp", "N": "minecraft:iron_nugget"}, f"{MOD}:tire", 2)
    dyes = {"red": "minecraft:red_dye", "blue": "minecraft:blue_dye", "green": "minecraft:lime_dye", "yellow": "minecraft:yellow_dye"}
    for c, dye in dyes.items():
        shaped(f"dirt_bike_{c}", ["D I", "IEI", "T T"], {"D": dye, "I": "minecraft:iron_ingot", "E": f"{MOD}:engine", "T": f"{MOD}:tire"},
               f"{MOD}:dirt_bike_{c}", cat="equipment")
        shaped(f"atv_{c}", ["TDT", "IEI", "T T"], {"D": dye, "I": "minecraft:iron_ingot", "E": f"{MOD}:engine", "T": f"{MOD}:tire"},
               f"{MOD}:atv_{c}", cat="equipment")
    shaped("mx_helmet", ["LRL", "LGL"], {"L": "minecraft:leather", "R": "minecraft:red_dye", "G": "minecraft:glass_pane"}, f"{MOD}:mx_helmet", cat="equipment")
    shaped("mx_jersey", ["L L", "RWR", "LLL"], {"L": "minecraft:leather", "R": "minecraft:red_dye", "W": "minecraft:white_wool"}, f"{MOD}:mx_jersey", cat="equipment")
    shaped("mx_pants", ["LIL", "W W", "L L"], {"L": "minecraft:leather", "I": "minecraft:iron_ingot", "W": "minecraft:white_wool"}, f"{MOD}:mx_pants", cat="equipment")
    shaped("mx_boots", ["I I", "L L", "L L"], {"I": "minecraft:iron_nugget", "L": "minecraft:leather"}, f"{MOD}:mx_boots", cat="equipment")
    shaped("track_dirt", ["DC", "CD"], {"D": "minecraft:dirt", "C": "minecraft:coarse_dirt"}, f"{MOD}:track_dirt", 4, "building")
    shaped("dirt_ramp", ["T  ", "TT ", "TTT"], {"T": f"{MOD}:track_dirt"}, f"{MOD}:dirt_ramp", 4, "building")
    shaped("dirt_slab", ["TTT"], {"T": f"{MOD}:track_dirt"}, f"{MOD}:dirt_slab", 6, "building")
    shaped("kicker_ramp", ["  P", " PP", "SSS"], {"P": "#minecraft:planks", "S": "#minecraft:wooden_slabs"}, f"{MOD}:kicker_ramp", 2, "building")
    shaped("start_finish_line", ["WBW", "TTT"], {"W": "minecraft:white_wool", "B": "minecraft:black_wool", "T": f"{MOD}:track_dirt"},
           f"{MOD}:start_finish_line", 3, "building")
    shaped("checkpoint", ["YBY", "TTT"], {"Y": "minecraft:yellow_wool", "B": "minecraft:black_wool", "T": f"{MOD}:track_dirt"},
           f"{MOD}:checkpoint", 3, "building")

    # sounds
    wjson(f"{A}/sounds.json", {
        "engine": {"sounds": [f"{MOD}:engine"], "subtitle": f"subtitles.{MOD}.engine"}})

    # lang
    lang = {
        f"itemGroup.{MOD}": "Dirt Legends",
        f"entity.{MOD}.dirt_bike": "Dirt Bike",
        f"entity.{MOD}.atv": "ATV",
        f"item.{MOD}.engine": "Engine",
        f"item.{MOD}.tire": "Knobby Tire",
        f"item.{MOD}.mx_helmet": "Motocross Helmet",
        f"item.{MOD}.mx_jersey": "Motocross Jersey",
        f"item.{MOD}.mx_pants": "Motocross Pants",
        f"item.{MOD}.mx_boots": "Motocross Boots",
        f"block.{MOD}.track_dirt": "Track Dirt",
        f"block.{MOD}.dirt_ramp": "Dirt Ramp",
        f"block.{MOD}.dirt_slab": "Dirt Slab",
        f"block.{MOD}.kicker_ramp": "Kicker Ramp",
        f"block.{MOD}.start_finish_line": "Start/Finish Line",
        f"block.{MOD}.checkpoint": "Checkpoint",
        f"subtitles.{MOD}.engine": "Engine revs",
        f"tooltip.{MOD}.vehicle": "Right-click a block to place, right-click to ride",
    }
    for c in COLOR_NAMES:
        lang[f"item.{MOD}.dirt_bike_{c}"] = f"{c.capitalize()} Dirt Bike"
        lang[f"item.{MOD}.atv_{c}"] = f"{c.capitalize()} ATV"
    wjson(f"{A}/lang/en_us.json", lang)


def main():
    write_bike_model()
    write_atv_model()
    for c, base in COLORS.items():
        bike_sprite(base).save(mk(f"{A}/textures/item/dirt_bike_{c}.png"))
        atv_sprite(base).save(mk(f"{A}/textures/item/atv_{c}.png"))
    for n, img in gear_sprites().items():
        img.save(mk(f"{A}/textures/item/{n}.png"))
    tex_track_dirt().save(mk(f"{A}/textures/block/track_dirt.png"))
    tex_track_dirt(top=True).save(mk(f"{A}/textures/block/track_dirt_top.png"))
    tex_checker().save(mk(f"{A}/textures/block/start_finish_line.png"))
    tex_line_side().save(mk(f"{A}/textures/block/start_finish_line_side.png"))
    tex_checkpoint().save(mk(f"{A}/textures/block/checkpoint.png"))
    tex_cp_side().save(mk(f"{A}/textures/block/checkpoint_side.png"))
    tex_kicker(True).save(mk(f"{A}/textures/block/kicker_ramp_top.png"))
    tex_kicker(False).save(mk(f"{A}/textures/block/kicker_ramp_side.png"))
    armor_textures()
    bike_sprite(COLORS["red"]).resize((128, 128), Image.NEAREST).save(mk(f"{A}/icon.png"))
    engine_sound()
    data_files()
    print("generated")


main()
