package com.donutalerts;

import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;

/** Edit the thresholds, colors, labels and sounds here. Amounts are in dollars. */
public enum Tier {
    //        min amount      label            color     confetti  seconds  flash  rainbow
    DONATION (10_000_000L,    "DONATION",      0x55FF55,  30,      4.0f,    false, false),
    BIG      (25_000_000L,    "BIG DONATION",  0x55FFFF,  60,      4.5f,    false, false),
    HUGE     (50_000_000L,    "HUGE DONATION", 0xAA55FF,  100,     5.0f,    true,  false),
    INSANE   (100_000_000L,   "INSANE DONO",   0xFFAA00,  160,     6.0f,    true,  false),
    MEGA     (500_000_000L,   "MEGA DONO",     0xFF5555,  240,     7.0f,    true,  true),
    LEGEND   (1_000_000_000L, "LEGENDARY",     0xFFFFFF,  360,     8.0f,    true,  true);

    public final long min;
    public final String label;
    public final int color;
    public final int confetti;
    public final float seconds;
    public final boolean flash;
    public final boolean rainbow;

    Tier(long min, String label, int color, int confetti, float seconds, boolean flash, boolean rainbow) {
        this.min = min; this.label = label; this.color = color; this.confetti = confetti;
        this.seconds = seconds; this.flash = flash; this.rainbow = rainbow;
    }

    public static Tier of(double amount) {
        Tier found = null;
        for (Tier t : values()) if (amount >= t.min) found = t;
        return found;
    }

    /** Sound layers: {event, pitch, volume}. Swap for custom sounds if you like. */
    public Object[][] sounds() {
        return switch (this) {
            case DONATION -> new Object[][]{{SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f}};
            case BIG -> new Object[][]{{SoundEvents.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f},
                                       {SoundEvents.ENTITY_FIREWORK_ROCKET_TWINKLE, 1.0f, 1.0f}};
            case HUGE -> new Object[][]{{SoundEvents.ENTITY_PLAYER_LEVELUP, 0.8f, 1.0f},
                                        {SoundEvents.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, 1.0f, 1.0f},
                                        {SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.2f, 1.0f}};
            case INSANE -> new Object[][]{{SoundEvents.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, 0.8f, 1.2f},
                                          {SoundEvents.ENTITY_PLAYER_LEVELUP, 0.6f, 1.0f},
                                          {SoundEvents.ENTITY_FIREWORK_ROCKET_TWINKLE, 1.0f, 1.0f}};
            case MEGA -> new Object[][]{{SoundEvents.ENTITY_WITHER_SPAWN, 1.3f, 0.6f},
                                        {SoundEvents.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, 0.7f, 1.3f},
                                        {SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.0f}};
            case LEGEND -> new Object[][]{{SoundEvents.ENTITY_ENDER_DRAGON_DEATH, 1.5f, 0.7f},
                                          {SoundEvents.ENTITY_WITHER_SPAWN, 1.0f, 0.8f},
                                          {SoundEvents.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, 0.6f, 1.5f},
                                          {SoundEvents.ENTITY_PLAYER_LEVELUP, 0.5f, 1.0f}};
        };
    }
}
