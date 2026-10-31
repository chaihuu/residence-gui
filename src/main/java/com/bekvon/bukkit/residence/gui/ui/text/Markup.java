package com.bekvon.bukkit.residence.gui.ui.text;

import java.util.Arrays;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Backwards compatibility helper for menu texts.
 * <p>
 * Every menu string is rendered with MiniMessage, this class only exists so that
 * a server owner who already replaced a menu string in an old config (with the
 * familiar {@code &6} / {@code §6} codes) is not shown a literal ampersand.
 * <p>
 * The conversion is tag aware: an ampersand inside a MiniMessage tag such as
 * {@code <click:run_command:"/res list &test">} is left untouched. Strings that
 * already contain MiniMessage tags are returned unchanged, so new configs are
 * always rendered exactly as written.
 */
public final class Markup {

    private static final Pattern COLOUR = Pattern.compile("(?i)&([0-9a-fk-or])");
    private static final Pattern HEX = Pattern.compile("(?i)&?#([0-9a-f]{6})");
    /**
     * Opening or closing MiniMessage tag. The optional slash of a closing tag is
     * part of the matched text, not of the tag name.
     */
    private static final Pattern TAG = Pattern.compile("</?[A-Za-z][^<>\\s]*((?:\\s|:)[^<>]*)?>");

    private Markup() {
    }

    /**
     * Converts legacy colour codes to MiniMessage tags when needed.
     * <p>
     * Only a string that is not already written in MiniMessage is converted, so
     * the ampersand of a normal sentence (or a URL) is never turned into a
     * colour. A {@code §} section sign is always treated as a legacy code,
     * because it cannot appear as plain text by accident.
     *
     * @param input raw text from a language file
     * @return the same text if it already uses MiniMessage, otherwise an
     *         equivalent MiniMessage string
     */
    public static String toMiniMessage(String input) {
        if (input == null || input.isEmpty())
            return input;
        boolean sectionSign = input.indexOf('\u00A7') >= 0;
        if (!sectionSign && (TAG.matcher(input).find() || !containsLegacy(input)))
            return input;
        return convert(input);
    }

    /**
     * Whether the string contains something that looks like a legacy colour code.
     */
    public static boolean containsLegacy(String input) {
        if (input == null || input.isEmpty())
            return false;
        if (input.indexOf('\u00A7') >= 0)
            return true;
        return COLOUR.matcher(input).find() || HEX.matcher(input).find();
    }

    /**
     * Converts a whole list, keeping the order.
     */
    public static java.util.List<String> toMiniMessage(java.util.List<String> input) {
        java.util.List<String> out = new java.util.ArrayList<String>();
        if (input == null)
            return out;
        for (String line : input)
            out.add(toMiniMessage(line));
        return out;
    }

    /**
     * Converts legacy codes only when the string is actually authored with them.
     * <p>
     * A string that already contains a MiniMessage tag is treated as MiniMessage
     * and returned unchanged: an ampersand in real text (a URL such as
     * {@code https://example.com/?a=1&b=2}) is just text there.
     */
    private static String convert(String input) {
        String text = input.replace('\u00A7', '&');
        return replaceCodes(text);
    }

    private static String replaceCodes(String input) {
        Matcher hex = HEX.matcher(input);
        StringBuffer step = new StringBuffer();
        while (hex.find())
            hex.appendReplacement(step, Matcher.quoteReplacement("<#" + hex.group(1) + ">"));
        hex.appendTail(step);

        Matcher colour = COLOUR.matcher(step.toString());
        StringBuffer out = new StringBuffer();
        while (colour.find())
            colour.appendReplacement(out, Matcher.quoteReplacement("<" + tagFor(colour.group(1)) + ">"));
        colour.appendTail(out);
        return out.toString();
    }

    private static String tagFor(String code) {
        switch (Character.toLowerCase(code.charAt(0))) {
        case '0':
            return "black";
        case '1':
            return "dark_blue";
        case '2':
            return "dark_green";
        case '3':
            return "dark_aqua";
        case '4':
            return "dark_red";
        case '5':
            return "dark_purple";
        case '6':
            return "gold";
        case '7':
            return "gray";
        case '8':
            return "dark_gray";
        case '9':
            return "blue";
        case 'a':
            return "green";
        case 'b':
            return "aqua";
        case 'c':
            return "red";
        case 'd':
            return "light_purple";
        case 'e':
            return "yellow";
        case 'f':
            return "white";
        case 'k':
            return "obfuscated";
        case 'l':
            return "bold";
        case 'm':
            return "strikethrough";
        case 'n':
            return "underlined";
        case 'o':
            return "italic";
        case 'r':
            return "reset";
        default:
            return "reset";
        }
    }

    /**
     * Joins parts with a separator, skipping empty parts.
     */
    public static String join(String separator, String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String part : Arrays.asList(parts)) {
            if (part == null || part.isEmpty())
                continue;
            if (sb.length() > 0)
                sb.append(separator);
            sb.append(part);
        }
        return sb.toString();
    }
}
