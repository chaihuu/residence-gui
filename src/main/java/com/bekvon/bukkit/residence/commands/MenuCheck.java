package com.bekvon.bukkit.residence.commands;

import org.bukkit.entity.Player;

import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.gui.ui.text.MenuText;

/**
 * Diagnostic of the menu language files, used by
 * {@code /res menu check [language]}.
 * <p>
 * It reports the active language of the sender, every configured language and
 * the keys that are still untranslated, so an administrator can complete a
 * translation without guessing key names.
 */
final class MenuCheck {

    private MenuCheck() {
    }

    static void run(Residence plugin, Player player, String language) {
        com.bekvon.bukkit.residence.gui.ui.text.UiLanguageManager lang = plugin.getUiLanguage();
        section(player, "menu.check.title");
        line(player, "menu.check.active", MenuText.Args.of().put("language", lang.languageFor(player))
                .put("detected", lang.detect(player)));
        line(player, "menu.check.default", MenuText.Args.of().put("language", lang.getDefaultLanguage()));
        line(player, "menu.check.available", MenuText.Args.of().put("languages", String.join(", ", lang.getAvailable())));

        String target = language == null || language.isEmpty() ? lang.getDefaultLanguage() : language;
        java.util.List<String> missing = lang.untranslatedIn(target);
        line(player, "menu.check.missing-title", MenuText.Args.of().put("language", target).put("count", missing.size()));
        int shown = 0;
        for (String key : missing) {
            if (shown++ >= 20) {
                line(player, "menu.check.missing-more", MenuText.Args.of().put("count", missing.size() - 20));
                break;
            }
            lineRaw(player, "menu.check.missing-entry", MenuText.Args.of().put("key", key));
        }

        java.util.Set<String> runtime = lang.getMissingKeys();
        line(player, "menu.check.runtime-title", MenuText.Args.of().put("count", runtime.size()));
        for (String key : runtime)
            lineRaw(player, "menu.check.missing-entry", MenuText.Args.of().put("key", key));
    }

    private static void section(Player player, String key) {
        player.sendMessage(Residence.getInstance().getUiLanguage().get(player, key, null));
    }

    private static void line(Player player, String key, MenuText.Args args) {
        player.sendMessage(Residence.getInstance().getUiLanguage().get(player, key, args));
    }

    private static void lineRaw(Player player, String key, MenuText.Args args) {
        player.sendMessage(Residence.getInstance().getUiLanguage().get(player, key, args));
    }
}
