package com.dirtlegends.npc;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;

/** Names and lines for everyone at the track. All original. */
public final class TrackTalk {
    private TrackTalk() {
    }

    public static final String[] FAN_NAMES = {"Dusty", "Kayla", "Big Earl", "Marisol", "Trent", "Jolene", "Cody", "Brandi", "Hank",
            "Sierra", "Wade", "Lupe", "Tanner", "Darla", "Rhett", "Mia", "Boone", "Tia", "Colt", "Reba"};
    public static final String[] RACER_NAMES = {"\"Rocket\" Reyes", "Jett Cole", "Mud Dog McCall", "Ty \"Throttle\" Banks", "Ace Varga",
            "Dakota Fritz", "Kenny Blaze", "Rio Santana", "Hollis \"Holeshot\" Grant", "Wyatt Kane"};

    public static final String[] FAN_CHAT = {"Best seats in the house!", "I came for the jumps and the hot dogs.", "Did you see that whip?!",
            "My money's on the red bike.", "Somebody's gonna eat dirt on that tabletop.", "Grab a soda, it's hot out here.",
            "I used to race back in the day. Knees said no.", "Brap brap!", "This track's gnarlier than last season.",
            "Ever tried the nachos? Trust me."};
    public static final String[] FAN_CHEER_AIR = {"WOOOO!", "SEND IT!", "Look at that air!", "HUGE!", "Holy cow, he's flying!"};
    public static final String[] FAN_CHEER_LAP = {"Go go go!", "Faster!", "That's a quick lap!", "Yeah buddy!"};
    public static final String[] FAN_CRASH = {"OOOOH!", "Ouch! He's okay... I think.", "Yard sale!", "That's gonna leave a mark!"};

    public static final String[] RACER_CHAT = {"Think you can keep up? Line up behind the start.", "Tabletop's all about the throttle. Don't chop it.",
            "Hold the inside line through the berms.", "Wear full gear. Trust me on that one.", "Last lap I hit 60 on the back straight.",
            "Whoops? Stay on the gas and skim 'em.", "I'll see you at the finish. From the front."};
    public static final String[] VENDOR_FOOD = {"Hot dogs! Get your hot dogs!", "Nachos with extra cheese, best at the track.",
            "Need a cold soda? You look thirsty.", "Energy drinks for the riders, popcorn for the fans!"};
    public static final String[] VENDOR_PITS = {"Need fuel? Tires? I got you.", "Bring me emeralds, I'll get you back on the track.",
            "Your chain's dry. Want a new engine while we're at it?", "Gear up before you go out there."};

    public static String pick(String[] lines, RandomSource r) {
        return lines[r.nextInt(lines.length)];
    }

    public static void say(ServerPlayer to, String name, String line) {
        to.sendSystemMessage(Component.literal("<" + name + "> ").withStyle(ChatFormatting.GOLD)
                .append(Component.literal(line).withStyle(ChatFormatting.WHITE)));
    }
}
