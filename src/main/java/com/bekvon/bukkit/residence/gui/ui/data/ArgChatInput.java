package com.bekvon.bukkit.residence.gui.ui.data;

import java.util.List;

import org.bukkit.entity.Player;

import net.kyori.adventure.text.Component;

import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.gui.bedrock.ClientCompat;

/**
 * Asks the player to type a free text argument in chat.
 * <p>
 * Free text is the only value a chest menu cannot offer as a list, and it is
 * the normal path for Bedrock players, whose Geyser keyboard prompt is more
 * comfortable than a form input. The prompt itself is translated.
 */
public final class ArgChatInput {

    private ArgChatInput() {
    }

    /**
     * Prompts for a value and remembers the wizard state, so the chat listener
     * continues the flow once the player answers.
     */
    public static void request(Residence plugin, Player player, CommandSpec spec, List<ArgDef> args, List<String> picked, int index, ArgDef def,
            MenuScreens screens) {
        if (player == null || !player.isOnline())
            return;

        ChatInputListener.setPending(player, new ChatInputListener.PendingInput(spec, picked, index));

        String label = def == null ? "" : plugin.getUiLanguage().getPlain(player, CommandRegistry.argLabelKey(spec == null ? "" : spec.getName(), def), null);
        com.bekvon.bukkit.residence.gui.ui.text.MenuText.Args placeholders = com.bekvon.bukkit.residence.gui.ui.text.MenuText.Args.of()
                .put("label", label).put("command", spec == null ? "" : spec.getName());
        Component prompt = plugin.getUiLanguage().get(player, "menu.prompt.chat-input", placeholders);
        String suggestion = plugin.getUiLanguage().getPlain(player, "menu.prompt.cancel", null);
        ClientCompat.requestChatInput(player, com.bekvon.bukkit.residence.gui.ui.text.MenuText.plain(prompt), suggestion == null ? "" : suggestion);
    }
}
