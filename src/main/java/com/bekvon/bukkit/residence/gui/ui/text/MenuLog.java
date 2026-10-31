package com.bekvon.bukkit.residence.gui.ui.text;

import java.util.logging.Logger;

/**
 * Console output of the menu.
 * <p>
 * Menu texts are language file driven and may be translated to any language, but
 * console messages are read by administrators with a console whose encoding the
 * plugin cannot control (a Windows server often runs with a legacy code page).
 * For that reason every message of the menu is written in plain ASCII, so it is
 * readable no matter which encoding the console uses. Everything a player sees
 * comes from the language files instead.
 */
public final class MenuLog {

    private MenuLog() {
    }

    public static void info(Logger logger, String message) {
        if (logger != null)
            logger.info(safe(message));
    }

    public static void warning(Logger logger, String message) {
        if (logger != null)
            logger.warning(safe(message));
    }

    public static void warning(Logger logger, String message, Throwable error) {
        if (logger == null)
            return;
        logger.warning(safe(message) + (error == null ? "" : " (" + error.getClass().getSimpleName() + ": " + safe(error.getMessage()) + ")"));
    }

    /**
     * Replaces characters a legacy console encoding cannot show, so a log line
     * never turns into a row of question marks.
     */
    public static String safe(String message) {
        if (message == null)
            return "";
        StringBuilder sb = new StringBuilder(message.length());
        for (int i = 0; i < message.length(); i++) {
            char c = message.charAt(i);
            sb.append(c >= 32 && c < 127 ? c : '?');
        }
        return sb.toString();
    }
}
