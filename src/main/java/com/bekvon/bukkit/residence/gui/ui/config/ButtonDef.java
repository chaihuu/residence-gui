package com.bekvon.bukkit.residence.gui.ui.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.configuration.ConfigurationSection;

/**
 * One configurable chest menu button.
 * <p>
 * Everything that used to be hard coded in a page class lives here: the
 * material, the slot, the translation keys and whether the button is enabled.
 * The chest renderer and the Bedrock form both consume this object, so a server
 * owner changes a button once and both menus follow.
 */
public final class ButtonDef {

    private final String key;
    private final boolean enabled;
    private final String material;
    /** Configured slot, or {@code -1} when the button only exists on Bedrock. */
    private final int slot;
    private final String nameKey;
    private final List<String> loreKeys;
    private final String iconPath;
    private final String iconUrl;
    private final List<String> fallbackMaterials;

    private ButtonDef(String key, boolean enabled, String material, int slot, String nameKey, List<String> loreKeys, String iconPath, String iconUrl,
            List<String> fallbackMaterials) {
        this.key = key;
        this.enabled = enabled;
        this.material = material;
        this.slot = slot;
        this.nameKey = nameKey;
        this.loreKeys = Collections.unmodifiableList(loreKeys);
        this.iconPath = iconPath;
        this.iconUrl = iconUrl;
        this.fallbackMaterials = Collections.unmodifiableList(fallbackMaterials);
    }

    /**
     * Reads a button section. Missing values fall back to the built in
     * defaults passed in, which is what keeps old configs working.
     *
     * @param key      key used in the config (also the translation suffix)
     * @param section  the config section, may be {@code null}
     * @param fallback default definition
     */
    public static ButtonDef read(String key, ConfigurationSection section, ButtonDef fallback) {
        ButtonDef base = fallback == null ? empty(key) : fallback;

        boolean enabled = section == null ? base.enabled : section.getBoolean("enabled", base.enabled);
        String material = section == null ? base.material : section.getString("material", base.material);
        int slot = section == null ? base.slot : section.getInt("slot", base.slot);
        String nameKey = section == null ? base.nameKey : section.getString("name", base.nameKey);
        List<String> lore = section == null ? base.loreKeys : section.getStringList("lore");
        if (lore == null || lore.isEmpty())
            lore = base.loreKeys;
        String iconPath = section == null ? base.iconPath : section.getString("bedrock.icon-path", base.iconPath);
        String iconUrl = section == null ? base.iconUrl : section.getString("bedrock.icon-url", base.iconUrl);
        List<String> fallbackMaterials = section == null || !section.isList("fallback-materials") ? base.fallbackMaterials
                : section.getStringList("fallback-materials");

        // A section that only enables/disables a button keeps the default
        // texts, which is the documented way to hide a button.
        if (nameKey == null)
            nameKey = "menu.button." + key.toLowerCase(java.util.Locale.ENGLISH) + ".name";

        return new ButtonDef(key, enabled, material, slot, nameKey, new ArrayList<String>(lore), iconPath, iconUrl,
                new ArrayList<String>(fallbackMaterials));
    }

    /**
     * Built in definition used when neither the config nor a bundled default
     * contains the button.
     */
    public static ButtonDef empty(String key) {
        return new ButtonDef(key, false, "GRAY_STAINED_GLASS_PANE", -1, null, new ArrayList<String>(), null, null, new ArrayList<String>());
    }

    /**
     * Creates a default definition used by the bundled config example.
     */
    public static ButtonDef of(String key, String material, int slot, String nameKey, List<String> loreKeys) {
        return new ButtonDef(key, true, material, slot, nameKey, new ArrayList<String>(loreKeys == null ? new ArrayList<String>() : loreKeys), null, null,
                new ArrayList<String>());
    }

    /**
     * The definition with a different slot, used for the layout defaults.
     */
    public ButtonDef withSlot(int newSlot) {
        return new ButtonDef(key, enabled, material, newSlot, nameKey, new ArrayList<String>(loreKeys), iconPath, iconUrl,
                new ArrayList<String>(fallbackMaterials));
    }

    /**
     * The definition with a different translation key prefix, used when the
     * built in defaults are resolved for a scoped screen (for example the page
     * footer buttons).
     */
    public ButtonDef withNameKey(String newNameKey) {
        return new ButtonDef(key, enabled, material, slot, newNameKey, new ArrayList<String>(loreKeys), iconPath, iconUrl,
                new ArrayList<String>(fallbackMaterials));
    }

    public String getKey() {
        return key;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean hasSlot() {
        return slot >= 0;
    }

    public String getMaterial() {
        return material;
    }

    public int getSlot() {
        return slot;
    }

    public String getNameKey() {
        return nameKey;
    }

    public List<String> getLoreKeys() {
        return loreKeys;
    }

    public String getIconPath() {
        return iconPath;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    /**
     * Materials tried in order when the configured one does not exist on this
     * server version (for example {@code SPYGLASS} before 1.17).
     */
    public List<String> getFallbackMaterials() {
        return fallbackMaterials;
    }
}
