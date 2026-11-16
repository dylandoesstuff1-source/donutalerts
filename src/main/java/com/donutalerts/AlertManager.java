package com.donutalerts;

import com.donutalerts.Tier.Fx;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.registry.entry.RegistryEntry;
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
    private static class Shooter { float x, y, vx, vy, life; int kind; }   // kind 0 = shooting star, 1 = meteor
    private static class Spark { float x, y, vx, vy, life, max; int color; }

    private static final ArrayDeque<Alert> QUEUE = new ArrayDeque<>();
    private static final List<Confetti> CONFETTI = new ArrayList<>();
    private static final List<Star> STARS = new ArrayList<>();
    private static final List<Shooter> SHOOTERS = new ArrayList<>();
    private static final List<Spark> SPARKS = new ArrayList<>();
    private static final List<Object[]> PENDING = new ArrayList<>();
    private static final Random RNG = new Random();

    private static final int[] STAR_COLORS = {0xFFFFFF, 0xBBDDFF, 0xFFEEBB};
    private static final int[] SPARK_COLORS = {0xFFFFFF, 0xFFE070, 0xFF8040, 0xFF4040, 0x80C0FF};

    private static Alert current;
    private static long startMs, lastFrameMs;
    private static float nextShooter, nextMeteor;
    private static boolean secondBurst;

    public static void push(String player, double amount) {
        Tier tier = Tier.of(amount);
        if (tier == null) return; // under 10M -> ignore
        QUEUE.add(new Alert(player, amount, tier));
    }

    @SuppressWarnings("unchecked")
    private static void play(Object[] s) {
        Object o = s[0];
        SoundEvent ev = o instanceof SoundEvent se ? se : ((RegistryEntry<SoundEvent>) o).value();
        MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(ev, (float) s[1], (float) s[2]));
    }

    private static void start(Alert a) {
        current = a;
        startMs = lastFrameMs = System.currentTimeMillis();
        MinecraftClient mc = MinecraftClient.getInstance();

        PENDING.clear();
        for (Object[] s : a.tier.sounds()) PENDING.add(s);

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

        STARS.clear();
        SHOOTERS.clear();
        SPARKS.clear();
        nextShooter = 0.3f;
        nextMeteor = 0.5f;
        secondBurst = false;
        float maxR = (float) Math.hypot(w / 2f, h / 2f);
        for (int i = 0; i < a.tier.stars; i++) {
            Star s = new Star();
            s.angle = RNG.nextFloat() * (float) (Math.PI * 2);
            s.r = RNG.nextFloat() * maxR;
            s.speed = 0.5f + RNG.nextFloat();
            s.phase = RNG.nextFloat() * 6.28f;
            s.size = RNG.nextInt(4) == 0 ? 2 : 1;
            s.color = STAR_COLORS[RNG.nextInt(STAR_COLORS.length)];
            STARS.add(s);
        }
        if (a.tier.has(Fx.SUPERNOVA)) spawnBurst(w / 2f, h / 2f, 160);
    }

    private static void spawnBurst(float cx, float cy, int n) {
        for (int i = 0; i < n; i++) {
            Spark s = new Spark();
            double ang = RNG.nextDouble() * Math.PI * 2;
            float speed = 80f + RNG.nextFloat() * 480f;
            s.x = cx; s.y = cy;
            s.vx = (float) Math.cos(ang) * speed;
            s.vy = (float) Math.sin(ang) * speed;
            s.max = s.life = 1.6f + RNG.nextFloat() * 1.2f;
            s.color = SPARK_COLORS[RNG.nextInt(SPARK_COLORS.length)];
            SPARKS.add(s);
        }
    }

    // ---------- drawing helpers ----------
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

    private static void ellipse(DrawContext ctx, float cx, float cy, float rx, float ry, int rgb, int a) {
        int r = (int) ry;
        for (int y = -r; y <= r; y++) {
            float k = 1f - (y * y) / (ry * ry);
            int w = (int) (rx * Math.sqrt(Math.max(0f, k)));
            ctx.fill(Math.round(cx - w), Math.round(cy + y), Math.round(cx + w), Math.round(cy + y + 1), col(a, rgb));
        }
    }

    /** Shaded sphere: dark base with a lit area from the top-left. */
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

    private static void aurora(DrawContext ctx, int sw, int sh, float elapsed, float alpha) {
        for (int x = 0; x < sw; x += 3) {
            float base = sh * 0.2f + (float) Math.sin(x * 0.02f + elapsed * 1.5f) * 18f + (float) Math.sin(x * 0.055f - elapsed * 1.1f) * 7f;
            float hue = 0.33f + 0.28f * (0.5f + 0.5f * (float) Math.sin(x * 0.01f + elapsed * 0.8f));
            int rgb = Color.HSBtoRGB(hue, 0.8f, 1f);
            for (int k = 0; k < 6; k++) {
                int y1 = (int) (base + k * 9);
                ctx.fill(x, y1, x + 3, y1 + 9, col((int) ((46 - k * 7) * alpha), rgb));
            }
        }
    }

    private static void galaxy(DrawContext ctx, float cx, float cy, float elapsed, float alpha) {
        int arms = 3, per = 110;
        for (int k = 0; k < arms; k++) {
            for (int i = 0; i < per; i++) {
                float t = i / (float) per;
                float r = 6 + t * 130;
                float th = (float) (k * Math.PI * 2 / arms + t * 5.0 + elapsed * 0.6);
                float jitter = (float) Math.sin(i * 12.9898f + k * 78.233f) * 7f * (0.3f + t);
                float x = cx + (float) Math.cos(th) * (r + jitter);
                float y = cy + (float) Math.sin(th) * (r + jitter) * 0.42f;
                int c = t < 0.25f ? 0xFFE9A0 : (t < 0.6f ? 0xA0C8FF : 0xB07CFF);
                rect(ctx, x, y, 2, 2, col((int) (220 * alpha * (1f - t * 0.6f)), c));
            }
        }
        disc(ctx, cx, cy, 16, 0xFFF2C0, (int) (40 * alpha), 2);
        disc(ctx, cx, cy, 9, 0xFFF8E0, (int) (80 * alpha), 2);
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

    private static void moon(DrawContext ctx, float x, float y, float alpha) {
        sphere(ctx, x, y, 18, 0xC8C8D0, alpha);
        int a = (int) (110 * alpha);
        disc(ctx, x - 5, y - 3, 3, 0x707080, a, 1);
        disc(ctx, x + 6, y + 5, 4, 0x707080, a, 1);
        disc(ctx, x + 3, y - 8, 2, 0x707080, a, 1);
        disc(ctx, x - 7, y + 8, 2, 0x707080, a, 1);
    }

    private static void rocket(DrawContext ctx, float x, float y, float elapsed, float alpha) {
        int a = (int) (255 * alpha);
        for (int j = 1; j <= 12; j++) {
            rect(ctx, x - 22 - j * 6, y + (float) Math.sin(j + elapsed * 10) * 2, 3, 3, col((int) (a * 0.4f * (1f - j / 13f)), 0xCCCCCC));
        }
        for (int i = 0; i < 8; i++) {
            float jit = (float) Math.sin(elapsed * 40 + i) * 1.5f;
            rect(ctx, x - 9 - i * 3, y - 2 + jit, 3, 4 - i / 3, col((int) (a * (1f - i / 9f)), i < 3 ? 0xFFE060 : 0xFF8030));
        }
        rect(ctx, x - 8, y - 4, 20, 8, col(a, 0xEEEEEE));
        rect(ctx, x + 12, y - 3, 3, 6, col(a, 0xE04040));
        rect(ctx, x + 15, y - 2, 2, 4, col(a, 0xE04040));
        rect(ctx, x + 17, y - 1, 1, 2, col(a, 0xE04040));
        rect(ctx, x - 8, y - 8, 6, 4, col(a, 0xE04040));
        rect(ctx, x - 8, y + 4, 6, 4, col(a, 0xE04040));
        rect(ctx, x + 2, y - 2, 4, 4, col(a, 0x66DDFF));
    }

    private static void ufo(DrawContext ctx, float x, float y, float elapsed, float alpha) {
        int a = (int) (255 * alpha);
        for (int i = 0; i < 64; i++) {
            float half = 3 + i * 0.5f;
            ctx.fill(Math.round(x - half), Math.round(y + 6 + i), Math.round(x + half), Math.round(y + 7 + i), col((int) (28 * alpha), 0xFFFF99));
        }
        ellipse(ctx, x, y, 20, 5, col(a, 0xB0B0C0), col(a, 0xB0B0C0) >>> 24);
        for (int yy = -9; yy <= 0; yy++) {
            int w = (int) Math.sqrt(81 - yy * yy);
            ctx.fill(Math.round(x - w), Math.round(y - 3 + yy), Math.round(x + w), Math.round(y - 2 + yy), col((int) (200 * alpha), 0x66FFFF));
        }
        for (int i = -2; i <= 2; i++) {
            float hue = (elapsed * 1.5f + i * 0.2f) % 1f;
            rect(ctx, x + i * 7 - 1, y + 1, 3, 2, col(a, Color.HSBtoRGB(hue, 0.8f, 1f)));
        }
    }

    private static void coreFlash(DrawContext ctx, float cx, float cy, float since) {
        if (since < 0f || since > 0.7f) return;
        float t = since / 0.7f;
        disc(ctx, cx, cy, since * 650f, 0xFFFFFF, (int) (200 * (1f - t)), 2);
    }

    // ---------- space scene ----------
    private static void renderSpace(DrawContext ctx, Tier tier, float elapsed, float dt, float sa, int sw, int sh) {
        if (tier.stars == 0 && tier.dim == 0 && tier.fx.isEmpty()) return;
        float cx = sw / 2f, cy = sh / 2f;
        float maxR = (float) Math.hypot(cx, cy);

        int dim = (int) (tier.dim * sa);
        if (dim > 0) ctx.fill(0, 0, sw, sh, col(dim, 0x050818));
        if (tier.has(Fx.RAINBOW_TINT)) ctx.fill(0, 0, sw, sh, col((int) (35 * sa), Color.HSBtoRGB((elapsed * 0.4f) % 1f, 0.8f, 1f)));

        if (tier.has(Fx.NEBULA)) {
            nebula(ctx, cx * 0.55f + (float) Math.sin(elapsed * 0.4) * 20f, cy * 1.25f, 140, 0x8833FF, sa);
            nebula(ctx, cx * 1.5f, cy * 0.75f + (float) Math.cos(elapsed * 0.3) * 15f, 120, 0xFF3388, sa);
        }
        if (tier.has(Fx.AURORA)) aurora(ctx, sw, sh, elapsed, sa);

        // star field: drifts at low tiers, becomes hyperspace warp at high tiers
        float warp = tier.warp;
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
            int steps = Math.min(24, (int) (warp * s.r * 0.025f));
            for (int i = 0; i <= steps; i++) {
                float rr = s.r - i * 4f;
                if (rr < 0) break;
                rect(ctx, cx + cs * rr, cy + sn * rr, s.size, s.size, col((int) (a * (1f - i / (float) (steps + 1))), s.color));
            }
        }

        if (tier.has(Fx.GALAXY)) galaxy(ctx, cx, cy + 10f, elapsed, sa);

        if (tier.has(Fx.MOON)) moon(ctx, sw * 0.1f + elapsed * 4f, sh * 0.72f, sa);

        if (tier.has(Fx.PLANET)) {
            float px = sw * 0.84f - elapsed * 6f, py = sh * 0.62f + (float) Math.sin(elapsed * 0.5) * 4f;
            float pr = 22 + tier.ordinal() * 3;
            int pc = tier.rainbow ? 0x4C8DFF : tier.color;
            boolean rings = tier.has(Fx.RINGS);
            if (rings) {
                ringDots(ctx, px, py, pr * 1.7f, pr * 0.4f, 0xD8C8A0, (int) (200 * sa), false);
                ringDots(ctx, px, py, pr * 2.1f, pr * 0.5f, 0xB8A888, (int) (150 * sa), false);
            }
            sphere(ctx, px, py, pr, pc, sa);
            if (rings) {
                ringDots(ctx, px, py, pr * 1.7f, pr * 0.4f, 0xD8C8A0, (int) (230 * sa), true);
                ringDots(ctx, px, py, pr * 2.1f, pr * 0.5f, 0xB8A888, (int) (170 * sa), true);
            }
        }

        // shooting stars + meteors
        if (tier.has(Fx.SHOOTING)) {
            nextShooter -= dt;
            if (nextShooter <= 0f) {
                Shooter s = new Shooter();
                s.x = RNG.nextFloat() * sw * 1.2f;
                s.y = RNG.nextFloat() * sh * 0.35f;
                s.vx = -(250f + RNG.nextFloat() * 250f);
                s.vy = 120f + RNG.nextFloat() * 140f;
                s.life = 1.2f;
                s.kind = 0;
                SHOOTERS.add(s);
                nextShooter = 0.7f;
            }
        }
        if (tier.has(Fx.METEORS)) {
            nextMeteor -= dt;
            if (nextMeteor <= 0f) {
                Shooter s = new Shooter();
                s.x = RNG.nextFloat() * sw * 1.3f;
                s.y = -20f;
                s.vx = -(100f + RNG.nextFloat() * 160f);
                s.vy = 320f + RNG.nextFloat() * 180f;
                s.life = 2.0f;
                s.kind = 1;
                SHOOTERS.add(s);
                nextMeteor = 0.3f;
            }
        }
        Iterator<Shooter> it = SHOOTERS.iterator();
        while (it.hasNext()) {
            Shooter s = it.next();
            s.x += s.vx * dt;
            s.y += s.vy * dt;
            s.life -= dt;
            if (s.life <= 0f) { it.remove(); continue; }
            if (s.kind == 0) {
                for (int i = 0; i < 14; i++) {
                    float back = i * 0.012f;
                    int fa = (int) (255 * sa * Math.min(1f, s.life) * (1f - i / 14f));
                    rect(ctx, s.x - s.vx * back, s.y - s.vy * back, 2, 2, col(fa, i < 2 ? 0xFFFFFF : 0x9FD0FF));
                }
            } else {
                for (int i = 0; i < 18; i++) {
                    float back = i * 0.016f;
                    int fa = (int) (255 * sa * Math.min(1f, s.life) * (1f - i / 18f));
                    rect(ctx, s.x - s.vx * back, s.y - s.vy * back, 3, 3, col(fa, i < 3 ? 0xFFF0B0 : (i < 9 ? 0xFF9030 : 0xD03020)));
                }
            }
        }

        // UFO flies right -> left
        if (tier.has(Fx.UFO)) {
            float p = (elapsed - 1.0f) / 3.2f;
            if (p >= 0f && p <= 1f) ufo(ctx, sw + 30 - p * (sw + 60), sh * 0.3f + (float) Math.sin(p * 14f) * 10f, elapsed, sa);
        }

        // rocket flies left -> right
        if (tier.has(Fx.ROCKET)) {
            float p = (elapsed - 0.6f) / 3.0f;
            if (p >= 0f && p <= 1f) rocket(ctx, -30 + p * (sw + 60), sh * 0.8f + (float) Math.sin(p * 12f) * 6f, elapsed, sa);
        }

        // shockwave rings
        if (tier.has(Fx.SHOCK) || tier.has(Fx.SHOCK_LOOP)) {
            float period = tier.has(Fx.SHOCK_LOOP) ? 1.5f : 999f;
            for (int k = 0; k < 8; k++) {
                float age = elapsed - k * period;
                if (age < 0) break;
                if (age < 1.6f) {
                    circleOutline(ctx, cx, cy, age * 520f, tier.rainbow ? Color.HSBtoRGB((elapsed * 0.7f) % 1f, 0.6f, 1f) : tier.color,
                        (int) (200 * sa * (1f - age / 1.6f)), 3);
                }
            }
        }

        // black hole
        if (tier.has(Fx.BLACKHOLE)) {
            accretion(ctx, cx, cy, elapsed, sa, false);
            disc(ctx, cx, cy, 14, 0x000000, (int) (255 * sa), 1);
            circleOutline(ctx, cx, cy, 15, 0xFFAA33, (int) (230 * sa), 2);
            accretion(ctx, cx, cy, elapsed, sa, true);
        }

        // supernova: white core flash + exploding sparks, twice
        if (tier.has(Fx.SUPERNOVA)) {
            if (!secondBurst && elapsed >= 2.0f) { spawnBurst(cx, cy, 160); secondBurst = true; }
            coreFlash(ctx, cx, cy, elapsed);
            coreFlash(ctx, cx, cy, elapsed - 2.0f);
            Iterator<Spark> sit = SPARKS.iterator();
            float drag = (float) Math.pow(0.3, dt);
            while (sit.hasNext()) {
                Spark s = sit.next();
                s.x += s.vx * dt;
                s.y += s.vy * dt;
                s.vx *= drag;
                s.vy *= drag;
                s.life -= dt;
                if (s.life <= 0f) { sit.remove(); continue; }
                rect(ctx, s.x, s.y, 3, 3, col((int) (255 * Math.min(1f, s.life / s.max * 1.5f) * sa), s.color));
            }
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

        if (elapsed >= tier.seconds) {
            current = null;
            CONFETTI.clear(); STARS.clear(); SHOOTERS.clear(); SPARKS.clear(); PENDING.clear();
            return;
        }

        // Scheduled sounds
        Iterator<Object[]> pit = PENDING.iterator();
        while (pit.hasNext()) {
            Object[] s = pit.next();
            if (elapsed >= (float) s[3]) { play(s); pit.remove(); }
        }

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
        float glow = level >= 3 ? 0.75f + 0.25f * (float) Math.sin(elapsed * 6f) : 1f;
        int border = col((int) (a8 * glow), color);
        ctx.fill(x1, by, x2, by + 2, border);
        ctx.fill(x1, by + bh - 2, x2, by + bh, border);
        ctx.fill(x1, by, x1 + 2, by + bh, border);
        ctx.fill(x2 - 2, by, x2, by + bh, border);

        // Pulsing tier label, bigger for bigger tiers
        float pulse = 1f + 0.08f * (float) Math.sin(elapsed * 8f);
        float labelScale = (1.4f + 0.05f * level) * pulse;
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
