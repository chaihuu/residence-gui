package com.bekvon.bukkit.residence.gui.bedrock;

import org.bukkit.entity.Player;

import net.Zrips.CMILib.RawMessages.RawMessage;

/**
 * Chat and input helpers that behave sensibly on both Java and Bedrock
 * clients.
 * <p>
 * The important differences:
 * <ul>
 * <li>Java players can click chat suggestions and hover for details, Bedrock
 * players cannot — so anything interactive must live in the inventory menu.</li>
 * <li>Bedrock players may have chat collapsed; a "click to type" prompt has to
 * be a {@code show_text} component that Geyser translates into a tappable
 * prompt.</li>
 * <li>Bedrock forms have a fixed height, so long lore lines are wrapped
 * manually instead of relying on the client.</li>
 * </ul>
 */
public final class ClientCompat {

    private static final int BEDROCK_LORE_WIDTH = 42;
    private static final int JAVA_LORE_WIDTH = 60;

    private ClientCompat() {
    }

    /**
     * Ask the player to type something in chat.
     *
     * @param player  the player that should type
     * @param prompt  the prompt to display
     * @param prefill text that should be pre-filled into the chat box
     */
    public static void requestChatInput(Player player, String prompt, String prefill) {
        if (player == null || !player.isOnline())
            return;
        try {
            RawMessage rm = new RawMessage();
            rm.addText(prompt);
            rm.addSuggestion(prefill == null ? "" : prefill);
            rm.show(player);
        } catch (Throwable e) {
            player.sendMessage(prompt + (prefill == null ? "" : prefill));
        }
    }

    /**
     * Wrap a lore line so it also looks reasonable on Bedrock forms, where long
     * lines are not wrapped automatically.
     */
    public static java.util.List<String> wrap(Player player, String line) {
        int width = isBedrock(player) ? BEDROCK_LORE_WIDTH : JAVA_LORE_WIDTH;
        java.util.List<String> out = new java.util.ArrayList<String>();
        if (line == null)
            return out;
        String stripped = stripFormatting(line);
        if (stripped.length() <= width) {
            out.add(line);
            return out;
        }
        StringBuilder current = new StringBuilder();
        for (String word : line.split(" ")) {
            String cleanWord = stripFormatting(word);
            if (current.length() > 0 && current.length() + cleanWord.length() + 1 > width) {
                out.add(current.toString());
                current.setLength(0);
            }
            if (current.length() > 0)
                current.append(' ');
            current.append(word);
        }
        if (current.length() > 0)
            out.add(current.toString());
        return out;
    }

    public static boolean isBedrock(Player player) {
        return BedrockSupport.isBedrockViewer(player);
    }

    private static String stripFormatting(String text) {
        return text.replaceAll("(?i)\u00A7[0-9A-FK-ORX]", "").replaceAll("(?i)&[0-9A-FK-ORX]", "");
    }
}
