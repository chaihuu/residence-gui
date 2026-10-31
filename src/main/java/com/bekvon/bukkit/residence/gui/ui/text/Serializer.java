package com.bekvon.bukkit.residence.gui.ui.text;

import net.kyori.adventure.text.Component;

/**
 * Serialisation helpers for Adventure components.
 * <p>
 * The menu keeps components in memory and only serialises where the platform
 * requires a string, so no legacy colour codes are involved at any point.
 */
public final class Serializer {

    private Serializer() {
    }

    /**
     * JSON form, understood by Paper, Geyser and Spigot.
     */
    public static String json(Component component) {
        if (component == null)
            return "";
        try {
            return net.kyori.adventure.text.serializer.gson.GsonComponentSerializer.gson().serialize(component);
        } catch (Throwable e) {
            try {
                return net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacyAmpersand().serialize(component);
            } catch (Throwable ignored) {
                return MenuText.plain(component);
            }
        }
    }

    /**
     * Parses a JSON component produced by {@link #json(Component)}.
     */
    public static Component parseJson(String json) {
        if (json == null || json.isEmpty())
            return Component.empty();
        try {
            return net.kyori.adventure.text.serializer.gson.GsonComponentSerializer.gson().deserialize(json);
        } catch (Throwable e) {
            return Component.text(json);
        }
    }
}
