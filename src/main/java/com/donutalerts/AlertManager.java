package com.donutalerts;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;

import java.awt.Color;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class AlertManager {
    private record Alert(String player, double amount, Tier tier) {}

    private static class Confetti { float x, y, vx, vy; int color; int size; }
    private static class Star { float angle, r, speed, phase; int size, color; }
    private static class Shooter { float x, y, vx, vy, life; }

    private static final ArrayDeque<Alert> QUEUE = new ArrayDeque<>();
    private static final List<Confetti> CONFETTI = new ArrayList<>();
    private static final List<Star> STARS = new ArrayList<>();
    private static final List<Shooter> SHOOTERS = new ArrayList<>();
    private static final Random RNG = new Random();

    // Index = tier level (0 = DONATION ... 5 = LEGEND)
    private static final float[] WARP = {0f, 0f, 0.3f, 1.0f, 1.6f, 2.4f};      // star speed streaks
    private static final int[] DIM = {0, 0, 60, 95, 120, 140};                // screen darkening
    private static final int[] PLANET = {0, 0, 0, 0xE8A040, 0xD05060, 0x4C8DFF};
    private static final int[] STAR_COLORS = {0xFFFFFF, 0xBBDDFF, 0xFFEEBB};

    private static Alert current;
    private static long startMs, lastFrameMs;
    private static float nextShooter;

    public static void push(String player, double amount) {
        Tier tier = Tier.of(amount);
        if (tier == null) return; // under 10M -> ignore
        QUEUE.add(new Alert(player, amount, tier));
    }

    private static void start(Alert a) {
        current = a;
        startMs = lastFrameMs = System.currentTimeMillis();
        MinecraftClient mc = MinecraftClient.getInstance();

        for (Object[] s : a.tier.sounds()) {
            mc.getSoundManager().play(PositionedSoundInstance.master((SoundEvent) s[0], (float) s[1], (float) s[2]));
        }

        int w = mc.getWindow().getScaledWidth();
        int h = mc.getWindow().getScaledHeight();

        CONFETTI.clear();
        for (int i = 0; i < a.tier.confetti; i++) {
            Confetti c = new Confetti();
            c.x = w / 2f + (RNG.nextFloat() - 0.5f) * 220f;
            c.y = 40;
            c.vx = (RNG.nextFloat() - 0.5f) * 260f;
            c.vy = -RNG.nextFloat() * 220f - 40f;
            c.size = 2 + RNG.nextInt(3);
            c.color = Color.HSBtoRGB(RNG.nextFloat(), 0.8f, 1f);
            CONFETTI.add(c);
        }

        int level = a.tier.ordinal();
        STARS.clear();
        SHOOTERS.clear();
        nextShooter = 0.3f;
        if (level > 0) {
            float maxR = (float) Math.hypot(w / 2f, h / 2f);
            int count = 70 + level * 35;
            for (int i = 0; i < count; i++) {
                Star s = new Star();
                s.angle = RNG.nextFloat() * (float) (Math.PI * 2);
                s.r = RNG.nextFloat() * maxR;
                s.speed = 0.5f + RNG.nextFloat();
                s.phase = RNG.nextFloat() * 6.28f;
                s.size = RNG.nextInt(4) == 0 ? 2 : 1;
                s.color = STAR_COLORS[RNG.nextInt(STAR_COLORS.length)];
                STARS.add(s);
            }
        }
    }

    // ---------- helpers ----------
    private static int col(int a, int rgb) {
        a = Math.max(0, Math.min(255, a));
        return (a << 24) | (rgb & 0xFFFFFF);
    }

    private static void rect(DrawContext ctx, float x, float y, float w, float h, int color) {
        int x1 = Math.round(x), y1 = Math.round(y);
        int x2 = Math.max(x1 + 1, Math.round(x + w)), y2 = Math.max(y1 + 1, Math.round(y + h));
        ctx.fill(x1, y1, x2, y2, color);
    }

    private static int scale(int rgb, float f) {
        int r = (int) (((rgb >> 16) & 0xFF) * f), g = (int) (((rgb >> 8) & 0xFF) * f), b = (int) ((rgb & 0xFF) * f);
        return (r << 16) | (g << 8) | b;
    }

    private static float easeOutBack(float t) {
        float c1 = 1.70158f, c3 = c1 + 1;
        return 1 + c3 * (float) Math.pow(t - 1, 3) + c1 * (float) Math.pow(t - 1, 2);
    }

    private static void disc(DrawContext ctx, float cx, float cy, float R, int rgb, int a, int step) {
        int r = (int) R;
        for (int y = -r; y <= r; y += step) {
            int w = (int) Math.sqrt(Math.max(0, R * R - (float) y * y));
            ctx.fill(Math.round(cx - w), Math.round(cy + y), Math.round(cx + w), Math.round(cy + y + step), col(a, rgb));
        }
    }

    /** A shaded planet: dark base with a lit crescent from the top-left. */
    private static void sphere(DrawContext ctx, float cx, float cy, float R, int rgb, float alpha) {
        int a = (int) (255 * alpha);
        int dark = scale(rgb, 0.28f);
        int r = (int) R;
        for (int y = -r; y <= r; y++) {
            float w = (float) Math.sqrt(Math.max(0, R * R - (float) y * y));
            ctx.fill(Math.round(cx - w), Math.round(cy + y), Math.round(cx + w), Math.round(cy + y + 1), col(a, dark));
            float ys = y + 0.25f * R;
            if (Math.abs(ys) <= R) {
                float wl = (float) Math.sqrt(R * R - ys * ys);
                float l = Math.max(-w, -0.35f * R - wl);
                float rr = Math.min(w, -0.35f * R + wl);
                if (rr > l) ctx.fill(Math.round(cx + l), Math.round(cy + y), Math.round(cx + rr), Math.round(cy + y + 1), col(a, rgb));
            }
        }
    }

    private static void ringDots(DrawContext ctx, float cx, float cy, float rx, float ry, int rgb, int a, boolean front) {
        int n = 140;
        for (int i = 0; i < n; i++) {
            double t = Math.PI * 2 * i / n;
            double s = Math.sin(t);
            if (front != (s >= 0)) continue;
            rect(ctx, cx + (float) Math.cos(t) * rx, cy + (float) s * ry, 2, 2, col(a, rgb));
        }
    }

    private static void circleOutline(DrawContext ctx, float cx, float cy, float r, int rgb, int a, int thick) {
        int n = Math.min(420, (int) (r * 0.7f) + 24);
        for (int i = 0; i < n; i++) {
            double t = Math.PI * 2 * i / n;
            rect(ctx, cx + (float) Math.cos(t) * r, cy + (float) Math.sin(t) * r, thick, thick, col(a, rgb));
        }
    }

    private static void nebula(DrawContext ctx, float x, float y, float R, int rgb, float alpha) {
        for (int k = 0; k < 6; k++) disc(ctx, x, y, R * (1f - k / 6f), rgb, (int) (10 * alpha), 2);
    }

    private static void accretion(DrawContext ctx, float cx, float cy, float elapsed, float alpha, boolean front) {
        int n = 160;
        for (int i = 0; i < n; i++) {
            double t = Math.PI * 2 * i / n;
            double s = Math.sin(t);
            if (front != (s >= 0)) continue;
            float hue = (float) (((t / (Math.PI * 2)) + elapsed * 0.5) % 1.0);
            rect(ctx, cx + (float) Math.cos(t) * 46, cy + (float) s * 11, 2, 2, col((int) (255 * alpha), Color.HSBtoRGB(hue, 0.7f, 1f)));
        }
    }

    // ---------- space scene ----------
    private static void renderSpace(DrawContext ctx, Tier tier, float elapsed, float dt, float sa, int sw, int sh) {
        int level = tier.ordinal();
        if (level == 0) return;
        float cx = sw / 2f, cy = sh / 2f;
        float maxR = (float) Math.hypot(cx, cy);

        // darken the world a bit
        int dim = (int) (DIM[level] * sa);
        if (dim > 0) ctx.fill(0, 0, sw, sh, col(dim, 0x050818));

        // nebula clouds
        if (level >= 4) {
            nebula(ctx, cx * 0.55f + (float) Math.sin(elapsed * 0.4) * 20f, cy * 1.25f, 140, 0x8833FF, sa);
            nebula(ctx, cx * 1.5f, cy * 0.75f + (float) Math.cos(elapsed * 0.3) * 15f, 120, 0xFF3388, sa);
        }

        // planet (back half of rings first)
        float px = sw * 0.84f - elapsed * 6f, py = sh * 0.62f + (float) Math.sin(elapsed * 0.5) * 4f;
        float pr = 22 + level * 5;
        if (level >= 3) {
            if (level >= 4) {
                ringDots(ctx, px, py, pr * 1.7f, pr * 0.4f, 0xD8C8A0, (int) (200 * sa), false);
                ringDots(ctx, px, py, pr * 2.1f, pr * 0.5f, 0xB8A888, (int) (150 * sa), false);
            }
            sphere(ctx, px, py, pr, PLANET[level], sa);
            if (level >= 4) {
                ringDots(ctx, px, py, pr * 1.7f, pr * 0.4f, 0xD8C8A0, (int) (230 * sa), true);
                ringDots(ctx, px, py, pr * 2.1f, pr * 0.5f, 0xB8A888, (int) (170 * sa), true);
            }
        }

        // star field: drifts at low tiers, becomes hyperspace warp at high tiers
        float warp = WARP[level];
        for (Star s : STARS) {
            float v = (15f + s.r * 0.5f) * s.speed * (1f + warp * 3.5f);
            s.r += v * dt;
            if (s.r > maxR) {
                s.r = RNG.nextFloat() * 10f;
                s.angle = RNG.nextFloat() * (float) (Math.PI * 2);
            }
            float cs = (float) Math.cos(s.angle), sn = (float) Math.sin(s.angle);
            float tw = 0.6f + 0.4f * (float) Math.sin(elapsed * 3f + s.phase);
            int a = (int) (255 * sa * tw * Math.min(1f, s.r / 40f));
            int steps = (int) (warp * s.r * 0.025f);
            for (int i = 0; i <= steps; i++) {
                float rr = s.r - i * 4f;
                if (rr < 0) break;
                int fa = (int) (a * (1f - i / (float) (steps + 1)));
                rect(ctx, cx + cs * rr, cy + sn * rr, s.size, s.size, col(fa, s.color));
            }
        }

        // shooting stars
        if (level >= 2) {
            nextShooter -= dt;
            if (nextShooter <= 0f) {
                Shooter sh1 = new Shooter();
                sh1.x = RNG.nextFloat() * sw * 1.2f;
                sh1.y = RNG.nextFloat() * sh * 0.35f;
                sh1.vx = -(250f + RNG.nextFloat() * 250f);
                sh1.vy = 120f + RNG.nextFloat() * 140f;
                sh1.life = 1.2f;
                SHOOTERS.add(sh1);
                nextShooter = 0.9f / (level - 1);
            }
            Iterator<Shooter> it = SHOOTERS.iterator();
            while (it.hasNext()) {
                Shooter s = it.next();
                s.x += s.vx * dt;
                s.y += s.vy * dt;
                s.life -= dt;
                if (s.life <= 0f) { it.remove(); continue; }
                for (int i = 0; i < 14; i++) {
                    float back = i * 0.012f;
                    int fa = (int) (255 * sa * Math.min(1f, s.life) * (1f - i / 14f));
                    rect(ctx, s.x - s.vx * back, s.y - s.vy * back, 2, 2, col(fa, i < 2 ? 0xFFFFFF : 0x9FD0FF));
                }
            }
        }

        // shockwave rings from the center
        if (level >= 3) {
            float period = level == 5 ? 1.5f : (level == 4 ? 2.5f : 999f);
            for (int k = 0; k < 6; k++) {
                float age = elapsed - k * period;
                if (age < 0) break;
                if (age < 1.6f) {
                    circleOutline(ctx, cx, cy, age * 520f, tier.rainbow ? Color.HSBtoRGB((elapsed * 0.7f) % 1f, 0.6f, 1f) : tier.color,
                        (int) (200 * sa * (1f - age / 1.6f)), 3);
                }
            }
        }

        // black hole for the top tier
        if (level == 5) {
            accretion(ctx, cx, cy, elapsed, sa, false);
            disc(ctx, cx, cy, 14, 0x000000, (int) (255 * sa), 1);
            circleOutline(ctx, cx, cy, 15, 0xFFAA33, (int) (230 * sa), 2);
            accretion(ctx, cx, cy, elapsed, sa, true);
        }
    }

    // ---------- main render ----------
    public static void render(DrawContext ctx) {
        long now = System.currentTimeMillis();
        if (current == null) {
            Alert next = QUEUE.poll();
            if (next == null) return;
            start(next);
            now = System.currentTimeMillis();
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        Tier tier = current.tier;
        int level = tier.ordinal();
        float elapsed = (now - startMs) / 1000f;
        float dt = Math.min((now - lastFrameMs) / 1000f, 0.05f);
        lastFrameMs = now;

        if (elapsed >= tier.seconds) { current = null; CONFETTI.clear(); STARS.clear(); SHOOTERS.clear(); return; }

        int sw = ctx.getScaledWindowWidth();
        int sh = ctx.getScaledWindowHeight();
        int cx = sw / 2;

        int color = tier.rainbow
            ? (Color.HSBtoRGB((now % 2000) / 2000f, 0.7f, 1f) & 0xFFFFFF)
            : tier.color;

        // Phases: slide in (0.6s) -> hold -> fade out (0.6s)
        float in = Math.min(elapsed / 0.6f, 1f);
        float out = Math.min(Math.max((tier.seconds - elapsed) / 0.6f, 0f), 1f);
        float alpha = Math.min(1f, out);
        int a8 = (int) (alpha * 255);

        // Space scene behind everything
        renderSpace(ctx, tier, elapsed, dt, in * out, sw, sh);

        // Screen flash on high tiers
        if (tier.flash && elapsed < 0.6f) {
            int fa = (int) ((1f - elapsed / 0.6f) * 90);
            ctx.fill(0, 0, sw, sh, col(fa, color));
        }

        // Banner
        int bw = 260, bh = 46;
        float slide = easeOutBack(in);
        int by = (int) (-bh + slide * (bh + 12));
        int x1 = cx - bw / 2, x2 = cx + bw / 2;

        ctx.fill(x1, by, x2, by + bh, col((int) (alpha * 210), 0x0A0C18));
        float glow = level >= 2 ? 0.75f + 0.25f * (float) Math.sin(elapsed * 6f) : 1f;
        int border = col((int) (a8 * glow), color);
        ctx.fill(x1, by, x2, by + 2, border);
        ctx.fill(x1, by + bh - 2, x2, by + bh, border);
        ctx.fill(x1, by, x1 + 2, by + bh, border);
        ctx.fill(x2 - 2, by, x2, by + bh, border);

        // Pulsing tier label, bigger for bigger tiers
        float pulse = 1f + 0.08f * (float) Math.sin(elapsed * 8f);
        float labelScale = (1.4f + 0.1f * level) * pulse;
        var m = ctx.getMatrices();
        m.push();
        m.translate(cx, by + 8, 0);
        m.scale(labelScale, labelScale, 1f);
        ctx.drawCenteredTextWithShadow(mc.textRenderer, Text.literal(tier.label), 0, 0, col(a8, color));
        m.pop();

        // Amount counts up from 0
        float t = Math.min(elapsed / 1.4f, 1f);
        double shown = current.amount * (1 - Math.pow(1 - t, 3));
        String line = current.player + " paid you $" + AmountParser.format(shown);
        ctx.drawCenteredTextWithShadow(mc.textRenderer, Text.literal(line), cx, by + 30, col(a8, 0xFFFFFF));

        // Confetti
        for (Confetti c : CONFETTI) {
            c.vy += 380f * dt;
            c.x += c.vx * dt;
            c.y += c.vy * dt;
            if (c.y > sh) continue;
            ctx.fill((int) c.x, (int) c.y, (int) c.x + c.size, (int) c.y + c.size, col(a8, c.color));
        }
    }
}
