package com.bekvon.bukkit.residence.gui.ui.text;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

/**
 * The single text engine of the menu.
 * <p>
 * Every string of the chest menu and of the Bedrock form goes through this
 * class, which gives three guarantees:
 * <ul>
 * <li><b>MiniMessage only</b> - no legacy colour codes are ever produced. The
 * Adventure component is kept as a component (Paper sends it directly, other
 * servers get JSON), so Geyser forwards readable colours to Bedrock,</li>
 * <li><b>safe values</b> - a value such as a residence named
 * {@code <rainbow>} is inserted as plain text and can never inject
 * formatting, while the language file itself still uses MiniMessage,</li>
 * <li><b>never throws</b> - a broken tag in a language file degrades to plain
 * text instead of breaking the menu.</li>
 * </ul>
 */
public final class MenuText {

    private static final Pattern TOKEN = Pattern.compile("\\{([A-Za-z0-9_]+)}");

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private MenuText() {
    }

    // ------------------------------------------------------------------
    // placeholders
    // ------------------------------------------------------------------

    /**
     * Collects the values of a translation before it is rendered.
     */
    public static final class Args {
        private final java.util.Map<String, Component> values = new java.util.LinkedHashMap<String, Component>();

        public Args() {
        }

        public static Args of() {
            return new Args();
        }

        /**
         * A plain value such as a player or residence name. It is inserted
         * verbatim and is never parsed as MiniMessage.
         */
        public Args put(String key, String value) {
            if (key != null && value != null)
                values.put(key, plain(value, null));
            return this;
        }

        /**
         * A plain value rendered in the given colour.
         */
        public Args put(String key, String value, NamedTextColor colour) {
            if (key != null && value != null)
                values.put(key, plain(value, colour));
            return this;
        }

        public Args put(String key, long value) {
            return put(key, String.valueOf(value));
        }

        /**
         * An already rendered component, e.g. a translated button name.
         */
        public Args put(String key, Component value) {
            if (key != null && value != null)
                values.put(key, value);
            return this;
        }

        public Args putAll(Args other) {
            if (other != null)
                values.putAll(other.values);
            return this;
        }

        public boolean isEmpty() {
            return values.isEmpty();
        }

        Component get(String key) {
            return values.get(key);
        }
    }

    /**
     * A plain value as a component, so it can be inserted into a template
     * without being parsed.
     */
    public static Component plain(String text, NamedTextColor colour) {
        if (text == null)
            return Component.empty();
        return Component.text().content(text).color(colour).decoration(TextDecoration.ITALIC, false).build();
    }

    // ------------------------------------------------------------------
    // rendering
    // ------------------------------------------------------------------

    /**
     * Renders a MiniMessage template.
     * <p>
     * Placeholders are written as {@code <key>} tags, which is the form
     * MiniMessage resolves reliably; a value is inserted as its own component,
     * so a residence called {@code <rainbow>} can never inject formatting.
     * {@code {key}} written in a language file is translated to the tag form
     * first, because that is the most readable form for a translator.
     *
     * @param template template from a language file
     * @param args     placeholder values, may be {@code null}
     */
    public static Component render(String template, Args args) {
        if (template == null || template.isEmpty())
            return Component.empty();
        try {
            TagResolver resolver = args == null || args.isEmpty() ? TagResolver.empty() : resolver(args);
            return MINI.deserialize(Token.braceToTag(template, args), resolver);
        } catch (Throwable brokenTemplate) {
            // Show the template as plain text so the failure is visible in game
            // while the menu keeps working.
            return substitute(TAG_STRIPPER.matcher(Markup.toMiniMessage(template)).replaceAll(" ").trim(), args);
        }
    }

    /**
     * Renders a template and removes placeholders the caller did not supply, so
     * a player never sees a raw token.
     */
    public static Component renderStrict(String template, Args args) {
        Component component = render(template, args);
        String text = plain(component);
        if (!TOKEN.matcher(text).find())
            return component;
        return Component.text(TOKEN.matcher(text).replaceAll("").replace("  ", " ").trim()).style(component.style());
    }

    /**
     * True when a rendered component still contains an unresolved token.
     */
    public static boolean hasLeftover(Component component) {
        return component != null && TOKEN.matcher(plain(component)).find();
    }

    /**
     * Sends a rendered component to an audience.
     */
    public static void send(Audience audience, Component component) {
        if (audience != null && component != null)
            audience.sendMessage(component);
    }

    /**
     * Plain text of a component, used for the Bedrock form content and for log
     * messages.
     */
    public static String plain(Component component) {
        if (component == null)
            return "";
        try {
            return PlainTextComponentSerializer.plainText().serialize(component);
        } catch (Throwable e) {
            return "";
        }
    }

    /**
     * Plain text of a rendered template.
     */
    public static String plainOf(String template, Args args) {
        return plain(render(template, args));
    }

    /**
     * Turns text that must not contain formatting into a component.
     */
    public static Component literal(String text) {
        if (text == null || text.isEmpty())
            return Component.empty();
        return Component.text().content(text).decoration(TextDecoration.ITALIC, false).build();
    }

    /**
     * Escapes raw user text so it can be embedded inside a MiniMessage template
     * as a literal value.
     */
    public static String escape(String text) {
        if (text == null || text.isEmpty())
            return "";
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\\' || c == '<' || c == '\u00A7')
                sb.append('\\');
            sb.append(c);
        }
        return sb.toString();
    }

    /**
     * Removes MiniMessage tags without rendering, used for the client language
     * database which has to stay plain text.
     */
    public static String stripTags(String text) {
        if (text == null)
            return "";
        try {
            return MINI.stripTags(Markup.toMiniMessage(text));
        } catch (Throwable e) {
            return TAG_STRIPPER.matcher(Markup.toMiniMessage(text)).replaceAll("");
        }
    }

    /**
     * Wraps a long plain text into lines of at most {@code width} characters.
     */
    public static List<String> wrap(String text, int width) {
        List<String> out = new ArrayList<String>();
        if (text == null || text.isEmpty())
            return out;
        if (width <= 0 || text.length() <= width) {
            out.add(text);
            return out;
        }
        StringBuilder current = new StringBuilder();
        for (String word : text.split(" ")) {
            if (current.length() > 0 && current.length() + word.length() + 1 > width) {
                out.add(current.toString());
                current.setLength(0);
            }
            if (current.length() > 0)
                current.append(' ');
            current.append(word);
        }
        if (current.length() > 0)
            out.add(current.toString());
        return out;
    }

    /**
     * Normalises a colour tag, e.g. {@code gray} or {@code <gray>} both become
     * {@code <gray>}.
     */
    public static String colourTag(String value) {
        if (value == null || value.isEmpty())
            return "";
        String trimmed = value.trim();
        if (trimmed.startsWith("<") && trimmed.endsWith(">"))
            return trimmed;
        return "<" + trimmed.toLowerCase(Locale.ENGLISH) + ">";
    }

    /**
     * Applies a colour tag around a template.
     */
    public static String tint(String template, String colour) {
        if (template == null)
            return "";
        String tag = colourTag(colour);
        if (tag.isEmpty())
            return template;
        return tag + template + "</" + tag.substring(1);
    }

    /**
     * Short clickable message used by the join hint.
     */
    public static Component clickable(String text, String command, String hover) {
        Component component = literal(text);
        Style.Builder style = component.style().toBuilder();
        if (command != null)
            style.clickEvent(ClickEvent.runCommand(command));
        if (hover != null)
            style.hoverEvent(HoverEvent.showText(literal(hover)));
        return component.style(style.build());
    }

    // ------------------------------------------------------------------
    // internals
    // ------------------------------------------------------------------

    private static final Pattern TAG_STRIPPER = Pattern.compile("<[^<>]*>");

    /**
     * Rewrites {@code {key}} tokens into the {@code <key>} tag form. Only tokens
     * that have a value are rewritten, so a literal brace in a sentence is left
     * alone.
     */
    static final class Token {
        private Token() {
        }

        static String braceToTag(String template, Args args) {
            if (template.indexOf('{') < 0 || args == null || args.isEmpty())
                return template;
            StringBuilder out = new StringBuilder(template.length());
            Matcher matcher = TOKEN.matcher(template);
            int last = 0;
            while (matcher.find()) {
                out.append(template, last, matcher.start());
                String key = matcher.group(1);
                // Only rewrite well known arguments, so text such as a JSON
                // snippet inside a tooltip is not touched.
                if (args.get(key) != null)
                    out.append('<').append(key).append('>');
                else
                    out.append(matcher.group());
                last = matcher.end();
            }
            out.append(template, last, template.length());
            return out.toString();
        }
    }

    private static TagResolver resolver(Args args) {
        TagResolver.Builder builder = TagResolver.builder();
        for (java.util.Map.Entry<String, Component> entry : args.values.entrySet())
            builder.resolver(net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.component(entry.getKey(), entry.getValue()));
        return builder.build();
    }

    /**
     * Fallback used when a template cannot be parsed: replaces the tokens of the
     * already rendered values one by one.
     */
    private static Component substitute(String template, Args args) {
        if (args == null || args.isEmpty())
            return literal(template);
        Component result = Component.empty();
        Matcher matcher = TOKEN.matcher(template);
        int last = 0;
        while (matcher.find()) {
            result = result.append(literal(template.substring(last, matcher.start())));
            Component replacement = args.get(matcher.group(1));
            result = result.append(replacement == null ? literal(matcher.group()) : replacement);
            last = matcher.end();
        }
        return result.append(literal(template.substring(last)));
    }
}
