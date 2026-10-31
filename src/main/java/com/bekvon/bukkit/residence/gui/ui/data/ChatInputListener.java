package com.bekvon.bukkit.residence.gui.ui.data;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import com.bekvon.bukkit.residence.Residence;

/**
 * Collects free text arguments through chat.
 * <p>
 * The menu asks for input, the player types it and the command continues
 * automatically. A timeout and the translated cancel word keep the player from
 * getting stuck, and every message of this listener comes from the language
 * files.
 */
public class ChatInputListener implements Listener {

    private static final long TIMEOUT_MILLIS = 120_000L;

    private static final Map<UUID, PendingInput> PENDING = new ConcurrentHashMap<UUID, PendingInput>();

    /** One outstanding request for chat input. */
    public static class PendingInput {
        final CommandSpec spec;
        final List<String> picked;
        final int index;
        final long created = System.currentTimeMillis();

        PendingInput(CommandSpec spec, List<String> picked, int index) {
            this.spec = spec;
            this.picked = picked;
            this.index = index;
        }
    }

    public static boolean isPending(Player player) {
        if (player == null)
            return false;
        PendingInput input = PENDING.get(player.getUniqueId());
        if (input == null)
            return false;
        if (System.currentTimeMillis() - input.created > TIMEOUT_MILLIS) {
            PENDING.remove(player.getUniqueId());
            return false;
        }
        return true;
    }

    public static void cancel(Player player) {
        if (player != null)
            PENDING.remove(player.getUniqueId());
    }

    static void setPending(Player player, PendingInput input) {
        if (player == null)
            return;
        PENDING.put(player.getUniqueId(), input);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (player == null)
            return;
        PendingInput input = PENDING.get(player.getUniqueId());
        if (input == null)
            return;

        if (System.currentTimeMillis() - input.created > TIMEOUT_MILLIS) {
            PENDING.remove(player.getUniqueId());
            return;
        }

        String message = event.getMessage();
        event.setCancelled(true);
        if (message == null)
            return;

        Residence plugin = Residence.getInstance();
        String trimmed = message.trim();

        if (isCancelWord(plugin, player, trimmed)) {
            PENDING.remove(player.getUniqueId());
            send(player, plugin, "menu.message.input-cancelled", null);
            return;
        }

        if (trimmed.isEmpty()) {
            send(player, plugin, "menu.message.input-empty", null);
            return;
        }

        PENDING.remove(player.getUniqueId());

        final List<String> next = CommandFlow.withValue(input.picked, input.index, trimmed);
        final int nextIndex = input.index + 1;
        final CommandSpec spec = input.spec;

        Bukkit.getScheduler().runTask(plugin, new Runnable() {
            @Override
            public void run() {
                if (!player.isOnline())
                    return;
                MenuScreens screens = plugin.getMenuManager().session(player);
                CommandFlow.next(plugin, screens, spec, next, nextIndex);
            }
        });
    }

    /**
     * The cancel words come from the language file, so a server can accept its
     * own word and Bedrock players get the word they expect.
     */
    private boolean isCancelWord(Residence plugin, Player player, String message) {
        if (message == null || message.isEmpty())
            return false;
        for (String word : plugin.getUiLanguage().rawList(player, "menu.cancel-words"))
            if (word != null && word.trim().equalsIgnoreCase(message))
                return true;
        return false;
    }

    private void send(Player player, Residence plugin, String key, com.bekvon.bukkit.residence.gui.ui.text.MenuText.Args args) {
        player.sendMessage(plugin.getUiLanguage().get(player, key, args));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        PENDING.remove(event.getPlayer().getUniqueId());
        Residence plugin = Residence.getInstance();
        if (plugin != null)
            plugin.getUiLanguage().forget(event.getPlayer());
    }

    /**
     * Removes stale entries so the map cannot grow forever.
     */
    public static void prune() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<UUID, PendingInput>> iterator = PENDING.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, PendingInput> entry = iterator.next();
            if (now - entry.getValue().created > TIMEOUT_MILLIS)
                iterator.remove();
        }
    }
}
