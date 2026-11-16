package com.donutalerts;

import net.minecraft.sound.SoundEvents;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

/**
 * Edit thresholds, colors, labels, effects and sounds here. Amounts are in dollars.
 * Keep the list in ascending order of amount.
 */
public enum Tier {
    //        min             label             color     conf  secs  flash  rainbow stars warp  dim   effects
    DONATION (10_000_000L,    "DONATION",       0x55FF55,  30,  4.0f, false, false,   0,   0.0f,   0),
    NICE     (20_000_000L,    "NICE DONATION",  0x99FF55,  45,  4.0f, false, false,  60,   0.0f,   0),
    BIG      (35_000_000L,    "BIG DONATION",   0x55FFFF,  60,  4.5f, false, false,  90,   0.1f,  30, Fx.SHOOTING),
    HUGE     (50_000_000L,    "HUGE DONATION",  0xAA55FF,  90,  5.0f, true,  false, 110,   0.3f,  60, Fx.SHOOTING, Fx.MOON),
    MASSIVE  (75_000_000L,    "MASSIVE DONO",   0xFF55FF, 120,  5.0f, true,  false, 130,   0.5f,  70, Fx.SHOOTING, Fx.MOON, Fx.ROCKET),
    INSANE   (100_000_000L,   "INSANE DONO",    0xFFAA00, 160,  5.5f, true,  false, 150,   1.0f,  90, Fx.SHOOTING, Fx.PLANET, Fx.ROCKET, Fx.SHOCK),
    ABSURD   (250_000_000L,   "ABSURD DONO",    0xFF7733, 200,  6.0f, true,  false, 170,   1.3f, 105, Fx.PLANET, Fx.RINGS, Fx.UFO, Fx.METEORS, Fx.SHOCK),
    MEGA     (500_000_000L,   "MEGA DONO",      0xFF5555, 240,  7.0f, true,  true,  190,   1.6f, 120, Fx.PLANET, Fx.RINGS, Fx.NEBULA, Fx.AURORA, Fx.UFO, Fx.SHOCK_LOOP),
    COSMIC   (750_000_000L,   "COSMIC DONO",    0x66AAFF, 300,  7.5f, true,  true,  210,   2.0f, 130, Fx.NEBULA, Fx.AURORA, Fx.GALAXY, Fx.PLANET, Fx.RINGS, Fx.METEORS, Fx.SUPERNOVA, Fx.SHOCK_LOOP),
    LEGEND   (1_000_000_000L, "LEGENDARY",      0xFFFFFF, 360,  8.0f, true,  true,  230,   2.4f, 140, Fx.NEBULA, Fx.AURORA, Fx.GALAXY, Fx.BLACKHOLE, Fx.PLANET, Fx.ROCKET, Fx.UFO, Fx.METEORS, Fx.SUPERNOVA, Fx.SHOCK_LOOP),
    GOD      (5_000_000_000L, "GOD DONO",       0xFFD700, 500, 10.0f, true,  true,  260,   3.0f, 150, Fx.NEBULA, Fx.AURORA, Fx.GALAXY, Fx.BLACKHOLE, Fx.PLANET, Fx.RINGS, Fx.MOON, Fx.ROCKET, Fx.UFO, Fx.METEORS, Fx.SHOOTING, Fx.SUPERNOVA, Fx.SHOCK_LOOP, Fx.RAINBOW_TINT);

    public enum Fx {
        SHOOTING, MOON, ROCKET, UFO, PLANET, RINGS, NEBULA, AURORA, METEORS,
        GALAXY, SHOCK, SHOCK_LOOP, SUPERNOVA, BLACKHOLE, RAINBOW_TINT
    }

    public final long min;
    public final String label;
    public final int color;
    public final int confetti;
    public final float seconds;
    public final boolean flash;
    public final boolean rainbow;
    public final int stars;
    public final float warp;
    public final int dim;
    public final Set<Fx> fx;

    Tier(long min, String label, int color, int confetti, float seconds, boolean flash, boolean rainbow,
         int stars, float warp, int dim, Fx... fx) {
        this.min = min; this.label = label; this.color = color; this.confetti = confetti;
        this.seconds = seconds; this.flash = flash; this.rainbow = rainbow;
        this.stars = stars; this.warp = warp; this.dim = dim;
        this.fx = fx.length == 0 ? EnumSet.noneOf(Fx.class) : EnumSet.copyOf(Arrays.asList(fx));
    }

    public boolean has(Fx f) { return fx.contains(f); }

    public static Tier of(double amount) {
        Tier found = null;
        for (Tier t : values()) if (amount >= t.min) found = t;
        return found;
    }

    private static Object[] snd(Object event, float pitch, float volume, float delaySeconds) {
        return new Object[]{event, pitch, volume, delaySeconds};
    }

    /** Sound layers: {event, pitch, volume, delay in seconds}. Swap for custom sounds if you like. */
    public Object[][] sounds() {
        return switch (this) {
            case DONATION -> new Object[][]{
                snd(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f, 0f)};
            case NICE -> new Object[][]{
                snd(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.2f, 1.0f, 0f),
                snd(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f, 0.15f)};
            case BIG -> new Object[][]{
                snd(SoundEvents.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f, 0f),
                snd(SoundEvents.ENTITY_FIREWORK_ROCKET_TWINKLE, 1.0f, 1.0f, 0.2f)};
            case HUGE -> new Object[][]{
                snd(SoundEvents.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.0f, 1.0f, 0f),
                snd(SoundEvents.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, 1.0f, 1.0f, 0.4f),
                snd(SoundEvents.ENTITY_PLAYER_LEVELUP, 0.8f, 1.0f, 0.5f)};
            case MASSIVE -> new Object[][]{
                snd(SoundEvents.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.0f, 1.0f, 0f),
                snd(SoundEvents.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.3f, 1.0f, 0.2f),
                snd(SoundEvents.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, 0.9f, 1.2f, 0.55f),
                snd(SoundEvents.ENTITY_PLAYER_LEVELUP, 0.7f, 1.0f, 0.55f)};
            case INSANE -> new Object[][]{
                snd(SoundEvents.BLOCK_BEACON_ACTIVATE, 1.0f, 1.0f, 0f),
                snd(SoundEvents.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, 0.8f, 1.2f, 0.3f),
                snd(SoundEvents.ENTITY_PLAYER_LEVELUP, 0.6f, 1.0f, 0.3f),
                snd(SoundEvents.ENTITY_FIREWORK_ROCKET_TWINKLE, 1.0f, 1.0f, 0.7f)};
            case ABSURD -> new Object[][]{
                snd(SoundEvents.BLOCK_BEACON_ACTIVATE, 0.8f, 1.0f, 0f),
                snd(SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, 1.2f, 0.6f, 0.2f),
                snd(SoundEvents.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, 0.7f, 1.3f, 0.5f),
                snd(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.0f, 0.5f)};
            case MEGA -> new Object[][]{
                snd(SoundEvents.ENTITY_WITHER_SPAWN, 1.3f, 0.6f, 0f),
                snd(SoundEvents.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, 0.7f, 1.3f, 0.4f),
                snd(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.0f, 0.8f),
                snd(SoundEvents.ENTITY_PLAYER_LEVELUP, 0.5f, 1.0f, 0.8f)};
            case COSMIC -> new Object[][]{
                snd(SoundEvents.BLOCK_END_PORTAL_SPAWN, 1.0f, 0.6f, 0f),
                snd(SoundEvents.BLOCK_BEACON_ACTIVATE, 0.6f, 1.0f, 0.3f),
                snd(SoundEvents.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, 0.6f, 1.4f, 0.1f),
                snd(SoundEvents.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, 0.5f, 1.4f, 2.0f),
                snd(SoundEvents.ENTITY_PLAYER_LEVELUP, 0.5f, 1.0f, 1.0f)};
            case LEGEND -> new Object[][]{
                snd(SoundEvents.ENTITY_ENDER_DRAGON_DEATH, 1.5f, 0.7f, 0f),
                snd(SoundEvents.ENTITY_WITHER_SPAWN, 1.0f, 0.8f, 0.3f),
                snd(SoundEvents.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, 0.6f, 1.5f, 0.1f),
                snd(SoundEvents.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, 0.5f, 1.5f, 2.0f),
                snd(SoundEvents.ENTITY_PLAYER_LEVELUP, 0.5f, 1.0f, 1.0f)};
            case GOD -> new Object[][]{
                snd(SoundEvents.ENTITY_ENDER_DRAGON_GROWL, 0.8f, 0.8f, 0f),
                snd(SoundEvents.ENTITY_ENDER_DRAGON_DEATH, 1.2f, 0.7f, 0.1f),
                snd(SoundEvents.BLOCK_END_PORTAL_SPAWN, 0.7f, 0.7f, 0.5f),
                snd(SoundEvents.ENTITY_WITHER_SPAWN, 0.8f, 0.8f, 1.0f),
                snd(SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, 0.8f, 0.8f, 1.5f),
                snd(SoundEvents.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, 0.5f, 1.5f, 2.0f),
                snd(SoundEvents.ENTITY_PLAYER_LEVELUP, 0.4f, 1.0f, 2.0f)};
        };
    }
}
