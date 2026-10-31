package com.bekvon.bukkit.residence.gui.ui.data;

/**
 * Defines how the menu should resolve values for one {@code /res} argument.
 */
public class ArgDef {

    private final ArgType type;
    private String label;
    private String[] choices = new String[0];
    private int min = 1;
    private int max = 1000;
    private int step = 1;
    private boolean optional = false;
    /**
     * When true the argument accepts every remaining token of the command line,
     * e.g. {@code /res message <res> enter "Hello there friend"}.
     */
    private boolean rest = false;
    /**
     * Skip this argument and continue with the next one automatically.
     * Used for sub commands that do not need every positional slot.
     */
    private boolean skipped = false;

    private ArgDef(ArgType type) {
        this.type = type;
    }

    /**
     * One of a fixed set of literal words.
     *
     * @param labelKey translation key suffix; when no translation exists the raw
     *                 label is shown
     */
    public static ArgDef choice(String labelKey, String... choices) {
        ArgDef def = new ArgDef(ArgType.CHOICE);
        def.label = labelKey;
        def.choices = choices == null ? new String[0] : choices;
        return def;
    }

    public static ArgDef flagState() {
        ArgDef def = new ArgDef(ArgType.FLAG_STATE);
        def.label = "State";
        def.choices = new String[] { "true", "false", "remove" };
        return def;
    }

    public static ArgDef residence() {
        ArgDef def = new ArgDef(ArgType.RESIDENCE);
        def.label = "Residence";
        return def;
    }

    public static ArgDef player() {
        ArgDef def = new ArgDef(ArgType.PLAYER);
        def.label = "Player";
        return def;
    }

    public static ArgDef flag() {
        ArgDef def = new ArgDef(ArgType.FLAG);
        def.label = "Flag";
        return def;
    }

    public static ArgDef group() {
        ArgDef def = new ArgDef(ArgType.GROUP);
        def.label = "Group";
        return def;
    }

    public static ArgDef world() {
        ArgDef def = new ArgDef(ArgType.WORLD);
        def.label = "World";
        return def;
    }

    /**
     * A number picked with the +/- buttons.
     *
     * @param labelKey translation key suffix, e.g. {@code amount}
     */
    public static ArgDef amount(String labelKey, int min, int max) {
        ArgDef def = new ArgDef(ArgType.AMOUNT);
        def.label = labelKey;
        def.min = min;
        def.max = max;
        def.step = 1;
        // A number is confirmed by clicking "run", so it is never optional by itself.
        def.optional = false;
        return def;
    }

    public static ArgDef amount(String labelKey, int min, int max, int step) {
        ArgDef def = amount(labelKey, min, max);
        def.step = Math.max(1, step);
        return def;
    }

    public static ArgDef text(String labelKey) {
        ArgDef def = new ArgDef(ArgType.TEXT);
        def.label = labelKey;
        return def;
    }

    public static ArgDef rawText(String labelKey) {
        ArgDef def = new ArgDef(ArgType.RAW_TEXT);
        def.label = labelKey;
        return def;
    }

    public ArgDef optional(boolean optional) {
        this.optional = optional;
        return this;
    }

    public ArgDef rest(boolean rest) {
        this.rest = rest;
        return this;
    }

    public ArgDef skipped(boolean skipped) {
        this.skipped = skipped;
        return this;
    }

    public ArgDef choices(String... choices) {
        this.choices = choices == null ? new String[0] : choices;
        return this;
    }

    public ArgType getType() {
        return type;
    }

    /**
     * Internal English label. Never shown to a player: it is only used to build
     * the usage line and the translation key, so translating a language file
     * never changes the command that is executed.
     */
    public String getLabel() {
        return label == null ? type.name() : label;
    }

    /**
     * Lower case identifier used to build the translation key, e.g.
     * {@code residence} for the residence argument.
     */
    public String getLabelKey() {
        return getLabel().toLowerCase(java.util.Locale.ENGLISH).replace(' ', '_');
    }

    public String[] getChoices() {
        return choices;
    }

    public int getMin() {
        return min;
    }

    public int getMax() {
        return max;
    }

    public int getStep() {
        return step;
    }

    public boolean isOptional() {
        return optional;
    }

    public boolean isRest() {
        return rest;
    }

    public boolean isSkipped() {
        return skipped;
    }

    public boolean isNumeric() {
        return type == ArgType.AMOUNT;
    }
}
