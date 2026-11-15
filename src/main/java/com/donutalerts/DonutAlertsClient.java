package com.donutalerts;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.text.Text;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DonutAlertsClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Chat listener (system/game messages, which is what servers use for pay notifications)
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            var r = AmountParser.parse(message.getString());
            if (r != null) AlertManager.push(r.player(), r.amount());
        });

        // Some servers route messages as chat instead
        ClientReceiveMessageEvents.CHAT.register((message, signed, sender, params, ts) -> {
            var r = AmountParser.parse(message.getString());
            if (r != null) AlertManager.push(r.player(), r.amount());
        });

        HudRenderCallback.EVENT.register((ctx, tickCounter) -> AlertManager.render(ctx));

        // /donotest <amount>  e.g. /donotest 10m, /donotest 1.5b  (for testing each tier)
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
            dispatcher.register(ClientCommandManager.literal("donotest")
                .then(ClientCommandManager.argument("amount", StringArgumentType.word())
                    .executes(ctx -> {
                        String raw = StringArgumentType.getString(ctx, "amount");
                        Matcher m = Pattern.compile("([\\d.]+)([kmbtKMBT]?)").matcher(raw);
                        if (!m.matches()) {
                            ctx.getSource().sendFeedback(Text.literal("Use e.g. 10m, 250m, 1b"));
                            return 0;
                        }
                        AlertManager.push("TestUser", AmountParser.toNumber(m.group(1), m.group(2)));
                        return 1;
                    }))));
    }
}
