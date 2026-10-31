package com.bekvon.bukkit.residence.gui.ui.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.bukkit.entity.Player;

import net.kyori.adventure.text.Component;

import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.gui.ui.MenuFormat;
import com.bekvon.bukkit.residence.gui.ui.config.ButtonDef;
import com.bekvon.bukkit.residence.gui.ui.config.MenuLayout;
import com.bekvon.bukkit.residence.gui.ui.text.MenuText;
import com.bekvon.bukkit.residence.gui.ui.text.UiLanguageManager;

/**
 * Screen ids and the small builders every screen shares.
 * <p>
 * The ids double as the suffix of the title keys
 * ({@code menu.title.<id>}), so adding a screen never introduces a hard coded
 * string.
 */
public final class Screens {

    public static final String MAIN = "main";
    public static final String CHAPTER = "chapter";
    public static final String RESIDENCE = "residence";
    public static final String ARGUMENT = "argument";
    public static final String CONFIRM = "confirm";
    public static final String LANGUAGE = "language";

    private Screens() {
    }

    /**
     * Title key of a screen, e.g. {@code menu.title.chapter}.
     */
    public static String titleKey(String kind) {
        return "menu.title." + key(kind);
    }

    /**
     * Joins breadcrumb parts with the configured separator.
     */
    public static String breadcrumb(String separator, List<String> parts) {
        StringBuilder sb = new StringBuilder();
        if (parts == null)
            return "";
        for (String part : parts) {
            if (part == null || part.isEmpty())
                continue;
            if (sb.length() > 0)
                sb.append(' ').append(separator).append(' ');
            sb.append(part);
        }
        return sb.toString();
    }

    /**
     * Keeps the last {@code max} breadcrumb entries, which bounds the title
     * length on deep navigation.
     */
    public static List<String> trim(List<String> path, int max) {
        List<String> out = new ArrayList<String>();
        if (path == null)
            return out;
        for (int i = Math.max(0, path.size() - max); i < path.size(); i++)
            if (path.get(i) != null && !path.get(i).isEmpty())
                out.add(path.get(i));
        return out;
    }

    /**
     * Lower case id used in config and translation keys.
     */
    public static String key(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ENGLISH);
    }

    // ------------------------------------------------------------------
    // builders shared by the screens
    // ------------------------------------------------------------------

    /**
     * A shortcut from the config, placed on its configured slot.
     */
    public static ViewNode shortcut(Residence plugin, Player player, String key, MenuText.Args args, List<String> extraLoreKeys) {
        MenuLayout layout = plugin.getMenuSettings().getLayout();
        ButtonDef def = layout.button(key);
        MenuFormat format = plugin.getMenuFormat();
        return ViewNode.builder(def.getSlot(), def.getMaterial())
                .name(format.buttonName(player, def, args))
                .lore(format.buttonLore(player, def, args, extraLoreKeys))
                .icon(def.getIconPath(), def.getIconUrl())
                .clickable(def.isEnabled())
                .build();
    }

    /**
     * A footer button from the config, e.g. back or close.
     */
    public static ViewNode footer(Residence plugin, Player player, String key, MenuText.Args args, List<String> extraLoreKeys) {
        MenuLayout layout = plugin.getMenuSettings().getLayout();
        ButtonDef def = layout.button(key);
        MenuFormat format = plugin.getMenuFormat();
        return ViewNode.builder(def.getSlot(), def.getMaterial())
                .name(format.buttonName(player, def, args))
                .lore(format.buttonLore(player, def, args, extraLoreKeys))
                .icon(def.getIconPath(), def.getIconUrl())
                .build();
    }

    /**
     * The page indicator of a paged screen.
     */
    public static ViewNode pageIndicator(Residence plugin, Player player, int page, int totalPages, MenuText.Args extra) {
        MenuLayout layout = plugin.getMenuSettings().getLayout();
        ButtonDef def = layout.button("page_indicator");
        MenuText.Args args = (extra == null ? MenuText.Args.of() : extra).put("page", page).put("pages", totalPages);
        return ViewNode.builder(layout.getPageSlot(), def.getMaterial())
                .name(plugin.getUiLanguage().get(player, def.getNameKey(), args))
                .lore(plugin.getMenuFormat().buttonLore(player, def, args, null))
                .icon(def.getIconPath(), def.getIconUrl())
                .clickable(false)
                .build();
    }

    /**
     * A previous / next page button. A direction that does not exist is drawn
     * with the configured disabled material so the footer never shifts.
     */
    public static ViewNode pageButton(Residence plugin, Player player, String key, boolean usable, int targetPage, int totalPages) {
        MenuLayout layout = plugin.getMenuSettings().getLayout();
        ButtonDef def = layout.button(key);
        UiLanguageManager lang = plugin.getUiLanguage();
        int slot = "previous".equals(key) ? layout.getPrevSlot() : layout.getNextSlot();
        String material = def.getMaterial();
        String nameKey = def.getNameKey();
        if (!usable) {
            String disabledMaterial = lang.getPlain(player, "menu.style.disabled-material", null);
            if (disabledMaterial != null && !disabledMaterial.isEmpty())
                material = disabledMaterial;
            nameKey = nameKey.endsWith(".name") ? nameKey.substring(0, nameKey.length() - 5) + ".disabled-name" : nameKey;
        }
        MenuText.Args args = MenuText.Args.of().put("page", targetPage).put("pages", totalPages);
        return ViewNode.builder(slot, material)
                .name(lang.get(player, nameKey, args))
                .lore(plugin.getMenuFormat().buttonLore(player, def, args, null))
                .icon(def.getIconPath(), def.getIconUrl())
                .clickable(usable)
                .build();
    }

    /**
     * The placeholder item shown instead of an empty list.
     *
     * @param kind one of {@code residence, player, member, flag, group, world,
     *             choice, section, action}
     */
    public static ViewNode emptyState(Residence plugin, Player player, String kind, MenuText.Args args) {
        MenuLayout layout = plugin.getMenuSettings().getLayout();
        ButtonDef def = layout.button("empty_" + key(kind));
        MenuFormat format = plugin.getMenuFormat();
        return ViewNode.builder(22, def.getMaterial())
                .name(format.buttonName(player, def, args))
                .lore(format.buttonLore(player, def, args, null))
                .icon(def.getIconPath(), def.getIconUrl())
                .clickable(false)
                .build();
    }

    /**
     * The disabled filler used for slots that hold no button on this screen.
     */
    public static Component fillerName(Residence plugin, Player player) {
        return plugin.getUiLanguage().get(player, "menu.style.filler-name", null);
    }
}
