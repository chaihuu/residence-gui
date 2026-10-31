package com.bekvon.bukkit.residence.gui.ui.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.bukkit.configuration.ConfigurationSection;

/**
 * One group of commands shown on the root screen.
 * <p>
 * Replaces the old {@code MenuCategory} enum, which hard coded an English
 * display name and an English description. The visible strings are now
 * translation keys ({@code menu.chapter.<id>.name} / {@code ...lore}) and the
 * icon is a configurable material with fallbacks.
 */
public final class Chapter {

    private final String id;
    private final boolean admin;
    private final String icon;
    private final List<String> iconFallbacks;
    private final String iconPath;
    private final String iconUrl;
    private final int slot;
    private final List<String> commandNames;

    private Chapter(String id, boolean admin, String icon, List<String> iconFallbacks, String iconPath, String iconUrl, int slot,
            List<String> commandNames) {
        this.id = id;
        this.admin = admin;
        this.icon = icon;
        this.iconFallbacks = iconFallbacks;
        this.iconPath = iconPath;
        this.iconUrl = iconUrl;
        this.slot = slot;
        this.commandNames = commandNames;
    }

    public String getId() {
        return id;
    }

    public boolean isAdmin() {
        return admin;
    }

    public String getIcon() {
        return icon;
    }

    public List<String> getIconFallbacks() {
        return iconFallbacks;
    }

    public String getIconPath() {
        return iconPath;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    /**
     * Fixed slot on the root screen, or {@code -1} to flow with the other
     * entries.
     */
    public int getSlot() {
        return slot;
    }

    public String getNameKey() {
        return "menu.chapter." + id + ".name";
    }

    public List<String> getLoreKeys() {
        return Collections.singletonList("menu.chapter." + id + ".lore");
    }

    /**
     * Command names that belong to this chapter. Empty means every command
     * whose own chapter field matches this id.
     */
    public List<String> getCommandNames() {
        return commandNames;
    }

    // ------------------------------------------------------------------
    // built in defaults
    // ------------------------------------------------------------------

    public static List<Chapter> defaults() {
        List<Chapter> out = new ArrayList<Chapter>();
        out.add(new Chapter("general", false, "GRASS_BLOCK", Arrays.asList("DIRT"), null, null, -1, new ArrayList<String>()));
        out.add(new Chapter("flags", false, "COMPARATOR", Arrays.asList("LEVER"), null, null, -1, new ArrayList<String>()));
        out.add(new Chapter("ownership", false, "NAME_TAG", Arrays.asList("PAPER"), null, null, -1, new ArrayList<String>()));
        out.add(new Chapter("areas", false, "GOLDEN_SHOVEL", Arrays.asList("WOODEN_SHOVEL", "STONE_SHOVEL"), null, null, -1, new ArrayList<String>()));
        out.add(new Chapter("teleport", false, "ENDER_PEARL", Arrays.asList("ENDER_EYE"), null, null, -1, new ArrayList<String>()));
        out.add(new Chapter("economy", false, "GOLD_INGOT", Arrays.asList("GOLD_NUGGET"), null, null, -1, new ArrayList<String>()));
        out.add(new Chapter("raid", false, "CROSSBOW", Arrays.asList("BOW"), null, null, -1, new ArrayList<String>()));
        out.add(new Chapter("chat", false, "WRITTEN_BOOK", Arrays.asList("PAPER"), null, null, -1, new ArrayList<String>()));
        out.add(new Chapter("admin", true, "COMMAND_BLOCK", Arrays.asList("REDSTONE_BLOCK", "STONE"), null, null, -1, new ArrayList<String>()));
        out.add(new Chapter("danger", false, "TNT", Arrays.asList("REDSTONE_BLOCK"), null, null, -1, new ArrayList<String>()));
        return out;
    }

    /**
     * Reads {@code Global.GUI.Menu.Chapters}. Entries that are not listed keep
     * the built in value, and a chapter can be removed with
     * {@code chapters.<id>.enabled: false}.
     */
    public static List<Chapter> read(ConfigurationSection section) {
        List<Chapter> base = defaults();
        if (section == null)
            return base;

        List<Chapter> out = new ArrayList<Chapter>();
        for (Chapter chapter : base) {
            if (!section.isConfigurationSection(chapter.id)) {
                out.add(chapter);
                continue;
            }
            ConfigurationSection one = section.getConfigurationSection(chapter.id);
            boolean enabled = one.getBoolean("enabled", true);
            if (!enabled)
                continue;
            String icon = one.getString("material", chapter.icon);
            List<String> fallbacks = one.isList("fallback-materials") ? one.getStringList("fallback-materials") : chapter.iconFallbacks;
            String iconPath = one.getString("bedrock.icon-path", chapter.iconPath);
            String iconUrl = one.getString("bedrock.icon-url", chapter.iconUrl);
            int slot = one.getInt("slot", chapter.slot);
            List<String> commands = one.isList("commands") ? one.getStringList("commands") : chapter.commandNames;
            out.add(new Chapter(chapter.id, chapter.admin, icon, fallbacks, iconPath, iconUrl, slot, commands));
        }

        // Additional chapters defined only in the config, e.g. for a custom
        // command group.
        for (String key : section.getKeys(false)) {
            if (find(base, key, true) != null)
                continue;
            ConfigurationSection one = section.getConfigurationSection(key);
            if (one == null || !one.getBoolean("enabled", true))
                continue;
            List<String> commands = one.getStringList("commands");
            if (commands == null || commands.isEmpty())
                continue;
            List<String> fallbacks = new ArrayList<String>();
            if (one.isList("fallback-materials"))
                fallbacks = one.getStringList("fallback-materials");
            String iconPath = one.getString("bedrock.icon-path");
            String iconUrl = one.getString("bedrock.icon-url");
            out.add(new Chapter(key.toLowerCase(Locale.ENGLISH), one.getBoolean("admin", false), one.getString("material", "BOOK"), fallbacks, iconPath,
                    iconUrl, one.getInt("slot", -1), commands));
        }
        return out;
    }

    private static Chapter find(List<Chapter> list, String id, boolean ignoreCase) {
        for (Chapter chapter : list)
            if (ignoreCase ? chapter.id.equalsIgnoreCase(id) : chapter.id.equals(id))
                return chapter;
        return null;
    }
}
