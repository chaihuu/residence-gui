package com.bekvon.bukkit.residence.gui.ui.data;

/**
 * What a click on an item, a footer button or a form button does.
 * <p>
 * Actions are plain value objects. The data layer produces them; the chest
 * renderer turns them into slot behaviour and the Bedrock renderer into a form
 * button index. No business logic lives in either renderer.
 */
public final class MenuAction {

    public enum Type {
        /** Do nothing (informative button). */
        NONE,
        /** Open a screen: value is {@code kind} or {@code kind:target}. */
        OPEN,
        /** Start a command wizard: value is the command name. */
        COMMAND,
        /** Change the page: amount holds the page number. */
        PAGE,
        /** Go back one screen. */
        BACK,
        /** Close the menu. */
        CLOSE,
        /** Switch to a language: value is the language code. */
        LANGUAGE,
        /** Open the language picker. */
        LANGUAGE_PICKER,
        /** Number picker: amount holds the delta. */
        VALUE_STEP,
        /** Accept the current number. */
        VALUE_CONFIRM,
        /** Ask for free text in chat. */
        CHAT_INPUT,
        /** Put the command into the chat box without running it. */
        SUGGEST,
        /** Run the assembled command. */
        EXECUTE
    }

    private final Type type;
    private final String value;
    private final int amount;
    private final boolean dangerous;

    private MenuAction(Type type, String value, int amount, boolean dangerous) {
        this.type = type;
        this.value = value;
        this.amount = amount;
        this.dangerous = dangerous;
    }

    public static MenuAction none() {
        return new MenuAction(Type.NONE, null, 0, false);
    }

    public static MenuAction open(String screen) {
        return new MenuAction(Type.OPEN, screen, 0, false);
    }

    public static MenuAction command(String command) {
        return new MenuAction(Type.COMMAND, command, 0, false);
    }

    public static MenuAction page(int page) {
        return new MenuAction(Type.PAGE, String.valueOf(page), page, false);
    }

    public static MenuAction back() {
        return new MenuAction(Type.BACK, null, 0, false);
    }

    public static MenuAction close() {
        return new MenuAction(Type.CLOSE, null, 0, false);
    }

    public static MenuAction language(String code) {
        return new MenuAction(Type.LANGUAGE, code, 0, false);
    }

    public static MenuAction languagePicker() {
        return new MenuAction(Type.LANGUAGE_PICKER, null, 0, false);
    }

    public static MenuAction step(int delta) {
        return new MenuAction(Type.VALUE_STEP, null, delta, false);
    }

    public static MenuAction valueConfirm(int value) {
        return new MenuAction(Type.VALUE_CONFIRM, String.valueOf(value), value, false);
    }

    public static MenuAction chatInput() {
        return new MenuAction(Type.CHAT_INPUT, null, 0, false);
    }

    public static MenuAction suggest(String command) {
        return new MenuAction(Type.SUGGEST, command, 0, false);
    }

    public static MenuAction execute(boolean unsafe) {
        return new MenuAction(Type.EXECUTE, null, 0, unsafe);
    }

    public Type getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    public int getAmount() {
        return amount;
    }

    /**
     * Whether the action destroys data; the confirmation screen paints its
     * confirm button red for these.
     */
    public boolean isDangerous() {
        return dangerous;
    }

    /**
     * Screen kind of an {@link Type#OPEN} action.
     */
    public String screenKind() {
        if (value == null)
            return "";
        int index = value.indexOf(':');
        return index < 0 ? value : value.substring(0, index);
    }

    /**
     * Screen target of an {@link Type#OPEN} action, may be {@code null}.
     */
    public String screenTarget() {
        if (value == null)
            return null;
        int index = value.indexOf(':');
        return index < 0 ? null : value.substring(index + 1);
    }

    @Override
    public String toString() {
        return type + (value == null ? "" : "(" + value + ")");
    }
}
