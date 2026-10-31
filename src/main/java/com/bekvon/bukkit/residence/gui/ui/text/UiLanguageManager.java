package com.bekvon.bukkit.residence.gui.ui.text;

import com.bekvon.bukkit.residence.gui.ui.text.MenuLog;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import net.kyori.adventure.text.Component;

import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.gui.bedrock.BedrockSupport;

/**
 * Translation source for the Residence menu (chest menu and Bedrock form).
 * <p>
 * Independent from the legacy {@code Language/<Name>.yml} files that CMILib
 * generates for chat messages. The menu owns the files below
 * {@code plugins/Residence/lang/<code>.yml} where every key is one string of the
 * user interface.
 * <p>
 * Resolution order for one key:
 * <ol>
 * <li>the player's language (detected from the client),</li>
 * <li>the configured default language,</li>
 * <li>the bundled resource of the default language, so a deleted key still
 * shows a readable sentence after an update,</li>
 * <li>the key itself, which makes a genuinely missing translation visible in
 * game instead of showing an empty button.</li>
 * </ol>
 */
public final class UiLanguageManager {

    /** Marker used when a key exists nowhere. */
    private static final String MISSING_PREFIX = "<dark_red>[missing: ";

    private final Residence plugin;

    /** Normalised language code to the loaded file. */
    private final Map<String, YamlConfiguration> files = new ConcurrentHashMap<String, YamlConfiguration>();
    /** Normalised language code to the resource bundled in the jar. */
    private final Map<String, YamlConfiguration> builtins = new ConcurrentHashMap<String, YamlConfiguration>();
    /** Keys that could not be resolved anywhere, reported by /res menu check. */
    private final Set<String> missingKeys = Collections.synchronizedSet(new LinkedHashSet<String>());
    /** Cached resolved language of a player. */
    private final Map<UUID, String> playerLanguage = new ConcurrentHashMap<UUID, String>();

    private String defaultLanguage = "en_US";
    private List<String> available = new ArrayList<String>();

    public UiLanguageManager(Residence plugin) {
        this.plugin = plugin;
    }

    // ------------------------------------------------------------------
    // loading
    // ------------------------------------------------------------------

    /**
     * Loads every configured language. Missing files are written from the
     * bundled resources on first use, so translating the menu is only editing
     * one file.
     */
    public void load() {
        com.bekvon.bukkit.residence.gui.ui.config.MenuSettings settings = settings();
        this.defaultLanguage = normalize(settings.getDefaultLanguage());
        List<String> configured = settings.getAvailableLanguages();
        this.available = configured == null || configured.isEmpty() ? new ArrayList<String>(Arrays.asList("zh_CN", "en_US"))
                : new ArrayList<String>(configured);

        files.clear();
        builtins.clear();
        playerLanguage.clear();
        missingKeys.clear();

        File folder = folder();
        if (!folder.isDirectory())
            folder.mkdirs();

        for (String code : new ArrayList<String>(available)) {
            String norm = normalize(code);
            YamlConfiguration builtin = readBuiltin(code);
            if (builtin != null)
                builtins.put(norm, builtin);

            File file = new File(folder, code + ".yml");
            if (!file.isFile()) {
                if (builtin != null)
                    writeBuiltin(code, file);
                else
                    MenuLog.warning(plugin.getLogger(), "No bundled language file for menu language " + code);
            }
            YamlConfiguration loaded = readFile(file);
            files.put(norm, loaded != null ? loaded : (builtin == null ? new YamlConfiguration() : builtin));
        }

        // The default language has to exist even when it is not part of the
        // available list.
        if (!files.containsKey(defaultLanguage)) {
            YamlConfiguration builtin = readBuiltin(defaultLanguage);
            if (builtin != null)
                builtins.put(defaultLanguage, builtin);
            YamlConfiguration loaded = readFile(new File(folder, defaultLanguage + ".yml"));
            files.put(defaultLanguage, loaded != null ? loaded : (builtin == null ? new YamlConfiguration() : builtin));
        }
    }

    public void reload() {
        load();
    }

    private com.bekvon.bukkit.residence.gui.ui.config.MenuSettings settings() {
        try {
            com.bekvon.bukkit.residence.gui.ui.config.MenuSettings settings = plugin.getConfigManager().getMenuSettings();
            if (settings != null)
                return settings;
        } catch (Throwable ignored) {
        }
        return com.bekvon.bukkit.residence.gui.ui.config.MenuSettings.defaults();
    }

    private File folder() {
        return new File(plugin.getDataFolder(), "lang");
    }

    private YamlConfiguration readBuiltin(String code) {
        String resource = "lang/" + code + ".yml";
        try (InputStream in = plugin.getResource(resource)) {
            if (in == null)
                return null;
            return load(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (Throwable e) {
            MenuLog.warning(plugin.getLogger(), "Unable to read bundled menu language " + resource, e);
            return null;
        }
    }

    private YamlConfiguration readFile(File file) {
        if (file == null || !file.isFile())
            return null;
        try (InputStreamReader reader = new InputStreamReader(Files.newInputStream(file.toPath()), StandardCharsets.UTF_8)) {
            return load(reader);
        } catch (Throwable e) {
            MenuLog.warning(plugin.getLogger(), "Unable to read menu language file " + file.getName(), e);
            return null;
        }
    }

    private YamlConfiguration load(InputStreamReader reader) throws Exception {
        YamlConfiguration config = new YamlConfiguration();
        config.load(reader);
        return config;
    }

    private void writeBuiltin(String code, File target) {
        try (InputStream in = plugin.getResource("lang/" + code + ".yml")) {
            if (in != null)
                Files.copy(in, target.toPath());
        } catch (Throwable e) {
            MenuLog.warning(plugin.getLogger(), "Unable to write menu language file " + code, e);
        }
    }

    // ------------------------------------------------------------------
    // language detection
    // ------------------------------------------------------------------

    /**
     * Normalises a language code. Java reports {@code zh_cn}, Bedrock reports
     * {@code zh_CN} or {@code zh-CN}; all of them become {@code zh_cn}.
     */
    public static String normalize(String code) {
        if (code == null)
            return "";
        return code.trim().replace('-', '_').toLowerCase(Locale.ENGLISH);
    }

    /**
     * Finds the configured language that matches a client language.
     *
     * @param clientLocale e.g. {@code zh_cn}, {@code zh-tw}, {@code en_us}
     * @return the configured code ({@code zh_CN}) or {@code null}
     */
    public String matchAvailable(String clientLocale) {
        String target = normalize(clientLocale);
        if (target.isEmpty())
            return null;

        // Exact regional match first, so an explicitly configured regional file
        // always wins over the language wide file.
        for (String code : files.keySet())
            if (normalize(code).equals(target))
                return code;

        // Language wide match: "zh_tw" uses the first configured "zh" language.
        String language = target.contains("_") ? target.substring(0, target.indexOf('_')) : target;
        for (String code : files.keySet()) {
            String norm = normalize(code);
            if (norm.equals(language) || norm.startsWith(language + "_"))
                return code;
        }
        return null;
    }

    /**
     * The language used for a player, cached because detection of a Bedrock
     * client uses the Floodgate API through reflection.
     */
    public String languageFor(Player player) {
        if (player == null)
            return defaultLanguage;
        String cached = playerLanguage.get(player.getUniqueId());
        if (cached != null)
            return cached;
        String resolved = matchAvailable(detect(player));
        if (resolved == null)
            resolved = defaultLanguage;
        playerLanguage.put(player.getUniqueId(), resolved);
        return resolved;
    }

    /**
     * Reads the client language of a player.
     * <p>
     * Bedrock clients are asked through Floodgate first, because that is where
     * Geyser forwards the console language; the Bukkit locale is the fallback
     * for Java players and for older Floodgate versions.
     *
     * @return the raw client locale, never {@code null}
     */
    public String detect(Player player) {
        if (player == null)
            return defaultLanguage;

        if (BedrockSupport.isBedrockViewer(player)) {
            String floodgateLanguage = BedrockSupport.getBedrockLanguage(player);
            if (floodgateLanguage != null && !floodgateLanguage.isEmpty())
                return floodgateLanguage;
        }
        return clientLocale(player);
    }

    private String clientLocale(Player player) {
        try {
            Object locale = player.getClass().getMethod("getLocale").invoke(player);
            if (locale instanceof String && !((String) locale).isEmpty())
                return (String) locale;
        } catch (Throwable ignored) {
        }
        try {
            Object locale = player.getClass().getMethod("locale").invoke(player);
            if (locale instanceof Locale)
                return ((Locale) locale).toLanguageTag();
        } catch (Throwable ignored) {
        }
        return defaultLanguage;
    }

    /**
     * Forces a language for the next menus of this player. An unknown code
     * clears the override so auto detection takes over again.
     */
    public boolean setLanguage(Player player, String code) {
        if (player == null)
            return false;
        String match = matchAvailable(code);
        if (match == null) {
            playerLanguage.remove(player.getUniqueId());
            return false;
        }
        playerLanguage.put(player.getUniqueId(), match);
        return true;
    }

    /**
     * Forgets the cached language so it is detected again on the next open.
     */
    public void forget(Player player) {
        if (player != null)
            playerLanguage.remove(player.getUniqueId());
    }

    // ------------------------------------------------------------------
    // lookup
    // ------------------------------------------------------------------

    /**
     * Raw template in the player's language.
     */
    public String raw(Player player, String key) {
        return raw(languageFor(player), key);
    }

    /**
     * Raw template in a specific language. Values are returned exactly as
     * written in the file (after legacy code conversion) so they can be used
     * inside other templates.
     */
    public String raw(String language, String key) {
        if (key == null)
            return "";
        String hit = value(files.get(normalize(language)), key);
        if (hit != null)
            return hit;
        hit = value(files.get(defaultLanguage), key);
        if (hit != null)
            return hit;
        hit = value(builtins.get(defaultLanguage), key);
        if (hit != null)
            return hit;
        for (YamlConfiguration config : builtins.values()) {
            hit = value(config, key);
            if (hit != null)
                return hit;
        }
        missingKeys.add(key);
        return MISSING_PREFIX + key + ">]";
    }

    /**
     * Whether the key exists in at least one language.
     */
    public boolean has(String key) {
        if (key == null)
            return false;
        for (YamlConfiguration config : files.values())
            if (value(config, key) != null)
                return true;
        for (YamlConfiguration config : builtins.values())
            if (value(config, key) != null)
                return true;
        return false;
    }

    /**
     * Rendered component. Player may be {@code null} for console output, in
     * which case the default language is used.
     */
    public Component get(Object player, String key, MenuText.Args args) {
        String language = player instanceof Player ? languageFor((Player) player) : defaultLanguage;
        return MenuText.renderStrict(raw(language, key), args);
    }

    public Component get(Player player, String key) {
        return get(player, key, null);
    }

    /**
     * Rendered component where unresolved placeholders are removed.
     */
    public Component getStrict(Player player, String key, MenuText.Args args) {
        return MenuText.renderStrict(raw(player, key), args);
    }

    /**
     * Plain text, for logs and for the Bedrock client language database.
     */
    public String getPlain(Player player, String key, MenuText.Args args) {
        return MenuText.plain(get(player, key, args));
    }

    public String getPlain(String language, String key, MenuText.Args args) {
        return MenuText.plain(MenuText.render(raw(language, key), args));
    }

    public String getPlain(Player player, String key) {
        return getPlain(player, key, null);
    }

    /**
     * A list of translated lines (usage bullets, hints ...).
     */
    public List<Component> getList(Player player, String key, MenuText.Args args) {
        List<Component> out = new ArrayList<Component>();
        for (String line : rawList(player, key))
            out.add(MenuText.render(line, args));
        return out;
    }

    public List<String> rawList(Player player, String key) {
        return rawList(languageFor(player), key);
    }

    public List<String> rawList(String language, String key) {
        List<String> out = new ArrayList<String>();
        collectList(files.get(normalize(language)), key, out);
        if (out.isEmpty())
            collectList(files.get(defaultLanguage), key, out);
        if (out.isEmpty())
            collectList(builtins.get(defaultLanguage), key, out);
        // A missing list is not reported when the key exists as a string in
        // any language, which is what the single line fallback of value() does.
        if (out.isEmpty() && value(files.get(normalize(language)), key) == null && value(files.get(defaultLanguage), key) == null
                && value(builtins.get(defaultLanguage), key) == null && !has(key))
            missingKeys.add(key);
        return out;
    }

    private void collectList(YamlConfiguration config, String key, List<String> out) {
        if (config == null || key == null)
            return;
        try {
            if (config.get(key) instanceof List) {
                for (String line : config.getStringList(key))
                    if (line != null && !line.isEmpty())
                        out.add(Markup.toMiniMessage(line));
            } else if (config.isString(key)) {
                out.add(Markup.toMiniMessage(config.getString(key)));
            }
        } catch (Throwable ignored) {
        }
    }

    private String value(YamlConfiguration config, String key) {
        if (config == null || key == null)
            return null;
        try {
            if (config.isString(key)) {
                String value = config.getString(key);
                return value == null ? null : Markup.toMiniMessage(value);
            }
            if (config.get(key) instanceof List) {
                List<String> lines = config.getStringList(key);
                return lines.isEmpty() ? "" : Markup.toMiniMessage(lines.get(0));
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    // ------------------------------------------------------------------
    // diagnostics
    // ------------------------------------------------------------------

    public String getDefaultLanguage() {
        return defaultLanguage;
    }

    /**
     * The configured codes, e.g. {@code zh_CN}.
     */
    public List<String> getAvailable() {
        return Collections.unmodifiableList(available);
    }

    public Set<String> getMissingKeys() {
        return missingKeys;
    }

    /**
     * Code to the language name as shown in the language picker
     * ({@code menu.language.<code>.name}).
     */
    public Map<String, String> getLanguageNames() {
        Map<String, String> out = new LinkedHashMap<String, String>();
        for (String code : available)
            out.put(code, getPlain(defaultLanguage, "menu.language." + normalize(code) + ".name", null));
        return out;
    }

    /**
     * Keys that exist somewhere but not in the given language, so an
     * administrator can see what still needs a translation.
     */
    public List<String> untranslatedIn(String language) {
        Set<String> all = new LinkedHashSet<String>();
        for (YamlConfiguration config : builtins.values())
            all.addAll(config.getKeys(true));
        for (YamlConfiguration config : files.values())
            all.addAll(config.getKeys(true));
        YamlConfiguration target = files.get(normalize(language));
        List<String> out = new ArrayList<String>();
        for (String key : all) {
            if (key == null)
                continue;
            if (target != null && value(target, key) != null)
                continue;
            if (has(key))
                out.add(key);
        }
        Collections.sort(out);
        return out;
    }

    /**
     * Every string key of a language, used to generate the Bedrock client
     * translations.
     */
    public Map<String, String> strings(String language) {
        Map<String, String> out = new LinkedHashMap<String, String>();
        YamlConfiguration config = files.get(normalize(language));
        if (config != null)
            for (String key : config.getKeys(true))
                if (config.isString(key))
                    out.put(key, config.getString(key));
        return out;
    }

    public Residence getPlugin() {
        return plugin;
    }
}
