package com.bekvon.bukkit.residence.gui.menu;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import net.kyori.adventure.text.Component;

import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.gui.bedrock.BedrockSupport;

/**
 * Points players at the menu once they join, with a stronger nudge for Bedrock
 * clients.
 * <p>
 * The message is a translated component containing a clickable suggestion, so a
 * Bedrock client receives a tap friendly prompt and a Java client a clickable
 * one.
 */
public class MenuJoinListener implements Listener {

    private final Residence plugin;

    public MenuJoinListener(Residence plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        if (player == null)
            return;
        if (!plugin.getConfigManager().isMenuEnabled() || !plugin.getConfigManager().isMenuSuggestToPlayers())
            return;
        if (!player.hasPermission("residence.command.menu"))
            return;

        // Delayed so the message lands after the join spam.
        Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
            @Override
            public void run() {
                if (!player.isOnline())
                    return;
                try {
                    boolean bedrock = BedrockSupport.isBedrockViewer(player);
                    Component message = plugin.getUiLanguage().get(player, bedrock ? "menu.message.join-bedrock" : "menu.message.join", null);
                    player.sendMessage(message);
                } catch (Throwable ignored) {
                }
            }
        }, 40L);
    }
}
