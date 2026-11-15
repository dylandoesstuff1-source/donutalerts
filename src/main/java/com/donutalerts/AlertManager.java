package com.donutalerts;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;

import java.awt.Color;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AlertManager {
    private record Alert(String player, double amount, Tier tier) {}

    private static class Confetti {
        float x, y, vx, vy; int color; int size;
    }

    private static final ArrayDeque<Alert> QUEUE = new ArrayDeque<>();
    private static final List<Confetti> CONFETTI = new ArrayList<>();
    private static final Random RNG = new Random();

    private static Alert current;
    private static long startMs, lastFrameMs;

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
    }

    private static float easeOutBack(float t) {
        float c1 = 1.70158f, c3 = c1 + 1;
        return 1 + c3 * (float) Math.pow(t - 1, 3) + c1 * (float) Math.pow(t - 1, 2);
    }

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
        float elapsed = (now - startMs) / 1000f;
        float dt = Math.min((now - lastFrameMs) / 1000f, 0.05f);
        lastFrameMs = now;

        if (elapsed >= tier.seconds) { current = null; CONFETTI.clear(); return; }

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

        // Screen flash on high tiers
        if (tier.flash && elapsed < 0.6f) {
            int fa = (int) ((1f - elapsed / 0.6f) * 90);
            ctx.fill(0, 0, sw, sh, (fa << 24) | color);
        }

        // Banner
        int bw = 260, bh = 46;
        float slide = easeOutBack(in);
        int by = (int) (-bh + slide * (bh + 12));
        int x1 = cx - bw / 2, x2 = cx + bw / 2;

        ctx.fill(x1, by, x2, by + bh, (int) (alpha * 200) << 24 | 0x101010);
        int border = (a8 << 24) | color;
        ctx.fill(x1, by, x2, by + 2, border);
        ctx.fill(x1, by + bh - 2, x2, by + bh, border);
        ctx.fill(x1, by, x1 + 2, by + bh, border);
        ctx.fill(x2 - 2, by, x2, by + bh, border);

        // Pulsing tier label (scaled text)
        float pulse = 1f + 0.08f * (float) Math.sin(elapsed * 8f);
        var m = ctx.getMatrices();
        m.push();
        m.translate(cx, by + 8, 0);
        m.scale(1.4f * pulse, 1.4f * pulse, 1f);
        ctx.drawCenteredTextWithShadow(mc.textRenderer, Text.literal(tier.label), 0, 0, (a8 << 24) | color);
        m.pop();

        String line = current.player + " paid you $" + AmountParser.format(current.amount);
        ctx.drawCenteredTextWithShadow(mc.textRenderer, Text.literal(line), cx, by + 30, (a8 << 24) | 0xFFFFFF);

        // Confetti
        for (Confetti c : CONFETTI) {
            c.vy += 380f * dt;
            c.x += c.vx * dt;
            c.y += c.vy * dt;
            if (c.y > sh) continue;
            ctx.fill((int) c.x, (int) c.y, (int) c.x + c.size, (int) c.y + c.size, (a8 << 24) | (c.color & 0xFFFFFF));
        }
    }
}
