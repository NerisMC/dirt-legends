#!/usr/bin/env python3
"""Assets for fuel, gas stations, liquid mud and generated tracks."""
import json, os, random, sys
from PIL import Image
ROOT = sys.argv[1] if len(sys.argv) > 1 else "."
MOD = "dirtlegends"
A = f"{ROOT}/src/main/resources/assets/{MOD}"
D = f"{ROOT}/src/main/resources/data"
random.seed(11)

def mk(p):
    os.makedirs(os.path.dirname(p), exist_ok=True); return p
def wj(p, o):
    json.dump(o, open(mk(p), "w"), indent=2)
def img(): return Image.new("RGBA", (16, 16), (0, 0, 0, 0))
def px(im, x, y, c):
    if 0 <= x < 16 and 0 <= y < 16: im.putpixel((x, y), tuple(c) + ((255,) if len(c) == 3 else ()))
def rect(im, x0, y0, x1, y1, c):
    for x in range(x0, x1 + 1):
        for y in range(y0, y1 + 1): px(im, x, y, c)
def noise(c, a=8):
    d = random.randint(-a, a); return tuple(max(0, min(255, v + d)) for v in c)

RED, DRED, WHITE, BLACK, GREY, YEL = (200, 30, 30), (140, 18, 18), (240, 240, 240), (25, 25, 28), (150, 150, 158), (245, 200, 20)

# ---- fuel pump block textures
side = img()
for x in range(16):
    for y in range(16): px(side, x, y, noise(RED, 6))
rect(side, 0, 3, 15, 4, WHITE); rect(side, 0, 0, 15, 0, DRED); rect(side, 0, 15, 15, 15, DRED)
side.save(mk(f"{A}/textures/block/fuel_pump_side.png"))
front = side.copy()
rect(front, 3, 6, 12, 10, BLACK)          # display
rect(front, 4, 7, 6, 9, (60, 230, 90)); rect(front, 8, 7, 11, 9, (60, 230, 90))
rect(front, 5, 12, 10, 13, GREY)          # nozzle holder
px(front, 12, 12, BLACK); px(front, 12, 13, BLACK)
rect(front, 4, 1, 11, 2, YEL)
front.save(mk(f"{A}/textures/block/fuel_pump_front.png"))
top = img()
for x in range(16):
    for y in range(16): px(top, x, y, noise(GREY, 6))
top.save(mk(f"{A}/textures/block/fuel_pump_top.png"))

# ---- items
can = img()                                # red jerry can
rect(can, 3, 4, 12, 14, RED); rect(can, 4, 5, 11, 13, (220, 40, 40))
rect(can, 3, 2, 8, 3, BLACK); rect(can, 10, 1, 12, 3, GREY)
for i in range(4, 12): px(can, i, 4 + (i - 4), DRED); px(can, i, 13 - (i - 4), DRED)
can.save(mk(f"{A}/textures/item/fuel_can.png"))
bucket = img()                             # bucket of mud
for y in range(5, 15):
    w = 5 - (y - 5) // 4
    rect(bucket, 3 + (5 - w), y, 12 - (5 - w), y, GREY)
rect(bucket, 3, 5, 12, 6, (95, 62, 36)); rect(bucket, 4, 4, 11, 4, (110, 74, 44))
rect(bucket, 2, 5, 2, 7, (110, 110, 118)); rect(bucket, 13, 5, 13, 7, (110, 110, 118))
bucket.save(mk(f"{A}/textures/item/mud_bucket.png"))
kit = img()                                # rolled-out track plan
rect(kit, 1, 2, 14, 13, (230, 220, 190)); rect(kit, 1, 2, 14, 2, (190, 175, 140)); rect(kit, 1, 13, 14, 13, (190, 175, 140))
for x in range(3, 13):
    px(kit, x, 4, (120, 84, 54)); px(kit, x, 11, (120, 84, 54))
for y in range(4, 12):
    px(kit, 3, y, (120, 84, 54)); px(kit, 12, y, (120, 84, 54))
px(kit, 12, 7, BLACK); px(kit, 12, 8, WHITE); px(kit, 7, 7, RED); px(kit, 8, 8, RED)
kit.save(mk(f"{A}/textures/item/track_kit.png"))

# ---- models / blockstates
tex = lambda n: f"{MOD}:block/{n}"
wj(f"{A}/models/block/fuel_pump.json", {
    "parent": "minecraft:block/block",
    "textures": {"particle": tex("fuel_pump_side"), "front": tex("fuel_pump_front"), "side": tex("fuel_pump_side"), "top": tex("fuel_pump_top")},
    "elements": [
        {"from": [3, 0, 4], "to": [13, 15, 12], "faces": {
            "north": {"texture": "#front"}, "south": {"texture": "#side"}, "east": {"texture": "#side"},
            "west": {"texture": "#side"}, "up": {"texture": "#top"}, "down": {"texture": "#top", "cullface": "down"}}},
        {"from": [13, 4, 7], "to": [14, 10, 9], "faces": {d: {"texture": "#top"} for d in ["north", "south", "east", "west", "up", "down"]}}]})
wj(f"{A}/blockstates/fuel_pump.json", {"variants": {
    "facing=north": {"model": f"{MOD}:block/fuel_pump"},
    "facing=east": {"model": f"{MOD}:block/fuel_pump", "y": 90},
    "facing=south": {"model": f"{MOD}:block/fuel_pump", "y": 180},
    "facing=west": {"model": f"{MOD}:block/fuel_pump", "y": 270}}})
wj(f"{A}/models/item/fuel_pump.json", {"parent": f"{MOD}:block/fuel_pump"})
wj(f"{A}/models/block/mud.json", {"textures": {"particle": "minecraft:block/mud"}})
wj(f"{A}/blockstates/mud.json", {"variants": {"": {"model": f"{MOD}:block/mud"}}})
for n in ["fuel_can", "mud_bucket", "track_kit"]:
    wj(f"{A}/models/item/{n}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"{MOD}:item/{n}"}})

# ---- lang
lp = f"{A}/lang/en_us.json"
lang = json.load(open(lp))
lang.update({
    f"block.{MOD}.fuel_pump": "Fuel Pump",
    f"block.{MOD}.mud": "Liquid Mud",
    f"fluid.{MOD}.mud": "Liquid Mud",
    f"item.{MOD}.fuel_can": "Fuel Can",
    f"item.{MOD}.mud_bucket": "Bucket of Liquid Mud",
    f"item.{MOD}.track_kit": "Track Kit",
})
wj(lp, lang)

# ---- recipes, loot, tags
R = f"{D}/{MOD}/recipe"
def item(i): return {"tag": i[1:]} if i.startswith("#") else {"item": i}
def shaped(name, pattern, key, result, count=1, cat="misc"):
    wj(f"{R}/{name}.json", {"type": "minecraft:crafting_shaped", "category": cat, "pattern": pattern,
                            "key": {k: item(v) for k, v in key.items()}, "result": {"id": result, "count": count}})
shaped("fuel_can", ["  I", "IRI", "III"], {"I": "minecraft:iron_ingot", "R": "minecraft:coal"}, f"{MOD}:fuel_can", 2)
shaped("fuel_pump", ["III", "IBI", "IRI"], {"I": "minecraft:iron_ingot", "B": "minecraft:bucket", "R": "minecraft:redstone_block"}, f"{MOD}:fuel_pump", 1, "building")
shaped("track_kit", ["TTT", "TST", "TKT"], {"T": f"{MOD}:track_dirt", "S": f"{MOD}:start_finish_line", "K": f"{MOD}:kicker_ramp"}, f"{MOD}:track_kit", 1)
wj(f"{R}/mud_bucket.json", {"type": "minecraft:crafting_shapeless", "category": "misc",
    "ingredients": [{"item": "minecraft:water_bucket"}, {"item": "minecraft:dirt"}, {"item": "minecraft:dirt"}, {"item": "minecraft:mud"}],
    "result": {"id": f"{MOD}:mud_bucket", "count": 1}})
wj(f"{D}/{MOD}/loot_table/blocks/fuel_pump.json", {"type": "minecraft:block", "pools": [{"rolls": 1,
    "entries": [{"type": "minecraft:item", "name": f"{MOD}:fuel_pump"}], "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
tp = f"{D}/minecraft/tags/block/mineable/pickaxe.json"
t = json.load(open(tp)); t["values"].append(f"{MOD}:fuel_pump"); wj(tp, t)

# ---- world generation: a track now and then in open biomes
wj(f"{D}/{MOD}/worldgen/configured_feature/motocross_track.json", {"type": f"{MOD}:motocross_track", "config": {}})
wj(f"{D}/{MOD}/worldgen/placed_feature/motocross_track.json", {
    "feature": f"{MOD}:motocross_track",
    "placement": [
        {"type": "minecraft:rarity_filter", "chance": 70},
        {"type": "minecraft:heightmap", "heightmap": "WORLD_SURFACE_WG"},
        {"type": "minecraft:biome"}]})
print("extra assets done")
