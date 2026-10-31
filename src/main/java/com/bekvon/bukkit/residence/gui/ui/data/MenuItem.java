package com.bekvon.bukkit.residence.gui.ui.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.entity.Player;

import net.kyori.adventure.text.Component;

import com.bekvon.bukkit.residence.gui.ui.MenuFormat;
import com.bekvon.bukkit.residence.gui.ui.text.MenuText;

/**
 * One pickable value of a list (a residence, a player, a flag, a literal
 * choice ...).
 * <p>
 * The item keeps translation keys plus the placeholder values for them, so the
 * text is rendered in the viewer's language at the moment the screen is built.
 * Both renderers read the same rendered components.
 */
public final class MenuItem {

    private final String value;
    private final Component name;
    private final List<String> loreKeys;
    private final MenuText.Args args;
    private final String material;
    private final String iconPath;
    private final String iconUrl;

    public MenuItem(String value, Component name, List<String> loreKeys, MenuText.Args args, String material, String iconPath, String iconUrl) {
        this.value = value;
        this.name = name == null ? Component.empty() : name;
        this.loreKeys = loreKeys == null ? new ArrayList<String>() : loreKeys;
        this.args = args == null ? MenuText.Args.of() : args;
        this.material = material;
        this.iconPath = iconPath;
        this.iconUrl = iconUrl;
    }

    public String getValue() {
        return value;
    }

    public Component getName() {
        return name;
    }

    public List<String> getLoreKeys() {
        return Collections.unmodifiableList(loreKeys);
    }

    public MenuText.Args getArgs() {
        return args;
    }

    public String getMaterial() {
        return material;
    }

    public String getIconPath() {
        return iconPath;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    /**
     * Renders the tooltip through the shared visual grammar.
     *
     * @param hints translation keys of the click hints, may be {@code null}
     */
    public List<Component> renderLore(MenuFormat format, Player player, List<String> hints) {
        List<Component> lore = new ArrayList<Component>();
        format.appendDescription(lore, player, loreKeys, args);
        format.appendHints(lore, player, hints, null);
        return lore;
    }
}
