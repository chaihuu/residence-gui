package com.bekvon.bukkit.residence.gui.ui.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.bukkit.configuration.ConfigurationSection;

/**
 * The visual contract shared by the chest menu and the Bedrock form.
 * <p>
 * It holds the border filler, the fixed footer slots and every button
 * definition. Pages ask this object for a button instead of hard coding a
 * material or a slot, which is what makes the two user interfaces look the same
 * and what makes the layout configurable.
 * <p>
 * {@link #defaults()} describes the shipped layout. Everything read from
 * {@code config.yml} overrides it key by key, so an old config that only has
 * {@code Global.GUI.Menu.Enabled} keeps working unchanged.
 */
public final class MenuLayout {

    /** Slots of the paged content area for a six row inventory. */
    public static final int CONTENT_SLOTS = 45;

    private final String fillerMaterial;
    private final List<String> fillerFallbacks;
    private final String titleKey;
    private final int prevSlot;
    private final int nextSlot;
    private final int pageSlot;
    private final int backSlot;
    private final int closeSlot;
    private final int infoSlot;
    private final int languageSlot;
    private final int rows;
    private final boolean fillBorder;
    private final int bedrockMaxButtons;
    private final Map<String, ButtonDef> buttons;

    private MenuLayout(String fillerMaterial, List<String> fillerFallbacks, String titleKey, int prevSlot, int nextSlot, int pageSlot, int backSlot,
            int closeSlot, int infoSlot, int languageSlot, int rows, boolean fillBorder, int bedrockMaxButtons, Map<String, ButtonDef> buttons) {
        this.fillerMaterial = fillerMaterial;
        this.fillerFallbacks = fillerFallbacks;
        this.titleKey = titleKey;
        this.prevSlot = prevSlot;
        this.nextSlot = nextSlot;
        this.pageSlot = pageSlot;
        this.backSlot = backSlot;
        this.closeSlot = closeSlot;
        this.infoSlot = infoSlot;
        this.languageSlot = languageSlot;
        this.rows = rows;
        this.fillBorder = fillBorder;
        this.bedrockMaxButtons = bedrockMaxButtons;
        this.buttons = buttons;
    }

    // ------------------------------------------------------------------
    // built in defaults
    // ------------------------------------------------------------------

    /**
     * The shipped layout. Kept in code so a missing config section still
     * produces a complete, pretty menu.
     */
    public static MenuLayout defaults() {
        MenuLayoutBuilder builder = new MenuLayoutBuilder();
        builder.fillerMaterial = "GRAY_STAINED_GLASS_PANE";
        builder.titleKey = "menu.title";
        builder.prevSlot = 48;
        builder.nextSlot = 50;
        builder.pageSlot = 49;
        builder.backSlot = 45;
        builder.closeSlot = 53;
        builder.infoSlot = 4;
        builder.languageSlot = 8;
        builder.rows = 6;
        builder.fillBorder = true;
        builder.bedrockMaxButtons = 12;

        // Navigation and paging.
        builder.nav("previous", "ARROW", "menu.button.previous.name", "menu.button.previous.lore");
        builder.nav("next", "ARROW", "menu.button.next.name", "menu.button.next.lore");
        builder.nav("page_indicator", "PAPER", "menu.button.page.name", "menu.button.page.lore");
        builder.nav("back", "SPECTRAL_ARROW", "menu.button.back.name", "menu.button.back.lore");
        builder.nav("close", "BARRIER", "menu.button.close.name", "menu.button.close.lore");
        builder.nav("info", "BOOK", "menu.button.info.name", "menu.button.info.lore");
        builder.nav("language", "KNOWLEDGE_BOOK", "menu.button.language.name", "menu.button.language.lore");

        // Root screen shortcuts and states.
        builder.button("current_residence", "GRASS_BLOCK", 40, "menu.button.current_residence.name", "menu.button.current_residence.lore");
        builder.button("current_residence_empty", "DIRT", 40, "menu.button.current_residence_empty.name", "menu.button.current_residence_empty.lore");
        builder.button("quick_flags", "COMPARATOR", 42, "menu.button.quick_flags.name", "menu.button.quick_flags.lore");
        builder.button("residence_list", "CHEST", 44, "menu.button.residence_list.name", "menu.button.residence_list.lore");

        // Confirmation screen.
        builder.button("confirm", "LIME_CONCRETE", 22, "menu.button.confirm.name", "menu.button.confirm.lore");
        builder.button("confirm_danger", "RED_CONCRETE", 22, "menu.button.confirm_danger.name", "menu.button.confirm_danger.lore");
        builder.button("cancel", "BARRIER", 24, "menu.button.cancel.name", "menu.button.cancel.lore");
        builder.button("command_preview", "WRITABLE_BOOK", 13, "menu.button.command_preview.name", "menu.button.command_preview.lore");

        // Argument screens.
        builder.button("type_in_chat", "OAK_SIGN", 49, "menu.button.type_in_chat.name", "menu.button.type_in_chat.lore");
        builder.button("type_manually", "WRITABLE_BOOK", 48, "menu.button.type_manually.name", "menu.button.type_manually.lore");
        builder.button("value_minus", "RED_STAINED_GLASS_PANE", 38, "menu.button.value_minus.name", "menu.button.value_minus.lore");
        builder.button("value_plus", "LIME_STAINED_GLASS_PANE", 40, "menu.button.value_plus.name", "menu.button.value_plus.lore");
        builder.button("value_current", "GOLD_BLOCK", 39, "menu.button.value_current.name", "menu.button.value_current.lore");
        builder.button("value_use", "EMERALD_BLOCK", 43, "menu.button.value_use.name", "menu.button.value_use.lore");

        // Empty states, shown as placeholder items when a list is empty.
        builder.button("empty_residences", "GRASS_BLOCK", 22, "menu.empty.residence.name", "menu.empty.residence.lore");
        builder.button("empty_players", "PLAYER_HEAD", 22, "menu.empty.player.name", "menu.empty.player.lore");
        builder.button("empty_members", "PLAYER_HEAD", 22, "menu.empty.member.name", "menu.empty.member.lore");
        builder.button("empty_flags", "WHITE_BANNER", 22, "menu.empty.flag.name", "menu.empty.flag.lore");
        builder.button("empty_groups", "NAME_TAG", 22, "menu.empty.group.name", "menu.empty.group.lore");
        builder.button("empty_worlds", "GRASS_BLOCK", 22, "menu.empty.world.name", "menu.empty.world.lore");
        builder.button("empty_choices", "BARRIER", 22, "menu.empty.choice.name", "menu.empty.choice.lore");
        builder.button("empty_sections", "BOOK", 22, "menu.empty.section.name", "menu.empty.section.lore");
        builder.button("empty_actions", "BARRIER", 22, "menu.empty.action.name", "menu.empty.action.lore");

        // Language picker.
        builder.button("language_entry", "KNOWLEDGE_BOOK", -1, "menu.button.language_entry.name", "menu.button.language_entry.lore");

        return builder.build();
    }

    /**
     * Reads the {@code Global.GUI.Menu.Layout} section.
     */
    public static MenuLayout read(ConfigurationSection section, MenuLayout base) {
        MenuLayoutBuilder builder = new MenuLayoutBuilder(base == null ? defaults() : base);
        if (section == null)
            return builder.build();

        builder.fillerMaterial = section.getString("filler.material", builder.fillerMaterial);
        // Alternative filler materials are tried in order when the primary one
        // does not exist on this server version.
        if (section.isList("filler.fallback-materials"))
            builder.fillerFallbacks = normalise(section.getStringList("filler.fallback-materials"));
        else if (section.isList("filler.materials"))
            builder.fillerFallbacks = normalise(section.getStringList("filler.materials"));
        else
            builder.fillerFallbacks = new ArrayList<String>();
        builder.titleKey = section.getString("title", builder.titleKey);
        builder.rows = clamp(section.getInt("rows", builder.rows), 2, 6);
        builder.fillBorder = section.getBoolean("fill-border", builder.fillBorder);
        builder.bedrockMaxButtons = clamp(section.getInt("bedrock.max-buttons", builder.bedrockMaxButtons), 4, 20);

        ConfigurationSection slots = section.getConfigurationSection("slots");
        if (slots != null) {
            builder.prevSlot = slots.getInt("previous", builder.prevSlot);
            builder.nextSlot = slots.getInt("next", builder.nextSlot);
            builder.pageSlot = slots.getInt("page", builder.pageSlot);
            builder.backSlot = slots.getInt("back", builder.backSlot);
            builder.closeSlot = slots.getInt("close", builder.closeSlot);
            builder.infoSlot = slots.getInt("info", builder.infoSlot);
            builder.languageSlot = slots.getInt("language", builder.languageSlot);
        }

        ConfigurationSection buttons = section.getConfigurationSection("buttons");
        if (buttons != null) {
            for (String key : buttons.getKeys(false)) {
                ConfigurationSection one = buttons.getConfigurationSection(key);
                if (one == null)
                    continue;
                String norm = key.toLowerCase(Locale.ENGLISH);
                builder.buttons.put(norm, ButtonDef.read(norm, one, builder.buttons.get(norm)));
            }
        }

        return builder.build();
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static List<String> normalise(List<String> input) {
        List<String> out = new ArrayList<String>();
        if (input == null)
            return out;
        for (String line : input)
            if (line != null && !line.isEmpty())
                out.add(line);
        return out;
    }

    // ------------------------------------------------------------------
    // accessors
    // ------------------------------------------------------------------

    /**
     * A button definition, never {@code null}.
     *
     * @param key e.g. {@code previous, next, back, close, confirm}
     */
    public ButtonDef button(String key) {
        if (key == null)
            return ButtonDef.empty("unknown");
        ButtonDef def = buttons.get(key.toLowerCase(Locale.ENGLISH));
        return def == null ? ButtonDef.empty(key.toLowerCase(Locale.ENGLISH)) : def;
    }

    public boolean isEnabled(String key) {
        return button(key).isEnabled();
    }

    public String getFillerMaterial() {
        return fillerMaterial;
    }

    public List<String> getFillerFallbacks() {
        return fillerFallbacks;
    }

    public String getTitleKey() {
        return titleKey;
    }

    public int getPrevSlot() {
        return prevSlot;
    }

    public int getNextSlot() {
        return nextSlot;
    }

    public int getPageSlot() {
        return pageSlot;
    }

    public int getBackSlot() {
        return backSlot;
    }

    public int getCloseSlot() {
        return closeSlot;
    }

    public int getInfoSlot() {
        return infoSlot;
    }

    public int getLanguageSlot() {
        return languageSlot;
    }

    public int getRows() {
        return rows;
    }

    /**
     * Number of slots available for the paged content, i.e. every slot except
     * the footer row when the inventory has more than one row.
     */
    public int contentSlots() {
        return Math.max(9, (rows - 1) * 9);
    }

    public int getInventorySize() {
        return rows * 9;
    }

    public boolean isFillBorder() {
        return fillBorder;
    }

    public int getBedrockMaxButtons() {
        return bedrockMaxButtons;
    }

    public Map<String, ButtonDef> getButtons() {
        return buttons;
    }

    /**
     * Mutable builder used while reading the config.
     */
    private static final class MenuLayoutBuilder {
        String fillerMaterial = "GRAY_STAINED_GLASS_PANE";
        List<String> fillerFallbacks = new ArrayList<String>();
        String titleKey = "menu.title";
        int prevSlot = 48;
        int nextSlot = 50;
        int pageSlot = 49;
        int backSlot = 45;
        int closeSlot = 53;
        int infoSlot = 4;
        int languageSlot = 8;
        int rows = 6;
        boolean fillBorder = true;
        int bedrockMaxButtons = 12;
        final java.util.LinkedHashMap<String, ButtonDef> buttons = new java.util.LinkedHashMap<String, ButtonDef>();

        MenuLayoutBuilder() {
        }

        MenuLayoutBuilder(MenuLayout base) {
            this.fillerMaterial = base.fillerMaterial;
            this.fillerFallbacks = new ArrayList<String>(base.fillerFallbacks);
            this.titleKey = base.titleKey;
            this.prevSlot = base.prevSlot;
            this.nextSlot = base.nextSlot;
            this.pageSlot = base.pageSlot;
            this.backSlot = base.backSlot;
            this.closeSlot = base.closeSlot;
            this.infoSlot = base.infoSlot;
            this.languageSlot = base.languageSlot;
            this.rows = base.rows;
            this.fillBorder = base.fillBorder;
            this.bedrockMaxButtons = base.bedrockMaxButtons;
            this.buttons.putAll(base.buttons);
        }

        void nav(String key, String material, String nameKey, String loreKey) {
            buttons.put(key, ButtonDef.of(key, material, -1, nameKey, single(loreKey)));
        }

        void button(String key, String material, int slot, String nameKey, String loreKey) {
            buttons.put(key, ButtonDef.of(key, material, slot, nameKey, single(loreKey)));
        }

        private List<String> single(String loreKey) {
            List<String> out = new ArrayList<String>();
            if (loreKey != null)
                out.add(loreKey);
            return out;
        }

        MenuLayout build() {
            buttons.put("previous", buttons.get("previous").withSlot(prevSlot));
            buttons.put("next", buttons.get("next").withSlot(nextSlot));
            buttons.put("page_indicator", buttons.get("page_indicator").withSlot(pageSlot));
            buttons.put("back", buttons.get("back").withSlot(backSlot));
            buttons.put("close", buttons.get("close").withSlot(closeSlot));
            buttons.put("info", buttons.get("info").withSlot(infoSlot));
            buttons.put("language", buttons.get("language").withSlot(languageSlot));
            return new MenuLayout(fillerMaterial, fillerFallbacks, titleKey, prevSlot, nextSlot, pageSlot, backSlot, closeSlot, infoSlot,
                    languageSlot, rows, fillBorder, bedrockMaxButtons, new java.util.LinkedHashMap<String, ButtonDef>(buttons));
        }
    }
}
