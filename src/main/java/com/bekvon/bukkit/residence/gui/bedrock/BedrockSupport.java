package com.bekvon.bukkit.residence.gui.bedrock;

import java.util.Locale;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.bekvon.bukkit.residence.Residence;
/**
 * Detects Geyser based Bedrock clients and adapts the menu for them.
 * <p>
 * Bedrock players reach the server through GeyserMC. Their native interface is
 * a form (dropdown / input screen), not a scripted chat message, so
 * <ul>
 * <li>inventory menus become far more important — they are the closest thing
 * Bedrock has to a form,</li>
 * <li>chat input has to be explicitly requested with the Java style
 * {@code tellraw} suggestion,</li>
 * <li>hover and click text cannot be relied upon, everything has to be a real
 * inventory button.</li>
 * </ul>
 * Detection is intentionally defensive: every signal is optional and any
 * failure silently degrades to "Java edition".
 */
public final class BedrockSupport {

    private static Plugin geyser = null;
    private static boolean floodgatePresent = false;
    private static Boolean geyserDetected = null;
    private static long lastPluginScan = 0L;

    private BedrockSupport() {
    }

    /**
     * Whether a Geyser based bridge is installed. Result is cached for a minute.
     */
    public static boolean isBedrockBridgePresent() {
        long now = System.currentTimeMillis();
        if (geyserDetected != null && now - lastPluginScan < 60_000L)
            return geyserDetected;
        lastPluginScan = now;
        Plugin plugin = Bukkit.getPluginManager().getPlugin("Geyser-Spigot");
        if (plugin == null)
            plugin = Bukkit.getPluginManager().getPlugin("Geyser");
        if (plugin == null)
            plugin = Bukkit.getPluginManager().getPlugin("floodgate");
        geyser = plugin;
        floodgatePresent = Bukkit.getPluginManager().getPlugin("floodgate") != null;
        geyserDetected = plugin != null;
        return geyserDetected;
    }

    /**
     * Best effort Bedrock detection for a player.
     */
    public static boolean isBedrock(Player player) {
        if (player == null || !player.isOnline())
            return false;

        UUID uuid = player.getUniqueId();

        // Floodgate generates offline style UUIDs where the most significant
        // bits are always zero. This is the same check Floodgate itself uses and
        // it is only trusted while a bridge is actually installed.
        if (uuid != null && uuid.getMostSignificantBits() == 0L && isBedrockBridgePresent())
            return true;

        // Paper exposes the client brand; Geyser reports "Geyser". Resolved
        // reflectively because Spigot does not have the method.
        try {
            Object brand = player.getClass().getMethod("getClientBrandName").invoke(player);
            if (brand instanceof String) {
                String lower = ((String) brand).toLowerCase(Locale.ENGLISH);
                if (lower.contains("geyser") || lower.contains("bedrock") || lower.contains("floodgate"))
                    return true;
            }
        } catch (Throwable ignored) {
        }

        // Floodgate API through reflection so no compile time dependency is needed.
        try {
            java.lang.reflect.Method method = Class.forName("org.geysermc.floodgate.api.FloodgateApi")
                    .getMethod("getInstance");
            Object api = method.invoke(null);
            if (api != null) {
                java.lang.reflect.Method isFloodgatePlayer = api.getClass().getMethod("isFloodgatePlayer", UUID.class);
                Object result = isFloodgatePlayer.invoke(api, uuid);
                if (result instanceof Boolean)
                    return (Boolean) result;
            }
        } catch (Throwable ignored) {
        }

        return false;
    }

    /**
     * True when the server runs a Bedrock bridge and this particular viewer is
     * on Bedrock.
     */
    public static boolean isBedrockViewer(Player player) {
        if (!isBedrockBridgePresent())
            return false;
        return isBedrock(player);
    }

    public static boolean isFloodgatePresent() {
        if (!isBedrockBridgePresent())
            return false;
        return floodgatePresent;
    }

    /**
     * Whether the menu should be forced open instead of the chat fallback for
     * this player.
     */
    public static boolean preferMenu(Player player) {
        Residence plugin = Residence.getInstance();
        if (plugin == null)
            return false;
        try {
            if (isBedrockViewer(player) && plugin.getConfigManager().isMenuForceForBedrock())
                return true;
            return plugin.getConfigManager().isMenuEnabled();
        } catch (Throwable e) {
            return false;
        }
    }

    /**
     * The language code reported by the Bedrock client.
     * <p>
     * Floodgate stores the console language of the Bedrock player, which is the
     * only reliable way to know it: Geyser maps the Bedrock locale onto a Java
     * locale, but an unlinked player often reports {@code en_us} regardless.
     * Resolved reflectively so no compile time dependency is needed.
     *
     * @return the language code such as {@code zh_CN}, or {@code null}
     */
    public static String getBedrockLanguage(Player player) {
        if (player == null || !isFloodgatePresent())
            return null;
        try {
            Class<?> apiClass = Class.forName("org.geysermc.floodgate.api.FloodgateApi");
            Object api = apiClass.getMethod("getInstance").invoke(null);
            if (api == null)
                return null;

            Object floodgatePlayer = null;
            try {
                floodgatePlayer = api.getClass().getMethod("getPlayer", UUID.class).invoke(api, player.getUniqueId());
            } catch (NoSuchMethodException missing) {
                // Very old Floodgate builds keep the player map elsewhere; the
                // Bukkit locale is used in that case.
                return null;
            }
            if (floodgatePlayer == null)
                return null;

            for (String method : new String[] { "getLanguageCode", "getLocale" }) {
                try {
                    Object value = floodgatePlayer.getClass().getMethod(method).invoke(floodgatePlayer);
                    if (value instanceof String && !((String) value).isEmpty())
                        return (String) value;
                    if (value instanceof java.util.Locale)
                        return ((java.util.Locale) value).toLanguageTag();
                } catch (NoSuchMethodException ignored) {
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    /**
     * Whether Floodgate exposes the Cumulus form classes, which is a hard
     * requirement for sending a form.
     */
    public static boolean hasFormApi() {
        if (!isFloodgatePresent())
            return false;
        try {
            Class.forName("org.geysermc.cumulus.form.SimpleForm");
            return true;
        } catch (Throwable e) {
            return false;
        }
    }
}
