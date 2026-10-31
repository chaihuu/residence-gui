package com.bekvon.bukkit.residence.gui.ui.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.configuration.ConfigurationSection;

import com.bekvon.bukkit.residence.gui.ui.data.Chapter;

/**
 * Runtime settings of the Residence menu.
 * <p>
 * Read from the existing {@code Global.GUI.Menu} section, which keeps every old
 * config valid: the three keys that existed before
 * ({@code Enabled}, {@code ForceForBedrock}, {@code SuggestOnJoin}) keep working
 * and everything below is optional.
 */
public final class MenuSettings {

    private final boolean enabled;
    private final boolean forceForBedrock;
    private final boolean suggestOnJoin;
    /** Which user interface a client gets. */
    private final ClientUi defaultClientUi;
    private final boolean bedrockForms;
    private final boolean bedrockAutoDetect;
    private final String defaultLanguage;
    private final List<String> availableLanguages;
    private final boolean autoDetectLanguage;
    private final boolean showLanguageButton;
    /** Diagnostic: render translation keys instead of text. */
    private final boolean debugPreferKeys;
    private final MenuLayout layout;
    private final List<Chapter> chapters;

    public enum ClientUi {
        /** Classic inventory menu for every client. */
        INVENTORY,
        /** Bedrock forms when the client can use them, inventory otherwise. */
        AUTO,
        /** Forms for Bedrock clients, inventory for Java players. */
        BEDROCK_FORM;

        public static ClientUi parse(String raw) {
            if (raw == null)
                return AUTO;
            String value = raw.trim().toUpperCase(java.util.Locale.ENGLISH);
            for (ClientUi one : values())
                if (one.name().equals(value))
                    return one;
            return AUTO;
        }
    }

    private MenuSettings(boolean enabled, boolean forceForBedrock, boolean suggestOnJoin, ClientUi defaultClientUi, boolean bedrockForms,
            boolean bedrockAutoDetect, String defaultLanguage, List<String> availableLanguages, boolean autoDetectLanguage,
            boolean showLanguageButton, boolean debugPreferKeys, MenuLayout layout, List<Chapter> chapters) {
        this.enabled = enabled;
        this.forceForBedrock = forceForBedrock;
        this.suggestOnJoin = suggestOnJoin;
        this.defaultClientUi = defaultClientUi;
        this.bedrockForms = bedrockForms;
        this.bedrockAutoDetect = bedrockAutoDetect;
        this.defaultLanguage = defaultLanguage;
        this.availableLanguages = availableLanguages;
        this.autoDetectLanguage = autoDetectLanguage;
        this.showLanguageButton = showLanguageButton;
        this.debugPreferKeys = debugPreferKeys;
        this.layout = layout;
        this.chapters = chapters;
    }

    /**
     * Settings used before the config has been read.
     */
    public static MenuSettings defaults() {
        return new MenuSettings(true, true, true, ClientUi.AUTO, true, true, "en_US", new ArrayList<String>(Arrays.asList("zh_CN", "en_US")), true, true,
                false, MenuLayout.defaults(), Chapter.defaults());
    }

    /**
     * Reads the section. Every value is optional.
     */
    public static MenuSettings read(ConfigurationSection section) {
        MenuSettings base = defaults();
        if (section == null)
            return base;

        boolean enabled = section.getBoolean("Enabled", base.enabled);
        boolean forceForBedrock = section.getBoolean("ForceForBedrock", base.forceForBedrock);
        boolean suggestOnJoin = section.getBoolean("SuggestOnJoin", base.suggestOnJoin);

        ClientUi ui = ClientUi.parse(section.getString("ClientUi", ClientUi.AUTO.name()));
        boolean forms = section.getBoolean("Bedrock.UseForms", true);
        boolean autoDetect = section.getBoolean("Bedrock.AutoDetect", true);

        ConfigurationSection lang = section.getConfigurationSection("Language");
        String defaultLanguage = "en_US";
        List<String> available = new ArrayList<String>(Arrays.asList("zh_CN", "en_US"));
        boolean autoDetectLanguage = true;
        boolean showLanguageButton = true;
        boolean debugPreferKeys = false;
        if (lang != null) {
            defaultLanguage = lang.getString("Default", "en_US");
            if (lang.isList("Available"))
                available = lang.getStringList("Available");
            autoDetectLanguage = lang.getBoolean("AutoDetect", true);
            showLanguageButton = lang.getBoolean("ShowLanguageButton", true);
            debugPreferKeys = lang.getBoolean("DebugPreferKeys", false);
        }

        MenuLayout layout = MenuLayout.read(section.getConfigurationSection("Layout"), MenuLayout.defaults());
        List<Chapter> chapters = Chapter.read(section.getConfigurationSection("Chapters"));

        return new MenuSettings(enabled, forceForBedrock, suggestOnJoin, ui, forms, autoDetect, defaultLanguage == null ? "en_US" : defaultLanguage,
                available == null ? new ArrayList<String>() : available, autoDetectLanguage, showLanguageButton, debugPreferKeys, layout, chapters);
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * When enabled, Bedrock players are pointed at the menu by default.
     */
    public boolean isForceForBedrock() {
        return forceForBedrock;
    }

    public boolean isSuggestOnJoin() {
        return suggestOnJoin;
    }

    public ClientUi getDefaultClientUi() {
        return defaultClientUi;
    }

    public boolean isBedrockForms() {
        return bedrockForms;
    }

    public boolean isBedrockAutoDetect() {
        return bedrockAutoDetect;
    }

    public String getDefaultLanguage() {
        return defaultLanguage;
    }

    public List<String> getAvailableLanguages() {
        return availableLanguages;
    }

    public boolean isAutoDetectLanguage() {
        return autoDetectLanguage;
    }

    public boolean isShowLanguageButton() {
        return showLanguageButton;
    }

    /**
     * Diagnostic mode: shows the translation key of every string next to the
     * text so a server owner can find the key to translate.
     */
    public boolean isDebugPreferKeys() {
        return debugPreferKeys;
    }

    public MenuLayout getLayout() {
        return layout;
    }

    /**
     * Chapters shown on the root screen, in display order.
     */
    public List<Chapter> getChapters() {
        return chapters;
    }

    /**
     * Whether a Bedrock client should receive forms instead of an inventory.
     * <p>
     * Forms need the Cumulus classes that Floodgate ships, so they are only used
     * when Floodgate is present. A server that forces the chest menu for every
     * client keeps working exactly as before.
     */
    public boolean useFormFor(boolean bedrockClient, boolean floodgateAvailable) {
        if (!bedrockForms || !bedrockClient)
            return false;
        if (defaultClientUi == ClientUi.INVENTORY)
            return false;
        return floodgateAvailable;
    }
}
