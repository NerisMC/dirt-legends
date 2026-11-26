#!/usr/bin/env python3
"""Dirt Legends 2.1 assets: fan, racer and vendor skins, concession food icons, and their lang/model JSON.
Run by gen_extra.py (which gen.py runs), or on its own: python3 tools/gen_track_life.py <project root>."""
import json, os, random, sys
from PIL import Image

ROOT = sys.argv[1] if len(sys.argv) > 1 else "."
MOD = "dirtlegends"
A = f"{ROOT}/src/main/resources/assets/{MOD}"


def mk(p):
    os.makedirs(os.path.dirname(p), exist_ok=True)
    return p


def wj(p, o):
    json.dump(o, open(mk(p), "w"), indent=2)


def clamp(c):
    return tuple(max(0, min(255, int(v))) for v in c)


def shade(c, d):
    return clamp((c[0] + d, c[1] + d, c[2] + d))


SKIN_TONES = [(255, 219, 172), (241, 194, 125), (224, 172, 105), (198, 134, 66), (141, 85, 36), (99, 57, 30), (236, 188, 150)]
HAIR = [(20, 16, 14), (60, 38, 22), (110, 70, 35), (200, 160, 90), (150, 40, 30), (90, 90, 95)]
TEAM = [(210, 30, 30), (30, 80, 200), (240, 190, 20), (240, 120, 20), (60, 170, 60), (140, 60, 170), (20, 20, 22), (240, 240, 240)]
PANTS = [(45, 70, 130), (35, 50, 95), (190, 165, 120), (30, 30, 34), (90, 90, 96)]


class Skin:
    def __init__(self, seed):
        self.r = random.Random(seed)
        self.img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))

    def box(self, u, v, w, h, d, fn):
        faces = [(u + d, v, w, d, "top"), (u + d + w, v, w, d, "bottom"), (u, v + d, d, h, "right"),
                 (u + d, v + d, w, h, "front"), (u + d + w, v + d, d, h, "left"), (u + 2 * d + w, v + d, w, h, "back")]
        for fx, fy, fw, fh, name in faces:
            for a in range(fw):
                for b in range(fh):
                    c = fn(name, a, b, fw, fh)
                    if c is not None:
                        self.img.putpixel((fx + a, fy + b), shade(c, self.r.randint(-4, 4)) + (255,))

    def face(self, tone, hair, bald=False, long_hair=False):
        eye = self.r.choice([(60, 40, 25), (40, 90, 160), (60, 120, 60)])

        def head(n, a, b, fw, fh):
            if n == "top":
                return tone if bald else hair
            if n == "bottom":
                return tone
            if n == "front":
                if b == 0 and not bald:
                    return hair
                if b == 4 and a in (1, 2, 5, 6):
                    return (245, 245, 245) if a in (1, 6) else eye
                if b == 3 and a in (1, 2, 5, 6) and not bald:
                    return shade(hair, -10)
                if b == 5 and a in (3, 4):
                    return shade(tone, -25)
                if b == 6 and 2 <= a <= 5:
                    return (150, 80, 70)
                return tone
            if bald:
                return tone
            if n == "back":
                return hair if b < (8 if long_hair else 5) else tone
            return hair if b < (6 if long_hair else 2) else tone
        self.box(0, 0, 8, 8, 8, head)

    def body(self, torso, sleeve_len, sleeve, arm_skin, legs, shoes):
        self.box(16, 16, 8, 12, 4, torso)

        def arm(n, a, b, fw, fh):
            if n == "bottom":
                return arm_skin
            return sleeve(n, a, b) if b < sleeve_len else arm_skin
        self.box(40, 16, 4, 12, 4, arm)
        self.box(32, 48, 4, 12, 4, arm)

        def leg(n, a, b, fw, fh):
            if b >= 10 or n == "bottom":
                return shoes
            return legs(n, a, b)
        self.box(0, 16, 4, 12, 4, leg)
        self.box(16, 48, 4, 12, 4, leg)

    def hat(self, fn):
        self.box(32, 0, 8, 8, 8, fn)

    def save(self, path):
        self.img.save(mk(path))


def fan(i):
    s = Skin(500 + i)
    r = s.r
    tone, hair = r.choice(SKIN_TONES), r.choice(HAIR)
    long_hair = r.random() < 0.4
    s.face(tone, hair, long_hair=long_hair)
    team = r.choice(TEAM)
    number = r.random() < 0.5
    pants = r.choice(PANTS)
    shorts = r.random() < 0.4

    def torso(n, a, b, fw, fh):
        if b >= 10:
            return shade(pants, -10)
        if n in ("front", "back") and b in (2, 3) and 1 <= a <= 6:
            return (240, 240, 240) if team != (240, 240, 240) else (20, 20, 22)   # team stripe
        if n == "back" and number and 5 <= b <= 8 and a in (3, 4):
            return (240, 240, 240)
        return team
    s.body(torso, 4, lambda n, a, b: team, tone,
           lambda n, a, b: tone if shorts and b >= 5 else pants, r.choice([(240, 240, 240), (25, 25, 25), (120, 70, 40)]))
    if r.random() < 0.45:                                    # trucker cap in team colours
        cap = r.choice(TEAM)
        s.hat(lambda n, a, b, fw, fh: None if n == "bottom" else cap if n == "top" or b < 2
              else (shade(cap, -40) if n == "front" and b == 2 else None))
    elif long_hair:
        s.hat(lambda n, a, b, fw, fh: hair if n in ("back", "left", "right", "top") and b < 7 and n != "bottom" else None)
    s.save(f"{A}/textures/entity/spectator/fan_{i}.png")


def racer(i):
    s = Skin(900 + i)
    r = s.r
    tone, hair = r.choice(SKIN_TONES), r.choice(HAIR)
    s.face(tone, hair)
    main = TEAM[i % len(TEAM)]
    accent = TEAM[(i * 3 + 1) % len(TEAM)]
    if accent == main:
        accent = (240, 240, 240)

    def jersey(n, a, b, fw, fh):
        if b >= 10:
            return (30, 30, 34)
        if n == "front" and 3 <= b <= 6 and 2 <= a <= 5:
            return accent                                      # chest panel
        if n == "back" and 3 <= b <= 7 and a in (3, 4):
            return (240, 240, 240)                             # race number
        return main

    def pants(n, a, b):
        if 4 <= b <= 5:
            return accent                                      # knee guard
        return main if b < 4 else shade(main, -30)
    s.body(jersey, 11, lambda n, a, b: accent if b in (2, 3) else main, (30, 30, 34), pants, (25, 25, 25))

    def helmet(n, a, b, fw, fh):
        if n == "bottom":
            return None
        if n == "front":
            if b in (3, 4) and 1 <= a <= 6:
                return (40, 40, 44) if a in (1, 6) else (90, 170, 220)    # goggles
            if b >= 5 and 2 <= a <= 5:
                return shade(main, -40)                                    # chin bar
            if b < 2:
                return accent                                              # visor/peak
            if b >= 5:
                return None
            return main
        if n == "top":
            return accent if 3 <= a <= 4 else main
        return main if b < 6 else None
    s.hat(helmet)
    s.save(f"{A}/textures/entity/racer/racer_{i}.png")


def vendor(kind):
    s = Skin(1300 + kind)
    tone, hair = s.r.choice(SKIN_TONES), s.r.choice(HAIR)
    s.face(tone, hair)
    if kind == 0:   # concessions: red shirt, white apron, paper hat
        def torso(n, a, b, fw, fh):
            if n == "front" and 1 <= a <= 6 and b >= 1:
                return (240, 240, 240) if b < 11 else (230, 230, 230)
            return (200, 30, 30) if b < 10 else (40, 40, 44)
        s.body(torso, 4, lambda n, a, b: (200, 30, 30), tone,
               lambda n, a, b: (240, 240, 240) if n == "front" and b < 4 else (40, 40, 44), (25, 25, 25))
        s.hat(lambda n, a, b, fw, fh: None if n == "bottom" else (245, 245, 245) if n == "top" or b < 2
              else ((200, 30, 30) if b == 2 and n != "top" else None))
    else:           # pit mechanic: navy coveralls, cap
        navy = (36, 50, 92)

        def torso(n, a, b, fw, fh):
            if n == "front" and a == 3:
                return shade(navy, -20)
            if n == "front" and b == 2 and a in (5, 6):
                return (240, 190, 20)                         # name patch
            return navy
        s.body(torso, 11, lambda n, a, b: navy, tone, lambda n, a, b: navy, (60, 40, 25))
        s.hat(lambda n, a, b, fw, fh: None if n == "bottom" else (200, 30, 30) if n == "top" or b < 2
              else ((150, 20, 20) if n == "front" and b == 2 else None))
    s.save(f"{A}/textures/entity/vendor/vendor_{kind}.png")


# ------------------------------------------------------------------ food icons (16x16)
def icon():
    return Image.new("RGBA", (16, 16), (0, 0, 0, 0))


def rect(im, x0, y0, x1, y1, c):
    for x in range(x0, x1 + 1):
        for y in range(y0, y1 + 1):
            if 0 <= x < 16 and 0 <= y < 16:
                im.putpixel((x, y), clamp(c) + (255,))


def food():
    im = icon()  # hot dog
    rect(im, 2, 7, 13, 10, (226, 180, 110)); rect(im, 3, 6, 12, 6, (240, 200, 130))
    rect(im, 1, 7, 14, 8, (170, 70, 50)); rect(im, 2, 7, 13, 7, (190, 85, 60))
    for x in range(3, 13, 2):
        rect(im, x, 6 + (x // 2) % 2, x, 6 + (x // 2) % 2, (240, 200, 30))
    im.save(mk(f"{A}/textures/item/hot_dog.png"))

    im = icon()  # burger
    rect(im, 3, 3, 12, 6, (220, 150, 70)); rect(im, 2, 5, 13, 6, (220, 150, 70))
    for x in (5, 8, 10):
        rect(im, x, 4, x, 4, (250, 240, 200))
    rect(im, 2, 7, 13, 7, (90, 180, 60)); rect(im, 2, 8, 13, 9, (110, 60, 35))
    rect(im, 2, 10, 13, 10, (240, 200, 40)); rect(im, 3, 11, 12, 12, (210, 140, 65))
    im.save(mk(f"{A}/textures/item/burger.png"))

    im = icon()  # nachos
    rect(im, 1, 10, 14, 13, (230, 230, 230)); rect(im, 2, 13, 13, 13, (200, 200, 200))
    for k in range(5):
        rect(im, 2 + k * 2, 6 + (k % 2), 4 + k * 2, 9, (236, 190, 70))
    rect(im, 3, 8, 12, 9, (250, 200, 30)); rect(im, 6, 7, 6, 7, (60, 160, 60)); rect(im, 10, 8, 10, 8, (200, 40, 30))
    im.save(mk(f"{A}/textures/item/nachos.png"))

    im = icon()  # popcorn
    for x in range(4, 12):
        rect(im, x, 7, x, 14, (220, 40, 40) if (x // 2) % 2 == 0 else (245, 245, 245))
    for (x, y) in [(4, 5), (6, 4), (8, 3), (10, 4), (11, 6), (5, 6), (7, 5), (9, 5)]:
        rect(im, x, y, x + 1, y + 1, (252, 240, 190))
    im.save(mk(f"{A}/textures/item/popcorn.png"))

    im = icon()  # soda cup
    rect(im, 4, 5, 11, 14, (220, 40, 40)); rect(im, 5, 8, 10, 10, (245, 245, 245))
    rect(im, 3, 4, 12, 4, (240, 240, 240)); rect(im, 9, 0, 9, 3, (240, 240, 240)); rect(im, 10, 0, 10, 0, (240, 240, 240))
    im.save(mk(f"{A}/textures/item/soda.png"))

    im = icon()  # energy drink can
    rect(im, 5, 2, 10, 14, (30, 30, 34)); rect(im, 5, 2, 10, 2, (180, 180, 186)); rect(im, 5, 14, 10, 14, (150, 150, 156))
    for k in range(4):
        rect(im, 6 + k % 2, 5 + k * 2, 8 + k % 2, 5 + k * 2, (120, 230, 40))
    rect(im, 7, 4, 9, 4, (120, 230, 40))
    im.save(mk(f"{A}/textures/item/energy_drink.png"))


FOOD = {"hot_dog": "Hot Dog", "burger": "Burger", "nachos": "Nachos", "popcorn": "Popcorn", "soda": "Soda",
        "energy_drink": "Nitro Energy Drink"}


def data():
    for k in FOOD:
        wj(f"{A}/models/item/{k}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"{MOD}:item/{k}"}})
    lp = f"{A}/lang/en_us.json"
    lang = json.load(open(lp)) if os.path.exists(lp) else {}
    for k, v in FOOD.items():
        lang[f"item.{MOD}.{k}"] = v
    lang[f"entity.{MOD}.spectator"] = "Fan"
    lang[f"entity.{MOD}.racer"] = "Rival Racer"
    lang[f"entity.{MOD}.vendor"] = "Vendor"
    wj(lp, lang)


def main():
    for i in range(12):
        fan(i)
    for i in range(6):
        racer(i)
    vendor(0)
    vendor(1)
    food()
    data()
    print("track life assets done")


main()
