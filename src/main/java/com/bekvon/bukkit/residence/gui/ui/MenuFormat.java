package com.bekvon.bukkit.residence.gui.ui;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.Player;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.gui.ui.config.ButtonDef;
import com.bekvon.bukkit.residence.gui.ui.text.MenuText;
import com.bekvon.bukkit.residence.gui.ui.text.UiLanguageManager;

/**
 * The visual grammar shared by the chest menu and the Bedrock form.
 * <p>
 * Buttons follow one structure everywhere:
 * <pre>
 *   &lt;name colour&gt;Name
 *   (blank)
 *   gray   description, wrapped
 *   (blank)
 *   dark_gray bullet click hint
 *              bullet modifier hint
 * </pre>
 * All decorations (separator, bullet, blank filler, colours and wrap width) are
 * language keys below {@code menu.style}, so the whole look can be restyled
 * without touching code. Both user interfaces stay identical because they
 * consume the very same {@link Component} tree.
 */
public final class MenuFormat {

    private final Residence plugin;

    public MenuFormat(Residence plugin) {
        this.plugin = plugin;
    }

    public UiLanguageManager lang() {
        return plugin.getUiLanguage();
    }

    // ------------------------------------------------------------------
    // style values
    // ------------------------------------------------------------------

    private String style(Player player, String key, String fallback) {
        String value = lang().getPlain(player, key, null);
        return value == null || value.isEmpty() ? fallback : value;
    }

    /** Name of the menu, e.g. {@code Residence}. */
    public String brand(Player player) {
        return style(player, "menu.style.brand", "Residence");
    }

    /** Breadcrumb separator. */
    public String separator(Player player) {
        return style(player, "menu.style.separator", ">");
    }

    /** Colour tag applied to descriptions. */
    public String descriptionColour(Player player) {
        return style(player, "menu.style.description-colour", "<gray>");
    }

    /** Colour tag applied to the hints block. */
    public String hintsColour(Player player) {
        return style(player, "menu.style.hints-colour", "<dark_gray>");
    }

    /** Bullet in front of a hint. */
    public String bullet(Player player) {
        return style(player, "menu.style.bullet", "\u2022");
    }

    /** The filler used as an empty tooltip line. */
    public String blankLine(Player player) {
        return style(player, "menu.style.blank-line", " ");
    }

    /** Maximum characters of a description line. */
    public int wrapWidth(Player player) {
        String key = com.bekvon.bukkit.residence.gui.bedrock.BedrockSupport.isBedrockViewer(player) ? "menu.style.wrap-width-bedrock"
                : "menu.style.wrap-width";
        try {
            return Integer.parseInt(style(player, key, "52").trim());
        } catch (NumberFormatException e) {
            return 52;
        }
    }

    // ------------------------------------------------------------------
    // titles
    // ------------------------------------------------------------------

    /**
     * Title of a screen: {@code Residence > Flags}.
     *
     * @param path rendered path, may be {@code null} for the root screen
     */
    public Component title(Player player, String path) {
        String template = lang().raw(player, "menu.style.title");
        MenuText.Args args = MenuText.Args.of().put("brand", MenuText.plain(brand(player), null));
        if (path != null && !path.isEmpty()) {
            args.put("separator", MenuText.plain(separator(player), NamedTextColor.DARK_GRAY));
            args.put("path", MenuText.plain(path, null));
        }
        return MenuText.renderStrict(template, args);
    }

    /**
     * Title plus page counter, used by the paged screens.
     */
    public Component title(Player player, String path, int page, int totalPages) {
        Component base = title(player, path);
        if (totalPages <= 1)
            return base;
        MenuText.Args args = MenuText.Args.of().put("page", page).put("pages", totalPages);
        return base.append(MenuText.renderStrict(lang().raw(player, "menu.style.title-page"), args));
    }

    /**
     * Rendered translation as a component.
     */
    public Component component(Player player, String key, MenuText.Args args) {
        return MenuText.renderStrict(lang().raw(player, key), args);
    }

    // ------------------------------------------------------------------
    // button tooltips
    // ------------------------------------------------------------------

    /**
     * Name of a configured button.
     */
    public Component buttonName(Player player, ButtonDef def, MenuText.Args args) {
        return MenuText.renderStrict(lang().raw(player, def.getNameKey()), args);
    }

    /**
     * Lore of a configured button, using its configured lore keys.
     *
     * @param extraKeys additional state lines appended after the configured
     *                  ones (selection state, counters ...)
     */
    public List<Component> buttonLore(Player player, ButtonDef def, MenuText.Args args, List<String> extraKeys) {
        List<String> keys = new ArrayList<String>(def.getLoreKeys());
        if (extraKeys != null)
            keys.addAll(extraKeys);
        List<Component> lore = new ArrayList<Component>();
        appendDescription(lore, player, keys, args);
        return lore;
    }

    /**
     * Appends a description block. A blank line is inserted first when the
     * tooltip already has content, which keeps every tooltip spaced the same
     * way.
     */
    public void appendDescription(List<Component> lore, Player player, List<String> keys, MenuText.Args args) {
        List<Component> lines = new ArrayList<Component>();
        if (keys != null) {
            for (String key : keys) {
                if (key == null || key.isEmpty())
                    continue;
                String template = lang().raw(player, key);
                if (template == null || template.isEmpty())
                    continue;
                Component rendered = MenuText.renderStrict(template, args);
                if (MenuText.plain(rendered).isEmpty())
                    continue;
                lines.addAll(wrap(player, rendered));
            }
        }
        if (lines.isEmpty())
            return;
        appendSeparated(lore, player, lines);
    }

    /**
     * Appends the hint block: one bulleted, dark gray line per hint key.
     */
    public void appendHints(List<Component> lore, Player player, List<String> keys, MenuText.Args args) {
        List<Component> lines = new ArrayList<Component>();
        if (keys != null) {
            for (String key : keys) {
                if (key == null || key.isEmpty())
                    continue;
                Component hint = MenuText.renderStrict(lang().raw(player, key), args);
                if (MenuText.plain(hint).isEmpty())
                    continue;
                lines.add(withBullet(player, hint));
            }
        }
        if (lines.isEmpty())
            return;
        appendSeparated(lore, player, lines);
    }

    /**
     * A complete tooltip: description lines followed by the hint lines.
     */
    public List<Component> tooltip(Player player, List<String> descriptionKeys, MenuText.Args descriptionArgs, List<String> hintKeys, MenuText.Args hintArgs) {
        List<Component> lore = new ArrayList<Component>();
        appendDescription(lore, player, descriptionKeys, descriptionArgs);
        appendHints(lore, player, hintKeys, hintArgs);
        return lore;
    }

    /**
     * Wraps a rendered line to the configured width and applies the description
     * colour when the line does not bring its own colour.
     */
    public List<Component> wrap(Player player, Component line) {
        List<Component> out = new ArrayList<Component>();
        if (line == null)
            return out;
        String plain = MenuText.plain(line);
        int width = wrapWidth(player);
        if (width <= 0 || plain.length() <= width) {
            out.add(applyDefaultColour(player, line));
            return out;
        }
        // A line that has to be re-flowed is rebuilt from its plain text. The
        // description colour is re-applied so the tooltip keeps its style even
        // though exact inline colours of a very long sentence are lost.
        for (String part : MenuText.wrap(plain, width))
            out.add(MenuText.render(descriptionColour(player) + MenuText.escape(part), null));
        return out;
    }

    private Component applyDefaultColour(Player player, Component line) {
        if (line.style().color() != null)
            return line;
        Component prefix = MenuText.render(descriptionColour(player), null);
        return Component.empty().append(prefix).append(line);
    }

    /**
     * Adds the configured blank separator and then the lines.
     */
    private void appendSeparated(List<Component> lore, Player player, List<Component> lines) {
        if (lines.isEmpty())
            return;
        if (!lore.isEmpty())
            lore.add(MenuText.render(blankLine(player), null));
        lore.addAll(lines);
    }

    /**
     * Prefixes a line with the translated hint bullet in the hint colour.
     */
    public Component withBullet(Player player, Component line) {
        Component bulletPart = MenuText.render(hintsColour(player) + MenuText.escape(bullet(player)) + " ", null);
        return Component.empty().append(bulletPart).append(line);
    }

    /**
     * A translated hint line on its own.
     */
    public Component hintLine(Player player, String key, MenuText.Args args) {
        return withBullet(player, MenuText.renderStrict(lang().raw(player, key), args));
    }

    /**
     * Empty line used by screens that need extra vertical space.
     */
    public Component blank(Player player) {
        return MenuText.render(blankLine(player), null);
    }

    public Residence getPlugin() {
        return plugin;
    }
}
